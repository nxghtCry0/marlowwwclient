package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.utils.FriendManager;
import com.eclipseware.imnotcheatingyouare.client.utils.MouseAimHelper;
import com.eclipseware.imnotcheatingyouare.client.utils.RotationManager;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Random;

public class AimAssist extends Module {
    private Entity target;
    private final Random random = new Random();

    public AimAssist() {
        super("AimAssist", Category.Combat, "Automatically aims at entities with Grim AC v3 bypass.");

        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Aim Speed", this, 5.0, 0.1, 20.0, false));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Max Rotation Delta", this, 2.5, 0.5, 10.0, false));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Noise", this, 0.5, 0.0, 3.0, false));
        
        ArrayList<String> bodyTargets = new ArrayList<>(Arrays.asList("Head", "Body"));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Target Area", this, "Body", bodyTargets));
        
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Range", this, 5.0, 0.0, 10.0, false));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("FOV", this, 360.0, 0.0, 360.0, true));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Ignore Walls", this, false));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Click Aim", this, false));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Weapon Only", this, false));
    }

    @Override
    public void onDisable() {
        target = null;
        MouseAimHelper.clearAimRate();
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.level == null) return;
        
        Setting clickAimSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Click Aim");
        if (clickAimSetting != null && clickAimSetting.getValBoolean() && !mc.options.keyAttack.isDown()) {
            target = null;
            MouseAimHelper.clearAimRate();
            return;
        }

        Setting weaponOnlySetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Weapon Only");
        if (mc.screen != null || (weaponOnlySetting != null && weaponOnlySetting.getValBoolean() && !isHoldingWeapon())) {
            target = null;
            MouseAimHelper.clearAimRate();
            return;
        }

        updateTarget();

        if (target == null) {
            MouseAimHelper.clearAimRate();
            return;
        }

        Setting bodyTargetSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Target Area");
        String bodyTarget = bodyTargetSetting != null ? bodyTargetSetting.getValString() : "Body";

        Vec3 targetPos = target.position();
        if ("Head".equals(bodyTarget)) {
            targetPos = targetPos.add(0, target.getEyeHeight(), 0);
        } else {
            targetPos = targetPos.add(0, target.getEyeHeight() / 2.0f, 0);
        }

        double deltaX = targetPos.x - mc.player.getX();
        double deltaZ = targetPos.z - mc.player.getZ();
        double deltaY = targetPos.y - (mc.player.getY() + mc.player.getEyeHeight());

        Setting noiseSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Noise");
        float noise = noiseSetting != null ? (float) noiseSetting.getValDouble() : 0.5f;

        Setting speedSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Aim Speed");
        float speed = speedSetting != null ? (float) speedSetting.getValDouble() : 5.0f;

        Setting maxDeltaSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Max Rotation Delta");
        float maxDelta = maxDeltaSetting != null ? (float) maxDeltaSetting.getValDouble() : 2.5f;

        // Yaw calculation
        double targetYaw = Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90;
        if (noise > 0.0) {
            targetYaw += (random.nextFloat() - 0.5f) * noise * 2.0;
        }

        double deltaYaw = Mth.wrapDegrees(targetYaw - mc.player.getYRot());
        double toRotateYaw = Math.min(Math.abs(deltaYaw), speed);
        toRotateYaw = Math.copySign(toRotateYaw, deltaYaw);
        
        if (Math.abs(toRotateYaw) > maxDelta) {
            toRotateYaw = Math.copySign(maxDelta, toRotateYaw);
        }

        // Pitch calculation
        double horizontalDist = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        double targetPitch = -Math.toDegrees(Math.atan2(deltaY, horizontalDist));
        if (noise > 0.0) {
            targetPitch += (random.nextFloat() - 0.5f) * noise * 2.0;
        }

        double deltaPitch = Mth.wrapDegrees(targetPitch - mc.player.getXRot());
        double toRotatePitch = Math.min(Math.abs(deltaPitch), speed);
        toRotatePitch = Math.copySign(toRotatePitch, deltaPitch);
        
        if (Math.abs(toRotatePitch) > maxDelta) {
            toRotatePitch = Math.copySign(maxDelta, toRotatePitch);
        }

        // Feed to hardware mouse simulator! 
        // toRotateYaw and toRotatePitch are the desired degrees of rotation for THIS TICK (50ms).
        MouseAimHelper.setAimRate(toRotateYaw, toRotatePitch);
    }

    private void updateTarget() {
        Entity bestEntity = null;
        double minDistance = Double.MAX_VALUE;

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (isValidTarget(entity)) {
                double dist = mc.player.distanceTo(entity);
                if (dist < minDistance) {
                    bestEntity = entity;
                    minDistance = dist;
                }
            }
        }
        target = bestEntity;
    }

    private boolean isValidTarget(Entity entity) {
        if (!(entity instanceof LivingEntity) || entity == mc.player || !entity.isAlive()) {
            return false;
        }

        Setting rangeSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Range");
        double range = rangeSetting != null ? rangeSetting.getValDouble() : 5.0;
        if (mc.player.distanceTo(entity) > range) {
            return false;
        }

        Setting ignoreWallsSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Ignore Walls");
        boolean ignoreWalls = ignoreWallsSetting != null && ignoreWallsSetting.getValBoolean();
        if (!ignoreWalls && !mc.player.hasLineOfSight(entity)) {
            return false;
        }

        if (entity instanceof Player p && FriendManager.isFriend(p)) {
            return false;
        }

        Setting fovSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "FOV");
        double maxFov = fovSetting != null ? fovSetting.getValDouble() : 360.0;
        
        return isInFov(entity, maxFov);
    }

    private boolean isInFov(Entity entity, double fov) {
        if (fov >= 360.0) return true;
        Vec3 entityPos = entity.position();
        Vec3 playerPos = mc.player.getEyePosition();
        Vec3 direction = entityPos.subtract(playerPos).normalize();
        double yaw = Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90;
        double pitch = -Math.toDegrees(Math.asin(direction.y));
        double yawDiff = Mth.wrapDegrees(yaw - mc.player.getYRot());
        double pitchDiff = Mth.wrapDegrees(pitch - mc.player.getXRot());
        return Math.sqrt(yawDiff * yawDiff + pitchDiff * pitchDiff) <= fov / 2.0;
    }

    private boolean isHoldingWeapon() {
        if (mc.player == null) return false;
        net.minecraft.world.item.Item item = mc.player.getMainHandItem().getItem();
        String name = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).getPath().toLowerCase();
        return name.contains("sword") || name.contains("axe");
    }
}
