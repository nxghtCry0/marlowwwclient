package com.eclipseware.imnotcheatingyouare.client.clickgui.comp;

import com.eclipseware.imnotcheatingyouare.client.clickgui.Clickgui;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.utils.AnimationUtil;
import com.eclipseware.imnotcheatingyouare.client.utils.FontUtils;
import net.minecraft.client.gui.GuiGraphics;
import java.awt.Color;

public class Slider extends Comp {
    private boolean dragging = false;
    private int renderWidth = 160;

    public Slider(double x, double y, Clickgui parent, Module module, Setting setting) {
        this.x = x; this.y = y; this.parent = parent; this.module = module; this.setting = setting;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        double diff = Math.min(renderWidth, Math.max(0, mouseX - (parent.posX + x)));
        if (dragging) {
            if (diff == 0) setting.setValDouble(setting.getMin());
            else {
                double newValue = roundToPlace(((diff / renderWidth) * (setting.getMax() - setting.getMin()) + setting.getMin()), 2);
                setting.setValDouble(newValue);
            }
        }
        double renderWidth2 = (renderWidth) * (setting.getValDouble() - setting.getMin()) / (setting.getMax() - setting.getMin());

        FontUtils.drawString(guiGraphics, setting.getName() + ": " + setting.getValDouble(), (int)(parent.posX + x), (int)(parent.posY + y), new Color(220, 220, 220).getRGB(), false);
        
        Module theme = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("Theme");
        int r = 155, g = 60, b = 255;
        if (theme != null) {
            r = (int) ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(theme, "Accent R").getValDouble();
            g = (int) ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(theme, "Accent G").getValDouble();
            b = (int) ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(theme, "Accent B").getValDouble();
        }

        boolean hovered = isInside(mouseX, mouseY, parent.posX + x, parent.posY + y + 16, parent.posX + x + renderWidth, parent.posY + y + 24);
        int trackColor = hovered ? new Color(60, 60, 65).getRGB() : new Color(45, 45, 50).getRGB();

        AnimationUtil.drawRoundedRect(guiGraphics, (int)(parent.posX + x), (int)(parent.posY + y + 18), renderWidth, 6, 3, trackColor);
        AnimationUtil.drawRoundedRect(guiGraphics, (int)(parent.posX + x), (int)(parent.posY + y + 18), (int)renderWidth2, 6, 3, new Color(r, g, b).getRGB());
        AnimationUtil.drawRoundedRect(guiGraphics, (int)(parent.posX + x + renderWidth2 - 5), (int)(parent.posY + y + 15), 10, 12, 4, -1);
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (isInside(mouseX, mouseY, parent.posX + x, parent.posY + y + 14, parent.posX + x + renderWidth, parent.posY + y + 26) && mouseButton == 0) {
            dragging = true;
        }
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int mouseButton) {
        dragging = false;
    }

    private double roundToPlace(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();
        long factor = (long) Math.pow(10, places);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }
}