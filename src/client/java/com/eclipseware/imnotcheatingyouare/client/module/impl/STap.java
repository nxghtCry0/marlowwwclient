package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import org.lwjgl.glfw.GLFW;

public class STap extends Module {
    private int phase = 0;
    private long lastPhaseTimeMs = 0L;
    private long targetDelayMs = 0L;

    public STap() {
        super("STap", Category.Combat, "Briefly taps backward key on hit to reset sprint knockback.");
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Chance (%)", this, 100.0, 0.0, 100.0, false));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Wait Delay (ms)", this, 0.0, 0.0, 500.0, true));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Action Delay (ms)", this, 150.0, 10.0, 500.0, true));
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

        Setting waitSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Wait Delay (ms)");
        long waitMs = waitSetting != null ? (long) waitSetting.getValDouble() : 0L;

        lastPhaseTimeMs = System.currentTimeMillis();
        if (waitMs <= 0) {
            phase = 2;
            if (isSilent()) {
                mc.player.setSprinting(false);
            } else {
                mc.options.keyDown.setDown(true);
            }
            Setting actionSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Action Delay (ms)");
            targetDelayMs = actionSetting != null ? (long) actionSetting.getValDouble() : 150L;
        } else {
            phase = 1;
            targetDelayMs = waitMs;
        }
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.options == null || phase == 0 || AutoTotem.shouldPauseInputs()) return;

        long now = System.currentTimeMillis();
        switch (phase) {
            case 1 -> {
                if (now - lastPhaseTimeMs >= targetDelayMs) {
                    if (isSilent()) {
                        mc.player.setSprinting(false);
                    } else {
                        mc.options.keyDown.setDown(true);
                    }
                    phase = 2;
                    lastPhaseTimeMs = now;
                    Setting actionSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Action Delay (ms)");
                    targetDelayMs = actionSetting != null ? (long) actionSetting.getValDouble() : 150L;
                }
            }
            case 2 -> {
                if (now - lastPhaseTimeMs >= targetDelayMs) {
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
        lastPhaseTimeMs = 0L;
        targetDelayMs = 0L;
    }
}
