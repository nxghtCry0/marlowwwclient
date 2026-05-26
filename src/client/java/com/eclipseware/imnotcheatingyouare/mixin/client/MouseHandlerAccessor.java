package com.eclipseware.imnotcheatingyouare.mixin.client;

import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MouseHandler.class)
public interface MouseHandlerAccessor {
    @Invoker("onButton")
    void invokeOnButton(long windowHandle, net.minecraft.client.input.MouseButtonInfo buttonInfo, int action);
}
