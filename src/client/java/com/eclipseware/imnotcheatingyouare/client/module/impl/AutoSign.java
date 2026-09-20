package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket;

public class AutoSign extends Module {
    public static String[] savedLines = null;
    public static boolean isFront = true;

    public AutoSign() {
        super("AutoSign", Category.World, "Automatically fills in signs with your last used text.");
    }

    public static void handleScreen(AbstractSignEditScreen screen) {
        if (savedLines != null && Minecraft.getInstance().player != null) {
            try {
                java.lang.reflect.Field signField = AbstractSignEditScreen.class.getDeclaredField("sign");
                signField.setAccessible(true);
                net.minecraft.world.level.block.entity.SignBlockEntity signEntity = (net.minecraft.world.level.block.entity.SignBlockEntity) signField.get(screen);
                
                net.minecraft.world.level.block.entity.SignTextSlot slot = isFront
                        ? net.minecraft.world.level.block.entity.SignTextSlot.FRONT
                        : net.minecraft.world.level.block.entity.SignTextSlot.BACK;
                Minecraft.getInstance().getConnection().send(new ServerboundSignUpdatePacket(signEntity.getBlockPos(), java.util.List.of(savedLines), slot));
                screen.onClose();
            } catch (Exception ignored) {}
        }
    }
}