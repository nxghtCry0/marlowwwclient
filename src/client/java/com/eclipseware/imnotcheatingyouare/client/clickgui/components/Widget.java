package com.eclipseware.imnotcheatingyouare.client.clickgui.components;

import com.eclipseware.imnotcheatingyouare.client.clickgui.Clickgui;
import com.eclipseware.imnotcheatingyouare.client.utils.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.DrawContext;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class Widget {
    protected DrawContext context;
    protected final Minecraft mc = Minecraft.getInstance();
    private final List<Item> items = new ArrayList<>();
    
    private final String name;
    public boolean drag;
    private int x;
    private int y;
    private int x2;
    private int y2;
    private int width;
    private int height;
    private boolean open;

    public Widget(String name, int x, int y, boolean open) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.width = 110;
        this.height = 18;
        this.open = open;
    }

    private void drag(int mouseX, int mouseY) {
        if (!this.drag) {
            return;
        }
        this.x = this.x2 + mouseX;
        this.y = this.y2 + mouseY;
        
        if (mc.getWindow() != null) {
            if (this.x < 0) this.x = 0;
            if (this.y < 0) this.y = 0;
            if (this.x + this.width > mc.getWindow().getGuiScaledWidth()) this.x = mc.getWindow().getGuiScaledWidth() - this.width;
            if (this.y + this.height > mc.getWindow().getGuiScaledHeight()) this.y = mc.getWindow().getGuiScaledHeight() - this.height;
        }
    }

    public void drawScreen(DrawContext context, int mouseX, int mouseY, float partialTicks) {
        this.context = context;
        this.drag(mouseX, mouseY);
        
        int color = RenderUtils.getThemeAccentColor().getRGB();
        java.awt.Color secondary = RenderUtils.getThemeSecondaryColor();
        int darkBg = new java.awt.Color(secondary.getRed(), secondary.getGreen(), secondary.getBlue(), 238).getRGB();
        
        context.fill(this.x, this.y, this.x + this.width, this.y + this.height, darkBg);
        context.text(mc.font, this.name, this.x + 4, this.y + 5, color, false);
        context.fill(this.x, this.y + this.height - 1, this.x + this.width, this.y + this.height, color);

        if (this.open) {
            float totalItemHeight = this.getTotalItemHeight();
            int itemsBg = new java.awt.Color(secondary.getRed(), secondary.getGreen(), secondary.getBlue(), 153).getRGB();
            context.fill(this.x, this.y + this.height, this.x + this.width, (int)(this.y + this.height + totalItemHeight), itemsBg);
            
            float currentY = this.y + this.height;
            for (Item item : this.getItems()) {
                if (item.isHidden()) continue;
                item.setLocation((float) this.x, currentY);
                item.setWidth(this.width);
                item.drawScreen(context, mouseX, mouseY, partialTicks);
                currentY += (float) item.getHeight() + 1f;
            }
            
            context.fill(this.x, (int)currentY, this.x + this.width, (int)currentY + 2, color);
        }
    }

    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton == 0 && this.isHovering(mouseX, mouseY)) {
            this.x2 = this.x - mouseX;
            this.y2 = this.y - mouseY;
            Clickgui.getInstance().getComponents().forEach(component -> {
                if (component.drag) {
                    component.drag = false;
                }
            });
            this.drag = true;
            return;
        }
        if (mouseButton == 1 && this.isHovering(mouseX, mouseY)) {
            this.open = !this.open;
            Clickgui.playSound();
            return;
        }
        if (!this.open) {
            return;
        }
        this.getItems().forEach(item -> item.mouseClicked(mouseX, mouseY, mouseButton));
    }

    public void mouseReleased(int mouseX, int mouseY, int releaseButton) {
        if (releaseButton == 0) {
            this.drag = false;
        }
        if (!this.open) {
            return;
        }
        this.getItems().forEach(item -> item.mouseReleased(mouseX, mouseY, releaseButton));
    }

    public void onKeyTyped(String typedChar, int keyCode) {
        if (!this.open) return;
        this.getItems().forEach(item -> item.onKeyTyped(typedChar, keyCode));
    }

    public void onKeyPressed(int key) {
        if (!open) return;
        this.getItems().forEach(item -> item.onKeyPressed(key));
    }

    public void addButton(Button button) {
        this.items.add(button);
    }

    public int getX() { return this.x; }
    public void setX(int x) { this.x = x; }
    public int getY() { return this.y; }
    public void setY(int y) { this.y = y; }
    public int getWidth() { return this.width; }
    public void setWidth(int width) { this.width = width; }
    public int getHeight() { return this.height; }
    public void setHeight(int height) { this.height = height; }
    public boolean isOpen() { return this.open; }
    public final List<Item> getItems() { return this.items; }

    public boolean isHovering(int mouseX, int mouseY) {
        return mouseX >= this.getX() && mouseX <= this.getX() + this.getWidth() && mouseY >= this.getY() && mouseY <= this.getY() + this.getHeight();
    }

    private float getTotalItemHeight() {
        float height = 0.0f;
        for (Item item : this.getItems()) {
            height += (float) item.getHeight() + 1;
        }
        return height;
    }
}
