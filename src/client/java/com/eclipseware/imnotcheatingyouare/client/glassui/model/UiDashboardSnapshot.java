package com.eclipseware.imnotcheatingyouare.client.glassui.model;

import java.util.List;

public record UiDashboardSnapshot(
    String title,
    String subtitle,
    String badgeText,
    List<UiStatCard> stats,
    List<UiDashboardAction> actions
) {
}
