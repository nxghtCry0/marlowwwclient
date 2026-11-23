package com.eclipseware.imnotcheatingyouare.client.glassui.screen;

import com.eclipseware.imnotcheatingyouare.client.glassui.asset.LucideIcon;
import com.eclipseware.imnotcheatingyouare.client.glassui.model.UiDashboardAction;
import com.eclipseware.imnotcheatingyouare.client.glassui.model.UiDashboardProvider;
import com.eclipseware.imnotcheatingyouare.client.glassui.model.UiDashboardSnapshot;
import com.eclipseware.imnotcheatingyouare.client.glassui.model.UiHitRect;
import com.eclipseware.imnotcheatingyouare.client.glassui.model.UiStatCard;
import com.eclipseware.imnotcheatingyouare.client.glassui.render.KineticTextRenderer;
import com.eclipseware.imnotcheatingyouare.client.glassui.render.SatoshiFont;
import com.eclipseware.imnotcheatingyouare.client.glassui.render.SatoshiWeight;
import com.eclipseware.imnotcheatingyouare.client.glassui.render.TextGradient;
import com.eclipseware.imnotcheatingyouare.client.glassui.render.UiRenderPrimitives;
import com.eclipseware.imnotcheatingyouare.client.glassui.theme.UiTheme;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

public final class GlassUiDashboardView {
    private GlassUiDashboardView() {
    }

