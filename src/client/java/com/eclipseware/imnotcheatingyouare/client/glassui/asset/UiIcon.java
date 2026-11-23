package com.eclipseware.imnotcheatingyouare.client.glassui.asset;

import net.minecraft.resources.Identifier;

public interface UiIcon {
    Identifier texture();

    static UiIcon of(Identifier texture) {
        return new TextureIcon(texture);
    }

    static UiIcon of(String namespace, String path) {
        Identifier id = Identifier.fromNamespaceAndPath(namespace, path);
        return new TextureIcon(id);
    }
}
