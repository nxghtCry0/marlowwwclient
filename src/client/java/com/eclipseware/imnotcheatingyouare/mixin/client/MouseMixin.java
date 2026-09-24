package com.eclipseware.imnotcheatingyouare.mixin.client;

import com.eclipseware.imnotcheatingyouare.client.utils.MouseAimHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseMixin {

    @Shadow private double accumulatedDX;
    @Shadow private double accumulatedDY;

    @Inject(method = "turnPlayer", at = @At("HEAD"))
    private void velaris$applyAimDelta(double timeDelta, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gui.screen() != null) return;
        
        double dx = MouseAimHelper.pollDX();
        double dy = MouseAimHelper.pollDY();
        
        if (dx == 0.0 && dy == 0.0) return;
        
        this.accumulatedDX += dx;
        this.accumulatedDY += dy;
    }

    @Inject(method = "onButton", at = @At("HEAD"))
    private void onMousePress(long windowHandle, net.minecraft.client.input.MouseButtonInfo buttonInfo, int action, CallbackInfo ci) {
        if (Minecraft.getInstance().gui.screen() != null) return;
        if (com.eclipseware.imnotcheatingyouare.client.macro.MacroManager.isRecording()) {
            if (action == 1) {
                com.eclipseware.imnotcheatingyouare.client.macro.MacroManager.recordMouse(buttonInfo.button(), true, windowHandle);
            } else if (action == 0) {
                com.eclipseware.imnotcheatingyouare.client.macro.MacroManager.recordMouse(buttonInfo.button(), false, windowHandle);
            }
        }
    }

    @Redirect(method = "turnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
    private void onTurnPlayer(net.minecraft.client.player.LocalPlayer player, double yaw, double pitch) {
        if (com.eclipseware.imnotcheatingyouare.client.module.impl.Freecam.isActive()) {
            com.eclipseware.imnotcheatingyouare.client.module.impl.Freecam.INSTANCE.turn(yaw, pitch);
        } else {
            player.turn(yaw, pitch);
        }
    }
}
