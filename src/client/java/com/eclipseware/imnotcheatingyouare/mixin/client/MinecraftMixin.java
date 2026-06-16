package com.eclipseware.imnotcheatingyouare.mixin.client;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public class MinecraftMixin {

    @Shadow public HitResult hitResult;

    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void onStartAttack(CallbackInfoReturnable<Boolean> cir) {
        Minecraft mc = (Minecraft) (Object) this;
        if (mc.player == null) return;

        Module crystalHelper = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("CrystalHelper");
        if (crystalHelper != null && crystalHelper.isToggled()) {
            if (this.hitResult != null && this.hitResult.getType() == HitResult.Type.BLOCK) {
                cir.setReturnValue(false);
                cir.cancel();
                return;
            }
        }

        Module autoMace = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("AutoMace");
        if (autoMace != null && autoMace.isToggled()) {
            Setting swingPrevSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(autoMace, "Swing Prevention");
            if (swingPrevSetting != null && swingPrevSetting.getValBoolean()) {
                boolean holdingMace = mc.player.getItemInHand(InteractionHand.MAIN_HAND).is(Items.MACE);
                if (holdingMace) {
                    boolean falling = mc.player.getDeltaMovement().y < -0.1 && !mc.player.onGround();
                    if (falling) {
                        if (this.hitResult == null || this.hitResult.getType() != HitResult.Type.ENTITY) {
                            cir.setReturnValue(false);
                            cir.cancel();
                        } else {
                            Entity entity = ((EntityHitResult) this.hitResult).getEntity();
                            double dist = mc.player.position().distanceTo(entity.position());
                            if (dist > 4.5) {
                                cir.setReturnValue(false);
                                cir.cancel();
                            }
                        }
                    }
                }
            }
        }
    }

    @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
    private void onContinueAttack(boolean leftClick, CallbackInfo ci) {
        if (leftClick) {
            Module crystalHelper = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("CrystalHelper");
            if (crystalHelper != null && crystalHelper.isToggled()) {
                if (this.hitResult != null && this.hitResult.getType() == HitResult.Type.BLOCK) {
                    ci.cancel();
                }
            }
        }
    }

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void onSetScreen(net.minecraft.client.gui.screens.Screen screen, CallbackInfo ci) {
        if (screen != null && !(screen instanceof com.eclipseware.imnotcheatingyouare.client.clickgui.PSAScreen)) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.font != null) {
                java.io.File psaFile = new java.io.File(mc.gameDirectory, "config/imnotcheatingyouare/psa_accepted");
                if (!psaFile.exists()) {
                    ci.cancel();
                    mc.setScreen(new com.eclipseware.imnotcheatingyouare.client.clickgui.PSAScreen(screen));
                }
            }
        }
    }
}
