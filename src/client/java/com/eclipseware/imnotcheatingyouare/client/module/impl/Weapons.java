package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class Weapons extends Module {
    public Weapons() {
        super("Weapons", Category.Combat, "Automatically manages weapons, such as axes for shields.");
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Auto Axe", this, true));
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.level == null) return;

        Setting autoAxe = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Auto Axe");
        if (autoAxe != null && autoAxe.getValBoolean() && mc.options.keyAttack.isDown()) {
            if (mc.hitResult != null && mc.hitResult.getType() == net.minecraft.world.phys.HitResult.Type.ENTITY) {
                net.minecraft.world.entity.Entity target = ((net.minecraft.world.phys.EntityHitResult) mc.hitResult).getEntity();
                if (target instanceof Player player && player.isBlocking()) {
                    int axeSlot = findAxe();
                    if (axeSlot != -1 && axeSlot != mc.player.getInventory().getSelectedSlot()) {
                        mc.player.getInventory().setSelectedSlot(axeSlot);
                    }
                }
            }
        }
    }

    private int findAxe() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (!stack.isEmpty() && net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().contains("axe")) {
                return i;
            }
        }
        return -1;
    }
}
