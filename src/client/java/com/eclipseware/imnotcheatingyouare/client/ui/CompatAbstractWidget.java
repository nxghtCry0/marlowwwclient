package com.eclipseware.imnotcheatingyouare.client.ui;


import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;

public abstract class CompatAbstractWidget extends AbstractWidget {
    protected CompatAbstractWidget(int x, int y, int width, int height, Component message) {
        super(x, y, width, height, message);
    }

    protected final void extractWidgetRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        renderWidget(new GuiGraphics(extractor), mouseX, mouseY, partialTick);
    }
    
    protected abstract void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick);
}
