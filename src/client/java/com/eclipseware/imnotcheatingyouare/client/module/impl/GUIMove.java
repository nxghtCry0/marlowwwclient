package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
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

            mc.options.keyUp.setDown(GLFW.glfwGetKey(window, mc.options.keyUp.getDefaultKey().getValue()) == GLFW.GLFW_PRESS);
            mc.options.keyDown.setDown(GLFW.glfwGetKey(window, mc.options.keyDown.getDefaultKey().getValue()) == GLFW.GLFW_PRESS);
            mc.options.keyLeft.setDown(GLFW.glfwGetKey(window, mc.options.keyLeft.getDefaultKey().getValue()) == GLFW.GLFW_PRESS);
            mc.options.keyRight.setDown(GLFW.glfwGetKey(window, mc.options.keyRight.getDefaultKey().getValue()) == GLFW.GLFW_PRESS);
            mc.options.keyJump.setDown(GLFW.glfwGetKey(window, mc.options.keyJump.getDefaultKey().getValue()) == GLFW.GLFW_PRESS);
            mc.options.keySprint.setDown(GLFW.glfwGetKey(window, mc.options.keySprint.getDefaultKey().getValue()) == GLFW.GLFW_PRESS);
        }
    }
}