package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Arrays;

public class AutoFish extends Module {
    private long lastCastMs = 0L;

    public AutoFish() {
        super("AutoFish", Category.Farming, "Automatically casts and reels in your fishing rod when a bite is detected.");
        setSubCategory("Fishing");
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Interaction Hand Mode", this, "Mainhand", new ArrayList<>(Arrays.asList("Mainhand", "Offhand"))));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Delay (ms)", this, 500.0, 0.0, 3000.0, true));
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.gameMode == null || mc.gui.screen() != null) return;

        Setting handModeSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Interaction Hand Mode");
        String mode = handModeSetting != null ? handModeSetting.getValString() : "Mainhand";
        InteractionHand hand = mode.equalsIgnoreCase("Offhand") ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;

        boolean holdingRod = (hand == InteractionHand.MAIN_HAND ? mc.player.getMainHandItem() : mc.player.getOffhandItem()).is(Items.FISHING_ROD);
        if (!holdingRod) return;

        Setting delaySetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Delay (ms)");
        long delayMs = delaySetting != null ? (long) delaySetting.getValDouble() : 500L;
        if (System.currentTimeMillis() - lastCastMs < delayMs) return;

        FishingHook hook = mc.player.fishing;
        boolean canFish = hook == null;
        if (!canFish) {
            Vec3 delta = hook.getDeltaMovement();
            canFish = delta.x == 0.0 && delta.y <= -0.2 && delta.z == 0.0;
        }
        if (!canFish) return;

        mc.gameMode.useItem(mc.player, hand);
        lastCastMs = System.currentTimeMillis();
    }
}
