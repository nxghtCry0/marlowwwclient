package com.eclipseware.imnotcheatingyouare.client.render;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.impl.*;
import xyz.breadloaf.imguimc.interfaces.Renderable;
import xyz.breadloaf.imguimc.interfaces.Theme;
import xyz.breadloaf.imguimc.theme.ImGuiDarkTheme;

public class ImGuiOverlayRenderable implements Renderable {
    private static final Theme THEME = new ImGuiDarkTheme();

    @Override
    public String getName() {
        return "ImGui Overlay Engine";
    }

    @Override
    public Theme getTheme() {
        return THEME;
    }

    @Override
    public void render() {
        if (ImnotcheatingyouareClient.INSTANCE == null || ImnotcheatingyouareClient.INSTANCE.moduleManager == null) return;

        com.eclipseware.imnotcheatingyouare.client.clickgui.ImGuiClickGui.render();

        ESP esp = (ESP) ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("ESP");
        if (esp != null && esp.isToggled()) {
            esp.renderImGuiOverlay();
        }

        Nametags nametags = (Nametags) ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("Nametags");
        if (nametags != null && nametags.isToggled()) {
            nametags.renderImGuiOverlay();
        }

        StorageESP storageESP = (StorageESP) ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("StorageESP");
        if (storageESP != null && storageESP.isToggled()) {
            storageESP.renderImGuiOverlay();
        }

        Tracers tracers = (Tracers) ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("Tracers");
        if (tracers != null && tracers.isToggled()) {
            tracers.renderImGuiOverlay();
        }

        Trajectories trajectories = (Trajectories) ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("Trajectories");
        if (trajectories != null && trajectories.isToggled()) {
            trajectories.renderImGuiOverlay();
        }

        com.eclipseware.imnotcheatingyouare.client.ui.ArrayListHud.INSTANCE.renderImGui();

        TargetHUD targetHUD = (TargetHUD) ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("TargetHUD");
        if (targetHUD != null) {
            targetHUD.renderImGuiOverlay();
        }

        BlockESP blockESP = (BlockESP) ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("BlockESP");
        if (blockESP != null) {
            blockESP.renderImGuiOverlay();
            blockESP.renderImGuiSelectorWindow();
        }

        WeakDevice weakDevice = (WeakDevice) ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("WeakDevice");
        if (weakDevice != null) {
            weakDevice.renderImGuiOverlay();
        }
    }
}
