package com.eclipseware.imnotcheatingyouare.client.ui;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.utils.FontUtils;
import com.eclipseware.imnotcheatingyouare.client.utils.AnimationUtil;
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
            r = (int) ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(theme, "Accent R").getValDouble();
            g = (int) ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(theme, "Accent G").getValDouble();
            b = (int) ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(theme, "Accent B").getValDouble();
            animSpeed = (float) ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(theme, "Anim Speed").getValDouble() * 0.03f;
        } else {
            r = (int) ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(arrayListMod, "Red").getValDouble();
            g = (int) ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(arrayListMod, "Green").getValDouble();
            b = (int) ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(arrayListMod, "Blue").getValDouble();
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

        double currentY = startY;
        int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();

        for (int i = 0; i < activeList.size(); i++) {
            ModRenderInfo info = activeList.get(i);
            float anim = info.anim;
            String displayName = info.displayName;
            int textWidth = info.textWidth;
            int rectWidth = textWidth + 15;
            
            boolean isRight = alignment.equals("Right");
            double xOffset = isRight ? (1.0f - anim) * 30f : (1.0f - anim) * -30f;
            
            int drawX = isRight ? (int)(screenWidth - rectWidth + xOffset) : (int)(x + xOffset);
            int drawY = (int) currentY;
            int rectHeight = 17;
            
            int alpha = Math.max(0, Math.min(255, (int)(255 * anim)));
            int bgAlpha = Math.max(0, Math.min(255, (int)(90 * anim))); 
            
            int currentBg = (bgAlpha << 24) | 0x08080C;
            
            float[] hsb = Color.RGBtoHSB(r, g, b, null);
            float shiftedHue = (hsb[0] + 0.08f) % 1.0f;
            int complementary = Color.HSBtoRGB(shiftedHue, hsb[1], hsb[2]);
            double wave = Math.sin((System.currentTimeMillis() / 1200.0) + (currentY * 0.04)) * 0.5 + 0.5;
            int elementAccent = interpolateColor(Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]), complementary, (float) wave);
            int currentAccent = (alpha << 24) | (elementAccent & 0x00FFFFFF);
            int textColor = (alpha << 24) | 0xFFFFFF;

            com.eclipseware.imnotcheatingyouare.client.utils.remnant.Render2DEngine.activeContext = (net.minecraft.client.gui.GuiGraphicsExtractor)guiGraphics;
            org.joml.Matrix4f matrix = new org.joml.Matrix4f(
                guiGraphics.pose().m00, guiGraphics.pose().m01, 0.0f, 0.0f,
                guiGraphics.pose().m10, guiGraphics.pose().m11, 0.0f, 0.0f,
                0.0f, 0.0f, 1.0f, 0.0f,
                guiGraphics.pose().m20, guiGraphics.pose().m21, 0.0f, 1.0f
            );
            com.eclipseware.imnotcheatingyouare.client.utils.ShaderManager.drawRoundedRect(matrix, drawX, drawY + 1, rectWidth, 14, 5.0f, currentBg);

            if (isRight) {
                com.eclipseware.imnotcheatingyouare.client.utils.ShaderManager.drawRoundedRect(matrix, drawX + rectWidth - 5, drawY + 3, 2, 10, 1.0f, currentAccent);
                FontUtils.drawString(guiGraphics, displayName, drawX + 6, (int)(drawY + 3.5f), textColor, true);
            } else {
                com.eclipseware.imnotcheatingyouare.client.utils.ShaderManager.drawRoundedRect(matrix, drawX + 3, drawY + 3, 2, 10, 1.0f, currentAccent);
                FontUtils.drawString(guiGraphics, displayName, drawX + 9, (int)(drawY + 3.5f), textColor, true);
            }

            currentY += rectHeight * anim;
        }
    }
}
