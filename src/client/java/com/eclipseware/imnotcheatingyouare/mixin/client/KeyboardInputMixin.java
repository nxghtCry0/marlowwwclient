package com.eclipseware.imnotcheatingyouare.mixin.client;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import net.minecraft.client.player.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.Minecraft;
import org.joml.Vector2f;

@Mixin(KeyboardInput.class)
public class KeyboardInputMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTickHead(CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && com.eclipseware.imnotcheatingyouare.client.module.impl.AutoTotem.shouldPauseInputs()) {
            if (mc.options != null) {
                mc.options.keyUp.setDown(false);
                mc.options.keyDown.setDown(false);
                mc.options.keyLeft.setDown(false);
                mc.options.keyRight.setDown(false);
                mc.options.keyJump.setDown(false);
                mc.options.keyShift.setDown(false);
                mc.options.keySprint.setDown(false);
            }
        }
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void onTickReturn(CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && com.eclipseware.imnotcheatingyouare.client.module.impl.AutoTotem.shouldPauseInputs()) {
            ((KeyboardInput) (Object) this).keyPresses = new net.minecraft.world.entity.player.Input(false, false, false, false, false, false, false);
            mc.player.setSprinting(false);
        }
    }
}
