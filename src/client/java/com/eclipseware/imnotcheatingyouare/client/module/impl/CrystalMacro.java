package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.utils.cheat.TimerUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import org.lwjgl.glfw.GLFW;

import java.util.Random;

public final class CrystalMacro extends Module {
    private final int crystalKeybind = GLFW.GLFW_MOUSE_BUTTON_4;
    private final TimerUtil breakTimer = new TimerUtil();
    private final TimerUtil placeTimer = new TimerUtil();
    private final Random random = new Random();
    private boolean keyPressed = false;
    private boolean isActive = false;
    private int originalSlot = -1;
    private long currentBreakDelay = 50;
    private long currentPlaceDelay = 30;

    public CrystalMacro() {
        super("Crystal Macro", Category.Combat, "Automatically places and explodes crystals and obsidian for PvP");
    }

    @Override
    public void onTick() {
        Minecraft mc = Module.mc;
        if (mc == null || mc.player == null || mc.level == null || mc.screen != null) return;

        boolean currentKeyState = isKeyPressed(crystalKeybind);
        if (currentKeyState && !keyPressed) startCrystalPvP();
        else if (!currentKeyState && keyPressed) stopCrystalPvP();
        keyPressed = currentKeyState;

        if (isActive) processCrystalPvP();
    }

    private void startCrystalPvP() {
        if (isActive) return;
        Minecraft mc = Module.mc;
        isActive = true;
        originalSlot = mc.player.getInventory().getSelectedSlot();
        resetDelays();
    }

    private void stopCrystalPvP() {
        if (!isActive) return;
        Minecraft mc = Module.mc;
        if (originalSlot != -1) mc.player.getInventory().setSelectedSlot(originalSlot);
        isActive = false;
        resetDelays();
    }

    private void resetDelays() {
        breakTimer.reset();
        placeTimer.reset();
        currentBreakDelay = 50 + random.nextInt(50);
        currentPlaceDelay = 30 + random.nextInt(50);
    }

    private void processCrystalPvP() {
        Minecraft mc = Module.mc;
        if (mc.player == null || mc.level == null) return;
        if (!mc.player.onGround()) return;

        if (mc.hitResult instanceof BlockHitResult blockHit && placeTimer.hasElapsedTime(currentPlaceDelay)) {
            BlockPos targetBlock = blockHit.getBlockPos();
            var dir = blockHit.getDirection();
            BlockPos placementPos = targetBlock.offset(dir.getStepX(), dir.getStepY(), dir.getStepZ());
            if (isValidPosition(placementPos)) {
                placeTimer.reset();
                currentPlaceDelay = 30 + random.nextInt(50);
            }
        }
    }

    private boolean isValidPosition(BlockPos pos) {
        Minecraft mc = Module.mc;
        if (mc == null || mc.level == null || mc.player == null) return false;
        double dist = mc.player.position().distanceTo(net.minecraft.world.phys.Vec3.atCenterOf(pos));
        if (dist > 4.5) return false;
        if (!mc.level.getBlockState(pos).isAir()) return false;
        BlockPos playerPos = mc.player.blockPosition();
        return !pos.equals(playerPos) && !pos.equals(playerPos.above());
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
    public void onDisable() { super.onDisable(); stopCrystalPvP(); }
    @Override
    public int getKeyBind() { return -1; }
}