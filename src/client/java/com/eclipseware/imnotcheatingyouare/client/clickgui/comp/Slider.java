package com.eclipseware.imnotcheatingyouare.client.clickgui.comp;

import com.eclipseware.imnotcheatingyouare.client.clickgui.Clickgui;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import java.awt.Color;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class Slider extends Comp {
    private boolean dragging = false;
    private double renderWidth;
    private final double sliderLength = 100;

    public Slider(double x, double y, Clickgui parent, Module module, Setting setting) {
        this.x = x; this.y = y; this.parent = parent; this.module = module; this.setting = setting;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        double min = setting.getMin();
        double max = setting.getMax();
        renderWidth = sliderLength * (setting.getValDouble() - min) / (max - min);

        if (dragging) {
            double diff = Math.min(sliderLength, Math.max(0, mouseX - (parent.posX + x)));
            if (diff == 0) setting.setValDouble(setting.getMin());
            else setting.setValDouble(roundToPlace(((diff / sliderLength) * (max - min) + min), 1));
        }

        guiGraphics.drawString(Minecraft.getInstance().font, setting.getName() + ": " + setting.getValDouble(), (int)(parent.posX + x), (int)(parent.posY + y), -1, false);
        guiGraphics.fill((int)(parent.posX + x), (int)(parent.posY + y + 10), (int)(parent.posX + x + sliderLength), (int)(parent.posY + y + 15), new Color(30, 30, 30).darker().getRGB());
        guiGraphics.fill((int)(parent.posX + x), (int)(parent.posY + y + 10), (int)(parent.posX + x + renderWidth), (int)(parent.posY + y + 15), new Color(230, 10, 230).getRGB());
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (isInside(mouseX, mouseY, parent.posX + x, parent.posY + y + 10, parent.posX + x + sliderLength, parent.posY + y + 20) && mouseButton == 0) {
            dragging = true;
        }
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int state) {
        dragging = false;
    }

    private double roundToPlace(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();
        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}