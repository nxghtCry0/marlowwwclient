package com.eclipseware.imnotcheatingyouare.client.glassui.bridge;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
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
import com.eclipseware.imnotcheatingyouare.client.module.ModuleManager;
import com.eclipseware.imnotcheatingyouare.client.setting.ConfigManager;
import com.mojang.blaze3d.platform.InputConstants;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;

public final class ConfigCenterSection implements UiCustomSection {
    public static final ConfigCenterSection INSTANCE = new ConfigCenterSection();

    private record BtnHit(String id, UiHitRect rect) {
    }

    private record ProfileHit(File file, String name, UiHitRect loadRect, UiHitRect saveRect, UiHitRect delRect) {
    }

    private static final String[][] TOP_ACTIONS = {
        {"save_active", "Save Active"},
        {"reload_disk", "Reload Disk"},
        {"export_clip", "Export Clipboard"},
        {"import_clip", "Import Clipboard"}
    };

    private boolean nameInputFocused = false;
    private String newProfileName = "";
    private float inputFocusAnim = 0f;
    private float inputKeyPulse = 0f;
    private final float[] topHovers = new float[4];
    private float createBtnHover = 0f;
    private final Map<String, Float> profileHovers = new HashMap<>();

    private UiHitRect inputHitbox = UiHitRect.EMPTY;
    private UiHitRect createBtnHitbox = UiHitRect.EMPTY;
    private final List<BtnHit> topBtnHitboxes = new ArrayList<>();
    private final List<ProfileHit> profileHitboxes = new ArrayList<>();

    private ConfigCenterSection() {
    }

    private static File profilesDir() {
        Minecraft mc = Minecraft.getInstance();
        File root = mc != null && mc.gameDirectory != null ? mc.gameDirectory : new File(".");
        File dir = new File(root, "config/imnotcheatingyouare/profiles");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    public static List<File> listProfiles() {
        File dir = profilesDir();
        File[] files = dir.listFiles((d, name) -> name.endsWith(".enc"));
        if (files == null || files.length == 0) {
            return List.of();
        }
        List<File> list = new ArrayList<>(Arrays.asList(files));
        list.sort(Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER));
        return list;
    }

    public static int profileCount() {
        return listProfiles().size();
    }

