package com.eclipseware.imnotcheatingyouare.client.glassui.bridge;

import com.eclipseware.imnotcheatingyouare.client.glassui.asset.LucideIcon;
import com.eclipseware.imnotcheatingyouare.client.glassui.audio.UiSoundSink;
import com.eclipseware.imnotcheatingyouare.client.glassui.bind.UiInputBind;
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
import com.eclipseware.imnotcheatingyouare.client.macro.Macro;
import com.eclipseware.imnotcheatingyouare.client.macro.MacroManager;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;

public final class MacroCenterSection implements UiCustomSection {
    public static final MacroCenterSection INSTANCE = new MacroCenterSection();

    private record MacroRowHit(
        Macro macro,
        UiHitRect toggleRect,
        UiHitRect bindRect,
        UiHitRect modeRect,
        UiHitRect recRect,
        UiHitRect playRect,
        UiHitRect clipRect,
        UiHitRect delRect
    ) {
    }

    private boolean nameInputFocused = false;
    private String newMacroName = "";
    private float inputFocusAnim = 0f;
    private float createHover = 0f;
    private float importHover = 0f;
    private float stopAllHover = 0f;
    private String listeningMacroName = null;
    private final Map<String, Float> rowHovers = new HashMap<>();

    private UiHitRect inputHitbox = UiHitRect.EMPTY;
    private UiHitRect createHitbox = UiHitRect.EMPTY;
    private UiHitRect importHitbox = UiHitRect.EMPTY;
    private UiHitRect stopAllHitbox = UiHitRect.EMPTY;
    private final List<MacroRowHit> rowHitboxes = new ArrayList<>();

    private MacroCenterSection() {
    }

    @Override
    public float measureHeight(UiSectionMeasureContext context) {
        List<Macro> macros = MacroManager.getMacros();
        float rowsH = macros.isEmpty() ? 38f : (macros.size() * 44f + Math.max(0, macros.size() - 1) * 7f);
        return 36f + 12f + 20f + rowsH + 10f;
    }

