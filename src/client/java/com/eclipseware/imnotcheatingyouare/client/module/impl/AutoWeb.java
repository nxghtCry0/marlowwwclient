package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.utils.FriendManager;
import com.eclipseware.imnotcheatingyouare.client.utils.ModuleUtils;
import com.eclipseware.imnotcheatingyouare.client.utils.RotationManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class AutoWeb extends Module {
    private long lastActionTime = 0;
    private boolean isSwappingBack = false;
    private int originalSlot = -1;
    private long swapBackTime = 0;
    private BlockPos currentTarget = null;

    public AutoWeb() {
        super("AutoWeb", Category.Combat, "Places cobwebs in enemy paths to trip them.");
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

        Setting delaySetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Delay (ms)");
        long delay = delaySetting != null ? (long) delaySetting.getValDouble() : 250;
        if (System.currentTimeMillis() - lastActionTime < delay) return;

        Setting moveCorrectSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Movement Correction");
        boolean moveCorrect = moveCorrectSetting != null && moveCorrectSetting.getValBoolean();

        Setting smoothSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Smooth Rotation");
        float turnSpeed = smoothSetting != null ? (float)(90.0 / Math.max(1, smoothSetting.getValDouble())) : 45f;

        if (currentTarget == null) {
            currentTarget = findWebTarget();
            if (currentTarget == null) return;
            originalSlot = mc.player.getInventory().getSelectedSlot();
        }

        Vec3 eyes = mc.player.getEyePosition();
        Vec3 targetCenter = currentTarget.getCenter();
        if (!RotationManager.hasLineOfSight(eyes, targetCenter)) { currentTarget = null; return; }

        int webSlot = ModuleUtils.findItemInHotbar(Items.COBWEB);
        if (webSlot == -1) { currentTarget = null; return; }

        float[] rots = ModuleUtils.getRotations(eyes, targetCenter);
        RotationManager.keepRotated(rots[0], rots[1], turnSpeed, moveCorrect);

        float yawDiff = Math.abs(net.minecraft.util.Mth.wrapDegrees(rots[0] - RotationManager.getServerYaw()));
        float pitchDiff = Math.abs(rots[1] - RotationManager.getServerPitch());
        if (yawDiff > 5f || pitchDiff > 5f) return;

        ModuleUtils.switchToSlot(webSlot);
        ModuleUtils.placeBlockPacket(currentTarget.below(), Direction.UP);
        mc.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        lastActionTime = System.currentTimeMillis();
        currentTarget = null;

        Setting swapSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Swap Back");
        if (swapSetting != null && swapSetting.getValBoolean()) {
            isSwappingBack = true;
            swapBackTime = System.currentTimeMillis() + 100;
        }
    }

    private BlockPos findWebTarget() {
        double range = mc.player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE).getValue();
        Player target = null;
        double closestDist = Double.MAX_VALUE;

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof Player p && p != mc.player && p.isAlive() && mc.player.distanceTo(p) <= range && !FriendManager.isFriend(p)) {
                double d = mc.player.distanceTo(p);
                if (d < closestDist) { closestDist = d; target = p; }
            }
        }

        if (target == null) return null;

        BlockPos feetBlock = target.blockPosition();
        BlockState state = mc.level.getBlockState(feetBlock);
        if (!state.isAir()) return null;

        BlockPos support = feetBlock.below();
        if (mc.level.getBlockState(support).isAir()) return null;

        Vec3 eyes = mc.player.getEyePosition();
        if (!RotationManager.hasLineOfSight(eyes, support.getCenter())) return null;

        return feetBlock;
    }

    @Override
    public void onDisable() {
        if (isSwappingBack && originalSlot != -1 && mc.player != null) {
            ModuleUtils.switchToSlot(originalSlot);
        }
        isSwappingBack = false;
        currentTarget = null;
        RotationManager.requestReturn();
    }
}