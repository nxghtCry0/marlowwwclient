package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.utils.FriendManager;
import com.eclipseware.imnotcheatingyouare.client.utils.RotationManager;
import com.eclipseware.imnotcheatingyouare.client.utils.MouseAimHelper;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

public class KillAura extends Module {
    private LivingEntity target;

    public LivingEntity getTarget() {
        return target;
    }
    private long lastAttackTime = 0;
    private long nextDelay = 0;
    private int aimTicks = 0;
    private LivingEntity lastTarget = null;
    private double startAimMult = 1.0;
    private double midAimMult = 1.0;
    private double endAimMult = 1.0;

    public KillAura() {
        super("KillAura", Category.Blatant, "Automatically attacks entities around you.");
    }

    @Override
    public void onDisable() {
        target = null;
        lastTarget = null;
        aimTicks = 0;
        RotationManager.requestReturn();
        MouseAimHelper.clearAimRate();
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.level == null) return;
        if (mc.player.isDeadOrDying()) return;
        if (mc.player.isBlocking()) return;

        double range = getSettingDouble("Range");
        boolean modernMode = getSettingCombo("Combat System").equalsIgnoreCase("Modern (1.9+)");
        boolean critOnly = getSettingBoolean("Criticals Only");
        float turnSpeed = (float) getSettingDouble("Turn Speed");
        boolean moveCorrect = getSettingBoolean("Movement Correction");
        String rotMode = getSettingCombo("Rotation Mode");
        boolean silent = getSettingBoolean("Silent");

        target = getBestTarget(range);
        if (target == null) {
            lastTarget = null;
            aimTicks = 0;
            MouseAimHelper.clearAimRate();
            RotationManager.requestReturn();
            return;
        }

        if (target != lastTarget) {
            lastTarget = target;
            aimTicks = 0;
            startAimMult = 0.2 + Math.random() * 0.3;
            midAimMult = 0.8 + Math.random() * 0.4;
            endAimMult = 0.3 + Math.random() * 0.3;
        }

        aimTicks++;
        double tAim = Math.min(aimTicks / 20.0, 1.0);
        double aimBezierMult = (1.0 - tAim) * (1.0 - tAim) * startAimMult + 2.0 * (1.0 - tAim) * tAim * midAimMult + tAim * tAim * endAimMult;

        Vec3 targetPos = target.position().add(0, target.getBbHeight() / 2.0f, 0);
        float[] rotations = getRotationsTo(targetPos);

