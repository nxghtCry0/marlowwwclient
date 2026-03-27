package com.eclipseware.imnotcheatingyouare.client.clickgui.comp;

import com.eclipseware.imnotcheatingyouare.client.clickgui.Clickgui;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import net.minecraft.client.gui.GuiGraphics;

public class Comp {
    public double x, y;
    public Clickgui parent;
    public Module module;
    public Setting setting;

    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY) {}
    public void mouseClicked(double mouseX, double mouseY, int mouseButton) {}
    public void mouseReleased(double mouseX, double mouseY, int state) {}
    public void keyPressed(int keyCode, int scanCode, int modifiers) {}
    
    public boolean isInside(double mouseX, double mouseY, double x, double y, double x2, double y2) {
        return (mouseX >= x && mouseX <= x2) && (mouseY >= y && mouseY <= y2);
    }
}