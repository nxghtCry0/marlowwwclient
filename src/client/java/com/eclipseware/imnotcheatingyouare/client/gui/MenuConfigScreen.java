package com.eclipseware.imnotcheatingyouare.client.gui;

import com.eclipseware.imnotcheatingyouare.client.clickgui.ImGuiClickGui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class MenuConfigScreen extends Screen {
    private final Screen parent;

    public MenuConfigScreen(Screen parent) {
        super(Component.literal("Marlowww Config"));
        this.parent = parent;
    }

    public Screen parent() {
        return parent;
    }

    @Override
    protected void init() {
        ImGuiClickGui.openMenuMode();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (PastelShaderBackground.render()) {
            graphics.blit(PastelShaderBackground.TEXTURE_ID, 0, 0, width, height, 0.0f, 1.0f, 1.0f, 0.0f);
        } else {
            graphics.fillGradient(0, 0, width, height, 0xFFC6A8F8, 0xFF8E86E8);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        extractBackground(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        ImGuiClickGui.markClosed();
        minecraft.setScreenAndShow(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
