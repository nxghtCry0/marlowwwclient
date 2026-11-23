package com.eclipseware.imnotcheatingyouare.client.glassui.screen;

import com.eclipseware.imnotcheatingyouare.client.glassui.asset.LucideIcon;
import com.eclipseware.imnotcheatingyouare.client.glassui.model.UiCatalog;
import com.eclipseware.imnotcheatingyouare.client.glassui.model.UiCategory;
import com.eclipseware.imnotcheatingyouare.client.glassui.model.UiDashboardProvider;
import com.eclipseware.imnotcheatingyouare.client.glassui.model.UiHitRect;
import com.eclipseware.imnotcheatingyouare.client.glassui.model.UiTab;
import com.eclipseware.imnotcheatingyouare.client.glassui.render.KineticTextRenderer;
import com.eclipseware.imnotcheatingyouare.client.glassui.render.SatoshiFont;
import com.eclipseware.imnotcheatingyouare.client.glassui.render.SatoshiWeight;
import com.eclipseware.imnotcheatingyouare.client.glassui.render.StreamproofOverlay;
import com.eclipseware.imnotcheatingyouare.client.glassui.render.TextGradient;
import com.eclipseware.imnotcheatingyouare.client.glassui.render.UiRenderPrimitives;
import com.eclipseware.imnotcheatingyouare.client.glassui.theme.BorderStyle;
import com.eclipseware.imnotcheatingyouare.client.glassui.theme.UiTheme;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerSkin;

public final class GlassUiChrome {
    public static final float SIDEBAR_COLLAPSED_WIDTH = 36f;

    private GlassUiChrome() {
    }

    public static float expandedSidebarWidth(GlassUiConfig config) {
        UiCatalog catalog = config.catalog();
        List<UiCategory> categories = catalog.categories();
        float widest = SatoshiFont.width("Menu", 6.1f, SatoshiWeight.SEMIBOLD) + 18f;
        for (int i = 0; i < categories.size(); i++) {
            UiCategory cat = categories.get(i);
            float w = SatoshiFont.width(cat.title(), 6.1f, SatoshiWeight.SEMIBOLD);
            if (w > widest) {
                widest = w;
            }
        }
        float closeW = SatoshiFont.width("Close", 5.9f, SatoshiWeight.MEDIUM);
        if (closeW > widest) {
            widest = closeW;
        }
        float target = 30.5f + widest + 13.5f;
        return clamp(target, 92f, 132f);
    }

    public static float sidebarExtraWidth(GlassUiConfig config, UiScreenState state) {
        if (state == null) {
            return 0f;
        }
        float expand = UiRenderPrimitives.easeOutCubic(state.sidebarExpandProgress);
        if (expand <= 0.0005f) {
            return 0f;
        }
        float maxExtra = expandedSidebarWidth(config) - SIDEBAR_COLLAPSED_WIDTH;
        return Math.max(0f, maxExtra) * expand;
    }

    public static float effectivePanelWidth(GlassUiConfig config, UiScreenState state) {
        return config.panelWidth() + sidebarExtraWidth(config, state);
    }

    public static float contentAreaX(GlassUiConfig config, UiScreenState state) {
        return 56f + sidebarExtraWidth(config, state);
    }

    public static int contentScissorLeft(GlassUiConfig config, UiScreenState state) {
        return Math.round(50f + sidebarExtraWidth(config, state));
    }

    public static boolean toggleSidebar(GlassUiConfig config, UiScreenState state) {
        UiNavigationMemory nav = config.navigationMemory();
        boolean next = nav.toggleSidebarExpanded();
        state.sidebarExpanded = next;
        Runnable autoSave = config.onAutoSave();
        if (autoSave != null) {
            autoSave.run();
        }
        return next;
    }

    public static void renderWindowSurface(
        GuiGraphicsExtractor graphics,
        GlassUiConfig config,
        float panelAlpha
    ) {
        renderWindowSurface(graphics, config, null, panelAlpha);
    }

