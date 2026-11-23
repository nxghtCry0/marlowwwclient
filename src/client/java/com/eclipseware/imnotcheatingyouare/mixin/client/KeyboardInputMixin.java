package com.eclipseware.imnotcheatingyouare.mixin.client;

import com.eclipseware.imnotcheatingyouare.client.module.impl.AutoTotem;
import com.eclipseware.imnotcheatingyouare.client.module.impl.Freecam;
import com.eclipseware.imnotcheatingyouare.client.module.impl.SprintResetController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public class KeyboardInputMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTickHead(CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && AutoTotem.shouldPauseInputs()) {
            if (mc.options != null) {
                var opts = mc.options;
                opts.keyUp.setDown(false);
                opts.keyDown.setDown(false);
                opts.keyLeft.setDown(false);
                opts.keyRight.setDown(false);
                opts.keyJump.setDown(false);
                opts.keyShift.setDown(false);
                opts.keySprint.setDown(false);
            }
            return;
        }
        SprintResetController.applyPreInputOverrides(mc);
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void onTickReturn(CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (Freecam.isActive()) {
            KeyboardInput self = (KeyboardInput) (Object) this;
            self.keyPresses = new Input(false, false, false, false, false, false, false);
            ClientInputAccessor accessor = (ClientInputAccessor) this;
            accessor.setMoveVector(Vec2.ZERO);
            return;
        }
        if (mc.player != null && AutoTotem.shouldPauseInputs()) {
            KeyboardInput self = (KeyboardInput) (Object) this;
            self.keyPresses = new Input(false, false, false, false, false, false, false);
            mc.player.setSprinting(false);
        }
    }
}
