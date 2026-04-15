package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public class Fullbright extends Module {

    public Fullbright() {
        super("Fullbright", Category.Render, "Lights up your world like it's daytime!");
    }

    @Override
    public void onTick() {
        if (mc.player == null) return;
        
        // Adds Night Vision with infinite duration. 
        // The three 'false' parameters completely hide the particles, ambient effects, and the UI icon!
        mc.player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 999999, 0, false, false, false));
    }

    @Override
    public void onDisable() {
        if (mc.player != null) {
            mc.player.removeEffect(MobEffects.NIGHT_VISION);
        }
    }
}