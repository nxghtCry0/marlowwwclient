package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.utils.ModuleUtils;
import com.eclipseware.imnotcheatingyouare.client.utils.RotationManager;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.ArrayList;

public class PearlCatch extends Module {
    private enum Stage { IDLE, AIM, PEARL, CHARGE }

    private Stage stage = Stage.IDLE;
    private int wait;
    private int originalSlot = -1;
    private float yaw, pitch;

    public PearlCatch() {
        super("PearlCatch", Category.Mace, "Throws a pearl and a wind charge that collides with it, launching the pearl much higher for a mace drop.");
        var sm = ImnotcheatingyouareClient.INSTANCE.settingsManager;
        ArrayList<String> aims = new ArrayList<>();
        aims.add("Look");
        aims.add("Straight Up");
        sm.rSetting(new Setting("Aim", this, "Look", aims));
        sm.rSetting(new Setting("Charge Delay", this, 1.0, 0.0, 6.0, true));
        sm.rSetting(new Setting("Min Pitch", this, 45.0, 0.0, 90.0, true));
    }

    private Setting setting(String name) {
        return ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, name);
    }

    @Override
    public void onKeybind() {
        if (mc.player == null || mc.gameMode == null || mc.getConnection() == null || stage != Stage.IDLE) return;
        if (slotOf(Items.ENDER_PEARL) == -1 || slotOf(Items.WIND_CHARGE) == -1) return;
        if (mc.player.getCooldowns().isOnCooldown(Items.ENDER_PEARL.getDefaultInstance())
                || mc.player.getCooldowns().isOnCooldown(Items.WIND_CHARGE.getDefaultInstance())) return;

        originalSlot = ModuleUtils.getSelectedSlot();
        yaw = mc.player.getYRot();
        Setting aim = setting("Aim");
        if (aim != null && aim.getValString().equals("Straight Up")) {
            pitch = -90f;
        } else {
            Setting min = setting("Min Pitch");
            float limit = -(float) (min != null ? min.getValDouble() : 45.0);
            pitch = Math.min(mc.player.getXRot(), limit);
        }
        stage = Stage.AIM;
    }

    @Override
    public void onTick() {
        if (stage == Stage.IDLE || mc.player == null || mc.gameMode == null) return;
        RotationManager.keepRotated(yaw, pitch, 180f, true);

        switch (stage) {
            case AIM -> stage = Stage.PEARL;
            case PEARL -> {
                if (!throwItem(Items.ENDER_PEARL)) {
                    finish();
                    return;
                }
                Setting d = setting("Charge Delay");
                wait = d != null ? (int) d.getValDouble() : 1;
                stage = Stage.CHARGE;
                if (wait == 0) {
                    throwItem(Items.WIND_CHARGE);
                    finish();
                }
            }
            case CHARGE -> {
                if (--wait > 0) return;
                throwItem(Items.WIND_CHARGE);
                finish();
            }
            default -> {
            }
        }
    }

    private boolean throwItem(Item item) {
        InteractionHand hand;
        if (mc.player.getOffhandItem().is(item)) {
            hand = InteractionHand.OFF_HAND;
        } else {
            int slot = slotOf(item);
            if (slot == -1) return false;
            ModuleUtils.switchToSlot(slot);
            hand = InteractionHand.MAIN_HAND;
        }
        mc.gameMode.useItem(mc.player, hand);
        return true;
    }

    private void finish() {
        if (originalSlot >= 0 && originalSlot < 9) ModuleUtils.switchToSlot(originalSlot);
        originalSlot = -1;
        RotationManager.requestReturn();
        stage = Stage.IDLE;
    }

    private int slotOf(Item item) {
        return ModuleUtils.findItemInHotbar(item);
    }

    @Override
    public boolean needsTick() {
        return stage != Stage.IDLE;
    }

    @Override
    public void onDisable() {
        if (stage != Stage.IDLE) finish();
    }
}
