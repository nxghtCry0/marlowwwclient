package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;

public class ClientSpoof extends Module {
    public ClientSpoof() {
        super("ClientSpoof", Category.Misc, "Spoofs the client brand name sent to the server.");
        setToggled(true);
    }

    public String getSpoofedBrand() {
        Setting modeSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Mode");
        if (modeSetting == null) return "lunarclient:v2.12.0-2629";
        
        String mode = modeSetting.getValString();
        if (mode.equalsIgnoreCase("Vanilla")) {
            return "vanilla";
        } else if (mode.equalsIgnoreCase("Fabric")) {
            return "fabric";
        } else if (mode.equalsIgnoreCase("Custom")) {
            Setting customSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Custom Brand");
            return customSetting != null ? customSetting.getValText() : "vanilla";
        }
        return "lunarclient:v2.12.0-2629";
    }
}
