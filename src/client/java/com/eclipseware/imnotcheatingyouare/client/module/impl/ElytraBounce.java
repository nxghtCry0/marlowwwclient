package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.utils.ModuleUtils;
import com.eclipseware.imnotcheatingyouare.mixin.client.MinecraftAccessor;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class ElytraBounce extends Module {

    private int airTicks = 0;
    private boolean jumpPulsed = false;

    public ElytraBounce() {
        super("ElytraBounce", Category.Movement, "Automates sprint jump, elytra glide activation, and ground impact bouncing.");
        
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Auto Equip Elytra", this, true));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Min Fall Height", this, 1.2, 0.5, 3.0, false));
    }

    @Override
    public void onEnable() {
        resetState();
    }

    @Override
    public void onDisable() {
        resetState();
    }

    @Override
    public void onTick() {
        if (!isToggled() || mc.player == null || mc.level == null || mc.options == null) return;

        ItemStack chestStack = mc.player.getItemBySlot(EquipmentSlot.CHEST);
        boolean wearingElytra = chestStack.is(Items.ELYTRA);

        if (!wearingElytra) {
            if (getBoolSetting("Auto Equip Elytra")) {
                if (!autoEquipElytra()) return;
            } else {
                return;
            }
        }

        if (jumpPulsed) {
            mc.options.keyJump.setDown(false);
            jumpPulsed = false;
        }

        if (mc.player.onGround()) {
            airTicks = 0;
            if (mc.options.keyUp != null && mc.options.keyUp.isDown()) {
                mc.options.keyJump.setDown(true);
                jumpPulsed = true;
            }
        } else {
            airTicks++;

            if (!mc.player.isFallFlying()) {
                if (airTicks >= 2 && mc.player.getDeltaMovement().y < 0.0) {
                    mc.player.tryToStartFallFlying();
                }
            }
        }
    }

    private boolean autoEquipElytra() {
        if (mc.player == null || mc.gameMode == null) return false;

        int hotbarSlot = ModuleUtils.findItemInHotbar(Items.ELYTRA);
        if (hotbarSlot != -1) {
            int originalSlot = ModuleUtils.getSelectedSlot();
            ModuleUtils.switchToSlot(hotbarSlot);
            ((MinecraftAccessor) mc).invokeStartUseItem();
            mc.player.swing(InteractionHand.MAIN_HAND);
            ModuleUtils.switchToSlot(originalSlot);
            return true;
        }

        return false;
    }

    private double getGroundY() {
        if (mc.player == null || mc.level == null) return 0.0;
        Vec3 pos = mc.player.position();
        AABB box = mc.player.getBoundingBox();
        for (double y = pos.y; y > pos.y - 3.0; y -= 0.1) {
            AABB testBox = box.move(0, y - pos.y, 0);
            if (!mc.level.noCollision(mc.player, testBox)) {
                return y;
            }
        }
        return pos.y;
    }

    private void resetState() {
        airTicks = 0;
        if (mc != null && mc.options != null && jumpPulsed) {
            mc.options.keyJump.setDown(false);
            jumpPulsed = false;
        }
    }

    private boolean getBoolSetting(String name) {
        Setting s = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, name);
        return s != null && s.getValBoolean();
    }

    private double getDoubleSetting(String name) {
        Setting s = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, name);
        return s != null ? s.getValDouble() : 1.2;
    }
}
