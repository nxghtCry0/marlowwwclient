package com.eclipseware.imnotcheatingyouare.client.glassui.asset;

import net.minecraft.resources.Identifier;

public final class UiAssets {
    private static volatile String namespace = "imnotcheatingyouare";

    private UiAssets() {
    }

    public static String getNamespace() {
        return namespace;
    }

    public static void setNamespace(String nextNamespace) {
        if (nextNamespace != null && !nextNamespace.isEmpty()) {
            namespace = nextNamespace;
        }
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(namespace, path);
    }

    public static Identifier fontAtlasTexture() {
        return id("textures/gui/font/satoshi_atlas.png");
    }

    public static String fontAtlasClasspath() {
        return "/assets/" + namespace + "/textures/gui/font/satoshi_atlas.png";
    }

    public static String fontMetricsClasspath() {
        return "/assets/" + namespace + "/gui_data/satoshi_metrics.json";
    }

    public static Identifier iconTexture(String fileName) {
        return id("textures/gui/icons/" + fileName + ".png");
    }
}
