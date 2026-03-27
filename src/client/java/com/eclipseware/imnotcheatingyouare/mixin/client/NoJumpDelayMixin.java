package com.eclipseware.imnotcheatingyouare.mixin.client;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class NoJumpDelayMixin {
@Shadow private int noJumpDelay;

@Inject(method = "tick", at = @At("HEAD"))
private void removeJumpDelay(CallbackInfo ci) {
    Module jumpMod = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("NoJumpDelay");
    if (jumpMod != null && jumpMod.isToggled()) {
        this.noJumpDelay = 0;
    }
}

}