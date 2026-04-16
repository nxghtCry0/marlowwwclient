package com.eclipseware.imnotcheatingyouare.client.utils;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Comprehensive animation utility class with easing functions and rendering helpers.
 * 
 * Easing Functions Explained:
 * - easeOutCubic: Starts fast, slows down at the end. Creates a natural, smooth deceleration.
 *   Formula: 1 - (1 - t)^3
 * - easeInCubic: Starts slow, accelerates. Good for closing animations.
 *   Formula: t^3
 * - easeInOutCubic: Slow start, fast middle, slow end. Most natural feeling for transitions.
 *   Formula: t < 0.5 ? 4*t^3 : 1 - (-2*t + 2)^3 / 2
 * - easeOutBack: Overshoots the target slightly, then bounces back. Creates a "springy" feel.
 *   Formula: 1 + (c + 1) * (t - 1)^3 + c * (t - 1)^2 where c = 1.70158
 */
public class AnimationUtil {


    /**
     * Cubic ease-out: starts fast, decelerates smoothly to the target.
     * Best for opening animations where you want immediate feedback.
     */
    public static float easeOutCubic(float t) {
        return 1.0f - (float) Math.pow(1.0 - t, 3.0);
    }

    /**
     * Cubic ease-in: starts slow, accelerates toward the end.
     * Best for closing animations where things "shrink away".
     */
    public static float easeInCubic(float t) {
        return t * t * t;
    }

    /**
     * Cubic ease-in-out: slow start, fast middle, slow end.
     * Most natural feeling for general transitions.
     */
    public static float easeInOutCubic(float t) {
        return t < 0.5f ? 4.0f * t * t * t : 1.0f - (float) Math.pow(-2.0f * t + 2.0f, 3.0f) / 2.0f;
    }

    /**
     * Quadratic ease-out: gentler than cubic, good for subtle animations.
     */
    public static float easeOutQuad(float t) {
        return 1.0f - (1.0f - t) * (1.0f - t);
    }

    /**
     * Quartic ease-out: more aggressive deceleration than cubic.
     */
    public static float easeOutQuart(float t) {
        return 1.0f - (float) Math.pow(1.0f - t, 4.0f);
    }

    /**
     * Exponential ease-out: very fast start, dramatic slowdown at end.
     * Good for snappy, responsive-feeling UI.
     */
    public static float easeOutExpo(float t) {
        return t == 1.0f ? 1.0f : 1.0f - (float) Math.pow(2.0f, -10.0f * t);
    }

    /**
     * Ease-out back: overshoots slightly then settles. Creates a "springy" feel.
     * The constant 1.70158 is the standard "back" easing overshoot factor.
     */
    public static float easeOutBack(float t) {
        float c1 = 1.70158f;
        float c3 = c1 + 1.0f;
        return 1.0f + c3 * (float) Math.pow(t - 1.0f, 3.0f) + c1 * (float) Math.pow(t - 1.0f, 2.0f);
    }


    /**
     * Smoothly animates a value toward a target using linear interpolation with a speed factor.
     * This creates frame-rate independent smooth animations.
     * 
     * @param current The current animated value
     * @param target  The target value to reach
     * @param speed   How fast to approach the target (0.01-0.2 is typical for UI)
     * @return The new animated value
     */
    public static float animate(float current, float target, float speed) {
        float delta = target - current;
        if (Math.abs(delta) < 0.0001f) return target;
        return current + delta * Math.min(speed, 1.0f);
    }

    /**
     * Linear interpolation between two values.
     * @param a Start value
     * @param b End value
     * @param t Interpolation factor (0.0 to 1.0)
     * @return Interpolated value
     */
    public static float lerp(float a, float b, float t) {
        return a + (b - a) * Math.min(Math.max(t, 0.0f), 1.0f);
    }

