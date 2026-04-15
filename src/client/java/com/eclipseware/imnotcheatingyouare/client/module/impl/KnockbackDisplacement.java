package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.HitResult;

public class KnockbackDisplacement extends Module {

    private boolean armed = false;
    private int delayTicks = 0;
    private int cooldown = 0;
    private boolean wasAttackDown = false;

    public KnockbackDisplacement() {
        super("KBDisplacement", Category.Combat, "Hit air while sprinting to arm, next hit pulls target back.");
    }

    /**
     * Detects air swings (attack pressed, not looking at entity) while sprinting
     * and arms the module. Next entity attack will send a Rot packet with reversed
     * yaw so the server calculates knockback toward you instead of away.
     *
     * Rot packets (no position data) avoid survival fly flags entirely.
     * Camera never moves — only server-side rotation state changes.
     */
    @Override
    public void onTick() {
        if (mc.player == null || mc.level == null || mc.screen != null) return;

        if (cooldown > 0) { cooldown--; return; }

        if (armed) {
            delayTicks++;
            Setting delaySetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Delay (Ticks)");
            int maxDelay = delaySetting != null ? (int) delaySetting.getValDouble() : 2;
            if (delayTicks > maxDelay) {
                armed = false;
                delayTicks = 0;
            }
            return;
        }

        boolean isDown = mc.options.keyAttack.isDown();
        boolean justClicked = isDown && !wasAttackDown;
        wasAttackDown = isDown;

        if (justClicked && mc.player.isSprinting()) {
            boolean lookingAtEntity = mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.ENTITY;
            if (!lookingAtEntity) {
                Setting autoSprint = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Auto Sprint");
                if (autoSprint != null && autoSprint.getValBoolean()) {
                    mc.player.setSprinting(true);
                }
                armed = true;
                delayTicks = 0;
            }
        }
    }

    public float[] getFlipRotation(Entity target) {
        if (!armed || cooldown > 0) return null;

        double dx = target.getX() - mc.player.getX();
        double dz = target.getZ() - mc.player.getZ();
        float yawToTarget = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0F);

        Setting modeSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Mode");
        String mode = modeSetting != null ? modeSetting.getValString() : "Pull";

        float yaw, pitch;
        switch (mode) {
            case "Pull" -> {
                // Face target + 180° → server KB vector reverses → target pulled back
                yaw = yawToTarget + 180.0F;
                pitch = mc.player.getXRot();
            }
            case "Upward" -> {
                yaw = yawToTarget;
                pitch = -70.0F;
            }
            case "Horizontal" -> {
                yaw = yawToTarget + 90.0F;
                pitch = mc.player.getXRot();
            }
            default -> {
                Setting cy = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Custom Yaw");
                Setting cp = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Custom Pitch");
                yaw = mc.player.getYRot() + (cy != null ? (float) cy.getValDouble() : 0.0f);
                pitch = mc.player.getXRot() + (cp != null ? (float) cp.getValDouble() : 0.0f);
            }
        }

        armed = false;
        delayTicks = 0;
        Setting cdSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Cooldown (Ticks)");
        cooldown = cdSetting != null ? (int) cdSetting.getValDouble() : 15;

        return new float[]{Mth.wrapDegrees(yaw), Mth.clamp(pitch, -90.0F, 90.0F)};
    }

    @Override
    public void onDisable() {
        armed = false;
        delayTicks = 0;
        cooldown = 0;
    }
}