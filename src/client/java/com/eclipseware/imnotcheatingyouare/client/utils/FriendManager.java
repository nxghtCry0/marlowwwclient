package com.eclipseware.imnotcheatingyouare.client.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

import java.io.File;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;

public class FriendManager {
    private static final File FILE = new File(Minecraft.getInstance().gameDirectory, "config/imnotcheatingyouare/friends.txt");
    private static final Set<String> friends = new HashSet<>();

    public static boolean isFriend(Player player) {
        return friends.contains(player.getGameProfile().name());
    }

    public static boolean isFriend(String name) {
        return friends.contains(name);
    }

    public static void toggleFriend(String name) {
        if (friends.contains(name)) friends.remove(name);
        else friends.add(name);
        save();
    }

    public static Set<String> getFriends() {
        return new HashSet<>(friends);
    }

    public static void load() {
        if (!FILE.exists()) return;
        try {
            for (String line : Files.readAllLines(FILE.toPath())) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty()) friends.add(trimmed);
            }
        } catch (Exception ignored) {}
    }

    public static void save() {
        try {
            FILE.getParentFile().mkdirs();
            Files.write(FILE.toPath(), String.join("\n", friends).getBytes());
        } catch (Exception ignored) {}
    }
}