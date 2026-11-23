package com.eclipseware.imnotcheatingyouare.client.glassui.render;

public enum SatoshiWeight {
    MEDIUM("medium"),
    SEMIBOLD("semibold");

    private final String key;

    SatoshiWeight(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }
}
