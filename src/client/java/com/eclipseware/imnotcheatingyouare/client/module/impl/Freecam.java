package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.utils.RenderUtils;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

import java.awt.Color;

public class Freecam extends Module {
    private Vec3 savedPos;
    private float savedYaw, savedPitch;
    private RemotePlayer dummy;
    private float lastHealth = -1.0f;

    public final Vector3d pos = new Vector3d();
    public final Vector3d prevPos = new Vector3d();
    public float yaw, pitch;
    public float lastYaw, lastPitch;

    public Freecam() {
        super("Freecam", Category.Render, "Leaves your body behind while your camera flies freely.");
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Flight Speed", this, 1.0, 0.1, 5.0, false));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Auto Disable on Damage", this, true));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Show Server Position", this, true));
    }

    @Override
    public void onEnable() {
        if (mc.player == null || mc.level == null) return;
        savedPos = mc.player.position();
        savedYaw = mc.player.getYRot();
        savedPitch = mc.player.getXRot();
        lastHealth = mc.player.getHealth();

        pos.set(mc.gameRenderer.mainCamera().position().x, mc.gameRenderer.mainCamera().position().y, mc.gameRenderer.mainCamera().position().z);
        prevPos.set(pos);
        yaw = savedYaw;
        pitch = savedPitch;
        lastYaw = yaw;
        lastPitch = pitch;

        dummy = new RemotePlayer(mc.level, mc.player.getGameProfile());
        dummy.setPos(savedPos);
        dummy.setYRot(savedYaw);
        dummy.setXRot(savedPitch);
        dummy.setYHeadRot(mc.player.getYHeadRot());
        dummy.getInventory().replaceWith(mc.player.getInventory());

        mc.level.addEntity(dummy);

        mc.player.getAbilities().flying = true;
        mc.player.noPhysics = true;
    }

    @Override
    public void onTick() {
        if (!isToggled() || mc.player == null) return;

        Setting autoDisable = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Auto Disable on Damage");
        if (autoDisable != null && autoDisable.getValBoolean()) {
            float currentHealth = mc.player.getHealth();
            if (lastHealth >= 0.0f && currentHealth < lastHealth) {
                this.toggle();
                return;
            }
            lastHealth = currentHealth;
        }

        mc.player.noPhysics = true;
        mc.player.getAbilities().flying = true;
        mc.player.setOnGround(false);

        Setting speedSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Flight Speed");
        double speed = speedSetting != null ? speedSetting.getValDouble() : 1.0;

        double radYaw = Math.toRadians(mc.player.getYRot());
        double forward = 0;
        double strafe = 0;
        double up = 0;

        if (mc.options.keyUp.isDown()) forward += 1;
        if (mc.options.keyDown.isDown()) forward -= 1;
        if (mc.options.keyLeft.isDown()) strafe += 1;
        if (mc.options.keyRight.isDown()) strafe -= 1;
        if (mc.options.keyJump.isDown()) up += 1;
        if (mc.options.keyShift.isDown()) up -= 1;

        double dx = (forward * -Math.sin(radYaw) + strafe * Math.cos(radYaw)) * speed * 0.5;
        double dz = (forward * Math.cos(radYaw) + strafe * Math.sin(radYaw)) * speed * 0.5;
        double dy = up * speed * 0.5;

        prevPos.set(pos);
        pos.set(pos.x + dx, pos.y + dy, pos.z + dz);

        mc.player.setPos(pos.x, pos.y, pos.z);
        mc.player.setDeltaMovement(0, 0, 0);
    }

    @Override
    public void onRenderHUD(GuiGraphicsExtractor guiGraphics, Object tickDeltaObj) {
        if (!isToggled() || mc.player == null || savedPos == null) return;

        Setting showServerPos = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Show Server Position");
        if (showServerPos != null && showServerPos.getValBoolean()) {
            float partialTick = getTickDelta(tickDeltaObj);
            float width = 0.6f;
            float height = 1.8f;
            double minX = savedPos.x - width / 2.0;
            double minY = savedPos.y;
            double minZ = savedPos.z - width / 2.0;
            double maxX = savedPos.x + width / 2.0;
            double maxY = savedPos.y + height;
            double maxZ = savedPos.z + width / 2.0;

            Color themeColor = RenderUtils.getThemeAccentColor();
            Color fillColor = new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), 60);
            Color outlineColor = new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), 200);

            RenderUtils.draw3DBox(guiGraphics, minX, minY, minZ, maxX, maxY, maxZ, fillColor, outlineColor, partialTick);
        }
    }

    @Override
    public void onDisable() {
        if (mc.player == null || mc.level == null) return;
       
        if (savedPos != null) {
            mc.player.setPos(savedPos);
            mc.player.setYRot(savedYaw);
            mc.player.setXRot(savedPitch);
        }
        mc.player.getAbilities().flying = false;
        mc.player.noPhysics = false;
        mc.player.setDeltaMovement(0, 0, 0);

        if (dummy != null) {
            mc.level.removeEntity(dummy.getId(), net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
            dummy = null;
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