package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.utils.RenderUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;

import java.awt.Color;
import java.util.ArrayList;

public class Chams extends Module {

    public Chams() {
        super("Chams", Category.Render, "Renders a clean, relaxing glowing outline around character and entity models.");

        ArrayList<String> colorModes = new ArrayList<>();
        colorModes.add("Custom");
        colorModes.add("Theme");
        colorModes.add("Health");
        colorModes.add("Distance");
        colorModes.add("Rainbow");
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Color Mode", this, "Custom", colorModes));

        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Show Players", this, true));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Show Self", this, true));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Show Mobs", this, true));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Show Crystals", this, true));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Show Items", this, false));

        // Relaxing pastel/neon default colors
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Player Color", this, new Color(0, 235, 215)));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Self Color", this, new Color(167, 139, 250)));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Mob Color", this, new Color(251, 191, 36)));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Crystal Color", this, new Color(244, 114, 182)));

        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Wallhack Colors", this, false));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Visible Color", this, new Color(52, 211, 153)));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Occluded Color", this, new Color(251, 113, 133)));

        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Max Distance", this, 128.0, 16.0, 256.0, true));
    }

    public boolean shouldGlow(Entity entity) {
        if (!isToggled() || entity == null || mc.player == null) return false;

        Setting maxDistSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Max Distance");
        double maxDist = maxDistSetting != null ? maxDistSetting.getValDouble() : 128.0;
        if (mc.player.distanceTo(entity) > maxDist) return false;

        if (entity == mc.player) {
            Setting selfSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Show Self");
            return selfSetting != null && selfSetting.getValBoolean();
        }

        if (entity instanceof Player) {
            Setting playersSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Show Players");
            return playersSetting == null || playersSetting.getValBoolean();
        }

        if (entity instanceof Mob || entity instanceof LivingEntity) {
            Setting mobsSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Show Mobs");
            return mobsSetting != null && mobsSetting.getValBoolean();
        }

        if (entity instanceof EndCrystal) {
            Setting crystalsSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Show Crystals");
            return crystalsSetting != null && crystalsSetting.getValBoolean();
        }

        if (entity instanceof ItemEntity) {
            Setting itemsSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Show Items");
            return itemsSetting != null && itemsSetting.getValBoolean();
        }

        return false;
    }

    public int getGlowColor(Entity entity) {
        if (entity == null) return 0xFFFFFF;

        Setting wallhackSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Wallhack Colors");
        if (wallhackSetting != null && wallhackSetting.getValBoolean() && mc.player != null && entity != mc.player) {
            boolean visible = mc.player.hasLineOfSight(entity);
            if (!visible) {
                Setting occSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Occluded Color");
                return occSetting != null ? (occSetting.getValColor() & 0xFFFFFF) : 0xFB7185;
            } else {
                Setting visSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Visible Color");
                return visSetting != null ? (visSetting.getValColor() & 0xFFFFFF) : 0x34D399;
            }
        }

        Setting colorModeSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Color Mode");
        String colorMode = colorModeSetting != null ? colorModeSetting.getValString() : "Custom";

        switch (colorMode) {
            case "Theme" -> {
                return RenderUtils.getThemeAccentColor().getRGB() & 0xFFFFFF;
            }
            case "Health" -> {
                if (entity instanceof LivingEntity living) {
                    float pct = Math.min(1f, Math.max(0f, living.getHealth() / Math.max(1f, living.getMaxHealth())));
                    return RenderUtils.getHealthColor(pct).getRGB() & 0xFFFFFF;
                }
                return 0x34D399;
            }
            case "Distance" -> {
                if (mc.player != null) {
                    Setting maxDistSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Max Distance");
                    double maxDist = maxDistSetting != null ? maxDistSetting.getValDouble() : 128.0;
                    float pct = (float) Math.min(1.0, mc.player.distanceTo(entity) / maxDist);
                    return Color.getHSBColor((1.0f - pct) * 0.33f, 0.9f, 1.0f).getRGB() & 0xFFFFFF;
                }
                return 0x38BDF8;
            }
            case "Rainbow" -> {
                float hue = ((System.currentTimeMillis() + (entity.getId() * 120L)) % 4000L) / 4000.0f;
                return Color.getHSBColor(hue, 0.85f, 1.0f).getRGB() & 0xFFFFFF;
            }
            default -> {
                if (entity == mc.player) {
                    Setting selfColorSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Self Color");
                    return selfColorSetting != null ? (selfColorSetting.getValColor() & 0xFFFFFF) : 0xA78BFA;
                } else if (entity instanceof Player) {
                    Setting playerColorSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Player Color");
                    return playerColorSetting != null ? (playerColorSetting.getValColor() & 0xFFFFFF) : 0x00EBD7;
                } else if (entity instanceof EndCrystal) {
                    Setting crystalColorSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Crystal Color");
                    return crystalColorSetting != null ? (crystalColorSetting.getValColor() & 0xFFFFFF) : 0xF472B6;
                } else {
                    Setting mobColorSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Mob Color");
                    return mobColorSetting != null ? (mobColorSetting.getValColor() & 0xFFFFFF) : 0xFBBF24;
                }
            }
        }
    }
}
