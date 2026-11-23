package com.eclipseware.imnotcheatingyouare.client.render;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.ModuleManager;
import com.eclipseware.imnotcheatingyouare.client.module.impl.BlockESP;
import com.eclipseware.imnotcheatingyouare.client.module.impl.ESP;
import com.eclipseware.imnotcheatingyouare.client.module.impl.Nametags;
import com.eclipseware.imnotcheatingyouare.client.module.impl.SpawnerFinder;
import com.eclipseware.imnotcheatingyouare.client.module.impl.StorageESP;
import com.eclipseware.imnotcheatingyouare.client.module.impl.SuspiciousChunks;
import com.eclipseware.imnotcheatingyouare.client.module.impl.TargetHUD;
import com.eclipseware.imnotcheatingyouare.client.module.impl.Tracers;
import com.eclipseware.imnotcheatingyouare.client.module.impl.Trajectories;
import com.eclipseware.imnotcheatingyouare.client.module.impl.WeakDevice;
import com.eclipseware.imnotcheatingyouare.client.ui.ArrayListHud;
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
        ImnotcheatingyouareClient client = ImnotcheatingyouareClient.INSTANCE;
        if (client == null || client.moduleManager == null) {
            return;
        }
        ModuleManager mm = client.moduleManager;

        ESP esp = (ESP) mm.getModule("ESP");
        if (esp != null && esp.isToggled()) {
            esp.renderImGuiOverlay();
        }

        Nametags nametags = (Nametags) mm.getModule("Nametags");
        if (nametags != null && nametags.isToggled()) {
            nametags.renderImGuiOverlay();
        }

        StorageESP storageESP = (StorageESP) mm.getModule("StorageESP");
        if (storageESP != null && storageESP.isToggled()) {
            storageESP.renderImGuiOverlay();
        }

        Tracers tracers = (Tracers) mm.getModule("Tracers");
        if (tracers != null && tracers.isToggled()) {
            tracers.renderImGuiOverlay();
        }

        Trajectories trajectories = (Trajectories) mm.getModule("Trajectories");
        if (trajectories != null && trajectories.isToggled()) {
            trajectories.renderImGuiOverlay();
        }

        ArrayListHud.INSTANCE.renderImGui();

        TargetHUD targetHUD = (TargetHUD) mm.getModule("TargetHUD");
        if (targetHUD != null) {
            targetHUD.renderImGuiOverlay();
        }

        BlockESP blockESP = (BlockESP) mm.getModule("BlockESP");
        if (blockESP != null) {
            blockESP.renderImGuiOverlay();
        }

        SpawnerFinder spawnerFinder = (SpawnerFinder) mm.getModule("SpawnerFinder");
        if (spawnerFinder != null) {
            spawnerFinder.renderImGuiOverlay();
        }

        SuspiciousChunks suspiciousChunks = (SuspiciousChunks) mm.getModule("SuspiciousChunks");
        if (suspiciousChunks != null) {
            suspiciousChunks.renderImGuiOverlay();
        }

        WeakDevice weakDevice = (WeakDevice) mm.getModule("WeakDevice");
        if (weakDevice != null) {
            weakDevice.renderImGuiOverlay();
        }
    }
}
