package com.eclipseware.imnotcheatingyouare.client.clickgui;

import com.eclipseware.imnotcheatingyouare.client.module.impl.RecommendedConfigs.FoundConfig;
import com.eclipseware.imnotcheatingyouare.client.setting.ConfigManager;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

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
            String message = "We found " + configs.size() + " config" + (configs.size() == 1 ? "" : "'s") + "! Would you like to choose and load one?";
            
            this.addRenderableWidget(Button.builder(Component.literal("Yes"), button -> {
                promptMode = false;
                this.init();
            }).bounds(centerX - 105, centerY + 20, 100, 20).build());

            this.addRenderableWidget(Button.builder(Component.literal("No"), button -> {
                this.minecraft.setScreen(null);
            }).bounds(centerX + 5, centerY + 20, 100, 20).build());
        } else {
            int yOffset = 40;
            for (FoundConfig config : configs) {
                int configY = yOffset;
                
                this.addRenderableWidget(Button.builder(Component.literal("Load " + config.name), button -> {
                    ConfigManager.importString(config.base64);
                    this.minecraft.player.sendSystemMessage(Component.literal("§d[EclipseWare] §aLoaded config: " + config.name));
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
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0x88000000);

        if (promptMode) {
            String title = "Recommended Configs Found!";
            String subtitle = "We found " + configs.size() + " config" + (configs.size() == 1 ? "" : "'s") + "! Would you like to choose and load one?";
            com.eclipseware.imnotcheatingyouare.client.utils.FontUtils.drawCenteredString(context, title, this.width / 2, this.height / 2 - 40, 0xFF55FF);
            com.eclipseware.imnotcheatingyouare.client.utils.FontUtils.drawCenteredString(context, subtitle, this.width / 2, this.height / 2 - 20, 0xFFFFFF);
        } else {
            com.eclipseware.imnotcheatingyouare.client.utils.FontUtils.drawCenteredString(context, "Available Configs", this.width / 2, 15, 0xFF55FF);
            
            int yOffset = 40;
            for (FoundConfig config : configs) {
                // Truncate preview if too long
                String preview = config.modulesPreview;
                if (preview.length() > 60) {
                    preview = preview.substring(0, 57) + "...";
                }
                com.eclipseware.imnotcheatingyouare.client.utils.FontUtils.drawCenteredString(context, "§7" + preview, this.width / 2, yOffset + 24, 0xAAAAAA);
                yOffset += 45;
            }
        }

        super.extractRenderState(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
