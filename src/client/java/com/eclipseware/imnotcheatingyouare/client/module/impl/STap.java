package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.utils.cheat.AntiCheatProfile;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

public class STap extends Module {
    private enum Phase { IDLE, WAITING, TAPPING }

    private Phase phase = Phase.IDLE;
    private int phaseTicks = 0;
    private int cooldownTicks = 0;
    private boolean silentActive = false;
    private boolean wasSprinting = false;
    private boolean pressedBack = false;

    public STap() {
        super("STap", Category.Combat, "Taps backward right after a hit to reset sprint and control knockback spacing.");

        ArrayList<String> modes = new ArrayList<>();
        modes.add("Normal");
        modes.add("Silent");
        modes.add("Dynamic");
        modes.add("Auto");
        var sm = ImnotcheatingyouareClient.INSTANCE.settingsManager;
        sm.rSetting(new Setting("STap Mode", this, "Normal", modes));
        sm.rSetting(new Setting("Chance (%)", this, 100.0, 0.0, 100.0, false));
        sm.rSetting(new Setting("Wait Ticks", this, 0.0, 0.0, 6.0, true));
        sm.rSetting(new Setting("Hold Ticks", this, 2.0, 1.0, 8.0, true));
        sm.rSetting(new Setting("Random Ticks", this, 1.0, 0.0, 3.0, true));
        sm.rSetting(new Setting("Cooldown Ticks", this, 2.0, 0.0, 20.0, true));
        sm.rSetting(new Setting("Fresh Hits Only", this, false));
        sm.rSetting(new Setting("Only Players", this, true));
        sm.rSetting(new Setting("Only On Ground", this, false));
        sm.rSetting(new Setting("Only Forward", this, true));
        sm.rSetting(new Setting("Sprint Only", this, true));
        sm.rSetting(new Setting("Distance Max (m)", this, 4.5, 1.0, 6.0, false));
    }

    private Setting setting(String name) {
        return ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, name);
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

    private boolean isSilent() {
        String mode = mode();
        if (mode.equalsIgnoreCase("Silent") || mode.equalsIgnoreCase("Dynamic")) return true;
        if (mode.equalsIgnoreCase("Normal")) return false;
        return AntiCheatProfile.wtapSilentMode();
    }

    private int jitter() {
        int r = (int) num("Random Ticks", 1.0);
        return r > 0 ? ThreadLocalRandom.current().nextInt(r + 1) : 0;
    }

    public void onAttackLanded(Entity target) {
        if (!isToggled() || mc.player == null || mc.options == null) return;
        if (phase != Phase.IDLE || cooldownTicks > 0) return;

        if (bool("Only Forward", true) && !mc.options.keyUp.isDown()) return;
        if (bool("Sprint Only", true) && !sprintBeforeAttack && !mc.player.isSprinting()) return;
        if (bool("Only On Ground", false) && !mc.player.onGround()) return;
        if (bool("Only Players", true) && !(target instanceof Player)) return;
        if (target != null && mc.player.distanceTo(target) > num("Distance Max (m)", 4.5)) return;
        if (bool("Fresh Hits Only", false) && target instanceof LivingEntity le && le.hurtTime > 3) return;
        if (ThreadLocalRandom.current().nextDouble(100.0) >= num("Chance (%)", 100.0)) return;

        int wait = (int) num("Wait Ticks", 0.0) + jitter();
        if (wait <= 0) {
            startTap(target);
        } else {
            phase = Phase.WAITING;
            phaseTicks = wait;
            pendingTarget = target;
        }
    }

    private Entity pendingTarget;
    private static boolean sprintBeforeAttack;

    public static void captureSprint(boolean sprinting) {
        sprintBeforeAttack = sprinting;
    }

    private void startTap(Entity target) {
        int hold = (int) num("Hold Ticks", 2.0) + jitter();
        if (mode().equalsIgnoreCase("Dynamic") && target != null) {
            double dist = mc.player.distanceTo(target);
            if (dist < 2.4) hold += 1;
            else if (dist > 3.4) hold = Math.max(1, hold - 1);
        }

        wasSprinting = sprintBeforeAttack || mc.player.isSprinting();
        if (isSilent()) {
            silentActive = true;
            mc.player.setSprinting(false);
        } else {
            pressedBack = true;
            mc.options.keyDown.setDown(true);
        }
        phase = Phase.TAPPING;
        phaseTicks = Math.max(1, hold);
    }

    private void endTap() {
        if (silentActive) {
            silentActive = false;
            if (mc.player != null && wasSprinting && mc.options.keyUp.isDown() && !mc.player.isShiftKeyDown()) {
                mc.player.setSprinting(true);
            }
        }
        if (pressedBack) {
            pressedBack = false;
            mc.options.keyDown.setDown(isPhysicallyDown(mc.options.keyDown));
        }
        phase = Phase.IDLE;
        pendingTarget = null;
        cooldownTicks = (int) num("Cooldown Ticks", 2.0);
    }

    @Override
    public void onTick() {
        if (cooldownTicks > 0) cooldownTicks--;
        if (mc.player == null || mc.options == null || phase == Phase.IDLE) return;
        if (AutoTotem.shouldPauseInputs()) return;

        if (phase == Phase.WAITING) {
            if (--phaseTicks <= 0) startTap(pendingTarget);
            return;
        }

        if (!mc.options.keyUp.isDown() && bool("Only Forward", true)) {
            endTap();
            return;
        }
        if (--phaseTicks <= 0) endTap();
    }

    public static boolean shouldSilentStopSprint() {
        if (ImnotcheatingyouareClient.INSTANCE == null || ImnotcheatingyouareClient.INSTANCE.moduleManager == null) return false;
        if (!(ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("STap") instanceof STap sTap) || !sTap.isToggled()) return false;
        return sTap.silentActive;
    }

    private boolean isPhysicallyDown(KeyMapping mapping) {
        try {
            InputConstants.Key key = InputConstants.getKey(mapping.saveString());
            if (key.getType() == InputConstants.Type.MOUSE) {
                return com.eclipseware.imnotcheatingyouare.client.utils.InputUtil.isMouseButtonDown(key.getValue());
            }
            return com.eclipseware.imnotcheatingyouare.client.utils.InputUtil.isDown(key.getValue());
        } catch (Exception e) {
            return mapping.isDown();
        }
    }

    @Override
    public void onDisable() {
        if (phase == Phase.TAPPING && mc.options != null) endTap();
        phase = Phase.IDLE;
        phaseTicks = 0;
        cooldownTicks = 0;
        pendingTarget = null;
    }
}
