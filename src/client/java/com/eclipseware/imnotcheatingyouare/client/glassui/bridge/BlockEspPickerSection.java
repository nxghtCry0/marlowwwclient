package com.eclipseware.imnotcheatingyouare.client.glassui.bridge;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.glassui.asset.LucideIcon;
import com.eclipseware.imnotcheatingyouare.client.glassui.audio.UiSoundSink;
import com.eclipseware.imnotcheatingyouare.client.glassui.model.UiCustomSection;
import com.eclipseware.imnotcheatingyouare.client.glassui.model.UiHitRect;
import com.eclipseware.imnotcheatingyouare.client.glassui.model.UiScreenActions;
import com.eclipseware.imnotcheatingyouare.client.glassui.model.UiSectionMeasureContext;
import com.eclipseware.imnotcheatingyouare.client.glassui.model.UiSectionRenderContext;
import com.eclipseware.imnotcheatingyouare.client.glassui.render.BlockIconRenderer;
import com.eclipseware.imnotcheatingyouare.client.glassui.render.SatoshiFont;
import com.eclipseware.imnotcheatingyouare.client.glassui.render.SatoshiWeight;
import com.eclipseware.imnotcheatingyouare.client.glassui.render.SmoothScroller;
import com.eclipseware.imnotcheatingyouare.client.glassui.render.TextGradient;
import com.eclipseware.imnotcheatingyouare.client.glassui.render.UiRenderPrimitives;
import com.eclipseware.imnotcheatingyouare.client.glassui.theme.UiTheme;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.module.ModuleManager;
import com.eclipseware.imnotcheatingyouare.client.module.impl.BlockESP;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.resources.Identifier;

public final class BlockEspPickerSection implements UiCustomSection {
    public static final BlockEspPickerSection INSTANCE = new BlockEspPickerSection();

    private record StringHit(String id, UiHitRect rect) {
    }

    private record CatalogHit(BlockESP.CatalogBlockInfo entry, UiHitRect rect) {
    }

    private static final String[][] PRESETS = {
        {"pvp", "PvP"},
        {"ores", "Ores"},
        {"base", "Base"},
        {"clear", "Clear"}
    };

    private boolean blockSearchFocused = false;
    private float blockSearchFocusAnim = 0f;
    private float blockSearchHover = 0f;
    private String blockSearchQuery = "";
    private float blockSearchCaretX = -1f;
    private float blockSearchKeyPulse = 0f;
    private float blockSearchClearHover = 0f;

    private final Map<String, Float> trackedHoverMap = new HashMap<>();
    private final Map<String, Float> catalogHoverMap = new HashMap<>();
    private final float[] blockPresetHovers = new float[4];

    private UiHitRect blockSearchHitbox = UiHitRect.EMPTY;
    private UiHitRect blockSearchClearHitbox = UiHitRect.EMPTY;
    private final List<StringHit> blockPresetHitboxes = new ArrayList<>();
    private final List<StringHit> trackedColorHitboxes = new ArrayList<>();
    private final List<StringHit> trackedRemoveHitboxes = new ArrayList<>();
    private final List<CatalogHit> catalogToggleHitboxes = new ArrayList<>();

    private BlockEspPickerSection() {
    }

    private static BlockESP resolveBlockEsp() {
        ImnotcheatingyouareClient client = ImnotcheatingyouareClient.INSTANCE;
        if (client == null || client.moduleManager == null) {
            return null;
        }
        ModuleManager mm = client.moduleManager;
        Module mod = mm.getModule("BlockESP");
        return mod instanceof BlockESP besp ? besp : null;
    }

    @Override
    public float measureHeight(UiSectionMeasureContext context) {
        BlockESP besp = resolveBlockEsp();
        if (besp == null) {
            return 40f;
        }
        int trackedCount = besp.getSelectedBlockCount();
        int trackedRows = trackedCount == 0 ? 0 : (trackedCount + 2) / 3;
        float trackedGridH = trackedCount == 0 ? 28f : (trackedRows * 30f + Math.max(0, trackedRows - 1) * 5.5f);
        int catalogLimit = blockSearchQuery.isBlank() ? 48 : 96;
        List<BlockESP.CatalogBlockInfo> matches = besp.searchBlockCatalog(blockSearchQuery, catalogLimit);
        int catRows = matches.isEmpty() ? 0 : (matches.size() + 2) / 3;
        float catGridH = matches.isEmpty() ? 28f : (catRows * 26f + Math.max(0, catRows - 1) * 5.5f);
        return 22f + trackedGridH + 10f + 24f + catGridH + 8f;
    }

