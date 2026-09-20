package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.utils.RenderUtils;
import imgui.ImDrawList;
import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;
import net.minecraft.client.gui.screens.Screen;
import xyz.breadloaf.imguimc.screen.EmptyScreen;

import java.awt.Color;
import java.util.*;

public class WeakDevice extends Module {

    public static WeakDevice INSTANCE;

    private boolean guiOpen = false;
    private final Map<Module, Boolean> expandedModules = new HashMap<>();
    private final Map<Category, float[]> categoryPosMap = new HashMap<>();

    // Pastel purple palette
    private static final Color PASTEL_PURPLE_ACCENT = new Color(195, 140, 245);
    private static final Color PASTEL_PURPLE_DIM = new Color(140, 100, 190);
    private static final Color CARD_BG = new Color(22, 22, 32, 235);
    private static final Color CARD_HEADER_BG = new Color(28, 28, 42, 245);
    private static final Color ITEM_BG_HOVER = new Color(40, 36, 58, 200);

    public WeakDevice() {
        super("WeakDevice", Category.Client, "Lightweight ImGui DarkClient-inspired GUI for low-end devices.");
        INSTANCE = this;
    }

    public boolean isGuiOpen() {
        return guiOpen;
    }

    public void setGuiOpen(boolean open) {
        this.guiOpen = open;
        if (open) {
            if (mc.gui != null && !(mc.gui.screen() instanceof EmptyScreen)) {
                mc.setScreenAndShow(new EmptyScreen());
            }
        } else {
            if (mc.gui != null && mc.gui.screen() instanceof EmptyScreen) {
                mc.setScreenAndShow((Screen) null);
            }
        }
    }

    public void toggleGui() {
        setGuiOpen(!guiOpen);
    }

    @Override
    public void onDisable() {
        if (guiOpen) {
            setGuiOpen(false);
        }
    }

