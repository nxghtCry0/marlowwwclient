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
        if (mc.screen != null) mc.screen.onClose();
        mc.setScreen(new ConfigGui());
        this.toggle(); 
    }
}