package com.eclipseware.imnotcheatingyouare.client.gui;

import com.eclipseware.imnotcheatingyouare.client.clickgui.ImGuiClickGui;
import com.eclipseware.imnotcheatingyouare.client.module.impl.Menu;
import java.awt.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class MenuConfigScreen extends Screen {
    private final Screen parent;

    public MenuConfigScreen(Screen parent) {
        super(Component.literal("Config"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        ImGuiClickGui.openMenuMode();
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        if (!Menu.isCustomMenuEnabled() || "Vanilla Panorama".equalsIgnoreCase(Menu.getBackgroundMode())) {
            super.renderPanorama(g, delta);
        } else if (Menu.shouldReplacePanorama() && PastelShaderBackground.render()) {
            g.blit(PastelShaderBackground.TEXTURE_ID, 0, 0, this.width, this.height, 0.0f, 1.0f, 1.0f, 0.0f);
        } else {
            Color top = Menu.getPrimaryColor();
            Color bot = Menu.getSecondaryColor();
            int topArgb = (0xFF << 24) | ((top.getRed() & 0xFF) << 16) | ((top.getGreen() & 0xFF) << 8) | (top.getBlue() & 0xFF);
            int botArgb = (0xFF << 24) | ((bot.getRed() & 0xFF) << 16) | ((bot.getGreen() & 0xFF) << 8) | (bot.getBlue() & 0xFF);
            g.fillGradient(0, 0, this.width, this.height, topArgb, botArgb);
        }
        if (!ImGuiClickGui.isOpen()) {
            this.onClose();
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.input();
        if (ImGuiClickGui.onScreenKeyPressed(key, this::onClose)) {
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        if (ImGuiClickGui.onScreenMouseClicked(event.button())) {
            return true;
        }
        return super.mouseClicked(event, doubled);
    }

    @Override
    public void onClose() {
        ImGuiClickGui.markClosed();
        Minecraft client = Minecraft.getInstance();
        if (this.parent instanceof TitleScreen) {
            Screen target = Menu.isCustomMenuEnabled() ? new MarlowTitleScreen() : new TitleScreen();
            client.setScreen(target);
        } else {
            client.setScreen(this.parent);
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