    @Override
    public void render(UiSectionRenderContext context) {
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
        float cardR = 10.5f * rScale;

        float curY = context.startY();
        float barH = 34f;
        if (curY + barH >= viewTop - 6f && curY <= viewBottom + 6f) {
            float barFade = alpha * scroller.edgeFadeAlpha(curY, barH, viewTop, viewBottom);
            UiRenderPrimitives.drawThemedGlassCard(graphics, areaX, curY, areaW, barH, cardR, 30f, 18f, 48f, 20f, barFade, 0f, 0f, theme);

            float btnH = 22f;
            float btnY = curY + (barH - btnH) * 0.5f;

            float stopW = 66f;
            float stopX = areaX + areaW - stopW - 8f;
            UiHitRect stHit = new UiHitRect(stopX, btnY, stopW, btnH);
            if (interactive) {
                stopAllHitbox = stHit;
            }
            stopAllHover = UiRenderPrimitives.approachExp(stopAllHover, (interactive && stHit.contains(lx, ly)) ? 1f : 0f, 18f, animDt);
            drawHeaderBtn(graphics, "Stop All", stHit, stopAllHover > 0.2f, false, barFade, 0xFFFF6470, theme);

            float impW = 80f;
            float impX = stopX - impW - 6f;
            UiHitRect imHit = new UiHitRect(impX, btnY, impW, btnH);
            if (interactive) {
                importHitbox = imHit;
            }
            importHover = UiRenderPrimitives.approachExp(importHover, (interactive && imHit.contains(lx, ly)) ? 1f : 0f, 18f, animDt);
            drawHeaderBtn(graphics, "Import Clip", imHit, importHover > 0.2f, false, barFade, accent, theme);

            float crW = 68f;
            float crX = impX - crW - 6f;
            UiHitRect crHit = new UiHitRect(crX, btnY, crW, btnH);
            if (interactive) {
                createHitbox = crHit;
            }
            createHover = UiRenderPrimitives.approachExp(createHover, (interactive && crHit.contains(lx, ly)) ? 1f : 0f, 18f, animDt);
            drawHeaderBtn(graphics, "Create", crHit, createHover > 0.2f, true, barFade, accent, theme);

            float inpX = areaX + 8f;
            float inpW = crX - inpX - 8f;
            UiHitRect iHit = new UiHitRect(inpX, btnY, inpW, btnH);
            if (interactive) {
                inputHitbox = iHit;
            }
            inputFocusAnim = UiRenderPrimitives.approachExp(inputFocusAnim, nameInputFocused ? 1f : 0f, 18f, animDt);
            int iFill = UiRenderPrimitives.lerpColor(
                UiRenderPrimitives.argb(Math.round(92f * barFade), 16, 16, 18),
                UiRenderPrimitives.argb(Math.round(142f * barFade), 12, 12, 14),
                inputFocusAnim
            );
            int iRim = UiRenderPrimitives.lerpColor(
                UiRenderPrimitives.argb(Math.round(42f * barFade), 255, 255, 255),
                UiRenderPrimitives.multiplyAlpha(accent, 0.90f * barFade),
                inputFocusAnim
            );
            UiRenderPrimitives.drawRoundedRect(graphics, inpX, btnY, inpW, btnH, Math.min(9f, 9f * rScale), iFill);
            UiRenderPrimitives.drawRoundedBorder(graphics, inpX, btnY, inpW, btnH, Math.min(9f, 9f * rScale), 0.9f, iRim, iRim);
            UiRenderPrimitives.drawIcon(graphics, LucideIcon.PLUS, inpX + 7f, btnY + 6f, 10f, UiRenderPrimitives.multiplyAlpha(accent, barFade));

            float tx = inpX + 21f;
            float ty = btnY + (btnH - 5.4f) * 0.5f;
            if (newMacroName.isEmpty() && !nameInputFocused) {
                SatoshiFont.draw(graphics, "New macro name...", tx, ty, 5.4f, TextGradient.mist(0.64f * barFade, theme), SatoshiWeight.MEDIUM);
            } else {
                String vis = SatoshiFont.trimTailToWidth(newMacroName, inpW - 28f, 5.4f, SatoshiWeight.MEDIUM);
                SatoshiFont.draw(graphics, vis, tx, ty, 5.4f, TextGradient.accent(0.98f * barFade, accent, theme), SatoshiWeight.MEDIUM);
                if (nameInputFocused) {
                    float pulse = (float) Math.sin(context.elapsedSeconds() * 6.2f) * 0.5f + 0.5f;
                    float cx = tx + SatoshiFont.width(vis, 5.4f, SatoshiWeight.MEDIUM) + 1.2f;
                    UiRenderPrimitives.drawRoundedRect(graphics, cx, btnY + (btnH - 8.2f) * 0.5f, 1.15f, 8.2f, 0.55f, UiRenderPrimitives.multiplyAlpha(accent, (0.45f + 0.45f * pulse) * barFade));
                }
            }
        }
        curY += barH + 12f;

        List<Macro> macros = MacroManager.getMacros();
        if (curY + 18f >= viewTop - 6f && curY <= viewBottom + 6f) {
            float hFade = alpha * scroller.edgeFadeAlpha(curY, 18f, viewTop, viewBottom);
            UiRenderPrimitives.drawIcon(graphics, LucideIcon.REPEAT, areaX + 2f, curY + 3f, 10f, UiRenderPrimitives.multiplyAlpha(accent, hFade));
            String status = MacroManager.isRecording() ? " • Recording active!" : (MacroManager.isPlaying() ? " • Playback active!" : "");
            SatoshiFont.draw(
                graphics,
                "Configured Macros (" + macros.size() + ")" + status,
                areaX + 15f,
                curY + 4.5f,
                6.2f,
                TextGradient.pearl(hFade, accent, 0.28f, theme),
                SatoshiWeight.SEMIBOLD
            );
        }
        curY += 20f;

        if (macros.isEmpty()) {
            if (curY + 36f >= viewTop - 6f && curY <= viewBottom + 6f) {
                float eFade = alpha * scroller.edgeFadeAlpha(curY, 36f, viewTop, viewBottom);
                UiRenderPrimitives.drawThemedGlassCard(graphics, areaX, curY, areaW, 36f, cardR, 26f, 15f, 38f, 16f, eFade, 0f, 0f, theme);
                SatoshiFont.drawCentered(
                    graphics,
                    "No macros created yet. Enter a name above and click Create.",
                    areaX + areaW * 0.5f,
                    curY + 15f,
                    5.3f,
                    TextGradient.mist(0.76f * eFade, theme),
                    SatoshiWeight.MEDIUM
                );
            }
            return;
        }

        float rowH = 44f;
        float gap = 7f;
        Macro activeCurrent = MacroManager.getCurrentMacro();

        for (int i = 0; i < macros.size(); i++) {
            Macro m = macros.get(i);
            float ry = curY + i * (rowH + gap);
            if (ry + rowH < viewTop - 6f || ry > viewBottom + 6f) {
                continue;
            }
            float rFade = alpha * scroller.edgeFadeAlpha(ry, rowH, viewTop, viewBottom);
            UiHitRect rowRect = new UiHitRect(areaX, ry, areaW, rowH);
            boolean hov = interactive && rowRect.contains(lx, ly);
            float prev = rowHovers.getOrDefault(m.getName(), 0f);
            float hv = UiRenderPrimitives.approachExp(prev, hov ? 1f : 0f, 16f, animDt);
            rowHovers.put(m.getName(), hv);

            boolean isRecThis = MacroManager.isRecording() && activeCurrent == m;
            boolean isPlayThis = MacroManager.isPlaying() && activeCurrent == m;

            UiRenderPrimitives.drawThemedGlassCard(
                graphics, areaX, ry, areaW, rowH, cardR,
                32f + 12f * hv + (m.isEnabled() ? 6f : 0f),
                18f + 8f * hv,
                48f + 22f * hv,
                22f + 10f * hv,
                rFade,
                m.isEnabled() ? 0.45f : 0f,
                hv,
                theme
            );

            float swX = areaX + 12f;
            float swY = ry + (rowH - 12.5f) * 0.5f;
            UiHitRect toggleRect = new UiHitRect(swX - 4f, swY - 4f, 31f, 20.5f);
            UiRenderPrimitives.drawSwitch(graphics, swX, swY, 23f, 12.5f, m.isEnabled() ? 1f : 0f, (interactive && toggleRect.contains(lx, ly)) ? 1f : 0f, rFade, theme);

            UiHitRect delRect = new UiHitRect(areaX + areaW - 24f, ry + 13f, 16f, 18f);
            UiHitRect clipRect = new UiHitRect(delRect.x() - 46f, ry + 13f, 40f, 18f);
            UiHitRect playRect = new UiHitRect(clipRect.x() - 48f, ry + 13f, 42f, 18f);
            UiHitRect recRect = new UiHitRect(playRect.x() - 48f, ry + 13f, 42f, 18f);
            UiHitRect modeRect = new UiHitRect(recRect.x() - 52f, ry + 13f, 46f, 18f);

            UiInputBind bind = UiInputBind.fromModuleKeyBind(m.getKeybind());
            boolean listening = m.getName().equalsIgnoreCase(listeningMacroName);
            String bindText = listening ? "Press..." : (bind.isBound() ? bind.formatLabel() : "Bind");
            float bindW = Math.max(38f, SatoshiFont.width(bindText, 4.8f, SatoshiWeight.SEMIBOLD) + 14f);
            UiHitRect bindRect = new UiHitRect(modeRect.x() - bindW - 6f, ry + 13f, bindW, 18f);

            if (interactive) {
                rowHitboxes.add(new MacroRowHit(m, toggleRect, bindRect, modeRect, recRect, playRect, clipRect, delRect));
            }

            float titleMaxW = Math.max(50f, bindRect.x() - (areaX + 44f) - 8f);
            SatoshiFont.draw(
                graphics,
                UiRenderPrimitives.ellipsize(m.getName(), titleMaxW, 6.2f, SatoshiWeight.SEMIBOLD),
                areaX + 44f,
                ry + 10.5f,
                6.2f,
                TextGradient.pearl(rFade, accent, m.isEnabled() ? 0.30f : 0.12f, theme),
                SatoshiWeight.SEMIBOLD
            );

            int actCount = m.getActions().size();
            String subText = isRecThis
                ? "Recording inputs... (" + actCount + " steps)"
                : (isPlayThis ? "Playing sequence..." : (actCount + " recorded events"));
            SatoshiFont.draw(
                graphics,
                subText,
                areaX + 44f,
                ry + 23.5f,
                4.5f,
                isRecThis ? TextGradient.accent(rFade, 0xFFFF6470, theme) : TextGradient.mist(0.75f * rFade, theme),
                SatoshiWeight.MEDIUM
            );

            drawHeaderBtn(graphics, bindText, bindRect, listening || (interactive && bindRect.contains(lx, ly)), listening || bind.isBound(), rFade, accent, theme);
            drawHeaderBtn(graphics, m.isHoldMode() ? "Hold" : "Toggle", modeRect, interactive && modeRect.contains(lx, ly), m.isHoldMode(), rFade, accent, theme);
            drawHeaderBtn(graphics, isRecThis ? "Stop" : "Rec", recRect, interactive && recRect.contains(lx, ly), isRecThis, rFade, isRecThis ? 0xFFFF6470 : accent, theme);
            drawHeaderBtn(graphics, isPlayThis ? "Stop" : "Play", playRect, interactive && playRect.contains(lx, ly), isPlayThis, rFade, accent, theme);
            drawHeaderBtn(graphics, "Copy", clipRect, interactive && clipRect.contains(lx, ly), false, rFade, accent, theme);

            int delCol = (interactive && delRect.contains(lx, ly))
                ? UiRenderPrimitives.argb(Math.round(255f * rFade), 255, 118, 128)
                : UiRenderPrimitives.argb(Math.round(155f * rFade), 255, 255, 255);
            UiRenderPrimitives.drawIcon(graphics, LucideIcon.X_CLOSE, delRect.x() + 3.5f, delRect.y() + 4.5f, 9f, delCol);
        }
    }

