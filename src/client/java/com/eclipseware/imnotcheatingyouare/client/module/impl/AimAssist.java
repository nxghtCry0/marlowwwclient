package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.utils.FriendManager;
import com.eclipseware.imnotcheatingyouare.client.utils.SilentAim;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class AimAssist extends Module {

    private Entity target;
    private int clickGraceTicks = 0;

    private float yawVelocity = 0f;
    private float pitchVelocity = 0f;

    private long targetAcquiredTime = 0;
    private Entity lastTarget = null;

    private float overflickOffset = 0f;
    private long lastOverflickTime = 0;

    private float breathPhase = 0f;

    public AimAssist() {
        super("AimAssist", Category.Combat);
    }

    @Override
    public void onTick() {
        if (mc == null || mc.player == null || mc.level == null || mc.screen != null) {
            resetPhysics();
            return;
        }

        if (mc.options.keyAttack.isDown()) {
            clickGraceTicks = 5;
        } else if (clickGraceTicks > 0) {
            clickGraceTicks--;
        }

        Setting attackOnlySetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Attack Only");
        if (attackOnlySetting != null && attackOnlySetting.getValBoolean()) {
            if (clickGraceTicks == 0) {
                resetPhysics();
                return;
            }
        }

        chooseTarget();
        if (target == null) {
            yawVelocity *= 0.75f;
            pitchVelocity *= 0.75f;
            if (Math.abs(yawVelocity) < 0.01f) yawVelocity = 0;
            if (Math.abs(pitchVelocity) < 0.01f) pitchVelocity = 0;
            return;
        }

        if (target != lastTarget) {
            targetAcquiredTime = System.currentTimeMillis();
            lastTarget = target;
        }

        Setting delaySetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Reaction Delay (ms)");
        long reactionDelay = delaySetting != null ? (long) delaySetting.getValDouble() : 50;
        if (System.currentTimeMillis() - targetAcquiredTime < reactionDelay) return;

        AABB box = target.getBoundingBox();
        Vec3 aimPoint = new Vec3(box.getCenter().x, box.getCenter().y, box.getCenter().z);
        Vec3 eyes = mc.player.getEyePosition();

        double diffX = aimPoint.x - eyes.x;
        double diffY = aimPoint.y - eyes.y;
        double diffZ = aimPoint.z - eyes.z;
        double dist = Math.sqrt(diffX * diffX + diffZ * diffZ);

        float neededYaw = (float) (Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0F);
        float neededPitch = (float) -Math.toDegrees(Math.atan2(diffY, dist));

        float currentYaw = mc.player.getYRot();
        float currentPitch = mc.player.getXRot();

        float yawDiff = Mth.wrapDegrees(neededYaw - currentYaw);
        float pitchDiff = Mth.wrapDegrees(neededPitch - currentPitch);

        Setting modeSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Mode");
        Setting speedSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Speed");
        Setting smoothnessSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Smoothness");

        String mode = modeSetting != null ? modeSetting.getValString() : "Spring";
        float speed = speedSetting != null ? (float) speedSetting.getValDouble() : 3.0f;
        float smoothness = smoothnessSetting != null ? (float) smoothnessSetting.getValDouble() : 7.0f;

        breathPhase += 0.06f + (float) (Math.random() * 0.01);
        float breathYaw = (float) Math.sin(breathPhase) * 0.06f;
        float breathPitch = (float) Math.cos(breathPhase * 0.73f + 1.2f) * 0.04f;

        float stepYaw = 0f;
        float stepPitch = 0f;

        if (mode.equals("Spring")) {
            float springK = speed * 14.0f;
            float dampRatio = 0.4f + (smoothness / 10.0f) * 1.2f;
            float dampK = dampRatio * 2.0f * (float) Math.sqrt(springK);

            float yawAccel = springK * (yawDiff / 180.0f) - dampK * yawVelocity;
            float pitchAccel = springK * (pitchDiff / 180.0f) - dampK * pitchVelocity;

            yawVelocity += yawAccel * 0.016f;
            pitchVelocity += pitchAccel * 0.016f;

            float maxVel = speed * 6.0f;
            yawVelocity = Mth.clamp(yawVelocity, -maxVel, maxVel);
            pitchVelocity = Mth.clamp(pitchVelocity, -maxVel, maxVel);

            stepYaw = yawVelocity + breathYaw;
            stepPitch = pitchVelocity + breathPitch;

        } else if (mode.equals("Smooth")) {
            float ease = Math.max(1.0f, smoothness * 1.8f);
            float maxTurn = speed * 2.8f;

            float targetYawVel = yawDiff / ease;
            float targetPitchVel = pitchDiff / ease;

            float momentumBlend = 1.0f - (1.0f / (smoothness * 0.6f + 1.0f));
            yawVelocity = yawVelocity * momentumBlend + targetYawVel * (1.0f - momentumBlend);
            pitchVelocity = pitchVelocity * momentumBlend + targetPitchVel * (1.0f - momentumBlend);

            stepYaw = Mth.clamp(yawVelocity + breathYaw, -maxTurn, maxTurn);
            stepPitch = Mth.clamp(pitchVelocity + breathPitch, -maxTurn, maxTurn);

        } else {
            float maxTurn = speed * 2.5f;
            float lerpFactor = Math.min(1.0f, speed * 0.12f);
            yawVelocity = Mth.clamp(yawDiff * lerpFactor, -maxTurn, maxTurn);
            pitchVelocity = Mth.clamp(pitchDiff * lerpFactor, -maxTurn, maxTurn);
            stepYaw = yawVelocity + breathYaw;
            stepPitch = pitchVelocity + breathPitch;
        }

        Setting doOverflick = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Overflick");
        if (doOverflick != null && doOverflick.getValBoolean()) {
            Setting ofPower = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Overflick Power");
            float power = ofPower != null ? (float) ofPower.getValDouble() : 3.0f;

            if (Math.abs(yawDiff) > 8.0f && System.currentTimeMillis() - lastOverflickTime > 700) {
                if (Math.random() < 0.07) {
                    float intensity = power * (0.4f + (float) Math.random() * 0.6f);
                    overflickOffset = Math.signum(yawDiff) * intensity;
                    lastOverflickTime = System.currentTimeMillis();
                }
            }

            if (Math.abs(overflickOffset) > 0.05f) {
                stepYaw += overflickOffset;
                overflickOffset *= 0.45f;
            } else {
                overflickOffset = 0f;
            }
        }

        Setting stopSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Stop On Target");
        if (stopSetting != null && stopSetting.getValBoolean()) {
            double angleToTarget = getAngleToLookVec(aimPoint);
            if (angleToTarget < 4.0) {
                float reduction = (float) (angleToTarget / 4.0);
                reduction *= reduction;
                stepYaw *= reduction;
                stepPitch *= reduction;
            }
        }

        if (Math.abs(stepYaw) > 0.001f || Math.abs(stepPitch) > 0.001f) {
            Setting randSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Randomness");
            float randVal = randSetting != null ? (float) randSetting.getValDouble() : 3.0f;
            float maxJitter = (randVal / 10.0f) * 0.3f;

            stepYaw *= (1.0f - maxJitter/2f) + (float) Math.random() * maxJitter;
            stepPitch *= (1.0f - maxJitter/2f) + (float) Math.random() * maxJitter;

            float gcd = com.eclipseware.imnotcheatingyouare.client.utils.RotationManager.getGCD();
            if (gcd < 0.001f) gcd = 0.15f;

            int yawSteps = Math.round(stepYaw / gcd);
            int pitchSteps = Math.round(stepPitch / gcd);
            
            float stepJitterChance = (randVal / 10.0f) * 0.2f;
            if (yawSteps != 0 && Math.random() < stepJitterChance) yawSteps += (Math.random() < 0.5 ? 1 : -1);
            if (pitchSteps != 0 && Math.random() < stepJitterChance) pitchSteps += (Math.random() < 0.5 ? 1 : -1);

            stepYaw = yawSteps * gcd;
            stepPitch = pitchSteps * gcd;

            if (yawSteps == 0 && pitchSteps == 0) return;

            Setting silentAimSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Silent Aim");
            boolean useSilent = silentAimSetting != null && silentAimSetting.getValBoolean();

            float newYaw = currentYaw + stepYaw;
            float newPitch = Mth.clamp(currentPitch + stepPitch, -90.0F, 90.0F);

            if (useSilent && mc.options.keyAttack.isDown()) {
                SilentAim.setRotation(newYaw, newPitch, 2);
            } else {
                mc.player.setYRot(newYaw);
                mc.player.setXRot(newPitch);
            }
        }
    }

    private void resetPhysics() {
        target = null;
        yawVelocity *= 0.5f;
        pitchVelocity *= 0.5f;
    }

    private void chooseTarget() {
        Setting rangeSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Range");
        Setting fovSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "FOV");

        double range = rangeSetting != null ? rangeSetting.getValDouble() : 4.5;
        double maxFov = fovSetting != null ? fovSetting.getValDouble() : 120.0;

        Entity bestTarget = null;
        double bestAngle = maxFov / 2.0;

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity == mc.player || !entity.isAlive() || !(entity instanceof LivingEntity)) continue;
            if (mc.player.distanceTo(entity) > range) continue;
            if (entity instanceof Player p && FriendManager.isFriend(p)) continue;
            if (!isValidTarget(entity)) continue;

            AABB box = entity.getBoundingBox();
            Vec3 aimPoint = new Vec3(box.getCenter().x, box.getCenter().y, box.getCenter().z);
            double angle = getAngleToLookVec(aimPoint);

            if (angle <= bestAngle) {
                bestAngle = angle;
                bestTarget = entity;
            }
        }
        target = bestTarget;
    }

    private double getAngleToLookVec(Vec3 targetVec) {
        Vec3 lookVec = mc.player.getViewVector(1.0F);
        Vec3 diffVec = targetVec.subtract(mc.player.getEyePosition()).normalize();
        double dot = lookVec.dot(diffVec);
        dot = Mth.clamp(dot, -1.0, 1.0);
        return Math.toDegrees(Math.acos(dot));
    }

    private boolean isValidTarget(Entity entity) {
        Setting playersSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Players");
        Setting hostileSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Hostile Mobs");
        Setting passiveSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Passive Mobs");

        if (entity instanceof Player) {
            return playersSetting != null && playersSetting.getValBoolean();
        }
        if (entity instanceof Enemy) {
            return hostileSetting != null && hostileSetting.getValBoolean();
        }
        if (entity instanceof Animal || entity instanceof LivingEntity) {
            return passiveSetting != null && passiveSetting.getValBoolean();
        }
        return false;
    }
}