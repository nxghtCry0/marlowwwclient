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
import java.util.Base64;
import java.util.List;

public class MacroManager {
    private static final Minecraft mc = Minecraft.getInstance();
    private static final File DIR = new File(mc.gameDirectory, "config/imnotcheatingyouare");
    private static final File MACROS_FILE = new File(DIR, "macros.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Macro currentMacro = new Macro("Default");
    private static boolean isRecording = false;
    private static boolean isPlaying = false;
    private static long lastActionTime = 0;

    private static int playIndex = 0;
    private static long nextActionTime = 0;
    private static long windowHandle = 0;

    public static Macro getCurrentMacro() {
        return currentMacro;
    }

    public static boolean isRecording() {
        return isRecording;
    }

    public static boolean isPlaying() {
        return isPlaying;
    }

    public static void startRecord() {
        if (isRecording) return;
        currentMacro.clear();
        isRecording = true;
        isPlaying = false;
        lastActionTime = System.currentTimeMillis();
    }

    public static void stopRecord() {
        if (!isRecording) return;
        isRecording = false;
        save();
    }

    public static void startPlay() {
        if (isPlaying) return;
        if (currentMacro.getActions().isEmpty()) return;
        isPlaying = true;
        isRecording = false;
        playIndex = 0;
        nextActionTime = System.currentTimeMillis();
    }

    public static void stopPlay() {
        isPlaying = false;
    }

    public static void recordKey(int key, boolean pressed, long window) {
        if (!isRecording) return;
        windowHandle = window;
        long current = System.currentTimeMillis();
        long delay = current - lastActionTime;
        if (delay > 0) {
            currentMacro.addAction(new MacroAction(MacroAction.ActionType.DELAY, 0, false, delay));
        }
        MacroAction.ActionType type = pressed ? MacroAction.ActionType.KEY_PRESS : MacroAction.ActionType.KEY_RELEASE;
        currentMacro.addAction(new MacroAction(type, key, pressed, 0));
        lastActionTime = current;
    }

    public static void recordMouse(int button, boolean pressed, long window) {
        if (!isRecording) return;
        windowHandle = window;
        long current = System.currentTimeMillis();
        long delay = current - lastActionTime;
        if (delay > 0) {
            currentMacro.addAction(new MacroAction(MacroAction.ActionType.DELAY, 0, false, delay));
        }
        MacroAction.ActionType type = pressed ? MacroAction.ActionType.MOUSE_CLICK : MacroAction.ActionType.MOUSE_RELEASE;
        currentMacro.addAction(new MacroAction(type, button, pressed, 0));
        lastActionTime = current;
    }

    public static void tick() {
        if (!isPlaying) return;
        if (mc.level == null || mc.player == null) {
            stopPlay();
            return;
        }

        List<MacroAction> actions = currentMacro.getActions();
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

    public static void save() {
        if (!DIR.exists()) DIR.mkdirs();
        try (FileWriter writer = new FileWriter(MACROS_FILE)) {
            GSON.toJson(currentMacro, writer);
        } catch (Exception e) {
            System.err.println("[EclipseWare] Failed to save macro: " + e.getMessage());
        }
    }

    public static void load() {
        if (!MACROS_FILE.exists()) return;
        try (FileReader reader = new FileReader(MACROS_FILE)) {
            Macro loaded = GSON.fromJson(reader, Macro.class);
            if (loaded != null) {
                currentMacro = loaded;
            }
        } catch (Exception e) {
            System.err.println("[EclipseWare] Failed to load macro: " + e.getMessage());
        }
    }

    public static void exportToClipboard() {
        try {
            String json = GSON.toJson(currentMacro);
            String encoded = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
            mc.keyboardHandler.setClipboard(encoded);
        } catch (Exception e) {
            System.err.println("[EclipseWare] Failed to export macro to clipboard: " + e.getMessage());
        }
    }

    public static void importFromClipboard() {
        try {
            String clipboard = mc.keyboardHandler.getClipboard().trim();
            if (clipboard.isEmpty()) return;
            String decoded = new String(Base64.getDecoder().decode(clipboard), StandardCharsets.UTF_8);
            Macro loaded = GSON.fromJson(decoded, Macro.class);
            if (loaded != null) {
                currentMacro = loaded;
                save();
            }
        } catch (Exception e) {
            System.err.println("[EclipseWare] Failed to import macro from clipboard: " + e.getMessage());
        }
    }
}
