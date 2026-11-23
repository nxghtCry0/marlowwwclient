package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.setting.SettingsManager;
import com.eclipseware.imnotcheatingyouare.client.utils.ModuleUtils;
import com.eclipseware.imnotcheatingyouare.client.utils.TargetFilterManager;
import java.util.ArrayList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.PiercingWeapon;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class SpearMaceDTap extends Module {
    public static SpearMaceDTap INSTANCE;

    private enum Phase {
        IDLE,
        UNGLIDE_CHEST,
        WAIT_MACE,
        RESTORE_SLOT
    }

    private Phase phase = Phase.IDLE;
    private int targetEntityId = -1;
    private int savedSlot = -1;
    private int maceSlot = -1;
    private int waitTicks = 0;
    private int cooldownTicks = 0;
    private boolean internalAttackGuard = false;

    public SpearMaceDTap() {
        super("SpearMaceDTap", Category.Mace, "Hits falling targets with a Spear first (keeping fall distance) then slams with a Density Mace to double-pop.");
        INSTANCE = this;
        ImnotcheatingyouareClient client = ImnotcheatingyouareClient.INSTANCE;
        if (client == null || client.settingsManager == null) {
            return;
        }
        SettingsManager sm = client.settingsManager;
        ArrayList<String> modes = new ArrayList<>();
        modes.add("Auto");
        modes.add("On Attack");
        sm.rSetting(new Setting("Mode", this, "Auto", modes));
        sm.rSetting(new Setting("Min Fall Distance", this, 2.5, 1.5, 15.0, false));
        sm.rSetting(new Setting("Swap Delay (Ticks)", this, 1.0, 1.0, 4.0, true));
        sm.rSetting(new Setting("Range", this, 3.3, 2.0, 4.5, false));
        sm.rSetting(new Setting("Auto Un-Glide", this, true));
        sm.rSetting(new Setting("Prefer Piercing", this, true));
        sm.rSetting(new Setting("Swap Back", this, true));
        sm.rSetting(new Setting("Target Players", this, true));
        sm.rSetting(new Setting("Target Mobs", this, false));
    }

    public void onManualPlayerAttack(Entity target) {
        if (!isToggled() || internalAttackGuard || phase != Phase.IDLE || cooldownTicks > 0) {
            return;
        }
        if (mc == null || mc.player == null || mc.level == null || mc.gameMode == null) {
            return;
        }
        if (!isValidTarget(target)) {
            return;
        }
        ItemStack held = mc.player.getMainHandItem();
        if (!isSpearStack(held)) {
            return;
        }
        double minFall = getDouble("Min Fall Distance", 2.5);
        if (mc.player.fallDistance < minFall && !mc.player.isFallFlying()) {
            return;
        }
        int foundMace = findBestMaceSlot();
        if (foundMace == -1) {
            return;
        }
        savedSlot = ModuleUtils.getSelectedSlot();
        targetEntityId = target.getId();
        maceSlot = foundMace;
        if (mc.player.isFallFlying() && getBool("Auto Un-Glide", true) && findChestplateSlot() != -1) {
            phase = Phase.UNGLIDE_CHEST;
            waitTicks = 0;
        } else {
            phase = Phase.WAIT_MACE;
            waitTicks = Math.max(1, (int) Math.round(getDouble("Swap Delay (Ticks)", 1.0)));
        }
    }

    @Override
    public void onEnable() {
        resetSequence();
    }

    @Override
    public void onDisable() {
        restoreSlotNow();
        resetSequence();
    }

    @Override
    public void onTick() {
        if (mc == null || mc.player == null || mc.level == null || mc.gameMode == null) {
            resetSequence();
            return;
        }
        if (cooldownTicks > 0) {
            cooldownTicks--;
        }

        switch (phase) {
            case IDLE -> {
                if ("Auto".equalsIgnoreCase(getMode()) && cooldownTicks <= 0) {
                    tryStartAutoSequence();
                }
            }
            case UNGLIDE_CHEST -> executeUnglideStep();
            case WAIT_MACE -> executeMaceSlamStep();
            case RESTORE_SLOT -> {
                if (getBool("Swap Back", true)) {
                    restoreSlotNow();
                } else {
                    savedSlot = -1;
                }
                cooldownTicks = 6;
                resetSequence();
            }
        }
    }

    private void tryStartAutoSequence() {
        Vec3 vel = mc.player.getDeltaMovement();
        boolean gliding = mc.player.isFallFlying();
        if (mc.player.onGround() || (!gliding && vel.y >= -0.08)) {
            return;
        }
        double minFall = getDouble("Min Fall Distance", 2.5);
        if (!gliding && mc.player.fallDistance < minFall) {
            return;
        }

        Entity target = resolveCrosshairOrNearestTarget();
        if (target == null) {
            return;
        }

        int spearSlot = findBestSpearSlot();
        int bestMace = findBestMaceSlot();
        if (spearSlot == -1 || bestMace == -1) {
            return;
        }
        ItemStack offhand = mc.player.getOffhandItem();
        if (gliding && getBool("Auto Un-Glide", true) && findChestplateSlot() == -1 && !isChestplate(offhand)) {
            return;
        }

        savedSlot = ModuleUtils.getSelectedSlot();
        switchSlot(spearSlot);

        Inventory inv = mc.player.getInventory();
        ItemStack spearStack = inv.getItem(spearSlot);
        PiercingWeapon piercing = spearStack.get(DataComponents.PIERCING_WEAPON);
        HitResult hr = mc.hitResult;
        boolean onCrosshair = hr instanceof EntityHitResult ehr && ehr.getEntity() == target;

        internalAttackGuard = true;
        try {
            if (piercing != null && getBool("Prefer Piercing", true) && onCrosshair && !mc.player.cannotAttackWithItem(spearStack, 0)) {
                mc.gameMode.piercingAttack(SwingAnimation.DEFAULT, piercing);
                ModuleUtils.swingHand(mc.player, InteractionHand.MAIN_HAND);
            } else {
                mc.gameMode.attack(mc.player, target);
                ModuleUtils.swingHand(mc.player, InteractionHand.MAIN_HAND);
            }
        } finally {
            internalAttackGuard = false;
        }

        targetEntityId = target.getId();
        maceSlot = bestMace;

        if (gliding && getBool("Auto Un-Glide", true)) {
            phase = Phase.UNGLIDE_CHEST;
            waitTicks = 0;
        } else {
            phase = Phase.WAIT_MACE;
            waitTicks = Math.max(1, (int) Math.round(getDouble("Swap Delay (Ticks)", 1.0)));
        }
    }

    private void executeUnglideStep() {
        ItemStack offhand = mc.player.getOffhandItem();
        if (isChestplate(offhand)) {
            mc.gameMode.useItem(mc.player, InteractionHand.OFF_HAND);
            ModuleUtils.swingHand(mc.player, InteractionHand.OFF_HAND);
            phase = Phase.WAIT_MACE;
            waitTicks = 1;
            return;
        }
        int chestSlot = findChestplateSlot();
        if (chestSlot == -1) {
            restoreSlotNow();
            resetSequence();
            return;
        }
        switchSlot(chestSlot);
        mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
        ModuleUtils.swingHand(mc.player, InteractionHand.MAIN_HAND);
        phase = Phase.WAIT_MACE;
        waitTicks = Math.max(1, (int) Math.round(getDouble("Swap Delay (Ticks)", 1.0)));
    }

    private void executeMaceSlamStep() {
        if (waitTicks > 1) {
            waitTicks--;
            return;
        }
        Entity raw = mc.level.getEntity(targetEntityId);
        if (!(raw instanceof LivingEntity target) || !target.isAlive() || mc.player.onGround()) {
            phase = Phase.RESTORE_SLOT;
            return;
        }
        double maxRange = getDouble("Range", 3.3) + 0.35;
        if (mc.player.distanceTo(target) > maxRange) {
            phase = Phase.RESTORE_SLOT;
            return;
        }
        int slot = maceSlot >= 0 && maceSlot < 9 ? maceSlot : findBestMaceSlot();
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

    private Entity resolveCrosshairOrNearestTarget() {
        double maxRange = getDouble("Range", 3.3);
        HitResult hr = mc.hitResult;
        if (hr instanceof EntityHitResult ehr) {
            Entity hitEnt = ehr.getEntity();
            if (isValidTarget(hitEnt) && mc.player.distanceTo(hitEnt) <= maxRange) {
                return hitEnt;
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

    static int findBestSpearSlot() {
        if (mc == null || mc.player == null) {
            return -1;
        }
        Inventory inv = mc.player.getInventory();
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inv.getItem(i);
            if (isSpearStack(stack)) {
                return i;
            }
        }
        return -1;
    }

    static boolean isSpearStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (stack.get(DataComponents.PIERCING_WEAPON) != null) {
            return true;
        }
        Item item = stack.getItem();
        Identifier key = BuiltInRegistries.ITEM.getKey(item);
        String path = key.getPath().toLowerCase();
        return path.contains("spear");
    }

    static int findBestMaceSlot() {
        if (mc == null || mc.player == null) {
            return -1;
        }
        Inventory inv = mc.player.getInventory();
        int anyMace = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.is(Items.MACE)) {
                if (AutoMace.hasEnchantmentKeyword(stack, "density")) {
                    return i;
                }
                if (anyMace == -1) {
                    anyMace = i;
                }
            }
        }
        return anyMace;
    }

    static int findChestplateSlot() {
        if (mc == null || mc.player == null) {
            return -1;
        }
        ItemStack worn = mc.player.getItemBySlot(EquipmentSlot.CHEST);
        if (!worn.is(Items.ELYTRA)) {
            return -1;
        }
        Inventory inv = mc.player.getInventory();
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inv.getItem(i);
            if (isChestplate(stack)) {
                return i;
            }
        }
        return -1;
    }

    static boolean isChestplate(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        return stack.is(Items.NETHERITE_CHESTPLATE)
            || stack.is(Items.DIAMOND_CHESTPLATE)
            || stack.is(Items.IRON_CHESTPLATE)
            || stack.is(Items.GOLDEN_CHESTPLATE)
            || stack.is(Items.CHAINMAIL_CHESTPLATE)
            || stack.is(Items.COPPER_CHESTPLATE)
            || stack.is(Items.LEATHER_CHESTPLATE);
    }

    private void switchSlot(int slot) {
        if (slot >= 0 && slot < 9 && ModuleUtils.getSelectedSlot() != slot) {
            ModuleUtils.switchToSlot(slot);
        }
    }

    private void restoreSlotNow() {
        if (savedSlot >= 0 && savedSlot < 9 && mc != null && mc.player != null) {
            switchSlot(savedSlot);
        }
        savedSlot = -1;
    }

    private void resetSequence() {
        phase = Phase.IDLE;
        targetEntityId = -1;
        maceSlot = -1;
        waitTicks = 0;
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
