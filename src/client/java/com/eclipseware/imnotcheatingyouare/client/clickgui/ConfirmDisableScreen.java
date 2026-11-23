package com.eclipseware.imnotcheatingyouare.client.clickgui;

import com.eclipseware.imnotcheatingyouare.client.glassui.render.SatoshiFont;
import com.eclipseware.imnotcheatingyouare.client.glassui.render.SatoshiWeight;
import com.eclipseware.imnotcheatingyouare.client.glassui.render.TextGradient;
import com.eclipseware.imnotcheatingyouare.client.glassui.render.UiRenderPrimitives;
import com.eclipseware.imnotcheatingyouare.client.glassui.theme.UiTheme;
import com.eclipseware.imnotcheatingyouare.client.glassui.theme.UiThemeContext;
import com.eclipseware.imnotcheatingyouare.client.module.impl.KeybindList;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ConfirmDisableScreen extends Screen {
    private final KeybindList keybindListModule;

    public ConfirmDisableScreen(KeybindList keybindListModule) {
        super(Component.literal("Disable Keybind List Confirmation"));
        this.keybindListModule = keybindListModule;
    }

    @Override
    protected void init() {
        super.init();
        int boxY = this.height / 2 + 24;

        Button.Builder confirmBuilder = Button.builder(Component.literal("Confirm Disable"), btn -> {
            keybindListModule.setConfirmedDisable(true);
            if (keybindListModule.isToggled()) {
                keybindListModule.toggle();
            }
            if (this.minecraft != null) {
                this.minecraft.setScreenAndShow(null);
            }
        });
        confirmBuilder.bounds(this.width / 2 - 110, boxY, 104, 20);
        this.addRenderableWidget(confirmBuilder.build());

        Button.Builder cancelBuilder = Button.builder(Component.literal("Cancel"), btn -> {
            if (this.minecraft != null) {
                this.minecraft.setScreenAndShow(null);
            }
        });
        cancelBuilder.bounds(this.width / 2 + 6, boxY, 104, 20);
        this.addRenderableWidget(cancelBuilder.build());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        UiTheme theme = UiThemeContext.current();
        if (theme.blurEnabled()) {
            extractBlurredBackground(guiGraphics);
        }
        guiGraphics.fill(0, 0, this.width, this.height, UiRenderPrimitives.argb(145, 20, 8, 10));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        UiTheme theme = UiThemeContext.current();
        float cx = this.width * 0.5f;
        float cy = this.height * 0.5f;
        float cardW = 300f;
        float cardH = 132f;

        UiRenderPrimitives.drawRoundedShadow(guiGraphics, cx - cardW * 0.5f, cy - 68f, cardW, cardH, 14f, 12f, UiRenderPrimitives.argb(105, 0, 0, 0));
        UiRenderPrimitives.drawGlassCard(
            guiGraphics,
            cx - cardW * 0.5f,
            cy - 70f,
            cardW,
            cardH,
            14f,
            theme.windowTopColor(232),
            theme.windowBottomColor(242),
            UiRenderPrimitives.argb(95, 255, 100, 112),
            UiRenderPrimitives.argb(36, 255, 100, 112)
        );

        SatoshiFont.drawCentered(guiGraphics, "Disable Keybind List?", cx, cy - 52f, 7.8f, TextGradient.accent(1f, 0xFFFF6470, theme), SatoshiWeight.SEMIBOLD);
        SatoshiFont.drawCentered(guiGraphics, "Keybind List is about to be hidden from your HUD.", cx, cy - 32f, 5.5f, TextGradient.silver(0.92f, theme), SatoshiWeight.MEDIUM);
        SatoshiFont.drawCentered(guiGraphics, "Loading unfamiliar configs without visible binds can trigger modules unexpectedly.", cx, cy - 17f, 4.9f, TextGradient.mist(0.82f, theme), SatoshiWeight.MEDIUM);

        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
    }
}
