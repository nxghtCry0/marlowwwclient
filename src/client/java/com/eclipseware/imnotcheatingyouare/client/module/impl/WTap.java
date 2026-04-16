package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import org.lwjgl.glfw.GLFW;

public class WTap extends Module {

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
        ticksRemaining = waitSetting != null ? (int) waitSetting.getValDouble() : 0;
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.options == null || phase == 0) return;

        switch (phase) {
            case 1 -> {
                if (!mc.options.keyUp.isDown()) {
                    phase = 0;
                    return;
                }
                ticksRemaining--;
                if (ticksRemaining <= 0) {
                    mc.options.keyUp.setDown(false);
                    phase = 2;
                    Setting actionSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Action Ticks");
                    ticksRemaining = actionSetting != null ? (int) actionSetting.getValDouble() : 1;
                }
            }
            case 2 -> {
                ticksRemaining--;
                if (ticksRemaining <= 0) {
                    if (isPhysicallyHoldingW()) {
                        mc.options.keyUp.setDown(true);
                    }
                    phase = 0;
                }
            }
        }
    }

    /**
     * Reads the raw hardware key state directly from GLFW, bypassing the game's
     * KeyMapping layer entirely. This lets us distinguish "we called setDown(false)"
     * from "the player physically lifted their finger off W".
     */
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
        phase = 0;
        ticksRemaining = 0;
    }
}