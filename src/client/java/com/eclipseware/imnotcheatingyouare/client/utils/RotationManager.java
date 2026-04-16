package com.eclipseware.imnotcheatingyouare.client.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

public class RotationManager {
    private static boolean active = false;
    private static boolean keepMode = false;
    private static boolean returning = false;
    private static float currentServerYaw, currentServerPitch;
    private static float returnFromYaw, returnFromPitch;
    private static int returnTick, returnDuration;
    private static boolean movementCorrection;
    private static int ticksSinceLastKeep = 999;
    private static int pauseTicksLeft = 0;
    private static final Random rand = new Random();

    public static float getGCD() {
        Minecraft mc = Minecraft.getInstance();
        double sens = mc.options.sensitivity().get();
        double f = sens * 0.6 + 0.2;
        return (float) (f * f * f * 8.0 * 0.15);
    }

    private static float snapToGCD(float delta, float gcd) {
        if (gcd < 0.01f) return delta;
        return Math.round(delta / gcd) * gcd;
    }

    /**
     * Exponential-decay aiming with path curvature.
     * ALL deltas are exact integer multiples of the mouse sensitivity GCD.
     * Randomization happens on the desired delta, then we quantize to GCD steps.
     */
    public static void keepRotated(float yaw, float pitch, float stepDegrees, boolean moveCorrect) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        float targetYaw = Mth.wrapDegrees(yaw);
        float targetPitch = Mth.clamp(pitch, -90, 90);
        movementCorrection = moveCorrect;
        ticksSinceLastKeep = 0;
        keepMode = true;

        if (!active || returning) {
            currentServerYaw = mc.player.getYRot();
            currentServerPitch = mc.player.getXRot();
            active = true;
            returning = false;
            pauseTicksLeft = 0;
        }

        if (pauseTicksLeft > 0) {
            pauseTicksLeft--;
            return;
        }
        if (rand.nextFloat() < 0.08f) {
            pauseTicksLeft = 1 + rand.nextInt(2);
            return;
        }

        float gcd = getGCD();
        if (gcd < 0.001f) gcd = 0.15f;

        float yawDiff = Mth.wrapDegrees(targetYaw - currentServerYaw);
        float pitchDiff = targetPitch - currentServerPitch;

        float yawDesired = yawDiff * (0.15f + rand.nextFloat() * 0.30f);
        float pitchDesired = pitchDiff * (0.15f + rand.nextFloat() * 0.30f);

        float yawMax = stepDegrees * (0.7f + rand.nextFloat() * 0.6f);
        float pitchMax = stepDegrees * 0.8f * (0.7f + rand.nextFloat() * 0.6f);
        if (Math.abs(yawDesired) > yawMax) yawDesired = Math.signum(yawDesired) * yawMax;
        if (Math.abs(pitchDesired) > pitchMax) pitchDesired = Math.signum(pitchDesired) * pitchMax;

        float curveAmount = Math.min(Math.abs(yawDiff), 30f) * 0.08f;
        yawDesired += (rand.nextFloat() - 0.5f) * curveAmount;

        int yawSteps = Math.round(yawDesired / gcd);
        int pitchSteps = Math.round(pitchDesired / gcd);

        yawSteps += rand.nextInt(3) - 1;
        pitchSteps += rand.nextInt(3) - 1;

        int maxYawSteps = Math.round(yawDiff / gcd);
        int maxPitchSteps = Math.round(pitchDiff / gcd);
        if (Math.abs(yawSteps) > Math.abs(maxYawSteps)) yawSteps = maxYawSteps;
        if (Math.abs(pitchSteps) > Math.abs(maxPitchSteps)) pitchSteps = maxPitchSteps;

        if (yawDiff > 0 && yawSteps < 0) yawSteps = 0;
        if (yawDiff < 0 && yawSteps > 0) yawSteps = 0;
        if (pitchDiff > 0 && pitchSteps < 0) pitchSteps = 0;
        if (pitchDiff < 0 && pitchSteps > 0) pitchSteps = 0;

        currentServerYaw += yawSteps * gcd;
        currentServerPitch += pitchSteps * gcd;

