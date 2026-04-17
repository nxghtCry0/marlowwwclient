package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.utils.cheat.TimerUtil;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

public class BlinkModule extends Module {
    private static final ConcurrentLinkedQueue<Packet<?>> PACKET_QUEUE = new ConcurrentLinkedQueue<>();
    private static volatile boolean isActive = false;
    private static volatile boolean blockAll = false;

    private TimerUtil pulseTimer = new TimerUtil();
    private TimerUtil dumpCooldown = new TimerUtil();

    public BlinkModule() {
        super("Blink", Category.Combat);
    }

    public static void queuePacket(Packet<?> packet) {
        if (!isActive || blockAll) return;
        PACKET_QUEUE.offer(packet);
    }

    public static boolean isActive() {
        return isActive;
    }

    public static void setBlockAll(boolean block) {
        blockAll = block;
    }

    public static ConcurrentLinkedQueue<Packet<?>> getPacketQueue() {
        return PACKET_QUEUE;
    }

    public static void dumpPackets() {
        if (mc.getConnection() == null) {
            PACKET_QUEUE.clear();
            return;
        }
        int guard = 0;
        while (!PACKET_QUEUE.isEmpty() && guard++ < 1000) {
            Packet<?> packet = PACKET_QUEUE.poll();
            if (packet == null) break;
            try {
                @SuppressWarnings("unchecked")
                Packet<PacketListener> typed = (Packet<PacketListener>) packet;
                typed.apply(mc.getConnection());
            } catch (Throwable ignored) {
            }
        }
    }

    @Override
    public void onEnable() {
        isActive = true;
        blockAll = false;
        PACKET_QUEUE.clear();
        Setting delayMin = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Delay Min (ms)");
        Setting delayMax = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Delay Max (ms)");
        long min = delayMin != null ? (long) delayMin.getValDouble() : 100L;
        long max = delayMax != null ? (long) delayMax.getValDouble() : 500L;
        pulseTimer.reset();
        dumpCooldown.reset();
    }

    @Override
    public void onDisable() {
        isActive = false;
        blockAll = false;
        dumpPackets();
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.getConnection() == null) {
            PACKET_QUEUE.clear();
            return;
        }

        Setting modeSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Mode");
        String mode = modeSetting != null ? modeSetting.getValString() : "Pulse";

        Setting delayMin = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Delay Min (ms)");
        Setting delayMax = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Delay Max (ms)");
        long min = delayMin != null ? (long) delayMin.getValDouble() : 100L;
        long max = delayMax != null ? (long) delayMax.getValDouble() : 500L;

        if ("Pulse".equals(mode)) {
            if (pulseTimer.hasElapsedTime((long) (min + Math.random() * (max - min)))) {
                dumpPackets();
                pulseTimer.reset();
            }
        }
    }

    public static boolean shouldBlockInventory() {
        return isActive;
    }
}