    @Override
    public float measureHeight(UiSectionMeasureContext context) {
        List<File> profiles = listProfiles();
        int rows = profiles.isEmpty() ? 1 : (profiles.size() + 1) / 2;
        float gridH = profiles.isEmpty() ? 38f : (rows * 42f + Math.max(0, rows - 1) * 7f);
        return 38f + 10f + 36f + 12f + 20f + gridH + 10f;
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

        inputKeyPulse = UiRenderPrimitives.approachExp(inputKeyPulse, 0f, 12f, animDt);
        float curY = context.startY();

        float btnGap = 8f;
        float topBtnW = (areaW - btnGap * (TOP_ACTIONS.length - 1)) / TOP_ACTIONS.length;
        float topBtnH = 34f;
        if (curY + topBtnH >= viewTop - 6f && curY <= viewBottom + 6f) {
            float rowFade = alpha * scroller.edgeFadeAlpha(curY, topBtnH, viewTop, viewBottom);
            for (int i = 0; i < TOP_ACTIONS.length; i++) {
                String id = TOP_ACTIONS[i][0];
                String lbl = TOP_ACTIONS[i][1];
                float bx = areaX + i * (topBtnW + btnGap);
                UiHitRect hit = new UiHitRect(bx, curY, topBtnW, topBtnH);
                if (interactive) {
                    topBtnHitboxes.add(new BtnHit(id, hit));
                }
                boolean hov = interactive && hit.contains(lx, ly);
                topHovers[i] = UiRenderPrimitives.approachExp(topHovers[i], hov ? 1f : 0f, 18f, animDt);
                UiRenderPrimitives.drawThemedGlassCard(
                    graphics, bx, curY, topBtnW, topBtnH, cardR,
                    34f + 22f * topHovers[i],
                    20f + 14f * topHovers[i],
                    52f + 30f * topHovers[i],
                    24f + 14f * topHovers[i],
                    rowFade,
                    topHovers[i] * 0.55f,
                    topHovers[i],
                    theme
                );
                LucideIcon ic = switch (i) {
                    case 0 -> LucideIcon.SAVE;
                    case 1 -> LucideIcon.REPEAT;
                    case 2 -> LucideIcon.LOG_IN;
                    default -> LucideIcon.PACKAGE;
                };
                UiRenderPrimitives.drawIcon(graphics, ic, bx + 10f, curY + 11f, 11.5f, UiRenderPrimitives.multiplyAlpha(accent, rowFade));
                SatoshiFont.draw(
                    graphics,
                    lbl,
                    bx + 26f,
                    curY + (topBtnH - 5.6f) * 0.5f,
                    5.6f,
                    TextGradient.pearl(rowFade, accent, 0.25f, theme),
                    SatoshiWeight.SEMIBOLD
                );
            }
        }
        curY += topBtnH + 10f;

        float barH = 34f;
        if (curY + barH >= viewTop - 6f && curY <= viewBottom + 6f) {
            float barFade = alpha * scroller.edgeFadeAlpha(curY, barH, viewTop, viewBottom);
            UiRenderPrimitives.drawThemedGlassCard(graphics, areaX, curY, areaW, barH, cardR, 30f, 18f, 48f, 20f, barFade, 0f, 0f, theme);

            float createW = 92f;
            float createH = 22f;
            float createX = areaX + areaW - createW - 8f;
            float createY = curY + (barH - createH) * 0.5f;
            UiHitRect cHit = new UiHitRect(createX, createY, createW, createH);
            if (interactive) {
                createBtnHitbox = cHit;
            }
            boolean cHov = interactive && cHit.contains(lx, ly);
            createBtnHover = UiRenderPrimitives.approachExp(createBtnHover, cHov ? 1f : 0f, 18f, animDt);
            UiRenderPrimitives.drawGlassCard(
                graphics,
                createX,
                createY,
                createW,
                createH,
                Math.min(9f, 9f * rScale),
                UiRenderPrimitives.multiplyAlpha(accent, (0.34f + 0.24f * createBtnHover) * barFade),
                UiRenderPrimitives.multiplyAlpha(accent, (0.20f + 0.16f * createBtnHover) * barFade),
                UiRenderPrimitives.multiplyAlpha(accent, 0.86f * barFade),
                UiRenderPrimitives.multiplyAlpha(accent, 0.45f * barFade)
            );
            SatoshiFont.drawCentered(
                graphics,
                "Save Profile",
                createX + createW * 0.5f,
                createY + (createH - 5.4f) * 0.5f,
                5.4f,
                TextGradient.pearl(barFade, accent, 0.20f, theme),
                SatoshiWeight.SEMIBOLD
            );

            float inpX = areaX + 8f;
            float inpY = createY;
            float inpW = createX - inpX - 8f;
            float inpH = createH;
            UiHitRect iHit = new UiHitRect(inpX, inpY, inpW, inpH);
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
            UiRenderPrimitives.drawRoundedRect(graphics, inpX, inpY, inpW, inpH, Math.min(9f, 9f * rScale), iFill);
            UiRenderPrimitives.drawRoundedBorder(graphics, inpX, inpY, inpW, inpH, Math.min(9f, 9f * rScale), 0.9f, iRim, iRim);
            UiRenderPrimitives.drawIcon(graphics, LucideIcon.PLUS, inpX + 7f, inpY + 6f, 10f, UiRenderPrimitives.multiplyAlpha(accent, barFade));

            float tx = inpX + 21f;
            float ty = inpY + (inpH - 5.5f) * 0.5f;
            if (newProfileName.isEmpty() && !nameInputFocused) {
                SatoshiFont.draw(graphics, "Enter new profile name... e.g. ranked_legit", tx, ty, 5.5f, TextGradient.mist(0.64f * barFade, theme), SatoshiWeight.MEDIUM);
            } else {
                String vis = SatoshiFont.trimTailToWidth(newProfileName, inpW - 30f, 5.5f, SatoshiWeight.MEDIUM);
                SatoshiFont.draw(graphics, vis, tx, ty, 5.5f, TextGradient.accent(0.98f * barFade, accent, theme), SatoshiWeight.MEDIUM);
                if (nameInputFocused) {
                    float pulse = (float) Math.sin(context.elapsedSeconds() * 6.2f) * 0.5f + 0.5f;
                    float cx = tx + SatoshiFont.width(vis, 5.5f, SatoshiWeight.MEDIUM) + 1.2f;
                    UiRenderPrimitives.drawRoundedRect(graphics, cx, inpY + (inpH - 8.4f) * 0.5f, 1.15f, 8.4f, 0.55f, UiRenderPrimitives.multiplyAlpha(accent, (0.45f + 0.45f * pulse) * barFade));
                }
            }
        }
        curY += barH + 12f;

        List<File> profiles = listProfiles();
        if (curY + 18f >= viewTop - 6f && curY <= viewBottom + 6f) {
            float hFade = alpha * scroller.edgeFadeAlpha(curY, 18f, viewTop, viewBottom);
            UiRenderPrimitives.drawIcon(graphics, LucideIcon.LAYERS, areaX + 2f, curY + 3f, 10f, UiRenderPrimitives.multiplyAlpha(accent, hFade));
            SatoshiFont.draw(
                graphics,
                "Saved Local Profiles (" + profiles.size() + ")",
                areaX + 15f,
                curY + 4.5f,
                6.2f,
                TextGradient.pearl(hFade, accent, 0.28f, theme),
                SatoshiWeight.SEMIBOLD
            );
        }
        curY += 20f;

        if (profiles.isEmpty()) {
            if (curY + 36f >= viewTop - 6f && curY <= viewBottom + 6f) {
                float eFade = alpha * scroller.edgeFadeAlpha(curY, 36f, viewTop, viewBottom);
                UiRenderPrimitives.drawThemedGlassCard(graphics, areaX, curY, areaW, 36f, cardR, 26f, 15f, 38f, 16f, eFade, 0f, 0f, theme);
                SatoshiFont.drawCentered(
                    graphics,
                    "No named profiles saved yet. Type a name above and click Save Profile.",
                    areaX + areaW * 0.5f,
                    curY + 15f,
                    5.3f,
                    TextGradient.mist(0.76f * eFade, theme),
                    SatoshiWeight.MEDIUM
                );
            }
            return;
        }

        float colGap = 8f;
        float cellW = (areaW - colGap) * 0.5f;
        float cellH = 42f;
        float rowGap = 7f;

        for (int i = 0; i < profiles.size(); i++) {
            File file = profiles.get(i);
            String rawName = file.getName();
            String dispName = rawName.endsWith(".enc") ? rawName.substring(0, rawName.length() - 4) : rawName;
            int col = i % 2;
            int row = i / 2;
            float cx = areaX + col * (cellW + colGap);
            float cy = curY + row * (cellH + rowGap);
            if (cy + cellH < viewTop - 6f || cy > viewBottom + 6f) {
                continue;
            }
            float pFade = alpha * scroller.edgeFadeAlpha(cy, cellH, viewTop, viewBottom);
            UiHitRect cardHit = new UiHitRect(cx, cy, cellW, cellH);
            boolean hov = interactive && cardHit.contains(lx, ly);
            float prev = profileHovers.getOrDefault(dispName, 0f);
            float hv = UiRenderPrimitives.approachExp(prev, hov ? 1f : 0f, 16f, animDt);
            profileHovers.put(dispName, hv);

            UiRenderPrimitives.drawThemedGlassCard(
                graphics, cx, cy, cellW, cellH, cardR,
                32f + 14f * hv,
                18f + 8f * hv,
                48f + 24f * hv,
                22f + 12f * hv,
                pFade,
                hv * 0.35f,
                hv,
                theme
            );

            UiRenderPrimitives.drawIconBadge(graphics, LucideIcon.SHIELD, cx + 9f, cy + 11f, 20f, 0.45f + hv * 0.45f, pFade, theme);

            UiHitRect delRect = new UiHitRect(cx + cellW - 22f, cy + 12f, 16f, 18f);
            UiHitRect saveRect = new UiHitRect(cx + cellW - 68f, cy + 12f, 42f, 18f);
            UiHitRect loadRect = new UiHitRect(cx + cellW - 112f, cy + 12f, 40f, 18f);
            if (interactive) {
                profileHitboxes.add(new ProfileHit(file, dispName, loadRect, saveRect, delRect));
            }

            float maxTitleW = Math.max(40f, loadRect.x() - (cx + 35f) - 6f);
            SatoshiFont.draw(
                graphics,
                UiRenderPrimitives.ellipsize(dispName, maxTitleW, 5.8f, SatoshiWeight.SEMIBOLD),
                cx + 35f,
                cy + 10.5f,
                5.8f,
                TextGradient.pearl(pFade, accent, 0.25f, theme),
                SatoshiWeight.SEMIBOLD
            );
            long kb = Math.max(1L, file.length() / 1024L);
            SatoshiFont.draw(
                graphics,
                "Encrypted • " + kb + " KB",
                cx + 35f,
                cy + 22.5f,
                4.35f,
                TextGradient.mist(0.72f * pFade, theme),
                SatoshiWeight.MEDIUM
            );

            drawSmallPill(graphics, "Load", loadRect, interactive && loadRect.contains(lx, ly), true, pFade, accent, theme);
            drawSmallPill(graphics, "Save", saveRect, interactive && saveRect.contains(lx, ly), false, pFade, accent, theme);

            int delCol = (interactive && delRect.contains(lx, ly))
                ? UiRenderPrimitives.argb(Math.round(255f * pFade), 255, 118, 128)
                : UiRenderPrimitives.argb(Math.round(155f * pFade), 255, 255, 255);
            UiRenderPrimitives.drawIcon(graphics, LucideIcon.X_CLOSE, delRect.x() + 3.5f, delRect.y() + 4.5f, 9f, delCol);
        }
    }

