package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.utils.RenderUtils;
import imgui.ImDrawList;
import imgui.ImGui;
import imgui.ImVec2;
import net.minecraft.client.Camera;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector4f;

import java.awt.Color;

public class ESP extends Module {

    public ESP() {
        super("ESP", Category.Render, "Renders high-performance ImGui overlay for entities.");
    }

    private static final Vector3d[] projBuffer = new Vector3d[8];
    static {
        for (int i = 0; i < 8; i++) {
            projBuffer[i] = new Vector3d();
        }
    }

    private static final Vector4f transformVec = new Vector4f();
    private static final ThreadLocal<Matrix4f> combinedMatrixBuffer = ThreadLocal.withInitial(Matrix4f::new);
    private static final Color MOB_COLOR = new Color(255, 95, 95);

    private static final ImVec2 nameSizeBuf = new ImVec2();
    private static final ImVec2 distSizeBuf = new ImVec2();
    private static final ImVec2 hpSizeBuf = new ImVec2();
    private static final ImVec2 itemSizeBuf = new ImVec2();

    private Setting modeSetting;
    private Setting mobsSetting;
    private Setting fillSetting;
    private Setting healthSetting;
    private Setting namesSetting;
    private Setting outlineSetting;
    private Setting borderSetting;
    private Setting cornerGapSetting;

    private void cacheSettings() {
        if (modeSetting == null) {
            modeSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Mode");
            mobsSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Show Mobs");
            fillSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Fill");
            healthSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Health");
            namesSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Names");
            outlineSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Outline Thickness");
            borderSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Border");
            cornerGapSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Corner Gap");
        }
    }

    @Override
    public void onRenderHUD(GuiGraphicsExtractor guiGraphics, Object tickDeltaObj) {
    }

    public void renderImGuiOverlay() {
        if (!isToggled() || mc.player == null || mc.level == null) return;

        Module bypassMod = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("Bypass");
        if (bypassMod != null && bypassMod.isToggled()) return;

        float partialTick = mc.getDeltaTracker() != null ? mc.getDeltaTracker().getGameTimeDeltaPartialTick(true) : 1.0f;
        cacheSettings();

        String mode = modeSetting != null ? modeSetting.getValString() : "Outline";
        boolean overlayOnly = mode.equals("Glow") || mode.equals("Shader");
        if (overlayOnly && !bool("Name Tags", true)) return;

        boolean showMobs = mobsSetting != null && mobsSetting.getValBoolean();
        boolean doFill = fillSetting == null || fillSetting.getValBoolean();
        boolean showHealth = healthSetting == null || healthSetting.getValBoolean();
        boolean showNames = namesSetting == null || namesSetting.getValBoolean();
        int outlineThickness = outlineSetting != null ? (int) outlineSetting.getValDouble() : 1;
        boolean doBorder = borderSetting == null || borderSetting.getValBoolean();

        boolean useCorner = mode.equals("Outline") || mode.equals("Hybrid");
        float cornerGap = cornerGapSetting != null ? (float) cornerGapSetting.getValDouble() : 50f;

        Color themeColor = RenderUtils.getThemeAccentColor();
        ImDrawList drawList = ImGui.getBackgroundDrawList();

        double maxDist = mc.options != null ? Math.max(256.0, mc.options.getEffectiveRenderDistance() * 16.0) : 256.0;
        float displayWidth = ImGui.getIO().getDisplaySizeX();
        float displayHeight = ImGui.getIO().getDisplaySizeY();

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity == mc.player || !(entity instanceof LivingEntity le) || !le.isAlive()) continue;
            double dist = mc.player.distanceTo(entity);
            if (dist > maxDist) continue;
            boolean isPlayer = entity instanceof Player;
            boolean isMob = entity instanceof Mob;
            if (!isPlayer && !(isMob && showMobs)) continue;

            Color color = getEntityColor(entity);

            double x = net.minecraft.util.Mth.lerp(partialTick, entity.xo, entity.getX());
            double y = net.minecraft.util.Mth.lerp(partialTick, entity.yo, entity.getY());
            double z = net.minecraft.util.Mth.lerp(partialTick, entity.zo, entity.getZ());
            float hw = entity.getBbWidth() / 2.0f;
            float h = entity.getBbHeight();

