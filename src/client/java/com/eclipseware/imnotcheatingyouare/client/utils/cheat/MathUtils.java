package com.eclipseware.imnotcheatingyouare.client.utils.cheat;

public class MathUtils {
    public static double randomDoubleBetween(double min, double max) {
        return min + (Math.random() * (max - min));
    }

    public static float randomFloatBetween(float min, float max) {
        return min + (float) Math.random() * (max - min);
    }

    public static int randomIntBetween(int min, int max) {
        return (int) (min + Math.random() * (max - min + 1));
    }

    public static float lerp(float start, float end, float delta) {
        return start + (end - start) * delta;
    }

    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
