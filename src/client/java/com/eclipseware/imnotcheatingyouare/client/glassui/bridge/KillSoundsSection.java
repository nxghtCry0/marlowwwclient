package com.eclipseware.imnotcheatingyouare.client.glassui.bridge;

import com.eclipseware.imnotcheatingyouare.client.glassui.asset.LucideIcon;
import com.eclipseware.imnotcheatingyouare.client.glassui.audio.UiSoundSink;
import com.eclipseware.imnotcheatingyouare.client.glassui.model.UiCustomSection;
import com.eclipseware.imnotcheatingyouare.client.glassui.model.UiHitRect;
import com.eclipseware.imnotcheatingyouare.client.glassui.model.UiScreenActions;
import com.eclipseware.imnotcheatingyouare.client.glassui.model.UiSectionMeasureContext;
import com.eclipseware.imnotcheatingyouare.client.glassui.model.UiSectionRenderContext;
import com.eclipseware.imnotcheatingyouare.client.glassui.render.SatoshiFont;
import com.eclipseware.imnotcheatingyouare.client.glassui.render.SatoshiWeight;
import com.eclipseware.imnotcheatingyouare.client.glassui.render.SmoothScroller;
import com.eclipseware.imnotcheatingyouare.client.glassui.render.TextGradient;
import com.eclipseware.imnotcheatingyouare.client.glassui.render.UiRenderPrimitives;
import com.eclipseware.imnotcheatingyouare.client.glassui.theme.UiTheme;
import com.eclipseware.imnotcheatingyouare.client.module.impl.KillSoundPlayer;
import com.eclipseware.imnotcheatingyouare.client.module.impl.KillSounds;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class KillSoundsSection implements UiCustomSection {
    public static final KillSoundsSection INSTANCE = new KillSoundsSection();

    private float browseHover = 0f;
    private float previewHover = 0f;
    private float clearHover = 0f;

    private UiHitRect browseHitbox = UiHitRect.EMPTY;
    private UiHitRect previewHitbox = UiHitRect.EMPTY;
    private UiHitRect clearHitbox = UiHitRect.EMPTY;

    private KillSoundsSection() {
    }

    @Override
    public float measureHeight(UiSectionMeasureContext context) {
        return 74f;
    }

    @Override
    public void clearHitboxes() {
        browseHitbox = UiHitRect.EMPTY;
        previewHitbox = UiHitRect.EMPTY;
        clearHitbox = UiHitRect.EMPTY;
    }

    @Override
    public void render(UiSectionRenderContext context) {
        KillSounds mod = KillSounds.INSTANCE;
        if (mod == null) {
            return;
        }
        GuiGraphicsExtractor graphics = context.graphics();
        SmoothScroller scroller = context.scroller();
        float areaX = context.areaX();
        float areaW = context.areaW();
        float curY = context.startY();
        float viewTop = context.viewTop();
        float viewBottom = context.viewBottom();
        float lx = context.mouseX();
        float ly = context.mouseY();
        float animDt = context.animDt();
        float alpha = context.alpha() * scroller.edgeFadeAlpha(curY, 68f, viewTop, viewBottom);
        boolean interactive = context.interactive();
        UiTheme theme = context.theme();
        int accent = theme.accentColor();
        float rScale = theme.cornerRadiusScale();
        float cardR = 11f * rScale;

        UiRenderPrimitives.drawThemedGlassCard(
            graphics,
            areaX,
            curY,
            areaW,
            66f,
            cardR,
            32f,
            18f,
            52f,
            22f,
            alpha,
            mod.hasValidSoundFile() ? 0.35f : 0f,
            0f,
            theme
        );

        UiRenderPrimitives.drawIcon(
            graphics,
            LucideIcon.BELL,
            areaX + 12f,
            curY + 11f,
            12f,
            UiRenderPrimitives.multiplyAlpha(accent, alpha)
        );
        SatoshiFont.draw(
            graphics,
            "Kill Sound Audio File",
            areaX + 29f,
            curY + 11.2f,
            6.1f,
            TextGradient.pearl(alpha, accent, 0.28f, theme),
            SatoshiWeight.SEMIBOLD
        );

        String statusLabel;
        if (KillSoundPlayer.isDialogOpen()) {
            statusLabel = "Waiting for file selection dialog...";
        } else if (mod.hasValidSoundFile()) {
            statusLabel = "Selected: " + mod.getSoundDisplayName() + "  (" + mod.getSoundFilePath() + ")";
        } else {
            statusLabel = "No audio file selected • Click Browse File to choose a .wav, .ogg, or .mp3";
        }
        String clippedStatus = UiRenderPrimitives.ellipsize(statusLabel, areaW - 42f, 4.8f, SatoshiWeight.MEDIUM);
        SatoshiFont.draw(
            graphics,
            clippedStatus,
            areaX + 29f,
            curY + 23.5f,
            4.8f,
            mod.hasValidSoundFile()
                ? TextGradient.accent(0.92f * alpha, accent, theme)
                : TextGradient.mist(0.78f * alpha, theme),
            SatoshiWeight.MEDIUM
        );

        float btnY = curY + 38f;
        float btnH = 21f;
        float gap = 8f;
        float browseW = (areaW - 24f - gap * 2f) * 0.46f;
        float previewW = (areaW - 24f - gap * 2f) * 0.30f;
        float clearW = (areaW - 24f - gap * 2f) * 0.24f;

        float bx = areaX + 12f;
        UiHitRect bRect = new UiHitRect(bx, btnY, browseW, btnH);
        UiHitRect pRect = new UiHitRect(bx + browseW + gap, btnY, previewW, btnH);
        UiHitRect cRect = new UiHitRect(bx + browseW + gap + previewW + gap, btnY, clearW, btnH);
        if (interactive) {
            browseHitbox = bRect;
            previewHitbox = pRect;
            clearHitbox = cRect;
        }

        browseHover = UiRenderPrimitives.approachExp(browseHover, (interactive && bRect.contains(lx, ly)) ? 1f : 0f, 16f, animDt);
        previewHover = UiRenderPrimitives.approachExp(previewHover, (interactive && pRect.contains(lx, ly)) ? 1f : 0f, 16f, animDt);
        clearHover = UiRenderPrimitives.approachExp(clearHover, (interactive && cRect.contains(lx, ly)) ? 1f : 0f, 16f, animDt);

        drawButton(graphics, bRect, "Select Sound File...", LucideIcon.PACKAGE, browseHover, true, alpha, theme);
        drawButton(graphics, pRect, "Test Sound", LucideIcon.SPARKLES, previewHover, mod.hasValidSoundFile(), alpha, theme);
        drawButton(graphics, cRect, "Clear", LucideIcon.X_CLOSE, clearHover, false, alpha, theme);
    }

    private static void drawButton(
        GuiGraphicsExtractor graphics,
        UiHitRect rect,
        String label,
        LucideIcon icon,
        float hover,
        boolean accented,
        float alpha,
        UiTheme theme
    ) {
        int accent = theme.accentColor();
        float r = Math.min(9.5f, 9.5f * theme.cornerRadiusScale());
        UiRenderPrimitives.drawThemedGlassCard(
            graphics,
            rect.x(),
            rect.y(),
            rect.w(),
            rect.h(),
            r,
            32f + 22f * hover,
            18f + 14f * hover,
            52f + 30f * hover,
            22f + 14f * hover,
            alpha,
            accented ? (0.48f + 0.30f * hover) : hover * 0.35f,
            hover,
            theme
        );
        float iconSz = 9.2f;
        float fontSz = 5.3f;
        float textW = SatoshiFont.width(label, fontSz, SatoshiWeight.SEMIBOLD);
        float totalW = iconSz + 5f + textW;
        float startX = rect.x() + (rect.w() - totalW) * 0.5f;
        int icTint = accented
            ? UiRenderPrimitives.multiplyAlpha(accent, alpha)
            : UiRenderPrimitives.argb(Math.round((200f + 50f * hover) * alpha), 255, 255, 255);
        UiRenderPrimitives.drawIcon(graphics, icon, startX, rect.y() + (rect.h() - iconSz) * 0.5f, iconSz, icTint);
        SatoshiFont.draw(
            graphics,
            label,
            startX + iconSz + 5f,
            rect.y() + (rect.h() - fontSz) * 0.5f,
            fontSz,
            TextGradient.pearl(alpha, accent, accented ? 0.30f : 0.18f, theme),
            SatoshiWeight.SEMIBOLD
        );
    }

    @Override
    public boolean onMouseClicked(UiScreenActions actions, float mouseX, float mouseY, int button) {
        if (button != InputConstants.MOUSE_BUTTON_LEFT) {
            return false;
        }
        KillSounds mod = KillSounds.INSTANCE;
        if (mod == null) {
            return false;
        }
        UiSoundSink sounds = actions.sounds();
        UiTheme theme = actions.theme();
        if (browseHitbox.contains(mouseX, mouseY)) {
            sounds.playClick(1.08f);
            actions.spawnRipple(mouseX, mouseY, 20f, theme.accentColor());
            mod.promptForSoundFile(chosen -> {
                Minecraft mc = Minecraft.getInstance();
                if (mc != null && chosen != null) {
                    mc.execute(() -> actions.showToast("Loaded kill sound: " + chosen.getName()));
                }
            });
            return true;
        }
        if (previewHitbox.contains(mouseX, mouseY)) {
            actions.spawnRipple(mouseX, mouseY, 18f, theme.accentColor());
            if (mod.playConfiguredSound()) {
                actions.showToast("Playing " + mod.getSoundDisplayName());
            } else {
                sounds.playClick(0.90f);
                actions.showToast("Select a valid audio file first");
            }
            return true;
        }
        if (clearHitbox.contains(mouseX, mouseY)) {
            mod.setSoundFilePath("");
            sounds.playClick(0.94f);
            actions.spawnRipple(mouseX, mouseY, 16f, 0xFFFFFFFF);
            actions.showToast("Cleared kill sound file");
            return true;
        }
        return false;
    }
}
