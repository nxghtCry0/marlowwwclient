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

    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"), cancellable = true)
    private void onSend(Packet<?> packet, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // Silent Aim Packet Interception (skip if RotationManager is sending its own packets)
        if (SilentAimUtil.isActive() && !isSpoofing && packet instanceof ServerboundMovePlayerPacket && !com.eclipseware.imnotcheatingyouare.client.utils.RotationManager.isSendingPacket()) {
            isSpoofing = true;
            float yaw = SilentAimUtil.getYaw();
            float pitch = SilentAimUtil.getPitch();
            boolean onGround = mc.player.onGround();
            double px = mc.player.getX();
            double py = mc.player.getY();
            double pz = mc.player.getZ();
            
            ServerboundMovePlayerPacket spoofed = null;
            
            if (packet instanceof ServerboundMovePlayerPacket.Rot) {
                // Constructor: (yRot, xRot, onGround, hasPosition)
                spoofed = new ServerboundMovePlayerPacket.Rot(yaw, pitch, onGround, false);
            } else if (packet instanceof ServerboundMovePlayerPacket.PosRot) {
                // Constructor: (x, y, z, yRot, xRot, onGround, hasPosition)
                spoofed = new ServerboundMovePlayerPacket.PosRot(px, py, pz, yaw, pitch, onGround, true);
            } else if (packet instanceof ServerboundMovePlayerPacket.Pos) {
                // Convert Pos to PosRot to inject rotations
                spoofed = new ServerboundMovePlayerPacket.PosRot(px, py, pz, yaw, pitch, onGround, true);
            }

            if (spoofed != null) {
                SilentAimUtil.consume();
                ci.cancel();
                ((Connection)(Object)this).send(spoofed);
                isSpoofing = false;
                return;
            }
            isSpoofing = false;
        }

        // AutoSign functionality
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

        // Freecam movement suppression
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
// Anti-translation key crash protection
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