package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.utils.FriendManager;
import com.eclipseware.imnotcheatingyouare.client.utils.ModuleUtils;
import com.eclipseware.imnotcheatingyouare.client.utils.RotationManager;
import com.eclipseware.imnotcheatingyouare.client.utils.FontUtils;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.EggItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SnowballItem;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

public final class BowAimbot extends Module {
    private Entity target;
    private float velocity;
    private int hotbarSwapTicks = 0;
    private int lastSlot = -1;
    private float aimProgress = 0.0f;
    private float startYaw = 0.0f;
    private float startPitch = 0.0f;
    private Entity lastTarget = null;

    public BowAimbot() {
        super("BowAimbot", Category.Combat, "Aims automatically at targets when using projectile weapons.");
        
        ArrayList<String> priorityModes = new ArrayList<>(Arrays.asList("Angle+Dist", "Distance", "Angle", "Health"));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Priority", this, "Angle+Dist", priorityModes));

        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Predict", this, true));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Predict Strength", this, 1.0, 0.0, 2.0, false));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Range", this, 40.0, 5.0, 100.0, false));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Silent", this, true));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Auto Fire", this, false));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Smooth Ticks", this, 8.0, 1.0, 30.0, false));

        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Players", this, true));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Hostile Mobs", this, true));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Passive Mobs", this, false));
    }

    @Override
    public void onDisable() {
        target = null;
        RotationManager.requestReturn();
        hotbarSwapTicks = 0;
        lastSlot = -1;
        aimProgress = 0.0f;
        startYaw = 0.0f;
        startPitch = 0.0f;
        lastTarget = null;
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.level == null) {
            target = null;
            return;
        }

        int currentSlot = mc.player.getInventory().getSelectedSlot();
        if (currentSlot != lastSlot) {
            hotbarSwapTicks = 2;
            lastSlot = currentSlot;
        }

        if (hotbarSwapTicks > 0) {
            hotbarSwapTicks--;
            target = null;
            return;
        }

        ItemStack heldItem = mc.player.getMainHandItem();
        Item item = heldItem.getItem();
        boolean isBow = item instanceof BowItem;
        boolean isCrossbow = item instanceof CrossbowItem;
        boolean isSnowball = item instanceof SnowballItem;
        boolean isEgg = item instanceof EggItem;

        if (!isBow && !isCrossbow && !isSnowball && !isEgg) {
            target = null;
            return;
        }

        if (isBow && !mc.options.keyUse.isDown() && !mc.player.isUsingItem()) {
            target = null;
            return;
        }

        if (isCrossbow && !CrossbowItem.isCharged(heldItem)) {
            target = null;
            return;
        }

        updateTarget();

        boolean silent = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Silent").getValBoolean();

        if (target == null) {
            aimProgress = 0.0f;
            lastTarget = null;
            RotationManager.requestReturn();
            return;
        }

        if (target != lastTarget || aimProgress == 0.0f) {
            lastTarget = target;
            startYaw = silent ? RotationManager.getServerYaw() : mc.player.getYRot();
            startPitch = silent ? RotationManager.getServerPitch() : mc.player.getXRot();
            if (Float.isNaN(startYaw)) {
                startYaw = mc.player.getYRot();
            }
            if (Float.isNaN(startPitch)) {
                startPitch = mc.player.getXRot();
            }
            aimProgress = 0.0f;
        }

        float projectileGravity = 0.05f;
        if (isSnowball || isEgg) {
            projectileGravity = 0.03f;
            velocity = 1.5f;
        } else if (isCrossbow) {
            projectileGravity = 0.05f;
            velocity = 3.15f;
        } else {
            projectileGravity = 0.05f;
            float charge = (float) (72000 - mc.player.getUseItemRemainingTicks()) / 20.0f;
            charge = (charge * charge + charge * 2.0f) / 3.0f;
            if (charge > 1.0f) {
                charge = 1.0f;
            }
            velocity = charge * 3.0f;
        }

        double strength = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Predict Strength").getValDouble();
        boolean predict = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Predict").getValBoolean();
        double d = 0.0;
        if (predict && velocity > 0.1f) {
            d = (mc.player.distanceTo(target) / velocity) * strength;
        }

        double targetX = target.getX() + (target.getX() - target.xo) * d;
        double targetY = target.getY() + (target.getY() - target.yo) * d + target.getBbHeight() * 0.5;
        double targetZ = target.getZ() + (target.getZ() - target.zo) * d;

        double deltaX = targetX - mc.player.getX();
        double deltaY = targetY - (mc.player.getY() + mc.player.getEyeHeight());
        double deltaZ = targetZ - mc.player.getZ();

        float neededYaw = (float) Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0f;
        double horizontalDist = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

        float neededPitch = calculatePitch(horizontalDist, deltaY, velocity, projectileGravity);

        if (Float.isNaN(neededPitch)) {
            neededPitch = (float) -Math.toDegrees(Math.atan2(deltaY, horizontalDist));
        }

        float gcd = RotationManager.getGCD();
        if (gcd < 0.001f) {
            gcd = 0.15f;
        }

        double smoothTicks = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Smooth Ticks").getValDouble();
        float step = (float) (1.0 / Math.max(1.0, smoothTicks));
        aimProgress = Math.min(1.0f, aimProgress + step);
        float t = aimProgress * aimProgress * (3.0f - 2.0f * aimProgress);

        float targetYaw = startYaw + Mth.wrapDegrees(neededYaw - startYaw) * t;
        float targetPitch = startPitch + (neededPitch - startPitch) * t;

        float yawDiff = Mth.wrapDegrees(targetYaw - startYaw);
        float pitchDiff = targetPitch - startPitch;

        int yawSteps = Math.round(yawDiff / gcd);
        int pitchSteps = Math.round(pitchDiff / gcd);

        float currentYaw = startYaw + yawSteps * gcd;
        float currentPitch = Mth.clamp(startPitch + pitchSteps * gcd, -90.0f, 90.0f);

        if (silent) {
            RotationManager.queueRotation(currentYaw, currentPitch, 0, 0, 0, true, null);
        } else {
            mc.player.setYRot(currentYaw);
            mc.player.setXRot(currentPitch);
        }

        boolean autoFire = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Auto Fire").getValBoolean();
        if (autoFire) {
            float yawDiff2 = Math.abs(Mth.wrapDegrees(neededYaw - currentYaw));
            float pitchDiff2 = Math.abs(neededPitch - currentPitch);

            if (yawDiff2 <= 3.0f && pitchDiff2 <= 3.0f) {
                if (isBow) {
                    if (velocity >= 3.0f) {
                        mc.gameMode.releaseUsingItem(mc.player);
                    }
                } else if (isCrossbow) {
                    mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
                } else {
                    mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
                }
            }
        }
    }

    @Override
    public void onRenderHUD(GuiGraphicsExtractor guiGraphics, Object tickDelta) {
        if (target == null) return;

        String message;
        if (velocity < 3.0f && mc.player.getMainHandItem().getItem() instanceof BowItem) {
            message = "Charging: " + (int) ((velocity / 3.0f) * 100.0f) + "%";
        } else {
            message = "Target Locked";
        }

        int width = FontUtils.width(message);
        int scaledWidth = mc.getWindow().getGuiScaledWidth();
        int scaledHeight = mc.getWindow().getGuiScaledHeight();

        int x1 = scaledWidth / 2 - width / 2;
        int x2 = x1 + width + 4;
        int y1 = scaledHeight / 2 + 10;
        int y2 = y1 + 10;

        guiGraphics.fill(x1, y1, x2, y2, 0x80000000);
        FontUtils.drawString(guiGraphics, message, x1 + 2, y1 + 1, 0xFFFFFFFF, false);
    }

    private float calculatePitch(double x, double y, float v, float g) {
        double vSq = v * v;
        double vPow4 = vSq * vSq;
        double root = vPow4 - g * (g * x * x + 2.0 * y * vSq);
        if (root < 0.0) {
            return Float.NaN;
        }
        double angle = Math.atan((vSq - Math.sqrt(root)) / (g * x));
        return (float) -Math.toDegrees(angle);
    }

    private void updateTarget() {
        String priority = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Priority").getValString();
        double range = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Range").getValDouble();

        Entity bestTarget = null;
        double bestVal = Double.MAX_VALUE;

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (isValidTarget(entity, range)) {
                double val = 0.0;
                if ("Distance".equals(priority)) {
                    val = mc.player.distanceTo(entity);
                } else if ("Angle".equals(priority)) {
                    val = getAngleToLookVec(entity.position().add(0.0, entity.getBbHeight() * 0.5, 0.0));
                } else if ("Angle+Dist".equals(priority)) {
                    val = Math.pow(getAngleToLookVec(entity.position().add(0.0, entity.getBbHeight() * 0.5, 0.0)), 2.0) + mc.player.distanceToSqr(entity);
                } else if ("Health".equals(priority) && entity instanceof LivingEntity le) {
                    val = le.getHealth();
                } else {
                    val = Integer.MAX_VALUE;
                }

                if (val < bestVal) {
                    bestTarget = entity;
                    bestVal = val;
                }
            }
        }
        target = bestTarget;
    }

    private double getAngleToLookVec(Vec3 targetVec) {
        Vec3 lookVec = mc.player.getViewVector(1.0f);
        Vec3 diffVec = targetVec.subtract(mc.player.getEyePosition()).normalize();
        double dot = lookVec.dot(diffVec);
        dot = Mth.clamp(dot, -1.0, 1.0);
        return Math.toDegrees(Math.acos(dot));
    }

    private boolean isValidTarget(Entity entity, double range) {
        if (!(entity instanceof LivingEntity) || entity == mc.player || !entity.isAlive()) {
            return false;
        }
        if (com.eclipseware.imnotcheatingyouare.client.utils.TargetFilterManager.isFiltered(entity)) {
            return false;
        }
        if (mc.player.distanceTo(entity) > range) {
            return false;
        }
        if (entity instanceof Player p) {
            if (FriendManager.isFriend(p)) {
                return false;
            }
            if (Teams.isTeam(p)) {
                return false;
            }
            Setting playersSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Players");
            if (playersSetting != null && !playersSetting.getValBoolean()) {
                return false;
            }
        } else if (isHostileMob(entity)) {
            Setting hostileSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Hostile Mobs");
            if (hostileSetting != null && !hostileSetting.getValBoolean()) {
                return false;
            }
        } else {
            Setting passiveSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Passive Mobs");
            if (passiveSetting != null && !passiveSetting.getValBoolean()) {
                return false;
            }
        }
        return true;
    }

    private boolean isHostileMob(Entity entity) {
        if (entity instanceof Enemy || entity instanceof net.minecraft.world.entity.monster.Monster) {
            return true;
        }
        String name = entity.getType().getDescriptionId().toLowerCase();
        return name.contains("slime") || name.contains("shulker") || name.contains("ghast") || name.contains("dragon");
    }
}
