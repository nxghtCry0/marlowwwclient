package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.gui.screens.inventory.SignEditScreen;
import com.eclipseware.imnotcheatingyouare.client.utils.InputUtil;

public class GUIMove extends Module {
    public GUIMove() {
        super("GUIMove", Category.Blatant, "Allows you to walk and jump while in menus.");
    }

    @Override
    public void onTick() {
        if (mc.player == null) return;
        if (com.eclipseware.imnotcheatingyouare.client.module.impl.AutoTotem.shouldPauseInputs()) return;
        if (mc.gui.screen() != null && !(mc.gui.screen() instanceof ChatScreen) && !(mc.gui.screen() instanceof SignEditScreen) && !(mc.gui.screen() instanceof AnvilScreen)) {
            mc.options.keyUp.setDown(InputUtil.isDown(getKeyCode(mc.options.keyUp)));
            mc.options.keyDown.setDown(InputUtil.isDown(getKeyCode(mc.options.keyDown)));
            mc.options.keyLeft.setDown(InputUtil.isDown(getKeyCode(mc.options.keyLeft)));
            mc.options.keyRight.setDown(InputUtil.isDown(getKeyCode(mc.options.keyRight)));
            mc.options.keyJump.setDown(InputUtil.isDown(getKeyCode(mc.options.keyJump)));
            mc.options.keySprint.setDown(InputUtil.isDown(getKeyCode(mc.options.keySprint)));
            if (mc.options.keySprint.isDown()) {
                mc.player.setSprinting(true);
            }
        }
    }

    private int getKeyCode(KeyMapping mapping) {
        try {
            for (java.lang.reflect.Method m : mapping.getClass().getMethods()) {
                if (m.getParameterCount() == 0 && m.getReturnType().getName().contains("InputConstants$Key")) {
                    Object keyObj = m.invoke(mapping);
                    java.lang.reflect.Method getValue = keyObj.getClass().getMethod("getValue");
                    return (int) getValue.invoke(keyObj);
                }
            }
        } catch (Exception ignored) {
        }
        return mapping.getDefaultKey().getValue();
    }
}