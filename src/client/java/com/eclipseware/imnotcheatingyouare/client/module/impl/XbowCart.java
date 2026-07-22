package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.mixin.client.MinecraftAccessor;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.level.GameType;
import java.util.Random;

public class XbowCart extends Module {
    private final Random random = new Random();
    private int stage = 0;
    private long lastTime = 0L;
    private int originalSlot = -1;
    private float originalPitch = 0.0F;
    private float originalYaw = 0.0F;
    private long randomizedDelay = 0L;
    private boolean loadingCrossbow = false;

    public XbowCart() {
        super("XbowCart", Category.CartPvP, "Crossbow carting macro");
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Swap Back", this, true));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Rail Delay Min", this, 50.0, 0.0, 1000.0, true));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Rail Delay Max", this, 250.0, 0.0, 1000.0, true));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Cart Delay Min", this, 50.0, 0.0, 1000.0, true));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Cart Delay Max", this, 250.0, 0.0, 1000.0, true));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Fire Delay Min", this, 100.0, 0.0, 1000.0, true));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Fire Delay Max", this, 300.0, 0.0, 1000.0, true));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Load Delay Min", this, 100.0, 0.0, 1000.0, true));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Load Delay Max", this, 300.0, 0.0, 1000.0, true));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Shoot Delay Min", this, 50.0, 0.0, 1000.0, true));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Shoot Delay Max", this, 250.0, 0.0, 1000.0, true));
    }

    private double getVal(String name) {
        Setting s = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, name);
        return s != null ? s.getValDouble() : 0.0;
    }

    private boolean getBool(String name) {
        Setting s = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, name);
        return s != null && s.getValBoolean();
    }

    @Override
    public void onEnable() {
        if (mc.player == null || mc.level == null) {
            this.toggle();
            return;
        }
        if (findRailSlot() == -1 || findItemSlot(Items.TNT_MINECART) == -1 || 
            findItemSlot(Items.FLINT_AND_STEEL) == -1 || findItemSlot(Items.CROSSBOW) == -1) {
            this.toggle();
            return;
        }
        originalSlot = mc.player.getInventory().getSelectedSlot();
        originalPitch = mc.player.getXRot();
        originalYaw = mc.player.getYRot();
        
        lastTime = System.currentTimeMillis();
        randomizedDelay = 0L;
        stage = 0;
        loadingCrossbow = false;
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.level == null) {
            this.toggle();
            return;
        }
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastTime < randomizedDelay) {
            return;
        }
        
        switch (stage) {
            case 0:
                int railSlot = findRailSlot();
                if (railSlot == -1) {
                    finish();
                    return;
                }
                switchTo(railSlot);
                tryPlace();
                
                lastTime = currentTime;
                randomizedDelay = 50L + getDelay(getVal("Rail Delay Min"), getVal("Rail Delay Max"));
                stage = 1;
                break;
              
            case 1:
                int cartSlot = findItemSlot(Items.TNT_MINECART);
                if (cartSlot == -1) {
                    finish();
                    return;
                }
                switchTo(cartSlot);
                tryPlace();
                
                lastTime = currentTime;
                randomizedDelay = 50L + getDelay(getVal("Cart Delay Min"), getVal("Cart Delay Max"));
                stage = 2;
                break;
              
            case 2:
                mc.player.setYRot(originalYaw);
                mc.player.setXRot(28.1F);
                
                int flintSlot = findItemSlot(Items.FLINT_AND_STEEL);
                if (flintSlot == -1) {
                    finish();
                    return;
                }
                switchTo(flintSlot);
                
                lastTime = currentTime;
                randomizedDelay = 50L;
                stage = 21;
                break;
              
            case 21:
                if (!tryPlace()) {
                    doClick();
                }
                lastTime = currentTime;
                randomizedDelay = 50L + getDelay(getVal("Fire Delay Min"), getVal("Fire Delay Max"));
                stage = 4;
                break;
              
            case 4:
                int crossbowSlot = findItemSlot(Items.CROSSBOW);
                if (crossbowSlot == -1) {
                    finish();
                    return;
                }
                switchTo(crossbowSlot);
                
                ItemStack crossbowStack = mc.player.getInventory().getItem(crossbowSlot);
                if (!CrossbowItem.isCharged(crossbowStack)) {
                    if (loadingCrossbow) {
                        finish();
                        return;
                    }
                    mc.options.keyUse.setDown(true);
                    doClick();
                    loadingCrossbow = true;
                    
                    lastTime = currentTime;
                    randomizedDelay = 1300L + getDelay(getVal("Load Delay Min"), getVal("Load Delay Max"));
                } else {
                    lastTime = currentTime;
                    randomizedDelay = 50L + getDelay(getVal("Shoot Delay Min"), getVal("Shoot Delay Max"));
                    stage = 5;
                }
                break;
        
            case 5:
                if (loadingCrossbow) {
                    mc.options.keyUse.setDown(false);
                    loadingCrossbow = false;
                    
                    lastTime = currentTime;
                    randomizedDelay = 100L;
                    stage = 6;
                    break;
                }
                stage = 6;
              
            case 6:
                mc.player.setXRot(24.2F);
                
                lastTime = currentTime;
                randomizedDelay = 50L;
                stage = 61;
                break;
              
            case 61:
                doClick();
                
                lastTime = currentTime;
                randomizedDelay = 150L;
                stage = 7;
                break;
              
            case 7:
                finish();
                break;
        }
    }

    private void finish() {
        mc.options.keyUse.setDown(false);
        if (getBool("Swap Back") && originalSlot != -1) {
            switchTo(originalSlot);
        }
        mc.player.setXRot(originalPitch);
        mc.player.setYRot(originalYaw);
        
        originalSlot = -1;
        stage = 0;
        this.toggle();
    }

    private void switchTo(int slot) {
        if (mc.player.getInventory().getSelectedSlot() != slot) {
            com.eclipseware.imnotcheatingyouare.client.utils.ModuleUtils.switchToSlot(slot);
        }
    }

    private void doClick() {
        if (mc instanceof MinecraftAccessor accessor) {
            accessor.invokeStartUseItem();
        }
    }

    private boolean tryPlace() {
        if (mc.player == null || mc.level == null || mc.gameMode == null) {
            return false;
        }
        if (mc.gui.screen() != null) {
            return false;
        }
        if (mc.hitResult == null || mc.hitResult.getType() != HitResult.Type.BLOCK) {
            return false;
        }
        if (mc.gameMode.getPlayerMode() == GameType.SPECTATOR) {
            return false;
        }
        doClick();
        return true;
    }

    private int findRailSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (!stack.isEmpty()) {
                Item item = stack.getItem();
                if (item == Items.RAIL || item == Items.POWERED_RAIL || item == Items.DETECTOR_RAIL || item == Items.ACTIVATOR_RAIL) {
                    return i;
                }
            }
        }
        return -1;
    }

    private int findItemSlot(Item item) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() == item) {
                return i;
            }
        }
        return -1;
    }

    private long getDelay(double min, double max) {
        if (min >= max) {
            return (long) min;
        }
        return (long) (min + random.nextDouble() * (max - min));
    }
}
