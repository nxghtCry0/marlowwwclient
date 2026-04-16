package com.eclipseware.imnotcheatingyouare.client.utils;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector4f;

import java.awt.Color;

public class RenderUtils {
    private static final Minecraft mc = Minecraft.getInstance();
    private static java.lang.reflect.Method getFovMethod;

    public static Vector3d project2D(double x, double y, double z, float partialTicks) {
        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 camPos = getCameraPos(camera);

        Vector4f vec = new Vector4f((float)(x - camPos.x), (float)(y - camPos.y), (float)(z - camPos.z), 1.0f);

        Matrix4f viewMatrix = new Matrix4f().rotation(camera.rotation()).invert();
        viewMatrix.transform(vec);

        float fov = getActualFov(camera, partialTicks);
        Matrix4f projMatrix = mc.gameRenderer.getProjectionMatrix(fov);
        projMatrix.transform(vec);

        if (vec.w <= 0.001f) return null;
        vec.div(vec.w);

        double screenWidth = mc.getWindow().getGuiScaledWidth();
        double screenHeight = mc.getWindow().getGuiScaledHeight();
        double screenX = (screenWidth / 2.0) * (vec.x() + 1.0);
        double screenY = (screenHeight / 2.0) * (1.0 - vec.y());

        return new Vector3d(screenX, screenY, vec.z());
    }

    private static float getActualFov(Camera camera, float partialTicks) {
        try {
            if (getFovMethod == null) {
                getFovMethod = mc.gameRenderer.getClass().getDeclaredMethod("getFov", Camera.class, float.class, boolean.class);
                getFovMethod.setAccessible(true);
            }
            return (float) getFovMethod.invoke(mc.gameRenderer, camera, partialTicks, true);
        } catch (Exception e) {
            return mc.options.fov().get().floatValue();
        }
    }

    private static Vec3 getCameraPos(Camera camera) {
        return camera.position();
    }

    public static void drawLine2D(GuiGraphics graphics, double x1, double y1, double x2, double y2, Color color) {
        double length = Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1));
        if (length < 0.01) return;
        float angle = (float) Math.atan2(y2 - y1, x2 - x1);

        graphics.pose().pushMatrix();
        graphics.pose().translate((float)x1, (float)y1);
        graphics.pose().rotate(angle);
        graphics.fill(0, 0, (int)Math.ceil(length), 1, color.getRGB());
        graphics.pose().popMatrix();
    }

    public static void drawCornerMarks(GuiGraphics graphics, double minX, double minY, double maxX, double maxY, Color color) {
        int c = color.getRGB();
        double boxW = maxX - minX;
        double boxH = maxY - minY;
        double cornerLen = Math.min(Math.min(boxW, boxH) * 0.3, 15.0);
        cornerLen = Math.max(cornerLen, 3.0);
        int cl = (int) cornerLen;

        graphics.fill((int)minX, (int)minY, (int)minX + cl, (int)minY + 1, c);
        graphics.fill((int)minX, (int)minY, (int)minX + 1, (int)minY + cl, c);
        graphics.fill((int)maxX - cl, (int)minY, (int)maxX, (int)minY + 1, c);
        graphics.fill((int)maxX - 1, (int)minY, (int)maxX, (int)minY + cl, c);
        graphics.fill((int)minX, (int)maxY - 1, (int)minX + cl, (int)maxY, c);
        graphics.fill((int)minX, (int)maxY - cl, (int)minX + 1, (int)maxY, c);
        graphics.fill((int)maxX - cl, (int)maxY - 1, (int)maxX, (int)maxY, c);
        graphics.fill((int)maxX - 1, (int)maxY - cl, (int)maxX, (int)maxY, c);
    }

    public static Color getHealthColor(float pct) {
        if (pct > 0.6f) return new Color(85, 255, 85);
        if (pct > 0.3f) return new Color(255, 255, 85);
        return new Color(255, 85, 85);
    }

    public static Color getThemeAccentColor() {
        Module theme = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("Theme");
        if (theme != null) {
            Setting rS = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(theme, "Accent R");
            Setting gS = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(theme, "Accent G");
            Setting bS = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(theme, "Accent B");
            if (rS != null && gS != null && bS != null) {
                return new Color((int) rS.getValDouble(), (int) gS.getValDouble(), (int) bS.getValDouble());
            }
        }
        return new Color(155, 60, 255);
    }
}