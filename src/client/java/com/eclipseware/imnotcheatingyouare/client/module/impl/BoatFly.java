package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;

public class BoatFly extends Module {
    public BoatFly() {
        super("BoatFly", Category.Blatant, "Allows boats to fly.");
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Speed", this, 1.0, 0.1, 5.0, false));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Up Speed", this, 0.5, 0.1, 2.0, false));
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.player.getVehicle() == null) return;

        if (mc.player.getVehicle().getType().toString().contains("boat")) {
            net.minecraft.world.entity.Entity boat = mc.player.getVehicle();
            Setting speedSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Speed");
            Setting upSpeedSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Up Speed");

            double speed = speedSetting != null ? speedSetting.getValDouble() : 1.0;
            double upSpeed = upSpeedSetting != null ? upSpeedSetting.getValDouble() : 0.5;

            boat.setNoGravity(true);

            net.minecraft.world.phys.Vec3 velocity = boat.getDeltaMovement();
            double motionX = velocity.x;
            double motionY = 0;
            double motionZ = velocity.z;

            if (mc.options.keyJump.isDown()) {
                motionY = upSpeed;
            } else if (mc.options.keySprint.isDown()) {
                motionY = -upSpeed;
            }

            if (mc.options.keyUp.isDown() || mc.options.keyDown.isDown() || mc.options.keyLeft.isDown() || mc.options.keyRight.isDown()) {
                float yaw = mc.player.getYRot();
                double radians = Math.toRadians(yaw);

                motionX = -Math.sin(radians) * speed;
                motionZ = Math.cos(radians) * speed;
            } else {
                motionX = 0;
                motionZ = 0;
            }

            boat.setDeltaMovement(new net.minecraft.world.phys.Vec3(motionX, motionY, motionZ));
        }
    }

    @Override
    public void onDisable() {
        if (mc.player != null && mc.player.getVehicle() != null && mc.player.getVehicle().getType().toString().contains("boat")) {
            net.minecraft.world.entity.Entity boat = mc.player.getVehicle();
            boat.setNoGravity(false);
        }
    }
}
