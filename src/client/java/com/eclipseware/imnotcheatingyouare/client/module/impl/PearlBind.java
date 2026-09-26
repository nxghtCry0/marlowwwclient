package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.InteractionResult;

public class PearlBind extends Module {
    public PearlBind() {
        super("PearlBind", Category.Utility, "Throws an ender pearl automatically when bound key is pressed");
        setSubCategory("Crystal PvP");
    }

    private int originalSlot = -1;
    private int phase = 0;
    
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
            originalSlot = com.eclipseware.imnotcheatingyouare.client.utils.ModuleUtils.getSelectedSlot();
            com.eclipseware.imnotcheatingyouare.client.utils.ModuleUtils.switchToSlot(pearlSlot);
            phase = 1; 
        } else {
            setToggled(false);
        }
    }

    @Override
    public void onTick() {
        if (mc.player == null) return;
        
        if (phase == 1) {
            mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
            mc.player.swing(InteractionHand.MAIN_HAND);
            phase = 2; 
        } else if (phase == 2) {
            com.eclipseware.imnotcheatingyouare.client.utils.ModuleUtils.switchToSlot(originalSlot);
            setToggled(false);
            phase = 0;
        }
    }
}
