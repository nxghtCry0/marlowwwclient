package com.eclipseware.imnotcheatingyouare.client.glassui.notify;

import com.eclipseware.imnotcheatingyouare.client.glassui.asset.UiIcon;
import com.eclipseware.imnotcheatingyouare.client.glassui.render.KineticTextRenderer;
import com.eclipseware.imnotcheatingyouare.client.glassui.render.SatoshiFont;
import com.eclipseware.imnotcheatingyouare.client.glassui.render.SatoshiWeight;
import com.eclipseware.imnotcheatingyouare.client.glassui.render.StreamproofOverlay;
import com.eclipseware.imnotcheatingyouare.client.glassui.render.TextGradient;
import com.eclipseware.imnotcheatingyouare.client.glassui.render.UiRenderPrimitives;
import com.eclipseware.imnotcheatingyouare.client.glassui.theme.UiTheme;
import com.mojang.blaze3d.platform.Window;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import org.joml.Matrix3x2fStack;

public final class UiNotificationOverlay {
    public static final class Entry {
        public final long id;
        public final String title;
        public final String tag;
        public final String subtitle;
        public final UiIcon icon;
        public final UiNotificationKind kind;
        public final long createdNanos;
        public final float durationSec;
        public float animSlide = 0f;
        public float animY = -1f;

        public Entry(long id, String title, String tag, String subtitle, UiIcon icon, UiNotificationKind kind, long createdNanos, float durationSec) {
            this.id = id;
            this.title = title;
            this.tag = tag;
            this.subtitle = subtitle;
            this.icon = icon;
            this.kind = kind;
            this.createdNanos = createdNanos;
            this.durationSec = durationSec;
        }
    }

    private final UiNotificationConfig config;
    private final CopyOnWriteArrayList<Entry> entries = new CopyOnWriteArrayList<>();
    private long nextId = 1L;
    private long lastRenderNanos = System.nanoTime();

    public UiNotificationOverlay(UiNotificationConfig config) {
        this.config = config != null ? config : UiNotificationConfig.defaults();
    }

    public boolean hasActiveNotifications() {
        if (!config.enabled().getAsBoolean()) {
            return false;
        }
        if (config.hideWhenStreamproof().getAsBoolean() && StreamproofOverlay.shouldUseExternalOverlay()) {
            return false;
        }
        return !entries.isEmpty();
    }

    public synchronized void push(
        String title,
        String tag,
        String subtitle,
        UiIcon icon,
        UiNotificationKind kind,
        boolean dedupeToggle
    ) {
        if (!config.enabled().getAsBoolean()) {
            return;
        }
        float rawDur = config.durationSeconds().get();
        float dur = clamp(rawDur, 1.0f, 5.0f);
        long now = System.nanoTime();
        if (dedupeToggle) {
            entries.removeIf(it ->
                it.title.equals(title)
                    && (it.kind == UiNotificationKind.ENABLED || it.kind == UiNotificationKind.DISABLED)
                    && (kind == UiNotificationKind.ENABLED || kind == UiNotificationKind.DISABLED)
            );
        }
        while (entries.size() >= 4) {
            entries.remove(0);
        }
        entries.add(new Entry(nextId++, title, tag, subtitle, icon, kind, now, dur));
    }

    public void renderHud(GuiGraphicsExtractor graphics) {
        Minecraft mc = Minecraft.getInstance();
        Screen currentScreen = mc.gui.screen();
        if (config.suppressHudForScreen().test(currentScreen)) {
            return;
        }
        if (!config.enabled().getAsBoolean()) {
            entries.clear();
            return;
        }
        if (entries.isEmpty()) {
            return;
        }

        Window window = mc.getWindow();
        int guiW = window.getGuiScaledWidth();
        int guiH = window.getGuiScaledHeight();
        if (guiW <= 0 || guiH <= 0) {
            return;
        }

        if (StreamproofOverlay.shouldUseExternalOverlay()) {
            if (config.hideWhenStreamproof().getAsBoolean()) {
                return;
            }
            renderStreamproofHud(mc, graphics, guiW, guiH);
            return;
        }

        renderStack(graphics, guiW, guiH, false, 1f);
    }

