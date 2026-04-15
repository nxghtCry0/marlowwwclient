package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Fireworks;

import java.util.List;

public class ElytraBoost extends Module {

    public ElytraBoost() {
        super("ElytraBoost", Category.Movement, "Fly like you have fireworks without 'em. Press keybind to boost.");
    }

    @Override
    public void onKeybind() {
        if (mc == null || mc.player == null || mc.level == null) return;

        if (mc.player.isFallFlying()) {
            Setting levelSet = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Firework Level");
            int level = levelSet != null ? (int) levelSet.getValDouble() : 1;

            Setting soundSet = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Play Sound");
            boolean playSound = soundSet == null || soundSet.getValBoolean();

            // Create a fake firework item stack with the desired flight duration
            ItemStack stack = new ItemStack(Items.FIREWORK_ROCKET);
            stack.set(DataComponents.FIREWORKS, new Fireworks((byte) level, List.of()));

            // Spawn the rocket entity bound to the player
            FireworkRocketEntity rocket = new FireworkRocketEntity(mc.level, stack, mc.player);
            mc.level.addFreshEntity(rocket);

            if (playSound) {
                mc.level.playLocalSound(mc.player.getX(), mc.player.getY(), mc.player.getZ(), 
                    SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.AMBIENT, 3.0F, 1.0F, false);
            }
        }
    }
}