package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;

public class AttributeSwap extends Module {
    public static AttributeSwap INSTANCE;
    private int prevSlot = -1;
    private int dDelay = 0;
    private String swapMode = "";

    public AttributeSwap() {
        super("AttributeSwap", Category.Exploit, "Swaps attributes of the main hand item with the target slot on attack");
        INSTANCE = this;
        
        ArrayList<String> modes = new ArrayList<>();
        modes.add("Swap");
        modes.add("Silent");
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Mode", this, "Swap", modes));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Target Slot", this, 1.0, 1.0, 9.0, true));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Swap Back", this, true));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Swap Back Delay (Ticks)", this, 1.0, 1.0, 20.0, true));
    }

    private boolean getBool(String name) {
        Setting s = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, name);
        return s != null && s.getValBoolean();
    }

    private double getDouble(String name) {
        Setting s = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, name);
        return s != null ? s.getValDouble() : 0;
    }

    private String getStringSetting(String name) {
        Setting s = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, name);
        return s != null ? s.getValString() : "";
    }

    public boolean handleAttack(Entity target, Player player) {
        if (!isToggled() || mc.player == null) return false;

        String mode = getStringSetting("Mode");
        int targetSlotVal = (int) getDouble("Target Slot") - 1;
        int oldSlot = mc.player.getInventory().getSelectedSlot();
        if (oldSlot == targetSlotVal) return false;

        boolean swapBackVal = getBool("Swap Back");

        if (mode.equals("Silent")) {
            if (mc.getConnection() != null) {
                mc.getConnection().send(new ServerboundSetCarriedItemPacket(targetSlotVal));
                mc.getConnection().send(new ServerboundInteractPacket(
                    target.getId(), 
                    net.minecraft.world.InteractionHand.MAIN_HAND, 
                    target.position(), 
                    player.isShiftKeyDown()
                ));
            }
            player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            player.resetAttackStrengthTicker();

            if (swapBackVal) {
                if (prevSlot == -1) {
                    prevSlot = oldSlot;
                }
                swapMode = "Silent";
                dDelay = (int) getDouble("Swap Back Delay (Ticks)");
            }
            return true; // Cancel default attack
        } else {
            if (swapBackVal) {
                if (prevSlot == -1) {
                    prevSlot = oldSlot;
                }
                swapMode = "Swap";
            }
            com.eclipseware.imnotcheatingyouare.client.utils.ModuleUtils.switchToSlot(targetSlotVal);
            if (swapBackVal) {
                dDelay = (int) getDouble("Swap Back Delay (Ticks)");
            }
            return false;
        }
    }

    public void handleBlockBreak() {
        if (!isToggled() || mc.player == null) return;

        String mode = getStringSetting("Mode");
        int targetSlotVal = (int) getDouble("Target Slot") - 1;
        int oldSlot = mc.player.getInventory().getSelectedSlot();

        boolean swapBackVal = getBool("Swap Back");

        // Check if we are already swapped
        boolean alreadySwapped = false;
        if (swapBackVal && prevSlot != -1) {
            if (mode.equals("Silent") && swapMode.equals("Silent")) {
                alreadySwapped = true;
            } else if (mode.equals("Swap") && swapMode.equals("Swap") && oldSlot == targetSlotVal) {
                alreadySwapped = true;
            }
        }

        if (alreadySwapped) {
            // Just refresh the delay
            dDelay = (int) getDouble("Swap Back Delay (Ticks)");
            return;
        }

        if (oldSlot == targetSlotVal) return;

        if (mode.equals("Silent")) {
            if (mc.getConnection() != null) {
                mc.getConnection().send(new ServerboundSetCarriedItemPacket(targetSlotVal));
            }
            if (swapBackVal) {
                prevSlot = oldSlot;
                swapMode = "Silent";
                dDelay = (int) getDouble("Swap Back Delay (Ticks)");
            }
        } else {
            // Swap mode
            if (swapBackVal) {
                prevSlot = oldSlot;
                swapMode = "Swap";
            }
            com.eclipseware.imnotcheatingyouare.client.utils.ModuleUtils.switchToSlot(targetSlotVal);
            if (swapBackVal) {
                dDelay = (int) getDouble("Swap Back Delay (Ticks)");
            }
        }
    }

    @Override
    public void onTick() {
        if (!isToggled() || mc.player == null) return;

        if (dDelay > 0) {
            dDelay--;
            if (dDelay == 0 && prevSlot != -1) {
                if (swapMode.equals("Silent")) {
                    if (mc.getConnection() != null) {
                        mc.getConnection().send(new ServerboundSetCarriedItemPacket(prevSlot));
                    }
                } else {
                    com.eclipseware.imnotcheatingyouare.client.utils.ModuleUtils.switchToSlot(prevSlot);
                }
                prevSlot = -1;
                swapMode = "";
            }
        }
    }

    @Override
    public void onDisable() {
        if (prevSlot != -1 && mc.player != null) {
            if (swapMode.equals("Silent")) {
                if (mc.getConnection() != null) {
                    mc.getConnection().send(new ServerboundSetCarriedItemPacket(prevSlot));
                }
            } else {
                com.eclipseware.imnotcheatingyouare.client.utils.ModuleUtils.switchToSlot(prevSlot);
            }
        }
        prevSlot = -1;
        dDelay = 0;
        swapMode = "";
    }
}
