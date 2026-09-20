package com.eclipseware.imnotcheatingyouare.mixin.client;

import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseScrollMixin {

    @Inject(method = "onScroll", at = @At("HEAD"))
    private void onScroll(long window, double xOffset, double yOffset, CallbackInfo ci) {
        xyz.breadloaf.imguimc.imgui.SdlImGuiPlatform.feedScroll(yOffset);
    }
}
