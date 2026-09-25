package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.utils.ModuleUtils;
import com.eclipseware.imnotcheatingyouare.mixin.client.MinecraftAccessor;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;

public class AutoHitCrystal extends Module {
    private final Setting onlyOnRightClick;
    private final Setting range;
    private final Setting breakMode;
    private final Setting requireHoldingCrystal;
    private final Setting requireHoldingWeapon;

    public static int lastHitTick = -1;

    public AutoHitCrystal() {
        super("AutoHitCrystal", Category.Crystal, "Attacks end crystals instantly with 2-tick synchronization.");

        onlyOnRightClick = new Setting("Only On Right Click", this, true);
        range = new Setting("Range", this, 4.5, 1.0, 6.0, false);

        ArrayList<String> modes = new ArrayList<>();
        modes.add("Crosshair");
        modes.add("Closest");
        breakMode = new Setting("Target Mode", this, "Crosshair", modes);

        requireHoldingCrystal = new Setting("Require Holding Crystal", this, false);
        requireHoldingWeapon = new Setting("Require Holding Weapon", this, false);

        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(onlyOnRightClick);
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(range);
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(breakMode);
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(requireHoldingCrystal);
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(requireHoldingWeapon);
    }

    public static boolean hasCrystalTarget() {
        if (mc.player == null || mc.level == null) return false;
        Module mod = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("AutoHitCrystal");
        if (mod == null || !mod.isToggled()) return false;
        AutoHitCrystal ahc = (AutoHitCrystal) mod;
        return ahc.findCrystalToBreak() != null;
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.level == null || mc.gameMode == null)
            return;

        if (mc.gui.screen() != null)
            return;

        if (onlyOnRightClick.getValBoolean() && !mc.options.keyUse.isDown())
            return;

        if (requireHoldingCrystal.getValBoolean() && !ModuleUtils.isHoldingCrystal(mc.player))
            return;

        if (requireHoldingWeapon.getValBoolean() && !ModuleUtils.isHoldingWeapon(mc.player.getMainHandItem()))
            return;

        if (AutoPlaceCrystal.holdsOtherUsable() && !(mc.hitResult instanceof EntityHitResult ehr && ehr.getEntity() instanceof EndCrystal)) return;

        EndCrystal crystal = findCrystalToBreak();
        if (crystal != null && crystal.isAlive()) {
            ((MinecraftAccessor) mc).setMissTime(0);
            mc.hitResult = new EntityHitResult(crystal);
            mc.crosshairPickEntity = crystal;

            ((MinecraftAccessor) mc).invokeStartAttack();
            mc.player.swing(InteractionHand.MAIN_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT, true);
            lastHitTick = mc.player.tickCount;

            if (mc.hitResult instanceof EntityHitResult) ((MinecraftAccessor) mc).setRightClickDelay(1);
        }
    }

    private EndCrystal findCrystalToBreak() {
        if (mc.player == null || mc.level == null) return null;
        double r = range.getValDouble();
        String mode = breakMode.getValString();

        if (mode.equalsIgnoreCase("Crosshair")) {
            HitResult target = mc.hitResult;
            if (target instanceof EntityHitResult ehr) {
                Entity entity = ehr.getEntity();
                if (entity instanceof EndCrystal crystal && crystal.isAlive()) {
                    if (mc.player.distanceTo(crystal) <= r) {
                        return crystal;
                    }
                }
            }
            return null;
        }

        EndCrystal closest = null;
        double closestDistSq = r * r;
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof EndCrystal crystal && crystal.isAlive()) {
                if (!mc.player.hasLineOfSight(crystal)) continue;
                double distSq = mc.player.distanceToSqr(crystal);
                if (distSq <= closestDistSq) {
                    closestDistSq = distSq;
                    closest = crystal;
                }
            }
        }
        return closest;
    }
}
