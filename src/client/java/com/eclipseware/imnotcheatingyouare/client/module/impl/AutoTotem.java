package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Items;

public class AutoTotem extends Module {
    private final Setting pauseInputs;
    private final Setting cooldownMs;

    private static volatile long pauseInputsUntilMs = 0L;
    private long lastSwapTime = 0L;

    public static boolean shouldPauseInputs() {
        return System.currentTimeMillis() < pauseInputsUntilMs;
    }

    public static void triggerInputPause() {
        pauseInputsUntilMs = System.currentTimeMillis() + 60L;
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.setSprinting(false);
        }
    }

    public AutoTotem() {
        super("AutoTotem", Category.Utility, "Automatically equips totems into offhand cleanly and instantly.");
        setSubCategory("Crystal PvP");

        pauseInputs = new Setting("Pause Inputs", this, true);
        cooldownMs = new Setting("Cooldown (ms)", this, 200.0, 50.0, 500.0, true);

        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(pauseInputs);
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(cooldownMs);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.level == null || mc.gameMode == null)
            return;

        if (mc.player.isDeadOrDying() || mc.player.isSpectator())
            return;

        if (mc.player.getOffhandItem().is(Items.TOTEM_OF_UNDYING))
            return;

        long now = System.currentTimeMillis();
        if (now - lastSwapTime < cooldownMs.getValDouble())
            return;

        performTotemSwap();
    }

    private void performTotemSwap() {
        if (mc.player == null || mc.gameMode == null)
            return;

        int totemSlot = findTotemSlot();
        if (totemSlot == -1)
            return;

        if (pauseInputs.getValBoolean()) {
            triggerInputPause();
        }

        mc.gameMode.handleContainerInput(
                0,
                totemSlot,
                40,
                ContainerInput.SWAP,
                mc.player
        );
        lastSwapTime = System.currentTimeMillis();
    }

    public void onLocalTotemPop() {
        if (!isToggled() || mc.player == null || mc.gameMode == null)
            return;

        long now = System.currentTimeMillis();
        if (now - lastSwapTime < 80L)
            return;

        performTotemSwap();
    }

    private int findTotemSlot() {
        if (mc.player == null)
            return -1;

        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getItem(i).is(Items.TOTEM_OF_UNDYING)) {
                return i + 36;
            }
        }

        for (int i = 9; i < 36; i++) {
            if (mc.player.getInventory().getItem(i).is(Items.TOTEM_OF_UNDYING)) {
                return i;
            }
        }

        return -1;
    }
}
