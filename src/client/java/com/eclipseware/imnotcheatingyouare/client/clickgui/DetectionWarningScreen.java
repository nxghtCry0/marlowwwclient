package com.eclipseware.imnotcheatingyouare.client.clickgui;

import com.eclipseware.imnotcheatingyouare.client.glassui.render.SatoshiFont;
import com.eclipseware.imnotcheatingyouare.client.glassui.render.SatoshiWeight;
import com.eclipseware.imnotcheatingyouare.client.glassui.render.TextGradient;
import com.eclipseware.imnotcheatingyouare.client.glassui.render.UiRenderPrimitives;
import com.eclipseware.imnotcheatingyouare.client.glassui.theme.UiTheme;
import com.eclipseware.imnotcheatingyouare.client.glassui.theme.UiThemeContext;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class DetectionWarningScreen extends Screen {
    private final List<Module> detectedModules;

    public DetectionWarningScreen(List<Module> detectedModules) {
        super(Component.literal("Detection Warning"));
        this.detectedModules = detectedModules;
    }

    @Override
    protected void init() {
        super.init();
        int boxY = this.height / 2 + 26;

        Button.Builder yesBuilder = Button.builder(Component.literal("Disable Detected"), btn -> {
            for (Module m : detectedModules) {
                if (m.isToggled()) {
                    m.toggle();
                }
            }
            if (this.minecraft != null) {
                this.minecraft.setScreenAndShow(null);
            }
        });
        yesBuilder.bounds(this.width / 2 - 112, boxY, 106, 20);
        this.addRenderableWidget(yesBuilder.build());

        Button.Builder noBuilder = Button.builder(Component.literal("Keep Enabled"), btn -> {
            if (this.minecraft != null) {
                this.minecraft.setScreenAndShow(null);
            }
        });
        noBuilder.bounds(this.width / 2 + 6, boxY, 106, 20);
        this.addRenderableWidget(noBuilder.build());
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
        guiGraphics.fill(0, 0, this.width, this.height, UiRenderPrimitives.argb(145, 22, 8, 10));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        UiTheme theme = UiThemeContext.current();
        float cx = this.width * 0.5f;
        float cy = this.height * 0.5f;
        float cardW = 320f;
        float cardH = 138f;

        UiRenderPrimitives.drawRoundedShadow(guiGraphics, cx - cardW * 0.5f, cy - 70f, cardW, cardH, 14f, 12f, UiRenderPrimitives.argb(105, 0, 0, 0));
        UiRenderPrimitives.drawGlassCard(
            guiGraphics,
            cx - cardW * 0.5f,
            cy - 72f,
            cardW,
            cardH,
            14f,
            theme.windowTopColor(232),
            theme.windowBottomColor(242),
            UiRenderPrimitives.argb(95, 255, 100, 112),
            UiRenderPrimitives.argb(36, 255, 100, 112)
        );

        StringBuilder mods = new StringBuilder();
        for (int i = 0; i < detectedModules.size(); i++) {
            Module m = detectedModules.get(i);
            mods.append(m.getName());
            if (i < detectedModules.size() - 1) {
                mods.append(", ");
            }
        }

        SatoshiFont.drawCentered(guiGraphics, "Server Anti-Cheat Warning", cx, cy - 54f, 7.8f, TextGradient.accent(1f, 0xFFFF6470, theme), SatoshiWeight.SEMIBOLD);
        String clippedMods = UiRenderPrimitives.ellipsize(mods.toString() + " currently enabled", cardW - 28f, 5.5f, SatoshiWeight.SEMIBOLD);
        SatoshiFont.drawCentered(guiGraphics, clippedMods, cx, cy - 33f, 5.5f, TextGradient.silver(0.94f, theme), SatoshiWeight.SEMIBOLD);
        SatoshiFont.drawCentered(guiGraphics, "These modules may flag on this server. Disable them now?", cx, cy - 17f, 5.1f, TextGradient.mist(0.84f, theme), SatoshiWeight.MEDIUM);

        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
    }
}
