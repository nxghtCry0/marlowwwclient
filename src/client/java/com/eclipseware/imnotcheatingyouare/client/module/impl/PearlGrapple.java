package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.setting.SettingsManager;
import com.eclipseware.imnotcheatingyouare.client.utils.ModuleUtils;
import com.eclipseware.imnotcheatingyouare.client.utils.MouseAimHelper;
import com.eclipseware.imnotcheatingyouare.client.utils.RotationManager;
import com.eclipseware.imnotcheatingyouare.client.utils.TargetFilterManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

public class PearlGrapple extends Module {
    private enum State {
        ACQUIRING_PEARL,
        AIMING,
        THROW_DELAY,
        RESETTING
    }

    private record CandidatePearl(ThrownEnderpearl pearl, Player owner, Vec3 landingPos) {
    }

    private static final float OWNER_ATTRIBUTION_RADIUS = 10.0f;
    private static final long STATE_TIMEOUT_MS = 5000L;
    private static final float AIM_TOLERANCE_DEG = 1.45f;

    private static final Map<Integer, UUID> PEARL_OWNER_BY_ENTITY_ID = new HashMap<>();
    private static ClientLevel lastTrackedLevel = null;

    private final Random random = new Random();
    private final Set<Integer> handledPearlIds = new HashSet<>();
    private State state = State.ACQUIRING_PEARL;
    private int lockedPearlId = -1;
    private UUID lockedOwnerUuid = null;
    private Vec3 lockedLandingPos = null;
    private float targetYaw = 0.0f;
    private float targetPitch = 0.0f;
    private int savedHotbarSlot = -1;
    private long stateStartedMs = 0L;
    private long throwReadyMs = 0L;
    private long lastPearlThrowMs = 0L;

    private int aimTicks = 0;
    private double initialAngularDist = 25.0;
    private double flickBaseSpeed = 38.0;
    private double startAimMult = 0.45;
    private double midAimMult = 1.10;
    private double endAimMult = 0.55;
    private float arcSign = 1.0f;

    public PearlGrapple() {
        super("PearlGrapple", Category.Utility, "Tracks enemy pearl trajectories and throws a pearl at their landing spot (AutoPearl).");
        ImnotcheatingyouareClient client = ImnotcheatingyouareClient.INSTANCE;
        if (client == null || client.settingsManager == null) {
            return;
        }
        SettingsManager sm = client.settingsManager;
        ArrayList<String> modes = new ArrayList<>();
        modes.add("On Bind");
        modes.add("Aggro");
        sm.rSetting(new Setting("Mode", this, "On Bind", modes));
        sm.rSetting(new Setting("Turn Speed", this, 28.0, 48.0, 5.0, 140.0, false));
        sm.rSetting(new Setting("Speed Curve", this, 0.25, 0.10, 0.25, 1.00));
        sm.rSetting(new Setting("Curve Range", this, 45.0, 10.0, 180.0, true));
        sm.rSetting(new Setting("Arc Curvature (%)", this, 60.0, 0.0, 100.0, true));
        sm.rSetting(new Setting("Angle Limit", this, 180.0, 30.0, 360.0, true));
        sm.rSetting(new Setting("Distance Limit", this, 6.0, 0.0, 20.0, false));
        sm.rSetting(new Setting("Min Health", this, 5.0, 1.0, 20.0, true));
        sm.rSetting(new Setting("Pearl Cooldown (s)", this, 1.0, 0.0, 15.0, false));
        sm.rSetting(new Setting("Throw Delay (ms)", this, 50.0, 0.0, 250.0, true));
        sm.rSetting(new Setting("Vertical Check", this, false));
        sm.rSetting(new Setting("Silent Aim", this, true));
        sm.rSetting(new Setting("Movement Correction", this, true));
        sm.rSetting(new Setting("Limit To Items", this, false));
    }

