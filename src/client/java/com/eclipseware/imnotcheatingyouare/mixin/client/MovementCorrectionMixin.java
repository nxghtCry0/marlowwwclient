package com.eclipseware.imnotcheatingyouare.mixin.client;

import com.eclipseware.imnotcheatingyouare.client.utils.RotationManager;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class MovementCorrectionMixin {
    @Unique
    private float eclipseSavedZza, eclipseSavedXxa;
    @Unique
    private boolean eclipseWasCorrecting = false;

    @Inject(method = "travel", at = @At("HEAD"))
    private void onTravelHead(CallbackInfo ci) {
        eclipseWasCorrecting = false;
        if (!((Object) this instanceof net.minecraft.client.player.LocalPlayer)) return;
        if (!RotationManager.isMovementCorrection()) return;

        LivingEntity entity = (LivingEntity) (Object) this;
        float delta = RotationManager.getYawDelta();
        if (Math.abs(delta) < 0.5f) return;

        eclipseSavedZza = entity.zza;
        eclipseSavedXxa = entity.xxa;
        eclipseWasCorrecting = true;

        float rad = (float) Math.toRadians(delta);
        float cos = (float) Math.cos(rad);
        float sin = (float) Math.sin(rad);

        entity.zza = eclipseSavedZza * cos + eclipseSavedXxa * sin;
        entity.xxa = eclipseSavedXxa * cos - eclipseSavedZza * sin;
    }

    @Inject(method = "travel", at = @At("RETURN"))
    private void onTravelReturn(CallbackInfo ci) {
        if (!eclipseWasCorrecting) return;
        LivingEntity entity = (LivingEntity) (Object) this;
        entity.zza = eclipseSavedZza;
        entity.xxa = eclipseSavedXxa;
        eclipseWasCorrecting = false;
    }
}