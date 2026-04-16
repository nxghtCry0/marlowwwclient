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

        // 1. Get the player's current bounding box
        AABB playerBox = mc.player.getBoundingBox();
        
        // 2. Adjust the box exactly like Meteor's SafeWalk does:
        //    expandTowards(0, -stepHeight, 0) stretches the box downwards to check the block we are standing on.
        //    inflate(-edgeDistance, 0, -edgeDistance) shrinks the box horizontally so we can hang off the edge.
        AABB adjustedBox = playerBox
            .expandTowards(0, -mc.player.maxUpStep(), 0)
            .inflate(-edgeDistance, 0, -edgeDistance);

        // 3. noCollision returns TRUE if the space is completely empty (no blocks).
        // If this inner column is empty, it means the floor has dropped out from under our core, and we are at the edge.
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