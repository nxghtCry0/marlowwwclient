package com.eclipseware.imnotcheatingyouare.client.utils.cheat;

public class BypassManager {

    private static boolean bypassActive = false;

    public static void update() {
        boolean bypass = shouldBypass();
        bypassActive = bypass;

        if (bypass) {
            applyBypass();
        }
    }

    public static boolean shouldBypass() {
        return false;
    }

    public static void applyBypass() {
    }

    public static boolean isBypassActive() {
        return bypassActive;
    }

    public static boolean shouldDisableCombat() {
        return false;
    }

    public static boolean shouldDisableMovement() {
        return false;
    }

    public static boolean shouldDisableAimbot() {
        return false;
    }
}
