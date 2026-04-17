package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.utils.cheat.TimerUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.BlockHitResult;
import org.lwjgl.glfw.GLFW;

public final class AnchorMacro extends Module {
    private final int anchorKeybind = GLFW.GLFW_KEY_X;
    private final int delay = 50;
    private final TimerUtil timer = new TimerUtil();
    private boolean keyPressed = false;
    private boolean isActive = false;
    private int originalSlot = -1;
    private boolean pendingRestoreSlot = false;
    private int pendingRestoreTicksLeft = 0;

    public AnchorMacro() {
        super("Anchor Macro", Category.Combat, "Automatically places and explodes respawn anchors for PvP");
    }

    @Override
    public void onTick() {
        Minecraft mc = Module.mc;
        if (mc == null || mc.player == null || mc.level == null || !isToggled()) return;
        if (mc.screen != null) return;

        boolean currentKeyState = isKeyPressed(anchorKeybind);
        if (currentKeyState && !keyPressed) startAnchorPvP();
        else if (!currentKeyState && keyPressed) stopAnchorPvP();
        keyPressed = currentKeyState;

        if (isActive && timer.hasElapsedTime(delay)) {
            processAnchorPvP();
            timer.reset();
        }

        if (pendingRestoreSlot) {
            if (pendingRestoreTicksLeft <= 0) {
                if (originalSlot != -1) mc.player.getInventory().setSelectedSlot(originalSlot);
                pendingRestoreSlot = false;
            } else {
                pendingRestoreTicksLeft--;
            }
        }
    }

    private void startAnchorPvP() {
        if (isActive) return;
        isActive = true;
        originalSlot = Module.mc.player.getInventory().getSelectedSlot();
        timer.reset();
    }

    private void stopAnchorPvP() {
        if (!isActive) return;
        isActive = false;
        if (originalSlot != -1) Module.mc.player.getInventory().setSelectedSlot(originalSlot);
        originalSlot = -1;
        pendingRestoreSlot = false;
    }

    private void processAnchorPvP() {
        Minecraft mc = Module.mc;
        if (mc == null || mc.level == null || mc.hitResult == null) return;
        if (!(mc.hitResult instanceof BlockHitResult blockHit)) return;
    }

    private boolean isKeyPressed(int keyCode) {
        Minecraft mc = Module.mc;
        if (mc == null) return false;
        long window = 0;
        try {
            for (java.lang.reflect.Field f : mc.getWindow().getClass().getDeclaredFields()) {
                if (f.getType() == long.class) { f.setAccessible(true); window = f.getLong(mc.getWindow()); break; }
            }
        } catch (Exception ignored) {}
        return window != 0 && GLFW.glfwGetKey(window, keyCode) == GLFW.GLFW_PRESS;
    }

    @Override
    public void onEnable() {
        keyPressed = false; isActive = false; originalSlot = -1;
        pendingRestoreSlot = false; pendingRestoreTicksLeft = 0;
        timer.reset();
        super.onEnable();
    }

    @Override
    public void onDisable() { stopAnchorPvP(); super.onDisable(); }
    @Override
    public int getKeyBind() { return -1; }
}