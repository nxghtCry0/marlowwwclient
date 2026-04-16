package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import net.minecraft.world.phys.AABB;

public class BridgeAssist extends Module {
    private boolean isShifting = false;
    public BridgeAssist() {
        super("BridgeAssist", Category.World, "Uses Meteor's SafeWalk logic to perfectly sneak at edges.");
    }

    @Override
    public void onTick() {
        if (mc == null || mc.player == null || mc.level == null) return;

        Setting pitchCheck = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Pitch Check");
        if (pitchCheck != null && pitchCheck.getValBoolean() && mc.player.getXRot() < 45.0f) {
            unShift();
            return;
        }

        Setting edgeSet = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Edge Distance");
        double edgeDistance = edgeSet != null ? edgeSet.getValDouble() : 0.25;

        AABB playerBox = mc.player.getBoundingBox();
        
        AABB adjustedBox = playerBox
            .expandTowards(0, -mc.player.maxUpStep(), 0)
            .inflate(-edgeDistance, 0, -edgeDistance);

        boolean closeToEdge = mc.level.noCollision(mc.player, adjustedBox) && mc.player.onGround();

        if (closeToEdge) {
            mc.options.keyShift.setDown(true);
            isShifting = true;
        } else {
            unShift();
        }
    }

    private void unShift() {
        if (isShifting) {
            mc.options.keyShift.setDown(false);
            isShifting = false;
        }
    }

    @Override
    public void onDisable() {
        unShift();
    }
}