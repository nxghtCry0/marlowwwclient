package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;

public class Scaffold extends Module {

    public Scaffold() {
        super("Scaffold", Category.World, "Macro: Holds S, enables BridgeAssist & FastPlace.");
    }

    @Override
    public void onEnable() {
        Module bridgeAssist = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("BridgeAssist");
        Module fastPlace = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("FastPlace");
        
        if (bridgeAssist != null && !bridgeAssist.isToggled()) bridgeAssist.toggle();
        if (fastPlace != null && !fastPlace.isToggled()) fastPlace.toggle();
    }

    @Override
    public void onTick() {
        if (mc != null && mc.player != null && mc.screen == null) {
            mc.options.keyDown.setDown(true);
            mc.options.keyUse.setDown(true);
        }
    }

    @Override
    public void onDisable() {
        Module bridgeAssist = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("BridgeAssist");
        Module fastPlace = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("FastPlace");
        
        if (bridgeAssist != null && bridgeAssist.isToggled()) bridgeAssist.toggle();
        if (fastPlace != null && fastPlace.isToggled()) fastPlace.toggle();

        if (mc.options != null) {
            mc.options.keyDown.setDown(false);
            mc.options.keyUse.setDown(false);
        }
    }
}