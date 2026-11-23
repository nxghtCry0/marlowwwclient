package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.setting.SettingsManager;
import com.eclipseware.imnotcheatingyouare.client.utils.ModuleUtils;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MaceItem;

public class BreachSwap extends Module {
    private boolean needsSwapBack = false;
    private boolean internalAttack = false;
    private int originalSlot = -1;
    private int ticksWaited = 0;

    public BreachSwap() {
        super("BreachSwap", Category.Exploit);
    }

    public boolean handleAttack(Entity target, Player player) {
        if (!this.isToggled() || internalAttack || mc == null || mc.gameMode == null) {
            return false;
        }
        int maceSlot = findBreachMace(player);
        if (maceSlot == -1) {
            return false;
        }
        Inventory inv = player.getInventory();
        int oldSlot = inv.getSelectedSlot();
        if (oldSlot == maceSlot) {
            return false;
        }

        Setting modeSetting = getSetting("Mode");
        String mode = modeSetting != null ? modeSetting.getValString() : "Swap";
        Setting swapBackSetting = getSetting("Swap Back");
        boolean swapBack = swapBackSetting == null || swapBackSetting.getValBoolean();

        if ("Silent".equalsIgnoreCase(mode)) {
            ModuleUtils.switchToSlot(maceSlot);
            internalAttack = true;
            try {
                mc.gameMode.attack(player, target);
                ModuleUtils.swingHand(player, InteractionHand.MAIN_HAND);
            } finally {
                internalAttack = false;
            }
            if (swapBack) {
                needsSwapBack = true;
                originalSlot = oldSlot;
                ticksWaited = 0;
            }
            return true;
        }

        ModuleUtils.switchToSlot(maceSlot);
        if (swapBack) {
            needsSwapBack = true;
            originalSlot = oldSlot;
            ticksWaited = 0;
        }
        return false;
    }

    @Override
    public void onTick() {
        if (!needsSwapBack || mc == null || mc.player == null) {
            return;
        }
        ticksWaited++;
        Setting delaySetting = getSetting("Swap Back Delay (ms)");
        int minTicks = delaySetting != null ? Math.max(1, (int) Math.round(delaySetting.getValDouble() / 50.0)) : 1;
        if (ticksWaited >= minTicks) {
            if (originalSlot >= 0 && originalSlot < 9) {
                ModuleUtils.switchToSlot(originalSlot);
            }
            needsSwapBack = false;
            originalSlot = -1;
        }
    }

    @Override
    public void onDisable() {
        if (needsSwapBack && mc != null && mc.player != null && originalSlot >= 0 && originalSlot < 9) {
            ModuleUtils.switchToSlot(originalSlot);
        }
        needsSwapBack = false;
        originalSlot = -1;
    }

    private int findBreachMace(Player player) {
        Inventory inv = player.getInventory();
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.getItem() instanceof MaceItem && AutoMace.hasEnchantmentKeyword(stack, "breach")) {
                return i;
            }
        }
        return -1;
    }

    private Setting getSetting(String name) {
        ImnotcheatingyouareClient client = ImnotcheatingyouareClient.INSTANCE;
        if (client == null || client.settingsManager == null) {
            return null;
        }
        SettingsManager sm = client.settingsManager;
        return sm.getSettingByName(this, name);
    }
}
