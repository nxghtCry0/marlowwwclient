package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.utils.cheat.ClickConsistency;
import net.minecraft.client.KeyMapping;

public class AutoClicker extends Module {

    public AutoClicker() {
        super("AutoClicker", Category.Combat, "Automatically clicks for you with randomized CPS.");
    }

    @Override
    public void onTick() {
        if (mc == null || mc.player == null || mc.screen != null) return;

        Setting requireClick = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Require Click");
        Setting buttonSet = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Button");
        Setting rightClickerSet = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Right Clicker");

        boolean rightClickerEnabled = rightClickerSet != null && rightClickerSet.getValBoolean();
        boolean leftClick = buttonSet == null || buttonSet.getValString().equals("Left");

        Setting minCpsSet = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Min CPS");
        Setting maxCpsSet = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Max CPS");

        double minCps = minCpsSet != null ? minCpsSet.getValDouble() : 9.0;
        double maxCps = maxCpsSet != null ? maxCpsSet.getValDouble() : 14.0;

        if (minCps > maxCps) {
            double temp = minCps; minCps = maxCps; maxCps = temp;
        }

        if (leftClick) {
            KeyMapping targetKey = mc.options.keyAttack;
            if (requireClick == null || !requireClick.getValBoolean() || targetKey.isDown()) {
                double targetCps = minCps + Math.random() * (maxCps - minCps);
                long baseDelayMs = (long) (1000.0 / Math.max(1.0, targetCps));
                if (ClickConsistency.shouldClick(baseDelayMs, (int) maxCps)) {
                    KeyMapping.click(targetKey.getDefaultKey());
                }
            }
        }

        boolean shouldRightClick = (buttonSet != null && buttonSet.getValString().equals("Right")) || rightClickerEnabled;
        if (shouldRightClick) {
            KeyMapping targetKey = mc.options.keyUse;
            if (requireClick == null || !requireClick.getValBoolean() || targetKey.isDown()) {
                double targetCps = minCps + Math.random() * (maxCps - minCps);
                long baseDelayMs = (long) (1000.0 / Math.max(1.0, targetCps));
                if (ClickConsistency.shouldRightClick(baseDelayMs, (int) maxCps)) {
                    KeyMapping.click(targetKey.getDefaultKey());
                }
            }
        }
    }
}