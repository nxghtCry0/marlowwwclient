package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class CrystalHelper extends Module {

    private boolean wasDown = false;

    public CrystalHelper() {
        super("CrystalHelper", Category.Crystal, "Silent swaps to obsidian or crystal on LMB depending on the targeted block.");
    }

    @Override
    public void onTick() {
        if (mc == null || mc.player == null || mc.level == null) return;

        boolean isDown = mc.options.keyAttack.isDown();

        if (isDown && !wasDown) {
            if (mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.BLOCK) {
                BlockHitResult blockHit = (BlockHitResult) mc.hitResult;
                Block block = mc.level.getBlockState(blockHit.getBlockPos()).getBlock();

                if (block == Blocks.OBSIDIAN || block == Blocks.BEDROCK) {
                    int crystalSlot = findItem(Items.END_CRYSTAL);
                    if (crystalSlot != -1) {
                        silentUseItem(crystalSlot, blockHit);
                    }
                } 
                else {
                    int obiSlot = findItem(Items.OBSIDIAN);
                    if (obiSlot != -1) {
                        silentUseItem(obiSlot, blockHit);
                    }
                }
            }
        }
        
        wasDown = isDown;
    }

    private void silentUseItem(int targetSlot, BlockHitResult hitResult) {
        int oldSlot = mc.player.getInventory().getSelectedSlot();
        
        if (targetSlot != oldSlot) {
            mc.getConnection().send(new ServerboundSetCarriedItemPacket(targetSlot));
            mc.player.getInventory().setSelectedSlot(targetSlot);
        }

        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hitResult);
        mc.player.swing(InteractionHand.MAIN_HAND);

        if (targetSlot != oldSlot) {
            mc.player.getInventory().setSelectedSlot(oldSlot);
            mc.getConnection().send(new ServerboundSetCarriedItemPacket(oldSlot));
        }
    }

    private int findItem(Item item) {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getItem(i).is(item)) return i;
        }
        return -1;
    }
}
