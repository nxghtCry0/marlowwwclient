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
import net.minecraft.client.gui.GuiGraphicsExtractor;
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

    public static void setOpen(boolean value) {
        open = value;
        if (!value) settingsModule = null;

        Minecraft mc = Minecraft.getInstance();
        if (mc.gui == null) return;

        if (value) {
            if (!(mc.gui.screen() instanceof EmptyScreen)) {
                mc.setScreenAndShow(new EmptyScreen());
            }
        } else if (mc.gui.screen() instanceof EmptyScreen) {
            mc.setScreenAndShow((Screen) null);
        }
    }

    public static void toggle() {
        setOpen(!open);
    }

    public static void markClosed() {
        open = false;
        settingsModule = null;
        bindingModule = null;
    }

    public static boolean isBinding() {
        return bindingModule != null;
    }

    public static void handleBindingKey(int key) {
        if (bindingModule == null) return;
        if (key == com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE) {
            bindingModule = null;
            return;
        }
        int code = key;
        if (key == com.mojang.blaze3d.platform.InputConstants.KEY_DELETE
                || key == com.mojang.blaze3d.platform.InputConstants.KEY_BACKSPACE) {
            code = 0;
        }
        bindingModule.setKeyBind(code);
        com.eclipseware.imnotcheatingyouare.client.setting.ConfigManager.save();
        bindingModule = null;
    }

    private static void pollBindingMouse() {
        if (bindingModule == null) return;

        if (!bindingArmed) {
            if (!InputUtil.isMouseButtonDown(InputUtil.MOUSE_MIDDLE)) {
                bindingArmed = true;
            }
            return;
        }

        int[] buttons = {InputUtil.MOUSE_LEFT, InputUtil.MOUSE_RIGHT, InputUtil.MOUSE_MIDDLE,
                InputUtil.MOUSE_BUTTON_4, InputUtil.MOUSE_BUTTON_5};
        for (int mb : buttons) {
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
        if (mc.player == null || mc.gui == null) {
            markClosed();
            return;
        }
        if (!(mc.gui.screen() instanceof EmptyScreen)) {
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
    private static final float SIDEBAR_W = 132f;
    private static final float SETTINGS_W = 220f;
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
    public static void renderChrome(GuiGraphicsExtractor guiGraphics) {
        if (!open) return;

        com.eclipseware.imnotcheatingyouare.client.utils.remnant.Render2DEngine.activeContext = guiGraphics;
        com.eclipseware.imnotcheatingyouare.client.utils.remnant.Render2DEngine.drawRoundedRect(
                guiGraphics.pose(), windowX, windowY, windowW, TITLE_H, WINDOW_ROUNDING, BG);

        float logoH = TITLE_H - 8f;
        float logoW = logoH * ((float) LOGO_NATIVE_WIDTH / LOGO_NATIVE_HEIGHT);
        int x = (int) (windowX + 14f);
        int y = (int) (windowY + (TITLE_H - logoH) / 2f);

        guiGraphics.blit(LOGO_ID, x, y, x + (int) logoW, y + (int) logoH, 0.0f, 1.0f, 0.0f, 1.0f);
    }

    public static void render() {
        if (!open) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            open = false;
            settingsModule = null;
            return;
        }

        ImGui.getStyle().setAlpha(1.0f);
        pollBindingMouse();

        float displayW = ImGui.getIO().getDisplaySizeX();
        float displayH = ImGui.getIO().getDisplaySizeY();
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

        drawMainWindow(accentCol, accentDim, accentFaint);
        drawSettingsWindow(accentCol, accentDim, displayW);

        ImGui.popStyleColor(pushedColors);
        ImGui.popStyleVar(pushedVars);
    }

    private static void recomputeLayout(float displayW, float displayH) {
        windowW = clamp(displayW * 0.42f, 360f, 480f);
        windowH = clamp(displayH * 0.66f, 250f, 380f);
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
        float wheel = ImGui.isWindowHovered() ? ImGui.getIO().getMouseWheel() : 0f;
        float current = ImGui.getScrollY();
        float target = scrollTarget.getOrDefault(key, current);
        if (wheel != 0f) {
            target = clamp(target - wheel * 60f, 0f, ImGui.getScrollMaxY());
        }
        float dt = ImGui.getIO().getDeltaTime();
        float next = current + (target - current) * Math.min(1f, dt * 14f);
        ImGui.setScrollY(next);
        scrollTarget.put(key, target);
    }

    private static int pushStyleVars() {
        int n = 0;
        ImGui.pushStyleVar(ImGuiStyleVar.Alpha, 1.0f); n++;
        ImGui.pushStyleVar(ImGuiStyleVar.WindowRounding, 12f); n++;
        ImGui.pushStyleVar(ImGuiStyleVar.ChildRounding, 10f); n++;
        ImGui.pushStyleVar(ImGuiStyleVar.FrameRounding, 7f); n++;
        ImGui.pushStyleVar(ImGuiStyleVar.PopupRounding, 10f); n++;
        ImGui.pushStyleVar(ImGuiStyleVar.ScrollbarRounding, 5f); n++;
        ImGui.pushStyleVar(ImGuiStyleVar.ScrollbarSize, 9f); n++;
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
        ImGui.pushStyleColor(ImGuiCol.ScrollbarGrab, RenderUtils.toImGuiColor(TOGGLE_OFF, 1.0f)); n++;
        ImGui.pushStyleColor(ImGuiCol.ScrollbarGrabHovered, accentDim); n++;
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
            windowDl.addRectFilled(ImGui.getWindowPosX(), ImGui.getWindowPosY() + TITLE_H,
                    ImGui.getWindowPosX() + ImGui.getWindowWidth(), ImGui.getWindowPosY() + ImGui.getWindowHeight(),
                    RenderUtils.toImGuiColor(BG, 1.0f), WINDOW_ROUNDING, ImDrawFlags.RoundCornersBottom);

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

    private static void drawTitleBar(int accentCol, float width, boolean draggable) {
        ImDrawList dl = ImGui.getWindowDrawList();
        ImVec2 origin = ImGui.getCursorScreenPos();
        float closeSize = 18f;
        float dragWidth = width - closeSize - 22f;

        ImGui.setCursorPos(0f, 0f);
        ImGui.invisibleButton("##titledrag", Math.max(10f, dragWidth), TITLE_H);
        if (draggable && ImGui.isItemActive()) {
            windowX += ImGui.getIO().getMouseDeltaX();
            windowY += ImGui.getIO().getMouseDeltaY();
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

        dl.addLine(origin.x + 14f, origin.y + TITLE_H - 1f, origin.x + width - 14f, origin.y + TITLE_H - 1f,
                RenderUtils.toImGuiColor(TOGGLE_OFF, 1.0f), 1f);
    }

    private static void drawSidebar(int accentCol, int accentFaint) {
        ImDrawList dl = ImGui.getWindowDrawList();
        float winX = ImGui.getWindowPosX();
        float winY = ImGui.getWindowPosY();
        float winW = ImGui.getWindowWidth();
        float winH = ImGui.getWindowHeight();
        dl.addRectFilled(winX, winY, winX + winW, winY + winH, RenderUtils.toImGuiColor(SIDEBAR_BG, 1.0f), 0f);

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
            if (ImGui.isItemClicked()) selectedCategory = category;
            ImGui.popID();

            if (selected) {
                inner.addRectFilled(cursor.x, cursor.y, cursor.x + rowW, cursor.y + rowH, accentFaint, 0f);
                inner.addRectFilled(cursor.x, cursor.y, cursor.x + 2f, cursor.y + rowH, accentCol, 0f);
            } else if (hovered) {
                inner.addRectFilled(cursor.x, cursor.y, cursor.x + rowW, cursor.y + rowH,
                        RenderUtils.toImGuiColor(ROW_HOVER, 1.0f), 0f);
            }

            int tint = selected ? accentCol : RenderUtils.toImGuiColor(TEXT_DIM, 1.0f);
            drawCategoryIcon(inner, category, cursor.x + 20f, cursor.y + rowH / 2f, 5.5f, tint);
            inner.addText(cursor.x + 34f, cursor.y + (rowH - th) / 2f,
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
        String lastSubCategory = null;
        for (Module mod : mods) {
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

        ImGui.invisibleButton("##row", Math.max(10f, cardW - toggleW - 18f), rowH);
        boolean hovered = ImGui.isItemHovered();
        if (bindingModule == null && ImGui.isItemClicked()) mod.toggle();
        if (hasSettings && ImGui.isItemClicked(1)) {
            boolean opening = !isSettings;
            settingsModule = opening ? mod : null;
            if (opening) {
                settingsAnimModule = mod;
                settingsOpenNanos = System.nanoTime();
            }
        }
        if (ImGui.isItemClicked(2)) {
            bindingModule = mod;
            bindingArmed = false;
        }

        boolean binding = bindingModule == mod;

        ImDrawList dl = ImGui.getWindowDrawList();
        Color bg = hovered ? ROW_HOVER : ROW_BG;
        dl.addRectFilled(cursor.x, cursor.y, cursor.x + cardW, cursor.y + rowH,
                RenderUtils.toImGuiColor(bg, 1.0f), 7f, ImDrawFlags.RoundCornersAll);
        if (binding) {
            dl.addRect(cursor.x, cursor.y, cursor.x + cardW, cursor.y + rowH,
                    RenderUtils.toImGuiColor(255, 205, 90, 255), 7f, ImDrawFlags.RoundCornersAll, 1.5f);
        }
        if (toggled) {
            dl.addRectFilled(cursor.x, cursor.y, cursor.x + 3f, cursor.y + rowH, accentCol, 3f, ImDrawFlags.RoundCornersAll);
        }
        if (isSettings) {
            dl.addRect(cursor.x, cursor.y, cursor.x + cardW, cursor.y + rowH, accentCol, 7f, ImDrawFlags.RoundCornersAll, 1f);
        }

        float th = ImGui.getFontSize();
        int nameCol = toggled ? accentCol : RenderUtils.toImGuiColor(TEXT, 1.0f);
        float textLimit = cursor.x + cardW - toggleW - 18f;

        dl.pushClipRect(cursor.x, cursor.y, textLimit, cursor.y + rowH, true);
        if (binding) {
            dl.addText(cursor.x + 14f, cursor.y + 7f, nameCol, mod.getName());
            dl.addText(cursor.x + 14f, cursor.y + 7f + th + 2f,
                    RenderUtils.toImGuiColor(255, 205, 90, 255), "Press a key or click...");
        } else if (subtitle.isEmpty()) {
            dl.addText(cursor.x + 14f, cursor.y + (rowH - th) / 2f, nameCol, mod.getName());
        } else {
            dl.addText(cursor.x + 14f, cursor.y + 7f, nameCol, mod.getName());
            dl.addText(cursor.x + 14f, cursor.y + 7f + th + 2f,
                    RenderUtils.toImGuiColor(TEXT_DIM, 1.0f), subtitle);
        }
        dl.popClipRect();

        ImGui.sameLine(cardW - toggleW - 10f);
        ImGui.setCursorPosY(ImGui.getCursorPosY() + (rowH - 16f) / 2f);
        boolean newToggled = drawToggleSwitch("##toggle", mod.getName(), toggled, accentCol);
        if (newToggled != toggled) mod.toggle();

        ImGui.popID();
        ImGui.dummy(0f, 3f);
    }

    private static String buildSubtitle(List<Setting> settings) {
        if (settings.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (Setting s : settings) {
            if (count >= 2) break;
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

    private static void drawSettingsWindow(int accentCol, int accentDim, float displayW) {
        Module mod = settingsModule;
        if (mod == null) return;

        float panelX = windowX + windowW + 8f;
        if (panelX + SETTINGS_W > displayW - 4f) {
            panelX = Math.max(4f, windowX - SETTINGS_W - 8f);
        }

        float openEase = 1f;
        if (mod == settingsAnimModule) {
            float t = (float) ((System.nanoTime() - settingsOpenNanos) / 1_000_000_000.0 / 0.16);
            t = clamp(t, 0f, 1f);
            openEase = 1f - (1f - t) * (1f - t);
        }

        int animPushed = 0;
        if (openEase < 1f) {
            ImGui.pushStyleVar(ImGuiStyleVar.Alpha, openEase);
            animPushed++;
            panelX -= (1f - openEase) * 16f;
        }

        ImGui.setNextWindowPos(panelX, windowY, ImGuiCond.Always);
        ImGui.setNextWindowSize(SETTINGS_W, windowH, ImGuiCond.Always);

        if (ImGui.begin("##marlow_settings", WINDOW_FLAGS)) {
            IMGUI.setWindowFontScale(FONT_SCALE);

            ImDrawList dl = ImGui.getWindowDrawList();
            dl.addRectFilled(ImGui.getWindowPosX(), ImGui.getWindowPosY(),
                    ImGui.getWindowPosX() + ImGui.getWindowWidth(), ImGui.getWindowPosY() + ImGui.getWindowHeight(),
                    RenderUtils.toImGuiColor(BG, 1.0f), WINDOW_ROUNDING, ImDrawFlags.RoundCornersAll);

            ImVec2 origin = ImGui.getCursorScreenPos();
            float th = ImGui.getFontSize();
            float closeSize = 18f;

            boolean toggled = mod.isToggled();
            dl.addText(origin.x + 16f, origin.y + (TITLE_H - th) / 2f,
                    toggled ? accentCol : RenderUtils.toImGuiColor(TEXT, 1.0f), mod.getName());

            ImGui.setCursorPos(SETTINGS_W - closeSize - 14f, (TITLE_H - closeSize) / 2f);
            ImVec2 closePos = ImGui.getCursorScreenPos();
            ImGui.invisibleButton("##sclose", closeSize, closeSize);
            boolean closeHovered = ImGui.isItemHovered();
            if (ImGui.isItemClicked()) settingsModule = null;

            int closeBg = closeHovered ? RenderUtils.toImGuiColor(214, 70, 70, 230) : RenderUtils.toImGuiColor(ROW_BG, 1.0f);
            dl.addRectFilled(closePos.x, closePos.y, closePos.x + closeSize, closePos.y + closeSize, closeBg, 6f, ImDrawFlags.RoundCornersAll);
            int xCol = RenderUtils.toImGuiColor(TEXT, 1.0f);
            float p = 6f;
            dl.addLine(closePos.x + p, closePos.y + p, closePos.x + closeSize - p, closePos.y + closeSize - p, xCol, 1.5f);
            dl.addLine(closePos.x + closeSize - p, closePos.y + p, closePos.x + p, closePos.y + closeSize - p, xCol, 1.5f);

            dl.addLine(origin.x + 14f, origin.y + TITLE_H - 1f, origin.x + SETTINGS_W - 14f, origin.y + TITLE_H - 1f,
                    RenderUtils.toImGuiColor(TOGGLE_OFF, 1.0f), 1f);

            ImGui.setCursorPos(14f, TITLE_H + 10f);
            ImGui.beginChild("##settingsscroll", SETTINGS_W - 28f, windowH - TITLE_H - 10f - BODY_BOTTOM_MARGIN, false,
                    ImGuiWindowFlags.AlwaysVerticalScrollbar);
            IMGUI.setWindowFontScale(FONT_SCALE);

            List<Setting> settings = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingsByMod(mod);
            for (Setting setting : settings) {
                renderSetting(setting, accentCol);
            }

            ImGui.endChild();
        }
        ImGui.end();

        if (animPushed > 0) ImGui.popStyleVar(animPushed);
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
        int bg = value ? accentCol : RenderUtils.toImGuiColor(TOGGLE_OFF, 1.0f);
        dl.addRectFilled(cursor.x, cursor.y, cursor.x + w, cursor.y + h, bg, h / 2f, ImDrawFlags.RoundCornersAll);

        float knobR = h / 2f - 2.2f;
        float knobCx = cursor.x + h / 2f + (w - h) * current;
        dl.addCircleFilled(knobCx, cursor.y + h / 2f, knobR, RenderUtils.toImGuiColor(255, 255, 255, 255));

        return clicked ? !value : value;
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
        dl.addRectFilled(cursor.x, trackY, cursor.x + avail * drawFrac, trackY + trackH, accentCol, trackH / 2f, ImDrawFlags.RoundCornersAll);
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
        } else if (s.isCombo()) {
            ImGui.textDisabled(s.getName());
            ImGui.dummy(0f, 3f);
            ImGui.setNextItemWidth(ImGui.getContentRegionAvailX());
            ImVec2 comboPos = ImGui.getCursorScreenPos();
            ImGui.setNextWindowPos(comboPos.x, comboPos.y + ImGui.getFrameHeight() + 3f, ImGuiCond.Always);
            if (ImGui.beginCombo("##v", s.getValString())) {
                for (String opt : s.getOptions()) {
                    boolean selected = opt.equals(s.getValString());
                    if (ImGui.selectable(opt, selected)) s.setValString(opt);
                    if (selected) ImGui.setItemDefaultFocus();
                }
                ImGui.endCombo();
            }
            ImGui.dummy(0f, 9f);
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
