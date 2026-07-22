package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.clickgui.ConfirmDisableScreen;
import net.minecraft.client.Minecraft;

public class KeybindList extends Module {
    private boolean confirmedDisable = false;

    public KeybindList() {
        super("KeybindList", Category.HUD, "Displays active keybinds on the HUD.");
        this.toggle(); 
    }

    public void setConfirmedDisable(boolean confirmed) {
        this.confirmedDisable = confirmed;
    }

    @Override
    public void onDisable() {
        if (!confirmedDisable) {
            this.toggle();
            if (mc != null) {
                mc.setScreenAndShow(new ConfirmDisableScreen(this));
            }
        } else {
            confirmedDisable = false;
        }
    }
}
