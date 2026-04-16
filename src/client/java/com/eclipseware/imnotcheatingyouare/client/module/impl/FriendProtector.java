package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.utils.FriendManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.lwjgl.glfw.GLFW;

public class FriendProtector extends Module {
    private boolean wasMidDown = false;

    public FriendProtector() {
        super("Filter", Category.Misc, "Middle-click players to add/remove friends. Friends bypass all modules.");
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.level == null) return;

        long window = 0;
        try {
            for (java.lang.reflect.Field f : mc.getWindow().getClass().getDeclaredFields()) {
                if (f.getType() == long.class) {
                    f.setAccessible(true);
                    window = f.getLong(mc.getWindow());
                    break;
                }
            }
        } catch (Exception ignored) {}

        if (window == 0) return;

        boolean midDown = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_MIDDLE) == GLFW.GLFW_PRESS;
        if (midDown && !wasMidDown && mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.ENTITY) {
            Entity ent = ((EntityHitResult) mc.hitResult).getEntity();
            if (ent instanceof Player target && target != mc.player) {
                String name = target.getGameProfile().name();
                FriendManager.toggleFriend(name);
                if (mc.player != null) {
                    String action = FriendManager.isFriend(name) ? "\u00a7aAdded" : "\u00a7cRemoved";
                    mc.player.displayClientMessage(
                        net.minecraft.network.chat.Component.literal("\u00a7d[Friends] " + action + " \u00a7f" + name), false
                    );
                }
            }
        }
        wasMidDown = midDown;
    }
}