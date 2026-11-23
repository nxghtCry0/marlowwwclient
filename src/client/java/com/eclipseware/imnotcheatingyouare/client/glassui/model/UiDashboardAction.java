package com.eclipseware.imnotcheatingyouare.client.glassui.model;

import com.eclipseware.imnotcheatingyouare.client.glassui.asset.UiIcon;

public record UiDashboardAction(
    String title,
    String subtitle,
    UiIcon icon,
    boolean active,
    boolean danger,
    ClickHandler onClick
) {
    @FunctionalInterface
    public interface ClickHandler {
        void handle(UiScreenActions actions, float localX, float localY);
    }
}
