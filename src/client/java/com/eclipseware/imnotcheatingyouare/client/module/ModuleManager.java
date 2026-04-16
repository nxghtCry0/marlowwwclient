package com.eclipseware.imnotcheatingyouare.client.module;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ModuleManager {
public List<Module> modules = new ArrayList<>();

public ModuleManager() {
}

public List<Module> getModules(Category category) {
    return modules.stream().filter(m -> m.getCategory() == category).collect(Collectors.toList());
}

public Module getModule(String name) {
    for (Module m : modules) {
        if (m.getName().equalsIgnoreCase(name)) {
            return m;
        }
    }
    return null;
}

}