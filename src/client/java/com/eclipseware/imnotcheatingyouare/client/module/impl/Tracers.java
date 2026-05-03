package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.utils.RenderUtils;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import org.joml.Vector3d;

import java.awt.Color;

public class Tracers extends Module {

    public Tracers() {
        super("Tracers", Category.Render);
        // HudRenderCallback.EVENT.register((guiGraphics, tickDelta) -> onHudRender(guiGraphics, tickDelta));
    }

    private void onHudRender(GuiGraphicsExtractor guiGraphics, Object tickDeltaObj) {
        if (!isToggled() || mc.player == null || mc.level == null) return;

        float partialTick = getTickDelta(tickDeltaObj);

        Setting crosshairSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Crosshair Attach");
        boolean attachCrosshair = crosshairSetting != null && crosshairSetting.getValBoolean();

        Setting mobsSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Show Mobs");
        boolean showMobs = mobsSetting != null && mobsSetting.getValBoolean();

        double startX, startY;
        if (attachCrosshair) {
            startX = mc.getWindow().getGuiScaledWidth() / 2.0;
            startY = mc.getWindow().getGuiScaledHeight() / 2.0;
        } else {
            double px = net.minecraft.util.Mth.lerp(partialTick, mc.player.xo, mc.player.getX());
            double py = net.minecraft.util.Mth.lerp(partialTick, mc.player.yo, mc.player.getY()) + mc.player.getEyeHeight();
            double pz = net.minecraft.util.Mth.lerp(partialTick, mc.player.zo, mc.player.getZ());
            Vector3d playerPos = RenderUtils.project2D(px, py, pz, partialTick);
            if (playerPos == null) return;
            startX = playerPos.x;
            startY = playerPos.y;
        }

        Color themeColor = RenderUtils.getThemeAccentColor();

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity == mc.player || !entity.isAlive()) continue;

            boolean isPlayer = entity instanceof Player;
            boolean isMob = entity instanceof Mob;
            if (!isPlayer && !(isMob && showMobs)) continue;

            double dist = mc.player.distanceTo(entity);
            if (dist > 64.0) continue;

            double ex = net.minecraft.util.Mth.lerp(partialTick, entity.xo, entity.getX());
            double ey = net.minecraft.util.Mth.lerp(partialTick, entity.yo, entity.getY()) + entity.getBbHeight() / 2.0;
            double ez = net.minecraft.util.Mth.lerp(partialTick, entity.zo, entity.getZ());

            Vector3d endProj = RenderUtils.project2D(ex, ey, ez, partialTick);
            if (endProj == null || endProj.z <= 0 || endProj.z >= 1.0) continue;

            float alpha = Math.max(0.25f, 1.0f - (float)(dist / 64.0));
            Color color = isPlayer ?
                new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), (int)(alpha * 180)) :
                new Color(255, 85, 85, (int)(alpha * 180));

            RenderUtils.drawLine2D(guiGraphics, startX, startY, endProj.x, endProj.y, color);
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