package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.utils.AnimationUtil;
import com.eclipseware.imnotcheatingyouare.client.utils.FontUtils;
import com.eclipseware.imnotcheatingyouare.client.utils.RenderUtils;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3d;

import java.awt.Color;

public class Nametags extends Module {

    public Nametags() {
        super("Nametags", Category.Render);
        HudRenderCallback.EVENT.register((guiGraphics, tickDelta) -> onHudRender(guiGraphics));
    }

    private void onHudRender(GuiGraphics guiGraphics) {
        if (!isToggled() || mc.player == null || mc.level == null) return;

        Setting playersSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Players");
        boolean showPlayers = playersSetting == null || playersSetting.getValBoolean();

        Setting mobsSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Show Mobs");
        boolean showMobs = mobsSetting != null && mobsSetting.getValBoolean();

        Color themeColor = RenderUtils.getThemeAccentColor();

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity == mc.player || !entity.isAlive()) continue;

            boolean isPlayer = entity instanceof Player;
            boolean isMob = entity instanceof Mob;
            if (!(isPlayer && showPlayers) && !(isMob && showMobs)) continue;

            double dist = mc.player.distanceTo(entity);
            if (dist > 64.0) continue;

            Vector3d proj = RenderUtils.project2D(
                entity.getX(), entity.getY() + entity.getBbHeight() + 0.4, entity.getZ(), 1.0f);
            if (proj == null || proj.z <= 0 || proj.z >= 1.0) continue;

            float alpha = Math.max(0.3f, 1.0f - (float)(dist / 64.0));
            int bgAlpha = (int)(alpha * 180);
            int textAlpha = (int)(alpha * 255);

            String name = entity.getName().getString();
            String hpStr = "";
            if (entity instanceof LivingEntity living) {
                hpStr = " " + (int) Math.ceil(living.getHealth()) + "HP";
            }
            String distStr = " " + (Math.round(dist * 10.0) / 10.0) + "m";

            int nameWidth = FontUtils.width(name);
            int hpWidth = FontUtils.width(hpStr);
            int distWidth = FontUtils.width(distStr);
            int totalWidth = nameWidth + hpWidth + distWidth;

            int drawX = (int) proj.x - totalWidth / 2;
            int drawY = (int) proj.y - 10;

            AnimationUtil.drawRoundedRect(guiGraphics, drawX - 3, drawY - 2, totalWidth + 6, 14, 3,
                new Color(10, 10, 10, bgAlpha).getRGB());

            FontUtils.drawString(guiGraphics, name, drawX, drawY,
                new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), textAlpha).getRGB(), false);
            if (!hpStr.isEmpty()) {
                Color hpColor = entity instanceof LivingEntity l ? RenderUtils.getHealthColor(l.getHealth() / l.getMaxHealth()) : Color.WHITE;
                FontUtils.drawString(guiGraphics, hpStr, drawX + nameWidth, drawY,
                    new Color(hpColor.getRed(), hpColor.getGreen(), hpColor.getBlue(), textAlpha).getRGB(), false);
            }
            FontUtils.drawString(guiGraphics, distStr, drawX + nameWidth + hpWidth, drawY,
                new Color(180, 180, 180, textAlpha).getRGB(), false);

            if (entity instanceof LivingEntity living) {
                ItemStack mainHand = living.getMainHandItem();
                ItemStack offHand = living.getOffhandItem();
                int itemY = drawY - (mainHand.isEmpty() && offHand.isEmpty() ? 0 : 18);

                if (!mainHand.isEmpty()) {
                    guiGraphics.renderItem(mainHand, (int)proj.x - (offHand.isEmpty() ? 8 : 16), itemY - 2);
                }
                if (!offHand.isEmpty()) {
                    guiGraphics.renderItem(offHand, (int)proj.x + (mainHand.isEmpty() ? -8 : 2), itemY - 2);
                }
            }
        }
    }
}