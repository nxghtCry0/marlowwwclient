package com.eclipseware.imnotcheatingyouare.client.utils;

import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.sdl.SDLKeyboard;
import org.lwjgl.sdl.SDLMouse;

public final class InputUtil {
    private InputUtil() {}

    public static final int MOUSE_LEFT = -1;
    public static final int MOUSE_RIGHT = -2;
    public static final int MOUSE_MIDDLE = -3;
    public static final int MOUSE_BUTTON_4 = -4;
    public static final int MOUSE_BUTTON_5 = -5;

    public static int fromClickOrdinal(int ordinal) {
        return -(ordinal + 1);
    }

    public static int toLegacyOrdinal(int sdlButton) {
        return switch (sdlButton) {
            case 1 -> 0;
            case 3 -> 1;
            case 2 -> 2;
            case 4 -> 3;
            case 5 -> 4;
            default -> sdlButton;
        };
    }

    public static int toGlfwMouseButton(int mouseCode) {
        if (mouseCode <= -100) {
            return -100 - mouseCode;
        }
        if (mouseCode < 0) {
            return -mouseCode - 1;
        }
        return -1;
    }

    public static boolean isMouseBind(int code) {
        return code < 0;
    }

    private static int sdlButtonFor(int mouseCode) {
        int ordinal = toGlfwMouseButton(mouseCode);
        return switch (ordinal) {
            case 0 -> SDLMouse.SDL_BUTTON_LEFT;
            case 1 -> SDLMouse.SDL_BUTTON_RIGHT;
            case 2 -> SDLMouse.SDL_BUTTON_MIDDLE;
            case 3 -> SDLMouse.SDL_BUTTON_X1;
            case 4 -> SDLMouse.SDL_BUTTON_X2;
            default -> -1;
        };
    }

    public static boolean isDown(int code) {
        if (code == 0) {
            return false;
        }
        if (isMouseBind(code)) {
            return isMouseButtonDown(code);
        }
        return InputConstants.isKeyDown(code);
    }

    public static boolean isMouseButtonDown(int mouseCode) {
        int sdlButton = sdlButtonFor(mouseCode);
        if (sdlButton < 0) {
            return false;
        }
        int mask = SDLMouse.SDL_GetMouseState(null, null);
        return (mask & (1 << (sdlButton - 1))) != 0;
    }

    public static String getName(int code) {
        if (code == 0) {
            return "NONE";
        }
        if (isMouseBind(code)) {
            int ordinal = toGlfwMouseButton(code);
            return switch (ordinal) {
                case 0 -> "MOUSE LEFT";
                case 1 -> "MOUSE RIGHT";
                case 2 -> "MOUSE MIDDLE";
                case 3 -> "MOUSE 4";
                case 4 -> "MOUSE 5";
                default -> ordinal >= 0 ? ("MOUSE " + (ordinal + 1)) : "MOUSE ?";
            };
        }

        switch (code) {
            case InputConstants.KEY_RSHIFT: return "RSHIFT";
            case InputConstants.KEY_LSHIFT: return "LSHIFT";
            case InputConstants.KEY_RCONTROL: return "RCTRL";
            case InputConstants.KEY_LCONTROL: return "LCTRL";
            case InputConstants.KEY_RALT: return "RALT";
            case InputConstants.KEY_LALT: return "LALT";
            case InputConstants.KEY_TAB: return "TAB";
            case InputConstants.KEY_SPACE: return "SPACE";
            case InputConstants.KEY_RETURN: return "ENTER";
            case InputConstants.KEY_ESCAPE: return "NONE";
            default: break;
        }

        try {
            String name = SDLKeyboard.SDL_GetScancodeName(code);
            if (name != null && !name.isEmpty()) {
                return name.toUpperCase();
            }
        } catch (Throwable ignored) {
        }
        return "KEY " + code;
    }
}
