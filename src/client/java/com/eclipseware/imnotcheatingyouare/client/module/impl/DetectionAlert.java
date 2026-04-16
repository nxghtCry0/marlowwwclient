package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.clickgui.DetectionWarningScreen;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import net.minecraft.network.chat.Component;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;

public class DetectionAlert extends Module {
    private List<String> dbRules = new ArrayList<>();
    private String lastServerIp = null;
    private boolean fetched = false;
    private List<Module> previouslyEnabled = new ArrayList<>();

    public DetectionAlert() {
        super("DetectionDB", Category.Misc, "Warns you if you are using detected modules on a server.", true);
        this.setToggled(true); 
    }

    @Override
    public void onTick() {
        if (!isToggled() || mc.player == null) return;

        String currentServer = mc.getCurrentServer() != null ? mc.getCurrentServer().ip : null;

        if (currentServer != null && !currentServer.equals(lastServerIp)) {
            lastServerIp = currentServer;
            previouslyEnabled.clear();
            
            if (!fetched) {
                fetchDatabaseAndCheck(currentServer);
            } else {
                checkCurrentServer(currentServer);
            }
        } else if (currentServer == null) {
            lastServerIp = null;
            previouslyEnabled.clear();
        }

        if (fetched && currentServer != null && mc.screen == null) {
            List<Module> currentlyEnabled = new ArrayList<>();
            for (Module m : ImnotcheatingyouareClient.INSTANCE.moduleManager.modules) {
                if (m.isToggled()) currentlyEnabled.add(m);
            }
            List<Module> newlyEnabled = new ArrayList<>(currentlyEnabled);
            newlyEnabled.removeAll(previouslyEnabled);
            
            List<Module> newlyFlagged = new ArrayList<>();
            for (Module m : newlyEnabled) {
                if (m != this && isModuleDetected(m, currentServer)) {
                    newlyFlagged.add(m);
                }
            }
            if (!newlyFlagged.isEmpty()) {
                mc.setScreen(new DetectionWarningScreen(newlyFlagged));
            }
            previouslyEnabled = currentlyEnabled;
        }
    }

    private void fetchDatabaseAndCheck(String ip) {
        CompletableFuture.supplyAsync(() -> {
            try {
                URL url = new URL("https://gitlab.com/tejascerts/marlowww-client-detection-db/-/raw/main/db.txt");
                Scanner s = new Scanner(url.openStream());
                List<String> rules = new ArrayList<>();
                while (s.hasNextLine()) {
                    String line = s.nextLine().trim();
                    if (!line.isEmpty()) rules.add(line);
                }
                s.close();
                return rules;
            } catch (Exception e) {
                return null;
            }
        }).thenAccept(rules -> {
            mc.execute(() -> {
                if (rules == null) {
                    if (mc.player != null) {
                        mc.player.displayClientMessage(
                            Component.literal("\u00a7cYour client is disconnected from the database and does not have the latest detections or bypasses from the database!"), false
                        );
                    }
                } else {
                    dbRules = rules;
                    fetched = true;
                    checkCurrentServer(ip);
                }
            });
        });
    }

    private void checkCurrentServer(String ip) {
        if (ip == null || dbRules.isEmpty() || mc.player == null) return;

        List<Module> flagged = new ArrayList<>();
        for (Module m : ImnotcheatingyouareClient.INSTANCE.moduleManager.modules) {
            if (m.isToggled() && m != this && isModuleDetected(m, ip)) {
                flagged.add(m);
            }
        }

        if (!flagged.isEmpty()) {
            mc.setScreen(new DetectionWarningScreen(flagged));
        }
    }

    public boolean isModuleDetected(Module m, String ip) {
        if (ip == null || dbRules.isEmpty()) return false;
        String normalizedIp = ip.toLowerCase();
        for (String rule : dbRules) {
            if (!rule.contains(":")) continue;
            String[] parts = rule.split(":");
            if (parts.length < 2) continue;
            String moduleName = parts[0].trim().toLowerCase();
            String serverGlob = parts[1].trim().toLowerCase();

            boolean matchesServer = false;
            if (serverGlob.startsWith("*")) {
                matchesServer = normalizedIp.endsWith(serverGlob.substring(1)) || normalizedIp.equals(serverGlob.substring(1));
            } else {
                matchesServer = normalizedIp.equals(serverGlob);
            }

            if (matchesServer) {
                if (moduleName.equals("*") || moduleName.equals(m.getName().toLowerCase())) {
                    return true;
                }
            }
        }
        return false;
    }
}
