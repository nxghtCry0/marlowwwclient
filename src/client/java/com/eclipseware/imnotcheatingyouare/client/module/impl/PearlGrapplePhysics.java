package com.eclipseware.imnotcheatingyouare.client.module.impl;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class PearlGrapplePhysics {
    public static final double LAUNCH_SPEED = 1.5;
    public static final double AIR_DRAG = 0.99;
    public static final double WATER_DRAG = 0.80;
    public static final double GRAVITY = 0.03;
    public static final double SPAWN_EYE_OFFSET_Y = 0.1;
    public static final int MAX_PREDICTION_TICKS = 300;
    public static final int MAX_SOLVE_TICKS = 160;

    public record ThrowSolution(float yaw, float pitch, double flightTicks) {
    }

    private PearlGrapplePhysics() {
    }

    public static Vec3 predictLandingPosition(Level level, ThrownEnderpearl pearl) {
        if (level == null || pearl == null) {
            return null;
        }
        Vec3 startPos = pearl.position();
        Vec3 startVel = pearl.getDeltaMovement();
        int minY = level.getMinY();
        double posX = startPos.x;
        double posY = startPos.y;
        double posZ = startPos.z;
        double motionX = startVel.x;
        double motionY = startVel.y;
        double motionZ = startVel.z;

        for (int step = 0; step < MAX_PREDICTION_TICKS; step++) {
            BlockPos fluidPos = BlockPos.containing(posX, posY, posZ);
            FluidState fluid = level.getFluidState(fluidPos);
            boolean inWater = !fluid.isEmpty() && fluid.is(FluidTags.WATER);
            double drag = inWater ? WATER_DRAG : AIR_DRAG;

            motionY -= GRAVITY;
            motionX *= drag;
            motionY *= drag;
            motionZ *= drag;

            Vec3 current = new Vec3(posX, posY, posZ);
            Vec3 next = new Vec3(posX + motionX, posY + motionY, posZ + motionZ);
            ClipContext clipCtx = new ClipContext(
                current,
                next,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                pearl
            );
            BlockHitResult hit = level.clip(clipCtx);
            if (hit != null && hit.getType() != HitResult.Type.MISS) {
                return hit.getLocation();
            }

            posX = next.x;
            posY = next.y;
            posZ = next.z;
            if (posY < minY - 64.0) {
                return null;
            }
        }
        return null;
    }

    public static ThrowSolution solveThrow(Level level, Player player, Vec3 target) {
        if (player == null || target == null) {
            return null;
        }
        Vec3 spawnPos = new Vec3(player.getX(), player.getEyeY() - SPAWN_EYE_OFFSET_Y, player.getZ());
        Vec3 rawVel = player.getDeltaMovement();
        Vec3 inheritedVel = new Vec3(rawVel.x, player.onGround() ? 0.0 : rawVel.y, rawVel.z);
        return solveThrowFromState(level, player, spawnPos, inheritedVel, target);
    }

    public static ThrowSolution solveThrowFromState(
        Level level,
        Entity clipEntity,
        Vec3 spawnPos,
        Vec3 inheritedVel,
        Vec3 target
    ) {
        if (spawnPos == null || inheritedVel == null || target == null) {
            return null;
        }
        ThrowSolution fallbackUnchecked = null;
        for (int n = 0; n < MAX_SOLVE_TICKS; n++) {
            Vec3 baseNow = calcPearlBasePos(spawnPos, inheritedVel, n);
            Vec3 baseNext = calcPearlBasePos(spawnPos, inheritedVel, n + 1);
            double d0x = target.x - baseNow.x;
            double d0y = target.y - baseNow.y;
            double d0z = target.z - baseNow.z;
            double vbx = baseNext.x - baseNow.x;
            double vby = baseNext.y - baseNow.y;
            double vbz = baseNext.z - baseNow.z;

            double sumNow = geometricDragSum(n);
            double nextDecay = Math.pow(AIR_DRAG, n + 1);
            double d10 = LAUNCH_SPEED * sumNow;
            double d11 = LAUNCH_SPEED * nextDecay;

            double a = dot(vbx, vby, vbz, vbx, vby, vbz) - d11 * d11;
            double b = -2.0 * dot(d0x, d0y, d0z, vbx, vby, vbz) - 2.0 * d10 * d11;
            double c = dot(d0x, d0y, d0z, d0x, d0y, d0z) - d10 * d10;

            double[] roots = solveUnitIntervalRoots(a, b, c);
            for (int rIdx = 0; rIdx < roots.length; rIdx++) {
                double alpha = roots[rIdx];
                if (Double.isNaN(alpha) || alpha < 0.0 || alpha > 1.0) {
                    continue;
                }
                double reqDist = d10 + alpha * d11;
                if (reqDist < 1.0e-7) {
                    continue;
                }
                double rx = (d0x - vbx * alpha) / reqDist;
                double ry = (d0y - vby * alpha) / reqDist;
                double rz = (d0z - vbz * alpha) / reqDist;
                double norm = Math.sqrt(dot(rx, ry, rz, rx, ry, rz));
                if (Math.abs(norm - 1.0) > 1.0e-3) {
                    continue;
                }
                rx /= norm;
                ry /= norm;
                rz /= norm;

                float yaw = Mth.wrapDegrees((float) Math.toDegrees(Math.atan2(-rx, rz)));
                float pitch = Mth.clamp(
                    (float) -Math.toDegrees(Math.atan2(ry, Math.hypot(rx, rz))),
                    -90.0f,
                    90.0f
                );
                ThrowSolution candidate = new ThrowSolution(yaw, pitch, n + alpha);
                if (fallbackUnchecked == null) {
                    fallbackUnchecked = candidate;
                }
                if (level == null || isArcClear(level, clipEntity, spawnPos, inheritedVel, yaw, pitch, target, n + 2)) {
                    return candidate;
                }
            }
        }
        return level == null ? fallbackUnchecked : null;
    }

    public static boolean isArcClear(
        Level level,
        Entity clipEntity,
        Vec3 spawnPos,
        Vec3 inheritedVel,
        float yaw,
        float pitch,
        Vec3 target,
        int maxTicks
    ) {
        Vec3 dir = directionFromRotation(yaw, pitch);
        double posX = spawnPos.x;
        double posY = spawnPos.y;
        double posZ = spawnPos.z;
        double motionX = dir.x * LAUNCH_SPEED + inheritedVel.x;
        double motionY = dir.y * LAUNCH_SPEED + inheritedVel.y;
        double motionZ = dir.z * LAUNCH_SPEED + inheritedVel.z;

        for (int step = 0; step < maxTicks; step++) {
            motionY -= GRAVITY;
            motionX *= AIR_DRAG;
            motionY *= AIR_DRAG;
            motionZ *= AIR_DRAG;

            Vec3 cur = new Vec3(posX, posY, posZ);
            Vec3 nxt = new Vec3(posX + motionX, posY + motionY, posZ + motionZ);
            ClipContext ctx = new ClipContext(
                cur,
                nxt,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                clipEntity
            );
            BlockHitResult hit = level.clip(ctx);
            if (hit != null && hit.getType() != HitResult.Type.MISS) {
                Vec3 hitLoc = hit.getLocation();
                return hitLoc.distanceToSqr(target) <= 2.25;
            }
            posX = nxt.x;
            posY = nxt.y;
            posZ = nxt.z;
            if (nxt.distanceToSqr(target) <= 0.64) {
                return true;
            }
        }
        return true;
    }

    public static Vec3 simulateAirPointAtTicks(Vec3 spawnPos, Vec3 inheritedVel, float yaw, float pitch, double flightTicks) {
        int wholeTicks = Math.max(0, (int) Math.floor(flightTicks));
        double alpha = Mth.clamp(flightTicks - wholeTicks, 0.0, 1.0);
        Vec3 dir = directionFromRotation(yaw, pitch);
        Vec3 baseNow = calcPearlBasePos(spawnPos, inheritedVel, wholeTicks);
        Vec3 baseNext = calcPearlBasePos(spawnPos, inheritedVel, wholeTicks + 1);
        double sumNow = geometricDragSum(wholeTicks);
        double sumNext = geometricDragSum(wholeTicks + 1);
        double launchScale = LAUNCH_SPEED * ((1.0 - alpha) * sumNow + alpha * sumNext);
        double bx = baseNow.x + (baseNext.x - baseNow.x) * alpha;
        double by = baseNow.y + (baseNext.y - baseNow.y) * alpha;
        double bz = baseNow.z + (baseNext.z - baseNow.z) * alpha;
        return new Vec3(bx + dir.x * launchScale, by + dir.y * launchScale, bz + dir.z * launchScale);
    }

    public static Vec3 calcPearlBasePos(Vec3 spawnPos, Vec3 inheritedVel, int ticks) {
        if (ticks <= 0) {
            return spawnPos;
        }
        double velMult = geometricDragSum(ticks);
        double gravMult = 99.0 * (ticks - velMult);
        return new Vec3(
            spawnPos.x + inheritedVel.x * velMult,
            spawnPos.y + inheritedVel.y * velMult - GRAVITY * gravMult,
            spawnPos.z + inheritedVel.z * velMult
        );
    }

    public static double geometricDragSum(int ticks) {
        if (ticks <= 0) {
            return 0.0;
        }
        return 99.0 * (1.0 - Math.pow(AIR_DRAG, ticks));
    }

    public static Vec3 directionFromRotation(float yaw, float pitch) {
        double yawRad = Math.toRadians(yaw);
        double pitchRad = Math.toRadians(pitch);
        double cosPitch = Math.cos(pitchRad);
        return new Vec3(-Math.sin(yawRad) * cosPitch, -Math.sin(pitchRad), Math.cos(yawRad) * cosPitch);
    }

    public static float horizontalAngleDelta(Vec3 playerPos, float playerYaw, Vec3 targetPos) {
        double dx = targetPos.x - playerPos.x;
        double dz = targetPos.z - playerPos.z;
        float targetYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        return Mth.wrapDegrees(targetYaw - playerYaw);
    }

    public static boolean isWithinAngleLimit(Vec3 playerPos, float playerYaw, Vec3 landingPos, double angleLimitDeg) {
        if (angleLimitDeg >= 360.0) {
            return true;
        }
        float delta = Math.abs(horizontalAngleDelta(playerPos, playerYaw, landingPos));
        return delta <= angleLimitDeg * 0.5;
    }

    public static boolean isPearlEscaping(Vec3 localPlayerPos, Vec3 ownerPos, Vec3 landingPos) {
        return localPlayerPos.distanceToSqr(landingPos) > localPlayerPos.distanceToSqr(ownerPos);
    }

    public static boolean passesVerticalCheck(double playerY, Vec3 landingPos, boolean verticalCheckEnabled) {
        if (!verticalCheckEnabled) {
            return true;
        }
        return (landingPos.y - playerY) < 7.0;
    }

    public static float stepAngleGcd(float current, float target, float maxStepDeg, float gcd, boolean wrap) {
        float safeGcd = gcd < 0.001f ? 0.15f : gcd;
        float diff = wrap ? Mth.wrapDegrees(target - current) : (target - current);
        if (Math.abs(diff) <= Math.max(0.35f, safeGcd * 0.5f)) {
            return current;
        }
        float desired = Math.abs(diff) > maxStepDeg ? Math.copySign(maxStepDeg, diff) : diff;
        int steps = Math.round(desired / safeGcd);
        int maxSteps = Math.round(diff / safeGcd);
        if (Math.abs(steps) > Math.abs(maxSteps)) {
            steps = maxSteps;
        }
        if (steps == 0 && maxSteps != 0) {
            steps = maxSteps > 0 ? 1 : -1;
        }
        float next = current + steps * safeGcd;
        return wrap ? Mth.wrapDegrees(next) : Mth.clamp(next, -90.0f, 90.0f);
    }

    public static double sampleCubicBezier(double[] curve, double x) {
        if (curve == null || curve.length < 4) {
            return Mth.clamp(x, 0.0, 1.0);
        }
        if (x <= 0.0) {
            return 0.0;
        }
        if (x >= 1.0) {
            return 1.0;
        }
        double lo = 0.0;
        double hi = 1.0;
        double t = x;
        for (int i = 0; i < 24; i++) {
            t = (lo + hi) * 0.5;
            double u = 1.0 - t;
            double bx = 3.0 * u * u * t * curve[0] + 3.0 * u * t * t * curve[2] + t * t * t;
            if (bx < x) {
                lo = t;
            } else {
                hi = t;
            }
        }
        double u = 1.0 - t;
        double by = 3.0 * u * u * t * curve[1] + 3.0 * u * t * t * curve[3] + t * t * t;
        return Math.max(0.0, by);
    }

    public static float computeHumanizedTurnSpeed(
        double baseSpeed,
        double angularDist,
        double curveRange,
        double[] speedCurve,
        double flickProgress,
        double startMult,
        double midMult,
        double endMult,
        double tickJitterFactor
    ) {
        double p = Mth.clamp(flickProgress, 0.0, 1.0);
        double inv = 1.0 - p;
        double bellEnvelope = inv * inv * startMult + 2.0 * inv * p * midMult + p * p * endMult;
        double rangeNorm = curveRange > 0.0 ? Math.min(angularDist / curveRange, 1.0) : 1.0;
        double distScale = 0.28 + 0.72 * sampleCubicBezier(speedCurve, rangeNorm);
        double rawSpeed = baseSpeed * bellEnvelope * distScale * tickJitterFactor;
        double settleFloor = angularDist <= 6.0 ? 8.5 : 3.5;
        return (float) Mth.clamp(rawSpeed, settleFloor, 160.0);
    }

    public static float[] computeCurvedWaypoint(
        float currentYaw,
        float currentPitch,
        float targetYaw,
        float targetPitch,
        double flickProgress,
        float arcPeakDeg,
        float arcSign
    ) {
        float yawDiff = Mth.wrapDegrees(targetYaw - currentYaw);
        float pitchDiff = targetPitch - currentPitch;
        double dist = Math.hypot(yawDiff, pitchDiff);
        if (dist <= 3.0 || Math.abs(arcPeakDeg) <= 0.05f) {
            return new float[]{targetYaw, targetPitch};
        }
        double p = Mth.clamp(flickProgress, 0.0, 1.0);
        double settleAtten = Mth.clamp((dist - 3.0) / 9.0, 0.0, 1.0);
        double bell = Math.sin(Math.PI * p) * (1.0 - p * 0.55) * settleAtten;
        double offset = arcSign * arcPeakDeg * bell;
        double nx = -pitchDiff / dist;
        double ny = yawDiff / dist;
        float curvedYaw = Mth.wrapDegrees((float) (targetYaw + nx * offset));
        float curvedPitch = Mth.clamp((float) (targetPitch + ny * offset * 0.72), -90.0f, 90.0f);
        return new float[]{curvedYaw, curvedPitch};
    }

    private static double[] solveUnitIntervalRoots(double a, double b, double c) {
        if (Math.abs(a) < 1.0e-11) {
            if (Math.abs(b) < 1.0e-11) {
                return new double[]{Double.NaN, Double.NaN};
            }
            return new double[]{-c / b, Double.NaN};
        }
        double disc = b * b - 4.0 * a * c;
        if (disc < 0.0) {
            return new double[]{Double.NaN, Double.NaN};
        }
        double root = Math.sqrt(disc);
        double r1 = (-b - root) / (2.0 * a);
        double r2 = (-b + root) / (2.0 * a);
        return r1 <= r2 ? new double[]{r1, r2} : new double[]{r2, r1};
    }

    private static double dot(double ax, double ay, double az, double bx, double by, double bz) {
        return ax * bx + ay * by + az * bz;
    }
}
