package com.eclipseware.imnotcheatingyouare.client.clickgui.components;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class Item {
    protected final Minecraft mc = Minecraft.getInstance();
    public static GuiGraphicsExtractor context;
    private final String name;
    protected float x;
    protected float y;
    protected int width;
    protected int height;
    private boolean hidden;

    public Item(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public void setLocation(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public void drawScreen(GuiGraphicsExtractor context, int mouseX, int mouseY, float partialTicks) {
    }

    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
    }

    public void mouseReleased(int mouseX, int mouseY, int releaseButton) {
    }

    public void update() {
    }

    public void onKeyTyped(String typedChar, int keyCode) {
    }

    public void onKeyPressed(int key) {
    }

    public float getX() {
        return this.x;
    }

    public float getY() {
        return this.y;
    }

    public int getWidth() {
        return this.width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return this.height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public boolean isHidden() {
        return this.hidden;
    }

    public boolean setHidden(boolean hidden) {
        this.hidden = hidden;
        return this.hidden;
    }

    protected void drawString(String text, double x, double y, int color) {
        context.text(mc.font, text, (int) x, (int) y, color, false);
    }

    public boolean isHovering(int mouseX, int mouseY) {
        return mouseX >= this.getX() && mouseX <= this.getX() + this.getWidth() && mouseY >= this.getY() && mouseY <= this.getY() + this.getHeight();
    }
}
