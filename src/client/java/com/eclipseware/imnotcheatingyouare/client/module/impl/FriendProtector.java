package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.utils.FriendManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import com.eclipseware.imnotcheatingyouare.client.utils.InputUtil;

public class FriendProtector extends Module {
    private boolean wasMidDown = false;

    public FriendProtector() {
        super("Filter", Category.Misc, "Middle-click players to add/remove friends. Friends bypass all modules.");
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.level == null) return;

        boolean midDown = InputUtil.isMouseButtonDown(InputUtil.MOUSE_MIDDLE);
        if (midDown && !wasMidDown && mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.ENTITY) {
            Entity ent = ((EntityHitResult) mc.hitResult).getEntity();
            if (ent instanceof Player target && target != mc.player) {
                String name = target.getGameProfile().name();
                FriendManager.toggleFriend(name);
                if (mc.player != null) {
                    String action = FriendManager.isFriend(name) ? "\u00a7aAdded" : "\u00a7cRemoved";
                    mc.player.sendSystemMessage(
                        net.minecraft.network.chat.Component.literal("\u00a7d[Friends] " + action + " \u00a7f" + name)
                    );
                }
            }
        }
        wasMidDown = midDown;
    }
}