package com.eclipseware.imnotcheatingyouare.client.macro;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MacroManager {
    private static final Minecraft mc = Minecraft.getInstance();
    private static final File DIR = new File(mc.gameDirectory, "config/imnotcheatingyouare");
    private static final File MACROS_FILE = new File(DIR, "macros.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final List<Macro> macros = new ArrayList<>();
    private static Macro activeMacro = null;
    private static Macro currentMacro = null; // Currently recording or playing macro
    
    private static boolean isRecording = false;
    private static boolean isPlaying = false;
    private static long lastActionTime = 0;

    private static int playIndex = 0;
    private static long nextActionTime = 0;
    private static long windowHandle = 0;

    private static final Map<Macro, Boolean> macroKeyStates = new HashMap<>();

    public static List<Macro> getMacros() {
        return macros;
    }

    public static Macro getActiveMacro() {
        if (activeMacro == null && !macros.isEmpty()) {
            activeMacro = macros.get(0);
        }
        return activeMacro;
    }

    public static void setActiveMacro(Macro m) {
        if (m != null && macros.contains(m)) {
            activeMacro = m;
            currentMacro = m;
        }
    }

    public static Macro createNewMacro(String name) {
        Macro m = new Macro(name);
        macros.add(m);
        if (activeMacro == null) {
            activeMacro = m;
            currentMacro = m;
        }
        save();
        return m;
    }

    public static void deleteMacro(Macro m) {
        if (m == null) return;
        macros.remove(m);
        if (activeMacro == m) {
            activeMacro = macros.isEmpty() ? null : macros.get(0);
            currentMacro = activeMacro;
        }
        save();
    }

    public static Macro getCurrentMacro() {
        return currentMacro != null ? currentMacro : getActiveMacro();
    }

    public static boolean isRecording() {
        return isRecording;
    }

    public static boolean isPlaying() {
        return isPlaying;
    }

    public static void startRecord(Macro macro) {
        if (isRecording || macro == null) return;
        currentMacro = macro;
        currentMacro.clear();
        isRecording = true;
        isPlaying = false;
        lastActionTime = System.currentTimeMillis();
    }

    public static void startRecord() {
        startRecord(getActiveMacro());
    }

    public static void stopRecord() {
        if (!isRecording) return;
        isRecording = false;
        save();
    }

    public static void startPlay(Macro macro) {
        if (isPlaying || macro == null) return;
        if (macro.getActions().isEmpty()) return;
        currentMacro = macro;
        isPlaying = true;
        isRecording = false;
        playIndex = 0;
        nextActionTime = System.currentTimeMillis();
    }

    public static void startPlay() {
        startPlay(getActiveMacro());
    }

    public static void stopPlay() {
        isPlaying = false;
    }

    public static void recordKey(int key, boolean pressed, long window) {
        if (!isRecording) return;
        windowHandle = window;
        long current = System.currentTimeMillis();
        long delay = current - lastActionTime;
        Macro m = getCurrentMacro();
        if (m == null) return;
        if (delay > 0) {
            m.addAction(new MacroAction(MacroAction.ActionType.DELAY, 0, false, delay));
        }
        MacroAction.ActionType type = pressed ? MacroAction.ActionType.KEY_PRESS : MacroAction.ActionType.KEY_RELEASE;
        m.addAction(new MacroAction(type, key, pressed, 0));
        lastActionTime = current;
    }

    public static void recordMouse(int button, boolean pressed, long window) {
        if (!isRecording) return;
        windowHandle = window;
        long current = System.currentTimeMillis();
        long delay = current - lastActionTime;
        Macro m = getCurrentMacro();
        if (m == null) return;
        if (delay > 0) {
            m.addAction(new MacroAction(MacroAction.ActionType.DELAY, 0, false, delay));
        }
        MacroAction.ActionType type = pressed ? MacroAction.ActionType.MOUSE_CLICK : MacroAction.ActionType.MOUSE_RELEASE;
        m.addAction(new MacroAction(type, button, pressed, 0));
        lastActionTime = current;
    }

    public static void tick() {
        if (!isPlaying) return;
        if (mc.level == null || mc.player == null) {
            stopPlay();
            return;
        }

        Macro m = currentMacro != null ? currentMacro : getActiveMacro();
        if (m == null) {
            stopPlay();
            return;
        }

        List<MacroAction> actions = m.getActions();
        while (isPlaying && playIndex < actions.size()) {
            MacroAction action = actions.get(playIndex);
            if (action.getType() == MacroAction.ActionType.DELAY) {
                long now = System.currentTimeMillis();
                if (now < nextActionTime + action.getDelayMs()) {
                    break;
                }
                nextActionTime += action.getDelayMs();
                playIndex++;
            } else {
                executeAction(action);
                playIndex++;
            }
        }

        if (playIndex >= actions.size()) {
            stopPlay();
        }
    }

    private static void executeAction(MacroAction action) {
        if (mc.level == null || mc.player == null) return;

        long win = windowHandle;
        if (win == 0) {
            try {
                for (java.lang.reflect.Field f : mc.getWindow().getClass().getDeclaredFields()) {
                    if (f.getType() == long.class) {
                        f.setAccessible(true);
                        win = f.getLong(mc.getWindow());
                        break;
                    }
                }
            } catch (Exception ignored) {}
        }
        if (win == 0) return;

        switch (action.getType()) {
            case KEY_PRESS -> {
                net.minecraft.client.input.KeyEvent ev = new net.minecraft.client.input.KeyEvent(action.getKeyCode(), 0, 0);
                ((com.eclipseware.imnotcheatingyouare.mixin.client.KeyboardHandlerAccessor) mc.keyboardHandler).invokeKeyPress(win, 1, ev);
            }
            case KEY_RELEASE -> {
                net.minecraft.client.input.KeyEvent ev = new net.minecraft.client.input.KeyEvent(action.getKeyCode(), 0, 0);
                ((com.eclipseware.imnotcheatingyouare.mixin.client.KeyboardHandlerAccessor) mc.keyboardHandler).invokeKeyPress(win, 0, ev);
            }
            case MOUSE_CLICK -> ((com.eclipseware.imnotcheatingyouare.mixin.client.MouseHandlerAccessor) mc.mouseHandler).invokeOnButton(win, new net.minecraft.client.input.MouseButtonInfo(action.getKeyCode(), 0), 1);
            case MOUSE_RELEASE -> ((com.eclipseware.imnotcheatingyouare.mixin.client.MouseHandlerAccessor) mc.mouseHandler).invokeOnButton(win, new net.minecraft.client.input.MouseButtonInfo(action.getKeyCode(), 0), 0);
        }
    }

    public static void tickKeybinds() {
        if (mc.screen != null || mc.player == null) return;
        long win = 0;
        try {
            for (java.lang.reflect.Field f : mc.getWindow().getClass().getDeclaredFields()) {
                if (f.getType() == long.class) {
                    f.setAccessible(true);
                    win = f.getLong(mc.getWindow());
                    break;
                }
            }
        } catch (Exception ignored) {}
        if (win == 0) return;

        for (Macro m : macros) {
            if (!m.isEnabled()) continue;
            int bind = m.getKeybind();
            if (bind == -1 || bind == 0) continue;

            boolean isPressed;
            if (bind >= 0 && bind <= 7) {
                isPressed = org.lwjgl.glfw.GLFW.glfwGetMouseButton(win, bind) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
            } else {
                isPressed = org.lwjgl.glfw.GLFW.glfwGetKey(win, bind) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
            }

            boolean wasPressed = macroKeyStates.getOrDefault(m, false);
            if (isPressed && !wasPressed) {
                if (m.isHoldMode()) {
                    startPlay(m);
                } else {
                    if (isPlaying && currentMacro == m) {
                        stopPlay();
                    } else {
                        startPlay(m);
                    }
                }
            } else if (!isPressed && wasPressed) {
                if (m.isHoldMode() && currentMacro == m) {
                    stopPlay();
                }
            }
            macroKeyStates.put(m, isPressed);
        }
    }

    private static class MacroContainer {
        List<Macro> macros = new ArrayList<>();
        String activeMacroName = "Default";
    }

    public static void save() {
        if (!DIR.exists()) DIR.mkdirs();
        try (FileWriter writer = new FileWriter(MACROS_FILE)) {
            MacroContainer container = new MacroContainer();
            container.macros = macros;
            container.activeMacroName = activeMacro != null ? activeMacro.getName() : "Default";
            GSON.toJson(container, writer);
        } catch (Exception e) {
            System.err.println("[EclipseWare] Failed to save macros: " + e.getMessage());
        }
    }

    public static void load() {
        if (!MACROS_FILE.exists()) {
            macros.clear();
            Macro def = new Macro("Default");
            macros.add(def);
            activeMacro = def;
            currentMacro = def;
            return;
        }
        try (FileReader reader = new FileReader(MACROS_FILE)) {
            JsonObject obj = JsonParser.parseReader(reader).getAsJsonObject();
            if (obj.has("macros")) {
                MacroContainer container = GSON.fromJson(obj, MacroContainer.class);
                if (container != null && container.macros != null && !container.macros.isEmpty()) {
                    macros.clear();
                    macros.addAll(container.macros);
                    activeMacro = null;
                    for (Macro m : macros) {
                        if (m.getName().equals(container.activeMacroName)) {
                            activeMacro = m;
                            break;
                        }
                    }
                    if (activeMacro == null) activeMacro = macros.get(0);
                    currentMacro = activeMacro;
                }
            } else {
                try (FileReader fallbackReader = new FileReader(MACROS_FILE)) {
                    Macro loaded = GSON.fromJson(fallbackReader, Macro.class);
                    if (loaded != null) {
                        macros.clear();
                        macros.add(loaded);
                        activeMacro = loaded;
                        currentMacro = loaded;
                    }
                }
            }
        } catch (Exception e) {
            macros.clear();
            Macro def = new Macro("Default");
            macros.add(def);
            activeMacro = def;
            currentMacro = def;
            System.err.println("[EclipseWare] Failed to load macros: " + e.getMessage());
        }
    }

    public static void exportToClipboard(Macro m) {
        if (m == null) return;
        try {
            String json = GSON.toJson(m);
            String encoded = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
            mc.keyboardHandler.setClipboard(encoded);
        } catch (Exception e) {
            System.err.println("[EclipseWare] Failed to export macro to clipboard: " + e.getMessage());
        }
    }

    public static void exportToClipboard() {
        exportToClipboard(getActiveMacro());
    }

    public static void importFromClipboard() {
        try {
            String clipboard = mc.keyboardHandler.getClipboard().trim();
            if (clipboard.isEmpty()) return;
            String decoded = new String(Base64.getDecoder().decode(clipboard), StandardCharsets.UTF_8);
            Macro loaded = GSON.fromJson(decoded, Macro.class);
            if (loaded != null) {
                // Ensure unique name
                String baseName = loaded.getName();
                String name = baseName;
                int count = 1;
                boolean exists = true;
                while (exists) {
                    exists = false;
                    for (Macro m : macros) {
                        if (m.getName().equalsIgnoreCase(name)) {
                            exists = true;
                            name = baseName + " (" + count + ")";
                            count++;
                            break;
                        }
                    }
                }
                loaded.setName(name);
                macros.add(loaded);
                activeMacro = loaded;
                currentMacro = loaded;
                save();
            }
        } catch (Exception e) {
            System.err.println("[EclipseWare] Failed to import macro from clipboard: " + e.getMessage());
        }
    }
}
