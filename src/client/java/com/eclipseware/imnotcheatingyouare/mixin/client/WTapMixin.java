package com.eclipseware.imnotcheatingyouare.mixin.client;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.impl.WTap;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiPlayerGameMode.class)
public class WTapMixin {

    // RETURN only fires if no other mixin cancelled the attack at HEAD,
    // so this exclusively catches attacks that actually reached the server.
    @Inject(method = "attack", at = @At("RETURN"))
    private void onAttackReturn(Player player, Entity target, CallbackInfo ci) {
        if (ImnotcheatingyouareClient.INSTANCE == null || ImnotcheatingyouareClient.INSTANCE.moduleManager == null) return;
        WTap wTap = (WTap) ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("WTap");
        if (wTap != null && wTap.isToggled()) {
            wTap.onAttackLanded();
        }
    }
}