    public void renderInsidePanel(GuiGraphicsExtractor graphics, float panelW, float panelH, float panelAlpha) {
        if (!config.enabled().getAsBoolean() || entries.isEmpty()) {
            return;
        }
        renderStack(graphics, panelW, panelH, true, panelAlpha);
    }

    private void renderStreamproofHud(Minecraft mc, GuiGraphicsExtractor graphics, int guiW, int guiH) {
        float boxW = 204f;
        float boxH = 176f;
        UiNotificationLayout layout = config.layout().get();
        float centerX = layout == UiNotificationLayout.CENTER ? guiW * 0.5f : guiW - 12f - boxW * 0.5f;
        float centerY = switch (layout) {
            case TOP_RIGHT -> 12f + boxH * 0.5f;
            case CENTER -> guiH - 46f - boxH * 0.5f;
            default -> guiH - 12f - boxH * 0.5f;
        };
        boolean captured = StreamproofOverlay.beginFrame(mc, guiW, guiH, centerX, centerY, 1.0f, boxW, boxH);
        if (!captured) {
            return;
        }
        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        try {
            renderStackInBox(
                graphics,
                boxW,
                boxH,
                layout == UiNotificationLayout.TOP_RIGHT,
                layout == UiNotificationLayout.CENTER
            );
        } finally {
            pose.popMatrix();
            StreamproofOverlay.endFrame();
        }
    }

    private void renderStackInBox(GuiGraphicsExtractor graphics, float boxW, float boxH, boolean topDown, boolean centered) {
        long now = System.nanoTime();
        float dt = clamp(Math.max(0L, now - lastRenderNanos) / 1_000_000_000f, 0.0005f, 0.05f);
        lastRenderNanos = now;

        boolean compact = config.layout().get() == UiNotificationLayout.COMPACT;
        float cardW = compact ? 158f : 188f;
        float cardH = compact ? 26f : 34f;
        float gap = 6f;

        List<Entry> expired = new ArrayList<>();
        List<Entry> snapshot = new ArrayList<>(entries);
        for (int idx = 0; idx < snapshot.size(); idx++) {
            Entry entry = snapshot.get(idx);
            float age = Math.max(0L, now - entry.createdNanos) / 1_000_000_000f;
            if (age >= entry.durationSec) {
                expired.add(entry);
                continue;
            }
            int reverseIdx = snapshot.size() - 1 - idx;
            float targetY = topDown ? 4f + reverseIdx * (cardH + gap) : boxH - 6f - cardH - reverseIdx * (cardH + gap);
            entry.animY = entry.animY < 0f ? targetY : UiRenderPrimitives.approachExp(entry.animY, targetY, 18f, dt);

            float outRem = Math.max(0f, entry.durationSec - age);
            float targetSlide = outRem < 0.24f ? clamp(outRem / 0.24f, 0f, 1f) : 1f;
            entry.animSlide = UiRenderPrimitives.approachExp(entry.animSlide, targetSlide, 18f, dt);

            float baseX = centered ? (boxW - cardW) * 0.5f : (boxW - cardW - 6f);
            float slideOffset = centered ? 0f : (1f - entry.animSlide) * 26f;
            drawCard(graphics, entry, baseX + slideOffset, entry.animY, cardW, cardH, age, entry.animSlide, compact);
        }
        if (!expired.isEmpty()) {
            entries.removeAll(expired);
        }
    }

