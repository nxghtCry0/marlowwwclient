package com.eclipseware.imnotcheatingyouare.client.utils;

import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.sdl.SDLKeyboard;
import org.lwjgl.sdl.SDLMouse;

/**
 * Keyboard/mouse polling for Minecraft 26.3+, which replaced its GLFW window with an SDL one.
 * Keyboard codes are {@link InputConstants} values (SDL scancodes). Mouse buttons are encoded
 * as negative ints, one more than Minecraft's own click-event button ordinal (0 = left, 1 =
 * right, 2 = middle, 3/4 = side buttons): -1 = left, -2 = right, -3 = middle, -4/-5 = side buttons.
 */
public final class InputUtil {
    private InputUtil() {}

    public static final int MOUSE_LEFT = -1;
    public static final int MOUSE_RIGHT = -2;
    public static final int MOUSE_MIDDLE = -3;
    public static final int MOUSE_BUTTON_4 = -4;
    public static final int MOUSE_BUTTON_5 = -5;

    /** Convert Minecraft's click-event button ordinal (0=left,1=right,2=middle,3+=extra) to our stored code. */
    public static int fromClickOrdinal(int ordinal) {
        return -(ordinal + 1);
    }

    /**
     * Minecraft 26.3's {@code MouseButtonEvent.button()} / {@code MouseButtonInfo.button()} now
     * return the raw SDL button number (1=left, 2=middle, 3=right, 4/5=side buttons) instead of
     * the old GLFW-derived 0-indexed convention this codebase was written against
     * (0=left, 1=right, 2=middle, 3/4=side buttons). Call this immediately after reading
     * {@code event.button()} in any click/release handler so every existing {@code == 0}
     * (left) / {@code == 1} (right) / {@code == 2} (middle) comparison keeps working unchanged.
     */
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

    public static boolean isMouseBind(int code) {
        return code < 0;
    }

    private static int sdlButtonFor(int mouseCode) {
        int ordinal = -mouseCode - 1;
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
        if (code == 0) return false;
        if (isMouseBind(code)) {
            return isMouseButtonDown(code);
        }
        return InputConstants.isKeyDown(code);
    }

    public static boolean isMouseButtonDown(int mouseCode) {
        int sdlButton = sdlButtonFor(mouseCode);
        if (sdlButton < 0) return false;
        int mask = SDLMouse.SDL_GetMouseState(null, null);
        return (mask & (1 << (sdlButton - 1))) != 0;
    }

    /** Human-readable name for a keybind code, mouse or keyboard. */
    public static String getName(int code) {
        if (code == 0) return "NONE";
        if (isMouseBind(code)) {
            return switch (code) {
                case MOUSE_LEFT -> "MOUSE LEFT";
                case MOUSE_MIDDLE -> "MOUSE MIDDLE";
                case MOUSE_RIGHT -> "MOUSE RIGHT";
                case MOUSE_BUTTON_4 -> "MOUSE 4";
                case MOUSE_BUTTON_5 -> "MOUSE 5";
                default -> "MOUSE ?";
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
            if (name != null && !name.isEmpty()) return name.toUpperCase();
        } catch (Throwable ignored) {}
        return "KEY " + code;
    }
}
