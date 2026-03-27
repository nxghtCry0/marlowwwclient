package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class LungeAssist extends Module {
    public LungeAssist() {
        super("LungeAssist", Category.Combat);
    }

    @Override
public void onKeybind() {
if (mc.player == null || mc.getConnection() == null) return;

    int spearSlot = findLungeSpear(mc.player);
    if (spearSlot == -1) {
        super.onKeybind();
        return;
    }
    
    int oldSlot = mc.player.getInventory().getSelectedSlot();
    if (oldSlot == spearSlot) {
        mc.player.swing(InteractionHand.MAIN_HAND);
        return;
    }
    
    // Physically swap the slot to force client-side attributes to apply
    mc.player.getInventory().setSelectedSlot(spearSlot);
    mc.getConnection().send(new ServerboundSetCarriedItemPacket(spearSlot));
    
    // Punch the air
    mc.player.swing(InteractionHand.MAIN_HAND);
    
    // Immediately swap back in the same tick
    mc.player.getInventory().setSelectedSlot(oldSlot);
    mc.getConnection().send(new ServerboundSetCarriedItemPacket(oldSlot));
}

    private int findLungeSpear(Player player) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            String itemName = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
            
            if (itemName.contains("spear")) {
for (var enchant : stack.getEnchantments().keySet()) {
// Bypass direct getter mappings by converting the ResourceKey to a string
if (enchant.unwrapKey().isPresent() && enchant.unwrapKey().get().toString().contains("lunge")) {
return i;
}
}
}
        }
        return -1;
    }
}