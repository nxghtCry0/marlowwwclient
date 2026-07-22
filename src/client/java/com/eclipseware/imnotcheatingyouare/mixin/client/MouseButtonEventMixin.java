package com.eclipseware.imnotcheatingyouare.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import com.eclipseware.imnotcheatingyouare.client.clickgui.Clickgui;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MouseButtonEvent.class)
public class MouseButtonEventMixin {

    @Inject(method = "x", at = @At("RETURN"), cancellable = true)
    private void onGetX(CallbackInfoReturnable<Double> cir) {
        Screen screen = Minecraft.getInstance().gui.screen();
        if (screen instanceof Clickgui clickgui) {
            cir.setReturnValue(cir.getReturnValue() / clickgui.getScaleFactor());
        }
    }

    @Inject(method = "y", at = @At("RETURN"), cancellable = true)
    private void onGetY(CallbackInfoReturnable<Double> cir) {
        Screen screen = Minecraft.getInstance().gui.screen();
        if (screen instanceof Clickgui clickgui) {
            cir.setReturnValue(cir.getReturnValue() / clickgui.getScaleFactor());
        }
    }
}
