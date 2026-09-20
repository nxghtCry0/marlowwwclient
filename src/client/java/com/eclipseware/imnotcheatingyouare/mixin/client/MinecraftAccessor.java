package com.eclipseware.imnotcheatingyouare.mixin.client;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Minecraft.class)
public interface MinecraftAccessor {
    @Invoker("startAttack")
    boolean invokeStartAttack();

    @Invoker("startUseItem")
    void invokeStartUseItem();

    @org.spongepowered.asm.mixin.gen.Accessor("missTime")
    void setMissTime(int missTime);

    @org.spongepowered.asm.mixin.gen.Accessor("rightClickDelay")
    void setRightClickDelay(int delay);
}