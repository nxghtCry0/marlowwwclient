package com.eclipseware.imnotcheatingyouare.client.setting;

import com.eclipseware.imnotcheatingyouare.client.module.Module;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SettingsManager {
private List<Setting> settings = new ArrayList<>();

public void rSetting(Setting in) {
    this.settings.add(in);
}

public List<Setting> getSettingsByMod(Module mod) {
    return settings.stream().filter(s -> s.getParentMod().equals(mod)).collect(Collectors.toList());
}

public Setting getSettingByName(Module mod, String name) {
    for (Setting s : getSettingsByMod(mod)) {
        if (s.getName().equalsIgnoreCase(name)) {
            return s;
        }
    }
    return null;
}

}