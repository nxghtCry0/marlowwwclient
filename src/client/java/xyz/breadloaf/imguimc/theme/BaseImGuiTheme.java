package xyz.breadloaf.imguimc.theme;

import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.ImGuiStyle;
import imgui.flag.ImGuiCol;

final class BaseImGuiTheme {

    private BaseImGuiTheme() {
    }

    static void applyClassic() {
        ImGuiStyle style = ImGui.getStyle();
        applyLayout(style);
        applyDarkColors(style, 150, 105, 255, 225, 95, 205);
    }

    static void applyDark() {
        ImGuiStyle style = ImGui.getStyle();
        applyLayout(style);
        applyDarkColors(style, 185, 125, 255, 245, 110, 205);
    }

    static void applyLight() {
        ImGuiStyle style = ImGui.getStyle();
        applyLayout(style);

        style.setColor(ImGuiCol.Text, 30, 27, 42, 255);
        style.setColor(ImGuiCol.TextDisabled, 104, 96, 120, 230);
        style.setColor(ImGuiCol.WindowBg, 250, 248, 255, 252);
        style.setColor(ImGuiCol.ChildBg, 255, 255, 255, 245);
        style.setColor(ImGuiCol.PopupBg, 255, 255, 255, 250);
        style.setColor(ImGuiCol.Border, 112, 96, 140, 78);
        style.setColor(ImGuiCol.BorderShadow, 0, 0, 0, 0);
        style.setColor(ImGuiCol.FrameBg, 247, 244, 255, 255);
        style.setColor(ImGuiCol.FrameBgHovered, 232, 224, 255, 255);
        style.setColor(ImGuiCol.FrameBgActive, 218, 205, 255, 255);
        style.setColor(ImGuiCol.TitleBg, 238, 233, 249, 255);
        style.setColor(ImGuiCol.TitleBgActive, 229, 222, 247, 255);
        style.setColor(ImGuiCol.TitleBgCollapsed, 238, 233, 249, 240);
        style.setColor(ImGuiCol.MenuBarBg, 240, 236, 249, 252);
        style.setColor(ImGuiCol.ScrollbarBg, 78, 60, 110, 24);
        style.setColor(ImGuiCol.ScrollbarGrab, 108, 82, 160, 105);
        style.setColor(ImGuiCol.ScrollbarGrabHovered, 136, 88, 220, 150);
        style.setColor(ImGuiCol.ScrollbarGrabActive, 176, 96, 210, 190);
        style.setColor(ImGuiCol.CheckMark, 118, 72, 210, 255);
        style.setColor(ImGuiCol.SliderGrab, 118, 72, 210, 230);
        style.setColor(ImGuiCol.SliderGrabActive, 190, 82, 185, 255);
        style.setColor(ImGuiCol.Button, 232, 224, 255, 255);
        style.setColor(ImGuiCol.ButtonHovered, 218, 205, 255, 255);
        style.setColor(ImGuiCol.ButtonActive, 204, 188, 250, 255);
        style.setColor(ImGuiCol.Header, 239, 234, 252, 255);
        style.setColor(ImGuiCol.HeaderHovered, 224, 214, 255, 255);
        style.setColor(ImGuiCol.HeaderActive, 211, 196, 252, 255);
        style.setColor(ImGuiCol.Separator, 112, 96, 140, 68);
        style.setColor(ImGuiCol.SeparatorHovered, 136, 88, 220, 150);
        style.setColor(ImGuiCol.SeparatorActive, 190, 82, 185, 190);
        style.setColor(ImGuiCol.ResizeGrip, 136, 88, 220, 82);
        style.setColor(ImGuiCol.ResizeGripHovered, 136, 88, 220, 132);
        style.setColor(ImGuiCol.ResizeGripActive, 190, 82, 185, 175);
        style.setColor(ImGuiCol.Tab, 239, 235, 249, 255);
        style.setColor(ImGuiCol.TabHovered, 222, 211, 255, 255);
        style.setColor(ImGuiCol.TabActive, 213, 199, 250, 255);
        style.setColor(ImGuiCol.TabUnfocused, 232, 228, 241, 240);
        style.setColor(ImGuiCol.TabUnfocusedActive, 224, 214, 248, 255);
        style.setColor(ImGuiCol.DockingPreview, 136, 88, 220, 150);
        style.setColor(ImGuiCol.DockingEmptyBg, 240, 236, 249, 230);
        style.setColor(ImGuiCol.PlotLines, 118, 72, 210, 230);
        style.setColor(ImGuiCol.PlotLinesHovered, 190, 82, 185, 255);
        style.setColor(ImGuiCol.PlotHistogram, 118, 72, 210, 220);
        style.setColor(ImGuiCol.PlotHistogramHovered, 190, 82, 185, 250);
        style.setColor(ImGuiCol.TableHeaderBg, 239, 235, 249, 255);
        style.setColor(ImGuiCol.TableBorderStrong, 112, 96, 140, 82);
        style.setColor(ImGuiCol.TableBorderLight, 112, 96, 140, 48);
        style.setColor(ImGuiCol.TableRowBg, 255, 255, 255, 0);
        style.setColor(ImGuiCol.TableRowBgAlt, 80, 58, 120, 16);
        style.setColor(ImGuiCol.TextSelectedBg, 136, 88, 220, 96);
        style.setColor(ImGuiCol.DragDropTarget, 190, 82, 185, 210);
        style.setColor(ImGuiCol.NavHighlight, 136, 88, 220, 170);
        style.setColor(ImGuiCol.NavWindowingHighlight, 255, 255, 255, 180);
        style.setColor(ImGuiCol.NavWindowingDimBg, 24, 28, 36, 72);
        style.setColor(ImGuiCol.ModalWindowDimBg, 24, 28, 36, 120);
    }

