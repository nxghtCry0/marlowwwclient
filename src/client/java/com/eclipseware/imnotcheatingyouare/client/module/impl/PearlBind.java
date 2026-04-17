package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.InteractionResult;

public class PearlBind extends Module {
    public PearlBind() {
        super("PearlBind", Category.Movement, "Throws an ender pearl automatically when bound key is pressed");
        setSubCategory("Crystal PvP");
    }

    @Override
    public void onEnable() {
        if (mc.player == null || mc.level == null) {
            setToggled(false);
            return;
        }

        int pearlSlot = -1;
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getItem(i).getItem() == Items.ENDER_PEARL) {
                pearlSlot = i;
                break;
            }
        }

        if (pearlSlot != -1) {
            int oldSlot = mc.player.getInventory().getSelectedSlot();
            com.eclipseware.imnotcheatingyouare.client.utils.ModuleUtils.switchToSlot(pearlSlot);
            
            mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
            
            com.eclipseware.imnotcheatingyouare.client.utils.ModuleUtils.switchToSlot(oldSlot);
            
            mc.player.swing(InteractionHand.MAIN_HAND);
        }

        // Always toggle off after use
        setToggled(false);
    }
}
