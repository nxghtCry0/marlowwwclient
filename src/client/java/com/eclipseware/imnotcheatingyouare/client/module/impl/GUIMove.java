package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.gui.screens.inventory.SignEditScreen;

public class GUIMove extends Module {
    public GUIMove() {
        super("GUIMove", Category.Blatant, "Allows you to walk and jump while in menus.");
    }

    @Override
    public void onTick() {
        if (mc == null || mc.player == null || mc.options == null) {
            return;
        }
        if (AutoTotem.shouldPauseInputs()) {
            return;
        }
        Screen screen = mc.gui.screen();
        if (screen == null
            || screen instanceof ChatScreen
            || screen instanceof SignEditScreen
            || screen instanceof AnvilScreen
            || screen.isInputCaptured()) {
            return;
        }

        var opts = mc.options;
        opts.keyUp.setDown(SprintResetController.isPhysicalKeyDown(opts.keyUp));
        opts.keyDown.setDown(SprintResetController.isPhysicalKeyDown(opts.keyDown));
        opts.keyLeft.setDown(SprintResetController.isPhysicalKeyDown(opts.keyLeft));
        opts.keyRight.setDown(SprintResetController.isPhysicalKeyDown(opts.keyRight));
        opts.keyJump.setDown(SprintResetController.isPhysicalKeyDown(opts.keyJump));
        opts.keySprint.setDown(SprintResetController.isPhysicalKeyDown(opts.keySprint));
        if (opts.keySprint.isDown() && !SprintResetController.isSuppressingSprint()) {
            mc.player.setSprinting(true);
        }
    }
}
