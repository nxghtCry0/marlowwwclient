package com.eclipseware.imnotcheatingyouare.client.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;

public class FontUtils {
    public static final Identifier VERDANA = Identifier.parse("imnotcheatingyouare:verdana");

    public static Component get(String text) {
        return Component.literal(text);
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
        return Minecraft.getInstance().font.width(get(text));
    }
}