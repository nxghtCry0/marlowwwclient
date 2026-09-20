package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.clickgui.Clickgui;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import net.minecraft.network.chat.Component;
import com.mojang.blaze3d.platform.InputConstants;

public class Menu extends Module {
    public Menu() {
        super("Menu", Category.Client, "Opens the ClickGUI.");
        this.setKeyBind(InputConstants.KEY_RSHIFT);
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

        if (this.getKeyBind() == 0 || mc == null || mc.getWindow() == null || mc.player == null) return;
        if (mc.gui.screen() != null) return;

        boolean isPressed = com.eclipseware.imnotcheatingyouare.client.utils.InputUtil.isDown(this.getKeyBind());

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

        Module weakDevice = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("WeakDevice");
        if (weakDevice != null && weakDevice.isToggled() && weakDevice instanceof com.eclipseware.imnotcheatingyouare.client.module.impl.WeakDevice wd) {
            wd.toggleGui();
            setToggled(false);
            return;
        }

        Module legacyUI = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("LegacyUI");
        if (legacyUI != null && legacyUI.isToggled()) {
            if (ImnotcheatingyouareClient.INSTANCE.clickGui == null) {
                ImnotcheatingyouareClient.INSTANCE.clickGui = new Clickgui();
            }
            if (!(mc.gui.screen() instanceof Clickgui)) {
                mc.setScreenAndShow(ImnotcheatingyouareClient.INSTANCE.clickGui);
            }
        } else {
            mc.setScreenAndShow(new com.eclipseware.imnotcheatingyouare.client.clickgui.MarlowGUI());
        }
        setToggled(false);
    }
}
