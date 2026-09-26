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

    @Inject(
            method = "renderFrame",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/systems/GpuSurface;blitFromTexture(Lcom/mojang/blaze3d/systems/CommandEncoder;Lcom/mojang/blaze3d/textures/GpuTextureView;)V"
            )
    )
    private void onRenderFrame(boolean renderLevel, CallbackInfo ci) {
        Minecraft mc = (Minecraft) (Object) this;
        if (mc.gameRenderer == null) return;
        xyz.breadloaf.imguimc.imgui.ImguiLoader.onFrameRender(mc.gameRenderer.mainRenderTarget().getColorTexture());
    }

    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void onStartAttack(CallbackInfoReturnable<Boolean> cir) {
        Minecraft mc = (Minecraft) (Object) this;
        if (mc.player == null) return;

        if (com.eclipseware.imnotcheatingyouare.client.module.impl.Triggerbot.shouldCancelManualAttack()) {
            cir.setReturnValue(false);
            cir.cancel();
            return;
        }

        Module lungeSwap = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("LungeSwap");
        if (lungeSwap != null && lungeSwap.isToggled() && lungeSwap instanceof com.eclipseware.imnotcheatingyouare.client.module.impl.LungeAssist la) {
            if (la.onPlayerAttack()) {
                cir.setReturnValue(false);
                cir.cancel();
                return;
            }
        }

        Module crystalHelper = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("CrystalHelper");
        if (crystalHelper != null && crystalHelper.isToggled()) {
            if (this.hitResult != null && this.hitResult.getType() == HitResult.Type.BLOCK) {
                cir.setReturnValue(false);
                cir.cancel();
                return;
            }
        }

        Module silentAim = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("SilentAim");
        if (silentAim != null && silentAim.isToggled() && silentAim instanceof com.eclipseware.imnotcheatingyouare.client.module.impl.SilentAim sa) {
            Entity sat = sa.getTarget();
            if (sat != null) {
                if (this.hitResult == null || this.hitResult.getType() != HitResult.Type.ENTITY || ((EntityHitResult) this.hitResult).getEntity() != sat) {
                    this.hitResult = new EntityHitResult(sat);
                    mc.crosshairPickEntity = sat;
                }
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

    @Inject(method = "setScreenAndShow", at = @At("HEAD"), cancellable = true)
    private void onSetScreen(net.minecraft.client.gui.screens.Screen screen, CallbackInfo ci) {
        if (screen != null
                && !(screen instanceof com.eclipseware.imnotcheatingyouare.client.clickgui.PSAScreen)
                && !(screen instanceof xyz.breadloaf.imguimc.screen.EmptyScreen)) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.font != null) {
                if (!com.eclipseware.imnotcheatingyouare.client.utils.PsaState.isAccepted(mc)) {
                    ci.cancel();
                    mc.setScreenAndShow(new com.eclipseware.imnotcheatingyouare.client.clickgui.PSAScreen(screen));
                }
            }
        }
    }
}
