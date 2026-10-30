package com.eclipseware.imnotcheatingyouare.client.gui;

import com.eclipseware.imnotcheatingyouare.client.module.impl.Menu;
import com.eclipseware.imnotcheatingyouare.client.utils.FontUtils;
import com.eclipseware.imnotcheatingyouare.client.utils.remnant.Render2DEngine;
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

    private static final Color BUTTON = new Color(26, 12, 38, 170);
    private static final Color BUTTON_HOVER = new Color(255, 118, 26, 115);
    private static final Color BUTTON_BORDER = new Color(255, 136, 42, 95);
    private static final Color SPOOKY_VIOLET = new Color(162, 68, 255, 140);
    private static final int BUTTON_HEIGHT = 22;
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
        float duration = Math.max(120f, 3000f / Math.max(1f, speed));
        float fade = Math.min(1f, (System.currentTimeMillis() - openedAt) / duration);
        return 1f - (1f - fade) * (1f - fade);
    }

    private class MenuButton extends AbstractButton {
        private final Runnable action;
        private final boolean secondary;
        private float hover;

        MenuButton(int x, int y, int w, String label, boolean secondary, Runnable action) {
            super(x, y, w, BUTTON_HEIGHT, Component.literal(label));
            this.action = action;
            this.secondary = secondary;
        }

        @Override
        public void onPress(InputWithModifiers input) {
            action.run();
        }

        @Override
        protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            float ease = ease();
            hover += ((isHoveredOrFocused() ? 1f : 0f) - hover) * 0.25f;
            Render2DEngine.activeContext = graphics;
            Color primary = Menu.getPrimaryColor();
            float glowFactor = Menu.getHoverGlow();
            float rad = Math.max(2f, Math.min(11f, Menu.getCornerRadius() * 0.6f));
            Color borderTarget = lerp(BUTTON_BORDER, withAlpha(primary, 0.85f), hover);
            Color fillTarget = lerp(BUTTON_HOVER, withAlpha(primary, 0.35f + glowFactor * 0.45f), glowFactor);
            Color fillCol = lerp(BUTTON, fillTarget, hover);
            Render2DEngine.drawRoundedRect(
                graphics.pose(),
                getX() - 1f,
                getY() - 1f,
                width + 2f,
                height + 2f,
                rad + 1f,
                withAlpha(borderTarget, ease * (0.6f + hover * 0.4f))
            );
            Render2DEngine.drawRoundedRect(
                graphics.pose(),
                getX(),
                getY(),
                width,
                height,
                rad,
                withAlpha(fillCol, ease)
            );
            int textAlpha = (int) (255 * ease);
            int textColor = (textAlpha << 24) | (secondary ? 0xE8DDFB : 0xFFFFFF);
            Component msg = getMessage();
            String rawLabel = msg.getString();
            Component styled = FontUtils.useVerdana() ? FontUtils.verdana(rawLabel) : Component.literal(rawLabel);
            graphics.centeredText(
                minecraft.font,
                styled,
                getX() + width / 2,
                getY() + (height - 8) / 2,
                textColor
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
        return height / 2 - 10;
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
        buttons.add(new MenuButton(x, y, w, "Settings", false, () -> minecraft.setScreenAndShow(new OptionsScreen(this, minecraft.options, false))));
        if (showCfg) {
            y += BUTTON_HEIGHT + BUTTON_GAP;
            buttons.add(new MenuButton(x, y, w, "Config", false, () -> minecraft.setScreenAndShow(new MenuConfigScreen(this))));
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
    public void added() {
    }

    @Override
    public void removed() {
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
        int topArgb = (0xFF << 24) | ((top.getRed() & 0xFF) << 16) | ((top.getGreen() & 0xFF) << 8) | (top.getBlue() & 0xFF);
        int botArgb = (0xFF << 24) | ((bot.getRed() & 0xFF) << 16) | ((bot.getGreen() & 0xFF) << 8) | (bot.getBlue() & 0xFF);
        graphics.fillGradient(0, 0, width, height, topArgb, botArgb);
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
        Render2DEngine.activeContext = graphics;
        float ease = ease();

        if (Menu.shouldShowLogo()) {
            float scale = Menu.getLogoScale();
            float logoW = Math.min(width * 0.55f, 300f) * scale;
            float logoH = logoW * LOGO_NATIVE_HEIGHT / (float) LOGO_NATIVE_WIDTH;
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
            float panelW = w + pad * 2;
            float panelH = last.getY() + BUTTON_HEIGHT - firstButtonY() + pad * 2;
            Color sec = Menu.getSecondaryColor();
            Color pri = Menu.getPrimaryColor();
            Color panelCol = new Color(sec.getRed(), sec.getGreen(), sec.getBlue(), Menu.getPanelAlpha());
            float cornerRad = Menu.getCornerRadius();
            float pulse = 0.5f + 0.5f * (float) Math.sin((System.currentTimeMillis() - openedAt) * 0.0032);
            Color outerGlow = lerp(SPOOKY_VIOLET, withAlpha(pri, 0.55f), pulse);
            Render2DEngine.drawRoundedRect(
                graphics.pose(),
                panelX - 2f,
                panelY - 2f,
                panelW + 4f,
                panelH + 4f,
                cornerRad + 2f,
                withAlpha(outerGlow, ease * 0.65f)
            );
            Render2DEngine.drawRoundedRect(
                graphics.pose(),
                panelX,
                panelY,
                panelW,
                panelH,
                cornerRad,
                withAlpha(panelCol, ease)
            );
        }

        for (MenuButton button : buttons) {
            button.extractRenderState(graphics, mouseX, mouseY, partialTick);
        }

        if (Menu.shouldShowWatermark()) {
            boolean useVerd = FontUtils.useVerdana();
            String brandStr = "Marlowww V5.5 · Spooky Edition";
            Component brand = useVerd ? FontUtils.verdana(brandStr) : Component.literal(brandStr);
            graphics.text(minecraft.font, brand, 6, height - 12, 0xFFFF8C2E, false);
            WorldVersion wv = SharedConstants.getCurrentVersion();
            String version = "Minecraft " + wv.name();
            Component verComp = useVerd ? FontUtils.verdana(version) : Component.literal(version);
            int verW = useVerd ? FontUtils.verdanaWidth(version) : minecraft.font.width(verComp);
            graphics.text(minecraft.font, verComp, width - 6 - verW, height - 12, 0xFFE6C8FF, false);
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

    private static Color withAlpha(Color color, float factor) {
        int a = Math.max(0, Math.min(255, (int) (color.getAlpha() * factor)));
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), a);
    }

    private static Color lerp(Color a, Color b, float t) {
        int r = (int) (a.getRed() + (b.getRed() - a.getRed()) * t);
        int g = (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t);
        int bl = (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t);
        int al = (int) (a.getAlpha() + (b.getAlpha() - a.getAlpha()) * t);
        return new Color(
            Math.max(0, Math.min(255, r)),
            Math.max(0, Math.min(255, g)),
            Math.max(0, Math.min(255, bl)),
            Math.max(0, Math.min(255, al))
        );
    }
}
