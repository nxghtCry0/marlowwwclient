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
}

