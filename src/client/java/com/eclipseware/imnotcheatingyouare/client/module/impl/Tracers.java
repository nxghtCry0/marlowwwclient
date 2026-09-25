package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.utils.RenderUtils;
import imgui.ImDrawList;
import imgui.ImGui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import org.joml.Vector3d;

import java.awt.Color;

public class Tracers extends Module {

    public Tracers() {
        super("Tracers", Category.Render, "Renders tracer lines to entities via ImGui.");
    }

    private static final Vector3d playerProj = new Vector3d();
    private static final Vector3d entityProj = new Vector3d();

    @Override
    public void onRenderHUD(GuiGraphicsExtractor guiGraphics, Object tickDeltaObj) {
    }

    public void renderImGuiOverlay() {
        if (!isToggled() || mc.player == null || mc.level == null) return;

        float partialTick = mc.getDeltaTracker() != null ? mc.getDeltaTracker().getGameTimeDeltaPartialTick(true) : 1.0f;

        Setting crosshairSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Crosshair Attach");
        boolean attachCrosshair = crosshairSetting != null && crosshairSetting.getValBoolean();

        Setting mobsSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Show Mobs");
        boolean showMobs = mobsSetting != null && mobsSetting.getValBoolean();

        float displayWidth = ImGui.getIO().getDisplaySizeX();
        float displayHeight = ImGui.getIO().getDisplaySizeY();
        ImDrawList drawList = ImGui.getBackgroundDrawList();

        float startX, startY;
        if (attachCrosshair) {
            startX = displayWidth / 2.0f;
            startY = displayHeight / 2.0f;
        } else {
            double px = net.minecraft.util.Mth.lerp(partialTick, mc.player.xo, mc.player.getX());
            double py = net.minecraft.util.Mth.lerp(partialTick, mc.player.yo, mc.player.getY()) + mc.player.getEyeHeight();
            double pz = net.minecraft.util.Mth.lerp(partialTick, mc.player.zo, mc.player.getZ());
            if (!RenderUtils.project2DImGui(px, py, pz, partialTick, playerProj)) return;
            startX = (float) playerProj.x;
            startY = (float) playerProj.y;
        }

        Color themeColor = RenderUtils.getThemeAccentColor();
        double maxDist = mc.options != null ? Math.max(256.0, mc.options.getEffectiveRenderDistance() * 16.0) : 256.0;

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity == mc.player || !entity.isAlive()) continue;

            boolean isPlayer = entity instanceof Player;
            boolean isMob = entity instanceof Mob;
            if (!isPlayer && !(isMob && showMobs)) continue;

            double dist = mc.player.distanceTo(entity);
            if (dist > maxDist) continue;

            double ex = net.minecraft.util.Mth.lerp(partialTick, entity.xo, entity.getX());
            double ey = net.minecraft.util.Mth.lerp(partialTick, entity.yo, entity.getY()) + entity.getBbHeight() / 2.0;
            double ez = net.minecraft.util.Mth.lerp(partialTick, entity.zo, entity.getZ());

            if (!RenderUtils.project2DImGui(ex, ey, ez, partialTick, entityProj)) continue;
            if (entityProj.z <= 0 || entityProj.z >= 1.0) continue;
            if (!Double.isFinite(entityProj.x) || !Double.isFinite(entityProj.y)) continue;

            float endX = (float) entityProj.x;
            float endY = (float) entityProj.y;
            if (endX < -500f || endX > displayWidth + 500f || endY < -500f || endY > displayHeight + 500f) continue;

            float alpha = Math.max(0.35f, 1.0f - (float)(dist / maxDist));
            Color color = isPlayer ? themeColor : new Color(255, 85, 85);
            int colInt = RenderUtils.toImGuiColor(color, alpha);

            drawList.addLine(startX, startY, endX, endY, colInt, 1.2f);
        }
    }
}