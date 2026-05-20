package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.utils.FontUtils;
import com.eclipseware.imnotcheatingyouare.client.utils.RenderUtils;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import org.joml.Vector3d;

import java.awt.Color;

public class ESP extends Module {

    public ESP() {
        super("ESP", Category.Render);
    }

    private static final Vector3d[] projBuffer = new Vector3d[8];
    static {
        for (int i = 0; i < 8; i++) {
            projBuffer[i] = new Vector3d();
        }
    }

    @Override
    public void onRenderHUD(GuiGraphicsExtractor guiGraphics, Object tickDeltaObj) {
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

            double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
            double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
            boolean valid = false;

            for (int i = 0; i < 8; i++) {
                double cx = x + ((i & 1) == 0 ? -hw : hw);
                double cy = y + ((i & 2) == 0 ? 0 : h);
                double cz = z + ((i & 4) == 0 ? -hw : hw);

                if (RenderUtils.project2D(cx, cy, cz, partialTick, projBuffer[i])) {
                    valid = true;
                    double px = projBuffer[i].x;
                    double py = projBuffer[i].y;
                    if (px < minX) minX = px;
                    if (px > maxX) maxX = px;
                    if (py < minY) minY = py;
                    if (py > maxY) maxY = py;
                }
            }
            if (!valid) continue;

            float rectW = (float)(maxX - minX);
            float rectH = (float)(maxY - minY);
            float alpha = Math.max(0.3f, 1.0f - (float)(dist / 64.0));
            int oa = (int)(alpha * 255);
            Color outlineColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), oa);
            int black = new Color(0, 0, 0, oa).getRGB();
            int oc = outlineColor.getRGB();
            int ix = (int) minX, iy = (int) minY, ix2 = (int) maxX, iy2 = (int) maxY;
            int t = outlineThickness;

            if (doFill) {
                guiGraphics.fill(ix + t, iy + t, ix2 - t, iy2 - t, new Color(0, 0, 0, 35).getRGB());
            }

            if (useCorner) {
                float gapPct = Math.min(1f, Math.max(0f, cornerGap / 100f));
                int cw = (int)(rectW * (1f - gapPct) / 2f);
                int ch = (int)(rectH * (1f - gapPct) / 2f);
                cw = Math.max(cw, 3);
                ch = Math.max(ch, 3);

                if (doBorder) {
                    drawCornerBox(guiGraphics, ix - 1, iy - 1, ix2 + 1, iy2 + 1, cw + 1, ch + 1, 1, black);
                    drawCornerBox(guiGraphics, ix + t, iy + t, ix2 - t, iy2 - t, cw - t, ch - t, 1, black);
                }
                drawCornerBox(guiGraphics, ix, iy, ix2, iy2, cw, ch, t, oc);
            } else {
                if (doBorder) {
                    drawBox(guiGraphics, ix - 1, iy - 1, ix2 + 1, iy2 + 1, 1, black);
                    drawBox(guiGraphics, ix + t, iy + t, ix2 - t, iy2 - t, 1, black);
                }
                drawBox(guiGraphics, ix, iy, ix2, iy2, t, oc);
            }

            if (showHealth) {
                float maxHp = le.getMaxHealth();
                float pct = Math.min(1f, Math.max(0f, le.getHealth() / Math.max(1f, maxHp)));
                Color hpColor = RenderUtils.getHealthColor(pct);
                int barH = (int)(rectH * pct);
                int barX = ix - 6;

                guiGraphics.fill(barX - 1, iy - 1, barX + 2, iy2 + 1, new Color(0, 0, 0, (int)(alpha * 160)).getRGB());
                guiGraphics.fill(barX, iy2 - barH, barX + 1, iy2,
                    new Color(hpColor.getRed(), hpColor.getGreen(), hpColor.getBlue(), oa).getRGB());
            }

            if (showNames) {
                String name = entity.getName().getString();
                double d = Math.round(dist * 10.0) / 10.0;
                String distStr = " [" + d + "m]";
                String fullText = name + distStr;
                int textWidth = FontUtils.width(fullText);
                int textX = (int)(minX + rectW / 2 - textWidth / 2);
                int textY = iy - 12;

                guiGraphics.fill(textX - 3, textY - 2, textX + textWidth + 3, textY + 9, new Color(0, 0, 0, (int)(alpha * 120)).getRGB());
                FontUtils.drawString(guiGraphics, name, textX, textY, Color.WHITE.getRGB(), true);
                FontUtils.drawString(guiGraphics, distStr, textX + FontUtils.width(name), textY, new Color(200, 200, 200, oa).getRGB(), true);
            }
        }
    }

    private void drawBox(GuiGraphicsExtractor g, int x1, int y1, int x2, int y2, int thickness, int color) {
        g.fill(x1, y1, x2, y1 + thickness, color);
        g.fill(x1, y2 - thickness, x2, y2, color);
        g.fill(x1, y1 + thickness, x1 + thickness, y2 - thickness, color);
        g.fill(x2 - thickness, y1 + thickness, x2, y2 - thickness, color);
    }

    private void drawCornerBox(GuiGraphicsExtractor g, int x1, int y1, int x2, int y2, int cw, int ch, int thickness, int color) {
        g.fill(x1, y1, x1 + cw, y1 + thickness, color);
        g.fill(x1, y1 + thickness, x1 + thickness, y1 + ch, color);

        g.fill(x2 - cw, y1, x2, y1 + thickness, color);
        g.fill(x2 - thickness, y1 + thickness, x2, y1 + ch, color);

        g.fill(x1, y2 - thickness, x1 + cw, y2, color);
        g.fill(x1, y2 - ch, x1 + thickness, y2 - thickness, color);

        g.fill(x2 - cw, y2 - thickness, x2, y2, color);
        g.fill(x2 - thickness, y2 - ch, x2, y2 - thickness, color);
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