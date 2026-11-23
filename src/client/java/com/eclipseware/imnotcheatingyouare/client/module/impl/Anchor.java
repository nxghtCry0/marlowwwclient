package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.setting.SettingsManager;
import com.eclipseware.imnotcheatingyouare.client.utils.InputUtil;
import com.eclipseware.imnotcheatingyouare.client.utils.ModuleUtils;
import com.eclipseware.imnotcheatingyouare.client.utils.RotationManager;
import com.eclipseware.imnotcheatingyouare.client.utils.TargetFilterManager;
import com.eclipseware.imnotcheatingyouare.mixin.client.MinecraftAccessor;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;
import net.minecraft.client.gui.Gui;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class Anchor extends Module {

    private final Setting activation;
    private final Setting activateKey;
    private final Setting placeKey;
    private final Setting switchDelay;
    private final Setting glowstoneDelay;
    private final Setting explodeDelay;
    private final Setting totemSlot;

    private final Setting fastMode;
    private final Setting doubleAnchor;
    private final Setting doubleAnchorCondition;
    private final Setting doubleAnchorKey;
    private final Setting doubleAnchorCount;

    private final Setting switchBack;
    private final Setting switchBackDelay;
    private final Setting pauseOnKill;
    private final Setting pauseTime;

    private final Setting anchorAura;
    private final Setting auraAutoExplode;
    private final Setting auraRange;
    private final Setting auraTurnSpeed;
    private final Setting auraCurve;
    private final Setting auraArc;
    private final Setting auraJitter;

    private final Random random = new Random();
    private int switchCounter;
    private int glowstoneCounter;
    private int explodeCounter;
    private int switchBackCounter;
    private int pauseCounter;
    private int chargeTimeout;
    private int currentDoubleAnchorCount;
    private boolean chargedGlowstone;
    private boolean exploded;
    private boolean needsSwitchBack;
    private boolean activateLatched;
    private boolean activateWasDown;
    private int latchWaitTicks;
    private BlockPos placedAnchorPos;
    private BlockPos fireBreakPending;
    private int fireBreakTicks;
    private int placeTimeout;
    private int noAimTicks;
    private final List<Player> deadPlayers = new ArrayList<>();

    private BlockPos auraLockedPos = null;
    private boolean auraWasRotating = false;
    private int auraSavedSlot = -1;
    private int auraAimTicks = 0;
    private int auraActionCooldown = 0;
    private double auraInitialAngDist = 22.0;
    private double auraBaseSpeed = 40.0;
    private double auraStartMult = 0.44;
    private double auraMidMult = 1.12;
    private double auraEndMult = 0.54;
    private float auraArcSign = 1.0f;
    private double auraFaceOffsetU = 0.0;
    private double auraFaceOffsetV = 0.0;
    private float auraJitterYaw = 0.0f;
    private float auraJitterPitch = 0.0f;

    public Anchor() {
        super("Anchor", Category.Combat, "Automatically places and blows up respawn anchors, including humanized silent-aim Anchor Aura.");

        activation = new Setting("Activation", this, "HOLD", new ArrayList<>(List.of("HOLD", "CLICK")));
        activateKey = new Setting("Activate Key", this, "MOUSE RIGHT", true);
        placeKey = new Setting("Place Key", this, "NONE", true);
        switchDelay = new Setting("Switch Delay", this, 0.0, 0.0, 20.0, true);
        glowstoneDelay = new Setting("Glowstone Delay", this, 0.0, 0.0, 20.0, true);
        explodeDelay = new Setting("Explode Delay", this, 0.0, 0.0, 20.0, true);
        totemSlot = new Setting("Totem Slot", this, 1.0, 1.0, 9.0, true);

        fastMode = new Setting("Fast Mode", this, false);
        doubleAnchor = new Setting("Double Anchor", this, false);
        doubleAnchorCondition = new Setting("Double Condition", this, "ALWAYS", new ArrayList<>(List.of("ALWAYS", "HOLD_SLOT", "KEY_BIND")));
        doubleAnchorKey = new Setting("Double Anchor Key", this, "NONE", true);
        doubleAnchorCount = new Setting("Double Anchor Count", this, 2.0, 2.0, 5.0, true);

        switchBack = new Setting("Switch Back", this, true);
        switchBackDelay = new Setting("Switch Back Delay", this, 5.0, 0.0, 20.0, true);
        pauseOnKill = new Setting("Pause On Kill", this, true);
        pauseTime = new Setting("Pause Time", this, 2.0, 0.5, 10.0, false);

        anchorAura = new Setting("Anchor Aura", this, false);
        auraAutoExplode = new Setting("Aura Auto Explode", this, true).visibleWhen(anchorAura::getValBoolean);
        auraRange = new Setting("Aura Range", this, 4.5, 1.5, 6.0, false).visibleWhen(anchorAura::getValBoolean);
        auraTurnSpeed = new Setting("Aura Turn Speed", this, 32.0, 54.0, 5.0, 140.0, false).visibleWhen(anchorAura::getValBoolean);
        auraCurve = new Setting("Aura Speed Curve", this, 0.25, 0.10, 0.25, 1.00).visibleWhen(anchorAura::getValBoolean);
        auraArc = new Setting("Aura Arc (%)", this, 55.0, 0.0, 100.0, true).visibleWhen(anchorAura::getValBoolean);
        auraJitter = new Setting("Aura Jitter", this, 1.15, 0.0, 4.0, false).visibleWhen(anchorAura::getValBoolean);

        ImnotcheatingyouareClient client = ImnotcheatingyouareClient.INSTANCE;
        if (client == null || client.settingsManager == null) {
            return;
        }
        SettingsManager sm = client.settingsManager;
        sm.rSetting(activation);
        sm.rSetting(activateKey);
        sm.rSetting(placeKey);
        sm.rSetting(switchDelay);
        sm.rSetting(glowstoneDelay);
        sm.rSetting(explodeDelay);
        sm.rSetting(totemSlot);
        sm.rSetting(fastMode);
        sm.rSetting(doubleAnchor);
        sm.rSetting(doubleAnchorCondition);
        sm.rSetting(doubleAnchorKey);
        sm.rSetting(doubleAnchorCount);
        sm.rSetting(switchBack);
        sm.rSetting(switchBackDelay);
        sm.rSetting(pauseOnKill);
        sm.rSetting(pauseTime);
        sm.rSetting(anchorAura);
        sm.rSetting(auraAutoExplode);
        sm.rSetting(auraRange);
        sm.rSetting(auraTurnSpeed);
        sm.rSetting(auraCurve);
        sm.rSetting(auraArc);
        sm.rSetting(auraJitter);
    }

    @Override
    public void onEnable() {
        resetState();
    }

    @Override
    public void onDisable() {
        releaseAuraAim();
        resetState();
    }

    private void resetState() {
        switchCounter = 0;
        glowstoneCounter = 0;
        explodeCounter = 0;
        switchBackCounter = 0;
        pauseCounter = 0;
        chargeTimeout = 0;
        currentDoubleAnchorCount = 0;
        chargedGlowstone = false;
        exploded = false;
        needsSwitchBack = false;
        activateLatched = false;
        activateWasDown = false;
        latchWaitTicks = 0;
        placedAnchorPos = null;
        fireBreakPending = null;
        fireBreakTicks = 0;
        placeTimeout = 0;
        noAimTicks = 0;
        auraLockedPos = null;
        auraAimTicks = 0;
        auraActionCooldown = 0;
        auraSavedSlot = -1;
        deadPlayers.clear();
    }

    private void releaseAuraAim() {
        if (auraWasRotating) {
            auraWasRotating = false;
            auraLockedPos = null;
            if (RotationManager.isActive()) {
                RotationManager.requestReturn();
            }
        }
    }

    private boolean shouldDoubleAnchor() {
        if (!doubleAnchor.getValBoolean()) {
            return false;
        }
        String cond = doubleAnchorCondition.getValString();
        if ("ALWAYS".equals(cond)) {
            return true;
        }
        if ("HOLD_SLOT".equals(cond)) {
            int anchorSlot = -1;
            Inventory inv = mc.player.getInventory();
            for (int i = 0; i < 9; i++) {
                ItemStack st = inv.getItem(i);
                if (st.is(Items.RESPAWN_ANCHOR)) {
                    anchorSlot = i;
                    break;
                }
            }
            Options opts = mc.options;
            if (anchorSlot != -1 && opts != null && anchorSlot < opts.keyHotbarSlots.length) {
                KeyMapping hotbarKey = opts.keyHotbarSlots[anchorSlot];
                return hotbarKey.isDown();
            }
            return false;
        }
        if ("KEY_BIND".equals(cond)) {
            return InputUtil.isDown(parseKeyCode(doubleAnchorKey.getValText()));
        }
        return false;
    }

    @Override
    public void onTick() {
        Gui gui = mc != null ? mc.gui : null;
        if (mc == null || mc.player == null || mc.level == null || gui == null || gui.screen() != null || mc.gameMode == null) {
            releaseAuraAim();
            return;
        }

        if (auraActionCooldown > 0) {
            auraActionCooldown--;
        }

        if (pauseCounter > 0) {
            pauseCounter--;
            releaseAuraAim();
            return;
        }

        if (pauseOnKill.getValBoolean() && checkDeadPlayers()) {
            pauseCounter = (int) (pauseTime.getValDouble() * 20);
            releaseAuraAim();
            return;
        }

        if (isEatingOrBlocking()) {
            releaseAuraAim();
            return;
        }

        if (needsSwitchBack && switchBack.getValBoolean()) {
            if (switchBackCounter < (int) switchBackDelay.getValDouble()) {
                switchBackCounter++;
                return;
            }
            switchBackCounter = 0;
            if (auraSavedSlot >= 0 && auraSavedSlot < 9) {
                ModuleUtils.switchToSlot(auraSavedSlot);
                auraSavedSlot = -1;
            } else {
                swapTo(Items.RESPAWN_ANCHOR);
            }
            needsSwitchBack = false;
            return;
        }

        int activateCode = parseKeyCode(activateKey.getValText());
        int placeCode = parseKeyCode(placeKey.getValText());
        boolean activateKeyDown = InputUtil.isDown(activateCode);
        boolean placeHeld = InputUtil.isDown(placeCode);

        boolean activateHeld;
        if ("CLICK".equals(activation.getValString())) {
            if (activateKeyDown && !activateWasDown) {
                activateLatched = true;
                latchWaitTicks = 0;
            }
            activateHeld = activateLatched;
        } else {
            activateHeld = activateKeyDown;
        }
        activateWasDown = activateKeyDown;

        if (anchorAura.getValBoolean() && (auraAutoExplode.getValBoolean() || activateHeld)) {
            if (tickAnchorAura()) {
                return;
            }
        } else {
            releaseAuraAim();
        }

        if (!activateHeld && !placeHeld) {
            activateLatched = false;
            latchWaitTicks = 0;
            chargedGlowstone = false;
            exploded = false;
            needsSwitchBack = false;
            currentDoubleAnchorCount = 0;
            placedAnchorPos = null;
            placeTimeout = 0;
            return;
        }

        BlockPos targetPos = null;
        KeyMapping keyUse = mc.options.keyUse;

        if (placeHeld && !activateHeld) {
            if (placedAnchorPos != null && exploded && currentDoubleAnchorCount == 0) {
                placedAnchorPos = null;
                placeTimeout = 0;
                chargedGlowstone = false;
                exploded = false;
            }

            if (placedAnchorPos == null) {
                if (!(mc.hitResult instanceof BlockHitResult hit)) {
                    return;
                }
                BlockPos hitPos = hit.getBlockPos();
                BlockState hitState = mc.level.getBlockState(hitPos);

                if (hitState.is(Blocks.RESPAWN_ANCHOR)) {
                    placedAnchorPos = hitPos;
                    placeTimeout = 0;
                } else {
                    keyUse.setDown(false);
                    keyUse.consumeClick();
                    ItemStack mainItem = mc.player.getMainHandItem();
                    if (!mainItem.is(Items.RESPAWN_ANCHOR)) {
                        if (switchCounter < (int) switchDelay.getValDouble()) {
                            switchCounter++;
                            return;
                        }
                        switchCounter = 0;
                        swapTo(Items.RESPAWN_ANCHOR);
                    }
                    BlockPos expectedPos = hitState.canBeReplaced() ? hitPos : hitPos.relative(hit.getDirection());
                    if (waitForFireClear(expectedPos)) {
                        return;
                    }
                    startUseItem();
                    placedAnchorPos = expectedPos;
                    placeTimeout = 0;
                    chargedGlowstone = false;
                    exploded = false;
                    return;
                }
            } else {
                BlockState placedState = mc.level.getBlockState(placedAnchorPos);
                if (!placedState.is(Blocks.RESPAWN_ANCHOR)) {
                    placeTimeout++;
                    if (placeTimeout > 10) {
                        placedAnchorPos = null;
                        placeTimeout = 0;
                    }
                    return;
                }
            }
            targetPos = placedAnchorPos;
        } else {
            BlockPos aimed = null;
            if (mc.hitResult instanceof BlockHitResult h) {
                BlockPos hp = h.getBlockPos();
                BlockState hs = mc.level.getBlockState(hp);
                if (hs.is(Blocks.RESPAWN_ANCHOR)) {
                    aimed = hp;
                }
            }

            if (aimed != null) {
                placedAnchorPos = aimed;
                targetPos = aimed;
                latchWaitTicks = 0;
            } else if (placedAnchorPos != null && isAnchorAt(placedAnchorPos)) {
                targetPos = placedAnchorPos;
            } else if (exploded && placedAnchorPos != null) {
                if ("CLICK".equals(activation.getValString())) {
                    activateLatched = false;
                }
                placedAnchorPos = null;
                placeTimeout = 0;
                chargedGlowstone = false;
                exploded = false;
                return;
            } else {
                if (placedAnchorPos != null) {
                    if (!exploded && ++placeTimeout > 10) {
                        placedAnchorPos = null;
                        placeTimeout = 0;
                    }
                    return;
                }

                if ("CLICK".equals(activation.getValString()) && ++latchWaitTicks > 20) {
                    activateLatched = false;
                    latchWaitTicks = 0;
                }

                if (keyUseIsActivateKey(activateCode)) {
                    return;
                }
                if (!(mc.hitResult instanceof BlockHitResult hit)) {
                    return;
                }
                BlockPos hitPos = hit.getBlockPos();
                BlockState hitState = mc.level.getBlockState(hitPos);
                if (hitState.is(Blocks.RESPAWN_ANCHOR)) {
                    return;
                }

                keyUse.setDown(false);
                keyUse.consumeClick();
                ItemStack mainItem = mc.player.getMainHandItem();
                if (!mainItem.is(Items.RESPAWN_ANCHOR)) {
                    if (switchCounter < (int) switchDelay.getValDouble()) {
                        switchCounter++;
                        return;
                    }
                    switchCounter = 0;
                    swapTo(Items.RESPAWN_ANCHOR);
                }
                BlockPos expectedPos = hitState.canBeReplaced() ? hitPos : hitPos.relative(hit.getDirection());
                if (waitForFireClear(expectedPos)) {
                    return;
                }
                startUseItem();
                placedAnchorPos = expectedPos;
                placeTimeout = 0;
                latchWaitTicks = 0;
                chargedGlowstone = false;
                exploded = false;
                return;
            }
        }

        if (targetPos == null || !isAnchorAt(targetPos)) {
            return;
        }

        keyUse.setDown(false);
        keyUse.consumeClick();

        if ("CLICK".equals(activation.getValString())) {
            if (aimOnTarget(targetPos)) {
                noAimTicks = 0;
            } else if (++noAimTicks > 15) {
                activateLatched = false;
                latchWaitTicks = 0;
                chargedGlowstone = false;
                exploded = false;
                needsSwitchBack = false;
                currentDoubleAnchorCount = 0;
                placedAnchorPos = null;
                placeTimeout = 0;
                noAimTicks = 0;
                ItemStack mainItem = mc.player.getMainHandItem();
                if (!mainItem.is(Items.RESPAWN_ANCHOR)) {
                    swapTo(Items.RESPAWN_ANCHOR);
                }
                return;
            }
        }

        BlockState anchorState = mc.level.getBlockState(targetPos);
        int charges = anchorState.getValue(BlockStateProperties.RESPAWN_ANCHOR_CHARGES);

        if (charges == 0) {
            exploded = false;
            if (!chargedGlowstone) {
                ItemStack mainItem = mc.player.getMainHandItem();
                if (!mainItem.is(Items.GLOWSTONE)) {
                    if (switchCounter < (int) switchDelay.getValDouble()) {
                        switchCounter++;
                        return;
                    }
                    switchCounter = 0;
                    swapTo(Items.GLOWSTONE);
                }
                if (glowstoneCounter < (int) glowstoneDelay.getValDouble()) {
                    glowstoneCounter++;
                    return;
                }
                glowstoneCounter = 0;

                if (aimOnTarget(targetPos)) {
                    startUseItem();
                    chargedGlowstone = true;
                    chargeTimeout = 0;
                }
            } else if (++chargeTimeout > 10) {
                chargedGlowstone = false;
                chargeTimeout = 0;
            }
        } else if (!exploded) {
            chargedGlowstone = false;
            chargeTimeout = 0;
            if (shouldDoubleAnchor()) {
                int targetMax = (int) doubleAnchorCount.getValDouble();
                int nextCount = currentDoubleAnchorCount + 1;

                if (nextCount < targetMax) {
                    ItemStack mainItem = mc.player.getMainHandItem();
                    if (!mainItem.is(Items.RESPAWN_ANCHOR)) {
                        if (switchCounter < (int) switchDelay.getValDouble()) {
                            switchCounter++;
                            return;
                        }
                        switchCounter = 0;
                        swapTo(Items.RESPAWN_ANCHOR);
                    }
                    if (explodeCounter < (int) explodeDelay.getValDouble()) {
                        explodeCounter++;
                        return;
                    }
                    explodeCounter = 0;
                    if (aimOnTarget(targetPos)) {
                        startUseItem();
                        startUseItem();
                    }
                    currentDoubleAnchorCount = nextCount;
                    exploded = true;
                } else {
                    int slot = resolveSafeDetonateSlot();
                    if (ModuleUtils.getSelectedSlot() != slot) {
                        if (switchCounter < (int) switchDelay.getValDouble()) {
                            switchCounter++;
                            return;
                        }
                        switchCounter = 0;
                        ModuleUtils.switchToSlot(slot);
                    }
                    if (explodeCounter < (int) explodeDelay.getValDouble()) {
                        explodeCounter++;
                        return;
                    }
                    explodeCounter = 0;
                    if (aimOnTarget(targetPos)) {
                        startUseItem();
                    }
                    exploded = true;
                    currentDoubleAnchorCount = 0;
                    if (switchBack.getValBoolean()) {
                        needsSwitchBack = true;
                        switchBackCounter = 0;
                    }
                }
            } else {
                int slot = resolveSafeDetonateSlot();
                if (ModuleUtils.getSelectedSlot() != slot) {
                    if (switchCounter < (int) switchDelay.getValDouble()) {
                        switchCounter++;
                        return;
                    }
                    switchCounter = 0;
                    ModuleUtils.switchToSlot(slot);
                }
                if (explodeCounter < (int) explodeDelay.getValDouble()) {
                    explodeCounter++;
                    return;
                }
                explodeCounter = 0;
                if (aimOnTarget(targetPos)) {
                    startUseItem();
                }
                exploded = true;
                if (switchBack.getValBoolean()) {
                    needsSwitchBack = true;
                    switchBackCounter = 0;
                }
            }
        }
    }

    private boolean tickAnchorAura() {
        double range = auraRange.getValDouble();
        BlockPos bestPos = findBestNearbyAnchor(range);
        if (bestPos == null) {
            releaseAuraAim();
            return false;
        }

        BlockState state = mc.level.getBlockState(bestPos);
        int charges = state.getValue(BlockStateProperties.RESPAWN_ANCHOR_CHARGES);
        if (charges == 0 && !hasGlowstoneAvailable()) {
            releaseAuraAim();
            return false;
        }

        Vec3 eyePos = mc.player.getEyePosition();
        Direction face = pickVisibleFace(bestPos, eyePos);

        if (!bestPos.equals(auraLockedPos)) {
            auraLockedPos = bestPos;
            initAuraFlickProfile(bestPos, face, eyePos);
        }

        updateAuraJitter();
        Vec3 hitVec = computeHumanizedFacePoint(bestPos, face);
        float[] exactRot = ModuleUtils.getRotations(eyePos, hitVec);
        float goalYaw = Mth.wrapDegrees(exactRot[0] + auraJitterYaw);
        float goalPitch = Mth.clamp(exactRot[1] + auraJitterPitch, -90.0f, 90.0f);

        stepAuraSilentRotation(goalYaw, goalPitch);
        auraWasRotating = true;

        if (auraActionCooldown > 0 || !isServerAimOnAnchor(bestPos, eyePos, range)) {
            return true;
        }

        BlockHitResult hitResult = new BlockHitResult(hitVec, face, bestPos, false);

        if (charges == 0) {
            ItemStack offhand = mc.player.getOffhandItem();
            boolean useOffhand = offhand.is(Items.GLOWSTONE);
            if (!useOffhand) {
                ItemStack held = mc.player.getMainHandItem();
                if (!held.is(Items.GLOWSTONE)) {
                    if (switchCounter < (int) switchDelay.getValDouble()) {
                        switchCounter++;
                        return true;
                    }
                    switchCounter = 0;
                    if (auraSavedSlot == -1) {
                        auraSavedSlot = ModuleUtils.getSelectedSlot();
                    }
                    swapTo(Items.GLOWSTONE);
                }
            }
            if (glowstoneCounter < (int) glowstoneDelay.getValDouble()) {
                glowstoneCounter++;
                return true;
            }
            glowstoneCounter = 0;
            InteractionHand hand = useOffhand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
            mc.gameMode.useItemOn(mc.player, hand, hitResult);
            ModuleUtils.swingHand(mc.player, hand);
            auraActionCooldown = fastMode.getValBoolean() ? 0 : 1;
            return true;
        }

        int detSlot = resolveSafeDetonateSlot();
        if (ModuleUtils.getSelectedSlot() != detSlot) {
            if (switchCounter < (int) switchDelay.getValDouble()) {
                switchCounter++;
                return true;
            }
            switchCounter = 0;
            if (auraSavedSlot == -1) {
                auraSavedSlot = ModuleUtils.getSelectedSlot();
            }
            ModuleUtils.switchToSlot(detSlot);
        }
        if (explodeCounter < (int) explodeDelay.getValDouble()) {
            explodeCounter++;
            return true;
        }
        explodeCounter = 0;

        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hitResult);
        ModuleUtils.swingHand(mc.player, InteractionHand.MAIN_HAND);
        auraActionCooldown = 2;
        releaseAuraAim();
        if (switchBack.getValBoolean()) {
            needsSwitchBack = true;
            switchBackCounter = 0;
        } else {
            auraSavedSlot = -1;
        }
        return true;
    }

    private BlockPos findBestNearbyAnchor(double range) {
        Vec3 eyePos = mc.player.getEyePosition();
        double rangeSq = range * range;
        int rCeil = (int) Math.ceil(range);
        BlockPos center = BlockPos.containing(eyePos);

        BlockPos best = null;
        double bestScore = Double.MAX_VALUE;

        for (int dx = -rCeil; dx <= rCeil; dx++) {
            for (int dy = -rCeil; dy <= rCeil; dy++) {
                for (int dz = -rCeil; dz <= rCeil; dz++) {
                    BlockPos pos = center.offset(dx, dy, dz);
                    Vec3 blockCenter = Vec3.atCenterOf(pos);
                    double distSq = eyePos.distanceToSqr(blockCenter);
                    if (distSq > rangeSq) {
                        continue;
                    }
                    BlockState state = mc.level.getBlockState(pos);
                    if (!state.is(Blocks.RESPAWN_ANCHOR)) {
                        continue;
                    }
                    int charges = state.getValue(BlockStateProperties.RESPAWN_ANCHOR_CHARGES);
                    if (charges == 0 && !hasGlowstoneAvailable()) {
                        continue;
                    }
                    double enemyDistBonus = nearestEnemyDistance(blockCenter) * 0.65;
                    double chargePriority = charges > 0 ? -4.0 : 0.0;
                    double score = Math.sqrt(distSq) + enemyDistBonus + chargePriority;
                    if (score < bestScore) {
                        bestScore = score;
                        best = pos.immutable();
                    }
                }
            }
        }
        return best;
    }

    private double nearestEnemyDistance(Vec3 anchorCenter) {
        double closest = 12.0;
        for (Player p : mc.level.players()) {
            if (p == mc.player || !p.isAlive() || AntiBot.isBot(p) || Teams.isTeam(p) || TargetFilterManager.isFiltered(p)) {
                continue;
            }
            Vec3 pPos = p.position();
            double d = pPos.distanceTo(anchorCenter);
            if (d < closest) {
                closest = d;
            }
        }
        return closest;
    }

    private void initAuraFlickProfile(BlockPos pos, Direction face, Vec3 eyePos) {
        auraFaceOffsetU = (random.nextDouble() - 0.5) * 0.22;
        auraFaceOffsetV = (random.nextDouble() - 0.5) * 0.22;
        Vec3 hitPoint = computeHumanizedFacePoint(pos, face);
        float[] rot = ModuleUtils.getRotations(eyePos, hitPoint);
        float curYaw = RotationManager.isActive() ? RotationManager.getServerYaw() : mc.player.getYRot();
        float curPitch = RotationManager.isActive() ? RotationManager.getServerPitch() : mc.player.getXRot();
        auraInitialAngDist = Math.max(8.0, Math.hypot(Mth.wrapDegrees(rot[0] - curYaw), rot[1] - curPitch));
        auraAimTicks = 0;

        double low = auraTurnSpeed.getRangeLow();
        double high = auraTurnSpeed.getRangeHigh();
        double sample = (random.nextDouble() + random.nextDouble()) * 0.5;
        auraBaseSpeed = low + (high - low) * sample;
        auraStartMult = 0.38 + random.nextDouble() * 0.24;
        auraMidMult = 0.98 + random.nextDouble() * 0.34;
        auraEndMult = 0.46 + random.nextDouble() * 0.24;
        auraArcSign = random.nextBoolean() ? 1.0f : -1.0f;
    }

    private void updateAuraJitter() {
        double jitterDeg = auraJitter.getValDouble();
        if (jitterDeg <= 0.001) {
            auraJitterYaw = 0.0f;
            auraJitterPitch = 0.0f;
            return;
        }
        float targetJYaw = (float) ((random.nextDouble() - 0.5) * 2.0 * jitterDeg);
        float targetJPitch = (float) ((random.nextDouble() - 0.5) * 1.6 * jitterDeg);
        auraJitterYaw = auraJitterYaw * 0.52f + targetJYaw * 0.48f;
        auraJitterPitch = auraJitterPitch * 0.52f + targetJPitch * 0.48f;
        auraFaceOffsetU = Mth.clamp(auraFaceOffsetU + (random.nextDouble() - 0.5) * 0.05, -0.18, 0.18);
        auraFaceOffsetV = Mth.clamp(auraFaceOffsetV + (random.nextDouble() - 0.5) * 0.05, -0.18, 0.18);
    }

    private void stepAuraSilentRotation(float goalYaw, float goalPitch) {
        auraAimTicks++;
        float curYaw = RotationManager.isActive() ? RotationManager.getServerYaw() : mc.player.getYRot();
        float curPitch = RotationManager.isActive() ? RotationManager.getServerPitch() : mc.player.getXRot();
        double angDist = Math.hypot(Mth.wrapDegrees(goalYaw - curYaw), goalPitch - curPitch);

        double distProgress = 1.0 - Mth.clamp(angDist / Math.max(8.0, auraInitialAngDist), 0.0, 1.0);
        double timeProgress = Math.min(auraAimTicks / 14.0, 1.0);
        double flickProgress = Mth.clamp(distProgress * 0.72 + timeProgress * 0.28, 0.0, 1.0);

        double[] speedCurve = auraCurve.getCurve();
        double arcPct = auraArc.getValDouble() / 100.0;
        float arcPeak = (float) (Mth.clamp(auraInitialAngDist * 0.16, 1.2, 11.0) * arcPct);
        double tickJitter = 0.90 + random.nextDouble() * 0.20;

        float humanizedStep = PearlGrapplePhysics.computeHumanizedTurnSpeed(
            auraBaseSpeed,
            angDist,
            55.0,
            speedCurve,
            flickProgress,
            auraStartMult,
            auraMidMult,
            auraEndMult,
            tickJitter
        );

        float[] waypoint = PearlGrapplePhysics.computeCurvedWaypoint(
            curYaw,
            curPitch,
            goalYaw,
            goalPitch,
            flickProgress,
            arcPeak,
            auraArcSign
        );

        RotationManager.keepRotated(waypoint[0], waypoint[1], humanizedStep, true);
    }

    private boolean isServerAimOnAnchor(BlockPos pos, Vec3 eyePos, double range) {
        float srvYaw = RotationManager.isActive() ? RotationManager.getServerYaw() : mc.player.getYRot();
        float srvPitch = RotationManager.isActive() ? RotationManager.getServerPitch() : mc.player.getXRot();
        Vec3 dir = PearlGrapplePhysics.directionFromRotation(srvYaw, srvPitch);
        Vec3 rayEnd = eyePos.add(dir.scale(range + 1.0));
        AABB box = new AABB(pos).inflate(0.02);
        Optional<Vec3> clip = box.clip(eyePos, rayEnd);
        return clip.isPresent();
    }

    private static Direction pickVisibleFace(BlockPos pos, Vec3 eyePos) {
        Vec3 center = Vec3.atCenterOf(pos);
        Vec3 delta = eyePos.subtract(center);
        Direction best = Direction.UP;
        double bestDot = -Double.MAX_VALUE;
        Direction[] dirs = Direction.values();
        for (int i = 0; i < dirs.length; i++) {
            Direction d = dirs[i];
            Vec3i normal = d.getUnitVec3i();
            double dot = delta.x * normal.getX() + delta.y * normal.getY() + delta.z * normal.getZ();
            if (dot > bestDot) {
                bestDot = dot;
                best = d;
            }
        }
        return best;
    }

    private Vec3 computeHumanizedFacePoint(BlockPos pos, Direction face) {
        Vec3i n = face.getUnitVec3i();
        double cx = pos.getX() + 0.5 + n.getX() * 0.5;
        double cy = pos.getY() + 0.5 + n.getY() * 0.5;
        double cz = pos.getZ() + 0.5 + n.getZ() * 0.5;
        if (n.getY() != 0) {
            cx += auraFaceOffsetU;
            cz += auraFaceOffsetV;
        } else if (n.getX() != 0) {
            cy += auraFaceOffsetU;
            cz += auraFaceOffsetV;
        } else {
            cx += auraFaceOffsetU;
            cy += auraFaceOffsetV;
        }
        return new Vec3(cx, cy, cz);
    }

    private boolean hasGlowstoneAvailable() {
        ItemStack offhand = mc.player.getOffhandItem();
        return offhand.is(Items.GLOWSTONE) || ModuleUtils.findItemInHotbar(Items.GLOWSTONE) != -1;
    }

    private int resolveSafeDetonateSlot() {
        Inventory inv = mc.player.getInventory();
        int preferred = Mth.clamp((int) totemSlot.getValDouble() - 1, 0, 8);
        ItemStack prefStack = inv.getItem(preferred);
        if (!prefStack.is(Items.GLOWSTONE)) {
            return preferred;
        }
        for (int i = 0; i < 9; i++) {
            ItemStack st = inv.getItem(i);
            if (!st.is(Items.GLOWSTONE)) {
                return i;
            }
        }
        return preferred;
    }

    private boolean isAnchorAt(BlockPos pos) {
        if (pos == null || mc.level == null) {
            return false;
        }
        BlockState state = mc.level.getBlockState(pos);
        return state.is(Blocks.RESPAWN_ANCHOR);
    }

    private boolean checkDeadPlayers() {
        for (Player p : mc.level.players()) {
            if (p != mc.player && (p.isDeadOrDying() || p.getHealth() <= 0) && !deadPlayers.contains(p)) {
                deadPlayers.add(p);
                return true;
            }
        }
        deadPlayers.removeIf(p -> !p.isDeadOrDying() && p.getHealth() > 0);
        return false;
    }

    private boolean isEatingOrBlocking() {
        ItemStack main = mc.player.getMainHandItem();
        ItemStack off = mc.player.getOffhandItem();
        boolean shield = main.getItem() instanceof ShieldItem || off.getItem() instanceof ShieldItem;
        boolean keyHeld = InputUtil.isDown(parseKeyCode(activateKey.getValText()))
            || activateLatched
            || InputUtil.isDown(parseKeyCode(placeKey.getValText()));
        return shield && keyHeld;
    }

    private boolean keyUseIsActivateKey(int activateCode) {
        Options opts = mc.options;
        if (activateCode == 0 || opts == null || opts.keyUse == null) {
            return false;
        }
        String bindSave;
        if (InputUtil.isMouseBind(activateCode)) {
            int ordinal = -activateCode - 1;
            int sdl = switch (ordinal) {
                case 0 -> 1;
                case 1 -> 3;
                case 2 -> 2;
                case 3 -> 4;
                case 4 -> 5;
                default -> -1;
            };
            if (sdl < 0) {
                return false;
            }
            InputConstants.Type mouseType = InputConstants.Type.MOUSE;
            InputConstants.Key keyObj = mouseType.getOrCreate(sdl);
            bindSave = keyObj.getName();
        } else {
            InputConstants.Type kbType = InputConstants.Type.KEYBOARD;
            InputConstants.Key keyObj = kbType.getOrCreate(activateCode);
            bindSave = keyObj.getName();
        }
        try {
            KeyMapping keyUse = opts.keyUse;
            return keyUse.saveString().equals(bindSave);
        } catch (Exception e) {
            return false;
        }
    }

    private void swapTo(Item item) {
        Inventory inv = mc.player.getInventory();
        for (int i = 0; i < 9; i++) {
            ItemStack st = inv.getItem(i);
            if (st.is(item)) {
                ModuleUtils.switchToSlot(i);
                return;
            }
        }
    }

    private void startUseItem() {
        MinecraftAccessor acc = (MinecraftAccessor) mc;
        acc.invokeStartUseItem();
    }

    private boolean waitForFireClear(BlockPos pos) {
        if (mc.level == null || pos == null) {
            return false;
        }
        BlockState state = mc.level.getBlockState(pos);
        if (!state.is(Blocks.FIRE) && !state.is(Blocks.SOUL_FIRE)) {
            fireBreakPending = null;
            fireBreakTicks = 0;
            return false;
        }
        if (!pos.equals(fireBreakPending)) {
            fireBreakPending = pos;
            fireBreakTicks = 0;
        }
        if (fireBreakTicks > 8) {
            fireBreakTicks = 0;
        }
        if (fireBreakTicks == 0) {
            mc.gameMode.startDestroyBlock(pos, Direction.UP);
            ModuleUtils.swingHand(mc.player, InteractionHand.MAIN_HAND);
        } else {
            mc.gameMode.continueDestroyBlock(pos, Direction.UP);
        }
        fireBreakTicks++;
        return true;
    }

    private boolean aimOnTarget(BlockPos pos) {
        if (!isAnchorAt(pos)) {
            return false;
        }
        return mc.hitResult instanceof BlockHitResult h && h.getBlockPos().equals(pos);
    }

    private static int parseKeyCode(String keyName) {
        if (keyName == null) {
            return 0;
        }
        String n = keyName.trim().toUpperCase();
        if (n.isEmpty() || n.equals("NONE") || n.equals("UNKNOWN")) {
            return 0;
        }

        switch (n) {
            case "MOUSE LEFT":
            case "MOUSELEFT":
            case "LCLICK":
            case "MOUSE1":
            case "M1":
                return InputUtil.MOUSE_LEFT;
            case "MOUSE RIGHT":
            case "MOUSERIGHT":
            case "RCLICK":
            case "MOUSE2":
            case "M2":
                return InputUtil.MOUSE_RIGHT;
            case "MOUSE MIDDLE":
            case "MOUSEMIDDLE":
            case "MCLICK":
            case "MOUSE3":
            case "M3":
                return InputUtil.MOUSE_MIDDLE;
            case "MOUSE 4":
            case "MOUSE4":
            case "M4":
                return InputUtil.MOUSE_BUTTON_4;
            case "MOUSE 5":
            case "MOUSE5":
            case "M5":
                return InputUtil.MOUSE_BUTTON_5;
            default:
                break;
        }

        if (n.length() == 1) {
            char c = n.charAt(0);
            if (c >= 'A' && c <= 'Z') {
                return InputConstants.KEY_A + (c - 'A');
            }
            switch (c) {
                case '0': return InputConstants.KEY_0;
                case '1': return InputConstants.KEY_1;
                case '2': return InputConstants.KEY_2;
                case '3': return InputConstants.KEY_3;
                case '4': return InputConstants.KEY_4;
                case '5': return InputConstants.KEY_5;
                case '6': return InputConstants.KEY_6;
                case '7': return InputConstants.KEY_7;
                case '8': return InputConstants.KEY_8;
                case '9': return InputConstants.KEY_9;
                default: break;
            }
        }

        switch (n) {
            case "SPACE": return InputConstants.KEY_SPACE;
            case "LSHIFT":
            case "L-SHIFT":
            case "SHIFT": return InputConstants.KEY_LSHIFT;
            case "RSHIFT":
            case "R-SHIFT": return InputConstants.KEY_RSHIFT;
            case "LCTRL":
            case "L-CTRL":
            case "CTRL":
            case "CONTROL": return InputConstants.KEY_LCONTROL;
            case "RCTRL":
            case "R-CTRL": return InputConstants.KEY_RCONTROL;
            case "LALT":
            case "L-ALT":
            case "ALT": return InputConstants.KEY_LALT;
            case "RALT":
            case "R-ALT": return InputConstants.KEY_RALT;
            case "TAB": return InputConstants.KEY_TAB;
            case "ENTER":
            case "RETURN": return InputConstants.KEY_RETURN;
            case "ESC":
            case "ESCAPE": return InputConstants.KEY_ESCAPE;
            case "BACKSPACE": return InputConstants.KEY_BACKSPACE;
            case "DEL":
            case "DELETE": return InputConstants.KEY_DELETE;
            case "INS":
            case "INSERT": return InputConstants.KEY_INSERT;
            case "HOME": return InputConstants.KEY_HOME;
            case "END": return InputConstants.KEY_END;
            case "PGUP":
            case "PAGE_UP": return InputConstants.KEY_PAGEUP;
            case "PGDN":
            case "PAGE_DOWN": return InputConstants.KEY_PAGEDOWN;
            case "UP": return InputConstants.KEY_UP;
            case "DOWN": return InputConstants.KEY_DOWN;
            case "LEFT": return InputConstants.KEY_LEFT;
            case "RIGHT": return InputConstants.KEY_RIGHT;
            case "CAPS":
            case "CAPSLOCK": return InputConstants.KEY_CAPSLOCK;
            case "GRAVE": return InputConstants.KEY_GRAVE;
            case "MINUS": return InputConstants.KEY_MINUS;
            case "EQUALS": return InputConstants.KEY_EQUALS;
            case "LBRACKET": return InputConstants.KEY_LBRACKET;
            case "RBRACKET": return InputConstants.KEY_RBRACKET;
            case "SEMICOLON": return InputConstants.KEY_SEMICOLON;
            case "APOSTROPHE": return InputConstants.KEY_APOSTROPHE;
            case "COMMA": return InputConstants.KEY_COMMA;
            case "PERIOD": return InputConstants.KEY_PERIOD;
            case "SLASH": return InputConstants.KEY_SLASH;
            case "BACKSLASH": return InputConstants.KEY_BACKSLASH;
            case "F1": return InputConstants.KEY_F1;
            case "F2": return InputConstants.KEY_F2;
            case "F3": return InputConstants.KEY_F3;
            case "F4": return InputConstants.KEY_F4;
            case "F5": return InputConstants.KEY_F5;
            case "F6": return InputConstants.KEY_F6;
            case "F7": return InputConstants.KEY_F7;
            case "F8": return InputConstants.KEY_F8;
            case "F9": return InputConstants.KEY_F9;
            case "F10": return InputConstants.KEY_F10;
            case "F11": return InputConstants.KEY_F11;
            case "F12": return InputConstants.KEY_F12;
            default: break;
        }

        if (n.startsWith("KEY-")) {
            try {
                return Integer.parseInt(n.substring(4));
            } catch (NumberFormatException ignored) {
            }
        }
        return 0;
    }
}
