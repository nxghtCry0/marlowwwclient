package com.eclipseware.imnotcheatingyouare.client.clickgui;

import com.eclipseware.imnotcheatingyouare.client.module.Module;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.awt.Color;
import java.util.List;

public class DetectionWarningScreen extends Screen {
    private final List<Module> detectedModules;

    public DetectionWarningScreen(List<Module> detectedModules) {
        super(Component.literal("Detection Warning"));
        this.detectedModules = detectedModules;
    }

    @Override
    protected void init() {
        super.init();
        
        int boxY = this.height / 2 + 30;

        this.addRenderableWidget(Button.builder(Component.literal("Yes, disable them"), btn -> {
            for (Module m : detectedModules) {
                if (m.isToggled()) {
                    m.toggle();
                }
            }
            if (this.minecraft != null) {
                this.minecraft.setScreen(null);
            }
        }).bounds(this.width / 2 - 110, boxY, 100, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("No, ignore"), btn -> {
            if (this.minecraft != null) {
                this.minecraft.setScreen(null);
            }
        }).bounds(this.width / 2 + 10, boxY, 100, 20).build());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.fill(0, 0, this.width, this.height, new Color(40, 5, 5, 150).getRGB());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        
        int startY = this.height / 2 - 60;
        
        guiGraphics.drawCenteredString(this.font, "\u00a7c\u00a7lWARNING!", this.width / 2, startY, -1);
        
        StringBuilder mods = new StringBuilder();
        for (int i = 0; i < detectedModules.size(); i++) {
            mods.append(detectedModules.get(i).getName());
            if (i < detectedModules.size() - 1) mods.append(", ");
        }

        guiGraphics.drawCenteredString(this.font, "\u00a7e" + mods.toString() + " \u00a7fare enabled.", this.width / 2, startY + 20, -1);
        guiGraphics.drawCenteredString(this.font, "This is detected on the server you are playing on!", this.width / 2, startY + 35, -1);
        guiGraphics.drawCenteredString(this.font, "Would you like to disable them?", this.width / 2, startY + 50, -1);
    }
}