            double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
            double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;

            int validCount = 0;
            for (int i = 0; i < 8; i++) {
                double cx = x + ((i & 1) == 0 ? -hw : hw);
                double cy = y + ((i & 2) == 0 ? 0 : h);
                double cz = z + ((i & 4) == 0 ? -hw : hw);

                if (project2DImGui(cx, cy, cz, partialTick, projBuffer[i])) {
                    validCount++;
                    double px = projBuffer[i].x;
                    double py = projBuffer[i].y;
                    if (px < minX) minX = px;
                    if (px > maxX) maxX = px;
                    if (py < minY) minY = py;
                    if (py > maxY) maxY = py;
                }
            }
            if (validCount == 0) continue;
            if (!Double.isFinite(minX) || !Double.isFinite(minY) || !Double.isFinite(maxX) || !Double.isFinite(maxY)) continue;

            float ix = (float) Math.floor(minX);
            float iy = (float) Math.floor(minY);
            float ix2 = (float) Math.ceil(maxX);
            float iy2 = (float) Math.ceil(maxY);

            if (ix2 < -500f || ix > displayWidth + 500f || iy2 < -500f || iy > displayHeight + 500f) continue;
            if (ix2 <= ix || iy2 <= iy) continue;

            if (overlayOnly) {
                float tagAlpha = Math.max(0.4f, 1.0f - (float)(dist / maxDist));
                drawNameTag(drawList, le, (ix + ix2) / 2f, iy, color, tagAlpha, dist);
                continue;
            }

            float rectW = (float)(maxX - minX);
            float rectH = (float)(maxY - minY);
            float alpha = Math.max(0.4f, 1.0f - (float)(dist / maxDist));
            int oa = (int)(alpha * 255);

            int oc = toImGuiColor(color, alpha);
            int black = toImGuiColor(0, 0, 0, oa);
            float t = (float) outlineThickness;

            if (doFill && (ix2 - t > ix + t) && (iy2 - t > iy + t)) {
                int fillTop = toImGuiColor(color, alpha * 0.04f);
                int fillBottom = toImGuiColor(color, alpha * 0.22f);
                drawList.addRectFilledMultiColor(ix + t, iy + t, ix2 - t, iy2 - t, fillTop, fillTop, fillBottom, fillBottom);
            }

            if (useCorner) {
                float gapPct = Math.min(1f, Math.max(0f, cornerGap / 100f));
                float cw = Math.max(4f, rectW * (1f - gapPct) / 2f);
                float ch = Math.max(4f, rectH * (1f - gapPct) / 2f);

                if (doBorder) {
                    drawImGuiCornerBox(drawList, ix - 1f, iy - 1f, ix2 + 1f, iy2 + 1f, cw + 1f, ch + 1f, 1.5f, black);
                    if (ix2 - t > ix + t && iy2 - t > iy + t) {
                        drawImGuiCornerBox(drawList, ix + t, iy + t, ix2 - t, iy2 - t, cw - t, ch - t, 1.5f, black);
                    }
                }
                drawImGuiCornerBox(drawList, ix, iy, ix2, iy2, cw, ch, t, oc);
            } else {
                if (doBorder) {
                    drawList.addRect(ix - 1f, iy - 1f, ix2 + 1f, iy2 + 1f, black, 2.0f, 0, 1.5f);
                    if (ix2 - t > ix + t && iy2 - t > iy + t) {
                        drawList.addRect(ix + t, iy + t, ix2 - t, iy2 - t, black, 2.0f, 0, 1.5f);
                    }
                }
                drawList.addRect(ix, iy, ix2, iy2, oc, 2.0f, 0, t);
            }

