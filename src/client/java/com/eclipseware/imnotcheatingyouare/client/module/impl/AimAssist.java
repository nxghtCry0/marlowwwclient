package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class AimAssist extends Module {

    private Entity target;

    public AimAssist() {
        super("AimAssist", Category.Combat);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.level == null || mc.screen != null) {
            target = null;
            return;
        }

        // If "Stop On Target" is enabled and we are already looking at a valid target, do nothing
        Setting stopOnTarget = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Stop On Target");
        if (stopOnTarget != null && stopOnTarget.getValBoolean()) {
            if (mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.ENTITY) {
                if (isValidTarget(((EntityHitResult) mc.hitResult).getEntity())) {
                    target = null;
                    return;
                }
            }
        }

        // 1. Find best target
        target = findTarget();
        if (target == null) return;

        // 2. Calculate exact rotation to target
        Vec3 eyes = mc.player.getEyePosition();
        Vec3 targetPos = getAimPoint(target);
        
        double diffX = targetPos.x - eyes.x;
        double diffY = targetPos.y - eyes.y;
        double diffZ = targetPos.z - eyes.z;
        double dist = Math.sqrt(diffX * diffX + diffZ * diffZ);

        float targetYaw = (float) (Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0F);
        float targetPitch = (float) -Math.toDegrees(Math.atan2(diffY, dist));

        // 3. Get settings
        Setting modeSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Mode");
        Setting speedSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Speed");
        Setting smoothSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Smoothness");

        String mode = modeSetting != null ? modeSetting.getValString() : "Wind";
        double speed = speedSetting != null ? speedSetting.getValDouble() : 5.0;
        double smoothness = smoothSetting != null ? smoothSetting.getValDouble() : 5.0;

        // 4. Apply Wind Mode Jitter (Human-like oscillation)
        if (mode.equals("Wind")) {
            float time = System.currentTimeMillis() / 150.0f;
            float windStrength = (11.0f - (float)smoothness) * 0.5f;
            targetYaw += (float)(Math.sin(time) * windStrength);
            targetPitch += (float)(Math.cos(time * 0.7f) * windStrength * 0.5f);
        }

        // 5. Calculate rotation difference
        float yawDiff = Mth.wrapDegrees(targetYaw - mc.player.getYRot());
        float pitchDiff = Mth.wrapDegrees(targetPitch - mc.player.getXRot());

        // 6. Determine how much to turn this tick
        float yawStep, pitchStep;

        if (mode.equals("Snap")) {
            // Snap mode: Instantly set to target
            yawStep = yawDiff;
            pitchStep = pitchDiff;
        } else {
            // Smooth mode: Interpolate based on smoothness/speed
            double easeFactor = (11.0 - smoothness) * 2.0;
            yawStep = (float) (yawDiff / easeFactor);
            pitchStep = (float) (pitchDiff / easeFactor);

            // Cap maximum turn speed per tick
            float maxTurn = (float) (speed * 3.0);
            yawStep = Mth.clamp(yawStep, -maxTurn, maxTurn);
            pitchStep = Mth.clamp(pitchStep, -maxTurn, maxTurn);
        }

        // 7. DIRECT ROTATION APPLICATION (No mouse simulation)
        // This modifies the camera angle directly for smooth, native movement.
        mc.player.setYRot(mc.player.getYRot() + yawStep);
        mc.player.setXRot(mc.player.getXRot() + pitchStep);
    }

    private Vec3 getAimPoint(Entity entity) {
        // Aim at upper body/chest area
        return new Vec3(
            entity.getX(),
            entity.getY() + entity.getEyeHeight() * 0.75,
            entity.getZ()
        );
    }

    private Entity findTarget() {
        Setting rangeSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Range");
        Setting fovSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "FOV");

        double range = rangeSetting != null ? rangeSetting.getValDouble() : 4.0;
        double maxFov = fovSetting != null ? fovSetting.getValDouble() : 90.0;

        Entity bestTarget = null;
        double bestAngle = maxFov;

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!isValidTarget(entity)) continue;
            if (mc.player.distanceTo(entity) > range) continue;

            double angle = getAngleToEntity(entity);
            if (angle < bestAngle) {
                bestAngle = angle;
                bestTarget = entity;
            }
        }
        return bestTarget;
    }

    private double getAngleToEntity(Entity entity) {
        Vec3 eyes = mc.player.getEyePosition();
        Vec3 targetPos = getAimPoint(entity);
        double diffX = targetPos.x - eyes.x;
        double diffZ = targetPos.z - eyes.z;
        float targetYaw = (float) (Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0F);
        return Math.abs(Mth.wrapDegrees(targetYaw - mc.player.getYRot()));
    }

    private boolean isValidTarget(Entity entity) {
        if (!(entity instanceof LivingEntity)) return false;
        if (!entity.isAlive() || entity == mc.player) return false;

        Setting playersSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Players");
        Setting hostileSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Hostile Mobs");
        Setting passiveSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Passive Mobs");

        // Check for Players first (highest priority for PVP)
        if (entity instanceof net.minecraft.world.entity.player.Player) {
            return playersSetting != null && playersSetting.getValBoolean();
        }

        // Check for Hostile Mobs (Zombies, Skeletons, etc.)
        if (entity instanceof Enemy) {
            return hostileSetting != null && hostileSetting.getValBoolean();
        }

        // Check for Passive Mobs (Animals, Iron Golems, etc.)
        // Anything living that isn't a player or hostile is considered passive here
        if (entity instanceof Animal || entity instanceof LivingEntity) {
            return passiveSetting != null && passiveSetting.getValBoolean();
        }

        return false;
    }
}