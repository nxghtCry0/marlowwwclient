package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.clickgui.ConfigGui;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;

public class Configurator extends Module {
    public Configurator() {
        super("Config Menu", Category.Configs);
    }

    @Override
    public void onEnable() {
        // Pop open the GUI, then immediately untoggle the module so it acts like a push-button
        if (mc.screen != null) mc.screen.onClose();
        mc.setScreen(new ConfigGui());
        this.toggle(); 
    }
}