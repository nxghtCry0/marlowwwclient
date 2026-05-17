package com.eclipseware.imnotcheatingyouare.client.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;

import java.util.LinkedList;
import java.util.Queue;

public class SpoofManager {
    private static final Minecraft mc = Minecraft.getInstance();
    
    private static class SpoofTask {
        int targetSlot;
        int originalSlot;
        Runnable action;
        int state = 0; 
        int waitTicks = 0;
    }
    
    private static final Queue<SpoofTask> tasks = new LinkedList<>();
    private static SpoofTask currentTask = null;

    public static void silentUse(int targetSlot, Runnable action) {
        if (mc.player == null) return;
        
        int oldSlot = ModuleUtils.getSelectedSlot();
        if (oldSlot == targetSlot) {
            action.run();
            return;
        }

        SpoofTask t = new SpoofTask();
        t.targetSlot = targetSlot;
        t.originalSlot = oldSlot;
        t.action = action;
        tasks.add(t);
    }

    public static void onTick() {
        if (currentTask == null && !tasks.isEmpty()) {
            currentTask = tasks.poll();
            ModuleUtils.setClientSlot(currentTask.targetSlot);
            currentTask.state = 1;
            currentTask.waitTicks = 1; // Wait 1 tick before using item
        } else if (currentTask != null) {
            currentTask.waitTicks--;
            if (currentTask.waitTicks <= 0) {
                if (currentTask.state == 1) {
                    currentTask.action.run();
                    currentTask.state = 2;
                    currentTask.waitTicks = 1; // Wait 1 tick before switching back
                } else if (currentTask.state == 2) {
                    ModuleUtils.setClientSlot(currentTask.originalSlot);
                    currentTask = null; // Task finished
                }
            }
        }
    }
}
