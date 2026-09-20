package com.eclipseware.imnotcheatingyouare.client.utils;

import imgui.ImGui;
import xyz.breadloaf.imguimc.interfaces.Renderable;
import com.eclipseware.imnotcheatingyouare.client.render.ImGuiOverlayRenderable;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Initializer {
    public static final List<Renderable> renderstack = new CopyOnWriteArrayList<>();

    static {
        renderstack.add(new ImGuiOverlayRenderable());
    }

    public static void pushRenderable(Renderable renderable) {
        renderstack.add(renderable);
    }

    public static void pullEveryRenderable() {
        renderstack.clear();
    }

    public static int getDockId() {
        return ImGui.getID("imgui-mc dockspace");
    }
}
