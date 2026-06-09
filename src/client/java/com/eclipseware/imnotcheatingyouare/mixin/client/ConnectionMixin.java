package com.eclipseware.imnotcheatingyouare.mixin.client;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.utils.SilentAimUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Connection.class)
public class ConnectionMixin {
    private static float lastSentYaw = Float.NaN;
    private static float lastSentPitch = Float.NaN;

    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"), cancellable = true)
    private void onSend(Packet<?> packet, CallbackInfo ci) {
        if (packet instanceof net.minecraft.network.protocol.game.ServerboundCommandSuggestionPacket) {
            if (ImnotcheatingyouareClient.INSTANCE != null && ImnotcheatingyouareClient.INSTANCE.moduleManager != null) {
                Module bypass = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("Bypass");
                if (bypass != null && bypass.isToggled()) {
                    com.eclipseware.imnotcheatingyouare.client.setting.Setting disableAutofill = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(bypass, "Disable Command Autofill");
                    if (disableAutofill != null && disableAutofill.getValBoolean()) {
                        ci.cancel();
                        return;
                    }
                }
            }
        }

        if (com.eclipseware.imnotcheatingyouare.client.module.impl.BlinkModule.isActive()) {
            com.eclipseware.imnotcheatingyouare.client.module.impl.BlinkModule.queuePacket(packet);
            if (!(packet instanceof ServerboundSignUpdatePacket)) {
                ci.cancel();
                return;
            }
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        if (packet instanceof ServerboundMovePlayerPacket movePacket && !com.eclipseware.imnotcheatingyouare.client.utils.ModuleUtils.isSpoofing) {
            boolean rotationSpoof = com.eclipseware.imnotcheatingyouare.client.utils.ModuleUtils.hasPendingPlacement() || com.eclipseware.imnotcheatingyouare.client.utils.RotationManager.isActive() || SilentAimUtil.isActive();
            boolean silentAimSpoof = SilentAimUtil.isActive();
            
            if (rotationSpoof || silentAimSpoof) {
                float yaw = silentAimSpoof ? SilentAimUtil.getYaw() : (com.eclipseware.imnotcheatingyouare.client.utils.RotationManager.isActive() ? com.eclipseware.imnotcheatingyouare.client.utils.RotationManager.getServerYaw() : mc.player.getYRot());
                float pitch = silentAimSpoof ? SilentAimUtil.getPitch() : (com.eclipseware.imnotcheatingyouare.client.utils.RotationManager.isActive() ? com.eclipseware.imnotcheatingyouare.client.utils.RotationManager.getServerPitch() : mc.player.getXRot());
                
                // Add tiny sub-GCD jitter to yaw and pitch to bypass AimDuplicateLook checks
                float jitterYaw = yaw + (float) ((Math.random() - 0.5) * 0.01);
                float jitterPitch = pitch + (float) ((Math.random() - 0.5) * 0.01);
                
                boolean onGround = mc.player.onGround();
                double px = movePacket.getX(mc.player.getX());
                double py = movePacket.getY(mc.player.getY());
                double pz = movePacket.getZ(mc.player.getZ());
                
                ServerboundMovePlayerPacket spoofed = null;
                boolean isRot = movePacket.hasRotation();
                boolean isPos = movePacket.hasPosition();
                
                boolean forceRot = com.eclipseware.imnotcheatingyouare.client.utils.ModuleUtils.hasPendingPlacement();
                boolean rotChanged = forceRot || Math.abs(jitterYaw - lastSentYaw) >= 0.01f || Math.abs(jitterPitch - lastSentPitch) >= 0.01f;

                if (rotChanged) {
                    if (isPos) {
                        spoofed = new ServerboundMovePlayerPacket.PosRot(px, py, pz, jitterYaw, jitterPitch, onGround, true);
                    } else {
                        spoofed = new ServerboundMovePlayerPacket.Rot(jitterYaw, jitterPitch, onGround, false);
                    }
                    lastSentYaw = jitterYaw;
                    lastSentPitch = jitterPitch;
                } else if (isPos) {
                    // Force PosRot anyway to ensure rotation synchronization when pending placement is active
                    if (forceRot) {
                        spoofed = new ServerboundMovePlayerPacket.PosRot(px, py, pz, jitterYaw, jitterPitch, onGround, true);
                        lastSentYaw = jitterYaw;
                        lastSentPitch = jitterPitch;
                    } else {
                        spoofed = new ServerboundMovePlayerPacket.Pos(px, py, pz, onGround, false);
                    }
                } else if (isRot) {
                    if (forceRot) {
                        spoofed = new ServerboundMovePlayerPacket.Rot(jitterYaw, jitterPitch, onGround, false);
                        lastSentYaw = jitterYaw;
                        lastSentPitch = jitterPitch;
                    } else {
                        ci.cancel();
                        if (silentAimSpoof) SilentAimUtil.consume();
                        com.eclipseware.imnotcheatingyouare.client.utils.ModuleUtils.processPostMovement();
                        return;
                    }
                }

                if (silentAimSpoof) SilentAimUtil.consume();

                if (spoofed != null) {
                    com.eclipseware.imnotcheatingyouare.client.utils.ModuleUtils.isSpoofing = true;
                    ci.cancel();
                    ((Connection)(Object)this).send(spoofed);
                    com.eclipseware.imnotcheatingyouare.client.utils.ModuleUtils.isSpoofing = false;
                    com.eclipseware.imnotcheatingyouare.client.utils.ModuleUtils.processPostMovement();
                    return;
                }
            } else {
                if (movePacket.hasRotation()) {
                    lastSentYaw = movePacket.getYRot(mc.player.getYRot());
                    lastSentPitch = movePacket.getXRot(mc.player.getXRot());
                }
            }
        }

        if (packet instanceof ServerboundSignUpdatePacket signPacket) {
            boolean hasText = false;
            for (String line : signPacket.getLines()) {
                if (line != null && !line.isEmpty()) {
                    hasText = true;
                    break;
                }
            }
            if (hasText) {
                com.eclipseware.imnotcheatingyouare.client.module.impl.AutoSign.savedLines = signPacket.getLines();
                com.eclipseware.imnotcheatingyouare.client.module.impl.AutoSign.isFront = signPacket.isFrontText();
            }
        }

        if (packet instanceof ServerboundMovePlayerPacket ||
            packet instanceof net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket ||
            packet instanceof net.minecraft.network.protocol.game.ServerboundSwingPacket) {
            if (ImnotcheatingyouareClient.INSTANCE != null && ImnotcheatingyouareClient.INSTANCE.moduleManager != null) {
                Module freecam = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("Freecam");
                if (freecam != null && freecam.isToggled()) {
                    ci.cancel();
                }
            }
        }

        }
    @Inject(method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"), cancellable = true)
    private void onChannelRead(io.netty.channel.ChannelHandlerContext context, Packet<?> packet, CallbackInfo ci) {
        if (com.eclipseware.imnotcheatingyouare.client.module.impl.Backtrack.isActive()) {
            if (Minecraft.getInstance().player != null && Minecraft.getInstance().level != null) {
                if (!(packet instanceof net.minecraft.network.protocol.common.ClientboundKeepAlivePacket) &&
                    !(packet instanceof net.minecraft.network.protocol.common.ClientboundPingPacket) &&
                    !(packet instanceof net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket) &&
                    !(packet instanceof net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket)) {
                    com.eclipseware.imnotcheatingyouare.client.module.impl.Backtrack.queuePacket(packet);
                    ci.cancel();
                    return;
                }
            }
        }

        if (packet instanceof net.minecraft.network.protocol.game.ClientboundSystemChatPacket chatPacket) {
            if (ImnotcheatingyouareClient.INSTANCE != null && ImnotcheatingyouareClient.INSTANCE.moduleManager != null) {
                Module antiTrans = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("AntiTranslationKey");
                if (antiTrans != null && antiTrans.isToggled()) {
                    String content = chatPacket.content().getString();
                    if (content.contains("%s%s%s%s%s")) {
                        ci.cancel();
                    }
                }
            }
        }
    }
}