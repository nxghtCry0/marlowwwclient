package com.eclipseware.imnotcheatingyouare.mixin.client;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.impl.FastPlace;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class FastPlaceMixin {

    @Shadow private int rightClickDelay;

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        if (FastPlace.INSTANCE != null && FastPlace.INSTANCE.isToggled()) {
            Setting delaySetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(FastPlace.INSTANCE, "Delay (Ticks)");
            int targetDelay = delaySetting != null ? (int) delaySetting.getValDouble() : 0;
            
            // Only lower the delay, don't increase it if the game sets it to 0 naturally
            if (this.rightClickDelay > targetDelay) {
                this.rightClickDelay = targetDelay;
            }
        }
    }
}