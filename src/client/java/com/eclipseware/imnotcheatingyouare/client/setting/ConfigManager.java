package com.eclipseware.imnotcheatingyouare.client.setting;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.utils.CryptoUtils;
import com.google.gson.*;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;

public class ConfigManager {
    private static final File DIR = new File(Minecraft.getInstance().gameDirectory, "config/imnotcheatingyouare");
    private static final File CONFIG_FILE = new File(DIR, "config.enc");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static String exportSpecific(java.util.List<Module> modulesToInclude) {
        JsonObject json = new JsonObject();
        JsonArray modulesArray = new JsonArray();

        for (Module m : modulesToInclude) {
            JsonObject moduleJson = new JsonObject();
            moduleJson.addProperty("Name", m.getName());
            moduleJson.addProperty("Toggled", m.isToggled());
            moduleJson.addProperty("Keybind", m.getKeyBind());

            JsonArray settingsArray = new JsonArray();
            if (ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingsByMod(m) != null) {
                for (Setting s : ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingsByMod(m)) {
                    JsonObject settingJson = new JsonObject();
                    settingJson.addProperty("Name", s.getName());
                    if (s.isCheck()) settingJson.addProperty("Value", s.getValBoolean());
                    else if (s.isSlider()) settingJson.addProperty("Value", s.getValDouble());
                    else if (s.isCombo()) settingJson.addProperty("Value", s.getValString());
                    settingsArray.add(settingJson);
                }
            }
            moduleJson.add("Settings", settingsArray);
            modulesArray.add(moduleJson);
        }
        json.add("Modules", modulesArray);

        try {
            return CryptoUtils.encrypt(GSON.toJson(json));
        } catch (Exception e) {
            System.err.println("[EclipseWare] Failed to export config: " + e.getMessage());
        }
        return "";
    }

    public static void importString(String encrypted) {
        try {
            String rawJson = CryptoUtils.decrypt(encrypted);
            if (rawJson == null) return;
            JsonObject json = JsonParser.parseString(rawJson).getAsJsonObject();
            if (json.has("Modules")) {
                JsonArray modulesArray = json.getAsJsonArray("Modules");
                for (JsonElement elem : modulesArray) {
                    JsonObject moduleJson = elem.getAsJsonObject();
                    String name = moduleJson.get("Name").getAsString();
                    Module m = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule(name);
                    
                    if (m != null) {
                        if (moduleJson.has("Toggled")) {
                            boolean shouldBeToggled = moduleJson.get("Toggled").getAsBoolean();
                            if (shouldBeToggled && !m.isToggled()) m.toggle();
                            else if (!shouldBeToggled && m.isToggled()) m.toggle();
                        }
                        if (moduleJson.has("Keybind")) {
                            m.setKeyBind(moduleJson.get("Keybind").getAsInt());
                        }
                        if (moduleJson.has("Settings")) {
                            JsonArray settingsArray = moduleJson.getAsJsonArray("Settings");
                            for (JsonElement sElem : settingsArray) {
                                JsonObject settingJson = sElem.getAsJsonObject();
                                Setting s = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(m, settingJson.get("Name").getAsString());
                                if (s != null) {
                                    if (s.isCheck()) s.setValBoolean(settingJson.get("Value").getAsBoolean());
                                    else if (s.isSlider()) s.setValDouble(settingJson.get("Value").getAsDouble());
                                    else if (s.isCombo()) s.setValString(settingJson.get("Value").getAsString());
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[EclipseWare] Failed to import config string: " + e.getMessage());
        }
    }

    public static void save() {
        if (!DIR.exists()) DIR.mkdirs();
        JsonObject json = new JsonObject();
        JsonArray modulesArray = new JsonArray();

        for (Module m : ImnotcheatingyouareClient.INSTANCE.moduleManager.modules) {
            JsonObject moduleJson = new JsonObject();
            moduleJson.addProperty("Name", m.getName());
            moduleJson.addProperty("Toggled", m.isToggled());
            moduleJson.addProperty("Keybind", m.getKeyBind());

            JsonArray settingsArray = new JsonArray();
            if (ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingsByMod(m) != null) {
                for (Setting s : ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingsByMod(m)) {
                    JsonObject settingJson = new JsonObject();
                    settingJson.addProperty("Name", s.getName());
                    if (s.isCheck()) settingJson.addProperty("Value", s.getValBoolean());
                    else if (s.isSlider()) settingJson.addProperty("Value", s.getValDouble());
                    else if (s.isCombo()) settingJson.addProperty("Value", s.getValString());
                    settingsArray.add(settingJson);
                }
            }
            moduleJson.add("Settings", settingsArray);
            modulesArray.add(moduleJson);
        }
        json.add("Modules", modulesArray);

        try {
            String rawJson = GSON.toJson(json);
            String encrypted = CryptoUtils.encrypt(rawJson);
            if (encrypted != null) {
                FileWriter writer = new FileWriter(CONFIG_FILE);
                writer.write(encrypted);
                writer.close();
            }
        } catch (Exception e) {
            System.err.println("[EclipseWare] Failed to save config to disk: " + e.getMessage());
        }
    }

    public static void load() {
        if (!CONFIG_FILE.exists()) return;
        try {
            String encrypted = new String(Files.readAllBytes(CONFIG_FILE.toPath()));
            String rawJson = CryptoUtils.decrypt(encrypted);
            if (rawJson == null) return; // If decryption fails, abort

            JsonObject json = JsonParser.parseString(rawJson).getAsJsonObject();
            if (json.has("Modules")) {
                JsonArray modulesArray = json.getAsJsonArray("Modules");
                for (JsonElement elem : modulesArray) {
                    JsonObject moduleJson = elem.getAsJsonObject();
                    String name = moduleJson.get("Name").getAsString();
                    Module m = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule(name);
                    
                    if (m != null) {
                        if (moduleJson.has("Toggled")) {
                            boolean shouldBeToggled = moduleJson.get("Toggled").getAsBoolean();
                            if (shouldBeToggled && !m.isToggled()) {
                                m.toggle();
                            } else if (!shouldBeToggled && m.isToggled()) {
                                m.toggle();
                            }
                        }
                        if (moduleJson.has("Keybind")) {
                            m.setKeyBind(moduleJson.get("Keybind").getAsInt());
                        }
                        if (moduleJson.has("Settings")) {
                            JsonArray settingsArray = moduleJson.getAsJsonArray("Settings");
                            for (JsonElement sElem : settingsArray) {
                                JsonObject settingJson = sElem.getAsJsonObject();
                                Setting s = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(m, settingJson.get("Name").getAsString());
                                if (s != null) {
                                    if (s.isCheck()) s.setValBoolean(settingJson.get("Value").getAsBoolean());
                                    else if (s.isSlider()) s.setValDouble(settingJson.get("Value").getAsDouble());
                                    else if (s.isCombo()) s.setValString(settingJson.get("Value").getAsString());
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[EclipseWare] Failed to load config from disk: " + e.getMessage());
        }
    }
}