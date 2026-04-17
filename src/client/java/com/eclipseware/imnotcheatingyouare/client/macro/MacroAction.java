package com.eclipseware.imnotcheatingyouare.client.macro;

public class MacroAction {
    public enum ActionType {
        KEY_PRESS, KEY_RELEASE, MOUSE_CLICK, MOUSE_RELEASE, DELAY
    }

    private final ActionType type;
    private final int keyCode;
    private final boolean pressed;
    private final long delayMs;

    public MacroAction(ActionType type, int keyCode, boolean pressed, long delayMs) {
        this.type = type;
        this.keyCode = keyCode;
        this.pressed = pressed;
        this.delayMs = delayMs;
    }

    public ActionType getType() { return type; }
    public int getKeyCode() { return keyCode; }
    public boolean isPressed() { return pressed; }
    public long getDelayMs() { return delayMs; }
}