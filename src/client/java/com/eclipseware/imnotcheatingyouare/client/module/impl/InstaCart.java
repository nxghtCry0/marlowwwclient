package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.mixin.client.MinecraftAccessor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.Random;

public class InstaCart extends Module {
    private final Random random = new Random();
    private int stage = 0;
    private long lastTime = 0L;
    private int originalSlot = -1;
    private long randomizedDelay = 0L;

    public InstaCart() {
        super("InstaCart", Category.CartPvP, "Places a rail, shoots a bow fast, and places a TNT minecart.");
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Rail Delay Min", this, 50.0, 0.0, 500.0, true));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Rail Delay Max", this, 150.0, 0.0, 500.0, true));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Min Cooldown", this, 5.0, 0.0, 100.0, true));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Max Cooldown", this, 15.0, 0.0, 100.0, true));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Cart Delay Min", this, 50.0, 0.0, 500.0, true));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Cart Delay Max", this, 150.0, 0.0, 500.0, true));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Swap Back", this, true));
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
        if (mc.player == null) {
            this.toggle();
            return;
        }
        lastTime = System.currentTimeMillis();
        originalSlot = mc.player.getInventory().getSelectedSlot();
        randomizedDelay = 0L;
        stage = 0;
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
                int bowSlot = findItemSlot(Items.BOW);
                if (bowSlot == -1) {
                    finish();
                    return;
                }
                if (mc.player.getInventory().getSelectedSlot() != bowSlot) {
                    com.eclipseware.imnotcheatingyouare.client.utils.ModuleUtils.switchToSlot(bowSlot);
                }

                mc.options.keyUse.setDown(true);
                doClick();

                lastTime = currentTime;
                randomizedDelay = getDelay(getVal("Min Cooldown") * 10.0, getVal("Max Cooldown") * 10.0);
                stage = 1;
                break;

            case 1:
                mc.options.keyUse.setDown(false);
                lastTime = currentTime;
                randomizedDelay = getDelay(getVal("Rail Delay Min"), getVal("Rail Delay Max"));
                stage = 2;
                break;

            case 2:
                int railSlot = findRailSlot();
                if (railSlot == -1) {
                    finish();
                    return;
                }
                if (mc.player.getInventory().getSelectedSlot() != railSlot) {
                    com.eclipseware.imnotcheatingyouare.client.utils.ModuleUtils.switchToSlot(railSlot);
                }

                doClick();

                lastTime = currentTime;
                randomizedDelay = getDelay(getVal("Cart Delay Min"), getVal("Cart Delay Max"));
                stage = 3;
                break;

            case 3:
                int cartSlot = findItemSlot(Items.TNT_MINECART);
                if (cartSlot == -1) {
                    finish();
                    return;
                }
                if (mc.player.getInventory().getSelectedSlot() != cartSlot) {
                    com.eclipseware.imnotcheatingyouare.client.utils.ModuleUtils.switchToSlot(cartSlot);
                }

                doClick();

                lastTime = currentTime;
                randomizedDelay = 100L;
                stage = 4;
                break;

            case 4:
                finish();
                break;
        }
    }

    private void finish() {
        mc.options.keyUse.setDown(false);
        if (getBool("Swap Back") && originalSlot != -1 && mc.player.getInventory().getSelectedSlot() != originalSlot) {
            com.eclipseware.imnotcheatingyouare.client.utils.ModuleUtils.switchToSlot(originalSlot);
        }
        originalSlot = -1;
        stage = 0;
        this.toggle();
    }

    private void doClick() {
        if (mc instanceof MinecraftAccessor accessor) {
            accessor.invokeStartUseItem();
        }
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
