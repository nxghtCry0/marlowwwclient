package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import net.minecraft.world.entity.player.Player;

public class Teams extends Module {
    public static Teams INSTANCE;

    public Teams() {
        super("Teams", Category.Combat, "Prevents combat modules from hitting team members.");
        INSTANCE = this;
    }

    public static boolean isTeam(Player target) {
        if (INSTANCE == null || !INSTANCE.isToggled() || mc.player == null) return false;
        
        if (mc.player.getTeam() != null && target.getTeam() != null) {
            if (mc.player.getTeam().isAlliedTo(target.getTeam())) {
                return true;
            }
        }
        
        String myName = mc.player.getDisplayName().getString();
        String targetName = target.getDisplayName().getString();
        
        if (myName.length() >= 2 && targetName.length() >= 2) {
            if (myName.charAt(0) == '\u00A7' && targetName.charAt(0) == '\u00A7') {
                if (myName.charAt(1) == targetName.charAt(1)) {
                    return true;
                }
            }
        }
        
        return false;
    }
}
