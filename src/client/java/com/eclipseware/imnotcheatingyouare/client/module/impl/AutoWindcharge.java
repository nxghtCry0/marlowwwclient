package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public class AutoWindcharge extends Module {
    private boolean active = false;
    private int ticksElapsed = 0;
    private int originalSlot = -1;

    public AutoWindcharge() {
        super("AutoWindcharge", Category.Movement, "Automatically throws a windcharge at your feet.");
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Delay Ticks", this, 2.0, 1.0, 10.0, true));
    }

    @Override
    public void onKeybind() {
        if (mc.player == null || mc.getConnection() == null || active) return;

        int chargeSlot = findItem("wind_charge");
        if (chargeSlot == -1) {
            super.onKeybind();
            return;
        }

        originalSlot = com.eclipseware.imnotcheatingyouare.client.utils.ModuleUtils.getSelectedSlot();
        com.eclipseware.imnotcheatingyouare.client.utils.ModuleUtils.setServerSlot(chargeSlot);
        active = true;
        ticksElapsed = 0;
    }

    @Override
    public void onTick() {
        if (!active || mc.player == null || mc.getConnection() == null) return;

        com.eclipseware.imnotcheatingyouare.client.utils.SilentAimUtil.setRotation(mc.player.getYRot(), 90.0f, 2);

        ticksElapsed++;

        Setting delaySetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Delay Ticks");
        int delay = delaySetting != null ? (int) delaySetting.getValDouble() : 2;

        if (ticksElapsed == delay) {
            mc.getConnection().send(new net.minecraft.network.protocol.game.ServerboundUseItemPacket(InteractionHand.MAIN_HAND, 0, mc.player.getYRot(), 90.0f));
            mc.player.swing(InteractionHand.MAIN_HAND);
        } else if (ticksElapsed > delay) {
            com.eclipseware.imnotcheatingyouare.client.utils.ModuleUtils.resetServerSlot();
            com.eclipseware.imnotcheatingyouare.client.utils.ModuleUtils.setServerSlot(originalSlot);
            active = false;
        }
    }

    @Override
    public void onDisable() {
        active = false;
    }

    private int findItem(String targetName) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            String itemName = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
            if (itemName.equals(targetName)) {
                return i;
            }
        }
        return -1;
    }
}
