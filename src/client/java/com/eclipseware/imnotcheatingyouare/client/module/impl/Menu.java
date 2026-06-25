package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.clickgui.Clickgui;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class Menu extends Module {
    public Menu() {
        super("Menu", Category.Client, "Opens the ClickGUI.");
        this.setKeyBind(GLFW.GLFW_KEY_RIGHT_SHIFT); 
    }

    private int pressCount = 0;
    private boolean wasPressed = false;
    private long lastPressTime = 0;

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

        if (isPressed && !wasPressed) {
            if (System.currentTimeMillis() - lastPressTime > 3000) {
                pressCount = 0;
            }
            pressCount++;
            lastPressTime = System.currentTimeMillis();
            if (pressCount >= 5) {
                this.onKeybind();
                pressCount = 0;
            }
        }
        wasPressed = isPressed;
    }

    @Override
    public void onEnable() {
        if (mc.player == null) {
            setToggled(false);
            return;
        }

        com.eclipseware.imnotcheatingyouare.client.setting.Setting reworkedSetting =
                ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Reworked UI");
        boolean useReworked = reworkedSetting != null && reworkedSetting.getValBoolean();

        if (useReworked) {
            mc.setScreen(new com.eclipseware.imnotcheatingyouare.client.clickgui.ReworkedClickgui());
            setToggled(false);
            return;
        }

        Module legacyUI = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("LegacyUI");
        if (legacyUI != null && legacyUI.isToggled()) {
            if (ImnotcheatingyouareClient.INSTANCE.clickGui == null) {
                ImnotcheatingyouareClient.INSTANCE.clickGui = new Clickgui();
            }
            if (!(mc.screen instanceof Clickgui)) {
                mc.setScreen(ImnotcheatingyouareClient.INSTANCE.clickGui);
            }
        } else {
            mc.setScreen(new com.eclipseware.imnotcheatingyouare.client.clickgui.NewClickgui());
        }
        setToggled(false);
    }
}
