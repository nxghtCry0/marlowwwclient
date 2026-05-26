package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;

public class JumpReset extends Module {
    private int hitsInTrade = 0;
    private long lastHitTime = 0;
    private boolean shouldJump = false;
    private int jumpDelayTicks = 0;
    private boolean keyWasPressed = false;

    private long lastKnockbackTime = 0;

    public JumpReset() {
        super("JumpReset", Category.Utility, "Converts horizontal KB into vertical KB by jumping on specific hits.");
    }

    @Override
    public void onTick() {
        if (!isToggled() || mc.player == null || mc.options == null) return;

        if (keyWasPressed) {
            if (!isPhysicallyHoldingJump()) {
                mc.options.keyJump.setDown(false);
            }
            keyWasPressed = false;
        }

        Setting timeoutSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Trade Timeout (ms)");
        long timeout = timeoutSetting != null ? (long) timeoutSetting.getValDouble() : 500;
        if (System.currentTimeMillis() - lastHitTime > timeout) {
            hitsInTrade = 0;
        }

        if (shouldJump) {
            if (System.currentTimeMillis() - lastKnockbackTime > 250) {
                shouldJump = false;
                jumpDelayTicks = 0;
            } else if (mc.player.onGround()) {
                Setting delaySetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Delay (Ticks)");
                Setting modeSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Mode");
                String mode = modeSetting != null ? modeSetting.getValString() : "Smart";
                int delay = (mode.equals("Classic") || mode.equals("Blatant")) ? 0 : (delaySetting != null ? (int) delaySetting.getValDouble() : 0);

                if (jumpDelayTicks >= delay) {
                    mc.options.keyJump.setDown(true);
                    keyWasPressed = true;
                    shouldJump = false;
                    jumpDelayTicks = 0;
                } else {
                    jumpDelayTicks++;
                }
            }
        }
    }

    public void onKnockback() {
        if (!isToggled() || mc.player == null) return;
        if (mc.player.isBlocking()) return;
        
        if (System.currentTimeMillis() - lastHitTime < 50) return;

        lastHitTime = System.currentTimeMillis();
        lastKnockbackTime = lastHitTime;
        hitsInTrade++;

        Setting chanceSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Chance (%)");
        double chance = chanceSetting != null ? chanceSetting.getValDouble() : 100.0;
        if (Math.random() * 100.0 > chance) return;

        Setting modeSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Mode");
        String mode = modeSetting != null ? modeSetting.getValString() : "Smart";

        if (mode.equals("Classic")) {
            shouldJump = true;
            jumpDelayTicks = 0;
        } else if (mode.equals("Smart")) {
            Setting resetHitSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Reset Hit");
            int resetHit = resetHitSetting != null ? (int) resetHitSetting.getValDouble() : 2;

            Setting maxTradeSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Max Trade Length");
            int maxTrade = maxTradeSetting != null ? (int) maxTradeSetting.getValDouble() : 4;

            if (hitsInTrade > maxTrade) {
                shouldJump = false;
                return;
            }

            Setting shortTradeSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Short Trade Reset");
            boolean shortTrade = shortTradeSetting != null && shortTradeSetting.getValBoolean();

            if (hitsInTrade == resetHit || (shortTrade && hitsInTrade <= 2)) {
                shouldJump = true;
                jumpDelayTicks = 0;
            }
        } else {
            shouldJump = true;
            jumpDelayTicks = 0;
        }
    }

    private boolean isPhysicallyHoldingJump() {
        long window = getWindowHandle();
        if (window == 0) return false;
        return org.lwjgl.glfw.GLFW.glfwGetKey(window, getKeyCode(mc.options.keyJump)) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
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
        shouldJump = false;
        jumpDelayTicks = 0;
        hitsInTrade = 0;
        keyWasPressed = false;
        if (mc.options != null && mc.options.keyJump != null) {
            mc.options.keyJump.setDown(false);
        }
    }
}