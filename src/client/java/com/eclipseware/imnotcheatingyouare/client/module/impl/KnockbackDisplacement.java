package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.HitResult;

public class KnockbackDisplacement extends Module {

    private int cooldown = 0;

    public KnockbackDisplacement() {
        super("KBDisplacement", Category.Combat, "Silently displaces knockback direction on hit.");
    }

    @Override
    public void onTick() {
        if (cooldown > 0) cooldown--;
    }

    public float[] getFlipRotation(Entity target) {
        if (cooldown > 0) return null;

        double dx = target.getX() - mc.player.getX();
        double dz = target.getZ() - mc.player.getZ();
        float yawToTarget = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0F);

        Setting modeSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Mode");
        String mode = modeSetting != null ? modeSetting.getValString() : "Pull";

        float yaw, pitch;
        switch (mode) {
            case "Pull" -> {
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

        Setting cdSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Cooldown (Ticks)");
        cooldown = cdSetting != null ? (int) cdSetting.getValDouble() : 15;

        float gcd = com.eclipseware.imnotcheatingyouare.client.utils.RotationManager.getGCD();
        if (gcd < 0.001f) gcd = 0.15f;
        yaw = Math.round(yaw / gcd) * gcd;
        pitch = Math.round(pitch / gcd) * gcd;

        return new float[]{Mth.wrapDegrees(yaw), Mth.clamp(pitch, -90.0F, 90.0F)};
    }

    @Override
    public void onDisable() {
        cooldown = 0;
    }
}