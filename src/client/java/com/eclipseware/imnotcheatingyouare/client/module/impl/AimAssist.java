package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.utils.FriendManager;
import com.eclipseware.imnotcheatingyouare.client.utils.MouseAimHelper;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Arrays;

public class AimAssist extends Module {
    private Entity target;
    private Entity lastTarget;
    private Vec3 targetOffset = Vec3.ZERO;

    public AimAssist() {
        super("AimAssist", Category.Combat, "Automatically aims at entities with Grim AC v3 bypass.");

        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Horizontal Speed", this, 20.0, 1.0, 100.0, false));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Vertical Speed", this, 20.0, 1.0, 100.0, false));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("FOV", this, 60.0, 0.0, 360.0, true));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Range", this, 4.0, 1.0, 8.0, false));

        ArrayList<String> bodyTargets = new ArrayList<>(Arrays.asList("Head", "Body", "Arms", "Legs", "Nearest Part"));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Target Area", this, "Body", bodyTargets));

        ArrayList<String> sortOptions = new ArrayList<>(Arrays.asList("Distance", "Angle"));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Sort By", this, "Distance", sortOptions));

        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Predict", this, false));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Prediction Ticks", this, 1.0, 0.0, 5.0, false));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Jitter", this, 0.0, 0.0, 5.0, false));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Aim Deviation", this, 0.0, 0.0, 1.0, false));

        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Click Aim", this, false));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Weapon Only", this, false));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Horizontal Only", this, false));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Team Check", this, true));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Ignore Walls", this, false));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Players", this, true));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Hostile Mobs", this, true));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Passive Mobs", this, false));
    }

    @Override
    public void onDisable() {
        target = null;
        lastTarget = null;
        targetOffset = Vec3.ZERO;
        MouseAimHelper.clearAimRate();
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.level == null) return;

        Setting clickAimSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Click Aim");
        if (clickAimSetting != null && clickAimSetting.getValBoolean() && !mc.options.keyAttack.isDown()) {
            target = null;
            lastTarget = null;
            targetOffset = Vec3.ZERO;
            MouseAimHelper.clearAimRate();
            return;
        }

        Setting weaponOnlySetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Weapon Only");
        if (mc.screen != null || (weaponOnlySetting != null && weaponOnlySetting.getValBoolean() && !isHoldingWeapon())) {
            target = null;
            lastTarget = null;
            targetOffset = Vec3.ZERO;
            MouseAimHelper.clearAimRate();
            return;
        }

        updateTarget();

        if (target == null) {
            lastTarget = null;
            targetOffset = Vec3.ZERO;
            MouseAimHelper.clearAimRate();
            return;
        }

        if (target != lastTarget) {
            lastTarget = target;
            Setting devSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Aim Deviation");
            double dev = devSetting != null ? devSetting.getValDouble() : 0.0;
            if (dev > 0.0) {
                targetOffset = new Vec3(
                    (Math.random() - 0.5) * dev,
                    (Math.random() - 0.5) * dev,
                    (Math.random() - 0.5) * dev
                );
            } else {
                targetOffset = Vec3.ZERO;
            }
        }

        Vec3 targetPos = getTargetBonePos(target).add(targetOffset);

        Setting predictSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Predict");
        if (predictSetting != null && predictSetting.getValBoolean()) {
            Setting predTicksSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Prediction Ticks");
            double predTicks = predTicksSetting != null ? predTicksSetting.getValDouble() : 1.0;
            Vec3 targetVel = target.getDeltaMovement();
            Vec3 playerVel = mc.player.getDeltaMovement();
            Vec3 relativeVel = targetVel.subtract(playerVel);
            targetPos = targetPos.add(relativeVel.scale(predTicks));
        }

        double deltaX = targetPos.x - mc.player.getX();
        double deltaZ = targetPos.z - mc.player.getZ();
        double deltaY = targetPos.y - (mc.player.getY() + mc.player.getEyeHeight());

        Setting hSpeedSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Horizontal Speed");
        float hSpeed = hSpeedSetting != null ? (float) hSpeedSetting.getValDouble() : 20.0f;
        Setting vSpeedSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Vertical Speed");
        float vSpeed = vSpeedSetting != null ? (float) vSpeedSetting.getValDouble() : 20.0f;

        double targetYaw = Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90;
        double deltaYaw = Mth.wrapDegrees(targetYaw - mc.player.getYRot());

        double horizontalDist = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        double targetPitch = -Math.toDegrees(Math.atan2(deltaY, horizontalDist));
        double deltaPitch = Mth.wrapDegrees(targetPitch - mc.player.getXRot());

        Setting horizOnlySetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Horizontal Only");
        if (horizOnlySetting != null && horizOnlySetting.getValBoolean()) {
            deltaPitch = 0.0;
        }

        Setting jitterSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Jitter");
        double jitter = jitterSetting != null ? jitterSetting.getValDouble() : 0.0;
        if (jitter > 0.0) {
            deltaYaw += (Math.random() - 0.5) * jitter;
            if (deltaPitch != 0.0) {
                deltaPitch += (Math.random() - 0.5) * jitter;
            }
        }

        double hFactor = hSpeed / 100.0;
        double vFactor = vSpeed / 100.0;
        MouseAimHelper.setAimRate(deltaYaw * hFactor, deltaPitch * vFactor);
    }

    private Vec3 getTargetBonePos(Entity ent) {
        Setting bodyTargetSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Target Area");
        String bodyTarget = bodyTargetSetting != null ? bodyTargetSetting.getValString() : "Body";

        Vec3 entPos = ent.position();
        double eyeHeight = ent.getEyeHeight();

        if ("Head".equals(bodyTarget)) {
            return entPos.add(0, eyeHeight, 0);
        } else if ("Body".equals(bodyTarget)) {
            return entPos.add(0, eyeHeight * 0.65, 0);
        } else if ("Arms".equals(bodyTarget)) {
            float yawRad = (float) Math.toRadians(ent.getYRot());
            double ox = Math.cos(yawRad) * 0.35;
            double oz = Math.sin(yawRad) * 0.35;
            Vec3 leftArm = entPos.add(ox, eyeHeight * 0.65, oz);
            Vec3 rightArm = entPos.add(-ox, eyeHeight * 0.65, -oz);
            return getClosest2D(leftArm, rightArm);
        } else if ("Legs".equals(bodyTarget)) {
            return entPos.add(0, eyeHeight * 0.25, 0);
        } else if ("Nearest Part".equals(bodyTarget)) {
            Vec3 head = entPos.add(0, eyeHeight, 0);
            Vec3 body = entPos.add(0, eyeHeight * 0.65, 0);
            Vec3 legs = entPos.add(0, eyeHeight * 0.25, 0);
            float yawRad = (float) Math.toRadians(ent.getYRot());
            double ox = Math.cos(yawRad) * 0.35;
            double oz = Math.sin(yawRad) * 0.35;
            Vec3 leftArm = entPos.add(ox, eyeHeight * 0.65, oz);
            Vec3 rightArm = entPos.add(-ox, eyeHeight * 0.65, -oz);
            return getClosest2D(head, body, leftArm, rightArm, legs);
        }
        return entPos.add(0, eyeHeight / 2.0f, 0);
    }

    private Vec3 getClosest2D(Vec3... positions) {
        Vec3 best = positions[0];
        double minDiff = Double.MAX_VALUE;
        for (Vec3 pos : positions) {
            double diff = get2DAngleDiff(pos);
            if (diff < minDiff) {
                minDiff = diff;
                best = pos;
            }
        }
        return best;
    }

    private double get2DAngleDiff(Vec3 pos) {
        Vec3 playerEye = mc.player.getEyePosition();
        Vec3 dir = pos.subtract(playerEye).normalize();
        double yaw = Math.toDegrees(Math.atan2(dir.z, dir.x)) - 90;
        double pitch = -Math.toDegrees(Math.asin(dir.y));
        double yawDiff = Mth.wrapDegrees(yaw - mc.player.getYRot());
        double pitchDiff = Mth.wrapDegrees(pitch - mc.player.getXRot());
        return Math.sqrt(yawDiff * yawDiff + pitchDiff * pitchDiff);
    }

    private void updateTarget() {
        Setting sortSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Sort By");
        String sortBy = sortSetting != null ? sortSetting.getValString() : "Distance";

        Entity bestEntity = null;
        double bestVal = Double.MAX_VALUE;

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (isValidTarget(entity)) {
                double val;
                if ("Angle".equals(sortBy)) {
                    val = get2DAngleDiff(entity.position().add(0, entity.getEyeHeight() * 0.65, 0));
                } else {
                    val = mc.player.distanceTo(entity);
                }
                if (val < bestVal) {
                    bestEntity = entity;
                    bestVal = val;
                }
            }
        }
        target = bestEntity;
    }

    private boolean isValidTarget(Entity entity) {
        if (!(entity instanceof LivingEntity) || entity == mc.player || !entity.isAlive()) {
            return false;
        }
        if (com.eclipseware.imnotcheatingyouare.client.utils.TargetFilterManager.isFiltered(entity)) {
            return false;
        }

        Setting rangeSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Range");
        double range = rangeSetting != null ? rangeSetting.getValDouble() : 4.0;
        if (mc.player.distanceTo(entity) > range) {
            return false;
        }

        Setting ignoreWallsSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Ignore Walls");
        boolean ignoreWalls = ignoreWallsSetting != null && ignoreWallsSetting.getValBoolean();
        if (!ignoreWalls && !mc.player.hasLineOfSight(entity)) {
            return false;
        }

        if (entity instanceof Player p) {
            if (FriendManager.isFriend(p)) {
                return false;
            }
            Setting teamCheckSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Team Check");
            if (teamCheckSetting != null && teamCheckSetting.getValBoolean() && Teams.isTeam(p)) {
                return false;
            }
            Setting playersSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Players");
            if (playersSetting != null && !playersSetting.getValBoolean()) {
                return false;
            }
        } else {
            boolean isHostile = isHostileMob(entity);
            if (isHostile) {
                Setting hostileSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Hostile Mobs");
                if (hostileSetting != null && !hostileSetting.getValBoolean()) {
                    return false;
                }
            } else {
                Setting passiveSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Passive Mobs");
                if (passiveSetting != null && !passiveSetting.getValBoolean()) {
                    return false;
                }
            }
        }

        Setting fovSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "FOV");
        double maxFov = fovSetting != null ? fovSetting.getValDouble() : 60.0;

        return isInFov(entity, maxFov);
    }

    private boolean isHostileMob(Entity entity) {
        if (entity instanceof net.minecraft.world.entity.monster.Monster) {
            return true;
        }
        String name = entity.getType().getDescriptionId().toLowerCase();
        return name.contains("slime") || name.contains("shulker") || name.contains("ghast") || name.contains("dragon");
    }

    private boolean isInFov(Entity entity, double fov) {
        if (fov >= 360.0) return true;
        return get2DAngleDiff(entity.position().add(0, entity.getEyeHeight() * 0.65, 0)) <= fov / 2.0;
    }

    private boolean isHoldingWeapon() {
        if (mc.player == null) return false;
        net.minecraft.world.item.Item item = mc.player.getMainHandItem().getItem();
        String name = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).getPath().toLowerCase();
        return name.contains("sword") || name.contains("axe") || name.contains("mace") || name.contains("trident") || name.contains("bow") || name.contains("crossbow");
    }
}
