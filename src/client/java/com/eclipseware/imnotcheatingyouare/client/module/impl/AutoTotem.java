package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import net.minecraft.world.item.Items;
import java.util.ArrayList;
import java.util.Arrays;

public class AutoTotem extends Module {
    private Setting mode;
    private Setting activationMode;
    private Setting healthThreshold;
    private Setting popDelay;

    private int popTickGrace = 0;
    private int antiDec = -1;
    private int fifteenTickSafety = -1;
    
    private int swapDelayCounter = -1;
    private int queuedSwapSlot = -1;

    public AutoTotem() {
        super("AutoTotem", Category.Combat, "Silently replaces totems automatically.");
        setSubCategory("Crystal PvP");
        
        mode = new Setting("Mode", this, "Crystal", new ArrayList<>(Arrays.asList("Crystal", "SMP")));
        activationMode = new Setting("Activation", this, "Always", new ArrayList<>(Arrays.asList("Always", "Low HP")));
        healthThreshold = new Setting("Health Threshold", this, 10, 1, 20, true);
        popDelay = new Setting("Pop Delay (Ticks)", this, 0, 0, 10, true);
        
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(mode);
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(activationMode);
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(healthThreshold);
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(popDelay);
    }

    @Override
    public void onEnable() {
        antiDec = -1;
        fifteenTickSafety = -1;
        swapDelayCounter = -1;
        queuedSwapSlot = -1;
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.level == null) return;

        if (swapDelayCounter > 0) {
            swapDelayCounter--;
            return;
        } else if (swapDelayCounter == 0 && queuedSwapSlot != -1) {
            performSilentSwap(queuedSwapSlot);
            swapDelayCounter = -1;
            queuedSwapSlot = -1;
            return;
        }

        boolean NeedsTotem = mc.player.getOffhandItem().getItem() != Items.TOTEM_OF_UNDYING;
        boolean activationActive = activationMode.getValString().equals("Always") || mc.player.getHealth() <= healthThreshold.getValDouble();

        if (mode.getValString().equals("SMP")) {
            if (mc.player.getHealth() > healthThreshold.getValDouble()) {
                if (mc.player.getOffhandItem().getItem() != Items.SHIELD) {
                    queueSwap(findItem(Items.SHIELD, true));
                }
                return;
            }
        }

        if (NeedsTotem && activationActive) {
            queueSwap(findItem(Items.TOTEM_OF_UNDYING, true)); 
        }
    }

    private void queueSwap(int slot) {
        if (slot == -1) return;
        int delay = Math.max(3, (int) popDelay.getValDouble()); 
        swapDelayCounter = delay;
        queuedSwapSlot = slot;
    }

    private void performSilentSwap(int slot) {
        if (mc.player == null) return;
        mc.player.setSprinting(false);
        
        if (mc.screen == null || mc.screen instanceof net.minecraft.client.gui.screens.inventory.InventoryScreen) {
            int containerId = mc.player.inventoryMenu.containerId;
            mc.gameMode.handleInventoryMouseClick(containerId, slot, 40, net.minecraft.world.inventory.ClickType.SWAP, mc.player);
        }
    }

    private int findItem(net.minecraft.world.item.Item item, boolean prioritizeHotbar) {
        if (prioritizeHotbar) {
            for (int i = 0; i < 9; i++) {
                if (mc.player.getInventory().getItem(i).getItem() == item) return i + 36;
            }
            for (int i = 9; i < 36; i++) {
                if (mc.player.getInventory().getItem(i).getItem() == item) return i;
            }
        } else {
            for (int i = 9; i < 36; i++) {
                if (mc.player.getInventory().getItem(i).getItem() == item) return i;
            }
            for (int i = 0; i < 9; i++) {
                if (mc.player.getInventory().getItem(i).getItem() == item) return i + 36;
            }
        }
        return -1;
    }
}
