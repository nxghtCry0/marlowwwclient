package com.eclipseware.imnotcheatingyouare.client.glassui.theme;

public enum AccentPreset {
    OCEAN_SKY,
    EMERALD,
    ROSE_PINK,
    CUSTOM;

    public static AccentPreset fromIndex(int index) {
        AccentPreset[] vals = values();
        if (index < 0 || index >= vals.length) {
            return OCEAN_SKY;
        }
        return vals[index];
    }
}
