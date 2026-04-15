package com.eclipseware.imnotcheatingyouare.mixin.client;

import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientCommonPacketListenerImpl.class)
public class ClientPacketListenerMixin {

    @Inject(method = "handleResourcePackPush", at = @At("HEAD"), cancellable = true)
    private void onResourcePackPush(ClientboundResourcePackPushPacket packet, CallbackInfo ci) {
        String url = packet.url().toLowerCase();
        
        // Block local SSRF (Server-Side Request Forgery) attempts
        if (url.contains("localhost") || url.contains("127.0.0.1") || url.contains("192.168.") || 
            url.contains("10.") || url.contains("172.16.") || url.contains("0.0.0.0")) {
            System.err.println("[EclipseWare] Blocked malicious local SSRF resource pack exploit from server: " + url);
            ci.cancel();
        }
    }
}