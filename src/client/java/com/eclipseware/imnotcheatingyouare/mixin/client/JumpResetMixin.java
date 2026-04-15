package com.eclipseware.imnotcheatingyouare.mixin.client;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.impl.JumpReset;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientboundSetEntityMotionPacket.class)
public class JumpResetMixin {
    @Inject(method = "<init>(ILnet/minecraft/world/phys/Vec3;)V", at = @At("TAIL"))
    private void onConstruct(int entityId, Vec3 velocity, CallbackInfo ci) {
        if (ImnotcheatingyouareClient.INSTANCE == null || ImnotcheatingyouareClient.INSTANCE.moduleManager == null) return;
        JumpReset jumpReset = (JumpReset) ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("JumpReset");
        if (jumpReset == null || !jumpReset.isToggled()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (entityId == mc.player.getId()) {
            double velocityMagnitude = Math.sqrt(velocity.x * velocity.x + velocity.y * velocity.y + velocity.z * velocity.z);
            Setting thresholdSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(jumpReset, "Velocity Threshold");
            double threshold = thresholdSetting != null ? thresholdSetting.getValDouble() : 0.1;
            if (velocityMagnitude > threshold) {
                jumpReset.onKnockback();
            }
        }
    }
}