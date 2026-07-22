package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;

public class NoSlow extends Module {
    public NoSlow() {
        super("NoSlow", Category.Movement);
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Multiplier", this, 0.5, 0.5, 0.6, false));
    }
}
