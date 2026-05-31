package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;

public class HUDEditor extends Module {
    public HUDEditor() {
        super("HUDEditor", Category.HUD, "Opens the interactive screen overlay editor.");
        this.setKeyBind(org.lwjgl.glfw.GLFW.GLFW_KEY_GRAVE_ACCENT);
    }

    @Override
    public void onEnable() {
        if (mc.player == null) {
            setToggled(false);
            return;
        }
        mc.setScreen(new com.eclipseware.imnotcheatingyouare.client.clickgui.HudEditorScreen());
        setToggled(false);
    }
}