    private static void drawSmallPill(
        GuiGraphicsExtractor graphics,
        String label,
        UiHitRect rect,
        boolean hovered,
        boolean primary,
        float alpha,
        int accent,
        UiTheme theme
    ) {
        int bg = primary
            ? UiRenderPrimitives.multiplyAlpha(accent, (hovered ? 0.45f : 0.28f) * alpha)
            : UiRenderPrimitives.argb(Math.round((hovered ? 56f : 32f) * alpha), 255, 255, 255);
        int rim = primary
            ? UiRenderPrimitives.multiplyAlpha(accent, 0.85f * alpha)
            : UiRenderPrimitives.argb(Math.round((hovered ? 85f : 48f) * alpha), 255, 255, 255);
        UiRenderPrimitives.drawRoundedRect(graphics, rect.x(), rect.y(), rect.w(), rect.h(), 6f, bg);
        UiRenderPrimitives.drawRoundedBorder(graphics, rect.x(), rect.y(), rect.w(), rect.h(), 6f, 0.85f, rim, rim);
        SatoshiFont.drawCentered(
            graphics,
            label,
            rect.x() + rect.w() * 0.5f,
            rect.y() + (rect.h() - 4.8f) * 0.5f,
            4.8f,
            TextGradient.pearl(alpha, accent, 0.22f, theme),
            SatoshiWeight.SEMIBOLD
        );
    }

