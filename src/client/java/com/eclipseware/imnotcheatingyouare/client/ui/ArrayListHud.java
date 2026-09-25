package com.eclipseware.imnotcheatingyouare.client.ui;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.utils.FontUtils;
import com.eclipseware.imnotcheatingyouare.client.utils.AnimationUtil;
import com.eclipseware.imnotcheatingyouare.client.utils.remnant.Render2DEngine;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.DynamicTexture;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ArrayListHud {
    public static final ArrayListHud INSTANCE = new ArrayListHud();
    
    public double x = 5;
    public double y = 5;

    private static class ModRenderInfo {
        final Module module;
        final String displayName;
        final int textWidth;
        final float anim;

        ModRenderInfo(Module module, String displayName, int textWidth, float anim) {
            this.module = module;
            this.displayName = displayName;
            this.textWidth = textWidth;
            this.anim = anim;
        }
    }

    private final Map<Module, Float> animMap = new HashMap<>();

    private static final Identifier LOGO_ID = Identifier.parse("imnotcheatingyouare:textures/logo.png");

    private String getDisplayName(Module m) {
        List<com.eclipseware.imnotcheatingyouare.client.setting.Setting> settings = 
            ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingsByMod(m);
        String suffix = "";
        if (settings != null) {
            for (com.eclipseware.imnotcheatingyouare.client.setting.Setting s : settings) {
                if (s.isCombo()) {
                    suffix = " \u00a77" + s.getValString();
                    break;
                }
            }
        }
        return m.getName() + suffix;
    }

    private int interpolateColor(int color1, int color2, float fraction) {
        int a1 = (color1 >> 24) & 0xff;
        int r1 = (color1 >> 16) & 0xff;
        int g1 = (color1 >> 8) & 0xff;
        int b1 = color1 & 0xff;

        int a2 = (color2 >> 24) & 0xff;
        int r2 = (color2 >> 16) & 0xff;
        int g2 = (color2 >> 8) & 0xff;
        int b2 = color2 & 0xff;

        int a = (int) (a1 + (a2 - a1) * fraction);
        int r = (int) (r1 + (r2 - r1) * fraction);
        int g = (int) (g1 + (g2 - g1) * fraction);
        int b = (int) (b1 + (b2 - b1) * fraction);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private int lastRenderR = 230, lastRenderG = 10, lastRenderB = 230;
    private String lastAlignment = "Left";
    private float lastStartY = 43f;
    private float lastScale = 1f;
    private float lastAnimSpeed = 0.15f;
    private boolean visibleThisFrame;
    private final Map<Module, Float> slotY = new HashMap<>();
    private final Map<Module, Float> imguiAnim = new HashMap<>();
    private static final imgui.ImVec2 textSize = new imgui.ImVec2();

    private static int col(int r, int g, int b, int a) {
        return ((Math.max(0, Math.min(255, a)) & 0xFF) << 24) | ((b & 0xFF) << 16) | ((g & 0xFF) << 8) | (r & 0xFF);
    }

    private static final imgui.ImVec2 keySize = new imgui.ImVec2();

    private void renderKeybinds(float k, float displayW, boolean arrayRight, int ar, int ag, int ab, float dim) {
        Module kbMod = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("KeybindList");
        if (kbMod == null || !kbMod.isToggled()) return;
        Setting onlyEnabledSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(kbMod, "Only Enabled");
        boolean onlyEnabled = onlyEnabledSetting != null && onlyEnabledSetting.getValBoolean();

        List<Module> mods = new ArrayList<>();
        for (Module m : ImnotcheatingyouareClient.INSTANCE.moduleManager.modules) {
            if (m.isHidden()) continue;
            if (onlyEnabled && !m.isToggled()) continue;
            int key = m.getKeyBind();
            if (key == -1 || key == 0 || getKeyName(key).equals("NONE")) continue;
            mods.add(m);
        }
        if (mods.isEmpty()) return;

        imgui.ImDrawList dl = imgui.ImGui.getBackgroundDrawList();
        float fontSize = imgui.ImGui.getFontSize();
        float rowH = fontSize + 7f;
        float headerH = fontSize + 12f;
        float maxName = 0f;
        float maxKey = 0f;
        for (Module m : mods) {
            imgui.ImGui.calcTextSize(textSize, m.getName());
            maxName = Math.max(maxName, textSize.x);
            imgui.ImGui.calcTextSize(keySize, getKeyName(m.getKeyBind()));
            maxKey = Math.max(maxKey, keySize.x);
        }
        float w = Math.max(130f, maxName + maxKey + 44f);
        float h = headerH + mods.size() * rowH + 8f;
        float margin = 8f * k;
        float x = arrayRight ? margin : displayW - margin - w;
        float y = margin;
        float round = 9f;
        int a255 = (int) (255 * dim);

        dl.addRectFilled(x + 2f, y + 3f, x + w + 2f, y + h + 3f, col(0, 0, 0, (int) (60 * dim)), round);
        dl.addRectFilled(x, y, x + w, y + h, col(16, 12, 26, (int) (200 * dim)), round);
        int shader = com.eclipseware.imnotcheatingyouare.client.gui.PastelShaderBackground.glTextureId();
        float dispH = Math.max(1f, imgui.ImGui.getIO().getDisplaySizeY());
        if (shader != 0) {
            dl.addImageRounded(shader, x, y, x + w, y + headerH, x / displayW, 1f - y / dispH, (x + w) / displayW, 1f - (y + headerH) / dispH,
                    col(255, 255, 255, (int) (120 * dim)), round, imgui.flag.ImDrawFlags.RoundCornersTop);
        }
        dl.addRect(x, y, x + w, y + h, col(ar, ag, ab, (int) (70 * dim)), round, 0, 1f);
        dl.addCircleFilled(x + 12f, y + headerH / 2f, 3f, col(ar, ag, ab, a255));
        dl.addText(x + 21f, y + (headerH - fontSize) / 2f, col(255, 255, 255, a255), "Keybinds");
        String count = String.valueOf(mods.size());
        imgui.ImGui.calcTextSize(keySize, count);
        dl.addText(x + w - 10f - keySize.x, y + (headerH - fontSize) / 2f, col(235, 225, 250, (int) (150 * dim)), count);

        float ry = y + headerH + 4f;
        for (Module m : mods) {
            boolean on = m.isToggled();
            dl.addText(x + 10f, ry + (rowH - fontSize) / 2f, on ? col(255, 255, 255, a255) : col(190, 182, 210, (int) (200 * dim)), m.getName());
            String key = getKeyName(m.getKeyBind());
            imgui.ImGui.calcTextSize(keySize, key);
            float pw = keySize.x + 10f;
            float px = x + w - 8f - pw;
            float py = ry + 2f;
            float ph = rowH - 4f;
            dl.addRectFilled(px, py, px + pw, py + ph, on ? col(ar, ag, ab, (int) (170 * dim)) : col(255, 255, 255, (int) (22 * dim)), ph / 2f);
            dl.addText(px + 5f, py + (ph - fontSize) / 2f, col(255, 255, 255, on ? a255 : (int) (170 * dim)), key);
            ry += rowH;
        }
    }

    public void renderImGui() {
        if (!visibleThisFrame) return;
        visibleThisFrame = false;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        float dim = com.eclipseware.imnotcheatingyouare.client.clickgui.ImGuiClickGui.isOpen() || mc.gui.screen() != null ? 0.35f : 1f;

        float guiScale = (float) mc.getWindow().getGuiScale();
        float k = guiScale / xyz.breadloaf.imguimc.imgui.ImguiLoader.getUiScale() * lastScale;
        float displayW = imgui.ImGui.getIO().getDisplaySizeX();
        boolean right = lastAlignment.equals("Right");
        float dt = Math.min(0.1f, imgui.ImGui.getIO().getDeltaTime());
        float speed = Math.max(4f, lastAnimSpeed * 60f);

        List<Object[]> rows = new ArrayList<>();
        for (Module m : ImnotcheatingyouareClient.INSTANCE.moduleManager.modules) {
            if (m.isHidden()) continue;
            float a = imguiAnim.getOrDefault(m, 0f);
            a += ((m.isToggled() ? 1f : 0f) - a) * Math.min(1f, dt * speed);
            imguiAnim.put(m, a);
            if (a < 0.01f) {
                slotY.remove(m);
                continue;
            }
            String suffix = "";
            List<Setting> settings = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingsByMod(m);
            if (settings != null) {
                for (Setting st : settings) {
                    if (st.isCombo()) {
                        suffix = st.getValString();
                        break;
                    }
                }
            }
            imgui.ImGui.calcTextSize(textSize, m.getName());
            float nameW = textSize.x;
            float suffixW = 0f;
            if (!suffix.isEmpty()) {
                imgui.ImGui.calcTextSize(textSize, suffix);
                suffixW = textSize.x + 5f;
            }
            rows.add(new Object[]{m, suffix, nameW, suffixW, a});
        }
        rows.sort((o1, o2) -> Float.compare((float) o2[2] + (float) o2[3], (float) o1[2] + (float) o1[3]));

        imgui.ImDrawList dl = imgui.ImGui.getBackgroundDrawList();
        float fontSize = imgui.ImGui.getFontSize();
        float rowH = fontSize + 8f;
        float gap = 2f;
        float margin = 5f * guiScale / xyz.breadloaf.imguimc.imgui.ImguiLoader.getUiScale();
        float targetY = lastStartY * guiScale / xyz.breadloaf.imguimc.imgui.ImguiLoader.getUiScale();
        float time = (float) (System.nanoTime() / 1_000_000_000.0);

        float[] hsb = Color.RGBtoHSB(lastRenderR, lastRenderG, lastRenderB, null);
        int shader = com.eclipseware.imnotcheatingyouare.client.gui.PastelShaderBackground.render()
                ? com.eclipseware.imnotcheatingyouare.client.gui.PastelShaderBackground.glTextureId() : 0;
        float dispH = Math.max(1f, imgui.ImGui.getIO().getDisplaySizeY());

        {
            int acc = Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]);
            renderKeybinds(guiScale / xyz.breadloaf.imguimc.imgui.ImguiLoader.getUiScale(), displayW, right, (acc >> 16) & 0xFF, (acc >> 8) & 0xFF, acc & 0xFF, dim);
        }

        for (int i = 0; i < rows.size(); i++) {
            Object[] row = rows.get(i);
            Module m = (Module) row[0];
            String suffix = (String) row[1];
            float nameW = (float) row[2];
            float suffixW = (float) row[3];
            float a = (float) row[4];
            float e = AnimationUtil.easeOutCubic(a);
            a *= dim;

            float current = slotY.getOrDefault(m, targetY);
            current += (targetY - current) * Math.min(1f, dt * 16f);
            slotY.put(m, current);

            float w = nameW + suffixW + 16f;
            float h = rowH * e;
            float slide = (1f - e) * (w + 12f);
            float x = right ? displayW - margin - w + slide : margin - slide;
            float y = current;

            float hue = (hsb[0] + 0.06f * (float) Math.sin(time * 1.3f + i * 0.45f) + 1f) % 1f;
            int accentRgb = Color.HSBtoRGB(hue, Math.min(1f, hsb[1] * 0.85f), Math.min(1f, hsb[2] * 1.05f + 0.05f));
            int ar = (accentRgb >> 16) & 0xFF, ag = (accentRgb >> 8) & 0xFF, ab = accentRgb & 0xFF;
            int alpha = (int) (255 * a);

            boolean first = i == 0;
            boolean last = i == rows.size() - 1;
            int flags = right
                    ? (first ? imgui.flag.ImDrawFlags.RoundCornersTopLeft | imgui.flag.ImDrawFlags.RoundCornersBottomLeft : imgui.flag.ImDrawFlags.RoundCornersLeft)
                    : (first ? imgui.flag.ImDrawFlags.RoundCornersTopRight | imgui.flag.ImDrawFlags.RoundCornersBottomRight : imgui.flag.ImDrawFlags.RoundCornersRight);
            float round = 6f;

            dl.addRectFilled(x + (right ? -1f : 1f), y + 2f, x + w + (right ? -1f : 1f), y + h + 2f, col(0, 0, 0, (int) (55 * a)), round, flags);
            dl.addRectFilled(x, y, x + w, y + h, col(16, 12, 26, (int) (185 * a)), round, flags);
            if (shader != 0) {
                float dw = Math.max(1f, displayW);
                dl.addImageRounded(shader, x, y, x + w, y + h, x / dw, 1f - y / dispH, (x + w) / dw, 1f - (y + h) / dispH,
                        col(255, 255, 255, (int) (48 * a)), round, flags);
            }
            int glowIn = col(ar, ag, ab, (int) (70 * a));
            int glowOut = col(ar, ag, ab, 0);
            float glowW = Math.min(w * 0.5f, 26f);
            if (right) {
                dl.addRectFilledMultiColor(x + w - glowW, y, x + w, y + h, glowOut, glowIn, glowIn, glowOut);
                dl.addRectFilled(x + w - 2.5f, y, x + w, y + h, col(ar, ag, ab, alpha), 1f);
            } else {
                dl.addRectFilledMultiColor(x, y, x + glowW, y + h, glowIn, glowOut, glowOut, glowIn);
                dl.addRectFilled(x, y, x + 2.5f, y + h, col(ar, ag, ab, alpha), 1f);
            }

            if (e > 0.35f) {
                float ty = y + (h - fontSize) / 2f;
                float tx = x + 9f;
                dl.addText(tx + 0.8f, ty + 0.8f, col(0, 0, 0, (int) (120 * a)), m.getName());
                dl.addText(tx, ty, col(255, 255, 255, alpha), m.getName());
                if (!suffix.isEmpty()) {
                    dl.addText(tx + nameW + 5f, ty, col(200, 190, 225, alpha), suffix);
                }
            }

            targetY += (rowH + gap) * e;
        }
    }

    public void render(GuiGraphicsExtractor guiGraphics, float partialTick) {
        Module arrayListMod = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("ArrayList");
        if (arrayListMod == null || !arrayListMod.isToggled()) return;

        Module bypassMod = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("Bypass");
        if (bypassMod != null && bypassMod.isToggled()) return;

        boolean syncTheme = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(arrayListMod, "Sync Theme").getValBoolean();
        String alignment = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(arrayListMod, "Alignment").getValString();
        double startY = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(arrayListMod, "Y Offset").getValDouble();

        int logoWidth = 110;
        int logoHeight = 33;
        int logoX = alignment.equals("Right") ? (Minecraft.getInstance().getWindow().getGuiScaledWidth() - logoWidth - 5) : 5;
        guiGraphics.blit(LOGO_ID, logoX, 5, logoX + logoWidth, 5 + logoHeight, 0.0f, 1.0f, 0.0f, 1.0f);

        startY = Math.max(startY, (double) (logoHeight + 10));

        int r = 230, g = 10, b = 230;
        float animSpeed = 0.15f;

        Module theme = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("Theme");
        if (syncTheme && theme != null) {
            Setting accColorSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(theme, "Accent Color");
            if (accColorSetting != null && accColorSetting.isColor()) {
                int cVal = accColorSetting.getValColor();
                r = (cVal >> 16) & 0xFF;
                g = (cVal >> 8) & 0xFF;
                b = cVal & 0xFF;
            } else {
                Setting rS = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(theme, "Accent R");
                Setting gS = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(theme, "Accent G");
                Setting bS = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(theme, "Accent B");
                r = rS != null ? (int) rS.getValDouble() : 155;
                g = gS != null ? (int) gS.getValDouble() : 60;
                b = bS != null ? (int) bS.getValDouble() : 255;
            }
            Setting speedS = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(theme, "Anim Speed");
            animSpeed = speedS != null ? (float) speedS.getValDouble() * 0.03f : 0.15f;
        } else {
            Setting textColorSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(arrayListMod, "Text Color");
            if (textColorSetting != null && textColorSetting.isColor()) {
                int cVal = textColorSetting.getValColor();
                r = (cVal >> 16) & 0xFF;
                g = (cVal >> 8) & 0xFF;
                b = cVal & 0xFF;
            } else {
                Setting rS = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(arrayListMod, "Red");
                Setting gS = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(arrayListMod, "Green");
                Setting bS = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(arrayListMod, "Blue");
                r = rS != null ? (int) rS.getValDouble() : 230;
                g = gS != null ? (int) gS.getValDouble() : 10;
                b = bS != null ? (int) bS.getValDouble() : 230;
            }
        }
        
        List<ModRenderInfo> activeList = new ArrayList<>();
        
        for (Module m : ImnotcheatingyouareClient.INSTANCE.moduleManager.modules) {
            if (m.isHidden()) continue;
            
            float currentAnim = animMap.getOrDefault(m, 0f);
            float target = m.isToggled() ? 1f : 0f;
            currentAnim += (target - currentAnim) * animSpeed;
            animMap.put(m, currentAnim);
            
            if (currentAnim > 0.01f) {
                String displayName = getDisplayName(m);
                int textWidth = FontUtils.width(displayName);
                activeList.add(new ModRenderInfo(m, displayName, textWidth, currentAnim));
            }
        }

        activeList.sort((i1, i2) -> Integer.compare(i2.textWidth, i1.textWidth));

        double scaleSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(arrayListMod, "Scale").getValDouble();
        float scale = (float) scaleSetting;

        double currentY = startY;
        int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();

        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().scale(scale, scale);

        lastRenderR = r;
        lastRenderG = g;
        lastRenderB = b;
        lastAlignment = alignment;
        lastStartY = (float) startY;
        lastScale = scale;
        lastAnimSpeed = animSpeed;
        visibleThisFrame = true;

        guiGraphics.pose().popMatrix();
    }

    private String getKeyName(int key) {
        return com.eclipseware.imnotcheatingyouare.client.utils.InputUtil.getName(key);
    }
}