    private static void drawHeaderBtn(
        GuiGraphicsExtractor graphics,
        String label,
        UiHitRect rect,
        boolean hovered,
        boolean active,
        float alpha,
        int tintColor,
        UiTheme theme
    ) {
        int bg = active
            ? UiRenderPrimitives.multiplyAlpha(tintColor, (hovered ? 0.48f : 0.32f) * alpha)
            : UiRenderPrimitives.argb(Math.round((hovered ? 56f : 30f) * alpha), 255, 255, 255);
        int rim = active
            ? UiRenderPrimitives.multiplyAlpha(tintColor, 0.88f * alpha)
            : UiRenderPrimitives.argb(Math.round((hovered ? 85f : 46f) * alpha), 255, 255, 255);
        UiRenderPrimitives.drawRoundedRect(graphics, rect.x(), rect.y(), rect.w(), rect.h(), 6.5f, bg);
        UiRenderPrimitives.drawRoundedBorder(graphics, rect.x(), rect.y(), rect.w(), rect.h(), 6.5f, 0.85f, rim, rim);
        SatoshiFont.drawCentered(
            graphics,
            label,
            rect.x() + rect.w() * 0.5f,
            rect.y() + (rect.h() - 4.85f) * 0.5f,
            4.85f,
            TextGradient.pearl(alpha, tintColor, 0.24f, theme),
            SatoshiWeight.SEMIBOLD
        );
    }

