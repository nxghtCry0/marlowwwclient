package com.eclipseware.imnotcheatingyouare.client.glassui.theme;

import java.awt.Color;

public class MutableUiTheme implements UiTheme {
    private float uiSizeScale = 0.96f;
    private float cornerRadiusScale = 1.0f;
    private float glassOpacityScale = 1.0f;
    private float animationSpeedScale = 1.0f;

    private AccentPreset accentPreset = AccentPreset.OCEAN_SKY;
    private float customHue = 205f;
    private float customSaturation = 82f;
    private float customBrightness = 100f;
    private float dualToneShift = 24f;

    private FontGradientStyle fontGradientStyle = FontGradientStyle.PEARL;
    private float gradientStrength = 1.0f;
    private GlassSurfacePreset glassSurface = GlassSurfacePreset.STONE;
    private float cardFrostScale = 1.0f;
    private float backdropScrimBase = 72f;
    private BorderStyle borderStyle = BorderStyle.SUBTLE;
    private float activeGlowScale = 0.65f;
    private MotionCurve motionCurve = MotionCurve.SPRING;
    private boolean blurEnabled = true;
    private boolean typingAnimationEnabled = true;
    private boolean toastsEnabled = true;

    @Override
    public float uiSizeScale() {
        return uiSizeScale;
    }

    public void setUiSizeScale(float uiSizeScale) {
        this.uiSizeScale = uiSizeScale;
    }

    @Override
    public float cornerRadiusScale() {
        return cornerRadiusScale;
    }

    public void setCornerRadiusScale(float cornerRadiusScale) {
        this.cornerRadiusScale = cornerRadiusScale;
    }

    @Override
    public float glassOpacityScale() {
        return glassOpacityScale;
    }

    public void setGlassOpacityScale(float glassOpacityScale) {
        this.glassOpacityScale = glassOpacityScale;
    }

    @Override
    public float animationSpeedScale() {
        return animationSpeedScale;
    }

    public void setAnimationSpeedScale(float animationSpeedScale) {
        this.animationSpeedScale = animationSpeedScale;
    }

    public AccentPreset accentPreset() {
        return accentPreset;
    }

    public void setAccentPreset(AccentPreset accentPreset) {
        this.accentPreset = accentPreset != null ? accentPreset : AccentPreset.OCEAN_SKY;
    }

    public float customHue() {
        return customHue;
    }

    public void setCustomHue(float customHue) {
        this.customHue = customHue;
    }

    public float customSaturation() {
        return customSaturation;
    }

    public void setCustomSaturation(float customSaturation) {
        this.customSaturation = customSaturation;
    }

    public float customBrightness() {
        return customBrightness;
    }

    public void setCustomBrightness(float customBrightness) {
        this.customBrightness = customBrightness;
    }

    public float dualToneShift() {
        return dualToneShift;
    }

    public void setDualToneShift(float dualToneShift) {
        this.dualToneShift = dualToneShift;
    }

    @Override
    public FontGradientStyle fontGradientStyle() {
        return fontGradientStyle;
    }

    public void setFontGradientStyle(FontGradientStyle fontGradientStyle) {
        this.fontGradientStyle = fontGradientStyle != null ? fontGradientStyle : FontGradientStyle.PEARL;
    }

    @Override
    public float gradientStrength() {
        return gradientStrength;
    }

    public void setGradientStrength(float gradientStrength) {
        this.gradientStrength = gradientStrength;
    }

    @Override
    public GlassSurfacePreset glassSurface() {
        return glassSurface;
    }

    public void setGlassSurface(GlassSurfacePreset glassSurface) {
        this.glassSurface = glassSurface != null ? glassSurface : GlassSurfacePreset.STONE;
    }

    @Override
    public float cardFrostScale() {
        return cardFrostScale;
    }

    public void setCardFrostScale(float cardFrostScale) {
        this.cardFrostScale = cardFrostScale;
    }

    @Override
    public float backdropScrimBase() {
        return backdropScrimBase;
    }

    public void setBackdropScrimBase(float backdropScrimBase) {
        this.backdropScrimBase = backdropScrimBase;
    }

