package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.utils.ModuleUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class AutoPlaceCrystal extends Module {
    private Setting delay;
    private Setting onlyOnRightClick;
    private Setting range;
    private Setting excludeBedrock;
    private Setting requireHoldingCrystal;
    private Setting requireHoldingWeapon;

    private long lastActionTime = 0;

    public AutoPlaceCrystal() {
        super("AutoPlaceCrystal", Category.Crystal, "Automatically places crystals on obsidian or bedrock.");
        
        delay = new Setting("Delay (ms)", this, 100.0, 0.0, 500.0, true);
        onlyOnRightClick = new Setting("Only On Right Click", this, true);
        range = new Setting("Range", this, 4.5, 1.0, 6.0, false);
        excludeBedrock = new Setting("Exclude Bedrock", this, false);
        requireHoldingCrystal = new Setting("Require Holding Crystal", this, false);
        requireHoldingWeapon = new Setting("Require Holding Weapon", this, false);

        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(delay);
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(onlyOnRightClick);
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(range);
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(excludeBedrock);
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(requireHoldingCrystal);
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(requireHoldingWeapon);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.level == null || mc.gameMode == null)
            return;

        if (mc.gui.screen() != null)
            return;

        if (onlyOnRightClick.getValBoolean() && !mc.options.keyUse.isDown())
            return;

        if (requireHoldingCrystal.getValBoolean() && !ModuleUtils.isHoldingCrystal(mc.player))
            return;

        if (requireHoldingWeapon.getValBoolean() && !ModuleUtils.isHoldingWeapon(mc.player.getMainHandItem()))
            return;

        if (System.currentTimeMillis() - lastActionTime < delay.getValDouble())
            return;

        int crystalSlot = ModuleUtils.getCrystalSlot();
        if (crystalSlot == -1)
            return;

        HitResult target = mc.hitResult;
        if (target instanceof BlockHitResult bhr) {
            BlockPos pos = bhr.getBlockPos();
            BlockState state = mc.level.getBlockState(pos);
            boolean allowBedrock = !excludeBedrock.getValBoolean();
            if ((state.is(Blocks.OBSIDIAN) || (allowBedrock && state.is(Blocks.BEDROCK))) && mc.level.isEmptyBlock(pos.above())) {
                double dist = mc.player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
                double r = range.getValDouble();
                if (dist <= r * r) {
                    int originalSlot = ModuleUtils.getSelectedSlot();
                    ModuleUtils.switchToSlot(crystalSlot);
                    mc.player.swing(InteractionHand.MAIN_HAND);
                    mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, bhr);
                    if (originalSlot != crystalSlot) {
                        ModuleUtils.switchToSlot(originalSlot);
                    }
                    lastActionTime = System.currentTimeMillis();
                }
            }
        }
    }
}