        if (silent) {
            float scaledSpeed = turnSpeed * (float) aimBezierMult;
            if (scaledSpeed < 1.0f) scaledSpeed = 1.0f;
            if (rotMode.equalsIgnoreCase("Bypass")) {
                RotationManager.keepRotated(rotations[0], rotations[1], scaledSpeed, moveCorrect);
            } else {
                RotationManager.keepRotatedSmooth(rotations[0], rotations[1], scaledSpeed, moveCorrect);
            }
            MouseAimHelper.clearAimRate();
        } else {
            double deltaYaw = Mth.wrapDegrees(rotations[0] - mc.player.getYRot());
            double toRotateYaw = Math.min(Math.abs(deltaYaw), turnSpeed * aimBezierMult);
            toRotateYaw = Math.copySign(toRotateYaw, deltaYaw);

            double deltaPitch = Mth.wrapDegrees(rotations[1] - mc.player.getXRot());
            double toRotatePitch = Math.min(Math.abs(deltaPitch), turnSpeed * aimBezierMult);
            toRotatePitch = Math.copySign(toRotatePitch, deltaPitch);

            MouseAimHelper.setAimRate(toRotateYaw, toRotatePitch);
            RotationManager.requestReturn();
        }
    }

    public void postSendPosition() {
        if (mc.player == null || mc.level == null || target == null) return;
        if (mc.player.isDeadOrDying()) return;
        if (mc.player.isBlocking()) return;

        double range = getSettingDouble("Range");
        boolean modernMode = getSettingCombo("Combat System").equalsIgnoreCase("Modern (1.9+)");
        boolean critOnly = getSettingBoolean("Criticals Only");
        boolean silent = getSettingBoolean("Silent");

        if (!isWeaponReady(modernMode)) return;

        if (critOnly) {
            if (mc.player.onGround() || mc.player.fallDistance <= 0.0f || mc.player.isPassenger() || mc.player.isInWater() || mc.player.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS)) {
                return;
            }
        }

        double attackRange = range;
        if (mc.player.distanceTo(target) > attackRange) return;

        Vec3 eyePos = mc.player.getEyePosition();
        Vec3 targetCenter = target.position().add(0, target.getBbHeight() / 2.0f, 0);
        if (!RotationManager.hasLineOfSight(eyePos, targetCenter)) return;

        float currentYaw = silent ? RotationManager.getServerYaw() : mc.player.getYRot();
        float currentPitch = silent ? RotationManager.getServerPitch() : mc.player.getXRot();

        Vec3 from = mc.player.getEyePosition();
        Vec3 looking = Vec3.directionFromRotation(currentPitch, currentYaw);
        Vec3 to = from.add(looking.scale(attackRange + 0.5));
        AABB aabb = mc.player.getBoundingBox().expandTowards(looking.scale(attackRange + 0.5)).inflate(1.0D);

        EntityHitResult hitResult = ProjectileUtil.getEntityHitResult(mc.player, from, to, aabb, (e) -> e == target, attackRange * attackRange);

        if (hitResult != null && hitResult.getEntity() == target) {
            mc.player.swing(InteractionHand.MAIN_HAND);
            mc.gameMode.attack(mc.player, target);
            lastAttackTime = System.currentTimeMillis();

            if (!modernMode) {
                double minCPS = getSettingDouble("1.8.9 Min CPS");
                double maxCPS = getSettingDouble("1.8.9 Max CPS");
                double cps = minCPS + (Math.random() * (maxCPS - minCPS));
                nextDelay = (long) (1000.0 / cps);
            } else {
                mc.player.resetAttackStrengthTicker();
            }
        }
    }

    private int readyTicks = 0;

    private boolean isWeaponReady(boolean modern) {
        if (modern) {
            if (mc.player.getAttackStrengthScale(0.0f) >= 1.0f) {
                readyTicks++;
            } else {
                readyTicks = 0;
            }
            float cooldownDelay = (float) getSettingDouble("Modern Delay (Ticks)");
            return readyTicks > cooldownDelay;
        } else {
            return System.currentTimeMillis() - lastAttackTime >= nextDelay;
        }
    }

    private LivingEntity getBestTarget(double range) {
        boolean targetPlayers = getSettingBoolean("Target Players");
        boolean targetMobs = getSettingBoolean("Target Mobs");
        boolean targetAnimals = getSettingBoolean("Target Animals");

        Vec3 eyePos = mc.player.getEyePosition();

        List<Entity> entities = mc.level.getEntities(mc.player, mc.player.getBoundingBox().inflate(range, range, range), entity -> {
            if (!(entity instanceof LivingEntity le)) return false;
            if (!entity.isAlive()) return false;
            if (mc.player.distanceTo(entity) > range) return false;
            if (com.eclipseware.imnotcheatingyouare.client.utils.TargetFilterManager.isFiltered(entity)) return false;

            Vec3 entityCenter = entity.position().add(0, le.getBbHeight() / 2.0f, 0);
            if (!RotationManager.hasLineOfSight(eyePos, entityCenter)) return false;

            if (entity instanceof Player player) {
                if (!targetPlayers) return false;
                if (player.isSpectator() || player.isCreative()) return false;
                if (FriendManager.isFriend(player.getGameProfile().name())) return false;
            } else {
                Module npcMod = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("NPC");
                if (npcMod == null || !npcMod.isToggled()) return false;
                
                if (entity instanceof Enemy) {
                    if (!targetMobs) return false;
                } else if (entity instanceof Animal) {
                    if (!targetAnimals) return false;
                } else {
                    if (!targetMobs) return false;
                }
            }
            return true;
        });

        if (entities.isEmpty()) return null;

        entities.sort(Comparator.comparingDouble(e -> mc.player.distanceTo(e)));
        return (LivingEntity) entities.get(0);
    }

    private float[] getRotationsTo(Vec3 targetPos) {
        Vec3 eyePos = mc.player.getEyePosition();
        double dX = targetPos.x - eyePos.x;
        double dY = targetPos.y - eyePos.y;
        double dZ = targetPos.z - eyePos.z;

        double dist = Math.sqrt(dX * dX + dZ * dZ);
        float yaw = (float) (Math.toDegrees(Math.atan2(dZ, dX)) - 90.0F);
        float pitch = (float) (-Math.toDegrees(Math.atan2(dY, dist)));

        return new float[]{yaw, pitch};
    }

    private double getSettingDouble(String name) {
        for (Setting s : ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingsByMod(this)) {
            if (s.getName().equals(name)) return s.getValDouble();
        }
        return 0;
    }

    private boolean getSettingBoolean(String name) {
        for (Setting s : ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingsByMod(this)) {
            if (s.getName().equals(name)) return s.getValBoolean();
        }
        return false;
    }

    private String getSettingCombo(String name) {
        for (Setting s : ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingsByMod(this)) {
            if (s.getName().equals(name)) return s.getValString();
        }
        return "";
    }
}
