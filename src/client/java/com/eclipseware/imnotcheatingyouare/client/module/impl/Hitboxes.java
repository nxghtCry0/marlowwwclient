package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;

public class Hitboxes extends Module {
    private Setting size;

    public Hitboxes() {
        super("Hitboxes", Category.Blatant, "Expands entity hitboxes (F3+B won't show it but it works)");
        setSubCategory("Semi-Blatant"); // Requested to go in Semi-Blatant inside Blatant category
        size = new Setting("Expand Size", this, 0.2, 0.0, 3.0, false);
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(size);
    }
}