            if (showHealth) {
                float maxHp = le.getMaxHealth();
                float currentHp = le.getHealth();
                float pct = Math.min(1f, Math.max(0f, currentHp / Math.max(1f, maxHp)));
                Color hpColor = RenderUtils.getHealthColor(pct);
                float barH = rectH * pct;
                float barX = ix - 7f;

                int hpImColor = toImGuiColor(hpColor, alpha);
                int bgImColor = toImGuiColor(15, 15, 20, (int)(alpha * 255.0f * 0.8f));
                int borderImColor = toImGuiColor(0, 0, 0, (int)(alpha * 255.0f * 0.9f));

                drawList.addRectFilled(barX - 1.5f, iy - 1.5f, barX + 3.5f, iy2 + 1.5f, bgImColor, 2.5f);
                drawList.addRect(barX - 1.5f, iy - 1.5f, barX + 3.5f, iy2 + 1.5f, borderImColor, 2.5f, 0, 1.0f);

                if (barH > 0) {
                    int hpTop = toImGuiColor(hpColor.brighter(), alpha);
                    drawList.addRectFilledMultiColor(barX, iy2 - barH, barX + 2f, iy2, hpTop, hpTop, hpImColor, hpImColor);
                }
            }

            if (showNames) {
                drawNameTag(drawList, le, ix + rectW / 2f, iy, color, alpha, dist);
            }
        }
    }

    private void drawNameTag(ImDrawList drawList, LivingEntity le, float centerX, float topY, Color color, float alpha, double dist) {
        boolean showDistance = bool("Tag Distance", true);
        boolean showHp = bool("Tag Health", true);
        boolean showItem = bool("Tag Item", true);
        int oa = (int)(alpha * 255);

        String name = le.getName().getString();
        String distStr = showDistance ? "  " + (Math.round(dist * 10.0) / 10.0) + "m" : "";
        String hpStr = showHp ? "  " + (Math.round(le.getHealth() * 10.0f) / 10.0f) + " hp" : "";

        ImGui.calcTextSize(nameSizeBuf, name);
        float distW = 0f;
        float hpW = 0f;
        if (!distStr.isEmpty()) {
            ImGui.calcTextSize(distSizeBuf, distStr);
            distW = distSizeBuf.x;
        }
        if (!hpStr.isEmpty()) {
            ImGui.calcTextSize(hpSizeBuf, hpStr);
            hpW = hpSizeBuf.x;
        }

        float paddingX = 7f;
        float cardWidth = nameSizeBuf.x + distW + hpW + paddingX * 2;
        float cardHeight = nameSizeBuf.y + 8f;
        float cardX = centerX - cardWidth / 2f;
        float cardY = topY - cardHeight - 6f;

        float pct = Math.min(1f, Math.max(0f, le.getHealth() / Math.max(1f, le.getMaxHealth())));
        Color hpColor = RenderUtils.getHealthColor(pct);

        drawList.addRectFilled(cardX + 1f, cardY + 2f, cardX + cardWidth + 1f, cardY + cardHeight + 2f, toImGuiColor(0, 0, 0, (int)(oa * 0.35f)), 5.0f);
        drawList.addRectFilled(cardX, cardY, cardX + cardWidth, cardY + cardHeight, toImGuiColor(16, 13, 24, (int)(oa * 0.88f)), 5.0f);
        drawList.addRect(cardX, cardY, cardX + cardWidth, cardY + cardHeight, toImGuiColor(color, alpha * 0.35f), 5.0f, 0, 1.0f);
        if (showHp) {
            float barW = (cardWidth - 8f) * pct;
            drawList.addRectFilled(cardX + 4f, cardY + cardHeight - 2.5f, cardX + 4f + barW, cardY + cardHeight - 1f, toImGuiColor(hpColor, alpha), 1.0f);
        } else {
            float inset = Math.min(8f, cardWidth / 4f);
            drawList.addLine(cardX + inset, cardY + 0.5f, cardX + cardWidth - inset, cardY + 0.5f, toImGuiColor(color, alpha), 1.5f);
        }

        float curX = cardX + paddingX;
        float textY = cardY + 3.5f;
        drawList.addText(curX, textY, toImGuiColor(255, 255, 255, oa), name);
        curX += nameSizeBuf.x;
        if (!distStr.isEmpty()) {
            drawList.addText(curX, textY, toImGuiColor(150, 145, 170, oa), distStr);
            curX += distW;
        }
        if (!hpStr.isEmpty()) {
            drawList.addText(curX, textY, toImGuiColor(hpColor, alpha), hpStr);
        }

        ItemStack mainHand = le.getMainHandItem();
        if (showItem && mainHand != null && !mainHand.isEmpty()) {
            String itemText = mainHand.getHoverName().getString();
            ImGui.calcTextSize(itemSizeBuf, itemText);
            float itemCardW = itemSizeBuf.x + 10f;
            float itemCardH = itemSizeBuf.y + 4f;
            float itemCardX = centerX - itemCardW / 2f;
            float itemCardY = cardY - itemCardH - 3f;
            drawList.addRectFilled(itemCardX, itemCardY, itemCardX + itemCardW, itemCardY + itemCardH, toImGuiColor(12, 10, 18, (int)(oa * 0.8f)), 4.0f);
            drawList.addText(itemCardX + 5f, itemCardY + 2f, toImGuiColor(220, 215, 235, oa), itemText);
        }
    }

    private void drawImGuiCornerBox(ImDrawList drawList, float x1, float y1, float x2, float y2, float cw, float ch, float thickness, int color) {
        drawList.addLine(x1, y1, x1 + cw, y1, color, thickness);
        drawList.addLine(x1, y1, x1, y1 + ch, color, thickness);

        drawList.addLine(x2, y1, x2 - cw, y1, color, thickness);
        drawList.addLine(x2, y1, x2, y1 + ch, color, thickness);

        drawList.addLine(x1, y2, x1 + cw, y2, color, thickness);
        drawList.addLine(x1, y2, x1, y2 - ch, color, thickness);

        drawList.addLine(x2, y2, x2 - cw, y2, color, thickness);
        drawList.addLine(x2, y2, x2, y2 - ch, color, thickness);
    }

    public boolean project2DImGui(double x, double y, double z, float partialTicks, Vector3d out) {
        if (mc.gameRenderer == null) return false;
        Camera camera = mc.gameRenderer.mainCamera();
        if (camera == null) return false;
        Vec3 camPos = camera.position();

        Matrix4f combinedMatrix = camera.getViewRotationProjectionMatrix(combinedMatrixBuffer.get());

        transformVec.set((float)(x - camPos.x), (float)(y - camPos.y), (float)(z - camPos.z), 1.0f);
        combinedMatrix.transform(transformVec);

        if (transformVec.w <= 0.001f) return false;
        transformVec.div(transformVec.w);

        float displayWidth = ImGui.getIO().getDisplaySizeX();
        float displayHeight = ImGui.getIO().getDisplaySizeY();

        double screenX = (displayWidth / 2.0f) * (transformVec.x + 1.0f);
        double screenY = (displayHeight / 2.0f) * (1.0f - transformVec.y);

        out.set(screenX, screenY, transformVec.z);
        return true;
    }

    private static int toImGuiColor(int r, int g, int b, int a) {
        return ((a & 0xFF) << 24) | ((b & 0xFF) << 16) | ((g & 0xFF) << 8) | (r & 0xFF);
    }

    private static int toImGuiColor(Color color, float alphaFactor) {
        int a = Math.max(0, Math.min(255, (int) (color.getAlpha() * alphaFactor)));
        return ((a & 0xFF) << 24) | ((color.getBlue() & 0xFF) << 16) | ((color.getGreen() & 0xFF) << 8) | (color.getRed() & 0xFF);
    }

    private Setting setting(String name) {
        return ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, name);
    }

    private boolean bool(String name, boolean fallback) {
        Setting s = setting(name);
        return s != null ? s.getValBoolean() : fallback;
    }

    private Color colorSetting(String name, Color fallback) {
        Setting s = setting(name);
        return s != null ? new Color(s.getValColor(), true) : fallback;
    }

    public net.minecraft.resources.Identifier getShaderChainId() {
        Setting featherSetting = setting("Feather");
        Setting fillOpacitySetting = setting("Fill Opacity");
        int feather = featherSetting != null ? (int) Math.max(1, Math.min(5, featherSetting.getValDouble())) : 3;
        int fill = fillOpacitySetting != null ? (int) Math.max(0, Math.min(5, Math.round(fillOpacitySetting.getValDouble() / 10.0))) : 2;
        return net.minecraft.resources.Identifier.parse("imnotcheatingyouare:shader_esp_f" + feather + "_a" + fill);
    }

    public boolean shouldHighlight(Entity entity) {
        if (entity == mc.player || !(entity instanceof LivingEntity)) return false;
        if (entity instanceof Player) return true;
        cacheSettings();
        return entity instanceof Mob && mobsSetting != null && mobsSetting.getValBoolean();
    }

    public Color getEntityColor(Entity entity) {
        Setting colorMode = setting("Color Mode");
        String mode = colorMode != null ? colorMode.getValString() : "Theme";
        if (mode.equals("Custom")) return colorSetting("Custom Color", RenderUtils.getThemeAccentColor());
        if (mode.equals("Per Type")) {
            if (entity instanceof Player) return colorSetting("Player Color", RenderUtils.getThemeAccentColor());
            if (entity instanceof net.minecraft.world.entity.monster.Enemy) return colorSetting("Hostile Color", MOB_COLOR);
            if (entity instanceof net.minecraft.world.entity.NeutralMob) return colorSetting("Neutral Color", new Color(255, 205, 90));
            if (entity instanceof net.minecraft.world.entity.animal.Animal) return colorSetting("Passive Color", new Color(120, 230, 150));
            return colorSetting("Neutral Color", new Color(255, 205, 90));
        }
        return entity instanceof Player ? RenderUtils.getThemeAccentColor() : MOB_COLOR;
    }

    public boolean isShaderMode() {
        if (!isToggled()) return false;
        cacheSettings();
        return modeSetting != null && modeSetting.getValString().equals("Shader");
    }

    public boolean shouldGlow() {
        if (!isToggled()) return false;
        cacheSettings();
        String mode = modeSetting != null ? modeSetting.getValString() : "2D";
        return mode.equals("Glow") || mode.equals("Both");
    }

    private static java.lang.reflect.Method cachedTickDeltaMethod = null;
    private static Class<?> cachedTickDeltaClass = null;
    private static Object[] cachedTickDeltaArgs = null;
    private static boolean tickDeltaResolved = false;

    private float getTickDelta(Object tickDeltaObj) {
        if (tickDeltaObj instanceof Float) return (Float) tickDeltaObj;
        if (tickDeltaObj == null) return 1.0f;

        Class<?> clazz = tickDeltaObj.getClass();
        if (tickDeltaResolved && clazz == cachedTickDeltaClass) {
            if (cachedTickDeltaMethod != null) {
                try {
                    return (float) cachedTickDeltaMethod.invoke(tickDeltaObj, cachedTickDeltaArgs);
                } catch (Exception e) {
                    return 1.0f;
                }
            }
            return 1.0f;
        }

        cachedTickDeltaClass = clazz;
        cachedTickDeltaMethod = null;
        cachedTickDeltaArgs = null;
        tickDeltaResolved = true;

        for (java.lang.reflect.Method m : clazz.getMethods()) {
            if (m.getReturnType() == float.class) {
                if (m.getParameterCount() == 0) {
                    String name = m.getName().toLowerCase();
                    if (name.contains("tick") || name.contains("delta") || name.contains("frame")) {
                        try {
                            m.setAccessible(true);
                            float val = (float) m.invoke(tickDeltaObj);
                            cachedTickDeltaMethod = m;
                            cachedTickDeltaArgs = new Object[0];
                            return val;
                        } catch (Exception e) {}
                    }
                }
            }
        }

        for (java.lang.reflect.Method m : clazz.getMethods()) {
            if (m.getReturnType() == float.class) {
                if (m.getParameterCount() == 1 && m.getParameterTypes()[0] == boolean.class) {
                    try {
                        m.setAccessible(true);
                        float val = (float) m.invoke(tickDeltaObj, true);
                        cachedTickDeltaMethod = m;
                        cachedTickDeltaArgs = new Object[]{true};
                        return val;
                    } catch (Exception e) {}
                }
            }
        }

        return 1.0f;
    }
}