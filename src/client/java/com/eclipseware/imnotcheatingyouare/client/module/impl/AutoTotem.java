package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Items;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class AutoTotem extends Module {
    private static final int MIN_DURATION_MS = 5;
    private static final int MAX_DURATION_MS = 175;

    private final Setting durationMs;
    private volatile boolean sequenceRunning = false;
    private volatile long lastPopMs = 0L;

    public AutoTotem() {
        super("AutoTotem", Category.Crystal, "Re-equips a totem right after a pop with a timed inventory macro.");
        setSubCategory("Crystal PvP");

        durationMs = new Setting("Duration (ms)", this, 45.0, MIN_DURATION_MS, MAX_DURATION_MS, true);
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(durationMs);
    }

    @Override
    public void onEnable() {
        sequenceRunning = false;
        lastPopMs = 0L;
    }

    public void onLocalTotemPop() {
        if (!isToggled() || mc.player == null || mc.gameMode == null) return;

        long now = System.currentTimeMillis();
        if (sequenceRunning || now - lastPopMs < 50L) return;

        int totemSlot = findTotemSlot();
        if (totemSlot == -1) return;

        lastPopMs = now;
        int requestedDuration = clampDuration((int) Math.round(durationMs.getValDouble()));
        sequenceRunning = true;

        Thread worker = new Thread(() -> runTimedSwapSequence(totemSlot, requestedDuration), "AutoTotemSequence");
        worker.setDaemon(true);
        worker.start();
    }

    private void runTimedSwapSequence(int totemSlot, int durationMs) {
        long startNanos = System.nanoTime();
        long totalNanos = Math.max(0L, durationMs) * 1_000_000L;
        long swapAtNanos = totalNanos / 2L;

        try {
            if (findTotemSlot() == -1) return;

            executeOnClientThread(() -> {
                if (mc.player != null && !(mc.screen instanceof InventoryScreen)) {
                    mc.setScreen(new InventoryScreen(mc.player));
                }
            });

            sleepUntil(startNanos + swapAtNanos);

            executeOnClientThread(() -> performMouseSwapToOffhand(totemSlot));

            sleepUntil(startNanos + totalNanos);

            executeOnClientThread(() -> {
                if (mc.screen instanceof InventoryScreen) {
                    mc.setScreen(null);
                }
            });
        } finally {
            sequenceRunning = false;
        }
    }

    private void performMouseSwapToOffhand(int totemSlot) {
        if (mc.player == null || mc.gameMode == null) return;
        if (!(mc.screen instanceof InventoryScreen invScreen)) return;
        if (totemSlot < 0 || totemSlot >= invScreen.getMenu().slots.size()) return;

        moveCursorToSlot(invScreen, invScreen.getMenu().getSlot(totemSlot));
        mc.gameMode.handleInventoryMouseClick(mc.player.inventoryMenu.containerId, totemSlot, 40, ClickType.SWAP, mc.player);
    }

    private void moveCursorToSlot(InventoryScreen screen, Slot slot) {
        try {
            java.lang.reflect.Field leftPosField = AbstractContainerScreen.class.getDeclaredField("leftPos");
            java.lang.reflect.Field topPosField = AbstractContainerScreen.class.getDeclaredField("topPos");
            leftPosField.setAccessible(true);
            topPosField.setAccessible(true);

            int leftPos = leftPosField.getInt(screen);
            int topPos = topPosField.getInt(screen);

            double targetX = slot.x + leftPos + 8;
            double targetY = slot.y + topPos + 8;

            long windowHandle = 0L;
            for (java.lang.reflect.Field field : mc.getWindow().getClass().getDeclaredFields()) {
                if (field.getType() == long.class) {
                    field.setAccessible(true);
                    windowHandle = field.getLong(mc.getWindow());
                    break;
                }
            }

            if (windowHandle != 0L) {
                double scale = mc.getWindow().getGuiScale();
                org.lwjgl.glfw.GLFW.glfwSetCursorPos(windowHandle, targetX * scale, targetY * scale);
            }
        } catch (Exception ignored) {
        }
    }

    private int findTotemSlot() {
        if (mc.player == null) return -1;

        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getItem(i).is(Items.TOTEM_OF_UNDYING)) {
                return i + 36;
            }
        }

        for (int i = 9; i < 36; i++) {
            if (mc.player.getInventory().getItem(i).is(Items.TOTEM_OF_UNDYING)) {
                return i;
            }
        }

        return -1;
    }

    private void executeOnClientThread(Runnable task) {
        CountDownLatch latch = new CountDownLatch(1);
        mc.execute(() -> {
            try {
                task.run();
            } finally {
                latch.countDown();
            }
        });

        try {
            latch.await(1, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    private void sleep(int ms) {
        if (ms <= 0) return;
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    private int clampDuration(int inputMs) {
        return Math.max(MIN_DURATION_MS, Math.min(MAX_DURATION_MS, inputMs));
    }

    private void sleepUntil(long targetNanos) {
        while (true) {
            long remainingNanos = targetNanos - System.nanoTime();
            if (remainingNanos <= 0L) return;

            long sleepMillis = remainingNanos / 1_000_000L;
            if (sleepMillis > 1L) {
                sleep((int) Math.min(Integer.MAX_VALUE, sleepMillis - 1L));
            } else {
                sleep(1);
            }
        }
    }
}
