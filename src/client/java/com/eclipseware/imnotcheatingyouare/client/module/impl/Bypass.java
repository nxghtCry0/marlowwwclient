package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;

public class Bypass extends Module {
    public Bypass() {
        super("Bypass", Category.Client, "Hides visuals and makes GUI require looking down + 5s hold.");
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Disable Command Autofill", this, false));
    }
}
