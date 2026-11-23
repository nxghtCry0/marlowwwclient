package com.eclipseware.imnotcheatingyouare.client.glassui.model;

import com.eclipseware.imnotcheatingyouare.client.glassui.asset.UiIcon;
import java.util.List;

public final class UiCategory {
    private final String id;
    private final String title;
    private final UiIcon icon;
    private final List<UiTab> tabs;
    public float animHover = 0f;
    public float animActive = 0f;

    public UiCategory(String id, String title, UiIcon icon, List<UiTab> tabs) {
        this.id = id;
        this.title = title;
        this.icon = icon;
        this.tabs = tabs != null ? List.copyOf(tabs) : List.of();
    }

    public String id() {
        return id;
    }

    public String title() {
        return title;
    }

    public UiIcon icon() {
        return icon;
    }

    public List<UiTab> tabs() {
        return tabs;
    }
}
