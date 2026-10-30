package com.eclipseware.imnotcheatingyouare.client.clickgui;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.gui.MenuConfigScreen;
import com.eclipseware.imnotcheatingyouare.client.gui.PastelShaderBackground;
import com.eclipseware.imnotcheatingyouare.client.macro.Macro;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.module.ModuleManager;
import com.eclipseware.imnotcheatingyouare.client.module.impl.GUI;
import com.eclipseware.imnotcheatingyouare.client.module.impl.Menu;
import com.eclipseware.imnotcheatingyouare.client.setting.ConfigManager;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.setting.SettingsManager;
import com.eclipseware.imnotcheatingyouare.client.utils.ImGuiTextures;
import com.eclipseware.imnotcheatingyouare.client.utils.InputUtil;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.textures.GpuTexture;
import imgui.ImDrawList;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.ImVec2;
import imgui.flag.ImDrawFlags;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiHoveredFlags;
import imgui.flag.ImGuiMouseButton;
import imgui.flag.ImGuiStyleVar;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;
import imgui.type.ImString;
import java.awt.Color;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import xyz.breadloaf.imguimc.screen.EmptyScreen;

public class ImGuiClickGui {

    private static final float BASE_WIN_W = 684f;
    private static final float BASE_WIN_H = 504f;
    private static final float SIDEBAR_W = 178f;
    private static final float HEADER_H = 44f;
    private static final float SUBBAR_H = 34f;
    private static final float ROW_H = 44f;
    private static final float ROW_GAP = 5f;
    private static final float SECTION_H = 24f;
    private static final float DRAWER_W = 284f;
    private static final float WIN_ROUND = 12f;
    private static final float CARD_ROUND = 8f;

    private static final float TITLE_FONT_SCALE = 0.92f;
    private static final float BODY_FONT_SCALE = 0.85f;
    private static final float SUB_FONT_SCALE = 0.76f;

    private static final Identifier LOGO_ID = Identifier.parse("imnotcheatingyouare:textures/logo.png");
    private static final int LOGO_NATIVE_WIDTH = 916;
    private static final int LOGO_NATIVE_HEIGHT = 272;

    private static boolean open = false;
    private static boolean menuMode = false;
    private static GuiTaxonomy.Group selectedGroup = GuiTaxonomy.Group.COMBAT;
    private static String selectedSubCategory = "All";
    private static GuiTaxonomy.Group previousGroupBeforeConfig = GuiTaxonomy.Group.COMBAT;
    private static String previousSubBeforeConfig = "All";

    private static final Map<GuiTaxonomy.Group, Boolean> expandedGroups = new EnumMap<>(GuiTaxonomy.Group.class);
    private static final Map<GuiTaxonomy.Group, Float> groupExpandAnim = new EnumMap<>(GuiTaxonomy.Group.class);
    private static Module inspectingModule = null;
    private static Module drawerRenderModule = null;
    private static Module bindingModule = null;
    private static long bindingConsumedFrame = -1L;
    private static long frameCounter = 0L;
    private static final boolean[] prevBindMouseDown = new boolean[8];

    private static String searchQueryDraft = "";
    private static ImString searchBuffer = null;
    private static boolean focusSearchNextFrame = false;
    private static final Map<Setting, ImString> textSettingBuffers = new HashMap<>();
    private static Setting expandedColorSetting = null;
    private static final Map<Setting, float[]> hsvCache = new HashMap<>();
    private static final Map<Module, Float> toggleAnimMap = new HashMap<>();
    private static final Map<Setting, Float> checkAnimMap = new HashMap<>();
    private static final Map<Setting, Float> sliderAnimMap = new HashMap<>();
    private static final Map<Setting, Float> rangeLoAnimMap = new HashMap<>();
    private static final Map<Setting, Float> rangeHiAnimMap = new HashMap<>();
    private static final Map<Setting, Float> colorExpandAnimMap = new HashMap<>();
    private static final Map<String, Float> hoverAnimMap = new HashMap<>();
    private static final Map<String, Float> scrollTargetMap = new HashMap<>();
    private static final Map<String, Float> scrollAppliedMap = new HashMap<>();

    private static float drawerAnim = 0f;
    private static float slidingChipX = -1f;
    private static float slidingChipW = 0f;
    private static GuiTaxonomy.Group lastChipGroup = null;

    private static float windowX = Float.NaN;
    private static float windowY = Float.NaN;
    private static float lastDispW = -1f;
    private static float lastDispH = -1f;

    private static final long startEpochNs = System.nanoTime();
    private static long lastFrameNs = System.nanoTime();
    private static long openedNanos = System.nanoTime();
    private static long viewChangedNanos = System.nanoTime();
    private static long drawerOpenedNanos = System.nanoTime();
    private static float currentDt = 1f / 60f;
    private static float speedScale = 1.0f;

    private static int accentR = 255;
    private static int accentG = 118;
    private static int accentB = 26;
    private static int bgAlpha = 242;
    private static float animSpeed = 6.0f;
    private static boolean glowEnabled = true;
    private static float glassOpacity = 0.72f;

    private static int shaderTexture = 0;
    private static int logoTexture = 0;
    private static float shaderWinX = 0f;
    private static float shaderWinY = 0f;
    private static float shaderWinW = BASE_WIN_W;
    private static float shaderWinH = BASE_WIN_H;

    private static ImGui imguiInstance;
    private static final ImVec2 tmpTextSize = new ImVec2();

    private static final ImGuiConfigPanel.DrawCallbacks CONFIG_DRAW_CB = new ImGuiConfigPanel.DrawCallbacks() {
        @Override
        public int col(int r, int g, int b, int a) {
            return ImGuiClickGui.col(r, g, b, a);
        }

        @Override
        public int getAccentR() {
            return accentR;
        }

        @Override
        public int getAccentG() {
            return accentG;
        }

        @Override
        public int getAccentB() {
            return accentB;
        }

        @Override
        public String keyName(int keyCode) {
            return ImGuiClickGui.keyName(keyCode);
        }

        @Override
        public float textW(String text) {
            return ImGuiClickGui.textW(text);
        }

        @Override
        public float textH() {
            return ImGuiClickGui.textH();
        }

        @Override
        public void applyFontScale() {
            ImGuiClickGui.setFontScale(BODY_FONT_SCALE);
        }

        @Override
        public void useTitleFont() {
            ImGuiClickGui.setFontScale(TITLE_FONT_SCALE);
        }

        @Override
        public void useBodyFont() {
            ImGuiClickGui.setFontScale(BODY_FONT_SCALE);
        }

        @Override
        public void useSubFont() {
            ImGuiClickGui.setFontScale(SUB_FONT_SCALE);
        }

        @Override
        public float animateHover(String key, boolean hovered) {
            return ImGuiClickGui.animateHover(key, hovered, 12f);
        }

        @Override
        public void applySmoothScroll(String key) {
            ImGuiClickGui.applySmoothScroll(key);
        }

        @Override
        public int lerpColor(int c0, int c1, float t) {
            return ImGuiClickGui.lerpColor(c0, c1, t);
        }

        @Override
        public String clipText(String text, float maxW) {
            return ImGuiClickGui.clipTextToWidth(text, maxW);
        }
    };

    static {
        initDefaultExpansion();
    }

    private static void initDefaultExpansion() {
        for (GuiTaxonomy.Group g : GuiTaxonomy.Group.values()) {
            boolean exp = (g == GuiTaxonomy.Group.COMBAT);
            expandedGroups.put(g, exp);
            groupExpandAnim.put(g, exp ? 1f : 0f);
        }
    }

    public static void resetStateForTests() {
        open = false;
        menuMode = false;
        selectedGroup = GuiTaxonomy.Group.COMBAT;
        selectedSubCategory = "All";
        previousGroupBeforeConfig = GuiTaxonomy.Group.COMBAT;
        previousSubBeforeConfig = "All";
        inspectingModule = null;
        drawerRenderModule = null;
        bindingModule = null;
        bindingConsumedFrame = -1L;
        frameCounter = 0L;
        searchQueryDraft = "";
        focusSearchNextFrame = false;
        if (searchBuffer != null) {
            searchBuffer.set("");
        }
        initDefaultExpansion();
        ImGuiConfigPanel.setBindingMacro(null);
    }

    public static boolean isOpen() {
        return open;
    }

    public static boolean isMenuMode() {
        return menuMode;
    }

    public static void openMenuMode() {
        menuMode = true;
        open = true;
        openedNanos = System.nanoTime();
        viewChangedNanos = openedNanos;
    }

    public static void setOpen(boolean value) {
        if (open == value) {
            return;
        }
        open = value;
        if (!value) {
            menuMode = false;
            bindingModule = null;
            ImGuiConfigPanel.setBindingMacro(null);
            GUI.notifyGuiClosed();
            saveConfigSafely();
        } else {
            openedNanos = System.nanoTime();
            viewChangedNanos = openedNanos;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) {
            return;
        }
        if (open) {
            if (!(mc.screen instanceof EmptyScreen) && !(mc.screen instanceof MenuConfigScreen)) {
                mc.setScreen(new EmptyScreen());
            }
        } else {
            if (mc.screen instanceof EmptyScreen) {
                mc.setScreen(null);
            }
        }
    }

    public static void toggle() {
        setOpen(!open);
    }

    public static void markClosed() {
        if (open) {
            saveConfigSafely();
        }
        open = false;
        menuMode = false;
        bindingModule = null;
        ImGuiConfigPanel.setBindingMacro(null);
    }

    public static boolean isBinding() {
        return bindingModule != null || ImGuiConfigPanel.isMacroBinding();
    }

    public static boolean isCapturingMouseBind() {
        return isBinding() && bindingConsumedFrame != frameCounter;
    }

    public static Module getBindingModule() {
        return bindingModule;
    }

    public static void startBinding(Module module) {
        bindingModule = module;
        bindingConsumedFrame = frameCounter;
    }

    public static void clearModuleKeybind(Module module) {
        if (module == null) {
            return;
        }
        module.setKeyBind(0);
        if (bindingModule == module) {
            bindingModule = null;
        }
        bindingConsumedFrame = frameCounter;
        saveConfigSafely();
    }

    public static boolean handleBindingKey(int keyCode) {
        if (ImGuiConfigPanel.isMacroBinding()) {
            return ImGuiConfigPanel.handleMacroBindingKey(keyCode, frameCounter);
        }
        if (bindingModule == null) {
            return false;
        }
        if (keyCode == InputConstants.KEY_ESCAPE
            || keyCode == InputConstants.KEY_DELETE
            || keyCode == InputConstants.KEY_BACKSPACE) {
            bindingModule.setKeyBind(0);
        } else if (keyCode > 0) {
            bindingModule.setKeyBind(keyCode);
        }
        bindingModule = null;
        bindingConsumedFrame = frameCounter;
        saveConfigSafely();
        return true;
    }

    public static boolean isBindableMouseButton(int glfwMouseButton) {
        return glfwMouseButton == 2
            || glfwMouseButton == 3
            || glfwMouseButton == 4
            || (glfwMouseButton >= 2 && glfwMouseButton <= 7);
    }

    public static int toMouseBindCode(int glfwMouseButton) {
        return -(glfwMouseButton + 1);
    }

    public static boolean handleBindingMouseButton(int glfwMouseButton) {
        int bindCode = toMouseBindCode(glfwMouseButton);
        if (ImGuiConfigPanel.isMacroBinding()) {
            if (glfwMouseButton == 0 || glfwMouseButton == 1) {
                ImGuiConfigPanel.setBindingMacro(null);
                bindingConsumedFrame = frameCounter;
                return true;
            }
            if (isBindableMouseButton(glfwMouseButton)) {
                boolean bound = ImGuiConfigPanel.handleMacroBindingMouseButton(bindCode, frameCounter);
                bindingConsumedFrame = frameCounter;
                return bound;
            }
            return false;
        }
        if (bindingModule == null) {
            return false;
        }
        if (glfwMouseButton == 0 || glfwMouseButton == 1) {
            bindingModule = null;
            bindingConsumedFrame = frameCounter;
            return true;
        }
        if (isBindableMouseButton(glfwMouseButton)) {
            bindingModule.setKeyBind(bindCode);
            bindingModule = null;
            bindingConsumedFrame = frameCounter;
            saveConfigSafely();
            return true;
        }
        return false;
    }

    public static boolean onScreenMouseClicked(int glfwMouseButton) {
        if (!isBinding()) {
            return false;
        }
        if (isBindableMouseButton(glfwMouseButton)) {
            return handleBindingMouseButton(glfwMouseButton);
        }
        return false;
    }

    public static boolean onScreenKeyPressed(int keyCode, Runnable closeAction) {
        if (isBinding()) {
            return handleBindingKey(keyCode);
        }
        if (keyCode == InputConstants.KEY_ESCAPE) {
            if (!searchQueryDraft.isEmpty()) {
                setSearchQuery("");
                return true;
            }
            if (expandedColorSetting != null) {
                expandedColorSetting = null;
                return true;
            }
            if (inspectingModule != null) {
                inspectingModule = null;
                return true;
            }
            if (closeAction != null) {
                closeAction.run();
            } else {
                setOpen(false);
            }
            return true;
        }
        int menuKey = getMenuModuleKeybind();
        if (keyCode == menuKey && !menuMode) {
            if (closeAction != null) {
                closeAction.run();
            } else {
                setOpen(false);
            }
            return true;
        }
        return false;
    }

    public static GuiTaxonomy.Group getSelectedGroup() {
        return selectedGroup;
    }

    private static void expandOnlyGroup(GuiTaxonomy.Group target) {
        for (GuiTaxonomy.Group g : GuiTaxonomy.Group.values()) {
            expandedGroups.put(g, g == target);
        }
    }

    public static void selectGroup(GuiTaxonomy.Group group) {
        if (group == null) {
            return;
        }
        if (selectedGroup == group) {
            boolean currentlyExpanded = isGroupExpanded(group);
            if (currentlyExpanded) {
                expandedGroups.put(group, false);
            } else {
                expandOnlyGroup(group);
                selectedSubCategory = group.getDefaultSubCategory();
                viewChangedNanos = System.nanoTime();
            }
            return;
        }
        selectedGroup = group;
        expandOnlyGroup(group);
        selectedSubCategory = group.getDefaultSubCategory();
        viewChangedNanos = System.nanoTime();
    }

