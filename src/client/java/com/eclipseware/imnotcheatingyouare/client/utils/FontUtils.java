package com.eclipseware.imnotcheatingyouare.client.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;

public class FontUtils {
    // Hooks directly into your high-res oversampled verdana.json provider
    public static final Identifier VERDANA = Identifier.parse("imnotcheatingyouare:verdana");

    public static Component get(String text) {
        // In 1.21+, the Identifier must be wrapped in a FontDescription.Resource
        return Component.literal(text).withStyle(Style.EMPTY.withFont(new FontDescription.Resource(VERDANA)));
    }

    public static void drawString(GuiGraphics graphics, String text, int x, int y, int color, boolean dropShadow) {
        graphics.drawString(Minecraft.getInstance().font, get(text), x, y, color, dropShadow);
    }

    public static void drawCenteredString(GuiGraphics graphics, String text, int x, int y, int color) {
        graphics.drawCenteredString(Minecraft.getInstance().font, get(text), x, y, color);
    }
    
    public static void drawRightAlignedString(GuiGraphics graphics, String text, int rightX, int y, int color) {
        int width = width(text);
        graphics.drawString(Minecraft.getInstance().font, get(text), rightX - width, y, color, false);
    }

    public static int width(String text) {
        return Minecraft.getInstance().font.width(get(text));
    }
}