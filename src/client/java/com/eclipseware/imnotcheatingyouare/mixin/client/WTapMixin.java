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

    @Inject(method = "attack", at = @At("HEAD"))
    private void onAttackHead(Player player, Entity target, CallbackInfo ci) {
        com.eclipseware.imnotcheatingyouare.client.module.impl.STap.captureSprint(player.isSprinting());
    }

    @Inject(method = "attack", at = @At("RETURN"))
    private void onAttackReturn(Player player, Entity target, CallbackInfo ci) {
        if (ImnotcheatingyouareClient.INSTANCE == null || ImnotcheatingyouareClient.INSTANCE.moduleManager == null) return;
        WTap wTap = (WTap) ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("WTap");
        if (wTap != null && wTap.isToggled()) {
            wTap.onAttackLanded(target);
        }
        com.eclipseware.imnotcheatingyouare.client.module.impl.STap sTap = (com.eclipseware.imnotcheatingyouare.client.module.impl.STap) ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("STap");
        if (sTap != null && sTap.isToggled()) {
            sTap.onAttackLanded(target);
        }
    }
}