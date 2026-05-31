package com.eclipseware.imnotcheatingyouare.client.utils;

import imgui.ImGui;
import imgui.glfw.ImGuiImplGlfw;
import imgui.gl3.ImGuiImplGl3;
import net.minecraft.client.Minecraft;

public class ImGuiManager {
    private static ImGuiImplGlfw implGlfw;
    private static ImGuiImplGl3 implGl3;
    private static boolean initialized = false;

    public static void init() {
        if (initialized) return;

        long windowHandle = 0;
        try {
            for (java.lang.reflect.Field f : Minecraft.getInstance().getWindow().getClass().getDeclaredFields()) {
                if (f.getType() == long.class) {
                    f.setAccessible(true);
                    windowHandle = f.getLong(Minecraft.getInstance().getWindow());
                    break;
                }
            }
        } catch (Exception e) {}

        if (windowHandle == 0) return;

        ImGui.createContext();
        implGlfw = new ImGuiImplGlfw();
        implGl3 = new ImGuiImplGl3();

        implGlfw.init(windowHandle, true);
        implGl3.init("#version 150");

        initialized = true;
    }

    public static ImGuiImplGlfw getImplGlfw() {
        return implGlfw;
    }

    public static ImGuiImplGl3 getImplGl3() {
        return implGl3;
    }

    public static boolean isInitialized() {
        return initialized;
    }
}
