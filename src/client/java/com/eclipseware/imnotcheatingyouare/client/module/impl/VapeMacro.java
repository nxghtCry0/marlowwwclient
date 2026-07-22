package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.utils.ModuleUtils;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public class VapeMacro extends Module {
    public VapeMacro() {
        super("VapeMacro", Category.Exploit, "Configure a macro to swap to an item name and right click.");
    }

    @Override
    public void onEnable() {
        if (mc.player == null || mc.gameMode == null || mc.getConnection() == null) {
            this.setToggled(false);
            return;
        }

        Setting targetSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Target Item");
        if (targetSetting == null) {
            this.setToggled(false);
            return;
        }

        String query = targetSetting.getValText().toLowerCase();
        if (query.isEmpty()) {
            this.setToggled(false);
            return;
        }

        int targetSlot = -1;
        int originalSlot = ModuleUtils.getSelectedSlot();
        boolean swapped = false;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (!stack.isEmpty()) {
                String regName = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().toLowerCase();
                String displayName = stack.getHoverName().getString().toLowerCase();
                if (regName.contains(query) || displayName.contains(query)) {
                    targetSlot = i;
                    break;
                }
            }
        }

        if (targetSlot == -1) {
            for (int i = 9; i < 36; i++) {
                ItemStack stack = mc.player.getInventory().getItem(i);
                if (!stack.isEmpty()) {
                    String regName = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().toLowerCase();
                    String displayName = stack.getHoverName().getString().toLowerCase();
                    if (regName.contains(query) || displayName.contains(query)) {
                        mc.gameMode.handleContainerInput(
                            mc.player.inventoryMenu.containerId,
                            i,
                            originalSlot,
                            net.minecraft.world.inventory.ContainerInput.SWAP,
                            mc.player
                        );
                        targetSlot = originalSlot;
                        swapped = true;
                        break;
                    }
                }
            }
        }

        if (targetSlot != -1) {
            if (!swapped) {
                ModuleUtils.switchToSlot(targetSlot);
            }

            com.eclipseware.imnotcheatingyouare.client.utils.ModuleUtils.useItemPacket(mc.player.getYRot(), mc.player.getXRot());

            if (!swapped) {
                ModuleUtils.switchToSlot(originalSlot);
            }
        }

        this.setToggled(false);
    }
}
