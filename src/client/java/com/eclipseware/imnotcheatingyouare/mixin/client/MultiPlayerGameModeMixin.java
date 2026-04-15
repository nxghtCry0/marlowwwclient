package com.eclipseware.imnotcheatingyouare.mixin.client;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.module.impl.HitSelect;
import com.eclipseware.imnotcheatingyouare.client.module.impl.AutoShieldBreaker;
import com.eclipseware.imnotcheatingyouare.client.module.impl.KnockbackDisplacement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {

    @Unique
    private boolean kbShouldRevert = false;

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void onAttack(Player player, Entity target, CallbackInfo ci) {
        // 1. HitSelect
        Module hitSelectMod = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("HitSelect");
        if (hitSelectMod != null && hitSelectMod.isToggled() && hitSelectMod instanceof HitSelect hs) {
            if (!hs.canAttack(target)) {
                ci.cancel();
                return;
            }
        }

        // 2. AutoShieldBreaker
        Module shieldBreakerMod = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("AutoShieldBreaker");
        if (shieldBreakerMod != null && shieldBreakerMod.isToggled() && shieldBreakerMod instanceof AutoShieldBreaker asb) {
            if (asb.handleAttack(target, player)) {
                ci.cancel();
                return;
            }
        }

        // 3. BreachSwap
        Module breachSwapMod = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("BreachSwap");
        if (breachSwapMod != null && breachSwapMod instanceof com.eclipseware.imnotcheatingyouare.client.module.impl.BreachSwap bs) {
            if (bs.handleAttack(target, player)) {
                ci.cancel();
                return;
            }
        }

        // 4. KBDisplacement
if (!ci.isCancelled()) {
Module kbMod = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("KBDisplacement");
if (kbMod != null && kbMod.isToggled() && kbMod instanceof KnockbackDisplacement kbd) {
float[] flip = kbd.getFlipRotation(target);
if (flip != null && Minecraft.getInstance().getConnection() != null) {
Minecraft.getInstance().getConnection().send(new ServerboundMovePlayerPacket.Rot(
flip[0], flip[1], player.onGround(), false
));
kbShouldRevert = true;
}
}
}
// 5. SilentAim Integration (Guarantees hit registration by syncing rotation right before attack)
if (!ci.isCancelled()) {
Module silentAim = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("SilentAim");
if (silentAim != null && silentAim.isToggled() && com.eclipseware.imnotcheatingyouare.client.utils.SilentAimUtil.isActive()) {
if (Minecraft.getInstance().getConnection() != null) {
Minecraft.getInstance().getConnection().send(new ServerboundMovePlayerPacket.Rot(
com.eclipseware.imnotcheatingyouare.client.utils.SilentAimUtil.getYaw(),
com.eclipseware.imnotcheatingyouare.client.utils.SilentAimUtil.getPitch(),
player.onGround(), false
));
}
com.eclipseware.imnotcheatingyouare.client.utils.SilentAimUtil.consume();
}
}
}

    @Inject(method = "attack", at = @At("RETURN"))
    private void afterAttack(Player player, Entity target, CallbackInfo ci) {
        if (kbShouldRevert) {
            var mc = Minecraft.getInstance();
            if (mc.getConnection() != null) {
                mc.getConnection().send(new ServerboundMovePlayerPacket.Rot(
                    mc.player.getYRot(), mc.player.getXRot(), mc.player.onGround(), false
                ));
            }
            kbShouldRevert = false;
        }
    }
}