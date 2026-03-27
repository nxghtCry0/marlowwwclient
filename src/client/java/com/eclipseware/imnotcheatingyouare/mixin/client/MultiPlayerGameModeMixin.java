package com.eclipseware.imnotcheatingyouare.mixin.client;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.module.impl.HitSelect;
import com.eclipseware.imnotcheatingyouare.client.module.impl.AutoShieldBreaker;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {

@Inject(method = "attack", at = @At("HEAD"), cancellable = true)
private void onAttack(Player player, Entity target, CallbackInfo ci) {
    // 1. HitSelect Check (Ensures timing is right first)
    Module hitSelectMod = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("HitSelect");
    if (hitSelectMod != null && hitSelectMod.isToggled() && hitSelectMod instanceof HitSelect hs) {
        if (!hs.canAttack(target)) {
            ci.cancel(); 
            return; // Stop here if HitSelect says we can't attack yet
        }
    }

    // 2. AutoShieldBreaker Check (Swap to axe right before hit if they are blocking)
Module shieldBreakerMod = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("AutoShieldBreaker");
if (shieldBreakerMod != null && shieldBreakerMod.isToggled() && shieldBreakerMod instanceof AutoShieldBreaker asb) {
if (asb.handleAttack(target, player)) {
ci.cancel(); // Cancel if Silent mode already sent the packet attack
return;
}
}

    // 3. BreachSwap Check (Swap to Mace with Breach on hit)
    Module breachSwapMod = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("BreachSwap");
    if (breachSwapMod != null && breachSwapMod instanceof com.eclipseware.imnotcheatingyouare.client.module.impl.BreachSwap bs) {
        if (bs.handleAttack(target, player)) {
            ci.cancel();
            return;
        }
    }
}

}