package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.utils.cheat.AntiCheatProfile;
import org.lwjgl.glfw.GLFW;

public class WTap extends Module {

    private int phase           = 0;
    private int ticksRemaining  = 0;

    public WTap() {
        super("WTap", Category.Combat, "Releases forward key on hit to reset sprint knockback.");
    }

    private boolean isSilent() {
        Setting modeSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "WTap Mode");
        String mode = modeSetting != null ? modeSetting.getValString() : "Auto";
        if (mode.equalsIgnoreCase("Silent")) return true;
        if (mode.equalsIgnoreCase("Normal")) return false;
        return AntiCheatProfile.wtapSilentMode();
    }

    /**
     * Called from the mixin when attack() returns without being cancelled.
     * Only starts a sequence if the player is currently holding W.
     */
    public void onAttackLanded(net.minecraft.world.entity.Entity target) {
        if (!isToggled() || mc.player == null || mc.options == null) return;
        if (phase != 0) return;
        if (!mc.options.keyUp.isDown()) return;

        Setting chanceSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Chance (%)");
        double chance = chanceSetting != null ? chanceSetting.getValDouble() : 100.0;
        if (Math.random() * 100.0 > chance) return;

        Setting onlyPlayersSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Only Players");
        boolean onlyPlayers = onlyPlayersSetting != null && onlyPlayersSetting.getValBoolean();
        if (onlyPlayers && !(target instanceof net.minecraft.world.entity.player.Player)) return;

        phase = 1;
        Setting waitSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Wait Ticks");
        int base = waitSetting != null ? (int) waitSetting.getValDouble() : 0;

        Setting jitterSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Jitter Ticks");
        int jitter = jitterSetting != null ? (int) jitterSetting.getValDouble() : 1;
        ticksRemaining = base + (int) (Math.random() * (jitter + 1));
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.options == null || phase == 0) return;

        switch (phase) {
            case 1 -> {
                if (!mc.options.keyUp.isDown()) { phase = 0; return; }
                ticksRemaining--;
                if (ticksRemaining <= 0) {
                    if (isSilent()) {
                        mc.player.setSprinting(false);
                    } else {
                        mc.options.keyUp.setDown(false);
                    }
                    phase = 2;
                    Setting actionSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Action Ticks");
                    int base = actionSetting != null ? (int) actionSetting.getValDouble() : 1;
                    Setting jitterSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Jitter Ticks");
                    int jitter = jitterSetting != null ? (int) jitterSetting.getValDouble() : 1;
                    ticksRemaining = base + (int) (Math.random() * (jitter + 1));
                }
            }
            case 2 -> {
                ticksRemaining--;
                if (ticksRemaining <= 0) {
                    if (isSilent()) {
                        mc.player.setSprinting(true);
                    } else if (isPhysicallyHoldingW()) {
                        mc.options.keyUp.setDown(true);
                    }
                    phase = 0;
                }
            }
        }
    }

    public static boolean shouldSilentStopSprint() {
        if (ImnotcheatingyouareClient.INSTANCE == null || ImnotcheatingyouareClient.INSTANCE.moduleManager == null) return false;
        WTap wTap = (WTap) ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("WTap");
        if (wTap == null || !wTap.isToggled()) return false;
        if (wTap.phase != 2) return false;
        return wTap.isSilent();
    }

    private boolean isPhysicallyHoldingW() {
        long window = getWindowHandle();
        if (window == 0) return false;
        return GLFW.glfwGetKey(window, getKeyCode(mc.options.keyUp)) == GLFW.GLFW_PRESS;
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
            } else if (isPhysicallyHoldingW()) {
                mc.options.keyUp.setDown(true);
            }
        }
        phase          = 0;
        ticksRemaining = 0;
    }
}
