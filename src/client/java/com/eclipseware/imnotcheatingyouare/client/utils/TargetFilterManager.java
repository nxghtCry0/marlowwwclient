package com.eclipseware.imnotcheatingyouare.client.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TargetFilterManager {
    private static final Minecraft mc = Minecraft.getInstance();
    private static final File DIR = new File(mc.gameDirectory, "config/imnotcheatingyouare");
    private static final File FILTERS_FILE = new File(DIR, "target_filters.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final Set<String> filteredPlayers = new HashSet<>();
    private static final Set<String> filteredEntityTypes = new HashSet<>();

    public static boolean isFiltered(Entity entity) {
        if (entity == null) return false;
        if (entity instanceof Player player) {
            String name = player.getGameProfile().name();
            return filteredPlayers.contains(name) || FriendManager.isFriend(name);
        } else {
            String typeId = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
            return filteredEntityTypes.contains(typeId);
        }
    }

    public static Set<String> getFilteredPlayers() {
        return filteredPlayers;
    }

    public static Set<String> getFilteredEntityTypes() {
        return filteredEntityTypes;
    }

    public static void addFilteredPlayer(String name) {
        if (name == null || name.trim().isEmpty()) return;
        filteredPlayers.add(name.trim());
        save();
    }

    public static void removeFilteredPlayer(String name) {
        if (name == null) return;
        filteredPlayers.remove(name);
        save();
    }

    public static void toggleFilteredEntityType(String typeId) {
        if (typeId == null) return;
        if (filteredEntityTypes.contains(typeId)) {
            filteredEntityTypes.remove(typeId);
        } else {
            filteredEntityTypes.add(typeId);
        }
        save();
    }

    public static boolean isEntityTypeFiltered(String typeId) {
        return filteredEntityTypes.contains(typeId);
    }

    private static class FilterContainer {
        List<String> players = new ArrayList<>();
        List<String> entityTypes = new ArrayList<>();
    }

    public static void save() {
        if (!DIR.exists()) DIR.mkdirs();
        try (FileWriter writer = new FileWriter(FILTERS_FILE)) {
            FilterContainer container = new FilterContainer();
            container.players.addAll(filteredPlayers);
            container.entityTypes.addAll(filteredEntityTypes);
            GSON.toJson(container, writer);
        } catch (Exception e) {
            System.err.println("[EclipseWare] Failed to save target filters: " + e.getMessage());
        }
    }

    public static void load() {
        if (!FILTERS_FILE.exists()) return;
        try (FileReader reader = new FileReader(FILTERS_FILE)) {
            JsonObject obj = JsonParser.parseReader(reader).getAsJsonObject();
            FilterContainer container = GSON.fromJson(obj, FilterContainer.class);
            if (container != null) {
                if (container.players != null) {
                    filteredPlayers.clear();
                    filteredPlayers.addAll(container.players);
                }
                if (container.entityTypes != null) {
                    filteredEntityTypes.clear();
                    filteredEntityTypes.addAll(container.entityTypes);
                }
            }
        } catch (Exception e) {
            System.err.println("[EclipseWare] Failed to load target filters: " + e.getMessage());
        }
    }
}
