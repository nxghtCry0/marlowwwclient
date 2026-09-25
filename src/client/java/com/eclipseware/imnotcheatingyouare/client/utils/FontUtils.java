package com.eclipseware.imnotcheatingyouare.client.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;

public class FontUtils {
    public static final Identifier VERDANA = Identifier.parse("imnotcheatingyouare:verdana");

    private static final java.util.Map<String, Component> componentCache = new java.util.LinkedHashMap<String, Component>(256, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(java.util.Map.Entry<String, Component> eldest) {
            return size() > 1024;
        }
    };

    public static boolean useVerdana() {
        try {
            com.eclipseware.imnotcheatingyouare.client.module.Module menu = 
                com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("Menu");
            if (menu != null) {
                com.eclipseware.imnotcheatingyouare.client.setting.Setting setting = 
                    com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(menu, "Use Verdana Font");
                if (setting != null) {
                    return setting.getValBoolean();
                }
            }
        } catch (Exception ignore) {}
        return false;
    }

    public static Component verdana(String text) {
        if (text == null) return Component.empty();
        return Component.literal(text).withStyle(Style.EMPTY.withFont(new FontDescription.Resource(VERDANA)));
    }

    public static int verdanaWidth(String text) {
        return Minecraft.getInstance().font.width(verdana(text));
    }

    public static Component get(String text) {
        if (text == null) return Component.empty();
        if (!useVerdana()) {
            return Component.literal(text);
        }
        Component cached = componentCache.get(text);
        if (cached == null) {
            cached = Component.literal(text).withStyle(Style.EMPTY.withFont(new FontDescription.Resource(VERDANA)));
            componentCache.put(text, cached);
        }
        return cached;
    }

    public static void drawString(GuiGraphicsExtractor graphics, String text, int x, int y, int color, boolean dropShadow) {
        graphics.text(Minecraft.getInstance().font, get(text), x, y, color, dropShadow);
    }

    public static void drawCenteredString(GuiGraphicsExtractor graphics, String text, int x, int y, int color) {
        graphics.centeredText(Minecraft.getInstance().font, get(text), x, y, color);
    }
    
    public static void drawRightAlignedString(GuiGraphicsExtractor graphics, String text, int rightX, int y, int color) {
        int width = width(text);
        graphics.text(Minecraft.getInstance().font, get(text), rightX - width, y, color, false);
    }

    public static int width(String text) {
        if (text == null) return 0;
        return Minecraft.getInstance().font.width(get(text));
    }
}