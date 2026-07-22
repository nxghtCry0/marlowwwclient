package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.entity.Entity;

public class PacketAuditor extends Module {
    public static PacketAuditor INSTANCE;
    
    private static long lastMovePacketTime = 0;
    private static int movePacketsThisTick = 0;
    private static long lastTickTime = 0;
    private static float lastYaw = Float.NaN;
    private static float lastPitch = Float.NaN;
    
    public PacketAuditor() {
        super("PacketAuditor", Category.Misc, "Audits outgoing packets for potential anti-cheat flags.");
        INSTANCE = this;
        
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Log to Chat", this, true));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Check Spikes", this, true));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Check Attack Range", this, true));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Check Rotations", this, true));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Log All Packets", this, false));
    }
    
    public static void auditPacket(Packet<?> packet) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        
        long now = System.currentTimeMillis();
        
        boolean logAll = getSettingVal("Log All Packets");
        boolean checkSpikes = getSettingVal("Check Spikes");
        boolean checkAttack = getSettingVal("Check Attack Range");
        boolean checkRots = getSettingVal("Check Rotations");
        
        if (logAll) {
            log("[Auditor] Sent: " + packet.getClass().getSimpleName());
        }
        
        if (packet instanceof ServerboundMovePlayerPacket movePacket) {
            long currentTick = mc.level != null ? mc.level.getGameTime() : 0;
            if (currentTick != lastTickTime) {
                if (checkSpikes && movePacketsThisTick > 2) {
                    log("§c[Warning] Packet Spike: Sent " + movePacketsThisTick + " movement packets in tick " + lastTickTime);
                }
                movePacketsThisTick = 0;
                lastTickTime = currentTick;
            }
            movePacketsThisTick++;
            
            if (movePacket.hasRotation()) {
                float yaw = movePacket.getYRot(mc.player.getYRot());
                float pitch = movePacket.getXRot(mc.player.getXRot());
                
                if (checkRots) {
                    if (pitch < -90.0f || pitch > 90.0f) {
                        log("§c[Flag] Pitch Out of Bounds: " + pitch);
                    }
                    if (!Float.isNaN(lastYaw) && !Float.isNaN(lastPitch)) {
                        float yawDiff = Math.abs(yaw - lastYaw);
                        float pitchDiff = Math.abs(pitch - lastPitch);
                        if (yawDiff > 120.0f || pitchDiff > 80.0f) {
                            log("§e[Warning] Large Rotation Jump: Yaw delta=" + String.format("%.2f", yawDiff) + ", Pitch delta=" + String.format("%.2f", pitchDiff));
                        }
                    }
                }
                lastYaw = yaw;
                lastPitch = pitch;
            }
            
            lastMovePacketTime = now;
        }
        
        if (packet instanceof ServerboundInteractPacket interactPacket) {
            if (checkAttack) {
                int targetId = interactPacket.entityId();
                Entity target = mc.level != null ? mc.level.getEntity(targetId) : null;
                if (target != null) {
                    double distance = mc.player.distanceTo(target);
                    boolean creative = mc.player.isCreative();
                    double maxReach = creative ? 4.5 : 3.0;
                    if (distance > maxReach + 0.05) {
                        log("§c[Warning] Long Attack: target " + target.getName().getString() + " at " + String.format("%.2f", distance) + " blocks (max: " + maxReach + ")");
                    }
                }
            }
        }
    }
    
    private static boolean getSettingVal(String name) {
        if (INSTANCE == null) return false;
        Setting setting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(INSTANCE, name);
        return setting != null && setting.getValBoolean();
    }
    
    private static void log(String message) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && getSettingVal("Log to Chat")) {
            mc.player.sendSystemMessage(Component.literal(message));
        }
    }
    
    @Override
    public void onTick() {
        if (mc.player == null) {
            movePacketsThisTick = 0;
            lastYaw = Float.NaN;
            lastPitch = Float.NaN;
        }
    }
}
