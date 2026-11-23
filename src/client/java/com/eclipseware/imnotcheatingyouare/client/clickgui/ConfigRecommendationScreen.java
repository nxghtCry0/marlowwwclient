package com.eclipseware.imnotcheatingyouare.client.clickgui;

import com.eclipseware.imnotcheatingyouare.client.glassui.render.SatoshiFont;
import com.eclipseware.imnotcheatingyouare.client.glassui.render.SatoshiWeight;
import com.eclipseware.imnotcheatingyouare.client.glassui.render.TextGradient;
import com.eclipseware.imnotcheatingyouare.client.glassui.render.UiRenderPrimitives;
import com.eclipseware.imnotcheatingyouare.client.glassui.theme.UiTheme;
import com.eclipseware.imnotcheatingyouare.client.glassui.theme.UiThemeContext;
import com.eclipseware.imnotcheatingyouare.client.module.impl.RecommendedConfigs.FoundConfig;
import com.eclipseware.imnotcheatingyouare.client.setting.ConfigManager;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ConfigRecommendationScreen extends Screen {
    private final List<FoundConfig> configs;
    private boolean promptMode = true;

    public ConfigRecommendationScreen(List<FoundConfig> configs) {
        super(Component.literal("Recommended Configs"));
        this.configs = configs;
    }

    @Override
    protected void init() {
        this.clearWidgets();
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        if (promptMode) {
            Button.Builder yesBuilder = Button.builder(Component.literal("Browse Configs"), button -> {
                promptMode = false;
                this.init();
            });
            yesBuilder.bounds(centerX - 105, centerY + 24, 100, 20);
            this.addRenderableWidget(yesBuilder.build());

            Button.Builder noBuilder = Button.builder(Component.literal("Dismiss"), button -> {
                this.minecraft.setScreenAndShow(null);
            });
            noBuilder.bounds(centerX + 5, centerY + 24, 100, 20);
            this.addRenderableWidget(noBuilder.build());
        } else {
            int yOffset = 48;
            for (FoundConfig config : configs) {
                int configY = yOffset;
                Button.Builder loadBuilder = Button.builder(Component.literal("Load " + config.name), button -> {
                    ConfigManager.importString(config.base64);
                    var player = this.minecraft.player;
                    if (player != null) {
                        player.sendSystemMessage(Component.literal("§d[Marlow] §aLoaded config: " + config.name));
                    }
                    this.minecraft.setScreenAndShow(null);
                });
                loadBuilder.bounds(centerX - 150, configY, 300, 20);
                this.addRenderableWidget(loadBuilder.build());
                yOffset += 44;
            }

            Button.Builder cancelBuilder = Button.builder(Component.literal("Cancel"), button -> {
                this.minecraft.setScreenAndShow(null);
            });
            cancelBuilder.bounds(centerX - 50, this.height - 32, 100, 20);
            this.addRenderableWidget(cancelBuilder.build());
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        UiTheme theme = UiThemeContext.current();
        if (theme.blurEnabled()) {
            extractBlurredBackground(guiGraphics);
        }
        guiGraphics.fill(0, 0, this.width, this.height, UiRenderPrimitives.argb(135, 10, 11, 14));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        UiTheme theme = UiThemeContext.current();
        int accent = theme.accentColor();
        float cx = this.width * 0.5f;
        float cy = this.height * 0.5f;

        if (promptMode) {
            float cardW = 280f;
            float cardH = 124f;
            UiRenderPrimitives.drawRoundedShadow(context, cx - cardW * 0.5f, cy - 64f, cardW, cardH, 14f, 12f, UiRenderPrimitives.argb(100, 0, 0, 0));
            UiRenderPrimitives.drawGlassCard(
                context,
                cx - cardW * 0.5f,
                cy - 66f,
                cardW,
                cardH,
                14f,
                theme.windowTopColor(232),
                theme.windowBottomColor(242),
                UiRenderPrimitives.argb(74, 255, 255, 255),
                UiRenderPrimitives.argb(26, 255, 255, 255)
            );
            SatoshiFont.drawCentered(context, "Recommended Server Configs", cx, cy - 48f, 7.6f, TextGradient.pearl(1f, accent, 0.34f, theme), SatoshiWeight.SEMIBOLD);
            String countLine = "Found " + configs.size() + " profile" + (configs.size() == 1 ? "" : "s") + " tailored for this server.";
            SatoshiFont.drawCentered(context, countLine, cx, cy - 28f, 5.6f, TextGradient.silver(0.92f, theme), SatoshiWeight.MEDIUM);
            SatoshiFont.drawCentered(context, "Would you like to preview and load one?", cx, cy - 14f, 5.3f, TextGradient.mist(0.80f, theme), SatoshiWeight.MEDIUM);
        } else {
            SatoshiFont.drawCentered(context, "Available Server Profiles", cx, 18f, 8.2f, TextGradient.pearl(1f, accent, 0.34f, theme), SatoshiWeight.SEMIBOLD);
            int yOffset = 48;
            for (FoundConfig config : configs) {
                String preview = UiRenderPrimitives.ellipsize(config.modulesPreview, 290f, 5.0f, SatoshiWeight.MEDIUM);
                SatoshiFont.drawCentered(context, preview, cx, yOffset + 24f, 5.0f, TextGradient.mist(0.82f, theme), SatoshiWeight.MEDIUM);
                yOffset += 44;
            }
        }

        super.extractRenderState(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
