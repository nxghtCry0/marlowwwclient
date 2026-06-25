package com.eclipseware.imnotcheatingyouare.client.ui.reworked;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;

import java.awt.Color;

public final class ReworkedTheme {

    public static boolean reworkedUI = false;
    public static boolean useVerdana = false;

    public static int accent  = 0xFF9B3CFF;
    public static int accentDim = 0x809B3CFF;
    public static int background = 0xF0141414;
    public static int bgRgb = 0x141414;

    public static int text       = 0xFFEAEAEA;
    public static int textMuted  = 0xFFBFBFBF;
    public static int textSubtle = 0xFF8F8F8F;

    public static int controlBg       = 0x40181818;
    public static int controlBgHover  = 0x55282828;
    public static int controlBgActive = 0x70383838;
    public static int controlBorder       = 0x33000000;
    public static int controlBorderHover = 0x55FFFFFF;

    public static int rowHover   = 0x18FFFFFF;
    public static int rowSelected = 0x22FFFFFF;

    public static int radius = 12;
    public static float animSpeed = 1.0f;
    public static float hoverGlow = 0.35f;

    private ReworkedTheme() {}

    public static void refresh() {
        ImnotcheatingyouareClient client = ImnotcheatingyouareClient.INSTANCE;
        if (client == null || client.moduleManager == null || client.settingsManager == null) return;

        Module menu = client.moduleManager.getModule("Menu");
        if (menu == null) return;

        reworkedUI   = bool(menu, "Reworked UI", false);
        useVerdana   = bool(menu, "Use Verdana Font", false);

        int ar = (int) dbl(menu, "Accent R", 155);
        int ag = (int) dbl(menu, "Accent G", 60);
        int ab = (int) dbl(menu, "Accent B", 255);
        accent     = 0xFF000000 | (ar << 16) | (ag << 8) | ab;
        accentDim  = (0x80 << 24) | (ar << 16) | (ag << 8) | ab;

        int br = (int) dbl(menu, "Background R", 20);
        int bgc = (int) dbl(menu, "Background G", 20);
        int bb = (int) dbl(menu, "Background B", 20);
        int ba = (int) dbl(menu, "Background Alpha", 240);
        bgRgb       = (br << 16) | (bgc << 8) | bb;
        background  = (ba << 24) | bgRgb;

        controlBg       = withAlpha(brighten(bgRgb, 0.10f), 0x40);
        controlBgHover  = withAlpha(brighten(bgRgb, 0.18f), 0x55);
        controlBgActive = withAlpha(brighten(bgRgb, 0.30f), 0x70);

        radius    = (int) Math.max(0, dbl(menu, "Corner Radius", 12));
        animSpeed = (float) Math.max(0.1, dbl(menu, "Animation Speed", 5));
        hoverGlow = (float) Math.max(0, Math.min(1, dbl(menu, "Hover Glow", 35) / 100.0));
    }

    private static boolean bool(Module m, String name, boolean def) {
        Setting s = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(m, name);
        return s != null ? s.getValBoolean() : def;
    }
    private static double dbl(Module m, String name, double def) {
        Setting s = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(m, name);
        return s != null ? s.getValDouble() : def;
    }

    public static int brighten(int rgb, float t) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        if (t >= 0) {
            r = (int) (r + (255 - r) * t);
            g = (int) (g + (255 - g) * t);
            b = (int) (b + (255 - b) * t);
        } else {
            r = (int) (r * (1 + t));
            g = (int) (g * (1 + t));
            b = (int) (b * (1 + t));
        }
        return (r << 16) | (g << 8) | b;
    }

    public static int withAlpha(int rgb, int alpha) {
        return (alpha << 24) | (rgb & 0x00FFFFFF);
    }

    public static Color accentColor() {
        return new Color(accent, true);
    }
}
