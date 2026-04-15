package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.utils.cheat.TimerUtil;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

public class Backtrack extends Module {
    private static final ConcurrentLinkedQueue<QueuedPacket> PACKET_QUEUE = new ConcurrentLinkedQueue<>();
    private static final AtomicLong LATENCY_TIMER = new AtomicLong(0);
    private static final List<Entity> TRACKED_ENTITIES = new ArrayList<>();
    private static volatile boolean isActive = false;

    private TimerUtil pulseTimer = new TimerUtil();
    private TimerUtil dumpCooldown = new TimerUtil();

    public Backtrack() {
        super("Backtrack", Category.Combat);
    }

    public static void queuePacket(Packet<?> packet) {
        if (!isActive) return;
        PACKET_QUEUE.offer(new QueuedPacket(packet, System.currentTimeMillis()));
    }

    public static boolean isActive() {
        return isActive;
    }

    public static ConcurrentLinkedQueue<QueuedPacket> getPacketQueue() {
        return PACKET_QUEUE;
    }

    public static void setLatencyTimer(long timer) {
        LATENCY_TIMER.set(timer);
    }

    public static void addTrackedEntity(Entity entity) {
        if (!TRACKED_ENTITIES.contains(entity)) {
            TRACKED_ENTITIES.add(entity);
        }
    }

    public static List<Entity> getTrackedEntities() {
        return new ArrayList<>(TRACKED_ENTITIES);
    }

    @Override
    public void onEnable() {
        isActive = true;
        PACKET_QUEUE.clear();
        Setting delayMin = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Delay Min (ms)");
        Setting delayMax = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Delay Max (ms)");
        long min = delayMin != null ? (long) delayMin.getValDouble() : 100L;
        long max = delayMax != null ? (long) delayMax.getValDouble() : 500L;
        LATENCY_TIMER.set((long) (min + Math.random() * (max - min)));
        pulseTimer.reset();
        dumpCooldown.reset();
    }

    @Override
    public void onDisable() {
        isActive = false;
        dumpPackets(false);
        TRACKED_ENTITIES.clear();
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.getConnection() == null) {
            PACKET_QUEUE.clear();
            return;
        }

        Setting modeSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Mode");
        String mode = modeSetting != null ? modeSetting.getValString() : "Latency";

        Setting delayMin = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Delay Min (ms)");
        Setting delayMax = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Delay Max (ms)");
        long min = delayMin != null ? (long) delayMin.getValDouble() : 100L;
        long max = delayMax != null ? (long) delayMax.getValDouble() : 500L;

        switch (mode) {
            case "Pulse" -> {
                if (pulseTimer.hasElapsedTime((long) (min + Math.random() * (max - min)))) {
                    dumpPackets(false);
                    pulseTimer.reset();
                }
            }
            case "Latency" -> dumpPackets(true);
            default -> dumpPackets(true);
        }
    }

    private void dumpPackets(boolean latencyOnly) {
        if (mc.getConnection() == null) {
            PACKET_QUEUE.clear();
            return;
        }

        int guard = 0;
        while (!PACKET_QUEUE.isEmpty() && guard++ < 1000) {
            QueuedPacket queued = PACKET_QUEUE.peek();
            if (queued == null) break;

            if (latencyOnly && queued.timestamp > 0 && queued.timestamp + LATENCY_TIMER.get() >= System.currentTimeMillis()) {
                break;
            }

            try {
                @SuppressWarnings("unchecked")
                Packet<PacketListener> typed = (Packet<PacketListener>) queued.packet;
                typed.apply(mc.getConnection());
            } catch (Throwable ignored) {
            }
            PACKET_QUEUE.poll();
        }
    }

    public static void dumpAll() {
        dumpPacketsStatic(false);
    }

    private static void dumpPacketsStatic(boolean latencyOnly) {
        if (mc.getConnection() == null) {
            PACKET_QUEUE.clear();
            return;
        }
        int guard = 0;
        while (!PACKET_QUEUE.isEmpty() && guard++ < 1000) {
            QueuedPacket queued = PACKET_QUEUE.poll();
            if (queued == null) break;
            try {
                @SuppressWarnings("unchecked")
                Packet<PacketListener> typed = (Packet<PacketListener>) queued.packet;
                typed.apply(mc.getConnection());
            } catch (Throwable ignored) {
            }
        }
    }

    public static record QueuedPacket(Packet<?> packet, long timestamp) {}
}
