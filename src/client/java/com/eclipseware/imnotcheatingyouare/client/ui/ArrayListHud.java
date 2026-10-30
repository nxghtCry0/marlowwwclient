package com.eclipseware.imnotcheatingyouare.client.ui;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.clickgui.ImGuiClickGui;
import com.eclipseware.imnotcheatingyouare.client.gui.PastelShaderBackground;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.module.ModuleManager;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.setting.SettingsManager;
import com.eclipseware.imnotcheatingyouare.client.utils.AnimationUtil;
import com.eclipseware.imnotcheatingyouare.client.utils.ImGuiTextures;
import com.eclipseware.imnotcheatingyouare.client.utils.InputUtil;
import com.mojang.blaze3d.platform.Window;
import imgui.ImDrawList;
import imgui.ImFont;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.ImVec2;
import imgui.flag.ImDrawFlags;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2fStack;
import xyz.breadloaf.imguimc.imgui.ImguiLoader;

public class ArrayListHud {
    public static final ArrayListHud INSTANCE = new ArrayListHud();

    public double x = 5;
    public double y = 5;

    private static final Identifier LOGO_ID = Identifier.parse("imnotcheatingyouare:textures/logo.png");
    private static final ImVec2 textSize = new ImVec2();
    private static final ImVec2 keySize = new ImVec2();

    private final Map<Module, Float> slotY = new HashMap<>();
    private final Map<Module, Float> imguiAnim = new HashMap<>();

    private int lastRenderR = 255;
    private int lastRenderG = 118;
    private int lastRenderB = 26;
    private int lastBgAlpha = 238;
    private float lastGlassOpacity = 0.78f;
    private boolean lastGlowEnabled = true;
    private String lastAlignment = "Left";
    private float lastStartY = 43f;
    private float lastScale = 1f;
    private float lastAnimSpeed = 0.15f;
    private boolean visibleThisFrame;

    private static class RowEntry {
        final Module module;
        final String suffix;
        final float nameW;
        final float suffixW;
        final float pillW;
        final float totalW;
        final float anim;

        RowEntry(Module module, String suffix, float nameW, float suffixW, float pillW, float totalW, float anim) {
            this.module = module;
            this.suffix = suffix;
            this.nameW = nameW;
            this.suffixW = suffixW;
            this.pillW = pillW;
            this.totalW = totalW;
            this.anim = anim;
        }
    }

    private static int col(int r, int g, int b, int a) {
        int ca = Math.max(0, Math.min(255, a));
        int cr = Math.max(0, Math.min(255, r));
        int cg = Math.max(0, Math.min(255, g));
        int cb = Math.max(0, Math.min(255, b));
        return ((ca & 0xFF) << 24) | ((cb & 0xFF) << 16) | ((cg & 0xFF) << 8) | (cr & 0xFF);
    }

