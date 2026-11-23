package com.eclipseware.imnotcheatingyouare.client.glassui.theme;

public interface UiTheme {
    float uiSizeScale();

    float cornerRadiusScale();

    float glassOpacityScale();

    float animationSpeedScale();

    int accentColor();

    int secondaryAccentColor();

    FontGradientStyle fontGradientStyle();

    float gradientStrength();

    GlassSurfacePreset glassSurface();

    float cardFrostScale();

    float backdropScrimBase();

    BorderStyle borderStyle();

    float activeGlowScale();

    MotionCurve motionCurve();

    boolean blurEnabled();

    boolean typingAnimationEnabled();

    boolean toastsEnabled();

    default int windowTopColor(int alpha) {
        int clamped = Math.max(0, Math.min(255, alpha));
        return switch (glassSurface()) {
            case OBSIDIAN -> (clamped << 24) | (38 << 16) | (39 << 8) | 43;
            case GLACIER -> (clamped << 24) | (52 << 16) | (66 << 8) | 84;
            case MIDNIGHT -> (clamped << 24) | (45 << 16) | (39 << 8) | 68;
            case STONE -> (clamped << 24) | (84 << 16) | (79 << 8) | 73;
        };
    }

    default int windowBottomColor(int alpha) {
        int clamped = Math.max(0, Math.min(255, alpha));
        return switch (glassSurface()) {
            case OBSIDIAN -> (clamped << 24) | (20 << 16) | (21 << 8) | 24;
            case GLACIER -> (clamped << 24) | (30 << 16) | (40 << 8) | 54;
            case MIDNIGHT -> (clamped << 24) | (25 << 16) | (22 << 8) | 41;
            case STONE -> (clamped << 24) | (53 << 16) | (49 << 8) | 45;
        };
    }

    default int backdropScrimAlpha(float easedOpen) {
        float base = Math.max(0f, Math.min(150f, backdropScrimBase()));
        int raw = (int) (base * easedOpen);
        return Math.max(0, Math.min(190, raw));
    }
}
