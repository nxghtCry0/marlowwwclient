package com.eclipseware.imnotcheatingyouare.client.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class RotationManager {
    private enum Phase { IDLE, SMOOTHING, HOLDING, RETURNING }

    private static Phase phase = Phase.IDLE;
    private static float fromYaw, fromPitch;
    private static float targetYaw, targetPitch;
    private static int smoothTicks, holdTicks, returnTicks;
    private static int currentTick;
    private static Runnable onReachTarget;
    private static boolean movementCorrection;
    private static float currentServerYaw;
    private static boolean sendingPacket = false;

    public static void queueRotation(float yaw, float pitch, int smooth, int hold, int ret, boolean moveCorrect, Runnable onReach) {
        cancel();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        fromYaw = mc.player.getYRot();
        fromPitch = mc.player.getXRot();
        targetYaw = Mth.wrapDegrees(yaw);
        targetPitch = Mth.clamp(pitch, -90, 90);
        smoothTicks = Math.max(1, smooth);
        holdTicks = hold;
        returnTicks = Math.max(1, ret);
        currentTick = 0;
        phase = Phase.SMOOTHING;
        movementCorrection = moveCorrect;
        onReachTarget = onReach;
        currentServerYaw = fromYaw;
    }

    public static void tick() {
        if (phase == Phase.IDLE) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) {
            cancel();
            return;
        }

        currentTick++;

        switch (phase) {
            case SMOOTHING -> {
                float progress = AnimationUtil.easeInOutCubic(Math.min(1f, (float) currentTick / smoothTicks));
                float yaw = fromYaw + Mth.wrapDegrees(targetYaw - fromYaw) * progress;
                float pitch = fromPitch + (targetPitch - fromPitch) * progress;
                currentServerYaw = yaw;
                sendRotation(yaw, pitch);

                if (currentTick >= smoothTicks) {
                    currentServerYaw = targetYaw;
                    sendRotation(targetYaw, targetPitch);
                    phase = Phase.HOLDING;
                    currentTick = 0;
                    if (onReachTarget != null) {
                        onReachTarget.run();
                        onReachTarget = null;
                    }
                }
            }
            case HOLDING -> {
                sendRotation(targetYaw, targetPitch);
                if (currentTick >= holdTicks) {
                    fromYaw = targetYaw;
                    fromPitch = targetPitch;
                    phase = Phase.RETURNING;
                    currentTick = 0;
                }
            }
            case RETURNING -> {
                float actualYaw = mc.player.getYRot();
                float actualPitch = mc.player.getXRot();
                float progress = AnimationUtil.easeOutCubic(Math.min(1f, (float) currentTick / returnTicks));
                float yaw = fromYaw + Mth.wrapDegrees(actualYaw - fromYaw) * progress;
                float pitch = fromPitch + (actualPitch - fromPitch) * progress;
                currentServerYaw = yaw;
                sendRotation(yaw, pitch);

                if (currentTick >= returnTicks) {
                    phase = Phase.IDLE;
                    movementCorrection = false;
                }
            }
        }
    }

    private static void sendRotation(float yaw, float pitch) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() != null && mc.player != null) {
            sendingPacket = true;
            mc.getConnection().send(new ServerboundMovePlayerPacket.Rot(yaw, pitch, mc.player.onGround(), false));
            sendingPacket = false;
        }
    }

    public static void cancel() {
        phase = Phase.IDLE;
        onReachTarget = null;
        movementCorrection = false;
    }

    public static boolean isActive() { return phase != Phase.IDLE; }
    public static boolean isAtTarget() { return phase == Phase.HOLDING; }
    public static boolean isMovementCorrection() { return isActive() && movementCorrection; }
    public static boolean isSendingPacket() { return sendingPacket; }

    public static float getYawDelta() {
        if (!isActive()) return 0f;
        return Mth.wrapDegrees(currentServerYaw - Minecraft.getInstance().player.getYRot());
    }

    public static boolean hasLineOfSight(Vec3 from, Vec3 to) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return false;
        ClipContext context = new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player);
        BlockHitResult result = mc.level.clip(context);
        return result.getType() == HitResult.Type.MISS;
    }
}