    private static void applyLayout(ImGuiStyle style) {
        float scale = getScale();

        style.setAlpha(1.0f);
        style.setDisabledAlpha(0.58f);
        style.setWindowPadding(12f * scale, 10f * scale);
        style.setFramePadding(8f * scale, 5f * scale);
        style.setCellPadding(5f * scale, 4f * scale);
        style.setItemSpacing(8f * scale, 6f * scale);
        style.setItemInnerSpacing(6f * scale, 4f * scale);
        style.setIndentSpacing(14f * scale);
        style.setScrollbarSize(12f * scale);
        style.setGrabMinSize(10f * scale);
        style.setWindowRounding(10f * scale);
        style.setChildRounding(8f * scale);
        style.setFrameRounding(6f * scale);
        style.setPopupRounding(8f * scale);
        style.setScrollbarRounding(8f * scale);
        style.setGrabRounding(6f * scale);
        style.setTabRounding(6f * scale);
        style.setWindowBorderSize(1f * scale);
        style.setChildBorderSize(1f * scale);
        style.setPopupBorderSize(1f * scale);
        style.setFrameBorderSize(0f);
        style.setTabBorderSize(0f);
        style.setWindowTitleAlign(0.5f, 0.5f);
        style.setAntiAliasedLines(true);
        style.setAntiAliasedFill(true);
    }

