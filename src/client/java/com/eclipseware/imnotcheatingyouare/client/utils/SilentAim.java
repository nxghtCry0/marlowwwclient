package com.eclipseware.imnotcheatingyouare.client.utils;

public class SilentAim {
    private static boolean active = false;
    private static float targetYaw = 0f;
    private static float targetPitch = 0f;
    private static int packetsRemaining = 0;

    public static void setRotation(float yaw, float pitch, int packets) {
        targetYaw = yaw;
        targetPitch = pitch;
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

    public static void tick() {
        if (packetsRemaining > 0) {
            packetsRemaining--;
        } else {
            active = false;
        }
    }
}