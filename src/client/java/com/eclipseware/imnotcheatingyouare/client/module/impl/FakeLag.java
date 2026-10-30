package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.setting.SettingsManager;
import com.eclipseware.imnotcheatingyouare.client.utils.FriendManager;
import com.eclipseware.imnotcheatingyouare.client.utils.RenderUtils;
import com.eclipseware.imnotcheatingyouare.client.utils.TargetFilterManager;
import com.eclipseware.imnotcheatingyouare.mixin.client.ServerboundInteractPacketAccessor;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket;
import net.minecraft.network.protocol.common.ClientboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ClientboundPingPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ServerboundPongPacket;
import net.minecraft.network.protocol.game.ClientboundAnimatePacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundHurtAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.network.protocol.game.ServerboundCommandSuggestionPacket;
import net.minecraft.network.protocol.game.ServerboundConfigurationAcknowledgedPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

public class FakeLag extends Module {
    public static FakeLag INSTANCE;

    private static final class TimedPacket {
        final Packet<?> packet;
        final long timestamp;

        TimedPacket(Packet<?> packet, long timestamp) {
            this.packet = packet;
            this.timestamp = timestamp;
        }
    }

    private static final class DelayedPacketEntry {
        final Packet<?> packet;
        boolean offsetApplied;

        DelayedPacketEntry(Packet<?> packet) {
            this.packet = packet;
            this.offsetApplied = false;
        }
    }

    private static final class AttackDetector implements ServerboundInteractPacket.Handler {
        boolean attack;

        @Override
        public void onInteraction(InteractionHand hand) {
        }

        @Override
        public void onInteraction(InteractionHand hand, Vec3 location) {
        }

        @Override
        public void onAttack() {
            this.attack = true;
        }
    }

    private static final ConcurrentLinkedQueue<TimedPacket> LATENCY_QUEUE = new ConcurrentLinkedQueue<>();
    private static final ConcurrentLinkedQueue<Packet<?>> PULSE_QUEUE = new ConcurrentLinkedQueue<>();
    private static final ConcurrentLinkedQueue<Packet<?>> REPEL_QUEUE = new ConcurrentLinkedQueue<>();
    private static final Map<DelayedPacketEntry, Long> DYNAMIC_PACKETS = new LinkedHashMap<>();
    private static final ReentrantReadWriteLock DYNAMIC_LOCK = new ReentrantReadWriteLock();
    private static final Set<Packet<?>> BYPASSED_PACKETS = ConcurrentHashMap.newKeySet();
    private static final ThreadLocal<Boolean> DISPATCHING = ThreadLocal.withInitial(() -> false);
    private static final AtomicBoolean FLUSHING = new AtomicBoolean(false);
    private static final List<Long> HIT_DELAYS = new ArrayList<>();
    private static final Vector3d[] PROJ_BUFFER = new Vector3d[8];

    private static volatile boolean isActive = false;
    private static volatile long lastDumpTime = 0L;
    private static volatile long currentPulseDelay = 150L;
    private static volatile long repelFlushDeadline = 0L;
    private static volatile LivingEntity repelTarget = null;
    private static volatile Entity dynamicTarget = null;
    private static volatile Vec3 lastPositionSnapshot = null;
    private static volatile Vec3 lastServerPosition = null;
    private static volatile long lastDynamicAttackTime = 0L;
    private static volatile long lastBlockPlaceTime = 0L;
    private static volatile long blockPlaceCooldownUntil = 0L;
    private static volatile long trackerLastHitTime = 0L;
    private static volatile long trackerLastAttackTime = 0L;
    private static volatile int trackedTargetId = -1;

    static {
        for (int i = 0; i < 8; i++) {
            PROJ_BUFFER[i] = new Vector3d();
        }
    }

    public FakeLag() {
        super("FakeLag", Category.Movement, "Simulates network latency with dynamic combat packet scheduling.");
        INSTANCE = this;
        ImnotcheatingyouareClient client = ImnotcheatingyouareClient.INSTANCE;
        if (client != null && client.settingsManager != null) {
            SettingsManager sm = client.settingsManager;
            ArrayList<String> modes = new ArrayList<>();
            modes.add("Dynamic");
            modes.add("Latency");
            modes.add("Repel");
            modes.add("Pulse");
            sm.rSetting(new Setting("Mode", this, "Dynamic", modes));
            sm.rSetting(new Setting("Delay (ms)", this, 100.0, 1.0, 1000.0, true));
            sm.rSetting(new Setting("Transmission Offset", this, 5.0, 0.0, 50.0, true));
            sm.rSetting(new Setting("Randomize (100-200ms)", this, false));
            sm.rSetting(new Setting("Flush on Attack", this, true));
            sm.rSetting(new Setting("Visualizer", this, true));
        }
    }

