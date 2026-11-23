package com.eclipseware.imnotcheatingyouare.client.glassui.model;

public record UiHitRect(float x, float y, float w, float h) {
    public static final UiHitRect EMPTY = new UiHitRect(0f, 0f, 0f, 0f);

    public boolean contains(float px, float py) {
        return px >= x && px <= x + w && py >= y && py <= y + h;
    }
}
