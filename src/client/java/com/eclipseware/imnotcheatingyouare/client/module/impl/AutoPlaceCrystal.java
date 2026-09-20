package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.utils.ModuleUtils;
import com.eclipseware.imnotcheatingyouare.mixin.client.MinecraftAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class AutoPlaceCrystal extends Module {
    private final Setting onlyOnRightClick;
    private final Setting range;
    private final Setting preferOffhand;
    private final Setting autoSwap;
    private final Setting excludeBedrock;
    private final Setting requireHoldingCrystal;
    private final Setting requireHoldingWeapon;

    public AutoPlaceCrystal() {
        super("AutoPlaceCrystal", Category.Crystal, "Places end crystals with tick-locked precision and zero multi-action flags.");

        onlyOnRightClick = new Setting("Only On Right Click", this, true);
        range = new Setting("Range", this, 4.5, 1.0, 6.0, false);
        preferOffhand = new Setting("Prefer Offhand", this, true);
        autoSwap = new Setting("Auto Swap", this, true);
        excludeBedrock = new Setting("Exclude Bedrock", this, false);
        requireHoldingCrystal = new Setting("Require Holding Crystal", this, false);
        requireHoldingWeapon = new Setting("Require Holding Weapon", this, false);

        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(onlyOnRightClick);
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(range);
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(preferOffhand);
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(autoSwap);
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

        // If AutoHitCrystal has a crystal to hit this tick, yield to maintain 2-tick cycle
        if (AutoHitCrystal.hasCrystalTarget() || AutoHitCrystal.lastHitTick == mc.player.tickCount) {
            return;
        }

        boolean offhandHasCrystal = preferOffhand.getValBoolean() && mc.player.getOffhandItem().is(Items.END_CRYSTAL);
        int crystalSlot = -1;
        if (!offhandHasCrystal) {
            crystalSlot = ModuleUtils.getCrystalSlot();
            if (crystalSlot == -1 && !mc.player.getMainHandItem().is(Items.END_CRYSTAL)) {
                return;
            }
        }

        HitResult target = mc.hitResult;
        if (target instanceof BlockHitResult bhr) {
            BlockPos pos = bhr.getBlockPos();
            BlockState state = mc.level.getBlockState(pos);
            boolean allowBedrock = !excludeBedrock.getValBoolean();

            if ((state.is(Blocks.OBSIDIAN) || (allowBedrock && state.is(Blocks.BEDROCK))) && mc.level.isEmptyBlock(pos.above())) {
                double distSq = mc.player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
                double r = range.getValDouble();
                if (distSq <= r * r) {
                    if (!offhandHasCrystal && autoSwap.getValBoolean() && crystalSlot != -1) {
                        if (ModuleUtils.getSelectedSlot() != crystalSlot) {
                            ModuleUtils.switchToSlot(crystalSlot);
                        }
                    }

                    // Zero right click delay so vanilla placement fires cleanly
                    ((MinecraftAccessor) mc).setRightClickDelay(0);

                    // If not holding keyUse (macro mode), invoke placement once
                    if (!mc.options.keyUse.isDown()) {
                        ((MinecraftAccessor) mc).invokeStartUseItem();
                    }
                }
            }
        }
    }
}
