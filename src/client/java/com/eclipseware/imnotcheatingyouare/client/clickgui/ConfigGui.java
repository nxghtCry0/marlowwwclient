package com.eclipseware.imnotcheatingyouare.client.clickgui;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.ConfigManager;
import com.eclipseware.imnotcheatingyouare.client.utils.FontUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class ConfigGui extends Screen {
    private final ArrayList<Module> includedModules = new ArrayList<>();
    private double scrollY = 0;
    private double targetScrollY = 0;
    private long lastRenderTime = 0;

    public ConfigGui() {
        super(Component.literal("Config Manager"));
        includedModules.addAll(ImnotcheatingyouareClient.INSTANCE.moduleManager.modules);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void drawBorder(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int color) {
        graphics.fill(x, y, x + w, y + 1, color);
        graphics.fill(x, y + h - 1, x + w, y + h, color);
        graphics.fill(x, y, x + 1, y + h, color);
        graphics.fill(x + w - 1, y, x + w, y + h, color);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
        
        long now = System.currentTimeMillis();
        if (lastRenderTime == 0) lastRenderTime = now;
        float timeDelta = Math.min(0.1f, (now - lastRenderTime) / 1000f);
        lastRenderTime = now;

        float factor = 1f - (float)Math.exp(-12f * timeDelta);
        scrollY = scrollY + (targetScrollY - scrollY) * factor;

        int w = this.width;
        int h = this.height;

        Module theme = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("Theme");
        int r = 155, g = 60, b = 255;
        if (theme != null) {
            r = (int) ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(theme, "Accent R").getValDouble();
            g = (int) ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(theme, "Accent G").getValDouble();
            b = (int) ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(theme, "Accent B").getValDouble();
        }
        int accent = new Color(r, g, b).getRGB();

        int dialogWidth = 480;
        int dialogHeight = 320;
        int startX = (w - dialogWidth) / 2;
        int startY = (h - dialogHeight) / 2;

        guiGraphics.fill(0, 0, w, h, 0x8A08080A);
        
        guiGraphics.fill(startX, startY, startX + dialogWidth, startY + dialogHeight, 0xEE121214);
        drawBorder(guiGraphics, startX, startY, dialogWidth, dialogHeight, 0x22FFFFFF);
        guiGraphics.fill(startX, startY, startX + dialogWidth, startY + 2, accent);

        FontUtils.drawString(guiGraphics, "Cloud Config Manager", startX + 20, startY + 16, accent, false);
        FontUtils.drawString(guiGraphics, "Choose modules to include in your config export/import", startX + 20, startY + 30, 0xFF888888, false);
        
        List<Module> modules = ImnotcheatingyouareClient.INSTANCE.moduleManager.modules;
        FontUtils.drawRightAlignedString(guiGraphics, includedModules.size() + "/" + modules.size() + " Selected", startX + dialogWidth - 20, startY + 16, 0xFFAAAAAA);

        guiGraphics.fill(startX + 20, startY + 44, startX + dialogWidth - 20, startY + 45, 0x15FFFFFF);

        boolean allHovered = mouseX >= startX + 20 && mouseX <= startX + 75 && mouseY >= startY + 50 && mouseY <= startY + 65;
        guiGraphics.fill(startX + 20, startY + 50, startX + 75, startY + 65, allHovered ? 0x2EFFFFFF : 0x14FFFFFF);
        drawBorder(guiGraphics, startX + 20, startY + 50, 55, 15, allHovered ? 0x60FFFFFF : 0x20FFFFFF);
        FontUtils.drawCenteredString(guiGraphics, "All", startX + 47, startY + 54, -1);

        boolean noneHovered = mouseX >= startX + 80 && mouseX <= startX + 135 && mouseY >= startY + 50 && mouseY <= startY + 65;
        guiGraphics.fill(startX + 80, startY + 50, startX + 135, startY + 65, noneHovered ? 0x2EFFFFFF : 0x14FFFFFF);
        drawBorder(guiGraphics, startX + 80, startY + 50, 55, 15, noneHovered ? 0x60FFFFFF : 0x20FFFFFF);
        FontUtils.drawCenteredString(guiGraphics, "None", startX + 107, startY + 54, -1);

        boolean invertHovered = mouseX >= startX + 140 && mouseX <= startX + 195 && mouseY >= startY + 50 && mouseY <= startY + 65;
        guiGraphics.fill(startX + 140, startY + 50, startX + 195, startY + 65, invertHovered ? 0x2EFFFFFF : 0x14FFFFFF);
        drawBorder(guiGraphics, startX + 140, startY + 50, 55, 15, invertHovered ? 0x60FFFFFF : 0x20FFFFFF);
        FontUtils.drawCenteredString(guiGraphics, "Invert", startX + 167, startY + 54, -1);

        int listHeight = 150;
        guiGraphics.enableScissor(startX + 18, startY + 75, startX + dialogWidth - 18, startY + listHeight + 75);
        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate(0f, (float) scrollY);

        int i = 0;
        for (Module m : modules) {
            int col = i % 2;
            int row = i / 2;
            int itemX = startX + 20 + col * 230;
            int itemY = startY + 75 + row * 18;

            boolean included = includedModules.contains(m);
            boolean itemHovered = mouseX >= itemX && mouseX <= itemX + 210 && mouseY >= itemY + (int) scrollY && mouseY <= itemY + 16 + (int) scrollY && mouseY >= startY + 75 && mouseY <= startY + 75 + listHeight;
            
            guiGraphics.fill(itemX, itemY, itemX + 12, itemY + 12, included ? accent : (itemHovered ? 0x30FFFFFF : 0x15FFFFFF));
            drawBorder(guiGraphics, itemX, itemY, 12, 12, included ? accent : (itemHovered ? 0x60FFFFFF : 0x30FFFFFF));
            if (included) {
                guiGraphics.fill(itemX + 3, itemY + 3, itemX + 9, itemY + 9, 0xFFFFFFFF);
            }

            FontUtils.drawString(guiGraphics, m.getName(), itemX + 18, itemY + 2, included ? -1 : 0xFF888888, false);
            i++;
        }

        guiGraphics.pose().popMatrix();
        guiGraphics.disableScissor();

        int totalRows = (modules.size() + 1) / 2;
        int contentHeight = totalRows * 18;
        if (contentHeight > listHeight) {
            int sbX = startX + dialogWidth - 12;
            int sbY = startY + 75;
            int sbW = 3;
            int sbH = listHeight;
            guiGraphics.fill(sbX, sbY, sbX + sbW, sbY + sbH, 0x15FFFFFF);
            int thumbH = Math.max(15, (int) ((double) sbH / contentHeight * sbH));
            int maxScroll = contentHeight - sbH;
            double pct = targetScrollY / -maxScroll;
            int thumbY = sbY + (int) (pct * (sbH - thumbH));
            guiGraphics.fill(sbX, thumbY, sbX + sbW, thumbY + thumbH, accent);
        }

        guiGraphics.fill(startX + 20, startY + 235, startX + dialogWidth - 20, startY + 236, 0x15FFFFFF);

        boolean exportHovered = mouseX >= startX + 20 && mouseX <= startX + 230 && mouseY >= startY + 242 && mouseY <= startY + 267;
        guiGraphics.fill(startX + 20, startY + 242, startX + 230, startY + 267, exportHovered ? 0x22FFFFFF : 0x14FFFFFF);
        drawBorder(guiGraphics, startX + 20, startY + 242, 210, 25, exportHovered ? 0x60FFFFFF : 0x30FFFFFF);
        FontUtils.drawCenteredString(guiGraphics, "Export to Clipboard", startX + 125, startY + 250, -1);

        boolean importHovered = mouseX >= startX + 250 && mouseX <= startX + 460 && mouseY >= startY + 242 && mouseY <= startY + 267;
        guiGraphics.fill(startX + 250, startY + 242, startX + 460, startY + 267, importHovered ? accent : 0x14FFFFFF);
        drawBorder(guiGraphics, startX + 250, startY + 242, 210, 25, importHovered ? accent : 0x30FFFFFF);
        FontUtils.drawCenteredString(guiGraphics, "Import from Clipboard", startX + 355, startY + 250, importHovered ? -1 : accent);

        guiGraphics.fill(startX + 20, startY + 274, startX + dialogWidth - 20, startY + 275, 0x15FFFFFF);

        boolean exportMacroHovered = mouseX >= startX + 20 && mouseX <= startX + 230 && mouseY >= startY + 282 && mouseY <= startY + 307;
        guiGraphics.fill(startX + 20, startY + 282, startX + 230, startY + 307, exportMacroHovered ? 0x22FFFFFF : 0x14FFFFFF);
        drawBorder(guiGraphics, startX + 20, startY + 282, 210, 25, exportMacroHovered ? 0x60FFFFFF : 0x30FFFFFF);
        FontUtils.drawCenteredString(guiGraphics, "Export Macro to Clipboard", startX + 125, startY + 290, -1);

        boolean importMacroHovered = mouseX >= startX + 250 && mouseX <= startX + 460 && mouseY >= startY + 282 && mouseY <= startY + 307;
        guiGraphics.fill(startX + 250, startY + 282, startX + 460, startY + 307, importMacroHovered ? accent : 0x14FFFFFF);
        drawBorder(guiGraphics, startX + 250, startY + 282, 210, 25, importMacroHovered ? accent : 0x30FFFFFF);
        FontUtils.drawCenteredString(guiGraphics, "Import Macro from Clipboard", startX + 355, startY + 290, importMacroHovered ? -1 : accent);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();

        int w = this.width;
        int h = this.height;

        int dialogWidth = 480;
        int dialogHeight = 320;
        int startX = (w - dialogWidth) / 2;
        int startY = (h - dialogHeight) / 2;
        int listHeight = 150;

        List<Module> modules = ImnotcheatingyouareClient.INSTANCE.moduleManager.modules;

        if (button == 0) {
            if (mouseX >= startX + 20 && mouseX <= startX + 75 && mouseY >= startY + 50 && mouseY <= startY + 65) {
                includedModules.clear();
                includedModules.addAll(modules);
                Clickgui.playSound();
                return true;
            }

            if (mouseX >= startX + 80 && mouseX <= startX + 135 && mouseY >= startY + 50 && mouseY <= startY + 65) {
                includedModules.clear();
                Clickgui.playSound();
                return true;
            }

            if (mouseX >= startX + 140 && mouseX <= startX + 195 && mouseY >= startY + 50 && mouseY <= startY + 65) {
                ArrayList<Module> temp = new ArrayList<>(modules);
                temp.removeAll(includedModules);
                includedModules.clear();
                includedModules.addAll(temp);
                Clickgui.playSound();
                return true;
            }

            if (mouseX >= startX + 20 && mouseX <= startX + 230 && mouseY >= startY + 242 && mouseY <= startY + 267) {
                String exported = ConfigManager.exportSpecific(includedModules);
                Minecraft.getInstance().keyboardHandler.setClipboard(exported);
                Clickgui.playSound();
                this.onClose();
                return true;
            }

            if (mouseX >= startX + 250 && mouseX <= startX + 460 && mouseY >= startY + 242 && mouseY <= startY + 267) {
                String clipboard = Minecraft.getInstance().keyboardHandler.getClipboard();
                ConfigManager.importString(clipboard);
                Clickgui.playSound();
                this.onClose();
                return true;
            }

            if (mouseX >= startX + 20 && mouseX <= startX + 230 && mouseY >= startY + 282 && mouseY <= startY + 307) {
                com.eclipseware.imnotcheatingyouare.client.macro.MacroManager.exportToClipboard();
                Clickgui.playSound();
                this.onClose();
                return true;
            }

            if (mouseX >= startX + 250 && mouseX <= startX + 460 && mouseY >= startY + 282 && mouseY <= startY + 307) {
                com.eclipseware.imnotcheatingyouare.client.macro.MacroManager.importFromClipboard();
                Clickgui.playSound();
                this.onClose();
                return true;
            }

            int i = 0;
            for (Module m : modules) {
                int col = i % 2;
                int row = i / 2;
                int itemX = startX + 20 + col * 230;
                int itemY = startY + 75 + row * 18 + (int) scrollY;

                if (mouseX >= itemX && mouseX <= itemX + 210 && mouseY >= itemY && mouseY <= itemY + 16) {
                    if (mouseY >= startY + 75 && mouseY <= startY + 75 + listHeight) {
                        if (includedModules.contains(m)) includedModules.remove(m);
                        else includedModules.add(m);
                        Clickgui.playSound();
                        return true;
                    }
                }
                i++;
            }
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
        int totalRows = (ImnotcheatingyouareClient.INSTANCE.moduleManager.modules.size() + 1) / 2;
        int contentHeight = totalRows * 18;
        int maxScroll = Math.max(0, contentHeight - 150);
        
        targetScrollY += scrollDelta * 20;
        if (targetScrollY > 0) targetScrollY = 0;
        if (targetScrollY < -maxScroll) targetScrollY = -maxScroll;
        return true;
    }
}