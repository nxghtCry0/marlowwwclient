package com.eclipseware.imnotcheatingyouare.client.utils;

import net.minecraft.util.Mth;

/**
 * Utility class for silent server-side rotation spoofing.
 * Works with ConnectionMixin to intercept and modify movement packets.
 */
public class SilentAimUtil {
    private static boolean active = false;
    private static float targetYaw = 0f;
    private static float targetPitch = 0f;
    private static int packetsRemaining = 0;

    /**
     * Activates silent aim for a specific number of packets.
     * @param yaw Target yaw angle
     * @param pitch Target pitch angle
     * @param packets Number of movement packets to spoof (typically 2)
     */
    public static void setRotation(float yaw, float pitch, int packets) {
        targetYaw = Mth.wrapDegrees(yaw);
        targetPitch = Mth.clamp(pitch, -90f, 90f);
        packetsRemaining = packets;
        active = true;
    }

    public static boolean isActive() {
        return active && packetsRemaining > 0;
    }

    public static float getYaw() {
        return targetYaw;
    }

    public static float getPitch() {
        return targetPitch;
    }

    /**
     * Consumes one packet credit. Call this after each spoofed packet is sent.
     */
    public static void consume() {
        if (packetsRemaining > 0) {
            packetsRemaining--;
        }
        if (packetsRemaining <= 0) {
            active = false;
        }
    }

    /**
     * Resets the silent aim state.
     */
    public static void reset() {
        active = false;
        packetsRemaining = 0;
    }
}