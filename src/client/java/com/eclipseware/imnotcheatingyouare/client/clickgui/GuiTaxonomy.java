package com.eclipseware.imnotcheatingyouare.client.clickgui;

import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class GuiTaxonomy {

    public enum Group {
        COMBAT("Combat", "General, Crystal, Mace, Cart, UHC & Blatant", Arrays.asList(
            "All", "General", "Crystal", "Mace", "Cart PvP", "UHC", "Blatant"
        )),
        MOVEMENT("Movement", "Sprint, Strafe, Elytra & Flight", Arrays.asList(
            "All", "Core", "Elytra", "Flight & Exploit"
        )),
        RENDER("Render", "ESP, World Visuals, HUD & Theme", Arrays.asList(
            "All", "ESP & Entities", "World Visuals", "HUD & Overlays", "View & Theme"
        )),
        UTILITY("Utility", "Totem, Traps, Macros & Bypass", Arrays.asList(
            "All", "Player & Totem", "Traps & Webs", "Inventory & Macros", "Exploit & Bypass"
        )),
        WORLD("World", "Mining, Scaffolding, Farming & Loot", Arrays.asList(
            "All", "Blocks & Mining", "Farming & Villagers"
        )),
        CLIENT("Client & Configs", "Menu, GUI, Profiles, Macros & Filters", Arrays.asList(
            "All", "Client & GUI", "Safety & Filters", "Config Center", "Macros"
        ));

        private final String label;
        private final String subtitle;
        private final List<String> subCategories;

        Group(String label, String subtitle, List<String> subCategories) {
            this.label = label;
            this.subtitle = subtitle;
            this.subCategories = Collections.unmodifiableList(subCategories);
        }

        public String getLabel() {
            return label;
        }

        public String getSubtitle() {
            return subtitle;
        }

        public List<String> getSubCategories() {
            return subCategories;
        }

        public String getDefaultSubCategory() {
            return subCategories.get(0);
        }
    }

    private GuiTaxonomy() {
    }

    public static List<String> getSubCategories(Group group) {
        if (group == null) {
            return Group.COMBAT.getSubCategories();
        }
        return group.getSubCategories();
    }

    public static boolean shouldHideFromList(Module module) {
        if (module == null) {
            return true;
        }
        if (module.isHidden()) {
            return true;
        }
        String name = safeLower(module.getName());
        return "config menu".equals(name);
    }

    public static Group resolveGroup(Module module) {
        if (module == null) {
            return Group.COMBAT;
        }
        String name = safeLower(module.getName());
        String sub = safeLower(module.getSubCategory());
        Category cat = module.getCategory();

        if (isClientFilterModule(name, cat) || isClientGuiModule(name, cat)) {
            return Group.CLIENT;
        }
        if (cat == Category.Macros) {
            return Group.CLIENT;
        }
        if (isCombatModule(name, sub, cat)) {
            return Group.COMBAT;
        }
        if (isMovementModule(name, cat)) {
            return Group.MOVEMENT;
        }
        if (cat == Category.Render || isRenderModule(name)) {
            return Group.RENDER;
        }
        if (isWorldModule(name, cat)) {
            return Group.WORLD;
        }
        return Group.UTILITY;
    }

    public static String resolveSubCategory(Module module) {
        Group group = resolveGroup(module);
        if (module == null) {
            return group.getDefaultSubCategory();
        }
        String name = safeLower(module.getName());
        String sub = safeLower(module.getSubCategory());
        Category cat = module.getCategory();

        switch (group) {
            case COMBAT:
                return resolveCombatSub(name, sub, cat);
            case MOVEMENT:
                return resolveMovementSub(name, cat);
            case RENDER:
                return resolveRenderSub(name);
            case UTILITY:
                return resolveUtilitySub(name, cat);
            case WORLD:
                return resolveWorldSub(name, cat);
            case CLIENT:
                return resolveClientSub(name, cat);
            default:
                return "General";
        }
    }

    public static List<Module> filterModules(
        List<Module> allModules,
        Group selectedGroup,
        String selectedSubCategory,
        String searchQuery
    ) {
        List<Module> result = new ArrayList<>();
        if (allModules == null) {
            return result;
        }
        String query = safeLower(searchQuery).trim();
        boolean searching = !query.isEmpty();
        Group targetGroup = selectedGroup != null ? selectedGroup : Group.COMBAT;
        String targetSub = selectedSubCategory != null ? selectedSubCategory : targetGroup.getDefaultSubCategory();

        for (Module mod : allModules) {
            if (shouldHideFromList(mod)) {
                continue;
            }
            if (searching) {
                if (matchesSearch(mod, query)) {
                    result.add(mod);
                }
                continue;
            }
            Group modGroup = resolveGroup(mod);
            if (modGroup != targetGroup) {
                continue;
            }
            if ("All".equalsIgnoreCase(targetSub)) {
                result.add(mod);
                continue;
            }
            String modSub = resolveSubCategory(mod);
            if (modSub.equalsIgnoreCase(targetSub)) {
                result.add(mod);
            }
        }
        return result;
    }

    public static Map<String, List<Module>> groupBySection(
        List<Module> modules,
        Group activeGroup,
        boolean globalSearch
    ) {
        Map<String, List<Module>> grouped = new LinkedHashMap<>();
        if (modules == null || modules.isEmpty()) {
            return grouped;
        }
        if (!globalSearch && activeGroup != null) {
            List<String> order = activeGroup.getSubCategories();
            for (String sub : order) {
                if (!"All".equalsIgnoreCase(sub)
                    && !"Config Center".equalsIgnoreCase(sub)
                    && !"Macros".equalsIgnoreCase(sub)) {
                    grouped.put(sub, new ArrayList<>());
                }
            }
        }
        for (Module mod : modules) {
            Group g = resolveGroup(mod);
            String sub = resolveSubCategory(mod);
            String key = globalSearch ? (g.getLabel() + "  /  " + sub) : sub;
            List<Module> bucket = grouped.get(key);
            if (bucket == null) {
                bucket = new ArrayList<>();
                grouped.put(key, bucket);
            }
            bucket.add(mod);
        }
        Map<String, List<Module>> nonEmpty = new LinkedHashMap<>();
        for (Map.Entry<String, List<Module>> entry : grouped.entrySet()) {
            List<Module> list = entry.getValue();
            if (list != null && !list.isEmpty()) {
                nonEmpty.put(entry.getKey(), list);
            }
        }
        return nonEmpty;
    }

    public static int countModules(List<Module> allModules, Group group, String subCategory) {
        if (allModules == null || group == null) {
            return 0;
        }
        if ("Config Center".equalsIgnoreCase(subCategory) || "Macros".equalsIgnoreCase(subCategory)) {
            return 0;
        }
        List<Module> filtered = filterModules(allModules, group, subCategory, "");
        return filtered.size();
    }

    public static boolean isSpecialClientPanel(Group group, String subCategory) {
        if (group != Group.CLIENT || subCategory == null) {
            return false;
        }
        return "Config Center".equalsIgnoreCase(subCategory) || "Macros".equalsIgnoreCase(subCategory);
    }

    private static boolean matchesSearch(Module mod, String query) {
        String name = safeLower(mod.getName());
        if (name.contains(query)) {
            return true;
        }
        String desc = safeLower(mod.getDescription());
        if (desc.contains(query)) {
            return true;
        }
        Group group = resolveGroup(mod);
        String groupLabel = safeLower(group.getLabel());
        if (groupLabel.contains(query)) {
            return true;
        }
        String sub = safeLower(resolveSubCategory(mod));
        return sub.contains(query);
    }

    private static boolean isClientFilterModule(String name, Category cat) {
        if (cat == Category.Filters) {
            return true;
        }
        return "antibot".equals(name)
            || "teams".equals(name)
            || "filter".equals(name)
            || "friendprotector".equals(name)
            || "nameprotect".equals(name)
            || "antitranslationkey".equals(name);
    }

    private static boolean isClientGuiModule(String name, Category cat) {
        if (cat == Category.Client) {
            return true;
        }
        return "gui".equals(name)
            || "menu".equals(name)
            || "legacy ui".equals(name)
            || "legacyui".equals(name)
            || "clientspoof".equals(name)
            || "client spoof".equals(name)
            || "recommendedconfigs".equals(name)
            || "recommended configs".equals(name);
    }

    private static boolean isCombatModule(String name, String sub, Category cat) {
        if (cat == Category.Combat || cat == Category.Crystal || cat == Category.Mace || cat == Category.CartPvP) {
            return true;
        }
        if ("crystal".equals(sub) || "mace".equals(sub) || "cart".equals(sub)) {
            return true;
        }
        if (name.contains("crystal") || name.contains("mace") || name.contains("cart") || "anchor".equals(name)) {
            return true;
        }
        if ("breachswap".equals(name) || "autowindcharge".equals(name) || "auto windcharge".equals(name)) {
            return true;
        }
        if ("killaura".equals(name) || "kill aura".equals(name) || "silentaim".equals(name) || "silent aim".equals(name) || "backtrack".equals(name)) {
            return true;
        }
        return "shielddrain".equals(name)
            || "shield drain".equals(name)
            || "autodrain".equals(name)
            || "auto drain".equals(name)
            || "autoshieldbreaker".equals(name)
            || "auto shield breaker".equals(name);
    }

    private static boolean isMovementModule(String name, Category cat) {
        if ("bridgeassist".equals(name) || "bridge assist".equals(name) || "safewalk".equals(name) || "scaffold".equals(name)) {
            return false;
        }
        if (cat == Category.Movement) {
            return true;
        }
        return "elytraboost".equals(name)
            || "elytra boost".equals(name)
            || "elytrabounce".equals(name)
            || "elytra bounce".equals(name)
            || "flight".equals(name)
            || "boatfly".equals(name)
            || "boat fly".equals(name)
            || "nofall".equals(name)
            || "no fall".equals(name)
            || "blink".equals(name)
            || "fakelag".equals(name)
            || "fake lag".equals(name);
    }

    private static boolean isRenderModule(String name) {
        return name.contains("esp")
            || name.contains("hud")
            || "chams".equals(name)
            || "tracers".equals(name)
            || "nametags".equals(name)
            || "fullbright".equals(name)
            || "xray".equals(name)
            || "handview".equals(name)
            || "hand view".equals(name)
            || "theme".equals(name)
            || "freecam".equals(name)
            || "trajectories".equals(name);
    }

    private static boolean isWorldModule(String name, Category cat) {
        if ("bridgeassist".equals(name) || "bridge assist".equals(name) || "safewalk".equals(name) || "scaffold".equals(name)) {
            return true;
        }
        if ("automine".equals(name) || "auto mine".equals(name) || "fastplace".equals(name) || "fast place".equals(name) || "autosign".equals(name) || "auto sign".equals(name)) {
            return true;
        }
        if ("autofish".equals(name) || "auto fish".equals(name) || "autotrade".equals(name) || "auto trade".equals(name) || "loot".equals(name)) {
            return true;
        }
        if (cat == Category.Farming) {
            return !name.contains("macro") && !name.contains("inventory") && !name.contains("clean") && !name.contains("fill");
        }
        return false;
    }

    private static String resolveCombatSub(String name, String sub, Category cat) {
        if (cat == Category.Crystal || "crystal".equals(sub) || name.contains("crystal") || "anchor".equals(name)) {
            return "Crystal";
        }
        if (cat == Category.Mace || "mace".equals(sub) || name.contains("mace") || "breachswap".equals(name) || name.contains("windcharge")) {
            return "Mace";
        }
        if (cat == Category.CartPvP || "cart".equals(sub) || name.contains("cart")) {
            return "Cart PvP";
        }
        if (cat == Category.Blatant || "killaura".equals(name) || "kill aura".equals(name) || "silentaim".equals(name) || "silent aim".equals(name) || "backtrack".equals(name)) {
            return "Blatant";
        }
        if (cat == Category.UHC || name.contains("drain") || name.contains("shield")) {
            return "UHC";
        }
        return "General";
    }

    private static String resolveMovementSub(String name, Category cat) {
        if (name.contains("elytra")) {
            return "Elytra";
        }
        if (cat == Category.Blatant || name.contains("flight") || name.contains("fly") || name.contains("nofall") || name.contains("no fall") || name.contains("blink") || name.contains("fakelag") || name.contains("fake lag")) {
            return "Flight & Exploit";
        }
        return "Core";
    }

    private static String resolveRenderSub(String name) {
        if ("storageesp".equals(name) || "storage esp".equals(name) || "blockesp".equals(name) || "block esp".equals(name) || name.contains("spawner") || name.contains("chunk") || "xray".equals(name) || "fullbright".equals(name)) {
            return "World Visuals";
        }
        if ("esp".equals(name) || "chams".equals(name) || "tracers".equals(name) || "nametags".equals(name) || name.contains("shader") || "trajectories".equals(name)) {
            return "ESP & Entities";
        }
        if (name.contains("hud") || "arraylist".equals(name) || "array list".equals(name) || name.contains("keybindlist") || name.contains("keybind list") || name.contains("detection")) {
            return "HUD & Overlays";
        }
        return "View & Theme";
    }

    private static String resolveUtilitySub(String name, Category cat) {
        if (name.contains("web") || "surround".equals(name)) {
            return "Traps & Webs";
        }
        if (name.contains("macro") || name.contains("keybind") || name.contains("inventory")) {
            return "Inventory & Macros";
        }
        if (name.contains("totem") || name.contains("dhand") || name.contains("mlg") || name.contains("pearl") || name.contains("throwable")) {
            return "Player & Totem";
        }
        if (cat == Category.UHC) {
            return "Player & Totem";
        }
        return "Exploit & Bypass";
    }

    private static String resolveWorldSub(String name, Category cat) {
        if (name.contains("fish") || name.contains("trade") || "loot".equals(name) || cat == Category.Farming) {
            if (!name.contains("mine") && !name.contains("place") && !name.contains("scaffold") && !name.contains("bridge") && !name.contains("sign")) {
                return "Farming & Villagers";
            }
        }
        return "Blocks & Mining";
    }

    private static String resolveClientSub(String name, Category cat) {
        if (isClientFilterModule(name, cat)) {
            return "Safety & Filters";
        }
        if (cat == Category.Macros) {
            return "Macros";
        }
        return "Client & GUI";
    }

    private static String safeLower(String input) {
        if (input == null) {
            return "";
        }
        return input.toLowerCase(Locale.ROOT);
    }
}
