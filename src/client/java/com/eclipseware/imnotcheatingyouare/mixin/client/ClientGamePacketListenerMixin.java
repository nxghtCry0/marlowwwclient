package com.eclipseware.imnotcheatingyouare.mixin.client;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.module.impl.AutoTotem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ClientGamePacketListenerMixin {

    @Inject(method = "handleEntityEvent", at = @At("HEAD"))
    private void onHandleEntityEventHead(ClientboundEntityEventPacket packet, CallbackInfo ci) {
        if (packet.getEventId() == 30 && ImnotcheatingyouareClient.INSTANCE != null) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null) {
                Entity eventEntity = packet.getEntity(mc.level);
                if (eventEntity != null) {
                    com.eclipseware.imnotcheatingyouare.client.module.impl.WebStun.onShieldBreak(eventEntity);
                }
            }
        }
    }

    @Inject(method = "handleEntityEvent", at = @At("TAIL"))
    private void onHandleEntityEvent(ClientboundEntityEventPacket packet, CallbackInfo ci) {
        if (packet.getEventId() != 35) return;
        if (ImnotcheatingyouareClient.INSTANCE == null || ImnotcheatingyouareClient.INSTANCE.moduleManager == null) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        Entity eventEntity = packet.getEntity(mc.level);
        if (eventEntity != mc.player) return;

        Module autoTotemModule = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("AutoTotem");
        if (autoTotemModule instanceof AutoTotem autoTotem && autoTotemModule.isToggled()) {
            autoTotem.onLocalTotemPop();
        }
    }

    @Inject(method = "handleCommands", at = @At("HEAD"), cancellable = true)
    private void onHandleCommands(net.minecraft.network.protocol.game.ClientboundCommandsPacket packet, CallbackInfo ci) {
        if (ImnotcheatingyouareClient.INSTANCE != null && ImnotcheatingyouareClient.INSTANCE.moduleManager != null) {
            Module bypass = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("Bypass");
            if (bypass != null && bypass.isToggled()) {
                com.eclipseware.imnotcheatingyouare.client.setting.Setting disableAutofill = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(bypass, "Disable Command Autofill");
                if (disableAutofill != null && disableAutofill.getValBoolean()) {
                    ci.cancel();
                }
            }
        }
    }

    @Inject(method = "handleSetEntityMotion", at = @At("TAIL"))
    private void onHandleSetEntityMotion(net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket packet, CallbackInfo ci) {
        if (ImnotcheatingyouareClient.INSTANCE == null || ImnotcheatingyouareClient.INSTANCE.moduleManager == null) return;
        com.eclipseware.imnotcheatingyouare.client.module.impl.JumpReset jumpReset = (com.eclipseware.imnotcheatingyouare.client.module.impl.JumpReset) ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("JumpReset");
        if (jumpReset == null || !jumpReset.isToggled()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (packet.id() == mc.player.getId()) {
            net.minecraft.world.phys.Vec3 velocity = packet.movement();
            double velocityMagnitude = Math.sqrt(velocity.x * velocity.x + velocity.y * velocity.y + velocity.z * velocity.z);
            com.eclipseware.imnotcheatingyouare.client.setting.Setting thresholdSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(jumpReset, "Velocity Threshold");
            double threshold = thresholdSetting != null ? thresholdSetting.getValDouble() : 0.1;
            if (velocityMagnitude > threshold) {
                jumpReset.onKnockback();
            }
        }
    }
}

