package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;

public class AutoWalk extends Module {
    public AutoWalk() {
        super("AutoWalk", Category.Movement, "Automatically holds down your walk forward key.");
    }

    @Override
    public void onTick() {
        if (mc.options != null) {
            mc.options.keyUp.setDown(true);
        }
    }

    @Override
    public void onDisable() {
        if (mc.options != null) {
            mc.options.keyUp.setDown(false);
        }
    }
}