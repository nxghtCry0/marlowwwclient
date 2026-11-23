package com.eclipseware.imnotcheatingyouare.client.module.impl;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class PearlCatchPlanner {
    public static final double PROJECTILE_SPEED = 1.5;
    public static final double PEARL_DRAG = 0.99;
    public static final double PEARL_GRAVITY = 0.03;
    public static final double PEARL_EYE_OFFSET_Y = 0.1;
    public static final double MAX_INTERCEPT_DISTANCE_SQ = 0.36;
    public static final int DEFAULT_MAX_FLIGHT_TICKS = 30;

    public record InterceptPlan(
        float pearlYaw,
        float pearlPitch,
        float chargeYaw,
        float chargePitch,
        int chargeTick,
        double missDistanceSq,
        boolean absoluteChargeTiming
    ) {
    }

    private PearlCatchPlanner() {
    }

    public static InterceptPlan findDiagonalPlan(
        Level level,
        Entity clipEntity,
        Vec3 playerPos,
        double eyeHeight,
        Vec3 rawVel,
        boolean onGround,
        float lookYaw,
        float lookPitch,
        int minimumDelay
    ) {
        if (playerPos == null || rawVel == null) {
            return null;
        }
        InterceptPlan direct = solveForChargeDirection(
            level,
            clipEntity,
            playerPos,
            eyeHeight,
            rawVel,
            onGround,
            lookYaw,
            lookPitch,
            minimumDelay
        );
        if (direct != null) {
            return direct;
        }

        float[] fallbackPitches = new float[]{-25.0f, -35.0f, -45.0f, -55.0f, -65.0f, -75.0f};
        for (int i = 0; i < fallbackPitches.length; i++) {
            InterceptPlan fallback = solveForChargeDirection(
                level,
                clipEntity,
                playerPos,
                eyeHeight,
                rawVel,
                onGround,
                lookYaw,
                fallbackPitches[i],
                minimumDelay
            );
            if (fallback != null) {
                return fallback;
            }
        }
        return null;
    }

    public static InterceptPlan solveForChargeDirection(
        Level level,
        Entity clipEntity,
        Vec3 playerPos,
        double eyeHeight,
        Vec3 rawVel,
        boolean onGround,
        float chargeYaw,
        float chargePitch,
        int minimumDelay
    ) {
        Vec3 chargeDir = direction(chargeYaw, chargePitch);
        double vx = rawVel.x;
        double vy = onGround ? 0.0 : rawVel.y;
        double vz = rawVel.z;
        Vec3 playerVel = new Vec3(vx, vy, vz);
        Vec3 chargeVel = new Vec3(
            chargeDir.x * PROJECTILE_SPEED + vx,
            chargeDir.y * PROJECTILE_SPEED + vy,
            chargeDir.z * PROJECTILE_SPEED + vz
        );
        Vec3 pearlStart = new Vec3(playerPos.x, playerPos.y + eyeHeight - PEARL_EYE_OFFSET_Y, playerPos.z);
        Vec3 chargeStart = new Vec3(playerPos.x, playerPos.y + eyeHeight, playerPos.z);

        InterceptPlan best = null;
        double bestScore = Double.MAX_VALUE;
        int minDelay = Math.max(2, minimumDelay);
        int maxDelay = minDelay + 12;

        for (int delay = minDelay; delay <= maxDelay; delay++) {
            Vec3 chargePosAtDelay = new Vec3(
                chargeStart.x + playerVel.x * delay,
                chargeStart.y + playerVel.y * delay,
                chargeStart.z + playerVel.z * delay
            );
            int maxFlightTicks = computeMaxFlightTicks(level, clipEntity, chargePosAtDelay, chargeVel);
            for (int flightTicks = 1; flightTicks <= maxFlightTicks; flightTicks++) {
                int pearlTicks = delay + flightTicks;
                Vec3 chargeTarget = new Vec3(
                    chargePosAtDelay.x + chargeVel.x * flightTicks,
                    chargePosAtDelay.y + chargeVel.y * flightTicks,
                    chargePosAtDelay.z + chargeVel.z * flightTicks
                );
                Vec3 pBase = calcPearlBasePos(pearlStart, playerVel, pearlTicks - 1);
                Vec3 pBaseNext = calcPearlBasePos(pearlStart, playerVel, pearlTicks);
                double d0x = chargeTarget.x - pBase.x;
                double d0y = chargeTarget.y - pBase.y;
                double d0z = chargeTarget.z - pBase.z;
                double vbx = pBaseNext.x - pBase.x;
                double vby = pBaseNext.y - pBase.y;
                double vbz = pBaseNext.z - pBase.z;

                double v0 = calcGeometricSum(pearlTicks - 1);
                double vDiff = Math.pow(PEARL_DRAG, pearlTicks);
                double d10 = PROJECTILE_SPEED * v0;
                double d11 = PROJECTILE_SPEED * vDiff;

                double a = dot(vbx, vby, vbz, vbx, vby, vbz) - d11 * d11;
                double b = -2.0 * dot(d0x, d0y, d0z, vbx, vby, vbz) - 2.0 * d10 * d11;
                double c = dot(d0x, d0y, d0z, d0x, d0y, d0z) - d10 * d10;

                double linRoot = Math.abs(b) > 1.0e-11 ? (-c / b) : Double.NaN;
                double disc = b * b - 4.0 * a * c;
                double qRoot1 = (Math.abs(a) > 1.0e-11 && disc >= 0.0) ? ((-b - Math.sqrt(disc)) / (2.0 * a)) : Double.NaN;
                double qRoot2 = (Math.abs(a) > 1.0e-11 && disc >= 0.0) ? ((-b + Math.sqrt(disc)) / (2.0 * a)) : Double.NaN;
                double[] alphas = new double[]{0.0, 0.5, 1.0, linRoot, qRoot1, qRoot2};

                for (int idx = 0; idx < alphas.length; idx++) {
                    double alpha = alphas[idx];
                    if (Double.isNaN(alpha) || alpha < 0.0 || alpha > 1.0) {
                        continue;
                    }
                    double reqDist = d10 + alpha * d11;
                    if (reqDist < 1.0e-9) {
                        continue;
                    }
                    double rx = (d0x - vbx * alpha) / reqDist;
                    double ry = (d0y - vby * alpha) / reqDist;
                    double rz = (d0z - vbz * alpha) / reqDist;
                    double len = Math.sqrt(dot(rx, ry, rz, rx, ry, rz));
                    if (len < 1.0e-9) {
                        continue;
                    }
                    rx /= len;
                    ry /= len;
                    rz /= len;

                    float pearlYaw = Mth.wrapDegrees((float) Math.toDegrees(Math.atan2(-rx, rz)));
                    float pearlPitch = Mth.clamp(
                        (float) -Math.toDegrees(Math.atan2(ry, Math.hypot(rx, rz))),
                        -90.0f,
                        90.0f
                    );
                    InterceptPlan candidate = evaluatePlan(
                        playerPos,
                        eyeHeight,
                        rawVel,
                        onGround,
                        pearlYaw,
                        pearlPitch,
                        chargeYaw,
                        chargePitch,
                        delay,
                        minDelay,
                        maxFlightTicks,
                        true
                    );
                    if (candidate != null) {
                        double score = Math.sqrt(candidate.missDistanceSq())
                            + flightTicks * 0.005
                            + Math.abs(Mth.wrapDegrees(pearlYaw - chargeYaw)) * 5.0e-4
                            + delay * 1.0e-4;
                        if (score < bestScore) {
                            best = candidate;
                            bestScore = score;
                        }
                    }
                }
            }
        }
        return best;
    }

    public static InterceptPlan findUpwardPlan(
        Vec3 playerPos,
        double eyeHeight,
        Vec3 rawVel,
        boolean onGround,
        float referenceYaw,
        int minimumDelay
    ) {
        if (playerPos == null || rawVel == null) {
            return null;
        }
        double horizSq = rawVel.x * rawVel.x + rawVel.z * rawVel.z;
        float[] pearlYaws;
        if (horizSq < 0.003) {
            pearlYaws = new float[]{referenceYaw};
        } else {
            float moveYaw = (float) Math.toDegrees(Math.atan2(-rawVel.x, rawVel.z));
            pearlYaws = Math.abs(Mth.wrapDegrees(moveYaw - referenceYaw)) < 20.0f
                ? new float[]{referenceYaw}
                : new float[]{referenceYaw, moveYaw};
        }

        int safeDelay = Math.max(0, minimumDelay);
        InterceptPlan best = null;
        double bestScore = Double.MAX_VALUE;

        for (float pearlPitch = -90.0f; pearlPitch <= -76.0f; pearlPitch += 0.5f) {
            for (int yIdx = 0; yIdx < pearlYaws.length; yIdx++) {
                float baseYaw = pearlYaws[yIdx];
                for (float pOff = -20.0f; pOff <= 20.0f; pOff += 2.5f) {
                    float pearlYaw = baseYaw + pOff;
                    for (float chargePitch = -90.0f; chargePitch <= -86.0f; chargePitch += 1.0f) {
                        for (float cOff = -20.0f; cOff <= 20.0f; cOff += 5.0f) {
                            float chargeYaw = referenceYaw + cOff;
                            InterceptPlan candidate = evaluatePlan(
                                playerPos,
                                eyeHeight,
                                rawVel,
                                onGround,
                                pearlYaw,
                                pearlPitch,
                                chargeYaw,
                                chargePitch,
                                safeDelay,
                                safeDelay,
                                DEFAULT_MAX_FLIGHT_TICKS,
                                false
                            );
                            if (candidate != null) {
                                double score = Math.sqrt(candidate.missDistanceSq())
                                    + Math.abs(Mth.wrapDegrees(chargeYaw - referenceYaw)) * 0.01
                                    + Math.abs(Mth.wrapDegrees(pearlYaw - referenceYaw)) * 0.003
                                    + Math.abs(chargePitch + 90.0f) * 0.03;
                                if (score < bestScore) {
                                    best = candidate;
                                    bestScore = score;
                                }
                            }
                        }
                    }
                }
            }
        }
        return best;
    }

    public static InterceptPlan evaluatePlan(
        Vec3 playerPos,
        double eyeHeight,
        Vec3 rawVel,
        boolean onGround,
        float pearlYaw,
        float pearlPitch,
        float chargeYaw,
        float chargePitch,
        int chargeTick,
        int minimumDelay,
        int maxFlightTicks,
        boolean absoluteChargeTiming
    ) {
        Vec3 pearlDir = direction(pearlYaw, pearlPitch);
        Vec3 chargeDir = direction(chargeYaw, chargePitch);
        double pmx = rawVel.x;
        double pmy = onGround ? 0.0 : rawVel.y;
        double pmz = rawVel.z;

        double pearlX = playerPos.x;
        double pearlY = playerPos.y + eyeHeight - PEARL_EYE_OFFSET_Y;
        double pearlZ = playerPos.z;
        double pearlVx = pearlDir.x * PROJECTILE_SPEED + pmx;
        double pearlVy = pearlDir.y * PROJECTILE_SPEED + pmy;
        double pearlVz = pearlDir.z * PROJECTILE_SPEED + pmz;

        double chargeX = playerPos.x + pmx * chargeTick;
        double chargeY = playerPos.y + eyeHeight + pmy * chargeTick;
        double chargeZ = playerPos.z + pmz * chargeTick;
        double chargeVx = chargeDir.x * PROJECTILE_SPEED + pmx;
        double chargeVy = chargeDir.y * PROJECTILE_SPEED + pmy;
        double chargeVz = chargeDir.z * PROJECTILE_SPEED + pmz;

        int flightLimit = Math.max(1, maxFlightTicks);
        double closest = Double.MAX_VALUE;

        for (int tick = 1; tick <= chargeTick + flightLimit; tick++) {
            pearlVx *= PEARL_DRAG;
            pearlVy = (pearlVy - PEARL_GRAVITY) * PEARL_DRAG;
            pearlVz *= PEARL_DRAG;
            pearlX += pearlVx;
            pearlY += pearlVy;
            pearlZ += pearlVz;

            if (tick > chargeTick) {
                chargeX += chargeVx;
                chargeY += chargeVy;
                chargeZ += chargeVz;
                double dx = pearlX - chargeX;
                double dy = pearlY - chargeY;
                double dz = pearlZ - chargeZ;
                double distSq = dx * dx + dy * dy + dz * dz;
                if (distSq < closest) {
                    closest = distSq;
                }
            }
        }

        if (chargeTick >= minimumDelay && closest <= MAX_INTERCEPT_DISTANCE_SQ) {
            return new InterceptPlan(
                Mth.wrapDegrees(pearlYaw),
                Mth.clamp(pearlPitch, -90.0f, 90.0f),
                Mth.wrapDegrees(chargeYaw),
                Mth.clamp(chargePitch, -90.0f, 90.0f),
                chargeTick,
                closest,
                absoluteChargeTiming
            );
        }
        return null;
    }

    public static int computeMaxFlightTicks(Level level, Entity clipEntity, Vec3 chargeStart, Vec3 chargeVel) {
        if (level == null || clipEntity == null) {
            return DEFAULT_MAX_FLIGHT_TICKS;
        }
        Vec3 chargeEnd = new Vec3(
            chargeStart.x + chargeVel.x * DEFAULT_MAX_FLIGHT_TICKS,
            chargeStart.y + chargeVel.y * DEFAULT_MAX_FLIGHT_TICKS,
            chargeStart.z + chargeVel.z * DEFAULT_MAX_FLIGHT_TICKS
        );
        ClipContext ctx = new ClipContext(
            chargeStart,
            chargeEnd,
            ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE,
            clipEntity
        );
        BlockHitResult hit = level.clip(ctx);
        if (hit != null && hit.getType() != HitResult.Type.MISS) {
            Vec3 loc = hit.getLocation();
            double dx = loc.x - chargeStart.x;
            double dy = loc.y - chargeStart.y;
            double dz = loc.z - chargeStart.z;
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            double speed = Math.sqrt(dot(chargeVel.x, chargeVel.y, chargeVel.z, chargeVel.x, chargeVel.y, chargeVel.z));
            if (speed < 1.0e-9) {
                return 0;
            }
            int ticks = (int) Math.ceil(dist / speed - 1.0e-9) - 1;
            return Math.max(0, Math.min(DEFAULT_MAX_FLIGHT_TICKS, ticks));
        }
        return DEFAULT_MAX_FLIGHT_TICKS;
    }

    public static double calcGeometricSum(int ticks) {
        if (ticks <= 0) {
            return 0.0;
        }
        return 99.0 * PEARL_DRAG * (1.0 - Math.pow(PEARL_DRAG, ticks));
    }

    public static Vec3 calcPearlBasePos(Vec3 startPos, Vec3 playerVel, int ticks) {
        if (ticks <= 0) {
            return startPos;
        }
        double velMult = calcGeometricSum(ticks);
        double gravMult = 99.0 * (ticks - velMult);
        return new Vec3(
            startPos.x + playerVel.x * velMult,
            startPos.y + playerVel.y * velMult - PEARL_GRAVITY * gravMult,
            startPos.z + playerVel.z * velMult
        );
    }

    public static Vec3 direction(float yaw, float pitch) {
        double yawRad = Math.toRadians(yaw);
        double pitchRad = Math.toRadians(pitch);
        double cosPitch = Math.cos(pitchRad);
        return new Vec3(-Math.sin(yawRad) * cosPitch, -Math.sin(pitchRad), Math.cos(yawRad) * cosPitch);
    }

    private static double dot(double ax, double ay, double az, double bx, double by, double bz) {
        return ax * bx + ay * by + az * bz;
    }
}