    public static void renderWindowSurface(
        GuiGraphicsExtractor graphics,
        GlassUiConfig config,
        UiScreenState state,
        float panelAlpha
    ) {
        UiTheme theme = config.theme();
        float rScale = theme.cornerRadiusScale();
        float crispMult = StreamproofOverlay.glassOpacityMultiplier();
        float opScale = theme.glassOpacityScale() * crispMult;
        float outerR = 17f * rScale;
        BorderStyle borderStyle = theme.borderStyle();
        int accent = theme.accentColor();
        float panelW = effectivePanelWidth(config, state);

        UiRenderPrimitives.drawRoundedShadow(
            graphics,
            0f,
            2f,
            panelW,
            config.panelHeight(),
            outerR,
            15f,
            UiRenderPrimitives.argb(Math.round(110f * panelAlpha), 5, 5, 6)
        );

        int topAlpha = clampInt(Math.round(232f * opScale * panelAlpha), 0, 252);
        int botAlpha = clampInt(Math.round(242f * opScale * panelAlpha), 0, 254);
        int topSurface = theme.windowTopColor(topAlpha);
        int botSurface = theme.windowBottomColor(botAlpha);

        float rimMult = switch (borderStyle) {
            case CRISP -> 1.38f;
            case MINIMAL -> 0.50f;
            default -> 1.0f;
        };
        int baseTopRim = UiRenderPrimitives.argb(clampInt(Math.round((74f * rimMult) * panelAlpha), 0, 255), 255, 255, 255);
        int baseBotRim = UiRenderPrimitives.argb(clampInt(Math.round((24f * rimMult) * panelAlpha), 0, 255), 255, 255, 255);

        int topRim = borderStyle == BorderStyle.ACCENT
            ? UiRenderPrimitives.lerpColor(baseTopRim, UiRenderPrimitives.multiplyAlpha(accent, 0.68f * panelAlpha), 0.45f)
            : baseTopRim;

        UiRenderPrimitives.drawGlassCard(
            graphics,
            0f,
            0f,
            panelW,
            config.panelHeight(),
            outerR,
            topSurface,
            botSurface,
            topRim,
            baseBotRim
        );
    }

