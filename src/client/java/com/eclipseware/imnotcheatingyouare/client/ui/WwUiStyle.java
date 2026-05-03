package com.eclipseware.imnotcheatingyouare.client.ui;

import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;

import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public final class WwUiStyle {
    public static final int PANEL_BG = 369098752;
    public static final int PANEL_OUTLINE = 822083583;
    public static final int SECTION_BG = 419430399;
    public static final int SECTION_DIVIDER = 671088640;
    public static final int ROW_BG = 603979776;
    public static final int ROW_BG_HOVER = 774515242;
    public static final int ROW_BG_PRESSED = 875573296;
    public static final int ROW_DIVIDER = 536870911;
    public static final int SEGMENT_DIVIDER = 687865855;
    
    public static void drawPanelContainer(GuiGraphics guiGraphics, int x, int y, int w, int h) {
        if (guiGraphics == null || w <= 0 || h <= 0) {
            return;
        }
        guiGraphics.fill(x, y, x + w, y + h, 369098752);
        guiGraphics.renderOutline(x, y, w, h, 822083583);
    }
    
    public static void drawSectionHeader(GuiGraphics guiGraphics, int x, int y, int w, int h, String title) {
        if (guiGraphics == null || w <= 0 || h <= 0) {
            return;
        }
        guiGraphics.fill(x, y, x + w, y + h, 419430399);
        guiGraphics.fill(x, y + h - 1, x + w, y + h, 671088640);
        if (title != null && !title.isBlank()) {
            Font font = Minecraft.getInstance().font;
            Objects.requireNonNull(font); 
            int ty = y + Math.max(0, (h - 9) / 2);
            guiGraphics.drawString(font, Component.literal(title), x + 10, ty, -1381654, false);
        } 
    }

    public static void drawRowBackground(GuiGraphics guiGraphics, int x, int y, int w, int h, boolean hovered, boolean pressed, boolean active) {
        int bg;
        if (guiGraphics == null || w <= 0 || h <= 0) {
            return;
        }
        
        if (!active) {
            bg = 537923600;
        } else if (pressed && hovered) {
            bg = 875573296;
        } else if (hovered) {
            bg = 774515242;
        } else {
            bg = 603979776;
        } 
        guiGraphics.fill(x, y, x + w, y + h, bg);
    }
    
    public static void drawRowDivider(GuiGraphics guiGraphics, int x, int y, int w) {
        if (guiGraphics == null || w <= 0) {
            return;
        }
        guiGraphics.fill(x, y, x + w, y + 1, 536870911);
    }

    public static void drawToggleSegment(GuiGraphics guiGraphics, int x, int y, int w, int h, boolean on, boolean hovered, boolean active) {
        int bg, text;
        if (guiGraphics == null || w <= 0 || h <= 0) {
            return;
        }

        if (!active) {
            bg = 1074794512;
            text = -7364429;
        } else if (on) {
            bg = hovered ? -1070366925 : -1338869198;
            text = -4194369;
        } else {
            bg = hovered ? -1067832790 : -1336793562;
            text = -16449;
        } 
        guiGraphics.fill(x, y, x + w, y + h, bg);
        guiGraphics.renderOutline(x, y, w, h, 587202559);
        
        String stateText = on ? "ON" : "OFF";
        Font font = Minecraft.getInstance().font;
        Objects.requireNonNull(font); 
        int stateY = y + Math.max(0, (h - 9) / 2) + 1;
        guiGraphics.drawCenteredString(font, Component.literal(stateText), x + w / 2, stateY, text);
    }

    public static void drawToggleSegmentAnimated(GuiGraphics guiGraphics, int x, int y, int w, int h, float onVisual, float pressDepth, boolean hovered, boolean active) {
        int bg, text;
        if (guiGraphics == null || w <= 0 || h <= 0) {
            return;
        }
        onVisual = Mth.clamp(onVisual, 0.0F, 1.0F);
        pressDepth = Mth.clamp(pressDepth, 0.0F, 1.0F);
        
        int offBg = hovered ? -1067832790 : -1336793562;
        int onBg = hovered ? -1070366925 : -1338869198;
        int offText = -16449;
        int onText = -4194369;

        if (!active) {
            bg = 1074794512;
            text = -7364429;
        } else {
            bg = lerpArgb(offBg, onBg, onVisual);
            text = lerpArgb(offText, onText, onVisual);
        } 
        if (pressDepth > 0.0F) {
            bg = lerpArgb(bg, -16777216, pressDepth * 0.14F);
        }
        
        int pressOffsetY = active ? Math.max(0, Math.min(2, Math.round(pressDepth * 1.6F))) : 0;
        int px = x;
        int py = y + pressOffsetY;
        int pw = w;
        int ph = h;
        
        guiGraphics.fill(px, py, px + pw, py + ph, bg);
        int border = active ? lerpArgb(587202559, 436207616, pressDepth) : 587202559;
        guiGraphics.renderOutline(px, py, pw, ph, border);
        
        String stateText = (onVisual >= 0.5F) ? "ON" : "OFF";
        Font font = Minecraft.getInstance().font;
        Objects.requireNonNull(font); 
        int stateY = py + Math.max(0, (ph - 9) / 2) + 1;
        guiGraphics.drawCenteredString(font, Component.literal(stateText), px + pw / 2, stateY, text);
    }
    
    private static int lerpArgb(int a, int b, float t) {
        t = Mth.clamp(t, 0.0F, 1.0F);
        int aA = a >>> 24 & 0xFF;
        int aR = a >>> 16 & 0xFF;
        int aG = a >>> 8 & 0xFF;
        int aB = a & 0xFF;
        int bA = b >>> 24 & 0xFF;
        int bR = b >>> 16 & 0xFF;
        int bG = b >>> 8 & 0xFF;
        int bB = b & 0xFF;
        int oA = aA + Math.round((bA - aA) * t);
        int oR = aR + Math.round((bR - aR) * t);
        int oG = aG + Math.round((bG - aG) * t);
        int oB = aB + Math.round((bB - aB) * t);
        return oA << 24 | oR << 16 | oG << 8 | oB;
    }
    
    public static void drawDropdownCaret(GuiGraphics guiGraphics, int x, int y, int w, int h, boolean active) {
        if (guiGraphics == null || w <= 0 || h <= 0) {
            return;
        }
        int cx = x + (w - 6) / 2;
        int cy = y + h / 2;
        int caret = active ? -855638017 : 1442840575;
        guiGraphics.fill(cx, cy - 1, cx + 6, cy, caret);
        guiGraphics.fill(cx + 1, cy, cx + 5, cy + 1, caret);
        guiGraphics.fill(cx + 2, cy + 1, cx + 4, cy + 2, caret);
    }
}
