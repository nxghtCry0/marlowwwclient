package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.utils.FontUtils;
import com.eclipseware.imnotcheatingyouare.client.utils.RenderUtils;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import org.joml.Vector3d;

import java.awt.Color;
import java.util.List;

public class ESP extends Module {
    private static final record CachedEntity(LivingEntity entity, Color color, double dist) {}
    private final List<CachedEntity> cache = new java.util.ArrayList<>();
    private int lastUpdateTick = -999;

    public ESP() {
        super("ESP", Category.Render);
        HudRenderCallback.EVENT.register((guiGraphics, tickDelta) -> onHudRender(guiGraphics, tickDelta));
    }

    private void onHudRender(GuiGraphics guiGraphics, Object tickDeltaObj) {
        if (!isToggled() || mc.player == null || mc.level == null) {
            cache.clear();
            return;
        }

        float partialTick = getTickDelta(tickDeltaObj);

        Setting modeSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Mode");
        String mode = modeSetting != null ? modeSetting.getValString() : "2D";
        if (mode.equals("Glow")) return;

        Setting mobsSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Show Mobs");
        boolean showMobs = mobsSetting != null && mobsSetting.getValBoolean();
        Setting showFill = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Fill");
        boolean doFill = showFill == null || showFill.getValBoolean();
        Setting healthSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Health");
        boolean showHealth = healthSetting == null || healthSetting.getValBoolean();
        Setting namesSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Names");
        boolean showNames = namesSetting == null || namesSetting.getValBoolean();

        if (mc.player.tickCount - lastUpdateTick >= 2) {
            lastUpdateTick = mc.player.tickCount;
            cache.clear();
            Color themeColor = RenderUtils.getThemeAccentColor();
            for (Entity entity : mc.level.entitiesForRendering()) {
                if (entity == mc.player || !(entity instanceof LivingEntity le) || !le.isAlive()) continue;
                double dist = mc.player.distanceTo(entity);
                if (dist > 64.0) continue;
                boolean isPlayer = entity instanceof Player;
                boolean isMob = entity instanceof Mob;
                if (!isPlayer && !(isMob && showMobs)) continue;
                cache.add(new CachedEntity(le, isPlayer ? themeColor : new Color(255, 85, 85), dist));
            }
            cache.sort((a, b) -> Double.compare(a.dist, b.dist));
        }

        for (CachedEntity ce : cache) {
            LivingEntity entity = ce.entity;
            Color color = ce.color;
            double dist = ce.dist;
            float hw = entity.getBbWidth() / 2.0f;
            float h = entity.getBbHeight();
            
            double x = net.minecraft.util.Mth.lerp((double)partialTick, entity.xo, entity.getX());
            double y = net.minecraft.util.Mth.lerp((double)partialTick, entity.yo, entity.getY());
            double z = net.minecraft.util.Mth.lerp((double)partialTick, entity.zo, entity.getZ());

            Vector3d center = RenderUtils.project2D(x, y + h * 0.5, z, partialTick);
            if (center == null) continue;
            Vector3d edge = RenderUtils.project2D(x + hw, y + h, z + hw, partialTick);
            if (edge == null) continue;

            double halfW = Math.abs(edge.x - center.x);
            double halfH = Math.abs(edge.y - center.y);
            double minX = center.x - halfW - 1.5;
            double minY = center.y - halfH - 1.5;
            double maxX = center.x + halfW + 1.5;
            double maxY = center.y + halfH + 1.5;
            float alpha = Math.max(0.3f, 1.0f - (float)(dist / 64.0));
            Color outlineColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), (int)(alpha * 255));
            Color fillColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), (int)(alpha * 35));

            if (doFill) guiGraphics.fill((int)minX + 1, (int)minY + 1, (int)maxX - 1, (int)maxY - 1, fillColor.getRGB());
            RenderUtils.drawCornerMarks(guiGraphics, minX, minY, maxX, maxY, outlineColor);

            if (showHealth) drawHealthBar(guiGraphics, minX, minY, maxY, entity.getHealth(), entity.getMaxHealth(), alpha);
            if (showNames) {
                String name = entity.getName().getString();
                double d = Math.round(dist * 10.0) / 10.0;
                int textWidth = FontUtils.width(name + " " + d + "m");
                int textX = (int)(minX + (maxX - minX) / 2 - textWidth / 2);
                int textY = (int)minY - 12;
                guiGraphics.fill(textX - 2, textY - 1, textX + textWidth + 2, textY + 10, new Color(0, 0, 0, (int)(alpha * 150)).getRGB());
                FontUtils.drawString(guiGraphics, name, textX, textY, new Color(color.getRed(), color.getGreen(), color.getBlue(), (int)(alpha * 255)).getRGB(), false);
                FontUtils.drawString(guiGraphics, " " + d + "m", textX + FontUtils.width(name), textY, new Color(200, 200, 200, (int)(alpha * 255)).getRGB(), false);
            }
        }
    }

    private void drawHealthBar(GuiGraphics graphics, double boxMinX, double boxMinY, double boxMaxY, float health, float maxHealth, float alpha) {
        float pct = Math.min(1.0f, Math.max(0.0f, health / maxHealth));
        int barHeight = (int)((boxMaxY - boxMinY - 2) * pct);
        Color hpColor = RenderUtils.getHealthColor(pct);

        int barX = (int)boxMinX - 4;
        int barW = 3;

        graphics.fill(barX, (int)boxMinY + 1, barX + barW, (int)boxMaxY - 1, new Color(0, 0, 0, (int)(alpha * 140)).getRGB());
        graphics.fill(barX, (int)boxMaxY - 1 - barHeight, barX + barW, (int)boxMaxY - 1, new Color(hpColor.getRed(), hpColor.getGreen(), hpColor.getBlue(), (int)(alpha * 255)).getRGB());
    }

    public boolean shouldGlow() {
        if (!isToggled()) return false;
        Setting modeSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Mode");
        String mode = modeSetting != null ? modeSetting.getValString() : "2D";
        return mode.equals("Glow") || mode.equals("Both");
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