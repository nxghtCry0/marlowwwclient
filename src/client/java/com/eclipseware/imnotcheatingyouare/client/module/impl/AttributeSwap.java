package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.entity.Entity;

public class AttributeSwap extends Module {
    public static AttributeSwap INSTANCE;
    private boolean needsSwapBack = false;
    private int originalSlot = -1;
    private int ticksWaited = 0;

    public AttributeSwap() {
        super("AttributeSwap", Category.Combat, "Attacks with Primary weapon and swaps to Secondary in the same tick.");
        INSTANCE = this;
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Primary Slot (0=Any)", this, 1.0, 0.0, 9.0, true));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Secondary Slot", this, 2.0, 1.0, 9.0, true));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Fix Delay (Ticks)", this, 1.0, 0.0, 10.0, true));
    }

    public void onAttack(Entity target) {
        if (!isToggled() || mc.player == null) return;
        
        Setting primarySetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Primary Slot (0=Any)");
        Setting secondarySetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Secondary Slot");
        
        int primary = primarySetting != null ? (int) primarySetting.getValDouble() - 1 : 0;
        int secondary = secondarySetting != null ? (int) secondarySetting.getValDouble() - 1 : 1;
        
        int currentSlot = mc.player.getInventory().getSelectedSlot();
        
        if (primary == -1 || currentSlot == primary) {
            mc.getConnection().send(new ServerboundSetCarriedItemPacket(secondary));
            mc.player.getInventory().setSelectedSlot(secondary);
            
            needsSwapBack = true;
            originalSlot = currentSlot;
            ticksWaited = 0;
        }
    }

    @Override
    public void onTick() {
        if (!isToggled() || mc.player == null) return;
        
        if (needsSwapBack) {
            Setting fixDelaySetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Fix Delay (Ticks)");
            int delay = fixDelaySetting != null ? (int) fixDelaySetting.getValDouble() : 1;
            
            if (ticksWaited >= delay) {
                mc.player.getInventory().setSelectedSlot(originalSlot);
                mc.getConnection().send(new ServerboundSetCarriedItemPacket(originalSlot));
                needsSwapBack = false;
            } else {
                ticksWaited++;
            }
        }
    }

    @Override
    public void onDisable() {
        if (needsSwapBack && mc.player != null && mc.getConnection() != null) {
            mc.player.getInventory().setSelectedSlot(originalSlot);
            mc.getConnection().send(new ServerboundSetCarriedItemPacket(originalSlot));
            needsSwapBack = false;
        }
    }
}
