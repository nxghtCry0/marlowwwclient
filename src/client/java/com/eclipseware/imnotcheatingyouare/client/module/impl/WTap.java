package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.utils.cheat.AntiCheatProfile;
import org.lwjgl.glfw.GLFW;

public class WTap extends Module {

    // 0 = idle, 1 = counting down before release, 2 = W released counting to re-press
    private int phase           = 0;
    private int ticksRemaining  = 0;

    // Randomised per-cycle jitter so the release timing isn't perfectly constant
    private int jitterTicks = 0;
    private int phase = 0;
    private int ticksRemaining = 0;

    public WTap() {
        super("WTap", Category.Combat, "Releases forward key on hit to reset sprint knockback.");
    }

    /**
     * Called from the mixin when attack() returns without being cancelled.
     * Only starts a sequence if the player is currently holding W.
     */
    public void onAttackLanded() {
        if (!isToggled() || mc.player == null || mc.options == null) return;
        if (phase != 0) return;
        if (!mc.options.keyUp.isDown()) return;

        phase = 1;
        Setting waitSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Wait Ticks");
        int base = waitSetting != null ? (int) waitSetting.getValDouble() : 0;

        // Add 0-1 tick of jitter to prevent perfectly periodic sprint-reset pattern
        jitterTicks = (int) (Math.random() * 2);
        ticksRemaining = base + jitterTicks;
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.options == null || phase == 0) return;

        switch (phase) {
            case 1 -> {
                if (!mc.options.keyUp.isDown()) { phase = 0; return; }
                ticksRemaining--;
                if (ticksRemaining <= 0) {
                    if (AntiCheatProfile.wtapSilentMode()) {
                        // Silent mode: send a stop-sprint packet without touching the key state
                        // This is invisible to screenshares since no key is physically released
                        sendSprintPacket(false);
                    } else {
                        mc.options.keyUp.setDown(false);
                    }
                if (!mc.options.keyUp.isDown()) {
                    phase = 0;
                    return;
                }
                ticksRemaining--;
                if (ticksRemaining <= 0) {
                    mc.options.keyUp.setDown(false);
                    phase = 2;
                    Setting actionSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Action Ticks");
                    int base = actionSetting != null ? (int) actionSetting.getValDouble() : 1;
                    ticksRemaining = base + (int) (Math.random() * 2); // jitter
                }
            }
            case 2 -> {
                ticksRemaining--;
                if (ticksRemaining <= 0) {
                    if (AntiCheatProfile.wtapSilentMode()) {
                        sendSprintPacket(true);
                    } else if (isPhysicallyHoldingW()) {
                    if (isPhysicallyHoldingW()) {
                        mc.options.keyUp.setDown(true);
                    }
                    phase = 0;
                }
            }
        }
    }

    /** Send a ServerboundPlayerCommandPacket to start/stop sprinting without touching key state. */
    private void sendSprintPacket(boolean start) {
        if (mc.getConnection() == null || mc.player == null) return;
        net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action action =
            start
            ? net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action.START_SPRINTING
            : net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action.STOP_SPRINTING;
        mc.getConnection().send(new net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket(
            mc.player, action));
    }

    private boolean isPhysicallyHoldingW() {
        long window = getWindowHandle();
        if (window == 0) return false;
        return GLFW.glfwGetKey(window, mc.options.keyUp.getDefaultKey().getValue()) == GLFW.GLFW_PRESS;
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
        if (phase == 2 && mc.options != null && isPhysicallyHoldingW()) {
            mc.options.keyUp.setDown(true);
        }
        phase          = 0;
        ticksRemaining = 0;
    }
}
