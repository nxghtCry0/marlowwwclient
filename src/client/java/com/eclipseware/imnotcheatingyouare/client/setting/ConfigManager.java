package com.eclipseware.imnotcheatingyouare.client.setting;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.glassui.bridge.ClientGuiRegistry;
import com.eclipseware.imnotcheatingyouare.client.glassui.bridge.ClientNotificationService;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.module.ModuleManager;
import com.eclipseware.imnotcheatingyouare.client.utils.CryptoUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.InputConstants;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.util.List;
import net.minecraft.client.Minecraft;

public class ConfigManager {
    private static final File DIR = new File(Minecraft.getInstance().gameDirectory, "config/imnotcheatingyouare");
    private static final File CONFIG_FILE = new File(DIR, "config.enc");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int CONFIG_VERSION = 2;

    public static String exportSpecific(List<Module> modulesToInclude) {
        JsonObject json = new JsonObject();
        JsonArray modulesArray = buildModulesArray(modulesToInclude);
        json.add("Modules", modulesArray);
        json.addProperty("ConfigVersion", CONFIG_VERSION);

        try {
            String serialized = GSON.toJson(json);
            return CryptoUtils.encrypt(serialized);
        } catch (Exception e) {
            System.err.println("[EclipseWare] Failed to export config: " + e.getMessage());
        }
        return "";
    }

    public static void importString(String encrypted) {
        ClientNotificationService.setSuppressModuleHook(true);
        try {
            String rawJson = CryptoUtils.decrypt(encrypted);
            if (rawJson == null) {
                return;
            }
            JsonElement parsed = JsonParser.parseString(rawJson);
            JsonObject json = parsed.getAsJsonObject();
            int savedVersion = readConfigVersion(json);
            boolean keybindsAreCurrent = savedVersion == CONFIG_VERSION;
            if (json.has("Modules")) {
                JsonArray modulesArray = json.getAsJsonArray("Modules");
                applyModulesArray(modulesArray, keybindsAreCurrent);
            }
        } catch (Exception e) {
            System.err.println("[EclipseWare] Failed to import config string: " + e.getMessage());
        } finally {
            ClientNotificationService.setSuppressModuleHook(false);
        }
    }

