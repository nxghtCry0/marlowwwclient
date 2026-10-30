package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.gui.MarlowTitleScreen;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.module.ModuleManager;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.setting.SettingsManager;
import java.awt.Color;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;

public class Menu extends Module {

    public Menu() {
        super("Menu", Category.Client, "Customizes the custom main menu. Turn off to revert to normal Minecraft.");
        this.setToggled(true);
    }

    @Override
    public void onEnable() {
        if (mc == null) {
            return;
        }
        Screen current = mc.screen;
        if (current != null && current.getClass() == TitleScreen.class) {
            mc.setScreen(new MarlowTitleScreen());
        }
    }

    @Override
    public void onDisable() {
        if (mc == null) {
            return;
        }
        Screen current = mc.screen;
        if (current instanceof MarlowTitleScreen) {
            mc.setScreen(new TitleScreen());
        }
    }

    public static Module getMenuModule() {
        ImnotcheatingyouareClient client = ImnotcheatingyouareClient.INSTANCE;
        if (client == null) {
            return null;
        }
        ModuleManager mm = client.moduleManager;
        if (mm == null) {
            return null;
        }
        return mm.getModule("Menu");
    }

    public static boolean isCustomMenuEnabled() {
        Module mod = getMenuModule();
        if (mod == null) {
            return true;
        }
        return mod.isToggled();
    }

    public static boolean shouldReplacePanorama() {
        if (!isCustomMenuEnabled()) {
            return false;
        }
        String bg = getBackgroundMode();
        return "Shader".equalsIgnoreCase(bg);
    }

    public static String getBackgroundMode() {
        Setting s = getSetting("Background");
        if (s != null && s.isCombo()) {
            String val = s.getValString();
            if (val != null && !val.isEmpty()) {
                return val;
            }
        }
        return "Shader";
    }

    public static boolean shouldShowLogo() {
        Setting s = getSetting("Show Logo");
        return s == null || s.getValBoolean();
    }

    public static float getLogoScale() {
        Setting s = getSetting("Logo Scale");
        if (s != null && s.isSlider()) {
            return (float) s.getValDouble();
        }
        return 1.0f;
    }

    public static boolean shouldShowConfigButton() {
        Setting s = getSetting("Show Config Button");
        return s == null || s.getValBoolean();
    }

    public static boolean shouldShowWatermark() {
        Setting s = getSetting("Show Watermark");
        return s == null || s.getValBoolean();
    }

    public static int getButtonWidth() {
        Setting s = getSetting("Button Width");
        if (s != null && s.isSlider()) {
            return (int) Math.round(s.getValDouble());
        }
        return 200;
    }

    public static int getPanelAlpha() {
        Setting s = getSetting("Panel Alpha");
        if (s != null && s.isSlider()) {
            int a = (int) Math.round(s.getValDouble());
            return Math.max(0, Math.min(255, a));
        }
        return 150;
    }

    public static float getCornerRadius() {
        Setting s = getSetting("Corner Radius");
        if (s != null && s.isSlider()) {
            return (float) s.getValDouble();
        }
        return 12.0f;
    }

    public static float getAnimationSpeed() {
        Setting s = getSetting("Animation Speed");
        if (s != null && s.isSlider()) {
            return (float) Math.max(1.0, s.getValDouble());
        }
        return 5.0f;
    }

    public static float getHoverGlow() {
        Setting s = getSetting("Hover Glow");
        if (s != null && s.isSlider()) {
            return (float) (s.getValDouble() / 100.0);
        }
        return 0.35f;
    }

    public static Color getPrimaryColor() {
        Setting s = getSetting("Primary");
        if (s != null && s.isColor()) {
            int rgb = s.getValColor();
            return new Color((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
        }
        return new Color(255, 118, 26);
    }

    public static Color getSecondaryColor() {
        Setting s = getSetting("Secondary");
        if (s != null && s.isColor()) {
            int rgb = s.getValColor();
            return new Color((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
        }
        return new Color(18, 9, 28);
    }

    private static Setting getSetting(String name) {
        ImnotcheatingyouareClient client = ImnotcheatingyouareClient.INSTANCE;
        if (client == null) {
            return null;
        }
        SettingsManager sm = client.settingsManager;
        Module mod = getMenuModule();
        if (sm == null || mod == null) {
            return null;
        }
        return sm.getSettingByName(mod, name);
    }
}
