package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public class Fullbright extends Module {

    public Fullbright() {
        super("Fullbright", Category.Render);
    }

    @Override
    public void onTick() {
        if (mc.player == null) return;
        Setting mode = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Mode");
        boolean useNV = mode != null && mode.getValString().equals("Night Vision");

        if (useNV) {
            // Invisible Night Vision
            mc.player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0, false, false, false));
        } else {
            // If they switched to Gamma, remove the potion effect
            if (mc.player.hasEffect(MobEffects.NIGHT_VISION)) {
                mc.player.removeEffect(MobEffects.NIGHT_VISION);
            }
        }
    }

    @Override
    public void onDisable() {
        if (mc.player != null && mc.player.hasEffect(MobEffects.NIGHT_VISION)) {
            mc.player.removeEffect(MobEffects.NIGHT_VISION);
        }
    }
}