    @Override
    public boolean onMouseClicked(UiScreenActions actions, float localX, float localY, int button) {
        if (button != InputConstants.MOUSE_BUTTON_LEFT) {
            return false;
        }
        UiSoundSink soundSink = actions.sounds();
        UiTheme theme = actions.theme();

        if (inputHitbox.contains(localX, localY)) {
            nameInputFocused = true;
            actions.syncTextInput();
            soundSink.playTab(1.06f);
            return true;
        }
        if (nameInputFocused && !createBtnHitbox.contains(localX, localY)) {
            nameInputFocused = false;
            actions.syncTextInput();
        }

        if (createBtnHitbox.contains(localX, localY)) {
            saveNamedProfile(actions);
            return true;
        }

        for (BtnHit entry : topBtnHitboxes) {
            if (entry.rect().contains(localX, localY)) {
                handleTopAction(entry.id(), actions, localX, localY);
                return true;
            }
        }

        for (ProfileHit entry : profileHitboxes) {
            if (entry.loadRect().contains(localX, localY)) {
                try {
                    String encrypted = Files.readString(entry.file().toPath(), StandardCharsets.UTF_8);
                    ConfigManager.importString(encrypted);
                    ConfigManager.save();
                    actions.showToast("Loaded profile: " + entry.name());
                    actions.spawnRipple(localX, localY, 20f, theme.accentColor());
                    soundSink.playNotify();
                } catch (Exception e) {
                    actions.showToast("Failed to load profile");
                }
                return true;
            }
            if (entry.saveRect().contains(localX, localY)) {
                writeEncryptedProfile(entry.file());
                actions.showToast("Overwrote profile: " + entry.name());
                actions.spawnRipple(localX, localY, 18f, theme.accentColor());
                soundSink.playNotify();
                return true;
            }
            if (entry.delRect().contains(localX, localY)) {
                entry.file().delete();
                actions.showToast("Deleted profile: " + entry.name());
                actions.spawnRipple(localX, localY, 16f, 0xFFFF6470);
                soundSink.playClick(0.92f);
                return true;
            }
        }
        return false;
    }

