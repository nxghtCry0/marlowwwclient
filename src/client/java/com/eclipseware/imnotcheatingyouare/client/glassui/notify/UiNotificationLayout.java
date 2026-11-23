package com.eclipseware.imnotcheatingyouare.client.glassui.notify;

public enum UiNotificationLayout {
    BOTTOM_RIGHT,
    TOP_RIGHT,
    CENTER,
    COMPACT;

    public static UiNotificationLayout fromIndex(int index) {
        UiNotificationLayout[] vals = values();
        if (index < 0 || index >= vals.length) {
            return BOTTOM_RIGHT;
        }
        return vals[index];
    }
}
