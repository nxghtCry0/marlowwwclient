package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;

public class FastBreak extends Module {
    public FastBreak() {
        super("FastBreak", Category.World);
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Speed Multiplier", this, 1.07, 1.05, 1.10, false));
    }
}
