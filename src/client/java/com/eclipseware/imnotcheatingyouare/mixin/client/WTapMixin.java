package com.eclipseware.imnotcheatingyouare.mixin.client;

import com.eclipseware.imnotcheatingyouare.client.module.impl.STap;
import com.eclipseware.imnotcheatingyouare.client.module.impl.SprintResetController;
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
        STap.captureSprint(player.isSprinting());
        SprintResetController.onPreAttack(player, target);
    }

    @Inject(method = "attack", at = @At("TAIL"))
    private void onAttackTail(Player player, Entity target, CallbackInfo ci) {
        SprintResetController.onPostAttack(player, target);
    }
}