    private void handleTopAction(String id, UiScreenActions actions, float lx, float ly) {
        Minecraft mc = Minecraft.getInstance();
        ImnotcheatingyouareClient client = ImnotcheatingyouareClient.INSTANCE;
        UiSoundSink soundSink = actions.sounds();
        UiTheme theme = actions.theme();

        switch (id) {
            case "save_active" -> {
                ConfigManager.save();
                ClientGuiRegistry.saveGuiPrefs();
                actions.showToast("Saved active configuration to disk");
                actions.spawnRipple(lx, ly, 22f, theme.accentColor());
                soundSink.playNotify();
            }
            case "reload_disk" -> {
                ConfigManager.load();
                ClientGuiRegistry.loadGuiPrefs();
                actions.showToast("Reloaded configuration from disk");
                actions.spawnRipple(lx, ly, 22f, theme.accentColor());
                soundSink.playNotify();
            }
            case "export_clip" -> {
                if (client != null && client.moduleManager != null && mc != null) {
                    ModuleManager mm = client.moduleManager;
                    String exported = ConfigManager.exportSpecific(mm.modules);
                    KeyboardHandler kh = mc.keyboardHandler;
                    kh.setClipboard(exported);
                    actions.showToast("Copied encrypted config to clipboard");
                    actions.spawnRipple(lx, ly, 22f, theme.accentColor());
                    soundSink.playNotify();
                }
            }
            case "import_clip" -> {
                if (mc != null) {
                    KeyboardHandler kh = mc.keyboardHandler;
                    String clip = kh.getClipboard();
                    if (clip != null && !clip.isBlank()) {
                        ConfigManager.importString(clip.trim());
                        ConfigManager.save();
                        actions.showToast("Imported configuration from clipboard");
                        actions.spawnRipple(lx, ly, 22f, theme.accentColor());
                        soundSink.playNotify();
                    } else {
                        actions.showToast("Clipboard is empty");
                        soundSink.playClick(0.92f);
                    }
                }
            }
            default -> {
            }
        }
    }