    @Override
    public boolean onMouseClicked(UiScreenActions actions, float localX, float localY, int button) {
        UiSoundSink soundSink = actions.sounds();
        UiTheme theme = actions.theme();

        if (listeningMacroName != null) {
            Macro target = findMacro(listeningMacroName);
            if (target != null) {
                if (button == InputConstants.MOUSE_BUTTON_MIDDLE || button >= InputConstants.MOUSE_BUTTON_4) {
                    UiInputBind bind = UiInputBind.mouse(button);
                    target.setKeybind(bind.toModuleKeyBind());
                    MacroManager.save();
                    listeningMacroName = null;
                    actions.syncTextInput();
                    actions.showToast("Bound macro " + target.getName() + " to " + bind.formatLabel());
                    soundSink.playNotify();
                    return true;
                }
                if (button == InputConstants.MOUSE_BUTTON_RIGHT) {
                    target.setKeybind(0);
                    MacroManager.save();
                    listeningMacroName = null;
                    actions.syncTextInput();
                    actions.showToast("Cleared macro bind");
                    soundSink.playClick(0.92f);
                    return true;
                }
            }
            listeningMacroName = null;
            actions.syncTextInput();
            soundSink.playClick(0.95f);
            return true;
        }

        if (button == InputConstants.MOUSE_BUTTON_RIGHT) {
            for (MacroRowHit row : rowHitboxes) {
                if (row.bindRect().contains(localX, localY)) {
                    row.macro().setKeybind(0);
                    MacroManager.save();
                    actions.showToast("Cleared " + row.macro().getName() + " bind");
                    soundSink.playClick(0.92f);
                    return true;
                }
            }
            return false;
        }

        if (button != InputConstants.MOUSE_BUTTON_LEFT) {
            return false;
        }

        if (inputHitbox.contains(localX, localY)) {
            nameInputFocused = true;
            actions.syncTextInput();
            soundSink.playTab(1.06f);
            return true;
        }
        if (nameInputFocused && !createHitbox.contains(localX, localY)) {
            nameInputFocused = false;
            actions.syncTextInput();
        }

        if (createHitbox.contains(localX, localY)) {
            createMacro(actions, localX, localY);
            return true;
        }
        if (importHitbox.contains(localX, localY)) {
            MacroManager.importFromClipboard();
            actions.showToast("Imported macro from clipboard");
            actions.spawnRipple(localX, localY, 20f, theme.accentColor());
            soundSink.playNotify();
            return true;
        }
        if (stopAllHitbox.contains(localX, localY)) {
            MacroManager.stopRecord();
            MacroManager.stopPlay();
            actions.showToast("Stopped all macro recording & playback");
            actions.spawnRipple(localX, localY, 20f, 0xFFFF6470);
            soundSink.playClick(0.92f);
            return true;
        }

        for (MacroRowHit row : rowHitboxes) {
            Macro m = row.macro();
            if (row.toggleRect().contains(localX, localY)) {
                m.setEnabled(!m.isEnabled());
                MacroManager.save();
                soundSink.playToggle(m.isEnabled());
                actions.spawnRipple(localX, localY, 18f, m.isEnabled() ? theme.accentColor() : 0xFFFFFFFF);
                return true;
            }
            if (row.bindRect().contains(localX, localY)) {
                listeningMacroName = m.getName();
                actions.syncTextInput();
                actions.showToast("Press key or MB4/MB5 for macro " + m.getName());
                actions.spawnRipple(localX, localY, 18f, theme.accentColor());
                soundSink.playClick(1.14f);
                return true;
            }
            if (row.modeRect().contains(localX, localY)) {
                m.setHoldMode(!m.isHoldMode());
                MacroManager.save();
                actions.showToast(m.getName() + " mode: " + (m.isHoldMode() ? "Hold" : "Toggle"));
                soundSink.playTab(1.08f);
                return true;
            }
            if (row.recRect().contains(localX, localY)) {
                Macro current = MacroManager.getCurrentMacro();
                if (MacroManager.isRecording() && current == m) {
                    MacroManager.stopRecord();
                    actions.showToast("Saved recording for " + m.getName());
                    soundSink.playNotify();
                } else {
                    MacroManager.setActiveMacro(m);
                    MacroManager.startRecord(m);
                    ClientNotificationService.notifyMacro(m.getName(), "Recording started — close GUI to record");
                    actions.showToast("Recording " + m.getName() + " — close GUI to record");
                }
                return true;
            }
            if (row.playRect().contains(localX, localY)) {
                Macro current = MacroManager.getCurrentMacro();
                if (MacroManager.isPlaying() && current == m) {
                    MacroManager.stopPlay();
                    actions.showToast("Stopped " + m.getName());
                    soundSink.playClick(0.94f);
                } else if (m.getActions().isEmpty()) {
                    actions.showToast("Record actions into " + m.getName() + " first");
                    soundSink.playClick(0.92f);
                } else {
                    MacroManager.setActiveMacro(m);
                    MacroManager.startPlay(m);
                    ClientNotificationService.notifyMacro(m.getName(), "Playing " + m.getActions().size() + " actions");
                }
                return true;
            }
            if (row.clipRect().contains(localX, localY)) {
                MacroManager.exportToClipboard(m);
                actions.showToast("Copied macro " + m.getName() + " to clipboard");
                soundSink.playNotify();
                return true;
            }
            if (row.delRect().contains(localX, localY)) {
                String deleted = m.getName();
                MacroManager.deleteMacro(m);
                actions.showToast("Deleted macro " + deleted);
                actions.spawnRipple(localX, localY, 16f, 0xFFFF6470);
                soundSink.playClick(0.92f);
                return true;
            }
        }
        return false;
    }

