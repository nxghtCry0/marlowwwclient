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
    private boolean waitingForRotation = false;

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

        if (waitingForRotation) return;

        Setting delaySetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Delay (ms)");
        long delay = delaySetting != null ? (long) delaySetting.getValDouble() : 250;
        if (System.currentTimeMillis() - lastActionTime < delay) return;

        double range = mc.player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE).getValue();
        Player target = null;
        double closestDist = Double.MAX_VALUE;

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof Player p && p != mc.player && p.isAlive() && mc.player.distanceTo(p) <= range && !FriendManager.isFriend(p)) {
                double d = mc.player.distanceTo(p);
                if (d < closestDist) {
                    closestDist = d;
                    target = p;
                }
            }
        }

        if (target == null) return;

        Vec3 feetPos = target.position().add(0, -0.1, 0);
        BlockPos feetBlock = BlockPos.containing(feetPos);
        BlockState state = mc.level.getBlockState(feetBlock);

        if (!state.isAir()) return;

        BlockPos support = feetBlock.below();
        if (mc.level.getBlockState(support).isAir()) return;

        Vec3 eyes = mc.player.getEyePosition();
        Vec3 targetCenter = support.getCenter();
        if (!RotationManager.hasLineOfSight(eyes, targetCenter)) return;

        int webSlot = ModuleUtils.findItemInHotbar(Items.COBWEB);
        if (webSlot == -1) return;

        originalSlot = mc.player.getInventory().getSelectedSlot();
        float[] rots = ModuleUtils.getRotations(eyes, targetCenter);

        Setting smoothSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Smooth Rotation");
        int smoothTicks = smoothSetting != null ? (int) smoothSetting.getValDouble() : 2;

        Setting moveCorrectSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Movement Correction");
        boolean moveCorrect = moveCorrectSetting != null && moveCorrectSetting.getValBoolean();

        RotationManager.queueRotation(rots[0], rots[1], smoothTicks, 1, 2, moveCorrect, () -> {
            if (mc.player == null || mc.level == null) return;
            ModuleUtils.switchToSlot(webSlot);
            ModuleUtils.placeBlockPacket(support, Direction.UP);
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