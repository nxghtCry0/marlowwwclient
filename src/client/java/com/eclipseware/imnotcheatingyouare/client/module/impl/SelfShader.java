package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.utils.RenderUtils;
import net.minecraft.resources.Identifier;

import java.awt.Color;
import java.util.ArrayList;

public class SelfShader extends Module {
    public static SelfShader INSTANCE;

    public SelfShader() {
        super("SelfShader", Category.Render, "Highlights your own body and first-person hands with a custom shader.");
        INSTANCE = this;
        var sm = ImnotcheatingyouareClient.INSTANCE.settingsManager;
        sm.rSetting(new Setting("Hands", this, true));
        sm.rSetting(new Setting("Body", this, true));
        ArrayList<String> colorModes = new ArrayList<>();
        colorModes.add("Pastel Flow");
        colorModes.add("Theme");
        colorModes.add("Custom");
        sm.rSetting(new Setting("Color Mode", this, "Pastel Flow", colorModes));
        sm.rSetting(new Setting("Custom Color", this, new Color(190, 150, 255)));
        sm.rSetting(new Setting("Fill Opacity", this, 45.0, 0.0, 100.0, true));
        sm.rSetting(new Setting("Outline Width", this, 3.0, 1.0, 5.0, true));
        sm.rSetting(new Setting("Glow", this, 60.0, 0.0, 100.0, true));
    }

    public static boolean active() {
        return INSTANCE != null && INSTANCE.isToggled() && mc.player != null;
    }

    private Setting get(String name) {
        return ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, name);
    }

    public boolean hands() {
        Setting s = get("Hands");
        return active() && (s == null || s.getValBoolean());
    }

    public boolean body() {
        Setting s = get("Body");
        return active() && (s == null || s.getValBoolean());
    }

    public int colorMode() {
        Setting s = get("Color Mode");
        String mode = s != null ? s.getValString() : "Pastel Flow";
        return switch (mode) {
            case "Theme" -> 1;
            case "Custom" -> 2;
            default -> 0;
        };
    }

    public Color color() {
        int mode = colorMode();
        if (mode == 1) return RenderUtils.getThemeAccentColor();
        if (mode == 2) {
            Setting s = get("Custom Color");
            return s != null ? new Color(s.getValColor(), true) : new Color(190, 150, 255);
        }
        float t = (System.currentTimeMillis() % 8000L) / 8000f;
        return Color.getHSBColor(0.72f + 0.12f * (float) Math.sin(t * Math.PI * 2), 0.35f, 1.0f);
    }

    public float fill() {
        Setting s = get("Fill Opacity");
        return s != null ? (float) (s.getValDouble() / 100.0) : 0.45f;
    }

    public int outlineWidth() {
        Setting s = get("Outline Width");
        return s != null ? (int) Math.max(1, Math.min(5, s.getValDouble())) : 3;
    }

    public float glow() {
        Setting s = get("Glow");
        return s != null ? (float) (s.getValDouble() / 100.0) : 0.6f;
    }

    public Identifier shaderChainId() {
        int fill = (int) Math.max(0, Math.min(5, Math.round(fill() * 5f)));
        return Identifier.parse("imnotcheatingyouare:shader_esp_f" + outlineWidth() + "_a" + fill);
    }
}
