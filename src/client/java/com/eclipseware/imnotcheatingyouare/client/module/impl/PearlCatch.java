package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public class PearlCatch extends Module {
    private boolean active = false;
    private int ticksElapsed = 0;
    private int originalSlot = -1;
    private int windChargeSlot = -1;

    public PearlCatch() {
        super("PearlCatch", Category.Movement);
    }

    @Override
    public void onKeybind() {
        if (mc.player == null || mc.getConnection() == null || mc.gameMode == null || active) return;

        int pearlSlot = findItem("ender_pearl");
        windChargeSlot = findItem("wind_charge");

        if (pearlSlot == -1 || windChargeSlot == -1) {
            super.onKeybind(); // Toggle normally if we don't have the required items
            return;
        }

        originalSlot = mc.player.getInventory().getSelectedSlot();
        
        // Swap to Pearl
        mc.player.getInventory().setSelectedSlot(pearlSlot);
        mc.getConnection().send(new ServerboundSetCarriedItemPacket(pearlSlot));

        // Throw Pearl
        mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
        mc.player.swing(InteractionHand.MAIN_HAND);

        Setting delaySetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Delay (Ticks)");
        int delay = delaySetting != null ? (int) delaySetting.getValDouble() : 5;

        if (delay <= 0) {
            // If delay is 0, fire wind charge instantly in the same tick
            fireWindCharge();
        } else {
            // Defer the wind charge to the tick loop
            active = true;
            ticksElapsed = 0;
        }
    }

    @Override
    public void onTick() {
        if (!active || mc.player == null || mc.getConnection() == null) return;

        ticksElapsed++;
        
        Setting delaySetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Delay (Ticks)");
        int delay = delaySetting != null ? (int) delaySetting.getValDouble() : 5;

        if (ticksElapsed >= delay) {
            fireWindCharge();
        }
    }
    
    private void fireWindCharge() {
        // Swap to Wind Charge
        mc.player.getInventory().setSelectedSlot(windChargeSlot);
        mc.getConnection().send(new ServerboundSetCarriedItemPacket(windChargeSlot));

        // Throw Wind Charge
        mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
        mc.player.swing(InteractionHand.MAIN_HAND);

        // Swap back to whatever weapon we were holding
        mc.player.getInventory().setSelectedSlot(originalSlot);
        mc.getConnection().send(new ServerboundSetCarriedItemPacket(originalSlot));

        active = false;
    }

    @Override
    public void onDisable() {
        active = false; // Failsafe
    }

    private int findItem(String targetName) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            String itemName = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
            if (itemName.equals(targetName)) {
                return i;
            }
        }
        return -1;
    }
}