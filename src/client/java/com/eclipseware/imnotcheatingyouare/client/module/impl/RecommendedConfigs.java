package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.clickgui.ConfigRecommendationScreen;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.network.chat.Component;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;

public class RecommendedConfigs extends Module {
    private String lastServerIp = null;
    private boolean fetched = false;

    public static class FoundConfig {
        public String name;
        public String base64;
        public String modulesPreview;

        public FoundConfig(String name, String base64, String modulesPreview) {
            this.name = name;
            this.base64 = base64;
            this.modulesPreview = modulesPreview;
        }
    }

    private List<FoundConfig> currentServerConfigs = new ArrayList<>();

    public RecommendedConfigs() {
        super("RecommendedConfigs", Category.Misc, "Recommends server-specific configs upon joining.", true);
        this.setToggled(true);
    }

    @Override
    public void onTick() {
        if (!isToggled() || mc.player == null) return;

        String currentServer = mc.getCurrentServer() != null ? mc.getCurrentServer().ip : null;

        if (currentServer != null && !currentServer.equals(lastServerIp)) {
            lastServerIp = currentServer;
            fetched = false;
            currentServerConfigs.clear();
            fetchConfigsAndCheck(currentServer);
        } else if (currentServer == null) {
            lastServerIp = null;
            fetched = false;
            currentServerConfigs.clear();
        }

        if (fetched && currentServer != null && mc.screen == null && !currentServerConfigs.isEmpty()) {
            mc.setScreen(new ConfigRecommendationScreen(new ArrayList<>(currentServerConfigs)));
            currentServerConfigs.clear(); 
        }
    }

    private void fetchConfigsAndCheck(String ip) {
        CompletableFuture.supplyAsync(() -> {
            List<FoundConfig> found = new ArrayList<>();
            try {
                URL apiUrl = java.net.URI.create("https://api.github.com/repos/nxghtCry0/marlowwwclient/contents/configs").toURL();
                HttpURLConnection conn = (HttpURLConnection) apiUrl.openConnection();
                conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
                if (conn.getResponseCode() != 200) return found;

                InputStreamReader reader = new InputStreamReader(conn.getInputStream());
                JsonArray filesArray = JsonParser.parseReader(reader).getAsJsonArray();
                reader.close();

                String normalizedIp = ip.toLowerCase();

                for (JsonElement elem : filesArray) {
                    JsonObject fileObj = elem.getAsJsonObject();
                    String fileName = fileObj.get("name").getAsString();
                    if (!fileName.endsWith(".json")) continue;

                    String downloadUrl = fileObj.get("download_url").getAsString();

                    URL configUrl = java.net.URI.create(downloadUrl).toURL();
                    Scanner s = new Scanner(configUrl.openStream());
                    StringBuilder sb = new StringBuilder();
                    while (s.hasNextLine()) sb.append(s.nextLine());
                    s.close();

                    JsonObject configObj = JsonParser.parseString(sb.toString()).getAsJsonObject();
                    if (!configObj.has("server") || !configObj.has("name") || !configObj.has("configBase64")) continue;

                    JsonElement serverElement = configObj.get("server");
                    List<String> serverGlobs = new ArrayList<>();
                    if (serverElement.isJsonArray()) {
                        for (JsonElement el : serverElement.getAsJsonArray()) {
                            serverGlobs.add(el.getAsString().toLowerCase());
                        }
                    } else {
                        String rawServer = serverElement.getAsString().toLowerCase();
                        if (rawServer.contains(",")) {
                            for (String part : rawServer.split(",")) {
                                serverGlobs.add(part.trim());
                            }
                        } else {
                            serverGlobs.add(rawServer);
                        }
                    }

                    boolean matchesServer = false;
                    for (String serverGlob : serverGlobs) {
                        if (serverGlob.startsWith("*")) {
                            if (normalizedIp.endsWith(serverGlob.substring(1)) || normalizedIp.equals(serverGlob.substring(1))) {
                                matchesServer = true;
                                break;
                            }
                        } else {
                            if (normalizedIp.equals(serverGlob)) {
                                matchesServer = true;
                                break;
                            }
                        }
                    }

                    if (matchesServer) {
                        String configName = configObj.get("name").getAsString();
                        String base64 = configObj.get("configBase64").getAsString();
                        
                        String preview = "Modules: ";
                        try {
                            String rawJson = com.eclipseware.imnotcheatingyouare.client.utils.CryptoUtils.decrypt(base64);
                            if (rawJson != null) {
                                JsonObject payload = JsonParser.parseString(rawJson).getAsJsonObject();
                                if (payload.has("Modules")) {
                                    JsonArray mods = payload.getAsJsonArray("Modules");
                                    List<String> modNames = new ArrayList<>();
                                    for (JsonElement mElem : mods) {
                                        JsonObject mObj = mElem.getAsJsonObject();
                                        String mName = mObj.get("Name").getAsString();
                                        String keybind = "None";
                                        if (mObj.has("Keybind") && mObj.get("Keybind").getAsInt() != -1) {
                                            keybind = String.valueOf(mObj.get("Keybind").getAsInt());
                                        }
                                        modNames.add(mName + " (Key: " + keybind + ")");
                                    }
                                    preview += String.join(", ", modNames);
                                }
                            }
                        } catch (Exception e) {
                            preview = "Could not preview modules.";
                        }

                        found.add(new FoundConfig(configName, base64, preview));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return found;
        }).thenAccept(found -> {
            mc.execute(() -> {
                currentServerConfigs = found;
                fetched = true;
            });
        });
    }
}
