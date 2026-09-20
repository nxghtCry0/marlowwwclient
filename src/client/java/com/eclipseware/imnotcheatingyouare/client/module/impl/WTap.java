package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.utils.cheat.AntiCheatProfile;

public class WTap extends Module {

    private int phase = 0;
    private long lastPhaseTimeMs = 0L;
    private long targetDelayMs = 0L;

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

        Setting waitSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Wait Delay (ms)");
        long baseWait = waitSetting != null ? (long) waitSetting.getValDouble() : 0L;

        Setting jitterSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Jitter (ms)");
        long jitter = jitterSetting != null ? (long) jitterSetting.getValDouble() : 20L;

        long waitMs = baseWait + (long) (Math.random() * (jitter + 1));

        lastPhaseTimeMs = System.currentTimeMillis();
        if (waitMs <= 0) {
            phase = 2;
            if (isSilent()) {
                mc.player.setSprinting(false);
            } else {
                mc.options.keyUp.setDown(false);
            }
            Setting actionSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Action Delay (ms)");
            long baseAction = actionSetting != null ? (long) actionSetting.getValDouble() : 100L;
            targetDelayMs = baseAction + (long) (Math.random() * (jitter + 1));
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
                if (!mc.options.keyUp.isDown()) { phase = 0; return; }
                if (now - lastPhaseTimeMs >= targetDelayMs) {
                    if (isSilent()) {
                        mc.player.setSprinting(false);
                    } else {
                        mc.options.keyUp.setDown(false);
                    }
                    phase = 2;
                    lastPhaseTimeMs = now;
                    Setting actionSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Action Delay (ms)");
                    long baseAction = actionSetting != null ? (long) actionSetting.getValDouble() : 100L;
                    Setting jitterSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Jitter (ms)");
                    long jitter = jitterSetting != null ? (long) jitterSetting.getValDouble() : 20L;
                    targetDelayMs = baseAction + (long) (Math.random() * (jitter + 1));
                }
            }
            case 2 -> {
                if (now - lastPhaseTimeMs >= targetDelayMs) {
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
        return com.eclipseware.imnotcheatingyouare.client.utils.InputUtil.isDown(getKeyCode(mc.options.keyUp));
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
        phase = 0;
        lastPhaseTimeMs = 0L;
        targetDelayMs = 0L;
    }
}
