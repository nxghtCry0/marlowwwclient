package com.eclipseware.imnotcheatingyouare.client.glassui.model;

import com.eclipseware.imnotcheatingyouare.client.glassui.asset.UiIcon;

public final class UiChoice {
    private final String id;
    private final String label;
    private final UiIcon icon;
    private final String description;
    public float animSelected = 0f;
    public float animHover = 0f;
    public float animPress = 0f;

    public UiChoice(String id, String label, UiIcon icon, String description) {
        this.id = id;
        this.label = label;
        this.icon = icon;
        this.description = description != null ? description : "";
    }

    public String id() {
        return id;
    }

    public String label() {
        return label;
    }

    public UiIcon icon() {
        return icon;
    }

    public String description() {
        return description;
    }
}
