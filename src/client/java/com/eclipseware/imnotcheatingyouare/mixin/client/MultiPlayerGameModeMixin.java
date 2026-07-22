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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {

    @org.spongepowered.asm.mixin.Shadow
    private float destroyProgress;

    @Inject(method = "continueDestroyBlock", at = @At("RETURN"))
    private void onContinueDestroyBlock(BlockPos pos, Direction directionFace, CallbackInfoReturnable<Boolean> cir) {
        Module fastBreak = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("FastBreak");
        if (fastBreak != null && fastBreak.isToggled()) {
            com.eclipseware.imnotcheatingyouare.client.setting.Setting speedSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(fastBreak, "Speed Multiplier");
            if (speedSetting != null) {
                float mult = (float) speedSetting.getValDouble();
                this.destroyProgress *= mult;
            }
        }
    }

    @Inject(method = "destroyBlock", at = @At("HEAD"))
    private void onDestroyBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        Module attributeSwapMod = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("AttributeSwap");
        if (attributeSwapMod != null && attributeSwapMod.isToggled() && attributeSwapMod instanceof com.eclipseware.imnotcheatingyouare.client.module.impl.AttributeSwap as) {
            as.handleBlockBreak();
        }
    }

    @Unique
    private boolean kbShouldRevert = false;

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void onAttack(Player player, Entity target, CallbackInfo ci) {
        if (com.eclipseware.imnotcheatingyouare.client.module.impl.HitSwap.INSTANCE != null) {
            com.eclipseware.imnotcheatingyouare.client.module.impl.HitSwap.INSTANCE.onPreAttack(target);
        }

        Module triggerBotMod = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("Triggerbot");
        if (triggerBotMod != null && triggerBotMod.isToggled() && triggerBotMod instanceof com.eclipseware.imnotcheatingyouare.client.module.impl.Triggerbot tb) {
            if (tb.shouldBlock(target)) {
                ci.cancel();
                return;
            }
        }

        Module shieldBreakerMod = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("AutoShieldBreaker");
        if (shieldBreakerMod != null && shieldBreakerMod.isToggled() && shieldBreakerMod instanceof AutoShieldBreaker asb) {
            long prevBreakTime = asb.lastBreakTime;
            if (asb.shouldCancelAttack(target)) {
                ci.cancel();
                return;
            }
            if (asb.lastBreakTime != prevBreakTime) {
                com.eclipseware.imnotcheatingyouare.client.module.impl.WebStun.trigger(target);
            }
        }

        Module hitSelectMod = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("HitSelect");
        if (hitSelectMod != null && hitSelectMod.isToggled() && hitSelectMod instanceof HitSelect hs) {
            if (!hs.canAttack(target)) {
                ci.cancel();
                return;
            }
        }

        Module breachSwapMod = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("BreachSwap");
        if (breachSwapMod != null && breachSwapMod instanceof com.eclipseware.imnotcheatingyouare.client.module.impl.BreachSwap bs) {
            if (bs.handleAttack(target, player)) {
                ci.cancel();
                return;
            }
        }

        Module attributeSwapMod = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("AttributeSwap");
        if (attributeSwapMod != null && attributeSwapMod.isToggled() && attributeSwapMod instanceof com.eclipseware.imnotcheatingyouare.client.module.impl.AttributeSwap as) {
            if (as.handleAttack(target, player)) {
                ci.cancel();
                return;
            }
        }

        if (!ci.isCancelled()) {
            Module kbMod = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("KBDisplacement");
            if (kbMod != null && kbMod.isToggled() && kbMod instanceof KnockbackDisplacement kbd) {
                float[] flip = kbd.getFlipRotation(target);
                if (flip != null) {
                    com.eclipseware.imnotcheatingyouare.client.utils.RotationManager.queueRotation(flip[0], flip[1], 0, 1, 1, false, null);
                }
            }
        }

        if (!ci.isCancelled() && !kbShouldRevert) {
            Module silentAim = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("SilentAim");
            if (silentAim != null && silentAim.isToggled() && com.eclipseware.imnotcheatingyouare.client.utils.SilentAimUtil.isActive()) {
                com.eclipseware.imnotcheatingyouare.client.utils.SilentAimUtil.consume();
            }
        }
    }

    @Inject(method = "attack", at = @At("RETURN"))
    private void afterAttack(Player player, Entity target, CallbackInfo ci) {
        if (com.eclipseware.imnotcheatingyouare.client.module.impl.HitSwap.INSTANCE != null) {
            com.eclipseware.imnotcheatingyouare.client.module.impl.HitSwap.INSTANCE.onPostAttack(target);
        }

        if (com.eclipseware.imnotcheatingyouare.client.module.impl.TargetHUD.INSTANCE != null) {
            com.eclipseware.imnotcheatingyouare.client.module.impl.TargetHUD.INSTANCE.onPostAttack(target);
        }



        if (com.eclipseware.imnotcheatingyouare.client.module.impl.Backtrack.INSTANCE != null) {
            com.eclipseware.imnotcheatingyouare.client.module.impl.Backtrack.INSTANCE.onAttack(target);
        }


    }
}