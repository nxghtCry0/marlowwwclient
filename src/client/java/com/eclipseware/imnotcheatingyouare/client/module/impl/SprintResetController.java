package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.module.ModuleManager;
import com.eclipseware.imnotcheatingyouare.client.utils.InputUtil;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.Options;
import net.minecraft.client.OptionInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class SprintResetController {
    public enum TapPhase {
        IDLE,
        WAITING,
        ACTIVE
    }

    public static final class WTapEngine {
        private TapPhase phase = TapPhase.IDLE;
        private long phaseStartMs = 0L;
        private long waitMs = 0L;
        private long holdMs = 85L;
        private int minTicksRemaining = 0;
        private boolean silentMode = false;
        private boolean needReSprint = false;

        public TapPhase phase() {
            return phase;
        }

        public boolean isSilentActive() {
            return phase == TapPhase.ACTIVE && silentMode;
        }

        public boolean isNormalReleaseActive() {
            return phase == TapPhase.ACTIVE && !silentMode;
        }

        public boolean isSuppressingSprint() {
            return phase == TapPhase.ACTIVE;
        }

        public void reset() {
            phase = TapPhase.IDLE;
            phaseStartMs = 0L;
            waitMs = 0L;
            holdMs = 85L;
            minTicksRemaining = 0;
            silentMode = false;
            needReSprint = false;
        }

        public boolean trigger(long nowMs, long waitDelayMs, long holdDelayMs, boolean silent, boolean wasSprinting) {
            if (phase != TapPhase.IDLE) {
                return false;
            }
            this.silentMode = silent;
            this.needReSprint = wasSprinting;
            this.waitMs = Math.max(0L, waitDelayMs);
            this.holdMs = Math.max(20L, holdDelayMs);
            this.phaseStartMs = nowMs;
            if (this.waitMs <= 0L) {
                enterActive(nowMs);
            } else {
                this.phase = TapPhase.WAITING;
            }
            return true;
        }

        private void enterActive(long nowMs) {
            this.phase = TapPhase.ACTIVE;
            this.phaseStartMs = nowMs;
            this.minTicksRemaining = Math.max(1, Math.round(this.holdMs / 50f));
        }

        public boolean stepTick(long nowMs, boolean physicallyHoldingForward) {
            if (phase == TapPhase.IDLE) {
                return false;
            }
            if (phase == TapPhase.WAITING) {
                if (!physicallyHoldingForward) {
                    reset();
                    return false;
                }
                if (nowMs - phaseStartMs >= waitMs) {
                    enterActive(nowMs);
                }
                return false;
            }
            if (minTicksRemaining > 0) {
                minTicksRemaining--;
            }
            if (minTicksRemaining <= 0 && nowMs - phaseStartMs >= holdMs) {
                boolean shouldReSprint = needReSprint && physicallyHoldingForward;
                reset();
                return shouldReSprint;
            }
            return false;
        }
    }

    public static final class STapEngine {
        private TapPhase phase = TapPhase.IDLE;
        private int phaseTicks = 0;
        private int holdTicks = 2;
        private int cooldownTicks = 0;
        private boolean silentMode = false;
        private boolean needReSprint = false;

        public TapPhase phase() {
            return phase;
        }

        public int cooldownTicks() {
            return cooldownTicks;
        }

        public boolean isSilentActive() {
            return phase == TapPhase.ACTIVE && silentMode;
        }

        public boolean isBackwardTapActive() {
            return phase == TapPhase.ACTIVE && !silentMode;
        }

        public boolean isSuppressingSprint() {
            return phase == TapPhase.ACTIVE;
        }

        public void reset() {
            phase = TapPhase.IDLE;
            phaseTicks = 0;
            cooldownTicks = 0;
            silentMode = false;
            needReSprint = false;
        }

        public boolean trigger(int waitTicks, int holdDurationTicks, boolean silent, boolean wasSprinting) {
            if (phase != TapPhase.IDLE || cooldownTicks > 0) {
                return false;
            }
            this.silentMode = silent;
            this.needReSprint = wasSprinting;
            this.holdTicks = Math.max(1, holdDurationTicks);
            if (waitTicks <= 0) {
                this.phase = TapPhase.ACTIVE;
                this.phaseTicks = this.holdTicks;
            } else {
                this.phase = TapPhase.WAITING;
                this.phaseTicks = waitTicks;
            }
            return true;
        }

        public boolean stepTick(boolean physicallyHoldingForward, boolean requireForward, int configuredCooldown) {
            if (cooldownTicks > 0) {
                cooldownTicks--;
            }
            if (phase == TapPhase.IDLE) {
                return false;
            }
            if (requireForward && !physicallyHoldingForward) {
                boolean shouldReSprint = false;
                phase = TapPhase.IDLE;
                phaseTicks = 0;
                cooldownTicks = Math.max(0, configuredCooldown);
                return shouldReSprint;
            }
            if (phase == TapPhase.WAITING) {
                phaseTicks--;
                if (phaseTicks <= 0) {
                    phase = TapPhase.ACTIVE;
                    phaseTicks = holdTicks;
                }
                return false;
            }
            phaseTicks--;
            if (phaseTicks <= 0) {
                boolean shouldReSprint = needReSprint && physicallyHoldingForward;
                phase = TapPhase.IDLE;
                phaseTicks = 0;
                cooldownTicks = Math.max(0, configuredCooldown);
                return shouldReSprint;
            }
            return false;
        }
    }

    private static boolean preAttackSprinting = false;
    private static float preAttackStrength = 1.0f;
    private static boolean reSprintPulsePending = false;
    private static boolean pulseHoldThisInputTick = false;

    private SprintResetController() {
    }

    public static boolean preAttackSprinting() {
        return preAttackSprinting;
    }

    public static float preAttackStrength() {
        return preAttackStrength;
    }

    public static void onPreAttack(Player player, Entity target) {
        Minecraft mc = Minecraft.getInstance();
        if (player == null || mc.player != player || !(target instanceof LivingEntity)) {
            return;
        }
        Options opts = mc.options;
        boolean keySprintDown = opts != null && opts.keySprint.isDown();
        preAttackSprinting = player.isSprinting() || keySprintDown;
        preAttackStrength = player.getAttackStrengthScale(0.5f);
    }

    public static void onPostAttack(Player player, Entity target) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player != player || mc.gui.screen() != null || !(target instanceof LivingEntity)) {
            return;
        }
        ImnotcheatingyouareClient client = ImnotcheatingyouareClient.INSTANCE;
        if (client == null || client.moduleManager == null) {
            return;
        }
        ModuleManager mm = client.moduleManager;
        Module sMod = mm.getModule("STap");
        Module wMod = mm.getModule("WTap");

        STap sTap = sMod instanceof STap st ? st : null;
        WTap wTap = wMod instanceof WTap wt ? wt : null;

        boolean sTapTriggered = false;
        if (sTap != null && sTap.isToggled()) {
            sTapTriggered = sTap.tryTriggerOnAttack(target, preAttackSprinting, preAttackStrength);
            if (sTapTriggered && wTap != null && wTap.isActive()) {
                wTap.abortAndRestore();
            }
        }

        if (!sTapTriggered && (sTap == null || !sTap.isActive()) && wTap != null && wTap.isToggled()) {
            wTap.tryTriggerOnAttack(target, preAttackSprinting, preAttackStrength);
        }
    }

    public static void onClientTick(Minecraft mc) {
        if (mc == null || mc.options == null) {
            return;
        }
        if (mc.gui.screen() != null) {
            if (reSprintPulsePending || pulseHoldThisInputTick) {
                reSprintPulsePending = false;
                pulseHoldThisInputTick = false;
                releaseSprintKeySafely(mc);
            }
            return;
        }
        if (reSprintPulsePending) {
            reSprintPulsePending = false;
            pulseHoldThisInputTick = false;
            if (!isAutoSprintEnabled()) {
                releaseSprintKeySafely(mc);
            }
        }
    }

    public static void requestReSprintPulse(Minecraft mc) {
        if (mc == null || mc.options == null) {
            return;
        }
        KeyMapping keySprint = mc.options.keySprint;
        if (!keySprint.isDown()) {
            keySprint.setDown(true);
            reSprintPulsePending = true;
            pulseHoldThisInputTick = true;
        }
        if (mc.player != null && !mc.player.isShiftKeyDown()) {
            mc.player.setSprinting(true);
        }
    }

    public static void applyPreInputOverrides(Minecraft mc) {
        if (mc == null || mc.options == null || mc.player == null) {
            return;
        }
        if (mc.gui.screen() != null || AutoTotem.shouldPauseInputs()) {
            return;
        }
        ImnotcheatingyouareClient client = ImnotcheatingyouareClient.INSTANCE;
        if (client == null || client.moduleManager == null) {
            return;
        }
        Options opts = mc.options;
        ModuleManager mm = client.moduleManager;
        Module sMod = mm.getModule("STap");
        if (sMod instanceof STap sTap && sTap.isToggled() && sTap.isBackwardTapActive()) {
            opts.keyUp.setDown(false);
            opts.keyDown.setDown(true);
            releaseSprintKeySafely(mc);
            return;
        }
        Module wMod = mm.getModule("WTap");
        if (wMod instanceof WTap wTap && wTap.isToggled() && wTap.isNormalReleaseActive()) {
            opts.keyUp.setDown(false);
            releaseSprintKeySafely(mc);
            return;
        }
        if (pulseHoldThisInputTick) {
            pulseHoldThisInputTick = false;
            opts.keySprint.setDown(true);
        }
    }

    public static boolean isSuppressingSprint() {
        ImnotcheatingyouareClient client = ImnotcheatingyouareClient.INSTANCE;
        if (client == null || client.moduleManager == null) {
            return false;
        }
        ModuleManager mm = client.moduleManager;
        Module sMod = mm.getModule("STap");
        if (sMod instanceof STap sTap && sTap.isToggled() && sTap.isSuppressingSprint()) {
            return true;
        }
        Module wMod = mm.getModule("WTap");
        return wMod instanceof WTap wTap && wTap.isToggled() && wTap.isSuppressingSprint();
    }

    public static boolean isAutoSprintEnabled() {
        ImnotcheatingyouareClient client = ImnotcheatingyouareClient.INSTANCE;
        if (client == null || client.moduleManager == null) {
            return false;
        }
        ModuleManager mm = client.moduleManager;
        Module sprint = mm.getModule("AutoSprint");
        return sprint != null && sprint.isToggled();
    }

    public static void releaseSprintKeySafely(Minecraft mc) {
        if (mc == null || mc.options == null) {
            return;
        }
        OptionInstance<Boolean> toggleOpt = mc.options.toggleSprint();
        if (!Boolean.TRUE.equals(toggleOpt.get())) {
            restorePhysicalKey(mc.options.keySprint);
        }
    }

    public static boolean isPhysicalKeyDown(KeyMapping mapping) {
        if (mapping == null) {
            return false;
        }
        try {
            InputConstants.Key bound = KeyMappingHelper.getBoundKeyOf(mapping);
            InputConstants.Type type = bound.getType();
            int code = bound.getValue();
            if (type == InputConstants.Type.KEYBOARD && code >= 0) {
                return InputConstants.isKeyDown(code);
            }
            if (type == InputConstants.Type.MOUSE && code >= 0) {
                Minecraft mc = Minecraft.getInstance();
                MouseHandler mh = mc != null ? mc.mouseHandler : null;
                if (mh != null) {
                    if (code == InputConstants.MOUSE_BUTTON_LEFT) {
                        return mh.isLeftPressed();
                    }
                    if (code == InputConstants.MOUSE_BUTTON_RIGHT) {
                        return mh.isRightPressed();
                    }
                    if (code == InputConstants.MOUSE_BUTTON_MIDDLE) {
                        return mh.isMiddlePressed();
                    }
                }
                int legacyOrd = InputUtil.toLegacyOrdinal(code);
                return InputUtil.isMouseButtonDown(InputUtil.fromClickOrdinal(legacyOrd));
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    public static void restorePhysicalKey(KeyMapping mapping) {
        if (mapping != null) {
            mapping.setDown(isPhysicalKeyDown(mapping));
        }
    }
}
