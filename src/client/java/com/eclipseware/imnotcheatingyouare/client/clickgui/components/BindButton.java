package com.eclipseware.imnotcheatingyouare.client.clickgui.components;

import com.eclipseware.imnotcheatingyouare.client.clickgui.Clickgui;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import com.eclipseware.imnotcheatingyouare.client.utils.InputUtil;
import com.mojang.blaze3d.platform.InputConstants;

import java.util.HashMap;

public class BindButton extends Button {
    private final Module module;
    public boolean isListening;

    public BindButton(Module module) {
        super("Bind");
        this.module = module;
        this.width = 15;
    }

    private String getKeyName(int key) {
        return InputUtil.getName(key);
    }

    private int lastKeyBind = Integer.MIN_VALUE;
    private String cachedDisplayString = null;

    private String getDisplayString() {
        int currentBind = this.module.getKeyBind();
        if (cachedDisplayString == null || currentBind != lastKeyBind) {
            lastKeyBind = currentBind;
            cachedDisplayString = "Bind " + net.minecraft.ChatFormatting.GRAY + getKeyName(currentBind);
        }
        return cachedDisplayString;
    }

    @Override
    public void drawScreen(GuiGraphicsExtractor context, int mouseX, int mouseY, float partialTicks) {
        int dark = 0x22000000;
        int hoverDark = 0x44222222;
        int fill = this.isHovering(mouseX, mouseY) ? hoverDark : dark;

        context.fill((int)this.x, (int)this.y, (int)(this.x + this.width), (int)(this.y + this.height), fill);
        
        if (this.isListening) {
            drawString("Listening...", this.x + 2.3f, this.y - 1.7f + 6, -1);
        } else {
            drawString(getDisplayString(), this.x + 2.3f, this.y - 1.7f + 6, -1);
        }
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        boolean wasListening = this.isListening;
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (wasListening) {
            if (mouseButton != 0 && mouseButton != 1) {
                this.module.setKeyBind(InputUtil.fromClickOrdinal(mouseButton));
            }
            this.isListening = false;
        } else if (this.isHovering(mouseX, mouseY)) {
            Clickgui.playSound();
        }
    }

    @Override
    public void onKeyPressed(int key) {
        if (this.isListening) {
            int targetKey = key;
            if (key == InputConstants.KEY_DELETE || key == InputConstants.KEY_BACKSPACE || key == InputConstants.KEY_ESCAPE) {
                targetKey = 0;
            }
            this.module.setKeyBind(targetKey);
            this.isListening = false;
        }
    }

    @Override
    public void toggle() {
        this.isListening = !this.isListening;
    }

    @Override
    public boolean getState() {
        return !this.isListening;
    }
}
