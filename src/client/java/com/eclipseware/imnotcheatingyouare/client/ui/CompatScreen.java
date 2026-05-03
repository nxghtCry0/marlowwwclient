package com.eclipseware.imnotcheatingyouare.client.ui;


import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public abstract class CompatScreen extends Screen {
    protected CompatScreen(Component title) {
        super(title);
    }

    public void extractRenderState(DrawContext extractor, int mouseX, int mouseY, float partialTick) {
        render(new DrawContext(extractor), mouseX, mouseY, partialTick);
    }
    
    public void render(DrawContext DrawContext, int mouseX, int mouseY, float partialTick) {
        for (GuiEventListener child : children()) {
            if (child instanceof Renderable) {
                Renderable renderable = (Renderable)child;
                renderable.extractRenderState(DrawContext.extractor(), mouseX, mouseY, partialTick);
            }
        } 
    }
}
