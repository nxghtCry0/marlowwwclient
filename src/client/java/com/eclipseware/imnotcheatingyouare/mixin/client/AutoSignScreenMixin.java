package com.eclipseware.imnotcheatingyouare.mixin.client;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractSignEditScreen.class)
public class AutoSignScreenMixin {
    @Inject(method = "init", at = @At("HEAD"))
    private void onInit(CallbackInfo ci) {
        if (ImnotcheatingyouareClient.INSTANCE != null && ImnotcheatingyouareClient.INSTANCE.moduleManager != null) {
            Module autoSign = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("AutoSign");
            if (autoSign != null && autoSign.isToggled()) {
                com.eclipseware.imnotcheatingyouare.client.module.impl.AutoSign.handleScreen((AbstractSignEditScreen)(Object)this);
            }
        }
    }
}