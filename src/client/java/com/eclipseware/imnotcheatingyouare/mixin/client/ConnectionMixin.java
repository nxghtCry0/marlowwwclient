package com.eclipseware.imnotcheatingyouare.mixin.client;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.module.ModuleManager;
import com.eclipseware.imnotcheatingyouare.client.module.impl.AutoSign;
import com.eclipseware.imnotcheatingyouare.client.module.impl.Backtrack;
import com.eclipseware.imnotcheatingyouare.client.module.impl.BlinkModule;
import com.eclipseware.imnotcheatingyouare.client.module.impl.FakeLag;
import com.eclipseware.imnotcheatingyouare.client.module.impl.PacketAuditor;
import com.eclipseware.imnotcheatingyouare.client.module.impl.Triggerbot;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.setting.SettingsManager;
import com.eclipseware.imnotcheatingyouare.client.utils.ModuleUtils;
import com.eclipseware.imnotcheatingyouare.client.utils.RotationManager;
import com.eclipseware.imnotcheatingyouare.client.utils.SilentAimUtil;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ClientboundPingPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.network.protocol.game.ServerboundCommandSuggestionPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Connection.class)
public class ConnectionMixin {

    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"), cancellable = true)
    private void onSend(Packet<?> packet, CallbackInfo ci) {
        if (FakeLag.isBypassedPacket(packet)) {
            return;
        }

        PacketAuditor auditor = PacketAuditor.INSTANCE;
        if (auditor != null && auditor.isToggled()) {
            PacketAuditor.auditPacket(packet);
        }

        ImnotcheatingyouareClient client = ImnotcheatingyouareClient.INSTANCE;
        ModuleManager mm = client != null ? client.moduleManager : null;
        SettingsManager sm = client != null ? client.settingsManager : null;

        if (packet instanceof ServerboundCommandSuggestionPacket) {
            if (mm != null && sm != null) {
                Module bypass = mm.getModule("Bypass");
                if (bypass != null && bypass.isToggled()) {
                    Setting disableAutofill = sm.getSettingByName(bypass, "Disable Command Autofill");
                    if (disableAutofill != null && disableAutofill.getValBoolean()) {
                        ci.cancel();
                        return;
                    }
                }
            }
        }

        if (BlinkModule.isActive()) {
            BlinkModule.queuePacket(packet);
            if (!(packet instanceof ServerboundSignUpdatePacket)) {
                ci.cancel();
                return;
            }
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }

        if (packet instanceof ServerboundMovePlayerPacket movePacket && !ModuleUtils.isSpoofing) {
            Module noFallMod = mm != null ? mm.getModule("NoFall") : null;
            if (noFallMod != null && noFallMod.isToggled() && mc.player.fallDistance > 2.0f && sm != null) {
                Setting modeSetting = sm.getSettingByName(noFallMod, "Mode");
                String modeVal = modeSetting != null ? modeSetting.getValString() : null;
                if (modeSetting == null || "Packet".equalsIgnoreCase(modeVal)) {
                    ((ServerboundMovePlayerPacketAccessor) movePacket).setOnGround(true);
                }
            }

            RotationManager.packetSentThisTick = true;
            boolean rotationSpoof = ModuleUtils.hasPendingPlacement() || RotationManager.isActive() || SilentAimUtil.isActive();
            boolean silentAimSpoof = SilentAimUtil.isActive();

            if (rotationSpoof || silentAimSpoof) {
                float yaw = silentAimSpoof
                    ? SilentAimUtil.getYaw()
                    : (RotationManager.isActive() ? RotationManager.getServerYaw() : mc.player.getYRot());
                float pitch = silentAimSpoof
                    ? SilentAimUtil.getPitch()
                    : (RotationManager.isActive() ? RotationManager.getServerPitch() : mc.player.getXRot());

                float lastYaw = Float.isNaN(RotationManager.lastSentYaw) ? mc.player.getYRot() : RotationManager.lastSentYaw;
                float lastPitch = Float.isNaN(RotationManager.lastSentPitch) ? mc.player.getXRot() : RotationManager.lastSentPitch;

                float gcd = RotationManager.getGCD();
                if (gcd < 0.001f) {
                    gcd = 0.15f;
                }

                float yawDiff = Mth.wrapDegrees(yaw - lastYaw);
                float pitchDiff = pitch - lastPitch;

                int yawSteps = Math.round(yawDiff / gcd);
                int pitchSteps = Math.round(pitchDiff / gcd);

                float finalYaw = lastYaw + yawSteps * gcd;
                float finalPitch = lastPitch + pitchSteps * gcd;
                finalPitch = Mth.clamp(finalPitch, -90.0f, 90.0f);

                boolean onGround = mc.player.onGround();
                if (noFallMod != null && noFallMod.isToggled() && mc.player.fallDistance > 2.0f && sm != null) {
                    Setting modeSetting = sm.getSettingByName(noFallMod, "Mode");
                    String modeVal = modeSetting != null ? modeSetting.getValString() : null;
                    if (modeSetting == null || "Packet".equalsIgnoreCase(modeVal)) {
                        onGround = true;
                    }
                }
                double px = movePacket.getX(mc.player.getX());
                double py = movePacket.getY(mc.player.getY());
                double pz = movePacket.getZ(mc.player.getZ());

                ServerboundMovePlayerPacket spoofed;
                boolean isRot = movePacket.hasRotation();
                boolean isPos = movePacket.hasPosition();

                boolean actualRotChanged = Math.abs(finalYaw - RotationManager.lastSentYaw) >= 0.01f
                    || Math.abs(finalPitch - RotationManager.lastSentPitch) >= 0.01f;

                if (actualRotChanged) {
                    if (isPos) {
                        spoofed = new ServerboundMovePlayerPacket.PosRot(px, py, pz, finalYaw, finalPitch, onGround, true);
                    } else {
                        spoofed = new ServerboundMovePlayerPacket.Rot(finalYaw, finalPitch, onGround, false);
                    }
                    RotationManager.lastSentYaw = finalYaw;
                    RotationManager.lastSentPitch = finalPitch;
                } else if (isPos) {
                    spoofed = new ServerboundMovePlayerPacket.Pos(px, py, pz, onGround, false);
                } else {
                    spoofed = new ServerboundMovePlayerPacket.Rot(RotationManager.lastSentYaw, RotationManager.lastSentPitch, onGround, false);
                }

                if (silentAimSpoof) {
                    SilentAimUtil.consume();
                }

                if (spoofed != null) {
                    ModuleUtils.isSpoofing = true;
                    ci.cancel();
                    Connection self = (Connection) (Object) this;
                    self.send(spoofed);
                    ModuleUtils.isSpoofing = false;
                    ModuleUtils.processPostMovement();
                    if (RotationManager.postMovementCallback != null) {
                        Runnable cb = RotationManager.postMovementCallback;
                        RotationManager.postMovementCallback = null;
                        cb.run();
                    }
                    return;
                }
            } else {
                if (movePacket.hasRotation()) {
                    RotationManager.lastSentYaw = movePacket.getYRot(mc.player.getYRot());
                    RotationManager.lastSentPitch = movePacket.getXRot(mc.player.getXRot());
                }
            }
        }

        if (FakeLag.shouldCancelOutgoing(packet)) {
            ci.cancel();
            return;
        }

        if (packet instanceof ServerboundSignUpdatePacket signPacket) {
            boolean hasText = false;
            String[] lines = signPacket.getLines();
            for (String line : lines) {
                if (line != null && !line.isEmpty()) {
                    hasText = true;
                    break;
                }
            }
            if (hasText) {
                AutoSign.savedLines = lines.clone();
                AutoSign.isFront = signPacket.isFrontText();
            }
        }
    }

    @Inject(method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"), cancellable = true)
    private void onChannelRead(ChannelHandlerContext context, Packet<?> packet, CallbackInfo ci) {
        FakeLag.onReceivePacket(packet);

        if (packet instanceof ClientboundSetTimePacket) {
            Triggerbot.onUpdateTimePacket();
        }
        Minecraft mc = Minecraft.getInstance();
        if (Backtrack.isActive()) {
            if (mc.player != null && mc.level != null) {
                if (!(packet instanceof ClientboundKeepAlivePacket)
                    && !(packet instanceof ClientboundPingPacket)
                    && !(packet instanceof ClientboundPlayerPositionPacket)
                    && !(packet instanceof ClientboundPlayerInfoUpdatePacket)) {
                    Backtrack.queuePacket(packet);
                    ci.cancel();
                    return;
                }
            }
        }

        if (packet instanceof ClientboundSystemChatPacket chatPacket) {
            ImnotcheatingyouareClient client = ImnotcheatingyouareClient.INSTANCE;
            ModuleManager mm = client != null ? client.moduleManager : null;
            if (mm != null) {
                Module antiTrans = mm.getModule("AntiTranslationKey");
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
