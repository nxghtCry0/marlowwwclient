package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public class ShieldDrain extends Module {
    private double attackDebt = 0.0;
    private long lastGuiTime = 0;

    public ShieldDrain() {
        super("ShieldDrain", Category.Combat, "Massively inflates CPS against shielding enemies to break/drain their shield.");
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("CPS", this, 90.0, 20.0, 200.0, true));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Range", this, 4.0, 1.0, 6.0, false));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Legit Mode", this, true));
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.level == null) return;

        if (mc.screen != null) {
            lastGuiTime = System.currentTimeMillis();
        }

        Setting legitSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Legit Mode");
        if (legitSetting != null && legitSetting.getValBoolean()) {
            if (mc.screen != null) {
                attackDebt = 0.0;
                return;
            }
            if (System.currentTimeMillis() - lastGuiTime < 3000) {
                attackDebt = 0.0;
                return;
            }
        }

        Setting cpsSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "CPS");
        Setting rangeSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Range");

        double cps = cpsSetting != null ? cpsSetting.getValDouble() : 90.0;
        double range = rangeSetting != null ? rangeSetting.getValDouble() : 4.0;

        Player bestTarget = null;
        double bestDist = range + 0.1;

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof Player target && target != mc.player && target.isAlive()) {
                double dist = mc.player.distanceTo(target);
                if (dist <= range && target.isBlocking()) {
                    if (dist < bestDist) {
                        bestDist = dist;
                        bestTarget = target;
                    }
                }
            }
        }

        if (bestTarget != null) {
            double attacksPerTick = cps / 20.0;
            attackDebt += attacksPerTick;

            int attacksThisTick = (int) attackDebt;
            if (attacksThisTick > 0) {
                for (int i = 0; i < attacksThisTick; i++) {
                    mc.gameMode.attack(mc.player, bestTarget);
                    mc.player.swing(InteractionHand.MAIN_HAND);
                }
                attackDebt -= attacksThisTick;
            }
        } else {
            attackDebt = 0.0;
        }
    }
}
