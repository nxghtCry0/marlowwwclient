package com.eclipseware.imnotcheatingyouare.client.ui;


import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;

public abstract class CompatAbstractWidget extends AbstractWidget {
    protected CompatAbstractWidget(int x, int y, int width, int height, Component message) {
        super(x, y, width, height, message);
    }

    protected final void extractWidgetRenderState(DrawContext extractor, int mouseX, int mouseY, float partialTick) {
        renderWidget(new DrawContext(extractor), mouseX, mouseY, partialTick);
    }
    
    protected abstract void renderWidget(DrawContext DrawContext, int mouseX, int mouseY, float partialTick);
}
