package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.setting.SettingsManager;
import com.eclipseware.imnotcheatingyouare.client.utils.ModuleUtils;
import com.eclipseware.imnotcheatingyouare.client.utils.MouseAimHelper;
import com.eclipseware.imnotcheatingyouare.client.utils.RotationManager;
import java.util.ArrayList;
import java.util.Random;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

public class PearlCatch extends Module {
    private enum Stage {
        IDLE,
        AIMING_PEARL,
        THROWING_PEARL,
        AIMING_CHARGE,
        WAITING_CHARGE,
        FINISHING
    }

    private static final int MAX_STAGE_TICKS = 40;
    private static final float AIM_TOLERANCE_DEG = 0.85f;

    private final Random random = new Random();
    private Stage stage = Stage.IDLE;
    private PearlCatchPlanner.InterceptPlan activePlan = null;
    private int savedSlot = -1;
    private int stageTicks = 0;
    private int ticksSincePearl = 0;

    private float goalYaw = 0.0f;
    private float goalPitch = 0.0f;
    private int aimStepCount = 0;
    private double initialAngDist = 20.0;
    private double flickBaseSpeed = 55.0;
    private double startMult = 0.48;
    private double midMult = 1.12;
    private double endMult = 0.58;
    private float arcSign = 1.0f;

    public PearlCatch() {
        super("PearlCatch", Category.Mace, "Throws an ender pearl and intercepts it mid-air with a wind charge (straight up or diagonal).");
        ImnotcheatingyouareClient client = ImnotcheatingyouareClient.INSTANCE;
        if (client == null || client.settingsManager == null) {
            return;
        }
        SettingsManager sm = client.settingsManager;
        ArrayList<String> aims = new ArrayList<>();
        aims.add("Diagonal");
        aims.add("Straight Up");
        sm.rSetting(new Setting("Aim", this, "Diagonal", aims));
        sm.rSetting(new Setting("Charge Delay", this, 2.0, 0.0, 10.0, true));
        sm.rSetting(new Setting("Turn Speed", this, 45.0, 75.0, 5.0, 180.0, false));
        sm.rSetting(new Setting("Speed Curve", this, 0.25, 0.10, 0.25, 1.00));
        sm.rSetting(new Setting("Arc Curvature (%)", this, 45.0, 0.0, 100.0, true));
        sm.rSetting(new Setting("Silent Aim", this, true));
        sm.rSetting(new Setting("Movement Correction", this, true));
    }

    @Override
    public void onEnable() {
        startCatchSequence();
    }

    @Override
    public void onDisable() {
        cleanupSequence();
    }

    @Override
    public boolean needsTick() {
        return stage != Stage.IDLE;
    }

    private void startCatchSequence() {
        if (mc == null || mc.player == null || mc.level == null || mc.gameMode == null || mc.getConnection() == null) {
            abortSequence();
            return;
        }
        LocalPlayer player = mc.player;
        if (!hasUsableItem(player, Items.ENDER_PEARL) || !hasUsableItem(player, Items.WIND_CHARGE)) {
            abortSequence();
            return;
        }
        ItemCooldowns cooldowns = player.getCooldowns();
        ItemStack defaultPearl = Items.ENDER_PEARL.getDefaultInstance();
        ItemStack defaultCharge = Items.WIND_CHARGE.getDefaultInstance();
        if (cooldowns.isOnCooldown(defaultPearl) || cooldowns.isOnCooldown(defaultCharge)) {
            abortSequence();
            return;
        }

        int minDelay = (int) Math.round(getDouble("Charge Delay", 2.0));
        Vec3 pos = player.position();
        double eyeHeight = player.getEyeHeight();
        Vec3 vel = player.getDeltaMovement();
        boolean grounded = player.onGround();
        float lookYaw = player.getYRot();
        float lookPitch = player.getXRot();

        Setting aimSetting = setting("Aim");
        String aimMode = aimSetting != null ? aimSetting.getValString() : "Diagonal";
        boolean upward = "Straight Up".equalsIgnoreCase(aimMode);

        activePlan = upward
            ? PearlCatchPlanner.findUpwardPlan(pos, eyeHeight, vel, grounded, lookYaw, minDelay)
            : PearlCatchPlanner.findDiagonalPlan(mc.level, player, pos, eyeHeight, vel, grounded, lookYaw, lookPitch, minDelay);

        if (activePlan == null) {
            abortSequence();
            return;
        }

        savedSlot = ModuleUtils.getSelectedSlot();
        stageTicks = 0;
        ticksSincePearl = 0;
        stage = Stage.AIMING_PEARL;
        beginAimTarget(activePlan.pearlYaw(), activePlan.pearlPitch());
        stepRotation(0);
    }

