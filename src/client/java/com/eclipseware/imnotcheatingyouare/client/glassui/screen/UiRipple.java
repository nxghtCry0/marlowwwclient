package com.eclipseware.imnotcheatingyouare.client.glassui.screen;

public final class UiRipple {
    public final float x;
    public final float y;
    public final float maxRadius;
    public final int color;
    public float progress;

    public UiRipple(float x, float y, float maxRadius, int color, float progress) {
        this.x = x;
        this.y = y;
        this.maxRadius = maxRadius;
        this.color = color;
        this.progress = progress;
    }
}
