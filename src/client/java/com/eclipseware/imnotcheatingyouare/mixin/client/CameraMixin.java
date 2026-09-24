package com.eclipseware.imnotcheatingyouare.mixin.client;

import com.eclipseware.imnotcheatingyouare.client.module.impl.Freecam;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {

    @Shadow private boolean detached;

    @Shadow protected abstract void setRotation(float yRot, float xRot);

    @Shadow protected abstract void setPosition(net.minecraft.world.phys.Vec3 pos);

    @Inject(method = "alignWithEntity", at = @At("TAIL"))
    private void onAlignWithEntity(float partialTicks, CallbackInfo ci) {
        if (!Freecam.isActive()) return;
        Freecam freecam = Freecam.INSTANCE;
        this.detached = true;
        setRotation(freecam.getCamYaw(), freecam.getCamPitch());
        setPosition(freecam.getCamPos(partialTicks));
    }
}
