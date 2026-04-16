package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import net.minecraft.client.KeyMapping;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class Automine extends Module {
    public Automine() {
        super("Automine", Category.Misc, "Automatically holds down your attack/break key.");
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.level == null || mc.screen != null) return;

        if (mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.BLOCK) {
            BlockHitResult blockHit = (BlockHitResult) mc.hitResult;
            if (!mc.level.getBlockState(blockHit.getBlockPos()).isAir()) {
                mc.options.keyAttack.setDown(true);
                if (mc.gameMode != null) {
                    mc.gameMode.continueDestroyBlock(blockHit.getBlockPos(), blockHit.getDirection());
                    mc.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                }
            }
        } else {
            mc.options.keyAttack.setDown(false);
            if (mc.gameMode != null) {
                mc.gameMode.stopDestroyBlock();
            }
        }
    }

    @Override
    public void onDisable() {
        if (mc.options != null) {
            mc.options.keyAttack.setDown(false);
        }
        if (mc.gameMode != null) {
            mc.gameMode.stopDestroyBlock();
        }
    }
}