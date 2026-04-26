package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

public class AutoDHand extends Module {
    private int oldOffhandSlot = -1;
    private boolean swapped = false;

    public AutoDHand() {
        super("AutoDHand", Category.Combat, "Automatically swaps to a totem if above a crystal.");
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.level == null) return;

        boolean aboveCrystal = false;
        AABB checkArea = mc.player.getBoundingBox().inflate(1.0, 3.0, 1.0).move(0, -1.0, 0);
        for (net.minecraft.world.entity.Entity entity : mc.level.getEntities(mc.player, checkArea)) {
            if (entity instanceof EndCrystal) {
                aboveCrystal = true;
                break;
            }
        }

        boolean targetOffhand = true;
        boolean needsTotem = mc.player.getOffhandItem().getItem() != Items.TOTEM_OF_UNDYING;

        if (aboveCrystal && needsTotem) {
            int totemSlot = findItem(Items.TOTEM_OF_UNDYING, true);
            if (totemSlot != -1) {
                if (!swapped) {
                    oldOffhandSlot = findCurrentOffhandItemSlot();
                }
                performSilentSwap(totemSlot);
                swapped = true;
            }
        } else if (!aboveCrystal && swapped) {
            if (oldOffhandSlot != -1) {
                int offhandItemSlot = findItem(mc.player.getOffhandItem().getItem(), true);
                if (offhandItemSlot != -1 && offhandItemSlot != 40) {
                     performSilentSwap(oldOffhandSlot);
                }
            }
            swapped = false;
            oldOffhandSlot = -1;
        }
    }

    private int findCurrentOffhandItemSlot() {
        return 40;
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

    @Override
    public void onDisable() {
        if (swapped && oldOffhandSlot != -1 && mc.player != null) {
            performSilentSwap(oldOffhandSlot);
        }
        swapped = false;
        oldOffhandSlot = -1;
    }
}
