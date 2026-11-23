package com.eclipseware.imnotcheatingyouare.client.glassui.screen;

import com.eclipseware.imnotcheatingyouare.client.glassui.asset.LucideIcon;
import com.eclipseware.imnotcheatingyouare.client.glassui.model.UiCard;
import com.eclipseware.imnotcheatingyouare.client.glassui.model.UiCatalog;
import com.eclipseware.imnotcheatingyouare.client.glassui.model.UiChoice;
import com.eclipseware.imnotcheatingyouare.client.glassui.model.UiControl;
import com.eclipseware.imnotcheatingyouare.client.glassui.model.UiCustomSection;
import com.eclipseware.imnotcheatingyouare.client.glassui.model.UiHeaderAccessory;
import com.eclipseware.imnotcheatingyouare.client.glassui.model.UiHitRect;
import com.eclipseware.imnotcheatingyouare.client.glassui.model.UiScreenActions;
import com.eclipseware.imnotcheatingyouare.client.glassui.model.UiSectionMeasureContext;
import com.eclipseware.imnotcheatingyouare.client.glassui.model.UiSectionRenderContext;
import com.eclipseware.imnotcheatingyouare.client.glassui.render.KineticTextRenderer;
import com.eclipseware.imnotcheatingyouare.client.glassui.render.SatoshiFont;
import com.eclipseware.imnotcheatingyouare.client.glassui.render.SatoshiWeight;
import com.eclipseware.imnotcheatingyouare.client.glassui.render.TextGradient;
import com.eclipseware.imnotcheatingyouare.client.glassui.render.UiRenderPrimitives;
import com.eclipseware.imnotcheatingyouare.client.glassui.theme.UiTheme;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class GlassUiDetailView {
    private GlassUiDetailView() {
    }

    public static void render(
        GuiGraphicsExtractor graphics,
        GlassUiConfig config,
        UiScreenState state,
        UiScreenActions actions,
        String cardId,
        float lx,
        float ly,
        float animDt,
        float alpha,
        boolean interactive
    ) {
        UiCatalog catalog = config.catalog();
        UiCard card = catalog.findCard(cardId);
        if (card == null) {
            return;
        }
        UiTheme theme = config.theme();
        float rScale = theme.cornerRadiusScale();
        int accent = theme.accentColor();
        int secondary = theme.secondaryAccentColor();
        float cardR = 12.5f * rScale;
        float slideY = (1f - alpha) * 8f;

        float areaX = GlassUiChrome.contentAreaX(config, state);
        float areaW = config.panelWidth() - 72f;
        float headerY = 46f + slideY;
        float headerH = 38f;

        boolean enabledNow = card.toggleable() && card.enabled();
        state.detailMasterToggleAnim = UiRenderPrimitives.approachExp(
            state.detailMasterToggleAnim,
            enabledNow ? 1f : 0f,
            16f,
            animDt
        );

        UiRenderPrimitives.drawThemedGlassCard(
            graphics,
            areaX,
            headerY,
            areaW,
            headerH,
            cardR,
            40f,
            23f,
            62f,
            26f,
            alpha,
            state.detailMasterToggleAnim * 0.7f,
            0f,
            theme
        );

        UiHitRect backBtn = new UiHitRect(areaX + 8f, headerY + 8.5f, 46f, 21f);
        if (interactive) {
            state.detailBackHitbox = backBtn;
        }
        state.detailBackHover = UiRenderPrimitives.approachExp(
            state.detailBackHover,
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
            Math.min(10.5f, 10.5f * rScale),
            34f + 28f * state.detailBackHover,
            20f + 16f * state.detailBackHover,
            56f + 34f * state.detailBackHover,
            26f + 16f * state.detailBackHover,
            alpha,
            0f,
            state.detailBackHover,
            theme
        );
        UiRenderPrimitives.drawIcon(
            graphics,
            LucideIcon.ARROW_LEFT,
            backBtn.x() + 6.5f,
            backBtn.y() + 5.5f,
            10f,
            UiRenderPrimitives.argb(Math.round(235f * alpha), 255, 255, 255)
        );
        SatoshiFont.draw(
            graphics,
            "Back",
            backBtn.x() + 19f,
            backBtn.y() + (backBtn.h() - 5.5f) * 0.5f,
            5.5f,
            TextGradient.pearl(0.95f * alpha, accent, 0.22f, theme),
            SatoshiWeight.SEMIBOLD
        );

        UiRenderPrimitives.drawIcon(graphics, card.icon(), areaX + 63f, headerY + 12f, 14f, UiRenderPrimitives.multiplyAlpha(accent, alpha));
        SatoshiFont.draw(graphics, card.title(), areaX + 82f, headerY + 8.6f, 7.4f, TextGradient.pearl(alpha, accent, 0.32f, theme), SatoshiWeight.SEMIBOLD);

        KineticTextRenderer.drawKineticDecode(
            graphics,
            card.summary(),
            areaX + 82f,
            headerY + 21.6f,
            areaW - 220f,
            4.75f,
            TextGradient.mist(0.82f * alpha, theme),
            SatoshiWeight.MEDIUM,
            state.elapsedSeconds - state.detailOpenTime,
            accent,
            secondary,
            alpha,
            0.34f,
            true,
            theme.typingAnimationEnabled(),
            theme
        );

        float rightControlEdge = areaX + areaW - 10f;
        UiHeaderAccessory accessory = card.headerAccessory();
        if (accessory != null) {
            float consumed = accessory.render(graphics, areaX + areaW - 10f, headerY, headerH, alpha, theme);
            rightControlEdge = areaX + areaW - 10f - consumed;
        } else if (card.onAction() != null && !card.toggleable()) {
            float runW = Math.max(38f, SatoshiFont.width(card.actionLabel(), 5.4f, SatoshiWeight.SEMIBOLD) + 16f);
            UiHitRect runBtn = new UiHitRect(areaX + areaW - 10f - runW, headerY + 9.5f, runW, 19f);
            if (interactive) {
                state.detailMasterHitbox = runBtn;
            }
            state.detailMasterHover = UiRenderPrimitives.approachExp(
                state.detailMasterHover,
                (interactive && runBtn.contains(lx, ly)) ? 1f : 0f,
                16f,
                animDt
            );
            UiRenderPrimitives.drawGlassCard(
                graphics,
                runBtn.x(),
                runBtn.y(),
                runBtn.w(),
                runBtn.h(),
                Math.min(9.5f, 9.5f * rScale),
                UiRenderPrimitives.multiplyAlpha(accent, (0.35f + 0.25f * state.detailMasterHover) * alpha),
                UiRenderPrimitives.multiplyAlpha(accent, (0.22f + 0.18f * state.detailMasterHover) * alpha),
                UiRenderPrimitives.multiplyAlpha(accent, 0.85f * alpha),
                UiRenderPrimitives.multiplyAlpha(accent, 0.45f * alpha)
            );
            SatoshiFont.drawCentered(
                graphics,
                card.actionLabel(),
                runBtn.x() + runBtn.w() * 0.5f,
                runBtn.y() + (runBtn.h() - 5.4f) * 0.5f,
                5.4f,
                TextGradient.pearl(alpha, accent, 0.2f, theme),
                SatoshiWeight.SEMIBOLD
            );
            if (card.bindable() && config.keybinds() != null) {
                GlassUiCards.renderKeybindPill(graphics, config, state, cardId, runBtn.x() - 7f, headerY + 13.5f, lx, ly, 1f, alpha, true, interactive);
                rightControlEdge = runBtn.x() - 40f;
            } else {
                rightControlEdge = runBtn.x() - 7f;
            }
        } else if (card.toggleable()) {
            float swX = areaX + areaW - 35f;
            float swY = headerY + 12.5f;
            UiHitRect mHit = new UiHitRect(swX - 4f, swY - 4f, 34f, 21f);
            if (interactive) {
                state.detailMasterHitbox = mHit;
            }
            state.detailMasterHover = UiRenderPrimitives.approachExp(
                state.detailMasterHover,
                (interactive && mHit.contains(lx, ly)) ? 1f : 0f,
                16f,
                animDt
            );
            UiRenderPrimitives.drawSwitch(graphics, swX, swY, 25f, 13.5f, state.detailMasterToggleAnim, state.detailMasterHover, alpha, theme);
            if (card.bindable() && config.keybinds() != null) {
                GlassUiCards.renderKeybindPill(graphics, config, state, cardId, swX - 7f, headerY + 13.5f, lx, ly, 1f, alpha, true, interactive);
                rightControlEdge = swX - 40f;
            } else {
                rightControlEdge = swX - 7f;
            }
        } else if (card.bindable() && config.keybinds() != null) {
            GlassUiCards.renderKeybindPill(graphics, config, state, cardId, areaX + areaW - 10f, headerY + 13.5f, lx, ly, 1f, alpha, true, interactive);
            rightControlEdge = areaX + areaW - 44f;
        }

        List<UiControl> settings = card.visibleControls();
        if (!settings.isEmpty()) {
            String countTxt = settings.size() + " Options";
            float countW = SatoshiFont.width(countTxt, 4.6f, SatoshiWeight.SEMIBOLD) + 12f;
            float countX = rightControlEdge - countW;
            float countY = headerY + 13.2f;
            if (countX > areaX + 260f) {
                UiRenderPrimitives.drawRoundedRect(graphics, countX, countY, countW, 11.5f, 5.75f, UiRenderPrimitives.argb(Math.round(28f * alpha), 255, 255, 255));
                SatoshiFont.drawCentered(graphics, countTxt, countX + countW * 0.5f, countY + (11.5f - 4.6f) * 0.5f, 4.6f, TextGradient.silver(0.84f * alpha, theme), SatoshiWeight.SEMIBOLD);
            }
        }

        float contentStartY = headerY + headerH + 6f;
        List<UiChoice> choices = card.choices();
        if (!choices.isEmpty()) {
            float modeBarH = 36f;
            UiRenderPrimitives.drawThemedGlassCard(graphics, areaX, contentStartY, areaW, modeBarH, 10.5f * rScale, 28f, 17f, 46f, 20f, alpha, 0f, 0f, theme);
            UiRenderPrimitives.drawIcon(graphics, LucideIcon.LAYERS, areaX + 10f, contentStartY + 8.5f, 9.0f, UiRenderPrimitives.multiplyAlpha(accent, 0.90f * alpha));
            SatoshiFont.draw(graphics, "Mode", areaX + 22f, contentStartY + 10.0f, 5.6f, TextGradient.silver(0.94f * alpha, theme), SatoshiWeight.SEMIBOLD);

            float segAreaX = areaX + 58f;
            float segAreaW = areaW - 67f;
            float gap = 7f;
            int count = choices.size();
            float segW = (segAreaW - gap * (count - 1)) / count;
            float segH = 17.5f;
            float segY = contentStartY + 4.2f;

            int hoveredChoiceIdx = -1;
            for (int idx = 0; idx < count; idx++) {
                UiChoice ch = choices.get(idx);
                float sx = segAreaX + idx * (segW + gap);
                UiHitRect sHit = new UiHitRect(sx, segY, segW, segH);
                if (interactive) {
                    state.detailModeHitboxes.add(new UiScreenState.DetailModeHit(cardId, idx, sHit));
                }
                boolean selected = card.selectedChoice() == idx;
                boolean hov = interactive && sHit.contains(lx, ly);
                if (hov) {
                    hoveredChoiceIdx = idx;
                }
                ch.animSelected = UiRenderPrimitives.approachExp(ch.animSelected, selected ? 1f : 0f, 18f, animDt);
                ch.animHover = UiRenderPrimitives.approachExp(ch.animHover, hov ? 1f : 0f, 18f, animDt);

                int fill = UiRenderPrimitives.lerpColor(
                    UiRenderPrimitives.argb(Math.round((24f + 22f * ch.animHover) * alpha), 255, 255, 255),
                    UiRenderPrimitives.argb(Math.round(248f * alpha), 255, 255, 255),
                    ch.animSelected
                );
                int border = UiRenderPrimitives.lerpColor(
                    UiRenderPrimitives.argb(Math.round(46f * alpha), 255, 255, 255),
                    UiRenderPrimitives.argb(Math.round(255f * alpha), 255, 255, 255),
                    ch.animSelected
                );
                float pillR = Math.min(8.75f, 8.75f * rScale);
                UiRenderPrimitives.drawRoundedRect(graphics, sx, segY, segW, segH, pillR, fill);
                UiRenderPrimitives.drawRoundedBorder(graphics, sx, segY, segW, segH, pillR, 0.85f, border, border);

                float iconSz = 8.6f;
                float lblSize = 5.3f;
                String lbl = UiRenderPrimitives.ellipsize(ch.label(), segW - 22f, lblSize, SatoshiWeight.SEMIBOLD);
                float lblW = SatoshiFont.width(lbl, lblSize, SatoshiWeight.SEMIBOLD);
                float groupW = iconSz + 4.5f + lblW;
                float groupStartX = sx + (segW - groupW) * 0.5f;

                int chIconTint = UiRenderPrimitives.lerpColor(
                    UiRenderPrimitives.argb(Math.round((195f + 50f * ch.animHover) * alpha), 255, 255, 255),
                    UiRenderPrimitives.argb(Math.round(255f * alpha), 30, 28, 26),
                    ch.animSelected
                );
                UiRenderPrimitives.drawIcon(graphics, ch.icon(), groupStartX, segY + (segH - iconSz) * 0.5f, iconSz, chIconTint);

                TextGradient textGrad;
                if (ch.animSelected > 0.55f) {
                    TextGradient darkGrad = new TextGradient(0xFF201E1C, 0xFF2B2825, 0xFF35312E, 0xFF201E1C);
                    textGrad = darkGrad.withOpacity(alpha);
                } else {
                    textGrad = TextGradient.pearl((0.80f + 0.20f * ch.animHover) * alpha, accent, 0.22f, theme);
                }
                SatoshiFont.draw(graphics, lbl, groupStartX + iconSz + 4.5f, segY + (segH - lblSize) * 0.5f, lblSize, textGrad, SatoshiWeight.SEMIBOLD);
            }

            int clampedSel = Math.max(0, Math.min(choices.size() - 1, card.selectedChoice()));
            int explainedIdx = hoveredChoiceIdx >= 0 ? hoveredChoiceIdx : clampedSel;
            UiChoice explained = choices.get(explainedIdx);
            if (!explained.description().isEmpty()) {
                String modeKey = "fs_" + cardId + "_" + explainedIdx;
                if (!modeKey.equals(state.lastHoveredModeKey)) {
                    state.lastHoveredModeKey = modeKey;
                    state.modeHoverStartSec = state.elapsedSeconds;
                }
                UiRenderPrimitives.drawIcon(graphics, explained.icon(), areaX + 59f, contentStartY + 24.4f, 7.6f, UiRenderPrimitives.multiplyAlpha(accent, 0.88f * alpha));
                KineticTextRenderer.drawKineticDecode(
                    graphics,
                    explained.label() + ": " + explained.description(),
                    areaX + 69.5f,
                    contentStartY + 25.6f,
                    segAreaW - 14f,
                    4.5f,
                    TextGradient.mist(0.84f * alpha, theme),
                    SatoshiWeight.MEDIUM,
                    state.elapsedSeconds - state.modeHoverStartSec,
                    accent,
                    secondary,
                    alpha,
                    0.28f,
                    true,
                    theme.typingAnimationEnabled(),
                    theme
                );
            }

            contentStartY += modeBarH + 6f;
        }

        UiCustomSection customSection = card.customSection();
        if (settings.isEmpty() && customSection == null) {
            state.detailScroller.updateBounds(0f, 100f, 100f);
            String emptyMsg = !choices.isEmpty()
                ? ("All options for " + card.title() + " are controlled via the mode bar above.")
                : ("No additional options for " + card.title() + ".");
            SatoshiFont.drawCentered(
                graphics,
                emptyMsg,
                areaX + areaW * 0.5f,
                contentStartY + 64f,
                6.0f,
                TextGradient.mist(0.74f * alpha, theme),
                SatoshiWeight.MEDIUM
            );
            return;
        }

        float gridBottom = config.panelHeight() - 12f;
        float viewportH = Math.max(40f, gridBottom - contentStartY);
        float colGap = 8f;
        float cellW = (areaW - colGap) * 0.5f;
        float cellH = 39f;
        float rowGap = 5.5f;
        int totalRows = (settings.size() + 1) / 2;
        float settingsH = totalRows * cellH + Math.max(0, totalRows - 1) * rowGap;
        float customExtraH = customSection != null
            ? customSection.measureHeight(new UiSectionMeasureContext(areaW, theme)) + (settings.isEmpty() ? 0f : 10f)
            : 0f;
        float totalContentH = settingsH + customExtraH;
        state.detailScroller.updateBounds(Math.max(0f, totalContentH - viewportH), viewportH, totalContentH);

        float scrollOff = state.detailScroller.visualOffset();

        UiRenderPrimitives.enableScissor(graphics, (int) areaX - 2, (int) contentStartY, (int) (areaX + areaW + 10f), (int) gridBottom);

        for (int idx = 0; idx < settings.size(); idx++) {
            UiControl row = settings.get(idx);
            int col = idx % 2;
            int rIdx = idx / 2;
            float accordion = state.detailScroller.accordionShift(rIdx, totalRows);
            float lag = clamp(-state.detailScroller.velocity() * 0.0036f * (rIdx + 1), -2.5f, 2.5f);
            float cx = areaX + col * (cellW + colGap);
            float cy = contentStartY + rIdx * (cellH + rowGap) - scrollOff + accordion + lag;
            if (cy + cellH >= contentStartY - 6f && cy <= gridBottom + 6f) {
                float edgeFade = state.detailScroller.edgeFadeAlpha(cy, cellH, contentStartY, gridBottom);
                renderSettingCell(graphics, config, state, row, cx, cy, cellW, cellH, 10f * rScale, lx, ly, animDt, alpha * edgeFade, interactive);
            }
        }

        if (customSection != null) {
            float sectionStartY = contentStartY + settingsH + (!settings.isEmpty() ? 10f : 0f) - scrollOff;
            customSection.render(
                new UiSectionRenderContext(
                    graphics,
                    state.detailScroller,
                    areaX,
                    areaW,
                    sectionStartY,
                    contentStartY,
                    gridBottom,
                    lx,
                    ly,
                    animDt,
                    alpha,
                    interactive,
                    actions
                )
            );
        }

        if (state.detailScroller.maxScroll() > 2f) {
            if (state.detailScroller.current() > 3f) {
                UiRenderPrimitives.drawRoundedGradient(
                    graphics,
                    areaX + 10f,
                    contentStartY,
                    areaW - 20f,
                    2.5f,
                    1.25f,
                    UiRenderPrimitives.multiplyAlpha(accent, 0.35f * alpha),
                    UiRenderPrimitives.argb(0, 255, 255, 255)
                );
            }
            if (state.detailScroller.maxScroll() - state.detailScroller.current() > 3f) {
                UiRenderPrimitives.drawRoundedGradient(
                    graphics,
                    areaX + 10f,
                    gridBottom - 2.5f,
                    areaW - 20f,
                    2.5f,
                    1.25f,
                    UiRenderPrimitives.argb(0, 255, 255, 255),
                    UiRenderPrimitives.multiplyAlpha(accent, 0.42f * alpha)
                );
            }
        }

        state.detailScroller.drawScrollbar(
            graphics,
            areaX + areaW + 3.8f,
            contentStartY,
            gridBottom,
            lx,
            ly,
            animDt,
            alpha,
            interactive,
            theme
        );

        UiRenderPrimitives.disableScissor(graphics);
    }

    private static void renderSettingCell(
        GuiGraphicsExtractor graphics,
        GlassUiConfig config,
        UiScreenState state,
        UiControl row,
        float x,
        float y,
        float w,
        float h,
        float radius,
        float lx,
        float ly,
        float animDt,
        float alpha,
        boolean interactive
    ) {
        UiHitRect hit = new UiHitRect(x, y, w, h);
        boolean hov = interactive && hit.contains(lx, ly);
        row.animHover = UiRenderPrimitives.approachExp(row.animHover, hov ? 1f : 0f, 16f, animDt);

        float activeAmt;
        if (row instanceof UiControl.Toggle t) {
            activeAmt = t.animToggle * 0.65f;
        } else if (row instanceof UiControl.Slider s) {
            activeAmt = s.animFocus * 0.45f;
        } else if (row instanceof UiControl.Action a) {
            activeAmt = a.animBtnHover * 0.45f;
        } else if (row instanceof UiControl.Cycle c) {
            activeAmt = c.animBtnHover * 0.45f;
        } else if (row instanceof UiControl.RangeSlider r) {
            activeAmt = r.animFocus * 0.45f;
        } else if (row instanceof UiControl.ColorPicker cp) {
            activeAmt = cp.animFocus * 0.45f;
        } else if (row instanceof UiControl.TextField tf) {
            activeAmt = tf.animFocus * 0.55f;
        } else {
            activeAmt = 0f;
        }

        UiRenderPrimitives.drawThemedGlassCard(
            graphics,
            x,
            y,
            w,
            h,
            radius,
            31f + 11f * row.animHover,
            18f + 7f * row.animHover,
            46f + 22f * row.animHover,
            20f + 10f * row.animHover,
            alpha,
            activeAmt,
            row.animHover,
            config.theme()
        );

        GlassUiCards.renderInlineControlRow(graphics, config, state, row, x, y, w, h, lx, ly, animDt, alpha, interactive, false);
    }

    private static float clamp(float val, float min, float max) {
        return Math.max(min, Math.min(max, val));
    }
}
