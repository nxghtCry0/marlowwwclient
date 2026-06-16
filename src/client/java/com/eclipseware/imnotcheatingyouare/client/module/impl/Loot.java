package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.ContainerInput;

import java.util.ArrayList;
import java.util.Random;

public class Loot extends Module {
    private final Setting delaySetting;
    private final Setting autoCloseSetting;
    private final Setting startDelaySetting;
    private final Setting focusSameTypeSetting;
    private final Setting randomizeSetting;

    private long lastClickTime = 0L;
    private long openTime = 0L;
    private boolean wasScreenOpen = false;
    private net.minecraft.world.item.Item focusedItem = null;
    private final Random random = new Random();

    public Loot() {
        super("Loot", Category.World, "Automatically steals items from chests with configurable and safe delays.");
        
        delaySetting = new Setting("Delay (ms)", this, 100.0, 0.0, 1000.0, true);
        autoCloseSetting = new Setting("Auto Close", this, true);
        startDelaySetting = new Setting("Start Delay (ms)", this, 150.0, 0.0, 1000.0, true);
        focusSameTypeSetting = new Setting("Focus Same Type", this, false);
        randomizeSetting = new Setting("Randomize Slots", this, false);

        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(delaySetting);
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(autoCloseSetting);
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(startDelaySetting);
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(focusSameTypeSetting);
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(randomizeSetting);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.gameMode == null) {
            return;
        }

        if (mc.screen instanceof ContainerScreen containerScreen) {
            if (!wasScreenOpen) {
                openTime = System.currentTimeMillis();
                lastClickTime = openTime;
                wasScreenOpen = true;
                focusedItem = null;
                return;
            }

            long now = System.currentTimeMillis();
            
            if (now - openTime < startDelaySetting.getValDouble()) {
                return;
            }

            double baseDelay = delaySetting.getValDouble();
            double jitter = baseDelay > 0 ? random.nextInt(40) : 0;
            if (now - lastClickTime < (baseDelay + jitter)) {
                return;
            }

            AbstractContainerMenu menu = containerScreen.getMenu();
            java.util.List<Slot> validSlots = new ArrayList<>();
            for (Slot slot : menu.slots) {
                if (slot.container != mc.player.getInventory() && slot.hasItem()) {
                    validSlots.add(slot);
                }
            }

            if (validSlots.isEmpty()) {
                focusedItem = null;
                if (autoCloseSetting.getValBoolean()) {
                    mc.player.closeContainer();
                    mc.setScreen(null);
                    wasScreenOpen = false;
                }
                return;
            }

            java.util.List<Slot> candidateSlots = new ArrayList<>();
            if (focusSameTypeSetting.getValBoolean() && focusedItem != null) {
                for (Slot slot : validSlots) {
                    if (slot.getItem().getItem() == focusedItem) {
                        candidateSlots.add(slot);
                    }
                }
            }

            if (candidateSlots.isEmpty()) {
                candidateSlots.addAll(validSlots);
                focusedItem = null;
            }

            Slot targetSlot = null;
            if (randomizeSetting.getValBoolean()) {
                targetSlot = candidateSlots.get(random.nextInt(candidateSlots.size()));
            } else {
                targetSlot = candidateSlots.get(0);
            }

            if (targetSlot != null) {
                if (focusSameTypeSetting.getValBoolean()) {
                    focusedItem = targetSlot.getItem().getItem();
                }

                int containerSlotId = -1;
                for (int i = 0; i < menu.slots.size(); i++) {
                    if (menu.getSlot(i) == targetSlot) {
                        containerSlotId = i;
                        break;
                    }
                }

                if (containerSlotId != -1) {
                    mc.gameMode.handleContainerInput(
                            menu.containerId,
                            containerSlotId,
                            0,
                            ContainerInput.QUICK_MOVE,
                            mc.player
                    );
                    lastClickTime = now;
                }
            }
        } else {
            wasScreenOpen = false;
            focusedItem = null;
        }
    }
}
