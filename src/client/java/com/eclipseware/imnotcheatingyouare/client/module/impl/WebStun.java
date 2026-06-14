package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.utils.ModuleUtils;
import com.eclipseware.imnotcheatingyouare.client.utils.RotationManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class WebStun extends Module {
    private static Entity pendingTarget = null;
    private static long shieldBreakTime = 0;
    
    private int state = 0;
    private BlockPos targetBlock = null;
    private PlaceData placeData = null;
    private int originalSlot = -1;
    private int ticksInState = 0;

    public static class PlaceData {
        public final BlockPos pos;
        public final Direction face;
        public final Vec3 hitVec;

        public PlaceData(BlockPos pos, Direction face, Vec3 hitVec) {
            this.pos = pos;
            this.face = face;
            this.hitVec = hitVec;
        }
    }

    public WebStun() {
        super("WebStun", Category.Utility, "Automatically places a cobweb under enemies when their shield is disabled.");
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Max Delay (ms)", this, 1000.0, 100.0, 3000.0, true));
    }

    public static void trigger(Entity entity) {
        Module webStun = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("WebStun");
        if (webStun != null && webStun.isToggled()) {
            pendingTarget = entity;
            shieldBreakTime = System.currentTimeMillis();
        }
    }

    public static void onShieldBreak(Entity entity) {
        Module webStun = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("WebStun");
        if (webStun != null && webStun.isToggled()) {
            Module shieldBreaker = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("AutoShieldBreaker");
            if (shieldBreaker != null && shieldBreaker.isToggled() && shieldBreaker instanceof AutoShieldBreaker asb) {
                if (System.currentTimeMillis() - asb.lastBreakTime < 1000) {
                    pendingTarget = entity;
                    shieldBreakTime = System.currentTimeMillis();
                }
            }
        }
    }

    private static java.lang.reflect.Method getHandlerMethod = null;
    
    private static net.minecraft.client.multiplayer.prediction.BlockStatePredictionHandler getPredictionHandler() {
        if (mc.level == null) return null;
        try {
            if (getHandlerMethod == null) {
                getHandlerMethod = net.minecraft.client.multiplayer.ClientLevel.class.getDeclaredMethod("getBlockStatePredictionHandler");
                getHandlerMethod.setAccessible(true);
            }
            return (net.minecraft.client.multiplayer.prediction.BlockStatePredictionHandler) getHandlerMethod.invoke(mc.level);
        } catch (Exception e) {
            return null;
        }
    }

    private PlaceData getPlaceData(BlockPos pos) {
        if (mc.level == null) return null;
        for (Direction side : new Direction[]{Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, Direction.UP}) {
            BlockPos neighbor = pos.relative(side);
            net.minecraft.world.level.block.state.BlockState state = mc.level.getBlockState(neighbor);
            if (!state.isAir() && !state.canBeReplaced()) {
                Direction face = side.getOpposite();
                Vec3 hitVec = Vec3.atCenterOf(neighbor).add(
                    face.getStepX() * 0.5,
                    face.getStepY() * 0.5,
                    face.getStepZ() * 0.5
                );
                return new PlaceData(neighbor, face, hitVec);
            }
        }
        return null;
    }

    private void placeWeb(PlaceData data) {
        if (mc.player == null || mc.getConnection() == null || data == null) return;
        
        BlockHitResult hitResult = new BlockHitResult(
            data.hitVec, data.face, data.pos, false
        );

        net.minecraft.client.multiplayer.prediction.BlockStatePredictionHandler handler = getPredictionHandler();
        int seq = 0;
        if (handler != null) {
            handler.startPredicting();
            seq = handler.currentSequence();
        }

        mc.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        mc.getConnection().send(new ServerboundUseItemOnPacket(
            net.minecraft.world.InteractionHand.MAIN_HAND, hitResult, seq
        ));
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.level == null) {
            resetState();
            return;
        }

        Setting maxDelaySetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Max Delay (ms)");
        long maxDelay = maxDelaySetting != null ? (long) maxDelaySetting.getValDouble() : 1000;
        if (pendingTarget != null && (System.currentTimeMillis() - shieldBreakTime > maxDelay)) {
            pendingTarget = null;
            resetState();
        }

        if (state == 0) {
            if (pendingTarget != null) {
                targetBlock = pendingTarget.blockPosition();
                
                int webSlot = ModuleUtils.findItemInHotbar(Items.COBWEB);
                if (webSlot == -1) {
                    pendingTarget = null;
                    return;
                }

                if (!mc.level.getBlockState(targetBlock).canBeReplaced()) {
                    pendingTarget = null;
                    return;
                }

                placeData = getPlaceData(targetBlock);
                if (placeData == null) {
                    pendingTarget = null;
                    return;
                }

                originalSlot = mc.player.getInventory().getSelectedSlot();
                state = 1;
                ticksInState = 0;
            }
        } else if (state == 1) {
            state = 2;
            ticksInState = 0;
        } else if (state == 2) {
            int webSlot = ModuleUtils.findItemInHotbar(Items.COBWEB);
            if (webSlot == -1 || targetBlock == null || !mc.level.getBlockState(targetBlock).canBeReplaced() || placeData == null) {
                resetState();
                return;
            }

            Vec3 targetVec = placeData.hitVec;
            Vec3 eyes = mc.player.getEyePosition();
            float[] rots = ModuleUtils.getRotations(eyes, targetVec);

            RotationManager.queueRotation(rots[0], rots[1], 1, 1, 1, true, null);
            
            state = 3;
            ticksInState = 0;
        } else if (state == 3) {
            int webSlot = ModuleUtils.findItemInHotbar(Items.COBWEB);
            if (webSlot == -1 || targetBlock == null || !mc.level.getBlockState(targetBlock).canBeReplaced() || placeData == null) {
                resetState();
                return;
            }

            Vec3 targetVec = placeData.hitVec;
            Vec3 eyes = mc.player.getEyePosition();
            float[] rots = ModuleUtils.getRotations(eyes, targetVec);
            RotationManager.queueRotation(rots[0], rots[1], 1, 1, 1, true, null);

            ModuleUtils.switchToSlot(webSlot);

            state = 4;
            ticksInState = 0;
        } else if (state == 4) {
            int webSlot = ModuleUtils.findItemInHotbar(Items.COBWEB);
            if (webSlot == -1 || targetBlock == null || !mc.level.getBlockState(targetBlock).canBeReplaced() || placeData == null) {
                state = 5;
                return;
            }

            Vec3 targetVec = placeData.hitVec;
            Vec3 eyes = mc.player.getEyePosition();
            float[] rots = ModuleUtils.getRotations(eyes, targetVec);
            RotationManager.queueRotation(rots[0], rots[1], 1, 1, 1, true, null);

            RotationManager.postMovementCallback = () -> {
                placeWeb(placeData);
            };

            state = 5;
            ticksInState = 0;
        } else if (state == 5) {
            if (originalSlot != -1) {
                ModuleUtils.switchToSlot(originalSlot);
            }
            
            RotationManager.requestReturn();

            resetState();
            pendingTarget = null;
        }
    }

    private void resetState() {
        state = 0;
        targetBlock = null;
        placeData = null;
        originalSlot = -1;
        ticksInState = 0;
    }

    @Override
    public void onDisable() {
        resetState();
        pendingTarget = null;
    }
}
