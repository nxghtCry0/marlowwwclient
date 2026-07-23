package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import net.minecraft.world.phys.AABB;

public class BridgeAssist extends Module {
    private boolean isShifting = false;
    public BridgeAssist() {
        super("BridgeAssist", Category.Utility, "Uses Meteor's SafeWalk logic to perfectly sneak at edges.");
    }

    @Override
    public void onTick() {
        if (mc == null || mc.player == null || mc.level == null) return;

        Setting modeSet = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Mode");
        if (modeSet != null && modeSet.getValString().equals("Blatant")) {
            unShift();
            return;
        }

        Setting pitchCheck = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Pitch Check");
        if (pitchCheck != null && pitchCheck.getValBoolean() && mc.player.getXRot() < 45.0f) {
            unShift();
            return;
        }

        Setting requireSneakSet = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Require Sneak");
        if (requireSneakSet != null && requireSneakSet.getValBoolean() && !isPhysicallyHoldingSneak()) {
            unShift();
            return;
        }

        Setting edgeSet = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Edge Distance");
        double edgeDistance = edgeSet != null ? edgeSet.getValDouble() : 0.25;

        AABB playerBox = mc.player.getBoundingBox();
        
        AABB adjustedBox = playerBox
            .expandTowards(0, -mc.player.maxUpStep(), 0)
            .inflate(-edgeDistance, 0, -edgeDistance);

        boolean closeToEdge = mc.level.noCollision(mc.player, adjustedBox) && mc.player.onGround();

        if (closeToEdge) {
            mc.options.keyShift.setDown(true);
            isShifting = true;
        } else {
            unShift();
        }
    }

    private void unShift() {
        if (isShifting) {
            mc.options.keyShift.setDown(false);
            isShifting = false;
        }
    }

    private boolean isPhysicallyHoldingSneak() {
        long window = getWindowHandle();
        if (window == 0) return false;
        return org.lwjgl.glfw.GLFW.glfwGetKey(window, getKeyCode(mc.options.keyShift)) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
    }

    private int getKeyCode(net.minecraft.client.KeyMapping mapping) {
        try {
            for (java.lang.reflect.Method m : mapping.getClass().getMethods()) {
                if (m.getParameterCount() == 0 && m.getReturnType().getName().contains("InputConstants$Key")) {
                    Object keyObj = m.invoke(mapping);
                    java.lang.reflect.Method getValue = keyObj.getClass().getMethod("getValue");
                    return (int) getValue.invoke(keyObj);
                }
            }
        } catch (Exception ignored) {}
        return mapping.getDefaultKey().getValue();
    }

    private long getWindowHandle() {
        try {
            for (java.lang.reflect.Field f : mc.getWindow().getClass().getDeclaredFields()) {
                if (f.getType() == long.class) {
                    f.setAccessible(true);
                    return f.getLong(mc.getWindow());
                }
            }
        } catch (Exception ignored) {}
        return 0;
    }

    @Override
    public void onDisable() {
        unShift();
    }
}