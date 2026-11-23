package com.eclipseware.imnotcheatingyouare.client.glassui.bind;

import com.eclipseware.imnotcheatingyouare.client.utils.InputUtil;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.network.chat.Component;

public record UiInputBind(boolean isMouse, int code) {
    public static final UiInputBind NONE = new UiInputBind(false, -1);

    public static UiInputBind key(int keyCode) {
        if (keyCode <= 0) {
            return NONE;
        }
        return new UiInputBind(false, keyCode);
    }

    public static UiInputBind mouse(int button) {
        if (button <= 0) {
            return NONE;
        }
        return new UiInputBind(true, button);
    }

    public static UiInputBind fromModuleKeyBind(int rawBind) {
        if (rawBind == 0) {
            return NONE;
        }
        if (rawBind < 0) {
            int legacyOrdinal = InputUtil.toGlfwMouseButton(rawBind);
            if (legacyOrdinal < 0) {
                return NONE;
            }
            int sdlMouseButton = switch (legacyOrdinal) {
                case 0 -> InputConstants.MOUSE_BUTTON_LEFT;
                case 1 -> InputConstants.MOUSE_BUTTON_RIGHT;
                case 2 -> InputConstants.MOUSE_BUTTON_MIDDLE;
                case 3 -> InputConstants.MOUSE_BUTTON_4;
                case 4 -> InputConstants.MOUSE_BUTTON_5;
                default -> legacyOrdinal + 1;
            };
            return mouse(sdlMouseButton);
        }
        return key(rawBind);
    }

    public int toModuleKeyBind() {
        if (!isBound()) {
            return 0;
        }
        if (isMouse) {
            int legacyOrdinal = InputUtil.toLegacyOrdinal(code);
            return InputUtil.fromClickOrdinal(legacyOrdinal);
        }
        return code;
    }

    public boolean isBound() {
        return code > 0;
    }

    public String formatLabel() {
        if (!isBound()) {
            return "Bind";
        }
        if (isMouse) {
            return switch (code) {
                case InputConstants.MOUSE_BUTTON_LEFT -> "MB1";
                case InputConstants.MOUSE_BUTTON_RIGHT -> "MB2";
                case InputConstants.MOUSE_BUTTON_MIDDLE -> "MB3";
                case InputConstants.MOUSE_BUTTON_4 -> "MB4";
                case InputConstants.MOUSE_BUTTON_5 -> "MB5";
                case InputConstants.MOUSE_BUTTON_6 -> "MB6";
                case InputConstants.MOUSE_BUTTON_7 -> "MB7";
                case InputConstants.MOUSE_BUTTON_8 -> "MB8";
                default -> "MB" + code;
            };
        }
        return switch (code) {
            case InputConstants.KEY_LSHIFT -> "LSHF";
            case InputConstants.KEY_RSHIFT -> "RSHF";
            case InputConstants.KEY_LCONTROL -> "LCTL";
            case InputConstants.KEY_RCONTROL -> "RCTL";
            case InputConstants.KEY_LALT -> "LALT";
            case InputConstants.KEY_RALT -> "RALT";
            case InputConstants.KEY_CAPSLOCK -> "CAPS";
            case InputConstants.KEY_TAB -> "TAB";
            case InputConstants.KEY_SPACE -> "SPC";
            case InputConstants.KEY_UP -> "UP";
            case InputConstants.KEY_DOWN -> "DOWN";
            case InputConstants.KEY_LEFT -> "LEFT";
            case InputConstants.KEY_RIGHT -> "RGHT";
            default -> resolveKeyShortName(code);
        };
    }

    private static String resolveKeyShortName(int keyCode) {
        try {
            InputConstants.Type keyboardType = InputConstants.Type.KEYBOARD;
            InputConstants.Key keyObj = keyboardType.getOrCreate(keyCode);
            Component display = keyObj.getDisplayName();
            String raw = display.getString();
            String trimmed = raw.trim();
            if (!trimmed.isEmpty()) {
                String upper = trimmed.toUpperCase();
                return upper.length() <= 5 ? upper : upper.substring(0, 5);
            }
        } catch (Throwable ignored) {
        }
        String fallback = InputUtil.getName(keyCode);
        if (fallback.isEmpty() || "NONE".equals(fallback)) {
            return "K" + keyCode;
        }
        return fallback.length() <= 5 ? fallback : fallback.substring(0, 5);
    }
}
