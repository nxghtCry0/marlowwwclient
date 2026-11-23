package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.module.ModuleManager;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.setting.SettingsManager;
import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class STap extends Module {
    private final SprintResetController.STapEngine engine = new SprintResetController.STapEngine();
    private static boolean sprintBeforeAttack = false;

    public STap() {
        super("STap", Category.Combat, "Releases forward and taps backward after a hit to reset sprint and space combos.");

        ArrayList<String> modes = new ArrayList<>();
        modes.add("Normal");
        modes.add("Dynamic");
        modes.add("Silent");
        modes.add("Auto");

        ImnotcheatingyouareClient client = ImnotcheatingyouareClient.INSTANCE;
        SettingsManager sm = client.settingsManager;
        sm.rSetting(new Setting("STap Mode", this, "Normal", modes));
        sm.rSetting(new Setting("Chance (%)", this, 100.0, 0.0, 100.0, false));
        sm.rSetting(new Setting("Wait Ticks", this, 0.0, 0.0, 6.0, true));
        sm.rSetting(new Setting("Hold Ticks", this, 2.0, 1.0, 8.0, true));
        sm.rSetting(new Setting("Random Ticks", this, 0.0, 0.0, 3.0, true));
        sm.rSetting(new Setting("Cooldown Ticks", this, 2.0, 0.0, 20.0, true));
        sm.rSetting(new Setting("Fresh Hits Only", this, false));
        sm.rSetting(new Setting("Only Players", this, true));
        sm.rSetting(new Setting("Only On Ground", this, false));
        sm.rSetting(new Setting("Only Forward", this, true));
        sm.rSetting(new Setting("Sprint Only", this, true));
        sm.rSetting(new Setting("Distance Max (m)", this, 4.5, 1.0, 6.0, false));
    }

    public static void captureSprint(boolean sprinting) {
        sprintBeforeAttack = sprinting;
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

    private String mode() {
        Setting s = setting("STap Mode");
        return s != null ? s.getValString() : "Normal";
    }

    private boolean isSilentMode() {
        return "Silent".equalsIgnoreCase(mode());
    }

    private int jitter() {
        int r = (int) Math.round(num("Random Ticks", 0.0));
        if (r <= 0) {
            return 0;
        }
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        return rng.nextInt(r + 1);
    }

    public boolean isActive() {
        return engine.phase() != SprintResetController.TapPhase.IDLE;
    }

    public boolean isBackwardTapActive() {
        return engine.isBackwardTapActive();
    }

    public boolean isSuppressingSprint() {
        return engine.isSuppressingSprint();
    }

    public void onAttackLanded(Entity target) {
        boolean sprinting = sprintBeforeAttack || (mc != null && mc.player != null && mc.player.isSprinting());
        float strength = mc != null && mc.player != null ? mc.player.getAttackStrengthScale(0.5f) : 1.0f;
        tryTriggerOnAttack(target, sprinting, strength);
    }

    public boolean tryTriggerOnAttack(Entity target, boolean preAttackSprinting, float preAttackStrength) {
        if (!isToggled() || mc == null || mc.player == null || mc.options == null) {
            return false;
        }
        if (mc.gui.screen() != null || isActive() || engine.cooldownTicks() > 0) {
            return false;
        }

        var opts = mc.options;
        boolean holdingForward = SprintResetController.isPhysicalKeyDown(opts.keyUp) || opts.keyUp.isDown();
        if (bool("Only Forward", true) && !holdingForward) {
            return false;
        }
        boolean wasSprint = preAttackSprinting || sprintBeforeAttack || mc.player.isSprinting();
        if (bool("Sprint Only", true) && !wasSprint) {
            return false;
        }
        if (bool("Only On Ground", false) && !mc.player.onGround()) {
            return false;
        }
        if (bool("Only Players", true) && !(target instanceof Player)) {
            return false;
        }
        if (target != null && mc.player.distanceTo(target) > num("Distance Max (m)", 4.5)) {
            return false;
        }
        if (bool("Fresh Hits Only", false)) {
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

        int baseWait = Math.max(0, (int) Math.round(num("Wait Ticks", 0.0)));
        int wait = baseWait > 0 ? baseWait + jitter() : 0;
        int hold = Math.max(1, (int) Math.round(num("Hold Ticks", 2.0)) + jitter());
        String curMode = mode();
        if (("Dynamic".equalsIgnoreCase(curMode) || "Auto".equalsIgnoreCase(curMode)) && target != null) {
            double dist = mc.player.distanceTo(target);
            if (dist < 2.4) {
                hold += 1;
            } else if (dist > 3.4) {
                hold = Math.max(1, hold - 1);
            }
        }

        boolean silent = isSilentMode();
        boolean started = engine.trigger(wait, hold, silent, wasSprint);
        if (started && engine.phase() == SprintResetController.TapPhase.ACTIVE) {
            applyTapKeys(silent);
        }
        return started;
    }

    private void applyTapKeys(boolean silent) {
        if (mc == null || mc.options == null || mc.player == null) {
            return;
        }
        if (silent) {
            mc.player.setSprinting(false);
        } else {
            var opts = mc.options;
            opts.keyUp.setDown(false);
            opts.keyDown.setDown(true);
            SprintResetController.releaseSprintKeySafely(mc);
        }
    }

    @Override
    public void onTick() {
        if (mc == null || mc.player == null || mc.options == null) {
            return;
        }
        if (mc.gui.screen() != null || AutoTotem.shouldPauseInputs()) {
            abortAndRestore();
            return;
        }
        if (!isActive() && engine.cooldownTicks() <= 0) {
            return;
        }

        boolean wasActive = engine.phase() == SprintResetController.TapPhase.ACTIVE;
        boolean wasSilent = engine.isSilentActive();
        boolean physicalForward = SprintResetController.isPhysicalKeyDown(mc.options.keyUp);
        int cooldown = Math.max(0, (int) Math.round(num("Cooldown Ticks", 2.0)));

        boolean shouldReSprint = engine.stepTick(physicalForward, bool("Only Forward", true), cooldown);
        if (engine.phase() == SprintResetController.TapPhase.ACTIVE) {
            applyTapKeys(engine.isSilentActive());
            return;
        }

        if (wasActive && engine.phase() == SprintResetController.TapPhase.IDLE) {
            if (wasSilent) {
                if (physicalForward && !mc.player.isShiftKeyDown()) {
                    mc.player.setSprinting(true);
                }
            } else {
                var opts = mc.options;
                SprintResetController.restorePhysicalKey(opts.keyUp);
                SprintResetController.restorePhysicalKey(opts.keyDown);
                if (opts.keyUp.isDown() && (shouldReSprint || SprintResetController.isAutoSprintEnabled())) {
                    SprintResetController.requestReSprintPulse(mc);
                }
            }
        }
    }

    public void abortAndRestore() {
        boolean wasBackward = engine.isBackwardTapActive();
        boolean wasSilent = engine.isSilentActive();
        engine.reset();
        if (mc == null || mc.options == null) {
            return;
        }
        if (wasBackward) {
            SprintResetController.restorePhysicalKey(mc.options.keyUp);
            SprintResetController.restorePhysicalKey(mc.options.keyDown);
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
        Module mod = mm.getModule("STap");
        return mod instanceof STap sTap && sTap.isToggled() && sTap.engine.isSilentActive();
    }

    @Override
    public void onDisable() {
        abortAndRestore();
    }
}
