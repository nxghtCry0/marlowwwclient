package com.eclipseware.imnotcheatingyouare.mixin.client;

import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Inventory.class)
public interface InventoryAccessor {
    @Accessor("selected")
    int getSelected();

    @Accessor("selected")
    void setSelected(int selected);
}
