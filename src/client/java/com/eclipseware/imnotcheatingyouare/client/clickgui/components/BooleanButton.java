package com.eclipseware.imnotcheatingyouare.client.clickgui.components;

import com.eclipseware.imnotcheatingyouare.client.clickgui.Clickgui;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.utils.RenderUtils;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class BooleanButton extends Button {
    private final Setting setting;

    public BooleanButton(Setting setting) {
        super(setting.getName());
        this.setting = setting;
        this.width = 15;
    }

    @Override
    public void drawScreen(GuiGraphicsExtractor context, int mouseX, int mouseY, float partialTicks) {
        java.awt.Color theme = RenderUtils.getThemeAccentColor();
        int accent = (theme.getRGB() & 0x00FFFFFF) | (120 << 24);
        int dark = 0x22000000;
        int hoverDark = 0x44222222;

        boolean active = getState();
        int fill = active ? accent : (this.isHovering(mouseX, mouseY) ? hoverDark : dark);

        context.fill((int)this.x, (int)this.y, (int)(this.x + this.width), (int)(this.y + this.height), fill);
        drawString(this.getName(), this.x + 2.3f, this.y - 1.7f + 6, active ? -1 : 0xFFAAAAAA);
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
        this.setting.setValBoolean(!this.setting.getValBoolean());
    }

    @Override
    public boolean getState() {
        return this.setting.getValBoolean();
    }
}