    public static void renderLeftRail(
        GuiGraphicsExtractor graphics,
        GlassUiConfig config,
        UiScreenState state,
        int selectedCategoryIndex,
        float lx,
        float ly,
        float animDt,
        float panelAlpha
    ) {
        UiTheme theme = config.theme();
        float rScale = theme.cornerRadiusScale();
        int accent = theme.accentColor();
        float expand = UiRenderPrimitives.easeOutCubic(state.sidebarExpandProgress);
        float extraW = sidebarExtraWidth(config, state);
        float railX = 9f;
        float railY = 9f;
        float railW = SIDEBAR_COLLAPSED_WIDTH + extraW;
        float railH = config.panelHeight() - 18f;
        float railR = 12f * rScale;
        float iconCenterX = railX + 18f;

        UiRenderPrimitives.drawThemedGlassCard(
            graphics,
            railX,
            railY,
            railW,
            railH,
            railR,
            32f,
            17f,
            42f,
            18f,
            panelAlpha,
            0f,
            0f,
            theme
        );

        float toggleCy = railY + 15.5f;
        UiHitRect toggleHit = new UiHitRect(railX + 4f, toggleCy - 11.5f, railW - 8f, 23f);
        state.sidebarToggleHitbox = toggleHit;
        boolean toggleHovered = toggleHit.contains(lx, ly);
        state.sidebarToggleHover = UiRenderPrimitives.approachExp(
            state.sidebarToggleHover,
            toggleHovered ? 1f : 0f,
            16f,
            animDt
        );

        float togglePillR = Math.min(9.5f, 9.5f * rScale);
        int toggleFill = UiRenderPrimitives.lerpColor(
            UiRenderPrimitives.argb(Math.round((18f + 24f * state.sidebarToggleHover) * panelAlpha), 255, 255, 255),
            UiRenderPrimitives.multiplyAlpha(accent, (0.20f + 0.12f * state.sidebarToggleHover) * panelAlpha),
            expand * 0.75f
        );
        UiRenderPrimitives.drawRoundedRect(
            graphics,
            railX + 6f,
            toggleCy - 10.5f,
            railW - 12f,
            21f,
            togglePillR,
            toggleFill
        );
        int toggleRim = UiRenderPrimitives.lerpColor(
            UiRenderPrimitives.argb(Math.round((28f + 38f * state.sidebarToggleHover) * panelAlpha), 255, 255, 255),
            UiRenderPrimitives.multiplyAlpha(accent, (0.48f + 0.28f * state.sidebarToggleHover) * panelAlpha),
            expand * 0.65f
        );
        UiRenderPrimitives.drawRoundedBorder(
            graphics,
            railX + 6f,
            toggleCy - 10.5f,
            railW - 12f,
            21f,
            togglePillR,
            0.85f,
            toggleRim
        );

        int barWhite = UiRenderPrimitives.argb(
            Math.round((196f + 59f * Math.max(state.sidebarToggleHover, expand * 0.6f)) * panelAlpha),
            255,
            255,
            255
        );
        int barTint = UiRenderPrimitives.lerpColor(
            barWhite,
            UiRenderPrimitives.multiplyAlpha(accent, panelAlpha),
            expand * 0.45f
        );
        float barLeft = iconCenterX - 5.2f;
        UiRenderPrimitives.drawRoundedRect(graphics, barLeft, toggleCy - 3.9f, 10.4f, 1.35f, 0.68f, barTint);
        UiRenderPrimitives.drawRoundedRect(graphics, barLeft + 1.4f * expand, toggleCy - 0.68f, 10.4f - 2.2f * expand, 1.35f, 0.68f, barTint);
        UiRenderPrimitives.drawRoundedRect(graphics, barLeft, toggleCy + 2.55f, 10.4f - 1.0f * (1f - expand), 1.35f, 0.68f, barTint);

        UiRenderPrimitives.drawRoundedRect(
            graphics,
            railX + 7f,
            railY + 33f,
            railW - 14f,
            1f,
            0.5f,
            UiRenderPrimitives.argb(Math.round(28f * panelAlpha), 255, 255, 255)
        );

        float firstCatCy = railY + 50f;
        UiCatalog catalog = config.catalog();
        List<UiCategory> categories = catalog.categories();
        if (!categories.isEmpty()) {
            float targetIndicatorY = firstCatCy + selectedCategoryIndex * 31f;
            state.railIndicatorY = state.railIndicatorY < 0f
                ? targetIndicatorY
                : UiRenderPrimitives.approachExp(state.railIndicatorY, targetIndicatorY, 18f, animDt);

            float pillR = Math.min(12f, 12f * rScale);
            UiRenderPrimitives.drawThemedGlassCard(
                graphics,
                railX + 6f,
                state.railIndicatorY - 12f,
                railW - 12f,
                24f,
                pillR,
                52f,
                30f,
                76f,
                34f,
                panelAlpha,
                0.55f,
                0f,
                theme
            );

            for (int i = 0; i < categories.size(); i++) {
                UiCategory cat = categories.get(i);
                float cy = firstCatCy + i * 31f;
                UiHitRect hit = new UiHitRect(railX + 4f, cy - 13f, railW - 8f, 26f);
                state.categoryHitboxes.add(new UiScreenState.IndexedHit(i, hit));

                boolean hovered = hit.contains(lx, ly);
                boolean isSelected = i == selectedCategoryIndex && !state.dashboardOpen;
                cat.animHover = UiRenderPrimitives.approachExp(cat.animHover, hovered ? 1f : 0f, 16f, animDt);
                cat.animActive = UiRenderPrimitives.approachExp(cat.animActive, isSelected ? 1f : 0f, 16f, animDt);

                if (cat.animHover > 0.01f && !isSelected) {
                    UiRenderPrimitives.drawRoundedRect(
                        graphics,
                        railX + 7f,
                        cy - 11f,
                        railW - 14f,
                        22f,
                        Math.min(11f, 11f * rScale),
                        UiRenderPrimitives.argb(Math.round(26f * cat.animHover * panelAlpha), 255, 255, 255)
                    );
                }

                float iconSize = 12.5f + 0.8f * Math.max(cat.animHover, cat.animActive * 0.5f);
                int baseWhite = UiRenderPrimitives.argb(
                    Math.round((168f + 87f * Math.max(cat.animActive, cat.animHover * 0.7f)) * panelAlpha),
                    255,
                    255,
                    255
                );
                int iconTint = UiRenderPrimitives.lerpColor(
                    baseWhite,
                    UiRenderPrimitives.multiplyAlpha(accent, panelAlpha),
                    cat.animActive * 0.35f
                );
                UiRenderPrimitives.drawIcon(
                    graphics,
                    cat.icon(),
                    iconCenterX - iconSize * 0.5f,
                    cy - iconSize * 0.5f,
                    iconSize,
                    iconTint
                );
            }
        }

        float powerCy = railY + railH - 21f;
        UiHitRect pHit = new UiHitRect(railX + 4f, powerCy - 13f, railW - 8f, 26f);
        state.powerHitbox = pHit;
        boolean pHovered = pHit.contains(lx, ly);
        state.powerHover = UiRenderPrimitives.approachExp(state.powerHover, pHovered ? 1f : 0f, 16f, animDt);

        if (state.powerHover > 0.01f) {
            UiRenderPrimitives.drawRoundedRect(
                graphics,
                railX + 7f,
                powerCy - 11f,
                railW - 14f,
                22f,
                Math.min(11f, 11f * rScale),
                UiRenderPrimitives.argb(Math.round(36f * state.powerHover * panelAlpha), 255, 110, 100)
            );
        }
        float pSize = 12.5f + 0.8f * state.powerHover;
        int pColor = UiRenderPrimitives.lerpColor(
            UiRenderPrimitives.argb(Math.round(195f * panelAlpha), 255, 255, 255),
            UiRenderPrimitives.argb(Math.round(255f * panelAlpha), 255, 145, 138),
            state.powerHover
        );
        UiRenderPrimitives.drawIcon(
            graphics,
            LucideIcon.POWER,
            iconCenterX - pSize * 0.5f,
            powerCy - pSize * 0.5f,
            pSize,
            pColor
        );

        if (expand > 0.04f) {
            float textAlpha = clamp((expand - 0.10f) / 0.90f, 0f, 1f) * panelAlpha;
            int clipLeft = Math.round(railX + 27f);
            int clipRight = Math.round(railX + railW - 6f);
            if (textAlpha > 0.01f && clipRight > clipLeft + 2) {
                float slideX = (1f - expand) * -5f;
                float labelX = railX + 30.5f + slideX;
                UiRenderPrimitives.enableScissor(
                    graphics,
                    clipLeft,
                    Math.round(railY),
                    clipRight,
                    Math.round(railY + railH)
                );

                float menuFontSz = 5.8f;
                TextGradient menuGrad = TextGradient.pearl(
                    textAlpha * (0.86f + 0.14f * state.sidebarToggleHover),
                    accent,
                    0.28f,
                    theme
                );
                SatoshiFont.draw(
                    graphics,
                    "Menu",
                    labelX,
                    toggleCy - menuFontSz * 0.5f,
                    menuFontSz,
                    menuGrad,
                    SatoshiWeight.SEMIBOLD
                );

                int collapseTint = UiRenderPrimitives.lerpColor(
                    UiRenderPrimitives.argb(Math.round(150f * textAlpha), 255, 255, 255),
                    UiRenderPrimitives.multiplyAlpha(accent, textAlpha),
                    Math.max(0.35f, state.sidebarToggleHover)
                );
                UiRenderPrimitives.drawIcon(
                    graphics,
                    LucideIcon.ARROW_LEFT,
                    railX + railW - 16.5f,
                    toggleCy - 4.1f,
                    8.2f,
                    collapseTint
                );

                float catFontSz = 6.1f;
                for (int i = 0; i < categories.size(); i++) {
                    UiCategory cat = categories.get(i);
                    float cy = firstCatCy + i * 31f;
                    SatoshiWeight catWeight = cat.animActive > 0.5f ? SatoshiWeight.SEMIBOLD : SatoshiWeight.MEDIUM;
                    float catBlend = Math.max(cat.animActive, cat.animHover * 0.72f);
                    TextGradient catGrad = TextGradient.lerp(
                        TextGradient.mist(0.76f * textAlpha, theme),
                        TextGradient.pearl(textAlpha, accent, 0.34f, theme),
                        catBlend
                    );
                    SatoshiFont.draw(
                        graphics,
                        cat.title(),
                        labelX,
                        cy - catFontSz * 0.5f,
                        catFontSz,
                        catGrad,
                        catWeight
                    );
                }

                float closeFontSz = 5.9f;
                TextGradient closeWarmBase = new TextGradient(0xFFFF9B94, 0xFFFF827A, 0xFFFFB2AC, 0xFFFF827A);
                TextGradient closeGrad = TextGradient.lerp(
                    TextGradient.mist(0.74f * textAlpha, theme),
                    closeWarmBase.withOpacity(textAlpha),
                    state.powerHover
                );
                SatoshiFont.draw(
                    graphics,
                    "Close",
                    labelX,
                    powerCy - closeFontSz * 0.5f,
                    closeFontSz,
                    closeGrad,
                    SatoshiWeight.MEDIUM
                );

                UiRenderPrimitives.disableScissor(graphics);
            }
        }
    }