    @Override
    public void render(UiSectionRenderContext context) {
        BlockESP besp = resolveBlockEsp();
        if (besp == null) {
            return;
        }
        GuiGraphicsExtractor graphics = context.graphics();
        SmoothScroller scroller = context.scroller();
        float areaX = context.areaX();
        float areaW = context.areaW();
        float viewTop = context.viewTop();
        float viewBottom = context.viewBottom();
        float lx = context.mouseX();
        float ly = context.mouseY();
        float animDt = context.animDt();
        float alpha = context.alpha();
        boolean interactive = context.interactive();
        UiTheme theme = context.theme();
        int accent = theme.accentColor();
        float rScale = theme.cornerRadiusScale();
        float cardR = 9f * rScale;

        List<BlockESP.TrackedBlockInfo> tracked = besp.getTrackedEntries();
        blockSearchKeyPulse = UiRenderPrimitives.approachExp(blockSearchKeyPulse, 0f, 12f, animDt);

        float curY = context.startY();
        if (curY + 20f >= viewTop - 6f && curY <= viewBottom + 6f) {
            float headFade = alpha * scroller.edgeFadeAlpha(curY, 18f, viewTop, viewBottom);
            UiRenderPrimitives.drawIcon(graphics, LucideIcon.CUBOID, areaX + 2f, curY + 3f, 10f, UiRenderPrimitives.multiplyAlpha(accent, headFade));
            SatoshiFont.draw(
                graphics,
                "Tracked Blocks (" + tracked.size() + ")",
                areaX + 15f,
                curY + 4.5f,
                6.2f,
                TextGradient.pearl(headFade, accent, 0.28f, theme),
                SatoshiWeight.SEMIBOLD
            );

            float rx = areaX + areaW;
            for (int i = PRESETS.length - 1; i >= 0; i--) {
                String pId = PRESETS[i][0];
                String pLbl = PRESETS[i][1];
                float pw = Math.max(32f, SatoshiFont.width(pLbl, 4.7f, SatoshiWeight.SEMIBOLD) + 12f);
                float px = rx - pw;
                float ph = 14.5f;
                float py = curY + 1.5f;
                UiHitRect pHit = new UiHitRect(px, py, pw, ph);
                if (interactive) {
                    blockPresetHitboxes.add(new StringHit(pId, pHit));
                }
                boolean pHov = interactive && pHit.contains(lx, ly);
                blockPresetHovers[i] = UiRenderPrimitives.approachExp(blockPresetHovers[i], pHov ? 1f : 0f, 16f, animDt);
                UiRenderPrimitives.drawThemedGlassCard(
                    graphics, px, py, pw, ph, Math.min(7f, 7f * rScale),
                    32f + 22f * blockPresetHovers[i],
                    18f + 14f * blockPresetHovers[i],
                    54f + 30f * blockPresetHovers[i],
                    24f + 16f * blockPresetHovers[i],
                    headFade,
                    "clear".equals(pId) ? 0f : blockPresetHovers[i] * 0.45f,
                    blockPresetHovers[i],
                    theme
                );
                TextGradient btnGrad = ("clear".equals(pId) && blockPresetHovers[i] > 0.2f)
                    ? TextGradient.accent(headFade, 0xFFFF6470, theme)
                    : TextGradient.silver(0.92f * headFade, theme);
                SatoshiFont.drawCentered(graphics, pLbl, px + pw * 0.5f, py + (ph - 4.7f) * 0.5f, 4.7f, btnGrad, SatoshiWeight.SEMIBOLD);
                rx = px - 5f;
            }
        }
        curY += 22f;

        int cols = 3;
        float colGap = 7f;
        float tileW = (areaW - colGap * (cols - 1)) / cols;

        if (tracked.isEmpty()) {
            if (curY + 28f >= viewTop - 6f && curY <= viewBottom + 6f) {
                float eFade = alpha * scroller.edgeFadeAlpha(curY, 28f, viewTop, viewBottom);
                UiRenderPrimitives.drawThemedGlassCard(graphics, areaX, curY, areaW, 28f, cardR, 26f, 15f, 38f, 16f, eFade, 0f, 0f, theme);
                SatoshiFont.drawCentered(
                    graphics,
                    "No blocks tracked. Pick any block below or load a preset above.",
                    areaX + areaW * 0.5f,
                    curY + 11f,
                    5.2f,
                    TextGradient.mist(0.76f * eFade, theme),
                    SatoshiWeight.MEDIUM
                );
            }
            curY += 28f;
        } else {
            int trackedRows = (tracked.size() + cols - 1) / cols;
            float tileH = 30f;
            float rowGap = 5.5f;
            for (int i = 0; i < tracked.size(); i++) {
                BlockESP.TrackedBlockInfo entry = tracked.get(i);
                int col = i % cols;
                int row = i / cols;
                float tx = areaX + col * (tileW + colGap);
                float ty = curY + row * (tileH + rowGap);
                if (ty + tileH >= viewTop - 6f && ty <= viewBottom + 6f) {
                    float tFade = alpha * scroller.edgeFadeAlpha(ty, tileH, viewTop, viewBottom);
                    UiHitRect tileHit = new UiHitRect(tx, ty, tileW, tileH);
                    boolean hov = interactive && tileHit.contains(lx, ly);
                    float prev = trackedHoverMap.getOrDefault(entry.blockId(), 0f);
                    float curHov = UiRenderPrimitives.approachExp(prev, hov ? 1f : 0f, 16f, animDt);
                    trackedHoverMap.put(entry.blockId(), curHov);

                    UiRenderPrimitives.drawThemedGlassCard(
                        graphics, tx, ty, tileW, tileH, cardR,
                        38f + 12f * curHov,
                        18f + 8f * curHov,
                        50f + 22f * curHov,
                        22f + 10f * curHov,
                        tFade,
                        0.50f,
                        curHov,
                        theme
                    );

                    int iconBoxBg = UiRenderPrimitives.multiplyAlpha(entry.color(), 0.24f * tFade);
                    int iconBoxRim = UiRenderPrimitives.multiplyAlpha(entry.color(), 0.72f * tFade);
                    UiRenderPrimitives.drawRoundedRect(graphics, tx + 5.5f, ty + 4.5f, 21f, 21f, 6f, iconBoxBg);
                    UiRenderPrimitives.drawRoundedBorder(graphics, tx + 5.5f, ty + 4.5f, 21f, 21f, 6f, 0.8f, iconBoxRim, iconBoxRim);
                    if (tFade > 0.25f) {
                        BlockIconRenderer.drawBlockIcon(graphics, entry.blockId(), entry.color(), tx + 7.5f, ty + 6.5f, 17f, tFade);
                    }

                    float colDotCx = tx + tileW - 26f;
                    float colDotCy = ty + tileH * 0.5f;
                    UiHitRect colHit = new UiHitRect(colDotCx - 6.5f, colDotCy - 6.5f, 13f, 13f);
                    float remX = tx + tileW - 15f;
                    float remY = ty + 9.5f;
                    UiHitRect remHit = new UiHitRect(remX - 2f, remY - 2f, 15f, 15f);
                    if (interactive) {
                        trackedColorHitboxes.add(new StringHit(entry.blockId(), colHit));
                        trackedRemoveHitboxes.add(new StringHit(entry.blockId(), remHit));
                    }

                    float nameMaxW = Math.max(38f, colDotCx - 8f - (tx + 30.5f));
                    String dispName = UiRenderPrimitives.ellipsize(entry.displayName(), nameMaxW, 5.15f, SatoshiWeight.SEMIBOLD);
                    SatoshiFont.draw(graphics, dispName, tx + 30.5f, ty + 6.5f, 5.15f, TextGradient.pearl(tFade, entry.color(), 0.25f, theme), SatoshiWeight.SEMIBOLD);
                    SatoshiFont.draw(graphics, "Click dot for color", tx + 30.5f, ty + 17.5f, 4.1f, TextGradient.mist(0.68f * tFade, theme), SatoshiWeight.MEDIUM);

                    float dotR = (interactive && colHit.contains(lx, ly)) ? 5.2f : 4.4f;
                    UiRenderPrimitives.drawCircleFilled(graphics, colDotCx, colDotCy, dotR, UiRenderPrimitives.multiplyAlpha(entry.color(), tFade));
                    UiRenderPrimitives.drawCircleOutline(graphics, colDotCx, colDotCy, dotR + 0.3f, 0.9f, UiRenderPrimitives.argb(Math.round(210f * tFade), 255, 255, 255));

                    int remTint = (interactive && remHit.contains(lx, ly))
                        ? UiRenderPrimitives.argb(Math.round(255f * tFade), 255, 118, 128)
                        : UiRenderPrimitives.argb(Math.round(145f * tFade), 255, 255, 255);
                    UiRenderPrimitives.drawIcon(graphics, LucideIcon.X_CLOSE, remX + 1f, remY + 1.5f, 8.5f, remTint);
                }
            }
            curY += trackedRows * tileH + Math.max(0, trackedRows - 1) * rowGap;
        }

        curY += 10f;

        if (curY + 22f >= viewTop - 6f && curY <= viewBottom + 6f) {
            float chFade = alpha * scroller.edgeFadeAlpha(curY, 20f, viewTop, viewBottom);
            UiRenderPrimitives.drawIcon(graphics, LucideIcon.PLUS, areaX + 2f, curY + 4f, 10f, UiRenderPrimitives.multiplyAlpha(accent, chFade));
            SatoshiFont.draw(graphics, "Add Any Block", areaX + 15f, curY + 5.5f, 6.2f, TextGradient.pearl(chFade, accent, 0.28f, theme), SatoshiWeight.SEMIBOLD);

            float bsW = 216f;
            float bsH = 18.5f;
            float bsX = areaX + areaW - bsW;
            float bsY = curY;
            UiHitRect bsHit = new UiHitRect(bsX, bsY, bsW, bsH);
            if (interactive) {
                blockSearchHitbox = bsHit;
            }
            boolean bsHov = interactive && bsHit.contains(lx, ly);
            blockSearchHover = UiRenderPrimitives.approachExp(blockSearchHover, bsHov ? 1f : 0f, 16f, animDt);
            blockSearchFocusAnim = UiRenderPrimitives.approachExp(blockSearchFocusAnim, blockSearchFocused ? 1f : 0f, 16f, animDt);

            float bsR = Math.min(9.25f, 9.25f * rScale);
            int bsFill = UiRenderPrimitives.lerpColor(
                UiRenderPrimitives.argb(Math.round(96f * chFade), 18, 18, 20),
                UiRenderPrimitives.argb(Math.round(140f * chFade), 13, 13, 15),
                Math.max(blockSearchFocusAnim, blockSearchHover * 0.5f)
            );
            int bsRim = UiRenderPrimitives.lerpColor(
                UiRenderPrimitives.argb(Math.round(42f * chFade), 255, 255, 255),
                UiRenderPrimitives.multiplyAlpha(accent, (0.86f + 0.14f * blockSearchKeyPulse) * chFade),
                blockSearchFocusAnim
            );
            UiRenderPrimitives.drawRoundedRect(graphics, bsX, bsY, bsW, bsH, bsR, bsFill);
            UiRenderPrimitives.drawRoundedBorder(graphics, bsX, bsY, bsW, bsH, bsR, 0.9f, bsRim, bsRim);
            UiRenderPrimitives.drawIcon(
                graphics,
                LucideIcon.SEARCH,
                bsX + 6.5f,
                bsY + 4.8f,
                9f,
                UiRenderPrimitives.lerpColor(
                    UiRenderPrimitives.argb(Math.round(155f * chFade), 255, 255, 255),
                    UiRenderPrimitives.multiplyAlpha(accent, chFade),
                    Math.max(blockSearchFocusAnim * 0.85f, blockSearchKeyPulse)
                )
            );

            float bFontSz = 5.4f;
            float bTextX = bsX + 18.5f;
            float bTextY = bsY + (bsH - bFontSz) * 0.5f;
            float rightPad = !blockSearchQuery.isEmpty() ? 18f : 8f;

            if (blockSearchQuery.isEmpty() && !blockSearchFocused) {
                blockSearchCaretX = -1f;
                SatoshiFont.draw(graphics, "Search blocks... e.g. obsidian", bTextX, bTextY, bFontSz, TextGradient.mist(0.66f * chFade, theme), SatoshiWeight.MEDIUM);
            } else {
                String vis = SatoshiFont.trimTailToWidth(blockSearchQuery, bsW - 19f - rightPad, bFontSz, SatoshiWeight.MEDIUM);
                SatoshiFont.draw(graphics, vis, bTextX, bTextY, bFontSz, TextGradient.accent(0.98f * chFade, accent, theme), SatoshiWeight.MEDIUM);
                if (blockSearchFocused) {
                    float pulse = (float) Math.sin(context.elapsedSeconds() * 6.2f) * 0.5f + 0.5f;
                    float targetCx = bTextX + SatoshiFont.width(vis, bFontSz, SatoshiWeight.MEDIUM) + 1.2f;
                    blockSearchCaretX = blockSearchCaretX < 0f ? targetCx : UiRenderPrimitives.approachExp(blockSearchCaretX, targetCx, 28f, animDt);
                    float caretH = 8.0f + blockSearchKeyPulse * 1.2f;
                    UiRenderPrimitives.drawRoundedRect(
                        graphics,
                        blockSearchCaretX,
                        bsY + (bsH - caretH) * 0.5f,
                        1.15f,
                        caretH,
                        0.55f,
                        UiRenderPrimitives.multiplyAlpha(accent, (0.45f + 0.45f * Math.max(pulse, blockSearchKeyPulse)) * chFade)
                    );
                }
            }

            if (!blockSearchQuery.isEmpty()) {
                float cx = bsX + bsW - 13f;
                float cy = bsY + 5f;
                UiHitRect cHit = new UiHitRect(cx - 3f, bsY + 1.5f, 14f, 15.5f);
                if (interactive) {
                    blockSearchClearHitbox = cHit;
                }
                blockSearchClearHover = UiRenderPrimitives.approachExp(blockSearchClearHover, (interactive && cHit.contains(lx, ly)) ? 1f : 0f, 16f, animDt);
                UiRenderPrimitives.drawIcon(graphics, LucideIcon.X_CLOSE, cx, cy, 8.5f, UiRenderPrimitives.argb(Math.round((155f + 95f * blockSearchClearHover) * chFade), 255, 255, 255));
            }
        }
        curY += 24f;

        int catalogLimit = blockSearchQuery.isBlank() ? 48 : 96;
        List<BlockESP.CatalogBlockInfo> matches = besp.searchBlockCatalog(blockSearchQuery, catalogLimit);
        if (matches.isEmpty()) {
            if (curY + 28f >= viewTop - 6f && curY <= viewBottom + 6f) {
                float nFade = alpha * scroller.edgeFadeAlpha(curY, 28f, viewTop, viewBottom);
                UiRenderPrimitives.drawThemedGlassCard(graphics, areaX, curY, areaW, 28f, cardR, 26f, 15f, 38f, 16f, nFade, 0f, 0f, theme);
                SatoshiFont.drawCentered(
                    graphics,
                    "No blocks found matching \"" + blockSearchQuery + "\".",
                    areaX + areaW * 0.5f,
                    curY + 11f,
                    5.2f,
                    TextGradient.mist(0.76f * nFade, theme),
                    SatoshiWeight.MEDIUM
                );
            }
        } else {
            float cTileH = 26f;
            float cRowGap = 5.5f;
            for (int i = 0; i < matches.size(); i++) {
                BlockESP.CatalogBlockInfo cat = matches.get(i);
                int col = i % cols;
                int row = i / cols;
                float tx = areaX + col * (tileW + colGap);
                float ty = curY + row * (cTileH + cRowGap);
                if (ty + cTileH >= viewTop - 6f && ty <= viewBottom + 6f) {
                    float cFade = alpha * scroller.edgeFadeAlpha(ty, cTileH, viewTop, viewBottom);
                    UiHitRect cHit = new UiHitRect(tx, ty, tileW, cTileH);
                    if (interactive) {
                        catalogToggleHitboxes.add(new CatalogHit(cat, cHit));
                    }
                    boolean hov = interactive && cHit.contains(lx, ly);
                    float prev = catalogHoverMap.getOrDefault(cat.blockId(), 0f);
                    float curHov = UiRenderPrimitives.approachExp(prev, hov ? 1f : 0f, 16f, animDt);
                    catalogHoverMap.put(cat.blockId(), curHov);

                    boolean alreadyAdded = besp.isBlockSelected(cat.block());
                    UiRenderPrimitives.drawThemedGlassCard(
                        graphics, tx, ty, tileW, cTileH, cardR,
                        28f + 12f * curHov + (alreadyAdded ? 6f : 0f),
                        16f + 8f * curHov,
                        44f + 22f * curHov,
                        20f + 10f * curHov,
                        cFade,
                        alreadyAdded ? 0.42f : 0f,
                        curHov,
                        theme
                    );

                    if (cFade > 0.25f) {
                        Identifier parsed = Identifier.tryParse(cat.blockId());
                        Identifier safeId = parsed != null ? parsed : Identifier.withDefaultNamespace("stone");
                        BlockIconRenderer.drawBlockIcon(graphics, cat.block(), safeId, cat.defaultColor(), tx + 6f, ty + 5f, 16f, cFade);
                    }

                    String badgeTxt = alreadyAdded ? "Added" : "Add";
                    float badgeW = SatoshiFont.width(badgeTxt, 4.35f, SatoshiWeight.SEMIBOLD) + 10f;
                    float badgeH = 11.5f;
                    float badgeX = tx + tileW - badgeW - 6f;
                    float badgeY = ty + (cTileH - badgeH) * 0.5f;

                    int badgeBg = alreadyAdded
                        ? UiRenderPrimitives.multiplyAlpha(accent, 0.30f * cFade)
                        : UiRenderPrimitives.argb(Math.round((24f + 22f * curHov) * cFade), 255, 255, 255);
                    int badgeRim = alreadyAdded
                        ? UiRenderPrimitives.multiplyAlpha(accent, 0.85f * cFade)
                        : UiRenderPrimitives.argb(Math.round((48f + 30f * curHov) * cFade), 255, 255, 255);
                    UiRenderPrimitives.drawRoundedRect(graphics, badgeX, badgeY, badgeW, badgeH, 5.75f, badgeBg);
                    UiRenderPrimitives.drawRoundedBorder(graphics, badgeX, badgeY, badgeW, badgeH, 5.75f, 0.75f, badgeRim, badgeRim);
                    SatoshiFont.drawCentered(
                        graphics,
                        badgeTxt,
                        badgeX + badgeW * 0.5f,
                        badgeY + (badgeH - 4.35f) * 0.5f,
                        4.35f,
                        alreadyAdded ? TextGradient.pearl(cFade, accent, 0.25f, theme) : TextGradient.silver(0.92f * cFade, theme),
                        SatoshiWeight.SEMIBOLD
                    );

                    float maxTextW = Math.max(36f, badgeX - 5f - (tx + 26f));
                    SatoshiFont.draw(
                        graphics,
                        UiRenderPrimitives.ellipsize(cat.displayName(), maxTextW, 4.95f, SatoshiWeight.SEMIBOLD),
                        tx + 26f,
                        ty + 5.0f,
                        4.95f,
                        TextGradient.pearl(0.96f * cFade, alreadyAdded ? accent : cat.defaultColor(), 0.22f, theme),
                        SatoshiWeight.SEMIBOLD
                    );
                    SatoshiFont.draw(
                        graphics,
                        UiRenderPrimitives.ellipsize(cat.path(), maxTextW, 4.05f, SatoshiWeight.MEDIUM),
                        tx + 26f,
                        ty + 14.8f,
                        4.05f,
                        TextGradient.mist(0.65f * cFade, theme),
                        SatoshiWeight.MEDIUM
                    );
                }
            }
        }
    }

