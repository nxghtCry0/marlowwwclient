package com.eclipseware.imnotcheatingyouare.client.clickgui.components;

import com.eclipseware.imnotcheatingyouare.client.clickgui.Clickgui;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.utils.RenderUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;

public class Slider extends Button {
    private final Setting setting;
    private boolean isDragging;

    public Slider(Setting setting) {
        super(setting.getName());
        this.setting = setting;
        this.width = 15;
    }

    @Override
    public void drawScreen(GuiGraphics context, int mouseX, int mouseY, float partialTicks) {
        if (this.isDragging) {
            this.dragSetting(mouseX, mouseY);
        }
        
        int dark = 0x22000000;
        int hoverDark = 0x44222222;
        int fill = this.isHovering(mouseX, mouseY) ? hoverDark : dark;

        context.fill((int)this.x, (int)this.y, (int)(this.x + this.width), (int)(this.y + this.height), fill);
        
        float length = this.x + ((float) this.width + 7.4f) * this.partialMultiplier();
        if (length > this.x) {
            java.awt.Color theme = RenderUtils.getThemeAccentColor();
            int accent = new java.awt.Color(theme.getRed(), theme.getGreen(), theme.getBlue(), 120).getRGB();
            context.fill((int)this.x, (int)this.y, (int)length, (int)(this.y + this.height), accent);
        }

        String val = String.valueOf(this.setting.getValDouble());
        if (this.setting.onlyInt()) {
            val = String.valueOf((int)this.setting.getValDouble());
        }
        drawString(this.getName() + " " + ChatFormatting.GRAY + val, this.x + 2.3f, this.y - 1.7f + 6, -1);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (this.isHovering(mouseX, mouseY) && mouseButton == 0) {
            this.isDragging = true;
            this.setSettingFromX(mouseX);
        }
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int releaseButton) {
        if (releaseButton == 0) {
            this.isDragging = false;
        }
    }

    @Override
    public boolean isHovering(int mouseX, int mouseY) {
        for (Widget widget : Clickgui.getInstance().getComponents()) {
            if (!widget.drag) continue;
            return false;
        }
        return (float) mouseX >= this.getX() && (float) mouseX <= this.getX() + (float) this.getWidth() + 8.0f && (float) mouseY >= this.getY() && (float) mouseY < this.getY() + (float) this.height;
    }

    private void dragSetting(int mouseX, int mouseY) {
        this.setSettingFromX(mouseX);
    }


    private void setSettingFromX(int mouseX) {
        float percent = ((float) mouseX - this.x) / ((float) this.width + 7.4f);
        percent = Math.max(0.0f, Math.min(1.0f, percent));
        double range = this.setting.getMax() - this.setting.getMin();
        double result = this.setting.getMin() + (range * percent);
        if (this.setting.onlyInt()) {
            this.setting.setValDouble(Math.round(result));
        } else {
            this.setting.setValDouble(Math.round(10.0 * result) / 10.0);
        }
    }

    private float partialMultiplier() {
        float mult = (float) ((this.setting.getValDouble() - this.setting.getMin()) / (this.setting.getMax() - this.setting.getMin()));
        return Math.max(0.0f, Math.min(1.0f, mult));
    }
}