    private void renderStack(GuiGraphicsExtractor graphics, float areaW, float areaH, boolean insidePanel, float masterAlpha) {
        long now = System.nanoTime();
        float dt = clamp(Math.max(0L, now - lastRenderNanos) / 1_000_000_000f, 0.0005f, 0.05f);
        lastRenderNanos = now;

        UiNotificationLayout layout = config.layout().get();
        boolean compact = layout == UiNotificationLayout.COMPACT || insidePanel;
        float cardW = insidePanel ? 162f : (compact ? 158f : 188f);
        float cardH = insidePanel ? 28f : (compact ? 26f : 34f);
        float gap = 6f;

        boolean topDown = !insidePanel && layout == UiNotificationLayout.TOP_RIGHT;
        boolean centered = !insidePanel && layout == UiNotificationLayout.CENTER;

        List<Entry> expired = new ArrayList<>();
        List<Entry> snapshot = new ArrayList<>(entries);
        for (int idx = 0; idx < snapshot.size(); idx++) {
            Entry entry = snapshot.get(idx);
            float age = Math.max(0L, now - entry.createdNanos) / 1_000_000_000f;
            if (age >= entry.durationSec) {
                expired.add(entry);
                continue;
            }
            int reverseIdx = snapshot.size() - 1 - idx;
            float marginY = insidePanel ? 12f : (centered ? 50f : 14f);
            float targetY = topDown ? marginY + reverseIdx * (cardH + gap) : areaH - marginY - cardH - reverseIdx * (cardH + gap);
            entry.animY = entry.animY < 0f ? targetY : UiRenderPrimitives.approachExp(entry.animY, targetY, 18f, dt);

            float outRem = Math.max(0f, entry.durationSec - age);
            float targetSlide = outRem < 0.24f ? clamp(outRem / 0.24f, 0f, 1f) : 1f;
            entry.animSlide = UiRenderPrimitives.approachExp(entry.animSlide, targetSlide, 18f, dt);

            float marginX = insidePanel ? 12f : 14f;
            float baseX = centered ? (areaW - cardW) * 0.5f : (areaW - cardW - marginX);
            float slideOffset = centered ? 0f : (1f - entry.animSlide) * 24f;
            float alpha = clamp(entry.animSlide * masterAlpha, 0f, 1f);
            drawCard(graphics, entry, baseX + slideOffset, entry.animY, cardW, cardH, age, alpha, compact);
        }
        if (!expired.isEmpty()) {
            entries.removeAll(expired);
        }
    }

