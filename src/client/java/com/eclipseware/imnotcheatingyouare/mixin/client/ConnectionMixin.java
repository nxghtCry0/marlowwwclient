package com.eclipseware.imnotcheatingyouare.mixin.client;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
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
        }

        if (packet instanceof ServerboundSignUpdatePacket signPacket) {
            // Only save the lines if there is actually text on them.
            // Otherwise, when we place a new blank sign, it overwrites our saved text with blanks!
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
                    ci.cancel(); // Silently stop movement, sprinting, and swinging while freecam is active
                }
            }
        }
    }

    // Intercept incoming packets to prevent translation key crashes
    @Inject(method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"), cancellable = true)
    private void onChannelRead(io.netty.channel.ChannelHandlerContext context, Packet<?> packet, CallbackInfo ci) {
        if (packet instanceof net.minecraft.network.protocol.game.ClientboundSystemChatPacket chatPacket) {
            if (ImnotcheatingyouareClient.INSTANCE != null && ImnotcheatingyouareClient.INSTANCE.moduleManager != null) {
                Module antiTrans = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("AntiTranslationKey");
                if (antiTrans != null && antiTrans.isToggled()) {
                    String content = chatPacket.content().getString();
                    // If the server sends a massive formatting chain designed to cause a String.format exception, nuke it
                    if (content.contains("%s%s%s%s%s")) {
                        ci.cancel();
                    }
                }
            }
        }
    }
}