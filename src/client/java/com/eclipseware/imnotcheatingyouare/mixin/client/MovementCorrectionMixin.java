package com.eclipseware.imnotcheatingyouare.mixin.client;

import com.eclipseware.imnotcheatingyouare.client.utils.RotationManager;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public class MovementCorrectionMixin {
    @Unique
    private float eclipseSavedYRot;
    @Unique
    private boolean eclipseWasCorrecting = false;

    @Inject(method = "aiStep", at = @At("HEAD"))
    private void onAiStepHead(CallbackInfo ci) {
        eclipseWasCorrecting = false;
        if (!RotationManager.isMovementCorrection()) return;
        if (!RotationManager.isActive()) return;

        LocalPlayer player = (LocalPlayer) (Object) this;
        eclipseSavedYRot = player.getYRot();
        eclipseWasCorrecting = true;

        player.setYRot(RotationManager.getServerYaw());
    }

    @Inject(method = "aiStep", at = @At("RETURN"))
    private void onAiStepReturn(CallbackInfo ci) {
        if (!eclipseWasCorrecting) return;
        LocalPlayer player = (LocalPlayer) (Object) this;
        player.setYRot(eclipseSavedYRot);
        eclipseWasCorrecting = false;
    }

    @Unique
    private float eclipseSavedSendYRot;
    @Unique
    private float eclipseSavedSendXRot;
    @Unique
    private boolean eclipseIsSendCorrecting = false;

    @Inject(method = "sendPosition", at = @At("HEAD"))
    private void onSendPositionHead(CallbackInfo ci) {
        eclipseIsSendCorrecting = false;
        if (!RotationManager.isActive()) return;

        LocalPlayer player = (LocalPlayer) (Object) this;
        eclipseSavedSendYRot = player.getYRot();
        eclipseSavedSendXRot = player.getXRot();
        eclipseIsSendCorrecting = true;

        player.setYRot(RotationManager.getServerYaw());
        player.setXRot(RotationManager.getServerPitch());
    }

    @Inject(method = "sendPosition", at = @At("TAIL"))
    private void onSendPositionTail(CallbackInfo ci) {
        LocalPlayer player = (LocalPlayer) (Object) this;
        if (eclipseIsSendCorrecting) {
            player.setYRot(eclipseSavedSendYRot);
            player.setXRot(eclipseSavedSendXRot);
            eclipseIsSendCorrecting = false;
        }

        com.eclipseware.imnotcheatingyouare.client.module.Module killAura = com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("KillAura");
        if (killAura != null && killAura.isToggled() && killAura instanceof com.eclipseware.imnotcheatingyouare.client.module.impl.KillAura ka) {
            ka.postSendPosition();
        }
    }
}