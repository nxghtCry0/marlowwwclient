package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;

public class RenderOptimizer extends Module {
    public static RenderOptimizer INSTANCE;

    public RenderOptimizer() {
        super("RenderOptimizer", Category.Render, "Optimizes standard UI rendering and overlays.");
        INSTANCE = this;
        
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("No Scoreboard", this, true));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("No Hurt Cam", this, true));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("No Bobbing", this, true));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Low Fire", this, true));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Low Shield", this, true));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Low Totem", this, true));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Fast Totem Anim", this, true));
    }

    @Override
    public void onTick() {
        if (!isToggled() || mc.player == null) return;
        
        Setting noBob = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "No Bobbing");
        if (noBob != null && noBob.getValBoolean()) {
            if (mc.options.bobView().get()) {
                mc.options.bobView().set(false);
            }
        }
    }
}
