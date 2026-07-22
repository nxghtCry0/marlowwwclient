package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class AutoTool extends Module {
    private int previousSlot = -1;
    private boolean wasBreaking = false;

    public AutoTool() {
        super("AutoTool", Category.Utility);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.level == null) return;

        boolean isBreaking = mc.options.keyAttack.isDown() && mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.BLOCK;

        if (isBreaking) {
            BlockHitResult blockHit = (BlockHitResult) mc.hitResult;
            BlockState state = mc.level.getBlockState(blockHit.getBlockPos());
            
            float bestSpeed = 1.0f;
            int bestSlot = -1;
            
            for (int i = 0; i < 9; i++) {
                net.minecraft.world.item.ItemStack stack = mc.player.getInventory().getItem(i);
                if (stack.isEmpty()) continue;
                
                float speed = stack.getDestroySpeed(state);
                if (speed > bestSpeed) {
                    bestSpeed = speed;
                    bestSlot = i;
                }
            }
            
            if (bestSlot != -1 && mc.player.getInventory().getSelectedSlot() != bestSlot) {
                if (!wasBreaking) {
                    previousSlot = mc.player.getInventory().getSelectedSlot();
                }
                mc.player.getInventory().setSelectedSlot(bestSlot);
                wasBreaking = true;
            } else if (bestSlot == -1 && !wasBreaking) {
                wasBreaking = true; 
            }
        } else {
            if (wasBreaking) {
                if (previousSlot != -1 && previousSlot >= 0 && previousSlot < 9) {
                    mc.player.getInventory().setSelectedSlot(previousSlot);
                }
                previousSlot = -1;
                wasBreaking = false;
            }
        }
    }
}
