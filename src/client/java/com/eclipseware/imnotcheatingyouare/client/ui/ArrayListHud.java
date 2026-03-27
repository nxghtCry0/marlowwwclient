package com.eclipseware.imnotcheatingyouare.client.ui;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.awt.Color;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ArrayListHud {
    public static ArrayListHud INSTANCE = new ArrayListHud();
    
    public double x = 2, y = 2;
    public boolean dragging = false;
    public double dragX = 0, dragY = 0;

    public void render(GuiGraphics guiGraphics, float partialTick) {
Module arrayListMod = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("ArrayList");
if (arrayListMod == null || !arrayListMod.isToggled()) return;

    com.eclipseware.imnotcheatingyouare.client.setting.Setting alignSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(arrayListMod, "Alignment");
    com.eclipseware.imnotcheatingyouare.client.setting.Setting rSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(arrayListMod, "Red");
    com.eclipseware.imnotcheatingyouare.client.setting.Setting gSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(arrayListMod, "Green");
    com.eclipseware.imnotcheatingyouare.client.setting.Setting bSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(arrayListMod, "Blue");

    boolean rightAlign = alignSetting != null && alignSetting.getValString().equals("Right");
    int r = rSetting != null ? (int) rSetting.getValDouble() : 230;
    int g = gSetting != null ? (int) gSetting.getValDouble() : 10;
    int b = bSetting != null ? (int) bSetting.getValDouble() : 230;
    Color accentColor = new Color(r, g, b, 255);

    Minecraft mc = Minecraft.getInstance();
    float currentY = (float) y;

    // 1. Sort and hide the "ArrayList" module itself
    List<Module> sorted = ImnotcheatingyouareClient.INSTANCE.moduleManager.modules.stream()
        .filter(m -> !m.getName().equalsIgnoreCase("ArrayList"))
        .sorted(Comparator.comparingInt(m -> -mc.font.width(m.getName())))
        .collect(Collectors.toList());

    // 2. Find max width for right alignment anchoring
    int maxWidth = 0;
    for (Module m : sorted) {
        if (m.isToggled() || m.arrayListAnim > 0.01f) {
            maxWidth = Math.max(maxWidth, mc.font.width(m.getName()));
        }
    }

    for (Module m : sorted) {
        float target = m.isToggled() ? 1.0f : 0.0f;
        m.arrayListAnim += (target - m.arrayListAnim) * 0.15f;

        if (m.arrayListAnim < 0.01f) continue;

        int width = mc.font.width(m.getName());
        float offsetX = (1.0f - m.arrayListAnim) * (rightAlign ? 20f : -20f); 

        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate((float)x + offsetX, currentY);
        guiGraphics.pose().scale(m.arrayListAnim, m.arrayListAnim); 

        if (rightAlign) {
            // Align text and accent line to the right edge of the invisible bounding box
            guiGraphics.fill(maxWidth - width, 0, maxWidth + 5, 12, new Color(20, 20, 20, 180).getRGB());
            guiGraphics.fill(maxWidth + 4, 0, maxWidth + 5, 12, accentColor.getRGB()); 
            guiGraphics.drawString(mc.font, m.getName(), maxWidth - width + 2, 2, accentColor.getRGB(), false);
        } else {
            // Align to the left edge
            guiGraphics.fill(0, 0, width + 5, 12, new Color(20, 20, 20, 180).getRGB());
            guiGraphics.fill(0, 0, 1, 12, accentColor.getRGB()); 
            guiGraphics.drawString(mc.font, m.getName(), 3, 2, accentColor.getRGB(), false);
        }
        
        guiGraphics.pose().popMatrix();

        currentY += (12 * m.arrayListAnim);
    }
}
}