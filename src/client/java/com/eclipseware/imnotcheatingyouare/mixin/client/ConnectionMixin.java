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
    private static boolean isSpoofing = false;
    private static float lastSentYaw = Float.NaN;
    private static float lastSentPitch = Float.NaN;

    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"), cancellable = true)
    private void onSend(Packet<?> packet, CallbackInfo ci) {
        if (com.eclipseware.imnotcheatingyouare.client.module.impl.BlinkModule.isActive()) {
            com.eclipseware.imnotcheatingyouare.client.module.impl.BlinkModule.queuePacket(packet);
            if (!(packet instanceof ServerboundSignUpdatePacket)) {
                ci.cancel();
                return;
            }
        }

        if (com.eclipseware.imnotcheatingyouare.client.module.impl.Backtrack.isActive()) {
            com.eclipseware.imnotcheatingyouare.client.module.impl.Backtrack.queuePacket(packet);
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        if (packet instanceof ServerboundMovePlayerPacket movePacket && !isSpoofing) {
            boolean rotationSpoof = com.eclipseware.imnotcheatingyouare.client.utils.RotationManager.isActive();
            boolean silentAimSpoof = SilentAimUtil.isActive();
            
            if (rotationSpoof || silentAimSpoof) {
                float yaw = silentAimSpoof ? SilentAimUtil.getYaw() : com.eclipseware.imnotcheatingyouare.client.utils.RotationManager.getServerYaw();
                float pitch = silentAimSpoof ? SilentAimUtil.getPitch() : com.eclipseware.imnotcheatingyouare.client.utils.RotationManager.getServerPitch();
                
                boolean onGround = mc.player.onGround();
                double px = movePacket.getX(mc.player.getX());
                double py = movePacket.getY(mc.player.getY());
                double pz = movePacket.getZ(mc.player.getZ());
                
                ServerboundMovePlayerPacket spoofed = null;
                boolean isRot = movePacket.hasRotation();
                boolean isPos = movePacket.hasPosition();
                boolean rotChanged = Math.abs(yaw - lastSentYaw) >= 0.05f || Math.abs(pitch - lastSentPitch) >= 0.05f;

                if (rotChanged) {
                    if (isPos) {
                        spoofed = new ServerboundMovePlayerPacket.PosRot(px, py, pz, yaw, pitch, onGround, true);
                    } else {
                        spoofed = new ServerboundMovePlayerPacket.Rot(yaw, pitch, onGround, false);
                    }
                    lastSentYaw = yaw;
                    lastSentPitch = pitch;
                } else if (isPos) {
                    spoofed = new ServerboundMovePlayerPacket.Pos(px, py, pz, onGround, false);
                } else if (isRot) {
                    ci.cancel();
                    if (silentAimSpoof) SilentAimUtil.consume();
                    return;
                }

                if (silentAimSpoof) SilentAimUtil.consume();

                if (spoofed != null) {
                    isSpoofing = true;
                    ci.cancel();
                    ((Connection)(Object)this).send(spoofed);
                    isSpoofing = false;
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