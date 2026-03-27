package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MaceItem;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;

public class BreachSwap extends Module {
private boolean needsSwapBack = false;
private long swapBackTime = 0;
private int originalSlot = -1;

public BreachSwap() {
    super("BreachSwap", Category.Combat);
}

public boolean handleAttack(Entity target, Player player) {
    if (!this.isToggled()) return false;

    int maceSlot = findBreachMace(player);
    if (maceSlot == -1) return false;

    int oldSlot = player.getInventory().getSelectedSlot();
    if (oldSlot == maceSlot) return false; // Already holding it

    Setting modeSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Mode");
    String mode = modeSetting != null ? modeSetting.getValString() : "Swap";

    if (mode.equals("Silent")) {
        // Silent attribute swap: Packet to Mace -> Attack -> Packet to Original
        if (mc.getConnection() != null) {
            mc.getConnection().send(new ServerboundSetCarriedItemPacket(maceSlot));
            mc.getConnection().send(ServerboundInteractPacket.createAttackPacket(target, player.isShiftKeyDown()));
            player.swing(InteractionHand.MAIN_HAND);
            mc.getConnection().send(new ServerboundSetCarriedItemPacket(oldSlot));
            
            // CRITICAL FIX: Reset the client-side cooldown visually so the hit registers properly on the UI
            player.resetAttackStrengthTicker();
        }
        return true; // Cancel physical hit
    } else {
        // Swap mode: Physically swap hotbar slot
        player.getInventory().setSelectedSlot(maceSlot);
        if (mc.getConnection() != null) {
            mc.getConnection().send(new ServerboundSetCarriedItemPacket(maceSlot));
        }
        
        Setting swapBackSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Swap Back");
        if (swapBackSetting != null && swapBackSetting.getValBoolean()) {
            Setting swapDelaySetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Swap Back Delay (ms)");
            long swapDelay = swapDelaySetting != null ? (long) swapDelaySetting.getValDouble() : 0;
            
            needsSwapBack = true;
            swapBackTime = System.currentTimeMillis() + swapDelay;
            originalSlot = oldSlot;
        }
        
        return false; // Let the normal attack go through with the newly selected mace
    }
}

@Override
public void onTick() {
    if (needsSwapBack && mc.player != null) {
        if (System.currentTimeMillis() >= swapBackTime) {
            mc.player.getInventory().setSelectedSlot(originalSlot);
            if (mc.getConnection() != null) {
                mc.getConnection().send(new ServerboundSetCarriedItemPacket(originalSlot));
            }
            needsSwapBack = false;
        }
    }
}

@Override
public void onDisable() {
    needsSwapBack = false;
}

private int findBreachMace(Player player) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof MaceItem) {
for (var enchant : stack.getEnchantments().keySet()) {
// Bypass direct getter mappings by converting the ResourceKey to a string
if (enchant.unwrapKey().isPresent() && enchant.unwrapKey().get().toString().contains("breach")) {
return i;
}
}
}
        }
        return -1;
    }
}