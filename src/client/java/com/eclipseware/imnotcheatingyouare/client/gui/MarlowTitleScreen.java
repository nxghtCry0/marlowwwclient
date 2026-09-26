package com.eclipseware.imnotcheatingyouare.client.gui;

import com.eclipseware.imnotcheatingyouare.client.utils.FontUtils;
import com.eclipseware.imnotcheatingyouare.client.utils.remnant.Render2DEngine;
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

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class MarlowTitleScreen extends TitleScreen {
    private static final Identifier LOGO_ID = Identifier.parse("imnotcheatingyouare:textures/logo.png");
    private static final int LOGO_NATIVE_WIDTH = 916;
    private static final int LOGO_NATIVE_HEIGHT = 272;

    private static final Color PANEL = new Color(24, 16, 40, 150);
    private static final Color BUTTON = new Color(255, 255, 255, 38);
    private static final Color BUTTON_HOVER = new Color(255, 255, 255, 78);
    private static final Color BUTTON_BORDER = new Color(255, 255, 255, 60);
    private static final int BUTTON_HEIGHT = 22;
    private static final int BUTTON_GAP = 6;

    private final List<MenuButton> buttons = new ArrayList<>();
    private final long openedAt = System.currentTimeMillis();

    public MarlowTitleScreen() {
        super(false);
    }

    private float ease() {
        float fade = Math.min(1f, (System.currentTimeMillis() - openedAt) / 600f);
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
            Render2DEngine.drawRoundedRect(graphics.pose(), getX() - 1f, getY() - 1f, width + 2f, height + 2f, 8f, withAlpha(BUTTON_BORDER, ease * (0.6f + hover * 0.4f)));
            Render2DEngine.drawRoundedRect(graphics.pose(), getX(), getY(), width, height, 7f, withAlpha(lerp(BUTTON, BUTTON_HOVER, hover), ease));
            int textAlpha = (int) (255 * ease);
            int textColor = (textAlpha << 24) | (secondary ? 0xE8DDFB : 0xFFFFFF);
            graphics.centeredText(minecraft.font, FontUtils.verdana(getMessage().getString()), getX() + width / 2, getY() + (height - 8) / 2, textColor);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }

    private int buttonWidth() {
        return (int) Math.min(200f, width * 0.5f);
    }

    private int firstButtonY() {
        return height / 2 - 10;
    }

    @Override
    protected void init() {
        buttons.clear();
        int w = buttonWidth();
        int x = width / 2 - w / 2;
        int y = firstButtonY();
        buttons.add(new MenuButton(x, y, w, "Singleplayer", false, () -> minecraft.setScreenAndShow(new SelectWorldScreen(this))));
        y += BUTTON_HEIGHT + BUTTON_GAP;
        buttons.add(new MenuButton(x, y, w, "Multiplayer", false, () -> minecraft.setScreenAndShow(new JoinMultiplayerScreen(this))));
        y += BUTTON_HEIGHT + BUTTON_GAP;
        buttons.add(new MenuButton(x, y, w, "Settings", false, () -> minecraft.setScreenAndShow(new OptionsScreen(this, minecraft.options))));
        y += BUTTON_HEIGHT + BUTTON_GAP;
        buttons.add(new MenuButton(x, y, w, "Config", false, () -> minecraft.setScreenAndShow(new MenuConfigScreen(this))));
        y += BUTTON_HEIGHT + BUTTON_GAP + 8;
        buttons.add(new MenuButton(width / 2 - w / 4, y, w / 2, "Quit", true, () -> minecraft.stop()));
        for (MenuButton button : buttons) addRenderableWidget(button);
    }

    @Override
    public void tick() {
    }

    @Override
    public void added() {
    }

    @Override
    public void removed() {
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
        Render2DEngine.activeContext = graphics;
        float ease = ease();

        float logoW = Math.min(width * 0.55f, 300f);
        float logoH = logoW * LOGO_NATIVE_HEIGHT / (float) LOGO_NATIVE_WIDTH;
        int logoX = (int) (width / 2f - logoW / 2f);
        int logoY = (int) (Math.max(16f, firstButtonY() - logoH - 28f) - (1f - ease) * 12f);
        graphics.blit(LOGO_ID, logoX, logoY, logoX + (int) logoW, logoY + (int) logoH, 0.0f, 1.0f, 0.0f, 1.0f);

        if (!buttons.isEmpty()) {
            float pad = 14f;
            int w = buttonWidth();
            MenuButton last = buttons.get(buttons.size() - 1);
            float panelY = firstButtonY() - pad;
            float panelH = last.getY() + BUTTON_HEIGHT - firstButtonY() + pad * 2;
            Render2DEngine.drawRoundedRect(graphics.pose(), width / 2f - w / 2f - pad, panelY, w + pad * 2, panelH, 12f, withAlpha(PANEL, ease));
        }

        for (MenuButton button : buttons) {
            button.extractRenderState(graphics, mouseX, mouseY, partialTick);
        }

        graphics.text(minecraft.font, FontUtils.verdana("Marlowww V5"), 6, height - 12, 0xB0FFFFFF, false);
        String version = "Minecraft " + net.minecraft.SharedConstants.getCurrentVersion().name();
        graphics.text(minecraft.font, FontUtils.verdana(version), width - 6 - FontUtils.verdanaWidth(version), height - 12, 0xB0FFFFFF, false);
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
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.max(0, Math.min(255, (int) (color.getAlpha() * factor))));
    }

    private static Color lerp(Color a, Color b, float t) {
        return new Color(
                (int) (a.getRed() + (b.getRed() - a.getRed()) * t),
                (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t),
                (int) (a.getAlpha() + (b.getAlpha() - a.getAlpha()) * t));
    }
}
