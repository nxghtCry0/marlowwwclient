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
    private Setting autoDisable;
    private Setting silentAim;
    private Setting autoDetonate;
    
    private int step = 0; 
    private int ticksAssigned = 0;
    private BlockPos targetAnchorPos = null;
    private int previousSlot = 0;
    private boolean chargeAttempted = false;

    public AnchorMacro() {
        super("AnchorMacro", Category.Crystal, "Automatically places, charges, and detonates respawn anchors.");
        setSubCategory("Semi-Blatant");

        delaySetting = new Setting("Delay Ticks", this, 1.0, 0.0, 5.0, true);
        safeAnchor = new Setting("Safe Anchor", this, true);
        autoDisable = new Setting("Auto Disable", this, true);
        silentAim = new Setting("Silent Aim", this, true);
        autoDetonate = new Setting("Auto Detonate", this, true);

        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(delaySetting);
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(safeAnchor);
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(autoDisable);
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(silentAim);
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(autoDetonate);
    }

    @Override
    public void onEnable() {
        step = 0;
        ticksAssigned = 0;
        targetAnchorPos = null;
        previousSlot = ModuleUtils.getSelectedSlot();
        chargeAttempted = false;
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.level == null) return;

        if (targetAnchorPos == null) {
            net.minecraft.world.phys.HitResult crosshairTarget = mc.hitResult;
            if (crosshairTarget != null && crosshairTarget.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
                targetAnchorPos = ((BlockHitResult) crosshairTarget).getBlockPos().relative(((BlockHitResult) crosshairTarget).getDirection());
            } else {
                if (autoDisable.getValBoolean()) setToggled(false);
                return;
            }
        }

        if (ticksAssigned > 0) {
            ticksAssigned--;
            return;
        }

        BlockState currentPosState = mc.level.getBlockState(targetAnchorPos);

        if (step == 0) {
            if (currentPosState.is(Blocks.RESPAWN_ANCHOR)) {
                step = 1;
                chargeAttempted = false;
                ticksAssigned = (int) delaySetting.getValDouble();
                return;
            } else {
                int anchorSlot = ModuleUtils.findItemInHotbar(Items.RESPAWN_ANCHOR);
                if (anchorSlot == -1) {
                    if (autoDisable.getValBoolean()) setToggled(false);
                    return;
                }

                if (silentAim.getValBoolean()) aimAt(targetAnchorPos);
                previousSlot = ModuleUtils.getSelectedSlot();
                ModuleUtils.switchToSlot(anchorSlot);
                
                Direction approachFace = Direction.UP;
                BlockPos supportPos = targetAnchorPos.below();
                if (mc.hitResult instanceof BlockHitResult bhr) {
                    approachFace = bhr.getDirection();
                    supportPos = targetAnchorPos.relative(approachFace.getOpposite());
                }
                
                ModuleUtils.placeBlockPacket(supportPos, approachFace);
                step = 1;
                chargeAttempted = false;
                ticksAssigned = (int) delaySetting.getValDouble();
                return;
            }
        } else if (step == 1) {
            if (!currentPosState.is(Blocks.RESPAWN_ANCHOR)) {
                step = 0;
                chargeAttempted = false;
                targetAnchorPos = null;
                return;
            }

            int charges = currentPosState.hasProperty(BlockStateProperties.RESPAWN_ANCHOR_CHARGES)
                ? currentPosState.getValue(BlockStateProperties.RESPAWN_ANCHOR_CHARGES)
                : 0;

            if (charges <= 0 && !chargeAttempted) {
                int glowstoneSlot = ModuleUtils.findItemInHotbar(Items.GLOWSTONE);
                if (glowstoneSlot == -1) {
                    if (autoDisable.getValBoolean()) setToggled(false);
                    return;
                }

                if (silentAim.getValBoolean()) aimAt(targetAnchorPos);
                ModuleUtils.switchToSlot(glowstoneSlot);
                BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(targetAnchorPos), Direction.UP, targetAnchorPos, false);
                mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
                mc.player.swing(InteractionHand.MAIN_HAND);

                chargeAttempted = true;
                ticksAssigned = (int) delaySetting.getValDouble();
                return;
            }

            if (safeAnchor.getValBoolean() && charges > 0) {
                step = 2;
                ticksAssigned = (int) delaySetting.getValDouble();
                return;
            }

            step = 2;
            ticksAssigned = (int) delaySetting.getValDouble();
            return;
        } else if (step == 2) {
            int fallbackSlot = previousSlot;
            if (fallbackSlot < 0 || fallbackSlot > 8) {
                fallbackSlot = ModuleUtils.getSelectedSlot();
            }

            if (mc.player.getInventory().getItem(fallbackSlot).is(Items.GLOWSTONE) || mc.player.getInventory().getItem(fallbackSlot).is(Items.RESPAWN_ANCHOR)) {
                for (int i = 0; i < 9; i++) {
                    if (!mc.player.getInventory().getItem(i).is(Items.GLOWSTONE) && !mc.player.getInventory().getItem(i).is(Items.RESPAWN_ANCHOR)) {
                        fallbackSlot = i;
                        break;
                    }
                }
            }
            if (fallbackSlot >= 0 && fallbackSlot < 9) {
                ModuleUtils.switchToSlot(fallbackSlot);
            }
            
            if (autoDetonate.getValBoolean()) {
                if (silentAim.getValBoolean()) aimAt(targetAnchorPos);
                BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(targetAnchorPos), Direction.UP, targetAnchorPos, false);
                mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
                mc.player.swing(InteractionHand.MAIN_HAND);
            }

            if (previousSlot >= 0 && previousSlot < 9) {
                ModuleUtils.switchToSlot(previousSlot);
            }
            
            step = 0;
            chargeAttempted = false;
            targetAnchorPos = null;
            if (autoDisable.getValBoolean()) setToggled(false);
        }
    }
    
    private void aimAt(BlockPos pos) {
        float[] rots = ModuleUtils.getRotations(mc.player.getEyePosition(), Vec3.atCenterOf(pos));
        RotationManager.keepRotated(rots[0], rots[1], 180f, false);
    }
}
