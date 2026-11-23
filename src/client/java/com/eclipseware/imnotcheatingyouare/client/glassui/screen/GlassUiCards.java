package com.eclipseware.imnotcheatingyouare.client.glassui.screen;

import com.eclipseware.imnotcheatingyouare.client.glassui.bind.UiInputBind;
import com.eclipseware.imnotcheatingyouare.client.glassui.bind.UiKeybindProvider;
import com.eclipseware.imnotcheatingyouare.client.glassui.model.UiCard;
import com.eclipseware.imnotcheatingyouare.client.glassui.model.UiCatalog;
import com.eclipseware.imnotcheatingyouare.client.glassui.model.UiCategory;
import com.eclipseware.imnotcheatingyouare.client.glassui.model.UiChoice;
import com.eclipseware.imnotcheatingyouare.client.glassui.model.UiControl;
import com.eclipseware.imnotcheatingyouare.client.glassui.model.UiCustomSection;
import com.eclipseware.imnotcheatingyouare.client.glassui.model.UiHitRect;
import com.eclipseware.imnotcheatingyouare.client.glassui.model.UiScreenActions;
import com.eclipseware.imnotcheatingyouare.client.glassui.model.UiSectionMeasureContext;
import com.eclipseware.imnotcheatingyouare.client.glassui.model.UiSectionRenderContext;
import com.eclipseware.imnotcheatingyouare.client.glassui.model.UiTab;
import com.eclipseware.imnotcheatingyouare.client.glassui.render.KineticTextRenderer;
import com.eclipseware.imnotcheatingyouare.client.glassui.render.SatoshiFont;
import com.eclipseware.imnotcheatingyouare.client.glassui.render.SatoshiWeight;
import com.eclipseware.imnotcheatingyouare.client.glassui.render.TextGradient;
import com.eclipseware.imnotcheatingyouare.client.glassui.render.UiRenderPrimitives;
import com.eclipseware.imnotcheatingyouare.client.glassui.theme.UiTheme;
import java.awt.Color;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class GlassUiCards {
    private GlassUiCards() {
    }

    public static void renderActiveTab(
        GuiGraphicsExtractor graphics,
        GlassUiConfig config,
        UiScreenState state,
        UiScreenActions actions,
        int selectedCategoryIndex,
        int selectedTabIndex,
        float lx,
        float ly,
        float animDt,
        float panelAlpha,
        boolean interactive
    ) {
        UiCatalog catalog = config.catalog();
        List<UiCategory> categories = catalog.categories();
        if (selectedCategoryIndex < 0 || selectedCategoryIndex >= categories.size()) {
            return;
        }
        UiCategory category = categories.get(selectedCategoryIndex);
        List<UiTab> tabs = category.tabs();
        if (tabs.isEmpty()) {
            return;
        }
        int clampedTab = Math.max(0, Math.min(tabs.size() - 1, selectedTabIndex));
        UiTab tab = tabs.get(clampedTab);
        UiTheme theme = config.theme();
        float rScale = theme.cornerRadiusScale();
        float cardR = 13f * rScale;

        float areaX = GlassUiChrome.contentAreaX(config, state);
        float areaW = config.panelWidth() - 72f;
        float topY = 48f;
        float bottomY = config.panelHeight() - 12f;
        float viewportH = bottomY - topY;

        float colGap = 10f;
        float cardW = (areaW - colGap) * 0.5f;
        float cardH = 104f;
        float rowGap = 10f;

        List<UiCard> cards = tab.cards();
        UiCustomSection customSection = tab.customSection();
        int totalRows = (cards.size() + 1) / 2;
        float totalH = totalRows * cardH + Math.max(0, totalRows - 1) * rowGap;
        float customExtra = customSection != null
            ? customSection.measureHeight(new UiSectionMeasureContext(areaW, theme)) + (cards.isEmpty() ? 0f : 10f)
            : 0f;
        String tabHint = tab.footerHint();
        String footerText = customSection == null ? (tabHint != null ? tabHint : config.defaultFooterHint()) : null;
        float hintFoot = customSection != null ? 14f : ((footerText != null && !footerText.isEmpty()) ? 18f : 6f);
        float totalContentH = totalH + customExtra + hintFoot;
        state.subtabScroller.updateBounds(Math.max(0f, totalContentH - viewportH), viewportH, totalContentH);

        float scrollOff = state.subtabScroller.visualOffset();

        for (int idx = 0; idx < cards.size(); idx++) {
            UiCard card = cards.get(idx);
            int col = idx % 2;
            int row = idx / 2;
            float cm = cascadeFactor(state.contentCascadeProgress, idx);
            float accordion = state.subtabScroller.accordionShift(row, totalRows);
            float lag = clamp(-state.subtabScroller.velocity() * 0.0038f * (row + 1), -3f, 3f);
            float mx = areaX + col * (cardW + colGap);
            float my = topY + row * (cardH + rowGap) - scrollOff + accordion + lag + (1f - cm) * 8f;
            float edgeFade = state.subtabScroller.edgeFadeAlpha(my, cardH, topY, bottomY);
            float mAlpha = panelAlpha * cm * edgeFade;
            if (my + cardH >= topY - 6f && my <= bottomY + 6f) {
                renderCard(graphics, config, state, card, mx, my, cardW, cardH, cardR, lx, ly, animDt, mAlpha, interactive);
            }
        }

        if (customSection != null) {
            float cm = cascadeFactor(state.contentCascadeProgress, cards.size());
            float gapAbove = cards.isEmpty() ? 0f : 10f;
            float sectionStartY = topY + totalH + gapAbove - scrollOff + (1f - cm) * 8f;
            customSection.render(
                new UiSectionRenderContext(
                    graphics,
                    state.subtabScroller,
                    areaX,
                    areaW,
                    sectionStartY,
                    topY,
                    bottomY,
                    lx,
                    ly,
                    animDt,
                    panelAlpha * cm,
                    interactive,
                    actions
                )
            );
        } else if (footerText != null && !footerText.isEmpty()) {
            float hintY = topY + totalH + 10f - scrollOff + state.subtabScroller.accordionShift(totalRows, totalRows + 1);
            if (hintY >= topY && hintY <= bottomY - 6f) {
                float cHint = cascadeFactor(state.contentCascadeProgress, cards.size());
                SatoshiFont.draw(
                    graphics,
                    footerText,
                    areaX + 4f,
                    hintY,
                    4.8f,
                    TextGradient.mist(0.56f * panelAlpha * cHint, theme),
                    SatoshiWeight.MEDIUM
                );
            }
        }

        state.subtabScroller.drawScrollbar(
            graphics,
            areaX + areaW + 3.8f,
            topY,
            bottomY,
            lx,
            ly,
            animDt,
            panelAlpha,
            interactive,
            theme
        );
    }

    public static void renderCard(
        GuiGraphicsExtractor graphics,
        GlassUiConfig config,
        UiScreenState state,
        UiCard card,
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
        UiTheme theme = config.theme();
        UiHitRect cardHit = new UiHitRect(x, y, w, h);
        if (interactive) {
            state.detailOpenHitboxes.add(new UiScreenState.IdHit(card.id(), cardHit));
            state.cardBodyHitboxes.add(new UiScreenState.CardHit(card, cardHit));
        }
        boolean cardHovered = interactive && cardHit.contains(lx, ly);
        card.animHover = UiRenderPrimitives.approachExp(card.animHover, cardHovered ? 1f : 0f, 16f, animDt);
        float targetToggle = (card.toggleable() && card.enabled()) ? 1f : 0f;
        card.animToggle = UiRenderPrimitives.approachExp(card.animToggle, targetToggle, 16f, animDt);

        int accent = theme.accentColor();
        int secondary = theme.secondaryAccentColor();
        float rScale = theme.cornerRadiusScale();
        float activeLift = card.animToggle * 7f;

        UiRenderPrimitives.drawThemedGlassCard(
            graphics,
            x,
            y,
            w,
            h,
            radius,
            34f + 9f * card.animHover + activeLift,
            20f + 5f * card.animHover + activeLift * 0.4f,
            52f + 20f * card.animHover + activeLift * 1.4f,
            22f + 10f * card.animHover,
            alpha,
            card.animToggle,
            card.animHover,
            theme
        );

        int iconColor = UiRenderPrimitives.lerpColor(
            UiRenderPrimitives.argb(Math.round(210f * alpha), 255, 255, 255),
            UiRenderPrimitives.multiplyAlpha(accent, alpha),
            Math.max(card.animToggle * 0.85f, !card.toggleable() ? 0.55f : 0f)
        );
        UiRenderPrimitives.drawIcon(graphics, card.icon(), x + 12f, y + 10.4f, 12.5f, iconColor);

        TextGradient titleGrad = TextGradient.pearl(0.98f * alpha, accent, 0.16f + 0.22f * card.animToggle, theme);
        String clippedTitle = UiRenderPrimitives.ellipsize(card.title(), w - 108f, 7.0f, SatoshiWeight.SEMIBOLD);
        SatoshiFont.draw(graphics, clippedTitle, x + 29f, y + 10.8f, 7.0f, titleGrad, SatoshiWeight.SEMIBOLD);

        if (card.toggleable()) {
            float swX = x + w - 35f;
            float swY = y + 10.6f;
            UiHitRect swHit = new UiHitRect(swX - 4f, swY - 4f, 31f, 20.5f);
            if (interactive) {
                state.cardToggleHitboxes.add(new UiScreenState.CardHit(card, swHit));
            }
            card.animToggleHover = UiRenderPrimitives.approachExp(
                card.animToggleHover,
                (interactive && swHit.contains(lx, ly)) ? 1f : 0f,
                16f,
                animDt
            );
            UiRenderPrimitives.drawSwitch(graphics, swX, swY, 23f, 12.5f, card.animToggle, card.animToggleHover, alpha, theme);
            if (card.bindable() && config.keybinds() != null) {
                renderKeybindPill(graphics, config, state, card.id(), swX - 7f, y + 11.0f, lx, ly, card.animHover, alpha, true, interactive);
            }
        } else if (card.onAction() != null) {
            float castW = Math.max(34f, SatoshiFont.width(card.actionLabel(), 5.1f, SatoshiWeight.SEMIBOLD) + 14f);
            float castH = 14f;
            float castX = x + w - 12f - castW;
            float castY = y + 9.8f;
            UiHitRect cHit = new UiHitRect(castX, castY, castW, castH);
            if (interactive) {
                state.instantActionHitboxes.add(new UiScreenState.CardHit(card, cHit));
            }
            card.animToggleHover = UiRenderPrimitives.approachExp(
                card.animToggleHover,
                (interactive && cHit.contains(lx, ly)) ? 1f : 0f,
                16f,
                animDt
            );
            float pillR = Math.min(7f, 7f * rScale);
            UiRenderPrimitives.drawGlassCard(
                graphics,
                castX,
                castY,
                castW,
                castH,
                pillR,
                UiRenderPrimitives.multiplyAlpha(accent, (0.32f + 0.24f * card.animToggleHover) * alpha),
                UiRenderPrimitives.multiplyAlpha(accent, (0.20f + 0.16f * card.animToggleHover) * alpha),
                UiRenderPrimitives.multiplyAlpha(accent, 0.85f * alpha),
                UiRenderPrimitives.multiplyAlpha(accent, 0.45f * alpha)
            );
            SatoshiFont.drawCentered(
                graphics,
                card.actionLabel(),
                castX + castW * 0.5f,
                castY + (castH - 5.1f) * 0.5f,
                5.1f,
                TextGradient.pearl(alpha, accent, 0.22f, theme),
                SatoshiWeight.SEMIBOLD
            );
            if (card.bindable() && config.keybinds() != null) {
                renderKeybindPill(graphics, config, state, card.id(), castX - 6f, y + 11.0f, lx, ly, card.animHover, alpha, true, interactive);
            }
        } else {
            float optW = Math.max(42f, SatoshiFont.width(card.optionsLabel(), 4.9f, SatoshiWeight.SEMIBOLD) + 14f);
            float optH = 14f;
            float optX = x + w - 12f - optW;
            float optY = y + 9.8f;
            UiHitRect tHit = new UiHitRect(optX, optY, optW, optH);
            if (interactive) {
                state.openOptionsBtnHitboxes.add(new UiScreenState.IdHit(card.id(), tHit));
            }
            card.animToggleHover = UiRenderPrimitives.approachExp(
                card.animToggleHover,
                (interactive && tHit.contains(lx, ly)) ? 1f : 0f,
                16f,
                animDt
            );
            float pillR = Math.min(7f, 7f * rScale);
            UiRenderPrimitives.drawThemedGlassCard(
                graphics,
                optX,
                optY,
                optW,
                optH,
                pillR,
                34f + 26f * card.animToggleHover,
                20f + 16f * card.animToggleHover,
                56f + 34f * card.animToggleHover,
                26f + 18f * card.animToggleHover,
                alpha,
                card.animToggleHover * 0.5f,
                card.animToggleHover,
                theme
            );
            SatoshiFont.drawCentered(
                graphics,
                card.optionsLabel(),
                optX + optW * 0.5f,
                optY + (optH - 4.9f) * 0.5f,
                4.9f,
                TextGradient.pearl(alpha, accent, 0.24f, theme),
                SatoshiWeight.SEMIBOLD
            );
            if (card.bindable() && config.keybinds() != null) {
                renderKeybindPill(graphics, config, state, card.id(), optX - 6f, y + 11.0f, lx, ly, card.animHover, alpha, true, interactive);
            }
        }

        UiRenderPrimitives.drawRoundedRect(
            graphics,
            x + 12f,
            y + 38.5f,
            w - 24f,
            0.7f,
            0.35f,
            UiRenderPrimitives.argb(Math.round(22f * alpha), 255, 255, 255)
        );

        UiChoice hoveredChoice = null;
        int hoveredChoiceIdx = -1;
        List<UiChoice> choices = card.choices();
        UiControl qRow = card.quickControl();

        if (!choices.isEmpty()) {
            int count = choices.size();
            float startCx = x + 36f;
            float stepX = count > 1 ? (w - 72f) / (count - 1) : 0f;
            float circleCy = y + 63.5f;

            for (int idx = 0; idx < count; idx++) {
                UiChoice choice = choices.get(idx);
                float cx = count == 1 ? x + w * 0.5f : startCx + idx * stepX;
                UiHitRect cHit = new UiHitRect(cx - 18f, circleCy - 14f, 36f, 36f);
                if (interactive) {
                    state.cardChoiceHitboxes.add(new UiScreenState.CardChoiceHit(card, idx, cHit));
                }

                boolean isSelected = idx == card.selectedChoice();
                boolean cHovered = interactive && cHit.contains(lx, ly);
                if (cHovered) {
                    hoveredChoice = choice;
                    hoveredChoiceIdx = idx;
                }
                choice.animSelected = UiRenderPrimitives.approachExp(choice.animSelected, isSelected ? 1f : 0f, 18f, animDt);
                choice.animHover = UiRenderPrimitives.approachExp(choice.animHover, cHovered ? 1f : 0f, 18f, animDt);
                choice.animPress = UiRenderPrimitives.approachExp(choice.animPress, 0f, 20f, animDt);

                float r = 11.2f + 0.7f * choice.animHover - 1.0f * choice.animPress;
                int circleFill = UiRenderPrimitives.lerpColor(
                    UiRenderPrimitives.argb(Math.round((24f + 22f * choice.animHover) * alpha), 255, 255, 255),
                    UiRenderPrimitives.argb(Math.round(252f * alpha), 255, 255, 255),
                    choice.animSelected
                );
                int circleBorder = UiRenderPrimitives.lerpColor(
                    UiRenderPrimitives.argb(Math.round((78f + 45f * choice.animHover) * alpha), 255, 255, 255),
                    UiRenderPrimitives.argb(Math.round(255f * alpha), 255, 255, 255),
                    choice.animSelected
                );

                if (choice.animSelected > 0.05f) {
                    UiRenderPrimitives.drawRoundedShadow(
                        graphics,
                        cx - r,
                        circleCy - r + 1f,
                        r * 2f,
                        r * 2f,
                        r,
                        4.5f,
                        UiRenderPrimitives.argb(Math.round(65f * choice.animSelected * alpha), 0, 0, 0)
                    );
                }

                UiRenderPrimitives.drawRoundedRect(graphics, cx - r, circleCy - r, r * 2f, r * 2f, r, circleFill);
                UiRenderPrimitives.drawRoundedBorder(graphics, cx - r, circleCy - r, r * 2f, r * 2f, r, 0.95f, circleBorder, circleBorder);

                float iconSize = 10.2f;
                int chIconColor = UiRenderPrimitives.lerpColor(
                    UiRenderPrimitives.argb(Math.round(225f * alpha), 255, 255, 255),
                    UiRenderPrimitives.argb(Math.round(255f * alpha), 32, 30, 28),
                    choice.animSelected
                );
                UiRenderPrimitives.drawIcon(graphics, choice.icon(), cx - iconSize * 0.5f, circleCy - iconSize * 0.5f, iconSize, chIconColor);

                SatoshiWeight captionWeight = choice.animSelected > 0.5f ? SatoshiWeight.SEMIBOLD : SatoshiWeight.MEDIUM;
                TextGradient captionGrad = TextGradient.lerp(
                    TextGradient.mist((0.68f + 0.20f * choice.animHover) * alpha, theme),
                    TextGradient.pearl(alpha, accent, 0.34f, theme),
                    choice.animSelected
                );
                String clippedChoice = UiRenderPrimitives.ellipsize(choice.label(), count >= 4 ? 54f : 72f, 5.0f, captionWeight);
                SatoshiFont.drawCentered(graphics, clippedChoice, cx, circleCy + 15.0f, 5.0f, captionGrad, captionWeight);
            }
        } else if (qRow != null && qRow.isVisible()) {
            UiHitRect qRect = new UiHitRect(x, y + 43f, w, 32f);
            boolean qHov = interactive && qRect.contains(lx, ly);
            renderInlineControlRow(graphics, config, state, qRow, x, y + 43f, w, 32f, lx, ly, animDt, alpha, interactive, true);
            List<UiControl> visControls = card.visibleControls();
            String qDesc = qRow.description();
            String footerText;
            if (qHov && !qDesc.isEmpty()) {
                footerText = qDesc;
            } else if (visControls.size() == 1 && !qDesc.isEmpty()) {
                footerText = qDesc;
            } else if (visControls.size() > 1) {
                footerText = "Right-click card for all " + visControls.size() + " settings";
            } else if (card.customSection() != null) {
                footerText = "Right-click card to open interactive manager";
            } else {
                footerText = "Right-click card for full view";
            }
            SatoshiFont.draw(
                graphics,
                UiRenderPrimitives.ellipsize(footerText, w - 24f, 4.55f, SatoshiWeight.MEDIUM),
                x + 12f,
                y + 85f,
                4.55f,
                qHov ? TextGradient.silver(0.80f * alpha, theme) : TextGradient.mist(0.60f * alpha, theme),
                SatoshiWeight.MEDIUM
            );
        } else {
            List<String> lines = SatoshiFont.wrapLines(card.summary(), w - 24f, 5.0f, SatoshiWeight.MEDIUM);
            int maxLines = Math.min(2, lines.size());
            float lyText = y + 50f;
            for (int i = 0; i < maxLines; i++) {
                SatoshiFont.draw(graphics, lines.get(i), x + 12f, lyText, 5.0f, TextGradient.mist(0.72f * alpha, theme), SatoshiWeight.MEDIUM);
                lyText += 8.2f;
            }
            List<UiControl> visControls = card.visibleControls();
            if (!visControls.isEmpty() || card.customSection() != null) {
                String hint = card.customSection() != null
                    ? "Right-click card to open configuration view"
                    : ("Right-click card for " + visControls.size() + " settings");
                SatoshiFont.draw(
                    graphics,
                    UiRenderPrimitives.ellipsize(hint, w - 24f, 4.55f, SatoshiWeight.MEDIUM),
                    x + 12f,
                    y + 85f,
                    4.55f,
                    TextGradient.mist(0.58f * alpha, theme),
                    SatoshiWeight.MEDIUM
                );
            }
        }

        if (hoveredChoice != null && !hoveredChoice.description().isEmpty()) {
            String cardKey = "card_" + card.id() + "_" + hoveredChoiceIdx;
            if (!cardKey.equals(state.lastHoveredModeKey)) {
                state.lastHoveredModeKey = cardKey;
                state.modeHoverStartSec = state.elapsedSeconds;
            }
            KineticTextRenderer.drawKineticDecode(
                graphics,
                hoveredChoice.label() + ": " + hoveredChoice.description(),
                x + 12f,
                y + 26.2f,
                w - 24f,
                4.85f,
                TextGradient.silver(0.88f * alpha, theme),
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
        } else {
            SatoshiFont.draw(
                graphics,
                UiRenderPrimitives.ellipsize(card.subtitle(), w - 24f, 4.85f, SatoshiWeight.MEDIUM),
                x + 12f,
                y + 26.2f,
                4.85f,
                TextGradient.mist(0.74f * alpha, theme),
                SatoshiWeight.MEDIUM
            );
        }
    }

    public static void renderInlineControlRow(
        GuiGraphicsExtractor graphics,
        GlassUiConfig config,
        UiScreenState state,
        UiControl control,
        float x,
        float y,
        float w,
        float h,
        float lx,
        float ly,
        float animDt,
        float alpha,
        boolean interactive,
        boolean compact
    ) {
        UiTheme theme = config.theme();
        int accent = theme.accentColor();
        int secondary = theme.secondaryAccentColor();
        float rScale = theme.cornerRadiusScale();
        UiHitRect hit = new UiHitRect(x, y, w, h);
        boolean hov = interactive && hit.contains(lx, ly);

        if (control instanceof UiControl.Toggle row) {
            row.animToggle = UiRenderPrimitives.approachExp(row.animToggle, row.enabled() ? 1f : 0f, 16f, animDt);
            row.animHover = UiRenderPrimitives.approachExp(row.animHover, hov ? 1f : 0f, 16f, animDt);
            if (interactive) {
                state.toggleRowActions.add(new UiScreenState.ControlHit<>(row, hit));
            }
            if (compact) {
                float iconSz = 10f;
                int iconTint = UiRenderPrimitives.lerpColor(
                    UiRenderPrimitives.argb(Math.round(195f * alpha), 255, 255, 255),
                    UiRenderPrimitives.multiplyAlpha(accent, alpha),
                    Math.max(row.animToggle, row.animHover * 0.5f)
                );
                UiRenderPrimitives.drawIcon(graphics, row.icon(), x + 12f, y + (h - iconSz) * 0.5f, iconSz, iconTint);
                float labelSize = 5.8f;
                String lbl = UiRenderPrimitives.ellipsize(row.label(), w - 68f, labelSize, SatoshiWeight.MEDIUM);
                SatoshiFont.draw(
                    graphics,
                    lbl,
                    x + 26f,
                    y + (h - labelSize) * 0.5f,
                    labelSize,
                    TextGradient.lerp(TextGradient.silver(0.92f * alpha, theme), TextGradient.pearl(alpha, accent, 0.30f, theme), Math.max(row.animToggle, row.animHover)),
                    SatoshiWeight.MEDIUM
                );
                UiRenderPrimitives.drawSwitch(graphics, x + w - 35f, y + (h - 12.5f) * 0.5f, 23f, 12.5f, row.animToggle, row.animHover, alpha, theme);
            } else {
                UiRenderPrimitives.drawIconBadge(graphics, row.icon(), x + 8.5f, y + (h - 19f) * 0.5f, 19f, Math.max(row.animToggle, row.animHover * 0.55f), alpha, theme);
                float textLeft = x + 32.5f;
                float maxDescW = Math.max(90f, w - 72f);
                String lbl = UiRenderPrimitives.ellipsize(row.label(), maxDescW, 5.7f, SatoshiWeight.SEMIBOLD);
                SatoshiFont.draw(
                    graphics,
                    lbl,
                    textLeft,
                    y + 6.6f,
                    5.7f,
                    TextGradient.lerp(TextGradient.silver(0.95f * alpha, theme), TextGradient.pearl(alpha, accent, 0.32f, theme), Math.max(row.animToggle, row.animHover)),
                    SatoshiWeight.SEMIBOLD
                );
                drawWrappedDescription(graphics, row.description(), textLeft, y + 16.5f, maxDescW, 4.35f, 5.9f, alpha, theme);
                UiRenderPrimitives.drawSwitch(graphics, x + w - 32f, y + (h - 12f) * 0.5f, 22f, 12f, row.animToggle, row.animHover, alpha, theme);
            }
        } else if (control instanceof UiControl.Slider row) {
            float trackX = x + (compact ? 12f : 10f);
            float trackW = w - (compact ? 24f : 20f);
            float trackY = y + h - (compact ? 10.5f : 8.8f);
            float trackH = compact ? 5.2f : 4.8f;
            if (interactive) {
                state.sliderRowHitboxes.add(new UiScreenState.TrackHit<>(row, hit, trackX, trackW));
            }
            boolean dragging = state.activeDraggingSlider == row;
            row.animFocus = UiRenderPrimitives.approachExp(row.animFocus, (dragging || hov) ? 1f : 0f, 18f, animDt);
            row.animNorm = UiRenderPrimitives.approachExp(row.animNorm, row.normalized(), dragging ? 28f : 18f, animDt);

            String valStr = row.formattedValue();
            float valW = SatoshiFont.width(valStr, 5.55f, SatoshiWeight.SEMIBOLD);

            if (compact) {
                UiRenderPrimitives.drawIcon(
                    graphics,
                    row.icon(),
                    x + 12f,
                    y + 5.2f,
                    9.5f,
                    UiRenderPrimitives.lerpColor(UiRenderPrimitives.argb(Math.round(195f * alpha), 255, 255, 255), UiRenderPrimitives.multiplyAlpha(accent, alpha), row.animFocus)
                );
                String lbl = UiRenderPrimitives.ellipsize(row.label(), w - 42f - valW, 5.7f, SatoshiWeight.MEDIUM);
                SatoshiFont.draw(graphics, lbl, x + 25f, y + 6.0f, 5.7f, TextGradient.silver(0.94f * alpha, theme), SatoshiWeight.MEDIUM);
                SatoshiFont.draw(graphics, valStr, x + w - 12f - valW, y + 6.0f, 5.7f, TextGradient.accent(0.98f * alpha, accent, theme), SatoshiWeight.SEMIBOLD);
            } else {
                UiRenderPrimitives.drawIconBadge(graphics, row.icon(), x + 8.5f, y + 5.0f, 17.5f, Math.max(row.animFocus, 0.28f), alpha, theme);
                float textLeft = x + 31f;
                String lbl = UiRenderPrimitives.ellipsize(row.label(), w - 46f - valW, 5.55f, SatoshiWeight.SEMIBOLD);
                SatoshiFont.draw(graphics, lbl, textLeft, y + 5.2f, 5.55f, TextGradient.pearl(0.96f * alpha, accent, 0.22f, theme), SatoshiWeight.SEMIBOLD);
                SatoshiFont.draw(graphics, valStr, x + w - 10f - valW, y + 5.2f, 5.55f, TextGradient.accent(0.98f * alpha, accent, theme), SatoshiWeight.SEMIBOLD);
                drawWrappedDescription(graphics, row.description(), textLeft, y + 14.0f, Math.max(100f, w - 41f), 4.25f, 5.5f, alpha, theme);
            }

            UiRenderPrimitives.drawRoundedRect(graphics, trackX, trackY, trackW, trackH, trackH * 0.5f, UiRenderPrimitives.argb(Math.round(46f * alpha), 255, 255, 255));
            float fillW = Math.max(trackH, trackW * row.animNorm);
            int fillTint = UiRenderPrimitives.lerpColor(accent, secondary, row.animNorm * 0.45f);
            UiRenderPrimitives.drawRoundedRect(graphics, trackX, trackY, fillW, trackH, trackH * 0.5f, UiRenderPrimitives.multiplyAlpha(fillTint, alpha));
            float thumbCx = (trackX + trackH * 0.5f) + (trackW - trackH) * row.animNorm;
            float thumbCy = trackY + trackH * 0.5f;
            float thumbR = (compact ? 4.0f : 3.6f) + 0.75f * row.animFocus;
            UiRenderPrimitives.drawRoundedRect(graphics, thumbCx - thumbR, thumbCy - thumbR, thumbR * 2f, thumbR * 2f, thumbR, UiRenderPrimitives.argb(Math.round(255f * alpha), 255, 255, 255));
        } else if (control instanceof UiControl.Action row) {
            float btnFontSize = 5.4f;
            float btnW = Math.max(42f, SatoshiFont.width(row.buttonLabel(), btnFontSize, SatoshiWeight.SEMIBOLD) + 16f);
            float btnH = 16.5f;
            float btnX = x + w - btnW - 10f;
            float btnY = y + (h - btnH) * 0.5f;
            UiHitRect bHit = new UiHitRect(btnX, btnY, btnW, btnH);
            if (interactive) {
                state.actionButtonHitboxes.add(new UiScreenState.ControlHit<>(row, bHit));
            }
            row.animBtnHover = UiRenderPrimitives.approachExp(row.animBtnHover, (interactive && bHit.contains(lx, ly)) ? 1f : 0f, 18f, animDt);
            row.animPress = UiRenderPrimitives.approachExp(row.animPress, 0f, 18f, animDt);

            if (!compact) {
                UiRenderPrimitives.drawIconBadge(graphics, row.icon(), x + 8.5f, y + (h - 19f) * 0.5f, 19f, row.animBtnHover, alpha, theme);
                float textLeft = x + 32.5f;
                float maxDescW = Math.max(80f, btnX - textLeft - 8f);
                SatoshiFont.draw(graphics, row.label(), textLeft, y + 6.8f, 5.7f, TextGradient.pearl(0.96f * alpha, accent, 0.24f, theme), SatoshiWeight.SEMIBOLD);
                drawWrappedDescription(graphics, row.description(), textLeft, y + 16.5f, maxDescW, 4.35f, 5.9f, alpha, theme);
            } else {
                SatoshiFont.draw(graphics, row.label(), x + 12f, y + (h - 5.8f) * 0.5f, 5.8f, TextGradient.silver(0.92f * alpha, theme), SatoshiWeight.MEDIUM);
            }

            float br = Math.min(btnH * 0.5f, 8.25f * rScale);
            UiRenderPrimitives.drawThemedGlassCard(
                graphics,
                btnX,
                btnY,
                btnW,
                btnH,
                br,
                42f + 28f * row.animBtnHover,
                24f + 18f * row.animBtnHover,
                66f + 36f * row.animBtnHover,
                30f + 18f * row.animBtnHover,
                alpha,
                row.animBtnHover * 0.65f,
                row.animBtnHover,
                theme
            );
            SatoshiFont.drawCentered(
                graphics,
                row.buttonLabel(),
                btnX + btnW * 0.5f,
                btnY + (btnH - btnFontSize) * 0.5f,
                btnFontSize,
                TextGradient.lerp(TextGradient.pearl(0.95f * alpha, accent, 0.22f, theme), TextGradient.accent(alpha, accent, theme), row.animBtnHover),
                SatoshiWeight.SEMIBOLD
            );
        } else if (control instanceof UiControl.Cycle row) {
            String curOpt = row.currentOption();
            float btnFontSize = 5.25f;
            float btnW = clamp(SatoshiFont.width(curOpt, btnFontSize, SatoshiWeight.SEMIBOLD) + 18f, 44f, w * 0.48f);
            float btnH = 16.5f;
            float btnX = x + w - btnW - 10f;
            float btnY = y + (h - btnH) * 0.5f;
            if (interactive) {
                state.cycleRowHitboxes.add(new UiScreenState.ControlHit<>(row, hit));
            }
            row.animBtnHover = UiRenderPrimitives.approachExp(row.animBtnHover, hov ? 1f : 0f, 18f, animDt);
            row.animPress = UiRenderPrimitives.approachExp(row.animPress, 0f, 18f, animDt);

            if (!compact) {
                UiRenderPrimitives.drawIconBadge(graphics, row.icon(), x + 8.5f, y + (h - 19f) * 0.5f, 19f, Math.max(0.35f, row.animBtnHover), alpha, theme);
                float textLeft = x + 32.5f;
                float maxDescW = Math.max(60f, btnX - textLeft - 8f);
                String lbl = UiRenderPrimitives.ellipsize(row.label(), maxDescW, 5.7f, SatoshiWeight.SEMIBOLD);
                SatoshiFont.draw(graphics, lbl, textLeft, y + 6.8f, 5.7f, TextGradient.pearl(0.96f * alpha, accent, 0.24f, theme), SatoshiWeight.SEMIBOLD);
                drawWrappedDescription(graphics, row.description(), textLeft, y + 16.5f, maxDescW, 4.35f, 5.9f, alpha, theme);
            } else {
                String lbl = UiRenderPrimitives.ellipsize(row.label(), btnX - x - 18f, 5.8f, SatoshiWeight.MEDIUM);
                SatoshiFont.draw(graphics, lbl, x + 12f, y + (h - 5.8f) * 0.5f, 5.8f, TextGradient.silver(0.92f * alpha, theme), SatoshiWeight.MEDIUM);
            }

            float br = Math.min(btnH * 0.5f, 8.25f * rScale);
            UiRenderPrimitives.drawThemedGlassCard(
                graphics,
                btnX,
                btnY,
                btnW,
                btnH,
                br,
                40f + 24f * row.animBtnHover,
                22f + 16f * row.animBtnHover,
                62f + 32f * row.animBtnHover,
                28f + 16f * row.animBtnHover,
                alpha,
                0.45f + row.animBtnHover * 0.40f,
                row.animBtnHover,
                theme
            );
            String clippedOpt = UiRenderPrimitives.ellipsize(curOpt, btnW - 10f, btnFontSize, SatoshiWeight.SEMIBOLD);
            SatoshiFont.drawCentered(
                graphics,
                clippedOpt,
                btnX + btnW * 0.5f,
                btnY + (btnH - btnFontSize) * 0.5f,
                btnFontSize,
                TextGradient.accent(0.98f * alpha, accent, theme),
                SatoshiWeight.SEMIBOLD
            );
        } else if (control instanceof UiControl.RangeSlider row) {
            float trackX = x + (compact ? 12f : 10f);
            float trackW = w - (compact ? 24f : 20f);
            float trackY = y + h - (compact ? 10.5f : 8.8f);
            float trackH = compact ? 5.2f : 4.8f;
            if (interactive) {
                state.rangeRowHitboxes.add(new UiScreenState.TrackHit<>(row, hit, trackX, trackW));
            }
            boolean dragging = state.activeDraggingRange == row;
            row.animFocus = UiRenderPrimitives.approachExp(row.animFocus, (dragging || hov) ? 1f : 0f, 18f, animDt);
            row.animLowNorm = UiRenderPrimitives.approachExp(row.animLowNorm, row.lowNormalized(), dragging ? 28f : 18f, animDt);
            row.animHighNorm = UiRenderPrimitives.approachExp(row.animHighNorm, row.highNormalized(), dragging ? 28f : 18f, animDt);

            String valStr = row.formattedValue();
            float valW = SatoshiFont.width(valStr, 5.45f, SatoshiWeight.SEMIBOLD);
            if (!compact) {
                UiRenderPrimitives.drawIconBadge(graphics, row.icon(), x + 8.5f, y + 5.0f, 17.5f, Math.max(row.animFocus, 0.28f), alpha, theme);
                float textLeft = x + 31f;
                String lbl = UiRenderPrimitives.ellipsize(row.label(), w - 46f - valW, 5.55f, SatoshiWeight.SEMIBOLD);
                SatoshiFont.draw(graphics, lbl, textLeft, y + 5.2f, 5.55f, TextGradient.pearl(0.96f * alpha, accent, 0.22f, theme), SatoshiWeight.SEMIBOLD);
                SatoshiFont.draw(graphics, valStr, x + w - 10f - valW, y + 5.2f, 5.45f, TextGradient.accent(0.98f * alpha, accent, theme), SatoshiWeight.SEMIBOLD);
                drawWrappedDescription(graphics, row.description(), textLeft, y + 14.0f, Math.max(100f, w - 41f), 4.25f, 5.5f, alpha, theme);
            } else {
                String lbl = UiRenderPrimitives.ellipsize(row.label(), w - 30f - valW, 5.7f, SatoshiWeight.MEDIUM);
                SatoshiFont.draw(graphics, lbl, x + 12f, y + 6.0f, 5.7f, TextGradient.silver(0.94f * alpha, theme), SatoshiWeight.MEDIUM);
                SatoshiFont.draw(graphics, valStr, x + w - 12f - valW, y + 6.0f, 5.5f, TextGradient.accent(0.98f * alpha, accent, theme), SatoshiWeight.SEMIBOLD);
            }

            UiRenderPrimitives.drawRoundedRect(graphics, trackX, trackY, trackW, trackH, trackH * 0.5f, UiRenderPrimitives.argb(Math.round(46f * alpha), 255, 255, 255));
            float lowCx = (trackX + trackH * 0.5f) + (trackW - trackH) * row.animLowNorm;
            float highCx = (trackX + trackH * 0.5f) + (trackW - trackH) * row.animHighNorm;
            float spanLeft = Math.min(lowCx, highCx);
            float spanW = Math.max(trackH, Math.abs(highCx - lowCx));
            int fillTint = UiRenderPrimitives.lerpColor(accent, secondary, 0.45f);
            UiRenderPrimitives.drawRoundedRect(graphics, spanLeft, trackY, spanW, trackH, trackH * 0.5f, UiRenderPrimitives.multiplyAlpha(fillTint, alpha));

            float thumbCy = trackY + trackH * 0.5f;
            float thumbR = 3.5f + 0.65f * row.animFocus;
            int thumbCol = UiRenderPrimitives.argb(Math.round(255f * alpha), 255, 255, 255);
            UiRenderPrimitives.drawRoundedRect(graphics, lowCx - thumbR, thumbCy - thumbR, thumbR * 2f, thumbR * 2f, thumbR, thumbCol);
            UiRenderPrimitives.drawRoundedRect(graphics, highCx - thumbR, thumbCy - thumbR, thumbR * 2f, thumbR * 2f, thumbR, thumbCol);
        } else if (control instanceof UiControl.ColorPicker row) {
            float swatchSz = 14f;
            float swatchX = x + w - 10f - swatchSz;
            float swatchY = y + 5.5f;
            UiHitRect swHit = new UiHitRect(swatchX - 2f, swatchY - 2f, swatchSz + 4f, swatchSz + 4f);
            float trackX = x + 10f;
            float trackW = w - 20f;
            float trackY = y + h - 8.8f;
            float trackH = 5.0f;
            UiHitRect hueHit = new UiHitRect(trackX - 3f, trackY - 5f, trackW + 6f, trackH + 10f);
            if (interactive) {
                state.colorSwatchHitboxes.add(new UiScreenState.ControlHit<>(row, swHit));
                state.colorHueHitboxes.add(new UiScreenState.TrackHit<>(row, hueHit, trackX, trackW));
            }
            boolean dragging = state.activeDraggingColor == row;
            row.animFocus = UiRenderPrimitives.approachExp(row.animFocus, (dragging || hov) ? 1f : 0f, 18f, animDt);

            UiRenderPrimitives.drawIconBadge(graphics, row.icon(), x + 8.5f, y + 5.0f, 17.5f, 0.55f, alpha, theme);
            String hex = row.hexLabel();
            float hexW = SatoshiFont.width(hex, 5.1f, SatoshiWeight.SEMIBOLD);
            String lbl = UiRenderPrimitives.ellipsize(row.label(), swatchX - x - 42f - hexW, 5.55f, SatoshiWeight.SEMIBOLD);
            SatoshiFont.draw(graphics, lbl, x + 31f, y + 5.5f, 5.55f, TextGradient.pearl(0.96f * alpha, row.color(), 0.25f, theme), SatoshiWeight.SEMIBOLD);
            SatoshiFont.draw(graphics, hex, swatchX - 6f - hexW, y + 6.8f, 5.1f, TextGradient.silver(0.88f * alpha, theme), SatoshiWeight.SEMIBOLD);
            SatoshiFont.draw(graphics, "Drag spectrum or click swatch", x + 31f, y + 14.8f, 4.2f, TextGradient.mist(0.72f * alpha, theme), SatoshiWeight.MEDIUM);

            UiRenderPrimitives.drawRoundedRect(graphics, swatchX, swatchY, swatchSz, swatchSz, 4.5f, UiRenderPrimitives.multiplyAlpha(row.color() | 0xFF000000, alpha));
            UiRenderPrimitives.drawRoundedBorder(graphics, swatchX, swatchY, swatchSz, swatchSz, 4.5f, 0.9f, UiRenderPrimitives.argb(Math.round(210f * alpha), 255, 255, 255));

            int segs = 12;
            float segW = trackW / segs;
            for (int i = 0; i < segs; i++) {
                int segRgb = Color.HSBtoRGB(i / (float) segs, row.saturation(), row.brightness());
                float sx = trackX + i * segW;
                UiRenderPrimitives.drawRoundedRect(graphics, sx, trackY, segW + 0.6f, trackH, i == 0 || i == segs - 1 ? trackH * 0.5f : 0.8f, UiRenderPrimitives.multiplyAlpha(segRgb, alpha));
            }
            float thumbCx = trackX + trackW * row.hue();
            float thumbCy = trackY + trackH * 0.5f;
            float thumbR = 3.8f + 0.6f * row.animFocus;
            UiRenderPrimitives.drawRoundedRect(graphics, thumbCx - thumbR, thumbCy - thumbR, thumbR * 2f, thumbR * 2f, thumbR, UiRenderPrimitives.argb(Math.round(255f * alpha), 255, 255, 255));
        } else if (control instanceof UiControl.TextField row) {
            float boxW = Math.min(132f, w * 0.52f);
            float boxH = 17.5f;
            float boxX = x + w - boxW - 9f;
            float boxY = y + (h - boxH) * 0.5f;
            UiHitRect tfHit = new UiHitRect(boxX, boxY, boxW, boxH);
            if (interactive) {
                state.textFieldHitboxes.add(new UiScreenState.ControlHit<>(row, tfHit));
            }
            boolean isFocused = state.focusedTextField == row;
            row.focused = isFocused;
            row.animFocus = UiRenderPrimitives.approachExp(row.animFocus, isFocused ? 1f : 0f, 18f, animDt);
            row.keyPulse = UiRenderPrimitives.approachExp(row.keyPulse, 0f, 12f, animDt);

            UiRenderPrimitives.drawIconBadge(graphics, row.icon(), x + 8.5f, y + (h - 19f) * 0.5f, 19f, Math.max(row.animFocus, hov ? 0.45f : 0.2f), alpha, theme);
            String lbl = UiRenderPrimitives.ellipsize(row.label(), boxX - (x + 32.5f) - 6f, 5.6f, SatoshiWeight.SEMIBOLD);
            SatoshiFont.draw(graphics, lbl, x + 32.5f, y + (h - 5.6f) * 0.5f, 5.6f, TextGradient.pearl(0.95f * alpha, accent, 0.22f, theme), SatoshiWeight.SEMIBOLD);

            float br = Math.min(8f, 8f * rScale);
            int fill = UiRenderPrimitives.lerpColor(
                UiRenderPrimitives.argb(Math.round(92f * alpha), 16, 16, 18),
                UiRenderPrimitives.argb(Math.round(142f * alpha), 12, 12, 14),
                row.animFocus
            );
            int rim = UiRenderPrimitives.lerpColor(
                UiRenderPrimitives.argb(Math.round(44f * alpha), 255, 255, 255),
                UiRenderPrimitives.multiplyAlpha(accent, 0.90f * alpha),
                row.animFocus
            );
            UiRenderPrimitives.drawRoundedRect(graphics, boxX, boxY, boxW, boxH, br, fill);
            UiRenderPrimitives.drawRoundedBorder(graphics, boxX, boxY, boxW, boxH, br, 0.9f, rim, rim);

            float tfFont = 5.2f;
            float tfTextX = boxX + 7f;
            float tfTextY = boxY + (boxH - tfFont) * 0.5f;
            String rawVal = row.value();
            if (rawVal.isEmpty() && !isFocused) {
                SatoshiFont.draw(graphics, "Type text...", tfTextX, tfTextY, tfFont, TextGradient.mist(0.60f * alpha, theme), SatoshiWeight.MEDIUM);
            } else {
                String vis = SatoshiFont.trimTailToWidth(rawVal, boxW - 14f, tfFont, SatoshiWeight.MEDIUM);
                SatoshiFont.draw(graphics, vis, tfTextX, tfTextY, tfFont, TextGradient.accent(0.98f * alpha, accent, theme), SatoshiWeight.MEDIUM);
                if (isFocused) {
                    float pulse = (float) Math.sin(state.elapsedSeconds * 6.2f) * 0.5f + 0.5f;
                    float cx = tfTextX + SatoshiFont.width(vis, tfFont, SatoshiWeight.MEDIUM) + 1.1f;
                    UiRenderPrimitives.drawRoundedRect(
                        graphics,
                        cx,
                        boxY + (boxH - 7.8f) * 0.5f,
                        1.15f,
                        7.8f,
                        0.55f,
                        UiRenderPrimitives.multiplyAlpha(accent, (0.45f + 0.48f * pulse) * alpha)
                    );
                }
            }
        }
    }

    private static void drawWrappedDescription(
        GuiGraphicsExtractor graphics,
        String description,
        float x,
        float startY,
        float maxW,
        float fontSize,
        float lineStep,
        float alpha,
        UiTheme theme
    ) {
        List<String> lines = SatoshiFont.wrapLines(description, maxW, fontSize, SatoshiWeight.MEDIUM);
        int maxLines = Math.min(2, lines.size());
        float dy = startY;
        for (int i = 0; i < maxLines; i++) {
            SatoshiFont.draw(graphics, lines.get(i), x, dy, fontSize, TextGradient.mist(0.76f * alpha, theme), SatoshiWeight.MEDIUM);
            dy += lineStep;
        }
    }

    public static void renderKeybindPill(
        GuiGraphicsExtractor graphics,
        GlassUiConfig config,
        UiScreenState state,
        String itemId,
        float rightEdgeX,
        float topY,
        float lx,
        float ly,
        float parentHover,
        float alpha,
        boolean alwaysVisible,
        boolean registerHitbox
    ) {
        UiKeybindProvider provider = config.keybinds();
        if (provider == null) {
            return;
        }
        UiTheme theme = config.theme();
        UiInputBind bind = provider.getBind(itemId);
        boolean listening = itemId.equals(state.listeningBindId);
        float showAmount = (listening || bind.isBound() || alwaysVisible) ? 1f : parentHover;
        if (showAmount <= 0.03f) {
            return;
        }

        String label;
        if (listening) {
            label = "Press...";
        } else if (bind.isBound()) {
            label = bind.formatLabel();
        } else {
            label = "Bind";
        }

        float fontSize = 4.7f;
        float pillH = 11.5f;
        float pillW = Math.max(23f, SatoshiFont.width(label, fontSize, SatoshiWeight.SEMIBOLD) + 9f);
        float px = rightEdgeX - pillW;
        UiHitRect hit = new UiHitRect(px - 2f, topY - 2f, pillW + 4f, pillH + 4f);
        if (registerHitbox) {
            state.bindBadgeHitboxes.add(new UiScreenState.IdHit(itemId, hit));
        }

        boolean hovered = registerHitbox && hit.contains(lx, ly);
        float visAlpha = alpha * showAmount;
        int accent = theme.accentColor();
        float pulse = listening ? ((float) Math.sin(state.elapsedSeconds * 7.5f) * 0.5f + 0.5f) : 0f;

        int bg;
        if (listening) {
            bg = UiRenderPrimitives.multiplyAlpha(accent, (0.36f + 0.18f * pulse) * visAlpha);
        } else if (hovered) {
            bg = UiRenderPrimitives.argb(Math.round(74f * visAlpha), 255, 255, 255);
        } else if (bind.isBound()) {
            bg = UiRenderPrimitives.argb(Math.round(46f * visAlpha), 255, 255, 255);
        } else {
            bg = UiRenderPrimitives.argb(Math.round(26f * visAlpha), 255, 255, 255);
        }

        int border;
        if (listening) {
            border = UiRenderPrimitives.multiplyAlpha(accent, 0.96f * visAlpha);
        } else if (bind.isBound()) {
            border = UiRenderPrimitives.multiplyAlpha(accent, (0.62f + (hovered ? 0.25f : 0f)) * visAlpha);
        } else {
            border = UiRenderPrimitives.argb(Math.round((48f + (hovered ? 38f : 0f)) * visAlpha), 255, 255, 255);
        }

        UiRenderPrimitives.drawRoundedRect(graphics, px, topY, pillW, pillH, pillH * 0.5f, bg);
        UiRenderPrimitives.drawRoundedBorder(graphics, px, topY, pillW, pillH, pillH * 0.5f, 0.85f, border, border);

        TextGradient textGrad;
        if (listening) {
            textGrad = TextGradient.pearl(visAlpha, accent, 0.25f, theme);
        } else if (bind.isBound()) {
            textGrad = TextGradient.accent(0.98f * visAlpha, accent, theme);
        } else if (hovered) {
            textGrad = TextGradient.pearl(0.94f * visAlpha, accent, 0.20f, theme);
        } else {
            textGrad = TextGradient.mist(0.80f * visAlpha, theme);
        }

        SatoshiFont.drawCentered(graphics, label, px + pillW * 0.5f, topY + (pillH - fontSize) * 0.5f, fontSize, textGrad, SatoshiWeight.SEMIBOLD);
    }

    public static void renderSearchResults(
        GuiGraphicsExtractor graphics,
        GlassUiConfig config,
        UiScreenState state,
        float lx,
        float ly,
        float animDt,
        float panelAlpha,
        boolean interactive
    ) {
        UiTheme theme = config.theme();
        UiCatalog catalog = config.catalog();
        List<UiCard> matched = catalog.searchCards(state.searchQuery);

        if (matched.isEmpty()) {
            state.searchScroller.updateBounds(0f, 100f, 100f);
            float emptyAreaX = GlassUiChrome.contentAreaX(config, state);
            float emptyAreaW = config.panelWidth() - 72f;
            SatoshiFont.drawCentered(
                graphics,
                "No results match \"" + state.searchQuery + "\"",
                emptyAreaX + emptyAreaW * 0.5f,
                config.panelHeight() * 0.5f - 6f,
                7.2f,
                TextGradient.mist(0.75f * panelAlpha, theme),
                SatoshiWeight.MEDIUM
            );
            return;
        }

        float rScale = theme.cornerRadiusScale();
        float cardR = 12f * rScale;
        int accent = theme.accentColor();
        float startX = GlassUiChrome.contentAreaX(config, state);
        float startY = 48f;
        float areaW = config.panelWidth() - 72f;
        float bottomY = config.panelHeight() - 12f;
        float viewportH = bottomY - startY;
        int cols = 2;
        float gap = 10f;
        float cardW = (areaW - gap) * 0.5f;
        float cardH = 62f;
        int totalRows = (matched.size() + cols - 1) / cols;
        float totalH = totalRows * cardH + Math.max(0, totalRows - 1) * gap;
        state.searchScroller.updateBounds(Math.max(0f, totalH - viewportH), viewportH, totalH);

        float scrollOff = state.searchScroller.visualOffset();

        for (int idx = 0; idx < matched.size(); idx++) {
            UiCard card = matched.get(idx);
            int col = idx % cols;
            int row = idx / cols;
            float accordion = state.searchScroller.accordionShift(row, totalRows);
            float lag = clamp(-state.searchScroller.velocity() * 0.0036f * (row + 1), -2.5f, 2.5f);
            float cx = startX + col * (cardW + gap);
            float cy = startY + row * (cardH + gap) - scrollOff + accordion + lag;
            if (cy + cardH < startY - 6f || cy > bottomY + 6f) {
                continue;
            }

            float edgeFade = state.searchScroller.edgeFadeAlpha(cy, cardH, startY, bottomY);
            float cardAlpha = panelAlpha * edgeFade;

            UiHitRect hit = new UiHitRect(cx, cy, cardW, cardH);
            if (interactive) {
                state.searchResultActions.add(new UiScreenState.CardHit(card, hit));
                state.detailOpenHitboxes.add(new UiScreenState.IdHit(card.id(), hit));
            }
            boolean hov = interactive && hit.contains(lx, ly);
            boolean enabled = card.toggleable() && card.enabled();
            float hovBoost = hov ? 12f : 0f;
            float onBoost = enabled ? 9f : 0f;

            UiRenderPrimitives.drawThemedGlassCard(
                graphics,
                cx,
                cy,
                cardW,
                cardH,
                cardR,
                34f + hovBoost + onBoost,
                20f + hovBoost * 0.5f,
                50f + hovBoost * 1.5f,
                22f + hovBoost,
                cardAlpha,
                enabled ? 1f : 0f,
                hov ? 1f : 0f,
                theme
            );

            int iconTint = UiRenderPrimitives.lerpColor(
                UiRenderPrimitives.argb(Math.round(210f * cardAlpha), 255, 255, 255),
                UiRenderPrimitives.multiplyAlpha(accent, cardAlpha),
                enabled ? 0.85f : 0f
            );
            UiRenderPrimitives.drawIcon(graphics, card.icon(), cx + 11f, cy + 10.0f, 12f, iconTint);

            String clippedTitle = UiRenderPrimitives.ellipsize(card.title(), cardW - 105f, 6.8f, SatoshiWeight.SEMIBOLD);
            SatoshiFont.draw(
                graphics,
                clippedTitle,
                cx + 28f,
                cy + 10.2f,
                6.8f,
                TextGradient.pearl(cardAlpha, accent, enabled ? 0.34f : 0.16f, theme),
                SatoshiWeight.SEMIBOLD
            );

            if (card.toggleable()) {
                float swX = cx + cardW - 34f;
                UiRenderPrimitives.drawSwitch(graphics, swX, cy + 10.0f, 22f, 12f, enabled ? 1f : 0f, hov ? 1f : 0f, cardAlpha, theme);
                if (card.bindable() && config.keybinds() != null) {
                    renderKeybindPill(graphics, config, state, card.id(), swX - 6f, cy + 10.2f, lx, ly, 1f, cardAlpha, true, interactive);
                }
            } else if (card.bindable() && config.keybinds() != null) {
                renderKeybindPill(graphics, config, state, card.id(), cx + cardW - 11f, cy + 10.2f, lx, ly, 1f, cardAlpha, true, interactive);
            }

            List<String> sumLines = SatoshiFont.wrapLines(card.summary(), cardW - 22f, 4.8f, SatoshiWeight.MEDIUM);
            int maxLines = Math.min(2, sumLines.size());
            float lyText = cy + 29f;
            for (int i = 0; i < maxLines; i++) {
                SatoshiFont.draw(graphics, sumLines.get(i), cx + 11f, lyText, 4.8f, TextGradient.mist(0.72f * cardAlpha, theme), SatoshiWeight.MEDIUM);
                lyText += 7.2f;
            }
        }

        state.searchScroller.drawScrollbar(
            graphics,
            startX + areaW + 3.8f,
            startY,
            bottomY,
            lx,
            ly,
            animDt,
            panelAlpha,
            interactive,
            theme
        );
    }

    private static float cascadeFactor(float progress, int slot) {
        float delayed = clamp((progress - slot * 0.11f) / 0.65f, 0f, 1f);
        return UiRenderPrimitives.easeOutCubic(delayed);
    }

    private static float clamp(float val, float min, float max) {
        return Math.max(min, Math.min(max, val));
    }
}
