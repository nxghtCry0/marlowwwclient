package com.eclipseware.imnotcheatingyouare.client.clickgui.comp;

import com.eclipseware.imnotcheatingyouare.client.clickgui.Clickgui;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import java.awt.Color;

public class CheckBox extends Comp {
    public CheckBox(double x, double y, Clickgui parent, Module module, Setting setting) {
        this.x = x; this.y = y; this.parent = parent; this.module = module; this.setting = setting;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.fill((int)(parent.posX + x), (int)(parent.posY + y), (int)(parent.posX + x + 10), (int)(parent.posY + y + 10), setting.getValBoolean() ? new Color(230, 10, 230).getRGB() : new Color(30, 30, 30).getRGB());
        guiGraphics.drawString(Minecraft.getInstance().font, setting.getName(), (int)(parent.posX + x + 15), (int)(parent.posY + y + 1), new Color(200, 200, 200).getRGB(), false);
    }

    @Override
public void mouseClicked(double mouseX, double mouseY, int mouseButton) {
if (isInside(mouseX, mouseY, parent.posX + x, parent.posY + y, parent.posX + x + 10, parent.posY + y + 10) && mouseButton == 0) {
setting.setValBoolean(!setting.getValBoolean());
Clickgui.playSound();
parent.loadComponents(module); // Immediately refresh layout for dynamic settings
}
}
}