        currentServerPitch = Mth.clamp(currentServerPitch, -90, 90);
    }

    /**
     * One-shot silent rotation for modules like AutoWeb/AutoDrain.
     */
    public static void queueRotation(float yaw, float pitch, int smooth, int hold, int ret, boolean moveCorrect, Runnable onReach) {
        cancel();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        active = true;
        keepMode = false;
        returning = false;
        movementCorrection = moveCorrect;
        currentServerYaw = Mth.wrapDegrees(yaw);
        currentServerPitch = Mth.clamp(pitch, -90, 90);

        if (onReach != null) onReach.run();
    }

    /**
     * Called at START_CLIENT_TICK. Handles returning and timeout.
     */
    public static void tick() {
        if (!active) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            hardCancel();
            return;
        }

        ticksSinceLastKeep++;

        if (!returning && ticksSinceLastKeep > 2) {
            if (keepMode) {
                startReturn();
            } else {
                hardCancel();
                return;
            }
        }

        if (returning) {
            returnTick++;
            float progress = Math.min(1f, (float) returnTick / returnDuration);
            progress = progress * progress * (3f - 2f * progress);
            float actualYaw = mc.player.getYRot();
            float actualPitch = mc.player.getXRot();
            currentServerYaw = returnFromYaw + Mth.wrapDegrees(actualYaw - returnFromYaw) * progress;
            currentServerPitch = returnFromPitch + (actualPitch - returnFromPitch) * progress;

            if (returnTick >= returnDuration) {
                currentServerYaw = mc.player.getYRot();
                currentServerPitch = mc.player.getXRot();
                hardCancel();
            }
        }
    }

    /**
     * Called at END_CLIENT_TICK — after player.tick() recalculates body/head from yRot.
     * Only overrides yaw-based body/head fields. Never touches xRot (camera pitch stays free).
     */
    public static void visualTick() {
        if (!active) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        if (!mc.options.getCameraType().isFirstPerson()) {
            float wrappedYaw = Mth.wrapDegrees(currentServerYaw);
            mc.player.yBodyRot = wrappedYaw;
            mc.player.yBodyRotO = wrappedYaw;
            mc.player.yHeadRot = wrappedYaw;
            mc.player.yHeadRotO = wrappedYaw;
        }
    }

    private static void startReturn() {
        if (returning) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) { hardCancel(); return; }

        returning = true;
        returnFromYaw = currentServerYaw;
        returnFromPitch = currentServerPitch;
        returnTick = 0;

        float yawDiff = Math.abs(Mth.wrapDegrees(mc.player.getYRot() - currentServerYaw));
        float pitchDiff = Math.abs(mc.player.getXRot() - currentServerPitch);
        float totalDiff = Math.max(yawDiff, pitchDiff);
        returnDuration = Math.max(4, Math.min(10, (int)(totalDiff / 15f) + 4));
    }

    /**
     * Modules should call this instead of cancel(). Starts smooth return to camera rotation.
     * Movement correction remains active during return so the server sees gradual transition.
     */
    public static void requestReturn() {
        if (!active) return;
        if (returning) return;
        startReturn();
    }

    private static void hardCancel() {
        active = false;
        keepMode = false;
        returning = false;
        movementCorrection = false;
        ticksSinceLastKeep = 999;
    }

    /**
     * @deprecated Use requestReturn() for graceful deactivation.
     * Only use cancel() if you need immediate hard stop (e.g., player disconnect).
     */
    @Deprecated
    public static void cancel() {
        hardCancel();
    }

    public static boolean isActive() { return active; }
    public static boolean isKeepMode() { return active && keepMode && !returning; }
    public static boolean isAtTarget() { return active && !returning && ticksSinceLastKeep <= 2; }
    public static boolean isMovementCorrection() { return active && movementCorrection; }

    public static float getServerYaw() { return currentServerYaw; }
    public static float getServerPitch() { return currentServerPitch; }

    public static float getYawDelta() {
        if (!active) return 0f;
        return Mth.wrapDegrees(currentServerYaw - Minecraft.getInstance().player.getYRot());
    }

    public static boolean hasLineOfSight(Vec3 from, Vec3 to) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return false;
        ClipContext context = new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player);
        BlockHitResult result = mc.level.clip(context);
        return result.getType() == HitResult.Type.MISS;
    }
}