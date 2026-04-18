package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import net.minecraft.world.entity.player.Player;

public class AntiBot extends Module {
    public static AntiBot INSTANCE;

    public AntiBot() {
        super("AntiBot", Category.Combat, "Prevents combat modules from targeting server bots/NPCs.");
        INSTANCE = this;
    }

    public static boolean isBot(Player player) {
        if (INSTANCE == null || !INSTANCE.isToggled()) return false;
        
        String name = player.getName().getString();
        if (!name.matches("^[a-zA-Z0-9_]{3,16}$")) {
            return true; 
        }

        if (mc.getConnection() != null) {
            if (mc.getConnection().getPlayerInfo(player.getUUID()) == null) {
                return true; 
            }
        }

        return false;
    }
}