    private void saveNamedProfile(UiScreenActions actions) {
        UiSoundSink soundSink = actions.sounds();
        UiTheme theme = actions.theme();
        String raw = newProfileName.trim();
        if (raw.isEmpty()) {
            actions.showToast("Enter a profile name first");
            soundSink.playClick(0.92f);
            return;
        }
        String sanitized = raw.replaceAll("[^a-zA-Z0-9_\\- ]", "").trim();
        if (sanitized.isEmpty()) {
            sanitized = "profile";
        }
        File target = new File(profilesDir(), sanitized + ".enc");
        writeEncryptedProfile(target);
        newProfileName = "";
        nameInputFocused = false;
        actions.syncTextInput();
        actions.showToast("Saved profile: " + sanitized);
        actions.spawnRipple(createBtnHitbox.x() + createBtnHitbox.w() * 0.5f, createBtnHitbox.y() + 11f, 22f, theme.accentColor());
        soundSink.playNotify();
    }

    private static void writeEncryptedProfile(File target) {
        ImnotcheatingyouareClient client = ImnotcheatingyouareClient.INSTANCE;
        if (client == null || client.moduleManager == null) {
            return;
        }
        ModuleManager mm = client.moduleManager;
        String encrypted = ConfigManager.exportSpecific(mm.modules);
        if (encrypted != null && !encrypted.isEmpty()) {
            try {
                Files.writeString(target.toPath(), encrypted, StandardCharsets.UTF_8);
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public boolean onKeyPressed(UiScreenActions actions, KeyEvent event) {
        if (!nameInputFocused) {
            return false;
        }
        int key = event.key();
        UiSoundSink soundSink = actions.sounds();
        if (key == InputConstants.KEY_BACKSPACE) {
            if (!newProfileName.isEmpty()) {
                newProfileName = (event.modifiers() & InputConstants.MOD_CONTROL) != 0
                    ? ""
                    : newProfileName.substring(0, newProfileName.length() - 1);
                inputKeyPulse = 1f;
                soundSink.playTypeKey();
            }
            return true;
        }
        if (key == InputConstants.KEY_DELETE) {
            newProfileName = "";
            inputKeyPulse = 1f;
            soundSink.playTypeKey();
            return true;
        }
        if (key == InputConstants.KEY_RETURN || key == InputConstants.KEY_NUMPADENTER) {
            saveNamedProfile(actions);
            return true;
        }
        return true;
    }

    @Override
    public boolean onCharTyped(UiScreenActions actions, CharacterEvent event) {
        if (!nameInputFocused) {
            return false;
        }
        String ch = event.codepointAsString();
        if ("`".equals(ch) && newProfileName.isEmpty()) {
            return true;
        }
        if (newProfileName.length() < 28) {
            newProfileName = newProfileName + ch;
            inputKeyPulse = 1f;
            UiSoundSink soundSink = actions.sounds();
            soundSink.playTypeKey();
            return true;
        }
        return false;
    }

    @Override
    public boolean isTextInputFocused() {
        return nameInputFocused;
    }

    @Override
    public void clearFocus(UiScreenActions actions) {
        if (nameInputFocused) {
            nameInputFocused = false;
            actions.syncTextInput();
        }
    }

    @Override
    public void clearHitboxes() {
        inputHitbox = UiHitRect.EMPTY;
        createBtnHitbox = UiHitRect.EMPTY;
        topBtnHitboxes.clear();
        profileHitboxes.clear();
    }
}
