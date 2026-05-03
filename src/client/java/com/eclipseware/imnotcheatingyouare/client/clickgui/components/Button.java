package com.eclipseware.imnotcheatingyouare.client.clickgui.components;

import net.minecraft.client.gui.DrawContext;

public class Button extends Item {
    private boolean state;

    public Button(String name) {
        super(name);
        this.height = 14;
    }

    @Override
    public void drawScreen(DrawContext context, int mouseX, int mouseY, float partialTicks) {
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton == 0 && this.isHovering(mouseX, mouseY)) {
            this.onMouseClick();
        }
    }

    public void onMouseClick() {
        this.state = !this.state;
        this.toggle();
    }

    public void toggle() {
    }

    public boolean getState() {
        return this.state;
    }
}
