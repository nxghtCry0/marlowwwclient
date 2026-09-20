package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.utils.ModuleUtils;
import com.eclipseware.imnotcheatingyouare.mixin.client.MinecraftAccessor;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;

public class ElytraSwapMacro extends Module {

    private int originalSlot = -1;
    private int targetSlot = -1;
    private int step = 0; 
    private boolean swapped = false;
    private long lastExecuteMs = 0L;

    public ElytraSwapMacro() {
        super("ElytraSwapMacro", Category.Mace, "Silently right-click swaps Elytra with Chestplate across multi-tick sequence like water bucket macros.");
        
        ArrayList<String> modes = new ArrayList<>();
        modes.add("RightClick");
        modes.add("Silent");
        
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Mode", this, "RightClick", modes));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Ticks Ahead", this, 3.0, 1.0, 10.0, false));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Min Fall Height", this, 2.0, 1.0, 10.0, false));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Target Players", this, true));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Target Mobs", this, false));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Swap Back", this, true));
    }

    @Override
    public void onEnable() {
        if (step != 0 || System.currentTimeMillis() - lastExecuteMs < 200L) {
            this.setToggled(false);
            return;
        }

        if (mc.player != null) {
            initiateSwapSequence();
        }
    }

    @Override
    public void onKeybind() {
        if (step == 0 && mc.player != null && System.currentTimeMillis() - lastExecuteMs >= 200L) {
            initiateSwapSequence();
        }
    }

    private void initiateSwapSequence() {
        ItemStack chestStack = mc.player.getItemBySlot(EquipmentSlot.CHEST);
        boolean wearingElytra = chestStack.is(Items.ELYTRA);
        targetSlot = wearingElytra ? findChestplateInHotbar() : findElytraInHotbar();

        if (targetSlot == -1) {
            resetState();
            this.setToggled(false);
            return;
        }

        originalSlot = ModuleUtils.getSelectedSlot();
        if (targetSlot != originalSlot) {
            ModuleUtils.switchToSlot(targetSlot);
        }
        step = 1;
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.level == null || mc.gameMode == null) {
            resetState();
            this.setToggled(false);
            return;
        }

        if (step == 1) {
            ((MinecraftAccessor) mc).invokeStartUseItem();
            mc.player.swing(InteractionHand.MAIN_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT, true);
            step = 2;
        } else if (step == 2) {
            if (originalSlot >= 0 && originalSlot < 9 && originalSlot != ModuleUtils.getSelectedSlot()) {
                ModuleUtils.switchToSlot(originalSlot);
            }
            lastExecuteMs = System.currentTimeMillis();
            swapped = true;
            resetState();
            this.setToggled(false);
        } else {
            handleAutoFallSwap();
        }
    }

    private void handleAutoFallSwap() {
        if (mc.player == null) return;

        if (mc.player.onGround()) {
            if (swapped && getBoolSetting("Swap Back") && step == 0) {
                initiateSwapSequence();
                swapped = false;
            }
            return;
        }

        ItemStack chestStack = mc.player.getItemBySlot(EquipmentSlot.CHEST);
        boolean wearingElytra = chestStack.is(Items.ELYTRA);

        if (!swapped) {
            if (!wearingElytra || !mc.player.isFallFlying() || mc.player.getDeltaMovement().y >= 0.0) {
                return;
            }

            LivingEntity target = findNearestTarget();
            if (target == null) return;

            double ticksAhead = getDoubleSetting("Ticks Ahead");
            double speed = mc.player.getDeltaMovement().length();
            double distance = mc.player.position().distanceTo(target.position());

            if (speed > 0.05 && (distance / speed <= ticksAhead * 0.1) && step == 0) {
                initiateSwapSequence();
            }
        } else {
            if (getBoolSetting("Swap Back") && step == 0) {
                if (mc.player.getDeltaMovement().y > 0.1 || mc.player.onGround()) {
                    initiateSwapSequence();
                    swapped = false;
                }
            }
        }
    }

    private int findChestplateInHotbar() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (isChestplate(stack)) return i;
        }
        return -1;
    }

    private int findElytraInHotbar() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.is(Items.ELYTRA)) return i;
        }
        return -1;
    }

    private boolean isChestplate(ItemStack stack) {
        if (stack.isEmpty()) return false;
        Item item = stack.getItem();
        return item == Items.NETHERITE_CHESTPLATE ||
               item == Items.DIAMOND_CHESTPLATE ||
               item == Items.IRON_CHESTPLATE ||
               item == Items.CHAINMAIL_CHESTPLATE ||
               item == Items.GOLDEN_CHESTPLATE ||
               item == Items.LEATHER_CHESTPLATE;
    }

    private LivingEntity findNearestTarget() {
        double closestDist = Double.MAX_VALUE;
        LivingEntity nearest = null;
        Vec3 pos = mc.player.position();
        AABB box = new AABB(
                pos.x - 15, pos.y - 30, pos.z - 15,
                pos.x + 15, pos.y + 10, pos.z + 15
        );
        for (Entity entity : mc.level.getEntities(mc.player, box)) {
            if (entity instanceof LivingEntity living) {
                if (isValidTarget(living)) {
                    double dist = pos.distanceTo(living.position());
                    if (dist < closestDist) {
                        closestDist = dist;
                        nearest = living;
                    }
                }
            }
        }
        return nearest;
    }

    private boolean isValidTarget(LivingEntity entity) {
        if (!entity.isAlive() || entity == mc.player) return false;
        if (entity instanceof Player) {
            return getBoolSetting("Target Players");
        } else {
            return getBoolSetting("Target Mobs");
        }
    }

    private void resetState() {
        originalSlot = -1;
        targetSlot = -1;
        step = 0;
    }

    @Override
    public void onDisable() {
        if (originalSlot >= 0 && originalSlot < 9 && mc.player != null && originalSlot != ModuleUtils.getSelectedSlot()) {
            ModuleUtils.switchToSlot(originalSlot);
        }
        resetState();
    }

    private double getDoubleSetting(String name) {
        Setting s = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, name);
        return s != null ? s.getValDouble() : 0.0;
    }

    private boolean getBoolSetting(String name) {
        Setting s = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, name);
        return s != null && s.getValBoolean();
    }
}
