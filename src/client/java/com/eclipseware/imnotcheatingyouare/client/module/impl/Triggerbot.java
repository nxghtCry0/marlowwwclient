package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.utils.cheat.AntiCheatProfile;
import com.eclipseware.imnotcheatingyouare.client.utils.cheat.ClickConsistency;
import com.eclipseware.imnotcheatingyouare.client.utils.cheat.GCDFix;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import com.eclipseware.imnotcheatingyouare.client.utils.FriendManager;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class Triggerbot extends Module {
    private static long lastTimePacketMs = 0;
    private static float serverTps = 20.0f;

    private long targetDelayMs = 0L;
    private long lastAttackMs = 0L;
    private boolean wasMouseDown = false;

    public Triggerbot() {
        super("Triggerbot", Category.Combat);
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("TPS Sync", this, true));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("CritOnly", this, false));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("AirCrit", this, false));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Require Mouse Down", this, false));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Ignore Activation Click", this, true));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Miss Hit Chance", this, 0.0, 0.0, 100.0, false));
    }

    public static void onUpdateTimePacket() {
        long now = System.currentTimeMillis();
        if (lastTimePacketMs != 0) {
            long diff = now - lastTimePacketMs;
            if (diff > 0) {
                float tps = 20000.0f / (float) diff;
                if (tps > 20.0f) tps = 20.0f;
                if (tps < 1.0f) tps = 1.0f;
                serverTps = (serverTps * 0.9f) + (tps * 0.1f);
            }
        }
        lastTimePacketMs = now;
    }

    @Override
    public void onEnable() {
        targetDelayMs = 0L;
        lastAttackMs = System.currentTimeMillis();
        wasMouseDown = false;
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.level == null) return;
        GCDFix.update(mc.options.sensitivity().get());
        runTriggerbot();
    }

    private void runTriggerbot() {
        if (mc.gui.screen() != null) { wasMouseDown = false; return; }

        Setting reqMouseSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Require Mouse Down");
        boolean requireMouseDown = reqMouseSetting != null && reqMouseSetting.getValBoolean();
        boolean isMouseDown = mc.options.keyAttack.isDown();

        if (requireMouseDown && !isMouseDown) {
            wasMouseDown = false;
            return;
        }

        boolean isActivationClick = isMouseDown && !wasMouseDown;
        wasMouseDown = isMouseDown;

        if (mc.hitResult == null || mc.hitResult.getType() != HitResult.Type.ENTITY) {
            attemptMissHit();
            return;
        }
        Entity target = ((EntityHitResult) mc.hitResult).getEntity();
        if (!isValidTarget(target)) {
            attemptMissHit();
            return;
        }
        Setting rangeSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Range");
        double range = rangeSetting != null ? rangeSetting.getValDouble() : 4.25;
        if (mc.player.distanceToSqr(target) > (range * range)) return;
        
        float attackCooldown = mc.player.getAttackStrengthScale(0.5f);

        Setting ignoreActivationSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Ignore Activation Click");
        boolean ignoreActivation = ignoreActivationSetting != null && ignoreActivationSetting.getValBoolean();
        if (requireMouseDown && isActivationClick && ignoreActivation) {
            attackCooldown = 1.0f;
        }

        Setting tpsSyncSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "TPS Sync");
        if (tpsSyncSetting != null && tpsSyncSetting.getValBoolean() && serverTps < 19.5f) {
            float scale = 20.0f / serverTps;
            attackCooldown *= scale;
        }
        if (attackCooldown < 1.0f) return;

        Setting critOnlySetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "CritOnly");
        if (critOnlySetting != null && critOnlySetting.getValBoolean()) {
            if (mc.player.onGround() || mc.player.fallDistance <= 0.0f) {
                return;
            }
        }

        Setting airCritSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "AirCrit");
        if (airCritSetting != null && airCritSetting.getValBoolean()) {
            if (!mc.player.onGround() && mc.player.fallDistance <= 0.0f) {
                return;
            }
        }

        if (System.currentTimeMillis() - lastAttackMs >= targetDelayMs) {
            long profileMin = AntiCheatProfile.safeTriggerMinDelayMs();
            if (!ClickConsistency.shouldClick(profileMin, 14)) return;
            Module hitSelectMod = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("HitSelect");
            if (hitSelectMod != null && hitSelectMod.isToggled() &&
                hitSelectMod instanceof HitSelect hs && !hs.canAttack(target)) return;
            
            isTriggerbotAttacking = true;
            try {
                ((com.eclipseware.imnotcheatingyouare.mixin.client.MinecraftAccessor) mc).invokeStartAttack();
            } finally {
                isTriggerbotAttacking = false;
            }
            mc.player.resetAttackStrengthTicker();
            
            lastAttackMs = System.currentTimeMillis();
            Setting minSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Min Delay (ms)");
            Setting maxSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Max Delay (ms)");
            int min = minSetting != null ? (int) minSetting.getValDouble() : 50;
            int max = maxSetting != null ? (int) maxSetting.getValDouble() : 150;
            if (min > max) { int t = min; min = max; max = t; }
            targetDelayMs = min + (long) (Math.random() * ((max - min) + 1));
        }
    }

    private void attemptMissHit() {
        Setting missChanceSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Miss Hit Chance");
        double missChance = missChanceSetting != null ? missChanceSetting.getValDouble() : 0.0;
        if (missChance <= 0.0) return;

        float attackCooldown = mc.player.getAttackStrengthScale(0.5f);

        Setting tpsSyncSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "TPS Sync");
        if (tpsSyncSetting != null && tpsSyncSetting.getValBoolean() && serverTps < 19.5f) {
            float scale = 20.0f / serverTps;
            attackCooldown *= scale;
        }
        if (attackCooldown < 1.0f) return;

        if (System.currentTimeMillis() - lastAttackMs < targetDelayMs) return;
        if (Math.random() * 100.0 >= missChance) return;

        long profileMin = AntiCheatProfile.safeTriggerMinDelayMs();
        if (!ClickConsistency.shouldClick(profileMin, 14)) return;

        isTriggerbotAttacking = true;
        try {
            ((com.eclipseware.imnotcheatingyouare.mixin.client.MinecraftAccessor) mc).invokeStartAttack();
        } finally {
            isTriggerbotAttacking = false;
        }
        mc.player.resetAttackStrengthTicker();

        lastAttackMs = System.currentTimeMillis();
        Setting minSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Min Delay (ms)");
        Setting maxSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Max Delay (ms)");
        int min = minSetting != null ? (int) minSetting.getValDouble() : 50;
        int max = maxSetting != null ? (int) maxSetting.getValDouble() : 150;
        if (min > max) { int t = min; min = max; max = t; }
        targetDelayMs = min + (long) (Math.random() * ((max - min) + 1));
    }

    public static boolean isTriggerbotAttacking = false;

    public static boolean shouldCancelManualAttack() {
        if (isTriggerbotAttacking) return false;
        Module mod = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("Triggerbot");
        if (mod == null || !mod.isToggled() || !(mod instanceof Triggerbot tb)) return false;
        Setting req = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(mod, "Require Mouse Down");
        if (req == null || !req.getValBoolean()) return false;
        Setting ign = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(mod, "Ignore Activation Click");
        if (ign == null || !ign.getValBoolean()) return false;

        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.hitResult == null || mc.hitResult.getType() != HitResult.Type.ENTITY) return false;
        Entity target = ((EntityHitResult) mc.hitResult).getEntity();
        if (!tb.isValidTarget(target)) return false;
        Setting rangeSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(mod, "Range");
        double range = rangeSetting != null ? rangeSetting.getValDouble() : 4.25;
        return mc.player != null && mc.player.distanceToSqr(target) <= (range * range);
    }

    public boolean shouldBlock(Entity target) {
        if (!this.isToggled() || mc.player == null || mc.level == null) return false;
        if (mc.gui.screen() != null) return false;
        if (mc.hitResult == null || mc.hitResult.getType() != HitResult.Type.ENTITY) return false;
        if (target != ((EntityHitResult) mc.hitResult).getEntity()) return false;
        if (!isValidTarget(target)) return false;
        Setting rangeSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Range");
        double range = rangeSetting != null ? rangeSetting.getValDouble() : 4.25;
        if (mc.player.distanceToSqr(target) > (range * range)) return false;
        if (mc.player.getAttackStrengthScale(0.0f) < 1.0f) return false;
        Module hitSelectMod = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("HitSelect");
        if (hitSelectMod != null && hitSelectMod.isToggled() && hitSelectMod instanceof HitSelect hs) {
            return !hs.canAttack(target);
        }
        return false;
    }

    private boolean isValidTarget(Entity entity) {
        if (!(entity instanceof LivingEntity)) return false;
        if (!entity.isAlive() || entity == mc.player) return false;
        if (com.eclipseware.imnotcheatingyouare.client.utils.TargetFilterManager.isFiltered(entity)) return false;
        if (entity instanceof net.minecraft.world.entity.player.Player p && FriendManager.isFriend(p)) return false;
        Setting weaponsOnlySetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Weapons Only");
        if (weaponsOnlySetting != null && weaponsOnlySetting.getValBoolean()) {
            net.minecraft.world.item.Item mainHand = mc.player.getMainHandItem().getItem();
            String name = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(mainHand).getPath();
            if (!name.contains("sword") && !name.contains("axe") && !name.contains("mace")) {
                return false;
            }
        }
        Setting playersSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Players");
        Setting hostileSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Hostile Mobs");
        Setting passiveSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Passive Mobs");
        if (entity instanceof net.minecraft.world.entity.player.Player)
            return playersSetting != null && playersSetting.getValBoolean();
        Module npcMod = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("NPC");
        if (npcMod == null || !npcMod.isToggled()) return false;
        if (entity instanceof Enemy)
            return hostileSetting != null && hostileSetting.getValBoolean();
        if (entity instanceof Animal || entity instanceof LivingEntity)
            return passiveSetting != null && passiveSetting.getValBoolean();
        return false;
    }
}