    @Override
    public void onTick() {
        if (stage == Stage.IDLE) {
            return;
        }
        if (mc == null || mc.player == null || mc.level == null || mc.gameMode == null || activePlan == null) {
            abortSequence();
            return;
        }

        stageTicks++;
        if (stage == Stage.AIMING_CHARGE || stage == Stage.WAITING_CHARGE) {
            ticksSincePearl++;
        }
        if (stageTicks > MAX_STAGE_TICKS && stage != Stage.WAITING_CHARGE) {
            abortSequence();
            return;
        }

        switch (stage) {
            case AIMING_PEARL -> {
                stepRotation(0);
                if (isAimReady()) {
                    stage = Stage.THROWING_PEARL;
                }
            }
            case THROWING_PEARL -> {
                stepRotation(0);
                if (!throwProjectile(Items.ENDER_PEARL)) {
                    abortSequence();
                    return;
                }
                stage = Stage.AIMING_CHARGE;
                stageTicks = 0;
                ticksSincePearl = 0;
                beginAimTarget(activePlan.chargeYaw(), activePlan.chargePitch());
                int budget = activePlan.absoluteChargeTiming() ? Math.max(1, activePlan.chargeTick()) : 0;
                stepRotation(budget);
                if (activePlan.chargeTick() == 0 && isAimReady()) {
                    throwProjectile(Items.WIND_CHARGE);
                    stage = Stage.FINISHING;
                }
            }
            case AIMING_CHARGE -> {
                int remainingBudget = activePlan.absoluteChargeTiming()
                    ? Math.max(1, activePlan.chargeTick() - ticksSincePearl)
                    : 0;
                stepRotation(remainingBudget);
                if (isAimReady()) {
                    boolean readyNow = activePlan.absoluteChargeTiming()
                        ? ticksSincePearl >= activePlan.chargeTick()
                        : activePlan.chargeTick() <= 0;
                    if (readyNow) {
                        throwProjectile(Items.WIND_CHARGE);
                        stage = Stage.FINISHING;
                    } else {
                        stage = Stage.WAITING_CHARGE;
                        stageTicks = 0;
                    }
                } else if (activePlan.absoluteChargeTiming() && ticksSincePearl >= activePlan.chargeTick()) {
                    abortSequence();
                }
            }
            case WAITING_CHARGE -> {
                stepRotation(1);
                boolean ready = activePlan.absoluteChargeTiming()
                    ? ticksSincePearl >= activePlan.chargeTick()
                    : stageTicks >= activePlan.chargeTick();
                if (ready) {
                    throwProjectile(Items.WIND_CHARGE);
                    stage = Stage.FINISHING;
                }
            }
            case FINISHING -> abortSequence();
            default -> {
            }
        }
    }

    private void beginAimTarget(float yaw, float pitch) {
        goalYaw = yaw;
        goalPitch = pitch;
        aimStepCount = 0;
        LocalPlayer player = mc.player;
        boolean silent = getBool("Silent Aim", true);
        float curYaw = silent && RotationManager.isActive() ? RotationManager.getServerYaw() : (player != null ? player.getYRot() : yaw);
        float curPitch = silent && RotationManager.isActive() ? RotationManager.getServerPitch() : (player != null ? player.getXRot() : pitch);
        initialAngDist = Math.max(8.0, Math.hypot(Mth.wrapDegrees(yaw - curYaw), pitch - curPitch));

        Setting speedRange = setting("Turn Speed");
        double low = speedRange != null && speedRange.isRange() ? speedRange.getRangeLow() : 45.0;
        double high = speedRange != null && speedRange.isRange() ? speedRange.getRangeHigh() : 75.0;
        double sample = (random.nextDouble() + random.nextDouble()) * 0.5;
        flickBaseSpeed = low + (high - low) * sample;
        startMult = 0.42 + random.nextDouble() * 0.22;
        midMult = 1.00 + random.nextDouble() * 0.30;
        endMult = 0.50 + random.nextDouble() * 0.24;
        arcSign = random.nextBoolean() ? 1.0f : -1.0f;
    }