    private static void applyDarkColors(ImGuiStyle style, int accentRed, int accentGreen, int accentBlue, int glowRed, int glowGreen, int glowBlue) {
        style.setColor(ImGuiCol.Text, 240, 240, 245, 255);
        style.setColor(ImGuiCol.TextDisabled, 140, 140, 155, 215);
        style.setColor(ImGuiCol.WindowBg, 24, 23, 28, 245);
        style.setColor(ImGuiCol.ChildBg, 30, 29, 36, 240);
        style.setColor(ImGuiCol.PopupBg, 27, 26, 33, 248);
        style.setColor(ImGuiCol.Border, 43, 42, 52, 120);
        style.setColor(ImGuiCol.BorderShadow, 0, 0, 0, 0);
        style.setColor(ImGuiCol.FrameBg, 38, 37, 46, 220);
        style.setColor(ImGuiCol.FrameBgHovered, 50, 48, 60, 240);
        style.setColor(ImGuiCol.FrameBgActive, 38, 166, 154, 255);
        style.setColor(ImGuiCol.TitleBg, 24, 23, 28, 255);
        style.setColor(ImGuiCol.TitleBgActive, 30, 29, 36, 255);
        style.setColor(ImGuiCol.TitleBgCollapsed, 24, 23, 28, 235);
        style.setColor(ImGuiCol.MenuBarBg, 30, 29, 36, 245);
        style.setColor(ImGuiCol.ScrollbarBg, 16, 15, 19, 80);
        style.setColor(ImGuiCol.ScrollbarGrab, 38, 166, 154, 160);
        style.setColor(ImGuiCol.ScrollbarGrabHovered, 38, 166, 154, 210);
        style.setColor(ImGuiCol.ScrollbarGrabActive, 0, 191, 165, 255);
        style.setColor(ImGuiCol.CheckMark, 38, 166, 154, 255);
        style.setColor(ImGuiCol.SliderGrab, 38, 166, 154, 255);
        style.setColor(ImGuiCol.SliderGrabActive, 0, 191, 165, 255);
        style.setColor(ImGuiCol.Button, 38, 37, 46, 200);
        style.setColor(ImGuiCol.ButtonHovered, 50, 48, 60, 240);
        style.setColor(ImGuiCol.ButtonActive, 38, 166, 154, 255);
        style.setColor(ImGuiCol.Header, 38, 37, 46, 180);
        style.setColor(ImGuiCol.HeaderHovered, 50, 48, 60, 220);
        style.setColor(ImGuiCol.HeaderActive, 38, 166, 154, 255);
        style.setColor(ImGuiCol.Separator, 43, 42, 52, 100);
        style.setColor(ImGuiCol.SeparatorHovered, 38, 166, 154, 180);
        style.setColor(ImGuiCol.SeparatorActive, 0, 191, 165, 230);
        style.setColor(ImGuiCol.ResizeGrip, 38, 166, 154, 100);
        style.setColor(ImGuiCol.ResizeGripHovered, 38, 166, 154, 180);
        style.setColor(ImGuiCol.ResizeGripActive, 0, 191, 165, 230);
        style.setColor(ImGuiCol.Tab, 30, 29, 36, 245);
        style.setColor(ImGuiCol.TabHovered, 50, 48, 60, 240);
        style.setColor(ImGuiCol.TabActive, 38, 166, 154, 255);
        style.setColor(ImGuiCol.TabUnfocused, 24, 23, 28, 220);
        style.setColor(ImGuiCol.TabUnfocusedActive, 38, 166, 154, 180);
        style.setColor(ImGuiCol.DockingPreview, 38, 166, 154, 160);
        style.setColor(ImGuiCol.DockingEmptyBg, 24, 23, 28, 230);
        style.setColor(ImGuiCol.PlotLines, 38, 166, 154, 230);
        style.setColor(ImGuiCol.PlotLinesHovered, 0, 191, 165, 255);
        style.setColor(ImGuiCol.PlotHistogram, 38, 166, 154, 220);
        style.setColor(ImGuiCol.PlotHistogramHovered, 0, 191, 165, 250);
        style.setColor(ImGuiCol.TableHeaderBg, 34, 33, 41, 245);
        style.setColor(ImGuiCol.TableBorderStrong, 43, 42, 52, 120);
        style.setColor(ImGuiCol.TableBorderLight, 43, 42, 52, 60);
        style.setColor(ImGuiCol.TableRowBg, 255, 255, 255, 0);
        style.setColor(ImGuiCol.TableRowBgAlt, 255, 255, 255, 18);
        style.setColor(ImGuiCol.TextSelectedBg, 38, 166, 154, 120);
        style.setColor(ImGuiCol.DragDropTarget, 0, 191, 165, 220);
        style.setColor(ImGuiCol.NavHighlight, 38, 166, 154, 190);
        style.setColor(ImGuiCol.NavWindowingHighlight, 255, 255, 255, 200);
        style.setColor(ImGuiCol.NavWindowingDimBg, 0, 0, 0, 90);
        style.setColor(ImGuiCol.ModalWindowDimBg, 0, 0, 0, 120);
    }

    private static float getScale() {
        ImGuiIO io = ImGui.getIO();
        float scale = Math.max(io.getDisplayFramebufferScaleX(), io.getDisplayFramebufferScaleY());

        if (!Float.isFinite(scale) || scale < 1.0f)
            return 1.0f;

        return scale;
    }
}
