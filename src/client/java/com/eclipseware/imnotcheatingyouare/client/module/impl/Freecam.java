package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.phys.Vec3;

public class Freecam extends Module {
    private Vec3 savedPos;
    private float savedYaw, savedPitch;
    private RemotePlayer dummy;

    public Freecam() {
        super("Freecam", Category.Render, "Leaves your body behind while your camera flies freely.");
    }

    @Override
    public void onEnable() {
        if (mc.player == null || mc.level == null) return;
        savedPos = mc.player.position();
        savedYaw = mc.player.getYRot();
        savedPitch = mc.player.getXRot();

        // Spawn a fake player entity to leave behind
        dummy = new RemotePlayer(mc.level, mc.player.getGameProfile());
        dummy.setPos(savedPos);
        dummy.setYRot(savedYaw);
        dummy.setXRot(savedPitch);
        dummy.setYHeadRot(mc.player.getYHeadRot());
        dummy.getInventory().replaceWith(mc.player.getInventory());

        mc.level.addEntity(dummy);

        mc.player.getAbilities().flying = true;
    }

    @Override
    public void onDisable() {
        if (mc.player == null || mc.level == null) return;
       
        // Snap back to the dummy's position
        mc.player.setPos(savedPos);
        mc.player.setYRot(savedYaw);
        mc.player.setXRot(savedPitch);
        mc.player.getAbilities().flying = false;
        mc.player.setDeltaMovement(0, 0, 0);

        // Remove the dummy
        if (dummy != null) {
            mc.level.removeEntity(dummy.getId(), net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
            dummy = null;
        }
    }
}