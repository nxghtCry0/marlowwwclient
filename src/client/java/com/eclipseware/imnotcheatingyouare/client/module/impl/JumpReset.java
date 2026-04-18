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

    public JumpReset() {
        super("JumpReset", Category.Combat, "Converts horizontal KB into vertical KB by jumping on specific hits.");
    }

    @Override
    public void onTick() {
        if (!isToggled() || mc.player == null || mc.options == null) return;

        Setting timeoutSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Trade Timeout (ms)");
        long timeout = timeoutSetting != null ? (long) timeoutSetting.getValDouble() : 500;
        if (System.currentTimeMillis() - lastHitTime > timeout) {
            hitsInTrade = 0;
        }

        if (shouldJump && mc.player.onGround()) {
            Setting delaySetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Delay (Ticks)");
            int delay = delaySetting != null ? (int) delaySetting.getValDouble() : 0;
            if (jumpDelayTicks >= delay) {
                mc.player.jumpFromGround();
                shouldJump = false;
                jumpDelayTicks = 0;
            } else {
                jumpDelayTicks++;
            }
        }
    }

    public void onKnockback() {
        if (!isToggled() || mc.player == null) return;
        
        if (System.currentTimeMillis() - lastHitTime < 50) return;

        hitsInTrade++;
        lastHitTime = System.currentTimeMillis();

        Setting chanceSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Chance (%)");
        double chance = chanceSetting != null ? chanceSetting.getValDouble() : 100.0;
        if (Math.random() * 100.0 > chance) return;

        Setting modeSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Mode");
        String mode = modeSetting != null ? modeSetting.getValString() : "Smart";

        if (mode.equals("Classic")) {
            if (mc.player.onGround()) {
                mc.player.jumpFromGround();
            }
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
            if (mc.player.onGround()) {
                mc.player.jumpFromGround();
            }
        }
    }

    @Override
    public void onDisable() {
        shouldJump = false;
        jumpDelayTicks = 0;
        hitsInTrade = 0;
        if (mc.options != null && mc.options.keyJump != null) {
            mc.options.keyJump.setDown(false);
        }
    }
}