package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;

public class HUDEditor extends Module {
    public HUDEditor() {
        super("HUDEditor", Category.HUD, "Opens the interactive screen overlay editor.");
        this.setKeyBind(com.mojang.blaze3d.platform.InputConstants.KEY_GRAVE);
    }

    @Override
    public void onEnable() {
        if (mc.player == null) {
            setToggled(false);
            return;
        }
        mc.setScreenAndShow(new com.eclipseware.imnotcheatingyouare.client.clickgui.HudEditorScreen());
        setToggled(false);
    }
}
