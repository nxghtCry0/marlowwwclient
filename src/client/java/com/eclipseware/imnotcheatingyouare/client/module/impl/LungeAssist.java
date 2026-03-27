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
            // If we don't have a lunge spear, just toggle the module normally
            super.onKeybind();
            return;
        }
        
        int oldSlot = mc.player.getInventory().getSelectedSlot();
        if (oldSlot == spearSlot) return; // Pointless to swap if we already hold it
        
        // Swap to spear -> Trigger the Lunge action (Attack or Use) -> Swap back instantly
        mc.getConnection().send(new ServerboundSetCarriedItemPacket(spearSlot));
        
        if (mc.hitResult != null && mc.hitResult.getType() == net.minecraft.world.phys.HitResult.Type.ENTITY) {
            net.minecraft.world.phys.EntityHitResult ehr = (net.minecraft.world.phys.EntityHitResult) mc.hitResult;
            mc.getConnection().send(net.minecraft.network.protocol.game.ServerboundInteractPacket.createAttackPacket(ehr.getEntity(), mc.player.isShiftKeyDown()));
        } else {
            mc.getConnection().send(new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, 0, mc.player.getYRot(), mc.player.getXRot()));
        }
        
        mc.player.swing(InteractionHand.MAIN_HAND);
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