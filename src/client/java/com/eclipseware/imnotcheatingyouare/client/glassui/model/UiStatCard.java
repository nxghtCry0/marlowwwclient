package com.eclipseware.imnotcheatingyouare.client.glassui.model;

import com.eclipseware.imnotcheatingyouare.client.glassui.asset.UiIcon;

public record UiStatCard(
    String label,
    String value,
    String detail,
    float progress,
    UiIcon icon
) {
}
