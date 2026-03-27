package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public class HitSelect extends Module {
    public HitSelect() {
        super("HitSelect", Category.Combat);
    }

    public boolean canAttack(Entity target) {
        if (!this.isToggled() || mc.player == null) return true;

        Setting modeSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Mode");
        String mode = modeSetting != null ? modeSetting.getValString() : "HurtTime";

        if (target instanceof LivingEntity) {
            LivingEntity livingTarget = (LivingEntity) target;
            
            if (mode.equals("Criticals")) {
                // Critical hit requirements: Falling down, not on the ground, not climbing, and not in water
                return !mc.player.onGround() && mc.player.getDeltaMovement().y < 0.0 && !mc.player.onClimbable() && !mc.player.isInWater();
            } else if (mode.equals("HurtTime")) {
                // HurtTime requirements: Only hit when the target's i-frames (hurtTime) are below a specific threshold
                Setting hurtTimeSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Max HurtTime");
                int maxHurtTime = hurtTimeSetting != null ? (int) hurtTimeSetting.getValDouble() : 5;
                return livingTarget.hurtTime <= maxHurtTime;
            }
        }
        return true;
    }
}