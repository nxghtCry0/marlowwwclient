package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

import java.util.concurrent.ConcurrentLinkedQueue;

public class FakeLag extends Module {
    private static final ConcurrentLinkedQueue<Packet<?>> PACKET_QUEUE = new ConcurrentLinkedQueue<>();
    private static volatile boolean isActive = false;
    private long lastDumpTime = 0;
    private long currentDelay = 150L;

    public FakeLag() {
        super("FakeLag", Category.Movement);
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Delay (ms)", this, 150.0, 50.0, 500.0, true));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Randomize (100-200ms)", this, true));
    }

    public static void queuePacket(Packet<?> packet) {
        if (!isActive) return;
        if (packet instanceof ServerboundMovePlayerPacket) {
            PACKET_QUEUE.offer(packet);
        }
    }

    public static boolean isActive() {
        return isActive;
    }

    public static void dumpPackets() {
        if (mc.getConnection() == null) {
            PACKET_QUEUE.clear();
            return;
        }
        boolean wasActive = isActive;
        isActive = false;
        while (!PACKET_QUEUE.isEmpty()) {
            Packet<?> packet = PACKET_QUEUE.poll();
            if (packet != null) {
                try {
                    mc.getConnection().send(packet);
                } catch (Exception ignored) {}
            }
        }
        isActive = wasActive;
    }

    @Override
    public void onEnable() {
        isActive = true;
        PACKET_QUEUE.clear();
        lastDumpTime = System.currentTimeMillis();
        currentDelay = 100L + (long)(Math.random() * 100L);
    }

    @Override
    public void onDisable() {
        isActive = false;
        dumpPackets();
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.getConnection() == null) {
            PACKET_QUEUE.clear();
            return;
        }

        Setting randomSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Randomize (100-200ms)");
        boolean randomize = randomSetting == null || randomSetting.getValBoolean();

        long delay;
        if (randomize) {
            delay = currentDelay;
        } else {
            Setting delaySetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Delay (ms)");
            delay = delaySetting != null ? (long) delaySetting.getValDouble() : 150L;
        }

        if (System.currentTimeMillis() - lastDumpTime >= delay) {
            dumpPackets();
            lastDumpTime = System.currentTimeMillis();
            currentDelay = 100L + (long)(Math.random() * 100L);
        }
    }
}
