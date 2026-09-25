package com.eclipseware.imnotcheatingyouare.mixin.client;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MultiPlayerGameMode.class)
public interface MultiPlayerGameModeAccessor {
    @Accessor("carriedIndex")
    int getCarriedIndex();

    @Accessor("carriedIndex")
    void setCarriedIndex(int index);
}
