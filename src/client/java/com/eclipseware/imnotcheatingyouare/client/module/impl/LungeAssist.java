package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.mixin.client.MinecraftAccessor;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.resources.ResourceKey;
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
        if (mc == null || mc.player == null || mc.getConnection() == null) {
            return;
        }
        int spearSlot = this.findLungeSpear(mc.player);
        if (spearSlot == -1) {
            super.onKeybind();
            return;
        }
        Setting autoJump = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "AutoJump");
        boolean doJump = autoJump != null && autoJump.getValBoolean();
        if (doJump) {
            if (mc.player.onGround()) {
                mc.player.jumpFromGround();
            }
            this.waitingForApex = true;
        } else {
            this.executeLunge(spearSlot);
        }
    }

    private void executeLunge(int spearSlot) {
        int oldSlot = mc.player.getInventory().getSelectedSlot();
        if (oldSlot == spearSlot) {
            ((MinecraftAccessor) mc).invokeStartAttack();
            return;
        }
        mc.player.getInventory().setSelectedSlot(spearSlot);
        mc.getConnection().send((Packet) new ServerboundSetCarriedItemPacket(spearSlot));
        ((MinecraftAccessor) mc).invokeStartAttack();
        this.needsSwapBack = true;
        this.originalSlot = oldSlot;
        this.swapDelayTicks = 1;
    }

    @Override
    public void onTick() {
        if (mc == null || mc.player == null || mc.getConnection() == null) {
            return;
        }
        if (this.waitingForApex && (mc.player.getDeltaMovement().y <= 0.0 || mc.player.onGround())) {
            int spearSlot = this.findLungeSpear(mc.player);
            if (spearSlot != -1) {
                this.executeLunge(spearSlot);
            }
            this.waitingForApex = false;
        }
        if (this.needsSwapBack && !this.waitingForApex) {
            --this.swapDelayTicks;
            if (this.swapDelayTicks <= 0) {
                mc.player.getInventory().setSelectedSlot(this.originalSlot);
                mc.getConnection().send((Packet) new ServerboundSetCarriedItemPacket(this.originalSlot));
                this.needsSwapBack = false;
            }
        }
    }

    @Override
    public void onDisable() {
        this.needsSwapBack = false;
        this.waitingForApex = false;
    }

    private int findLungeSpear(Player player) {
        for (int i = 0; i < 9; ++i) {
            ItemStack stack = player.getInventory().getItem(i);
            String itemName = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
            if (!itemName.contains("spear")) {
                continue;
            }
            for (Holder<?> enchant : stack.getEnchantments().keySet()) {
                if (!enchant.unwrapKey().isPresent()
                        || !((ResourceKey<?>) enchant.unwrapKey().get()).toString().contains("lunge")) {
                    continue;
                }
                return i;
            }
        }
        return -1;
    }
}