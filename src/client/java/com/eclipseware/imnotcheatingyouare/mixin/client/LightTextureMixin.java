package com.eclipseware.imnotcheatingyouare.mixin.client;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LightTexture.class)
public class LightTextureMixin {

    // Injects into the dimension's brightness calculation
    @Inject(method = "getBrightness", at = @At("HEAD"), cancellable = true)
    private static void onGetBrightness(net.minecraft.world.level.dimension.DimensionType dimensionType, int lightLevel, CallbackInfoReturnable<Float> cir) {
        Module fbMod = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("Fullbright");
        
        if (fbMod != null && fbMod.isToggled()) {
            Setting mode = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(fbMod, "Mode");
            if (mode != null && mode.getValString().equals("Gamma")) {
                cir.setReturnValue(15.0F); // Forces maximum ambient light
            }
        }
    }
}