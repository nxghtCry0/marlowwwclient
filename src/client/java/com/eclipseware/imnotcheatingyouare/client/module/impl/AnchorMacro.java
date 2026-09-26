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
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class AnchorMacro extends Module {
    private enum Step { IDLE, CHARGE, SHIELD, DETONATE, DONE }

    private final Setting delaySetting;
    private final Setting safeAnchor;
    private final Setting silentAim;
    private final Setting autoDetonate;
    private final Setting chargeCount;
    private final Setting preferTotem;

    private Step step = Step.IDLE;
    private BlockPos anchor;
    private int originalSlot = -1;
    private int waitTicks = 0;
    private boolean rotated = false;

    public AnchorMacro() {
        super("AnchorMacro", Category.Farming, "Charges and detonates the respawn anchor you look at, optionally shielding yourself with glowstone first.");
        setSubCategory("Macro");

        delaySetting = new Setting("Delay Ticks", this, 1.0, 0.0, 5.0, true);
        safeAnchor = new Setting("Safe Anchor", this, true);
        silentAim = new Setting("Silent Aim", this, true);
        autoDetonate = new Setting("Auto Detonate", this, true);
        chargeCount = new Setting("Charges", this, 1.0, 1.0, 4.0, true);
        preferTotem = new Setting("Detonate With Totem", this, true);

        var sm = ImnotcheatingyouareClient.INSTANCE.settingsManager;
        sm.rSetting(delaySetting);
        sm.rSetting(safeAnchor);
        sm.rSetting(silentAim);
        sm.rSetting(autoDetonate);
        sm.rSetting(chargeCount);
        sm.rSetting(preferTotem);
    }

    @Override
    public void onEnable() {
        reset();
    }

    @Override
    public void onDisable() {
        finish();
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.level == null || mc.gameMode == null) return;
        if (mc.player.isDeadOrDying() || mc.gui.screen() != null) {
            finish();
            return;
        }
        if (mc.player.isUsingItem()) return;

        if (step == Step.IDLE) {
            if (!(mc.hitResult instanceof BlockHitResult bhr) || mc.hitResult.getType() != HitResult.Type.BLOCK) return;
            if (!mc.level.getBlockState(bhr.getBlockPos()).is(Blocks.RESPAWN_ANCHOR)) return;
            anchor = bhr.getBlockPos();
            originalSlot = ModuleUtils.getSelectedSlot();
            step = Step.CHARGE;
            waitTicks = 0;
            rotated = false;
        }

        BlockState state = mc.level.getBlockState(anchor);
        if (!state.is(Blocks.RESPAWN_ANCHOR)) {
            finish();
            return;
        }
        if (mc.player.getEyePosition().distanceToSqr(Vec3.atCenterOf(anchor)) > 5.5 * 5.5) {
            finish();
            return;
        }

        if (waitTicks > 0) {
            waitTicks--;
            return;
        }

        int charges = state.getValue(BlockStateProperties.RESPAWN_ANCHOR_CHARGES);

        switch (step) {
            case CHARGE -> {
                int wanted = (int) chargeCount.getValDouble();
                if (charges >= wanted) {
                    step = safeAnchor.getValBoolean() ? Step.SHIELD : Step.DETONATE;
                    rotated = false;
                    return;
                }
                int glowstone = ModuleUtils.findItemInHotbar(Items.GLOWSTONE);
                if (glowstone == -1) {
                    finish();
                    return;
                }
                BlockHitResult hit = anchorHit();
                if (!aim(hit)) return;
                interact(glowstone, hit);
                waitTicks = delay();
            }
            case SHIELD -> {
                BlockHitResult shieldHit = shieldPlacement();
                if (shieldHit == null) {
                    step = Step.DETONATE;
                    rotated = false;
                    return;
                }
                int glowstone = ModuleUtils.findItemInHotbar(Items.GLOWSTONE);
                if (glowstone == -1) {
                    step = Step.DETONATE;
                    rotated = false;
                    return;
                }
                if (!aim(shieldHit)) return;
                interact(glowstone, shieldHit);
                step = Step.DETONATE;
                rotated = false;
                waitTicks = delay();
            }
            case DETONATE -> {
                if (!autoDetonate.getValBoolean()) {
                    finish();
                    return;
                }
                if (charges == 0) {
                    finish();
                    return;
                }
                int slot = detonateSlot();
                if (slot == -1) {
                    finish();
                    return;
                }
                BlockHitResult hit = detonateHit();
                if (!aim(hit)) return;
                interact(slot, hit);
                step = Step.DONE;
                waitTicks = delay();
            }
            case DONE -> finish();
            default -> {
            }
        }
    }

    private int delay() {
        return (int) delaySetting.getValDouble();
    }

    private boolean aim(BlockHitResult hit) {
        if (!silentAim.getValBoolean()) {
            return mc.hitResult instanceof BlockHitResult current && current.getBlockPos().equals(hit.getBlockPos());
        }
        float[] rots = ModuleUtils.getRotations(mc.player.getEyePosition(), hit.getLocation());
        RotationManager.keepRotated(rots[0], rots[1], 180f, true);
        if (!rotated) {
            rotated = true;
            return false;
        }
        return true;
    }

    private void interact(int slot, BlockHitResult hit) {
        AutoTotem.triggerInputPause();
        ModuleUtils.switchToSlot(slot);
        InteractionResult result = mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
        if (result.consumesAction()) {
            mc.player.swing(InteractionHand.MAIN_HAND);
        }
        rotated = false;
    }

    private BlockHitResult anchorHit() {
        if (!silentAim.getValBoolean() && mc.hitResult instanceof BlockHitResult bhr && bhr.getBlockPos().equals(anchor)) {
            return bhr;
        }
        return detonateHit();
    }

    private BlockHitResult detonateHit() {
        Vec3 eye = mc.player.getEyePosition();
        Direction best = Direction.UP;
        double bestDist = Double.MAX_VALUE;
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = anchor.relative(dir);
            BlockState ns = mc.level.getBlockState(neighbor);
            if (!ns.isAir() && !ns.canBeReplaced()) continue;
            Vec3 face = Vec3.atCenterOf(anchor).add(dir.getStepX() * 0.5, dir.getStepY() * 0.5, dir.getStepZ() * 0.5);
            double d = eye.distanceToSqr(face);
            Vec3 toEye = eye.subtract(face);
            if (toEye.dot(new Vec3(dir.getStepX(), dir.getStepY(), dir.getStepZ())) <= 0) continue;
            if (d < bestDist) {
                bestDist = d;
                best = dir;
            }
        }
        Vec3 face = Vec3.atCenterOf(anchor).add(best.getStepX() * 0.5, best.getStepY() * 0.5, best.getStepZ() * 0.5);
        return new BlockHitResult(face, best, anchor, false);
    }

    private BlockHitResult shieldPlacement() {
        Direction toPlayer = horizontalToPlayer();
        BlockPos shield = anchor.relative(toPlayer);
        BlockState shieldState = mc.level.getBlockState(shield);
        if (!shieldState.isAir() && !shieldState.canBeReplaced()) return null;
        if (mc.player.getBoundingBox().intersects(new AABB(shield))) return null;

        BlockPos support = shield.below();
        BlockState supportState = mc.level.getBlockState(support);
        if (supportState.isAir() || supportState.canBeReplaced() || supportState.is(Blocks.RESPAWN_ANCHOR)) return null;

        Vec3 top = new Vec3(support.getX() + 0.5, support.getY() + 1.0, support.getZ() + 0.5);
        return new BlockHitResult(top, Direction.UP, support, false);
    }

    private Direction horizontalToPlayer() {
        double dx = mc.player.getX() - (anchor.getX() + 0.5);
        double dz = mc.player.getZ() - (anchor.getZ() + 0.5);
        if (Math.abs(dx) > Math.abs(dz)) return dx > 0 ? Direction.EAST : Direction.WEST;
        return dz > 0 ? Direction.SOUTH : Direction.NORTH;
    }

    private int detonateSlot() {
        if (preferTotem.getValBoolean()) {
            int totem = ModuleUtils.findItemInHotbar(Items.TOTEM_OF_UNDYING);
            if (totem != -1) return totem;
        }
        if (originalSlot >= 0 && originalSlot < 9 && !mc.player.getInventory().getItem(originalSlot).is(Items.GLOWSTONE)) {
            return originalSlot;
        }
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (!stack.is(Items.GLOWSTONE)) return i;
        }
        return -1;
    }

    private void finish() {
        if (originalSlot >= 0 && originalSlot < 9 && mc.player != null) {
            ModuleUtils.switchToSlot(originalSlot);
        }
        reset();
    }

    private void reset() {
        step = Step.IDLE;
        anchor = null;
        originalSlot = -1;
        waitTicks = 0;
        rotated = false;
    }
}
