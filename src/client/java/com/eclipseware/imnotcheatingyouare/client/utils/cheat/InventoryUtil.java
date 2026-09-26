package com.eclipseware.imnotcheatingyouare.client.utils.cheat;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.AxeItem;

import net.minecraft.core.registries.BuiltInRegistries;

public class InventoryUtil {
    private static final Minecraft mc = Minecraft.getInstance();
    private static final int HOTBAR_SIZE = 9;

    public static int findItemInHotbar(Item item) {
        if (mc.player == null) return -1;
        for (int i = 0; i < HOTBAR_SIZE; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() == item) {
                return i;
            }
        }
        return -1;
    }

    public static int findAxeInHotbar() {
        if (mc.player == null) return -1;
        for (int i = 0; i < HOTBAR_SIZE; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.getItem() instanceof AxeItem) {
                return i;
            }
        }
        return -1;
    }

    public static int findSwordInHotbar() {
        if (mc.player == null) return -1;
        for (int i = 0; i < HOTBAR_SIZE; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (!stack.isEmpty() && getItemName(stack).contains("sword")) {
                return i;
            }
        }
        return -1;
    }

    public static int findWeaponInHotbar() {
        if (mc.player == null) return -1;
        for (int i = 0; i < HOTBAR_SIZE; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (!stack.isEmpty() && (getItemName(stack).contains("sword") || stack.getItem() instanceof AxeItem)) {
                return i;
            }
        }
        return -1;
    }

    public static boolean hasWeapon() {
        return findWeaponInHotbar() != -1;
    }

    public static boolean isHoldingWeapon() {
        if (mc.player == null) return false;
        ItemStack stack = mc.player.getMainHandItem();
        return !stack.isEmpty() && (getItemName(stack).contains("sword") || stack.getItem() instanceof AxeItem);
    }

    public static String getItemName(ItemStack stack) {
        if (stack.isEmpty()) return "";
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
    }

    public static boolean isShieldInHotbar() {
        if (mc.player == null) return false;
        for (int i = 0; i < HOTBAR_SIZE; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() instanceof net.minecraft.world.item.ShieldItem) {
                return true;
            }
        }
        return false;
    }

    public static int findItemContaining(String name) {
        if (mc.player == null) return -1;
        for (int i = 0; i < HOTBAR_SIZE; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (!stack.isEmpty() && getItemName(stack).contains(name)) {
                return i;
            }
        }
        return -1;
    }
}
