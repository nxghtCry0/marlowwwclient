package com.eclipseware.imnotcheatingyouare.mixin.client;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public class AutoSprintMixin {
@Inject(method = "tick", at = @At("HEAD"))
private void onTick(CallbackInfo ci) {
Module sprintMod = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("AutoSprint");
if (sprintMod == null || !sprintMod.isToggled()) return;

    LocalPlayer player = (LocalPlayer) (Object) this;
    
    // zza > 0 checks for forward movement in Mojang Mappings
    if (player.zza > 0 && !player.isShiftKeyDown() && !player.isUsingItem()) {
        player.setSprinting(true);
    }
}

}