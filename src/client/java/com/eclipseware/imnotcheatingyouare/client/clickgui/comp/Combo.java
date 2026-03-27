package com.eclipseware.imnotcheatingyouare.client.clickgui.comp;

import com.eclipseware.imnotcheatingyouare.client.clickgui.Clickgui;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import java.awt.Color;

public class Combo extends Comp {
    public Combo(double x, double y, Clickgui parent, Module module, Setting setting) {
        this.x = x; this.y = y; this.parent = parent; this.module = module; this.setting = setting;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.fill((int)(parent.posX + x), (int)(parent.posY + y), (int)(parent.posX + x + 80), (int)(parent.posY + y + 12), new Color(30, 30, 30).getRGB());
        guiGraphics.drawString(Minecraft.getInstance().font, setting.getName() + ": " + setting.getValString(), (int)(parent.posX + x + 2), (int)(parent.posY + y + 2), new Color(200, 200, 200).getRGB(), false);
    }

    @Override
public void mouseClicked(double mouseX, double mouseY, int mouseButton) {
if (isInside(mouseX, mouseY, parent.posX + x, parent.posY + y, parent.posX + x + 80, parent.posY + y + 12) && mouseButton == 0) {
int max = setting.getOptions().size();
int currentIndex = setting.getOptions().indexOf(setting.getValString());

    if (currentIndex + 1 >= max) {
        setting.setValString(setting.getOptions().get(0));
    } else {
        setting.setValString(setting.getOptions().get(currentIndex + 1));
    }
    
    Clickgui.playSound();

    // Immediately refresh the GUI to update conditional settings!
    parent.loadComponents(module);
}

}
}