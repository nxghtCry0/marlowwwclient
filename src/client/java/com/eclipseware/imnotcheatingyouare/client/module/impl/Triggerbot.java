package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class Triggerbot extends Module {
private int tickCounter = 0;
private int currentTargetDelay = 0;

public Triggerbot() {
        super("Triggerbot", Category.Combat);
    }

    @Override
    public void onTick() {
        // Guard against a missing player or world
        if (mc.player == null || mc.level == null) return;

        Setting modeSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager
                .getSettingByName(this, "Mode");
        String mode = modeSetting != null ? modeSetting.getValString() : "Legit";

        if ("Legit".equalsIgnoreCase(mode)) {
            runLegit();
        } else {
            runBlatant();
        }
    }

    private void runLegit() {
        if (mc.screen instanceof AbstractContainerScreen<?>) {
            tickCounter = 0;
            return;
        }

        if (mc.hitResult == null || mc.hitResult.getType() != HitResult.Type.ENTITY) {
            tickCounter = 0;
            return;
        }
        Entity target = ((EntityHitResult) mc.hitResult).getEntity();
        if (!isValidTarget(target)) {
            tickCounter = 0;
            return;
        }

        Setting rangeSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Range");
        double range = rangeSetting != null ? rangeSetting.getValDouble() : 4.25;
        if (mc.player.distanceToSqr(target) > (range * range)) {
            tickCounter = 0;
            return;
        }

        if (mc.player.getAttackStrengthScale(0.5f) < 1.0f) {
            tickCounter = 0;
            return;
        }

        tickCounter++;
        if (tickCounter >= currentTargetDelay) {
            Module hitSelectMod = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("HitSelect");
            if (hitSelectMod != null && hitSelectMod.isToggled() && hitSelectMod instanceof com.eclipseware.imnotcheatingyouare.client.module.impl.HitSelect hs) {
                if (!hs.canAttack(target)) {
                    return;
                }
            }

            boolean autoPunishActive = hitSelectMod != null && hitSelectMod.isToggled();

            if (autoPunishActive) {
                if (hitSelectMod instanceof com.eclipseware.imnotcheatingyouare.client.module.impl.HitSelect hs) {
                    hs.performPunishAttack();
                } else {
                    pressAttackKey();
                }
            } else {
                Setting simulateClick = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Simulate Mouse Click");
                if (simulateClick != null && simulateClick.getValBoolean()) {
                    pressAttackKey();
                } else {
                    mc.gameMode.attack(mc.player, target);
                    mc.player.swing(InteractionHand.MAIN_HAND);
                }
            }

            Setting minSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Min Delay (Ticks)");
            Setting maxSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Max Delay (Ticks)");
            
            int min = minSetting != null ? (int) minSetting.getValDouble() : 1;
            int max = maxSetting != null ? (int) maxSetting.getValDouble() : 4;
            
            if (min > max) {
                int temp = min;
                min = max;
                max = temp;
            }

            currentTargetDelay = min + (int) (Math.random() * ((max - min) + 1));
            tickCounter = 0;
        }
    }

    private void runBlatant() {
        if (mc.screen != null) return;
        if (mc.hitResult == null || mc.hitResult.getType() != HitResult.Type.ENTITY) return;

        Entity target = ((EntityHitResult) mc.hitResult).getEntity();
        if (!isValidTarget(target)) return;

        if (mc.player.getAttackStrengthScale(0.0f) < 1.0f) return;
// --> HitSelect Interception for Blatant Triggerbot <--
Module hitSelectMod = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("HitSelect");
if (hitSelectMod != null && hitSelectMod.isToggled() && hitSelectMod instanceof com.eclipseware.imnotcheatingyouare.client.module.impl.HitSelect hs) {
if (!hs.canAttack(target)) {
return; // Hold the attack!
}
}
Setting bypassSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Packet Bypass");
if (bypassSetting != null && bypassSetting.getValBoolean()) {
runPacketBypass();
}
mc.gameMode.attack(mc.player, target);
mc.player.swing(InteractionHand.MAIN_HAND);
    }

    private void runPacketBypass() {
        if (mc.getConnection() == null) return;
        int currentSlot = mc.player.getInventory().getSelectedSlot();
        int fakeSlot = (currentSlot + 1) % 9;

        mc.getConnection().send(new ServerboundSetCarriedItemPacket(fakeSlot));
        mc.getConnection().send(new ServerboundSetCarriedItemPacket(currentSlot));
    }

    private void pressAttackKey() {
KeyMapping attackKey = mc.options.keyAttack;

    // Properly simulate a click event in Mojang mappings
    KeyMapping.click(attackKey.getDefaultKey());
}

    public boolean shouldBlock(Entity target) {
        if (!this.isToggled() || mc.player == null || mc.level == null) return false;
        if (mc.screen != null) return false;
        if (mc.hitResult == null || mc.hitResult.getType() != HitResult.Type.ENTITY) return false;
        if (target != ((EntityHitResult) mc.hitResult).getEntity()) return false;
        if (!isValidTarget(target)) return false;

        Setting rangeSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Range");
        double range = rangeSetting != null ? rangeSetting.getValDouble() : 4.25;
        if (mc.player.distanceToSqr(target) > (range * range)) return false;

        if (mc.player.getAttackStrengthScale(0.0f) < 1.0f) return false;

        Module hitSelectMod = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("HitSelect");
        if (hitSelectMod != null && hitSelectMod.isToggled() && hitSelectMod instanceof com.eclipseware.imnotcheatingyouare.client.module.impl.HitSelect hs) {
            return !hs.canAttack(target);
        }

        return false;
    }

    private boolean isValidTarget(Entity entity) {
        if (!(entity instanceof LivingEntity)) return false;
        if (!entity.isAlive() || entity == mc.player) return false;

        Setting playersSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Players");
        Setting hostileSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Hostile Mobs");
        Setting passiveSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Passive Mobs");

        if (entity instanceof net.minecraft.world.entity.player.Player) {
            return playersSetting != null && playersSetting.getValBoolean();
        }

        if (entity instanceof Enemy) {
            return hostileSetting != null && hostileSetting.getValBoolean();
        }

        if (entity instanceof Animal || entity instanceof LivingEntity) {
            return passiveSetting != null && passiveSetting.getValBoolean();
        }

        return false;
    }
}