    public static String getSelectedSubCategory() {
        return selectedSubCategory;
    }

    public static void selectSubCategory(String subCategory) {
        if (subCategory == null || subCategory.isEmpty()) {
            return;
        }
        List<String> valid = selectedGroup.getSubCategories();
        for (String s : valid) {
            if (s.equalsIgnoreCase(subCategory)) {
                if (!s.equalsIgnoreCase(selectedSubCategory)) {
                    viewChangedNanos = System.nanoTime();
                }
                selectedSubCategory = s;
                expandOnlyGroup(selectedGroup);
                return;
            }
        }
        selectedSubCategory = valid.get(0);
        viewChangedNanos = System.nanoTime();
    }

    public static boolean isGroupExpanded(GuiTaxonomy.Group group) {
        Boolean val = expandedGroups.get(group);
        return val != null && val;
    }

    public static void toggleGroupExpanded(GuiTaxonomy.Group group) {
        if (group == null) {
            return;
        }
        if (isGroupExpanded(group)) {
            expandedGroups.put(group, false);
        } else {
            expandOnlyGroup(group);
        }
    }

    public static void openConfigCenter() {
        viewChangedNanos = System.nanoTime();
        if (isConfigCenterActive()) {
            selectedGroup = previousGroupBeforeConfig;
            selectedSubCategory = previousSubBeforeConfig;
            expandOnlyGroup(selectedGroup);
            return;
        }
        previousGroupBeforeConfig = selectedGroup;
        previousSubBeforeConfig = selectedSubCategory;
        selectedGroup = GuiTaxonomy.Group.CLIENT;
        selectedSubCategory = "Config Center";
        expandOnlyGroup(GuiTaxonomy.Group.CLIENT);
    }

    public static boolean isConfigCenterActive() {
        return selectedGroup == GuiTaxonomy.Group.CLIENT
            && "Config Center".equalsIgnoreCase(selectedSubCategory);
    }

    public static Module getInspectingModule() {
        return inspectingModule;
    }

    public static void setInspectingModule(Module module) {
        if (module != null && module != inspectingModule) {
            drawerOpenedNanos = System.nanoTime();
            drawerRenderModule = module;
        }
        inspectingModule = module;
    }

    public static String getSearchQuery() {
        return searchQueryDraft;
    }

