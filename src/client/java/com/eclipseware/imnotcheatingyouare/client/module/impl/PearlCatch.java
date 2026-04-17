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
    private float targetYaw, targetPitch;

    public PearlCatch() {
        super("PearlCatch", Category.Movement);
    }

    @Override
    public void onKeybind() {
        if (mc.player == null || mc.getConnection() == null || mc.gameMode == null || active) return;

        int pearlSlot = findItem("ender_pearl");
        windChargeSlot = findItem("wind_charge");

        if (pearlSlot == -1 || windChargeSlot == -1) {
            super.onKeybind(); 
            return;
        }

        originalSlot = mc.player.getInventory().getSelectedSlot();
        targetYaw = mc.player.getYRot();
        targetPitch = mc.player.getXRot();
        
        mc.player.getInventory().setSelectedSlot(pearlSlot);
        mc.getConnection().send(new ServerboundSetCarriedItemPacket(pearlSlot));

        mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
        mc.player.swing(InteractionHand.MAIN_HAND);

        Setting delaySetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Delay (Ticks)");
        int delay = delaySetting != null ? (int) delaySetting.getValDouble() : 5;

        if (delay <= 0) {
            fireWindCharge();
        } else {
            active = true;
            ticksElapsed = 0;
        }
    }

    @Override
    public void onTick() {
        if (!active || mc.player == null || mc.getConnection() == null) return;

        // Hard lock local camera rotation
        mc.player.setYRot(targetYaw);
        mc.player.setXRot(targetPitch);
        mc.player.yRotO = targetYaw;
        mc.player.xRotO = targetPitch;

        com.eclipseware.imnotcheatingyouare.client.utils.RotationManager.keepRotated(targetYaw, targetPitch, 40.0f, false);

        ticksElapsed++;
        
        Setting delaySetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Delay (Ticks)");
        int delay = delaySetting != null ? (int) delaySetting.getValDouble() : 5;

        if (ticksElapsed >= delay) {
            fireWindCharge();
        }
    }
    
    private void fireWindCharge() {
        com.eclipseware.imnotcheatingyouare.client.utils.RotationManager.keepRotated(targetYaw, targetPitch, 40.0f, false);

        mc.player.getInventory().setSelectedSlot(windChargeSlot);
        mc.getConnection().send(new ServerboundSetCarriedItemPacket(windChargeSlot));

        mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
        mc.player.swing(InteractionHand.MAIN_HAND);

        mc.player.getInventory().setSelectedSlot(originalSlot);
        mc.getConnection().send(new ServerboundSetCarriedItemPacket(originalSlot));

        com.eclipseware.imnotcheatingyouare.client.utils.RotationManager.requestReturn();
        active = false;
    }

    @Override
    public void onDisable() {
        active = false; 
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