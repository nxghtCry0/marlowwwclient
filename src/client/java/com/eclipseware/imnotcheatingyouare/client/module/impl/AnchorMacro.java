package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.utils.ModuleUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class AnchorMacro extends Module {
    private Setting delaySetting;
    private Setting safeAnchor;
    private Setting silentAim;
    private Setting autoDetonate;

    private BlockPos trackedAnchor = null;
    private int originalSlot = -1;
    private int lookTicks = 0;
    private long lastActionTime = 0L;
    private int step = 0; 

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
        reset();
    }

    @Override
    public void onDisable() {
        restoreSlot();
        reset();
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.level == null || mc.gameMode == null) return;
        if (mc.player.isDeadOrDying() || mc.gui.screen() instanceof net.minecraft.client.gui.screens.DeathScreen) {
            reset();
            return;
        }
        if (mc.player.isUsingItem()) return;

        HitResult hit = mc.hitResult;
        if (hit == null || hit.getType() != HitResult.Type.BLOCK) {
            restoreSlot();
            reset();
            return;
        }

        BlockHitResult bhr = (BlockHitResult) hit;
        BlockPos targetPos = bhr.getBlockPos();
        BlockState state = mc.level.getBlockState(targetPos);

        if (!state.is(Blocks.RESPAWN_ANCHOR)) {
            restoreSlot();
            reset();
            return;
        }

        if (trackedAnchor == null || !trackedAnchor.equals(targetPos)) {
            restoreSlot();
            reset();
            trackedAnchor = targetPos;
            originalSlot = ModuleUtils.getSelectedSlot();
        }

        lookTicks++;
        if (lookTicks < 2) return;

        long delayMs = (long) (delaySetting.getValDouble() * 50.0);
        if (System.currentTimeMillis() - lastActionTime < delayMs) return;

        int charges = state.hasProperty(BlockStateProperties.RESPAWN_ANCHOR_CHARGES)
                ? state.getValue(BlockStateProperties.RESPAWN_ANCHOR_CHARGES)
                : 0;

        if (charges == 0 && step == 0) {
            int gsSlot = ModuleUtils.findItemInHotbar(Items.GLOWSTONE);
            if (gsSlot == -1) return;

            AutoTotem.triggerInputPause();
            ModuleUtils.switchToSlot(gsSlot);
            if (canPlace()) {
                ((com.eclipseware.imnotcheatingyouare.mixin.client.MinecraftAccessor) mc).invokeStartUseItem();
                lastActionTime = System.currentTimeMillis();
                step = safeAnchor.getValBoolean() ? 1 : 2;
            }
            return;
        }

        if (charges > 0 && step == 0 && safeAnchor.getValBoolean()) {
            step = 1;
        }

        if (step == 1 && safeAnchor.getValBoolean()) {
            Direction dir = getDirectionToPlayer(targetPos);
            BlockPos shieldPos = targetPos.relative(dir);
            BlockState shieldState = mc.level.getBlockState(shieldPos);

            if (!shieldState.isAir() && !shieldState.canBeReplaced()) {
                step = 2;
            } else {
                int shieldSlot = findShieldBlockSlot();
                if (shieldSlot != -1) {
                    BlockPos floorSupport = shieldPos.below();
                    BlockState floorState = mc.level.getBlockState(floorSupport);
                    
                    BlockPos placeOnPos = (!floorState.isAir() && !floorState.canBeReplaced()) ? floorSupport : targetPos;
                    Direction placeFace = (placeOnPos.equals(floorSupport)) ? Direction.UP : dir;

                    double x = placeOnPos.getX() + 0.5 + placeFace.getStepX() * 0.5;
                    double y = placeOnPos.getY() + 0.5 + placeFace.getStepY() * 0.5;
                    double z = placeOnPos.getZ() + 0.5 + placeFace.getStepZ() * 0.5;

                    net.minecraft.world.phys.Vec3 hitVec = new net.minecraft.world.phys.Vec3(x, y, z);
                    BlockHitResult shieldHit = new BlockHitResult(hitVec, placeFace, placeOnPos, false);
                    
                    if (silentAim.getValBoolean()) {
                        float[] rots = ModuleUtils.getRotations(mc.player.getEyePosition(), hitVec);
                        com.eclipseware.imnotcheatingyouare.client.utils.RotationManager.keepRotated(rots[0], rots[1], 180f, false);
                    }
                    
                    AutoTotem.triggerInputPause();
                    int prevSlot = ModuleUtils.getSelectedSlot();
                    ModuleUtils.switchToSlot(shieldSlot);
                    mc.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT, true);
                    mc.gameMode.useItemOn(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND, shieldHit);
                    ModuleUtils.switchToSlot(prevSlot);
                    lastActionTime = System.currentTimeMillis();
                }
                step = 2;
            }
            return;
        }

        if (autoDetonate.getValBoolean() && (charges > 0 || step == 2)) {
            int detonateSlot = findDetonateSlot();
            int slotToUse = detonateSlot != -1 ? detonateSlot : originalSlot;

            int anchorSlot = ModuleUtils.findItemInHotbar(Items.RESPAWN_ANCHOR);
            if (anchorSlot != -1) {
                slotToUse = anchorSlot;
            }

            if (slotToUse != -1) {
                ModuleUtils.switchToSlot(slotToUse);
            }

            if (canPlace()) {
                AutoTotem.triggerInputPause();
                ((com.eclipseware.imnotcheatingyouare.mixin.client.MinecraftAccessor) mc).invokeStartUseItem();
                lastActionTime = System.currentTimeMillis();
                restoreSlot();
                reset();
            }
        }
    }

    private boolean canPlace() {
        if (mc.player == null || mc.level == null || mc.gameMode == null) return false;
        if (mc.gui != null && mc.gui.screen() != null) return false;
        if (mc.hitResult == null || mc.hitResult.getType() != HitResult.Type.BLOCK) return false;
        if (mc.player.isUsingItem()) return false;
        return true;
    }

    private int findShieldBlockSlot() {
        if (mc.player == null) return -1;
        int obsidianSlot = ModuleUtils.findItemInHotbar(Items.OBSIDIAN);
        if (obsidianSlot != -1) return obsidianSlot;

        int cryingSlot = ModuleUtils.findItemInHotbar(Items.CRYING_OBSIDIAN);
        if (cryingSlot != -1) return cryingSlot;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;
            if (stack.getItem() instanceof BlockItem blockItem) {
                if (blockItem.getBlock() instanceof FallingBlock) continue;
                if (blockItem.getBlock() == Blocks.RESPAWN_ANCHOR) continue;
                if (stack.is(Items.GLOWSTONE)) continue;
                return i;
            }
        }
        return -1;
    }

    private int findDetonateSlot() {
        if (mc.player == null) return -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;
            if (!stack.is(Items.GLOWSTONE)) {
                return i;
            }
        }
        return -1;
    }

    private void restoreSlot() {
        int anchorSlot = ModuleUtils.findItemInHotbar(Items.RESPAWN_ANCHOR);
        if (anchorSlot != -1) {
            ModuleUtils.switchToSlot(anchorSlot);
        } else if (originalSlot >= 0 && originalSlot < 9) {
            ModuleUtils.switchToSlot(originalSlot);
        }
        originalSlot = -1;
    }

    private void reset() {
        trackedAnchor = null;
        lookTicks = 0;
        lastActionTime = 0L;
        step = 0;
    }

    private Direction getDirectionToPlayer(BlockPos pos) {
        if (mc.player == null) return Direction.NORTH;
        double dx = mc.player.getX() - (pos.getX() + 0.5);
        double dz = mc.player.getZ() - (pos.getZ() + 0.5);
        if (Math.abs(dx) > Math.abs(dz)) {
            return dx > 0 ? Direction.EAST : Direction.WEST;
        } else {
            return dz > 0 ? Direction.SOUTH : Direction.NORTH;
        }
    }
}
