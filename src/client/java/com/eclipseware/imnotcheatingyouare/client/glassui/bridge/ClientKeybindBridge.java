package com.eclipseware.imnotcheatingyouare.client.glassui.bridge;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.glassui.bind.UiInputBind;
import com.eclipseware.imnotcheatingyouare.client.glassui.bind.UiKeybindProvider;
import com.eclipseware.imnotcheatingyouare.client.macro.Macro;
import com.eclipseware.imnotcheatingyouare.client.macro.MacroManager;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.module.ModuleManager;
import com.eclipseware.imnotcheatingyouare.client.setting.ConfigManager;
import java.util.HashMap;
import java.util.Map;

public final class ClientKeybindBridge implements UiKeybindProvider {
    public static final ClientKeybindBridge INSTANCE = new ClientKeybindBridge();

    private final Map<String, UiInputBind> systemBinds = new HashMap<>();

    private ClientKeybindBridge() {
    }

    @Override
    public UiInputBind getBind(String itemId) {
        if (itemId == null) {
            return UiInputBind.NONE;
        }
        if (itemId.startsWith("macro:")) {
            String macroName = itemId.substring("macro:".length());
            Macro m = findMacro(macroName);
            return m != null ? UiInputBind.fromModuleKeyBind(m.getKeybind()) : UiInputBind.NONE;
        }
        Module mod = ClientGuiRegistry.findModuleByCardId(itemId);
        if (mod != null) {
            return UiInputBind.fromModuleKeyBind(mod.getKeyBind());
        }
        return systemBinds.getOrDefault(itemId, UiInputBind.NONE);
    }

    @Override
    public void setBind(String itemId, UiInputBind bind) {
        if (itemId == null) {
            return;
        }
        UiInputBind safe = bind != null ? bind : UiInputBind.NONE;
        if (itemId.startsWith("macro:")) {
            String macroName = itemId.substring("macro:".length());
            Macro m = findMacro(macroName);
            if (m != null) {
                m.setKeybind(safe.toModuleKeyBind());
                MacroManager.save();
            }
            return;
        }
        Module mod = ClientGuiRegistry.findModuleByCardId(itemId);
        if (mod != null) {
            mod.setKeyBind(safe.toModuleKeyBind());
            ConfigManager.save();
            return;
        }
        if (safe.isBound()) {
            systemBinds.put(itemId, safe);
        } else {
            systemBinds.remove(itemId);
        }
        ClientGuiRegistry.saveGuiPrefs();
    }

    public Map<String, UiInputBind> systemBindsSnapshot() {
        return new HashMap<>(systemBinds);
    }

    public void loadSystemBinds(Map<String, UiInputBind> loaded) {
        systemBinds.clear();
        if (loaded != null) {
            systemBinds.putAll(loaded);
        }
    }

    @Override
    public int boundCount() {
        int count = 0;
        ImnotcheatingyouareClient client = ImnotcheatingyouareClient.INSTANCE;
        if (client != null && client.moduleManager != null) {
            ModuleManager mm = client.moduleManager;
            for (Module mod : mm.modules) {
                if (!mod.isHidden() && mod.getKeyBind() != 0) {
                    count++;
                }
            }
        }
        for (Macro m : MacroManager.getMacros()) {
            if (m.getKeybind() != 0) {
                count++;
            }
        }
        return count;
    }

    private static Macro findMacro(String name) {
        for (Macro m : MacroManager.getMacros()) {
            if (m.getName().equalsIgnoreCase(name)) {
                return m;
            }
        }
        return null;
    }
}
