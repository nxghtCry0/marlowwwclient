package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.utils.SpoofManager;
import com.eclipseware.imnotcheatingyouare.mixin.client.MinecraftAccessor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class LungeAssist extends Module {
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
        SpoofManager.silentUse(spearSlot, () -> {
            ((MinecraftAccessor) mc).invokeStartAttack();
        });
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
    }

    @Override
    public void onDisable() {
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