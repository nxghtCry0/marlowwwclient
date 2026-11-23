package com.eclipseware.imnotcheatingyouare.mixin.client;

import com.eclipseware.imnotcheatingyouare.client.glassui.screen.GlassUiScreen;
import com.eclipseware.imnotcheatingyouare.client.macro.MacroManager;
import com.eclipseware.imnotcheatingyouare.client.module.impl.Freecam;
import com.eclipseware.imnotcheatingyouare.client.utils.MouseAimHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseMixin {

    @Shadow
    private double accumulatedDX;

    @Shadow
    private double accumulatedDY;

    @Inject(method = "turnPlayer", at = @At("HEAD"))
    private void velaris$applyAimDelta(double timeDelta, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gui.screen() != null) {
            return;
        }

        double dx = MouseAimHelper.pollDX();
        double dy = MouseAimHelper.pollDY();

        if (dx == 0.0 && dy == 0.0) {
            return;
        }

        this.accumulatedDX += dx;
        this.accumulatedDY += dy;
    }

    @Inject(method = "onButton", at = @At("HEAD"), cancellable = true)
    private void onMousePress(long windowHandle, MouseButtonInfo buttonInfo, int action, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        Screen current = mc.gui.screen();
        if (action == 1 && current instanceof GlassUiScreen gs) {
            if (gs.onRawMouseButtonForBind(buttonInfo.button())) {
                ci.cancel();
                return;
            }
        }
        if (current != null) {
            return;
        }
        if (MacroManager.isRecording()) {
            if (action == 1) {
                MacroManager.recordMouse(buttonInfo.button(), true, windowHandle);
            } else if (action == 0) {
                MacroManager.recordMouse(buttonInfo.button(), false, windowHandle);
            }
        }
    }

    @Redirect(method = "turnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
    private void onTurnPlayer(LocalPlayer player, double yaw, double pitch) {
        if (Freecam.isActive()) {
            Freecam freecam = Freecam.INSTANCE;
            freecam.turn(yaw, pitch);
        } else {
            player.turn(yaw, pitch);
        }
    }
}
