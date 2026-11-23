package com.eclipseware.imnotcheatingyouare.client.glassui.render;

public enum StreamproofMode {
    OVERLAY,
    WINDOW,
    CRISP,
    IN_GAME;

    public static StreamproofMode fromIndex(int index) {
        StreamproofMode[] vals = values();
        if (index < 0 || index >= vals.length) {
            return OVERLAY;
        }
        return vals[index];
    }
}
