package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import net.minecraft.client.KeyMapping;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public class HitSelect extends Module {
    private boolean wasHit = false;
    private int punishDelayTicks = 0;
    private int lastHurtTime = 0;

    public HitSelect() {
        super("HitSelect", Category.Combat);
    }

    @Override
    public void onTick() {
        if (!isToggled() || mc.player == null) return;
        if (mc.player.hurtTime > 0 && lastHurtTime == 0) {
            wasHit = true;
            punishDelayTicks = 0;
        }
        lastHurtTime = mc.player.hurtTime;
        if (wasHit) {
            punishDelayTicks++;
        }
    }

    private boolean isCritState() {
        if (mc.player == null) return false;
        boolean falling = mc.player.getDeltaMovement().y < 0.0 && !mc.player.onGround();
        boolean notClimbing = !mc.player.onClimbable();
        boolean notInWater = !mc.player.isInWater() && !mc.player.isInLava();
        boolean notRiding = !mc.player.isPassenger();
        boolean notBlind = !mc.player.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS);
        return falling && notClimbing && notInWater && notRiding && notBlind;
    }

    public boolean canAttack(Entity target) {
        if (!this.isToggled() || mc.player == null) return true;

        Setting autoPunishSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Auto Punish");
        if (autoPunishSetting != null && autoPunishSetting.getValBoolean()) {
            if (wasHit) {
                Setting delaySetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Punish Delay (Ticks)");
                int punishDelay = delaySetting != null ? (int) delaySetting.getValDouble() : 3;
                if (punishDelayTicks < punishDelay) {
                    return false;
                }
                wasHit = false;
                return true;
            }
        }

        if (target instanceof LivingEntity livingTarget) {
            Setting modeSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Mode");
            String mode = modeSetting != null ? modeSetting.getValString() : "Pause";

            if (mode.equalsIgnoreCase("Pause")) {
                return livingTarget.hurtTime <= 0 && isCritState();
            } else if (mode.equalsIgnoreCase("Dynamic")) {
                if (livingTarget.hurtTime <= 0) {
                    return true;
                }
                return isCritState() && livingTarget.hurtTime <= 5;
            }
        }
        return true;
    }

    public void performPunishAttack() {
        KeyMapping attackKey = mc.options.keyAttack;
        KeyMapping.click(attackKey.getDefaultKey());
    }

    @Override
    public void onDisable() {
        wasHit = false;
        punishDelayTicks = 0;
        lastHurtTime = 0;
    }
}