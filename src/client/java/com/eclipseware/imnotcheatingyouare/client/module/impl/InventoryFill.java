package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.mixin.client.AbstractContainerScreenAccessor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;

public class InventoryFill extends Module {

    public InventoryFill() {
        super("InventoryFill", Category.Farming, "While holding shift, automatically clicks while in inventory to quickly move items.");
        setSubCategory("Inventory");
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.gameMode == null) return;
        if (!(mc.gui.screen() instanceof AbstractContainerScreen<?> screen)) return;
        if (!mc.options.keyShift.isDown()) return;

        Slot hovered = ((AbstractContainerScreenAccessor) screen).getHoveredSlot();
        if (hovered == null || !hovered.hasItem()) return;

        mc.gameMode.handleContainerInput(screen.getMenu().containerId, hovered.index, 0, ContainerInput.QUICK_MOVE, mc.player);
    }
}