    public static boolean isActive() {
        return isActive;
    }

    public static boolean isBypassedPacket(Packet<?> packet) {
        if (packet == null) {
            return false;
        }
        Boolean localDispatch = DISPATCHING.get();
        if (Boolean.TRUE.equals(localDispatch)) {
            return true;
        }
        return BYPASSED_PACKETS.contains(packet);
    }

    public static void queuePacket(Packet<?> packet) {
        shouldCancelOutgoing(packet);
    }

    public static boolean shouldCancelOutgoing(Packet<?> packet) {
        if (!isActive || packet == null) {
            return false;
        }
        if (isBypassedPacket(packet) || isExcludedPacket(packet)) {
            return false;
        }
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null || client.level == null) {
            flushAllModes(false);
            return false;
        }
        ClientPacketListener conn = client.getConnection();
        if (conn == null) {
            clearAllQueues();
            return false;
        }

        if (packet instanceof ServerboundInteractPacket interactPacket) {
            recordAttackTiming(interactPacket, client.level);
        }

        String mode = getModeString();
        if ("Latency".equalsIgnoreCase(mode)) {
            return handleLatencyOutgoing(packet, client.level);
        }
        if ("Repel".equalsIgnoreCase(mode)) {
            return handleRepelOutgoing(packet, client);
        }
        if ("Pulse".equalsIgnoreCase(mode)) {
            return handlePulseOutgoing(packet, client.level);
        }
        return handleDynamicOutgoing(packet, client);
    }

    public static void onReceivePacket(Packet<?> packet) {
        if (packet == null) {
            return;
        }
        if (packet instanceof ClientboundDisconnectPacket || packet instanceof ClientboundRespawnPacket) {
            clearAllQueues();
            return;
        }
        if (packet instanceof ClientboundPlayerPositionPacket) {
            if (isActive) {
                flushAllModes(true);
            }
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.level == null) {
            return;
        }
        int damagedId = extractDamagedEntityId(packet, client.level);
        if (damagedId != -1 && damagedId == trackedTargetId) {
            recordHitDelaySample();
        }
    }

    private static int extractDamagedEntityId(Packet<?> packet, ClientLevel level) {
        if (packet instanceof ClientboundHurtAnimationPacket hurtPacket) {
            return hurtPacket.id();
        }
        if (packet instanceof ClientboundEntityEventPacket statusPacket) {
            if (statusPacket.getEventId() == 2) {
                Entity entity = statusPacket.getEntity(level);
                return entity != null ? entity.getId() : -1;
            }
            return -1;
        }
        if (packet instanceof ClientboundAnimatePacket animPacket) {
            if (animPacket.getAction() == 1) {
                return animPacket.getId();
            }
        }
        return -1;
    }

    private static void recordHitDelaySample() {
        long now = System.currentTimeMillis();
        long delay = now - trackerLastHitTime;
        if (trackerLastHitTime > 0L && delay > 0L && delay < 500L) {
            synchronized (HIT_DELAYS) {
                HIT_DELAYS.add(delay);
                if (HIT_DELAYS.size() > 20) {
                    HIT_DELAYS.remove(0);
                }
            }
        }
    }

    private static void recordAttackTiming(ServerboundInteractPacket interactPacket, ClientLevel level) {
        if (!isAttackPacket(interactPacket) || level == null) {
            return;
        }
        int entityId = getInteractEntityId(interactPacket);
        Entity entity = level.getEntity(entityId);
        if (!(entity instanceof LivingEntity living)) {
            return;
        }
        trackedTargetId = entityId;
        long now = System.currentTimeMillis();
        long avgDelay = getAverageHitDelay();
        if (living.hurtTime == 0 && now - trackerLastHitTime > 400L && now - trackerLastAttackTime > avgDelay * 2L) {
            trackerLastHitTime = now;
        }
        trackerLastAttackTime = now;
    }

    public static long getAverageHitDelay() {
        synchronized (HIT_DELAYS) {
            if (HIT_DELAYS.isEmpty()) {
                return 0L;
            }
            long sum = 0L;
            for (long d : HIT_DELAYS) {
                sum += d;
            }
            return sum / HIT_DELAYS.size();
        }
    }

    public static int getExpectedHurtTimeTicks() {
        double avg = (double) getAverageHitDelay();
        return (int) Math.floor(avg / 50.0);
    }

    private static boolean handleLatencyOutgoing(Packet<?> packet, ClientLevel level) {
        if (FLUSHING.get()) {
            return false;
        }
        if (isFlushOnAttackEnabled() && isAttackPacket(packet)) {
            Entity target = getAttackedEntity(packet, level);
            if (target instanceof LivingEntity living) {
                int expected = getExpectedHurtTimeTicks();
                if (living.hurtTime <= expected + 1) {
                    flushLatency(true);
                    return false;
                }
            }
        }
        flushLatency(false);
        long now = System.currentTimeMillis();
        LATENCY_QUEUE.add(new TimedPacket(packet, now));
        return true;
    }

    private static boolean handlePulseOutgoing(Packet<?> packet, ClientLevel level) {
        if (FLUSHING.get()) {
            return false;
        }
        if (isFlushOnAttackEnabled() && isAttackPacket(packet)) {
            Entity target = getAttackedEntity(packet, level);
            if (target instanceof LivingEntity) {
                flushPulse();
                return false;
            }
        }
        if (packet instanceof ServerboundMovePlayerPacket) {
            PULSE_QUEUE.offer(packet);
            return true;
        }
        return false;
    }

    private static boolean handleRepelOutgoing(Packet<?> packet, Minecraft client) {
        if (FLUSHING.get()) {
            return false;
        }
        long now = System.currentTimeMillis();
        if (!REPEL_QUEUE.isEmpty()) {
            boolean shouldFlush = now >= repelFlushDeadline;
            if (isAttackPacket(packet)) {
                shouldFlush = true;
            }
            if (packet instanceof ServerboundChatPacket || packet instanceof ServerboundChatCommandPacket) {
                shouldFlush = true;
            }
            if (!client.isSameThread()) {
                shouldFlush = false;
            }
            REPEL_QUEUE.add(packet);
            if (shouldFlush) {
                flushRepel();
            }
            return true;
        }

        if (isAttackPacket(packet)) {
            Entity attacked = getAttackedEntity(packet, client.level);
            if (attacked instanceof LivingEntity living) {
                repelTarget = living;
                long baseDelay = getConfiguredDelayMs();
                int randomExtra = ThreadLocalRandom.current().nextInt(100);
                long delay = baseDelay + randomExtra;
                int maxHurtTicks = (int) Math.ceil(delay / 50.0);
                if (living.hurtTime > 0 && living.hurtTime <= maxHurtTicks) {
                    REPEL_QUEUE.add(packet);
                    repelFlushDeadline = now + delay;
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean handleDynamicOutgoing(Packet<?> packet, Minecraft client) {
        LocalPlayer player = client.player;
        ClientLevel level = client.level;
        if (player == null || level == null) {
            return false;
        }
        long now = System.currentTimeMillis();

        if (isAttackPacket(packet)) {
            Entity attacked = getAttackedEntity(packet, level);
            if (attacked instanceof LivingEntity livingTarget) {
                int expectedHurt = getExpectedHurtTimeTicks();
                if (livingTarget.hurtTime <= expectedHurt) {
                    if (now - lastDynamicAttackTime > 250L) {
                        flushDynamicAll();
                        return false;
                    }
                    lastDynamicAttackTime = now;
                } else if (livingTarget.hurtTime <= expectedHurt + 1) {
                    flushDynamicAll();
                    return false;
                }
            }
        } else {
            if (isActionOrUiPacket(packet)) {
                return delaySendDynamic(packet);
            }
            if (isPlacementOrUsePacket(packet)) {
                ItemStack mainStack = player.getMainHandItem();
                if (mainStack != null && !mainStack.isEmpty()) {
                    if (!isSwordItem(mainStack)) {
                        boolean queued = delaySendDynamic(packet);
                        blockPlaceCooldownUntil = now + 500L;
                        return queued;
                    }
                    if (now - lastBlockPlaceTime >= 500L) {
                        boolean queued = delaySendDynamic(packet);
                        lastBlockPlaceTime = now;
                        return queued;
                    }
                }
            } else {
                boolean usingNonBlocking = player.isUsingItem() && !player.isBlocking();
                boolean fallingFar = !player.onGround() && player.fallDistance > 3.0F;
                if (blockPlaceCooldownUntil > now || usingNonBlocking || fallingFar) {
                    return delaySendDynamic(packet);
                }
            }
        }

        Entity currentTarget = dynamicTarget;
        if (currentTarget instanceof LivingEntity living && living.isAlive()) {
            if (packet instanceof ServerboundMovePlayerPacket movePacket && movePacket.hasPosition()) {
                double px = movePacket.getX(player.getX());
                double py = movePacket.getY(player.getY());
                double pz = movePacket.getZ(player.getZ());
                Vec3 packetPosition = new Vec3(px, py, pz);
                Vec3 prevSnapshot = lastPositionSnapshot;
                lastPositionSnapshot = packetPosition;
                if (prevSnapshot != null) {
                    Vec3 targetPosition = living.position();
                    double prevRay = rayIntersectionDistance(prevSnapshot, targetPosition);
                    double currRay = rayIntersectionDistance(packetPosition, targetPosition);
                    if (prevRay < currRay - 0.03) {
                        return delaySendDynamic(packet);
                    }
                }
            }

            long baseDelay = getConfiguredDelayMs();
            long sendTime = now + baseDelay;
            long lastPosPacketTime = now;

            ReentrantReadWriteLock.ReadLock readLock = DYNAMIC_LOCK.readLock();
            readLock.lock();
            try {
                for (Map.Entry<DelayedPacketEntry, Long> entry : DYNAMIC_PACKETS.entrySet()) {
                    DelayedPacketEntry key = entry.getKey();
                    Packet<?> queued = key.packet;
                    if (queued instanceof ServerboundMovePlayerPacket move && move.hasPosition()) {
                        lastPosPacketTime = entry.getValue();
                    }
                }
            } finally {
                readLock.unlock();
            }

            sendTime = Math.min(lastPosPacketTime + 90L, sendTime);
            ReentrantReadWriteLock.WriteLock writeLock = DYNAMIC_LOCK.writeLock();
            writeLock.lock();
            try {
                DYNAMIC_PACKETS.put(new DelayedPacketEntry(packet), sendTime);
            } finally {
                writeLock.unlock();
            }
            return true;
        }

        if (packet instanceof ServerboundMovePlayerPacket movePacket && movePacket.hasPosition()) {
            double px = movePacket.getX(player.getX());
            double py = movePacket.getY(player.getY());
            double pz = movePacket.getZ(player.getZ());
            lastPositionSnapshot = new Vec3(px, py, pz);
        }
        return delaySendDynamic(packet);
    }

    private static boolean delaySendDynamic(Packet<?> packet) {
        ReentrantReadWriteLock.WriteLock writeLock = DYNAMIC_LOCK.writeLock();
        writeLock.lock();
        try {
            long offset = getTransmissionOffsetMs();
            long now = System.currentTimeMillis();
            if (offset > 0L) {
                long accumulated = 0L;
                for (Map.Entry<DelayedPacketEntry, Long> entry : DYNAMIC_PACKETS.entrySet()) {
                    DelayedPacketEntry delayedEntry = entry.getKey();
                    accumulated += offset;
                    if (!delayedEntry.offsetApplied) {
                        entry.setValue(now + accumulated);
                        delayedEntry.offsetApplied = true;
                    }
                }
                if (packet != null) {
                    DYNAMIC_PACKETS.put(new DelayedPacketEntry(packet), now + accumulated + offset);
                    return true;
                }
                return false;
            }
            flushDynamicUnlocked();
            return false;
        } finally {
            writeLock.unlock();
        }
    }

    private static void flushDynamicAll() {
        ReentrantReadWriteLock.WriteLock writeLock = DYNAMIC_LOCK.writeLock();
        writeLock.lock();
        try {
            flushDynamicUnlocked();
        } finally {
            writeLock.unlock();
        }
    }

    private static void flushDynamicUnlocked() {
        Iterator<Map.Entry<DelayedPacketEntry, Long>> iterator = DYNAMIC_PACKETS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<DelayedPacketEntry, Long> entry = iterator.next();
            DelayedPacketEntry key = entry.getKey();
            iterator.remove();
            if (key != null) {
                dispatchPacket(key.packet);
            }
        }
    }

    private static void flushDynamicExpired() {
        ReentrantReadWriteLock.WriteLock writeLock = DYNAMIC_LOCK.writeLock();
        writeLock.lock();
        try {
            if (DYNAMIC_PACKETS.isEmpty()) {
                return;
            }
            long now = System.currentTimeMillis();
            Iterator<Map.Entry<DelayedPacketEntry, Long>> iterator = DYNAMIC_PACKETS.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<DelayedPacketEntry, Long> entry = iterator.next();
                Long scheduled = entry.getValue();
                if (scheduled != null && now < scheduled) {
                    break;
                }
                DelayedPacketEntry key = entry.getKey();
                iterator.remove();
                if (key != null) {
                    dispatchPacket(key.packet);
                }
            }
        } finally {
            writeLock.unlock();
        }
    }

    private static void flushLatency(boolean force) {
        if (!FLUSHING.compareAndSet(false, true)) {
            return;
        }
        try {
            long delay = getConfiguredDelayMs();
            long now = System.currentTimeMillis();
            TimedPacket head;
            while ((head = LATENCY_QUEUE.peek()) != null) {
                if (!force && now - head.timestamp <= delay) {
                    break;
                }
                TimedPacket polled = LATENCY_QUEUE.poll();
                if (polled != null) {
                    dispatchPacket(polled.packet);
                }
            }
        } finally {
            FLUSHING.set(false);
        }
    }

    private static void flushPulse() {
        if (!FLUSHING.compareAndSet(false, true)) {
            return;
        }
        try {
            while (!PULSE_QUEUE.isEmpty()) {
                Packet<?> polled = PULSE_QUEUE.poll();
                if (polled != null) {
                    dispatchPacket(polled);
                }
            }
        } finally {
            FLUSHING.set(false);
        }
    }

    private static void flushRepel() {
        if (REPEL_QUEUE.isEmpty()) {
            return;
        }
        if (!FLUSHING.compareAndSet(false, true)) {
            return;
        }
        try {
            while (!REPEL_QUEUE.isEmpty()) {
                Packet<?> polled = REPEL_QUEUE.poll();
                if (polled != null) {
                    dispatchPacket(polled);
                }
            }
        } finally {
            FLUSHING.set(false);
        }
    }

    public static void dumpPackets() {
        flushAllModes(true);
    }

    private static void flushAllModes(boolean force) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.getConnection() == null) {
            clearAllQueues();
            return;
        }
        flushLatency(force);
        if (force) {
            flushDynamicAll();
            flushRepel();
            flushPulse();
        } else {
            flushDynamicExpired();
        }
    }

    private static void clearAllQueues() {
        LATENCY_QUEUE.clear();
        PULSE_QUEUE.clear();
        REPEL_QUEUE.clear();
        ReentrantReadWriteLock.WriteLock writeLock = DYNAMIC_LOCK.writeLock();
        writeLock.lock();
        try {
            DYNAMIC_PACKETS.clear();
        } finally {
            writeLock.unlock();
        }
    }

    private static void dispatchPacket(Packet<?> packet) {
        if (packet == null) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return;
        }
        ClientPacketListener conn = client.getConnection();
        if (conn == null) {
            return;
        }
        LocalPlayer player = client.player;
        if (packet instanceof ServerboundMovePlayerPacket move && move.hasPosition() && player != null) {
            double sx = move.getX(player.getX());
            double sy = move.getY(player.getY());
            double sz = move.getZ(player.getZ());
            lastServerPosition = new Vec3(sx, sy, sz);
        }
        BYPASSED_PACKETS.add(packet);
        DISPATCHING.set(true);
        try {
            conn.send(packet);
        } catch (Exception ignored) {
        } finally {
            DISPATCHING.set(false);
            BYPASSED_PACKETS.remove(packet);
        }
    }

    public static double rayIntersectionDistance(Vec3 fromPos, Vec3 targetPos) {
        if (fromPos == null || targetPos == null) {
            return 0.0;
        }
        Vec3 eye = new Vec3(fromPos.x, fromPos.y + 1.62, fromPos.z);
        double dx = targetPos.x - fromPos.x;
        double dy = targetPos.y - fromPos.y;
        double dz = targetPos.z - fromPos.z;
        double horiz = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) Math.toDegrees(-Math.atan(dx / dz));
        float pitch = (float) (-Math.toDegrees(Math.atan(dy / horiz)));
        if (dz < 0.0 && dx < 0.0) {
            yaw = (float) (90.0 + Math.toDegrees(Math.atan(dz / dx)));
        } else if (dz < 0.0 && dx > 0.0) {
            yaw = (float) (-90.0 + Math.toDegrees(Math.atan(dz / dx)));
        }
        Vec3 dir = directionFromRotation(yaw, pitch);
        Vec3 rayEnd = eye.add(dir.x * 64.0, dir.y * 64.0, dir.z * 64.0);
        AABB box = new AABB(
            targetPos.x - 0.3,
            targetPos.y,
            targetPos.z - 0.3,
            targetPos.x + 0.3,
            targetPos.y + 1.8,
            targetPos.z + 0.3
        ).inflate(0.1, 0.1, 0.1);
        Optional<Vec3> hit = box.clip(eye, rayEnd);
        if (hit.isPresent()) {
            return eye.distanceTo(hit.get());
        }
        return 0.0;
    }

    private static Vec3 directionFromRotation(float yaw, float pitch) {
        double radYaw = -yaw * (Math.PI / 180.0) - Math.PI;
        double radPitch = -pitch * (Math.PI / 180.0);
        double f3 = Math.cos(radYaw);
        double f4 = Math.sin(radYaw);
        double f5 = -Math.cos(radPitch);
        double f6 = Math.sin(radPitch);
        return new Vec3(f4 * f5, f6, f3 * f5);
    }

    private static Entity findTargetEntity(Minecraft client, float partialTick) {
        if (client == null || client.player == null || client.level == null) {
            return null;
        }
        LocalPlayer player = client.player;
        ClientLevel level = client.level;
        double maxDist = 10.0;
        Vec3 eyePos = player.getEyePosition(partialTick);
        Vec3 lookVec = player.getViewVector(partialTick);
        Vec3 reachVec = eyePos.add(lookVec.x * maxDist, lookVec.y * maxDist, lookVec.z * maxDist);

        BlockHitResult blockHit = level.clip(new ClipContext(
            eyePos,
            reachVec,
            ClipContext.Block.OUTLINE,
            ClipContext.Fluid.NONE,
            player
        ));
        double clipDistSq = maxDist * maxDist;
        if (blockHit != null && blockHit.getType() != HitResult.Type.MISS) {
            Vec3 hitLocation = blockHit.getLocation();
            clipDistSq = eyePos.distanceToSqr(hitLocation);
        }

        AABB searchBox = player.getBoundingBox().expandTowards(lookVec.scale(maxDist)).inflate(1.0, 1.0, 1.0);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
            player,
            eyePos,
            reachVec,
            searchBox,
            e -> isValidCombatEntity(e, player),
            clipDistSq
        );
        if (entityHit != null && entityHit.getEntity() != null) {
            return entityHit.getEntity();
        }

        long now = System.currentTimeMillis();
        if (trackedTargetId != -1 && now - trackerLastAttackTime <= 1200L) {
            Entity recent = level.getEntity(trackedTargetId);
            if (isValidCombatEntity(recent, player) && player.distanceTo(recent) <= 6.5f) {
                return recent;
            }
        }
        return null;
    }

    private static boolean isValidCombatEntity(Entity entity, LocalPlayer self) {
        if (entity == null || entity == self || !(entity instanceof LivingEntity living)) {
            return false;
        }
        if (!living.isAlive() || living.isSpectator()) {
            return false;
        }
        if (TargetFilterManager.isFiltered(entity)) {
            return false;
        }
        if (entity instanceof Player p && FriendManager.isFriend(p)) {
            return false;
        }
        return true;
    }

    private static boolean isExcludedPacket(Packet<?> packet) {
        return packet instanceof ServerboundKeepAlivePacket
            || packet instanceof ServerboundPongPacket
            || packet instanceof ServerboundConfigurationAcknowledgedPacket
            || packet instanceof ServerboundCustomPayloadPacket
            || packet instanceof ServerboundSignUpdatePacket;
    }

    private static boolean isActionOrUiPacket(Packet<?> packet) {
        return packet instanceof ServerboundContainerClickPacket
            || packet instanceof ServerboundChatPacket
            || packet instanceof ServerboundChatCommandPacket
            || packet instanceof ServerboundPlayerActionPacket
            || packet instanceof ServerboundCommandSuggestionPacket;
    }

    private static boolean isPlacementOrUsePacket(Packet<?> packet) {
        return packet instanceof ServerboundUseItemOnPacket
            || packet instanceof ServerboundUseItemPacket;
    }

    private static boolean isSwordItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (stack.is(ItemTags.SWORDS)) {
            return true;
        }
        DataComponentMap components = stack.getComponents();
        if (components != null && components.get(DataComponents.WEAPON) != null) {
            return true;
        }
        String itemName = stack.getItem().toString();
        return itemName != null && itemName.toLowerCase().contains("sword");
    }

    private static boolean isAttackPacket(Packet<?> packet) {
        if (!(packet instanceof ServerboundInteractPacket interactPacket)) {
            return false;
        }
        AttackDetector detector = new AttackDetector();
        interactPacket.dispatch(detector);
        return detector.attack;
    }

    private static int getInteractEntityId(ServerboundInteractPacket interactPacket) {
        return ((ServerboundInteractPacketAccessor) interactPacket).getEntityId();
    }

    private static Entity getAttackedEntity(Packet<?> packet, ClientLevel level) {
        if (!(packet instanceof ServerboundInteractPacket interactPacket) || level == null) {
            return null;
        }
        int id = getInteractEntityId(interactPacket);
        return level.getEntity(id);
    }

    private static String getModeString() {
        Setting modeSetting = getSetting("Mode");
        if (modeSetting != null && modeSetting.getValString() != null) {
            return modeSetting.getValString();
        }
        return "Dynamic";
    }

    private static long getConfiguredDelayMs() {
        Setting randomSetting = getSetting("Randomize (100-200ms)");
        if (randomSetting != null && randomSetting.getValBoolean()) {
            String mode = getModeString();
            if ("Pulse".equalsIgnoreCase(mode)) {
                return currentPulseDelay;
            }
        }
        Setting delaySetting = getSetting("Delay (ms)");
        if (delaySetting != null) {
            return Math.max(1L, (long) delaySetting.getValDouble());
        }
        return 100L;
    }

    private static long getTransmissionOffsetMs() {
        Setting offsetSetting = getSetting("Transmission Offset");
        if (offsetSetting != null) {
            return Math.max(0L, (long) offsetSetting.getValDouble());
        }
        return 5L;
    }

    private static boolean isFlushOnAttackEnabled() {
        Setting flushSetting = getSetting("Flush on Attack");
        return flushSetting == null || flushSetting.getValBoolean();
    }

    private static Setting getSetting(String name) {
        ImnotcheatingyouareClient client = ImnotcheatingyouareClient.INSTANCE;
        if (client == null || client.settingsManager == null || INSTANCE == null) {
            return null;
        }
        return client.settingsManager.getSettingByName(INSTANCE, name);
    }

    public static int getQueuedPacketCount() {
        String mode = getModeString();
        if ("Latency".equalsIgnoreCase(mode)) {
            return LATENCY_QUEUE.size();
        }
        if ("Repel".equalsIgnoreCase(mode)) {
            return REPEL_QUEUE.size();
        }
        if ("Pulse".equalsIgnoreCase(mode)) {
            return PULSE_QUEUE.size();
        }
        ReentrantReadWriteLock.ReadLock readLock = DYNAMIC_LOCK.readLock();
        readLock.lock();
        try {
            return DYNAMIC_PACKETS.size();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public void onEnable() {
        isActive = true;
        clearAllQueues();
        lastDumpTime = System.currentTimeMillis();
        currentPulseDelay = 100L + (long) (Math.random() * 100L);
        repelFlushDeadline = 0L;
        repelTarget = null;
        dynamicTarget = null;
        lastDynamicAttackTime = 0L;
        lastBlockPlaceTime = 0L;
        blockPlaceCooldownUntil = 0L;
        if (mc != null && mc.player != null) {
            Vec3 pos = mc.player.position();
            lastPositionSnapshot = pos;
            lastServerPosition = pos;
        } else {
            lastPositionSnapshot = null;
            lastServerPosition = null;
        }
    }

    @Override
    public void onDisable() {
        isActive = false;
        flushAllModes(true);
        repelTarget = null;
        dynamicTarget = null;
        lastPositionSnapshot = null;
        lastServerPosition = null;
    }

    @Override
    public void onTick() {
        if (mc == null || mc.player == null || mc.getConnection() == null) {
            clearAllQueues();
            return;
        }
        String mode = getModeString();
        if ("Latency".equalsIgnoreCase(mode)) {
            flushLatency(false);
            return;
        }
        if ("Repel".equalsIgnoreCase(mode)) {
            tickRepelMode();
            return;
        }
        if ("Pulse".equalsIgnoreCase(mode)) {
            tickPulseMode();
            return;
        }
        dynamicTarget = findTargetEntity(mc, 1.0F);
        flushDynamicExpired();
    }

    private void tickRepelMode() {
        if (REPEL_QUEUE.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now >= repelFlushDeadline) {
            flushRepel();
            return;
        }
        LivingEntity target = repelTarget;
        if (target != null && target.hurtTime > 0 && target.hurtTime <= getExpectedHurtTimeTicks()) {
            flushRepel();
        }
    }

    private void tickPulseMode() {
        long delay = getConfiguredDelayMs();
        long now = System.currentTimeMillis();
        if (now - lastDumpTime >= delay) {
            flushPulse();
            lastDumpTime = now;
            currentPulseDelay = 100L + (long) (Math.random() * 100L);
        }
    }

    @Override
    public void onRenderHUD(GuiGraphics guiGraphics, Object tickDeltaObj) {
        if (!isToggled() || mc == null || mc.player == null || mc.getConnection() == null) {
            return;
        }
        float partialTick = extractTickDelta(tickDeltaObj);
        String mode = getModeString();
        if ("Dynamic".equalsIgnoreCase(mode)) {
            dynamicTarget = findTargetEntity(mc, partialTick);
            flushDynamicExpired();
        } else if ("Latency".equalsIgnoreCase(mode)) {
            flushLatency(false);
        } else if ("Repel".equalsIgnoreCase(mode)) {
            tickRepelMode();
        }

        Setting vizSetting = getSetting("Visualizer");
        if (vizSetting != null && !vizSetting.getValBoolean()) {
            return;
        }
        if (getQueuedPacketCount() <= 0) {
            return;
        }
        Vec3 serverPos = lastServerPosition;
        if (serverPos == null) {
            return;
        }
        Vec3 clientPos = mc.player.position();
        if (clientPos.distanceToSqr(serverPos) < 0.04) {
            return;
        }
        renderServerGhost(guiGraphics, serverPos, partialTick);
    }

    private void renderServerGhost(GuiGraphics guiGraphics, Vec3 serverPos, float partialTick) {
        float hw = mc.player.getBbWidth() / 2.0f;
        float h = mc.player.getBbHeight();

        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;
        boolean valid = false;

        for (int i = 0; i < 8; i++) {
            double cx = serverPos.x + ((i & 1) == 0 ? -hw : hw);
            double cy = serverPos.y + ((i & 2) == 0 ? 0 : h);
            double cz = serverPos.z + ((i & 4) == 0 ? -hw : hw);

            if (RenderUtils.project2D(cx, cy, cz, partialTick, PROJ_BUFFER[i])) {
                valid = true;
                double px = PROJ_BUFFER[i].x;
                double py = PROJ_BUFFER[i].y;
                if (px < minX) minX = px;
                if (px > maxX) maxX = px;
                if (py < minY) minY = py;
                if (py > maxY) maxY = py;
            }
        }
        if (!valid) {
            return;
        }

        int ix = (int) minX;
        int iy = (int) minY;
        int ix2 = (int) maxX;
        int iy2 = (int) maxY;

        Color themeColor = RenderUtils.getThemeAccentColor();
        int r = themeColor.getRed();
        int g = themeColor.getGreen();
        int b = themeColor.getBlue();
        int ghostFill = new Color(r, g, b, 14).getRGB();
        int ghostOutline = new Color(r, g, b, 42).getRGB();
        int black = new Color(0, 0, 0, 28).getRGB();

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

    private float extractTickDelta(Object tickDeltaObj) {
        if (tickDeltaObj instanceof Float f) {
            return f;
        }
        if (tickDeltaObj instanceof DeltaTracker tracker) {
            return tracker.getGameTimeDeltaPartialTick(true);
        }
        return 1.0f;
    }
}