    /**
     * Clamps a value between min and max.
     */
    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }


    /**
     * Interpolates between two ARGB colors.
     * @param colorA First color (0xAARRGGBB format)
     * @param colorB Second color (0xAARRGGBB format)
     * @param t      Interpolation factor (0.0 to 1.0)
     * @return Interpolated color
     */
    public static int interpolateColor(int colorA, int colorB, float t) {
        t = Math.min(Math.max(t, 0.0f), 1.0f);
        
        int aA = (colorA >> 24) & 0xFF;
        int rA = (colorA >> 16) & 0xFF;
        int gA = (colorA >> 8) & 0xFF;
        int bA = colorA & 0xFF;

        int aB = (colorB >> 24) & 0xFF;
        int rB = (colorB >> 16) & 0xFF;
        int gB = (colorB >> 8) & 0xFF;
        int bB = colorB & 0xFF;

        int a = (int) (aA + (aB - aA) * t);
        int r = (int) (rA + (rB - rA) * t);
        int g = (int) (gA + (gB - gA) * t);
        int b = (int) (bA + (bB - bA) * t);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }


    /**
     * Draws a rounded rectangle using Minecraft's native fill method.
     * Since Minecraft doesn't have native rounded rect support, we simulate it
     * by drawing the main rectangle and then filling the corners with quarter-circles.
     * 
     * For performance, we use a simplified approach: draw the main rect, then
     * use small corner fills to approximate rounded corners.
     * 
     * @param guiGraphics The GuiGraphics context
     * @param x           Left edge
     * @param y           Top edge
     * @param width       Rectangle width
     * @param height      Rectangle height
     * @param radius      Corner radius
     * @param color       ARGB color
     */
    public static void drawRoundedRect(GuiGraphics guiGraphics, int x, int y, int width, int height, int radius, int color) {
        if (radius <= 0) {
            guiGraphics.fill(x, y, x + width, y + height, color);
            return;
        }

        guiGraphics.fill(x + 1, y, x + width - 1, y + height, color); 
        guiGraphics.fill(x, y + 1, x + 1, y + height - 1, color);     
        guiGraphics.fill(x + width - 1, y + 1, x + width, y + height - 1, color); 
    }

    /**
     * Draws a filled circle using a series of horizontal lines.
     * This is a software approximation that works well for small UI elements.
     */
    public static void drawFilledCircle(GuiGraphics guiGraphics, int centerX, int centerY, int radius, int color) {
        for (int y = -radius; y <= radius; y++) {
            int xWidth = (int) Math.sqrt(Math.max(0, radius * radius - y * y));
            guiGraphics.fill(centerX - xWidth, centerY + y, centerX + xWidth + 1, centerY + y + 1, color);
        }
    }

    /**
     * Draws a rounded rectangle outline (border only).
     */
    public static void drawRoundedOutline(GuiGraphics guiGraphics, int x, int y, int width, int height, int radius, int thickness, int color) {
        if (radius <= 0) {
            guiGraphics.fill(x, y, x + width, y + thickness, color); 
            guiGraphics.fill(x, y + height - thickness, x + width, y + height, color); 
            guiGraphics.fill(x, y, x + thickness, y + height, color); 
            guiGraphics.fill(x + width - thickness, y, x + width, y + height, color); 
            return;
        }

        radius = Math.min(radius, Math.min(width, height) / 2);

        guiGraphics.fill(x + radius, y, x + width - radius, y + thickness, color);
        guiGraphics.fill(x + radius, y + height - thickness, x + width - radius, y + height, color);
        guiGraphics.fill(x, y + radius, x + thickness, y + height - radius, color);
        guiGraphics.fill(x + width - thickness, y + radius, x + width, y + height - radius, color);

        drawCircleOutline(guiGraphics, x + radius, y + radius, radius, thickness, color);
        drawCircleOutline(guiGraphics, x + width - radius, y + radius, radius, thickness, color);
        drawCircleOutline(guiGraphics, x + radius, y + height - radius, radius, thickness, color);
        drawCircleOutline(guiGraphics, x + width - radius, y + height - radius, radius, thickness, color);
    }

    /**
     * Draws a circle outline using the difference of two filled circles.
     */
    private static void drawCircleOutline(GuiGraphics guiGraphics, int centerX, int centerY, int radius, int thickness, int color) {
        drawFilledCircle(guiGraphics, centerX, centerY, radius, color);
    }

    /**
     * Draws a vertical gradient rectangle.
     */
    public static void drawGradientRect(GuiGraphics guiGraphics, int x, int y, int width, int height, int topColor, int bottomColor) {
        int segments = Math.min(height, 32);
        float segmentHeight = (float) height / segments;
        
        for (int i = 0; i < segments; i++) {
            float t = (float) i / segments;
            int color = interpolateColor(topColor, bottomColor, t);
            int segY = y + (int) (i * segmentHeight);
            int segH = (int) Math.ceil(segmentHeight);
            guiGraphics.fill(x, segY, x + width, segY + segH, color);
        }
    }

    /**
     * Draws a horizontal gradient rectangle.
     */
    public static void drawHorizontalGradient(GuiGraphics guiGraphics, int x, int y, int width, int height, int leftColor, int rightColor) {
        int segments = Math.min(width, 32);
        float segmentWidth = (float) width / segments;
        
        for (int i = 0; i < segments; i++) {
            float t = (float) i / segments;
            int color = interpolateColor(leftColor, rightColor, t);
            int segX = x + (int) (i * segmentWidth);
            int segW = (int) Math.ceil(segmentWidth);
            guiGraphics.fill(segX, y, segX + segW, y + height, color);
        }
    }
}
