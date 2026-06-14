package com.eclipseware.imnotcheatingyouare.client.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class ModuleUtils {
    public static final Minecraft mc = Minecraft.getInstance();

    public static int findItemInHotbar(Item item) {
        if (mc.player == null) return -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.is(item)) return i;
        }
        return -1;
    }

    private static java.lang.reflect.Field selectedField = null;

    private static void setupReflection() {
        if (selectedField != null) return;
        try {
            selectedField = net.minecraft.world.entity.player.Inventory.class.getDeclaredField("selected");
            selectedField.setAccessible(true);
        } catch (Exception ignored) {}
    }

    public static int getSelectedSlot() {
        if (mc.player == null) return 0;
        setupReflection();
        if (selectedField != null) {
            try { return selectedField.getInt(mc.player.getInventory()); } catch (Exception ignored) {}
        }
        return 0; 
    }

    public static void setClientSlot(int slot) {
        if (mc.player == null) return;
        setupReflection();
        if (selectedField != null) {
            try { selectedField.setInt(mc.player.getInventory(), slot); } catch (Exception ignored) {}
        }
    }

    private static java.lang.reflect.Field carriedIndexField = null;

    private static void updateCarriedIndex(int slot) {
        if (mc.gameMode == null) return;
        try {
            if (carriedIndexField == null) {
                for (java.lang.reflect.Field f : net.minecraft.client.multiplayer.MultiPlayerGameMode.class.getDeclaredFields()) {
                    if (f.getType() == int.class) {
                        f.setAccessible(true);
                        if (f.getName().equals("carriedIndex") || f.getName().equals("field_3716") || f.getName().equals("c")) {
                            carriedIndexField = f;
                            break;
                        }
                    }
                }
                if (carriedIndexField == null) {
                    int oldSlot = getSelectedSlot();
                    for (java.lang.reflect.Field f : net.minecraft.client.multiplayer.MultiPlayerGameMode.class.getDeclaredFields()) {
                        if (f.getType() == int.class) {
                            f.setAccessible(true);
                            if (f.getInt(mc.gameMode) == oldSlot) {
                                carriedIndexField = f;
                                break;
                            }
                        }
                    }
                }
            }
            if (carriedIndexField != null) {
                carriedIndexField.setInt(mc.gameMode, slot);
            }
        } catch (Exception ignored) {}
    }

    public static void switchToSlot(int slot) {
        if (mc.player == null) return;
        setupReflection();
        if (selectedField != null) {
            try { selectedField.setInt(mc.player.getInventory(), slot); } catch (Exception ignored) {}
        }
        updateCarriedIndex(slot);
        if (mc.getConnection() != null) {
            mc.getConnection().send(new ServerboundSetCarriedItemPacket(slot));
        }
    }
    
    private static int lastSentSlot = -1;

    public static void setServerSlot(int slot) {
        if (mc.player == null || mc.getConnection() == null) return;
        if (lastSentSlot == slot) return; 
        
        mc.getConnection().send(new ServerboundSetCarriedItemPacket(slot));
        lastSentSlot = slot;
    }
    
    public static void resetServerSlot() {
        lastSentSlot = -1;
    }

    public static float[] getRotations(Vec3 from, Vec3 to) {
        double diffX = to.x - from.x;
        double diffY = to.y - from.y;
        double diffZ = to.z - from.z;
        double distXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);
        
        float yaw = (float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90f;
        float pitch = (float) -Math.toDegrees(Math.atan2(diffY, distXZ));
        return new float[] { yaw, pitch };
    }

    public static void placeBlockPacket(BlockPos pos, Direction face) {
        if (mc.player == null || mc.getConnection() == null) return;
        BlockHitResult hitResult = new BlockHitResult(
            Vec3.atCenterOf(pos), face, pos, false
        );

        boolean rotationSpoof = com.eclipseware.imnotcheatingyouare.client.utils.RotationManager.isActive();
        boolean silentAimSpoof = com.eclipseware.imnotcheatingyouare.client.utils.SilentAimUtil.isActive();

        net.minecraft.client.multiplayer.prediction.BlockStatePredictionHandler handler = getPredictionHandler();
        int seq = 0;
        if (handler != null) {
            handler.startPredicting();
            seq = handler.currentSequence();
        }

        if (rotationSpoof || silentAimSpoof) {
            float yaw = silentAimSpoof ? com.eclipseware.imnotcheatingyouare.client.utils.SilentAimUtil.getYaw() : com.eclipseware.imnotcheatingyouare.client.utils.RotationManager.getServerYaw();
            float pitch = silentAimSpoof ? com.eclipseware.imnotcheatingyouare.client.utils.SilentAimUtil.getPitch() : com.eclipseware.imnotcheatingyouare.client.utils.RotationManager.getServerPitch();
            
            mc.getConnection().send(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Rot(
                yaw, pitch, mc.player.onGround(), false
            ));
            
            mc.player.swing(InteractionHand.MAIN_HAND);
            mc.getConnection().send(new net.minecraft.network.protocol.game.ServerboundUseItemOnPacket(
                InteractionHand.MAIN_HAND, hitResult, seq
            ));
        } else {
            mc.player.swing(InteractionHand.MAIN_HAND);
            mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hitResult);
        }
    }

    public static void useItemPacket() {
        useItemPacket(mc.player.getYRot(), mc.player.getXRot());
    }

    public static void useItemPacket(float yaw, float pitch) {
        if (mc.player == null || mc.getConnection() == null) return;
        ServerboundUseItemPacket packet = new ServerboundUseItemPacket(
            InteractionHand.MAIN_HAND, 0, yaw, pitch
        );
        mc.getConnection().send(packet);
        mc.player.swing(InteractionHand.MAIN_HAND);
    }

    public static void spoofSlot(int fakeSlot) {
        if (mc.getConnection() == null || mc.player == null) return;
        mc.getConnection().send(new ServerboundSetCarriedItemPacket(fakeSlot));
    }

    public static void spoofRestore() {
        if (mc.getConnection() == null || mc.player == null) return;
        int cur = getSelectedSlot();
        mc.getConnection().send(new ServerboundSetCarriedItemPacket(cur));
    }

    public static void spoofPlaceBlockPacket(BlockPos pos, Direction face) {
        if (mc.player == null || mc.getConnection() == null) return;
        BlockHitResult hitResult = new BlockHitResult(
            Vec3.atCenterOf(pos), face, pos, false
        );
        mc.player.swing(InteractionHand.MAIN_HAND);
        mc.getConnection().send(new ServerboundUseItemOnPacket(
            InteractionHand.MAIN_HAND, hitResult, 0
        ));
    }

    private static int silentRevertSlot = -1;

    public static void runSilentSwap(int targetSlot, Runnable action) {
        if (mc.player == null || mc.getConnection() == null) return;
        int originalSlot = getSelectedSlot();
        if (originalSlot == targetSlot) {
            action.run();
            return;
        }

        mc.getConnection().send(new ServerboundSetCarriedItemPacket(targetSlot));
        setClientSlot(targetSlot);
        try {
            action.run();
        } finally {
            setClientSlot(originalSlot);
        }
        silentRevertSlot = originalSlot;
    }

    public static void tickSilentRevert() {
        if (silentRevertSlot != -1 && mc.player != null && mc.getConnection() != null) {
            mc.getConnection().send(new ServerboundSetCarriedItemPacket(silentRevertSlot));
            silentRevertSlot = -1;
        }
    }

    public static boolean isSpoofing = false;

    private static java.lang.reflect.Method getHandlerMethod = null;
    
    private static net.minecraft.client.multiplayer.prediction.BlockStatePredictionHandler getPredictionHandler() {
        if (mc.level == null) return null;
        try {
            if (getHandlerMethod == null) {
                getHandlerMethod = net.minecraft.client.multiplayer.ClientLevel.class.getDeclaredMethod("getBlockStatePredictionHandler");
                getHandlerMethod.setAccessible(true);
            }
            return (net.minecraft.client.multiplayer.prediction.BlockStatePredictionHandler) getHandlerMethod.invoke(mc.level);
        } catch (Exception e) {
            return null;
        }
    }

    public static class PendingPlacement {
        public final BlockPos pos;
        public final Direction face;
        public final int targetSlot;
        public final int originalSlot;
        public final BlockHitResult hitResult;

        public PendingPlacement(BlockPos pos, Direction face, int targetSlot, int originalSlot) {
            this.pos = pos;
            this.face = face;
            this.targetSlot = targetSlot;
            this.originalSlot = originalSlot;
            this.hitResult = new BlockHitResult(Vec3.atCenterOf(pos), face, pos, false);
        }

        public PendingPlacement(BlockHitResult hitResult, int targetSlot, int originalSlot) {
            this.pos = hitResult.getBlockPos();
            this.face = hitResult.getDirection();
            this.targetSlot = targetSlot;
            this.originalSlot = originalSlot;
            this.hitResult = hitResult;
        }
    }

    public static int spoofState = 0; // 0 = idle, 1 = slot swapped, 2 = placing/placed, waiting to revert
    private static PendingPlacement pendingPlacement = null;
    public static int revertSlot = -1;

    public static boolean hasPendingPlacement() {
        return spoofState == 1 || spoofState == 2;
    }

    public static void placeBlockSilent(BlockPos pos, Direction face, int targetSlot) {
        if (spoofState != 0 || mc.player == null || mc.getConnection() == null) return;
        
        int originalSlot = getSelectedSlot();
        pendingPlacement = new PendingPlacement(pos, face, targetSlot, originalSlot);
        
        spoofState = 1;
        revertSlot = originalSlot;
        
        if (originalSlot != targetSlot) {
            mc.getConnection().send(new ServerboundSetCarriedItemPacket(targetSlot));
            setClientSlot(targetSlot);
            updateCarriedIndex(targetSlot);
        }
    }

    public static void placeBlockSilent(BlockHitResult hitResult, int targetSlot) {
        if (spoofState != 0 || mc.player == null || mc.getConnection() == null) return;
        
        int originalSlot = getSelectedSlot();
        pendingPlacement = new PendingPlacement(hitResult, targetSlot, originalSlot);
        
        spoofState = 1;
        revertSlot = originalSlot;
        
        if (originalSlot != targetSlot) {
            mc.getConnection().send(new ServerboundSetCarriedItemPacket(targetSlot));
            setClientSlot(targetSlot);
            updateCarriedIndex(targetSlot);
        }
    }

    public static void processPostMovement() {
        if (spoofState != 2 || pendingPlacement == null || mc.player == null || mc.getConnection() == null) return;
        
        PendingPlacement placement = pendingPlacement;
        pendingPlacement = null; // Clear to prevent loops
        
        mc.player.swing(InteractionHand.MAIN_HAND);
        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, placement.hitResult);
    }

    public static void onClientTickStart() {
        if (mc.player == null || mc.getConnection() == null) {
            spoofState = 0;
            pendingPlacement = null;
            revertSlot = -1;
            return;
        }

        if (spoofState == 1) {
            // Move to state 2, waiting for the movement packet to be sent in this tick
            spoofState = 2;
        } else if (spoofState == 2) {
            // Revert back to original slot in this tick
            if (revertSlot != -1) {
                int current = getSelectedSlot();
                if (current != revertSlot) {
                    mc.getConnection().send(new ServerboundSetCarriedItemPacket(revertSlot));
                    setClientSlot(revertSlot);
                    updateCarriedIndex(revertSlot);
                }
                revertSlot = -1;
            }
            spoofState = 0;
            pendingPlacement = null;
        }
    }

    public static void onClientTickEnd() {
        if (spoofState == 2 && pendingPlacement != null && mc.player != null && mc.getConnection() != null) {
            // No movement packet was sent during tick N+1 (player was still),
            // so we manually send a Rot packet followed by the placement packets.
            boolean rotationSpoof = com.eclipseware.imnotcheatingyouare.client.utils.RotationManager.isActive();
            boolean silentAimSpoof = com.eclipseware.imnotcheatingyouare.client.utils.SilentAimUtil.isActive();

            float yaw, pitch;
            if (rotationSpoof || silentAimSpoof) {
                yaw = silentAimSpoof ? com.eclipseware.imnotcheatingyouare.client.utils.SilentAimUtil.getYaw() : com.eclipseware.imnotcheatingyouare.client.utils.RotationManager.getServerYaw();
                pitch = silentAimSpoof ? com.eclipseware.imnotcheatingyouare.client.utils.SilentAimUtil.getPitch() : com.eclipseware.imnotcheatingyouare.client.utils.RotationManager.getServerPitch();
            } else {
                float[] rots = getRotations(mc.player.getEyePosition(), pendingPlacement.hitResult.getLocation());
                yaw = rots[0];
                pitch = rots[1];
            }

            float finalYaw = yaw;
            float finalPitch = pitch;
            float lastYaw = Float.isNaN(com.eclipseware.imnotcheatingyouare.client.utils.RotationManager.lastSentYaw) ? mc.player.getYRot() : com.eclipseware.imnotcheatingyouare.client.utils.RotationManager.lastSentYaw;
            float lastPitch = Float.isNaN(com.eclipseware.imnotcheatingyouare.client.utils.RotationManager.lastSentPitch) ? mc.player.getXRot() : com.eclipseware.imnotcheatingyouare.client.utils.RotationManager.lastSentPitch;
            
            float gcd = com.eclipseware.imnotcheatingyouare.client.utils.RotationManager.getGCD();
            if (gcd < 0.001f) gcd = 0.15f;
            
            float yawDiff = net.minecraft.util.Mth.wrapDegrees(yaw - lastYaw);
            float pitchDiff = pitch - lastPitch;
            
            int yawSteps = Math.round(yawDiff / gcd);
            int pitchSteps = Math.round(pitchDiff / gcd);
            
            finalYaw = lastYaw + yawSteps * gcd;
            finalPitch = lastPitch + pitchSteps * gcd;
            finalPitch = net.minecraft.util.Mth.clamp(finalPitch, -90.0f, 90.0f);

            isSpoofing = true;
            mc.getConnection().send(new ServerboundMovePlayerPacket.Rot(
                finalYaw, finalPitch, mc.player.onGround(), false
            ));
            isSpoofing = false;

            com.eclipseware.imnotcheatingyouare.client.utils.RotationManager.lastSentYaw = finalYaw;
            com.eclipseware.imnotcheatingyouare.client.utils.RotationManager.lastSentPitch = finalPitch;

            processPostMovement();
        }
    }
}