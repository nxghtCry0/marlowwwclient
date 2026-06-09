package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.utils.ModuleUtils;
import com.eclipseware.imnotcheatingyouare.client.utils.RotationManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class AnchorMacro extends Module {
    private Setting delaySetting;
    private Setting safeAnchor;
    private Setting silentAim;
    private Setting autoDetonate;

    private int step = 0;
    private int ticksWait = 0;
    private BlockPos targetAnchorPos = null;
    private int previousSlot = 0;

    public AnchorMacro() {
        super("AnchorMacro", Category.Crystal, "Automatically places, charges, and detonates respawn anchors.");
        setSubCategory("Semi-Blatant");

        delaySetting = new Setting("Delay Ticks", this, 1.0, 0.0, 5.0, true);
        safeAnchor = new Setting("Safe Anchor", this, true);
        silentAim = new Setting("Silent Aim", this, true);
        autoDetonate = new Setting("Auto Detonate", this, true);

        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(delaySetting);
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(safeAnchor);
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(silentAim);
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(autoDetonate);
    }

    @Override
    public void onEnable() {
        step = 0;
        ticksWait = 0;
        targetAnchorPos = null;
        previousSlot = ModuleUtils.getSelectedSlot();
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.level == null) return;

        for (int iteration = 0; iteration < 4; iteration++) {
            if (ticksWait > 0) {
                ticksWait--;
                break;
            }

            if (step == 0) {
                net.minecraft.world.phys.HitResult hitResult = mc.hitResult;
                if (hitResult == null || hitResult.getType() != net.minecraft.world.phys.HitResult.Type.BLOCK) break;

                BlockHitResult bhr = (BlockHitResult) hitResult;
                BlockPos lookingAt = bhr.getBlockPos();
                BlockState state = mc.level.getBlockState(lookingAt);

                if (!state.is(Blocks.RESPAWN_ANCHOR)) break;

                int charges = state.hasProperty(BlockStateProperties.RESPAWN_ANCHOR_CHARGES)
                        ? state.getValue(BlockStateProperties.RESPAWN_ANCHOR_CHARGES)
                        : 0;

                targetAnchorPos = lookingAt;
                previousSlot = ModuleUtils.getSelectedSlot();

                if (charges > 0) {
                    if (safeAnchor.getValBoolean()) {
                        step = 2;
                    } else {
                        step = 3;
                    }
                } else {
                    step = 1;
                }

                ticksWait = (int) delaySetting.getValDouble();
                if (ticksWait > 0) break;
                continue;
            }

            int delay = (int) delaySetting.getValDouble();

            if (step == 1) {
                BlockState anchorState = mc.level.getBlockState(targetAnchorPos);

                if (!anchorState.is(Blocks.RESPAWN_ANCHOR)) {
                    reset();
                    break;
                }

                int charges = anchorState.hasProperty(BlockStateProperties.RESPAWN_ANCHOR_CHARGES)
                        ? anchorState.getValue(BlockStateProperties.RESPAWN_ANCHOR_CHARGES)
                        : 0;

                if (charges > 0) {
                    if (safeAnchor.getValBoolean()) {
                        step = 2;
                    } else {
                        step = 3;
                    }
                    ticksWait = delay;
                    if (ticksWait > 0) break;
                    continue;
                }

                int glowstoneSlot = ModuleUtils.findItemInHotbar(Items.GLOWSTONE);
                if (glowstoneSlot == -1) {
                    reset();
                    break;
                }

                ModuleUtils.switchToSlot(glowstoneSlot);
                if (silentAim.getValBoolean()) aimAt(targetAnchorPos);

                BlockHitResult hit = new BlockHitResult(
                        Vec3.atCenterOf(targetAnchorPos), Direction.UP, targetAnchorPos, false);
                mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
                mc.player.swing(InteractionHand.MAIN_HAND);

                if (safeAnchor.getValBoolean()) {
                    step = 2;
                } else {
                    step = 3;
                }
                ticksWait = delay;
                if (ticksWait > 0) break;
                continue;
            }

            if (step == 2) {
                BlockPos shieldPos = getShieldPos();
                if (shieldPos == null) {
                    step = 3;
                    ticksWait = delay;
                    if (ticksWait > 0) break;
                    continue;
                }

                BlockState shieldState = mc.level.getBlockState(shieldPos);
                if (!shieldState.isAir() && !shieldState.canBeReplaced()) {
                    step = 3;
                    ticksWait = delay;
                    if (ticksWait > 0) break;
                    continue;
                }

                int shieldSlot = findShieldBlockSlot();
                if (shieldSlot == -1) {
                    step = 3;
                    ticksWait = delay;
                    if (ticksWait > 0) break;
                    continue;
                }

                ModuleUtils.switchToSlot(shieldSlot);
                if (silentAim.getValBoolean()) aimAt(shieldPos);

                BlockPos supportBlock = findSupportForShield(shieldPos);
                Direction supportFace = getPlaceFace(supportBlock, shieldPos);

                BlockHitResult hit = new BlockHitResult(
                        Vec3.atCenterOf(supportBlock), supportFace, supportBlock, false);
                mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
                mc.player.swing(InteractionHand.MAIN_HAND);

                step = 3;
                ticksWait = delay;
                if (ticksWait > 0) break;
                continue;
            }

            if (step == 3) {
                if (!autoDetonate.getValBoolean()) {
                    restoreSlot();
                    finish();
                    break;
                }

                BlockState anchorState = mc.level.getBlockState(targetAnchorPos);
                if (!anchorState.is(Blocks.RESPAWN_ANCHOR)) {
                    restoreSlot();
                    finish();
                    break;
                }

                int charges = anchorState.hasProperty(BlockStateProperties.RESPAWN_ANCHOR_CHARGES)
                        ? anchorState.getValue(BlockStateProperties.RESPAWN_ANCHOR_CHARGES)
                        : 0;

                if (charges <= 0) break;

                int detonateSlot = findNonAnchorNonGlowstoneSlot();
                if (detonateSlot != -1) {
                    ModuleUtils.switchToSlot(detonateSlot);
                }

                if (silentAim.getValBoolean()) aimAt(targetAnchorPos);

                BlockHitResult hit = new BlockHitResult(
                        Vec3.atCenterOf(targetAnchorPos), Direction.UP, targetAnchorPos, false);
                mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
                mc.player.swing(InteractionHand.MAIN_HAND);

                restoreSlot();
                finish();
                break;
            }
        }
    }

    private BlockPos getShieldPos() {
        if (mc.player == null || targetAnchorPos == null) return null;
        Direction dir = getDirectionFromAnchorToPlayer();
        BlockPos shieldPos = targetAnchorPos.relative(dir);

        if (intersectsPlayer(shieldPos)) {
            // Fallback to dominant horizontal direction
            double dx = mc.player.getX() - (targetAnchorPos.getX() + 0.5);
            double dz = mc.player.getZ() - (targetAnchorPos.getZ() + 0.5);
            Direction horizontalDir = Math.abs(dx) > Math.abs(dz)
                    ? (dx > 0 ? Direction.EAST : Direction.WEST)
                    : (dz > 0 ? Direction.SOUTH : Direction.NORTH);
            shieldPos = targetAnchorPos.relative(horizontalDir);
            if (intersectsPlayer(shieldPos)) {
                return null; // Don't place if it still intersects the player
            }
        }
        return shieldPos;
    }

    private boolean intersectsPlayer(BlockPos pos) {
        if (mc.player == null) return false;
        net.minecraft.world.phys.AABB playerBox = mc.player.getBoundingBox();
        net.minecraft.world.phys.AABB blockBox = new net.minecraft.world.phys.AABB(pos);
        return playerBox.intersects(blockBox);
    }

    private BlockPos findSupportForShield(BlockPos shieldPos) {
        BlockPos below = shieldPos.below();
        if (!mc.level.getBlockState(below).isAir()) return below;

        for (Direction dir : Direction.values()) {
            BlockPos neighbor = shieldPos.relative(dir);
            if (neighbor.equals(targetAnchorPos)) continue;
            if (!mc.level.getBlockState(neighbor).isAir()) return neighbor;
        }

        return below;
    }

    private Direction getPlaceFace(BlockPos supportBlock, BlockPos shieldPos) {
        int dx = shieldPos.getX() - supportBlock.getX();
        int dy = shieldPos.getY() - supportBlock.getY();
        int dz = shieldPos.getZ() - supportBlock.getZ();

        if (dy == 1) return Direction.UP;
        if (dy == -1) return Direction.DOWN;
        if (dx == 1) return Direction.EAST;
        if (dx == -1) return Direction.WEST;
        if (dz == 1) return Direction.SOUTH;
        if (dz == -1) return Direction.NORTH;

        return Direction.UP;
    }

    private Direction getDirectionFromAnchorToPlayer() {
        if (mc.player == null || targetAnchorPos == null) return Direction.NORTH;

        double dx = mc.player.getX() - (targetAnchorPos.getX() + 0.5);
        double dy = (mc.player.getY() + mc.player.getEyeHeight()) - (targetAnchorPos.getY() + 0.5);
        double dz = mc.player.getZ() - (targetAnchorPos.getZ() + 0.5);

        double absX = Math.abs(dx);
        double absY = Math.abs(dy);
        double absZ = Math.abs(dz);

        if (absX > absY && absX > absZ) {
            return dx > 0 ? Direction.EAST : Direction.WEST;
        } else if (absY > absX && absY > absZ) {
            return dy > 0 ? Direction.UP : Direction.DOWN;
        } else {
            return dz > 0 ? Direction.SOUTH : Direction.NORTH;
        }
    }

    private int findShieldBlockSlot() {
        if (mc.player == null) return -1;
        
        int obsidianSlot = ModuleUtils.findItemInHotbar(Items.OBSIDIAN);
        if (obsidianSlot != -1) return obsidianSlot;

        int cryingSlot = ModuleUtils.findItemInHotbar(Items.CRYING_OBSIDIAN);
        if (cryingSlot != -1) return cryingSlot;

        for (int i = 0; i < 9; i++) {
            net.minecraft.world.item.ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;
            net.minecraft.world.item.Item item = stack.getItem();
            if (item == Items.GLOWSTONE || item == Items.RESPAWN_ANCHOR) continue;
            if (item instanceof net.minecraft.world.item.BlockItem blockItem) {
                net.minecraft.world.level.block.Block block = blockItem.getBlock();
                if (block.defaultBlockState().isCollisionShapeFullBlock(mc.level, BlockPos.ZERO)) {
                    return i;
                }
            }
        }
        return -1;
    }

    private int findNonAnchorNonGlowstoneSlot() {
        if (mc.player == null) return -1;
        for (int i = 0; i < 9; i++) {
            var stack = mc.player.getInventory().getItem(i);
            if (!stack.is(Items.GLOWSTONE) && !stack.is(Items.RESPAWN_ANCHOR)) {
                return i;
            }
        }
        return -1;
    }

    private void restoreSlot() {
        if (previousSlot >= 0 && previousSlot < 9) {
            ModuleUtils.switchToSlot(previousSlot);
        }
    }

    private void reset() {
        step = 0;
        targetAnchorPos = null;
    }

    private void finish() {
        step = 0;
        targetAnchorPos = null;
    }

    private void aimAt(BlockPos pos) {
        float[] rots = ModuleUtils.getRotations(mc.player.getEyePosition(), Vec3.atCenterOf(pos));
        RotationManager.keepRotated(rots[0], rots[1], 180f, false);
    }
}
