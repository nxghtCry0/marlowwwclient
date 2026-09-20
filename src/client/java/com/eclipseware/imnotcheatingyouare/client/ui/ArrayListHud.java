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

        for (int i = 0; i < activeList.size(); i++) {
            ModRenderInfo info = activeList.get(i);
            float anim = info.anim;
            
            float slideAnim = AnimationUtil.easeOutBack(anim);
            float heightAnim = AnimationUtil.easeOutCubic(anim);
            
            String displayName = info.displayName;
            int textWidth = info.textWidth;
            int rectWidth = textWidth + 14;
            
            boolean isRight = alignment.equals("Right");
            double xOffset = isRight ? (1.0f - slideAnim) * 35f : (1.0f - slideAnim) * -35f;
            
            int drawX = isRight ? (int)(screenWidth - rectWidth * scale + xOffset * scale) : (int)(x + xOffset * scale);
            int drawY = (int) currentY;
            
            float scaledX = (float)(drawX / scale);
            float scaledY = (float)(drawY / scale);
            float scaledW = (float)rectWidth;
            float scaledH = 14.5f * heightAnim;
            
            int alpha = Math.max(0, Math.min(255, (int)(255 * anim)));
            int bgAlpha = Math.max(0, Math.min(255, (int)(160 * anim))); 
            
            int currentBg = (bgAlpha << 24) | 0x0F0F16;
            
            float[] hsb = Color.RGBtoHSB(r, g, b, null);
            float shiftedHue = (hsb[0] + 0.08f) % 1.0f;
            int complementary = Color.HSBtoRGB(shiftedHue, hsb[1], hsb[2]);
            double wave = Math.sin((System.currentTimeMillis() / 1200.0) + (currentY * 0.04)) * 0.5 + 0.5;
            int elementAccent = interpolateColor(Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]), complementary, (float) wave);
            int currentAccent = (alpha << 24) | (elementAccent & 0x00FFFFFF);
            int textColor = (alpha << 24) | 0xFFFFFF;

            Render2DEngine.activeContext = guiGraphics;
            
            Render2DEngine.drawRoundedRect(
                guiGraphics.pose(), 
                scaledX, 
                scaledY, 
                scaledW, 
                scaledH, 
                4.0f, 
                new java.awt.Color(currentBg, true)
            );
            
            int outlineColor = (alpha << 24) | 0x22222E;
            Render2DEngine.drawRoundedOutline(
                guiGraphics.pose(),
                scaledX,
                scaledY,
                scaledW,
                scaledH,
                4.0f,
                0.8f,
                new java.awt.Color(outlineColor, true)
            );

            float textY = scaledY + (scaledH - 8.0f) / 2.0f - 0.5f;

            if (isRight) {
                Render2DEngine.drawRoundedRect(
                    guiGraphics.pose(), 
                    scaledX + scaledW - 2.5f, 
                    scaledY, 
                    2.5f, 
                    scaledH, 
                    1.0f, 
                    new java.awt.Color(currentAccent, true)
                );
                FontUtils.drawString(guiGraphics, displayName, (int)(scaledX + 5.0f), (int)textY, textColor, true);
            } else {
                Render2DEngine.drawRoundedRect(
                    guiGraphics.pose(), 
                    scaledX, 
                    scaledY, 
                    2.5f, 
                    scaledH, 
                    1.0f, 
                    new java.awt.Color(currentAccent, true)
                );
                FontUtils.drawString(guiGraphics, displayName, (int)(scaledX + 7.5f), (int)textY, textColor, true);
            }

            currentY += 17.5f * anim * scale;
        }

        Module keybindListMod = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("KeybindList");
        if (keybindListMod != null && keybindListMod.isToggled()) {
            boolean isKBRight = !alignment.equals("Right");
            
            Setting kbScaleSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(keybindListMod, "Scale");
            float customScale = kbScaleSetting != null ? (float) kbScaleSetting.getValDouble() : 0.75f;
            
            Setting onlyEnabledSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(keybindListMod, "Only Enabled");
            boolean onlyEnabled = onlyEnabledSetting != null && onlyEnabledSetting.getValBoolean();
            
            List<Module> keyboundModules = new ArrayList<>();
            List<String> mNames = new ArrayList<>();
            List<String> mKeys = new ArrayList<>();
            int maxItemWidth = FontUtils.width("Keybinds") + 20;
            
            for (Module m : ImnotcheatingyouareClient.INSTANCE.moduleManager.modules) {
                if (m.isHidden()) continue;
                if (onlyEnabled && !m.isToggled()) continue;
                int key = m.getKeyBind();
                if (key != -1 && !getKeyName(key).equals("NONE")) {
                    keyboundModules.add(m);
                    String nameStr = m.getName();
                    String keyStr = getKeyName(key);
                    mNames.add(nameStr);
                    mKeys.add(keyStr);
                    int itemWidth = FontUtils.width(nameStr) + FontUtils.width(keyStr) + 24;
                    if (itemWidth > maxItemWidth) {
                        maxItemWidth = itemWidth;
                    }
                }
            }
            
            if (!mNames.isEmpty()) {
                guiGraphics.pose().pushMatrix();

                float kbScale = scale * customScale;
                guiGraphics.pose().scale(customScale, customScale);
                
                float rowHeight = 14.0f;
                float headerHeight = 20.0f;
                
                float screenH = (float) Minecraft.getInstance().getWindow().getGuiScaledHeight();
                float maxScreenHeightAllowed = (screenH / kbScale) * 0.45f;
                int maxItemsPerCol = Math.max(5, (int)((maxScreenHeightAllowed - headerHeight - 6.0f) / rowHeight));
                
                int numCols = (int) Math.ceil((double) mNames.size() / maxItemsPerCol);
                int itemsInFirstCol = Math.min(mNames.size(), maxItemsPerCol);
                
                float singleColWidth = maxItemWidth + 12;
                float cardWidth = (singleColWidth * numCols) + ((numCols - 1) * 8) + 16;
                float cardHeight = headerHeight + (itemsInFirstCol * rowHeight) + 6.0f;
                
                int drawX = isKBRight ? (int)(screenWidth - cardWidth * kbScale - 10) : 10;
                int drawY = 10;
                
                float scaledX = (float)(drawX / kbScale);
                float scaledY = (float)(drawY / kbScale);
                float scaledW = cardWidth;
                float scaledH = cardHeight;
                
                Render2DEngine.activeContext = guiGraphics;

                int bgAlpha = 225;
                int currentBg = (bgAlpha << 24) | 0x0C0C12;
                Render2DEngine.drawRoundedRect(
                    guiGraphics.pose(), 
                    scaledX, 
                    scaledY, 
                    scaledW, 
                    scaledH, 
                    4.5f, 
                    new java.awt.Color(currentBg, true)
                );
                
                int outlineColor = 0x22FFFFFF;
                Render2DEngine.drawRoundedOutline(
                    guiGraphics.pose(),
                    scaledX,
                    scaledY,
                    scaledW,
                    scaledH,
                    4.5f,
                    0.8f,
                    new java.awt.Color(outlineColor, true)
                );

                int alpha = 255;
                float[] hsb = Color.RGBtoHSB(r, g, b, null);
                float shiftedHue = (hsb[0] + 0.08f) % 1.0f;
                int complementary = Color.HSBtoRGB(shiftedHue, hsb[1], hsb[2]);
                double wave = Math.sin((System.currentTimeMillis() / 1200.0) + (drawY * 0.04)) * 0.5 + 0.5;
                int elementAccent = interpolateColor(Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]), complementary, (float) wave);
                int currentAccent = (alpha << 24) | (elementAccent & 0x00FFFFFF);

                Render2DEngine.drawRoundedRect(
                    guiGraphics.pose(), 
                    scaledX, 
                    scaledY, 
                    scaledW, 
                    1.5f, 
                    1.0f, 
                    new java.awt.Color(currentAccent, true)
                );

                FontUtils.drawString(guiGraphics, "Keybinds", (int)(scaledX + 8), (int)(scaledY + 5), currentAccent, true);

                String countStr = String.valueOf(mNames.size());
                int countW = FontUtils.width(countStr);
                FontUtils.drawString(guiGraphics, "\u00a77" + countStr, (int)(scaledX + scaledW - countW - 8), (int)(scaledY + 5), 0x99FFFFFF, true);

                int dividerColor = (alpha << 24) | 0x1A1A24;
                guiGraphics.fill((int)(scaledX + 6), (int)(scaledY + 18.5f), (int)(scaledX + scaledW - 6), (int)(scaledY + 19.5f), dividerColor);

                for (int i = 0; i < mNames.size(); i++) {
                    int col = i / maxItemsPerCol;
                    int row = i % maxItemsPerCol;
                    
                    float colOffsetX = col * (singleColWidth + 8);
                    float itemX = scaledX + 8 + colOffsetX;
                    float itemY = scaledY + 23.0f + (row * rowHeight);

                    Module m = keyboundModules.get(i);
                    String name = mNames.get(i);
                    String key = mKeys.get(i);
                    boolean toggled = m.isToggled();

                    int textColor = toggled ? 0xFFFFFFFF : 0x88AAAAAA;
                    FontUtils.drawString(guiGraphics, name, (int)itemX, (int)itemY, textColor, true);

                    int keyTextW = FontUtils.width(key);
                    float keyCapW = keyTextW + 8.0f;
                    float keyCapH = 11.0f;
                    float keyCapX = itemX + singleColWidth - keyCapW - 4.0f;
                    float keyCapY = itemY - 1.0f;

                    int keyBg = toggled ? (35 << 24) | 0xFFFFFF : (15 << 24) | 0xFFFFFF;
                    int keyBorder = toggled ? (currentAccent & 0x55FFFFFF) : 0x20FFFFFF;
                    
                    Render2DEngine.drawRoundedRect(guiGraphics.pose(), keyCapX, keyCapY, keyCapW, keyCapH, 3.0f, new java.awt.Color(keyBg, true));
                    Render2DEngine.drawRoundedOutline(guiGraphics.pose(), keyCapX, keyCapY, keyCapW, keyCapH, 3.0f, 0.8f, new java.awt.Color(keyBorder, true));

                    int keyTextColor = toggled ? 0xFFFFFFFF : 0x99FFFFFF;
                    FontUtils.drawString(guiGraphics, key, (int)(keyCapX + 4.0f), (int)itemY, keyTextColor, true);
                }

                guiGraphics.pose().popMatrix();
            }
        }

        guiGraphics.pose().popMatrix();
    }

    private String getKeyName(int key) {
        return com.eclipseware.imnotcheatingyouare.client.utils.InputUtil.getName(key);
    }
}
