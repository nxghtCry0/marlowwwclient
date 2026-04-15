package com.eclipseware.imnotcheatingyouare.mixin.client;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.impl.HandView;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public class HandViewMixin {

    @Inject(method = "renderArmWithItem", at = @At("HEAD"))
    private void onRenderArmWithItem(AbstractClientPlayer player, float partialTicks, float pitch, InteractionHand hand, float swingProgress, ItemStack stack, float equipProgress, PoseStack poseStack, SubmitNodeCollector buffer, int combinedLight, CallbackInfo ci) {
        if (HandView.INSTANCE != null && HandView.INSTANCE.isToggled()) {
            
            String handPrefix = (hand == InteractionHand.MAIN_HAND) ? "Main " : "Off ";
            
            Setting scaleX = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(HandView.INSTANCE, handPrefix + "Scale X");
            Setting scaleY = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(HandView.INSTANCE, handPrefix + "Scale Y");
            Setting scaleZ = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(HandView.INSTANCE, handPrefix + "Scale Z");

            Setting posX = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(HandView.INSTANCE, handPrefix + "Pos X");
            Setting posY = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(HandView.INSTANCE, handPrefix + "Pos Y");
            Setting posZ = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(HandView.INSTANCE, handPrefix + "Pos Z");

            float sX = scaleX != null ? (float) scaleX.getValDouble() : 1.0f;
            float sY = scaleY != null ? (float) scaleY.getValDouble() : 1.0f;
            float sZ = scaleZ != null ? (float) scaleZ.getValDouble() : 1.0f;
            poseStack.scale(sX, sY, sZ);

            float pX = posX != null ? (float) posX.getValDouble() : 0.0f;
            float pY = posY != null ? (float) posY.getValDouble() : 0.0f;
            float pZ = posZ != null ? (float) posZ.getValDouble() : 0.0f;
            poseStack.translate(pX, pY, pZ);
        }
    }
}