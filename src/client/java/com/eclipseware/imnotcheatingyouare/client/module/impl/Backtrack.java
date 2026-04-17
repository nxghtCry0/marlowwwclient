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
import com.eclipseware.imnotcheatingyouare.client.utils.FriendManager;
import com.eclipseware.imnotcheatingyouare.client.utils.RenderUtils;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

import java.awt.Color;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

public class Backtrack extends Module {

    private static final class TrackedPos {
        final long timestamp;
        final Vec3 pos;
        TrackedPos(long timestamp, Vec3 pos) {
            this.timestamp = timestamp;
            this.pos = pos;
        }
    }

    private final java.util.Map<Integer, Deque<TrackedPos>> positionHistory = new java.util.concurrent.ConcurrentHashMap<>();
    private Entity target;
    private long lastAttackTime = 0;
    private int currentChance = 0;

    public static Backtrack INSTANCE;

    public Backtrack() {
        super("Backtrack", Category.Combat, "Delays entity position updates to extend hitbox window.");
        INSTANCE = this;
        HudRenderCallback.EVENT.register((guiGraphics, tickDelta) -> renderBacktrack(guiGraphics, tickDelta));
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
        positionHistory.clear();
        target = null;
        currentChance = (int)(Math.random() * 100);
    }

    @Override
    public void onDisable() {
        isActive = false;
        dumpPackets(false);
        TRACKED_ENTITIES.clear();
        positionHistory.clear();
        target = null;
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
        if (!isToggled() || mc.player == null || mc.level == null) return;

        Setting rangeSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Range");
        double range = rangeSetting != null ? rangeSetting.getValDouble() : 3.0;

        Setting delaySetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Delay");
        int delay = delaySetting != null ? (int) delaySetting.getValDouble() : 150;

        Setting chanceSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Chance");
        float chance = chanceSetting != null ? (float) chanceSetting.getValDouble() : 50f;

        Setting attackTimeSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Attack Timeout");
        long attackTimeout = attackTimeSetting != null ? (long) attackTimeSetting.getValDouble() : 1000;

        long now = System.currentTimeMillis();
        boolean recentAttack = (now - lastAttackTime) < attackTimeout;

        Entity bestTarget = null;
        double bestDist = range + 1.0;

        for (Entity e : mc.level.entitiesForRendering()) {
            if (e == mc.player || !(e instanceof LivingEntity le) || !le.isAlive()) continue;
            if (e instanceof Player p && FriendManager.isFriend(p)) continue;

            double dist = mc.player.distanceTo(e);
            if (dist <= range && dist < bestDist) {
                bestDist = dist;
                bestTarget = e;
            }
        }

        if (bestTarget != null && recentAttack && currentChance < chance) {
            target = bestTarget;

            int id = target.getId();
            Deque<TrackedPos> history = positionHistory.computeIfAbsent(id, k -> new ArrayDeque<>());
            history.addLast(new TrackedPos(now, target.position()));

            while (!history.isEmpty() && (now - history.peekFirst().timestamp) > delay) {
                history.pollFirst();
            }
        } else {
            if (target != null) {
                positionHistory.remove(target.getId());
                target = null;
            }
            currentChance = (int)(Math.random() * 100);
        }

        Iterator<java.util.Map.Entry<Integer, Deque<TrackedPos>>> it = positionHistory.entrySet().iterator();
        while (it.hasNext()) {
            java.util.Map.Entry<Integer, Deque<TrackedPos>> entry = it.next();
            if (target == null || entry.getKey() != target.getId()) {
                it.remove();
            }
        }
    }

    public void onAttack(Entity entity) {
        lastAttackTime = System.currentTimeMillis();
        currentChance = (int)(Math.random() * 100);
    }

    public Vec3 getBacktrackedPos(Entity entity) {
        if (!isToggled() || entity == null) return null;
        Deque<TrackedPos> history = positionHistory.get(entity.getId());
        if (history == null || history.isEmpty()) return null;

        TrackedPos oldest = history.peekFirst();
        double myDist = mc.player.distanceTo(entity);
        Vec3 oldPos = oldest.pos;
        double oldDist = mc.player.position().distanceTo(oldPos);

        if (oldDist < myDist) {
            return oldPos;
        }

        return null;
    }

