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
    
    private int step = 0; // 0=place anchor, 1=charge, 2=explode
    private int ticksAssigned = 0;
    private BlockPos targetAnchorPos = null;

    public AnchorMacro() {
        super("AnchorMacro", Category.Blatant, "Automatically places, charges, and detonates respawn anchors.");
        setSubCategory("Semi-Blatant");

        delaySetting = new Setting("Delay Ticks", this, 1.0, 0.0, 5.0, true);
        safeAnchor = new Setting("Safe Anchor", this, true);
        autoDisable = new Setting("Auto Disable", this, true);
        silentAim = new Setting("Silent Aim", this, true);

        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(delaySetting);
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(safeAnchor);
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(autoDisable);
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(silentAim);
    }

    @Override
    public void onEnable() {
        step = 0;
        ticksAssigned = 0;
        targetAnchorPos = null;
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

        int anchorSlot = ModuleUtils.findItemInHotbar(Items.RESPAWN_ANCHOR);
        int glowstoneSlot = ModuleUtils.findItemInHotbar(Items.GLOWSTONE);

        if (anchorSlot == -1 || glowstoneSlot == -1) {
            if (autoDisable.getValBoolean()) setToggled(false);
            return;
        }

        BlockState currentPosState = mc.level.getBlockState(targetAnchorPos);

        if (step == 0) {
            if (currentPosState.is(Blocks.RESPAWN_ANCHOR)) {
                step = 1;
            } else {
                if (silentAim.getValBoolean()) aimAt(targetAnchorPos);
                int oldSlot = mc.player.getInventory().getSelectedSlot();
                ModuleUtils.switchToSlot(anchorSlot);
                ModuleUtils.placeBlockPacket(targetAnchorPos.below(), Direction.UP);
                ModuleUtils.switchToSlot(oldSlot);
                step = 1;
                ticksAssigned = (int) delaySetting.getValDouble();
            }
        } else if (step == 1) {
            if (safeAnchor.getValBoolean() && currentPosState.is(Blocks.RESPAWN_ANCHOR)) {
                if (currentPosState.hasProperty(BlockStateProperties.RESPAWN_ANCHOR_CHARGES) && 
                    currentPosState.getValue(BlockStateProperties.RESPAWN_ANCHOR_CHARGES) > 0) {
                    step = 2; // already charged enough to explode
                    return;
                }
            }

            if (silentAim.getValBoolean()) aimAt(targetAnchorPos);
            int oldSlot = mc.player.getInventory().getSelectedSlot();
            ModuleUtils.switchToSlot(glowstoneSlot);
            ModuleUtils.placeBlockPacket(targetAnchorPos, Direction.UP);
            ModuleUtils.switchToSlot(oldSlot);
            step = 2;
            ticksAssigned = (int) delaySetting.getValDouble();
        } else if (step == 2) {
            if (silentAim.getValBoolean()) aimAt(targetAnchorPos);
            // Must right click with something that doesn't inherently place/block
            int oldSlot = mc.player.getInventory().getSelectedSlot();
            // Just reuse anchor slot if it doesn't matter, or a non-item
            // Usually an empty explicit packet works
            mc.getConnection().send(new net.minecraft.network.protocol.game.ServerboundUseItemOnPacket(
                InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(targetAnchorPos), Direction.UP, targetAnchorPos, false), 0
            ));
            mc.player.swing(InteractionHand.MAIN_HAND);
            step = 0;
            targetAnchorPos = null;
            if (autoDisable.getValBoolean()) setToggled(false);
        }
    }
    
    private void aimAt(BlockPos pos) {
        float[] rots = ModuleUtils.getRotations(mc.player.getEyePosition(), Vec3.atCenterOf(pos));
        RotationManager.keepRotated(rots[0], rots[1], 180f, false);
    }
}