    @Override
    public boolean onMouseClicked(UiScreenActions actions, float localX, float localY, int button) {
        BlockESP besp = resolveBlockEsp();
        if (besp == null) {
            return false;
        }
        UiSoundSink soundSink = actions.sounds();
        UiTheme theme = actions.theme();

        if (!blockSearchQuery.isEmpty() && blockSearchClearHitbox.contains(localX, localY)) {
            blockSearchQuery = "";
            soundSink.playClick(1.05f);
            return true;
        }
        if (blockSearchHitbox.contains(localX, localY)) {
            blockSearchFocused = true;
            actions.syncTextInput();
            soundSink.playTab(1.08f);
            return true;
        }
        if (blockSearchFocused) {
            blockSearchFocused = false;
            actions.syncTextInput();
        }

        for (StringHit entry : blockPresetHitboxes) {
            if (entry.rect().contains(localX, localY)) {
                if ("clear".equals(entry.id())) {
                    besp.clearSelectedBlocks();
                    actions.showToast("Cleared tracked BlockESP list");
                    actions.spawnRipple(localX, localY, 20f, 0xFFFF6470);
                    soundSink.playClick(0.92f);
                } else {
                    besp.applyPreset(entry.id());
                    actions.showToast("Loaded " + entry.id().toUpperCase(Locale.ROOT) + " blocks preset");
                    actions.spawnRipple(localX, localY, 20f, theme.accentColor());
                    soundSink.playNotify();
                }
                return true;
            }
        }

        for (StringHit entry : trackedRemoveHitboxes) {
            if (entry.rect().contains(localX, localY)) {
                String name = BlockESP.displayNameForId(entry.id());
                besp.removeBlockById(entry.id());
                actions.showToast("Removed " + name + " from BlockESP");
                soundSink.playClick(0.92f);
                return true;
            }
        }

        for (StringHit entry : trackedColorHitboxes) {
            if (entry.rect().contains(localX, localY)) {
                int nextCol = besp.cycleBlockColor(entry.id());
                String name = BlockESP.displayNameForId(entry.id());
                actions.showToast("Updated " + name + " ESP color");
                actions.spawnRipple(localX, localY, 16f, nextCol);
                soundSink.playTab(1.12f);
                return true;
            }
        }

        for (CatalogHit entry : catalogToggleHitboxes) {
            if (entry.rect().contains(localX, localY)) {
                BlockESP.CatalogBlockInfo cat = entry.entry();
                boolean added = besp.toggleCatalogBlock(cat);
                actions.showToast(added ? ("Added " + cat.displayName() + " to BlockESP") : ("Removed " + cat.displayName()));
                actions.spawnRipple(localX, localY, 18f, added ? cat.defaultColor() : 0xFFFFFFFF);
                soundSink.playClick(added ? 1.15f : 0.94f);
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean onKeyPressed(UiScreenActions actions, KeyEvent event) {
        if (!blockSearchFocused) {
            return false;
        }
        int key = event.key();
        UiSoundSink soundSink = actions.sounds();
        if (key == InputConstants.KEY_BACKSPACE) {
            if (!blockSearchQuery.isEmpty()) {
                blockSearchQuery = (event.modifiers() & InputConstants.MOD_CONTROL) != 0
                    ? ""
                    : blockSearchQuery.substring(0, blockSearchQuery.length() - 1);
                blockSearchKeyPulse = 1f;
                soundSink.playTypeKey();
            }
            return true;
        }
        if (key == InputConstants.KEY_DELETE) {
            blockSearchQuery = "";
            blockSearchKeyPulse = 1f;
            soundSink.playTypeKey();
            return true;
        }
        if (key == InputConstants.KEY_RETURN || key == InputConstants.KEY_NUMPADENTER) {
            blockSearchFocused = false;
            actions.syncTextInput();
            soundSink.playClick(1.06f);
            return true;
        }
        return true;
    }

    @Override
    public boolean onCharTyped(UiScreenActions actions, CharacterEvent event) {
        if (!blockSearchFocused) {
            return false;
        }
        String ch = event.codepointAsString();
        if ("`".equals(ch) && blockSearchQuery.isEmpty()) {
            return true;
        }
        if (blockSearchQuery.length() < 32) {
            blockSearchQuery = blockSearchQuery + ch;
            blockSearchKeyPulse = 1f;
            UiSoundSink soundSink = actions.sounds();
            soundSink.playTypeKey();
            return true;
        }
        return false;
    }

    @Override
    public boolean isTextInputFocused() {
        return blockSearchFocused;
    }

    @Override
    public void clearFocus(UiScreenActions actions) {
        if (blockSearchFocused) {
            blockSearchFocused = false;
            actions.syncTextInput();
        }
    }

    @Override
    public void clearHitboxes() {
        blockSearchHitbox = UiHitRect.EMPTY;
        blockSearchClearHitbox = UiHitRect.EMPTY;
        blockPresetHitboxes.clear();
        trackedColorHitboxes.clear();
        trackedRemoveHitboxes.clear();
        catalogToggleHitboxes.clear();
    }
}
