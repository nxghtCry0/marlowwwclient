package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.utils.SilentAimUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class SilentAim extends Module {
    private Entity target;
    private long lastTargetTime = 0;
    private static final long TARGET_COOLDOWN = 100; 

    public SilentAim() {
        super("SilentAim", Category.Combat, "Silently aims at targets when attacking without moving your camera.");
    }

    @Override
public void onTick() {
if (mc == null || mc.player == null || mc.level == null || mc.screen != null) {
target = null;
return;
}
if (!mc.options.keyAttack.isDown()) {
target = null;
return;
}
chooseTarget();
if (target == null) return;
AABB box = target.getBoundingBox();
Vec3 aimPoint = new Vec3(box.getCenter().x, box.getCenter().y, box.getCenter().z);
Vec3 eyes = mc.player.getEyePosition();
double diffX = aimPoint.x - eyes.x;
double diffY = aimPoint.y - eyes.y;
double diffZ = aimPoint.z - eyes.z;
double distXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);
float neededYaw = (float) (Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0F);
float neededPitch = (float) -Math.toDegrees(Math.atan2(diffY, distXZ));
SilentAimUtil.setRotation(neededYaw, neededPitch, 4);
if (SilentAimUtil.isActive() && mc.getConnection() != null) {
mc.getConnection().send(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Rot(
SilentAimUtil.getYaw(), SilentAimUtil.getPitch(), mc.player.onGround(), false
));
SilentAimUtil.consume();
}
}

    private void chooseTarget() {
        if (System.currentTimeMillis() - lastTargetTime < TARGET_COOLDOWN) return;

        Setting rangeSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Range");
        Setting fovSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "FOV");

        double range = rangeSetting != null ? rangeSetting.getValDouble() : 4.5;
        double maxFov = fovSetting != null ? fovSetting.getValDouble() : 120.0;

        Entity bestTarget = null;
        double bestAngle = maxFov / 2.0;

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity == mc.player || !entity.isAlive() || !(entity instanceof LivingEntity)) continue;
            if (mc.player.distanceTo(entity) > range) continue;
            if (!isValidTarget(entity)) continue;

            AABB box = entity.getBoundingBox();
            Vec3 aimPoint = new Vec3(box.getCenter().x, box.getCenter().y, box.getCenter().z);
            double angle = getAngleToLookVec(aimPoint);
            
            if (angle <= bestAngle) {
                bestAngle = angle;
                bestTarget = entity;
            }
        }

        if (bestTarget != target) {
            target = bestTarget;
            lastTargetTime = System.currentTimeMillis();
        }
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

    @Override
    public void onDisable() {
        target = null;
    }
}