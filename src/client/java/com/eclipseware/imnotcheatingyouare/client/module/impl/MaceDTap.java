package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.setting.SettingsManager;
import com.eclipseware.imnotcheatingyouare.client.utils.ModuleUtils;
import com.eclipseware.imnotcheatingyouare.client.utils.TargetFilterManager;
import java.util.ArrayList;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class MaceDTap extends Module {
    public static MaceDTap INSTANCE;

    private enum Phase {
        IDLE,
        UNGLIDE_CHEST,
        DENSITY_SLAM,
        RESTORE_SLOT,
        WAIT_LAND_REEQUIP,
        REEQUIP_ELYTRA_USE,
        REEQUIP_RESTORE
    }

    private Phase phase = Phase.IDLE;
    private int targetEntityId = -1;
    private int savedSlot = -1;
    private int densitySlot = -1;
    private int stepDelayTicks = 0;
    private int cooldownTicks = 0;
    private boolean unglidedFromElytra = false;
    private boolean internalAttackGuard = false;

    public MaceDTap() {
        super("MaceDTap", Category.Mace, "Pops totems twice in one fall by hitting with a Breach Mace (in Elytra or fast swap) then slamming with a Density Mace.");
        INSTANCE = this;
        ImnotcheatingyouareClient client = ImnotcheatingyouareClient.INSTANCE;
        if (client == null || client.settingsManager == null) {
            return;
        }
        SettingsManager sm = client.settingsManager;
        ArrayList<String> modes = new ArrayList<>();
        modes.add("Auto");
        modes.add("Elytra Un-Glide");
        modes.add("Fast Swap");
        sm.rSetting(new Setting("Mode", this, "Auto", modes));
        sm.rSetting(new Setting("Min Fall Distance", this, 2.5, 1.5, 15.0, false));
        sm.rSetting(new Setting("Swap Delay (Ticks)", this, 1.0, 1.0, 4.0, true));
        sm.rSetting(new Setting("Range", this, 3.3, 2.0, 4.5, false));
        sm.rSetting(new Setting("Trigger On Manual Hit", this, true));
        sm.rSetting(new Setting("Re-Equip Elytra", this, true));
        sm.rSetting(new Setting("Swap Back", this, true));
        sm.rSetting(new Setting("Target Players", this, true));
        sm.rSetting(new Setting("Target Mobs", this, false));
    }

    public void onManualPlayerAttack(Entity target) {
        if (!isToggled() || internalAttackGuard || phase != Phase.IDLE || cooldownTicks > 0) {
            return;
        }
        if (!getBool("Trigger On Manual Hit", true) || mc == null || mc.player == null || mc.level == null) {
            return;
        }
        if (!isValidTarget(target)) {
            return;
        }
        ItemStack held = mc.player.getMainHandItem();
        if (!held.is(Items.MACE) || !AutoMace.hasEnchantmentKeyword(held, "breach")) {
            return;
        }
        int foundDensity = findDensityOrAlternateMaceSlot(ModuleUtils.getSelectedSlot());
        if (foundDensity == -1) {
            return;
        }

        boolean gliding = mc.player.isFallFlying();
        if ("Elytra Un-Glide".equalsIgnoreCase(getMode()) && !gliding) {
            return;
        }

        savedSlot = ModuleUtils.getSelectedSlot();
        targetEntityId = target.getId();
        densitySlot = foundDensity;
        unglidedFromElytra = false;

        if (gliding && !"Fast Swap".equalsIgnoreCase(getMode()) && hasChestplateReady()) {
            phase = Phase.UNGLIDE_CHEST;
            stepDelayTicks = 0;
        } else {
            phase = Phase.DENSITY_SLAM;
            stepDelayTicks = Math.max(1, (int) Math.round(getDouble("Swap Delay (Ticks)", 1.0)));
        }
    }

    @Override
    public void onEnable() {
        resetState();
    }

    @Override
    public void onDisable() {
        restoreSavedSlot();
        resetState();
    }

    @Override
    public void onTick() {
        if (mc == null || mc.player == null || mc.level == null || mc.gameMode == null) {
            resetState();
            return;
        }
        if (cooldownTicks > 0) {
            cooldownTicks--;
        }

        switch (phase) {
            case IDLE -> {
                if (cooldownTicks <= 0) {
                    tryStartAutoDTap();
                }
            }
            case UNGLIDE_CHEST -> runUnglideChestStep();
            case DENSITY_SLAM -> runDensitySlamStep();
            case RESTORE_SLOT -> {
                if (getBool("Swap Back", true)) {
                    restoreSavedSlot();
                } else {
                    savedSlot = -1;
                }
                cooldownTicks = 6;
                if (unglidedFromElytra && getBool("Re-Equip Elytra", true)) {
                    phase = Phase.WAIT_LAND_REEQUIP;
                } else {
                    resetState();
                }
            }
            case WAIT_LAND_REEQUIP -> runWaitLandReequip();
            case REEQUIP_ELYTRA_USE -> {
                mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
                ModuleUtils.swingHand(mc.player, InteractionHand.MAIN_HAND);
                phase = Phase.REEQUIP_RESTORE;
            }
            case REEQUIP_RESTORE -> {
                restoreSavedSlot();
                resetState();
            }
        }
    }

    private void tryStartAutoDTap() {
        String mode = getMode();
        boolean gliding = mc.player.isFallFlying();
        Vec3 vel = mc.player.getDeltaMovement();
        if (mc.player.onGround() || vel.y >= -0.05) {
            return;
        }
        if ("Elytra Un-Glide".equalsIgnoreCase(mode) && !gliding) {
            return;
        }
        double minFall = getDouble("Min Fall Distance", 2.5);
        double effectiveFall = Math.max(mc.player.fallDistance, gliding ? 3.0 : 0.0);
        if (effectiveFall < minFall) {
            return;
        }

        int breachSlot = findBreachMaceSlot();
        if (breachSlot == -1) {
            return;
        }
        int densSlot = findDensityOrAlternateMaceSlot(breachSlot);
        if (densSlot == -1) {
            return;
        }
        boolean shouldUnglide = gliding && !"Fast Swap".equalsIgnoreCase(mode);
        if (shouldUnglide && !hasChestplateReady()) {
            if ("Elytra Un-Glide".equalsIgnoreCase(mode)) {
                return;
            }
            shouldUnglide = false;
        }

        Entity target = resolveTarget();
        if (target == null) {
            return;
        }

        savedSlot = ModuleUtils.getSelectedSlot();
        switchSlot(breachSlot);

        internalAttackGuard = true;
        try {
            mc.gameMode.attack(mc.player, target);
            ModuleUtils.swingHand(mc.player, InteractionHand.MAIN_HAND);
        } finally {
            internalAttackGuard = false;
        }

        targetEntityId = target.getId();
        densitySlot = densSlot;
        unglidedFromElytra = false;

        if (shouldUnglide) {
            phase = Phase.UNGLIDE_CHEST;
            stepDelayTicks = 0;
        } else {
            phase = Phase.DENSITY_SLAM;
            stepDelayTicks = Math.max(1, (int) Math.round(getDouble("Swap Delay (Ticks)", 1.0)));
        }
    }

    private void runUnglideChestStep() {
        ItemStack offhand = mc.player.getOffhandItem();
        if (SpearMaceDTap.isChestplate(offhand)) {
            mc.gameMode.useItem(mc.player, InteractionHand.OFF_HAND);
            ModuleUtils.swingHand(mc.player, InteractionHand.OFF_HAND);
            unglidedFromElytra = true;
            phase = Phase.DENSITY_SLAM;
            stepDelayTicks = 1;
            return;
        }
        int chestSlot = SpearMaceDTap.findChestplateSlot();
        if (chestSlot == -1) {
            phase = Phase.RESTORE_SLOT;
            return;
        }
        switchSlot(chestSlot);
        mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
        ModuleUtils.swingHand(mc.player, InteractionHand.MAIN_HAND);
        unglidedFromElytra = true;
        phase = Phase.DENSITY_SLAM;
        stepDelayTicks = Math.max(1, (int) Math.round(getDouble("Swap Delay (Ticks)", 1.0)));
    }

    private void runDensitySlamStep() {
        if (stepDelayTicks > 1) {
            stepDelayTicks--;
            return;
        }
        Entity raw = mc.level.getEntity(targetEntityId);
        if (!(raw instanceof LivingEntity target) || !target.isAlive() || mc.player.onGround()) {
            phase = Phase.RESTORE_SLOT;
            return;
        }
        double maxRange = getDouble("Range", 3.3) + 0.4;
        if (mc.player.distanceTo(target) > maxRange) {
            phase = Phase.RESTORE_SLOT;
            return;
        }
        int slot = densitySlot >= 0 && densitySlot < 9 ? densitySlot : findDensityOrAlternateMaceSlot(-1);
        if (slot == -1) {
            phase = Phase.RESTORE_SLOT;
            return;
        }

        switchSlot(slot);
        internalAttackGuard = true;
        try {
            mc.gameMode.attack(mc.player, target);
            ModuleUtils.swingHand(mc.player, InteractionHand.MAIN_HAND);
        } finally {
            internalAttackGuard = false;
        }
        phase = Phase.RESTORE_SLOT;
    }

    private void runWaitLandReequip() {
        Vec3 motion = mc.player.getDeltaMovement();
        if (!mc.player.onGround() && motion.y <= 0.1) {
            return;
        }
        ItemStack chest = mc.player.getItemBySlot(EquipmentSlot.CHEST);
        if (chest.is(Items.ELYTRA)) {
            resetState();
            return;
        }
        int elytraSlot = findElytraSlot();
        if (elytraSlot == -1) {
            resetState();
            return;
        }
        savedSlot = ModuleUtils.getSelectedSlot();
        switchSlot(elytraSlot);
        phase = Phase.REEQUIP_ELYTRA_USE;
    }

    private boolean hasChestplateReady() {
        ItemStack offhand = mc.player.getOffhandItem();
        return SpearMaceDTap.isChestplate(offhand) || SpearMaceDTap.findChestplateSlot() != -1;
    }

    private int findBreachMaceSlot() {
        Inventory inv = mc.player.getInventory();
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.is(Items.MACE) && AutoMace.hasEnchantmentKeyword(stack, "breach")) {
                return i;
            }
        }
        return -1;
    }

    private int findDensityOrAlternateMaceSlot(int excludeSlot) {
        Inventory inv = mc.player.getInventory();
        int fallbackMace = -1;
        for (int i = 0; i < 9; i++) {
            if (i == excludeSlot) {
                continue;
            }
            ItemStack stack = inv.getItem(i);
            if (stack.is(Items.MACE)) {
                if (AutoMace.hasEnchantmentKeyword(stack, "density")) {
                    return i;
                }
                if (fallbackMace == -1) {
                    fallbackMace = i;
                }
            }
        }
        return fallbackMace;
    }

    private int findElytraSlot() {
        Inventory inv = mc.player.getInventory();
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.is(Items.ELYTRA)) {
                return i;
            }
        }
        return -1;
    }

    private Entity resolveTarget() {
        double maxRange = getDouble("Range", 3.3);
        HitResult hr = mc.hitResult;
        if (hr instanceof EntityHitResult ehr) {
            Entity hit = ehr.getEntity();
            if (isValidTarget(hit) && mc.player.distanceTo(hit) <= maxRange) {
                return hit;
            }
        }
        Entity best = null;
        double bestDist = maxRange;
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!isValidTarget(entity)) {
                continue;
            }
            double d = mc.player.distanceTo(entity);
            if (d <= bestDist) {
                bestDist = d;
                best = entity;
            }
        }
        return best;
    }

    private boolean isValidTarget(Entity entity) {
        if (entity == null || entity == mc.player || !(entity instanceof LivingEntity living) || !living.isAlive()) {
            return false;
        }
        if (TargetFilterManager.isFiltered(entity)) {
            return false;
        }
        if (entity instanceof Player player) {
            if (!getBool("Target Players", true) || AntiBot.isBot(player) || Teams.isTeam(player)) {
                return false;
            }
            return true;
        }
        return getBool("Target Mobs", false);
    }

    private void switchSlot(int slot) {
        if (slot >= 0 && slot < 9 && ModuleUtils.getSelectedSlot() != slot) {
            ModuleUtils.switchToSlot(slot);
        }
    }

    private void restoreSavedSlot() {
        if (savedSlot >= 0 && savedSlot < 9 && mc != null && mc.player != null) {
            switchSlot(savedSlot);
        }
        savedSlot = -1;
    }

    private void resetState() {
        phase = Phase.IDLE;
        targetEntityId = -1;
        densitySlot = -1;
        stepDelayTicks = 0;
        unglidedFromElytra = false;
    }

    private String getMode() {
        Setting s = setting("Mode");
        return s != null ? s.getValString() : "Auto";
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
