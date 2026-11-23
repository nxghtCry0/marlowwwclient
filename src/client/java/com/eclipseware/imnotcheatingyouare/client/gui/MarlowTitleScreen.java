package com.eclipseware.imnotcheatingyouare.client.gui;

import com.eclipseware.imnotcheatingyouare.client.glassui.bridge.ClientUiSounds;
import com.eclipseware.imnotcheatingyouare.client.glassui.render.SatoshiFont;
import com.eclipseware.imnotcheatingyouare.client.glassui.render.SatoshiWeight;
import com.eclipseware.imnotcheatingyouare.client.glassui.render.TextGradient;
import com.eclipseware.imnotcheatingyouare.client.glassui.render.UiRenderPrimitives;
import com.eclipseware.imnotcheatingyouare.client.glassui.theme.UiTheme;
import com.eclipseware.imnotcheatingyouare.client.glassui.theme.UiThemeContext;
import com.eclipseware.imnotcheatingyouare.client.module.impl.Menu;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.WorldVersion;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class MarlowTitleScreen extends TitleScreen {
    private static final Identifier LOGO_ID = Identifier.parse("imnotcheatingyouare:textures/logo.png");
    private static final int LOGO_NATIVE_WIDTH = 916;
    private static final int LOGO_NATIVE_HEIGHT = 272;
    private static final int BUTTON_HEIGHT = 24;
    private static final int BUTTON_GAP = 6;

    private final List<MenuButton> buttons = new ArrayList<>();
    private final long openedAt = System.currentTimeMillis();
    private boolean lastConfigBtnState = true;
    private int lastBuiltBtnWidth = -1;

    public MarlowTitleScreen() {
        super(false);
    }

    private float ease() {
        float speed = Menu.getAnimationSpeed();
        float duration = Math.max(120f, 1800f / Math.max(1f, speed));
        float fade = Math.min(1f, (System.currentTimeMillis() - openedAt) / duration);
        return UiRenderPrimitives.easeOutCubic(fade);
    }

    private class MenuButton extends AbstractButton {
        private final String labelText;
        private final Runnable action;
        private final boolean danger;
        private float hover;

        MenuButton(int x, int y, int w, String label, boolean danger, Runnable action) {
            super(x, y, w, BUTTON_HEIGHT, Component.literal(label));
            this.labelText = label;
            this.action = action;
            this.danger = danger;
        }

        @Override
        public void onPress(InputWithModifiers input) {
            ClientUiSounds.INSTANCE.playClick(1.06f);
            action.run();
        }

        @Override
        protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            float ease = ease();
            hover += ((isHoveredOrFocused() ? 1f : 0f) - hover) * 0.24f;
            UiTheme theme = UiThemeContext.current();
            int accent = theme.accentColor();
            float rad = Math.max(4f, Math.min(11f, Menu.getCornerRadius() * 0.65f));

            if (danger) {
                int dangerCol = 0xFFFF6470;
                int topCol = UiRenderPrimitives.lerpColor(
                    UiRenderPrimitives.argb(Math.round(34f * ease), 255, 110, 120),
                    UiRenderPrimitives.multiplyAlpha(dangerCol, 0.42f * ease),
                    hover
                );
                int botCol = UiRenderPrimitives.argb(Math.round((20f + 16f * hover) * ease), 180, 50, 60);
                int rimCol = UiRenderPrimitives.multiplyAlpha(dangerCol, (0.55f + 0.35f * hover) * ease);
                UiRenderPrimitives.drawGlassCard(graphics, getX(), getY(), width, height, rad, topCol, botCol, rimCol, botCol);
            } else {
                UiRenderPrimitives.drawThemedGlassCard(
                    graphics,
                    getX(),
                    getY(),
                    width,
                    height,
                    rad,
                    34f + 24f * hover,
                    20f + 14f * hover,
                    56f + 34f * hover,
                    24f + 16f * hover,
                    ease,
                    hover * 0.65f,
                    hover,
                    theme
                );
            }

            SatoshiFont.drawCentered(
                graphics,
                labelText,
                getX() + width * 0.5f,
                getY() + (height - 6.6f) * 0.5f,
                6.6f,
                TextGradient.pearl(ease, danger ? 0xFFFF6470 : accent, 0.28f + 0.20f * hover, theme),
                SatoshiWeight.SEMIBOLD
            );
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }

    private int buttonWidth() {
        float target = Menu.getButtonWidth();
        return (int) Math.min(target, width * 0.55f);
    }

    private int firstButtonY() {
        return height / 2 - 12;
    }

    @Override
    protected void init() {
        rebuildMenuButtons();
    }

    private void rebuildMenuButtons() {
        clearWidgets();
        buttons.clear();
        int w = buttonWidth();
        boolean showCfg = Menu.shouldShowConfigButton();
        lastBuiltBtnWidth = w;
        lastConfigBtnState = showCfg;
        int x = width / 2 - w / 2;
        int y = firstButtonY();
        buttons.add(new MenuButton(x, y, w, "Singleplayer", false, () -> minecraft.setScreenAndShow(new SelectWorldScreen(this))));
        y += BUTTON_HEIGHT + BUTTON_GAP;
        buttons.add(new MenuButton(x, y, w, "Multiplayer", false, () -> minecraft.setScreenAndShow(new JoinMultiplayerScreen(this))));
        y += BUTTON_HEIGHT + BUTTON_GAP;
        buttons.add(new MenuButton(x, y, w, "Options", false, () -> minecraft.setScreenAndShow(new OptionsScreen(this, minecraft.options))));
        if (showCfg) {
            y += BUTTON_HEIGHT + BUTTON_GAP;
            buttons.add(new MenuButton(x, y, w, "Marlow Menu", false, () -> minecraft.setScreenAndShow(new MenuConfigScreen(this))));
        }
        y += BUTTON_HEIGHT + BUTTON_GAP + 8;
        buttons.add(new MenuButton(width / 2 - w / 4, y, w / 2, "Quit", true, () -> minecraft.stop()));
        for (MenuButton button : buttons) {
            addRenderableWidget(button);
        }
    }

    @Override
    public void tick() {
        if (!Menu.isCustomMenuEnabled() && minecraft != null) {
            minecraft.gui.setScreen(new TitleScreen());
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        String bgMode = Menu.getBackgroundMode();
        if ("Vanilla Panorama".equalsIgnoreCase(bgMode)) {
            super.extractPanorama(graphics, partialTick);
            return;
        }
        if ("Shader".equalsIgnoreCase(bgMode) && PastelShaderBackground.render()) {
            graphics.blit(PastelShaderBackground.TEXTURE_ID, 0, 0, width, height, 0.0f, 1.0f, 1.0f, 0.0f);
            return;
        }
        Color top = Menu.getPrimaryColor();
        Color bot = Menu.getSecondaryColor();
        graphics.fillGradient(0, 0, width, height, top.getRGB(), bot.getRGB());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (!Menu.isCustomMenuEnabled()) {
            if (minecraft != null) {
                minecraft.gui.setScreen(new TitleScreen());
            }
            return;
        }
        if (lastBuiltBtnWidth != buttonWidth() || lastConfigBtnState != Menu.shouldShowConfigButton()) {
            rebuildMenuButtons();
        }
        extractBackground(graphics, mouseX, mouseY, partialTick);
        UiTheme theme = UiThemeContext.current();
        int accent = theme.accentColor();
        float ease = ease();

        if (Menu.shouldShowLogo()) {
            float scale = Menu.getLogoScale();
            float logoW = Math.min(width * 0.55f, 300f) * scale;
            float logoH = logoW * LOGO_NATIVE_HEIGHT / LOGO_NATIVE_WIDTH;
            int logoX = (int) (width / 2f - logoW / 2f);
            int logoY = (int) (Math.max(12f, firstButtonY() - logoH - 28f) - (1f - ease) * 12f);
            graphics.blit(LOGO_ID, logoX, logoY, logoX + (int) logoW, logoY + (int) logoH, 0.0f, 1.0f, 0.0f, 1.0f);
        }

        if (!buttons.isEmpty()) {
            float pad = 14f;
            int w = buttonWidth();
            MenuButton last = buttons.get(buttons.size() - 1);
            float panelX = width / 2f - w / 2f - pad;
            float panelY = firstButtonY() - pad;
            float panelW = w + pad * 2f;
            float panelH = last.getY() + BUTTON_HEIGHT - firstButtonY() + pad * 2f;
            float cornerRad = Math.max(6f, Menu.getCornerRadius());

            UiRenderPrimitives.drawRoundedShadow(graphics, panelX, panelY + 2f, panelW, panelH, cornerRad, 14f, UiRenderPrimitives.argb(Math.round(105f * ease), 0, 0, 0));
            UiRenderPrimitives.drawGlassCard(
                graphics,
                panelX,
                panelY,
                panelW,
                panelH,
                cornerRad,
                theme.windowTopColor(Math.round(218f * ease)),
                theme.windowBottomColor(Math.round(234f * ease)),
                UiRenderPrimitives.argb(Math.round(72f * ease), 255, 255, 255),
                UiRenderPrimitives.argb(Math.round(26f * ease), 255, 255, 255)
            );
        }

        for (MenuButton button : buttons) {
            button.extractRenderState(graphics, mouseX, mouseY, partialTick);
        }

        if (Menu.shouldShowWatermark()) {
            String brandStr = "Marlow Client • Glass UI";
            SatoshiFont.draw(graphics, brandStr, 8f, height - 13f, 5.8f, TextGradient.pearl(ease, accent, 0.35f, theme), SatoshiWeight.SEMIBOLD);
            WorldVersion wv = SharedConstants.getCurrentVersion();
            String version = "Minecraft " + wv.name();
            float verW = SatoshiFont.width(version, 5.6f, SatoshiWeight.MEDIUM);
            SatoshiFont.draw(graphics, version, width - 8f - verW, height - 13f, 5.6f, TextGradient.mist(0.85f * ease, theme), SatoshiWeight.MEDIUM);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        for (MenuButton button : buttons) {
            if (button.mouseClicked(event, doubleClick)) {
                setFocused(button);
                return true;
            }
        }
        return false;
    }
}
