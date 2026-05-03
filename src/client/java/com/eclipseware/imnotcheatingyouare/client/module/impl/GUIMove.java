package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.gui.screens.inventory.SignEditScreen;
import org.lwjgl.glfw.GLFW;

public class GUIMove extends Module {
    public GUIMove() {
        super("GUIMove", Category.Movement, "Allows you to walk and jump while in menus.");
    }

    @Override
    public void onTick() {
        if (mc.player == null) return;
        if (mc.screen != null && !(mc.screen instanceof ChatScreen) && !(mc.screen instanceof SignEditScreen) && !(mc.screen instanceof AnvilScreen)) {
            long window = 0;
            try {
                for (java.lang.reflect.Field f : mc.getWindow().getClass().getDeclaredFields()) {
                    if (f.getType() == long.class) {
                        f.setAccessible(true);
                        window = f.getLong(mc.getWindow());
                        break;
                    }
                }
            } catch (Exception ignored) {}

            if (window == 0) return;

            mc.options.keyUp.setDown(GLFW.glfwGetKey(window, getKeyCode(mc.options.keyUp)) == GLFW.GLFW_PRESS);
            mc.options.keyDown.setDown(GLFW.glfwGetKey(window, getKeyCode(mc.options.keyDown)) == GLFW.GLFW_PRESS);
            mc.options.keyLeft.setDown(GLFW.glfwGetKey(window, getKeyCode(mc.options.keyLeft)) == GLFW.GLFW_PRESS);
            mc.options.keyRight.setDown(GLFW.glfwGetKey(window, getKeyCode(mc.options.keyRight)) == GLFW.GLFW_PRESS);
            mc.options.keyJump.setDown(GLFW.glfwGetKey(window, getKeyCode(mc.options.keyJump)) == GLFW.GLFW_PRESS);
            mc.options.keySprint.setDown(GLFW.glfwGetKey(window, getKeyCode(mc.options.keySprint)) == GLFW.GLFW_PRESS);
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