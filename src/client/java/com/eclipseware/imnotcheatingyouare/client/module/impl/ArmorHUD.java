package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.utils.FontUtils;
import com.eclipseware.imnotcheatingyouare.client.utils.RenderUtils;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import java.awt.Color;
import java.util.ArrayList;

public class ArmorHUD extends Module {
    public ArmorHUD() {
        super("ArmorHUD", Category.HUD, "Displays your equipped armor and durability status.");
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("X", this, 10.0, 0.0, 2000.0, true));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Y", this, 300.0, 0.0, 2000.0, true));
        
        ArrayList<String> modes = new ArrayList<>();
        modes.add("Durability");
        modes.add("Percent");
        modes.add("Bar");
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Display Mode", this, "Percent", modes));
        
        ArrayList<String> layouts = new ArrayList<>();
        layouts.add("Horizontal");
        layouts.add("Vertical");
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Layout", this, "Horizontal", layouts));
    }

    @Override
    public void onRenderHUD(GuiGraphicsExtractor guiGraphics, Object tickDelta) {
        boolean inEditor = mc.screen instanceof com.eclipseware.imnotcheatingyouare.client.clickgui.HudEditorScreen;
        if (!isToggled() && !inEditor) return;
        if (mc.player == null) return;

        double xVal = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "X").getValDouble();
        double yVal = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Y").getValDouble();
        String mode = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Display Mode").getValString();
        String layout = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Layout").getValString();

        int x = (int) xVal;
        int y = (int) yVal;

        boolean isVertical = layout.equals("Vertical");

        EquipmentSlot[] slots = {
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
        };

        for (int i = 0; i < slots.length; i++) {
            ItemStack stack = mc.player.getItemBySlot(slots[i]);
            if (stack == null || stack.isEmpty()) continue;

            int drawX = x;
            int drawY = y;

            if (isVertical) {
                y += 28;
            } else {
                x += 24;
            }

            guiGraphics.item(stack, drawX, drawY);

            if (stack.isDamageableItem()) {
                int max = stack.getMaxDamage();
                int current = max - stack.getDamageValue();
                float pct = (float) current / max;
                Color color = RenderUtils.getHealthColor(pct);

                if (mode.equals("Durability")) {
                    String str = String.valueOf(current);
                    int textW = FontUtils.width(str);
                    FontUtils.drawString(guiGraphics, str, drawX + 8 - textW / 2, drawY + 17, color.getRGB(), true);
                } else if (mode.equals("Percent")) {
                    String str = (int) (pct * 100) + "%";
                    int textW = FontUtils.width(str);
                    FontUtils.drawString(guiGraphics, str, drawX + 8 - textW / 2, drawY + 17, color.getRGB(), true);
                } else if (mode.equals("Bar")) {
                    int barBg = 0x66000000;
                    int barColor = color.getRGB();
                    guiGraphics.fill(drawX, drawY + 17, drawX + 16, drawY + 19, barBg);
                    guiGraphics.fill(drawX, drawY + 17, drawX + (int)(16 * pct), drawY + 19, barColor);
                }
            }
        }
    }
}
