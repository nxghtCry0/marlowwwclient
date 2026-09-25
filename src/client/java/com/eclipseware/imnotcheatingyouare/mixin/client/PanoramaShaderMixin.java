package com.eclipseware.imnotcheatingyouare.mixin.client;

import com.eclipseware.imnotcheatingyouare.client.gui.PastelShaderBackground;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public class PanoramaShaderMixin {

    @Shadow public int width;
    @Shadow public int height;

    @Inject(method = "extractPanorama", at = @At("HEAD"), cancellable = true)
    private void replacePanorama(GuiGraphicsExtractor graphics, float partialTick, CallbackInfo ci) {
        if (PastelShaderBackground.render()) {
            graphics.blit(PastelShaderBackground.TEXTURE_ID, 0, 0, width, height, 0.0f, 1.0f, 1.0f, 0.0f);
            ci.cancel();
        }
    }
}
