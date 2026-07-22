package com.eclipseware.imnotcheatingyouare.client.setting;

import com.eclipseware.imnotcheatingyouare.client.module.Module;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SettingsManager {
    private final List<Setting> settings = new ArrayList<>();
    private final Map<Module, List<Setting>> settingsByMod = new HashMap<>();
    private final Map<String, Setting> settingByName = new HashMap<>();

    public synchronized void rSetting(Setting in) {
        this.settings.add(in);
        
        Module parent = in.getParentMod();
        if (parent != null) {
            settingsByMod.computeIfAbsent(parent, k -> new ArrayList<>()).add(in);
            String key = parent.getName().toLowerCase() + ":" + in.getName().toLowerCase();
            settingByName.put(key, in);
        }
    }

    public synchronized List<Setting> getSettingsByMod(Module mod) {
        return settingsByMod.getOrDefault(mod, java.util.Collections.emptyList());
    }

    public Setting getSettingByName(Module mod, String name) {
        if (mod == null || name == null) return null;
        String key = mod.getName().toLowerCase() + ":" + name.toLowerCase();
        Setting exact = settingByName.get(key);
        if (exact != null) return exact;

        if (name.length() > 2 && (name.endsWith(" R") || name.endsWith(" G") || name.endsWith(" B") ||
                                 name.endsWith(" r") || name.endsWith(" g") || name.endsWith(" b"))) {
            String baseName = name.substring(0, name.length() - 2);
            String baseKey = mod.getName().toLowerCase() + ":" + baseName.toLowerCase();
            Setting foundBase = settingByName.get(baseKey);
            if (foundBase == null) {
                foundBase = settingByName.get(baseKey + " color");
            }
            if (foundBase != null && foundBase.isColor()) {
                final Setting baseSetting = foundBase;
                final char channel = Character.toUpperCase(name.charAt(name.length() - 1));
                return new Setting(name, mod, 0.0, 0.0, 255.0, true) {
                    @Override
                    public double getValDouble() {
                        int color = baseSetting.getValColor();
                        if (channel == 'R') return (color >> 16) & 0xFF;
                        if (channel == 'G') return (color >> 8) & 0xFF;
                        return color & 0xFF;
                    }
                    @Override
                    public void setValDouble(double in) {
                        int color = baseSetting.getValColor();
                        int r = (color >> 16) & 0xFF;
                        int g = (color >> 8) & 0xFF;
                        int b = color & 0xFF;
                        if (channel == 'R') r = (int) in;
                        else if (channel == 'G') g = (int) in;
                        else b = (int) in;
                        baseSetting.setValColor((0xFF << 24) | (r << 16) | (g << 8) | b);
                    }
                };
            }
        }
        return null;
    }
}