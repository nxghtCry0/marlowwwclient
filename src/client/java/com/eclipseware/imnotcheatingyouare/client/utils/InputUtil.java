package com.eclipseware.imnotcheatingyouare.client.utils;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

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

    public static boolean isDown(int code) {
        if (code == 0) {
            return false;
        }
        if (isMouseBind(code)) {
            return isMouseButtonDown(code);
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getWindow() == null) {
            return false;
        }
        return InputConstants.isKeyDown(mc.getWindow(), code);
    }

    public static boolean isMouseButtonDown(int mouseCode) {
        int ordinal = toGlfwMouseButton(mouseCode);
        if (ordinal < 0 || ordinal > GLFW.GLFW_MOUSE_BUTTON_LAST) {
            return false;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getWindow() == null) {
            return false;
        }
        Window win = mc.getWindow();
        return GLFW.glfwGetMouseButton(win.handle(), ordinal) == GLFW.GLFW_PRESS;
    }

    public static String getName(int code) {
        if (code == 0) {
            return "NONE";
        }
        if (isMouseBind(code)) {
            int ordinal = toGlfwMouseButton(code);
            return switch (ordinal) {
                case GLFW.GLFW_MOUSE_BUTTON_LEFT -> "MOUSE LEFT";
                case GLFW.GLFW_MOUSE_BUTTON_RIGHT -> "MOUSE RIGHT";
                case GLFW.GLFW_MOUSE_BUTTON_MIDDLE -> "MOUSE MIDDLE";
                case GLFW.GLFW_MOUSE_BUTTON_4 -> "MOUSE 4";
                case GLFW.GLFW_MOUSE_BUTTON_5 -> "MOUSE 5";
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
            InputConstants.Type keyType = InputConstants.Type.KEYSYM;
            InputConstants.Key keySym = keyType.getOrCreate(code);
            String name = keySym.getDisplayName().getString();
            if (name != null && !name.isEmpty()) {
                return name.toUpperCase();
            }
        } catch (Throwable ignored) {
        }
        return "KEY " + code;
    }
}
