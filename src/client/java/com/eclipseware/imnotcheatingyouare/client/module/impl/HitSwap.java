package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

public class HitSwap extends Module {
    public static HitSwap INSTANCE;
    private int originalSlot = -1;
    private boolean swapped = false;

    public HitSwap() {
        super("HitSwap", Category.Combat, "Automatically swaps to best weapon before hitting, then swaps back.");
        INSTANCE = this;
    }

    private int deferredSlot = -1;

    public void onPreAttack(Entity target) {
        if (!isToggled() || mc.player == null) return;
        
        int bestSlot = getBestWeapon();
        originalSlot = mc.player.getInventory().getSelectedSlot();
        
        if (bestSlot != -1 && bestSlot != originalSlot) {
            com.eclipseware.imnotcheatingyouare.client.utils.ModuleUtils.setServerSlot(bestSlot);
            swapped = true;
        }
    }

    public void onPostAttack(Entity target) {
        if (swapped) {
            deferredSlot = originalSlot;
            swapped = false;
        }
    }

    @Override
    public void onTick() {
        if (deferredSlot != -1) {
            com.eclipseware.imnotcheatingyouare.client.utils.ModuleUtils.setServerSlot(deferredSlot);
            deferredSlot = -1;
        }
    }

    private int getBestWeapon() {
        int bestSlot = -1;
        double bestDamage = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;
            
            double dmg = 1.0;
            String name = stack.getItem().toString().toLowerCase();
            if (name.contains("sword")) {
                if (name.contains("netherite")) dmg = 8;
                else if (name.contains("diamond")) dmg = 7;
                else if (name.contains("iron")) dmg = 6;
                else dmg = 5;
            } else if (name.contains("axe")) {
                if (name.contains("netherite") || name.contains("diamond")) dmg = 9;
                else dmg = 7;
            }
            
            if (dmg > bestDamage) {
                bestDamage = dmg;
                bestSlot = i;
            }
        }
        return bestSlot;
    }
}
