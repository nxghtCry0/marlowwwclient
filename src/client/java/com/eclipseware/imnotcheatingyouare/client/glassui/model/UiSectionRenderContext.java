package com.eclipseware.imnotcheatingyouare.client.glassui.model;

import com.eclipseware.imnotcheatingyouare.client.glassui.render.SmoothScroller;
import com.eclipseware.imnotcheatingyouare.client.glassui.theme.UiTheme;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public record UiSectionRenderContext(
    GuiGraphicsExtractor graphics,
    SmoothScroller scroller,
    float areaX,
    float areaW,
    float startY,
    float viewTop,
    float viewBottom,
    float mouseX,
    float mouseY,
    float animDt,
    float alpha,
    boolean interactive,
    UiScreenActions actions
) {
    public UiTheme theme() {
        return actions.theme();
    }

    public float elapsedSeconds() {
        return actions.elapsedSeconds();
    }
}
