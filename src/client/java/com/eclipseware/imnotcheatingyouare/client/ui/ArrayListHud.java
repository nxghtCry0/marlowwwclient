package com.eclipseware.imnotcheatingyouare.client.ui;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.utils.FontUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ArrayListHud {
    public static final ArrayListHud INSTANCE = new ArrayListHud();
    
    public double x = 5;
    public double y = 5;

    private final Map<Module, Float> animMap = new HashMap<>();

    public void render(GuiGraphics guiGraphics, float partialTick) {
        Module arrayListMod = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("ArrayList");
        if (arrayListMod == null || !arrayListMod.isToggled()) return;

        boolean syncTheme = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(arrayListMod, "Sync Theme").getValBoolean();
        String alignment = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(arrayListMod, "Alignment").getValString();
        double startY = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(arrayListMod, "Y Offset").getValDouble();

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
        
        List<Module> activeMods = new ArrayList<>();
        
        for (Module m : ImnotcheatingyouareClient.INSTANCE.moduleManager.modules) {
            if (m.isHidden()) continue;
            
            float currentAnim = animMap.getOrDefault(m, 0f);
            float target = m.isToggled() ? 1f : 0f;
            currentAnim += (target - currentAnim) * animSpeed;
            animMap.put(m, currentAnim);
            
            if (currentAnim > 0.01f) {
                activeMods.add(m);
            }
        }

        activeMods.sort(Comparator.comparingInt(m -> -FontUtils.width(m.getName())));

        double currentY = startY;
        int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();

        for (int i = 0; i < activeMods.size(); i++) {
            Module m = activeMods.get(i);
            float anim = animMap.get(m);
            String name = m.getName();
            int textWidth = FontUtils.width(name);
            int rectWidth = textWidth + 8;
            
            boolean isRight = alignment.equals("Right");
            double xOffset = isRight ? (1.0f - anim) * 30f : (1.0f - anim) * -30f;
            
            int drawX = isRight ? (int)(screenWidth - rectWidth + xOffset) : (int)(x + xOffset);
            
            int drawY = (int) currentY;
            int nextY = (int) (currentY + 14 * anim);
            int rectHeight = nextY - drawY;
            
            if (rectHeight <= 0) continue;
            
            int alpha = Math.max(0, Math.min(255, (int)(255 * anim)));
            int bgAlpha = Math.max(0, Math.min(255, (int)(140 * anim))); 
            int currentBg = new Color(15, 15, 15, bgAlpha).getRGB();
            int currentAccent = new Color(r, g, b, alpha).getRGB();
            int textColor = new Color(255, 255, 255, alpha).getRGB();

            int bgTopY = (i == 0) ? drawY + 1 : drawY;
            int bgBottomY = (i == activeMods.size() - 1) ? drawY + rectHeight - 1 : drawY + rectHeight;

            if (isRight) {
                guiGraphics.fill(drawX + 1, bgTopY, drawX + rectWidth - 2, bgBottomY, currentBg);
                
                if (i == 0) guiGraphics.fill(drawX, drawY, drawX + rectWidth, drawY + 1, currentAccent);
                
                guiGraphics.fill(drawX + rectWidth - 2, bgTopY, drawX + rectWidth, bgBottomY, currentAccent);
                
                guiGraphics.fill(drawX, bgTopY, drawX + 1, drawY + rectHeight - 1, currentAccent);
                
                if (i < activeMods.size() - 1) {
                    Module nextM = activeMods.get(i + 1);
                    float nextAnim = animMap.get(nextM);
                    int nextRectWidth = FontUtils.width(nextM.getName()) + 8;
                    int nextDrawX = (int)(screenWidth - nextRectWidth + (1.0f - nextAnim) * 30f);
                    
                    if (drawX != nextDrawX) {
                        int minX = Math.min(drawX, nextDrawX);
                        int maxX = Math.max(drawX, nextDrawX);
                        guiGraphics.fill(minX, drawY + rectHeight - 1, maxX + 1, drawY + rectHeight, currentAccent);
                    }
                } else {
                    guiGraphics.fill(drawX, drawY + rectHeight - 1, drawX + rectWidth, drawY + rectHeight, currentAccent);
                }
            } else {
                guiGraphics.fill(drawX + 2, bgTopY, drawX + rectWidth - 1, bgBottomY, currentBg);
                
                if (i == 0) guiGraphics.fill(drawX, drawY, drawX + rectWidth, drawY + 1, currentAccent);
                
                guiGraphics.fill(drawX, bgTopY, drawX + 2, bgBottomY, currentAccent);
                
                guiGraphics.fill(drawX + rectWidth - 1, bgTopY, drawX + rectWidth, drawY + rectHeight - 1, currentAccent);
                
                if (i < activeMods.size() - 1) {
                    Module nextM = activeMods.get(i + 1);
                    float nextAnim = animMap.get(nextM);
                    int nextRectWidth = FontUtils.width(nextM.getName()) + 8;
                    int nextDrawX = (int)(x + (1.0f - nextAnim) * -30f);
                    
                    int thisRight = drawX + rectWidth;
                    int nextRight = nextDrawX + nextRectWidth;
                    
                    if (thisRight != nextRight) {
                        int minX = Math.min(thisRight, nextRight);
                        int maxX = Math.max(thisRight, nextRight);
                        guiGraphics.fill(minX - 1, drawY + rectHeight - 1, maxX, drawY + rectHeight, currentAccent);
                    }
                } else {
                    guiGraphics.fill(drawX, drawY + rectHeight - 1, drawX + rectWidth, drawY + rectHeight, currentAccent);
                }
            }
            
            FontUtils.drawString(guiGraphics, name, drawX + 4, drawY + 3, textColor, true);

            currentY += 14 * anim;
        }
    }
}