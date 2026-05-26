package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.utils.ModuleUtils;
import com.eclipseware.imnotcheatingyouare.client.utils.RotationManager;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class PearlGrapple extends Module {
    private boolean active = false;
    private int ticksElapsed = 0;
    private int originalSlot = -1;
    private float targetYaw, targetPitch;

    public PearlGrapple() {
        super("PearlGrapple", Category.Utility, "Automatically throws a pearl at the target.");
    }

    @Override
    public void onKeybind() {
        if (mc.player == null || mc.getConnection() == null || active) return;

        int pearlSlot = -1;
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getItem(i).getItem() == net.minecraft.world.item.Items.ENDER_PEARL) {
                pearlSlot = i;
                break;
            }
        }

        if (pearlSlot == -1) {
            super.onKeybind(); 
            return;
        }

        Entity target = null;
        double bestDist = 100.0;
        for (Entity e : mc.level.entitiesForRendering()) {
            if (e != mc.player && e.isAlive() && e instanceof Player) {
                double d = mc.player.distanceTo(e);
                if (d < bestDist && d < 30.0) {
                    target = e;
                    bestDist = d;
                }
            }
        }
        
        if (target == null) {
            super.onKeybind();
            return;
        }

        Vec3 eyes = mc.player.getEyePosition();
        Vec3 aim = target.position().add(0, target.getBbHeight() / 2.0, 0);
        float[] rots = ModuleUtils.getRotations(eyes, aim);

        targetYaw = rots[0];
        targetPitch = rots[1];

        originalSlot = ModuleUtils.getSelectedSlot();
        ModuleUtils.switchToSlot(pearlSlot);
        active = true;
        ticksElapsed = 0;
    }

    @Override
    public void onTick() {
        if (!active || mc.player == null) return;
        
        RotationManager.keepRotated(targetYaw, targetPitch, 30.0f, false);
        ticksElapsed++;

        if (ticksElapsed == 5) {
            mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
            mc.player.swing(InteractionHand.MAIN_HAND);
        }
        
        if (ticksElapsed >= 8) {
            ModuleUtils.switchToSlot(originalSlot);
            RotationManager.requestReturn();
            active = false;
        }
    }

    @Override
    public void onDisable() {
        active = false;
        RotationManager.requestReturn();
    }
}