    private void stepRotation(int deadlineTicks) {
        LocalPlayer player = mc.player;
        if (player == null) {
            return;
        }
        aimStepCount++;
        boolean silent = getBool("Silent Aim", true);
        boolean moveCorrect = getBool("Movement Correction", true);

        float curYaw = silent && RotationManager.isActive() ? RotationManager.getServerYaw() : player.getYRot();
        float curPitch = silent && RotationManager.isActive() ? RotationManager.getServerPitch() : player.getXRot();
        float yawErr = Mth.wrapDegrees(goalYaw - curYaw);
        float pitchErr = goalPitch - curPitch;
        double angDist = Math.hypot(yawErr, pitchErr);

        double distProgress = 1.0 - Mth.clamp(angDist / Math.max(8.0, initialAngDist), 0.0, 1.0);
        double timeProgress = Math.min(aimStepCount / 12.0, 1.0);
        double flickProgress = Mth.clamp(distProgress * 0.72 + timeProgress * 0.28, 0.0, 1.0);

        Setting curveSetting = setting("Speed Curve");
        double[] curve = curveSetting != null && curveSetting.isCurve()
            ? curveSetting.getCurve()
            : new double[]{0.25, 0.10, 0.25, 1.00};
        double arcPct = getDouble("Arc Curvature (%)", 45.0) / 100.0;
        float arcPeak = deadlineTicks == 1
            ? 0.0f
            : (float) (Mth.clamp(initialAngDist * 0.12, 1.0, 9.0) * arcPct);
        double tickJitter = 0.92 + random.nextDouble() * 0.16;

        float stepDeg = PearlGrapplePhysics.computeHumanizedTurnSpeed(
            flickBaseSpeed,
            angDist,
            60.0,
            curve,
            flickProgress,
            startMult,
            midMult,
            endMult,
            tickJitter
        );
        if (deadlineTicks > 0) {
            float minRequired = (float) ((angDist / deadlineTicks) + 2.5);
            stepDeg = Math.max(stepDeg, minRequired);
        }

        float[] waypoint = PearlGrapplePhysics.computeCurvedWaypoint(
            curYaw,
            curPitch,
            goalYaw,
            goalPitch,
            flickProgress,
            arcPeak,
            arcSign
        );

        if (silent) {
            MouseAimHelper.clearAimRate();
            RotationManager.keepRotated(waypoint[0], waypoint[1], stepDeg, moveCorrect);
            return;
        }
        if (RotationManager.isActive()) {
            RotationManager.requestReturn();
        }
        double dyaw = Mth.wrapDegrees(waypoint[0] - player.getYRot());
        double dpitch = waypoint[1] - player.getXRot();
        MouseAimHelper.setAimRate(
            Math.copySign(Math.min(Math.abs(dyaw), stepDeg), dyaw),
            Math.copySign(Math.min(Math.abs(dpitch), stepDeg * 0.88), dpitch)
        );
    }

    private boolean isAimReady() {
        LocalPlayer player = mc.player;
        if (player == null) {
            return false;
        }
        boolean silent = getBool("Silent Aim", true);
        float curYaw = silent && RotationManager.isActive() ? RotationManager.getServerYaw() : player.getYRot();
        float curPitch = silent && RotationManager.isActive() ? RotationManager.getServerPitch() : player.getXRot();
        float gcd = Math.max(0.15f, RotationManager.getGCD());
        float tol = Math.max(AIM_TOLERANCE_DEG, gcd * 2.2f);
        return Math.abs(Mth.wrapDegrees(goalYaw - curYaw)) <= tol && Math.abs(goalPitch - curPitch) <= tol;
    }

    private boolean throwProjectile(Item item) {
        LocalPlayer player = mc.player;
        if (player == null || mc.gameMode == null) {
            return false;
        }
        MouseAimHelper.clearAimRate();
        ItemStack offhand = player.getOffhandItem();
        if (offhand.is(item)) {
            mc.gameMode.useItem(player, InteractionHand.OFF_HAND);
            ModuleUtils.swingHand(player, InteractionHand.OFF_HAND);
            return true;
        }
        int slot = ModuleUtils.findItemInHotbar(item);
        if (slot == -1) {
            return false;
        }
        if (ModuleUtils.getSelectedSlot() != slot) {
            ModuleUtils.switchToSlot(slot);
        }
        mc.gameMode.useItem(player, InteractionHand.MAIN_HAND);
        ModuleUtils.swingHand(player, InteractionHand.MAIN_HAND);
        return true;
    }

    private boolean hasUsableItem(LocalPlayer player, Item item) {
        ItemStack offhand = player.getOffhandItem();
        return offhand.is(item) || ModuleUtils.findItemInHotbar(item) != -1;
    }

    private void abortSequence() {
        if (isToggled()) {
            toggle();
        } else {
            cleanupSequence();
        }
    }

    private void cleanupSequence() {
        MouseAimHelper.clearAimRate();
        if (savedSlot >= 0 && savedSlot < 9 && mc != null && mc.player != null) {
            ModuleUtils.switchToSlot(savedSlot);
        }
        savedSlot = -1;
        activePlan = null;
        stage = Stage.IDLE;
        stageTicks = 0;
        ticksSincePearl = 0;
        if (RotationManager.isActive()) {
            RotationManager.requestReturn();
        }
    }

    private double getDouble(String name, double fallback) {
        Setting s = setting(name);
        return s != null ? s.getValDouble() : fallback;
    }

    private boolean getBool(String name, boolean fallback) {
        Setting s = setting(name);
        return s != null ? s.getValBoolean() : fallback;
    }

    private Setting setting(String name) {
        ImnotcheatingyouareClient client = ImnotcheatingyouareClient.INSTANCE;
        if (client == null || client.settingsManager == null) {
            return null;
        }
        SettingsManager sm = client.settingsManager;
        return sm.getSettingByName(this, name);
    }
}