    public static void render(
        GuiGraphicsExtractor graphics,
        Minecraft minecraft,
        GlassUiConfig config,
        UiScreenState state,
        float lx,
        float ly,
        float animDt,
        float alpha,
        boolean interactive
    ) {
        UiDashboardProvider provider = config.dashboard();
        if (provider == null) {
            return;
        }
        UiDashboardSnapshot snapshot = provider.buildSnapshot(minecraft);
        UiTheme theme = config.theme();
        float rScale = theme.cornerRadiusScale();
        int accent = theme.accentColor();
        int secondary = theme.secondaryAccentColor();
        float cardR = 13f * rScale;
        float slideY = (1f - alpha) * 8f;

        float areaX = GlassUiChrome.contentAreaX(config, state);
        float areaW = config.panelWidth() - 72f;
        float headerY = 48f + slideY;
        float headerH = 54f;

        UiRenderPrimitives.drawThemedGlassCard(
            graphics,
            areaX,
            headerY,
            areaW,
            headerH,
            cardR,
            42f,
            24f,
            66f,
            28f,
            alpha,
            0.45f,
            0f,
            theme
        );

        UiHitRect backBtn = new UiHitRect(areaX + 10f, headerY + 16f, 48f, 22f);
        if (interactive) {
            state.dashboardBackHitbox = backBtn;
        }
        state.dashboardBackHover = UiRenderPrimitives.approachExp(
            state.dashboardBackHover,
            (interactive && backBtn.contains(lx, ly)) ? 1f : 0f,
            18f,
            animDt
        );
        UiRenderPrimitives.drawThemedGlassCard(
            graphics,
            backBtn.x(),
            backBtn.y(),
            backBtn.w(),
            backBtn.h(),
            Math.min(11f, 11f * rScale),
            34f + 28f * state.dashboardBackHover,
            20f + 16f * state.dashboardBackHover,
            56f + 34f * state.dashboardBackHover,
            26f + 16f * state.dashboardBackHover,
            alpha,
            0f,
            state.dashboardBackHover,
            theme
        );
        UiRenderPrimitives.drawIcon(
            graphics,
            LucideIcon.ARROW_LEFT,
            backBtn.x() + 7f,
            backBtn.y() + 6f,
            10f,
            UiRenderPrimitives.argb(Math.round(235f * alpha), 255, 255, 255)
        );
        SatoshiFont.draw(
            graphics,
            "Back",
            backBtn.x() + 20f,
            backBtn.y() + (backBtn.h() - 5.6f) * 0.5f,
            5.6f,
            TextGradient.pearl(0.95f * alpha, accent, 0.22f, theme),
            SatoshiWeight.SEMIBOLD
        );

        float avCx = areaX + 82f;
        float avCy = headerY + headerH * 0.5f;
        float avR = 16.5f;
        UiRenderPrimitives.drawRoundedShadow(
            graphics,
            avCx - avR,
            avCy - avR + 1f,
            avR * 2f,
            avR * 2f,
            avR,
            5.5f,
            UiRenderPrimitives.argb(Math.round(90f * alpha), 0, 0, 0)
        );
        Identifier customSkin = provider.avatarSkin(minecraft);
        Identifier skin = customSkin != null ? customSkin : GlassUiChrome.resolveDefaultPlayerSkin(minecraft);
        UiRenderPrimitives.drawCircularPlayerAvatar(
            graphics,
            skin,
            avCx,
            avCy,
            avR,
            UiRenderPrimitives.argb(Math.round(255f * alpha), 255, 255, 255)
        );
        UiRenderPrimitives.drawRoundedBorder(
            graphics,
            avCx - avR,
            avCy - avR,
            avR * 2f,
            avR * 2f,
            avR,
            1.4f,
            UiRenderPrimitives.multiplyAlpha(accent, 0.92f * alpha),
            UiRenderPrimitives.multiplyAlpha(secondary, 0.80f * alpha)
        );

        SatoshiFont.draw(
            graphics,
            snapshot.title(),
            areaX + 107f,
            headerY + 13.5f,
            8.4f,
            TextGradient.pearl(alpha, accent, 0.36f, theme),
            SatoshiWeight.SEMIBOLD
        );

        KineticTextRenderer.drawKineticDecode(
            graphics,
            snapshot.subtitle(),
            areaX + 107f,
            headerY + 29.5f,
            310f,
            5.0f,
            TextGradient.mist(0.84f * alpha, theme),
            SatoshiWeight.MEDIUM,
            state.elapsedSeconds - state.dashboardOpenTime,
            accent,
            secondary,
            alpha,
            0.34f,
            true,
            theme.typingAnimationEnabled(),
            theme
        );

        String badgeText = snapshot.badgeText();
        if (badgeText != null && !badgeText.isEmpty()) {
            float pillFont = 5.1f;
            float pillW = SatoshiFont.width(badgeText, pillFont, SatoshiWeight.SEMIBOLD) + 18f;
            float pillH = 20f;
            float pillX = areaX + areaW - pillW - 12f;
            float pillY = headerY + (headerH - pillH) * 0.5f;

            UiRenderPrimitives.drawThemedGlassCard(
                graphics,
                pillX,
                pillY,
                pillW,
                pillH,
                pillH * 0.5f,
                38f,
                22f,
                64f,
                28f,
                alpha,
                0.65f,
                0f,
                theme
            );
            SatoshiFont.drawCentered(
                graphics,
                badgeText,
                pillX + pillW * 0.5f,
                pillY + (pillH - pillFont) * 0.5f,
                pillFont,
                TextGradient.accent(0.98f * alpha, accent, theme),
                SatoshiWeight.SEMIBOLD
            );
        }

        float gridY = headerY + headerH + 10f;
        List<UiStatCard> stats = snapshot.stats();
        int cols = (stats.size() == 1 || stats.size() == 2) ? stats.size() : 3;
        float colGap = 10f;
        float rowGap = 10f;
        float statW = (areaW - colGap * (cols - 1)) / cols;
        float statH = 102f;

        for (int idx = 0; idx < stats.size(); idx++) {
            UiStatCard st = stats.get(idx);
            int col = idx % cols;
            int rIdx = idx / cols;
            float sx = areaX + col * (statW + colGap);
            float sy = gridY + rIdx * (statH + rowGap);
            float sRad = 12f * rScale;
            float barFill = clamp(st.progress(), 0f, 1f);

            UiRenderPrimitives.drawThemedGlassCard(
                graphics,
                sx,
                sy,
                statW,
                statH,
                sRad,
                32f,
                19f,
                50f,
                22f,
                alpha,
                barFill * 0.45f,
                0f,
                theme
            );

            UiRenderPrimitives.drawIconBadge(graphics, st.icon(), sx + 11f, sy + 11f, 20f, 0.55f, alpha, theme);
            SatoshiFont.draw(graphics, st.label(), sx + 37f, sy + 16.5f, 5.7f, TextGradient.silver(0.88f * alpha, theme), SatoshiWeight.MEDIUM);
            SatoshiFont.draw(graphics, st.value(), sx + 12f, sy + 44f, 9.2f, TextGradient.pearl(alpha, accent, 0.34f, theme), SatoshiWeight.SEMIBOLD);
            SatoshiFont.draw(
                graphics,
                UiRenderPrimitives.ellipsize(st.detail(), statW - 24f, 4.8f, SatoshiWeight.MEDIUM),
                sx + 12f,
                sy + 66f,
                4.8f,
                TextGradient.mist(0.76f * alpha, theme),
                SatoshiWeight.MEDIUM
            );

            float barX = sx + 12f;
            float barW = statW - 24f;
            float barY = sy + statH - 11f;
            UiRenderPrimitives.drawRoundedRect(graphics, barX, barY, barW, 3.5f, 1.75f, UiRenderPrimitives.argb(Math.round(38f * alpha), 255, 255, 255));
            int fillTint = UiRenderPrimitives.lerpColor(accent, secondary, barFill * 0.6f);
            UiRenderPrimitives.drawRoundedRect(graphics, barX, barY, Math.max(3.5f, barW * barFill), 3.5f, 1.75f, UiRenderPrimitives.multiplyAlpha(fillTint, 0.92f * alpha));
        }

        List<UiDashboardAction> buttons = snapshot.actions();
        if (buttons.isEmpty()) {
            return;
        }

        int statRows = stats.isEmpty() ? 0 : (stats.size() + cols - 1) / cols;
        float actionBarY = gridY + statRows * statH + Math.max(0, statRows - 1) * rowGap + 12f;
        float actionH = 44f;
        float btnGap = 9f;
        float btnW = (areaW - btnGap * (buttons.size() - 1)) / buttons.size();

        for (int i = 0; i < buttons.size(); i++) {
            UiDashboardAction b = buttons.get(i);
            float bx = areaX + i * (btnW + btnGap);
            UiHitRect hit = new UiHitRect(bx, actionBarY, btnW, actionH);
            if (interactive) {
                state.dashboardActionHitboxes.add(new UiScreenState.ActionHit(b, hit));
            }
            boolean hov = interactive && hit.contains(lx, ly);
            float prevHv = state.dashboardBtnHovers.getOrDefault(i, 0f);
            float hv = UiRenderPrimitives.approachExp(prevHv, hov ? 1f : 0f, 18f, animDt);
            state.dashboardBtnHovers.put(i, hv);
            float br = Math.min(11f, 11f * rScale);

            if (b.danger()) {
                int dangerColor = 0xFFFF6470;
                int topCol = UiRenderPrimitives.lerpColor(
                    UiRenderPrimitives.argb(Math.round(34f * alpha), 255, 110, 120),
                    UiRenderPrimitives.multiplyAlpha(dangerColor, 0.42f * alpha),
                    hv
                );
                int botCol = UiRenderPrimitives.argb(Math.round((20f + 16f * hv) * alpha), 180, 50, 60);
                int rimCol = UiRenderPrimitives.multiplyAlpha(dangerColor, (0.55f + 0.35f * hv) * alpha);
                UiRenderPrimitives.drawGlassCard(graphics, bx, actionBarY, btnW, actionH, br, topCol, botCol, rimCol, botCol);
            } else {
                UiRenderPrimitives.drawThemedGlassCard(
                    graphics,
                    bx,
                    actionBarY,
                    btnW,
                    actionH,
                    br,
                    32f + 22f * hv,
                    18f + 14f * hv,
                    50f + 32f * hv,
                    22f + 14f * hv,
                    alpha,
                    b.active() ? 0.85f : hv * 0.45f,
                    hv,
                    theme
                );
            }

            int icTint;
            if (b.danger()) {
                icTint = UiRenderPrimitives.argb(Math.round(240f * alpha), 255, 130, 138);
            } else if (b.active()) {
                icTint = UiRenderPrimitives.multiplyAlpha(accent, alpha);
            } else {
                icTint = UiRenderPrimitives.argb(Math.round((200f + 50f * hv) * alpha), 255, 255, 255);
            }
            UiRenderPrimitives.drawIcon(graphics, b.icon(), bx + 10f, actionBarY + 16f, 12f, icTint);

            SatoshiFont.draw(
                graphics,
                b.title(),
                bx + 26.5f,
                actionBarY + 12f,
                5.6f,
                TextGradient.pearl(alpha, b.danger() ? 0xFFFF6470 : accent, 0.28f, theme),
                SatoshiWeight.SEMIBOLD
            );
            SatoshiFont.draw(
                graphics,
                UiRenderPrimitives.ellipsize(b.subtitle(), btnW - 32f, 4.5f, SatoshiWeight.MEDIUM),
                bx + 26.5f,
                actionBarY + 24.5f,
                4.5f,
                TextGradient.mist(0.78f * alpha, theme),
                SatoshiWeight.MEDIUM
            );
        }
    }

    private static float clamp(float val, float min, float max) {
        return Math.max(min, Math.min(max, val));
    }
}
