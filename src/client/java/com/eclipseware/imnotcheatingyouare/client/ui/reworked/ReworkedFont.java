package com.eclipseware.imnotcheatingyouare.client.ui.reworked;

import com.eclipseware.imnotcheatingyouare.client.ui.GuiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

public final class ReworkedFont {

    public static final Identifier VERDANA_ID = Identifier.parse("imnotcheatingyouare:verdana");

    private static final java.util.Map<String, Component> CACHE = new java.util.LinkedHashMap<>(256, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(java.util.Map.Entry<String, Component> eldest) {
            return size() > 1024;
        }
    };

    private ReworkedFont() {}

    public static Component wrap(String text) {
        if (text == null) return Component.empty();
        String key = (ReworkedTheme.useVerdana ? "V|" : "M|") + text;
        Component cached = CACHE.get(key);
        if (cached != null) return cached;

        MutableComponent comp = Component.literal(text);
        if (ReworkedTheme.useVerdana) {
            comp.setStyle(Style.EMPTY.withFont(new FontDescription.Resource(VERDANA_ID)));
        }
        CACHE.put(key, comp);
        return comp;
    }

    public static void drawString(GuiGraphics g, String text, int x, int y, int color, boolean shadow) {
        Font font = Minecraft.getInstance().font;
        g.extractor().text(font, wrap(text), x, y, color, shadow);
    }

    public static void drawCenteredString(GuiGraphics g, String text, int x, int y, int color) {
        Font font = Minecraft.getInstance().font;
        g.extractor().centeredText(font, wrap(text), x, y, color);
    }

    public static void drawRightAlignedString(GuiGraphics g, String text, int rightX, int y, int color) {
        Font font = Minecraft.getInstance().font;
        int w = font.width(wrap(text));
        g.extractor().text(font, wrap(text), rightX - w, y, color, false);
    }

    public static int width(String text) {
        if (text == null) return 0;
        Font font = Minecraft.getInstance().font;
        return font.width(wrap(text));
    }

    public static void invalidateCache() {
        CACHE.clear();
    }
}
