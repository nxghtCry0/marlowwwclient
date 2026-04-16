package com.eclipseware.imnotcheatingyouare.client.clickgui.comp;

import com.eclipseware.imnotcheatingyouare.client.clickgui.Clickgui;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.utils.AnimationUtil;
import com.eclipseware.imnotcheatingyouare.client.utils.FontUtils;
import net.minecraft.client.gui.GuiGraphics;
import java.awt.Color;

public class CheckBox extends Comp {
    private float slideAnim = 0f;

    public CheckBox(double x, double y, Clickgui parent, Module module, Setting setting) {
        this.x = x; this.y = y; this.parent = parent; this.module = module; this.setting = setting;
        this.slideAnim = setting.getValBoolean() ? 1f : 0f;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        float target = setting.getValBoolean() ? 1f : 0f;
        slideAnim += (target - slideAnim) * 0.2f;

        Module theme = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("Theme");
        int r = 155, g = 60, b = 255;
        if (theme != null) {
            r = (int) ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(theme, "Accent R").getValDouble();
            g = (int) ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(theme, "Accent G").getValDouble();
            b = (int) ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(theme, "Accent B").getValDouble();
        }

        int bgOff = new Color(45, 45, 50).getRGB();
        int bgOn = new Color(r, g, b).getRGB(); 
        int currentBg = interpolateColor(new Color(bgOff), new Color(bgOn), slideAnim).getRGB();

        boolean hovered = isInside(mouseX, mouseY, parent.posX + x, parent.posY + y, parent.posX + x + 35 + FontUtils.width(setting.getName()), parent.posY + y + 16);
        if (hovered && slideAnim < 0.5f) currentBg = new Color(60, 60, 65).getRGB();

        AnimationUtil.drawRoundedRect(guiGraphics, (int)(parent.posX + x), (int)(parent.posY + y), 32, 16, 8, currentBg);
        
        int knobX = (int)(parent.posX + x + 2 + (slideAnim * 16));
        AnimationUtil.drawRoundedRect(guiGraphics, knobX, (int)(parent.posY + y + 2), 12, 12, 6, -1);

        FontUtils.drawString(guiGraphics, setting.getName(), (int)(parent.posX + x + 42), (int)(parent.posY + y + 4), new Color(220, 220, 220).getRGB(), false);
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (isInside(mouseX, mouseY, parent.posX + x, parent.posY + y, parent.posX + x + 35 + FontUtils.width(setting.getName()), parent.posY + y + 16) && mouseButton == 0) {
            setting.setValBoolean(!setting.getValBoolean());
            Clickgui.playSound();
        }
    }

    private Color interpolateColor(Color color1, Color color2, float fraction) {
        int red = (int) (color1.getRed() + (color2.getRed() - color1.getRed()) * fraction);
        int green = (int) (color1.getGreen() + (color2.getGreen() - color1.getGreen()) * fraction);
        int blue = (int) (color1.getBlue() + (color2.getBlue() - color1.getBlue()) * fraction);
        int alpha = (int) (color1.getAlpha() + (color2.getAlpha() - color1.getAlpha()) * fraction);
        return new Color(red, green, blue, alpha);
    }
}