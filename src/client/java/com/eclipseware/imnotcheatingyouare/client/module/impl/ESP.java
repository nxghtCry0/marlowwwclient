package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.utils.FontUtils;
import com.eclipseware.imnotcheatingyouare.client.utils.RenderUtils;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3d;

import java.awt.Color;

public class ESP extends Module {

    public ESP() {
        super("ESP", Category.Render);
    }

    private void onHudRender(DrawContext DrawContext, Object tickDeltaObj) {
        if (!isToggled() || mc.player == null || mc.level == null) return;

        Module bypassMod = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("Bypass");
        if (bypassMod != null && bypassMod.isToggled()) return;

        float partialTick = getTickDelta(tickDeltaObj);

        Setting modeSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Mode");
        String mode = modeSetting != null ? modeSetting.getValString() : "Outline";
        if (mode.equals("Glow")) return;

        Setting mobsSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Show Mobs");
        boolean showMobs = mobsSetting != null && mobsSetting.getValBoolean();
        Setting fillSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Fill");
        boolean doFill = fillSetting == null || fillSetting.getValBoolean();
        Setting healthSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Health");
        boolean showHealth = healthSetting == null || healthSetting.getValBoolean();
        Setting namesSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Names");
        boolean showNames = namesSetting == null || namesSetting.getValBoolean();
        Setting outlineSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Outline Thickness");
        int outlineThickness = outlineSetting != null ? (int) outlineSetting.getValDouble() : 1;
        Setting borderSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Border");
        boolean doBorder = borderSetting == null || borderSetting.getValBoolean();

        boolean useCorner = mode.equals("Outline") || mode.equals("Hybrid");
        boolean useFull = mode.equals("2D") || mode.equals("3D") || mode.equals("Hybrid");
        Setting cornerGapSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Corner Gap");
        float cornerGap = cornerGapSetting != null ? (float) cornerGapSetting.getValDouble() : 50f;

        Color themeColor = RenderUtils.getThemeAccentColor();

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity == mc.player || !(entity instanceof LivingEntity le) || !le.isAlive()) continue;
            double dist = mc.player.distanceTo(entity);
            if (dist > 64.0) continue;
            boolean isPlayer = entity instanceof Player;
            boolean isMob = entity instanceof Mob;
            if (!isPlayer && !(isMob && showMobs)) continue;

            Color color = isPlayer ? themeColor : new Color(255, 85, 85);

            double x = net.minecraft.util.Mth.lerp(partialTick, entity.xo, entity.getX());
            double y = net.minecraft.util.Mth.lerp(partialTick, entity.yo, entity.getY());
            double z = net.minecraft.util.Mth.lerp(partialTick, entity.zo, entity.getZ());
            float hw = entity.getBbWidth() / 2.0f;
            float h = entity.getBbHeight();

            double[][] corners = {
                {x - hw, y,     z - hw},
                {x + hw, y,     z - hw},
                {x - hw, y + h, z - hw},
                {x + hw, y + h, z - hw},
                {x - hw, y,     z + hw},
                {x + hw, y,     z + hw},
                {x - hw, y + h, z + hw},
                {x + hw, y + h, z + hw},
            };

            double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
            double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
            boolean valid = false;

            for (double[] c : corners) {
                Vector3d proj = RenderUtils.project2D(c[0], c[1], c[2], partialTick);
                if (proj == null) continue;
                valid = true;
                if (proj.x < minX) minX = proj.x;
                if (proj.x > maxX) maxX = proj.x;
                if (proj.y < minY) minY = proj.y;
                if (proj.y > maxY) maxY = proj.y;
            }
            if (!valid) continue;

