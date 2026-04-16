package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.mixin.client.MinecraftAccessor;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class LungeAssist extends Module {
    private boolean needsSwapBack = false;
    private int originalSlot = -1;
    private int swapDelayTicks = -1;
    
    private boolean waitingForApex = false;

    public LungeAssist() {
        super("LungeAssist", Category.Combat);
    }

    @Override
    public void onKeybind() {
        if (mc == null || mc.player == null || mc.getConnection() == null) return;
        
        int spearSlot = findLungeSpear(mc.player);
        if (spearSlot == -1) {
            super.onKeybind();
            return;
        }

        Setting autoJump = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "AutoJump");
        boolean doJump = autoJump != null && autoJump.getValBoolean();

        if (doJump && mc.player.onGround()) {
            mc.player.jumpFromGround();
            waitingForApex = true; 
        } else {
            executeLunge(spearSlot);
        }
    }

    private void executeLunge(int spearSlot) {
        int oldSlot = mc.player.getInventory().getSelectedSlot();
        if (oldSlot == spearSlot) {
            ((MinecraftAccessor) mc).invokeStartAttack();
            return;
        }
        
        mc.player.getInventory().setSelectedSlot(spearSlot);
        mc.getConnection().send(new ServerboundSetCarriedItemPacket(spearSlot));
        
        ((MinecraftAccessor) mc).invokeStartAttack();
        
        needsSwapBack = true;
        originalSlot = oldSlot;
        swapDelayTicks = 1;
    }

    @Override
    public void onTick() {
        if (mc == null || mc.player == null || mc.getConnection() == null) return;

        if (waitingForApex) {
            if (mc.player.getDeltaMovement().y <= 0.0 || mc.player.onGround()) {
                int spearSlot = findLungeSpear(mc.player);
                if (spearSlot != -1) {
                    executeLunge(spearSlot);
                }
                waitingForApex = false;
            }
        }

        if (needsSwapBack && !waitingForApex) {
            swapDelayTicks--;
            if (swapDelayTicks <= 0) {
                mc.player.getInventory().setSelectedSlot(originalSlot);
                mc.getConnection().send(new ServerboundSetCarriedItemPacket(originalSlot));
                needsSwapBack = false;
            }
        }
    }

    @Override
    public void onDisable() {
        needsSwapBack = false;
        waitingForApex = false;
    }

    private int findLungeSpear(Player player) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            String itemName = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
            
            if (itemName.contains("spear")) {
                for (var enchant : stack.getEnchantments().keySet()) {
                    if (enchant.unwrapKey().isPresent() && enchant.unwrapKey().get().toString().contains("lunge")) {
                        return i;
                    }
                }
            }
        }
        return -1;
    }
}