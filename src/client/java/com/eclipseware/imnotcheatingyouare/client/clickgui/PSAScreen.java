package com.eclipseware.imnotcheatingyouare.client.clickgui;

import com.eclipseware.imnotcheatingyouare.client.glassui.render.SatoshiFont;
import com.eclipseware.imnotcheatingyouare.client.glassui.render.SatoshiWeight;
import com.eclipseware.imnotcheatingyouare.client.glassui.render.TextGradient;
import com.eclipseware.imnotcheatingyouare.client.glassui.render.UiRenderPrimitives;
import com.eclipseware.imnotcheatingyouare.client.glassui.theme.UiTheme;
import com.eclipseware.imnotcheatingyouare.client.glassui.theme.UiThemeContext;
import com.eclipseware.imnotcheatingyouare.client.utils.PsaState;
import java.io.File;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class PSAScreen extends Screen {
    private final Screen parentScreen;

    public PSAScreen(Screen parentScreen) {
        super(Component.literal("PSA Warning"));
        this.parentScreen = parentScreen;
    }

    @Override
    protected void init() {
        super.init();
        int btnWidth = 180;
        int btnHeight = 20;
        int btnX = this.width / 2 - btnWidth / 2;
        int btnY = this.height / 2 + 76;

        Button.Builder continueBuilder = Button.builder(Component.literal("I Understand"), btn -> {
            try {
                File dir = new File(this.minecraft.gameDirectory, "config/imnotcheatingyouare");
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                File psaFile = new File(dir, "psa_accepted");
                psaFile.createNewFile();
                PsaState.markAccepted();
            } catch (Exception ignored) {
            }
            if (this.minecraft != null) {
                this.minecraft.setScreenAndShow(this.parentScreen);
            }
        });
        continueBuilder.bounds(btnX, btnY, btnWidth, btnHeight);
        this.addRenderableWidget(continueBuilder.build());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fill(0, 0, this.width, this.height, UiRenderPrimitives.argb(185, 10, 10, 14));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        UiTheme theme = UiThemeContext.current();
        int accent = theme.accentColor();
        float cx = this.width * 0.5f;
        float cy = this.height * 0.5f;
        float cardW = Math.min(this.width - 28f, 430f);
        float cardH = 206f;

        UiRenderPrimitives.drawRoundedShadow(guiGraphics, cx - cardW * 0.5f, cy - 98f, cardW, cardH, 15f, 14f, UiRenderPrimitives.argb(115, 0, 0, 0));
        UiRenderPrimitives.drawGlassCard(
            guiGraphics,
            cx - cardW * 0.5f,
            cy - 100f,
            cardW,
            cardH,
            15f,
            theme.windowTopColor(236),
            theme.windowBottomColor(244),
            UiRenderPrimitives.argb(82, 255, 255, 255),
            UiRenderPrimitives.argb(28, 255, 255, 255)
        );

        SatoshiFont.drawCentered(guiGraphics, "PSA: Official Marlow Client Sources", cx, cy - 82f, 8.2f, TextGradient.accent(1f, 0xFFFF6470, theme), SatoshiWeight.SEMIBOLD);
        SatoshiFont.drawCentered(guiGraphics, "Marlowww Client only has TWO official distribution sources:", cx, cy - 60f, 5.6f, TextGradient.pearl(1f, accent, 0.25f, theme), SatoshiWeight.SEMIBOLD);
        SatoshiFont.drawCentered(guiGraphics, "https://discord.gg/zgurRAZNte", cx, cy - 42f, 5.5f, TextGradient.accent(1f, accent, theme), SatoshiWeight.SEMIBOLD);
        SatoshiFont.drawCentered(guiGraphics, "https://github.com/nxghtCry0/marlowwwclient", cx, cy - 28f, 5.5f, TextGradient.accent(1f, accent, theme), SatoshiWeight.SEMIBOLD);
        SatoshiFont.drawCentered(guiGraphics, "Unverified reuploads from third parties may contain malware.", cx, cy - 8f, 5.2f, TextGradient.silver(0.90f, theme), SatoshiWeight.MEDIUM);
        SatoshiFont.drawCentered(guiGraphics, "Unauthorized modified distributions violate the license.", cx, cy + 6f, 5.0f, TextGradient.mist(0.80f, theme), SatoshiWeight.MEDIUM);
        SatoshiFont.drawCentered(guiGraphics, "Thanks for using Marlowww Client — nxght", cx, cy + 28f, 5.4f, TextGradient.pearl(0.95f, accent, 0.34f, theme), SatoshiWeight.SEMIBOLD);

        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
    }
}
