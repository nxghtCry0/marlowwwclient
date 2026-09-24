package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class Freecam extends Module {
    public static Freecam INSTANCE;

    private Vec3 camPos = Vec3.ZERO;
    private Vec3 prevCamPos = Vec3.ZERO;
    private float camYaw, camPitch;
    private float lastHealth = -1.0f;
    private ClientLevel lastLevel;

    public Freecam() {
        super("Freecam", Category.Render, "Leaves your body behind while your camera flies freely.");
        INSTANCE = this;
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Horizontal Speed", this, 1.0, 0.1, 4.0, false));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Vertical Speed", this, 0.5, 0.1, 4.0, false));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Auto Disable on Damage", this, true));
    }

    public static boolean isActive() {
        return INSTANCE != null && INSTANCE.isToggled() && mc.player != null;
    }

    @Override
    public void onEnable() {
        if (mc.player == null || mc.level == null) {
            toggle();
            return;
        }
        camPos = mc.player.getEyePosition();
        prevCamPos = camPos;
        camYaw = mc.player.getYRot();
        camPitch = mc.player.getXRot();
        lastHealth = mc.player.getHealth();
        lastLevel = mc.level;
        reloadChunks();
    }

    @Override
    public void onDisable() {
        lastLevel = null;
        reloadChunks();
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.level == null || mc.level != lastLevel) {
            toggle();
            return;
        }

        Setting autoDisable = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Auto Disable on Damage");
        if (autoDisable != null && autoDisable.getValBoolean()) {
            float currentHealth = mc.player.getHealth();
            if (lastHealth >= 0.0f && currentHealth < lastHealth) {
                toggle();
                return;
            }
            lastHealth = currentHealth;
        }

        prevCamPos = camPos;
        if (mc.gui.screen() != null) return;

        double left = 0, forward = 0, up = 0;
        if (mc.options.keyLeft.isDown()) left += 1;
        if (mc.options.keyRight.isDown()) left -= 1;
        if (mc.options.keyUp.isDown()) forward += 1;
        if (mc.options.keyDown.isDown()) forward -= 1;
        if (mc.options.keyJump.isDown()) up += 1;
        if (mc.options.keyShift.isDown()) up -= 1;

        double length = Math.sqrt(left * left + forward * forward);
        if (length > 1.0) {
            left /= length;
            forward /= length;
        }

        double hSpeed = getDouble("Horizontal Speed", 1.0);
        double vSpeed = getDouble("Vertical Speed", 0.5);

        double yawRad = Math.toRadians(camYaw);
        double sin = Math.sin(yawRad);
        double cos = Math.cos(yawRad);
        double dx = (left * cos - forward * sin) * hSpeed;
        double dz = (left * sin + forward * cos) * hSpeed;

        camPos = camPos.add(dx, up * vSpeed, dz);
    }

    private double getDouble(String name, double fallback) {
        Setting s = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, name);
        return s != null ? s.getValDouble() : fallback;
    }

    private void reloadChunks() {
        if (mc.level != null) mc.levelExtractor.allChanged();
    }

    public Vec3 getCamPos(float partialTicks) {
        return new Vec3(
                Mth.lerp(partialTicks, prevCamPos.x, camPos.x),
                Mth.lerp(partialTicks, prevCamPos.y, camPos.y),
                Mth.lerp(partialTicks, prevCamPos.z, camPos.z)
        );
    }

    public float getCamYaw() {
        return camYaw;
    }

    public float getCamPitch() {
        return camPitch;
    }

    public void turn(double deltaYaw, double deltaPitch) {
        camYaw += (float) (deltaYaw * 0.15);
        camPitch = Mth.clamp(camPitch + (float) (deltaPitch * 0.15), -90f, 90f);
    }
}
