package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import net.minecraft.world.entity.ai.attributes.Attributes;
import java.util.Objects;

public class Reach extends Module {

    public Reach() {
        super("Reach", Category.Combat, "Slightly increases your interaction range.");
    }

    @Override
    public void onTick() {
        if (mc == null || mc.player == null) return;

        Setting reachSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Distance");
        double reachAdd = reachSetting != null ? reachSetting.getValDouble() : 0.5;

        if (mc.player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE) != null) {
            Objects.requireNonNull(mc.player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE)).setBaseValue(4.5 + reachAdd);
        }
        
        if (mc.player.getAttribute(Attributes.ENTITY_INTERACTION_RANGE) != null) {
            Objects.requireNonNull(mc.player.getAttribute(Attributes.ENTITY_INTERACTION_RANGE)).setBaseValue(3.0 + reachAdd);
        }
    }
    
    @Override
    public void onDisable() {
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