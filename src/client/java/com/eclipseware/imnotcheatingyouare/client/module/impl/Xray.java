package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;

public class Xray extends Module {
    public static Xray INSTANCE;
    
    private final java.util.Set<String> customBlocks = new java.util.HashSet<>();

    public Xray() {
        super("Xray", Category.Render, "Only renders ores, chests, and custom blocks.");
        INSTANCE = this;
        
        addDefault("diamond_ore");
        addDefault("ancient_debris");
        addDefault("gold_ore");
        addDefault("iron_ore");
        addDefault("chest");
        addDefault("spawner");
    }
    
    private void addDefault(String id) {
        customBlocks.add(id);
        com.eclipseware.imnotcheatingyouare.client.setting.SettingsManager sm = com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient.INSTANCE.settingsManager;
        if (sm.getSettingByName(this, "Show " + id) == null) {
            sm.rSetting(new com.eclipseware.imnotcheatingyouare.client.setting.Setting("Show " + id, this, true));
        }
    }
    
    public boolean isImportantBlock(String blockName) {
        if (blockName.contains("ore") && !customBlocks.contains(blockName)) {
            // By default, if it's an ore not in the list, just show it or fallback
            // But let's dynamically register it if seen to populate the GUI!
            addDefault(blockName);
        }
        
        com.eclipseware.imnotcheatingyouare.client.setting.Setting s = com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Show " + blockName);
        if (s != null) return s.getValBoolean();
        
        return false;
    }

    @Override
    public void onEnable() {
        if (mc.levelRenderer != null) mc.levelRenderer.allChanged(); 
    }

    @Override
    public void onDisable() {
        if (mc.levelRenderer != null) mc.levelRenderer.allChanged(); 
    }
}