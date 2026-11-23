package com.eclipseware.imnotcheatingyouare.client.glassui.theme;

public final class UiThemeContext {
    private static volatile UiTheme current = new MutableUiTheme();

    private UiThemeContext() {
    }

    public static UiTheme current() {
        return current;
    }

    public static void setCurrent(UiTheme next) {
        if (next != null) {
            current = next;
        }
    }
}
