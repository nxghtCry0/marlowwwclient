package com.eclipseware.imnotcheatingyouare.client.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class GuiGraphics {
    private final GuiGraphicsExtractor extractor;

    public GuiGraphics(GuiGraphicsExtractor extractor) {
        this.extractor = extractor;
    }

    public GuiGraphicsExtractor extractor() {
        return this.extractor;
    }

    public void fill(int minX, int minY, int maxX, int maxY, int color) {
        this.extractor.fill(minX, minY, maxX, maxY, color);
    }

    public void renderOutline(int x, int y, int w, int h, int color) {
        this.extractor.fill(x, y, x + w, y + 1, color);
        this.extractor.fill(x, y + h - 1, x + w, y + h, color);
        this.extractor.fill(x, y, x + 1, y + h, color);
        this.extractor.fill(x + w - 1, y, x + w, y + h, color);
    }

    public void drawCenteredString(Font font, Component text, int x, int y, int color) {
        this.extractor.centeredText(font, text.getString(), x, y, color);
    }

    public void drawString(Font font, Component text, int x, int y, int color, boolean dropShadow) {
        this.extractor.text(font, text.getString(), x, y, color, dropShadow);
    }
    
    public void drawWordWrap(Font font, Component text, int x, int y, int width, int color) {
        this.extractor.text(font, text.getString(), x, y, color, false);
    }

    public void blit(Identifier id, int x, int y, int u, int v, int width, int height) {
    }

    public void blitSprite(Identifier id, int x, int y, int width, int height) {
    }
}
