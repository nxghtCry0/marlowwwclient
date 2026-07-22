package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CartRefill extends Module {
    private final Random random = new Random();
    private int stage = 0;
    private long lastActionTime = 0L;
    private long currentDelay = 0L;
    private final List<Integer> targetHotbarSlots = new ArrayList<>();
    private final List<Integer> slotsToRefill = new ArrayList<>();

    public CartRefill() {
        super("CartRefill", Category.CartPvP, "Automatically refills TNT minecarts into chosen hotbar slots.");
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Smart Mode", this, true));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Auto Open", this, true));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Auto Close", this, true));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Min Open Delay", this, 50.0, 0.0, 300.0, true));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Max Open Delay", this, 150.0, 0.0, 300.0, true));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Min Refill Delay", this, 50.0, 0.0, 300.0, true));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Max Refill Delay", this, 150.0, 0.0, 300.0, true));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Min Close Delay", this, 50.0, 0.0, 300.0, true));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Max Close Delay", this, 150.0, 0.0, 300.0, true));

        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Refill Slot 1", this, false));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Refill Slot 2", this, false));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Refill Slot 3", this, false));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Refill Slot 4", this, false));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Refill Slot 5", this, false));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Refill Slot 6", this, false));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Refill Slot 7", this, false));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Refill Slot 8", this, false));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Refill Slot 9", this, false));
    }

    private double getVal(String name) {
        Setting s = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, name);
        return s != null ? s.getValDouble() : 0.0;
    }

    private boolean getBool(String name) {
        Setting s = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, name);
        return s != null && s.getValBoolean();
    }

    @Override
    public void onEnable() {
        if (mc.player == null) {
            this.toggle();
            return;
        }
        updateTargetSlots();
        stage = 0;
        lastActionTime = 0L;
        currentDelay = 0L;
        slotsToRefill.clear();

        if (!getBool("Smart Mode")) {
            if (!hasCartsInMainInventory()) {
                this.toggle();
                return;
            }
            populateSlotsToRefill();
            if (slotsToRefill.isEmpty()) {
                this.toggle();
                return;
            }
            startRefillSequence();
        }
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.gameMode == null) {
            if (this.isToggled()) {
                this.toggle();
            }
            return;
        }
        updateTargetSlots();

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastActionTime < currentDelay) {
            return;
        }

        switch (stage) {
            case 0:
                if (getBool("Smart Mode") && hasCartsInMainInventory()) {
                    populateSlotsToRefill();
                    if (!slotsToRefill.isEmpty()) {
                        startRefillSequence();
                    }
                }
                break;

            case 1:
                if (getBool("Auto Open") && !(mc.gui.screen() instanceof InventoryScreen)) {
                    mc.setScreenAndShow(new InventoryScreen(mc.player));
                }
                stage = 2;
                lastActionTime = System.currentTimeMillis();
                currentDelay = getDelay(getVal("Min Open Delay"), getVal("Max Open Delay"));
                break;

            case 2:
                if (slotsToRefill.isEmpty()) {
                    stage = 3;
                    lastActionTime = System.currentTimeMillis();
                    currentDelay = 0L;
                    break;
                }
                int hotbarSlotIndex = slotsToRefill.remove(0);
                int inventoryCartSlot = findCartInMainInventory();

                if (inventoryCartSlot != -1) {
                    mc.gameMode.handleContainerInput(
                        mc.player.inventoryMenu.containerId,
                        inventoryCartSlot,
                        hotbarSlotIndex,
                        ContainerInput.SWAP,
                        mc.player
                    );
                    lastActionTime = System.currentTimeMillis();
                    currentDelay = getDelay(getVal("Min Refill Delay"), getVal("Max Refill Delay"));
                } else {
                    slotsToRefill.clear();
                }
                break;

            case 3:
                if (getBool("Auto Close") && mc.gui.screen() instanceof InventoryScreen) {
                    mc.player.closeContainer();
                }
                stage = 4;
                lastActionTime = System.currentTimeMillis();
                currentDelay = getDelay(getVal("Min Close Delay"), getVal("Max Close Delay"));
                break;

            case 4:
                if (getBool("Smart Mode")) {
                    stage = 0;
                } else {
                    this.toggle();
                }
                break;
        }
    }

    private void startRefillSequence() {
        stage = 1;
        lastActionTime = System.currentTimeMillis();
        currentDelay = 0L;
    }

    private void updateTargetSlots() {
        targetHotbarSlots.clear();
        addSlotIfValid("Refill Slot 1", 0);
        addSlotIfValid("Refill Slot 2", 1);
        addSlotIfValid("Refill Slot 3", 2);
        addSlotIfValid("Refill Slot 4", 3);
        addSlotIfValid("Refill Slot 5", 4);
        addSlotIfValid("Refill Slot 6", 5);
        addSlotIfValid("Refill Slot 7", 6);
        addSlotIfValid("Refill Slot 8", 7);
        addSlotIfValid("Refill Slot 9", 8);
    }

    private void addSlotIfValid(String settingName, int slotIndex) {
        if (getBool(settingName)) {
            targetHotbarSlots.add(slotIndex);
        }
    }

    private void populateSlotsToRefill() {
        slotsToRefill.clear();
        for (int slot : targetHotbarSlots) {
            ItemStack stack = mc.player.getInventory().getItem(slot);
            if (stack.isEmpty() || stack.getItem() != Items.TNT_MINECART) {
                slotsToRefill.add(slot);
            }
        }
    }

    private boolean hasCartsInMainInventory() {
        return findCartInMainInventory() != -1;
    }

    private int findCartInMainInventory() {
        for (int i = 9; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() == Items.TNT_MINECART) {
                return i;
            }
        }
        return -1;
    }

    private long getDelay(double min, double max) {
        if (min >= max) {
            return (long) min;
        }
        return (long) (min + random.nextDouble() * (max - min));
    }
}
