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

public class BreachSwap extends Module {
    public BreachSwap() {
        super("BreachSwap", Category.Combat);
    }

    public boolean handleAttack(Entity target, Player player) {
        if (!this.isToggled()) return false;

        int maceSlot = findBreachMace(player);
        if (maceSlot == -1) return false;

        int oldSlot = player.getInventory().getSelectedSlot();
        if (oldSlot == maceSlot) return false; // Already holding it

        // Silent attribute swap: Packet to Mace -> Attack -> Packet to Original
        if (mc.getConnection() != null) {
            mc.getConnection().send(new ServerboundSetCarriedItemPacket(maceSlot));
            mc.getConnection().send(ServerboundInteractPacket.createAttackPacket(target, player.isShiftKeyDown()));
            player.swing(InteractionHand.MAIN_HAND);
            mc.getConnection().send(new ServerboundSetCarriedItemPacket(oldSlot));
        }
        
        return true; // We handled the attack manually
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