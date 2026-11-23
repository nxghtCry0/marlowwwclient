package com.eclipseware.imnotcheatingyouare.client.utils;

import com.eclipseware.imnotcheatingyouare.mixin.client.InventoryAccessor;
import com.eclipseware.imnotcheatingyouare.mixin.client.MultiPlayerGameModeAccessor;
import java.lang.reflect.Method;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.prediction.BlockStatePredictionHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class ModuleUtils {
    public static final Minecraft mc = Minecraft.getInstance();

    private static int lastSentSlot = -1;
    private static int silentRevertSlot = -1;
    public static boolean isSpoofing = false;
    private static Method getHandlerMethod = null;
    public static int spoofState = 0;
    private static PendingPlacement pendingPlacement = null;
    public static int revertSlot = -1;

    public static int findItemInHotbar(Item item) {
        if (mc.player == null) {
            return -1;
        }
        Inventory inv = mc.player.getInventory();
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.is(item)) {
                return i;
            }
        }
        return -1;
    }

    public static int getSelectedSlot() {
        if (mc.player == null) {
            return 0;
        }
        InventoryAccessor inv = (InventoryAccessor) mc.player.getInventory();
        return inv.getSelected();
    }

    public static void setClientSlot(int slot) {
        if (mc.player == null) {
            return;
        }
        InventoryAccessor inv = (InventoryAccessor) mc.player.getInventory();
        inv.setSelected(slot);
    }

    public static void switchToSlot(int slot) {
        if (mc.player == null) {
            return;
        }
        setClientSlot(slot);
        setServerSlot(slot);
    }

    public static void setServerSlot(int slot) {
        ClientPacketListener conn = mc.getConnection();
        if (mc.player == null || conn == null) {
            return;
        }
        if (mc.gameMode != null) {
            MultiPlayerGameModeAccessor gm = (MultiPlayerGameModeAccessor) mc.gameMode;
            if (gm.getCarriedIndex() == slot) {
                lastSentSlot = slot;
                return;
            }
            conn.send(new ServerboundSetCarriedItemPacket(slot));
            gm.setCarriedIndex(slot);
            lastSentSlot = slot;
            return;
        }
        if (lastSentSlot == slot) {
            return;
        }
        conn.send(new ServerboundSetCarriedItemPacket(slot));
        lastSentSlot = slot;
    }

    public static void swingHand(Player player, InteractionHand hand) {
        if (player == null || hand == null) {
            return;
        }
        player.swing(hand, SwingAnimation.DEFAULT, false);
    }

    public static void attackSwing() {
        swingHand(mc.player, InteractionHand.MAIN_HAND);
    }

    public static void resetServerSlot() {
        lastSentSlot = -1;
    }

    public static float[] getRotations(Vec3 from, Vec3 to) {
        double diffX = to.x - from.x;
        double diffY = to.y - from.y;
        double diffZ = to.z - from.z;
        double distXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);

        float yaw = (float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0f;
        float pitch = (float) -Math.toDegrees(Math.atan2(diffY, distXZ));
        return new float[]{yaw, pitch};
    }

    public static void placeBlockPacket(BlockPos pos, Direction face) {
        if (mc.player == null || mc.gameMode == null || mc.getConnection() == null) {
            return;
        }
        BlockHitResult hitResult = new BlockHitResult(Vec3.atCenterOf(pos), face, pos, false);
        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hitResult);
        swingHand(mc.player, InteractionHand.MAIN_HAND);
    }

    public static void useItemPacket() {
        if (mc.player == null) {
            return;
        }
        useItemPacket(mc.player.getYRot(), mc.player.getXRot());
    }

    public static void useItemPacket(float yaw, float pitch) {
        ClientPacketListener conn = mc.getConnection();
        if (mc.player == null || conn == null) {
            return;
        }
        ServerboundUseItemPacket packet = new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, 0, yaw, pitch);
        conn.send(packet);
        swingHand(mc.player, InteractionHand.MAIN_HAND);
    }

    public static void spoofSlot(int fakeSlot) {
        ClientPacketListener conn = mc.getConnection();
        if (conn == null || mc.player == null) {
            return;
        }
        conn.send(new ServerboundSetCarriedItemPacket(fakeSlot));
    }

    public static void spoofRestore() {
        ClientPacketListener conn = mc.getConnection();
        if (conn == null || mc.player == null) {
            return;
        }
        int cur = getSelectedSlot();
        conn.send(new ServerboundSetCarriedItemPacket(cur));
    }

    public static void spoofPlaceBlockPacket(BlockPos pos, Direction face) {
        if (mc.player == null || mc.gameMode == null || mc.getConnection() == null) {
            return;
        }
        BlockHitResult hitResult = new BlockHitResult(Vec3.atCenterOf(pos), face, pos, false);
        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hitResult);
        swingHand(mc.player, InteractionHand.MAIN_HAND);
    }

    public static void runSilentSwap(int targetSlot, Runnable action) {
        ClientPacketListener conn = mc.getConnection();
        if (mc.player == null || conn == null) {
            return;
        }
        int originalSlot = getSelectedSlot();
        if (originalSlot == targetSlot) {
            action.run();
            return;
        }

        conn.send(new ServerboundSetCarriedItemPacket(targetSlot));
        setClientSlot(targetSlot);
        try {
            action.run();
        } finally {
            setClientSlot(originalSlot);
        }
        silentRevertSlot = originalSlot;
    }

    public static void tickSilentRevert() {
        ClientPacketListener conn = mc.getConnection();
        if (silentRevertSlot != -1 && mc.player != null && conn != null) {
            conn.send(new ServerboundSetCarriedItemPacket(silentRevertSlot));
            silentRevertSlot = -1;
        }
    }

    private static BlockStatePredictionHandler getPredictionHandler() {
        if (mc.level == null) {
            return null;
        }
        try {
            if (getHandlerMethod == null) {
                getHandlerMethod = ClientLevel.class.getDeclaredMethod("getBlockStatePredictionHandler");
                getHandlerMethod.setAccessible(true);
            }
            return (BlockStatePredictionHandler) getHandlerMethod.invoke(mc.level);
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

    public static boolean hasPendingPlacement() {
        return spoofState == 1 || spoofState == 2;
    }

    public static void placeBlockSilent(BlockPos pos, Direction face, int targetSlot) {
        if (spoofState != 0 || mc.player == null || mc.getConnection() == null) {
            return;
        }
        int originalSlot = getSelectedSlot();
        pendingPlacement = new PendingPlacement(pos, face, targetSlot, originalSlot);
        spoofState = 1;
        revertSlot = originalSlot;
    }

    public static void placeBlockSilent(BlockHitResult hitResult, int targetSlot) {
        if (spoofState != 0 || mc.player == null || mc.getConnection() == null) {
            return;
        }
        int originalSlot = getSelectedSlot();
        pendingPlacement = new PendingPlacement(hitResult, targetSlot, originalSlot);
        spoofState = 1;
        revertSlot = originalSlot;
    }

    public static void processPostMovement() {
        if (spoofState != 2 || pendingPlacement == null || mc.player == null || mc.gameMode == null || mc.getConnection() == null) {
            return;
        }

        PendingPlacement placement = pendingPlacement;
        pendingPlacement = null;

        int curSlot = getSelectedSlot();
        if (curSlot != placement.targetSlot) {
            setServerSlot(placement.targetSlot);
            setClientSlot(placement.targetSlot);
        }

        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, placement.hitResult);
        swingHand(mc.player, InteractionHand.MAIN_HAND);

        if (revertSlot != -1 && revertSlot != placement.targetSlot) {
            setClientSlot(revertSlot);
            revertSlot = -1;
        }
        spoofState = 0;
    }

    public static void onClientTickStart() {
        ClientPacketListener conn = mc.getConnection();
        if (mc.player == null || conn == null) {
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
                    conn.send(new ServerboundSetCarriedItemPacket(revertSlot));
                    setClientSlot(revertSlot);
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
        return findItemInHotbar(Items.END_CRYSTAL);
    }

    public static int getObsidianSlot() {
        return findItemInHotbar(Items.OBSIDIAN);
    }

    public static boolean isHoldingWeapon(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        Item item = stack.getItem();
        String id = item.getDescriptionId().toLowerCase();
        return id.contains("sword") || id.contains("axe") || id.contains("mace");
    }

    public static boolean isHoldingCrystal(Player player) {
        if (player == null) {
            return false;
        }
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        return main.is(Items.END_CRYSTAL) || off.is(Items.END_CRYSTAL);
    }
}
