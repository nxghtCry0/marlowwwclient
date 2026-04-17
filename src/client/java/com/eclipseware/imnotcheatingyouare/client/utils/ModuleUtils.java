package com.eclipseware.imnotcheatingyouare.client.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
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

    private static int lastSentSlot = -1;

    public static void switchToSlot(int slot) {
        if (mc.player == null) return;
        mc.player.getInventory().setSelectedSlot(slot);
        setServerSlot(slot);
    }
    
    public static void setServerSlot(int slot) {
        if (mc.player == null || mc.getConnection() == null) return;
        if (lastSentSlot == slot) return; // Prevent duplicate packet bursts!
        
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
        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hitResult);
        mc.player.swing(InteractionHand.MAIN_HAND);
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
}