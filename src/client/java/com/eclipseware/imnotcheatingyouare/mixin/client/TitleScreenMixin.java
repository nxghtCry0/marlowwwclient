package com.eclipseware.imnotcheatingyouare.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public class TitleScreenMixin {
    private static boolean warned = false;

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        if (!warned) {
            SystemToast.add(Minecraft.getInstance().getToasts(), SystemToast.SystemToastIds.TUTORIAL_HINT, Component.literal("§cDeprecation Warning"), Component.literal("Version 1.21.11 will be deprecated on May 30."));
            warned = true;
        }
    }
}
