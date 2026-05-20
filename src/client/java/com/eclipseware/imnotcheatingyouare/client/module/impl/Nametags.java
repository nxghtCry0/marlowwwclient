package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.utils.AnimationUtil;
import com.eclipseware.imnotcheatingyouare.client.utils.FontUtils;
import com.eclipseware.imnotcheatingyouare.client.utils.RenderUtils;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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
    }

    private static final Vector3d projVec = new Vector3d();

    @Override
    public void onRenderHUD(GuiGraphicsExtractor guiGraphics, Object tickDeltaObj) {
        if (!isToggled() || mc.player == null || mc.level == null) return;

        float partialTick = getTickDelta(tickDeltaObj);

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

            double ex = net.minecraft.util.Mth.lerp(partialTick, entity.xo, entity.getX());
            double ey = net.minecraft.util.Mth.lerp(partialTick, entity.yo, entity.getY()) + entity.getBbHeight() + 0.4;
            double ez = net.minecraft.util.Mth.lerp(partialTick, entity.zo, entity.getZ());

            if (!RenderUtils.project2D(ex, ey, ez, partialTick, projVec)) continue;
            if (projVec.z <= 0 || projVec.z >= 1.0) continue;
            Vector3d proj = projVec;

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
                    int mainX = (int)proj.x - (offHand.isEmpty() ? 8 : 16);
                    guiGraphics.item(mainHand, mainX, itemY - 2);
                    if (mainHand.getCount() > 1) {
                        FontUtils.drawString(guiGraphics, String.valueOf(mainHand.getCount()), mainX + 8, itemY + 6, -1, true);
                    }
                }
                if (!offHand.isEmpty()) {
                    int offX = (int)proj.x + (mainHand.isEmpty() ? -8 : 2);
                    guiGraphics.item(offHand, offX, itemY - 2);
                    if (offHand.getCount() > 1) {
                        FontUtils.drawString(guiGraphics, String.valueOf(offHand.getCount()), offX + 8, itemY + 6, -1, true);
                    }
                }
            }
        }
    }

    private float getTickDelta(Object tickDeltaObj) {
        if (tickDeltaObj instanceof Float) return (Float) tickDeltaObj;
        for (java.lang.reflect.Method m : tickDeltaObj.getClass().getMethods()) {
            if (m.getReturnType() == float.class) {
                if (m.getParameterCount() == 1 && m.getParameterTypes()[0] == boolean.class) {
                    try { return (float) m.invoke(tickDeltaObj, true); } catch (Exception e) {}
                } else if (m.getParameterCount() == 0) {
                    String name = m.getName().toLowerCase();
                    if (name.contains("tick") || name.contains("delta") || name.contains("frame")) {
                        try { return (float) m.invoke(tickDeltaObj); } catch (Exception e) {}
                    }
                }
            }
        }
        return 1.0f;
    }
}