    private static float clamp(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private static boolean shouldExcludeFromArrayList(Module m) {
        if (m == null || m.isHidden()) {
            return true;
        }
        String name = m.getName();
        if (name == null) {
            return true;
        }
        return "Menu".equalsIgnoreCase(name)
            || "GUI".equalsIgnoreCase(name)
            || "Theme".equalsIgnoreCase(name)
            || "HUDEditor".equalsIgnoreCase(name)
            || "Configurator".equalsIgnoreCase(name)
            || "RecommendedConfigs".equalsIgnoreCase(name)
            || "Config Menu".equalsIgnoreCase(name);
    }

    private static void drawShaderBox(
        ImDrawList dl,
        int shaderTex,
        float x1,
        float y1,
        float x2,
        float y2,
        float boxX,
        float boxY,
        float boxW,
        float boxH,
        int alpha,
        float round,
        int flags
    ) {
        int clampedA = Math.max(0, Math.min(255, alpha));
        if (clampedA == 0 || shaderTex == 0) {
            return;
        }
        float bw = Math.max(1f, boxW);
        float bh = Math.max(1f, boxH);
        float u1 = clamp(0.06f + ((x1 - boxX) / bw) * 0.88f, 0.04f, 0.96f);
        float v1 = clamp(0.94f - ((y1 - boxY) / bh) * 0.88f, 0.04f, 0.96f);
        float u2 = clamp(0.06f + ((x2 - boxX) / bw) * 0.88f, 0.04f, 0.96f);
        float v2 = clamp(0.94f - ((y2 - boxY) / bh) * 0.88f, 0.04f, 0.96f);
        int tint = col(255, 255, 255, clampedA);
        if (round > 0f) {
            dl.addImageRounded(shaderTex, x1, y1, x2, y2, u1, v1, u2, v2, tint, round, flags);
        } else {
            dl.addImage(shaderTex, x1, y1, x2, y2, u1, v1, u2, v2, tint);
        }
    }

    private static void drawHudPumpkin(ImDrawList dl, float cx, float cy, float r, float time, float dim) {
        float flicker = 0.65f + 0.35f * (float) Math.sin(time * 5.2f);
        int pumpkinDark = col(205, 74, 14, (int) (245 * dim));
        int pumpkinBright = col(255, 124, 24, (int) (255 * dim));
        int eyeGlow = col(255, (int) (212 + 38 * flicker), 64, (int) (255 * dim));
        int stemCol = col(92, 168, 62, (int) (245 * dim));
        dl.addRectFilled(cx - 1.1f, cy - r - 2.3f, cx + 1.3f, cy - r + 0.7f, stemCol, 1f);
        dl.addCircleFilled(cx - r * 0.36f, cy, r * 0.82f, pumpkinDark);
        dl.addCircleFilled(cx + r * 0.36f, cy, r * 0.82f, pumpkinDark);
        dl.addCircleFilled(cx, cy, r * 0.92f, pumpkinBright);
        dl.addTriangleFilled(
            cx - r * 0.52f, cy - r * 0.08f,
            cx - r * 0.16f, cy - r * 0.08f,
            cx - r * 0.34f, cy - r * 0.42f,
            eyeGlow
        );
        dl.addTriangleFilled(
            cx + r * 0.16f, cy - r * 0.08f,
            cx + r * 0.52f, cy - r * 0.08f,
            cx + r * 0.34f, cy - r * 0.42f,
            eyeGlow
        );
        dl.addLine(cx - r * 0.44f, cy + r * 0.28f, cx, cy + r * 0.45f, eyeGlow, 1.2f);
        dl.addLine(cx, cy + r * 0.45f, cx + r * 0.44f, cy + r * 0.28f, eyeGlow, 1.2f);
    }

    private static void drawHudCobweb(ImDrawList dl, float cx, float cy, float radius, float dim, boolean flipX) {
        int webCol = col(235, 215, 255, (int) (36 * dim));
        int webHi = col(255, 126, 28, (int) (44 * dim));
        float dirX = flipX ? -1f : 1f;
        int spokes = 5;
        for (int i = 0; i < spokes; i++) {
            double ang = (Math.PI * 0.5 * i) / (spokes - 1);
            float sx = cx + dirX * (float) Math.cos(ang) * radius;
            float sy = cy + (float) Math.sin(ang) * radius;
            dl.addLine(cx, cy, sx, sy, (i % 2 == 0) ? webCol : webHi, 0.9f);
        }
        for (int ring = 1; ring <= 2; ring++) {
            float r = radius * (ring / 2.3f);
            for (int i = 0; i < spokes - 1; i++) {
                double a0 = (Math.PI * 0.5 * i) / (spokes - 1);
                double a1 = (Math.PI * 0.5 * (i + 1)) / (spokes - 1);
                float x0 = cx + dirX * (float) Math.cos(a0) * r;
                float y0 = cy + (float) Math.sin(a0) * r;
                float x1 = cx + dirX * (float) Math.cos(a1) * r;
                float y1 = cy + (float) Math.sin(a1) * r;
                dl.addLine(x0, y0, x1, y1, webCol, 0.85f);
            }
        }
    }

    private static int[] sampleHalloweenWave(int baseR, int baseG, int baseB, float time, int index) {
        float phase = (float) Math.sin(time * 1.55f + index * 0.42f) * 0.5f + 0.5f;
        int r;
        int g;
        int b;
        if (phase < 0.55f) {
            float t = phase / 0.55f;
            r = Math.round(baseR + (255 - baseR) * t);
            g = Math.round(baseG + (168 - baseG) * t);
            b = Math.round(baseB + (38 - baseB) * t);
        } else {
            float t = (phase - 0.55f) / 0.45f;
            r = Math.round(255 + (176 - 255) * t);
            g = Math.round(168 + (76 - 168) * t);
            b = Math.round(38 + (255 - 38) * t);
        }
        return new int[]{
            Math.max(0, Math.min(255, r)),
            Math.max(0, Math.min(255, g)),
            Math.max(0, Math.min(255, b))
        };
    }

    private void renderKeybinds(
        float k,
        float displayW,
        boolean arrayRight,
        int ar,
        int ag,
        int ab,
        float dim,
        int shaderTex,
        float time
    ) {
        ImnotcheatingyouareClient client = ImnotcheatingyouareClient.INSTANCE;
        if (client == null || client.moduleManager == null || client.settingsManager == null) {
            return;
        }
        ModuleManager mm = client.moduleManager;
        SettingsManager sm = client.settingsManager;
        Module kbMod = mm.getModule("KeybindList");
        if (kbMod == null || !kbMod.isToggled()) {
            return;
        }
        Setting onlyEnabledSetting = sm.getSettingByName(kbMod, "Only Enabled");
        boolean onlyEnabled = onlyEnabledSetting != null && onlyEnabledSetting.getValBoolean();
        Setting scaleSetting = sm.getSettingByName(kbMod, "Scale");
        float kbScale = scaleSetting != null ? (float) scaleSetting.getValDouble() : 0.85f;
        float fontScale = clamp(kbScale * 1.12f, 0.65f, 1.35f) * 0.88f;

        List<Module> mods = new ArrayList<>();
        for (Module m : mm.modules) {
            if (m.isHidden()) {
                continue;
            }
            if (onlyEnabled && !m.isToggled()) {
                continue;
            }
            int key = m.getKeyBind();
            if (key == -1 || key == 0 || "NONE".equals(getKeyName(key))) {
                continue;
            }
            mods.add(m);
        }
        if (mods.isEmpty()) {
            return;
        }

        ImDrawList dl = ImGui.getBackgroundDrawList();
        ImFont font = ImGui.getFont();
        float baseFontSize = ImGui.getFontSize();
        float fontSize = baseFontSize * fontScale;
        float subFontSize = fontSize * 0.88f;
        float rowH = fontSize + 8f * fontScale;
        float headerH = fontSize + 13f * fontScale;
        float maxName = 0f;
        float maxKey = 0f;
        for (Module m : mods) {
            ImGui.calcTextSize(textSize, m.getName());
            maxName = Math.max(maxName, textSize.x * fontScale);
            ImGui.calcTextSize(keySize, getKeyName(m.getKeyBind()));
            maxKey = Math.max(maxKey, keySize.x * fontScale * 0.88f);
        }
        float w = Math.max(142f * fontScale, maxName + maxKey + 48f * fontScale);
        float h = headerH + mods.size() * rowH + 8f * fontScale;
        float margin = 8f * k;
        float kx = arrayRight ? margin : (displayW - margin - w);
        float ky = margin;
        float round = 9f;
        int a255 = (int) (255 * dim);

        if (lastGlowEnabled) {
            float breathe = 0.5f + 0.5f * (float) Math.sin(time * 1.6f);
            for (int i = 2; i >= 1; i--) {
                float expand = i * 3.2f + breathe * 1.6f;
                int glowA = (int) (((16f + breathe * 8f) / i) * dim);
                int glowCol = (i == 2)
                    ? col(162, 68, 255, glowA)
                    : col(ar, ag, ab, glowA);
                dl.addRect(
                    kx - expand,
                    ky - expand,
                    kx + w + expand,
                    ky + h + expand,
                    glowCol,
                    round + expand,
                    0,
                    1.6f
                );
            }
        }

        dl.addRectFilled(kx + 2f, ky + 4f, kx + w + 2f, ky + h + 4f, col(0, 0, 0, (int) (82 * dim)), round);
        dl.addRectFilled(kx, ky, kx + w, ky + h, col(14, 8, 22, (int) (lastBgAlpha * dim)), round);
        drawShaderBox(
            dl,
            shaderTex,
            kx,
            ky,
            kx + w,
            ky + h,
            kx,
            ky,
            w,
            h,
            (int) (142 * lastGlassOpacity * dim),
            round,
            0
        );
        dl.addRectFilled(kx, ky, kx + w, ky + h, col(13, 8, 20, (int) (140 * dim)), round);
        drawHudCobweb(dl, kx + w - 2f, ky + headerH + 1f, 22f * fontScale, dim, true);

        dl.addRectFilled(
            kx,
            ky,
            kx + w,
            ky + headerH,
            col(11, 7, 18, (int) (228 * dim)),
            round,
            ImDrawFlags.RoundCornersTop
        );
        drawShaderBox(
            dl,
            shaderTex,
            kx,
            ky,
            kx + w,
            ky + headerH,
            kx,
            ky,
            w,
            h,
            (int) (125 * lastGlassOpacity * dim),
            round,
            ImDrawFlags.RoundCornersTop
        );
        dl.addLine(kx, ky + headerH, kx + w, ky + headerH, col(ar, ag, ab, (int) (64 * dim)), 1f);

        float pulse = 0.55f + 0.45f * (float) Math.sin(time * 2.2f);
        int borderA = (int) ((105f + 50f * pulse) * dim);
        dl.addRect(kx, ky, kx + w, ky + h, col(ar, ag, ab, borderA), round, 0, 1.2f);

        drawHudPumpkin(dl, kx + 12f, ky + headerH * 0.5f + 0.5f, 4.8f * fontScale, time, dim);
        dl.addText(font, fontSize, kx + 22f, ky + (headerH - fontSize) * 0.5f, col(255, 244, 232, a255), "Keybinds");

        String count = String.valueOf(mods.size());
        ImGui.calcTextSize(keySize, count);
        float cntTextW = keySize.x * (subFontSize / baseFontSize);
        float cntW = cntTextW + 10f;
        float cntH = subFontSize + 4f;
        float cntX = kx + w - 8f - cntW;
        float cntY = ky + (headerH - cntH) * 0.5f;
        dl.addRectFilled(cntX, cntY, cntX + cntW, cntY + cntH, col(22, 11, 32, (int) (225 * dim)), 4f);
        dl.addRect(cntX, cntY, cntX + cntW, cntY + cntH, col(ar, ag, ab, (int) (110 * dim)), 4f, 0, 0.9f);
        dl.addText(font, subFontSize, cntX + 5f, cntY + (cntH - subFontSize) * 0.5f, col(255, 168, 52, (int) (245 * dim)), count);

        float ry = ky + headerH + 4f * fontScale;
        for (int idx = 0; idx < mods.size(); idx++) {
            Module m = mods.get(idx);
            boolean on = m.isToggled();
            int[] waveRgb = sampleHalloweenWave(ar, ag, ab, time, idx);
            int wr = waveRgb[0];
            int wg = waveRgb[1];
            int wb = waveRgb[2];
            int nameCol = on ? col(255, 244, 232, a255) : col(194, 178, 214, (int) (208 * dim));
            dl.addText(font, fontSize, kx + 10f, ry + (rowH - fontSize) * 0.5f, nameCol, m.getName());
            String key = getKeyName(m.getKeyBind());
            ImGui.calcTextSize(keySize, key);
            float keyTextW = keySize.x * (subFontSize / baseFontSize);
            float pw = keyTextW + 11f;
            float px = kx + w - 8f - pw;
            float py = ry + 2f;
            float ph = rowH - 4f;
            int pillBg = on ? col(wr, wg, wb, (int) (92 * dim)) : col(18, 11, 28, (int) (215 * dim));
            int pillBd = on ? col(wr, wg, wb, (int) (215 * dim)) : col(162, 68, 255, (int) (55 * dim));
            dl.addRectFilled(px, py, px + pw, py + ph, pillBg, 4.5f);
            dl.addRect(px, py, px + pw, py + ph, pillBd, 4.5f, 0, 1f);
            int keyCol = on ? col(255, 248, 236, a255) : col(205, 190, 224, (int) (215 * dim));
            dl.addText(font, subFontSize, px + 5.5f, py + (ph - subFontSize) * 0.5f, keyCol, key);
            ry += rowH;
        }
    }

    public void renderImGui() {
        if (!visibleThisFrame) {
            return;
        }
        visibleThisFrame = false;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) {
            return;
        }
        ImnotcheatingyouareClient client = ImnotcheatingyouareClient.INSTANCE;
        if (client == null || client.moduleManager == null || client.settingsManager == null) {
            return;
        }
        ModuleManager mm = client.moduleManager;
        SettingsManager sm = client.settingsManager;

        float dim = ImGuiClickGui.isOpen() || mc.gui.screen() != null ? 0.35f : 1f;
        Window window = mc.getWindow();
        float guiScale = (float) window.getGuiScale();
        float uiScale = ImguiLoader.getUiScale();
        ImGuiIO io = ImGui.getIO();
        float displayW = io.getDisplaySizeX();
        boolean right = "Right".equalsIgnoreCase(lastAlignment);
        float dt = Math.min(0.1f, io.getDeltaTime());
        float speed = Math.max(4f, lastAnimSpeed * 60f);

        ImFont font = ImGui.getFont();
        float rawFontSize = ImGui.getFontSize();
        float fontScale = clamp(lastScale, 0.5f, 1.5f) * 0.90f;
        float titleFontSize = rawFontSize * fontScale;
        float subFontSize = titleFontSize * 0.85f;
        float rowH = titleFontSize + 9f * fontScale;
        float gap = 3f * fontScale;

        List<RowEntry> rows = new ArrayList<>();
        float maxRowW = 120f;
        for (Module m : mm.modules) {
            if (shouldExcludeFromArrayList(m)) {
                slotY.remove(m);
                continue;
            }
            float a = imguiAnim.getOrDefault(m, 0f);
            float target = m.isToggled() ? 1f : 0f;
            a += (target - a) * Math.min(1f, dt * speed);
            imguiAnim.put(m, a);
            if (a < 0.01f) {
                slotY.remove(m);
                continue;
            }
            String suffix = resolveModuleSuffix(sm, m);
            ImGui.calcTextSize(textSize, m.getName());
            float nameW = textSize.x * (titleFontSize / rawFontSize);
            float suffixW = 0f;
            float pillW = 0f;
            if (!suffix.isEmpty()) {
                ImGui.calcTextSize(textSize, suffix);
                suffixW = textSize.x * (subFontSize / rawFontSize);
                pillW = suffixW + 10f * fontScale;
            }
            float totalW = nameW + (pillW > 0f ? (pillW + 7f * fontScale) : 0f) + 20f * fontScale;
            maxRowW = Math.max(maxRowW, totalW);
            rows.add(new RowEntry(m, suffix, nameW, suffixW, pillW, totalW, a));
        }
        rows.sort((r1, r2) -> Float.compare(r2.totalW, r1.totalW));

        ImDrawList dl = ImGui.getBackgroundDrawList();
        float margin = 6f * (guiScale / uiScale);
        float startY = lastStartY * (guiScale / uiScale);
        float targetY = startY;
        float time = (float) (System.nanoTime() / 1_000_000_000.0);

        float[] hsb = Color.RGBtoHSB(lastRenderR, lastRenderG, lastRenderB, null);
        int shader = PastelShaderBackground.render() ? PastelShaderBackground.glTextureId() : 0;
        if (shader != 0) {
            ImGuiTextures.prepare(shader, true);
        }

        int baseAcc = Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]);
        int baseAr = (baseAcc >> 16) & 0xFF;
        int baseAg = (baseAcc >> 8) & 0xFF;
        int baseAb = baseAcc & 0xFF;
        renderKeybinds(guiScale / uiScale, displayW, right, baseAr, baseAg, baseAb, dim, shader, time);

        if (rows.isEmpty()) {
            return;
        }

        float totalStackH = Math.max(180f, rows.size() * (rowH + gap) + 16f);
        float shaderBoxW = Math.max(180f, maxRowW + 16f);
        float shaderBoxX = right ? (displayW - margin - shaderBoxW) : margin;
        float shaderBoxY = startY;

        for (int i = 0; i < rows.size(); i++) {
            RowEntry row = rows.get(i);
            Module m = row.module;
            float a = row.anim;
            float e = AnimationUtil.easeOutCubic(a);
            float visA = a * dim;

            float current = slotY.getOrDefault(m, targetY);
            current += (targetY - current) * Math.min(1f, dt * 16f);
            slotY.put(m, current);

            float w = row.totalW;
            float h = rowH * e;
            float slide = (1f - e) * (w + 14f);
            float rx = right ? (displayW - margin - w + slide) : (margin - slide);
            float ry = current;
            float round = 7f * fontScale;

            int[] waveRgb = sampleHalloweenWave(lastRenderR, lastRenderG, lastRenderB, time, i);
            int ar = waveRgb[0];
            int ag = waveRgb[1];
            int ab = waveRgb[2];
            int alpha = (int) (255 * visA);

            if (lastGlowEnabled && e > 0.4f) {
                dl.addRect(
                    rx - 1.5f,
                    ry - 1.5f,
                    rx + w + 1.5f,
                    ry + h + 1.5f,
                    col(ar, ag, ab, (int) (34 * visA)),
                    round + 1.5f,
                    0,
                    1.6f
                );
            }

            float shadowOffX = right ? -1.5f : 1.5f;
            dl.addRectFilled(
                rx + shadowOffX,
                ry + 2.5f,
                rx + w + shadowOffX,
                ry + h + 2.5f,
                col(0, 0, 0, (int) (78 * visA)),
                round
            );

            dl.addRectFilled(rx, ry, rx + w, ry + h, col(14, 8, 22, (int) (lastBgAlpha * visA)), round);

            drawShaderBox(
                dl,
                shader,
                rx,
                ry,
                rx + w,
                ry + h,
                shaderBoxX,
                shaderBoxY,
                shaderBoxW,
                totalStackH,
                (int) (162 * lastGlassOpacity * visA),
                round,
                0
            );

            dl.addRectFilled(rx, ry, rx + w, ry + h, col(15, 9, 24, (int) (108 * visA)), round);
            dl.addRectFilled(rx, ry, rx + w, ry + h, col(ar, ag, ab, (int) (32 * visA)), round);

            float sweep = (((time * 0.34f + i * 0.09f) % 2.2f) - 0.5f);
            float bandW = Math.max(24f, w * 0.32f);
            float bx = rx + sweep * w;
            if (bx + bandW > rx && bx - bandW < rx + w && h > 4f) {
                dl.pushClipRect(rx + 2f, ry + 1f, rx + w - 2f, ry + h - 1f, true);
                int shimClear = col(ar, ag, ab, 0);
                int shimPeak = col(ar, ag, ab, (int) (34 * visA));
                dl.addRectFilledMultiColor(bx - bandW, ry, bx, ry + h, shimClear, shimPeak, shimPeak, shimClear);
                dl.addRectFilledMultiColor(bx, ry, bx + bandW, ry + h, shimPeak, shimClear, shimClear, shimPeak);
                dl.popClipRect();
            }

            int glowIn = col(ar, ag, ab, (int) (78 * visA));
            int glowOut = col(ar, ag, ab, 0);
            float glowW = Math.min(w * 0.48f, 28f);
            float barH = Math.max(4f, (h - 9f * fontScale) * e);
            float midY = ry + h * 0.5f;
            if (right) {
                dl.addRectFilledMultiColor(rx + w - glowW, ry, rx + w, ry + h, glowOut, glowIn, glowIn, glowOut);
                dl.addRectFilled(rx + w - 6.5f * fontScale, midY - barH * 0.5f, rx + w - 3.3f * fontScale, midY + barH * 0.5f, col(ar, ag, ab, alpha), 1.6f);
            } else {
                dl.addRectFilledMultiColor(rx, ry, rx + glowW, ry + h, glowIn, glowOut, glowOut, glowIn);
                dl.addRectFilled(rx + 3.3f * fontScale, midY - barH * 0.5f, rx + 6.5f * fontScale, midY + barH * 0.5f, col(ar, ag, ab, alpha), 1.6f);
            }

            dl.addRect(rx, ry, rx + w, ry + h, col(ar, ag, ab, (int) (168 * visA)), round, 0, 1.0f);

            if (e > 0.35f) {
                float ty = ry + (h - titleFontSize) * 0.5f;
                float tx = right ? (rx + 9f * fontScale) : (rx + 11f * fontScale);
                dl.addText(font, titleFontSize, tx + 0.8f, ty + 0.8f, col(0, 0, 0, (int) (145 * visA)), m.getName());
                dl.addText(font, titleFontSize, tx, ty, col(255, 245, 234, alpha), m.getName());

                if (!row.suffix.isEmpty()) {
                    float px = tx + row.nameW + 6f * fontScale;
                    float ph = subFontSize + 3.5f * fontScale;
                    float py = ry + (h - ph) * 0.5f;
                    dl.addRectFilled(px, py, px + row.pillW, py + ph, col(18, 10, 28, (int) (220 * visA)), 4f * fontScale);
                    dl.addRect(px, py, px + row.pillW, py + ph, col(ar, ag, ab, (int) (115 * visA)), 4f * fontScale, 0, 1f);
                    dl.addText(
                        font,
                        subFontSize,
                        px + 5f * fontScale,
                        py + (ph - subFontSize) * 0.5f,
                        col(ar, ag, ab, (int) (248 * visA)),
                        row.suffix
                    );
                }
            }

            targetY += (rowH + gap) * e;
        }
    }

    private String resolveModuleSuffix(SettingsManager sm, Module m) {
        List<Setting> settings = sm.getSettingsByMod(m);
        if (settings == null) {
            return "";
        }
        for (Setting st : settings) {
            if (st.isCombo()) {
                String val = st.getValString();
                return val != null ? val : "";
            }
        }
        return "";
    }

    public void render(GuiGraphicsExtractor guiGraphics, float partialTick) {
        ImnotcheatingyouareClient client = ImnotcheatingyouareClient.INSTANCE;
        if (client == null || client.moduleManager == null || client.settingsManager == null) {
            return;
        }
        ModuleManager mm = client.moduleManager;
        SettingsManager sm = client.settingsManager;

        Module arrayListMod = mm.getModule("ArrayList");
        if (arrayListMod == null || !arrayListMod.isToggled()) {
            return;
        }

        Module bypassMod = mm.getModule("Bypass");
        if (bypassMod != null && bypassMod.isToggled()) {
            return;
        }

        Setting syncSetting = sm.getSettingByName(arrayListMod, "Sync Theme");
        boolean syncTheme = syncSetting == null || syncSetting.getValBoolean();
        Setting alignSetting = sm.getSettingByName(arrayListMod, "Alignment");
        String alignment = alignSetting != null ? alignSetting.getValString() : "Left";
        Setting offsetSetting = sm.getSettingByName(arrayListMod, "Y Offset");
        double startY = offsetSetting != null ? offsetSetting.getValDouble() : 5.0;

        Minecraft mc = Minecraft.getInstance();
        Window win = mc.getWindow();
        int logoWidth = 110;
        int logoHeight = 33;
        int logoX = "Right".equalsIgnoreCase(alignment) ? (win.getGuiScaledWidth() - logoWidth - 5) : 5;
        guiGraphics.blit(LOGO_ID, logoX, 5, logoX + logoWidth, 5 + logoHeight, 0.0f, 1.0f, 0.0f, 1.0f);

        startY = Math.max(startY, (double) (logoHeight + 10));

        int r = 255;
        int g = 118;
        int b = 26;
        int bgA = 238;
        float glassOp = 0.78f;
        boolean glowOn = true;
        float animSpeed = 0.15f;

        Module theme = mm.getModule("Theme");
        if (theme != null) {
            Setting alphaS = sm.getSettingByName(theme, "Background Alpha");
            if (alphaS != null) {
                bgA = Math.max(150, Math.min(255, (int) alphaS.getValDouble()));
            }
            Setting speedS = sm.getSettingByName(theme, "Anim Speed");
            if (speedS != null) {
                animSpeed = (float) speedS.getValDouble() * 0.03f;
            }
        }
        Module guiMod = mm.getModule("GUI");
        if (guiMod != null) {
            Setting glassS = sm.getSettingByName(guiMod, "Glass Opacity");
            if (glassS != null) {
                glassOp = (float) (glassS.getValDouble() / 100.0);
            }
            Setting glowS = sm.getSettingByName(guiMod, "Glow");
            if (glowS != null) {
                glowOn = glowS.getValBoolean();
            }
        }

        if (syncTheme && theme != null) {
            Setting accColorSetting = sm.getSettingByName(theme, "Accent Color");
            if (accColorSetting != null && accColorSetting.isColor()) {
                int cVal = accColorSetting.getValColor();
                r = (cVal >> 16) & 0xFF;
                g = (cVal >> 8) & 0xFF;
                b = cVal & 0xFF;
            }
        } else {
            Setting textColorSetting = sm.getSettingByName(arrayListMod, "Text Color");
            if (textColorSetting != null && textColorSetting.isColor()) {
                int cVal = textColorSetting.getValColor();
                r = (cVal >> 16) & 0xFF;
                g = (cVal >> 8) & 0xFF;
                b = cVal & 0xFF;
            }
        }

        Setting scaleSetting = sm.getSettingByName(arrayListMod, "Scale");
        float scale = scaleSetting != null ? (float) scaleSetting.getValDouble() : 1.0f;

        Matrix3x2fStack pose = guiGraphics.pose();
        pose.pushMatrix();
        pose.scale(scale, scale);

        lastRenderR = r;
        lastRenderG = g;
        lastRenderB = b;
        lastBgAlpha = bgA;
        lastGlassOpacity = glassOp;
        lastGlowEnabled = glowOn;
        lastAlignment = alignment;
        lastStartY = (float) startY;
        lastScale = scale;
        lastAnimSpeed = animSpeed;
        visibleThisFrame = true;

        pose.popMatrix();
    }

    private String getKeyName(int key) {
        return InputUtil.getName(key);
    }
}
