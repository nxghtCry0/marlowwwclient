package xyz.breadloaf.imguimc.imgui;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.flag.ImGuiKey;
import net.minecraft.client.Minecraft;
import org.lwjgl.sdl.SDLMouse;

/**
 * Minimal replacement for imgui-java's {@code ImGuiImplGlfw}, since Minecraft 26.3 replaced its
 * GLFW window with an SDL one and imgui-java only ships a GLFW platform backend. This polls
 * Minecraft's own SDL-backed input state each frame instead of hooking native SDL events, which
 * is enough to drive mouse/keyboard interaction with ImGui widgets (checkboxes, buttons, sliders)
 * but does not support IME/text composition.
 */
public final class SdlImGuiPlatform {
    private long lastFrameNanos = 0L;
    private static volatile float pendingScrollY = 0f;

    public void init(long windowHandle) {
        ImGuiIO io = ImGui.getIO();

        io.setKeyMap(ImGuiKey.Tab, InputConstants.KEY_TAB);
        io.setKeyMap(ImGuiKey.LeftArrow, InputConstants.KEY_LEFT);
        io.setKeyMap(ImGuiKey.RightArrow, InputConstants.KEY_RIGHT);
        io.setKeyMap(ImGuiKey.UpArrow, InputConstants.KEY_UP);
        io.setKeyMap(ImGuiKey.DownArrow, InputConstants.KEY_DOWN);
        io.setKeyMap(ImGuiKey.PageUp, InputConstants.KEY_PAGEUP);
        io.setKeyMap(ImGuiKey.PageDown, InputConstants.KEY_PAGEDOWN);
        io.setKeyMap(ImGuiKey.Home, InputConstants.KEY_HOME);
        io.setKeyMap(ImGuiKey.End, InputConstants.KEY_END);
        io.setKeyMap(ImGuiKey.Insert, InputConstants.KEY_INSERT);
        io.setKeyMap(ImGuiKey.Delete, InputConstants.KEY_DELETE);
        io.setKeyMap(ImGuiKey.Backspace, InputConstants.KEY_BACKSPACE);
        io.setKeyMap(ImGuiKey.Space, InputConstants.KEY_SPACE);
        io.setKeyMap(ImGuiKey.Enter, InputConstants.KEY_RETURN);
        io.setKeyMap(ImGuiKey.Escape, InputConstants.KEY_ESCAPE);
        io.setKeyMap(ImGuiKey.KeyPadEnter, InputConstants.KEY_NUMPADENTER);
        io.setKeyMap(ImGuiKey.A, InputConstants.KEY_A);
        io.setKeyMap(ImGuiKey.C, InputConstants.KEY_C);
        io.setKeyMap(ImGuiKey.V, InputConstants.KEY_V);
        io.setKeyMap(ImGuiKey.X, InputConstants.KEY_X);
        io.setKeyMap(ImGuiKey.Y, InputConstants.KEY_Y);
        io.setKeyMap(ImGuiKey.Z, InputConstants.KEY_Z);
    }

    public void newFrame() {
        ImGuiIO io = ImGui.getIO();
        Minecraft mc = Minecraft.getInstance();
        Window window = mc.getWindow();

        int guiScaledWidth = window.getGuiScaledWidth();
        int guiScaledHeight = window.getGuiScaledHeight();
        io.setDisplaySize(guiScaledWidth, guiScaledHeight);

        Window.FramebufferSize framebufferSize = window.queryFramebufferSize();
        float scaleX = guiScaledWidth > 0 ? framebufferSize.width() / (float) guiScaledWidth : 1f;
        float scaleY = guiScaledHeight > 0 ? framebufferSize.height() / (float) guiScaledHeight : 1f;
        io.setDisplayFramebufferScale(scaleX, scaleY);

        long now = System.nanoTime();
        float delta = (lastFrameNanos == 0L) ? (1f / 60f) : (now - lastFrameNanos) / 1_000_000_000f;
        io.setDeltaTime(Math.max(delta, 1f / 1000f));
        lastFrameNanos = now;

        io.setMousePos((float) mc.mouseHandler.getScaledXPos(window), (float) mc.mouseHandler.getScaledYPos(window));

        int mask = SDLMouse.SDL_GetMouseState(null, null);
        io.setMouseDown(0, (mask & (1 << (SDLMouse.SDL_BUTTON_LEFT - 1))) != 0);
        io.setMouseDown(1, (mask & (1 << (SDLMouse.SDL_BUTTON_RIGHT - 1))) != 0);
        io.setMouseDown(2, (mask & (1 << (SDLMouse.SDL_BUTTON_MIDDLE - 1))) != 0);
        io.setMouseDown(3, (mask & (1 << (SDLMouse.SDL_BUTTON_X1 - 1))) != 0);
        io.setMouseDown(4, (mask & (1 << (SDLMouse.SDL_BUTTON_X2 - 1))) != 0);

        io.setKeyCtrl(InputConstants.isKeyDown(InputConstants.KEY_LCONTROL) || InputConstants.isKeyDown(InputConstants.KEY_RCONTROL));
        io.setKeyShift(InputConstants.isKeyDown(InputConstants.KEY_LSHIFT) || InputConstants.isKeyDown(InputConstants.KEY_RSHIFT));
        io.setKeyAlt(InputConstants.isKeyDown(InputConstants.KEY_LALT) || InputConstants.isKeyDown(InputConstants.KEY_RALT));
        io.setKeySuper(InputConstants.isKeyDown(InputConstants.KEY_LGUI) || InputConstants.isKeyDown(InputConstants.KEY_RGUI));

        for (int key : TRACKED_KEYS) {
            io.setKeysDown(key, InputConstants.isKeyDown(key));
        }

        io.setMouseWheel(pollAndResetScroll());
    }

    private static final int[] TRACKED_KEYS = {
            InputConstants.KEY_TAB, InputConstants.KEY_LEFT, InputConstants.KEY_RIGHT, InputConstants.KEY_UP,
            InputConstants.KEY_DOWN, InputConstants.KEY_PAGEUP, InputConstants.KEY_PAGEDOWN, InputConstants.KEY_HOME,
            InputConstants.KEY_END, InputConstants.KEY_INSERT, InputConstants.KEY_DELETE, InputConstants.KEY_BACKSPACE,
            InputConstants.KEY_SPACE, InputConstants.KEY_RETURN, InputConstants.KEY_ESCAPE, InputConstants.KEY_NUMPADENTER,
            InputConstants.KEY_A, InputConstants.KEY_C, InputConstants.KEY_V, InputConstants.KEY_X, InputConstants.KEY_Y,
            InputConstants.KEY_Z,
    };

    /** Called from a mixin on {@code MouseHandler.onScroll} since SDL's wheel state is delta-only, not polled. */
    public static void feedScroll(double verticalAmount) {
        pendingScrollY += (float) verticalAmount;
    }

    private static float pollAndResetScroll() {
        float s = pendingScrollY;
        pendingScrollY = 0f;
        return s;
    }

    public void dispose() {
    }
}
