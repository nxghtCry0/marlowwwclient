package com.eclipseware.imnotcheatingyouare.client.clickgui;

import com.eclipseware.imnotcheatingyouare.client.module.impl.RecommendedConfigs.FoundConfig;
import com.eclipseware.imnotcheatingyouare.client.setting.ConfigManager;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.awt.Color;
import java.util.List;

public class ConfigRecommendationScreen extends Screen {
    private final List<FoundConfig> configs;
    private boolean promptMode = true; // True when asking Yes/No, False when listing

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
            this.addRenderableWidget(Button.builder(Component.literal("Yes"), button -> {
                promptMode = false;
                this.init();
            }).bounds(centerX - 105, centerY + 30, 100, 20).build());

            this.addRenderableWidget(Button.builder(Component.literal("No"), button -> {
                this.minecraft.setScreen(null);
            }).bounds(centerX + 5, centerY + 30, 100, 20).build());
        } else {
            int yOffset = 40;
            for (FoundConfig config : configs) {
                int configY = yOffset;
                
                this.addRenderableWidget(Button.builder(Component.literal("Load " + config.name), button -> {
                    ConfigManager.importString(config.base64);
                    this.minecraft.player.sendSystemMessage(Component.literal("\u00A7d[EclipseWare] \u00A7aLoaded config: " + config.name));
                    this.minecraft.player.sendSystemMessage(Component.literal("\u00A77(Tip: Press F3 + D to clear chat before screenshares)"));
                    this.minecraft.setScreen(null);
                }).bounds(centerX - 150, configY, 300, 20).build());
                
                yOffset += 45;
            }

            this.addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> {
                this.minecraft.setScreen(null);
            }).bounds(centerX - 50, this.height - 30, 100, 20).build());
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.fill(0, 0, this.width, this.height, new Color(15, 15, 40, 200).getRGB());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);

        if (promptMode) {
            context.centeredText(this.font, "\u00a7b\u00a7l[Cloud Configs Found]", this.width / 2, this.height / 2 - 40, -1);
            context.centeredText(this.font, "\u00a7fWe found \u00a7d" + configs.size() + " custom config" + (configs.size() == 1 ? "" : "s") + "\u00a7f for this server!", this.width / 2, this.height / 2 - 20, -1);
            context.centeredText(this.font, "Would you like to browse and load them?", this.width / 2, this.height / 2 - 5, -1);
        } else {
            context.centeredText(this.font, "\u00a7b\u00a7lAvailable Server Configs", this.width / 2, 15, -1);
            
            int yOffset = 40;
            for (FoundConfig config : configs) {
                String preview = config.modulesPreview;
                if (preview.length() > 60) {
                    preview = preview.substring(0, 57) + "...";
                }
                context.centeredText(this.font, "\u00a77" + preview, this.width / 2, yOffset + 24, -1);
                yOffset += 45;
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
