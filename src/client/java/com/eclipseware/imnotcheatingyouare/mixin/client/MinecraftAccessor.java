package com.eclipseware.imnotcheatingyouare.mixin.client;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Minecraft.class)
public interface MinecraftAccessor {
    @Invoker("startAttack")
    boolean invokeStartAttack();

    @Invoker("doItemUse")
    boolean invokeDoItemUse();
}