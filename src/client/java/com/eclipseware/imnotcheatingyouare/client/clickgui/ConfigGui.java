package com.eclipseware.imnotcheatingyouare.client.clickgui;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.ConfigManager;
import com.eclipseware.imnotcheatingyouare.client.ui.GlassyTheme;
import com.eclipseware.imnotcheatingyouare.client.utils.FontUtils;
import com.eclipseware.imnotcheatingyouare.client.utils.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class ConfigGui extends Screen {
    private static final int PANEL_WIDTH = 520;
    private static final int PANEL_HEIGHT = 340;

    private final ArrayList<Module> includedModules = new ArrayList<>();
    private double scrollY = 0;
    private double targetScrollY = 0;
    private long lastRenderTime = 0;
    private boolean draggingScrollbar = false;
    private EditBox searchBox;
    
    private String statusMessage = "";
    private long statusMessageTime = 0;

    public ConfigGui() {
        super(Component.literal("Config Manager"));
        includedModules.addAll(ImnotcheatingyouareClient.INSTANCE.moduleManager.modules);
    }

    private void showStatus(String msg) {
        statusMessage = msg;
        statusMessageTime = System.currentTimeMillis() + 2500;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public float getScaleFactor() {
        float scale = 1.0f;
        if (this.width < 580f) {
            scale = Math.min(scale, this.width / 580f);
        }
        if (this.height < 380f) {
            scale = Math.min(scale, this.height / 380f);
        }
        return scale;
    }

    @Override
    protected void init() {
        this.clearWidgets();
        float scale = getScaleFactor();
        int virtualWidth = (int) (this.width / scale);
        int virtualHeight = (int) (this.height / scale);

        int startX = (virtualWidth - PANEL_WIDTH) / 2;
        int startY = (virtualHeight - PANEL_HEIGHT) / 2;

        searchBox = new EditBox(this.font, startX + 160, startY + 48, 180, 18, Component.literal("Search Modules"));
        searchBox.setMaxLength(30);
        this.addRenderableWidget(searchBox);
    }

    private void drawBorder(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int color) {
        graphics.fill(x, y, x + w, y + 1, color);
        graphics.fill(x, y + h - 1, x + w, y + h, color);
        graphics.fill(x, y, x + 1, y + h, color);
        graphics.fill(x + w - 1, y, x + w, y + h, color);
    }

    private List<Module> getFilteredModules() {
        List<Module> result = new ArrayList<>();
        String query = searchBox != null ? searchBox.getValue().trim().toLowerCase() : "";
        List<Module> all = ImnotcheatingyouareClient.INSTANCE.moduleManager.modules;
        for (Module m : all) {
            if (m.isHidden()) continue;
            if (query.isEmpty() || m.getName().toLowerCase().contains(query)) {
                result.add(m);
            }
        }
        return result;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        long now = System.currentTimeMillis();
        if (lastRenderTime == 0) lastRenderTime = now;
        float timeDelta = Math.min(0.1f, (now - lastRenderTime) / 1000f);
        lastRenderTime = now;

        float factor = 1f - (float) Math.exp(-12f * timeDelta);
        scrollY = scrollY + (targetScrollY - scrollY) * factor;

        float scale = getScaleFactor();
        int scaledMouseX = (int) (mouseX / scale);
        int scaledMouseY = (int) (mouseY / scale);

        guiGraphics.fill(0, 0, this.width, this.height, 0x88000000);

        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().scale(scale, scale);

        int virtualWidth = (int) (this.width / scale);
        int virtualHeight = (int) (this.height / scale);

        int startX = (virtualWidth - PANEL_WIDTH) / 2;
        int startY = (virtualHeight - PANEL_HEIGHT) / 2;

        int r = 155, g = 60, b = 255;
        Module theme = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("Theme");
        if (theme != null) {
            r = (int) ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(theme, "Accent R").getValDouble();
            g = (int) ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(theme, "Accent G").getValDouble();
            b = (int) ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(theme, "Accent B").getValDouble();
        }
        int accent = new Color(r, g, b).getRGB();

        // Main window background
        guiGraphics.fill(startX, startY, startX + PANEL_WIDTH, startY + PANEL_HEIGHT, GlassyTheme.PANEL_BG);
        drawBorder(guiGraphics, startX, startY, PANEL_WIDTH, PANEL_HEIGHT, GlassyTheme.PANEL_BORDER);
        guiGraphics.fill(startX, startY, startX + PANEL_WIDTH, startY + 2, accent);

        // Header text
        FontUtils.drawString(guiGraphics, "Cloud Config Manager", startX + 20, startY + 12, accent, false);
        FontUtils.drawString(guiGraphics, "Customize and export/import your setups.", startX + 20, startY + 26, 0xFF8F8F8F, false);

        // Sidebar background
        guiGraphics.fill(startX + 10, startY + 44, startX + 145, startY + PANEL_HEIGHT - 10, 0x15FFFFFF);
        drawBorder(guiGraphics, startX + 10, startY + 44, 135, PANEL_HEIGHT - 54, 0x22FFFFFF);

        // Sidebar buttons
        // Button 1: Export Config
        boolean exH = scaledMouseX >= startX + 15 && scaledMouseX <= startX + 140 && scaledMouseY >= startY + 50 && scaledMouseY <= startY + 75;
        guiGraphics.fill(startX + 15, startY + 50, startX + 140, startY + 75, exH ? 0x2EFFFFFF : 0x14FFFFFF);
        drawBorder(guiGraphics, startX + 15, startY + 50, 125, 25, exH ? 0x60FFFFFF : 0x20FFFFFF);
        FontUtils.drawCenteredString(guiGraphics, "Export Config", startX + 77, startY + 58, -1);

        // Button 2: Import Config
        boolean imH = scaledMouseX >= startX + 15 && scaledMouseX <= startX + 140 && scaledMouseY >= startY + 85 && scaledMouseY <= startY + 110;
        guiGraphics.fill(startX + 15, startY + 85, startX + 140, startY + 110, imH ? accent : 0x14FFFFFF);
        drawBorder(guiGraphics, startX + 15, startY + 85, 125, 25, imH ? accent : 0x20FFFFFF);
        FontUtils.drawCenteredString(guiGraphics, "Import Config", startX + 77, startY + 93, imH ? -1 : accent);

        // Button 3: Export Macros
        boolean exMH = scaledMouseX >= startX + 15 && scaledMouseX <= startX + 140 && scaledMouseY >= startY + 120 && scaledMouseY <= startY + 145;
        guiGraphics.fill(startX + 15, startY + 120, startX + 140, startY + 145, exMH ? 0x2EFFFFFF : 0x14FFFFFF);
        drawBorder(guiGraphics, startX + 15, startY + 120, 125, 25, exMH ? 0x60FFFFFF : 0x20FFFFFF);
        FontUtils.drawCenteredString(guiGraphics, "Export Macros", startX + 77, startY + 128, -1);

        // Button 4: Import Macros
        boolean imMH = scaledMouseX >= startX + 15 && scaledMouseX <= startX + 140 && scaledMouseY >= startY + 155 && scaledMouseY <= startY + 180;
        guiGraphics.fill(startX + 15, startY + 155, startX + 140, startY + 180, imMH ? accent : 0x14FFFFFF);
        drawBorder(guiGraphics, startX + 15, startY + 155, 125, 25, imMH ? accent : 0x20FFFFFF);
        FontUtils.drawCenteredString(guiGraphics, "Import Macros", startX + 77, startY + 163, imMH ? -1 : accent);

        // Right panel
        List<Module> filtered = getFilteredModules();
        List<Module> allModules = ImnotcheatingyouareClient.INSTANCE.moduleManager.modules;

        // Bulk selectors
        boolean allH = scaledMouseX >= startX + 350 && scaledMouseX <= startX + 390 && scaledMouseY >= startY + 48 && scaledMouseY <= startY + 66;
        guiGraphics.fill(startX + 350, startY + 48, startX + 390, startY + 66, allH ? 0x2EFFFFFF : 0x14FFFFFF);
        drawBorder(guiGraphics, startX + 350, startY + 48, 40, 18, allH ? 0x60FFFFFF : 0x20FFFFFF);
        FontUtils.drawCenteredString(guiGraphics, "All", startX + 370, startY + 53, -1);

        boolean noneH = scaledMouseX >= startX + 395 && scaledMouseX <= startX + 435 && scaledMouseY >= startY + 48 && scaledMouseY <= startY + 66;
        guiGraphics.fill(startX + 395, startY + 48, startX + 435, startY + 66, noneH ? 0x2EFFFFFF : 0x14FFFFFF);
        drawBorder(guiGraphics, startX + 395, startY + 48, 40, 18, noneH ? 0x60FFFFFF : 0x20FFFFFF);
        FontUtils.drawCenteredString(guiGraphics, "None", startX + 415, startY + 53, -1);

        boolean invH = scaledMouseX >= startX + 440 && scaledMouseX <= startX + 490 && scaledMouseY >= startY + 48 && scaledMouseY <= startY + 66;
        guiGraphics.fill(startX + 440, startY + 48, startX + 490, startY + 66, invH ? 0x2EFFFFFF : 0x14FFFFFF);
        drawBorder(guiGraphics, startX + 440, startY + 48, 50, 18, invH ? 0x60FFFFFF : 0x20FFFFFF);
        FontUtils.drawCenteredString(guiGraphics, "Invert", startX + 465, startY + 53, -1);

        // Modules Checklist Area
        int listHeight = 220;
        guiGraphics.fill(startX + 160, startY + 75, startX + 500, startY + 75 + listHeight, 0x10FFFFFF);
        drawBorder(guiGraphics, startX + 160, startY + 75, 340, listHeight, 0x22FFFFFF);

        guiGraphics.enableScissor(startX + 162, startY + 77, startX + 498, startY + listHeight + 73);
        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate(0f, (float) scrollY);

        int i = 0;
        for (Module m : filtered) {
            int col = i % 2;
            int row = i / 2;
            int itemX = startX + 165 + col * 165;
            int itemY = startY + 80 + row * 22;

            boolean included = includedModules.contains(m);
            boolean itemHovered = scaledMouseX >= itemX && scaledMouseX <= itemX + 160 
                && scaledMouseY >= itemY + (int) scrollY && scaledMouseY <= itemY + 18 + (int) scrollY 
                && scaledMouseY >= startY + 75 && scaledMouseY <= startY + 75 + listHeight;

            // Row Card BG
            guiGraphics.fill(itemX, itemY, itemX + 160, itemY + 18, itemHovered ? 0x25FFFFFF : 0x15FFFFFF);
            drawBorder(guiGraphics, itemX, itemY, 160, 18, itemHovered ? 0x3EFFFFFF : 0x1EFFFFFF);

            // Checkbox
            int checkColor = included ? accent : (itemHovered ? 0x2EFFFFFF : 0x14FFFFFF);
            guiGraphics.fill(itemX + 6, itemY + 3, itemX + 18, itemY + 15, checkColor);
            drawBorder(guiGraphics, itemX + 6, itemY + 3, 12, 12, included ? accent : (itemHovered ? 0x60FFFFFF : 0x20FFFFFF));
            if (included) {
                guiGraphics.fill(itemX + 9, itemY + 6, itemX + 15, itemY + 12, 0xFFFFFFFF);
            }

            FontUtils.drawString(guiGraphics, m.getName(), itemX + 24, itemY + 5, included ? -1 : 0xFFBFBFBF, false);
            i++;
        }

        guiGraphics.pose().popMatrix();
        guiGraphics.disableScissor();

        // Scrollbar logic & render
        int totalRows = (filtered.size() + 1) / 2;
        int contentHeight = totalRows * 22 + 10;
        if (contentHeight > listHeight) {
            int sbX = startX + 504;
            int sbY = startY + 75;
            int sbW = 3;
            int sbH = listHeight;
            guiGraphics.fill(sbX, sbY, sbX + sbW, sbY + sbH, 0x15FFFFFF);
            int thumbH = Math.max(15, (int) ((double) sbH / contentHeight * sbH));
            int maxScroll = contentHeight - sbH;
            double pct = scrollY / -maxScroll;
            int thumbY = sbY + (int) (pct * (sbH - thumbH));
            guiGraphics.fill(sbX, thumbY, sbX + sbW, thumbY + thumbH, draggingScrollbar ? accent : 0x44FFFFFF);
        }

        // Selected module count & Info
        FontUtils.drawString(guiGraphics, includedModules.size() + " of " + allModules.size() + " modules included", startX + 160, startY + 308, 0xFFEAEAEA, false);
        FontUtils.drawString(guiGraphics, "Filter matches: " + filtered.size(), startX + 400, startY + 308, 0xFF8F8F8F, false);

        // Status Message Toast Banner
        if (System.currentTimeMillis() < statusMessageTime) {
            long statusRemaining = statusMessageTime - System.currentTimeMillis();
            float alpha = 1f;
            if (statusRemaining < 500) {
                alpha = statusRemaining / 500f;
            }
            int textAlpha = (int) (alpha * 255) << 24;
            int bgAlpha = (int) (alpha * 0xEA) << 24;

            int toastW = FontUtils.width(statusMessage) + 30;
            int toastH = 20;
            int toastX = startX + (PANEL_WIDTH - toastW) / 2;
            int toastY = startY + PANEL_HEIGHT - 35;

            guiGraphics.fill(toastX, toastY, toastX + toastW, toastY + toastH, bgAlpha | 0x121214);
            drawBorder(guiGraphics, toastX, toastY, toastW, toastH, textAlpha | (accent & 0x00FFFFFF));
            FontUtils.drawCenteredString(guiGraphics, statusMessage, toastX + toastW / 2, toastY + 6, textAlpha | 0xFFFFFF);
        }

        super.extractRenderState(guiGraphics, scaledMouseX, scaledMouseY, partialTick);

        guiGraphics.pose().popMatrix();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        float scale = getScaleFactor();
        double mouseX = event.x() / scale;
        double mouseY = event.y() / scale;
        int button = event.button();

        int virtualWidth = (int) (this.width / scale);
        int virtualHeight = (int) (this.height / scale);

        int startX = (virtualWidth - PANEL_WIDTH) / 2;
        int startY = (virtualHeight - PANEL_HEIGHT) / 2;
        int listHeight = 220;

        List<Module> filtered = getFilteredModules();
        List<Module> allModules = ImnotcheatingyouareClient.INSTANCE.moduleManager.modules;

        if (button == 0) {
            // Sidebar buttons click
            if (mouseX >= startX + 15 && mouseX <= startX + 140) {
                if (mouseY >= startY + 50 && mouseY <= startY + 75) {
                    // Export Config
                    String exported = ConfigManager.exportSpecific(includedModules);
                    Minecraft.getInstance().keyboardHandler.setClipboard(exported);
                    Clickgui.playSound();
                    showStatus("Config copied to clipboard!");
                    return true;
                }
                if (mouseY >= startY + 85 && mouseY <= startY + 110) {
                    // Import Config
                    String clipboard = Minecraft.getInstance().keyboardHandler.getClipboard();
                    if (clipboard != null && !clipboard.isEmpty()) {
                        ConfigManager.importString(clipboard);
                        Clickgui.playSound();
                        showStatus("Config imported successfully!");
                    } else {
                        showStatus("Clipboard is empty!");
                    }
                    return true;
                }
                if (mouseY >= startY + 120 && mouseY <= startY + 145) {
                    // Export Macros
                    com.eclipseware.imnotcheatingyouare.client.macro.MacroManager.exportToClipboard();
                    Clickgui.playSound();
                    showStatus("Macros copied to clipboard!");
                    return true;
                }
                if (mouseY >= startY + 155 && mouseY <= startY + 180) {
                    // Import Macros
                    com.eclipseware.imnotcheatingyouare.client.macro.MacroManager.importFromClipboard();
                    Clickgui.playSound();
                    showStatus("Macros imported successfully!");
                    return true;
                }
            }

            // Bulk action clicks
            if (mouseY >= startY + 48 && mouseY <= startY + 66) {
                if (mouseX >= startX + 350 && mouseX <= startX + 390) {
                    // All
                    includedModules.clear();
                    includedModules.addAll(allModules);
                    Clickgui.playSound();
                    return true;
                }
                if (mouseX >= startX + 395 && mouseX <= startX + 435) {
                    // None
                    includedModules.clear();
                    Clickgui.playSound();
                    return true;
                }
                if (mouseX >= startX + 440 && mouseX <= startX + 490) {
                    // Invert
                    ArrayList<Module> temp = new ArrayList<>(allModules);
                    temp.removeAll(includedModules);
                    includedModules.clear();
                    includedModules.addAll(temp);
                    Clickgui.playSound();
                    return true;
                }
            }

            // Scrollbar click detection
            if (mouseX >= startX + 502 && mouseX <= startX + 510 && mouseY >= startY + 75 && mouseY <= startY + 295) {
                this.draggingScrollbar = true;
                return true;
            }

            // Checklist clicks
            if (mouseX >= startX + 160 && mouseX <= startX + 500 && mouseY >= startY + 75 && mouseY <= startY + 295) {
                int i = 0;
                for (Module m : filtered) {
                    int col = i % 2;
                    int row = i / 2;
                    int itemX = startX + 165 + col * 165;
                    int itemY = startY + 80 + row * 22 + (int) scrollY;

                    if (mouseX >= itemX && mouseX <= itemX + 160 && mouseY >= itemY && mouseY <= itemY + 18) {
                        if (includedModules.contains(m)) {
                            includedModules.remove(m);
                        } else {
                            includedModules.add(m);
                        }
                        Clickgui.playSound();
                        return true;
                    }
                    i++;
                }
            }
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        this.draggingScrollbar = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (this.draggingScrollbar) {
            float scale = getScaleFactor();
            double mouseY = event.y() / scale;

            int startY = (int) ((this.height / scale - PANEL_HEIGHT) / 2);
            int listY = startY + 75;
            int listHeight = 220;

            List<Module> filtered = getFilteredModules();
            int totalRows = (filtered.size() + 1) / 2;
            int contentHeight = totalRows * 22 + 10;

            if (contentHeight > listHeight) {
                double relativeY = mouseY - listY;
                double pct = relativeY / listHeight;
                pct = Math.max(0.0, Math.min(1.0, pct));
                targetScrollY = -pct * (contentHeight - listHeight);
            }
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        return handleScroll(verticalAmount);
    }



    private boolean handleScroll(double scrollDelta) {
        List<Module> filtered = getFilteredModules();
        int totalRows = (filtered.size() + 1) / 2;
        int contentHeight = totalRows * 22 + 10;
        int listHeight = 220;
        int maxScroll = Math.max(0, contentHeight - listHeight);

        targetScrollY += scrollDelta * 20;
        if (targetScrollY > 0) targetScrollY = 0;
        if (targetScrollY < -maxScroll) targetScrollY = -maxScroll;
        return true;
    }
}