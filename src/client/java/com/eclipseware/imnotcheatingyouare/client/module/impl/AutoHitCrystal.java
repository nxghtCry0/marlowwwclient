package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.utils.ModuleUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class AutoHitCrystal extends Module {
    private Setting delay;
    private Setting onlyOnRightClick;
    private Setting simClick;
    private Setting range;
    private Setting requireHoldingCrystal;
    private Setting requireHoldingWeapon;

    private long lastActionTime = 0;

    public AutoHitCrystal() {
        super("AutoHitCrystal", Category.Crystal, "Automatically hits and breaks end crystals within range.");
        
        delay = new Setting("Delay (ms)", this, 100.0, 50.0, 500.0, true);
        onlyOnRightClick = new Setting("Only On Right Click", this, true);
        simClick = new Setting("SimClick", this, true);
        range = new Setting("Range", this, 4.5, 1.0, 6.0, false);
        requireHoldingCrystal = new Setting("Require Holding Crystal", this, false);
        requireHoldingWeapon = new Setting("Require Holding Weapon", this, false);

        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(delay);
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(onlyOnRightClick);
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(simClick);
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(range);
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(requireHoldingCrystal);
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(requireHoldingWeapon);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.level == null || mc.gameMode == null)
            return;

        if (onlyOnRightClick.getValBoolean() && !mc.options.keyUse.isDown())
            return;

        if (requireHoldingCrystal.getValBoolean() && !ModuleUtils.isHoldingCrystal(mc.player))
            return;

        if (requireHoldingWeapon.getValBoolean() && !ModuleUtils.isHoldingWeapon(mc.player.getMainHandItem()))
            return;

        if (System.currentTimeMillis() - lastActionTime < delay.getValDouble())
            return;

        EndCrystal crystal = findCrystalToBreak();
        if (crystal != null) {
            mc.gameMode.attack(mc.player, crystal);
            mc.player.swing(InteractionHand.MAIN_HAND);
            lastActionTime = System.currentTimeMillis();
        }
    }

    private EndCrystal findCrystalToBreak() {
        double r = range.getValDouble();
        if (simClick.getValBoolean()) {
            HitResult target = mc.hitResult;
            if (target instanceof EntityHitResult ehr) {
                Entity entity = ehr.getEntity();
                if (entity instanceof EndCrystal crystal) {
                    if (mc.player.distanceTo(crystal) <= r) {
                        return crystal;
                    }
                }
            }
            return null;
        }

        EndCrystal closest = null;
        double closestDist = r;
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof EndCrystal crystal) {
                double dist = mc.player.distanceTo(crystal);
                if (dist <= closestDist) {
                    closestDist = dist;
                    closest = crystal;
                }
            }
        }
        return closest;
    }
}
