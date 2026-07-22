package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.utils.ModuleUtils;
import com.eclipseware.imnotcheatingyouare.client.utils.SilentAimUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

public class AutoMLG extends Module {
    private Setting fallDistSetting;
    private Setting onlyLethalSetting;
    private Setting invRetrieveSetting;
    private Setting autoPickupSetting;
    private Setting delaySetting;

    private boolean active = false;
    private int ticksElapsed = 0;
    private int originalSlot = -1;
    private int mlgSlot = -1;
    private Item placedItem = null;
    private int retrievedFromSlot = -1;
    private int hotbarSwapSlot = -1;

    public AutoMLG() {
        super("AutoMLG", Category.Utility, "Automatically places a water bucket, powdered snow, or cobwebs to prevent fall damage.");
        
        fallDistSetting = new Setting("Fall Distance", this, 5.0, 3.0, 20.0, false);
        onlyLethalSetting = new Setting("Only Lethal", this, false);
        invRetrieveSetting = new Setting("Inventory Retrieve", this, false);
        autoPickupSetting = new Setting("Auto Pickup", this, true);
        delaySetting = new Setting("Delay Ticks", this, 1.0, 0.0, 5.0, true);

        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(fallDistSetting);
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(onlyLethalSetting);
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(invRetrieveSetting);
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(autoPickupSetting);
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(delaySetting);
    }

    @Override
    public void onEnable() {
        resetState();
    }

    private int findMlgItem(boolean hotbarOnly) {
        if (hotbarOnly) {
            int slot = ModuleUtils.findItemInHotbar(Items.WATER_BUCKET);
            if (slot != -1) return slot;
            slot = ModuleUtils.findItemInHotbar(Items.POWDER_SNOW_BUCKET);
            if (slot != -1) return slot;
            slot = ModuleUtils.findItemInHotbar(Items.COBWEB);
            if (slot != -1) return slot;
        } else {
            for (int i = 0; i < 36; i++) {
                Item item = mc.player.getInventory().getItem(i).getItem();
                if (item == Items.WATER_BUCKET || item == Items.POWDER_SNOW_BUCKET || item == Items.COBWEB) {
                    return i;
                }
            }
        }
        return -1;
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.getConnection() == null || mc.gameMode == null) return;

        if (!active) {
            if (mc.player.onGround() || mc.player.isInWater() || mc.player.isInLava() || mc.player.onClimbable()) return;
            if (mc.player.getDeltaMovement().y >= 0.0) return;

            double minFallDist = fallDistSetting.getValDouble();
            if (mc.player.fallDistance < minFallDist) return;

            if (onlyLethalSetting.getValBoolean()) {
                double expectedDamage = Math.max(0.0, mc.player.fallDistance - 3.0);
                double currentHealth = mc.player.getHealth() + mc.player.getAbsorptionAmount();
                if (expectedDamage < currentHealth) return;
            }

            Vec3 pos = mc.player.position();
            BlockPos playerBlock = mc.player.blockPosition();
            BlockPos groundBlock = null;
            for (int i = 1; i <= 6; i++) {
                BlockPos check = playerBlock.below(i);
                if (!mc.level.getBlockState(check).isAir() && mc.level.getBlockState(check).getFluidState().isEmpty()) {
                    groundBlock = check;
                    break;
                }
            }

            if (groundBlock == null) return;
            double distToGround = pos.y - (groundBlock.getY() + 1.0);
            if (distToGround > 4.5 || distToGround < 0.1) return;

            int mlgHotbarSlot = findMlgItem(true);
            if (mlgHotbarSlot == -1 && invRetrieveSetting.getValBoolean() && retrievedFromSlot == -1) {
                int invSlot = findMlgItem(false);
                if (invSlot != -1 && invSlot >= 9) {
                    int targetHbSlot = mc.player.getInventory().getSelectedSlot();
                    int containerId = mc.player.inventoryMenu.containerId;
                    mc.gameMode.handleContainerInput(containerId, invSlot, targetHbSlot, ContainerInput.SWAP, mc.player);
                    retrievedFromSlot = invSlot;
                    hotbarSwapSlot = targetHbSlot;
                    mlgHotbarSlot = targetHbSlot;
                }
            }

            if (mlgHotbarSlot == -1) return;

            originalSlot = ModuleUtils.getSelectedSlot();
            mlgSlot = mlgHotbarSlot;
            placedItem = mc.player.getInventory().getItem(mlgSlot).getItem();
            active = true;
            ticksElapsed = 0;
        }

        if (active) {
            ticksElapsed++;
            int delay = (int) delaySetting.getValDouble();

            if (ticksElapsed <= delay + 3) {
                SilentAimUtil.setRotation(mc.player.getYRot(), 90.0f, 2);
            }

            if (ticksElapsed == 1) {
                ModuleUtils.switchToSlot(mlgSlot);
            } else if (ticksElapsed == delay + 1) {
                ModuleUtils.useItemPacket(mc.player.getYRot(), 90.0f);
            }

            boolean needsPickup = autoPickupSetting.getValBoolean() && (placedItem == Items.WATER_BUCKET || placedItem == Items.POWDER_SNOW_BUCKET);
            int pickupTick = delay + 2;
            int endTick = needsPickup ? delay + 4 : delay + 3;

            if (ticksElapsed == pickupTick && needsPickup) {
                int bucketSlot = ModuleUtils.findItemInHotbar(Items.BUCKET);
                if (bucketSlot != -1) {
                    ModuleUtils.switchToSlot(bucketSlot);
                    ModuleUtils.useItemPacket(mc.player.getYRot(), 90.0f);
                }
            }

            if (ticksElapsed >= endTick) {
                ModuleUtils.switchToSlot(originalSlot);
                if (retrievedFromSlot != -1 && hotbarSwapSlot != -1) {
                    int containerId = mc.player.inventoryMenu.containerId;
                    mc.gameMode.handleContainerInput(containerId, retrievedFromSlot, hotbarSwapSlot, ContainerInput.SWAP, mc.player);
                }
                resetState();
            }
        }
    }

    private void resetState() {
        active = false;
        ticksElapsed = 0;
        originalSlot = -1;
        mlgSlot = -1;
        placedItem = null;
        retrievedFromSlot = -1;
        hotbarSwapSlot = -1;
    }

    @Override
    public void onDisable() {
        if (active) {
            ModuleUtils.switchToSlot(originalSlot);
            if (retrievedFromSlot != -1 && hotbarSwapSlot != -1 && mc.player != null && mc.gameMode != null) {
                int containerId = mc.player.inventoryMenu.containerId;
                mc.gameMode.handleContainerInput(containerId, retrievedFromSlot, hotbarSwapSlot, ContainerInput.SWAP, mc.player);
            }
        }
        resetState();
    }
}