    public static void setSearchQuery(String query) {
        String next = query != null ? query : "";
        if (!next.equals(searchQueryDraft)) {
            viewChangedNanos = System.nanoTime();
        }
        searchQueryDraft = next;
        if (searchBuffer != null) {
            searchBuffer.set(searchQueryDraft);
        }
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) {
            return;
        }
        if (open && mc.screen == null) {
            open = false;
            menuMode = false;
            bindingModule = null;
            ImGuiConfigPanel.setBindingMacro(null);
            saveConfigSafely();
        }
    }

    public static void render() {
        if (!open) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) {
            return;
        }
        if (!(mc.screen instanceof EmptyScreen) && !(mc.screen instanceof MenuConfigScreen)) {
            open = false;
            menuMode = false;
            return;
        }

        frameCounter++;
        long nowNs = System.nanoTime();
        currentDt = Math.min(0.05f, Math.max(0.001f, (nowNs - lastFrameNs) / 1_000_000_000.0f));
        lastFrameNs = nowNs;

        syncThemeSettings();
        ensureSearchBuffer();
        speedScale = clamp(animSpeed / 6.0f, 0.55f, 1.65f);

        boolean specialPanelOpen = searchQueryDraft.trim().isEmpty()
            && GuiTaxonomy.isSpecialClientPanel(selectedGroup, selectedSubCategory);
        if (inspectingModule != null && !specialPanelOpen) {
            drawerRenderModule = inspectingModule;
        }
        float drawerTarget = (inspectingModule != null && !specialPanelOpen) ? 1f : 0f;
        drawerAnim = damp(drawerAnim, drawerTarget, 10.5f, currentDt);
        if (drawerAnim < 0.003f && drawerTarget == 0f) {
            drawerAnim = 0f;
            drawerRenderModule = null;
        }

        ImGuiIO io = ImGui.getIO();
        float dispW = io.getDisplaySizeX();
        float dispH = io.getDisplaySizeY();
        if (dispW <= 0f || dispH <= 0f) {
            return;
        }

        shaderTexture = PastelShaderBackground.render() ? PastelShaderBackground.glTextureId() : 0;
        if (shaderTexture != 0) {
            ImGuiTextures.prepare(shaderTexture, true);
        }
        logoTexture = resolveLogoTexture();

        renderViewportBackdrop(dispW, dispH);
        pollBindingMouse();

        float openEase = easeOutCubic((secondsSince(openedNanos) * speedScale) / 0.30f);
        ImGui.pushStyleVar(ImGuiStyleVar.Alpha, Math.max(0.02f, openEase));

        String layoutMode = getGuiLayoutMode();
        if ("Vertical".equalsIgnoreCase(layoutMode)) {
            renderVerticalColumnsLayout(dispW, dispH, currentDt, openEase);
        } else {
            renderCompactDashboardLayout(dispW, dispH, currentDt, openEase);
        }

        ImGui.popStyleVar();
    }

    private static int resolveLogoTexture() {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null) {
                return 0;
            }
            TextureManager tm = mc.getTextureManager();
            AbstractTexture tex = tm.getTexture(LOGO_ID);
            if (tex != null) {
                GpuTexture gpu = tex.getTexture();
                if (gpu instanceof GlTexture gl) {
                    return ImGuiTextures.prepare(gl.glId(), true);
                }
            }
        } catch (Throwable ignored) {
        }
        return 0;
    }

    private static void renderViewportBackdrop(float dispW, float dispH) {
        ImDrawList bgDl = ImGui.getBackgroundDrawList();
        if (menuMode) {
            if (Menu.shouldReplacePanorama() && shaderTexture != 0) {
                bgDl.addImage(shaderTexture, 0f, 0f, dispW, dispH, 0f, 1f, 1f, 0f, col(255, 255, 255, 255));
            }
            bgDl.addRectFilled(0f, 0f, dispW, dispH, col(12, 9, 20, 110));
        } else if (glowEnabled) {
            if (shaderTexture != 0) {
                bgDl.addImage(shaderTexture, 0f, 0f, dispW, dispH, 0f, 1f, 1f, 0f, col(255, 255, 255, 42));
            }
            bgDl.addRectFilled(0f, 0f, dispW, dispH, col(10, 8, 16, 95));
        }
    }

    private static void renderCompactDashboardLayout(float dispW, float dispH, float dt, float openEase) {
        float baseW = clamp(dispW * 0.50f, 620f, BASE_WIN_W);
        float baseH = clamp(dispH * 0.64f, 420f, BASE_WIN_H);
        float drawerWidthVisible = drawerAnim > 0.004f ? (DRAWER_W * drawerAnim) : 0f;
        float winW = Math.min(dispW - 16f, baseW + drawerWidthVisible);
        float winH = Math.min(dispH - 16f, baseH);

        if (Float.isNaN(windowX) || Float.isNaN(windowY)
            || Math.abs(dispW - lastDispW) > 1f || Math.abs(dispH - lastDispH) > 1f) {
            windowX = (dispW - baseW) * 0.5f;
            windowY = (dispH - winH) * 0.5f;
            lastDispW = dispW;
            lastDispH = dispH;
        }
        if (menuMode) {
            windowX = (dispW - winW) * 0.5f;
            windowY = (dispH - winH) * 0.5f;
        }

        windowX = clamp(windowX, 8f, Math.max(8f, dispW - winW - 8f));
        windowY = clamp(windowY, 8f, Math.max(8f, dispH - winH - 8f));

        float slideY = (1f - openEase) * 16f;
        float drawWinX = windowX;
        float drawWinY = windowY + slideY;

        int flags = ImGuiWindowFlags.NoTitleBar
            | ImGuiWindowFlags.NoResize
            | ImGuiWindowFlags.NoMove
            | ImGuiWindowFlags.NoScrollbar
            | ImGuiWindowFlags.NoScrollWithMouse
            | ImGuiWindowFlags.NoCollapse
            | ImGuiWindowFlags.NoSavedSettings
            | ImGuiWindowFlags.NoBackground;

        ImGui.setNextWindowPos(drawWinX, drawWinY, ImGuiCond.Always);
        ImGui.setNextWindowSize(winW, winH, ImGuiCond.Always);

        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, 0f, 0f);
        ImGui.pushStyleVar(ImGuiStyleVar.WindowBorderSize, 0f);
        ImGui.pushStyleVar(ImGuiStyleVar.ScrollbarSize, 5f);
        ImGui.pushStyleVar(ImGuiStyleVar.FrameRounding, 6f);
        ImGui.pushStyleColor(ImGuiCol.ScrollbarBg, col(12, 10, 19, 80));
        ImGui.pushStyleColor(ImGuiCol.ScrollbarGrab, col(accentR, accentG, accentB, 130));
        ImGui.pushStyleColor(ImGuiCol.ScrollbarGrabHovered, col(accentR, accentG, accentB, 190));
        ImGui.pushStyleColor(ImGuiCol.ScrollbarGrabActive, col(accentR, accentG, accentB, 240));

        ImGui.begin("##EclipseWareMainGui", new ImBoolean(true), flags);
        setFontScale(BODY_FONT_SCALE);

        float wx = drawWinX;
        float wy = drawWinY;
        shaderWinX = wx;
        shaderWinY = wy;
        shaderWinW = winW;
        shaderWinH = winH;

        ImDrawList dl = ImGui.getWindowDrawList();

        drawWindowShell(dl, wx, wy, winW, winH);
        renderTopHeader(dl, wx, wy, winW);

        float bodyY = wy + HEADER_H;
        float bodyH = winH - HEADER_H;
        renderSidebar(dl, wx, bodyY, SIDEBAR_W, bodyH, dt);

        float mainX = wx + SIDEBAR_W;
        float mainW = Math.max(260f, winW - SIDEBAR_W - drawerWidthVisible);

        renderMainContentArea(dl, mainX, bodyY, mainW, bodyH, dt);

        if (drawerWidthVisible > 1f && drawerRenderModule != null) {
            float drawerX = wx + winW - drawerWidthVisible;
            renderSettingsDrawer(dl, drawerX, bodyY, DRAWER_W, drawerWidthVisible, bodyH, dt);
        }

        ImGui.end();
        ImGui.popStyleColor(4);
        ImGui.popStyleVar(4);
    }

    private static void drawWindowShell(ImDrawList dl, float x, float y, float w, float h) {
        float time = elapsedSeconds();
        if (glowEnabled) {
            float breathe = 0.5f + 0.5f * (float) Math.sin(time * 1.6f);
            for (int i = 3; i >= 1; i--) {
                float expand = i * 4.2f + breathe * 2.2f;
                int glowAlpha = (int) ((16f + breathe * 9f) / i);
                int glowCol = (i == 2)
                    ? col(162, 68, 255, glowAlpha)
                    : col(accentR, accentG, accentB, glowAlpha);
                dl.addRect(
                    x - expand,
                    y - expand,
                    x + w + expand,
                    y + h + expand,
                    glowCol,
                    WIN_ROUND + expand,
                    0,
                    2.0f
                );
            }
        }
        dl.addRectFilled(x + 3f, y + 6f, x + w + 3f, y + h + 8f, col(0, 0, 0, 78), WIN_ROUND + 2f);
        dl.addRectFilled(x, y, x + w, y + h, col(14, 9, 22, bgAlpha), WIN_ROUND);
        shaderFill(dl, x, y, x + w, y + h, (int) (108 * glassOpacity), WIN_ROUND, 0);
        dl.addRectFilled(x, y, x + w, y + h, col(13, 8, 20, 158), WIN_ROUND);

        drawSpookyEmbers(dl, x, y, w, h, time);
        drawCornerCobweb(dl, x + 2f, y + HEADER_H + 1f, 36f, time, false);
        drawCornerCobweb(dl, x + w - 2f, y + HEADER_H + 1f, 34f, time, true);

        float pulse = 0.55f + 0.45f * (float) Math.sin(time * 2.2f);
        int borderA = (int) (85f + 50f * pulse);
        int borderCol = lerpColor(col(accentR, accentG, accentB, borderA), col(175, 82, 255, borderA), 0.28f * (1f - pulse));
        dl.addRect(x, y, x + w, y + h, borderCol, WIN_ROUND, 0, 1.3f);
    }

    private static void drawCornerCobweb(ImDrawList dl, float cx, float cy, float radius, float time, boolean flipX) {
        int webCol = col(235, 215, 255, 34);
        int webHi = col(accentR, accentG, accentB, 42);
        float dirX = flipX ? -1f : 1f;
        int spokes = 5;
        for (int i = 0; i < spokes; i++) {
            double ang = (Math.PI * 0.5 * i) / (spokes - 1);
            float sx = cx + dirX * (float) Math.cos(ang) * radius;
            float sy = cy + (float) Math.sin(ang) * radius;
            dl.addLine(cx, cy, sx, sy, (i % 2 == 0) ? webCol : webHi, 1.0f);
        }
        for (int ring = 1; ring <= 3; ring++) {
            float r = radius * (ring / 3.2f);
            for (int i = 0; i < spokes - 1; i++) {
                double a0 = (Math.PI * 0.5 * i) / (spokes - 1);
                double a1 = (Math.PI * 0.5 * (i + 1)) / (spokes - 1);
                float x0 = cx + dirX * (float) Math.cos(a0) * r;
                float y0 = cy + (float) Math.sin(a0) * r;
                float x1 = cx + dirX * (float) Math.cos(a1) * r;
                float y1 = cy + (float) Math.sin(a1) * r;
                dl.addLine(x0, y0, x1, y1, webCol, 0.9f);
            }
        }
        if (!flipX) {
            float drop = 14f + 5f * (float) Math.sin(time * 1.9f);
            float spX = cx + 15f;
            float spY = cy + drop;
            dl.addLine(spX, cy, spX, spY, col(235, 215, 255, 48), 0.9f);
            dl.addCircleFilled(spX, spY, 2.2f, col(20, 12, 28, 230));
            dl.addCircleFilled(spX, spY, 1.4f, col(accentR, accentG, accentB, 220));
        }
    }

    private static void drawSpookyEmbers(ImDrawList dl, float x, float y, float w, float h, float time) {
        dl.pushClipRect(x + 2f, y + 2f, x + w - 2f, y + h - 2f, true);
        for (int i = 0; i < 14; i++) {
            float seed = i * 37.17f;
            float px = x + ((seed * 19.3f + (float) Math.sin(time * 0.9f + i) * 18f) % Math.max(20f, w));
            if (px < x) {
                px += w;
            }
            float cycle = (time * (18f + (i % 5) * 4f) + seed * 11.7f) % Math.max(20f, h);
            float py = y + h - cycle;
            float flicker = 0.45f + 0.55f * (float) Math.sin(time * 3.8f + seed);
            int alpha = (int) (52f * flicker * glassOpacity);
            int emberCol = (i % 3 == 0)
                ? col(175, 88, 255, alpha)
                : col(255, 138, 36, alpha);
            float rad = 1.4f + (i % 3) * 0.7f;
            dl.addCircleFilled(px, py, rad, emberCol);
        }
        dl.popClipRect();
    }

    private static void drawMiniJackOLantern(ImDrawList dl, float cx, float cy, float r, float time) {
        float flicker = 0.65f + 0.35f * (float) Math.sin(time * 5.2f);
        int pumpkinDark = col(205, 74, 14, 245);
        int pumpkinBright = col(255, 124, 24, 255);
        int eyeGlow = col(255, (int) (210 + 40 * flicker), 64, 255);
        int stemCol = col(92, 168, 62, 245);
        dl.addRectFilled(cx - 1.2f, cy - r - 2.6f, cx + 1.4f, cy - r + 0.8f, stemCol, 1f);
        dl.addCircleFilled(cx - r * 0.36f, cy, r * 0.82f, pumpkinDark);
        dl.addCircleFilled(cx + r * 0.36f, cy, r * 0.82f, pumpkinDark);
        dl.addCircleFilled(cx, cy, r * 0.92f, pumpkinBright);
        dl.addTriangleFilled(
            cx - r * 0.52f, cy - r * 0.08f,
            cx - r * 0.16f, cy - r * 0.08f,
            cx - r * 0.34f, cy - r * 0.42f,
            eyeGlow
        );
        dl.addTriangleFilled(
            cx + r * 0.16f, cy - r * 0.08f,
            cx + r * 0.52f, cy - r * 0.08f,
            cx + r * 0.34f, cy - r * 0.42f,
            eyeGlow
        );
        dl.addLine(cx - r * 0.45f, cy + r * 0.28f, cx, cy + r * 0.46f, eyeGlow, 1.3f);
        dl.addLine(cx, cy + r * 0.46f, cx + r * 0.45f, cy + r * 0.28f, eyeGlow, 1.3f);
    }

    private static void renderTopHeader(ImDrawList dl, float wx, float wy, float winW) {
        dl.addRectFilled(
            wx,
            wy,
            wx + winW,
            wy + HEADER_H,
            col(11, 7, 18, 242),
            WIN_ROUND,
            ImDrawFlags.RoundCornersTop
        );
        shaderFill(
            dl,
            wx,
            wy,
            wx + winW,
            wy + HEADER_H,
            (int) (84 * glassOpacity),
            WIN_ROUND,
            ImDrawFlags.RoundCornersTop
        );

        float time = elapsedSeconds();
        float sweep = (((time + wx * 0.002f) * 0.28f) % 1.8f) - 0.4f;
        float bandW = winW * 0.24f;
        float bx = wx + sweep * winW;
        dl.pushClipRect(wx + WIN_ROUND, wy, wx + winW - WIN_ROUND, wy + HEADER_H, true);
        int shimmerClear = col(accentR, accentG, accentB, 0);
        int shimmerGlow = col(accentR, accentG, accentB, 38);
        dl.addRectFilledMultiColor(bx - bandW, wy, bx, wy + HEADER_H, shimmerClear, shimmerGlow, shimmerGlow, shimmerClear);
        dl.addRectFilledMultiColor(bx, wy, bx + bandW, wy + HEADER_H, shimmerGlow, shimmerClear, shimmerClear, shimmerGlow);
        dl.popClipRect();

        if (logoTexture != 0) {
            float logoH = 24f;
            float logoW = logoH * ((float) LOGO_NATIVE_WIDTH / LOGO_NATIVE_HEIGHT);
            float lx = wx + (SIDEBAR_W - logoW) * 0.5f;
            float ly = wy + (HEADER_H - logoH) * 0.5f;
            dl.addImage(logoTexture, lx, ly, lx + logoW, ly + logoH);
        } else {
            float logoX = wx + 14f;
            float logoCy = wy + HEADER_H * 0.5f;
            drawMiniJackOLantern(dl, logoX + 7f, logoCy, 6.2f, time);
            setFontScale(TITLE_FONT_SCALE);
            dl.addText(logoX + 20f, wy + (HEADER_H - textH()) * 0.5f, col(255, 242, 230, 255), "ECLIPSEWARE");
        }

        float pumpkinCx = wx + SIDEBAR_W + 18f;
        drawMiniJackOLantern(dl, pumpkinCx, wy + HEADER_H * 0.5f, 5.8f, time);

        float crumbX = wx + SIDEBAR_W + 30f;
        setFontScale(TITLE_FONT_SCALE);
        String grpLabel = selectedGroup.getLabel();
        float grpY = wy + (HEADER_H - textH()) * 0.5f;
        dl.addText(crumbX, grpY, col(255, 244, 234, 255), grpLabel);
        float grpW = textW(grpLabel);

        setFontScale(BODY_FONT_SCALE);
        String subCrumb = "  /  " + selectedSubCategory;
        dl.addText(crumbX + grpW, wy + (HEADER_H - textH()) * 0.5f, col(accentR, accentG, accentB, 245), subCrumb);

        float rightEdge = wx + winW - 12f;
        float btnY = wy + (HEADER_H - 28f) * 0.5f;

        if (menuMode) {
            float closeW = 28f;
            float closeX = rightEdge - closeW;
            if (drawHeaderPillButton(dl, "hdr_close_btn", "×", closeX, btnY, closeW, 28f, false)) {
                setOpen(false);
            }
            rightEdge = closeX - 8f;
        }

        float searchW = 236f;
        float searchH = 28f;
        float searchX = Math.max(crumbX + grpW + textW(subCrumb) + 16f, rightEdge - searchW);
        searchW = Math.max(140f, rightEdge - searchX);
        float searchY = wy + (HEADER_H - searchH) * 0.5f;

        if (!menuMode) {
            float dragW = Math.max(40f, searchX - wx - 6f);
            ImGui.setCursorScreenPos(wx, wy);
            ImGui.invisibleButton("##header_drag_area", dragW, HEADER_H);
            if (ImGui.isItemActive()) {
                ImGuiIO io = ImGui.getIO();
                windowX += io.getMouseDeltaX();
                windowY += io.getMouseDeltaY();
            }
        }

        boolean searchActive = !searchQueryDraft.isEmpty();
        setFontScale(BODY_FONT_SCALE);

        float focusAnim = hoverAnimMap.getOrDefault("hdr_search_focus", 0f);
        int boxBg = lerpColor(col(20, 16, 31, 240), col(26, 20, 40, 248), focusAnim);
        int boxBorder = lerpColor(
            col(255, 255, 255, 26),
            col(accentR, accentG, accentB, 215),
            focusAnim
        );
        dl.addRectFilled(searchX, searchY, searchX + searchW, searchY + searchH, boxBg, 7f);
        dl.addRect(searchX, searchY, searchX + searchW, searchY + searchH, boxBorder, 7f, 0, 1f);

        int iconCol = lerpColor(col(156, 148, 178, 220), col(accentR, accentG, accentB, 255), focusAnim);
        drawSearchIcon(dl, searchX + 13f, searchY + searchH * 0.5f, iconCol);

        ImGui.setCursorScreenPos(searchX, searchY);
        if (ImGui.invisibleButton("##search_icon_hit", 22f, searchH)) {
            focusSearchNextFrame = true;
        }
        boolean iconHovered = ImGui.isItemHovered();

        float inputX = searchX + 22f;
        float inputW = searchActive ? (searchW - 46f) : (searchW - 28f);
        float padY = Math.max(1f, (searchH - textH()) * 0.5f);

        ImGui.setCursorScreenPos(inputX, searchY);
        if (focusSearchNextFrame) {
            ImGui.setKeyboardFocusHere();
            focusSearchNextFrame = false;
        }
        ImGui.pushItemWidth(inputW);
        ImGui.pushStyleVar(ImGuiStyleVar.FramePadding, 4f, padY);
        ImGui.pushStyleColor(ImGuiCol.FrameBg, col(0, 0, 0, 0));
        ImGui.pushStyleColor(ImGuiCol.Text, col(242, 238, 252, 255));
        ImGui.pushStyleColor(ImGuiCol.TextDisabled, col(152, 144, 176, 210));
        if (ImGui.inputTextWithHint("##global_mod_search", "Search modules...", searchBuffer)) {
            String next = searchBuffer.get();
            if (!next.equals(searchQueryDraft)) {
                viewChangedNanos = System.nanoTime();
            }
            searchQueryDraft = next;
        }
        boolean inputFocused = ImGui.isItemActive();
        boolean inputHovered = ImGui.isItemHovered() || iconHovered;
        ImGui.popStyleColor(3);
        ImGui.popStyleVar();
        ImGui.popItemWidth();

        animateHover("hdr_search_focus", inputFocused || searchActive || inputHovered, 13f);

        if (searchActive) {
            float clrX = searchX + searchW - 24f;
            float clrY = searchY + 4f;
            ImGui.setCursorScreenPos(clrX, clrY);
            if (ImGui.invisibleButton("##clear_global_search", 20f, 20f)) {
                setSearchQuery("");
            }
            float clrHov = animateHover("hdr_clr_search", ImGui.isItemHovered(), 14f);
            int clrCol = lerpColor(col(175, 168, 196, 220), col(accentR, accentG, accentB, 255), clrHov);
            float xw = textW("×");
            dl.addText(clrX + (20f - xw) * 0.5f, clrY + (20f - textH()) * 0.5f, clrCol, "×");
        }

        dl.addLine(wx, wy + HEADER_H, wx + winW, wy + HEADER_H, col(255, 255, 255, 18), 1f);
    }

    private static void drawSearchIcon(ImDrawList dl, float cx, float cy, int color) {
        float r = 4.0f;
        dl.addCircle(cx - 1.0f, cy - 1.0f, r, color, 14, 1.5f);
        dl.addLine(cx + 1.8f, cy + 1.8f, cx + 5.2f, cy + 5.2f, color, 1.6f);
    }

    private static boolean drawHeaderPillButton(
        ImDrawList dl,
        String id,
        String label,
        float x,
        float y,
        float w,
        float h,
        boolean active
    ) {
        setFontScale(BODY_FONT_SCALE);
        ImGui.setCursorScreenPos(x, y);
        boolean clicked = ImGui.invisibleButton("##" + id, w, h);
        float hov = animateHover("hdr_pill_" + id, ImGui.isItemHovered(), 13f);
        float act = animateHover("hdr_pill_act_" + id, active, 11f);

        int idleBg = lerpColor(col(26, 21, 38, 225), col(44, 35, 64, 240), hov);
        int activeBg = col(accentR, accentG, accentB, 78);
        int bg = lerpColor(idleBg, activeBg, act);

        int idleBd = lerpColor(col(255, 255, 255, 26), col(accentR, accentG, accentB, 145), hov);
        int activeBd = col(accentR, accentG, accentB, 220);
        int border = lerpColor(idleBd, activeBd, act);

        dl.addRectFilled(x, y, x + w, y + h, bg, 6f);
        dl.addRect(x, y, x + w, y + h, border, 6f, 0, 1f);

        float tw = textW(label);
        float tx = x + (w - tw) * 0.5f;
        float ty = y + (h - textH()) * 0.5f;
        int tc = lerpColor(col(230, 224, 244, 255), col(255, 248, 252, 255), Math.max(hov, act));
        dl.addText(tx, ty, tc, label);
        return clicked;
    }

    private static void renderSidebar(ImDrawList dl, float sx, float sy, float sw, float sh, float dt) {
        dl.addRectFilled(sx, sy, sx + sw, sy + sh, col(12, 10, 19, 228), WIN_ROUND, ImDrawFlags.RoundCornersBottomLeft);
        shaderFill(dl, sx, sy, sx + sw, sy + sh, (int) (52 * glassOpacity), WIN_ROUND, ImDrawFlags.RoundCornersBottomLeft);

        float quickCardH = 52f;
        float scrollH = sh - quickCardH - 10f;

        resetParentFontScale();
        ImGui.setCursorScreenPos(sx, sy);
        int flags = ImGuiWindowFlags.NoScrollbar | ImGuiWindowFlags.NoScrollWithMouse;
        ImGui.beginChild("##sidebar_nav_tree", sw, scrollH, false, flags);
        setFontScale(BODY_FONT_SCALE);
        applySmoothScroll("sidebar_nav_tree");

        ImDrawList sdl = ImGui.getWindowDrawList();
        List<Module> allModules = getAllModules();
        float scrollY = ImGui.getScrollY();
        float curY = sy + 6f - scrollY;
        float logicalY = 6f;

        for (GuiTaxonomy.Group group : GuiTaxonomy.Group.values()) {
            boolean isCurrentGroup = (selectedGroup == group);
            boolean isExpanded = isGroupExpanded(group);
            float prevExp = groupExpandAnim.getOrDefault(group, isExpanded ? 1f : 0f);
            float expAnim = damp(prevExp, isExpanded ? 1f : 0f, 11.5f, dt);
            groupExpandAnim.put(group, expAnim);

            float rowH = 29f;
            float rx = sx + 7f;
            float rw = sw - 14f;

            ImGui.setCursorPos(7f, logicalY);
            if (ImGui.invisibleButton("##nav_grp_" + group.name(), rw, rowH)) {
                selectGroup(group);
            }
            float grpHov = animateHover("nav_grp_hov_" + group.name(), ImGui.isItemHovered(), 12f);
            float grpAct = animateHover("nav_grp_act_" + group.name(), isCurrentGroup, 11f);

            int baseFill = lerpColor(col(20, 16, 30, 155), col(34, 27, 50, 225), grpHov);
            int actFill = col(accentR, accentG, accentB, 48);
            int rowFill = lerpColor(baseFill, actFill, grpAct);

            int baseBd = lerpColor(col(255, 255, 255, 14), col(accentR, accentG, accentB, 95), grpHov);
            int actBd = col(accentR, accentG, accentB, 185);
            int rowBorder = lerpColor(baseBd, actBd, grpAct);

            sdl.addRectFilled(rx, curY, rx + rw, curY + rowH, rowFill, 7f);
            sdl.addRect(rx, curY, rx + rw, curY + rowH, rowBorder, 7f, 0, 1f);

            if (grpAct > 0.01f) {
                float barH = 16f * grpAct;
                float midY = curY + rowH * 0.5f;
                sdl.addRectFilled(
                    rx + 1.5f,
                    midY - barH * 0.5f,
                    rx + 4.5f,
                    midY + barH * 0.5f,
                    col(accentR, accentG, accentB, (int) (255 * grpAct)),
                    2f
                );
            }

            float nudge = grpHov * 2.5f;
            drawGroupIcon(sdl, group, rx + 8f + nudge, curY + (rowH - 16f) * 0.5f, Math.max(grpHov, grpAct));

            setFontScale(BODY_FONT_SCALE);
            float labelX = rx + 29f + nudge;
            float labelY = curY + (rowH - textH()) * 0.5f;
            int labelCol = lerpColor(col(214, 206, 232, 240), col(250, 246, 255, 255), Math.max(grpHov, grpAct));
            sdl.addText(labelX, labelY, labelCol, group.getLabel());

            int groupCount = GuiTaxonomy.countModules(allModules, group, "All");
            float chevX = rx + rw - 11f;
            drawAnimatedChevron(sdl, chevX, curY + rowH * 0.5f, expAnim, col(accentR, accentG, accentB, 230));

            if (groupCount > 0) {
                setFontScale(SUB_FONT_SCALE);
                String cntStr = String.valueOf(groupCount);
                float cntW = textW(cntStr) + 10f;
                float cntX = chevX - cntW - 7f;
                float cntH = 16f;
                float cntY = curY + (rowH - cntH) * 0.5f;
                sdl.addRectFilled(cntX, cntY, cntX + cntW, cntY + cntH, col(28, 22, 42, 220), 4f);
                sdl.addText(cntX + 5f, cntY + (cntH - textH()) * 0.5f, col(168, 160, 192, 235), cntStr);
            }

            curY += rowH + 3f;
            logicalY += rowH + 3f;

            if (expAnim > 0.008f) {
                List<String> subs = group.getSubCategories();
                float subH = 20f;
                float subGap = 2f;
                float fullSubH = subs.size() * (subH + subGap) + 3f;
                float visSubH = fullSubH * expAnim;

                float clipTop = Math.max(sy, curY);
                float clipBot = Math.min(sy + scrollH, curY + visSubH);
                if (clipBot > clipTop) {
                    sdl.pushClipRect(sx, clipTop, sx + sw, clipBot, true);
                    float treeLineX = rx + 14f;
                    float slideOffset = (1f - expAnim) * -6f;
                    float treeTopY = curY + 3f + slideOffset;

                    for (int i = 0; i < subs.size(); i++) {
                        String sub = subs.get(i);
                        boolean isSubSelected = isCurrentGroup && sub.equalsIgnoreCase(selectedSubCategory);
                        float subX = rx + 22f;
                        float subW = rw - 24f;
                        float itemY = curY + i * (subH + subGap) + slideOffset;
                        float itemLogicalY = logicalY + i * (subH + subGap);

                        if (expAnim > 0.5f) {
                            ImGui.setCursorPos(7f + 22f, itemLogicalY);
                            if (ImGui.invisibleButton("##nav_sub_" + group.name() + "_" + i, subW, subH)) {
                                if (selectedGroup != group || !sub.equalsIgnoreCase(selectedSubCategory)) {
                                    viewChangedNanos = System.nanoTime();
                                }
                                selectedGroup = group;
                                selectedSubCategory = sub;
                            }
                        }
                        boolean rawSubHov = expAnim > 0.5f && ImGui.isItemHovered();
                        float subHov = animateHover("nav_sub_hov_" + group.name() + "_" + i, rawSubHov, 13f);
                        float subAct = animateHover("nav_sub_act_" + group.name() + "_" + i, isSubSelected, 12f);
                        float combined = Math.max(subHov, subAct);

                        if (combined > 0.01f) {
                            int subBg = lerpColor(
                                col(36, 28, 52, (int) (195 * subHov * expAnim)),
                                col(accentR, accentG, accentB, (int) (64 * expAnim)),
                                subAct
                            );
                            float pillW = subW * (0.72f + 0.28f * combined);
                            sdl.addRectFilled(subX, itemY, subX + pillW, itemY + subH, subBg, 5f);
                        }

                        float dotR = 2.0f + 1.2f * subAct;
                        int dotCol = lerpColor(
                            col(120, 112, 144, (int) (160 * expAnim)),
                            col(accentR, accentG, accentB, (int) (255 * expAnim)),
                            subAct
                        );
                        sdl.addCircleFilled(treeLineX, itemY + subH * 0.5f, dotR, dotCol);

                        setFontScale(SUB_FONT_SCALE);
                        float sty = itemY + (subH - textH()) * 0.5f;
                        int stc = lerpColor(
                            col(156, 148, 178, (int) (230 * expAnim)),
                            col(250, 246, 255, (int) (255 * expAnim)),
                            combined
                        );
                        sdl.addText(subX + 8f + subHov * 2.0f, sty, stc, sub);

                        int subCount = GuiTaxonomy.countModules(allModules, group, sub);
                        if (subCount > 0) {
                            String scStr = String.valueOf(subCount);
                            float scW = textW(scStr);
                            sdl.addText(subX + subW - scW - 7f, sty, col(138, 130, 160, (int) (195 * expAnim)), scStr);
                        }
                    }

                    float treeBotY = curY + (subs.size() - 1) * (subH + subGap) + subH * 0.5f + slideOffset;
                    if (treeBotY > treeTopY) {
                        sdl.addLine(
                            treeLineX,
                            treeTopY,
                            treeLineX,
                            treeBotY,
                            col(accentR, accentG, accentB, (int) (72 * expAnim)),
                            1f
                        );
                    }
                    sdl.popClipRect();
                }

                curY += visSubH;
                logicalY += visSubH;
            }
        }

        ImGui.setCursorPosY(logicalY + 6f);
        ImGui.dummy(1f, 1f);
        ImGui.endChild();

        setFontScale(SUB_FONT_SCALE);
        float cardX = sx + 7f;
        float cardY = sy + sh - quickCardH - 6f;
        float cardW = sw - 14f;
        dl.addRectFilled(cardX, cardY, cardX + cardW, cardY + quickCardH, col(20, 16, 31, 238), 7f);
        dl.addRect(cardX, cardY, cardX + cardW, cardY + quickCardH, col(accentR, accentG, accentB, 85), 7f, 0, 1f);

        String profLine = clipTextToWidth("Profile: " + ImGuiConfigPanel.getActiveProfileName(), cardW - 16f);
        dl.addText(cardX + 9f, cardY + 5f, col(228, 222, 242, 245), profLine);

        setFontScale(BODY_FONT_SCALE);
        ImGui.setCursorScreenPos(cardX + 7f, cardY + 23f);
        if (ImGui.invisibleButton("##sidebar_quick_cfg_btn", cardW - 14f, 23f)) {
            openConfigCenter();
        }
        float qHov = animateHover("sidebar_quick_cfg", ImGui.isItemHovered(), 13f);
        int qBg = lerpColor(col(34, 27, 50, 230), col(accentR, accentG, accentB, 105), qHov);
        dl.addRectFilled(cardX + 7f, cardY + 23f, cardX + cardW - 7f, cardY + 46f, qBg, 5f);
        String qTxt = isConfigCenterActive() ? "< Back to Modules" : "Open Config Menu";
        float qTw = textW(qTxt);
        dl.addText(
            cardX + (cardW - qTw) * 0.5f,
            cardY + 23f + (23f - textH()) * 0.5f,
            col(244, 240, 255, 255),
            qTxt
        );

        dl.addLine(sx + sw, sy, sx + sw, sy + sh, col(255, 255, 255, 16), 1f);
    }

    private static void renderMainContentArea(
        ImDrawList dl,
        float mx,
        float my,
        float mw,
        float mh,
        float dt
    ) {
        renderSubCategoryBar(dl, mx, my, mw, dt);

        float contentY = my + SUBBAR_H;
        float contentH = mh - SUBBAR_H;

        boolean searching = !searchQueryDraft.trim().isEmpty();
        if (!searching && GuiTaxonomy.isSpecialClientPanel(selectedGroup, selectedSubCategory)) {
            resetParentFontScale();
            if ("Macros".equalsIgnoreCase(selectedSubCategory)) {
                ImGuiConfigPanel.renderMacroManager(dl, mx, contentY, mw, contentH, frameCounter, CONFIG_DRAW_CB);
            } else {
                ImGuiConfigPanel.renderConfigCenter(dl, mx, contentY, mw, contentH, CONFIG_DRAW_CB);
            }
            return;
        }

        List<Module> allModules = getAllModules();
        List<Module> filtered = GuiTaxonomy.filterModules(
            allModules,
            selectedGroup,
            selectedSubCategory,
            searchQueryDraft
        );
        Map<String, List<Module>> sections = GuiTaxonomy.groupBySection(filtered, selectedGroup, searching);

        resetParentFontScale();
        ImGui.setCursorScreenPos(mx, contentY);
        int scrollFlags = ImGuiWindowFlags.NoScrollWithMouse;
        ImGui.beginChild("##module_list_scroll", mw, contentH, false, scrollFlags);
        setFontScale(BODY_FONT_SCALE);
        applySmoothScroll("module_list_scroll");
        ImDrawList mdl = ImGui.getWindowDrawList();

        float padX = 12f;
        float rowW = mw - padX * 2f;
        float logicalY = 8f;
        float scrollY = ImGui.getScrollY();

        if (sections.isEmpty()) {
            setFontScale(BODY_FONT_SCALE);
            float emptyY = contentY + contentH * 0.38f;
            String msg = searching
                ? ("No modules match \"" + searchQueryDraft.trim() + "\"")
                : "No modules in this sub-category.";
            float tw = textW(msg);
            mdl.addText(mx + (mw - tw) * 0.5f, emptyY, col(156, 148, 178, 230), msg);
        } else {
            int modIndex = 0;
            float viewElapsed = secondsSince(viewChangedNanos) * speedScale;
            for (Map.Entry<String, List<Module>> entry : sections.entrySet()) {
                String sectionTitle = entry.getKey();
                List<Module> mods = entry.getValue();

                float secEase = easeOutCubic((viewElapsed - modIndex * 0.014f) / 0.22f);
                float secScreenY = contentY + logicalY - scrollY;
                drawSectionHeader(mdl, mx + padX + (1f - secEase) * 10f, secScreenY, rowW, sectionTitle, mods.size(), secEase);
                logicalY += SECTION_H;

                for (Module mod : mods) {
                    float rowEase = easeOutCubic((viewElapsed - modIndex * 0.016f) / 0.25f);
                    float rowScreenY = contentY + logicalY - scrollY;
                    renderModuleRow(mdl, mod, modIndex++, mx + padX, rowScreenY, logicalY, rowW, dt, rowEase);
                    logicalY += ROW_H + ROW_GAP;
                }
                logicalY += 4f;
            }
        }

        ImGui.setCursorPosY(logicalY + 10f);
        ImGui.dummy(1f, 1f);
        ImGui.endChild();
    }

    private static void renderSubCategoryBar(ImDrawList dl, float x, float y, float w, float dt) {
        dl.addRectFilled(x, y, x + w, y + SUBBAR_H, col(18, 14, 28, 224));
        shaderFill(dl, x, y, x + w, y + SUBBAR_H, (int) (45 * glassOpacity), 0f, 0);

        setFontScale(BODY_FONT_SCALE);
        List<String> subs = selectedGroup.getSubCategories();
        float chipX = x + 12f;
        float chipY = y + 5f;
        float chipH = 24f;

        float targetSlideX = -1f;
        float targetSlideW = 0f;

        for (int i = 0; i < subs.size(); i++) {
            String sub = subs.get(i);
            boolean active = sub.equalsIgnoreCase(selectedSubCategory);
            float chipW = textW(sub) + 20f;
            if (chipX + chipW > x + w - 12f) {
                break;
            }
            if (active) {
                targetSlideX = chipX - x;
                targetSlideW = chipW;
            }
            chipX += chipW + 6f;
        }

        if (targetSlideX >= 0f) {
            if (slidingChipX < 0f || lastChipGroup != selectedGroup) {
                slidingChipX = targetSlideX;
                slidingChipW = targetSlideW;
                lastChipGroup = selectedGroup;
            } else {
                slidingChipX = damp(slidingChipX, targetSlideX, 15f, dt);
                slidingChipW = damp(slidingChipW, targetSlideW, 15f, dt);
            }
            float sx = x + slidingChipX;
            dl.addRectFilled(sx, chipY, sx + slidingChipW, chipY + chipH, col(accentR, accentG, accentB, 72), 6f);
            dl.addRect(sx, chipY, sx + slidingChipW, chipY + chipH, col(accentR, accentG, accentB, 215), 6f, 0, 1f);
            dl.addRectFilled(
                sx + 6f,
                y + SUBBAR_H - 2.5f,
                sx + slidingChipW - 6f,
                y + SUBBAR_H - 0.5f,
                col(accentR, accentG, accentB, 245),
                1.5f
            );
        }

        chipX = x + 12f;
        for (int i = 0; i < subs.size(); i++) {
            String sub = subs.get(i);
            boolean active = sub.equalsIgnoreCase(selectedSubCategory);
            float chipW = textW(sub) + 20f;
            if (chipX + chipW > x + w - 12f) {
                break;
            }
            ImGui.setCursorScreenPos(chipX, chipY);
            if (ImGui.invisibleButton("##subbar_chip_" + i, chipW, chipH)) {
                if (!sub.equalsIgnoreCase(selectedSubCategory)) {
                    viewChangedNanos = System.nanoTime();
                }
                selectedSubCategory = sub;
            }
            float hov = animateHover("sub_chip_hov_" + selectedGroup.name() + "_" + i, ImGui.isItemHovered(), 13f);
            float act = animateHover("sub_chip_act_" + selectedGroup.name() + "_" + i, active, 12f);

            if (!active) {
                int bg = lerpColor(col(26, 21, 38, 175), col(40, 32, 58, 225), hov);
                int bd = lerpColor(col(255, 255, 255, 18), col(accentR, accentG, accentB, 110), hov);
                dl.addRectFilled(chipX, chipY, chipX + chipW, chipY + chipH, bg, 6f);
                dl.addRect(chipX, chipY, chipX + chipW, chipY + chipH, bd, 6f, 0, 1f);
            }

            float ty = chipY + (chipH - textH()) * 0.5f;
            int tc = lerpColor(col(186, 178, 208, 240), col(252, 248, 255, 255), Math.max(hov, act));
            dl.addText(chipX + 10f, ty, tc, sub);

            chipX += chipW + 6f;
        }

        dl.addLine(x, y + SUBBAR_H, x + w, y + SUBBAR_H, col(255, 255, 255, 14), 1f);
    }

    private static void drawSectionHeader(
        ImDrawList dl,
        float x,
        float y,
        float w,
        String title,
        int count,
        float ease
    ) {
        setFontScale(SUB_FONT_SCALE);
        String label = title.toUpperCase(Locale.ROOT) + " (" + count + ")";
        float ty = y + (SECTION_H - textH()) * 0.5f;
        int textA = (int) (220 * ease);
        dl.addText(x + 4f, ty, col(accentR, accentG, accentB, textA), label);
        float lineX = x + textW(label) + 12f;
        if (lineX < x + w - 8f) {
            dl.addLine(
                lineX,
                y + SECTION_H * 0.5f,
                x + w - 4f,
                y + SECTION_H * 0.5f,
                col(255, 255, 255, (int) (20 * ease)),
                1f
            );
        }
    }

    private static void renderModuleRow(
        ImDrawList dl,
        Module mod,
        int idx,
        float baseRx,
        float screenY,
        float logicalY,
        float rw,
        float dt,
        float rowEase
    ) {
        float rx = baseRx + (1f - rowEase) * 14f;
        boolean enabled = mod.isToggled();
        boolean inspected = (inspectingModule == mod);
        boolean isBindingThis = (bindingModule == mod);
        int keyBind = mod.getKeyBind();
        boolean hasKey = keyBind != 0;
        List<Setting> settings = getVisibleSettings(mod);
        boolean hasSettings = !settings.isEmpty();

        float switchW = 36f;
        float switchH = 18f;
        float switchX = rx + rw - switchW - 10f;
        float switchY = screenY + (ROW_H - switchH) * 0.5f;

        float gearW = hasSettings ? 28f : 0f;
        float gearX = hasSettings ? (switchX - gearW - 6f) : switchX;
        float gearY = screenY + (ROW_H - 22f) * 0.5f;

        float clearBtnW = (hasKey || isBindingThis) ? 22f : 0f;
        float clearBtnX = (hasKey || isBindingThis) ? (gearX - clearBtnW - 4f) : gearX;
        float bindBtnY = screenY + (ROW_H - 22f) * 0.5f;

        setFontScale(SUB_FONT_SCALE);
        String bindText;
        if (isBindingThis) {
            bindText = "Press Key...";
        } else if (hasKey) {
            String kn = keyName(keyBind);
            bindText = mod.isHoldMode() ? (kn + " · Hold") : kn;
        } else {
            bindText = "+ Bind";
        }
        float bindBtnW = Math.max(58f, textW(bindText) + 16f);
        float bindBtnX = ((hasKey || isBindingThis) ? clearBtnX : gearX) - bindBtnW - 5f;

        float bodyHitW = Math.max(70f, bindBtnX - rx - 4f);
        ImGui.setCursorPos(12f, logicalY);
        boolean rowLeftClicked = ImGui.invisibleButton("##mod_row_body_" + idx, bodyHitW, ROW_H);
        boolean bodyHovered = ImGui.isItemHovered();
        boolean rowRightClicked = bodyHovered && ImGui.isMouseClicked(ImGuiMouseButton.Right);
        boolean rowMiddleClicked = bodyHovered && ImGui.isMouseClicked(ImGuiMouseButton.Middle);

        float rowHov = animateHover("mod_row_hov_" + mod.getName(), bodyHovered, 12f);
        float inspAnim = animateHover("mod_row_insp_" + mod.getName(), inspected, 11f);
        float prevToggle = toggleAnimMap.getOrDefault(mod, enabled ? 1f : 0f);
        float toggleAnim = damp(prevToggle, enabled ? 1f : 0f, 9.5f, dt);
        toggleAnimMap.put(mod, toggleAnim);

        int baseBg = lerpColor(col(24, 19, 36, 228), col(37, 29, 54, 240), rowHov);
        dl.addRectFilled(rx, screenY, rx + rw, screenY + ROW_H, baseBg, CARD_ROUND);
        int shaderA = (int) ((38f + 22f * toggleAnim + 18f * rowHov) * glassOpacity);
        shaderFill(dl, rx, screenY, rx + rw, screenY + ROW_H, shaderA, CARD_ROUND, 0);

        if (toggleAnim > 0.01f) {
            int tintA = (int) (34f * toggleAnim);
            dl.addRectFilled(rx, screenY, rx + rw, screenY + ROW_H, col(accentR, accentG, accentB, tintA), CARD_ROUND);
        }

        int offBorder = lerpColor(col(255, 255, 255, 18), col(accentR, accentG, accentB, 90), rowHov);
        int onBorder = col(accentR, accentG, accentB, 145);
        int borderCol = lerpColor(lerpColor(offBorder, onBorder, toggleAnim), col(accentR, accentG, accentB, 235), inspAnim);
        dl.addRect(rx, screenY, rx + rw, screenY + ROW_H, borderCol, CARD_ROUND, 0, 1f);

        float barH = 8f + (ROW_H - 22f) * toggleAnim;
        float midY = screenY + ROW_H * 0.5f;
        int barCol = lerpColor(col(96, 88, 120, 145), col(accentR, accentG, accentB, 255), toggleAnim);
        dl.addRectFilled(rx + 4f, midY - barH * 0.5f, rx + 7.5f, midY + barH * 0.5f, barCol, 2f);

        setFontScale(TITLE_FONT_SCALE);
        float titleX = rx + 15f + rowHov * 2.0f;
        String desc = mod.getDescription();
        boolean hasDesc = desc != null && !desc.trim().isEmpty();
        float titleY = hasDesc ? (screenY + 5f) : (screenY + (ROW_H - textH()) * 0.5f);
        int titleCol = lerpColor(col(220, 214, 236, 245), col(250, 246, 255, 255), Math.max(toggleAnim, rowHov));
        dl.addText(titleX, titleY, titleCol, mod.getName());
        float titleW = textW(mod.getName());

        setFontScale(SUB_FONT_SCALE);
        String summary = buildSettingsSummary(settings);
        if (!summary.isEmpty()) {
            float sumX = titleX + titleW + 8f;
            float sumMaxW = bindBtnX - sumX - 8f;
            if (sumMaxW > 42f) {
                String clippedSum = clipTextToWidth(summary, sumMaxW - 12f);
                float swPill = textW(clippedSum) + 10f;
                float pillH = textH() + 3f;
                float pillY = titleY + 1f;
                dl.addRectFilled(sumX, pillY, sumX + swPill, pillY + pillH, col(15, 12, 23, 205), 4f);
                dl.addText(sumX + 5f, pillY + 1.5f, col(accentR, accentG, accentB, 225), clippedSum);
            }
        }

        if (hasDesc) {
            float maxDescW = bindBtnX - titleX - 10f;
            if (maxDescW > 50f) {
                String clippedDesc = clipTextToWidth(desc.trim(), maxDescW);
                dl.addText(titleX, screenY + 23f, col(150, 142, 174, 220), clippedDesc);
            }
        }

        ImGui.setCursorScreenPos(bindBtnX, bindBtnY);
        if (ImGui.invisibleButton("##mod_bind_btn_" + idx, bindBtnW, 22f)) {
            if (bindingConsumedFrame != frameCounter) {
                bindingModule = isBindingThis ? null : mod;
                bindingConsumedFrame = frameCounter;
            }
        }
        float bindHov = animateHover("mod_bind_hov_" + mod.getName(), ImGui.isItemHovered(), 13f);
        float bindPulse = isBindingThis ? (0.5f + 0.5f * (float) Math.sin(elapsedSeconds() * 6.5f)) : 0f;

        int bindBg;
        int bindBd;
        if (isBindingThis) {
            bindBg = col(accentR, accentG, accentB, (int) (150 + 65 * bindPulse));
            bindBd = col(255, 255, 255, (int) (180 + 75 * bindPulse));
        } else if (hasKey) {
            bindBg = lerpColor(col(36, 28, 54, 230), col(56, 44, 80, 245), bindHov);
            bindBd = lerpColor(col(accentR, accentG, accentB, 155), col(accentR, accentG, accentB, 230), bindHov);
        } else {
            bindBg = lerpColor(col(19, 15, 29, 215), col(44, 35, 64, 238), bindHov);
            bindBd = lerpColor(col(255, 255, 255, 32), col(accentR, accentG, accentB, 145), bindHov);
        }
        dl.addRectFilled(bindBtnX, bindBtnY, bindBtnX + bindBtnW, bindBtnY + 22f, bindBg, 5f);
        dl.addRect(bindBtnX, bindBtnY, bindBtnX + bindBtnW, bindBtnY + 22f, bindBd, 5f, 0, 1f);
        float bTw = textW(bindText);
        int bTc = isBindingThis
            ? col(18, 12, 24, 255)
            : (hasKey ? col(244, 238, 255, 255) : lerpColor(col(178, 170, 200, 235), col(240, 234, 252, 255), bindHov));
        dl.addText(bindBtnX + (bindBtnW - bTw) * 0.5f, bindBtnY + (22f - textH()) * 0.5f, bTc, bindText);

        if (hasKey || isBindingThis) {
            ImGui.setCursorScreenPos(clearBtnX, bindBtnY);
            if (ImGui.invisibleButton("##mod_clr_bind_" + idx, clearBtnW, 22f)) {
                clearModuleKeybind(mod);
            }
            float clrHov = animateHover("mod_clr_hov_" + mod.getName(), ImGui.isItemHovered(), 14f);
            int clrBg = lerpColor(col(42, 26, 46, 220), col(198, 58, 78, 235), clrHov);
            dl.addRectFilled(clearBtnX, bindBtnY, clearBtnX + clearBtnW, bindBtnY + 22f, clrBg, 5f);
            float xw = textW("×");
            dl.addText(clearBtnX + (clearBtnW - xw) * 0.5f, bindBtnY + (22f - textH()) * 0.5f, col(248, 222, 230, 255), "×");
        }

        if (hasSettings) {
            ImGui.setCursorScreenPos(gearX, gearY);
            if (ImGui.invisibleButton("##mod_cfg_gear_" + idx, gearW, 22f)) {
                setInspectingModule(inspected ? null : mod);
            }
            float gearHov = animateHover("mod_gear_hov_" + mod.getName(), ImGui.isItemHovered(), 13f);
            int idleGearBg = lerpColor(col(20, 16, 30, 215), col(48, 38, 70, 240), gearHov);
            int gBg = lerpColor(idleGearBg, col(accentR, accentG, accentB, 95), inspAnim);
            int gBd = lerpColor(
                lerpColor(col(255, 255, 255, 24), col(accentR, accentG, accentB, 140), gearHov),
                col(accentR, accentG, accentB, 220),
                inspAnim
            );
            dl.addRectFilled(gearX, gearY, gearX + gearW, gearY + 22f, gBg, 5f);
            dl.addRect(gearX, gearY, gearX + gearW, gearY + 22f, gBd, 5f, 0, 1f);
            drawSlidersIcon(dl, gearX + 7f, gearY + 6f, 14f, 10f, Math.max(gearHov, inspAnim), col(234, 228, 246, 245));
        }

        ImGui.setCursorScreenPos(switchX, switchY);
        boolean switchClicked = ImGui.invisibleButton("##mod_toggle_sw_" + idx, switchW, switchH);
        drawToggleSwitch(dl, switchX, switchY, switchW, switchH, toggleAnim, enabled);

        if (rowMiddleClicked && bindingConsumedFrame != frameCounter) {
            bindingModule = isBindingThis ? null : mod;
            bindingConsumedFrame = frameCounter;
        } else if (rowRightClicked && hasSettings) {
            setInspectingModule(inspected ? null : mod);
        } else if ((rowLeftClicked || switchClicked) && bindingConsumedFrame != frameCounter) {
            mod.toggle();
            saveConfigSafely();
        }
    }

    private static void drawAnimatedChevron(ImDrawList dl, float cx, float cy, float expandAnim, int color) {
        double angle = expandAnim * (Math.PI * 0.5);
        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);

        float p1x = -1.8f;
        float p1y = -3.6f;
        float p2x = 2.4f;
        float p2y = 0.0f;
        float p3x = -1.8f;
        float p3y = 3.6f;

        float r1x = cx + p1x * cos - p1y * sin;
        float r1y = cy + p1x * sin + p1y * cos;
        float r2x = cx + p2x * cos - p2y * sin;
        float r2y = cy + p2x * sin + p2y * cos;
        float r3x = cx + p3x * cos - p3y * sin;
        float r3y = cy + p3x * sin + p3y * cos;

        dl.addLine(r1x, r1y, r2x, r2y, color, 1.6f);
        dl.addLine(r2x, r2y, r3x, r3y, color, 1.6f);
    }

    private static void drawSlidersIcon(ImDrawList dl, float x, float y, float w, float h, float anim, int color) {
        float y1 = y + 2f;
        float y2 = y + h * 0.5f;
        float y3 = y + h - 2f;
        dl.addLine(x, y1, x + w, y1, color, 1.3f);
        dl.addLine(x, y2, x + w, y2, color, 1.3f);
        dl.addLine(x, y3, x + w, y3, color, 1.3f);
        float c1 = 0.28f + 0.38f * anim;
        float c2 = 0.72f - 0.38f * anim;
        float c3 = 0.42f + 0.25f * anim;
        dl.addCircleFilled(x + w * c1, y1, 2.2f, col(accentR, accentG, accentB, 255));
        dl.addCircleFilled(x + w * c2, y2, 2.2f, col(accentR, accentG, accentB, 255));
        dl.addCircleFilled(x + w * c3, y3, 2.2f, col(accentR, accentG, accentB, 255));
    }

    private static void drawToggleSwitch(
        ImDrawList dl,
        float x,
        float y,
        float w,
        float h,
        float anim,
        boolean targetOn
    ) {
        int trackOff = col(18, 14, 28, 240);
        int trackOn = col(accentR, accentG, accentB, 230);
        int trackCol = lerpColor(trackOff, trackOn, anim);
        float radius = h * 0.5f;
        dl.addRectFilled(x, y, x + w, y + h, trackCol, radius);
        dl.addRect(x, y, x + w, y + h, col(255, 255, 255, 32), radius, 0, 1f);

        float knobR = radius - 2.5f;
        float knobDiam = knobR * 2f;
        float velocity = Math.abs((targetOn ? 1f : 0f) - anim);
        float stretch = velocity * 5.0f;
        float knobW = knobDiam + stretch;

        float minLeft = x + 2.5f;
        float maxLeft = x + w - 2.5f - knobW;
        float knobLeft = minLeft + (maxLeft - minLeft) * anim;
        float knobTop = y + 2.5f;
        dl.addRectFilled(knobLeft, knobTop, knobLeft + knobW, knobTop + knobDiam, col(250, 246, 255, 255), knobR);
    }

    private static void renderSettingsDrawer(
        ImDrawList dl,
        float dx,
        float dy,
        float fullDw,
        float visibleDw,
        float dh,
        float dt
    ) {
        Module mod = drawerRenderModule;
        if (mod == null) {
            return;
        }
        dl.pushClipRect(dx, dy, dx + visibleDw, dy + dh, true);

        dl.addRectFilled(dx, dy, dx + fullDw, dy + dh, col(14, 11, 22, 246), WIN_ROUND, ImDrawFlags.RoundCornersBottomRight);
        shaderFill(dl, dx, dy, dx + fullDw, dy + dh, (int) (58 * glassOpacity), WIN_ROUND, ImDrawFlags.RoundCornersBottomRight);
        dl.addLine(dx, dy, dx, dy + dh, col(accentR, accentG, accentB, 130), 1.2f);

        float hdrH = 44f;
        setFontScale(TITLE_FONT_SCALE);
        dl.addText(dx + 14f, dy + 6f, col(246, 242, 255, 255), mod.getName());
        setFontScale(SUB_FONT_SCALE);
        String subBadge = GuiTaxonomy.resolveGroup(mod).getLabel() + " · " + GuiTaxonomy.resolveSubCategory(mod);
        dl.addText(dx + 14f, dy + 24f, col(accentR, accentG, accentB, 225), subBadge);

        setFontScale(BODY_FONT_SCALE);
        float closeX = dx + fullDw - 32f;
        float closeY = dy + 11f;
        ImGui.setCursorScreenPos(closeX, closeY);
        if (ImGui.invisibleButton("##drawer_close_btn", 22f, 22f)) {
            setInspectingModule(null);
            dl.popClipRect();
            return;
        }
        float cHov = animateHover("drawer_close_hov", ImGui.isItemHovered(), 14f);
        int cBg = lerpColor(col(28, 22, 42, 220), col(195, 58, 78, 230), cHov);
        dl.addRectFilled(closeX, closeY, closeX + 22f, closeY + 22f, cBg, 5f);
        float xw = textW("×");
        dl.addText(closeX + (22f - xw) * 0.5f, closeY + (22f - textH()) * 0.5f, col(244, 236, 250, 255), "×");
        dl.addLine(dx, dy + hdrH, dx + fullDw, dy + hdrH, col(255, 255, 255, 16), 1f);

        float bodyY = dy + hdrH;
        float bodyH = dh - hdrH;
        resetParentFontScale();
        ImGui.setCursorScreenPos(dx, bodyY);
        int flags = ImGuiWindowFlags.NoScrollWithMouse;
        ImGui.beginChild("##drawer_settings_scroll", visibleDw, bodyH, false, flags);
        setFontScale(BODY_FONT_SCALE);
        applySmoothScroll("drawer_settings_scroll");
        ImDrawList sdl = ImGui.getWindowDrawList();

        float pad = 12f;
        float cardW = fullDw - pad * 2f;
        float scrollY = ImGui.getScrollY();
        float curY = bodyY + 10f - scrollY;
        float logicalY = 10f;

        float kbCardH = 64f;
        sdl.addRectFilled(dx + pad, curY, dx + pad + cardW, curY + kbCardH, col(24, 19, 36, 232), 7f);
        sdl.addRect(dx + pad, curY, dx + pad + cardW, curY + kbCardH, col(255, 255, 255, 20), 7f, 0, 1f);

        boolean bindingThis = (bindingModule == mod);
        int key = mod.getKeyBind();
        setFontScale(SUB_FONT_SCALE);
        String keyLabel = bindingThis
            ? "Keybind: [Press Key / ESC Clear]"
            : (key == 0 ? "Keybind: None" : ("Keybind: " + keyName(key)));
        sdl.addText(dx + pad + 10f, curY + 8f, col(238, 234, 248, 255), keyLabel);

        setFontScale(SUB_FONT_SCALE);
        float btnY = curY + 32f;
        float setBtnW = 86f;
        ImGui.setCursorScreenPos(dx + pad + 8f, btnY);
        if (ImGui.invisibleButton("##drawer_set_bind", setBtnW, 24f)) {
            if (bindingConsumedFrame != frameCounter) {
                bindingModule = bindingThis ? null : mod;
                bindingConsumedFrame = frameCounter;
            }
        }
        float sbHov = animateHover("drawer_set_bind_hov", ImGui.isItemHovered(), 13f);
        int sbBg = bindingThis
            ? col(accentR, accentG, accentB, 195)
            : lerpColor(col(36, 28, 54, 235), col(54, 42, 78, 245), sbHov);
        sdl.addRectFilled(dx + pad + 8f, btnY, dx + pad + 8f + setBtnW, btnY + 24f, sbBg, 5f);
        String sbTxt = bindingThis ? "Cancel" : (key == 0 ? "+ Add Key" : "Change Key");
        sdl.addText(
            dx + pad + 8f + (setBtnW - textW(sbTxt)) * 0.5f,
            btnY + (24f - textH()) * 0.5f,
            col(244, 238, 255, 255),
            sbTxt
        );

        float clrW = 62f;
        float clrX = dx + pad + 14f + setBtnW;
        ImGui.setCursorScreenPos(clrX, btnY);
        if (ImGui.invisibleButton("##drawer_clr_bind", clrW, 24f)) {
            clearModuleKeybind(mod);
        }
        float dcHov = animateHover("drawer_clr_bind_hov", ImGui.isItemHovered(), 13f);
        int dcBg = lerpColor(col(44, 28, 48, 230), col(195, 58, 78, 230), dcHov);
        sdl.addRectFilled(clrX, btnY, clrX + clrW, btnY + 24f, dcBg, 5f);
        sdl.addText(
            clrX + (clrW - textW("Clear")) * 0.5f,
            btnY + (24f - textH()) * 0.5f,
            col(248, 220, 228, 255),
            "Clear"
        );

        float modeW = cardW - setBtnW - clrW - 28f;
        float modeX = clrX + clrW + 6f;
        ImGui.setCursorScreenPos(modeX, btnY);
        if (ImGui.invisibleButton("##drawer_hold_mode", modeW, 24f)) {
            mod.setHoldMode(!mod.isHoldMode());
            saveConfigSafely();
        }
        float dmHov = animateHover("drawer_mode_hov", ImGui.isItemHovered(), 13f);
        String holdTxt = mod.isHoldMode() ? "Hold" : "Toggle";
        sdl.addRectFilled(modeX, btnY, modeX + modeW, btnY + 24f, lerpColor(col(34, 27, 50, 230), col(50, 40, 74, 240), dmHov), 5f);
        sdl.addText(
            modeX + (modeW - textW(holdTxt)) * 0.5f,
            btnY + (24f - textH()) * 0.5f,
            col(accentR, accentG, accentB, 245),
            holdTxt
        );

        curY += kbCardH + 10f;
        logicalY += kbCardH + 10f;

        List<Setting> visibleSettings = getVisibleSettings(mod);
        float drawerElapsed = secondsSince(drawerOpenedNanos) * speedScale;
        for (int i = 0; i < visibleSettings.size(); i++) {
            Setting s = visibleSettings.get(i);
            float setEase = easeOutCubic((drawerElapsed - i * 0.018f) / 0.24f);
            float slideX = (1f - setEase) * 10f;
            float used = renderSettingControl(sdl, s, i, dx + pad + slideX, curY, logicalY, cardW, dt);
            curY += used + 6f;
            logicalY += used + 6f;
        }

        ImGui.setCursorPosY(logicalY + 12f);
        ImGui.dummy(1f, 1f);
        ImGui.endChild();
        dl.popClipRect();
    }

    private static float renderSettingControl(
        ImDrawList dl,
        Setting s,
        int idx,
        float x,
        float screenY,
        float logicalY,
        float w,
        float dt
    ) {
        setFontScale(BODY_FONT_SCALE);
        if (s.isCheck()) {
            float h = 32f;
            ImGui.setCursorScreenPos(x, screenY);
            if (ImGui.invisibleButton("##set_chk_" + idx, w, h)) {
                s.setValBoolean(!s.getValBoolean());
                saveConfigSafely();
            }
            float hov = animateHover("set_chk_hov_" + idx, ImGui.isItemHovered(), 13f);
            dl.addRectFilled(x, screenY, x + w, screenY + h, lerpColor(col(24, 19, 36, 224), col(34, 27, 50, 236), hov), 6f);
            float ty = screenY + (h - textH()) * 0.5f;
            dl.addText(x + 10f, ty, col(234, 228, 246, 255), s.getName());

            boolean val = s.getValBoolean();
            float prev = checkAnimMap.getOrDefault(s, val ? 1f : 0f);
            float anim = damp(prev, val ? 1f : 0f, 10f, dt);
            checkAnimMap.put(s, anim);
            drawToggleSwitch(dl, x + w - 42f, screenY + (h - 18f) * 0.5f, 32f, 18f, anim, val);
            return h;
        }

        if (s.isSlider()) {
            float h = 44f;
            dl.addRectFilled(x, screenY, x + w, screenY + h, col(24, 19, 36, 224), 6f);
            double min = s.getMin();
            double max = s.getMax();
            double val = s.getValDouble();
            String valStr = s.onlyInt()
                ? String.valueOf((int) Math.round(val))
                : String.format(Locale.ROOT, "%.2f", val);

            dl.addText(x + 10f, screenY + 6f, col(232, 226, 244, 255), s.getName());
            setFontScale(SUB_FONT_SCALE);
            dl.addText(x + w - textW(valStr) - 10f, screenY + 7f, col(accentR, accentG, accentB, 245), valStr);

            float trackX = x + 10f;
            float trackW = w - 20f;
            float trackY = screenY + 30f;
            ImGui.setCursorScreenPos(trackX, screenY + 22f);
            ImGui.invisibleButton("##set_sld_" + idx, trackW, 16f);
            boolean active = ImGui.isItemActive();
            if (active) {
                float mx = ImGui.getMousePosX();
                double pct = Math.max(0.0, Math.min(1.0, (mx - trackX) / trackW));
                double next = min + pct * (max - min);
                if (s.onlyInt()) {
                    next = Math.round(next);
                }
                s.setValDouble(next);
            }
            float sldHov = animateHover("set_sld_hov_" + idx, active || ImGui.isItemHovered(), 14f);

            float targetPct = (max > min) ? (float) ((s.getValDouble() - min) / (max - min)) : 0f;
            targetPct = clamp(targetPct, 0f, 1f);
            float prevPct = sliderAnimMap.getOrDefault(s, targetPct);
            float animPct = damp(prevPct, targetPct, 16f, dt);
            sliderAnimMap.put(s, animPct);

            dl.addRectFilled(trackX, trackY - 2.5f, trackX + trackW, trackY + 2.5f, col(14, 11, 22, 240), 2.5f);
            dl.addRectFilled(trackX, trackY - 2.5f, trackX + trackW * animPct, trackY + 2.5f, col(accentR, accentG, accentB, 235), 2.5f);
            float thumbR = 4.8f + 1.4f * sldHov;
            dl.addCircleFilled(trackX + trackW * animPct, trackY, thumbR, col(250, 246, 255, 255));
            return h;
        }

        if (s.isRange()) {
            float h = 46f;
            dl.addRectFilled(x, screenY, x + w, screenY + h, col(24, 19, 36, 224), 6f);
            double min = s.getMin();
            double max = s.getMax();
            double lo = s.getRangeLow();
            double hi = s.getRangeHigh();
            String valStr = s.onlyInt()
                ? ((int) lo + " - " + (int) hi)
                : String.format(Locale.ROOT, "%.2f - %.2f", lo, hi);

            dl.addText(x + 10f, screenY + 6f, col(232, 226, 244, 255), s.getName());
            setFontScale(SUB_FONT_SCALE);
            dl.addText(x + w - textW(valStr) - 10f, screenY + 7f, col(accentR, accentG, accentB, 245), valStr);

            float trackX = x + 10f;
            float trackW = w - 20f;
            float trackY = screenY + 32f;
            ImGui.setCursorScreenPos(trackX, screenY + 24f);
            ImGui.invisibleButton("##set_rng_" + idx, trackW, 16f);
            boolean active = ImGui.isItemActive();
            if (active) {
                float mx = ImGui.getMousePosX();
                double pct = Math.max(0.0, Math.min(1.0, (mx - trackX) / trackW));
                double v = min + pct * (max - min);
                if (Math.abs(v - lo) <= Math.abs(v - hi)) {
                    s.setRange(v, hi);
                } else {
                    s.setRange(lo, v);
                }
            }
            float rngHov = animateHover("set_rng_hov_" + idx, active || ImGui.isItemHovered(), 14f);

            float tLo = (max > min) ? (float) ((s.getRangeLow() - min) / (max - min)) : 0f;
            float tHi = (max > min) ? (float) ((s.getRangeHigh() - min) / (max - min)) : 1f;
            float aLo = damp(rangeLoAnimMap.getOrDefault(s, tLo), clamp(tLo, 0f, 1f), 16f, dt);
            float aHi = damp(rangeHiAnimMap.getOrDefault(s, tHi), clamp(tHi, 0f, 1f), 16f, dt);
            rangeLoAnimMap.put(s, aLo);
            rangeHiAnimMap.put(s, aHi);

            dl.addRectFilled(trackX, trackY - 2.5f, trackX + trackW, trackY + 2.5f, col(14, 11, 22, 240), 2.5f);
            dl.addRectFilled(trackX + trackW * aLo, trackY - 2.5f, trackX + trackW * aHi, trackY + 2.5f, col(accentR, accentG, accentB, 235), 2.5f);
            float thumbR = 4.5f + 1.2f * rngHov;
            dl.addCircleFilled(trackX + trackW * aLo, trackY, thumbR, col(250, 246, 255, 255));
            dl.addCircleFilled(trackX + trackW * aHi, trackY, thumbR, col(250, 246, 255, 255));
            return h;
        }

        if (s.isCombo()) {
            List<String> opts = s.getOptions();
            int count = opts != null ? opts.size() : 0;
            int cols = 2;
            int rows = Math.max(1, (count + cols - 1) / cols);
            float h = 30f + rows * 26f;
            dl.addRectFilled(x, screenY, x + w, screenY + h, col(24, 19, 36, 224), 6f);
            dl.addText(x + 10f, screenY + 6f, col(232, 226, 244, 255), s.getName());

            if (opts != null) {
                setFontScale(SUB_FONT_SCALE);
                float cellGap = 6f;
                float cellW = (w - 20f - cellGap) * 0.5f;
                for (int i = 0; i < opts.size(); i++) {
                    String opt = opts.get(i);
                    int c = i % cols;
                    int r = i / cols;
                    float ox = x + 10f + c * (cellW + cellGap);
                    float oy = screenY + 26f + r * 26f;
                    ImGui.setCursorScreenPos(ox, oy);
                    if (ImGui.invisibleButton("##set_cmb_" + idx + "_" + i, cellW, 22f)) {
                        s.setValString(opt);
                        saveConfigSafely();
                    }
                    boolean sel = opt.equalsIgnoreCase(s.getValString());
                    float hov = animateHover("set_cmb_hov_" + idx + "_" + i, ImGui.isItemHovered(), 13f);
                    float act = animateHover("set_cmb_act_" + idx + "_" + i, sel, 12f);

                    int bg = lerpColor(lerpColor(col(16, 13, 24, 215), col(34, 27, 50, 235), hov), col(accentR, accentG, accentB, 92), act);
                    int bd = lerpColor(lerpColor(col(255, 255, 255, 18), col(accentR, accentG, accentB, 110), hov), col(accentR, accentG, accentB, 215), act);
                    dl.addRectFilled(ox, oy, ox + cellW, oy + 22f, bg, 4f);
                    dl.addRect(ox, oy, ox + cellW, oy + 22f, bd, 4f, 0, 1f);
                    String clippedOpt = clipTextToWidth(opt, cellW - 10f);
                    float tw = textW(clippedOpt);
                    dl.addText(ox + (cellW - tw) * 0.5f, oy + (22f - textH()) * 0.5f, col(240, 236, 250, 255), clippedOpt);
                }
            }
            return h;
        }

        if (s.isColor()) {
            boolean expanded = (expandedColorSetting == s);
            float prevExp = colorExpandAnimMap.getOrDefault(s, expanded ? 1f : 0f);
            float expAnim = damp(prevExp, expanded ? 1f : 0f, 11.5f, dt);
            colorExpandAnimMap.put(s, expAnim);

            float h = 32f + 112f * expAnim;
            dl.addRectFilled(x, screenY, x + w, screenY + h, col(24, 19, 36, 224), 6f);
            ImGui.setCursorScreenPos(x, screenY);
            if (ImGui.invisibleButton("##set_col_hdr_" + idx, w, 32f)) {
                expandedColorSetting = expanded ? null : s;
            }
            dl.addText(x + 10f, screenY + (32f - textH()) * 0.5f, col(232, 226, 244, 255), s.getName());
            int rgb = s.getValColor();
            int cr = (rgb >> 16) & 0xFF;
            int cg = (rgb >> 8) & 0xFF;
            int cb = rgb & 0xFF;
            dl.addRectFilled(x + w - 36f, screenY + 7f, x + w - 10f, screenY + 25f, col(cr, cg, cb, 255), 4f);
            dl.addRect(x + w - 36f, screenY + 7f, x + w - 10f, screenY + 25f, col(255, 255, 255, 85), 4f, 0, 1f);

            if (expAnim > 0.02f) {
                dl.pushClipRect(x, screenY + 32f, x + w, screenY + h, true);
                float[] hsv = hsvCache.computeIfAbsent(s, k -> Color.RGBtoHSB(cr, cg, cb, null));
                float svX = x + 10f;
                float svY = screenY + 36f;
                float svW = w - 42f;
                float svH = 96f;

                int hueRgb = Color.HSBtoRGB(hsv[0], 1f, 1f);
                int hr = (hueRgb >> 16) & 0xFF;
                int hg = (hueRgb >> 8) & 0xFF;
                int hb = hueRgb & 0xFF;
                dl.addRectFilledMultiColor(
                    svX,
                    svY,
                    svX + svW,
                    svY + svH,
                    col(255, 255, 255, 255),
                    col(hr, hg, hb, 255),
                    col(hr, hg, hb, 255),
                    col(255, 255, 255, 255)
                );
                dl.addRectFilledMultiColor(
                    svX,
                    svY,
                    svX + svW,
                    svY + svH,
                    col(0, 0, 0, 0),
                    col(0, 0, 0, 0),
                    col(0, 0, 0, 255),
                    col(0, 0, 0, 255)
                );
                if (expAnim > 0.7f) {
                    ImGui.setCursorScreenPos(svX, svY);
                    ImGui.invisibleButton("##set_col_sv_" + idx, svW, svH);
                    if (ImGui.isItemActive()) {
                        hsv[1] = clamp((ImGui.getMousePosX() - svX) / svW, 0f, 1f);
                        hsv[2] = clamp(1f - (ImGui.getMousePosY() - svY) / svH, 0f, 1f);
                        s.setValColor(Color.HSBtoRGB(hsv[0], hsv[1], hsv[2]));
                    }
                }
                dl.addCircle(svX + hsv[1] * svW, svY + (1f - hsv[2]) * svH, 4f, col(255, 255, 255, 255), 12, 1.5f);

                float hueX = svX + svW + 8f;
                float hueW = 14f;
                for (int seg = 0; seg < 6; seg++) {
                    float y0 = svY + (svH / 6f) * seg;
                    float y1 = svY + (svH / 6f) * (seg + 1);
                    int c0 = Color.HSBtoRGB(seg / 6f, 1f, 1f);
                    int c1 = Color.HSBtoRGB((seg + 1) / 6f, 1f, 1f);
                    int c0Col = col((c0 >> 16) & 0xFF, (c0 >> 8) & 0xFF, c0 & 0xFF, 255);
                    int c1Col = col((c1 >> 16) & 0xFF, (c1 >> 8) & 0xFF, c1 & 0xFF, 255);
                    dl.addRectFilledMultiColor(hueX, y0, hueX + hueW, y1, c0Col, c0Col, c1Col, c1Col);
                }
                if (expAnim > 0.7f) {
                    ImGui.setCursorScreenPos(hueX, svY);
                    ImGui.invisibleButton("##set_col_hue_" + idx, hueW, svH);
                    if (ImGui.isItemActive()) {
                        hsv[0] = clamp((ImGui.getMousePosY() - svY) / svH, 0f, 1f);
                        s.setValColor(Color.HSBtoRGB(hsv[0], hsv[1], hsv[2]));
                    }
                }
                dl.popClipRect();
            }
            return h;
        }

        if (s.isCurve()) {
            float h = 114f;
            dl.addRectFilled(x, screenY, x + w, screenY + h, col(24, 19, 36, 224), 6f);
            dl.addText(x + 10f, screenY + 6f, col(232, 226, 244, 255), s.getName());
            double[] cv = s.getCurve();
            float gx = x + 10f;
            float gy = screenY + 26f;
            float gw = w - 20f;
            float gh = 80f;
            dl.addRectFilled(gx, gy, gx + gw, gy + gh, col(14, 11, 22, 240), 4f);
            float p1x = gx + (float) cv[0] * gw;
            float p1y = gy + (1f - (float) cv[1]) * gh;
            float p2x = gx + (float) cv[2] * gw;
            float p2y = gy + (1f - (float) cv[3]) * gh;
            dl.addBezierCubic(gx, gy + gh, p1x, p1y, p2x, p2y, gx + gw, gy, col(accentR, accentG, accentB, 235), 2f);
            dl.addCircleFilled(p1x, p1y, 4.2f, col(250, 245, 255, 255));
            dl.addCircleFilled(p2x, p2y, 4.2f, col(250, 245, 255, 255));
            ImGui.setCursorScreenPos(gx, gy);
            ImGui.invisibleButton("##set_crv_" + idx, gw, gh);
            if (ImGui.isItemActive()) {
                float mx = clamp((ImGui.getMousePosX() - gx) / gw, 0f, 1f);
                float my = clamp(1f - (ImGui.getMousePosY() - gy) / gh, 0f, 1f);
                float d1 = (ImGui.getMousePosX() - p1x) * (ImGui.getMousePosX() - p1x)
                    + (ImGui.getMousePosY() - p1y) * (ImGui.getMousePosY() - p1y);
                float d2 = (ImGui.getMousePosX() - p2x) * (ImGui.getMousePosX() - p2x)
                    + (ImGui.getMousePosY() - p2y) * (ImGui.getMousePosY() - p2y);
                if (d1 <= d2) {
                    s.setCurve(mx, my, cv[2], cv[3]);
                } else {
                    s.setCurve(cv[0], cv[1], mx, my);
                }
            }
            return h;
        }

        if (s.isText()) {
            float h = 56f;
            dl.addRectFilled(x, screenY, x + w, screenY + h, col(24, 19, 36, 224), 6f);
            dl.addText(x + 10f, screenY + 6f, col(232, 226, 244, 255), s.getName());
            ImString buf = textSettingBuffers.computeIfAbsent(s, k -> new ImString(k.getValText(), 128));
            float boxX = x + 10f;
            float boxY = screenY + 24f;
            float boxW = w - 20f;
            float boxH = 24f;
            dl.addRectFilled(boxX, boxY, boxX + boxW, boxY + boxH, col(14, 11, 22, 242), 5f);
            float padY = Math.max(1f, (boxH - textH()) * 0.5f);
            ImGui.setCursorScreenPos(boxX, boxY);
            ImGui.pushItemWidth(boxW);
            ImGui.pushStyleVar(ImGuiStyleVar.FramePadding, 7f, padY);
            ImGui.pushStyleColor(ImGuiCol.FrameBg, col(0, 0, 0, 0));
            ImGui.pushStyleColor(ImGuiCol.Text, col(240, 236, 250, 255));
            if (ImGui.inputText("##set_txt_" + idx, buf)) {
                s.setValText(buf.get());
            }
            boolean txtActive = ImGui.isItemActive() || ImGui.isItemHovered();
            ImGui.popStyleColor(2);
            ImGui.popStyleVar();
            ImGui.popItemWidth();
            float fAnim = animateHover("set_txt_focus_" + idx, txtActive, 13f);
            int bd = lerpColor(col(255, 255, 255, 22), col(accentR, accentG, accentB, 185), fAnim);
            dl.addRect(boxX, boxY, boxX + boxW, boxY + boxH, bd, 5f, 0, 1f);
            return h;
        }

        return 30f;
    }

    private static void renderVerticalColumnsLayout(float dispW, float dispH, float dt, float openEase) {
        GuiTaxonomy.Group[] groups = GuiTaxonomy.Group.values();
        int count = groups.length;
        float gap = 10f;
        float colW = Math.min(230f, (dispW - 40f - gap * (count - 1)) / count);
        float colH = Math.min(480f, dispH - 48f);
        float startX = (dispW - (colW * count + gap * (count - 1))) * 0.5f;
        float startY = 24f + (1f - openEase) * 16f;

        List<Module> allModules = getAllModules();

        for (int i = 0; i < count; i++) {
            GuiTaxonomy.Group g = groups[i];
            float cx = startX + i * (colW + gap);
            ImGui.setNextWindowPos(cx, startY, ImGuiCond.FirstUseEver);
            ImGui.setNextWindowSize(colW, colH, ImGuiCond.Always);

            int flags = ImGuiWindowFlags.NoTitleBar
                | ImGuiWindowFlags.NoResize
                | ImGuiWindowFlags.NoCollapse
                | ImGuiWindowFlags.NoBackground;

            ImGui.begin("##VertCol_" + g.name(), new ImBoolean(true), flags);
            setFontScale(TITLE_FONT_SCALE);
            float wx = ImGui.getWindowPosX();
            float wy = ImGui.getWindowPosY();
            shaderWinX = wx;
            shaderWinY = wy;
            shaderWinW = colW;
            shaderWinH = colH;

            ImDrawList dl = ImGui.getWindowDrawList();
            drawWindowShell(dl, wx, wy, colW, colH);
            dl.addText(wx + 12f, wy + 9f, col(accentR, accentG, accentB, 255), g.getLabel());
            dl.addLine(wx, wy + 34f, wx + colW, wy + 34f, col(255, 255, 255, 18), 1f);

            setFontScale(BODY_FONT_SCALE);
            List<Module> mods = GuiTaxonomy.filterModules(allModules, g, "All", searchQueryDraft);
            float ry = wy + 40f;
            for (int mIdx = 0; mIdx < mods.size(); mIdx++) {
                Module m = mods.get(mIdx);
                if (ry + 30f > wy + colH - 8f) {
                    break;
                }
                ImGui.setCursorScreenPos(wx + 8f, ry);
                if (ImGui.invisibleButton("##vmod_" + g.name() + "_" + mIdx, colW - 16f, 28f)) {
                    m.toggle();
                    saveConfigSafely();
                }
                float hov = animateHover("vmod_hov_" + m.getName(), ImGui.isItemHovered(), 12f);
                if (ImGui.isItemHovered() && ImGui.isMouseClicked(ImGuiMouseButton.Middle)) {
                    bindingModule = (bindingModule == m) ? null : m;
                    bindingConsumedFrame = frameCounter;
                }
                int offFill = lerpColor(col(25, 20, 37, 220), col(40, 32, 58, 235), hov);
                int fill = m.isToggled() ? col(accentR, accentG, accentB, 78) : offFill;
                dl.addRectFilled(wx + 8f, ry, wx + colW - 8f, ry + 28f, fill, 5f);
                dl.addText(wx + 14f + hov * 2f, ry + (28f - textH()) * 0.5f, col(238, 234, 248, 255), m.getName());
                ry += 32f;
            }
            ImGui.end();
        }
    }

    private static void pollBindingMouse() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getWindow() == null) {
            return;
        }
        boolean activeBind = isCapturingMouseBind();
        for (int btn = 0; btn < prevBindMouseDown.length; btn++) {
            boolean down = InputUtil.isMouseButtonDown(toMouseBindCode(btn));
            boolean clicked = (down && !prevBindMouseDown[btn]) || (btn >= 2 && btn < 5 && ImGui.isMouseClicked(btn));
            prevBindMouseDown[btn] = down;
            if (activeBind && clicked) {
                handleBindingMouseButton(btn);
                activeBind = false;
            }
        }
    }

    private static void shaderFill(
        ImDrawList dl,
        float x1,
        float y1,
        float x2,
        float y2,
        int alpha,
        float rounding,
        int flags
    ) {
        int clampedA = Math.max(0, Math.min(255, alpha));
        if (clampedA == 0) {
            return;
        }
        if (shaderTexture == 0) {
            dl.addRectFilled(x1, y1, x2, y2, col(accentR, accentG, accentB, clampedA / 3), rounding, flags);
            return;
        }
        float w = Math.max(1f, shaderWinW);
        float h = Math.max(1f, shaderWinH);
        float u1 = clamp((x1 - shaderWinX) / w, 0f, 1f);
        float v1 = clamp(1f - (y1 - shaderWinY) / h, 0f, 1f);
        float u2 = clamp((x2 - shaderWinX) / w, 0f, 1f);
        float v2 = clamp(1f - (y2 - shaderWinY) / h, 0f, 1f);
        int tint = col(255, 255, 255, clampedA);
        if (rounding > 0f) {
            dl.addImageRounded(shaderTexture, x1, y1, x2, y2, u1, v1, u2, v2, tint, rounding, flags);
        } else {
            dl.addImage(shaderTexture, x1, y1, x2, y2, u1, v1, u2, v2, tint);
        }
    }

    private static void syncThemeSettings() {
        ImnotcheatingyouareClient client = ImnotcheatingyouareClient.INSTANCE;
        if (client == null || client.settingsManager == null) {
            return;
        }
        SettingsManager sm = client.settingsManager;
        Module themeMod = findModuleByName("Theme");
        if (themeMod != null) {
            Setting accent = sm.getSettingByName(themeMod, "Accent Color");
            if (accent != null) {
                int c = accent.getValColor();
                accentR = (c >> 16) & 0xFF;
                accentG = (c >> 8) & 0xFF;
                accentB = c & 0xFF;
            }
            Setting alpha = sm.getSettingByName(themeMod, "Background Alpha");
            if (alpha != null) {
                bgAlpha = Math.max(140, Math.min(255, (int) alpha.getValDouble()));
            }
            Setting spd = sm.getSettingByName(themeMod, "Anim Speed");
            if (spd != null) {
                animSpeed = (float) spd.getValDouble();
            }
        }
        Module guiMod = findModuleByName("GUI");
        if (guiMod != null) {
            Setting glass = sm.getSettingByName(guiMod, "Glass Opacity");
            if (glass != null) {
                glassOpacity = (float) (glass.getValDouble() / 100.0);
            }
            Setting glow = sm.getSettingByName(guiMod, "Glow");
            if (glow != null) {
                glowEnabled = glow.getValBoolean();
            }
        }
    }

    private static String getGuiLayoutMode() {
        ImnotcheatingyouareClient client = ImnotcheatingyouareClient.INSTANCE;
        if (client == null || client.settingsManager == null) {
            return "Compact";
        }
        Module guiMod = findModuleByName("GUI");
        if (guiMod == null) {
            return "Compact";
        }
        SettingsManager sm = client.settingsManager;
        Setting layout = sm.getSettingByName(guiMod, "Layout");
        return layout != null ? layout.getValString() : "Compact";
    }

    private static int getMenuModuleKeybind() {
        Module guiMod = findModuleByName("GUI");
        if (guiMod != null && guiMod.getKeyBind() > 0) {
            return guiMod.getKeyBind();
        }
        Module menuMod = findModuleByName("Menu");
        if (menuMod != null && menuMod.getKeyBind() > 0) {
            return menuMod.getKeyBind();
        }
        return InputConstants.KEY_RSHIFT;
    }

    private static List<Module> getAllModules() {
        ImnotcheatingyouareClient client = ImnotcheatingyouareClient.INSTANCE;
        if (client == null || client.moduleManager == null) {
            return new ArrayList<>();
        }
        ModuleManager mm = client.moduleManager;
        return mm.modules;
    }

    private static Module findModuleByName(String name) {
        for (Module m : getAllModules()) {
            if (m.getName().equalsIgnoreCase(name)) {
                return m;
            }
        }
        return null;
    }

    private static List<Setting> getVisibleSettings(Module mod) {
        List<Setting> visible = new ArrayList<>();
        List<Setting> raw = mod.getSettings();
        if (raw == null) {
            return visible;
        }
        for (Setting s : raw) {
            if (s != null && s.isVisible()) {
                visible.add(s);
            }
        }
        return visible;
    }

    private static String buildSettingsSummary(List<Setting> settings) {
        if (settings == null || settings.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        int added = 0;
        for (Setting s : settings) {
            if (added >= 2) {
                break;
            }
            if (s.isCombo()) {
                if (sb.length() > 0) {
                    sb.append(" · ");
                }
                sb.append(s.getValString());
                added++;
            } else if (s.isSlider() && added == 0) {
                double v = s.getValDouble();
                String vs = s.onlyInt() ? String.valueOf((int) v) : String.format(Locale.ROOT, "%.1f", v);
                sb.append(vs);
                added++;
            }
        }
        return sb.toString();
    }

    private static void drawGroupIcon(ImDrawList dl, GuiTaxonomy.Group group, float x, float y, float activeOrHover) {
        String spriteName;
        String fallbackSprite;
        switch (group) {
            case COMBAT:
                spriteName = "netherite_sword";
                fallbackSprite = "netherite_sword";
                break;
            case MOVEMENT:
                spriteName = "phantom_membrane";
                fallbackSprite = "feather";
                break;
            case RENDER:
                spriteName = "carved_pumpkin";
                fallbackSprite = "ender_eye";
                break;
            case UTILITY:
                spriteName = "totem_of_undying";
                fallbackSprite = "totem_of_undying";
                break;
            case WORLD:
                spriteName = "jack_o_lantern";
                fallbackSprite = "diamond_pickaxe";
                break;
            case CLIENT:
            default:
                spriteName = "soul_lantern";
                fallbackSprite = "comparator";
                break;
        }
        ImGuiTextures.Region region = ImGuiTextures.item(spriteName);
        if (region == null || region.texture() == 0) {
            region = ImGuiTextures.item(fallbackSprite);
        }
        if (region != null && region.texture() != 0) {
            int tintA = (int) (195 + 60 * activeOrHover);
            dl.addImage(
                region.texture(),
                x,
                y,
                x + 16f,
                y + 16f,
                region.u0(),
                region.v0(),
                region.u1(),
                region.v1(),
                col(255, 255, 255, tintA)
            );
        } else {
            drawMiniJackOLantern(dl, x + 8f, y + 8f, 5.2f, elapsedSeconds());
        }
    }

    private static void resetParentFontScale() {
        setFontScale(1.0f);
    }

    private static void setFontScale(float scale) {
        if (imguiInstance == null) {
            imguiInstance = new ImGui();
        }
        imguiInstance.setWindowFontScale(scale);
    }

    private static void applySmoothScroll(String key) {
        float wx = ImGui.getWindowPosX();
        float wy = ImGui.getWindowPosY();
        float ww = ImGui.getWindowWidth();
        float wh = ImGui.getWindowHeight();
        int hovFlags = ImGuiHoveredFlags.ChildWindows | ImGuiHoveredFlags.AllowWhenBlockedByActiveItem;
        boolean hovered = ImGui.isMouseHoveringRect(wx, wy, wx + ww, wy + wh, false)
            || ImGui.isWindowHovered(hovFlags);
        ImGuiIO io = ImGui.getIO();
        float wheel = hovered ? io.getMouseWheel() : 0f;
        float current = ImGui.getScrollY();
        float max = ImGui.getScrollMaxY();
        Float last = scrollAppliedMap.get(key);
        float target = scrollTargetMap.getOrDefault(key, current);
        if (last == null || Math.abs(current - last) > 0.8f) {
            target = current;
        }
        if (wheel != 0f) {
            target -= wheel * 115f;
        }
        target = clamp(target, 0f, Math.max(0f, max));
        float next = damp(current, target, 18f, currentDt);
        if (Math.abs(target - next) < 0.4f) {
            next = target;
        }
        ImGui.setScrollY(next);
        scrollTargetMap.put(key, target);
        scrollAppliedMap.put(key, next);
    }

    private static float animateHover(String key, boolean hovered, float lambda) {
        float cur = hoverAnimMap.getOrDefault(key, 0f);
        float target = hovered ? 1f : 0f;
        float next = damp(cur, target, lambda, currentDt);
        hoverAnimMap.put(key, next);
        return next;
    }

    private static float damp(float current, float target, float lambda, float dt) {
        float diff = target - current;
        if (Math.abs(diff) < 0.0005f) {
            return target;
        }
        float factor = 1.0f - (float) Math.exp(-lambda * speedScale * dt);
        return current + diff * clamp(factor, 0f, 1f);
    }

    private static float easeOutCubic(float t) {
        float c = clamp(t, 0f, 1f);
        float inv = 1f - c;
        return 1f - inv * inv * inv;
    }

    private static float secondsSince(long startNs) {
        return Math.max(0f, (System.nanoTime() - startNs) / 1_000_000_000.0f);
    }

    private static float elapsedSeconds() {
        return (System.nanoTime() - startEpochNs) / 1_000_000_000.0f;
    }

    private static float clamp(float val, float min, float max) {
        return Math.max(min, Math.min(max, val));
    }

    private static void ensureSearchBuffer() {
        if (searchBuffer == null) {
            searchBuffer = new ImString(64);
            searchBuffer.set(searchQueryDraft);
        }
    }

    private static void saveConfigSafely() {
        try {
            if (Minecraft.getInstance() != null) {
                ConfigManager.save();
            }
        } catch (Throwable ignored) {
        }
    }

    public static String keyName(int keyCode) {
        if (keyCode == 0) {
            return "None";
        }
        if (keyCode < 0) {
            int btn = (keyCode <= -100) ? (-100 - keyCode) : (-keyCode - 1);
            if (btn == 2) {
                return "MMB";
            }
            if (btn == 3) {
                return "MB4";
            }
            if (btn == 4) {
                return "MB5";
            }
            return "MB" + (btn + 1);
        }
        switch (keyCode) {
            case InputConstants.KEY_RSHIFT:
                return "RSHIFT";
            case InputConstants.KEY_LSHIFT:
                return "LSHIFT";
            case InputConstants.KEY_LCONTROL:
                return "LCTRL";
            case InputConstants.KEY_RCONTROL:
                return "RCTRL";
            case InputConstants.KEY_LALT:
                return "LALT";
            case InputConstants.KEY_RALT:
                return "RALT";
            case InputConstants.KEY_SPACE:
                return "SPACE";
            case InputConstants.KEY_TAB:
                return "TAB";
            case InputConstants.KEY_CAPSLOCK:
                return "CAPS";
            case InputConstants.KEY_RETURN:
                return "ENTER";
            default:
                break;
        }
        if (keyCode >= InputConstants.KEY_F1 && keyCode <= InputConstants.KEY_F12) {
            return "F" + (keyCode - InputConstants.KEY_F1 + 1);
        }
        String utilName = InputUtil.getName(keyCode);
        if (utilName != null && !utilName.isEmpty() && !utilName.startsWith("KEY ")) {
            return utilName.toUpperCase(Locale.ROOT);
        }
        return "KEY_" + keyCode;
    }

    public static int col(int r, int g, int b, int a) {
        int cr = Math.max(0, Math.min(255, r));
        int cg = Math.max(0, Math.min(255, g));
        int cb = Math.max(0, Math.min(255, b));
        int ca = Math.max(0, Math.min(255, a));
        return (ca << 24) | (cb << 16) | (cg << 8) | cr;
    }

    public static int lerpColor(int c0, int c1, float t) {
        float k = clamp(t, 0f, 1f);
        int r0 = c0 & 0xFF;
        int g0 = (c0 >> 8) & 0xFF;
        int b0 = (c0 >> 16) & 0xFF;
        int a0 = (c0 >> 24) & 0xFF;

        int r1 = c1 & 0xFF;
        int g1 = (c1 >> 8) & 0xFF;
        int b1 = (c1 >> 16) & 0xFF;
        int a1 = (c1 >> 24) & 0xFF;

        int r = Math.round(r0 + (r1 - r0) * k);
        int g = Math.round(g0 + (g1 - g0) * k);
        int b = Math.round(b0 + (b1 - b0) * k);
        int a = Math.round(a0 + (a1 - a0) * k);
        return col(r, g, b, a);
    }

    public static String clipTextToWidth(String text, float maxW) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        if (textW(text) <= maxW) {
            return text;
        }
        String ellipsis = "...";
        float ellW = textW(ellipsis);
        int len = text.length();
        while (len > 1 && textW(text.substring(0, len)) + ellW > maxW) {
            len--;
        }
        return text.substring(0, len) + ellipsis;
    }

    public static float textW(String s) {
        if (s == null || s.isEmpty()) {
            return 0f;
        }
        ImGui.calcTextSize(tmpTextSize, s);
        return tmpTextSize.x;
    }

    public static float textH() {
        return ImGui.getTextLineHeight();
    }
}