    private void drawCard(
        GuiGraphicsExtractor graphics,
        Entry entry,
        float x,
        float y,
        float w,
        float h,
        float ageSec,
        float alpha,
        boolean compact
    ) {
        if (alpha <= 0.02f) {
            return;
        }
        UiTheme theme = config.theme().get();
        float r = Math.min(h * 0.45f, 10f * theme.cornerRadiusScale());
        int accent = theme.accentColor();
        int secondary = theme.secondaryAccentColor();
        int statusColor = switch (entry.kind) {
            case ENABLED, INFO -> accent;
            case DISABLED -> 0xFFFF6E7A;
            case ACTION -> secondary;
        };

        UiRenderPrimitives.drawRoundedShadow(graphics, x, y + 1.2f, w, h, r, 6f, UiRenderPrimitives.argb(Math.round(88f * alpha), 0, 0, 0));

        int topBg = theme.windowTopColor(clampInt(Math.round(228f * alpha), 0, 248));
        int botBg = theme.windowBottomColor(clampInt(Math.round(238f * alpha), 0, 252));
        int topRim = UiRenderPrimitives.lerpColor(
            UiRenderPrimitives.argb(Math.round(72f * alpha), 255, 255, 255),
            UiRenderPrimitives.multiplyAlpha(statusColor, 0.78f * alpha),
            entry.kind == UiNotificationKind.DISABLED ? 0.35f : 0.55f
        );
        int botRim = UiRenderPrimitives.argb(Math.round(28f * alpha), 255, 255, 255);
        UiRenderPrimitives.drawGlassCard(graphics, x, y, w, h, r, topBg, botBg, topRim, botRim);

        float badgeSz = compact ? 16f : 20f;
        float bx = x + 7f;
        float by = y + (h - badgeSz) * 0.5f - 0.8f;
        UiRenderPrimitives.drawRoundedRect(graphics, bx, by, badgeSz, badgeSz, badgeSz * 0.42f, UiRenderPrimitives.multiplyAlpha(statusColor, 0.24f * alpha));
        float iconSz = badgeSz * 0.56f;
        UiRenderPrimitives.drawIcon(graphics, entry.icon, bx + (badgeSz - iconSz) * 0.5f, by + (badgeSz - iconSz) * 0.5f, iconSz, UiRenderPrimitives.multiplyAlpha(statusColor, alpha));

        float textX = bx + badgeSz + 7f;
        float tagFont = compact ? 4.4f : 4.6f;
        float tagW = SatoshiFont.width(entry.tag, tagFont, SatoshiWeight.SEMIBOLD) + 8f;
        float tagH = compact ? 10f : 11f;
        float tagX = x + w - tagW - 7f;
        float tagY = compact ? y + (h - tagH) * 0.5f - 0.8f : y + 6.2f;

        UiRenderPrimitives.drawRoundedRect(graphics, tagX, tagY, tagW, tagH, tagH * 0.5f, UiRenderPrimitives.multiplyAlpha(statusColor, 0.26f * alpha));
        SatoshiFont.drawCentered(
            graphics, entry.tag, tagX + tagW * 0.5f, tagY + (tagH - tagFont) * 0.5f, tagFont,
            TextGradient.pearl(alpha, statusColor, 0.45f, theme), SatoshiWeight.SEMIBOLD
        );

        boolean useTyping = config.typingAnimation().getAsBoolean() && theme.typingAnimationEnabled();
        float maxTitleW = Math.max(40f, tagX - textX - 5f);
        float titleSize = compact ? 5.5f : 6.0f;
        float titleY = compact ? y + (h - titleSize) * 0.5f - 0.8f : y + 6.4f;

        if (useTyping) {
            KineticTextRenderer.drawKineticDecode(
                graphics, entry.title, textX, titleY, maxTitleW, titleSize,
                TextGradient.pearl(alpha, statusColor, 0.28f, theme), SatoshiWeight.SEMIBOLD,
                ageSec, statusColor, secondary, alpha, 0.22f, compact, true, theme
            );
        } else {
            String clipped = UiRenderPrimitives.ellipsize(entry.title, maxTitleW, titleSize, SatoshiWeight.SEMIBOLD);
            SatoshiFont.draw(graphics, clipped, textX, titleY, titleSize, TextGradient.pearl(alpha, statusColor, 0.28f, theme), SatoshiWeight.SEMIBOLD);
        }

        if (!compact) {
            float subSize = 4.6f;
            float subY = y + 18.6f;
            float maxSubW = Math.max(50f, w - (textX - x) - 8f);
            if (useTyping) {
                KineticTextRenderer.drawKineticDecode(
                    graphics, entry.subtitle, textX, subY, maxSubW, subSize,
                    TextGradient.mist(0.84f * alpha, theme), SatoshiWeight.MEDIUM,
                    Math.max(0f, ageSec - 0.04f), statusColor, secondary, alpha, 0.28f, true, true, theme
                );
            } else {
                String subText = UiRenderPrimitives.ellipsize(entry.subtitle, maxSubW, subSize, SatoshiWeight.MEDIUM);
                SatoshiFont.draw(graphics, subText, textX, subY, subSize, TextGradient.mist(0.84f * alpha, theme), SatoshiWeight.MEDIUM);
            }
        }

        float lifeFrac = clamp(1f - (ageSec / entry.durationSec), 0f, 1f);
        float barPad = 7f;
        float barW = Math.max(10f, w - barPad * 2f) * lifeFrac;
        if (barW > 1.2f) {
            UiRenderPrimitives.drawRoundedRect(graphics, x + barPad, y + h - 2.6f, barW, 1.4f, 0.7f, UiRenderPrimitives.multiplyAlpha(statusColor, 0.85f * alpha));
        }
    }

    private static float clamp(float val, float min, float max) {
        return Math.max(min, Math.min(max, val));
    }

    private static int clampInt(int val, int min, int max) {
        return Math.max(min, Math.min(max, val));
    }
}