    public static void save() {
        if (!DIR.exists()) {
            DIR.mkdirs();
        }
        ImnotcheatingyouareClient client = ImnotcheatingyouareClient.INSTANCE;
        if (client == null || client.moduleManager == null) {
            return;
        }
        ModuleManager mm = client.moduleManager;
        JsonObject json = new JsonObject();
        JsonArray modulesArray = buildModulesArray(mm.modules);
        json.add("Modules", modulesArray);
        json.addProperty("ConfigVersion", CONFIG_VERSION);

        try {
            String rawJson = GSON.toJson(json);
            String encrypted = CryptoUtils.encrypt(rawJson);
            if (encrypted != null) {
                try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
                    writer.write(encrypted);
                }
            }
        } catch (Exception e) {
            System.err.println("[EclipseWare] Failed to save config to disk: " + e.getMessage());
        }
        if (ClientGuiRegistry.isInitialized()) {
            ClientGuiRegistry.saveGuiPrefs();
        }
    }

    public static void load() {
        if (!CONFIG_FILE.exists()) {
            return;
        }
        ClientNotificationService.setSuppressModuleHook(true);
        try {
            byte[] bytes = Files.readAllBytes(CONFIG_FILE.toPath());
            String encrypted = new String(bytes);
            String rawJson = CryptoUtils.decrypt(encrypted);
            if (rawJson == null) {
                return;
            }

            JsonElement parsed = JsonParser.parseString(rawJson);
            JsonObject json = parsed.getAsJsonObject();
            int savedVersion = readConfigVersion(json);
            boolean keybindsAreCurrent = savedVersion == CONFIG_VERSION;
            if (!keybindsAreCurrent) {
                System.out.println("[EclipseWare] Config was saved with a different keybind encoding; ignoring saved keybinds and keeping defaults.");
            }

            if (json.has("Modules")) {
                JsonArray modulesArray = json.getAsJsonArray("Modules");
                applyModulesArray(modulesArray, keybindsAreCurrent);
            }

            if (!keybindsAreCurrent) {
                save();
            }
        } catch (Exception e) {
            System.err.println("[EclipseWare] Failed to load config from disk: " + e.getMessage());
        } finally {
            ClientNotificationService.setSuppressModuleHook(false);
        }
    }

    private static int readConfigVersion(JsonObject json) {
        if (json.has("ConfigVersion")) {
            JsonElement verElem = json.get("ConfigVersion");
            return verElem.getAsInt();
        }
        return 1;
    }

    private static JsonArray buildModulesArray(List<Module> modules) {
        JsonArray modulesArray = new JsonArray();
        ImnotcheatingyouareClient client = ImnotcheatingyouareClient.INSTANCE;
        SettingsManager sm = client != null ? client.settingsManager : null;

        for (Module m : modules) {
            JsonObject moduleJson = new JsonObject();
            moduleJson.addProperty("Name", m.getName());
            moduleJson.addProperty("Toggled", m.isToggled());
            moduleJson.addProperty("Keybind", m.getKeyBind());
            moduleJson.addProperty("BindHold", m.isHoldMode());

            JsonArray settingsArray = new JsonArray();
            List<Setting> modSettings = sm != null ? sm.getSettingsByMod(m) : null;
            if (modSettings != null) {
                for (Setting s : modSettings) {
                    JsonObject settingJson = new JsonObject();
                    settingJson.addProperty("Name", s.getName());
                    if (s.isCheck()) {
                        settingJson.addProperty("Value", s.getValBoolean());
                    } else if (s.isSlider()) {
                        settingJson.addProperty("Value", s.getValDouble());
                    } else if (s.isRange()) {
                        settingJson.addProperty("Value", s.getRangeLow());
                        settingJson.addProperty("Value2", s.getRangeHigh());
                    } else if (s.isCurve()) {
                        double[] c = s.getCurve();
                        settingJson.addProperty("Value", c[0]);
                        settingJson.addProperty("Value2", c[1]);
                        settingJson.addProperty("Value3", c[2]);
                        settingJson.addProperty("Value4", c[3]);
                    } else if (s.isCombo()) {
                        settingJson.addProperty("Value", s.getValString());
                    } else if (s.isText()) {
                        settingJson.addProperty("Value", s.getValText());
                    } else if (s.isColor()) {
                        settingJson.addProperty("Value", s.getValColor());
                    }
                    settingsArray.add(settingJson);
                }
            }
            moduleJson.add("Settings", settingsArray);
            modulesArray.add(moduleJson);
        }
        return modulesArray;
    }

    private static void applyModulesArray(JsonArray modulesArray, boolean keybindsAreCurrent) {
        ImnotcheatingyouareClient client = ImnotcheatingyouareClient.INSTANCE;
        if (client == null || client.moduleManager == null) {
            return;
        }
        ModuleManager mm = client.moduleManager;
        SettingsManager sm = client.settingsManager;

        for (JsonElement elem : modulesArray) {
            JsonObject moduleJson = elem.getAsJsonObject();
            JsonElement nameElem = moduleJson.get("Name");
            String name = nameElem.getAsString();
            Module m = mm.getModule(name);
            if (m == null) {
                continue;
            }

            boolean isLegacyMenuEntry = "Menu".equalsIgnoreCase(name) && !hasSettingInJson(moduleJson, "Show Logo");

            if (moduleJson.has("Toggled") && !isLegacyMenuEntry) {
                JsonElement togElem = moduleJson.get("Toggled");
                boolean shouldBeToggled = togElem.getAsBoolean();
                if (shouldBeToggled != m.isToggled()) {
                    m.toggle();
                }
            }
            if (keybindsAreCurrent && moduleJson.has("Keybind")) {
                JsonElement kbElem = moduleJson.get("Keybind");
                int savedKb = kbElem.getAsInt();
                if (isLegacyMenuEntry) {
                    Module guiMod = mm.getModule("GUI");
                    if (savedKb != 0 && guiMod != null) {
                        guiMod.setKeyBind(savedKb);
                    }
                    m.setKeyBind(0);
                } else if ("GUI".equalsIgnoreCase(name) && savedKb == 0) {
                    if (m.getKeyBind() == 0) {
                        m.setKeyBind(InputConstants.KEY_RSHIFT);
                    }
                } else {
                    m.setKeyBind(savedKb);
                    if (moduleJson.has("BindHold")) {
                        JsonElement holdElem = moduleJson.get("BindHold");
                        m.setHoldMode(holdElem.getAsBoolean());
                    }
                }
            }
            if (moduleJson.has("Settings") && sm != null) {
                JsonArray settingsArray = moduleJson.getAsJsonArray("Settings");
                applySettingsArray(sm, m, settingsArray);
            }
        }
        Module guiMod = mm.getModule("GUI");
        if (guiMod != null && guiMod.getKeyBind() == 0) {
            guiMod.setKeyBind(InputConstants.KEY_RSHIFT);
        }
    }

    private static boolean hasSettingInJson(JsonObject moduleJson, String settingName) {
        if (!moduleJson.has("Settings")) {
            return false;
        }
        JsonArray arr = moduleJson.getAsJsonArray("Settings");
        for (JsonElement elem : arr) {
            JsonObject obj = elem.getAsJsonObject();
            if (obj.has("Name")) {
                JsonElement n = obj.get("Name");
                if (settingName.equalsIgnoreCase(n.getAsString())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void applySettingsArray(SettingsManager sm, Module m, JsonArray settingsArray) {
        for (JsonElement sElem : settingsArray) {
            JsonObject settingJson = sElem.getAsJsonObject();
            JsonElement sNameElem = settingJson.get("Name");
            Setting s = sm.getSettingByName(m, sNameElem.getAsString());
            if (s == null || !settingJson.has("Value")) {
                continue;
            }
            JsonElement v1 = settingJson.get("Value");
            if (s.isCheck() && v1.isJsonPrimitive() && v1.getAsJsonPrimitive().isBoolean()) {
                s.setValBoolean(v1.getAsBoolean());
            } else if (s.isSlider() && v1.isJsonPrimitive() && v1.getAsJsonPrimitive().isNumber()) {
                s.setValDouble(v1.getAsDouble());
            } else if (s.isRange() && settingJson.has("Value2")) {
                JsonElement v2 = settingJson.get("Value2");
                s.setRange(v1.getAsDouble(), v2.getAsDouble());
            } else if (s.isCurve() && settingJson.has("Value4")) {
                JsonElement v2 = settingJson.get("Value2");
                JsonElement v3 = settingJson.get("Value3");
                JsonElement v4 = settingJson.get("Value4");
                s.setCurve(v1.getAsDouble(), v2.getAsDouble(), v3.getAsDouble(), v4.getAsDouble());
            } else if (s.isCombo() && v1.isJsonPrimitive() && v1.getAsJsonPrimitive().isString()) {
                s.setValString(v1.getAsString());
            } else if (s.isText() && v1.isJsonPrimitive() && v1.getAsJsonPrimitive().isString()) {
                s.setValText(v1.getAsString());
            } else if (s.isColor() && v1.isJsonPrimitive() && v1.getAsJsonPrimitive().isNumber()) {
                int loadedColor = v1.getAsInt();
                int rgb24 = loadedColor & 0xFFFFFF;
                int oldOrange = (255 << 16) | (118 << 8) | 26;
                int oldDark = (18 << 16) | (9 << 8) | 28;
                if (rgb24 == oldOrange) {
                    loadedColor = (loadedColor & 0xFF000000) | 0x2AA8FF;
                } else if (rgb24 == oldDark) {
                    loadedColor = (loadedColor & 0xFF000000) | 0x10141E;
                }
                s.setValColor(loadedColor);
            }
        }
    }
}
