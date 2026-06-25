package com.eclipseware.imnotcheatingyouare.client.ui.reworked;

import com.eclipseware.imnotcheatingyouare.client.ui.GuiGraphics;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class Rounded {

    private Rounded() {}

    public static void fill(GuiGraphics g, int x, int y, int w, int h, int radius, int color) {
        if (w <= 0 || h <= 0) return;
        com.eclipseware.imnotcheatingyouare.client.utils.AnimationUtil.drawSquircleFilled(
                g.extractor(), x, y, w, h, Math.min(radius, Math.min(w, h) / 2f), color);
    }

    public static void outline(GuiGraphics g, int x, int y, int w, int h, int radius, float thickness, int color) {
        if (w <= 0 || h <= 0) return;
        com.eclipseware.imnotcheatingyouare.client.utils.AnimationUtil.drawSquircleOutline(
                g.extractor(), x, y, w, h, Math.min(radius, Math.min(w, h) / 2f), thickness, color);
    }

    public static void shadow(GuiGraphics g, int x, int y, int w, int h, int radius) {
        GuiGraphicsExtractor e = g.extractor();
        int r = Math.min(radius, Math.min(w, h) / 2);
        for (int i = 6; i >= 1; i--) {
            int a = (int) (8 * i * 0.5f);
            int col = (a << 24);
            com.eclipseware.imnotcheatingyouare.client.utils.AnimationUtil.drawSquircleFilled(
                    e, x - i, y - i + 1, w + i * 2, h + i * 2, r + i, col);
        }
    }

    public static void surface(GuiGraphics g, int x, int y, int w, int h, int radius, int bodyColor) {
        shadow(g, x, y, w, h, radius);
        fill(g, x, y, w, h, radius, bodyColor);
        outline(g, x, y, w, h, radius, 1f, 0x22FFFFFF);
    }

    public static void surfaceAccent(GuiGraphics g, int x, int y, int w, int h, int radius, int bodyColor, int accentColor) {
        fill(g, x, y, w, h, radius, bodyColor);
        outline(g, x, y, w, h, radius, 1.5f, accentColor);
    }

    public static int lerpArgb(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int aA = (a >>> 24) & 0xFF;
        int aR = (a >>> 16) & 0xFF;
        int aG = (a >>> 8) & 0xFF;
        int aB = a & 0xFF;
        int bA = (b >>> 24) & 0xFF;
        int bR = (b >>> 16) & 0xFF;
        int bG = (b >>> 8) & 0xFF;
        int bB = b & 0xFF;
        int oA = aA + Math.round((bA - aA) * t);
        int oR = aR + Math.round((bR - aR) * t);
        int oG = aG + Math.round((bG - aG) * t);
        int oB = aB + Math.round((bB - aB) * t);
        return (oA << 24) | (oR << 16) | (oG << 8) | oB;
    }

    public static int lerpRgb(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int aA = (a >>> 24) & 0xFF;
        int aR = (a >>> 16) & 0xFF;
        int aG = (a >>> 8) & 0xFF;
        int aB = a & 0xFF;
        int bR = (b >>> 16) & 0xFF;
        int bG = (b >>> 8) & 0xFF;
        int bB = b & 0xFF;
        int oR = aR + Math.round((bR - aR) * t);
        int oG = aG + Math.round((bG - aG) * t);
        int oB = aB + Math.round((bB - aB) * t);
        return (aA << 24) | (oR << 16) | (oG << 8) | oB;
    }
}
