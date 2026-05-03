package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.clickgui.Clickgui;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import org.lwjgl.glfw.GLFW;

public class Menu extends Module {
    public Menu() {
        super("Menu", Category.Client, "Opens the ClickGUI.");
        this.setKeyBind(GLFW.GLFW_KEY_RIGHT_SHIFT); // Default bind
    }

    private long keyHoldStartTime = 0;
    private int lastSecondsLeft = -1;

    @Override
    public void tickKeybind() {
        Module bypassMod = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("Bypass");
        boolean bypassActive = bypassMod != null && bypassMod.isToggled();
        
        if (!bypassActive) {
            super.tickKeybind();
            return;
        }

        if (this.getKeyBind() == -1 || mc == null || mc.getWindow() == null || mc.player == null) return;
        if (mc.screen != null) return;

        long windowHandle = 0;
        try {
            for (java.lang.reflect.Field f : mc.getWindow().getClass().getDeclaredFields()) {
                if (f.getType() == long.class) {
                    f.setAccessible(true);
                    windowHandle = f.getLong(mc.getWindow());
                    break;
                }
            }
        } catch (Exception e) {}

        if (windowHandle == 0) return;

        boolean isPressed;
        if (this.getKeyBind() >= 0 && this.getKeyBind() <= 7) {
            isPressed = org.lwjgl.glfw.GLFW.glfwGetMouseButton(windowHandle, this.getKeyBind()) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
        } else {
            isPressed = org.lwjgl.glfw.GLFW.glfwGetKey(windowHandle, this.getKeyBind()) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
        }

        boolean lookingDown = mc.player.getXRot() > 75.0f;

        if (isPressed && lookingDown) {
            if (keyHoldStartTime == 0) {
                keyHoldStartTime = System.currentTimeMillis();
            } else {
                long elapsed = System.currentTimeMillis() - keyHoldStartTime;
                if (elapsed >= 5000) {
                    this.onKeybind();
                    keyHoldStartTime = 0;
                    mc.player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§aBypass: Menu Opened"));
                    lastSecondsLeft = -1;
                } else {
                    int secondsLeft = 5 - (int)(elapsed / 1000);
                    if (secondsLeft != lastSecondsLeft) {
                        mc.player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§cHold for " + secondsLeft + "s to open..."));
                        lastSecondsLeft = secondsLeft;
                    }
                }
            }
        } else {
            keyHoldStartTime = 0;
            lastSecondsLeft = -1;
        }
    }

    @Override
    public void onEnable() {
        if (mc.player == null) {
            setToggled(false);
            return;
        }
        
        if (ImnotcheatingyouareClient.INSTANCE.clickGui == null) {
            ImnotcheatingyouareClient.INSTANCE.clickGui = new Clickgui();
        }
        
        if (!(mc.screen instanceof Clickgui)) {
            mc.setScreen(ImnotcheatingyouareClient.INSTANCE.clickGui);
        }
        
        setToggled(false);
    }
}
