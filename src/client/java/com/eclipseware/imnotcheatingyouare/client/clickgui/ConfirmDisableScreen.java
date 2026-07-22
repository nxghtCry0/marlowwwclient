package com.eclipseware.imnotcheatingyouare.client.clickgui;

import com.eclipseware.imnotcheatingyouare.client.module.impl.KeybindList;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.awt.Color;

public class ConfirmDisableScreen extends Screen {
    private final KeybindList keybindListModule;

    public ConfirmDisableScreen(KeybindList keybindListModule) {
        super(Component.literal("Disable Keybind List Confirmation"));
        this.keybindListModule = keybindListModule;
    }

    @Override
    protected void init() {
        super.init();
        
        int boxY = this.height / 2 + 30;

        this.addRenderableWidget(Button.builder(Component.literal("Confirm Disable"), btn -> {
            keybindListModule.setConfirmedDisable(true);
            if (keybindListModule.isToggled()) {
                keybindListModule.toggle();
            }
            if (this.minecraft != null) {
                this.minecraft.setScreenAndShow(null);
            }
        }).bounds(this.width / 2 - 110, boxY, 100, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Cancel"), btn -> {
            if (this.minecraft != null) {
                this.minecraft.setScreenAndShow(null);
            }
        }).bounds(this.width / 2 + 10, boxY, 100, 20).build());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.fill(0, 0, this.width, this.height, new Color(40, 5, 5, 150).getRGB());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
        
        int startY = this.height / 2 - 60;
        
        guiGraphics.centeredText(this.font, "\u00a7c\u00a7lWARNING!", this.width / 2, startY, -1);
        guiGraphics.centeredText(this.font, "\u00a7eKeybind List \u00a7fis being disabled.", this.width / 2, startY + 20, -1);
        guiGraphics.centeredText(this.font, "If you load a config you aren't familiar with,", this.width / 2, startY + 35, -1);
        guiGraphics.centeredText(this.font, "this could be bad!", this.width / 2, startY + 50, -1);
    }
}
