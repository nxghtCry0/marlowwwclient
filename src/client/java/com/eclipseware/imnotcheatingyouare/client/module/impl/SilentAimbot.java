package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;

public class SilentAimbot extends Module {
    public SilentAimbot() {
        super("SilentAimbot", Category.Combat, "Automatically toggles SilentAim and Triggerbot.");
    }

    @Override
    public void onEnable() {
        Module silentAim = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("SilentAim");
        Module triggerbot = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("Triggerbot");

        if (silentAim != null) silentAim.setToggled(true);
        if (triggerbot != null) triggerbot.setToggled(true);
    }

    @Override
    public void onDisable() {
        Module silentAim = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("SilentAim");
        Module triggerbot = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("Triggerbot");

        if (silentAim != null) silentAim.setToggled(false);
        if (triggerbot != null) triggerbot.setToggled(false);
    }
}
