package com.eclipseware.imnotcheatingyouare.client.utils.cheat;

/**
 * GCD (Greatest Common Divisor) Fix.
 *
 * Minecraft applies mouse sensitivity scaling that produces a fixed GCD pattern
 * in rotation deltas. Anticheats (Polar, Intave, Spartan, etc.) detect
 * rotations that DON'T follow this pattern — a dead giveaway that rotations
 * are being set programmatically.
 *
 * This class snaps any rotation delta to the nearest valid GCD-aligned step
 * so our aim assist / triggerbot rotations look exactly like real mouse input.
 */
public class GCDFix {

    private static double cachedSens = -1.0;
    private static double cachedGCD  = -1.0;

    /**
     * Recompute the GCD for the current sensitivity setting.
     * Call once per tick before using snapDelta().
     */
    public static void update(double sensitivityOption) {
        if (sensitivityOption == cachedSens) return;
        cachedSens = sensitivityOption;
        double f = sensitivityOption * 0.6 + 0.2;
        cachedGCD = f * f * f * 8.0;
    }

    /**
     * Snap a raw rotation delta to the nearest GCD-aligned value.
     * This makes programmatic rotations indistinguishable from real mouse input.
     *
     * @param delta  the raw rotation change (yaw or pitch, in degrees)
     * @return       the GCD-snapped delta
     */
    public static float snapDelta(float delta) {
        if (cachedGCD <= 0) return delta;
        return (float) (Math.round(delta / cachedGCD) * cachedGCD);
    }

    /**
     * Full rotation snap: given current and target angle, return a
     * GCD-aligned step that moves toward the target.
     *
     * @param current  current yaw or pitch
     * @param target   desired yaw or pitch
     * @param maxStep  maximum degrees to move this tick
     * @return         GCD-snapped new angle
     */
    public static float stepToward(float current, float target, float maxStep) {
        float raw = net.minecraft.util.Mth.wrapDegrees(target - current);
        float clamped = net.minecraft.util.Mth.clamp(raw, -maxStep, maxStep);
        float snapped = snapDelta(clamped);
        return current + snapped;
    }
}
