package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.module.ModuleManager;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.setting.SettingsManager;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class WTap extends Module {
    private final SprintResetController.WTapEngine engine = new SprintResetController.WTapEngine();

    public WTap() {
        super("WTap", Category.Combat, "Releases forward input on hit and pulses re-sprint for maximum knockback.");
    }

    private Setting setting(String name) {
        ImnotcheatingyouareClient client = ImnotcheatingyouareClient.INSTANCE;
        if (client == null || client.settingsManager == null) {
            return null;
        }
        SettingsManager sm = client.settingsManager;
        return sm.getSettingByName(this, name);
    }

    private boolean bool(String name, boolean fallback) {
        Setting s = setting(name);
        return s != null ? s.getValBoolean() : fallback;
    }

    private double num(String name, double fallback) {
        Setting s = setting(name);
        return s != null ? s.getValDouble() : fallback;
    }

    private boolean isSilentMode() {
        Setting modeSetting = setting("WTap Mode");
        String mode = modeSetting != null ? modeSetting.getValString() : "Normal";
        return "Silent".equalsIgnoreCase(mode);
    }

    public boolean isActive() {
        return engine.phase() != SprintResetController.TapPhase.IDLE;
    }

    public boolean isNormalReleaseActive() {
        return engine.isNormalReleaseActive();
    }

    public boolean isSuppressingSprint() {
        return engine.isSuppressingSprint();
    }

    public void onAttackLanded(Entity target) {
        boolean sprinting = mc != null && mc.player != null && mc.player.isSprinting();
        float strength = mc != null && mc.player != null ? mc.player.getAttackStrengthScale(0.5f) : 1.0f;
        tryTriggerOnAttack(target, sprinting, strength);
    }

    public boolean tryTriggerOnAttack(Entity target, boolean preAttackSprinting, float preAttackStrength) {
        if (!isToggled() || mc == null || mc.player == null || mc.options == null) {
            return false;
        }
        if (mc.gui.screen() != null || isActive()) {
            return false;
        }
        var opts = mc.options;
        boolean holdingForward = SprintResetController.isPhysicalKeyDown(opts.keyUp) || opts.keyUp.isDown();
        if (!holdingForward) {
            return false;
        }
        if (bool("Only Players", true) && !(target instanceof Player)) {
            return false;
        }
        boolean sprintingNow = preAttackSprinting || mc.player.isSprinting();
        if (bool("Sprint Only", true) && !sprintingNow) {
            return false;
        }
        if (bool("Fresh Hits Only", true)) {
            if (preAttackStrength < 0.70f) {
                return false;
            }
            if (target instanceof LivingEntity le && le.hurtTime > 4) {
                return false;
            }
        }

        double chance = num("Chance (%)", 100.0);
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        if (chance < 99.9 && (chance <= 0.0 || rng.nextDouble(100.0) > chance)) {
            return false;
        }

        long baseWait = Math.max(0L, Math.round(num("Wait Delay (ms)", 0.0)));
        long baseAction = Math.max(20L, Math.round(num("Action Delay (ms)", 85.0)));
        long jitter = Math.max(0L, Math.round(num("Jitter (ms)", 15.0)));
        long waitJitter = (baseWait > 0L && jitter > 0L) ? rng.nextLong(jitter + 1L) : 0L;
        long holdJitter = jitter > 0L ? rng.nextLong(jitter + 1L) : 0L;

        boolean silent = isSilentMode();
        long now = System.currentTimeMillis();
        boolean started = engine.trigger(now, baseWait + waitJitter, baseAction + holdJitter, silent, sprintingNow);
        if (started && engine.phase() == SprintResetController.TapPhase.ACTIVE) {
            applyActiveState(silent);
        }
        return started;
    }

    private void applyActiveState(boolean silent) {
        if (mc == null || mc.options == null || mc.player == null) {
            return;
        }
        if (silent) {
            mc.player.setSprinting(false);
        } else {
            var opts = mc.options;
            opts.keyUp.setDown(false);
            SprintResetController.releaseSprintKeySafely(mc);
        }
    }

    @Override
    public void onTick() {
        if (!isActive()) {
            return;
        }
        if (mc == null || mc.player == null || mc.options == null || mc.gui.screen() != null || AutoTotem.shouldPauseInputs()) {
            abortAndRestore();
            return;
        }

        boolean wasWaiting = engine.phase() == SprintResetController.TapPhase.WAITING;
        boolean wasSilent = engine.isSilentActive();
        boolean physicalForward = SprintResetController.isPhysicalKeyDown(mc.options.keyUp);
        long now = System.currentTimeMillis();

        boolean shouldReSprint = engine.stepTick(now, physicalForward);
        if (engine.phase() == SprintResetController.TapPhase.ACTIVE) {
            if (wasWaiting) {
                applyActiveState(engine.isSilentActive());
            } else if (!engine.isSilentActive()) {
                var opts = mc.options;
                opts.keyUp.setDown(false);
                SprintResetController.releaseSprintKeySafely(mc);
            } else {
                mc.player.setSprinting(false);
            }
            return;
        }

        if (engine.phase() == SprintResetController.TapPhase.IDLE) {
            if (wasSilent) {
                if (physicalForward && !mc.player.isShiftKeyDown()) {
                    mc.player.setSprinting(true);
                }
            } else {
                var opts = mc.options;
                SprintResetController.restorePhysicalKey(opts.keyUp);
                if (opts.keyUp.isDown() && (shouldReSprint || SprintResetController.isAutoSprintEnabled())) {
                    SprintResetController.requestReSprintPulse(mc);
                }
            }
        }
    }

    public void abortAndRestore() {
        boolean wasSilent = engine.isSilentActive();
        boolean wasNormal = engine.isNormalReleaseActive();
        engine.reset();
        if (mc == null || mc.options == null) {
            return;
        }
        if (wasNormal) {
            SprintResetController.restorePhysicalKey(mc.options.keyUp);
        } else if (wasSilent && mc.player != null && SprintResetController.isPhysicalKeyDown(mc.options.keyUp)) {
            mc.player.setSprinting(true);
        }
    }

    public static boolean shouldSilentStopSprint() {
        ImnotcheatingyouareClient client = ImnotcheatingyouareClient.INSTANCE;
        if (client == null || client.moduleManager == null) {
            return false;
        }
        ModuleManager mm = client.moduleManager;
        Module mod = mm.getModule("WTap");
        return mod instanceof WTap wTap && wTap.isToggled() && wTap.engine.isSilentActive();
    }

    @Override
    public void onDisable() {
        abortAndRestore();
    }
}
