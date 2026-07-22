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
        return mc.player.getInventory().getSelectedSlot();
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
        setServerSlot(slot);
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

        net.minecraft.client.multiplayer.prediction.BlockStatePredictionHandler handler = getPredictionHandler();
        if (handler != null) {
            handler.startPredicting();
        }

        mc.player.swing(InteractionHand.MAIN_HAND);
        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hitResult);
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

    public static int spoofState = 0; 
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
    }

    public static void placeBlockSilent(BlockHitResult hitResult, int targetSlot) {
        if (spoofState != 0 || mc.player == null || mc.getConnection() == null) return;
        
        int originalSlot = getSelectedSlot();
        pendingPlacement = new PendingPlacement(hitResult, targetSlot, originalSlot);
        
        spoofState = 1;
        revertSlot = originalSlot;
    }

    public static void processPostMovement() {
        if (spoofState != 2 || pendingPlacement == null || mc.player == null || mc.getConnection() == null) return;
        
        PendingPlacement placement = pendingPlacement;
        pendingPlacement = null; 
        
        int curSlot = getSelectedSlot();
        boolean slotChanged = curSlot != placement.targetSlot;
        if (slotChanged) {
            setServerSlot(placement.targetSlot);
            setClientSlot(placement.targetSlot);
            updateCarriedIndex(placement.targetSlot);
        }

        mc.player.swing(InteractionHand.MAIN_HAND);
        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, placement.hitResult);

        if (revertSlot != -1 && revertSlot != placement.targetSlot) {
            setClientSlot(revertSlot);
            updateCarriedIndex(revertSlot);
            revertSlot = -1;
        }
        spoofState = 0;
    }

    public static void onClientTickStart() {
        if (mc.player == null || mc.getConnection() == null) {
            spoofState = 0;
            pendingPlacement = null;
            revertSlot = -1;
            return;
        }

        if (spoofState == 1) {
            spoofState = 2;
        } else if (spoofState == 2) {
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
        if (spoofState != 0) {
            spoofState = 0;
            pendingPlacement = null;
        }
    }

    public static int getCrystalSlot() {
        return findItemInHotbar(net.minecraft.world.item.Items.END_CRYSTAL);
    }

    public static int getObsidianSlot() {
        return findItemInHotbar(net.minecraft.world.item.Items.OBSIDIAN);
    }

    public static boolean isHoldingWeapon(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        String id = stack.getItem().getDescriptionId().toLowerCase();
        return id.contains("sword") || id.contains("axe") || id.contains("mace");
    }

    public static boolean isHoldingCrystal(net.minecraft.world.entity.player.Player player) {
        if (player == null) return false;
        return player.getMainHandItem().is(net.minecraft.world.item.Items.END_CRYSTAL) ||
               player.getOffhandItem().is(net.minecraft.world.item.Items.END_CRYSTAL);
    }
}