package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;

import java.util.ArrayList;

public class AutoElytraSwap extends Module {

    private boolean swapped = false;
    private int swappedFromSlot = -1;
    private int originalHotbarSlot = -1;

    public AutoElytraSwap() {
        super("Auto Elytra Swap", Category.Combat, "Automatically swaps Elytra with Chestplate when falling towards a target.");
        ArrayList<String> modes = new ArrayList<>();
        modes.add("Silent");
        modes.add("Interact");
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Mode", this, "Silent", modes));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Ticks Ahead", this, 3.0, 1.0, 10.0, false));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Min Fall Height", this, 2.0, 1.0, 10.0, false));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Target Players", this, true));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Target Mobs", this, false));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Swap Back", this, true));
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.level == null || mc.gameMode == null || mc.getConnection() == null) return;

        if (mc.player.onGround()) {
            if (swapped && getBoolSetting("Swap Back") && swappedFromSlot != -1) {
                performEquipmentSwap(swappedFromSlot);
            }
            resetState();
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

            if (speed > 0.05 && (distance / speed <= ticksAhead * 0.1)) {
                int chestplateSlot = findChestplateInInventory();
                if (chestplateSlot != -1) {
                    String mode = getStringSetting("Mode");
                    if ("Silent".equalsIgnoreCase(mode)) {
                        performEquipmentSwap(chestplateSlot);
                        swapped = true;
                        swappedFromSlot = chestplateSlot;
                    } else {
                        int hotbarSlot = findChestplateInHotbar();
                        if (hotbarSlot != -1) {
                            originalHotbarSlot = mc.player.getInventory().getSelectedSlot();
                            mc.getConnection().send(new ServerboundSetCarriedItemPacket(hotbarSlot));
                            mc.getConnection().send(new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, 0, mc.player.getYRot(), mc.player.getXRot()));
                            mc.player.swing(InteractionHand.MAIN_HAND);
                            mc.getConnection().send(new ServerboundSetCarriedItemPacket(originalHotbarSlot));
                            swapped = true;
                            swappedFromSlot = hotbarSlot + 36;
                        }
                    }
                }
            }
        } else {
            if (getBoolSetting("Swap Back") && swappedFromSlot != -1) {
                if (mc.player.getDeltaMovement().y > 0.1 || mc.player.onGround()) {
                    performEquipmentSwap(swappedFromSlot);
                    resetState();
                }
            }
        }
    }

    private void performEquipmentSwap(int targetSlot) {
        if (mc.player == null || mc.gameMode == null) return;
        int containerId = mc.player.inventoryMenu.containerId;
        int equipmentSlot = 6;
        mc.gameMode.handleContainerInput(containerId, targetSlot, 0, ContainerInput.PICKUP, mc.player);
        mc.gameMode.handleContainerInput(containerId, equipmentSlot, 0, ContainerInput.PICKUP, mc.player);
        mc.gameMode.handleContainerInput(containerId, targetSlot, 0, ContainerInput.PICKUP, mc.player);
    }

    private int findChestplateInInventory() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (isChestplate(stack)) return i + 36;
        }
        for (int i = 9; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (isChestplate(stack)) return i;
        }
        return -1;
    }

    private int findChestplateInHotbar() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (isChestplate(stack)) return i;
        }
        return -1;
    }

    private boolean isChestplate(ItemStack stack) {
        if (stack.isEmpty()) return false;
        net.minecraft.world.item.Item item = stack.getItem();
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
        swapped = false;
        swappedFromSlot = -1;
        originalHotbarSlot = -1;
    }

    @Override
    public void onDisable() {
        if (swapped && swappedFromSlot != -1 && mc.player != null && mc.gameMode != null) {
            performEquipmentSwap(swappedFromSlot);
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

    private String getStringSetting(String name) {
        Setting s = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, name);
        return s != null ? s.getValString() : "";
    }
}
