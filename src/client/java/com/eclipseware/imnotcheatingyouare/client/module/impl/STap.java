package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.utils.cheat.AntiCheatProfile;
import org.lwjgl.glfw.GLFW;

public class STap extends Module {
    private int phase = 0;
    private int ticksRemaining = 0;

    public STap() {
        super("STap", Category.Combat, "Briefly taps backward key on hit to reset sprint knockback.");
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Chance (%)", this, 100.0, 0.0, 100.0, false));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Wait Ticks", this, 0.0, 0.0, 5.0, true));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Action Ticks", this, 5.0, 1.0, 10.0, true));
        java.util.ArrayList<String> modes = new java.util.ArrayList<>();
        modes.add("Normal");
        modes.add("Silent");
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("STap Mode", this, "Normal", modes));
    }

    private boolean isSilent() {
        Setting modeSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "STap Mode");
        String mode = modeSetting != null ? modeSetting.getValString() : "Normal";
        return mode.equalsIgnoreCase("Silent");
    }

    public void onAttackLanded(net.minecraft.world.entity.Entity target) {
        if (!isToggled() || mc.player == null || mc.options == null) return;
        if (phase != 0) return;
        if (!mc.options.keyUp.isDown()) return;

        Setting chanceSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Chance (%)");
        double chance = chanceSetting != null ? chanceSetting.getValDouble() : 100.0;
        if (Math.random() * 100.0 > chance) return;

        phase = 1;
        Setting waitSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Wait Ticks");
        ticksRemaining = waitSetting != null ? (int) waitSetting.getValDouble() : 0;
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.options == null || phase == 0 || AutoTotem.shouldPauseInputs()) return;

        switch (phase) {
            case 1 -> {
                ticksRemaining--;
                if (ticksRemaining <= 0) {
                    if (isSilent()) {
                        mc.player.setSprinting(false);
                    } else {
                        mc.options.keyDown.setDown(true);
                    }
                    phase = 2;
                    Setting actionSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Action Ticks");
                    ticksRemaining = actionSetting != null ? (int) actionSetting.getValDouble() : 5;
                }
            }
            case 2 -> {
                ticksRemaining--;
                if (ticksRemaining <= 0) {
                    if (isSilent()) {
                        mc.player.setSprinting(true);
                    } else if (!isPhysicallyHoldingS()) {
                        mc.options.keyDown.setDown(false);
                    }
                    phase = 0;
                }
            }
        }
    }

    private boolean isPhysicallyHoldingS() {
        long window = getWindowHandle();
        if (window == 0) return false;
        return GLFW.glfwGetKey(window, getKeyCode(mc.options.keyDown)) == GLFW.GLFW_PRESS;
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
        if (phase == 2 && mc.options != null) {
            if (isSilent()) {
                if (mc.player != null) {
                    mc.player.setSprinting(true);
                }
            } else if (!isPhysicallyHoldingS()) {
                mc.options.keyDown.setDown(false);
            }
        }
        phase = 0;
        ticksRemaining = 0;
    }
}