            float rectW = (float)(maxX - minX);
            float rectH = (float)(maxY - minY);
            float alpha = Math.max(0.3f, 1.0f - (float)(dist / 64.0));
            int oa = (int)(alpha * 255);
            int fa = (int)(alpha * 35);
            Color outlineColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), oa);
            Color fillColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), fa);
            int black = new Color(0, 0, 0, oa).getRGB();
            int oc = outlineColor.getRGB();
            int fc = fillColor.getRGB();
            int ix = (int) minX, iy = (int) minY, ix2 = (int) maxX, iy2 = (int) maxY;
            int t = outlineThickness;

            if (doFill) {
                DrawContext.fill(ix + t, iy + t, ix2 - t, iy2 - t, fc);
            }

            if (useCorner && !useFull) {
                float gapPct = Math.min(1f, Math.max(0f, cornerGap / 100f));
                int cw = (int)(rectW * (1f - gapPct) / 2f);
                int ch = (int)(rectH * (1f - gapPct) / 2f);
                cw = Math.max(cw, 3);
                ch = Math.max(ch, 3);

                if (doBorder) {
                    drawHLine(DrawContext, ix - 1, ix + cw + 1, iy - 1, t + 2, black);
                    drawVLine(DrawContext, ix - 1, iy - 1, iy + ch + 1, t + 2, black);
                    drawHLine(DrawContext, ix2 - cw - 1, ix2 + 1, iy - 1, t + 2, black);
                    drawVLine(DrawContext, ix2 - t - 1, iy - 1, iy + ch + 1, t + 2, black);
                    drawHLine(DrawContext, ix - 1, ix + cw + 1, iy2 - t - 1, t + 2, black);
                    drawVLine(DrawContext, ix - 1, iy2 - ch - 1, iy2 + 1, t + 2, black);
                    drawHLine(DrawContext, ix2 - cw - 1, ix2 + 1, iy2 - t - 1, t + 2, black);
                    drawVLine(DrawContext, ix2 - t - 1, iy2 - ch - 1, iy2 + 1, t + 2, black);
                }

                drawHLine(DrawContext, ix, ix + cw, iy, t, oc);
                drawVLine(DrawContext, ix, iy, iy + ch, t, oc);
                drawHLine(DrawContext, ix2 - cw, ix2, iy, t, oc);
                drawVLine(DrawContext, ix2 - t, iy, iy + ch, t, oc);
                drawHLine(DrawContext, ix, ix + cw, iy2 - t, t, oc);
                drawVLine(DrawContext, ix, iy2 - ch, iy2, t, oc);
                drawHLine(DrawContext, ix2 - cw, ix2, iy2 - t, t, oc);
                drawVLine(DrawContext, ix2 - t, iy2 - ch, iy2, t, oc);
            } else {
                if (doBorder) {
                    drawHLine(DrawContext, ix - 1, ix2 + 1, iy - 1, t + 2, black);
                    drawHLine(DrawContext, ix - 1, ix2 + 1, iy2 - t - 1, t + 2, black);
                    drawVLine(DrawContext, ix - 1, iy - 1, iy2 + 1, t + 2, black);
                    drawVLine(DrawContext, ix2 - t - 1, iy - 1, iy2 + 1, t + 2, black);
                }

                drawHLine(DrawContext, ix, ix2, iy, t, oc);
                drawHLine(DrawContext, ix, ix2, iy2 - t, t, oc);
                drawVLine(DrawContext, ix, iy, iy2, t, oc);
                drawVLine(DrawContext, ix2 - t, iy, iy2, t, oc);
            }

            if (showHealth) {
                float maxHp = le.getMaxHealth();
                float pct = Math.min(1f, Math.max(0f, le.getHealth() / Math.max(1f, maxHp)));
                Color hpColor = RenderUtils.getHealthColor(pct);
                int barH = (int)(rectH * pct);
                int barX = ix - 5 - (doBorder ? 2 : 0);
                int barW = 3;

                DrawContext.fill(barX, iy, barX + barW, iy2, new Color(0, 0, 0, (int)(alpha * 140)).getRGB());
                DrawContext.fill(barX, iy2 - barH, barX + barW, iy2,
                    new Color(hpColor.getRed(), hpColor.getGreen(), hpColor.getBlue(), oa).getRGB());
            }

            if (showNames) {
                String name = entity.getName().getString();
                double d = Math.round(dist * 10.0) / 10.0;
                String distStr = " " + d + "m";
                int textWidth = FontUtils.width(name + distStr);
                int textX = (int)(minX + rectW / 2 - textWidth / 2);
                int textY = iy - 12 - (doBorder ? 2 : 0);
                DrawContext.fill(textX - 2, textY - 1, textX + textWidth + 2, textY + 10,
                    new Color(0, 0, 0, (int)(alpha * 150)).getRGB());
                FontUtils.drawString(DrawContext, name, textX, textY, outlineColor.getRGB(), false);
                FontUtils.drawString(DrawContext, distStr, textX + FontUtils.width(name), textY,
                    new Color(200, 200, 200, oa).getRGB(), false);
            }
        }
    }

    private void drawHLine(DrawContext g, int x1, int x2, int y, int thickness, int color) {
        g.fill(x1, y, x2, y + thickness, color);
    }

    private void drawVLine(DrawContext g, int x, int y1, int y2, int thickness, int color) {
        g.fill(x, y1, x + thickness, y2, color);
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
