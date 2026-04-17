package com.eclipseware.imnotcheatingyouare.mixin.client;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.impl.Hitboxes;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityMixin {
    @Inject(method = "getPickRadius", at = @At("RETURN"), cancellable = true)
    private void adjustPickRadius(CallbackInfoReturnable<Float> cir) {
        Hitboxes hitboxes = (Hitboxes) ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("Hitboxes");
        if (hitboxes != null && hitboxes.isToggled()) {
            Setting size = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(hitboxes, "Expand Size");
            if (size != null) {
                cir.setReturnValue(cir.getReturnValue() + (float) size.getValDouble());
            }
        }
    }
}
