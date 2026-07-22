package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.utils.ModuleUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;

public class Surround extends Module {
    private final Setting center;
    private final Setting onlyOnGround;
    private final Setting disableOnJump;
    private final Setting delay;
    
    private long lastPlaceTime = 0;

    public Surround() {
        super("Surround", Category.Crystal, "Automatically surrounds your feet with obsidian to reduce crystal explosion damage.");
        
        center = new Setting("Center", this, true);
        onlyOnGround = new Setting("Only on Ground", this, true);
        disableOnJump = new Setting("Disable on Jump", this, true);
        delay = new Setting("Delay (ms)", this, 50.0, 0.0, 500.0, true);

        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(center);
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(onlyOnGround);
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(disableOnJump);
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(delay);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.level == null) return;
        if (mc.gui.screen() != null) return;

        if (onlyOnGround.getValBoolean() && !mc.player.onGround()) {
            return;
        }
        
        if (disableOnJump.getValBoolean() && mc.options.keyJump.isDown()) {
            toggle();
            return;
        }

        if (center.getValBoolean()) {
            boolean isMoving = mc.options.keyUp.isDown() || mc.options.keyDown.isDown() || mc.options.keyLeft.isDown() || mc.options.keyRight.isDown();
            if (!isMoving) {
                double cx = Math.floor(mc.player.getX()) + 0.5;
                double cz = Math.floor(mc.player.getZ()) + 0.5;
                if (Math.abs(mc.player.getX() - cx) > 0.1 || Math.abs(mc.player.getZ() - cz) > 0.1) {
                    mc.player.setPos(cx, mc.player.getY(), cz);
                }
            }
        }

        long now = System.currentTimeMillis();
        if (now - lastPlaceTime < delay.getValDouble()) return;

        int obbySlot = ModuleUtils.findItemInHotbar(Items.OBSIDIAN);
        if (obbySlot == -1) {
            obbySlot = ModuleUtils.findItemInHotbar(Items.CRYING_OBSIDIAN);
        }
        if (obbySlot == -1) return;

        BlockPos playerPos = mc.player.blockPosition();
        BlockPos[] surrounding = new BlockPos[]{
            playerPos.north(),
            playerPos.south(),
            playerPos.east(),
            playerPos.west()
        };

        for (BlockPos pos : surrounding) {
            BlockState state = mc.level.getBlockState(pos);
            if (state.isAir() || state.canBeReplaced()) {
                BlockPos below = pos.below();
                BlockState belowState = mc.level.getBlockState(below);
                if (!belowState.isAir() && belowState.getFluidState().isEmpty()) {
                    float[] rots = com.eclipseware.imnotcheatingyouare.client.utils.ModuleUtils.getRotations(
                        mc.player.getEyePosition(),
                        new net.minecraft.world.phys.Vec3(below.getX() + 0.5, below.getY() + 0.5, below.getZ() + 0.5)
                    );
                    com.eclipseware.imnotcheatingyouare.client.utils.SilentAimUtil.setRotation(rots[0], rots[1], 2);
                    com.eclipseware.imnotcheatingyouare.client.utils.ModuleUtils.runSilentSwap(obbySlot, () -> {
                        com.eclipseware.imnotcheatingyouare.client.utils.ModuleUtils.placeBlockPacket(below, net.minecraft.core.Direction.UP);
                    });
                    lastPlaceTime = System.currentTimeMillis();
                    break;
                }
            }
        }
    }
}
