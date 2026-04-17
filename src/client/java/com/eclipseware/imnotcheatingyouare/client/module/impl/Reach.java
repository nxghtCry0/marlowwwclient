package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.utils.cheat.AntiCheatProfile;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.world.entity.ai.attributes.Attributes;
import java.util.Objects;

public class Reach extends Module {

    // Vanilla entity reach is 3.0, block reach is 4.5
    private static final double BASE_ENTITY = 3.0;
    private static final double BASE_BLOCK  = 4.5;

    public Reach() {
        super("Reach", Category.Combat, "Slightly increases your interaction range.");
    }

    @Override
    public void onTick() {
        if (mc == null || mc.player == null) return;

        Setting reachSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Distance");
        double rawExtra = reachSetting != null ? reachSetting.getValDouble() : 0.3;

        // Clamp to what's safe for the current AC profile
        double extra = Math.min(rawExtra, AntiCheatProfile.safeReachExtra());

        if (isToggled()) {
            // Randomise the last decimal slightly each tick so it doesn't
            // produce a perfectly constant attribute value (Grim/Polar check this)
            double noise = (Math.random() - 0.5) * 0.02;
            Objects.requireNonNull(mc.player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE))
                   .setBaseValue(BASE_BLOCK  + extra + noise);
            Objects.requireNonNull(mc.player.getAttribute(Attributes.ENTITY_INTERACTION_RANGE))
                   .setBaseValue(BASE_ENTITY + extra + noise);
        } else {
            Objects.requireNonNull(mc.player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE))
                   .setBaseValue(BASE_BLOCK);
            Objects.requireNonNull(mc.player.getAttribute(Attributes.ENTITY_INTERACTION_RANGE))
                   .setBaseValue(BASE_ENTITY);
        if (mc.player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE) != null) {
            Objects.requireNonNull(mc.player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE)).setBaseValue(4.5 + reachAdd);
        }
        
        if (mc.player.getAttribute(Attributes.ENTITY_INTERACTION_RANGE) != null) {
            Objects.requireNonNull(mc.player.getAttribute(Attributes.ENTITY_INTERACTION_RANGE)).setBaseValue(3.0 + reachAdd);
        }
    }

    @Override
    public void onDisable() {
        if (mc.player != null) {
            Objects.requireNonNull(mc.player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE))
                   .setBaseValue(BASE_BLOCK);
            Objects.requireNonNull(mc.player.getAttribute(Attributes.ENTITY_INTERACTION_RANGE))
                   .setBaseValue(BASE_ENTITY);
        if (mc != null && mc.player != null) {
            if (mc.player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE) != null) {
                Objects.requireNonNull(mc.player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE)).setBaseValue(4.5);
            }
            if (mc.player.getAttribute(Attributes.ENTITY_INTERACTION_RANGE) != null) {
                Objects.requireNonNull(mc.player.getAttribute(Attributes.ENTITY_INTERACTION_RANGE)).setBaseValue(3.0);
            }
        }
    }
}
