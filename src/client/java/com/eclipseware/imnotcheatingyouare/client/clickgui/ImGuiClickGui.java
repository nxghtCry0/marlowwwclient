package com.eclipseware.imnotcheatingyouare.client.clickgui;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.utils.RenderUtils;
import com.eclipseware.imnotcheatingyouare.client.utils.InputUtil;
import imgui.ImDrawList;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImDrawFlags;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiStyleVar;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImString;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.Identifier;
import xyz.breadloaf.imguimc.screen.EmptyScreen;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ImGuiClickGui {
    private static final ImGui IMGUI = new ImGui();

    private static boolean open = false;
    private static Category selectedCategory = Category.Combat;
    private static Module settingsModule = null;
    private static Module settingsAnimModule = null;
    private static long settingsOpenNanos = 0L;
    private static Module bindingModule = null;
    private static boolean bindingArmed = false;
    private static final ImString searchBuffer = new ImString(64);
    private static final Map<String, Float> toggleAnim = new HashMap<>();
    private static final Map<String, Float> scrollTarget = new HashMap<>();
    private static final Map<String, Float> scrollApplied = new HashMap<>();

    private static float windowX = 30f;
    private static float windowY = 30f;
    private static float windowW = 360f;
    private static float windowH = 250f;
    private static float lastDisplayW = -1f;
    private static float lastDisplayH = -1f;

    private static final Category[] CATEGORY_ORDER = new Category[]{
            Category.Combat, Category.Crystal, Category.Mace, Category.CartPvP, Category.UHC,
            Category.Movement, Category.Render, Category.HUD, Category.World,
            Category.Exploit, Category.Utility, Category.Macros, Category.Filters,
            Category.Client, Category.Configs, Category.Misc, Category.Blatant, Category.Farming
    };

    public static boolean isOpen() {
        return open;
    }

    private static long openedNanos;
    private static long categoryChangedNanos;
    private static final java.util.Map<String, Float> hoverAnim = new java.util.HashMap<>();

    private static float easeOut(float t) {
        t = clamp(t, 0f, 1f);
        return 1f - (1f - t) * (1f - t) * (1f - t);
    }

    private static float secondsSince(long nanos) {
        return (float) ((System.nanoTime() - nanos) / 1_000_000_000.0);
    }

    private static float animateHover(String key, boolean hovered) {
        float current = hoverAnim.getOrDefault(key, 0f);
        current += ((hovered ? 1f : 0f) - current) * Math.min(1f, ImGui.getIO().getDeltaTime() * 14f);
        hoverAnim.put(key, current);
        return current;
    }

    private static boolean menuMode = false;

    public static boolean isMenuMode() {
        return menuMode;
    }

    public static void openMenuMode() {
        if (!open) openedNanos = System.nanoTime();
        menuMode = true;
        open = true;
    }

    public static void setOpen(boolean value) {
        if (!value && menuMode) {
            Minecraft mc = Minecraft.getInstance();
            open = false;
            menuMode = false;
            settingsModule = null;
            if (mc.gui != null && mc.screen instanceof com.eclipseware.imnotcheatingyouare.client.gui.MenuConfigScreen screen) {
                mc.setScreenAndShow(screen.parent());
            }
            return;
        }
        if (value && !open) openedNanos = System.nanoTime();
        open = value;
        if (!value) settingsModule = null;

        Minecraft mc = Minecraft.getInstance();
        if (mc.gui == null) return;

        if (value) {
            if (!(mc.screen instanceof EmptyScreen)) {
                mc.setScreenAndShow(new EmptyScreen());
            }
        } else if (mc.screen instanceof EmptyScreen) {
            mc.setScreenAndShow((Screen) null);
        }
    }

    public static void toggle() {
        setOpen(!open);
    }

    public static void markClosed() {
        menuMode = false;
        open = false;
        settingsModule = null;
        bindingModule = null;
    }

    public static boolean isBinding() {
        return bindingModule != null;
    }

    public static void handleBindingKey(int key) {
        if (bindingModule == null) return;
        int code = key;
        if (key == com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE
                || key == com.mojang.blaze3d.platform.InputConstants.KEY_DELETE
                || key == com.mojang.blaze3d.platform.InputConstants.KEY_BACKSPACE) {
            code = 0;
        }
        bindingModule.setKeyBind(code);
        com.eclipseware.imnotcheatingyouare.client.setting.ConfigManager.save();
        bindingModule = null;
    }

    public static boolean isCapturingMouseBind() {
        return bindingModule != null && bindingArmed;
    }

    private static final int[] BIND_MOUSE_BUTTONS = {InputUtil.MOUSE_LEFT, InputUtil.MOUSE_RIGHT, InputUtil.MOUSE_MIDDLE,
            InputUtil.MOUSE_BUTTON_4, InputUtil.MOUSE_BUTTON_5};

    private static void pollBindingMouse() {
        if (bindingModule == null) return;

        // binding can be started with any button, so wait for all of them to be released
        if (!bindingArmed) {
            for (int mb : BIND_MOUSE_BUTTONS) {
                if (InputUtil.isMouseButtonDown(mb)) return;
            }
            bindingArmed = true;
            return;
        }

        for (int mb : BIND_MOUSE_BUTTONS) {
            if (InputUtil.isMouseButtonDown(mb)) {
                bindingModule.setKeyBind(mb);
                com.eclipseware.imnotcheatingyouare.client.setting.ConfigManager.save();
                bindingModule = null;
                return;
            }
        }
    }

    /**
     * Called every client tick. Our EmptyScreen owns the menu's lifetime: if anything swapped the
     * screen away from it, the menu is gone, so drop our own open flag to match. Re-showing the
     * screen here instead would allocate and {@code init} a fresh screen every tick whenever the
     * two disagree, which thrashes the mouse grab and swallows key and click events.
     */
    public static void tick() {
        if (!open) return;
        Minecraft mc = Minecraft.getInstance();
        if (menuMode) {
            if (mc.gui == null || !(mc.screen instanceof com.eclipseware.imnotcheatingyouare.client.gui.MenuConfigScreen)) {
                markClosed();
            }
            return;
        }
        if (mc.player == null || mc.gui == null) {
            markClosed();
            return;
        }
        if (!(mc.screen instanceof EmptyScreen)) {
            markClosed();
        }
    }

    private static final Color BG = new Color(16, 13, 24, 250);
    private static final Color SIDEBAR_BG = new Color(12, 10, 19, 250);
    private static final Color ROW_BG = new Color(26, 21, 38, 255);
    private static final Color ROW_HOVER = new Color(37, 29, 54, 255);
    private static final Color TEXT = new Color(232, 228, 242);
    private static final Color TEXT_DIM = new Color(146, 138, 168);
    private static final Color TOGGLE_OFF = new Color(54, 46, 74);

    private static final float FONT_SCALE = 0.9f;
    private static final float TITLE_H = 32f;
    private static final float SIDEBAR_W = 150f;
    private static final float SETTINGS_W = 280f;
    private static final float BODY_BOTTOM_MARGIN = 8f;
    private static final float WINDOW_ROUNDING = 12f;

    private static final Identifier LOGO_ID = Identifier.parse("imnotcheatingyouare:textures/logo.png");
    private static final int LOGO_NATIVE_WIDTH = 916;
    private static final int LOGO_NATIVE_HEIGHT = 272;

    /**
     * Renders the client wordmark into the menu's title-bar strip using the exact same
     * {@code guiGraphics.blit} call ArrayListHud uses for it. Must run during the vanilla HUD
     * render pass (before ImGui composites on top), since ImGui draws last in the frame and would
     * otherwise cover anything drawn here. The click-GUI itself leaves that strip transparent so
     * this shows through.
     */
    public static void render() {
        if (!open) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null && !menuMode) {
            open = false;
            settingsModule = null;
            return;
        }

        ImGui.getStyle().setAlpha(1.0f);
        ImGui.getStyle().setCircleTessellationMaxError(0.12f);
        ImGui.getStyle().setAntiAliasedFill(true);
        ImGui.getStyle().setAntiAliasedLines(true);
        pollBindingMouse();
        shaderTexture = com.eclipseware.imnotcheatingyouare.client.gui.PastelShaderBackground.render()
                ? com.eclipseware.imnotcheatingyouare.client.gui.PastelShaderBackground.glTextureId() : 0;
        logoTexture = resolveLogoTexture();
        if (shaderTexture != 0) com.eclipseware.imnotcheatingyouare.client.utils.ImGuiTextures.prepare(shaderTexture, true);

        float displayW = ImGui.getIO().getDisplaySizeX();
        float displayH = ImGui.getIO().getDisplaySizeY();
        if (displayW < 64f || displayH < 64f) return;
        if (Math.abs(displayW - lastDisplayW) > 1f || Math.abs(displayH - lastDisplayH) > 1f) {
            recomputeLayout(displayW, displayH);
        }
        clampPosition(displayW, displayH);

        Color accent = RenderUtils.getThemeAccentColor();
        int accentCol = RenderUtils.toImGuiColor(accent, 1.0f);
        int accentDim = RenderUtils.toImGuiColor(accent, 0.30f);
        int accentFaint = RenderUtils.toImGuiColor(accent, 0.13f);

        int pushedVars = pushStyleVars();
        int pushedColors = pushStyleColors(accentCol, accentDim, accentFaint);

        float openEase = easeOut(secondsSince(openedNanos) / 0.28f);
        ImGui.pushStyleVar(ImGuiStyleVar.Alpha, Math.max(0.01f, openEase));
        float slideOffset = (1f - openEase) * 18f;
        windowY += slideOffset;
        if (isVerticalLayout()) {
            drawVerticalLayout(accentCol, accentDim, accentFaint, displayW, displayH);
        } else {
            drawMainWindow(accentCol, accentDim, accentFaint);
        }
        drawSettingsWindow(accentCol, accentDim, displayW, displayH);
        windowY -= slideOffset;
        ImGui.popStyleVar();

        ImGui.popStyleColor(pushedColors);
        ImGui.popStyleVar(pushedVars);
    }

    private static int shaderTexture;
    private static int logoTexture;

    private static int resolveLogoTexture() {
        try {
            net.minecraft.client.renderer.texture.AbstractTexture tex = Minecraft.getInstance().getTextureManager().getTexture(LOGO_ID);
            if (tex != null && tex.getTexture() instanceof com.mojang.blaze3d.opengl.GlTexture gl)
                return com.eclipseware.imnotcheatingyouare.client.utils.ImGuiTextures.prepare(gl.glId(), true);
        } catch (Throwable ignored) {
        }
        return 0;
    }

    private static void shaderFill(ImDrawList dl, float x1, float y1, float x2, float y2, int tint, float rounding, int flags) {
        if (shaderTexture == 0) {
            dl.addRectFilled(x1, y1, x2, y2, tint, rounding, flags);
            return;
        }
        float dw = Math.max(1f, ImGui.getIO().getDisplaySizeX());
        float dh = Math.max(1f, ImGui.getIO().getDisplaySizeY());
        dl.addImageRounded(shaderTexture, x1, y1, x2, y2, x1 / dw, 1f - y1 / dh, x2 / dw, 1f - y2 / dh, tint, rounding, flags);
    }

    private static int tinted(Color c, int alpha) {
        return RenderUtils.toImGuiColor(c.getRed(), c.getGreen(), c.getBlue(), alpha);
    }

    private static void recomputeLayout(float displayW, float displayH) {
        windowW = clamp(displayW * 0.40f, 520f, 720f);
        windowH = clamp(displayH * 0.55f, 360f, 540f);
        if (windowW > displayW - 16f) windowW = displayW - 16f;
        if (windowH > displayH - 16f) windowH = displayH - 16f;
        lastDisplayW = displayW;
        lastDisplayH = displayH;
    }

    private static void clampPosition(float displayW, float displayH) {
        windowX = clamp(windowX, 4f, Math.max(4f, displayW - windowW - 4f));
        windowY = clamp(windowY, 4f, Math.max(4f, displayH - windowH - 4f));
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static void applySmoothScroll(String key) {
        float wx = ImGui.getWindowPosX(), wy = ImGui.getWindowPosY();
        boolean hovered = ImGui.isMouseHoveringRect(wx, wy, wx + ImGui.getWindowWidth(), wy + ImGui.getWindowHeight(), false)
                || ImGui.isWindowHovered(imgui.flag.ImGuiHoveredFlags.ChildWindows | imgui.flag.ImGuiHoveredFlags.AllowWhenBlockedByActiveItem);
        float wheel = hovered ? ImGui.getIO().getMouseWheel() : 0f;
        float current = ImGui.getScrollY();
        float max = ImGui.getScrollMaxY();
        Float last = scrollApplied.get(key);
        float target = scrollTarget.getOrDefault(key, current);
        if (last == null || Math.abs(current - last) > 0.5f) target = current;
        if (wheel != 0f) target -= wheel * 130f;
        target = clamp(target, 0f, Math.max(0f, max));
        float dt = ImGui.getIO().getDeltaTime();
        float next = current + (target - current) * Math.min(1f, dt * 22f);
        if (Math.abs(target - next) < 0.5f) next = target;
        ImGui.setScrollY(next);
        scrollTarget.put(key, target);
        scrollApplied.put(key, next);
    }

    private static int pushStyleVars() {
        int n = 0;
        ImGui.pushStyleVar(ImGuiStyleVar.Alpha, 1.0f); n++;
        ImGui.pushStyleVar(ImGuiStyleVar.WindowRounding, 12f); n++;
        ImGui.pushStyleVar(ImGuiStyleVar.ChildRounding, 10f); n++;
        ImGui.pushStyleVar(ImGuiStyleVar.FrameRounding, 7f); n++;
        ImGui.pushStyleVar(ImGuiStyleVar.PopupRounding, 10f); n++;
        ImGui.pushStyleVar(ImGuiStyleVar.ScrollbarRounding, 5f); n++;
        ImGui.pushStyleVar(ImGuiStyleVar.ScrollbarSize, 6f); n++;
        ImGui.pushStyleVar(ImGuiStyleVar.WindowBorderSize, 1f); n++;
        ImGui.pushStyleVar(ImGuiStyleVar.ChildBorderSize, 0f); n++;
        ImGui.pushStyleVar(ImGuiStyleVar.FrameBorderSize, 0f); n++;
        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, 0f, 0f); n++;
        ImGui.pushStyleVar(ImGuiStyleVar.ItemSpacing, 8f, 8f); n++;
        ImGui.pushStyleVar(ImGuiStyleVar.FramePadding, 10f, 9f); n++;
        ImGui.pushStyleVar(ImGuiStyleVar.PopupBorderSize, 0f); n++;
        return n;
    }

    private static int pushStyleColors(int accentCol, int accentDim, int accentFaint) {
        int n = 0;
        ImGui.pushStyleColor(ImGuiCol.WindowBg, RenderUtils.toImGuiColor(0, 0, 0, 0)); n++;
        ImGui.pushStyleColor(ImGuiCol.ChildBg, RenderUtils.toImGuiColor(0, 0, 0, 0)); n++;
        ImGui.pushStyleColor(ImGuiCol.Border, accentDim); n++;
        ImGui.pushStyleColor(ImGuiCol.FrameBg, RenderUtils.toImGuiColor(ROW_BG, 1.0f)); n++;
        ImGui.pushStyleColor(ImGuiCol.FrameBgHovered, RenderUtils.toImGuiColor(ROW_HOVER, 1.0f)); n++;
        ImGui.pushStyleColor(ImGuiCol.FrameBgActive, accentDim); n++;
        ImGui.pushStyleColor(ImGuiCol.Button, RenderUtils.toImGuiColor(ROW_BG, 1.0f)); n++;
        ImGui.pushStyleColor(ImGuiCol.ButtonHovered, RenderUtils.toImGuiColor(ROW_HOVER, 1.0f)); n++;
        ImGui.pushStyleColor(ImGuiCol.ButtonActive, accentDim); n++;
        ImGui.pushStyleColor(ImGuiCol.Text, RenderUtils.toImGuiColor(TEXT, 1.0f)); n++;
        ImGui.pushStyleColor(ImGuiCol.TextDisabled, RenderUtils.toImGuiColor(TEXT_DIM, 1.0f)); n++;
        ImGui.pushStyleColor(ImGuiCol.ScrollbarBg, RenderUtils.toImGuiColor(0, 0, 0, 0)); n++;
        ImGui.pushStyleColor(ImGuiCol.ScrollbarGrab, (accentCol & 0x00FFFFFF) | (120 << 24)); n++;
        ImGui.pushStyleColor(ImGuiCol.ScrollbarGrabHovered, (accentCol & 0x00FFFFFF) | (200 << 24)); n++;
        ImGui.pushStyleColor(ImGuiCol.ScrollbarGrabActive, accentCol); n++;
        ImGui.pushStyleColor(ImGuiCol.Separator, accentDim); n++;
        ImGui.pushStyleColor(ImGuiCol.PopupBg, RenderUtils.toImGuiColor(34, 27, 49, 255)); n++;
        ImGui.pushStyleColor(ImGuiCol.Header, accentFaint); n++;
        ImGui.pushStyleColor(ImGuiCol.HeaderHovered, accentDim); n++;
        ImGui.pushStyleColor(ImGuiCol.HeaderActive, accentCol); n++;
        return n;
    }

    private static final int WINDOW_FLAGS = ImGuiWindowFlags.NoTitleBar | ImGuiWindowFlags.NoCollapse
            | ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoMove | ImGuiWindowFlags.NoScrollbar
            | ImGuiWindowFlags.NoScrollWithMouse | ImGuiWindowFlags.NoSavedSettings;

    private static void drawMainWindow(int accentCol, int accentDim, int accentFaint) {
        ImGui.setNextWindowPos(windowX, windowY, ImGuiCond.Always);
        ImGui.setNextWindowSize(windowW, windowH, ImGuiCond.Always);

        if (ImGui.begin("##marlow_clickgui", WINDOW_FLAGS)) {
            IMGUI.setWindowFontScale(FONT_SCALE);

            ImDrawList windowDl = ImGui.getWindowDrawList();
            drawWindowSurface(windowDl, ImGui.getWindowPosX(), ImGui.getWindowPosY(), ImGui.getWindowWidth(), ImGui.getWindowHeight(), accentDim);

            drawTitleBar(accentCol, windowW, true);

            float bodyH = windowH - TITLE_H - BODY_BOTTOM_MARGIN;
            ImGui.setCursorPos(0f, TITLE_H);

            ImGui.beginChild("##sidebar", SIDEBAR_W, bodyH, false, ImGuiWindowFlags.NoScrollbar);
            IMGUI.setWindowFontScale(FONT_SCALE);
            drawSidebar(accentCol, accentFaint);
            ImGui.endChild();

            ImGui.sameLine(0f, 0f);

            ImGui.beginChild("##listpane", windowW - SIDEBAR_W, bodyH, false, ImGuiWindowFlags.NoScrollbar);
            IMGUI.setWindowFontScale(FONT_SCALE);
            drawModuleList(accentCol);
            ImGui.endChild();
        }
        ImGui.end();
    }

    private static com.eclipseware.imnotcheatingyouare.client.setting.Setting guiSetting(String name) {
        Module gui = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("GUI");
        return gui == null ? null : ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(gui, name);
    }

    private static boolean isVerticalLayout() {
        com.eclipseware.imnotcheatingyouare.client.setting.Setting layout = guiSetting("Layout");
        return layout != null && layout.getValString().equals("Vertical");
    }

    private static float glassOpacity() {
        com.eclipseware.imnotcheatingyouare.client.setting.Setting opacity = guiSetting("Glass Opacity");
        return opacity != null ? (float) (opacity.getValDouble() / 100.0) : 0.7f;
    }

    private static boolean glowEnabled() {
        com.eclipseware.imnotcheatingyouare.client.setting.Setting glow = guiSetting("Glow");
        return glow == null || glow.getValBoolean();
    }

    private static void drawWindowSurface(ImDrawList dl, float x, float y, float w, float h, int accentDim) {
        drawWindowSurface(dl, x, y, w, h, accentDim, TITLE_H, WINDOW_ROUNDING);
    }

    private static void drawWindowSurface(ImDrawList dl, float x, float y, float w, float h, int accentDim, float titleH, float rounding) {
        float time = (float) (System.nanoTime() / 1_000_000_000.0);
        if (glowEnabled()) {
            float breathe = 0.5f + 0.5f * (float) Math.sin(time * 1.6f);
            int base = accentDim & 0x00FFFFFF;
            for (int i = 3; i >= 1; i--) {
                float spread = i * 3.5f + breathe * 2f;
                int a = (int) ((10 + breathe * 8) / i);
                dl.addRectFilled(x - spread, y - spread, x + w + spread, y + h + spread, base | (a << 24), rounding + spread, ImDrawFlags.RoundCornersAll);
            }
        }
        dl.addRectFilled(x + 3f, y + 6f, x + w + 3f, y + h + 8f, RenderUtils.toImGuiColor(0, 0, 0, 60), rounding + 2f, ImDrawFlags.RoundCornersAll);
        shaderFill(dl, x, y, x + w, y + h, tinted(new Color(92, 70, 140), 255), rounding, ImDrawFlags.RoundCornersAll);
        int glass = (int) (235 * glassOpacity());
        int bodyTop = RenderUtils.toImGuiColor(BG.getRed(), BG.getGreen(), BG.getBlue(), (int) (glass * 0.82f));
        int bodyBottom = RenderUtils.toImGuiColor(BG.getRed(), BG.getGreen(), BG.getBlue(), glass);
        dl.addRectFilled(x, y + titleH + rounding, x + w, y + h, bodyBottom, rounding, ImDrawFlags.RoundCornersBottom);
        dl.addRectFilledMultiColor(x, y + titleH, x + w, y + titleH + rounding, bodyTop, bodyTop, bodyBottom, bodyBottom);
        shaderFill(dl, x, y, x + w, y + titleH, tinted(new Color(170, 140, 225), 255), rounding, ImDrawFlags.RoundCornersTop);
        int shadeTop = RenderUtils.toImGuiColor(20, 14, 34, 30);
        int shadeBottom = RenderUtils.toImGuiColor(20, 14, 34, 130);
        float shadeStart = Math.max(titleH * 0.35f, rounding + 1f);
        if (shadeStart < titleH) dl.addRectFilledMultiColor(x, y + shadeStart, x + w, y + titleH, shadeTop, shadeTop, shadeBottom, shadeBottom);
        dl.addLine(x + rounding, y + 1f, x + w - rounding, y + 1f, RenderUtils.toImGuiColor(255, 255, 255, 60), 1f);
        float sweep = (((time + x * 0.003f) * 0.35f) % 1.6f) - 0.3f;
        float bandW = w * 0.28f;
        float bx = x + sweep * w;
        dl.pushClipRect(x + rounding, y, x + w - rounding, y + titleH, true);
        int clear = RenderUtils.toImGuiColor(255, 255, 255, 0);
        int glow = RenderUtils.toImGuiColor(255, 255, 255, 34);
        dl.addRectFilledMultiColor(bx - bandW, y, bx, y + titleH, clear, glow, glow, clear);
        dl.addRectFilledMultiColor(bx, y, bx + bandW, y + titleH, glow, clear, clear, glow);
        dl.popClipRect();

        float pulse = 0.55f + 0.45f * (float) Math.sin(time * 2.2f);
        int border = (accentDim & 0x00FFFFFF) | ((int) (((accentDim >>> 24) & 0xFF) * (0.6f + pulse * 0.4f)) << 24);
        dl.addRect(x + 0.5f, y + 0.5f, x + w - 0.5f, y + h - 0.5f, border, rounding, ImDrawFlags.RoundCornersAll, 1f);
    }

    private static void drawTitleBar(int accentCol, float width, boolean draggable) {
        ImDrawList dl = ImGui.getWindowDrawList();
        ImVec2 origin = ImGui.getCursorScreenPos();
        float closeSize = 18f;
        float dragWidth = width - closeSize * 2f - 30f;

        ImGui.setCursorPos(0f, 0f);
        ImGui.invisibleButton("##titledrag", Math.max(10f, dragWidth), TITLE_H);
        if (draggable && ImGui.isItemActive()) {
            windowX += ImGui.getIO().getMouseDeltaX();
            windowY += ImGui.getIO().getMouseDeltaY();
        }

        ImGui.setCursorPos(width - closeSize * 2f - 20f, (TITLE_H - closeSize) / 2f);
        ImVec2 gearPos = ImGui.getCursorScreenPos();
        ImGui.invisibleButton("##guisettings", closeSize, closeSize);
        float gearHover = animateHover("gear", ImGui.isItemHovered());
        if (ImGui.isItemClicked()) {
            Module guiModule = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("GUI");
            if (guiModule != null) openSettings(guiModule, windowX, windowY, windowW);
        }
        dl.addRectFilled(gearPos.x, gearPos.y, gearPos.x + closeSize, gearPos.y + closeSize,
                RenderUtils.toImGuiColor((int) (30 + 90 * gearHover), (int) (22 + 60 * gearHover), (int) (44 + 120 * gearHover), 200), closeSize / 2f, ImDrawFlags.RoundCornersAll);
        com.eclipseware.imnotcheatingyouare.client.utils.ImGuiTextures.Region gear =
                com.eclipseware.imnotcheatingyouare.client.utils.ImGuiTextures.item("comparator");
        if (gear != null) {
            float gp = 2.5f;
            dl.addImage(gear.texture(), gearPos.x + gp, gearPos.y + gp, gearPos.x + closeSize - gp, gearPos.y + closeSize - gp, gear.u0(), gear.v0(), gear.u1(), gear.v1());
        }

        ImGui.setCursorPos(width - closeSize - 14f, (TITLE_H - closeSize) / 2f);
        ImVec2 closePos = ImGui.getCursorScreenPos();
        ImGui.invisibleButton("##close", closeSize, closeSize);
        boolean closeHovered = ImGui.isItemHovered();
        if (ImGui.isItemClicked()) setOpen(false);

        int closeBg = closeHovered ? RenderUtils.toImGuiColor(214, 70, 70, 230) : RenderUtils.toImGuiColor(ROW_BG, 1.0f);
        dl.addRectFilled(closePos.x, closePos.y, closePos.x + closeSize, closePos.y + closeSize, closeBg, 6f, ImDrawFlags.RoundCornersAll);
        int xCol = RenderUtils.toImGuiColor(TEXT, 1.0f);
        float p = 6f;
        dl.addLine(closePos.x + p, closePos.y + p, closePos.x + closeSize - p, closePos.y + closeSize - p, xCol, 1.5f);
        dl.addLine(closePos.x + closeSize - p, closePos.y + p, closePos.x + p, closePos.y + closeSize - p, xCol, 1.5f);

        if (logoTexture != 0) {
            float logoH = TITLE_H - 10f;
            float logoW = logoH * ((float) LOGO_NATIVE_WIDTH / LOGO_NATIVE_HEIGHT);
            float lx = origin.x + 14f;
            float ly = origin.y + (TITLE_H - logoH) / 2f;
            dl.addImage(logoTexture, lx, ly, lx + logoW, ly + logoH);
        }
    }

    private static String categoryItem(Category category) {
        return switch (category) {
            case Combat -> "diamond_sword";
            case Crystal -> "end_crystal";
            case Movement -> "feather";
            case Exploit -> "redstone";
            case Utility -> "iron_pickaxe";
            case Render -> "ender_eye";
            case HUD -> "name_tag";
            case Misc -> "bundle";
            case World -> "map";
            case Blatant -> "fire_charge";
            case Configs -> "writable_book";
            case Client -> "nether_star";
            case Macros -> "comparator";
            case Filters -> "hopper_minecart";
            case CartPvP -> "tnt_minecart";
            case Mace -> "mace";
            case UHC -> "golden_apple";
            case Farming -> "wheat";
        };
    }

    private static void drawSidebar(int accentCol, int accentFaint) {
        ImDrawList dl = ImGui.getWindowDrawList();
        float winX = ImGui.getWindowPosX();
        float winY = ImGui.getWindowPosY();
        float winW = ImGui.getWindowWidth();
        float winH = ImGui.getWindowHeight();
        dl.addRectFilled(winX, winY, winX + winW, winY + winH, RenderUtils.toImGuiColor(SIDEBAR_BG.getRed(), SIDEBAR_BG.getGreen(), SIDEBAR_BG.getBlue(), 120), WINDOW_ROUNDING, ImDrawFlags.RoundCornersBottomLeft);

        ImGui.setCursorPos(0f, 6f);
        ImGui.beginChild("##catscroll", winW, winH - 6f, false,
                ImGuiWindowFlags.AlwaysVerticalScrollbar | ImGuiWindowFlags.NoScrollWithMouse);
        IMGUI.setWindowFontScale(FONT_SCALE);
        applySmoothScroll("catscroll");

        ImDrawList inner = ImGui.getWindowDrawList();
        float rowW = ImGui.getContentRegionAvailX();
        float rowH = 32f;
        float th = ImGui.getFontSize();

        for (Category category : CATEGORY_ORDER) {
            List<Module> mods = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModules(category);
            if (mods.isEmpty()) continue;

            boolean selected = category == selectedCategory;
            ImVec2 cursor = ImGui.getCursorScreenPos();
            ImGui.pushID(category.name());
            ImGui.invisibleButton("##catrow", rowW, rowH);
            boolean hovered = ImGui.isItemHovered();
            if (ImGui.isItemClicked() && selectedCategory != category) {
                selectedCategory = category;
                categoryChangedNanos = System.nanoTime();
            }
            float catHover = animateHover("cat:" + category.name(), hovered || selected);
            ImGui.popID();

            float px1 = cursor.x + 6f;
            float px2 = cursor.x + rowW - 4f;
            if (selected) {
                shaderFill(inner, px1, cursor.y + 2f, px2, cursor.y + rowH - 2f, RenderUtils.toImGuiColor(255, 255, 255, 70), 8f, ImDrawFlags.RoundCornersAll);
                inner.addRectFilled(px1, cursor.y + 2f, px2, cursor.y + rowH - 2f, accentFaint, 8f, ImDrawFlags.RoundCornersAll);
                inner.addRectFilled(px1, cursor.y + 9f, px1 + 3f, cursor.y + rowH - 9f, accentCol, 1.5f, ImDrawFlags.RoundCornersAll);
            } else if (catHover > 0.01f) {
                inner.addRectFilled(px1, cursor.y + 2f, px1 + (px2 - px1) * (0.6f + 0.4f * catHover), cursor.y + rowH - 2f,
                        RenderUtils.toImGuiColor(ROW_HOVER.getRed(), ROW_HOVER.getGreen(), ROW_HOVER.getBlue(), (int) (255 * catHover)), 8f, ImDrawFlags.RoundCornersAll);
            }

            int tint = selected ? accentCol : RenderUtils.toImGuiColor(TEXT_DIM, 1.0f);
            float nudge = catHover * 3f;
            com.eclipseware.imnotcheatingyouare.client.utils.ImGuiTextures.Region icon =
                    com.eclipseware.imnotcheatingyouare.client.utils.ImGuiTextures.item(categoryItem(category));
            if (icon != null) {
                float isz = 16f;
                float icx = cursor.x + 20f + nudge;
                float icy = cursor.y + rowH / 2f;
                int iconTint = RenderUtils.toImGuiColor(255, 255, 255, selected ? 255 : (int) (190 + 65 * catHover));
                inner.addImage(icon.texture(), icx - isz / 2f, icy - isz / 2f, icx + isz / 2f, icy + isz / 2f, icon.u0(), icon.v0(), icon.u1(), icon.v1(), iconTint);
            } else {
                drawCategoryIcon(inner, category, cursor.x + 20f + nudge, cursor.y + rowH / 2f, 5.5f, tint);
            }
            inner.addText(cursor.x + 34f + nudge, cursor.y + (rowH - th) / 2f,
                    selected ? accentCol : RenderUtils.toImGuiColor(TEXT, 1.0f), category.name());
        }

        ImGui.endChild();
    }

    private static void drawModuleList(int accentCol) {
        float winW = ImGui.getWindowWidth();
        float winH = ImGui.getWindowHeight();

        ImGui.setCursorPos(14f, 10f);
        ImGui.setNextItemWidth(winW - 28f);
        ImGui.inputTextWithHint("##search", "Search...", searchBuffer);

        String filter = searchBuffer.get().trim().toLowerCase();
        List<Module> mods;
        if (filter.isEmpty()) {
            mods = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModules(selectedCategory);
        } else {
            mods = new ArrayList<>();
            for (Category c : CATEGORY_ORDER) {
                for (Module m : ImnotcheatingyouareClient.INSTANCE.moduleManager.getModules(c)) {
                    if (m.getName().toLowerCase().contains(filter)) mods.add(m);
                }
            }
        }

        float listTop = 44f;
        ImGui.setCursorPos(8f, listTop);
        ImGui.beginChild("##modscroll", winW - 16f, winH - listTop - 8f, false,
                ImGuiWindowFlags.AlwaysVerticalScrollbar | ImGuiWindowFlags.NoScrollWithMouse);
        IMGUI.setWindowFontScale(FONT_SCALE);
        applySmoothScroll("modscroll");

        float rowW = ImGui.getContentRegionAvailX();
        float listEase = easeOut(secondsSince(categoryChangedNanos) / 0.25f);
        ImGui.pushStyleVar(ImGuiStyleVar.Alpha, Math.max(0.01f, listEase) * ImGui.getStyle().getAlpha());
        String lastSubCategory = null;
        int rowIndex = 0;
        for (Module mod : mods) {
            float rowEase = easeOut((secondsSince(categoryChangedNanos) - rowIndex * 0.018f) / 0.22f);
            rowIndex++;
            ImGui.setCursorPosX(ImGui.getCursorPosX() + (1f - rowEase) * 14f);
            String subCategory = mod.getSubCategory();
            if (subCategory == null) subCategory = "";
            if (!subCategory.equals(lastSubCategory)) {
                if (!subCategory.isEmpty()) {
                    if (lastSubCategory != null) ImGui.dummy(0f, 8f);
                    drawSubCategoryHeader(subCategory);
                }
                lastSubCategory = subCategory;
            }
            drawModuleRow(mod, rowW, accentCol);
        }
        ImGui.popStyleVar();

        ImGui.endChild();
    }

    private static void drawSubCategoryHeader(String label) {
        float rowW = ImGui.getContentRegionAvailX();
        ImVec2 cursor = ImGui.getCursorScreenPos();
        float th = ImGui.getFontSize();
        ImDrawList dl = ImGui.getWindowDrawList();
        dl.addText(cursor.x + 4f, cursor.y, RenderUtils.toImGuiColor(TEXT_DIM, 1.0f), label.toUpperCase());
        ImGui.dummy(rowW, th + 8f);
    }

    private static final String BIND_POPUP = "##bindctx";
    private static Module bindPopupModule = null;

    private static float bindPillWidth(Module mod, boolean showEmpty) {
        String text = bindPillText(mod, showEmpty);
        if (text.isEmpty()) return 0f;
        ImVec2 sz = new ImVec2();
        ImGui.calcTextSize(sz, text);
        return sz.x + 14f;
    }

    private static String bindPillText(Module mod, boolean showEmpty) {
        int code = mod.getKeyBind();
        if (code == 0) return showEmpty ? "+" : "";
        String name = InputUtil.getName(code);
        return mod.isHoldMode() ? name + " \u00B7 H" : name;
    }

    private static boolean drawBindPill(Module mod, float rightX, float topY, float rowH, boolean showEmpty, int accentCol) {
        String text = bindPillText(mod, showEmpty);
        if (text.isEmpty()) return false;

        ImVec2 sz = new ImVec2();
        ImGui.calcTextSize(sz, text);
        float pw = sz.x + 14f;
        float ph = Math.min(rowH - 8f, sz.y + 6f);
        float px = rightX - pw;
        float py = topY + (rowH - ph) / 2f;

        ImVec2 saved = ImGui.getCursorScreenPos();
        ImGui.setCursorScreenPos(px, py);
        ImGui.invisibleButton("##bindpill", pw, ph);
        boolean hovered = ImGui.isItemHovered();
        boolean leftClick = ImGui.isItemClicked(0);
        boolean rightClick = ImGui.isItemClicked(1);
        ImGui.setCursorScreenPos(saved.x, saved.y);

        boolean bound = mod.getKeyBind() != 0;
        boolean binding = bindingModule == mod;
        ImDrawList dl = ImGui.getWindowDrawList();
        int fill = binding ? RenderUtils.toImGuiColor(255, 205, 90, 60)
                : bound ? ((accentCol & 0x00FFFFFF) | ((hovered ? 90 : 60) << 24))
                : RenderUtils.toImGuiColor(255, 255, 255, hovered ? 30 : 14);
        int border = binding ? RenderUtils.toImGuiColor(255, 205, 90, 255)
                : bound ? ((accentCol & 0x00FFFFFF) | (140 << 24))
                : RenderUtils.toImGuiColor(255, 255, 255, 40);
        dl.addRectFilled(px, py, px + pw, py + ph, fill, 5f, ImDrawFlags.RoundCornersAll);
        dl.addRect(px, py, px + pw, py + ph, border, 5f, ImDrawFlags.RoundCornersAll, 1f);
        int textCol = bound || binding ? RenderUtils.toImGuiColor(255, 255, 255, 255) : RenderUtils.toImGuiColor(TEXT_DIM, 1.0f);
        dl.addText(px + 7f, py + (ph - sz.y) / 2f, textCol, binding ? "..." : text);

        if (leftClick && bindingModule == null) {
            bindingModule = mod;
            bindingArmed = false;
        }
        if (rightClick && bound) {
            bindPopupModule = mod;
            ImGui.openPopup(BIND_POPUP);
        }
        if (bindPopupModule == mod) drawBindPopup(accentCol);
        return hovered;
    }

    private static void drawBindPopup(int accentCol) {
        if (bindPopupModule == null) return;
        Module mod = bindPopupModule;

        ImGui.pushStyleVar(ImGuiStyleVar.WindowRounding, 9f);
        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, 5f, 5f);
        ImGui.pushStyleVar(ImGuiStyleVar.PopupBorderSize, 1f);
        ImGui.pushStyleColor(ImGuiCol.PopupBg, RenderUtils.toImGuiColor(ROW_BG.getRed(), ROW_BG.getGreen(), ROW_BG.getBlue(), 250));
        ImGui.pushStyleColor(ImGuiCol.Border, RenderUtils.toImGuiColor(255, 255, 255, 28));
        boolean open = ImGui.beginPopup(BIND_POPUP);
        if (open) {
            float w = 170f;
            float rowH = 24f;
            ImDrawList dl = ImGui.getWindowDrawList();
            float th = ImGui.getFontSize();

            ImVec2 hp = ImGui.getCursorScreenPos();
            dl.addText(hp.x + 8f, hp.y + 3f, RenderUtils.toImGuiColor(TEXT_DIM, 1.0f), mod.getName() + "  " + InputUtil.getName(mod.getKeyBind()));
            ImGui.dummy(w, th + 8f);

            String[] labels = {"Unbind", "Toggle (default)", "Only On Hold"};
            for (int i = 0; i < labels.length; i++) {
                ImGui.pushID(i);
                ImVec2 rc = ImGui.getCursorScreenPos();
                ImGui.invisibleButton("##opt", w, rowH);
                boolean hov = ImGui.isItemHovered();
                boolean clicked = ImGui.isItemClicked(0);
                boolean current = (i == 1 && !mod.isHoldMode()) || (i == 2 && mod.isHoldMode());
                if (hov) dl.addRectFilled(rc.x, rc.y, rc.x + w, rc.y + rowH, RenderUtils.toImGuiColor(255, 255, 255, 18), 6f, ImDrawFlags.RoundCornersAll);
                if (current) dl.addRectFilled(rc.x + 2f, rc.y + 5f, rc.x + 5f, rc.y + rowH - 5f, accentCol, 1.5f, ImDrawFlags.RoundCornersAll);
                int col = i == 0 ? RenderUtils.toImGuiColor(255, 120, 120, 255)
                        : current ? RenderUtils.toImGuiColor(255, 255, 255, 255) : RenderUtils.toImGuiColor(TEXT, 1.0f);
                dl.addText(rc.x + 12f, rc.y + (rowH - th) / 2f, col, labels[i]);
                if (clicked) {
                    if (i == 0) mod.setKeyBind(0);
                    else mod.setHoldMode(i == 2);
                    com.eclipseware.imnotcheatingyouare.client.setting.ConfigManager.save();
                    ImGui.closeCurrentPopup();
                }
                ImGui.popID();
            }
            ImGui.endPopup();
        } else {
            bindPopupModule = null;
        }
        ImGui.popStyleColor(2);
        ImGui.popStyleVar(3);
    }

    private static void drawModuleRow(Module mod, float rowW, int accentCol) {
        boolean toggled = mod.isToggled();
        boolean isSettings = mod == settingsModule;
        List<Setting> settings = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingsByMod(mod);
        boolean hasSettings = !settings.isEmpty();

        String subtitle = buildSubtitle(settings);
        float rowH = subtitle.isEmpty() ? 32f : 42f;
        float toggleW = 30f;
        float rightMargin = 6f;
        float cardW = rowW - rightMargin;

        ImGui.pushID(mod.getName());
        ImVec2 cursor = ImGui.getCursorScreenPos();

        float rowBtnW = Math.max(10f, cardW - toggleW - 18f);
        ImGui.invisibleButton("##row", rowBtnW, rowH);
        boolean hovered = ImGui.isItemHovered();
        boolean boundKey = mod.getKeyBind() != 0;
        float pillW = bindPillWidth(mod, hovered);
        boolean overPill = pillW > 0f && ImGui.isMouseHoveringRect(cursor.x + rowBtnW - pillW - 6f, cursor.y, cursor.x + rowBtnW - 6f, cursor.y + rowH);
        if (bindingModule == null && ImGui.isItemClicked() && !overPill) mod.toggle();
        if (hasSettings && ImGui.isItemClicked(1) && !overPill) {
            openSettings(mod, windowX, windowY, windowW);
        }
        if (ImGui.isItemClicked(2)) {
            bindingModule = mod;
            bindingArmed = false;
        }

        boolean binding = bindingModule == mod;

        ImDrawList dl = ImGui.getWindowDrawList();
        float hov = animateHover("mod:" + mod.getName(), hovered);
        Color bg = new Color(
                (int) (ROW_BG.getRed() + (ROW_HOVER.getRed() - ROW_BG.getRed()) * hov),
                (int) (ROW_BG.getGreen() + (ROW_HOVER.getGreen() - ROW_BG.getGreen()) * hov),
                (int) (ROW_BG.getBlue() + (ROW_HOVER.getBlue() - ROW_BG.getBlue()) * hov));
        dl.addRectFilled(cursor.x, cursor.y, cursor.x + cardW, cursor.y + rowH,
                RenderUtils.toImGuiColor(bg.getRed(), bg.getGreen(), bg.getBlue(), 215), 8f, ImDrawFlags.RoundCornersAll);
        float onAnim = toggleAnim.getOrDefault(mod.getName(), toggled ? 1f : 0f);
        if (onAnim > 0.01f || hov > 0.01f) {
            int washAlpha = (int) (26 * onAnim + 18 * hov);
            shaderFill(dl, cursor.x, cursor.y, cursor.x + cardW, cursor.y + rowH, RenderUtils.toImGuiColor(255, 255, 255, washAlpha), 8f, ImDrawFlags.RoundCornersAll);
        }
        if (hov > 0.01f) {
            dl.addRect(cursor.x, cursor.y, cursor.x + cardW, cursor.y + rowH, (accentCol & 0x00FFFFFF) | ((int) (70 * hov) << 24), 8f, ImDrawFlags.RoundCornersAll, 1f);
        }
        if (binding) {
            dl.addRect(cursor.x, cursor.y, cursor.x + cardW, cursor.y + rowH,
                    RenderUtils.toImGuiColor(255, 205, 90, 255), 7f, ImDrawFlags.RoundCornersAll, 1.5f);
        }
        if (toggled) {
            float barGrow = (rowH - 16f) * onAnim;
            float mid = cursor.y + rowH / 2f;
            dl.addRectFilled(cursor.x + 4f, mid - barGrow / 2f, cursor.x + 7f, mid + barGrow / 2f, accentCol, 1.5f, ImDrawFlags.RoundCornersAll);
        }
        if (isSettings) {
            dl.addRect(cursor.x, cursor.y, cursor.x + cardW, cursor.y + rowH, accentCol, 7f, ImDrawFlags.RoundCornersAll, 1f);
        }

        float th = ImGui.getFontSize();
        int nameCol = toggled ? accentCol : RenderUtils.toImGuiColor(TEXT, 1.0f);
        float textLimit = cursor.x + cardW - toggleW - 18f - (boundKey ? pillW + 8f : 0f);

        dl.pushClipRect(cursor.x, cursor.y, textLimit, cursor.y + rowH, true);
        if (binding) {
            dl.addText(cursor.x + 14f, cursor.y + 7f, nameCol, mod.getName());
            dl.addText(cursor.x + 14f, cursor.y + 7f + th + 2f,
                    RenderUtils.toImGuiColor(255, 205, 90, 255), "Press a key (Esc to unbind)");
        } else if (subtitle.isEmpty()) {
            dl.addText(cursor.x + 14f, cursor.y + (rowH - th) / 2f, nameCol, mod.getName());
        } else {
            dl.addText(cursor.x + 14f, cursor.y + 7f, nameCol, mod.getName());
            dl.addText(cursor.x + 14f, cursor.y + 7f + th + 2f,
                    RenderUtils.toImGuiColor(TEXT_DIM, 1.0f), subtitle);
        }
        dl.popClipRect();

        drawBindPill(mod, cursor.x + rowBtnW - 6f, cursor.y, rowH, hovered, accentCol);

        ImGui.setCursorScreenPos(cursor.x + cardW - toggleW - 10f, cursor.y + (rowH - 16f) / 2f);
        boolean newToggled = drawToggleSwitch("##toggle", mod.getName(), toggled, accentCol);
        if (newToggled != toggled) mod.toggle();
        ImGui.setCursorScreenPos(cursor.x, cursor.y + rowH);

        ImGui.popID();
        ImGui.dummy(0f, 3f);
    }

    private static String buildSubtitle(List<Setting> settings) {
        if (settings.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (Setting s : settings) {
            if (count >= 2) break;
            if (!s.isVisible()) continue;
            String part;
            if (s.isCheck()) {
                if (!s.getValBoolean()) continue;
                part = s.getName();
            } else if (s.isSlider()) {
                part = s.getName() + " " + (s.onlyInt()
                        ? String.valueOf((int) s.getValDouble())
                        : String.format("%.1f", s.getValDouble()));
            } else if (s.isCombo()) {
                part = s.getValString();
            } else {
                continue;
            }
            if (sb.length() > 0) sb.append(", ");
            sb.append(part);
            count++;
        }
        return sb.toString();
    }

    private static boolean settingsPlaced = false;
    private static float settingsX, settingsY;
    private static float settingsAnchorX = Float.NaN, settingsAnchorY, settingsAnchorW;

    private static void openSettings(Module mod, float anchorX, float anchorY, float anchorW) {
        boolean opening = settingsModule != mod;
        settingsModule = opening ? mod : null;
        if (opening) {
            settingsAnimModule = mod;
            settingsOpenNanos = System.nanoTime();
            settingsAnchorX = anchorX;
            settingsAnchorY = anchorY;
            settingsAnchorW = anchorW;
            settingsPlaced = false;
        }
    }

    private static void drawSettingsWindow(int accentCol, int accentDim, float displayW, float displayH) {
        Module mod = settingsModule;
        if (mod == null) return;

        float height = isVerticalLayout() ? Math.min(460f, displayH - 40f) : windowH;
        float panelX;
        float panelY;
        if (settingsPlaced) {
            panelX = settingsX;
            panelY = settingsY;
        } else {
            float ax = Float.isNaN(settingsAnchorX) || !isVerticalLayout() ? windowX : settingsAnchorX;
            float ay = Float.isNaN(settingsAnchorX) || !isVerticalLayout() ? windowY : settingsAnchorY;
            float aw = Float.isNaN(settingsAnchorX) || !isVerticalLayout() ? windowW : settingsAnchorW;
            panelX = ax + aw + 10f;
            if (panelX + SETTINGS_W > displayW - 4f) panelX = Math.max(4f, ax - SETTINGS_W - 10f);
            panelY = ay;
        }
        panelX = clamp(panelX, 4f, Math.max(4f, displayW - SETTINGS_W - 4f));
        panelY = clamp(panelY, 4f, Math.max(4f, displayH - height - 4f));

        float openEase = 1f;
        if (mod == settingsAnimModule) {
            openEase = easeOut(secondsSince(settingsOpenNanos) / 0.22f);
        }

        ImGui.pushStyleVar(ImGuiStyleVar.Alpha, Math.max(0.01f, openEase) * ImGui.getStyle().getAlpha());
        float drawX = panelX - (1f - openEase) * 16f;

        ImGui.setNextWindowPos(drawX, panelY, ImGuiCond.Always);
        ImGui.setNextWindowSize(SETTINGS_W, height, ImGuiCond.Always);

        if (ImGui.begin("##marlow_settings", WINDOW_FLAGS)) {
            IMGUI.setWindowFontScale(FONT_SCALE);

            ImDrawList dl = ImGui.getWindowDrawList();
            float headerH = TITLE_H + 18f;
            drawWindowSurface(dl, ImGui.getWindowPosX(), ImGui.getWindowPosY(), ImGui.getWindowWidth(), ImGui.getWindowHeight(), accentDim, headerH, WINDOW_ROUNDING);

            ImVec2 origin = ImGui.getCursorScreenPos();
            float th = ImGui.getFontSize();
            float closeSize = 18f;

            ImGui.setCursorPos(0f, 0f);
            ImGui.invisibleButton("##sdrag", Math.max(10f, SETTINGS_W - closeSize - 24f), headerH);
            if (ImGui.isItemActive() && (ImGui.getIO().getMouseDeltaX() != 0f || ImGui.getIO().getMouseDeltaY() != 0f)) {
                settingsPlaced = true;
                settingsX = panelX + ImGui.getIO().getMouseDeltaX();
                settingsY = panelY + ImGui.getIO().getMouseDeltaY();
            } else if (settingsPlaced) {
                settingsX = panelX;
                settingsY = panelY;
            }

            com.eclipseware.imnotcheatingyouare.client.utils.ImGuiTextures.Region icon =
                    com.eclipseware.imnotcheatingyouare.client.utils.ImGuiTextures.item(categoryItem(mod.getCategory()));
            float textX = origin.x + 16f;
            if (icon != null) {
                float isz = 20f;
                float iy = origin.y + (headerH - isz) / 2f;
                dl.addRectFilled(origin.x + 12f, iy - 4f, origin.x + 12f + isz + 8f, iy + isz + 4f, RenderUtils.toImGuiColor(20, 14, 34, 120), 7f, ImDrawFlags.RoundCornersAll);
                dl.addImage(icon.texture(), origin.x + 16f, iy, origin.x + 16f + isz, iy + isz, icon.u0(), icon.v0(), icon.u1(), icon.v1());
                textX = origin.x + 16f + isz + 14f;
            }

            boolean toggled = mod.isToggled();
            float nameY = origin.y + headerH / 2f - th - 1f;
            dl.addText(textX, nameY, RenderUtils.toImGuiColor(255, 255, 255, 255), mod.getName());
            String desc = mod.getDescription() == null || mod.getDescription().isEmpty() ? (toggled ? "Enabled" : "Disabled") : mod.getDescription();
            dl.pushClipRect(textX, origin.y, origin.x + SETTINGS_W - closeSize - 20f, origin.y + headerH, true);
            dl.addText(textX, nameY + th + 3f, RenderUtils.toImGuiColor(235, 225, 250, 170), desc);
            dl.popClipRect();

            ImGui.setCursorPos(SETTINGS_W - closeSize - 14f, (headerH - closeSize) / 2f);
            ImVec2 closePos = ImGui.getCursorScreenPos();
            ImGui.invisibleButton("##sclose", closeSize, closeSize);
            float closeHover = animateHover("sclose", ImGui.isItemHovered());
            if (ImGui.isItemClicked()) settingsModule = null;

            int closeBg = RenderUtils.toImGuiColor((int) (30 + 184 * closeHover), (int) (22 + 48 * closeHover), (int) (44 + 26 * closeHover), 200);
            dl.addRectFilled(closePos.x, closePos.y, closePos.x + closeSize, closePos.y + closeSize, closeBg, closeSize / 2f, ImDrawFlags.RoundCornersAll);
            int xCol = RenderUtils.toImGuiColor(TEXT, 1.0f);
            float p = 6f;
            dl.addLine(closePos.x + p, closePos.y + p, closePos.x + closeSize - p, closePos.y + closeSize - p, xCol, 1.5f);
            dl.addLine(closePos.x + closeSize - p, closePos.y + p, closePos.x + p, closePos.y + closeSize - p, xCol, 1.5f);

            ImGui.setCursorPos(10f, headerH + 10f);
            ImGui.beginChild("##settingsscroll", SETTINGS_W - 20f, height - headerH - 10f - BODY_BOTTOM_MARGIN, false,
                    ImGuiWindowFlags.AlwaysVerticalScrollbar | ImGuiWindowFlags.NoScrollWithMouse);
            IMGUI.setWindowFontScale(FONT_SCALE);
            applySmoothScroll("settingsscroll");

            List<Setting> settings = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingsByMod(mod);
            ImDrawList sdl = ImGui.getWindowDrawList();
            int index = 0;
            for (Setting setting : settings) {
                if (!setting.isVisible()) continue;
                float rowEase = easeOut((secondsSince(settingsOpenNanos) - index * 0.02f) / 0.2f);
                index++;
                sdl.channelsSplit(2);
                sdl.channelsSetCurrent(1);
                ImVec2 top = ImGui.getCursorScreenPos();
                float cardW = ImGui.getContentRegionAvailX();
                float inset = 10f + (1f - rowEase) * 10f;
                ImGui.setCursorScreenPos(top.x + inset, top.y + 8f);
                ImGui.beginGroup();
                renderSetting(setting, accentCol);
                ImGui.endGroup();
                float bottom = ImGui.getItemRectMaxY() + 2f;
                sdl.channelsSetCurrent(0);
                sdl.addRectFilled(top.x, top.y, top.x + cardW, bottom, RenderUtils.toImGuiColor(255, 255, 255, (int) (10 * rowEase)), 8f, ImDrawFlags.RoundCornersAll);
                sdl.addRect(top.x, top.y, top.x + cardW, bottom, RenderUtils.toImGuiColor(255, 255, 255, (int) (16 * rowEase)), 8f, ImDrawFlags.RoundCornersAll, 1f);
                sdl.channelsMerge();
                ImGui.setCursorScreenPos(top.x, bottom + 6f);
                ImGui.dummy(0f, 0f);
            }

            ImGui.endChild();
        }
        ImGui.end();
        ImGui.popStyleVar();
    }

    private static final java.util.Map<Category, float[]> panelState = new java.util.EnumMap<>(Category.class);
    private static final float PANEL_W = 170f;
    private static final float PANEL_HEADER = 30f;
    private static final float PANEL_ROW = 24f;

    private static void drawVerticalLayout(int accentCol, int accentDim, int accentFaint, float displayW, float displayH) {
        if (panelState.isEmpty() || Math.abs(displayW - lastPackedWidth) > 1f) {
            packPanels(displayW, displayH);
        }

        String filter = searchBuffer.get().trim().toLowerCase();
        drawSearchPanel(accentDim, displayW);

        int panelIndex = 0;
        for (Category category : CATEGORY_ORDER) {
            List<Module> all = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModules(category);
            if (all.isEmpty()) continue;
            List<Module> mods = new ArrayList<>();
            for (Module m : all) {
                if (filter.isEmpty() || m.getName().toLowerCase().contains(filter)) mods.add(m);
            }
            if (!filter.isEmpty() && mods.isEmpty()) continue;
            drawCategoryPanel(category, mods, accentCol, accentDim, accentFaint, displayW, displayH, panelIndex++);
        }
    }

    private static float lastPackedWidth = -1f;

    private static void packPanels(float displayW, float displayH) {
        lastPackedWidth = displayW;
        panelState.clear();
        float gap = 12f;
        float left = 16f;
        float usable = displayW - left * 2f - PANEL_W - gap;
        int columns = Math.max(1, (int) ((usable + gap) / (PANEL_W + gap)));
        float[] bottoms = new float[columns];
        java.util.Arrays.fill(bottoms, 16f);
        for (Category category : CATEGORY_ORDER) {
            int count = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModules(category).size();
            if (count == 0) continue;
            float h = PANEL_HEADER + Math.min(count * (PANEL_ROW + 3f) + 8f, displayH * 0.62f);
            int best = 0;
            for (int c = 1; c < columns; c++) if (bottoms[c] < bottoms[best] - 0.5f) best = c;
            float x = left + best * (PANEL_W + gap);
            panelState.put(category, new float[]{x, bottoms[best], 0f, 1f});
            bottoms[best] += h + gap;
        }
        searchPanelPos[0] = displayW - PANEL_W - left;
        searchPanelPos[1] = 16f;
    }

    private static final float[] searchPanelPos = {Float.NaN, 0f};

    private static void drawSearchPanel(int accentDim, float displayW) {
        if (Float.isNaN(searchPanelPos[0])) {
            searchPanelPos[0] = displayW - PANEL_W - 16f;
            searchPanelPos[1] = 16f;
        }
        float h = PANEL_HEADER + 44f;
        ImGui.setNextWindowPos(searchPanelPos[0], searchPanelPos[1], ImGuiCond.Always);
        ImGui.setNextWindowSize(PANEL_W, h, ImGuiCond.Always);
        if (ImGui.begin("##vp_search", WINDOW_FLAGS)) {
            IMGUI.setWindowFontScale(FONT_SCALE);
            ImDrawList dl = ImGui.getWindowDrawList();
            float x = ImGui.getWindowPosX();
            float y = ImGui.getWindowPosY();
            drawWindowSurface(dl, x, y, PANEL_W, h, accentDim, PANEL_HEADER, 10f);
            ImGui.setCursorPos(0f, 0f);
            ImGui.invisibleButton("##sp_drag", PANEL_W, PANEL_HEADER);
            if (ImGui.isItemActive()) {
                searchPanelPos[0] += ImGui.getIO().getMouseDeltaX();
                searchPanelPos[1] += ImGui.getIO().getMouseDeltaY();
            }
            float th = ImGui.getFontSize();
            dl.addText(x + 14f, y + (PANEL_HEADER - th) / 2f, RenderUtils.toImGuiColor(255, 255, 255, 255), "Search");
            ImGui.setCursorPos(10f, PANEL_HEADER + 8f);
            ImGui.setNextItemWidth(PANEL_W - 20f);
            ImGui.inputTextWithHint("##vsearch", "Find a module...", searchBuffer);
        }
        ImGui.end();
    }

    private static void drawCategoryPanel(Category category, List<Module> mods, int accentCol, int accentDim, int accentFaint, float displayW, float displayH, int panelIndex) {
        float[] st = panelState.get(category);
        float collapseTarget = st[2] > 0.5f ? 0f : 1f;
        st[3] += (collapseTarget - st[3]) * Math.min(1f, ImGui.getIO().getDeltaTime() * 14f);
        float expand = st[3];

        float listH = Math.min(mods.size() * (PANEL_ROW + 3f) + 8f, displayH * 0.62f);
        float h = PANEL_HEADER + listH * expand;
        st[0] = clamp(st[0], 4f, Math.max(4f, displayW - PANEL_W - 4f));
        st[1] = clamp(st[1], 4f, Math.max(4f, displayH - PANEL_HEADER - 4f));

        float enter = easeOut((secondsSince(openedNanos) - panelIndex * 0.03f) / 0.3f);
        ImGui.pushStyleVar(ImGuiStyleVar.Alpha, Math.max(0.01f, enter) * ImGui.getStyle().getAlpha());
        ImGui.setNextWindowPos(st[0], st[1] + (1f - enter) * 14f, ImGuiCond.Always);
        ImGui.setNextWindowSize(PANEL_W, h, ImGuiCond.Always);

        if (ImGui.begin("##vp_" + category.name(), WINDOW_FLAGS)) {
            IMGUI.setWindowFontScale(FONT_SCALE);
            ImDrawList dl = ImGui.getWindowDrawList();
            float x = ImGui.getWindowPosX();
            float y = ImGui.getWindowPosY();
            drawWindowSurface(dl, x, y, PANEL_W, h, accentDim, PANEL_HEADER, 10f);

            ImGui.setCursorPos(0f, 0f);
            ImGui.invisibleButton("##hdr", PANEL_W, PANEL_HEADER);
            if (ImGui.isItemActive()) {
                st[0] += ImGui.getIO().getMouseDeltaX();
                st[1] += ImGui.getIO().getMouseDeltaY();
            }
            if (ImGui.isItemClicked(1)) st[2] = st[2] > 0.5f ? 0f : 1f;

            float th = ImGui.getFontSize();
            com.eclipseware.imnotcheatingyouare.client.utils.ImGuiTextures.Region icon =
                    com.eclipseware.imnotcheatingyouare.client.utils.ImGuiTextures.item(categoryItem(category));
            float tx = x + 12f;
            if (icon != null) {
                float isz = 16f;
                float iy = y + (PANEL_HEADER - isz) / 2f;
                dl.addImage(icon.texture(), tx, iy, tx + isz, iy + isz, icon.u0(), icon.v0(), icon.u1(), icon.v1());
                tx += isz + 8f;
            }
            dl.addText(tx, y + (PANEL_HEADER - th) / 2f, RenderUtils.toImGuiColor(255, 255, 255, 255), category.name());
            int enabled = 0;
            for (Module m : mods) if (m.isToggled()) enabled++;
            String count = enabled + "/" + mods.size();
            ImVec2 cs = new ImVec2();
            ImGui.calcTextSize(cs, count);
            dl.addText(x + PANEL_W - 12f - cs.x, y + (PANEL_HEADER - th) / 2f, RenderUtils.toImGuiColor(240, 230, 255, 150), count);

            if (expand > 0.02f) {
                ImGui.setCursorPos(6f, PANEL_HEADER + 4f);
                ImGui.beginChild("##vlist", PANEL_W - 12f, Math.max(1f, listH * expand - 6f), false,
                        ImGuiWindowFlags.NoScrollbar | ImGuiWindowFlags.NoScrollWithMouse);
                IMGUI.setWindowFontScale(FONT_SCALE);
                applySmoothScroll("vlist_" + category.name());
                ImDrawList ldl = ImGui.getWindowDrawList();
                float rowW = ImGui.getContentRegionAvailX();
                for (Module mod : mods) {
                    ImGui.pushID(mod.getName());
                    ImVec2 c = ImGui.getCursorScreenPos();
                    ImGui.invisibleButton("##vrow", rowW, PANEL_ROW);
                    boolean hovered = ImGui.isItemHovered();
                    float vPillW = bindPillWidth(mod, hovered);
                    boolean vOverPill = vPillW > 0f && ImGui.isMouseHoveringRect(c.x + rowW - 22f - vPillW, c.y, c.x + rowW - 22f, c.y + PANEL_ROW);
                    if (bindingModule == null && ImGui.isItemClicked() && !vOverPill) mod.toggle();
                    if (ImGui.isItemClicked(1) && !vOverPill && !ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingsByMod(mod).isEmpty()) {
                        openSettings(mod, x, y, PANEL_W);
                    }
                    if (ImGui.isItemClicked(2)) {
                        bindingModule = mod;
                        bindingArmed = false;
                    }
                    float hov = animateHover("v:" + mod.getName(), hovered);
                    float target = mod.isToggled() ? 1f : 0f;
                    float on = toggleAnim.getOrDefault(mod.getName(), target);
                    on += (target - on) * Math.min(1f, ImGui.getIO().getDeltaTime() * 12f);
                    toggleAnim.put(mod.getName(), on);

                    if (on > 0.01f) {
                        shaderFill(ldl, c.x, c.y, c.x + rowW, c.y + PANEL_ROW, RenderUtils.toImGuiColor(255, 255, 255, (int) (70 * on)), 7f, ImDrawFlags.RoundCornersAll);
                        ldl.addRectFilled(c.x, c.y, c.x + rowW, c.y + PANEL_ROW, (accentFaint & 0x00FFFFFF) | ((int) (((accentFaint >>> 24) & 0xFF) * on) << 24), 7f, ImDrawFlags.RoundCornersAll);
                    }
                    if (hov > 0.01f) {
                        ldl.addRectFilled(c.x, c.y, c.x + rowW, c.y + PANEL_ROW, RenderUtils.toImGuiColor(255, 255, 255, (int) (16 * hov)), 7f, ImDrawFlags.RoundCornersAll);
                    }
                    if (mod == settingsModule) {
                        ldl.addRect(c.x, c.y, c.x + rowW, c.y + PANEL_ROW, accentCol, 7f, ImDrawFlags.RoundCornersAll, 1f);
                    }
                    float bar = (PANEL_ROW - 10f) * on;
                    float mid = c.y + PANEL_ROW / 2f;
                    if (bar > 0.5f) ldl.addRectFilled(c.x + 3f, mid - bar / 2f, c.x + 5.5f, mid + bar / 2f, accentCol, 1.2f, ImDrawFlags.RoundCornersAll);

                    boolean binding = bindingModule == mod;
                    int textCol = binding ? RenderUtils.toImGuiColor(255, 205, 90, 255)
                            : mod.isToggled() ? RenderUtils.toImGuiColor(255, 255, 255, 255) : RenderUtils.toImGuiColor(205, 198, 222, 235);
                    String label = binding ? "Press a key (Esc to unbind)" : mod.getName();
                    ldl.pushClipRect(c.x, c.y, c.x + rowW - 26f - vPillW, c.y + PANEL_ROW, true);
                    ldl.addText(c.x + 11f + hov * 2f, c.y + (PANEL_ROW - th) / 2f, textCol, label);
                    ldl.popClipRect();
                    drawBindPill(mod, c.x + rowW - 22f, c.y, PANEL_ROW, hovered, accentCol);
                    if (!ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingsByMod(mod).isEmpty()) {
                        float dx = c.x + rowW - 10f;
                        int dotCol = RenderUtils.toImGuiColor(TEXT_DIM.getRed(), TEXT_DIM.getGreen(), TEXT_DIM.getBlue(), (int) (120 + 100 * hov));
                        for (int d = -1; d <= 1; d++) ldl.addCircleFilled(dx, mid + d * 3.5f, 1.2f, dotCol);
                    }
                    ImGui.popID();
                    ImGui.dummy(0f, 3f);
                }
                float maxScroll = ImGui.getScrollMaxY();
                if (maxScroll > 0.5f) {
                    float viewH = ImGui.getWindowHeight();
                    float contentH = viewH + maxScroll;
                    float thumbH = Math.max(18f, viewH * viewH / contentH);
                    float thumbY = ImGui.getWindowPosY() + (viewH - thumbH) * (ImGui.getScrollY() / maxScroll);
                    float trackX = ImGui.getWindowPosX() + ImGui.getWindowWidth() - 3f;
                    ImDrawList fg = ImGui.getWindowDrawList();
                    fg.addRectFilled(trackX - 1.5f, ImGui.getWindowPosY() + 2f, trackX + 1.5f, ImGui.getWindowPosY() + viewH - 2f, RenderUtils.toImGuiColor(255, 255, 255, 18), 1.5f, ImDrawFlags.RoundCornersAll);
                    fg.addRectFilled(trackX - 1.5f, thumbY, trackX + 1.5f, thumbY + thumbH, (accentCol & 0x00FFFFFF) | (190 << 24), 1.5f, ImDrawFlags.RoundCornersAll);
                }
                ImGui.endChild();
            }
        }
        ImGui.end();
        ImGui.popStyleVar();
    }

    private static boolean drawToggleSwitch(String id, String animKey, boolean value, int accentCol) {
        float w = 30f;
        float h = 16f;
        ImVec2 cursor = ImGui.getCursorScreenPos();
        ImGui.invisibleButton(id, w, h);
        boolean clicked = ImGui.isItemClicked();

        float target = value ? 1f : 0f;
        float current = toggleAnim.getOrDefault(animKey, target);
        float dt = ImGui.getIO().getDeltaTime();
        current += (target - current) * Math.min(1f, dt * 12f);
        if (Math.abs(target - current) < 0.004f) current = target;
        toggleAnim.put(animKey, current);

        ImDrawList dl = ImGui.getWindowDrawList();
        dl.addRectFilled(cursor.x, cursor.y, cursor.x + w, cursor.y + h, RenderUtils.toImGuiColor(TOGGLE_OFF, 1.0f), h / 2f, ImDrawFlags.RoundCornersAll);
        if (current > 0.01f) {
            int a = (int) (255 * current);
            shaderFill(dl, cursor.x, cursor.y, cursor.x + w, cursor.y + h, RenderUtils.toImGuiColor(255, 255, 255, a), h / 2f, ImDrawFlags.RoundCornersAll);
            dl.addRectFilled(cursor.x, cursor.y, cursor.x + w, cursor.y + h, (accentCol & 0x00FFFFFF) | ((int) (a * 0.45f) << 24), h / 2f, ImDrawFlags.RoundCornersAll);
        }

        float knobR = h / 2f - 2.2f;
        float knobCx = cursor.x + h / 2f + (w - h) * current;
        dl.addCircleFilled(knobCx, cursor.y + h / 2f + 0.8f, knobR + 0.6f, RenderUtils.toImGuiColor(0, 0, 0, 60));
        dl.addCircleFilled(knobCx, cursor.y + h / 2f, knobR, RenderUtils.toImGuiColor(255, 255, 255, 255));

        return clicked ? !value : value;
    }

    private static final java.util.Set<String> openCombos = new java.util.HashSet<>();

    private static String drawComboSetting(String label, String current, List<String> options, int accentCol) {
        ImGui.pushID(label);

        float avail = ImGui.getContentRegionAvailX();
        float th = ImGui.getFontSize();
        ImDrawList dl = ImGui.getWindowDrawList();
        ImVec2 lp = ImGui.getCursorScreenPos();

        dl.pushClipRect(lp.x, lp.y, lp.x + avail, lp.y + th + 2f, true);
        dl.addText(lp.x, lp.y, RenderUtils.toImGuiColor(TEXT, 1.0f), label);
        dl.popClipRect();
        ImGui.dummy(avail, th + 5f);

        String result = current;
        float h = 24f;
        int trackCol = RenderUtils.toImGuiColor(TOGGLE_OFF, 1.0f);
        int accentFill = (accentCol & 0x00FFFFFF) | (170 << 24);
        int white = RenderUtils.toImGuiColor(255, 255, 255, 255);
        int dim = RenderUtils.toImGuiColor(TEXT_DIM, 1.0f);
        ImVec2 tsz = new ImVec2();

        if (options.size() <= 3) {
            ImVec2 c = ImGui.getCursorScreenPos();
            ImGui.invisibleButton("##seg", avail, h);
            boolean hovered = ImGui.isItemHovered();
            boolean clicked = ImGui.isItemClicked(0);
            float segW = avail / options.size();

            dl.addRectFilled(c.x, c.y, c.x + avail, c.y + h, trackCol, 7f, ImDrawFlags.RoundCornersAll);
            for (int i = 0; i < options.size(); i++) {
                String opt = options.get(i);
                float x0 = c.x + segW * i;
                float x1 = x0 + segW;
                boolean sel = opt.equals(current);
                boolean segHover = hovered && ImGui.getMousePosX() >= x0 && ImGui.getMousePosX() < x1;
                if (sel) {
                    dl.addRectFilled(x0 + 2f, c.y + 2f, x1 - 2f, c.y + h - 2f, accentFill, 5f, ImDrawFlags.RoundCornersAll);
                } else if (segHover) {
                    dl.addRectFilled(x0 + 2f, c.y + 2f, x1 - 2f, c.y + h - 2f, RenderUtils.toImGuiColor(255, 255, 255, 18), 5f, ImDrawFlags.RoundCornersAll);
                }
                ImGui.calcTextSize(tsz, opt);
                dl.pushClipRect(x0 + 3f, c.y, x1 - 3f, c.y + h, true);
                dl.addText(x0 + (segW - tsz.x) / 2f, c.y + (h - tsz.y) / 2f, sel ? white : dim, opt);
                dl.popClipRect();
                if (clicked && segHover && !sel) result = opt;
            }
            ImGui.dummy(0f, 9f);
        } else {
            boolean open = openCombos.contains(label);
            ImVec2 c = ImGui.getCursorScreenPos();
            ImGui.invisibleButton("##field", avail, h);
            boolean hovered = ImGui.isItemHovered();
            if (ImGui.isItemClicked(0)) {
                if (open) openCombos.remove(label); else openCombos.add(label);
                open = !open;
            }

            dl.addRectFilled(c.x, c.y, c.x + avail, c.y + h,
                    hovered ? RenderUtils.toImGuiColor(ROW_HOVER, 1.0f) : trackCol, 7f, ImDrawFlags.RoundCornersAll);
            ImGui.calcTextSize(tsz, current);
            dl.pushClipRect(c.x + 8f, c.y, c.x + avail - 24f, c.y + h, true);
            dl.addText(c.x + 10f, c.y + (h - tsz.y) / 2f, white, current);
            dl.popClipRect();

            float ax = c.x + avail - 14f;
            float ay = c.y + h / 2f;
            if (open) dl.addTriangleFilled(ax - 4f, ay + 2f, ax + 4f, ay + 2f, ax, ay - 3f, dim);
            else dl.addTriangleFilled(ax - 4f, ay - 2f, ax + 4f, ay - 2f, ax, ay + 3f, dim);

            if (open) {
                float rowH = 22f;
                ImGui.dummy(0f, 3f);
                ImVec2 lc = ImGui.getCursorScreenPos();
                float panelH = rowH * options.size() + 6f;
                dl.addRectFilled(lc.x, lc.y, lc.x + avail, lc.y + panelH, RenderUtils.toImGuiColor(ROW_BG, 1.0f), 7f, ImDrawFlags.RoundCornersAll);
                dl.addRect(lc.x, lc.y, lc.x + avail, lc.y + panelH, RenderUtils.toImGuiColor(255, 255, 255, 22), 7f, ImDrawFlags.RoundCornersAll, 1f);
                ImGui.setCursorScreenPos(lc.x, lc.y + 3f);
                for (String opt : options) {
                    ImVec2 rc = ImGui.getCursorScreenPos();
                    ImGui.pushID(opt);
                    ImGui.invisibleButton("##opt", avail, rowH);
                    boolean rowHover = ImGui.isItemHovered();
                    boolean sel = opt.equals(current);
                    if (rowHover) {
                        dl.addRectFilled(rc.x + 3f, rc.y, rc.x + avail - 3f, rc.y + rowH, RenderUtils.toImGuiColor(255, 255, 255, 16), 5f, ImDrawFlags.RoundCornersAll);
                    }
                    if (sel) {
                        dl.addRectFilled(rc.x + 3f, rc.y + 4f, rc.x + 6f, rc.y + rowH - 4f, accentCol, 2f, ImDrawFlags.RoundCornersAll);
                    }
                    ImGui.calcTextSize(tsz, opt);
                    dl.addText(rc.x + 14f, rc.y + (rowH - tsz.y) / 2f, sel ? white : RenderUtils.toImGuiColor(TEXT, 1.0f), opt);
                    if (ImGui.isItemClicked(0)) {
                        result = opt;
                        openCombos.remove(label);
                    }
                    ImGui.popID();
                }
                ImGui.setCursorScreenPos(lc.x, lc.y + panelH);
                ImGui.dummy(0f, 0f);
            }
            ImGui.dummy(0f, 9f);
        }

        ImGui.popID();
        return result;
    }

    private static final Map<String, Integer> curveDrag = new HashMap<>();

    private static double[] drawCurveEditor(String label, double[] c, double[] def, int accentCol) {
        ImGui.pushID(label);

        float avail = ImGui.getContentRegionAvailX();
        float th = ImGui.getFontSize();
        ImDrawList dl = ImGui.getWindowDrawList();
        ImVec2 lp = ImGui.getCursorScreenPos();

        dl.addText(lp.x, lp.y, RenderUtils.toImGuiColor(TEXT, 1.0f), label);
        String resetText = "Reset";
        ImVec2 rs = new ImVec2();
        ImGui.calcTextSize(rs, resetText);
        ImVec2 saved = ImGui.getCursorScreenPos();
        ImGui.setCursorScreenPos(lp.x + avail - rs.x - 8f, lp.y - 1f);
        ImGui.invisibleButton("##reset", rs.x + 8f, th + 2f);
        boolean resetHover = ImGui.isItemHovered();
        boolean resetClick = ImGui.isItemClicked(0);
        ImGui.setCursorScreenPos(saved.x, saved.y);
        dl.addText(lp.x + avail - rs.x - 4f, lp.y, resetHover ? accentCol : RenderUtils.toImGuiColor(TEXT_DIM, 1.0f), resetText);
        ImGui.dummy(avail, th + 5f);

        float size = Math.min(avail, 170f);
        float gx = ImGui.getCursorScreenPos().x + (avail - size) / 2f;
        float gy = ImGui.getCursorScreenPos().y;
        float pad = 10f;
        float inner = size - pad * 2f;

        ImGui.setCursorScreenPos(gx, gy);
        ImGui.invisibleButton("##graph", size, size);
        boolean active = ImGui.isItemActive();
        boolean clicked = ImGui.isItemClicked(0);

        double[] out = c.clone();

        float p1x = gx + pad + (float) c[0] * inner, p1y = gy + pad + inner - (float) c[1] * inner;
        float p2x = gx + pad + (float) c[2] * inner, p2y = gy + pad + inner - (float) c[3] * inner;

        Integer dragging = curveDrag.get(label);
        if (clicked) {
            float mx = ImGui.getMousePosX(), my = ImGui.getMousePosY();
            float d1 = (mx - p1x) * (mx - p1x) + (my - p1y) * (my - p1y);
            float d2 = (mx - p2x) * (mx - p2x) + (my - p2y) * (my - p2y);
            dragging = d1 <= d2 ? 0 : 1;
            curveDrag.put(label, dragging);
        }
        if (!active) {
            curveDrag.remove(label);
            dragging = null;
        }
        if (active && dragging != null) {
            double nx = clamp((ImGui.getMousePosX() - gx - pad) / inner, 0f, 1f);
            double ny = clamp(1f - (ImGui.getMousePosY() - gy - pad) / inner, -0.25f, 1.5f);
            if (dragging == 0) { out[0] = nx; out[1] = ny; }
            else { out[2] = nx; out[3] = ny; }
        }
        if (resetClick) out = def.clone();

        p1x = gx + pad + (float) out[0] * inner; p1y = gy + pad + inner - (float) out[1] * inner;
        p2x = gx + pad + (float) out[2] * inner; p2y = gy + pad + inner - (float) out[3] * inner;

        dl.addRectFilled(gx, gy, gx + size, gy + size, RenderUtils.toImGuiColor(TOGGLE_OFF.getRed(), TOGGLE_OFF.getGreen(), TOGGLE_OFF.getBlue(), 120), 8f, ImDrawFlags.RoundCornersAll);
        dl.addRect(gx, gy, gx + size, gy + size, RenderUtils.toImGuiColor(255, 255, 255, 26), 8f, ImDrawFlags.RoundCornersAll, 1f);
        int grid = RenderUtils.toImGuiColor(255, 255, 255, 14);
        for (int i = 1; i < 4; i++) {
            float t = pad + inner * i / 4f;
            dl.addLine(gx + t, gy + pad, gx + t, gy + pad + inner, grid, 1f);
            dl.addLine(gx + pad, gy + t, gx + pad + inner, gy + t, grid, 1f);
        }

        float ox = gx + pad, oy = gy + pad + inner;
        float ex = gx + pad + inner, ey = gy + pad;
        int handleLine = RenderUtils.toImGuiColor(255, 255, 255, 60);
        dl.addLine(ox, oy, p1x, p1y, handleLine, 1f);
        dl.addLine(ex, ey, p2x, p2y, handleLine, 1f);

        int steps = 40;
        float prevX = ox, prevY = oy;
        for (int i = 1; i <= steps; i++) {
            double t = i / (double) steps;
            double u = 1.0 - t;
            double bx = 3 * u * u * t * out[0] + 3 * u * t * t * out[2] + t * t * t;
            double by = 3 * u * u * t * out[1] + 3 * u * t * t * out[3] + t * t * t;
            float px = gx + pad + (float) bx * inner;
            float py = gy + pad + inner - (float) by * inner;
            dl.addLine(prevX, prevY, px, py, accentCol, 2.2f);
            prevX = px;
            prevY = py;
        }

        int white = RenderUtils.toImGuiColor(255, 255, 255, 255);
        dl.addCircleFilled(p1x, p1y, 5.5f, white);
        dl.addCircleFilled(p1x, p1y, 3f, accentCol);
        dl.addCircleFilled(p2x, p2y, 5.5f, white);
        dl.addCircleFilled(p2x, p2y, 3f, accentCol);

        dl.addText(gx + 6f, gy + size - th - 3f, RenderUtils.toImGuiColor(TEXT_DIM, 1.0f), "distance");
        ImVec2 sp = new ImVec2();
        ImGui.calcTextSize(sp, "speed");
        dl.addText(gx + size - sp.x - 6f, gy + 3f, RenderUtils.toImGuiColor(TEXT_DIM, 1.0f), "speed");

        ImGui.setCursorScreenPos(gx, gy + size);
        ImGui.dummy(0f, 0f);
        ImGui.popID();
        ImGui.dummy(0f, 9f);
        return out;
    }

    private static final Map<String, Integer> rangeDrag = new HashMap<>();

    private static float[] drawRangeSlider(String label, float lo, float hi, float min, float max, boolean isInt, int accentCol) {
        ImGui.pushID(label);

        String valueText = isInt ? ((int) lo + " - " + (int) hi) : String.format("%.1f - %.1f", lo, hi);
        float avail = ImGui.getContentRegionAvailX();
        float th = ImGui.getFontSize();

        ImVec2 labelPos = ImGui.getCursorScreenPos();
        ImDrawList dl = ImGui.getWindowDrawList();
        ImVec2 valueSize = new ImVec2();
        ImGui.calcTextSize(valueSize, valueText);

        dl.pushClipRect(labelPos.x, labelPos.y, labelPos.x + avail - valueSize.x - 4f, labelPos.y + th + 2f, true);
        dl.addText(labelPos.x, labelPos.y, RenderUtils.toImGuiColor(TEXT, 1.0f), label);
        dl.popClipRect();
        dl.addText(labelPos.x + avail - valueSize.x, labelPos.y, RenderUtils.toImGuiColor(TEXT_DIM, 1.0f), valueText);

        ImGui.dummy(avail, th + 4f);

        float hitH = 16f;
        float pad = 6f;
        float trackW = Math.max(1f, avail - pad * 2f);
        ImVec2 cursor = ImGui.getCursorScreenPos();
        ImGui.invisibleButton("##range", avail, hitH);
        boolean active = ImGui.isItemActive();
        boolean clicked = ImGui.isItemClicked(0);

        float range = Math.max(0.0001f, max - min);
        float loX = cursor.x + pad + trackW * clamp((lo - min) / range, 0f, 1f);
        float hiX = cursor.x + pad + trackW * clamp((hi - min) / range, 0f, 1f);

        Integer dragging = rangeDrag.get(label);
        if (clicked) {
            float mx = ImGui.getMousePosX();
            float dLo = Math.abs(mx - loX), dHi = Math.abs(mx - hiX);
            dragging = dLo < dHi ? 0 : (dHi < dLo ? 1 : (mx < loX ? 0 : 1));
            rangeDrag.put(label, dragging);
        }
        if (!active) {
            rangeDrag.remove(label);
            dragging = null;
        }

        float newLo = lo, newHi = hi;
        if (active && dragging != null) {
            float frac = clamp((ImGui.getMousePosX() - cursor.x - pad) / trackW, 0f, 1f);
            float v = min + frac * range;
            if (isInt) v = Math.round(v);
            if (dragging == 0) newLo = Math.min(v, hi);
            else newHi = Math.max(v, lo);
        }

        float drawLo = clamp((newLo - min) / range, 0f, 1f);
        float drawHi = clamp((newHi - min) / range, 0f, 1f);
        float x0 = cursor.x + pad + trackW * drawLo;
        float x1 = cursor.x + pad + trackW * drawHi;
        float trackY = cursor.y + hitH / 2f - 2.5f;
        float trackH = 5f;
        dl.addRectFilled(cursor.x + pad, trackY, cursor.x + pad + trackW, trackY + trackH,
                RenderUtils.toImGuiColor(TOGGLE_OFF, 1.0f), trackH / 2f, ImDrawFlags.RoundCornersAll);
        dl.addRectFilled(x0, trackY, Math.max(x0 + 1f, x1), trackY + trackH, (accentCol & 0x00FFFFFF) | (200 << 24), trackH / 2f, ImDrawFlags.RoundCornersAll);
        int white = RenderUtils.toImGuiColor(255, 255, 255, 255);
        dl.addCircleFilled(x0, cursor.y + hitH / 2f, 5.5f, white);
        dl.addCircleFilled(x1, cursor.y + hitH / 2f, 5.5f, white);

        ImGui.popID();
        ImGui.dummy(0f, 9f);
        return new float[]{newLo, newHi};
    }

    private static float drawCustomSlider(String label, float value, float min, float max, boolean isInt, int accentCol) {
        ImGui.pushID(label);

        String valueText = isInt ? String.valueOf((int) value) : String.format("%.2f", value);
        float avail = ImGui.getContentRegionAvailX();
        float th = ImGui.getFontSize();

        ImVec2 labelPos = ImGui.getCursorScreenPos();
        ImDrawList dl = ImGui.getWindowDrawList();
        ImVec2 valueSize = new ImVec2();
        ImGui.calcTextSize(valueSize, valueText);

        dl.pushClipRect(labelPos.x, labelPos.y, labelPos.x + avail - valueSize.x - 4f, labelPos.y + th + 2f, true);
        dl.addText(labelPos.x, labelPos.y, RenderUtils.toImGuiColor(TEXT, 1.0f), label);
        dl.popClipRect();
        dl.addText(labelPos.x + avail - valueSize.x, labelPos.y, RenderUtils.toImGuiColor(TEXT_DIM, 1.0f), valueText);

        ImGui.dummy(avail, th + 4f);

        float hitH = 16f;
        ImVec2 cursor = ImGui.getCursorScreenPos();
        ImGui.invisibleButton("##slider", avail, hitH);
        boolean active = ImGui.isItemActive();

        float newValue = value;
        if (active) {
            float frac = clamp((ImGui.getMousePosX() - cursor.x) / avail, 0f, 1f);
            newValue = min + frac * (max - min);
            if (isInt) newValue = Math.round(newValue);
        }

        float drawFrac = (max > min) ? clamp((newValue - min) / (max - min), 0f, 1f) : 0f;
        float trackY = cursor.y + hitH / 2f - 2.5f;
        float trackH = 5f;
        dl.addRectFilled(cursor.x, trackY, cursor.x + avail, trackY + trackH,
                RenderUtils.toImGuiColor(TOGGLE_OFF, 1.0f), trackH / 2f, ImDrawFlags.RoundCornersAll);
        if (drawFrac > 0f) {
            shaderFill(dl, cursor.x, trackY, cursor.x + Math.max(trackH, avail * drawFrac), trackY + trackH, RenderUtils.toImGuiColor(255, 255, 255, 255), trackH / 2f, ImDrawFlags.RoundCornersAll);
            dl.addRectFilled(cursor.x, trackY, cursor.x + Math.max(trackH, avail * drawFrac), trackY + trackH, (accentCol & 0x00FFFFFF) | (110 << 24), trackH / 2f, ImDrawFlags.RoundCornersAll);
        }
        dl.addCircleFilled(cursor.x + avail * drawFrac, cursor.y + hitH / 2f, 5.5f,
                RenderUtils.toImGuiColor(255, 255, 255, 255));

        ImGui.popID();
        ImGui.dummy(0f, 9f);
        return newValue;
    }

    private static void drawCrossedSwords(ImDrawList dl, float cx, float cy, float r, int color) {
        drawSword(dl, cx, cy, r, 45f, color);
        drawSword(dl, cx, cy, r, -45f, color);
    }

    private static void drawSword(ImDrawList dl, float cx, float cy, float r, float angleDeg, int color) {
        double a = Math.toRadians(angleDeg);
        float dx = (float) Math.cos(a);
        float dy = (float) Math.sin(a);
        float px = -dy;
        float py = dx;

        float tipX = cx + dx * r;
        float tipY = cy + dy * r;
        float guardX = cx - dx * r * 0.12f;
        float guardY = cy - dy * r * 0.12f;
        float handleEndX = cx - dx * r * 0.78f;
        float handleEndY = cy - dy * r * 0.78f;
        float bladeHalfW = r * 0.13f;
        float guardHalfW = r * 0.3f;

        dl.addTriangleFilled(
                tipX, tipY,
                guardX + px * bladeHalfW, guardY + py * bladeHalfW,
                guardX - px * bladeHalfW, guardY - py * bladeHalfW,
                color);
        dl.addLine(guardX - px * guardHalfW, guardY - py * guardHalfW,
                guardX + px * guardHalfW, guardY + py * guardHalfW, color, 1.3f);
        dl.addLine(guardX, guardY, handleEndX, handleEndY, color, 1.4f);
        dl.addCircleFilled(handleEndX, handleEndY, r * 0.1f, color);
    }

    private static void drawCategoryIcon(ImDrawList dl, Category category, float cx, float cy, float r, int color) {
        switch (category) {
            case Combat -> drawCrossedSwords(dl, cx, cy, r * 1.25f, color);
            case Crystal -> {
                float topY = cy - r;
                float shoulderY = cy - r * 0.1f;
                float botY = cy + r * 0.95f;
                float tableHalf = r * 0.4f;
                float shoulderHalf = r;

                dl.pathLineTo(cx - tableHalf, topY);
                dl.pathLineTo(cx + tableHalf, topY);
                dl.pathLineTo(cx + shoulderHalf, shoulderY);
                dl.pathLineTo(cx, botY);
                dl.pathLineTo(cx - shoulderHalf, shoulderY);
                dl.pathFillConvex(color);

                dl.addLine(cx - tableHalf, topY, cx, botY, color, 1f);
                dl.addLine(cx + tableHalf, topY, cx, botY, color, 1f);
            }
            case Mace -> {
                float headTop = cy - r;
                float headMidY = cy - r * 0.45f;
                float headBotY = cy - r * 0.05f;
                float headHalf = r * 0.55f;

                dl.pathLineTo(cx, headTop);
                dl.pathLineTo(cx + headHalf, headMidY);
                dl.pathLineTo(cx + headHalf * 0.6f, headBotY);
                dl.pathLineTo(cx - headHalf * 0.6f, headBotY);
                dl.pathLineTo(cx - headHalf, headMidY);
                dl.pathFillConvex(color);

                dl.addLine(cx - headHalf, headMidY, cx - headHalf * 1.35f, headMidY, color, 1.2f);
                dl.addLine(cx + headHalf, headMidY, cx + headHalf * 1.35f, headMidY, color, 1.2f);

                dl.addLine(cx, headBotY, cx, cy + r * 0.85f, color, 1.4f);
                dl.addCircleFilled(cx, cy + r * 0.95f, r * 0.12f, color);
            }
            case CartPvP -> {
                float top = cy - r * 0.6f;
                float bot = cy + r * 0.15f;
                float topHalf = r;
                float botHalf = r * 0.75f;

                dl.pathLineTo(cx - topHalf, top);
                dl.pathLineTo(cx + topHalf, top);
                dl.pathLineTo(cx + botHalf, bot);
                dl.pathLineTo(cx - botHalf, bot);
                dl.pathStroke(color, ImDrawFlags.Closed, 1.3f);

                dl.addLine(cx - topHalf * 1.05f, top, cx + topHalf * 1.05f, top, color, 1.3f);
                dl.addCircleFilled(cx - r * 0.5f, cy + r * 0.75f, r * 0.16f, color);
                dl.addCircleFilled(cx + r * 0.5f, cy + r * 0.75f, r * 0.16f, color);
            }
            case UHC -> {
                float circleR = r * 0.52f;
                float circleY = cy - r * 0.32f;
                dl.addCircleFilled(cx - r * 0.42f, circleY, circleR, color);
                dl.addCircleFilled(cx + r * 0.42f, circleY, circleR, color);
                dl.addTriangleFilled(cx - r * 0.95f, cy - r * 0.05f, cx + r * 0.95f, cy - r * 0.05f, cx, cy + r * 0.95f, color);
            }
            case Movement -> dl.addTriangleFilled(cx - r * 0.65f, cy - r, cx - r * 0.65f, cy + r, cx + r, cy, color);
            case Render -> {
                dl.addCircle(cx, cy, r, color, 12, 1.2f);
                dl.addCircleFilled(cx, cy, 1.6f, color);
            }
            case World -> {
                dl.addCircle(cx, cy, r, color, 14, 1.2f);
                dl.addLine(cx - r, cy, cx + r, cy, color, 1.1f);
            }
            case Exploit -> {
                dl.addLine(cx + r * 0.4f, cy - r, cx - r * 0.4f, cy, color, 1.3f);
                dl.addLine(cx - r * 0.4f, cy, cx + r * 0.2f, cy, color, 1.3f);
                dl.addLine(cx + r * 0.2f, cy, cx - r * 0.4f, cy + r, color, 1.3f);
            }
            case Utility -> {
                dl.addCircle(cx - r * 0.45f, cy - r * 0.45f, r * 0.42f, color, 8, 1.2f);
                dl.addLine(cx - r * 0.15f, cy - r * 0.15f, cx + r * 0.85f, cy + r * 0.85f, color, 1.4f);
            }
            case HUD -> {
                dl.addRect(cx - r, cy - r * 0.7f, cx + r, cy + r * 0.7f, color, 1.5f, 0, 1.2f);
                dl.addLine(cx - r * 0.35f, cy + r * 0.7f, cx + r * 0.35f, cy + r * 0.7f, color, 1.2f);
            }
            case Configs -> {
                dl.addRect(cx - r, cy - r * 0.55f, cx + r, cy + r * 0.75f, color, 1.5f, 0, 1.2f);
                dl.addLine(cx - r, cy - r * 0.55f, cx - r * 0.2f, cy - r * 0.55f, color, 1.2f);
                dl.addLine(cx - r * 0.2f, cy - r * 0.55f, cx, cy - r, color, 1.2f);
            }
            case Macros -> {
                dl.addLine(cx - r, cy - r * 0.55f, cx + r, cy - r * 0.55f, color, 1.2f);
                dl.addLine(cx - r, cy, cx + r, cy, color, 1.2f);
                dl.addLine(cx - r, cy + r * 0.55f, cx + r * 0.3f, cy + r * 0.55f, color, 1.2f);
            }
            case Filters -> dl.addTriangleFilled(cx - r, cy - r * 0.75f, cx + r, cy - r * 0.75f, cx, cy + r, color);
            case Client -> {
                dl.addLine(cx, cy - r, cx + r, cy, color, 1.3f);
                dl.addLine(cx + r, cy, cx, cy + r, color, 1.3f);
                dl.addLine(cx, cy + r, cx - r, cy, color, 1.3f);
                dl.addLine(cx - r, cy, cx, cy - r, color, 1.3f);
            }
            case Misc -> {
                dl.addCircleFilled(cx - r * 0.65f, cy, 1.3f, color);
                dl.addCircleFilled(cx, cy, 1.3f, color);
                dl.addCircleFilled(cx + r * 0.65f, cy, 1.3f, color);
            }
            case Blatant -> {
                dl.addLine(cx, cy - r, cx, cy + r * 0.25f, color, 1.5f);
                dl.addCircleFilled(cx, cy + r * 0.85f, 1.3f, color);
            }
            case Farming -> {
                dl.addLine(cx, cy + r, cx, cy - r * 0.1f, color, 1.4f);
                dl.addTriangleFilled(cx, cy - r * 0.1f, cx - r * 0.75f, cy - r * 0.35f, cx, cy - r, color);
                dl.addTriangleFilled(cx, cy - r * 0.1f, cx + r * 0.75f, cy - r * 0.35f, cx, cy - r, color);
            }
            default -> dl.addCircle(cx, cy, r * 0.8f, color, 10, 1.2f);
        }
    }

    private static void renderSetting(Setting s, int accentCol) {
        ImGui.pushID(s.getName());

        if (s.isCheck()) {
            boolean val = s.getValBoolean();
            float avail = ImGui.getContentRegionAvailX();
            float th = ImGui.getFontSize();
            ImVec2 pos = ImGui.getCursorScreenPos();
            ImDrawList dl = ImGui.getWindowDrawList();

            dl.pushClipRect(pos.x, pos.y, pos.x + avail - 42f, pos.y + th + 2f, true);
            dl.addText(pos.x, pos.y, RenderUtils.toImGuiColor(TEXT, 1.0f), s.getName());
            dl.popClipRect();

            ImGui.setCursorPosX(ImGui.getCursorPosX() + avail - 34f);
            boolean newVal = drawToggleSwitch("##s_toggle", s.getName(), val, accentCol);
            if (newVal != val) s.setValBoolean(newVal);
            ImGui.dummy(0f, 9f);
        } else if (s.isSlider()) {
            float val = (float) s.getValDouble();
            float newVal = drawCustomSlider(s.getName(), val, (float) s.getMin(), (float) s.getMax(),
                    s.onlyInt(), accentCol);
            if (newVal != val) s.setValDouble(newVal);
        } else if (s.isCurve()) {
            double[] cur = s.getCurve();
            double[] res = drawCurveEditor(s.getName(), cur, s.getCurveDefault(), accentCol);
            if (res[0] != cur[0] || res[1] != cur[1] || res[2] != cur[2] || res[3] != cur[3]) s.setCurve(res[0], res[1], res[2], res[3]);
        } else if (s.isRange()) {
            float[] r = drawRangeSlider(s.getName(), (float) s.getRangeLow(), (float) s.getRangeHigh(),
                    (float) s.getMin(), (float) s.getMax(), s.onlyInt(), accentCol);
            if (r[0] != (float) s.getRangeLow() || r[1] != (float) s.getRangeHigh()) s.setRange(r[0], r[1]);
        } else if (s.isCombo()) {
            String picked = drawComboSetting(s.getName(), s.getValString(), s.getOptions(), accentCol);
            if (!picked.equals(s.getValString())) s.setValString(picked);
        } else if (s.isColor()) {
            Color color = new Color(s.getValColor(), true);
            float[] col4 = new float[]{
                    color.getRed() / 255f, color.getGreen() / 255f,
                    color.getBlue() / 255f, color.getAlpha() / 255f
            };
            ImGui.textDisabled(s.getName());
            ImGui.dummy(0f, 3f);
            ImGui.setNextItemWidth(ImGui.getContentRegionAvailX());
            if (ImGui.colorEdit4("##v", col4)) {
                s.setValColor(new Color(col4[0], col4[1], col4[2], col4[3]).getRGB());
            }
            ImGui.dummy(0f, 9f);
        } else if (s.isText()) {
            ImGui.textDisabled(s.getName());
            ImGui.dummy(0f, 3f);
            ImString imStr = new ImString(s.getValText(), 256);
            ImGui.setNextItemWidth(ImGui.getContentRegionAvailX());
            if (ImGui.inputText("##v", imStr)) s.setValText(imStr.get());
            ImGui.dummy(0f, 9f);
        }

        ImGui.popID();
    }
}