    private void createMacro(UiScreenActions actions, float lx, float ly) {
        UiSoundSink soundSink = actions.sounds();
        UiTheme theme = actions.theme();
        String raw = newMacroName.trim();
        if (raw.isEmpty()) {
            raw = "Macro " + (MacroManager.getMacros().size() + 1);
        }
        Macro created = MacroManager.createNewMacro(raw);
        MacroManager.setActiveMacro(created);
        newMacroName = "";
        nameInputFocused = false;
        actions.syncTextInput();
        actions.showToast("Created macro: " + created.getName());
        actions.spawnRipple(lx, ly, 20f, theme.accentColor());
        soundSink.playNotify();
    }

    private static Macro findMacro(String name) {
        for (Macro m : MacroManager.getMacros()) {
            if (m.getName().equalsIgnoreCase(name)) {
                return m;
            }
        }
        return null;
    }

    @Override
    public boolean onKeyPressed(UiScreenActions actions, KeyEvent event) {
        int key = event.key();
        UiSoundSink soundSink = actions.sounds();
        if (listeningMacroName != null) {
            Macro m = findMacro(listeningMacroName);
            listeningMacroName = null;
            actions.syncTextInput();
            if (m != null) {
                if (key == InputConstants.KEY_BACKSPACE || key == InputConstants.KEY_DELETE) {
                    m.setKeybind(0);
                    MacroManager.save();
                    actions.showToast("Cleared " + m.getName() + " bind");
                    soundSink.playClick(0.92f);
                    return true;
                }
                if (key > 0 && key != InputConstants.KEY_ESCAPE) {
                    UiInputBind bind = UiInputBind.key(key);
                    m.setKeybind(bind.toModuleKeyBind());
                    MacroManager.save();
                    actions.showToast("Bound " + m.getName() + " to " + bind.formatLabel());
                    soundSink.playNotify();
                    return true;
                }
            }
            return true;
        }
        if (!nameInputFocused) {
            return false;
        }
        if (key == InputConstants.KEY_BACKSPACE) {
            if (!newMacroName.isEmpty()) {
                newMacroName = (event.modifiers() & InputConstants.MOD_CONTROL) != 0
                    ? ""
                    : newMacroName.substring(0, newMacroName.length() - 1);
                soundSink.playTypeKey();
            }
            return true;
        }
        if (key == InputConstants.KEY_DELETE) {
            newMacroName = "";
            soundSink.playTypeKey();
            return true;
        }
        if (key == InputConstants.KEY_RETURN || key == InputConstants.KEY_NUMPADENTER) {
            createMacro(actions, createHitbox.x() + createHitbox.w() * 0.5f, createHitbox.y() + 11f);
            return true;
        }
        return true;
    }

    @Override
    public boolean onCharTyped(UiScreenActions actions, CharacterEvent event) {
        if (listeningMacroName != null) {
            return true;
        }
        if (!nameInputFocused) {
            return false;
        }
        String ch = event.codepointAsString();
        if ("`".equals(ch) && newMacroName.isEmpty()) {
            return true;
        }
        if (newMacroName.length() < 26) {
            newMacroName = newMacroName + ch;
            UiSoundSink soundSink = actions.sounds();
            soundSink.playTypeKey();
            return true;
        }
        return false;
    }

    @Override
    public boolean isTextInputFocused() {
        return nameInputFocused || listeningMacroName != null;
    }

    @Override
    public void clearFocus(UiScreenActions actions) {
        boolean changed = nameInputFocused || listeningMacroName != null;
        nameInputFocused = false;
        listeningMacroName = null;
        if (changed) {
            actions.syncTextInput();
        }
    }

    @Override
    public void clearHitboxes() {
        inputHitbox = UiHitRect.EMPTY;
        createHitbox = UiHitRect.EMPTY;
        importHitbox = UiHitRect.EMPTY;
        stopAllHitbox = UiHitRect.EMPTY;
        rowHitboxes.clear();
    }
}
