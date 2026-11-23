package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.glassui.bridge.ClientGuiScreen;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.module.ModuleManager;
import com.eclipseware.imnotcheatingyouare.client.utils.InputUtil;
import com.mojang.blaze3d.platform.InputConstants;

public class GUI extends Module {
    private static boolean suppressUntilRelease = false;
    private int pressCount = 0;
    private boolean wasPressed = false;
    private long lastPressTime = 0L;

    public GUI() {
        super("GUI", Category.Client, "Opens and customizes the Glass UI menu, scale, surface, and streamproof mode.");
        this.setKeyBind(InputConstants.KEY_RSHIFT);
    }

    public static void notifyGuiClosed() {
        suppressUntilRelease = true;
    }

    public int getEffectiveKeyBind() {
        int kb = this.getKeyBind();
        return kb != 0 ? kb : InputConstants.KEY_RSHIFT;
    }

    @Override
    public void tickKeybind() {
        ImnotcheatingyouareClient client = ImnotcheatingyouareClient.INSTANCE;
        if (client == null || client.moduleManager == null) {
            return;
        }
        if (this.getKeyBind() == 0) {
            this.setKeyBind(InputConstants.KEY_RSHIFT);
        }
        int key = getEffectiveKeyBind();
        if (mc == null || mc.getWindow() == null || mc.player == null) {
            return;
        }

        boolean isPressed = InputUtil.isDown(key);
        if (suppressUntilRelease) {
            if (!isPressed) {
                suppressUntilRelease = false;
            }
            wasPressed = isPressed;
            return;
        }

        if (mc.gui.screen() != null) {
            wasPressed = isPressed;
            return;
        }

        ModuleManager mm = client.moduleManager;
        Module bypassMod = mm.getModule("Bypass");
        boolean bypassActive = bypassMod != null && bypassMod.isToggled();

        if (!bypassActive) {
            if (isPressed && !wasPressed) {
                openClickGui();
            }
            wasPressed = isPressed;
            return;
        }

        if (isPressed && !wasPressed) {
            long now = System.currentTimeMillis();
            if (now - lastPressTime > 3000L) {
                pressCount = 0;
            }
            pressCount++;
            lastPressTime = now;
            if (pressCount >= 5) {
                openClickGui();
                pressCount = 0;
            }
        }
        wasPressed = isPressed;
    }

    @Override
    public void onKeybind() {
        openClickGui();
    }

    @Override
    public void onEnable() {
        openClickGui();
        setToggled(false);
    }

    public static void openClickGui() {
        if (mc == null || mc.player == null) {
            return;
        }
        ImnotcheatingyouareClient client = ImnotcheatingyouareClient.INSTANCE;
        if (client == null || client.moduleManager == null) {
            return;
        }
        ModuleManager mm = client.moduleManager;
        Module weakDevice = mm.getModule("WeakDevice");
        if (weakDevice != null && weakDevice.isToggled() && weakDevice instanceof WeakDevice wd) {
            wd.toggleGui();
            return;
        }
        ClientGuiScreen.open();
    }
}
