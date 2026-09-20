package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.utils.cheat.AntiCheatProfile;

import java.util.ArrayList;

public class STap extends Module {
    private int phase = 0; // 0: Idle, 1: Waiting before tap, 2: Tapping (holding S / silent stop)
    private long lastPhaseTimeMs = 0L;
    private long targetDelayMs = 0L;

    public STap() {
        super("STap", Category.Combat, "Briefly taps backward key on hit to reset sprint knockback.");

        ArrayList<String> modes = new ArrayList<>();
        modes.add("Normal");
        modes.add("Silent");
        modes.add("Dynamic");
        modes.add("Auto");
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("STap Mode", this, "Normal", modes));

        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Chance (%)", this, 100.0, 0.0, 100.0, false));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Wait Delay (ms)", this, 0.0, 0.0, 300.0, true));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Action Delay (ms)", this, 90.0, 20.0, 400.0, true));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Jitter (ms)", this, 20.0, 0.0, 80.0, true));

        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Only Players", this, true));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Only On Ground", this, false));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Only Forward", this, true));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Sprint Only", this, true));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Distance Max (m)", this, 4.5, 1.0, 6.0, false));
    }

    private boolean isSilent() {
        Setting modeSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "STap Mode");
        String mode = modeSetting != null ? modeSetting.getValString() : "Normal";
        if (mode.equalsIgnoreCase("Silent") || mode.equalsIgnoreCase("Dynamic")) return true;
        if (mode.equalsIgnoreCase("Normal")) return false;
        return AntiCheatProfile.wtapSilentMode();
    }

    public void onAttackLanded(net.minecraft.world.entity.Entity target) {
        if (!isToggled() || mc.player == null || mc.options == null) return;
        if (phase != 0) return;

        Setting onlyForwardSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Only Forward");
        if ((onlyForwardSetting == null || onlyForwardSetting.getValBoolean()) && !mc.options.keyUp.isDown()) {
            return;
        }

        Setting sprintOnlySetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Sprint Only");
        if ((sprintOnlySetting != null && sprintOnlySetting.getValBoolean()) && !mc.player.isSprinting()) {
            return;
        }

        Setting onGroundSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Only On Ground");
        if (onGroundSetting != null && onGroundSetting.getValBoolean() && !mc.player.onGround()) {
            return;
        }

        Setting onlyPlayersSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Only Players");
        if (onlyPlayersSetting != null && onlyPlayersSetting.getValBoolean() && !(target instanceof net.minecraft.world.entity.player.Player)) {
            return;
        }

        if (target != null) {
            Setting distSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Distance Max (m)");
            double maxDist = distSetting != null ? distSetting.getValDouble() : 4.5;
            if (mc.player.distanceTo(target) > maxDist) return;
        }

        Setting chanceSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Chance (%)");
        double chance = chanceSetting != null ? chanceSetting.getValDouble() : 100.0;
        if (Math.random() * 100.0 > chance) return;

        Setting waitSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Wait Delay (ms)");
        long baseWait = waitSetting != null ? (long) waitSetting.getValDouble() : 0L;

        Setting jitterSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Jitter (ms)");
        long jitter = jitterSetting != null ? (long) jitterSetting.getValDouble() : 20L;

        long waitMs = baseWait + (jitter > 0 ? (long) (Math.random() * (jitter + 1)) : 0L);

        Setting actionSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Action Delay (ms)");
        long baseAction = actionSetting != null ? (long) actionSetting.getValDouble() : 90L;

        Setting modeSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "STap Mode");
        String mode = modeSetting != null ? modeSetting.getValString() : "Normal";
        if (mode.equalsIgnoreCase("Dynamic") && target != null) {
            double dist = mc.player.distanceTo(target);
            if (dist < 2.5) {
                baseAction = Math.min(220L, (long)(baseAction * 1.35));
            } else if (dist > 3.6) {
                baseAction = Math.max(35L, (long)(baseAction * 0.75));
            }
        }

        long actionMs = baseAction + (jitter > 0 ? (long) (Math.random() * (jitter + 1)) : 0L);

        lastPhaseTimeMs = System.currentTimeMillis();
        if (waitMs <= 0) {
            phase = 2;
            if (isSilent()) {
                mc.player.setSprinting(false);
            } else {
                mc.options.keyDown.setDown(true);
            }
            targetDelayMs = actionMs;
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
                    long baseAction = actionSetting != null ? (long) actionSetting.getValDouble() : 90L;
                    Setting jitterSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Jitter (ms)");
                    long jitter = jitterSetting != null ? (long) jitterSetting.getValDouble() : 20L;
                    targetDelayMs = baseAction + (jitter > 0 ? (long) (Math.random() * (jitter + 1)) : 0L);
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

    public static boolean shouldSilentStopSprint() {
        if (ImnotcheatingyouareClient.INSTANCE == null || ImnotcheatingyouareClient.INSTANCE.moduleManager == null) return false;
        STap sTap = (STap) ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("STap");
        if (sTap == null || !sTap.isToggled()) return false;
        if (sTap.phase != 2) return false;
        return sTap.isSilent();
    }

    private boolean isPhysicallyHoldingS() {
        return com.eclipseware.imnotcheatingyouare.client.utils.InputUtil.isDown(getKeyCode(mc.options.keyDown));
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