    public static void renderHeader(
        GuiGraphicsExtractor graphics,
        Minecraft minecraft,
        GlassUiConfig config,
        UiScreenState state,
        int selectedCategoryIndex,
        int selectedTabIndex,
        float lx,
        float ly,
        float animDt,
        float panelAlpha
    ) {
        UiTheme theme = config.theme();
        float rScale = theme.cornerRadiusScale();
        int accent = theme.accentColor();
        int secondary = theme.secondaryAccentColor();
        UiCatalog catalog = config.catalog();
        List<UiCategory> categories = catalog.categories();
        UiCategory category = (selectedCategoryIndex >= 0 && selectedCategoryIndex < categories.size())
            ? categories.get(selectedCategoryIndex)
            : null;
        List<UiTab> tabs = category != null ? category.tabs() : List.of();

        float panelW = effectivePanelWidth(config, state);
        float extraW = sidebarExtraWidth(config, state);
        float rightCursorX = panelW - 14f;

        UiDashboardProvider dashboard = config.dashboard();
        if (dashboard != null) {
            float avatarCx = rightCursorX - 12f;
            float avatarCy = 24f;
            float avatarR = 10.5f;
            UiHitRect avHit = new UiHitRect(avatarCx - 13f, avatarCy - 13f, 26f, 26f);
            state.avatarHitbox = avHit;
            boolean avHovered = avHit.contains(lx, ly);
            state.avatarHover = UiRenderPrimitives.approachExp(
                state.avatarHover,
                (avHovered || state.dashboardOpen) ? 1f : 0f,
                16f,
                animDt
            );

            Identifier customSkin = dashboard.avatarSkin(minecraft);
            Identifier skinTex = customSkin != null ? customSkin : resolveDefaultPlayerSkin(minecraft);
            float drawR = avatarR + 0.8f * state.avatarHover;
            UiRenderPrimitives.drawRoundedShadow(
                graphics,
                avatarCx - drawR,
                avatarCy - drawR + 1f,
                drawR * 2f,
                drawR * 2f,
                drawR,
                4.5f,
                UiRenderPrimitives.argb(Math.round(82f * panelAlpha), 0, 0, 0)
            );
            UiRenderPrimitives.drawCircularPlayerAvatar(
                graphics,
                skinTex,
                avatarCx,
                avatarCy,
                drawR,
                UiRenderPrimitives.argb(Math.round(255f * panelAlpha), 255, 255, 255)
            );

            int ringColor = UiRenderPrimitives.lerpColor(
                UiRenderPrimitives.argb(Math.round(95f * panelAlpha), 255, 255, 255),
                UiRenderPrimitives.multiplyAlpha(accent, panelAlpha),
                Math.max(state.avatarHover * 0.85f, state.dashboardProgress)
            );
            UiRenderPrimitives.drawRoundedBorder(
                graphics,
                avatarCx - drawR,
                avatarCy - drawR,
                drawR * 2f,
                drawR * 2f,
                drawR,
                1.25f,
                ringColor
            );

            int dotColor = dashboard.hasActiveStatus() ? accent : 0xFF8C94A0;
            float dotX = avatarCx + drawR - 3.6f;
            float dotY = avatarCy + drawR - 3.6f;
            UiRenderPrimitives.drawRoundedRect(
                graphics,
                dotX - 0.8f,
                dotY - 0.8f,
                5.2f,
                5.2f,
                2.6f,
                UiRenderPrimitives.argb(Math.round(220f * panelAlpha), 18, 18, 20)
            );
            UiRenderPrimitives.drawRoundedRect(
                graphics,
                dotX,
                dotY,
                3.6f,
                3.6f,
                1.8f,
                UiRenderPrimitives.multiplyAlpha(dotColor, panelAlpha)
            );

            rightCursorX = avatarCx - 17f;
        }

        if (config.showSaveButton() && config.onSave() != null) {
            float saveSize = 20f;
            float saveX = rightCursorX - saveSize;
            float saveY = 14f;
            UiHitRect svHit = new UiHitRect(saveX, saveY, saveSize, saveSize);
            state.saveHitbox = svHit;
            state.saveHover = UiRenderPrimitives.approachExp(state.saveHover, svHit.contains(lx, ly) ? 1f : 0f, 16f, animDt);

            float saveR = Math.min(7f, 7f * rScale);
            float confirmBlend = clamp(state.saveConfirmTimer / 1.2f, 0f, 1f);
            int saveBg = UiRenderPrimitives.lerpColor(
                UiRenderPrimitives.argb(Math.round((96f + 36f * state.saveHover) * panelAlpha), 20, 20, 22),
                UiRenderPrimitives.multiplyAlpha(accent, 0.35f * panelAlpha),
                confirmBlend
            );
            UiRenderPrimitives.drawRoundedRect(graphics, saveX, saveY, saveSize, saveSize, saveR, saveBg);
            UiRenderPrimitives.drawRoundedBorder(
                graphics,
                saveX,
                saveY,
                saveSize,
                saveSize,
                saveR,
                0.9f,
                UiRenderPrimitives.argb(Math.round((36f + 42f * Math.max(state.saveHover, confirmBlend)) * panelAlpha), 255, 255, 255)
            );
            LucideIcon saveIcon = state.saveConfirmTimer > 0.15f ? LucideIcon.CHECK : LucideIcon.SAVE;
            int saveIconTint = state.saveConfirmTimer > 0.15f
                ? UiRenderPrimitives.multiplyAlpha(accent, panelAlpha)
                : UiRenderPrimitives.argb(Math.round((185f + 65f * state.saveHover) * panelAlpha), 255, 255, 255);
            UiRenderPrimitives.drawIcon(graphics, saveIcon, saveX + 4.75f, saveY + 4.75f, 10.5f, saveIconTint);

            rightCursorX = saveX - 8f;
        }

        if (config.searchEnabled()) {
            float searchW = 136f;
            float searchX = rightCursorX - searchW;
            float searchY = 14f;
            float searchH = 20f;

            UiHitRect sHit = new UiHitRect(searchX, searchY, searchW, searchH);
            state.searchHitbox = sHit;
            state.searchHover = UiRenderPrimitives.approachExp(state.searchHover, sHit.contains(lx, ly) ? 1f : 0f, 16f, animDt);
            state.searchFocusAnim = UiRenderPrimitives.approachExp(state.searchFocusAnim, state.searchFocused ? 1f : 0f, 16f, animDt);

            float searchRadius = Math.min(10f, 10f * rScale);
            int searchFill = UiRenderPrimitives.lerpColor(
                UiRenderPrimitives.argb(Math.round(96f * panelAlpha), 18, 18, 20),
                UiRenderPrimitives.argb(Math.round(138f * panelAlpha), 13, 13, 15),
                Math.max(state.searchFocusAnim, state.searchHover * 0.5f)
            );
            int searchBorder = UiRenderPrimitives.lerpColor(
                UiRenderPrimitives.argb(Math.round(38f * panelAlpha), 255, 255, 255),
                UiRenderPrimitives.multiplyAlpha(accent, (0.86f + 0.14f * state.searchKeyPulse) * panelAlpha),
                state.searchFocusAnim
            );
            UiRenderPrimitives.drawRoundedRect(graphics, searchX, searchY, searchW, searchH, searchRadius, searchFill);
            UiRenderPrimitives.drawRoundedBorder(graphics, searchX, searchY, searchW, searchH, searchRadius, 0.9f, searchBorder, searchBorder);

            int searchIconColor = UiRenderPrimitives.lerpColor(
                UiRenderPrimitives.argb(Math.round(150f * panelAlpha), 255, 255, 255),
                UiRenderPrimitives.multiplyAlpha(accent, panelAlpha),
                Math.max(state.searchFocusAnim * 0.85f, state.searchKeyPulse)
            );
            UiRenderPrimitives.drawIcon(graphics, LucideIcon.SEARCH, searchX + 7f, searchY + 5f, 10f, searchIconColor);

            float textX = searchX + 20.5f;
            float searchFontSize = 5.9f;
            float searchTextY = searchY + (searchH - searchFontSize) * 0.5f;
            float rightPad = !state.searchQuery.isEmpty() ? 20f : 8f;

            if (state.searchQuery.isEmpty() && !state.searchFocused) {
                state.searchCaretX = -1f;
                KineticTextRenderer.drawSearchPlaceholder(
                    graphics,
                    config.searchPrompts(),
                    textX,
                    searchTextY,
                    searchW - 28f,
                    searchFontSize,
                    state.elapsedSeconds,
                    accent,
                    secondary,
                    panelAlpha,
                    theme
                );
            } else {
                String visible = SatoshiFont.trimTailToWidth(state.searchQuery, searchW - 21f - rightPad, searchFontSize, SatoshiWeight.MEDIUM);
                SatoshiFont.draw(
                    graphics,
                    visible,
                    textX,
                    searchTextY,
                    searchFontSize,
                    TextGradient.accent(0.98f * panelAlpha, accent, theme),
                    SatoshiWeight.MEDIUM
                );
                if (state.searchFocused) {
                    float pulse = (float) Math.sin(state.elapsedSeconds * 6.2f) * 0.5f + 0.5f;
                    float targetCaretOff = SatoshiFont.width(visible, searchFontSize, SatoshiWeight.MEDIUM) + 1.3f;
                    state.searchCaretX = state.searchCaretX < 0f
                        ? targetCaretOff
                        : UiRenderPrimitives.approachExp(state.searchCaretX, targetCaretOff, 28f, animDt);
                    float caretDrawX = textX + state.searchCaretX;
                    if (state.searchKeyPulse > 0.05f) {
                        float tailW = Math.min(14f, 6f + state.searchKeyPulse * 8f);
                        UiRenderPrimitives.drawRoundedGradient(
                            graphics,
                            Math.max(textX, caretDrawX - tailW),
                            searchY + searchH - 3.6f,
                            tailW,
                            1.4f,
                            0.7f,
                            UiRenderPrimitives.multiplyAlpha(accent, 0.55f * state.searchKeyPulse * panelAlpha),
                            UiRenderPrimitives.multiplyAlpha(secondary, 0.15f * state.searchKeyPulse * panelAlpha)
                        );
                    }
                    float caretH = 8.4f + state.searchKeyPulse * 1.4f;
                    float caretY = searchY + (searchH - caretH) * 0.5f;
                    float caretAlpha = (0.42f + 0.48f * Math.max(pulse, state.searchKeyPulse)) * panelAlpha;
                    UiRenderPrimitives.drawRoundedRect(
                        graphics,
                        caretDrawX,
                        caretY,
                        1.2f,
                        caretH,
                        0.6f,
                        UiRenderPrimitives.multiplyAlpha(accent, caretAlpha)
                    );
                }
            }

            if (!state.searchQuery.isEmpty()) {
                float cx = searchX + searchW - 14f;
                float cy = searchY + 5.5f;
                UiHitRect cHit = new UiHitRect(cx - 3f, searchY + 2f, 15f, 16f);
                state.searchClearHitbox = cHit;
                state.searchClearHover = UiRenderPrimitives.approachExp(state.searchClearHover, cHit.contains(lx, ly) ? 1f : 0f, 16f, animDt);
                UiRenderPrimitives.drawIcon(
                    graphics,
                    LucideIcon.X_CLOSE,
                    cx,
                    cy,
                    9f,
                    UiRenderPrimitives.argb(Math.round((155f + 95f * state.searchClearHover) * panelAlpha), 255, 255, 255)
                );
            } else {
                state.searchClearHitbox = UiHitRect.EMPTY;
            }
            rightCursorX = searchX - 10f;
        }

        float tabStartX = 58f + extraW;
        float tabCursorX = tabStartX;
        float tabY = 20.5f;
        float tabFontSize = tabs.size() >= 6 ? 6.7f : 7.3f;
        float tabGap = tabs.size() >= 6 ? 12.5f : 16.5f;
        float targetUnderOffset = 0f;
        float targetUnderW = 30f;

        for (int i = 0; i < tabs.size(); i++) {
            UiTab tab = tabs.get(i);
            SatoshiWeight weight = tab.animActive > 0.5f ? SatoshiWeight.SEMIBOLD : SatoshiWeight.MEDIUM;
            float textW = SatoshiFont.width(tab.title(), tabFontSize, weight);
            if (tabCursorX + textW > rightCursorX && i > 0) {
                break;
            }
            UiHitRect hit = new UiHitRect(tabCursorX - 4f, tabY - 5f, textW + 8f, 18f);
            state.subtabHitboxes.add(new UiScreenState.IndexedHit(i, hit));

            boolean hovered = hit.contains(lx, ly);
            boolean active = i == selectedTabIndex && state.searchQuery.isBlank() && state.openDetailCardId == null && !state.dashboardOpen;
            tab.animHover = UiRenderPrimitives.approachExp(tab.animHover, hovered ? 1f : 0f, 16f, animDt);
            tab.animActive = UiRenderPrimitives.approachExp(tab.animActive, active ? 1f : 0f, 16f, animDt);

            if (i == selectedTabIndex) {
                targetUnderOffset = tabCursorX - tabStartX;
                targetUnderW = textW;
            }

            float blend = Math.max(tab.animActive, tab.animHover * 0.65f);
            TextGradient grad = TextGradient.lerp(
                TextGradient.mist(0.58f * panelAlpha, theme),
                TextGradient.pearl(panelAlpha, accent, 0.34f, theme),
                blend
            );
            SatoshiFont.draw(graphics, tab.title(), tabCursorX, tabY, tabFontSize, grad, weight);
            tabCursorX += textW + tabGap;
        }

        state.subtabUnderlineX = state.subtabUnderlineX < 0f
            ? targetUnderOffset
            : UiRenderPrimitives.approachExp(state.subtabUnderlineX, targetUnderOffset, 18f, animDt);
        state.subtabUnderlineW = UiRenderPrimitives.approachExp(state.subtabUnderlineW, targetUnderW, 18f, animDt);

        if (!tabs.isEmpty() && state.searchQuery.isBlank() && state.openDetailCardId == null && !state.dashboardOpen) {
            UiRenderPrimitives.drawRoundedGradient(
                graphics,
                tabStartX + state.subtabUnderlineX,
                tabY + tabFontSize + 3.2f,
                state.subtabUnderlineW,
                1.7f,
                0.85f,
                UiRenderPrimitives.multiplyAlpha(accent, 0.92f * panelAlpha),
                UiRenderPrimitives.multiplyAlpha(secondary, 0.82f * panelAlpha)
            );
        }
    }

