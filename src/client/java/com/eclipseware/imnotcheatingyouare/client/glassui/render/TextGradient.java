package com.eclipseware.imnotcheatingyouare.client.glassui.render;

import com.eclipseware.imnotcheatingyouare.client.glassui.theme.FontGradientStyle;
import com.eclipseware.imnotcheatingyouare.client.glassui.theme.UiTheme;
import com.eclipseware.imnotcheatingyouare.client.glassui.theme.UiThemeContext;

public record TextGradient(int topLeft, int topRight, int bottomLeft, int bottomRight) {
    public TextGradient withOpacity(float multiplier) {
        float m = clamp(multiplier, 0f, 1f);
        return new TextGradient(
            UiRenderPrimitives.multiplyAlpha(topLeft, m),
            UiRenderPrimitives.multiplyAlpha(topRight, m),
            UiRenderPrimitives.multiplyAlpha(bottomLeft, m),
            UiRenderPrimitives.multiplyAlpha(bottomRight, m)
        );
    }

    public int sample(float hx, float vy) {
        float tx = clamp(hx, 0f, 1f);
        float ty = clamp(vy, 0f, 1f);
        int top = UiRenderPrimitives.lerpColor(topLeft, topRight, tx);
        int bot = UiRenderPrimitives.lerpColor(bottomLeft, bottomRight, tx);
        return UiRenderPrimitives.lerpColor(top, bot, ty);
    }

    public static TextGradient lerp(TextGradient a, TextGradient b, float t) {
        float f = clamp(t, 0f, 1f);
        return new TextGradient(
            UiRenderPrimitives.lerpColor(a.topLeft, b.topLeft, f),
            UiRenderPrimitives.lerpColor(a.topRight, b.topRight, f),
            UiRenderPrimitives.lerpColor(a.bottomLeft, b.bottomLeft, f),
            UiRenderPrimitives.lerpColor(a.bottomRight, b.bottomRight, f)
        );
    }

    public static TextGradient pearl(float alpha, int accentColor, float accentMix, UiTheme theme) {
        int solidAccent = accentColor | 0xFF000000;
        int secAccent = theme.secondaryAccentColor() | 0xFF000000;
        float strength = theme.gradientStrength();
        FontGradientStyle style = theme.fontGradientStyle();
        TextGradient base = switch (style) {
            case AURORA -> {
                int tr = UiRenderPrimitives.lerpColor(0xFFFFFFFF, solidAccent, clamp(0.34f * strength, 0f, 1f));
                int bl = UiRenderPrimitives.lerpColor(0xFFE6EEF8, solidAccent, clamp(0.48f * strength, 0f, 1f));
                int br = UiRenderPrimitives.lerpColor(0xFFDCE6F4, secAccent, clamp(0.72f * strength, 0f, 1f));
                yield new TextGradient(0xFFFFFFFF, tr, bl, br);
            }
            case CHAMPAGNE -> {
                int tr = UiRenderPrimitives.lerpColor(0xFFFFFCF5, 0xFFF5D08A, clamp(0.38f * strength, 0f, 1f));
                int br = UiRenderPrimitives.lerpColor(0xFFE5DDD0, 0xFFD9A86C, clamp(0.65f * strength, 0f, 1f));
                yield new TextGradient(0xFFFFFEFA, tr, 0xFFEDE2D2, br);
            }
            case PLATINUM -> {
                int br = UiRenderPrimitives.lerpColor(0xFFD4DCE6, 0xFFB8D4F2, clamp(0.52f * strength, 0f, 1f));
                yield new TextGradient(0xFFFFFFFF, 0xFFF4F8FC, 0xFFD2DAE4, br);
            }
            case PEARL -> {
                float mix = clamp(accentMix * strength, 0f, 1f);
                int tr = UiRenderPrimitives.lerpColor(0xFFF5F9FF, solidAccent, mix * 0.35f);
                int br = UiRenderPrimitives.lerpColor(0xFFD6CFC8, secAccent, mix);
                yield new TextGradient(0xFFFFFFFF, tr, 0xFFD8D0C7, br);
            }
        };
        return base.withOpacity(alpha);
    }

    public static TextGradient pearl(float alpha) {
        return pearl(alpha, 0xFF2AA8FF, 0.26f, UiThemeContext.current());
    }

    public static TextGradient silver(float alpha, UiTheme theme) {
        float strength = theme.gradientStrength();
        int sec = theme.secondaryAccentColor() | 0xFF000000;
        FontGradientStyle style = theme.fontGradientStyle();
        TextGradient base = switch (style) {
            case AURORA -> new TextGradient(
                0xFFFAFBFD,
                UiRenderPrimitives.lerpColor(0xFFF0F5FB, sec, clamp(0.24f * strength, 0f, 0.8f)),
                0xFFC7D0DC,
                UiRenderPrimitives.lerpColor(0xFFB8C5D4, sec, clamp(0.42f * strength, 0f, 0.9f))
            );
            case CHAMPAGNE -> new TextGradient(0xFFFFFBF2, 0xFFF8EEDB, 0xFFD6C8B4, 0xFFCBB596);
            default -> new TextGradient(
                0xFFFAF8F5,
                0xFFF0F5FB,
                0xFFC7BFB6,
                UiRenderPrimitives.lerpColor(0xFFB8C5D4, sec, clamp(0.18f * strength, 0f, 0.6f))
            );
        };
        return base.withOpacity(alpha);
    }

    public static TextGradient mist(float alpha, UiTheme theme) {
        float strength = theme.gradientStrength();
        int acc = theme.accentColor() | 0xFF000000;
        FontGradientStyle style = theme.fontGradientStyle();
        TextGradient base = switch (style) {
            case AURORA -> new TextGradient(
                0xFFEAEFF7,
                UiRenderPrimitives.lerpColor(0xFFE4ECF5, acc, clamp(0.20f * strength, 0f, 0.7f)),
                0xFFA4B0C0,
                UiRenderPrimitives.lerpColor(0xFF97A4B3, acc, clamp(0.32f * strength, 0f, 0.8f))
            );
            case CHAMPAGNE -> new TextGradient(0xFFF2ECE2, 0xFFEDE2D0, 0xFFB5A896, 0xFFA8967E);
            default -> new TextGradient(0xFFEDE9E3, 0xFFE4ECF5, 0xFFA49C93, 0xFF97A4B3);
        };
        return base.withOpacity(alpha);
    }

    public static TextGradient accent(float alpha, int accentColor, UiTheme theme) {
        int solidAccent = accentColor | 0xFF000000;
        int secAccent = theme.secondaryAccentColor() | 0xFF000000;
        float strength = clamp(theme.gradientStrength(), 0.35f, 1.5f);
        TextGradient base = new TextGradient(
            0xFFFFFFFF,
            UiRenderPrimitives.lerpColor(0xFFFFFFFF, solidAccent, clamp(0.28f * strength, 0f, 1f)),
            UiRenderPrimitives.lerpColor(0xFFDDEEFF, solidAccent, clamp(0.52f * strength, 0f, 1f)),
            UiRenderPrimitives.lerpColor(0xFFCBE4FF, secAccent, clamp(0.78f * strength, 0f, 1f))
        );
        return base.withOpacity(alpha);
    }

    private static float clamp(float val, float min, float max) {
        return Math.max(min, Math.min(max, val));
    }
}
