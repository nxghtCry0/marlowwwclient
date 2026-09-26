package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.utils.ModuleUtils;
import com.eclipseware.imnotcheatingyouare.mixin.client.MinecraftAccessor;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class KeybindWeb extends Module {
    public KeybindWeb() {
        super("KeybindWeb", Category.UHC, "Silently places cobweb at your feet or targeted block.");
    }

    @Override
    public void onEnable() {
        if (mc.player == null || mc.gameMode == null || mc.getConnection() == null) {
            this.setToggled(false);
            return;
        }

        int originalSlot = ModuleUtils.getSelectedSlot();
        int webSlot = ModuleUtils.findItemInHotbar(Items.COBWEB);
        boolean swapped = false;

        if (webSlot == -1) {
            for (int i = 9; i < 36; i++) {
                ItemStack stack = mc.player.getInventory().getItem(i);
                if (stack.is(Items.COBWEB)) {
                    mc.gameMode.handleContainerInput(
                        mc.player.inventoryMenu.containerId,
                        i,
                        originalSlot,
                        net.minecraft.world.inventory.ContainerInput.SWAP,
                        mc.player
                    );
                    webSlot = originalSlot;
                    swapped = true;
                    break;
                }
            }
        }

        if (webSlot != -1) {
            if (!swapped) {
                ModuleUtils.switchToSlot(webSlot);
            }
            
            ((MinecraftAccessor) mc).invokeStartUseItem();
            mc.player.swing(InteractionHand.MAIN_HAND);

            if (!swapped) {
                ModuleUtils.switchToSlot(originalSlot);
            }
        }

        this.setToggled(false);
    }
}

