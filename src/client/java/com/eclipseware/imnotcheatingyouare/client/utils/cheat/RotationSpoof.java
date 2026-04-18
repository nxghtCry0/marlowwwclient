package com.eclipseware.imnotcheatingyouare.client.utils.cheat;

/**
 * Silent / Server-side rotation spoof.
 *
 * Instead of moving the actual camera (which screenshares and OBS capture
 * would reveal), we store a "server yaw/pitch" that gets injected into
 * outgoing packets only. The client camera stays where the player physically
 * aimed it.
 *
 * Usage:
 *   1. Call RotationSpoof.set(yaw, pitch) when you want to aim silently.
 *   2. In your packet mixin, replace the rotation fields with getServerYaw()
 *      / getServerPitch() when isActive() is true.
 *   3. Call RotationSpoof.reset() after the packet is sent.
 */
public class RotationSpoof {

    private static float serverYaw   = 0f;
    private static float serverPitch = 0f;
    private static boolean active    = false;

    private static int ttl = 0;

    public static void set(float yaw, float pitch) {
        set(yaw, pitch, 1);
    }

    public static void set(float yaw, float pitch, int ticks) {
        serverYaw   = yaw;
        serverPitch = pitch;
        ttl         = ticks;
        active      = true;
    }

    public static void tick() {
        if (active && --ttl <= 0) reset();
    }

    public static void reset() {
        active = false;
        ttl    = 0;
    }

    public static boolean isActive()    { return active; }
    public static float getServerYaw()  { return serverYaw; }
    public static float getServerPitch(){ return serverPitch; }
}
