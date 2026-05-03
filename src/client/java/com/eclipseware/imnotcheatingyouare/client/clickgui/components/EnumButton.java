package com.eclipseware.imnotcheatingyouare.client.clickgui.components;

import com.eclipseware.imnotcheatingyouare.client.clickgui.Clickgui;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;

public class EnumButton extends Button {
    public Setting setting;

    public EnumButton(Setting setting) {
        super(setting.getName());
        this.setting = setting;
        this.width = 15;
    }

    @Override
    public void drawScreen(DrawContext context, int mouseX, int mouseY, float partialTicks) {
        int dark = 0x22000000;
        int hoverDark = 0x44222222;
        int fill = this.isHovering(mouseX, mouseY) ? hoverDark : dark;

        context.fill((int)this.x, (int)this.y, (int)(this.x + this.width), (int)(this.y + this.height), fill);
        drawString(this.setting.getName() + " " + ChatFormatting.GRAY + this.setting.getValString(), this.x + 2.3f, this.y - 1.7f + 6, -1);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (this.isHovering(mouseX, mouseY)) {
            Clickgui.playSound();
        }
    }

    @Override
    public void toggle() {
        ArrayList<String> options = this.setting.getOptions();
        int index = options.indexOf(this.setting.getValString());
        if (index + 1 >= options.size()) {
            this.setting.setValString(options.get(0));
        } else {
            this.setting.setValString(options.get(index + 1));
        }
    }

    @Override
    public boolean getState() {
        return true;
    }
}
