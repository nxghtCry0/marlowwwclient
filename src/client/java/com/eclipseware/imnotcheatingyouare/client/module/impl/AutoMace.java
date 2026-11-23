package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.setting.SettingsManager;
import com.eclipseware.imnotcheatingyouare.client.utils.ModuleUtils;
import com.eclipseware.imnotcheatingyouare.client.utils.TimerUtil;
import com.eclipseware.imnotcheatingyouare.mixin.client.MinecraftAccessor;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class AutoMace extends Module {
    private final TimerUtil attackTimer = new TimerUtil();
    private final Map<Integer, Integer> slammedAt = new HashMap<>();

    private int savedSlot = -1;
    private double fallStartY = -1;
    private boolean isFalling = false;
    private boolean slamExecuted = false;
    private boolean maceHit = false;
    private int attackDelayOverride = -1;

    private int pendingStunTargetId = -1;
    private int pendingStunMaceSlot = -1;
    private int pendingStunWaitTicks = 0;
    private int pendingSlotRestoreTicks = -1;

    public AutoMace() {
        super("AutoMace", Category.Mace, "Automatically attacks with mace and executes visible two-tick axe stun slams.");
        ImnotcheatingyouareClient client = ImnotcheatingyouareClient.INSTANCE;
        if (client == null || client.settingsManager == null) {
            return;
        }
        SettingsManager sm = client.settingsManager;
        sm.rSetting(new Setting("Min Fall Distance", this, 3.0, 1.0, 10.0, false));
        sm.rSetting(new Setting("Attack Delay", this, 100.0, 0.0, 500.0, true));
        sm.rSetting(new Setting("Density Threshold", this, 7.0, 1.0, 20.0, false));
        sm.rSetting(new Setting("Target Players", this, true));
        sm.rSetting(new Setting("Target Mobs", this, false));
        sm.rSetting(new Setting("Stun Slam", this, false));
        sm.rSetting(new Setting("Two-Tick Stun Slam", this, true));
        sm.rSetting(new Setting("Axe Show Ticks", this, 1.0, 1.0, 4.0, true));
        sm.rSetting(new Setting("Only Axe", this, false));
        sm.rSetting(new Setting("Chain Slams", this, true));
        sm.rSetting(new Setting("Slam Min Fall", this, 1.5, 0.5, 10.0, false));
        sm.rSetting(new Setting("Auto Switch Mace", this, true));
        sm.rSetting(new Setting("Detect Mace", this, true));
        sm.rSetting(new Setting("Require Full Cooldown", this, true));
        sm.rSetting(new Setting("Stay On Mace", this, false));
        sm.rSetting(new Setting("Swing Prevention", this, true));
    }

    @Override
    public void onTick() {
        if (mc == null || mc.player == null || mc.level == null || mc.gameMode == null) {
            return;
        }
        if (tickPendingStunSlam()) {
            return;
        }
        tickPendingSlotRestore();
        updateFall();
        attack();
    }

    private boolean tickPendingStunSlam() {
        if (pendingStunTargetId == -1) {
            return false;
        }
        if (pendingStunWaitTicks > 1) {
            pendingStunWaitTicks--;
            return true;
        }

        Entity rawTarget = mc.level.getEntity(pendingStunTargetId);
        int maceSlot = pendingStunMaceSlot;
        pendingStunTargetId = -1;
        pendingStunMaceSlot = -1;
        pendingStunWaitTicks = 0;

        if (!(rawTarget instanceof LivingEntity target) || !target.isAlive()) {
            scheduleSlotRestore();
            return true;
        }
        if (mc.player.distanceTo(target) > 3.6f || mc.player.onGround()) {
            scheduleSlotRestore();
            return true;
        }
        if (maceSlot < 0 || maceSlot >= 9) {
            maceSlot = pickMaceSlot(currentFallDistance());
        }
        if (maceSlot == -1) {
            scheduleSlotRestore();
            return true;
        }

        switchToSlot(maceSlot);
        mc.gameMode.attack(mc.player, target);
        ModuleUtils.swingHand(mc.player, InteractionHand.MAIN_HAND);

        slammedAt.put(target.getId(), mc.player.tickCount);
        slamExecuted = true;
        maceHit = true;
        attackTimer.reset();
        scheduleSlotRestore();
        return true;
    }

    private void scheduleSlotRestore() {
        if (savedSlot != -1 && !getBoolSetting("Stay On Mace")) {
            pendingSlotRestoreTicks = 1;
        }
    }

    private void tickPendingSlotRestore() {
        if (pendingSlotRestoreTicks < 0) {
            return;
        }
        if (pendingSlotRestoreTicks > 0) {
            pendingSlotRestoreTicks--;
            return;
        }
        pendingSlotRestoreTicks = -1;
        if (savedSlot != -1 && !getBoolSetting("Stay On Mace")) {
            switchToSlot(savedSlot);
            savedSlot = -1;
        }
    }

    private void updateFall() {
        boolean onGround = mc.player.onGround();
        Vec3 vel = mc.player.getDeltaMovement();
        boolean falling = vel.y < -0.1;
        boolean rising = vel.y > 0.1;
        double currentY = mc.player.getY();

        if (onGround) {
            if (isFalling) {
                resetFall();
            }
            if (savedSlot != -1 && pendingStunTargetId == -1 && !getBoolSetting("Stay On Mace")) {
                switchToSlot(savedSlot);
                savedSlot = -1;
            }
            return;
        }
        if (rising && (maceHit || slamExecuted)) {
            maceHit = false;
            if (getBoolSetting("Chain Slams")) {
                slamExecuted = false;
            }
            fallStartY = currentY;
        }
        if (!isFalling) {
            isFalling = true;
            fallStartY = currentY;
            slamExecuted = false;
            maceHit = false;
        } else if (falling && fallStartY != -1 && currentY > fallStartY) {
            fallStartY = currentY;
        }
    }

    private double currentFallDistance() {
        double tracked = fallStartY == -1 ? 0.0 : Math.max(0.0, fallStartY - mc.player.getY());
        return Math.max(tracked, mc.player.fallDistance);
    }

    private void attack() {
        Vec3 vel = mc.player.getDeltaMovement();
        if (!isFalling || vel.y >= -0.1) {
            return;
        }
        double fallDist = currentFallDistance();
        if (fallDist < getDoubleSetting("Min Fall Distance")) {
            return;
        }
        HitResult hr = mc.hitResult;
        Entity target = hr != null && hr.getType() == HitResult.Type.ENTITY
            ? ((EntityHitResult) hr).getEntity()
            : null;
        if (!isValidTarget(target)) {
            return;
        }
        if (getBoolSetting("Stun Slam") && tryStunSlam(target, fallDist)) {
            return;
        }
        handleMaceAttack(target, fallDist);
    }

    private boolean tryStunSlam(Entity target, double fallDist) {
        if (!(target instanceof Player player) || !player.isBlocking()) {
            return false;
        }
        ItemStack activeUse = player.getUseItem();
        if (!activeUse.is(Items.SHIELD)) {
            return false;
        }
        if (fallDist < getDoubleSetting("Slam Min Fall")) {
            return false;
        }
        if (!getBoolSetting("Chain Slams") && slamExecuted) {
            return false;
        }
        Integer last = slammedAt.get(player.getId());
        if (last != null && mc.player.tickCount - last < 4) {
            return false;
        }

        ItemStack mainHand = mc.player.getMainHandItem();
        int axeSlot = getBoolSetting("Only Axe") && isAxe(mainHand)
            ? ModuleUtils.getSelectedSlot()
            : getAxeSlotId();
        if (axeSlot == -1) {
            return false;
        }
        int maceSlot = pickMaceSlot(fallDist);
        if (maceSlot == -1) {
            return false;
        }
        if (mc.player.distanceTo(player) > 3.3f) {
            return false;
        }

        if (savedSlot == -1) {
            savedSlot = ModuleUtils.getSelectedSlot();
        }

        switchToSlot(axeSlot);
        mc.gameMode.attack(mc.player, player);
        ModuleUtils.swingHand(mc.player, InteractionHand.MAIN_HAND);

        if (getBoolSetting("Two-Tick Stun Slam")) {
            int holdTicks = Math.max(1, (int) Math.round(getDoubleSetting("Axe Show Ticks")));
            pendingStunTargetId = player.getId();
            pendingStunMaceSlot = maceSlot;
            pendingStunWaitTicks = holdTicks;
            return true;
        }

        switchToSlot(maceSlot);
        mc.gameMode.attack(mc.player, player);
        ModuleUtils.swingHand(mc.player, InteractionHand.MAIN_HAND);

        slammedAt.put(player.getId(), mc.player.tickCount);
        slamExecuted = true;
        maceHit = true;
        attackTimer.reset();
        scheduleSlotRestore();
        return true;
    }

    private int pickMaceSlot(double fallDist) {
        int slot = -1;
        if (getBoolSetting("Detect Mace")) {
            slot = fallDist <= 20.0 ? findBreachMaceSlot() : findDensityMaceSlot();
        } else if (getBoolSetting("Auto Switch Mace")) {
            slot = fallDist >= getDoubleSetting("Density Threshold") ? findDensityMaceSlot() : findBreachMaceSlot();
        }
        if (slot == -1) {
            slot = fallDist <= 20.0 ? findDensityMaceSlot() : findBreachMaceSlot();
        }
        return slot != -1 ? slot : findAnyMaceSlot();
    }

    private void handleMaceAttack(Entity target, double fallDist) {
        if (maceHit) {
            return;
        }
        Inventory inv = mc.player.getInventory();
        if (!hasMace()) {
            if (savedSlot == -1) {
                savedSlot = inv.getSelectedSlot();
            }
            if (getBoolSetting("Auto Switch Mace") || getBoolSetting("Detect Mace")) {
                switchToAppropriateMace(fallDist);
            } else {
                switchToMace();
            }
        } else if (getBoolSetting("Auto Switch Mace") || getBoolSetting("Detect Mace")) {
            switchToAppropriateMace(fallDist);
        }
        if (getBoolSetting("Require Full Cooldown") && mc.player.getAttackStrengthScale(0.5f) < 1.0f) {
            return;
        }
        if (hasMace() && attackTimer.hasElapsedTime(getEffectiveAttackDelay(), true)) {
            MinecraftAccessor acc = (MinecraftAccessor) mc;
            acc.invokeStartAttack();
            maceHit = true;
            scheduleSlotRestore();
        }
    }

    private boolean isValidTarget(Entity entity) {
        if (entity == null || entity == mc.player) {
            return false;
        }
        if (!(entity instanceof LivingEntity livingEntity) || !livingEntity.isAlive()) {
            return false;
        }
        if (entity instanceof Player player) {
            if (!getBoolSetting("Target Players") || AntiBot.isBot(player) || Teams.isTeam(player)) {
                return false;
            }
            return true;
        }
        return getBoolSetting("Target Mobs");
    }

    private int getAxeSlotId() {
        Inventory inv = mc.player.getInventory();
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inv.getItem(i);
            if (isAxe(stack)) {
                return i;
            }
        }
        return -1;
    }

    private boolean isAxe(ItemStack stack) {
        return stack != null && stack.is(ItemTags.AXES);
    }

    private boolean hasMace() {
        ItemStack held = mc.player.getItemInHand(InteractionHand.MAIN_HAND);
        return held.is(Items.MACE);
    }

    private void switchToMace() {
        int anySlot = findAnyMaceSlot();
        if (anySlot != -1) {
            switchToSlot(anySlot);
        }
    }

    private void switchToAppropriateMace(double fallDistance) {
        int targetSlot = pickMaceSlot(fallDistance);
        if (targetSlot != -1) {
            switchToSlot(targetSlot);
        }
    }

    private int findDensityMaceSlot() {
        Inventory inv = mc.player.getInventory();
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.is(Items.MACE) && hasEnchantmentKeyword(stack, "density")) {
                return i;
            }
        }
        return -1;
    }

    private int findBreachMaceSlot() {
        Inventory inv = mc.player.getInventory();
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.is(Items.MACE) && hasEnchantmentKeyword(stack, "breach")) {
                return i;
            }
        }
        return -1;
    }

    private int findAnyMaceSlot() {
        Inventory inv = mc.player.getInventory();
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.is(Items.MACE)) {
                return i;
            }
        }
        return -1;
    }

    static boolean hasEnchantmentKeyword(ItemStack stack, String keyword) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        ItemEnchantments enchantments = stack.getEnchantments();
        for (Holder<Enchantment> holder : enchantments.keySet()) {
            Optional<ResourceKey<Enchantment>> keyOpt = holder.unwrapKey();
            if (keyOpt.isPresent()) {
                ResourceKey<Enchantment> key = keyOpt.get();
                String keyText = key.toString().toLowerCase();
                if (keyText.contains(keyword)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void switchToSlot(int slot) {
        if (slot >= 0 && slot < 9 && ModuleUtils.getSelectedSlot() != slot) {
            ModuleUtils.switchToSlot(slot);
        }
    }

    private void resetFall() {
        isFalling = false;
        fallStartY = -1;
        slamExecuted = false;
        maceHit = false;
    }

    private double getDoubleSetting(String name) {
        ImnotcheatingyouareClient client = ImnotcheatingyouareClient.INSTANCE;
        if (client == null || client.settingsManager == null) {
            return 0.0;
        }
        SettingsManager sm = client.settingsManager;
        Setting s = sm.getSettingByName(this, name);
        return s != null ? s.getValDouble() : 0.0;
    }

    private boolean getBoolSetting(String name) {
        ImnotcheatingyouareClient client = ImnotcheatingyouareClient.INSTANCE;
        if (client == null || client.settingsManager == null) {
            return false;
        }
        SettingsManager sm = client.settingsManager;
        Setting s = sm.getSettingByName(this, name);
        return s != null && s.getValBoolean();
    }

    @Override
    public void onEnable() {
        resetAll();
    }

    @Override
    public void onDisable() {
        if (savedSlot != -1 && mc != null && mc.player != null) {
            switchToSlot(savedSlot);
        }
        resetAll();
    }

    private void resetAll() {
        savedSlot = -1;
        fallStartY = -1;
        isFalling = false;
        slamExecuted = false;
        maceHit = false;
        pendingStunTargetId = -1;
        pendingStunMaceSlot = -1;
        pendingStunWaitTicks = 0;
        pendingSlotRestoreTicks = -1;
        attackTimer.reset();
    }

    public void setAttackDelayOverride(int delay) {
        this.attackDelayOverride = delay;
    }

    public void clearAttackDelayOverride() {
        this.attackDelayOverride = -1;
    }

    private int getEffectiveAttackDelay() {
        return attackDelayOverride >= 0 ? attackDelayOverride : (int) getDoubleSetting("Attack Delay");
    }
}
