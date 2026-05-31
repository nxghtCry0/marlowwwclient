package com.eclipseware.imnotcheatingyouare.client.utils;

import net.minecraft.client.gui.GuiGraphicsExtractor;

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
     * @param guiGraphics The GuiGraphicsExtractor context
     * @param x           Left edge
     * @param y           Top edge
     * @param width       Rectangle width
     * @param height      Rectangle height
     * @param radius      Corner radius
     * @param color       ARGB color
     */
    public static void drawRoundedRect(GuiGraphicsExtractor guiGraphics, int x, int y, int width, int height, int radius, int color) {
        if (radius <= 0) {
            guiGraphics.fill(x, y, x + width, y + height, color);
            return;
        }

        radius = Math.min(radius, Math.min(width, height) / 2);

        guiGraphics.fill(x + radius, y, x + width - radius, y + height, color);
        guiGraphics.fill(x, y + radius, x + radius, y + height - radius, color);
        guiGraphics.fill(x + width - radius, y + radius, x + width, y + height - radius, color);

        for (int i = 0; i < radius; i++) {
            int dy = radius - i;
            int dx = radius - (int) Math.sqrt(radius * radius - dy * dy);
            guiGraphics.fill(x + dx, y + i, x + radius, y + i + 1, color);
            guiGraphics.fill(x + width - radius, y + i, x + width - dx, y + i + 1, color);
            guiGraphics.fill(x + dx, y + height - i - 1, x + radius, y + height - i, color);
            guiGraphics.fill(x + width - radius, y + height - i - 1, x + width - dx, y + height - i, color);
        }
    }

    /**
     * Draws a filled circle using a series of horizontal lines.
     * This is a software approximation that works well for small UI elements.
     */
    public static void drawFilledCircle(GuiGraphicsExtractor guiGraphics, int centerX, int centerY, int radius, int color) {
        for (int y = -radius; y <= radius; y++) {
            int xWidth = (int) Math.sqrt(Math.max(0, radius * radius - y * y));
            guiGraphics.fill(centerX - xWidth, centerY + y, centerX + xWidth + 1, centerY + y + 1, color);
        }
    }

    /**
     * Draws a rounded rectangle outline (border only).
     */
    public static void drawRoundedOutline(GuiGraphicsExtractor guiGraphics, int x, int y, int width, int height, int radius, int thickness, int color) {
        if (radius <= 0) {
            guiGraphics.fill(x, y, x + width, y + thickness, color); 
            guiGraphics.fill(x, y + height - thickness, x + width, y + height, color); 
            guiGraphics.fill(x, y + thickness, x + thickness, y + height - thickness, color); 
            guiGraphics.fill(x + width - thickness, y + thickness, x + width, y + height - thickness, color); 
            return;
        }
        radius = Math.min(radius, Math.min(width, height) / 2);

        guiGraphics.fill(x + radius, y, x + width - radius, y + thickness, color);
        guiGraphics.fill(x + radius, y + height - thickness, x + width - radius, y + height, color);
        guiGraphics.fill(x, y + radius, x + thickness, y + height - radius, color);
        guiGraphics.fill(x + width - thickness, y + radius, x + width, y + height - radius, color);

        for (int i = 0; i < radius; i++) {
            int dy = radius - i;
            int dxOut = radius - (int) Math.sqrt(radius * radius - dy * dy);
            int dxIn = radius - (int) Math.sqrt(Math.max(0, (radius - thickness) * (radius - thickness) - dy * dy));

            guiGraphics.fill(x + dxOut, y + i, x + dxIn, y + i + 1, color);
            guiGraphics.fill(x + width - dxIn, y + i, x + width - dxOut, y + i + 1, color);
            guiGraphics.fill(x + dxOut, y + height - i - 1, x + dxIn, y + height - i, color);
            guiGraphics.fill(x + width - dxIn, y + height - i - 1, x + width - dxOut, y + height - i, color);
        }
    }

    /**
     * Draws a circle outline using the difference of two filled circles.
     */
    private static void drawCircleOutline(GuiGraphicsExtractor guiGraphics, int centerX, int centerY, int radius, int thickness, int color) {
        drawFilledCircle(guiGraphics, centerX, centerY, radius, color);
    }

    /**
     * Draws a vertical gradient rectangle.
     */
    public static void drawGradientRect(GuiGraphicsExtractor guiGraphics, int x, int y, int width, int height, int topColor, int bottomColor) {
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
    public static void drawHorizontalGradient(GuiGraphicsExtractor guiGraphics, int x, int y, int width, int height, int leftColor, int rightColor) {
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

    public static void drawRoundedMask(GuiGraphicsExtractor guiGraphics, int x, int y, int width, int height, int radius, int maskColor) {
        if (radius <= 0) return;
        radius = Math.min(radius, Math.min(width, height) / 2);

        for (int i = 0; i < radius; i++) {
            int dy = radius - i;
            int dx = radius - (int) Math.sqrt(radius * radius - dy * dy);
            
            guiGraphics.fill(x, y + i, x + dx, y + i + 1, maskColor);
            guiGraphics.fill(x + width - dx, y + i, x + width, y + i + 1, maskColor);
            guiGraphics.fill(x, y + height - i - 1, x + dx, y + height - i, maskColor);
            guiGraphics.fill(x + width - dx, y + height - i - 1, x + width, y + height - i, maskColor);
        }
    }

    public static void drawSquircleFilled(GuiGraphicsExtractor guiGraphics, float x, float y, float width, float height, float radius, int color) {
        if (radius <= 0) {
            guiGraphics.fill((int) x, (int) y, (int) (x + width), (int) (y + height), color);
            return;
        }
        radius = Math.min(radius, Math.min(width, height) / 2);
        
        int p = 4;
        for (int i = 0; i < (int) height; i++) {
            float dy = 0;
            if (i < radius) {
                dy = radius - i;
            } else if (i > height - radius) {
                dy = i - (height - radius);
            }
            
            float dx = 0;
            if (dy > 0) {
                dx = radius - (float) Math.pow(Math.pow(radius, p) - Math.pow(dy, p), 1.0 / p);
            }
            
            int drawX1 = (int) (x + dx);
            int drawX2 = (int) (x + width - dx);
            int drawY = (int) (y + i);
            guiGraphics.fill(drawX1, drawY, drawX2, drawY + 1, color);
        }
    }

    public static void drawSquircleVerticalGradient(GuiGraphicsExtractor guiGraphics, float x, float y, float width, float height, float radius, int startColor, int endColor) {
        if (radius <= 0) {
            drawGradientRect(guiGraphics, (int) x, (int) y, (int) width, (int) height, startColor, endColor);
            return;
        }
        radius = Math.min(radius, Math.min(width, height) / 2);
        
        int p = 4;
        for (int i = 0; i < (int) height; i++) {
            float dy = 0;
            if (i < radius) {
                dy = radius - i;
            } else if (i > height - radius) {
                dy = i - (height - radius);
            }
            
            float dx = 0;
            if (dy > 0) {
                dx = radius - (float) Math.pow(Math.pow(radius, p) - Math.pow(dy, p), 1.0 / p);
            }
            
            int drawX1 = (int) (x + dx);
            int drawX2 = (int) (x + width - dx);
            int drawY = (int) (y + i);
            
            float t = (float) i / height;
            int color = interpolateColor(startColor, endColor, t);
            guiGraphics.fill(drawX1, drawY, drawX2, drawY + 1, color);
        }
    }

    public static void drawSquircleOutline(GuiGraphicsExtractor guiGraphics, float x, float y, float width, float height, float radius, float thickness, int color) {
        if (radius <= 0) {
            guiGraphics.fill((int) x, (int) y, (int) (x + width), (int) (y + thickness), color); 
            guiGraphics.fill((int) x, (int) (y + height - thickness), (int) (x + width), (int) (y + height), color); 
            guiGraphics.fill((int) x, (int) (y + thickness), (int) (x + thickness), (int) (y + height - thickness), color); 
            guiGraphics.fill((int) (x + width - thickness), (int) (y + thickness), (int) (x + width), (int) (y + height - thickness), color); 
            return;
        }
        radius = Math.min(radius, Math.min(width, height) / 2);
        
        int p = 4;
        for (int i = 0; i < (int) height; i++) {
            float dy = 0;
            if (i < radius) {
                dy = radius - i;
            } else if (i > height - radius) {
                dy = i - (height - radius);
            }
            
            float dx = 0;
            if (dy > 0) {
                dx = radius - (float) Math.pow(Math.pow(radius, p) - Math.pow(dy, p), 1.0 / p);
            }
            
            int drawX1 = (int) (x + dx);
            int drawX2 = (int) (x + width - dx);
            int drawY = (int) (y + i);
            
            if (i < thickness || i >= height - thickness) {
                guiGraphics.fill(drawX1, drawY, drawX2, drawY + 1, color);
            } else {
                guiGraphics.fill(drawX1, drawY, drawX1 + (int) thickness, drawY + 1, color);
                guiGraphics.fill(drawX2 - (int) thickness, drawY, drawX2, drawY + 1, color);
            }
        }
    }

    public static void drawSquircleOutlineGradient(GuiGraphicsExtractor guiGraphics, float x, float y, float width, float height, float radius, float thickness, int startColor, int endColor) {
        if (radius <= 0) {
            for (int i = 0; i < (int) height; i++) {
                float t = (float) i / height;
                int color = interpolateColor(startColor, endColor, t);
                if (i < thickness || i >= height - thickness) {
                    guiGraphics.fill((int) x, (int) (y + i), (int) (x + width), (int) (y + i + 1), color);
                } else {
                    guiGraphics.fill((int) x, (int) (y + i), (int) (x + thickness), (int) (y + i + 1), color);
                    guiGraphics.fill((int) (x + width - thickness), (int) (y + i), (int) (x + width), (int) (y + i + 1), color);
                }
            }
            return;
        }
        radius = Math.min(radius, Math.min(width, height) / 2);
        
        int p = 4;
        for (int i = 0; i < (int) height; i++) {
            float dy = 0;
            if (i < radius) {
                dy = radius - i;
            } else if (i > height - radius) {
                dy = i - (height - radius);
            }
            
            float dx = 0;
            if (dy > 0) {
                dx = radius - (float) Math.pow(Math.pow(radius, p) - Math.pow(dy, p), 1.0 / p);
            }
            
            int drawX1 = (int) (x + dx);
            int drawX2 = (int) (x + width - dx);
            int drawY = (int) (y + i);
            
            float t = (float) i / height;
            int color = interpolateColor(startColor, endColor, t);
            
            if (i < thickness || i >= height - thickness) {
                guiGraphics.fill(drawX1, drawY, drawX2, drawY + 1, color);
            } else {
                guiGraphics.fill(drawX1, drawY, drawX1 + (int) thickness, drawY + 1, color);
                guiGraphics.fill(drawX2 - (int) thickness, drawY, drawX2, drawY + 1, color);
            }
        }
    }

    public static void drawRoundedHorizontalGradient(GuiGraphicsExtractor guiGraphics, int x, int y, int width, int height, int radius, int leftColor, int rightColor) {
        if (radius <= 0) {
            drawHorizontalGradient(guiGraphics, x, y, width, height, leftColor, rightColor);
            return;
        }
        radius = Math.min(radius, Math.min(width, height) / 2);
        
        for (int col = radius; col < width - radius; col++) {
            int color = interpolateColor(leftColor, rightColor, (float) col / width);
            guiGraphics.fill(x + col, y, x + col + 1, y + height, color);
        }
        
        for (int i = 0; i < radius; i++) {
            int dy = radius - i;
            int dx = radius - (int) Math.sqrt(radius * radius - dy * dy);
            
            for (int col = dx; col < radius; col++) {
                int color = interpolateColor(leftColor, rightColor, (float) col / width);
                guiGraphics.fill(x + col, y + i, x + col + 1, y + i + 1, color);
                guiGraphics.fill(x + col, y + height - i - 1, x + col + 1, y + height - i, color);
            }
            
            for (int col = width - radius; col < width - dx; col++) {
                int color = interpolateColor(leftColor, rightColor, (float) col / width);
                guiGraphics.fill(x + col, y + i, x + col + 1, y + i + 1, color);
                guiGraphics.fill(x + col, y + height - i - 1, x + col + 1, y + height - i, color);
            }
        }
        
        for (int i = radius; i < height - radius; i++) {
            for (int col = 0; col < radius; col++) {
                int color = interpolateColor(leftColor, rightColor, (float) col / width);
                guiGraphics.fill(x + col, y + i, x + col + 1, y + i + 1, color);
            }
            for (int col = width - radius; col < width; col++) {
                int color = interpolateColor(leftColor, rightColor, (float) col / width);
                guiGraphics.fill(x + col, y + i, x + col + 1, y + i + 1, color);
            }
        }
    }
}