    public void renderImGuiOverlay() {
        if (!isToggled() || mc.player == null) return;

        // Auto-close if screen is closed externally
        if (guiOpen && mc.gui != null && !(mc.gui.screen() instanceof EmptyScreen)) {
            guiOpen = false;
        }

        if (!guiOpen) return;

        float displayWidth = ImGui.getIO().getDisplaySizeX();
        float displayHeight = ImGui.getIO().getDisplaySizeY();
        ImDrawList drawList = ImGui.getForegroundDrawList();

        int accentColor = RenderUtils.toImGuiColor(PASTEL_PURPLE_ACCENT, 1.0f);
        int dimAccentColor = RenderUtils.toImGuiColor(PASTEL_PURPLE_DIM, 1.0f);
        int cardBgColor = RenderUtils.toImGuiColor(CARD_BG, 1.0f);
        int cardHeaderBgColor = RenderUtils.toImGuiColor(CARD_HEADER_BG, 1.0f);

        // --- 1. TOP BAR / HEADER MENU ---
        ImGui.setNextWindowPos(15f, 15f, ImGuiCond.FirstUseEver);
        ImGui.setNextWindowSize(600f, 45f, ImGuiCond.FirstUseEver);

        int topBarFlags = ImGuiWindowFlags.NoTitleBar | ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoScrollbar | ImGuiWindowFlags.NoCollapse;
        
        ImGui.pushStyleColor(imgui.flag.ImGuiCol.WindowBg, RenderUtils.toImGuiColor(16, 16, 24, 230));
        ImGui.pushStyleColor(imgui.flag.ImGuiCol.Border, RenderUtils.toImGuiColor(PASTEL_PURPLE_ACCENT, 0.4f));
        ImGui.pushStyleVar(imgui.flag.ImGuiStyleVar.WindowRounding, 8.0f);

        if (ImGui.begin("DarkClient_TopBar", topBarFlags)) {
            // Brand title
            ImGui.alignTextToFramePadding();
            ImGui.textColored(PASTEL_PURPLE_ACCENT.getRed()/255f, PASTEL_PURPLE_ACCENT.getGreen()/255f, PASTEL_PURPLE_ACCENT.getBlue()/255f, 1.0f, "DarkClient");
            ImGui.sameLine();
            ImGui.textDisabled(" (WeakDevice Mode)");

            ImGui.sameLine(180f);
            
            if (ImGui.button("Panic", 70f, 24f)) {
                for (Module m : ImnotcheatingyouareClient.INSTANCE.moduleManager.modules) {
                    if (m != this && m.isToggled()) {
                        m.setToggled(false);
                    }
                }
            }

            ImGui.sameLine();
            if (ImGui.button("Reset UI", 80f, 24f)) {
                categoryPosMap.clear();
            }

            ImGui.sameLine();
            if (ImGui.button("Reset Settings", 100f, 24f)) {
                com.eclipseware.imnotcheatingyouare.client.setting.ConfigManager.load();
            }

            ImGui.sameLine(ImGui.getWindowWidth() - 90f);
            if (ImGui.button("Close UI", 75f, 24f)) {
                setGuiOpen(false);
            }
        }
        ImGui.end();
        ImGui.popStyleVar();
        ImGui.popStyleColor(2);

        // --- 2. CATEGORY WINDOW PANELS ---
        Category[] categories = new Category[]{
            Category.Combat, Category.Movement, Category.Render, Category.World, Category.Misc, Category.Client
        };

        float startX = 15f;
        float startY = 70f;
        float panelWidth = 175f;
        float spacingX = 185f;

        for (int cIdx = 0; cIdx < categories.length; cIdx++) {
            Category category = categories[cIdx];
            List<Module> mods = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModules(category);

            float defaultX = startX + (cIdx % 6) * spacingX;
            float defaultY = startY + (cIdx / 6) * 320f;

            ImGui.setNextWindowPos(defaultX, defaultY, ImGuiCond.FirstUseEver);
            ImGui.setNextWindowSize(panelWidth, 300f, ImGuiCond.FirstUseEver);

            int windowFlags = ImGuiWindowFlags.NoCollapse;

            ImGui.pushStyleColor(imgui.flag.ImGuiCol.WindowBg, cardBgColor);
            ImGui.pushStyleColor(imgui.flag.ImGuiCol.TitleBg, cardHeaderBgColor);
            ImGui.pushStyleColor(imgui.flag.ImGuiCol.TitleBgActive, cardHeaderBgColor);
            ImGui.pushStyleColor(imgui.flag.ImGuiCol.Border, RenderUtils.toImGuiColor(PASTEL_PURPLE_ACCENT, 0.35f));
            ImGui.pushStyleColor(imgui.flag.ImGuiCol.Header, RenderUtils.toImGuiColor(PASTEL_PURPLE_ACCENT, 0.25f));
            ImGui.pushStyleColor(imgui.flag.ImGuiCol.HeaderHovered, RenderUtils.toImGuiColor(PASTEL_PURPLE_ACCENT, 0.45f));
            ImGui.pushStyleVar(imgui.flag.ImGuiStyleVar.WindowRounding, 6.0f);
            ImGui.pushStyleVar(imgui.flag.ImGuiStyleVar.FrameRounding, 4.0f);

            if (ImGui.begin(category.name() + "##DarkClient", windowFlags)) {
                // Top pastel purple line under title bar
                float winX = ImGui.getWindowPosX();
                float winY = ImGui.getWindowPosY();
                float winW = ImGui.getWindowWidth();
                drawList.addLine(winX, winY + 24f, winX + winW, winY + 24f, accentColor, 2.0f);

                ImGui.dummy(0f, 2f);

                for (Module mod : mods) {
                    boolean isToggled = mod.isToggled();
                    boolean isExpanded = expandedModules.getOrDefault(mod, false);

                    // Module row background accent if toggled
                    if (isToggled) {
                        float curY = ImGui.getCursorScreenPosY();
                        float curX = ImGui.getCursorScreenPosY();
                        drawList.addRectFilled(winX + 4f, curY, winX + 8f, curY + 22f, accentColor, 2.0f);
                    }

                    ImGui.pushID(mod.getName());

                    // Module toggle button
                    String label = mod.getName();
                    if (isToggled) {
                        ImGui.pushStyleColor(imgui.flag.ImGuiCol.Text, accentColor);
                    } else {
                        ImGui.pushStyleColor(imgui.flag.ImGuiCol.Text, RenderUtils.toImGuiColor(200, 200, 210, 255));
                    }

                    if (ImGui.button(label, panelWidth - 38f, 22f)) {
                        mod.toggle();
                    }

                    // Right click menu or click arrow
                    if (ImGui.isItemClicked(1)) {
                        expandedModules.put(mod, !isExpanded);
                    }

                    ImGui.popStyleColor();

                    ImGui.sameLine(panelWidth - 32f);
                    if (ImGui.button(isExpanded ? "v" : ">", 20f, 22f)) {
                        expandedModules.put(mod, !isExpanded);
                    }

                    // Render inline settings if expanded
                    if (isExpanded) {
                        ImGui.indent(8f);
                        List<Setting> settings = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingsByMod(mod);
                        if (settings.isEmpty()) {
                            ImGui.textDisabled(" No settings");
                        } else {
                            for (Setting setting : settings) {
                                renderSetting(mod, setting);
                            }
                        }
                        ImGui.unindent(8f);
                        ImGui.separator();
                    }

                    ImGui.popID();
                }
            }
            ImGui.end();

            ImGui.popStyleVar(2);
            ImGui.popStyleColor(6);
        }
    }

    private void renderSetting(Module mod, Setting s) {
        ImGui.pushID(s.getName());

        if (s.isCheck()) {
            boolean val = s.getValBoolean();
            if (ImGui.checkbox(s.getName(), val)) {
                s.setValBoolean(!val);
            }
        } else if (s.isSlider()) {
            float val = (float) s.getValDouble();
            float min = (float) s.getMin();
            float max = (float) s.getMax();
            float[] valBuf = new float[]{val};

            if (s.onlyInt()) {
                int[] intBuf = new int[]{(int) val};
                if (ImGui.sliderInt(s.getName(), intBuf, (int) min, (int) max)) {
                    s.setValDouble(intBuf[0]);
                }
            } else {
                if (ImGui.sliderFloat(s.getName(), valBuf, min, max, "%.2f")) {
                    s.setValDouble(valBuf[0]);
                }
            }
        } else if (s.isCombo()) {
            List<String> options = s.getOptions();
            String current = s.getValString();
            if (ImGui.beginCombo(s.getName(), current)) {
                for (String opt : options) {
                    boolean isSelected = opt.equals(current);
                    if (ImGui.selectable(opt, isSelected)) {
                        s.setValString(opt);
                    }
                    if (isSelected) {
                        ImGui.setItemDefaultFocus();
                    }
                }
                ImGui.endCombo();
            }
        } else if (s.isColor()) {
            Color color = new Color(s.getValColor());
            float[] col4 = new float[]{
                color.getRed() / 255f,
                color.getGreen() / 255f,
                color.getBlue() / 255f,
                color.getAlpha() / 255f
            };
            if (ImGui.colorEdit4(s.getName(), col4)) {
                Color updated = new Color(col4[0], col4[1], col4[2], col4[3]);
                s.setValColor(updated.getRGB());
            }
        }

        ImGui.popID();
    }
}