    @Override
    public BorderStyle borderStyle() {
        return borderStyle;
    }

    public void setBorderStyle(BorderStyle borderStyle) {
        this.borderStyle = borderStyle != null ? borderStyle : BorderStyle.SUBTLE;
    }

    @Override
    public float activeGlowScale() {
        return activeGlowScale;
    }

    public void setActiveGlowScale(float activeGlowScale) {
        this.activeGlowScale = activeGlowScale;
    }

    @Override
    public MotionCurve motionCurve() {
        return motionCurve;
    }

    public void setMotionCurve(MotionCurve motionCurve) {
        this.motionCurve = motionCurve != null ? motionCurve : MotionCurve.SPRING;
    }

    @Override
    public boolean blurEnabled() {
        return blurEnabled;
    }

    public void setBlurEnabled(boolean blurEnabled) {
        this.blurEnabled = blurEnabled;
    }

    @Override
    public boolean typingAnimationEnabled() {
        return typingAnimationEnabled;
    }

    public void setTypingAnimationEnabled(boolean typingAnimationEnabled) {
        this.typingAnimationEnabled = typingAnimationEnabled;
    }

    @Override
    public boolean toastsEnabled() {
        return toastsEnabled;
    }

    public void setToastsEnabled(boolean toastsEnabled) {
        this.toastsEnabled = toastsEnabled;
    }

    @Override
    public int accentColor() {
        if (accentPreset == AccentPreset.CUSTOM) {
            float h = clamp(customHue / 360f, 0f, 1f);
            float s = clamp(customSaturation / 100f, 0.20f, 1f);
            float b = clamp(customBrightness / 100f, 0.50f, 1f);
            return Color.HSBtoRGB(h, s, b);
        }
        int base = switch (accentPreset) {
            case EMERALD -> 0xFF2ED89A;
            case ROSE_PINK -> 0xFFFF5C8A;
            default -> 0xFF2AA8FF;
        };
        float briScale = clamp(customBrightness / 100f, 0.55f, 1f);
        if (briScale >= 0.99f) {
            return base;
        }
        int r = clampInt((int) (((base >>> 16) & 0xFF) * briScale), 0, 255);
        int g = clampInt((int) (((base >>> 8) & 0xFF) * briScale), 0, 255);
        int bl = clampInt((int) ((base & 0xFF) * briScale), 0, 255);
        return (0xFF << 24) | (r << 16) | (g << 8) | bl;
    }

    @Override
    public int secondaryAccentColor() {
        int primary = accentColor();
        if (Math.abs(dualToneShift) < 0.5f) {
            return primary;
        }
        int r = (primary >>> 16) & 0xFF;
        int g = (primary >>> 8) & 0xFF;
        int b = primary & 0xFF;
        float[] hsb = Color.RGBtoHSB(r, g, b, null);
        float shiftedH = ((hsb[0] + dualToneShift / 360f) % 1f + 1f) % 1f;
        return Color.HSBtoRGB(shiftedH, hsb[1], hsb[2]);
    }

    public void resetDefaults() {
        uiSizeScale = 0.96f;
        cornerRadiusScale = 1.0f;
        glassOpacityScale = 1.0f;
        animationSpeedScale = 1.0f;
        accentPreset = AccentPreset.OCEAN_SKY;
        customHue = 205f;
        customSaturation = 82f;
        customBrightness = 100f;
        dualToneShift = 24f;
        fontGradientStyle = FontGradientStyle.PEARL;
        gradientStrength = 1.0f;
        glassSurface = GlassSurfacePreset.STONE;
        cardFrostScale = 1.0f;
        backdropScrimBase = 72f;
        borderStyle = BorderStyle.SUBTLE;
        activeGlowScale = 0.65f;
        motionCurve = MotionCurve.SPRING;
        blurEnabled = true;
        typingAnimationEnabled = true;
        toastsEnabled = true;
    }

    private static float clamp(float val, float min, float max) {
        return Math.max(min, Math.min(max, val));
    }

    private static int clampInt(int val, int min, int max) {
        return Math.max(min, Math.min(max, val));
    }
}
