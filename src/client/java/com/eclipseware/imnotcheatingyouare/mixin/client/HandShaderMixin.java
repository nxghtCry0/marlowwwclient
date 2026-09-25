package com.eclipseware.imnotcheatingyouare.mixin.client;

import com.eclipseware.imnotcheatingyouare.client.render.HandShaderRenderer;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.renderpearl.api.commands.CommandEncoder;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import org.joml.Vector4f;
import org.joml.Vector4fc;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Supplier;

@Mixin(GameRenderer.class)
public class HandShaderMixin {

    @Shadow @Final private RenderTarget mainRenderTarget;

    @Redirect(method = "renderItemInHand", at = @At(value = "INVOKE", target = "Lcom/mojang/renderpearl/api/commands/CommandEncoder;createRenderPass(Ljava/util/function/Supplier;Lcom/mojang/renderpearl/api/textures/GpuTextureView;Ljava/util/Optional;Lcom/mojang/renderpearl/api/textures/GpuTextureView;Ljava/util/OptionalDouble;)Lcom/mojang/renderpearl/api/commands/RenderPass;"))
    private RenderPass redirectHandPass(CommandEncoder encoder, Supplier<String> label, GpuTextureView color, Optional<Vector4fc> clearColor, GpuTextureView depth, OptionalDouble clearDepth) {
        if (HandShaderRenderer.shouldCapture()) {
            try {
                RenderTarget capture = HandShaderRenderer.captureTarget(mainRenderTarget);
                return encoder.createRenderPass(label, capture.getColorTextureView(), Optional.of(new Vector4f(0f, 0f, 0f, 0f)), depth, clearDepth);
            } catch (Throwable ignored) {
            }
        }
        return encoder.createRenderPass(label, color, clearColor, depth, clearDepth);
    }

    @Inject(method = "renderItemInHand", at = @At("TAIL"))
    private void compositeHands(CameraRenderState camera, PlayerRenderState player, GpuTextureView depth, CallbackInfo ci) {
        HandShaderRenderer.composite(mainRenderTarget);
    }
}
