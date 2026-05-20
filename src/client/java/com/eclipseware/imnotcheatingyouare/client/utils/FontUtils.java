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

    public static Component get(String text) {
        if (text == null) return Component.empty();
        Component cached = componentCache.get(text);
        if (cached == null) {
            cached = Component.literal(text);
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
        return Minecraft.getInstance().font.width(text);
    }
}