    public boolean isTracking() {
        return isToggled() && target != null && !positionHistory.isEmpty();
    }

    public Entity getTarget() {
        return target;
    }

    private void renderBacktrack(GuiGraphics guiGraphics, Object tickDeltaObj) {
        if (!isToggled() || mc.player == null || target == null) return;

        Setting vizSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Visualizer");
        if (vizSetting != null && !vizSetting.getValBoolean()) return;

        Vec3 btPos = getBacktrackedPos(target);
        if (btPos == null) return;

        float partialTick = getTickDelta(tickDeltaObj);
        float hw = target.getBbWidth() / 2.0f;
        float h = target.getBbHeight();

        double[][] corners = {
            {btPos.x - hw, btPos.y,     btPos.z - hw},
            {btPos.x + hw, btPos.y,     btPos.z - hw},
            {btPos.x - hw, btPos.y + h, btPos.z - hw},
            {btPos.x + hw, btPos.y + h, btPos.z - hw},
            {btPos.x - hw, btPos.y,     btPos.z + hw},
            {btPos.x + hw, btPos.y,     btPos.z + hw},
            {btPos.x - hw, btPos.y + h, btPos.z + hw},
            {btPos.x + hw, btPos.y + h, btPos.z + hw},
        };

        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        boolean valid = false;

        for (double[] c : corners) {
            Vector3d proj = RenderUtils.project2D(c[0], c[1], c[2], partialTick);
            if (proj == null) continue;
            valid = true;
            if (proj.x < minX) minX = proj.x;
            if (proj.x > maxX) maxX = proj.x;
            if (proj.y < minY) minY = proj.y;
            if (proj.y > maxY) maxY = proj.y;
        }
        if (!valid) return;

        int ix = (int) minX, iy = (int) minY, ix2 = (int) maxX, iy2 = (int) maxY;

        Color themeColor = RenderUtils.getThemeAccentColor();
        int ghostAlpha = 100;
        int ghostFill = new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), 25).getRGB();
        int ghostOutline = new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), ghostAlpha).getRGB();
        int black = new Color(0, 0, 0, ghostAlpha).getRGB();

        guiGraphics.fill(ix + 1, iy + 1, ix2 - 1, iy2 - 1, ghostFill);

        guiGraphics.fill(ix - 1, iy - 1, ix2 + 1, iy, black);
        guiGraphics.fill(ix - 1, iy2, ix2 + 1, iy2 + 1, black);
        guiGraphics.fill(ix - 1, iy - 1, ix, iy2 + 1, black);
        guiGraphics.fill(ix2, iy - 1, ix2 + 1, iy2 + 1, black);

        guiGraphics.fill(ix, iy, ix2, iy + 1, ghostOutline);
        guiGraphics.fill(ix, iy2 - 1, ix2, iy2, ghostOutline);
        guiGraphics.fill(ix, iy, ix + 1, iy2, ghostOutline);
        guiGraphics.fill(ix2 - 1, iy, ix2, iy2, ghostOutline);
    }

    private float getTickDelta(Object tickDeltaObj) {
        if (tickDeltaObj instanceof Float) return (Float) tickDeltaObj;
        for (java.lang.reflect.Method m : tickDeltaObj.getClass().getMethods()) {
            if (m.getReturnType() == float.class) {
                if (m.getParameterCount() == 1 && m.getParameterTypes()[0] == boolean.class) {
                    try { return (float) m.invoke(tickDeltaObj, true); } catch (Exception e) {}
                } else if (m.getParameterCount() == 0) {
                    String name = m.getName().toLowerCase();
                    if (name.contains("tick") || name.contains("delta") || name.contains("frame")) {
                        try { return (float) m.invoke(tickDeltaObj); } catch (Exception e) {}
                    }
                }
            }
        }
        return 1.0f;
    }
}
