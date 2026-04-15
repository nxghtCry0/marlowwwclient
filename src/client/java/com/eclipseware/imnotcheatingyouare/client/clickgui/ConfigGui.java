package com.eclipseware.imnotcheatingyouare.client.clickgui;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.ConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.awt.Color;
import java.util.ArrayList;

public class ConfigGui extends Screen {
    private final ArrayList<Module> includedModules = new ArrayList<>();
    private double scrollY = 0;

    public ConfigGui() {
        super(Component.literal("Config Manager"));
        // Default to exporting everything
        includedModules.addAll(ImnotcheatingyouareClient.INSTANCE.moduleManager.modules);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        
        int w = this.width;
        int h = this.height;

        // Theme Sync
        Module theme = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("Theme");
        int r = 155, g = 60, b = 255;
        if (theme != null) {
            r = (int) ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(theme, "Accent R").getValDouble();
            g = (int) ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(theme, "Accent G").getValDouble();
            b = (int) ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(theme, "Accent B").getValDouble();
        }
        int accent = new Color(r, g, b).getRGB();

        // Background
        guiGraphics.fill(0, 0, w, h, new Color(10, 10, 10, 240).getRGB());
        com.eclipseware.imnotcheatingyouare.client.utils.FontUtils.drawCenteredString(guiGraphics, "Cloud Config Manager", w / 2, 20, accent);
        com.eclipseware.imnotcheatingyouare.client.utils.FontUtils.drawCenteredString(guiGraphics, "Select modules to include in your export", w / 2, 35, new Color(170, 170, 170).getRGB());

        // Buttons
        boolean exportHovered = mouseX >= w / 2 - 160 && mouseX <= w / 2 - 10 && mouseY >= h - 40 && mouseY <= h - 20;
        guiGraphics.fill(w / 2 - 160, h - 40, w / 2 - 10, h - 20, exportHovered ? new Color(50, 50, 52).getRGB() : new Color(30, 30, 32).getRGB());
        com.eclipseware.imnotcheatingyouare.client.utils.FontUtils.drawCenteredString(guiGraphics, "Export to Clipboard", w / 2 - 85, h - 34, -1);

        boolean importHovered = mouseX >= w / 2 + 10 && mouseX <= w / 2 + 160 && mouseY >= h - 40 && mouseY <= h - 20;
        guiGraphics.fill(w / 2 + 10, h - 40, w / 2 + 160, h - 20, importHovered ? new Color(50, 50, 52).getRGB() : new Color(30, 30, 32).getRGB());
        com.eclipseware.imnotcheatingyouare.client.utils.FontUtils.drawCenteredString(guiGraphics, "Import from Clipboard", w / 2 + 85, h - 34, accent);

        // Scrollable Module List
        guiGraphics.enableScissor(w / 2 - 100, 55, w / 2 + 100, h - 60);
        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate(0f, (float) scrollY);

        int y = 60;
        for (Module m : ImnotcheatingyouareClient.INSTANCE.moduleManager.modules) {
            boolean included = includedModules.contains(m);
            guiGraphics.fill(w / 2 - 80, y, w / 2 - 65, y + 15, included ? accent : new Color(40, 40, 45).getRGB());
            com.eclipseware.imnotcheatingyouare.client.utils.FontUtils.drawString(guiGraphics, m.getName(), w / 2 - 55, y + 4, included ? -1 : new Color(140, 140, 140).getRGB(), false);
            y += 20;
        }

        guiGraphics.pose().popMatrix();
        guiGraphics.disableScissor();
    }

    @Override
public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
double mouseX = event.x();
double mouseY = event.y();
int button = event.button();

    int w = this.width;
    int h = this.height;

    // Export Action
    if (mouseX >= w / 2 - 160 && mouseX <= w / 2 - 10 && mouseY >= h - 40 && mouseY <= h - 20) {
        String exported = ConfigManager.exportSpecific(includedModules);
        Minecraft.getInstance().keyboardHandler.setClipboard(exported);
        Clickgui.playSound();
        this.onClose();
        return true;
    }

    // Import Action
    if (mouseX >= w / 2 + 10 && mouseX <= w / 2 + 160 && mouseY >= h - 40 && mouseY <= h - 20) {
        String clipboard = Minecraft.getInstance().keyboardHandler.getClipboard();
        ConfigManager.importString(clipboard);
        Clickgui.playSound();
        this.onClose();
        return true;
    }

    // Module Toggling
    int y = 60 + (int) scrollY;
    for (Module m : ImnotcheatingyouareClient.INSTANCE.moduleManager.modules) {
        if (mouseX >= w / 2 - 80 && mouseX <= w / 2 + 80 && mouseY >= y && mouseY <= y + 15) {
            if (includedModules.contains(m)) includedModules.remove(m);
            else includedModules.add(m);
            Clickgui.playSound();
            return true;
        }
        y += 20;
    }

    return super.mouseClicked(event, doubleClick);
}

public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
    return handleScroll(scrollY);
}

public boolean mouseScrolled(double mouseX, double mouseY, double scrollDelta) {
    return handleScroll(scrollDelta);
}

private boolean handleScroll(double scrollDelta) {
    this.scrollY += scrollDelta * 15;
    if (this.scrollY > 0) this.scrollY = 0;
    return true;
}

}