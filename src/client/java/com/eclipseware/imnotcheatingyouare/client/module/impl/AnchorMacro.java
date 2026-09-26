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

    private int sentCharges = 0;
    private Vec3 aimedAt = null;

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
            BlockState looked = mc.level.getBlockState(bhr.getBlockPos());
            if (!looked.is(Blocks.RESPAWN_ANCHOR)) return;
            anchor = bhr.getBlockPos();
            originalSlot = ModuleUtils.getSelectedSlot();
            sentCharges = looked.getValue(BlockStateProperties.RESPAWN_ANCHOR_CHARGES);
            step = Step.CHARGE;
            waitTicks = 0;
            aimedAt = null;
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
        sentCharges = Math.max(sentCharges, state.getValue(BlockStateProperties.RESPAWN_ANCHOR_CHARGES));

        if (waitTicks > 0) {
            waitTicks--;
            Action next = nextAction();
            if (next != null) preAim(next.hit);
            return;
        }

        Action action = nextAction();
        if (action == null) {
            finish();
            return;
        }
        if (!ready(action.hit)) return;

        interact(action.slot, action.hit);
        advance(action);

        Action next = nextAction();
        if (next == null) {
            finish();
            return;
        }
        waitTicks = delay();
        preAim(next.hit);
    }

    private record Action(Step step, int slot, BlockHitResult hit) {}

    private Action nextAction() {
        while (true) {
            switch (step) {
                case CHARGE -> {
                    if (sentCharges >= (int) chargeCount.getValDouble()) {
                        step = safeAnchor.getValBoolean() ? Step.SHIELD : Step.DETONATE;
                        continue;
                    }
                    int glowstone = ModuleUtils.findItemInHotbar(Items.GLOWSTONE);
                    if (glowstone == -1) return null;
                    return new Action(Step.CHARGE, glowstone, anchorHit());
                }
                case SHIELD -> {
                    BlockHitResult shieldHit = shieldPlacement();
                    int glowstone = ModuleUtils.findItemInHotbar(Items.GLOWSTONE);
                    if (shieldHit == null || glowstone == -1) {
                        step = Step.DETONATE;
                        continue;
                    }
                    return new Action(Step.SHIELD, glowstone, shieldHit);
                }
                case DETONATE -> {
                    if (!autoDetonate.getValBoolean() || sentCharges == 0) return null;
                    int slot = detonateSlot();
                    if (slot == -1) return null;
                    return new Action(Step.DETONATE, slot, detonateHit());
                }
                default -> {
                    return null;
                }
            }
        }
    }

    private void advance(Action action) {
        switch (action.step) {
            case CHARGE -> sentCharges++;
            case SHIELD -> step = Step.DETONATE;
            case DETONATE -> step = Step.DONE;
            default -> {
            }
        }
    }

    private int delay() {
        return (int) delaySetting.getValDouble();
    }

    private void preAim(BlockHitResult hit) {
        if (!silentAim.getValBoolean()) return;
        float[] rots = ModuleUtils.getRotations(mc.player.getEyePosition(), hit.getLocation());
        RotationManager.keepRotated(rots[0], rots[1], 180f, true);
        aimedAt = hit.getLocation();
    }

    private boolean ready(BlockHitResult hit) {
        if (!silentAim.getValBoolean()) {
            return mc.hitResult instanceof BlockHitResult current && current.getBlockPos().equals(hit.getBlockPos());
        }
        boolean aimedLastTick = aimedAt != null && aimedAt.distanceToSqr(hit.getLocation()) < 0.01;
        preAim(hit);
        return aimedLastTick;
    }

    private void interact(int slot, BlockHitResult hit) {
        AutoTotem.triggerInputPause();
        ModuleUtils.switchToSlot(slot);
        InteractionResult result = mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
        if (result.consumesAction()) {
            mc.player.swing(InteractionHand.MAIN_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT, true);
        }
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
        sentCharges = 0;
        aimedAt = null;
    }
}
