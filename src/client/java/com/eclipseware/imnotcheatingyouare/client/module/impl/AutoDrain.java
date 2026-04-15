package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.utils.ModuleUtils;
import com.eclipseware.imnotcheatingyouare.client.utils.RotationManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;

public class AutoDrain extends Module {
    private long lastActionTime = 0;
    private boolean isSwappingBack = false;
    private int originalSlot = -1;
    private long swapBackTime = 0;
    private boolean waitingForRotation = false;

    public AutoDrain() {
        super("AutoDrain", Category.Combat, "Instantly removes enemy water using webs or buckets.");
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.level == null) return;

        if (isSwappingBack) {
            Setting swapBackSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Swap Back");
            if (swapBackSetting != null && swapBackSetting.getValBoolean()) {
                if (System.currentTimeMillis() >= swapBackTime) {
                    ModuleUtils.switchToSlot(originalSlot);
                    isSwappingBack = false;
                    originalSlot = -1;
                }
            } else {
                isSwappingBack = false;
            }
            return;
        }

        if (waitingForRotation) return;

        Setting delaySetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Delay (ms)");
        long delay = delaySetting != null ? (long) delaySetting.getValDouble() : 150;
        if (System.currentTimeMillis() - lastActionTime < delay) return;

        int radius = 6;
        BlockPos playerPos = mc.player.blockPosition();

        Player closestEnemy = null;
        double closestDist = Double.MAX_VALUE;
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof Player p && p != mc.player && p.isAlive() && mc.player.distanceTo(p) <= 8.0) {
                double d = mc.player.distanceTo(p);
                if (d < closestDist) {
                    closestDist = d;
                    closestEnemy = p;
                }
            }
        }

        BlockPos targetWater = null;
        BlockPos searchCenter = closestEnemy != null ? closestEnemy.blockPosition() : playerPos;
        int searchRadius = closestEnemy != null ? 3 : radius;

        for (int x = -searchRadius; x <= searchRadius; x++) {
            for (int y = -searchRadius; y <= searchRadius; y++) {
                for (int z = -searchRadius; z <= searchRadius; z++) {
                    BlockPos pos = searchCenter.offset(x, y, z);

                    if (pos.getY() >= playerPos.getY() && pos.getY() <= playerPos.getY() + 1) {
                        double distToPlayerFeet = Math.sqrt(
                            (pos.getX() - playerPos.getX()) * (pos.getX() - playerPos.getX()) +
                            (pos.getZ() - playerPos.getZ()) * (pos.getZ() - playerPos.getZ())
                        );
                        if (distToPlayerFeet < 1.5) continue;
                    }

                    if (mc.level.getFluidState(pos).getType() == Fluids.WATER && mc.level.getFluidState(pos).isSource()) {
                        targetWater = pos;
                        break;
                    }
                }
                if (targetWater != null) break;
            }
            if (targetWater != null) break;
        }

        if (targetWater == null) return;

        Vec3 eyes = mc.player.getEyePosition();
        Vec3 waterCenter = targetWater.getCenter();
        if (!RotationManager.hasLineOfSight(eyes, waterCenter)) return;

        int webSlot = ModuleUtils.findItemInHotbar(Items.COBWEB);
        int bucketSlot = ModuleUtils.findItemInHotbar(Items.BUCKET);
        if (webSlot == -1 && bucketSlot == -1) return;

        boolean useWeb = webSlot != -1;
        int useSlot = useWeb ? webSlot : bucketSlot;

        originalSlot = mc.player.getInventory().getSelectedSlot();
        float[] rots = ModuleUtils.getRotations(eyes, waterCenter);

        Setting smoothSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Smooth Rotation");
        int smoothTicks = smoothSetting != null ? (int) smoothSetting.getValDouble() : 2;

        Setting moveCorrectSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Movement Correction");
        boolean moveCorrect = moveCorrectSetting != null && moveCorrectSetting.getValBoolean();

        BlockPos finalTargetWater = targetWater;
        RotationManager.queueRotation(rots[0], rots[1], smoothTicks, 1, 2, moveCorrect, () -> {
            if (mc.player == null || mc.level == null) return;
            ModuleUtils.switchToSlot(useSlot);

            if (useWeb) {
                Direction dir = Direction.getNearest(
                    (int)(eyes.x - finalTargetWater.getX()),
                    (int)(eyes.y - finalTargetWater.getY()),
                    (int)(eyes.z - finalTargetWater.getZ()),
                    Direction.UP
                );
                ModuleUtils.placeBlockPacket(finalTargetWater, dir.getOpposite());
            } else {
                ModuleUtils.useItemPacket();
            }

            mc.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            lastActionTime = System.currentTimeMillis();
            waitingForRotation = false;

            Setting swapSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Swap Back");
            if (swapSetting != null && swapSetting.getValBoolean()) {
                isSwappingBack = true;
                swapBackTime = System.currentTimeMillis() + 100;
            }
        });

        waitingForRotation = true;
    }

    @Override
    public void onDisable() {
        if (isSwappingBack && originalSlot != -1 && mc.player != null) {
            ModuleUtils.switchToSlot(originalSlot);
        }
        isSwappingBack = false;
        waitingForRotation = false;
        RotationManager.cancel();
    }
}