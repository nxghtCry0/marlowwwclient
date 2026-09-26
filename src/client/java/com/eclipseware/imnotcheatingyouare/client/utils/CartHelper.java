package com.eclipseware.imnotcheatingyouare.client.utils;

import com.eclipseware.imnotcheatingyouare.client.module.impl.AntiBot;
import com.eclipseware.imnotcheatingyouare.client.module.impl.Teams;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class CartHelper {
    private static final Minecraft mc = Minecraft.getInstance();

    private CartHelper() {
    }

    public static int railSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack s = mc.player.getInventory().getItem(i);
            if (s.is(Items.RAIL) || s.is(Items.POWERED_RAIL) || s.is(Items.DETECTOR_RAIL) || s.is(Items.ACTIVATOR_RAIL)) return i;
        }
        return -1;
    }

    public static int slot(Item item) {
        return ModuleUtils.findItemInHotbar(item);
    }

    public static boolean isRail(BlockPos pos) {
        return mc.level.getBlockState(pos).getBlock() instanceof BaseRailBlock;
    }

    public static boolean canHoldRail(BlockPos ground) {
        BlockState state = mc.level.getBlockState(ground);
        if (state.isAir() || state.getBlock() instanceof BaseRailBlock) return false;
        if (!state.isFaceSturdy(mc.level, ground, Direction.UP)) return false;
        BlockState above = mc.level.getBlockState(ground.above());
        return above.canBeReplaced() || above.getBlock() instanceof BaseRailBlock;
    }

    public static boolean inReach(BlockPos ground) {
        Vec3 top = new Vec3(ground.getX() + 0.5, ground.getY() + 1.0, ground.getZ() + 0.5);
        return mc.player.getEyePosition().distanceTo(top) <= mc.player.blockInteractionRange();
    }

    public static Player nearestEnemy(double range) {
        Player best = null;
        double bestDist = range * range;
        for (Player p : mc.level.players()) {
            if (p == mc.player || !p.isAlive() || p.isSpectator()) continue;
            if (FriendManager.isFriend(p) || TargetFilterManager.isFiltered(p) || AntiBot.isBot(p) || Teams.isTeam(p)) continue;
            double d = mc.player.distanceToSqr(p);
            if (d < bestDist) {
                bestDist = d;
                best = p;
            }
        }
        return best;
    }

    public static BlockPos findSpot(String mode, double targetRange) {
        if (!mode.equals("Target") && mc.hitResult instanceof BlockHitResult bhr && mc.hitResult.getType() == HitResult.Type.BLOCK) {
            BlockPos hit = bhr.getBlockPos();
            if (isRail(hit)) return hit.below();
            if (bhr.getDirection() == Direction.UP && canHoldRail(hit) && inReach(hit)) return hit;
            BlockPos below = hit.relative(bhr.getDirection()).below();
            if (canHoldRail(below) && inReach(below)) return below;
            if (mode.equals("Crosshair")) return null;
        }
        Player target = nearestEnemy(targetRange);
        if (target == null) return null;
        BlockPos feet = target.blockPosition();
        BlockPos best = null;
        double bestScore = Double.MAX_VALUE;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dy = -2; dy <= 0; dy++) {
                    BlockPos ground = feet.offset(dx, dy, dz);
                    if (!canHoldRail(ground) || !inReach(ground)) continue;
                    double score = target.position().distanceToSqr(Vec3.atBottomCenterOf(ground.above())) * 2 + mc.player.distanceToSqr(Vec3.atCenterOf(ground));
                    if (score < bestScore) {
                        bestScore = score;
                        best = ground;
                    }
                }
            }
        }
        return best;
    }

    public static Vec3 railAim(BlockPos ground) {
        return new Vec3(ground.getX() + 0.5, ground.getY() + 1.0, ground.getZ() + 0.5);
    }

    public static void aim(Vec3 point) {
        float[] rots = ModuleUtils.getRotations(mc.player.getEyePosition(), point);
        RotationManager.keepRotated(rots[0], rots[1], 180f, true);
    }

    public static boolean useOn(int slot, BlockHitResult hit) {
        if (slot < 0) return false;
        InteractionHand hand = InteractionHand.MAIN_HAND;
        ModuleUtils.switchToSlot(slot);
        InteractionResult result = mc.gameMode.useItemOn(mc.player, hand, hit);
        if (result.consumesAction()) mc.player.swing(hand, net.minecraft.world.item.component.SwingAnimation.DEFAULT, true);
        return result.consumesAction();
    }

    public static boolean placeRail(BlockPos ground) {
        if (isRail(ground.above())) return true;
        return useOn(railSlot(), new BlockHitResult(railAim(ground), Direction.UP, ground, false));
    }

    public static boolean placeCart(BlockPos ground) {
        BlockPos rail = ground.above();
        Vec3 point = new Vec3(rail.getX() + 0.5, rail.getY() + 0.0625, rail.getZ() + 0.5);
        return useOn(slot(Items.TNT_MINECART), new BlockHitResult(point, Direction.UP, rail, false));
    }
}
