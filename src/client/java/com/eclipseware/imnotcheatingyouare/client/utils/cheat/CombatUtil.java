package com.eclipseware.imnotcheatingyouare.client.utils.cheat;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3;

public class CombatUtil {
    private static final Minecraft mc = Minecraft.getInstance();

    public static boolean isShieldFacingAway(Player target) {
        if (mc.player == null || target == null) return false;

        Vec3 playerPos = mc.player.getEyePosition();
        Vec3 targetPos = target.position();

        Vec3 toPlayer = playerPos.subtract(targetPos);
        Vec3 toPlayerHorizontal = new Vec3(toPlayer.x, 0.0, toPlayer.z);
        if (toPlayerHorizontal.lengthSqr() == 0.0) return false;
        toPlayerHorizontal = toPlayerHorizontal.normalize();

        double yaw = Math.toRadians(target.getYRot());
        double pitch = Math.toRadians(target.getXRot());

        Vec3 facing = new Vec3(
                -Math.sin(yaw) * Math.cos(pitch),
                -Math.sin(pitch),
                Math.cos(yaw) * Math.cos(pitch)
        ).normalize();

        return facing.dot(toPlayerHorizontal) < 0.0;
    }

    public static Vec3 getAimPoint(LivingEntity entity) {
        if (entity == null) return null;
        double minX = entity.getBoundingBox().minX;
        double minY = entity.getBoundingBox().minY;
        double minZ = entity.getBoundingBox().minZ;
        double maxX = entity.getBoundingBox().maxX;
        double maxY = entity.getBoundingBox().maxY;
        double maxZ = entity.getBoundingBox().maxZ;

        Vec3 eyes = mc.player.getEyePosition();

        double x = MathHelper.clamp(eyes.x, minX, maxX);
        double y = MathHelper.clamp(eyes.y, minY, maxY);
        double z = MathHelper.clamp(eyes.z, minZ, maxZ);

        return new Vec3(x, y, z);
    }

    public static float getAngleToTarget(Vec3 target) {
        if (mc.player == null || target == null) return Float.MAX_VALUE;
        Vec3 lookVec = mc.player.getViewVector(1.0F);
        Vec3 diffVec = target.subtract(mc.player.getEyePosition()).normalize();
        double dot = lookVec.dot(diffVec);
        dot = MathHelper.clamp(dot, -1.0, 1.0);
        return (float) Math.toDegrees(Math.acos(dot));
    }

    public static boolean isTargetInFov(LivingEntity entity, double fov) {
        Vec3 aimPoint = getAimPoint(entity);
        if (aimPoint == null) return false;
        return getAngleToTarget(aimPoint) <= fov / 2.0;
    }

    public static double getDistanceToBox(Vec3 point, net.minecraft.world.phys.AABB box) {
        double x = MathHelper.clamp(point.x, box.minX, box.maxX);
        double y = MathHelper.clamp(point.y, box.minY, box.maxY);
        double z = MathHelper.clamp(point.z, box.minZ, box.maxZ);
        return point.distanceTo(new Vec3(x, y, z));
    }
}
