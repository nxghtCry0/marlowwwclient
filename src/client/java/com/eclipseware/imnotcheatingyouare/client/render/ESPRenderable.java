package com.eclipseware.imnotcheatingyouare.client.render;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.impl.ESP;
import xyz.breadloaf.imguimc.interfaces.Renderable;
import xyz.breadloaf.imguimc.interfaces.Theme;
import xyz.breadloaf.imguimc.theme.ImGuiDarkTheme;

public class ESPRenderable implements Renderable {
    private static final Theme THEME = new ImGuiDarkTheme();

    @Override
    public String getName() {
        return "ESP Overlay";
    }

    @Override
    public Theme getTheme() {
        return THEME;
    }

    @Override
    public void render() {
        if (ImnotcheatingyouareClient.INSTANCE == null || ImnotcheatingyouareClient.INSTANCE.moduleManager == null) return;
        ESP esp = (ESP) ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("ESP");
        if (esp != null && esp.isToggled()) {
            esp.renderImGuiOverlay();
        }
    }
}
