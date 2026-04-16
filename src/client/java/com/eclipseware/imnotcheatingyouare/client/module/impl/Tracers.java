package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.utils.RenderUtils;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import org.joml.Vector3d;

import java.awt.Color;

public class Tracers extends Module {

    public Tracers() {
        super("Tracers", Category.Render);
        HudRenderCallback.EVENT.register((guiGraphics, tickDelta) -> onHudRender(guiGraphics));
    }

    private void onHudRender(GuiGraphics guiGraphics) {
        if (!isToggled() || mc.player == null || mc.level == null) return;

        Setting crosshairSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Crosshair Attach");
        boolean attachCrosshair = crosshairSetting != null && crosshairSetting.getValBoolean();

        Setting mobsSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Show Mobs");
        boolean showMobs = mobsSetting != null && mobsSetting.getValBoolean();

        double startX = mc.getWindow().getGuiScaledWidth() / 2.0;
        double startY = mc.getWindow().getGuiScaledHeight() / 2.0;

        if (!attachCrosshair) {
            Vector3d playerPos = RenderUtils.project2D(
                mc.player.getX(), mc.player.getY() + mc.player.getEyeHeight(), mc.player.getZ(), 1.0f);
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

            Vector3d endProj = RenderUtils.project2D(
                entity.getX(), entity.getY() + entity.getBbHeight() / 2.0, entity.getZ(), 1.0f);
            if (endProj == null || endProj.z <= 0 || endProj.z >= 1.0) continue;

            float alpha = Math.max(0.25f, 1.0f - (float)(dist / 64.0));
            Color color = isPlayer ?
                new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), (int)(alpha * 180)) :
                new Color(255, 85, 85, (int)(alpha * 180));

            RenderUtils.drawLine2D(guiGraphics, startX, startY, endProj.x, endProj.y, color);
        }
    }
}