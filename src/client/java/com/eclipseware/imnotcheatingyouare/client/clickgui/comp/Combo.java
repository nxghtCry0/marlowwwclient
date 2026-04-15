package com.eclipseware.imnotcheatingyouare.client.clickgui.comp;

import com.eclipseware.imnotcheatingyouare.client.clickgui.Clickgui;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.utils.AnimationUtil;
import com.eclipseware.imnotcheatingyouare.client.utils.FontUtils;
import net.minecraft.client.gui.GuiGraphics;
import java.awt.Color;

public class Combo extends Comp {
    public Combo(double x, double y, Clickgui parent, Module module, Setting setting) {
        this.x = x; this.y = y; this.parent = parent; this.module = module; this.setting = setting;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        Module theme = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("Theme");
        int r = 155, g = 60, b = 255;
        if (theme != null) {
            r = (int) ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(theme, "Accent R").getValDouble();
            g = (int) ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(theme, "Accent G").getValDouble();
            b = (int) ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(theme, "Accent B").getValDouble();
        }

        String label = setting.getName() + ": ";
        String value = setting.getValString();
        
        int totalWidth = FontUtils.width(label + value) + 12;
        boolean hovered = isInside(mouseX, mouseY, parent.posX + x, parent.posY + y - 2, parent.posX + x + totalWidth, parent.posY + y + 18);
        
        AnimationUtil.drawRoundedRect(guiGraphics, (int)(parent.posX + x), (int)(parent.posY + y - 2), totalWidth, 20, 4, hovered ? new Color(45, 45, 50).getRGB() : new Color(30, 30, 35).getRGB());

        FontUtils.drawString(guiGraphics, label, (int)(parent.posX + x + 6), (int)(parent.posY + y + 4), new Color(200, 200, 200).getRGB(), false);
        FontUtils.drawString(guiGraphics, value, (int)(parent.posX + x + 6) + FontUtils.width(label), (int)(parent.posY + y + 4), new Color(r, g, b).getRGB(), false);
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int mouseButton) {
        int totalWidth = FontUtils.width(setting.getName() + ": " + setting.getValString()) + 12;
        if (isInside(mouseX, mouseY, parent.posX + x, parent.posY + y - 2, parent.posX + x + totalWidth, parent.posY + y + 18) && mouseButton == 0) {
            int index = setting.getOptions().indexOf(setting.getValString());
            if (index + 1 >= setting.getOptions().size()) {
                setting.setValString(setting.getOptions().get(0));
            } else {
                setting.setValString(setting.getOptions().get(index + 1));
            }
            Clickgui.playSound();
        }
    }
}