    public static void renderRipples(GuiGraphicsExtractor graphics, UiScreenState state, float dt, float panelAlpha) {
        Iterator<UiRipple> iter = state.ripples.iterator();
        while (iter.hasNext()) {
            UiRipple r = iter.next();
            r.progress += dt * 3.4f;
            if (r.progress >= 1f) {
                iter.remove();
                continue;
            }
            float eased = UiRenderPrimitives.easeOutCubic(r.progress);
            float rad = 4f + (r.maxRadius - 4f) * eased;
            float fade = (1f - eased) * panelAlpha;
            UiRenderPrimitives.drawRoundedBorder(
                graphics,
                r.x - rad,
                r.y - rad,
                rad * 2f,
                rad * 2f,
                rad,
                1.1f,
                UiRenderPrimitives.multiplyAlpha(r.color, 0.55f * fade)
            );
        }
    }

    public static void renderToast(GuiGraphicsExtractor graphics, GlassUiConfig config, UiScreenState state, float panelAlpha) {
        if (state.toastAnim <= 0.01f || state.toastText.isEmpty()) {
            return;
        }
        UiTheme theme = config.theme();
        float a = state.toastAnim * panelAlpha;
        int accent = theme.accentColor();
        int secondary = theme.secondaryAccentColor();
        float toastFontSize = 5.8f;
        float fullW = SatoshiFont.width(state.toastText, toastFontSize, SatoshiWeight.MEDIUM);
        float boxW = fullW + 28f;
        float boxH = 20f;
        float panelW = effectivePanelWidth(config, state);
        float railRight = 45f + sidebarExtraWidth(config, state);
        float bx = (panelW + railRight - boxW) * 0.5f;
        float by = config.panelHeight() - 28f + (1f - state.toastAnim) * 8f;
        float r = Math.min(10f, 10f * theme.cornerRadiusScale());

        UiRenderPrimitives.drawRoundedShadow(
            graphics,
            bx,
            by + 1f,
            boxW,
            boxH,
            r,
            6f,
            UiRenderPrimitives.argb(Math.round(95f * a), 0, 0, 0)
        );
        UiRenderPrimitives.drawGlassCard(
            graphics,
            bx,
            by,
            boxW,
            boxH,
            r,
            theme.windowTopColor(clampInt(Math.round(232f * a), 0, 250)),
            theme.windowBottomColor(clampInt(Math.round(242f * a), 0, 252)),
            UiRenderPrimitives.argb(Math.round(84f * a), 255, 255, 255),
            UiRenderPrimitives.argb(Math.round(34f * a), 255, 255, 255)
        );
        UiRenderPrimitives.drawIcon(
            graphics,
            LucideIcon.CHECK,
            bx + 7.5f,
            by + 5f,
            10f,
            UiRenderPrimitives.multiplyAlpha(accent, a)
        );
        KineticTextRenderer.drawKineticDecode(
            graphics,
            state.toastText,
            bx + 20f,
            by + (boxH - toastFontSize) * 0.5f,
            fullW + 4f,
            toastFontSize,
            TextGradient.accent(0.98f * a, accent, theme),
            SatoshiWeight.MEDIUM,
            state.elapsedSeconds - state.toastCreatedSec,
            accent,
            secondary,
            a,
            0.24f,
            true,
            theme.typingAnimationEnabled(),
            theme
        );
    }

    public static Identifier resolveDefaultPlayerSkin(Minecraft minecraft) {
        LocalPlayer localPlayer = minecraft.player;
        if (localPlayer != null) {
            PlayerSkin skin = localPlayer.getSkin();
            var body = skin.body();
            return body.texturePath();
        }
        User user = minecraft.getUser();
        UUID profileId = user.getProfileId();
        PlayerSkin defSkin = DefaultPlayerSkin.get(profileId);
        var defBody = defSkin.body();
        return defBody.texturePath();
    }

    private static float clamp(float val, float min, float max) {
        return Math.max(min, Math.min(max, val));
    }

    private static int clampInt(int val, int min, int max) {
        return Math.max(min, Math.min(max, val));
    }
}