    public static void onWorldTick(Minecraft client) {
        if (client == null || client.level == null || client.player == null) {
            PEARL_OWNER_BY_ENTITY_ID.clear();
            lastTrackedLevel = null;
            return;
        }
        ClientLevel level = client.level;
        if (level != lastTrackedLevel) {
            PEARL_OWNER_BY_ENTITY_ID.clear();
            lastTrackedLevel = level;
        }

        List<Player> players = new ArrayList<>();
        Set<Integer> livePearlIds = new HashSet<>();
        List<ThrownEnderpearl> pearls = new ArrayList<>();

        for (Entity entity : level.entitiesForRendering()) {
            if (entity instanceof Player p && p.isAlive()) {
                players.add(p);
            } else if (entity instanceof ThrownEnderpearl pearl && pearl.isAlive() && !pearl.isRemoved()) {
                livePearlIds.add(pearl.getId());
                pearls.add(pearl);
            }
        }

        Iterator<Map.Entry<Integer, UUID>> it = PEARL_OWNER_BY_ENTITY_ID.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, UUID> entry = it.next();
            if (!livePearlIds.contains(entry.getKey())) {
                it.remove();
            }
        }

        for (int i = 0; i < pearls.size(); i++) {
            ThrownEnderpearl pearl = pearls.get(i);
            int id = pearl.getId();
            if (PEARL_OWNER_BY_ENTITY_ID.containsKey(id)) {
                continue;
            }
            Entity directOwner = pearl.getOwner();
            if (directOwner instanceof Player ownerPlayer) {
                PEARL_OWNER_BY_ENTITY_ID.put(id, ownerPlayer.getUUID());
                continue;
            }
            Player nearest = findNearestPlayerWithin(pearl, players, OWNER_ATTRIBUTION_RADIUS);
            if (nearest != null) {
                PEARL_OWNER_BY_ENTITY_ID.put(id, nearest.getUUID());
            }
        }
    }

    private static Player findNearestPlayerWithin(ThrownEnderpearl pearl, List<Player> players, float maxDist) {
        Player best = null;
        float bestDist = maxDist;
        for (int i = 0; i < players.size(); i++) {
            Player candidate = players.get(i);
            float dist = candidate.distanceTo(pearl);
            if (dist <= bestDist) {
                bestDist = dist;
                best = candidate;
            }
        }
        return best;
    }

    @Override
    public void onEnable() {
        handledPearlIds.clear();
        clearLockState(false);
        stateStartedMs = System.currentTimeMillis();
    }

    @Override
    public void onDisable() {
        restoreSavedSlotIfNeeded();
        clearLockState(true);
    }

    @Override
    public boolean needsTick() {
        return state != State.ACQUIRING_PEARL || savedHotbarSlot != -1;
    }

    @Override
    public void onTick() {
        if (mc == null || mc.player == null || mc.level == null || mc.gameMode == null) {
            if (isToggled()) {
                toggle();
            } else {
                clearLockState(true);
            }
            return;
        }

        if (!isToggled() && state == State.RESETTING) {
            finishReset(false);
            return;
        }
        if (!isToggled()) {
            clearLockState(true);
            return;
        }

        long now = System.currentTimeMillis();
        if (state != State.ACQUIRING_PEARL && now - stateStartedMs > STATE_TIMEOUT_MS) {
            restoreSavedSlotIfNeeded();
            clearLockState(true);
            if (isOnBindMode() && isToggled()) {
                toggle();
            }
            return;
        }

        switch (state) {
            case ACQUIRING_PEARL -> tickAcquiring(now);
            case AIMING -> tickAiming(now);
            case THROW_DELAY -> tickThrowDelay(now);
            case RESETTING -> finishReset(true);
        }
    }

    private void tickAcquiring(long now) {
        List<CandidatePearl> candidates = collectValidCandidates();
        if (!candidates.isEmpty()) {
            CandidatePearl picked = candidates.get(0);
            ThrownEnderpearl pearl = picked.pearl();
            Player owner = picked.owner();
            PearlGrapplePhysics.ThrowSolution sol = PearlGrapplePhysics.solveThrow(mc.level, mc.player, picked.landingPos());
            if (sol != null) {
                lockedPearlId = pearl.getId();
                lockedOwnerUuid = owner.getUUID();
                lockedLandingPos = picked.landingPos();
                targetYaw = sol.yaw();
                targetPitch = sol.pitch();
                handledPearlIds.add(lockedPearlId);
                initializeHumanizedFlickProfile(targetYaw, targetPitch);
                state = State.AIMING;
                stateStartedMs = now;
                applyAimStep();
                return;
            }
        }

        if (isOnBindMode() && !isHoldMode() && isToggled()) {
            toggle();
        }
    }

    private void initializeHumanizedFlickProfile(float goalYaw, float goalPitch) {
        LocalPlayer player = mc.player;
        boolean silent = getBoolSetting("Silent Aim", true);
        float startYaw = silent && RotationManager.isActive() ? RotationManager.getServerYaw() : (player != null ? player.getYRot() : goalYaw);
        float startPitch = silent && RotationManager.isActive() ? RotationManager.getServerPitch() : (player != null ? player.getXRot() : goalPitch);
        float dyaw = Mth.wrapDegrees(goalYaw - startYaw);
        float dpitch = goalPitch - startPitch;
        initialAngularDist = Math.max(8.0, Math.hypot(dyaw, dpitch));
        aimTicks = 0;

        Setting speedRange = setting("Turn Speed");
        double low = speedRange != null && speedRange.isRange() ? speedRange.getRangeLow() : 28.0;
        double high = speedRange != null && speedRange.isRange() ? speedRange.getRangeHigh() : 48.0;
        double sample = (random.nextDouble() + random.nextDouble()) * 0.5;
        flickBaseSpeed = low + (high - low) * sample;

        startAimMult = 0.36 + random.nextDouble() * 0.24;
        midAimMult = 0.96 + random.nextDouble() * 0.34;
        endAimMult = 0.44 + random.nextDouble() * 0.24;
        arcSign = random.nextBoolean() ? 1.0f : -1.0f;
    }

    private void tickAiming(long now) {
        if (!isEngagementStillValid()) {
            abortActiveSequence();
            return;
        }

        refreshLockedTrajectory();
        PearlGrapplePhysics.ThrowSolution sol = PearlGrapplePhysics.solveThrow(mc.level, mc.player, lockedLandingPos);
        if (sol == null) {
            abortActiveSequence();
            return;
        }
        targetYaw = sol.yaw();
        targetPitch = sol.pitch();
        applyAimStep();

        if (isAimOnTarget()) {
            long delayMs = Math.round(getDoubleSetting("Throw Delay (ms)", 50.0));
            long jitteredDelay = delayMs > 0L ? delayMs + random.nextInt(22) : 0L;
            if (jitteredDelay <= 0L) {
                executePearlThrow(now);
            } else {
                state = State.THROW_DELAY;
                throwReadyMs = now + jitteredDelay;
            }
        }
    }

    private void tickThrowDelay(long now) {
        if (!isEngagementStillValid()) {
            abortActiveSequence();
            return;
        }

        refreshLockedTrajectory();
        PearlGrapplePhysics.ThrowSolution sol = PearlGrapplePhysics.solveThrow(mc.level, mc.player, lockedLandingPos);
        if (sol == null) {
            abortActiveSequence();
            return;
        }
        targetYaw = sol.yaw();
        targetPitch = sol.pitch();
        applyAimStep();

        if (now >= throwReadyMs && isAimOnTarget()) {
            executePearlThrow(now);
        }
    }

    private void executePearlThrow(long now) {
        LocalPlayer player = mc.player;
        if (player == null || mc.gameMode == null) {
            abortActiveSequence();
            return;
        }

        MouseAimHelper.clearAimRate();

        ItemStack offhand = player.getOffhandItem();
        if (offhand.is(Items.ENDER_PEARL)) {
            mc.gameMode.useItem(player, InteractionHand.OFF_HAND);
            ModuleUtils.swingHand(player, InteractionHand.OFF_HAND);
            lastPearlThrowMs = now;
            state = State.RESETTING;
            return;
        }

        int pearlSlot = ModuleUtils.findItemInHotbar(Items.ENDER_PEARL);
        if (pearlSlot == -1) {
            abortActiveSequence();
            return;
        }

        int currentSlot = ModuleUtils.getSelectedSlot();
        if (currentSlot != pearlSlot) {
            savedHotbarSlot = currentSlot;
            ModuleUtils.switchToSlot(pearlSlot);
        } else {
            savedHotbarSlot = -1;
        }

        mc.gameMode.useItem(player, InteractionHand.MAIN_HAND);
        ModuleUtils.swingHand(player, InteractionHand.MAIN_HAND);
        lastPearlThrowMs = now;
        state = State.RESETTING;
    }

    private void finishReset(boolean allowAutoDisable) {
        restoreSavedSlotIfNeeded();
        clearLockState(true);
        if (allowAutoDisable && isOnBindMode() && !isHoldMode() && isToggled()) {
            toggle();
        }
    }

    private void abortActiveSequence() {
        restoreSavedSlotIfNeeded();
        clearLockState(true);
        if (isOnBindMode() && !isHoldMode() && isToggled()) {
            toggle();
        }
    }

    private void restoreSavedSlotIfNeeded() {
        if (savedHotbarSlot >= 0 && savedHotbarSlot < 9 && mc != null && mc.player != null) {
            ModuleUtils.switchToSlot(savedHotbarSlot);
        }
        savedHotbarSlot = -1;
    }

    private void clearLockState(boolean releaseRotation) {
        MouseAimHelper.clearAimRate();
        if (releaseRotation && RotationManager.isActive()) {
            RotationManager.requestReturn();
        }
        state = State.ACQUIRING_PEARL;
        lockedPearlId = -1;
        lockedOwnerUuid = null;
        lockedLandingPos = null;
        throwReadyMs = 0L;
        aimTicks = 0;
    }

    private void refreshLockedTrajectory() {
        if (mc.level == null || lockedPearlId == -1) {
            return;
        }
        Entity entity = mc.level.getEntity(lockedPearlId);
        if (entity instanceof ThrownEnderpearl pearl && isPearlActive(pearl)) {
            Vec3 updated = PearlGrapplePhysics.predictLandingPosition(mc.level, pearl);
            if (updated != null) {
                lockedLandingPos = updated;
            }
        }
    }

    private void applyAimStep() {
        LocalPlayer player = mc.player;
        if (player == null) {
            return;
        }

        aimTicks++;
        boolean silent = getBoolSetting("Silent Aim", true);
        boolean moveCorrect = getBoolSetting("Movement Correction", true);

        float curYaw = silent && RotationManager.isActive() ? RotationManager.getServerYaw() : player.getYRot();
        float curPitch = silent && RotationManager.isActive() ? RotationManager.getServerPitch() : player.getXRot();
        float yawErr = Mth.wrapDegrees(targetYaw - curYaw);
        float pitchErr = targetPitch - curPitch;
        double angDist = Math.hypot(yawErr, pitchErr);

        double distProgress = 1.0 - Mth.clamp(angDist / Math.max(8.0, initialAngularDist), 0.0, 1.0);
        double timeProgress = Math.min(aimTicks / 16.0, 1.0);
        double flickProgress = Mth.clamp(distProgress * 0.72 + timeProgress * 0.28, 0.0, 1.0);

        Setting curveSetting = setting("Speed Curve");
        double[] speedCurve = curveSetting != null && curveSetting.isCurve()
            ? curveSetting.getCurve()
            : new double[]{0.25, 0.10, 0.25, 1.00};
        double curveRange = getDoubleSetting("Curve Range", 45.0);
        double arcPct = getDoubleSetting("Arc Curvature (%)", 60.0) / 100.0;
        float arcPeakDeg = (float) (Mth.clamp(initialAngularDist * 0.15, 1.5, 11.5) * arcPct);
        double tickJitter = 0.90 + random.nextDouble() * 0.20;

        float humanizedStepDeg = PearlGrapplePhysics.computeHumanizedTurnSpeed(
            flickBaseSpeed,
            angDist,
            curveRange,
            speedCurve,
            flickProgress,
            startAimMult,
            midAimMult,
            endAimMult,
            tickJitter
        );

        float[] waypoint = PearlGrapplePhysics.computeCurvedWaypoint(
            curYaw,
            curPitch,
            targetYaw,
            targetPitch,
            flickProgress,
            arcPeakDeg,
            arcSign
        );

        if (silent) {
            MouseAimHelper.clearAimRate();
            RotationManager.keepRotated(waypoint[0], waypoint[1], humanizedStepDeg, moveCorrect);
            return;
        }

        if (RotationManager.isActive()) {
            RotationManager.requestReturn();
        }
        double waypointYawDiff = Mth.wrapDegrees(waypoint[0] - player.getYRot());
        double waypointPitchDiff = waypoint[1] - player.getXRot();
        double rateYaw = Math.copySign(Math.min(Math.abs(waypointYawDiff), humanizedStepDeg), waypointYawDiff);
        double ratePitch = Math.copySign(Math.min(Math.abs(waypointPitchDiff), humanizedStepDeg * 0.85), waypointPitchDiff);
        MouseAimHelper.setAimRate(rateYaw, ratePitch);
    }

    private boolean isAimOnTarget() {
        LocalPlayer player = mc.player;
        if (player == null) {
            return false;
        }
        boolean silent = getBoolSetting("Silent Aim", true);
        float curYaw = silent && RotationManager.isActive() ? RotationManager.getServerYaw() : player.getYRot();
        float curPitch = silent && RotationManager.isActive() ? RotationManager.getServerPitch() : player.getXRot();
        float gcd = Math.max(0.15f, RotationManager.getGCD());
        float tol = Math.max(AIM_TOLERANCE_DEG, gcd * 2.2f);
        float yawErr = Math.abs(Mth.wrapDegrees(targetYaw - curYaw));
        float pitchErr = Math.abs(targetPitch - curPitch);
        return yawErr <= tol && pitchErr <= tol;
    }

    private List<CandidatePearl> collectValidCandidates() {
        List<CandidatePearl> valid = new ArrayList<>();
        if (mc.level == null || mc.player == null) {
            return valid;
        }
        if (!canLocalPlayerThrowPearl()) {
            return valid;
        }

        Map<UUID, Player> playersByUuid = new HashMap<>();
        List<Player> allPlayers = new ArrayList<>();
        List<ThrownEnderpearl> pearls = new ArrayList<>();

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof Player p && p.isAlive()) {
                playersByUuid.put(p.getUUID(), p);
                allPlayers.add(p);
            } else if (entity instanceof ThrownEnderpearl pearl && isPearlActive(pearl)) {
                pearls.add(pearl);
            }
        }

        for (int i = pearls.size() - 1; i >= 0; i--) {
            ThrownEnderpearl pearl = pearls.get(i);
            int id = pearl.getId();
            if (handledPearlIds.contains(id)) {
                continue;
            }
            Player owner = resolveOwner(pearl, playersByUuid, allPlayers);
            if (!isValidEnemyOwner(owner)) {
                continue;
            }
            Vec3 landing = PearlGrapplePhysics.predictLandingPosition(mc.level, pearl);
            if (landing == null) {
                continue;
            }
            if (!isLandingAcceptable(owner, landing)) {
                continue;
            }
            valid.add(new CandidatePearl(pearl, owner, landing));
        }
        return valid;
    }

    private Player resolveOwner(ThrownEnderpearl pearl, Map<UUID, Player> playersByUuid, List<Player> allPlayers) {
        Entity direct = pearl.getOwner();
        if (direct instanceof Player p) {
            return p;
        }
        UUID recorded = PEARL_OWNER_BY_ENTITY_ID.get(pearl.getId());
        if (recorded != null) {
            Player mapped = playersByUuid.get(recorded);
            if (mapped != null) {
                return mapped;
            }
        }
        Player nearest = findNearestPlayerWithin(pearl, allPlayers, OWNER_ATTRIBUTION_RADIUS);
        if (nearest != null) {
            PEARL_OWNER_BY_ENTITY_ID.put(pearl.getId(), nearest.getUUID());
        }
        return nearest;
    }

    private boolean isValidEnemyOwner(Player owner) {
        if (owner == null || owner == mc.player || !owner.isAlive()) {
            return false;
        }
        if (AntiBot.isBot(owner) || Teams.isTeam(owner) || TargetFilterManager.isFiltered(owner)) {
            return false;
        }
        return true;
    }

    private boolean isLandingAcceptable(Player owner, Vec3 landing) {
        LocalPlayer player = mc.player;
        if (player == null || landing == null) {
            return false;
        }
        Vec3 playerPos = player.position();
        if (owner != null) {
            Vec3 ownerPos = owner.position();
            if (!PearlGrapplePhysics.isPearlEscaping(playerPos, ownerPos, landing)) {
                return false;
            }
        }
        double minDistance = getDoubleSetting("Distance Limit", 6.0);
        if (playerPos.distanceTo(landing) <= minDistance) {
            return false;
        }
        double angleLimit = getDoubleSetting("Angle Limit", 180.0);
        if (!PearlGrapplePhysics.isWithinAngleLimit(playerPos, player.getYRot(), landing, angleLimit)) {
            return false;
        }
        boolean verticalCheck = getBoolSetting("Vertical Check", false);
        return PearlGrapplePhysics.passesVerticalCheck(player.getY(), landing, verticalCheck);
    }

    private boolean isEngagementStillValid() {
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || lockedLandingPos == null) {
            return false;
        }
        if (!canLocalPlayerThrowPearl()) {
            return false;
        }
        Player owner = null;
        if (lockedOwnerUuid != null) {
            owner = mc.level.getPlayerByUUID(lockedOwnerUuid);
        }
        double minDistance = getDoubleSetting("Distance Limit", 6.0);
        Vec3 playerPos = player.position();
        if (playerPos.distanceTo(lockedLandingPos) <= minDistance) {
            return false;
        }
        if (owner != null && owner.isAlive()) {
            Vec3 ownerPos = owner.position();
            if (!PearlGrapplePhysics.isPearlEscaping(playerPos, ownerPos, lockedLandingPos)) {
                return false;
            }
        }
        boolean verticalCheck = getBoolSetting("Vertical Check", false);
        return PearlGrapplePhysics.passesVerticalCheck(player.getY(), lockedLandingPos, verticalCheck);
    }

    private boolean canLocalPlayerThrowPearl() {
        LocalPlayer player = mc.player;
        if (player == null || !player.isAlive()) {
            return false;
        }
        double minHealth = getDoubleSetting("Min Health", 5.0);
        if (player.getHealth() < minHealth) {
            return false;
        }
        if (!isHoldingAllowedItem(player)) {
            return false;
        }
        if (!hasAvailablePearl(player)) {
            return false;
        }
        double cooldownSec = getDoubleSetting("Pearl Cooldown (s)", 1.0);
        if (cooldownSec > 0.0) {
            long elapsedMs = System.currentTimeMillis() - lastPearlThrowMs;
            if (elapsedMs < Math.round(cooldownSec * 1000.0)) {
                return false;
            }
        }
        ItemCooldowns cooldowns = player.getCooldowns();
        ItemStack defaultPearl = Items.ENDER_PEARL.getDefaultInstance();
        return !cooldowns.isOnCooldown(defaultPearl);
    }

    private boolean hasAvailablePearl(LocalPlayer player) {
        ItemStack offhand = player.getOffhandItem();
        if (offhand.is(Items.ENDER_PEARL)) {
            return true;
        }
        Inventory inv = player.getInventory();
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.is(Items.ENDER_PEARL)) {
                return true;
            }
        }
        return false;
    }

    private boolean isHoldingAllowedItem(LocalPlayer player) {
        if (player.isUsingItem()) {
            return false;
        }
        if (!getBoolSetting("Limit To Items", false)) {
            return true;
        }
        ItemStack main = player.getMainHandItem();
        return main.isEmpty() || main.is(Items.ENDER_PEARL) || ModuleUtils.isHoldingWeapon(main);
    }

    private static boolean isPearlActive(ThrownEnderpearl pearl) {
        return pearl != null && pearl.isAlive() && !pearl.isRemoved() && !pearl.onGround();
    }

    private boolean isOnBindMode() {
        Setting s = setting("Mode");
        String val = s != null ? s.getValString() : "On Bind";
        return !"Aggro".equalsIgnoreCase(val);
    }

    private double getDoubleSetting(String name, double fallback) {
        Setting s = setting(name);
        return s != null ? s.getValDouble() : fallback;
    }

    private boolean getBoolSetting(String name, boolean fallback) {
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
