package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.utils.RotationManager;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.phys.Vec3;

public class AutoMaceCounter extends Module {
    private int cooldownTicks = 0;
    private boolean swappingBack = false;
    private int originalSlot = -1;
    private int swapDelay = 0;

    public AutoMaceCounter() {
        super("AutoMaceCounter", Category.Combat, "Throws a wind charge at falling mace users to stop them.");
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Distance", this, 6.0, 2.0, 10.0, false));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Cooldown (Ticks)", this, 20.0, 0.0, 100.0, true));
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.level == null) return;

        if (cooldownTicks > 0) cooldownTicks--;

        if (swappingBack) {
            swapDelay--;
            if (swapDelay <= 0) {
                mc.player.getInventory().setSelectedSlot(originalSlot);
                mc.getConnection().send(new ServerboundSetCarriedItemPacket(originalSlot));
                swappingBack = false;
            }
        }

        if (cooldownTicks > 0) return;

        Setting distSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Distance");
        double maxDist = distSetting != null ? distSetting.getValDouble() : 6.0;

        Player bestTarget = null;
        double minDistance = maxDist;

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof Player target && target != mc.player && target.isAlive()) {
                double dist = mc.player.distanceTo(target);
                if (dist <= maxDist && target.getY() - mc.player.getY() >= 1.0) {
                    if (target.getMainHandItem().getItem() instanceof MaceItem || target.getOffhandItem().getItem() instanceof MaceItem) {
                        if (dist < minDistance) {
                            minDistance = dist;
                            bestTarget = target;
                        }
                    }
                }
            }
        }

        if (bestTarget != null) {
            Vec3 aimPoint = bestTarget.getEyePosition();
            Vec3 eyes = mc.player.getEyePosition();
            double diffX = aimPoint.x - eyes.x;
            double diffY = aimPoint.y - eyes.y;
            double diffZ = aimPoint.z - eyes.z;
            double distXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);
            
            float neededYaw = (float) (Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0F);
            float neededPitch = (float) -Math.toDegrees(Math.atan2(diffY, distXZ));

            RotationManager.keepRotated(neededYaw, neededPitch, 50.0f, false);

            if (Math.abs(Mth.wrapDegrees(RotationManager.getServerYaw() - neededYaw)) < 20f && 
                Math.abs(RotationManager.getServerPitch() - neededPitch) < 20f) {
                
                int slot = findWindCharge();
                if (slot != -1) {
                    int oldSlot = mc.player.getInventory().getSelectedSlot();
                    if (slot != oldSlot) {
                        mc.getConnection().send(new ServerboundSetCarriedItemPacket(slot));
                        mc.player.getInventory().setSelectedSlot(slot);
                        swappingBack = true;
                        originalSlot = oldSlot;
                        swapDelay = 1;
                    }
                    
                    mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
                    mc.player.swing(InteractionHand.MAIN_HAND);
                    
                    Setting cdSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Cooldown (Ticks)");
                    cooldownTicks = cdSetting != null ? (int) cdSetting.getValDouble() : 20;

                    RotationManager.requestReturn();
                }
            }
        }
    }

    private int findWindCharge() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.getItem() instanceof net.minecraft.world.item.WindChargeItem) {
                return i;
            }
        }
        return -1;
    }
}
