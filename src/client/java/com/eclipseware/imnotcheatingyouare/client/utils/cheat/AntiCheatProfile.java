package com.eclipseware.imnotcheatingyouare.client.utils.cheat;

/**
 * Per-server anticheat profile.
 *
 * Different servers run different ACs with different detection vectors.
 * This class centralises the "how aggressive can we be" decision so every
 * module can query it instead of hard-coding limits.
 *
 * Profiles:
 *   VANILLA  — no AC, full speed
 *   LIGHT    — basic AC (NCP legacy, basic Spartan) — moderate limits
 *   MEDIUM   — Spartan 4+, Intave, AAC — tighter limits
 *   STRICT   — Polar, Matrix, Grim — very conservative, legit-only values
 */
public class AntiCheatProfile {

    public enum Level { VANILLA, LIGHT, MEDIUM, STRICT }

    private static Level current = Level.MEDIUM;

    public static void set(Level l) { current = l; }
    public static Level get()       { return current; }

    /** Max extra reach (blocks) safe for current profile. */
    public static double safeReachExtra() {
        return switch (current) {
            case VANILLA -> 1.5;
            case LIGHT   -> 0.8;
            case MEDIUM  -> 0.4;
            case STRICT  -> 0.1;
        };
    }

    /** Max degrees per tick the aim assist should move. */
    public static float safeAimMaxTurn() {
        return switch (current) {
            case VANILLA -> 15.0f;
            case LIGHT   -> 8.0f;
            case MEDIUM  -> 4.5f;
            case STRICT  -> 2.5f;
        };
    }

    /** Minimum ms between triggerbot clicks. */
    public static int safeTriggerMinDelayMs() {
        return switch (current) {
            case VANILLA -> 40;
            case LIGHT   -> 65;
            case MEDIUM  -> 85;
            case STRICT  -> 110;
        };
    }

    /** Whether WTap should use silent (packet-level) sprint reset. */
    public static boolean wtapSilentMode() {
        return current == Level.STRICT || current == Level.MEDIUM;
    }

    /** Whether rotations should be server-side only (silent aim). */
    public static boolean silentRotations() {
        return current == Level.STRICT;
    }
}
