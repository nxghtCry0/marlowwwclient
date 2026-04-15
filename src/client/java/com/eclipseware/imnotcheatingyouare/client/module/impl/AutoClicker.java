package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import net.minecraft.client.KeyMapping;

public class AutoClicker extends Module {
    private long lastClickTime = 0;
    private long nextDelay = 0;

    public AutoClicker() {
        super("AutoClicker", Category.Combat, "Automatically clicks for you with randomized CPS.");
    }

    @Override
    public void onTick() {
        if (mc == null || mc.player == null || mc.screen != null) return;

        Setting requireClick = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Require Click");
        Setting buttonSet = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Button");
        
        boolean leftClick = buttonSet == null || buttonSet.getValString().equals("Left");
        KeyMapping targetKey = leftClick ? mc.options.keyAttack : mc.options.keyUse;

        if (requireClick != null && requireClick.getValBoolean() && !targetKey.isDown()) {
            return;
        }

        if (System.currentTimeMillis() - lastClickTime >= nextDelay) {
            // Send Native Click
            KeyMapping.click(targetKey.getDefaultKey());
            lastClickTime = System.currentTimeMillis();

            // Calculate next randomized delay
            Setting minCpsSet = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Min CPS");
            Setting maxCpsSet = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Max CPS");
            
            double minCps = minCpsSet != null ? minCpsSet.getValDouble() : 9.0;
            double maxCps = maxCpsSet != null ? maxCpsSet.getValDouble() : 14.0;
            
            if (minCps > maxCps) {
                double temp = minCps; minCps = maxCps; maxCps = temp;
            }

            double randomCps = minCps + (Math.random() * (maxCps - minCps));
            // Add extreme micro-jitter to prevent static CPS detection
            randomCps += (Math.random() - 0.5); 
            
            nextDelay = (long) (1000.0 / Math.max(1.0, randomCps));
        }
    }
}