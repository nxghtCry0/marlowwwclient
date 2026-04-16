package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;

public class Xray extends Module {
    public static Xray INSTANCE;

    public Xray() {
        super("Xray", Category.Render, "Only renders ores, chests, and spawners. Good for mining.");
        INSTANCE = this;
    }

    @Override
    public void onEnable() {
        if (mc.levelRenderer != null) {
            mc.levelRenderer.allChanged(); // Force a chunk remesh
        }
    }

    @Override
    public void onDisable() {
        if (mc.levelRenderer != null) {
            mc.levelRenderer.allChanged(); // Force a chunk remesh
        }
    }
}