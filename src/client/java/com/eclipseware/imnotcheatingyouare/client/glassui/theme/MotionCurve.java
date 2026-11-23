package com.eclipseware.imnotcheatingyouare.client.glassui.theme;

public enum MotionCurve {
    LINEAR,
    SPRING,
    EASE,
    SNAPPY;

    public static MotionCurve fromIndex(int index) {
        MotionCurve[] vals = values();
        if (index < 0 || index >= vals.length) {
            return SPRING;
        }
        return vals[index];
    }
}
