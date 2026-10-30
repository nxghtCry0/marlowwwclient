package com.eclipseware.imnotcheatingyouare.client.clickgui;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.macro.Macro;
import com.eclipseware.imnotcheatingyouare.client.macro.MacroAction;
import com.eclipseware.imnotcheatingyouare.client.macro.MacroManager;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.module.ModuleManager;
import com.eclipseware.imnotcheatingyouare.client.setting.ConfigManager;
import com.mojang.blaze3d.platform.InputConstants;
import imgui.ImDrawList;
import imgui.ImGui;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiMouseButton;
import imgui.flag.ImGuiStyleVar;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImString;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;

public final class ImGuiConfigPanel {

    public interface Backend {
        List<Module> getModules();
        String exportModules(List<Module> modules);
        void importConfigString(String encoded);
        void saveActiveConfig();
        void loadActiveConfig();
        File getProfilesDir();
        String getClipboard();
        void setClipboard(String text);
        List<Macro> getMacros();
        Macro getActiveMacro();
        void setActiveMacro(Macro macro);
        Macro createMacro(String name);
        void deleteMacro(Macro macro);
        void saveMacros();
        boolean isMacroRecording();
        boolean isMacroPlaying();
        void startMacroRecord(Macro macro);
        void stopMacroRecord();
        void startMacroPlay(Macro macro);
        void stopMacroPlay();
        void exportMacroToClipboard(Macro macro);
        void importMacroFromClipboard();
        boolean isInWorld();
    }

    public interface DrawCallbacks {
        int col(int r, int g, int b, int a);
        int getAccentR();
        int getAccentG();
        int getAccentB();
        String keyName(int keyCode);
        float textW(String text);
        float textH();
        default void applyFontScale() {
        }
        default void useTitleFont() {
            applyFontScale();
        }
        default void useBodyFont() {
            applyFontScale();
        }
        default void useSubFont() {
            applyFontScale();
        }
        default float animateHover(String key, boolean hovered) {
            return hovered ? 1f : 0f;
        }
        default void applySmoothScroll(String key) {
        }
        default int lerpColor(int c0, int c1, float t) {
            return t >= 0.5f ? c1 : c0;
        }
        default String clipText(String text, float maxW) {
            return text != null ? text : "";
        }
    }

    private static Backend backend = new DefaultBackend();
    private static final Set<Module> selectedForExport = new HashSet<>();
    private static boolean exportSelectionInitialized = false;
    private static String activeProfileName = "Default";
    private static String profileNameDraft = "";
    private static String exportSearchDraft = "";
    private static String macroNameDraft = "";
    private static String statusMessage = "Ready - save a local profile or share via clipboard";
    private static boolean statusIsError = false;
    private static long statusTimestampMs = 0L;
    private static Macro bindingMacro = null;
    private static long macroBindConsumedFrame = -1L;

    private static ImString profileInputBuf;
    private static ImString exportSearchBuf;
    private static ImString macroInputBuf;

    private ImGuiConfigPanel() {
    }

    public static void setBackend(Backend customBackend) {
        backend = customBackend != null ? customBackend : new DefaultBackend();
    }

    public static void resetBackend() {
        backend = new DefaultBackend();
        selectedForExport.clear();
        exportSelectionInitialized = false;
        activeProfileName = "Default";
        profileNameDraft = "";
        exportSearchDraft = "";
        macroNameDraft = "";
        statusMessage = "Ready - save a local profile or share via clipboard";
        statusIsError = false;
        bindingMacro = null;
    }

    public static String getActiveProfileName() {
        return activeProfileName;
    }

    public static String getStatusMessage() {
        return statusMessage;
    }

    public static boolean isStatusError() {
        return statusIsError;
    }

    public static Macro getBindingMacro() {
        return bindingMacro;
    }

    public static void setBindingMacro(Macro macro) {
        bindingMacro = macro;
    }

    public static boolean isMacroBinding() {
        return bindingMacro != null;
    }

    public static boolean handleMacroBindingKey(int keyCode, long frameCounter) {
        if (bindingMacro == null) {
            return false;
        }
        if (keyCode == InputConstants.KEY_ESCAPE) {
            bindingMacro = null;
            macroBindConsumedFrame = frameCounter;
            setStatus("Macro keybind cancelled", false);
            return true;
        }
        if (keyCode == InputConstants.KEY_DELETE || keyCode == InputConstants.KEY_BACKSPACE) {
            bindingMacro.setKeybind(0);
            backend.saveMacros();
            String name = bindingMacro.getName();
            bindingMacro = null;
            macroBindConsumedFrame = frameCounter;
            setStatus("Cleared keybind for macro '" + name + "'", false);
            return true;
        }
        bindingMacro.setKeybind(keyCode);
        backend.saveMacros();
        String name = bindingMacro.getName();
        bindingMacro = null;
        macroBindConsumedFrame = frameCounter;
        setStatus("Bound macro '" + name + "'", false);
        return true;
    }

    public static boolean handleMacroBindingMouseButton(int mouseBindCode, long frameCounter) {
        if (bindingMacro == null) {
            return false;
        }
        bindingMacro.setKeybind(mouseBindCode);
        backend.saveMacros();
        String name = bindingMacro.getName();
        bindingMacro = null;
        macroBindConsumedFrame = frameCounter;
        setStatus("Bound macro '" + name + "'", false);
        return true;
    }

    public static String sanitizeProfileName(String raw) {
        if (raw == null) {
            return "";
        }
        String trimmed = raw.trim();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < trimmed.length() && sb.length() < 28; i++) {
            char c = trimmed.charAt(i);
            boolean valid = Character.isLetterOrDigit(c) || c == ' ' || c == '-' || c == '_';
            if (valid) {
                sb.append(c);
            }
        }
        return sb.toString().trim();
    }

    public static List<String> listProfiles() {
        List<String> names = new ArrayList<>();
        File dir = backend.getProfilesDir();
        if (dir != null && dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    String fn = f.getName();
                    if (f.isFile() && fn.endsWith(".cfg")) {
                        int cut = fn.length() - 4;
                        String base = fn.substring(0, cut).trim();
                        if (!base.isEmpty() && !containsIgnoreCase(names, base)) {
                            names.add(base);
                        }
                    }
                }
            }
        }
        Collections.sort(names, String.CASE_INSENSITIVE_ORDER);
        if (!containsIgnoreCase(names, "Default")) {
            names.add(0, "Default");
        }
        return names;
    }

    public static boolean saveProfile(String rawName) {
        String clean = sanitizeProfileName(rawName);
        if (clean.isEmpty()) {
            setStatus("Enter a valid profile name (letters, numbers, spaces, - or _)", true);
            return false;
        }
        syncExportSelection();
        List<Module> modulesToExport = getSelectedExportModules();
        if (modulesToExport.isEmpty()) {
            modulesToExport = getVisibleModules();
        }
        String encoded = backend.exportModules(modulesToExport);
        if (encoded == null || encoded.isEmpty()) {
            setStatus("Failed to serialize config profile", true);
            return false;
        }
        File dir = backend.getProfilesDir();
        if (dir == null) {
            setStatus("Profile directory unavailable", true);
            return false;
        }
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File target = new File(dir, clean + ".cfg");
        try {
            Path path = target.toPath();
            byte[] bytes = encoded.getBytes(StandardCharsets.UTF_8);
            Files.write(path, bytes);
            backend.saveActiveConfig();
            activeProfileName = clean;
            setStatus("Saved profile '" + clean + "' (" + modulesToExport.size() + " modules)", false);
            return true;
        } catch (Exception e) {
            setStatus("Could not write profile '" + clean + "'", true);
            return false;
        }
    }

    public static boolean loadProfile(String rawName) {
        String clean = sanitizeProfileName(rawName);
        if (clean.isEmpty()) {
            setStatus("Select a valid profile to load", true);
            return false;
        }
        File dir = backend.getProfilesDir();
        if (dir == null) {
            setStatus("Profile directory unavailable", true);
            return false;
        }
        File target = new File(dir, clean + ".cfg");
        if (!target.exists()) {
            if ("Default".equalsIgnoreCase(clean)) {
                backend.loadActiveConfig();
                activeProfileName = "Default";
                setStatus("Loaded active Default config", false);
                return true;
            }
            setStatus("Profile '" + clean + "' does not exist on disk yet", true);
            return false;
        }
        try {
            Path path = target.toPath();
            byte[] bytes = Files.readAllBytes(path);
            String encoded = new String(bytes, StandardCharsets.UTF_8).trim();
            if (!isValidConfigPayload(encoded)) {
                setStatus("Profile '" + clean + "' has corrupted data", true);
                return false;
            }
            backend.importConfigString(encoded);
            backend.saveActiveConfig();
            activeProfileName = clean;
            setStatus("Loaded profile '" + clean + "'", false);
            return true;
        } catch (Exception e) {
            setStatus("Failed to load profile '" + clean + "'", true);
            return false;
        }
    }

    public static boolean deleteProfile(String rawName) {
        String clean = sanitizeProfileName(rawName);
        if (clean.isEmpty() || "Default".equalsIgnoreCase(clean)) {
            setStatus("The Default profile cannot be deleted", true);
            return false;
        }
        File dir = backend.getProfilesDir();
        if (dir == null) {
            setStatus("Profile directory unavailable", true);
            return false;
        }
        File target = new File(dir, clean + ".cfg");
        if (target.exists() && target.delete()) {
            if (clean.equalsIgnoreCase(activeProfileName)) {
                activeProfileName = "Default";
            }
            setStatus("Deleted profile '" + clean + "'", false);
            return true;
        }
        setStatus("Profile '" + clean + "' was not found", true);
        return false;
    }

    public static boolean copyConfigToClipboard() {
        syncExportSelection();
        List<Module> modulesToExport = getSelectedExportModules();
        if (modulesToExport.isEmpty()) {
            setStatus("Select at least one module to export to clipboard", true);
            return false;
        }
        String encoded = backend.exportModules(modulesToExport);
        if (encoded == null || encoded.isEmpty()) {
            setStatus("Failed to encode config for clipboard", true);
            return false;
        }
        backend.setClipboard(encoded);
        setStatus("Copied " + modulesToExport.size() + " modules to clipboard", false);
        return true;
    }

    public static boolean importConfigFromClipboard() {
        String clip = backend.getClipboard();
        if (!isValidConfigPayload(clip)) {
            setStatus("Clipboard does not contain a valid config string", true);
            return false;
        }
        backend.importConfigString(clip.trim());
        backend.saveActiveConfig();
        setStatus("Imported config from clipboard & saved", false);
        return true;
    }

    public static boolean isValidConfigPayload(String raw) {
        if (raw == null) {
            return false;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return false;
        }
        try {
            Base64.Decoder decoder = Base64.getDecoder();
            byte[] decoded = decoder.decode(trimmed);
            String json = new String(decoded, StandardCharsets.UTF_8).trim();
            return json.startsWith("{") && json.endsWith("}");
        } catch (Exception e) {
            return false;
        }
    }

    public static void syncExportSelection() {
        List<Module> visible = getVisibleModules();
        if (!exportSelectionInitialized && !visible.isEmpty()) {
            selectedForExport.addAll(visible);
            exportSelectionInitialized = true;
            return;
        }
        selectedForExport.retainAll(visible);
    }

    public static void selectAllForExport() {
        syncExportSelection();
        selectedForExport.clear();
        selectedForExport.addAll(getVisibleModules());
        setStatus("Selected all " + selectedForExport.size() + " modules for export", false);
    }

    public static void selectNoneForExport() {
        syncExportSelection();
        selectedForExport.clear();
        setStatus("Cleared module export selection", false);
    }

    public static void invertExportSelection() {
        syncExportSelection();
        List<Module> visible = getVisibleModules();
        Set<Module> next = new HashSet<>();
        for (Module m : visible) {
            if (!selectedForExport.contains(m)) {
                next.add(m);
            }
        }
        selectedForExport.clear();
        selectedForExport.addAll(next);
        setStatus("Inverted export selection (" + selectedForExport.size() + " selected)", false);
    }

    public static boolean isSelectedForExport(Module module) {
        syncExportSelection();
        return selectedForExport.contains(module);
    }

    public static void toggleExportSelection(Module module) {
        if (module == null) {
            return;
        }
        syncExportSelection();
        if (selectedForExport.contains(module)) {
            selectedForExport.remove(module);
        } else {
            selectedForExport.add(module);
        }
    }

    public static List<Module> getSelectedExportModules() {
        syncExportSelection();
        List<Module> ordered = new ArrayList<>();
        for (Module m : getVisibleModules()) {
            if (selectedForExport.contains(m)) {
                ordered.add(m);
            }
        }
        return ordered;
    }

    public static List<Module> getVisibleModules() {
        List<Module> all = backend.getModules();
        List<Module> visible = new ArrayList<>();
        if (all == null) {
            return visible;
        }
        for (Module m : all) {
            if (!GuiTaxonomy.shouldHideFromList(m)) {
                visible.add(m);
            }
        }
        return visible;
    }

    public static void renderConfigCenter(
        ImDrawList dl,
        float x,
        float y,
        float w,
        float h,
        DrawCallbacks cb
    ) {
        ensureImStrings();
        syncExportSelection();

        ImGui.setCursorScreenPos(x, y);
        int flags = ImGuiWindowFlags.NoScrollWithMouse;
        ImGui.beginChild("##config_center_scroll", w, h, false, flags);
        cb.useBodyFont();
        cb.applySmoothScroll("config_center_scroll");

        ImDrawList cdl = ImGui.getWindowDrawList();
        float pad = 12f;
        float contentW = w - pad * 2f;
        float scrollY = ImGui.getScrollY();
        float curY = y + 8f - scrollY;
        float baseX = x + pad;

        curY = renderStatusBanner(cdl, baseX, curY, contentW, cb);
        curY += 10f;
        curY = renderProfilesCard(cdl, baseX, curY, contentW, cb);
        curY += 12f;
        curY = renderClipboardAndFilterCard(cdl, baseX, curY, contentW, cb);

        float usedH = (curY + scrollY - y) + 18f;
        ImGui.setCursorPosY(Math.max(usedH, h));
        ImGui.dummy(1f, 1f);
        ImGui.endChild();
    }

    public static void renderMacroManager(
        ImDrawList dl,
        float x,
        float y,
        float w,
        float h,
        long frameCounter,
        DrawCallbacks cb
    ) {
        ensureImStrings();

        ImGui.setCursorScreenPos(x, y);
        int flags = ImGuiWindowFlags.NoScrollWithMouse;
        ImGui.beginChild("##macro_center_scroll", w, h, false, flags);
        cb.useBodyFont();
        cb.applySmoothScroll("macro_center_scroll");

        ImDrawList cdl = ImGui.getWindowDrawList();
        float pad = 12f;
        float contentW = w - pad * 2f;
        float scrollY = ImGui.getScrollY();
        float curY = y + 8f - scrollY;
        float baseX = x + pad;

        curY = renderStatusBanner(cdl, baseX, curY, contentW, cb);
        curY += 10f;
        curY = renderMacroCard(cdl, baseX, curY, contentW, frameCounter, cb);

        float usedH = (curY + scrollY - y) + 18f;
        ImGui.setCursorPosY(Math.max(usedH, h));
        ImGui.dummy(1f, 1f);
        ImGui.endChild();
    }

    private static float renderStatusBanner(
        ImDrawList dl,
        float x,
        float y,
        float w,
        DrawCallbacks cb
    ) {
        float bannerH = 34f;
        int bgCol = statusIsError
            ? cb.col(62, 22, 32, 230)
            : cb.col(24, 19, 36, 230);
        int borderCol = statusIsError
            ? cb.col(235, 95, 115, 165)
            : cb.col(cb.getAccentR(), cb.getAccentG(), cb.getAccentB(), 115);
        int dotCol = statusIsError
            ? cb.col(245, 105, 120, 255)
            : cb.col(cb.getAccentR(), cb.getAccentG(), cb.getAccentB(), 255);

        dl.addRectFilled(x, y, x + w, y + bannerH, bgCol, 7f);
        dl.addRect(x, y, x + w, y + bannerH, borderCol, 7f, 0, 1f);
        dl.addCircleFilled(x + 13f, y + bannerH * 0.5f, 4f, dotCol);

        cb.useSubFont();
        String activeBadge = "Profile: " + activeProfileName;
        float bw = cb.textW(activeBadge) + 16f;
        float bx = x + w - bw - 8f;
        float by = y + 5f;
        dl.addRectFilled(bx, by, bx + bw, by + bannerH - 10f, cb.col(36, 28, 52, 225), 5f);
        float badgeTy = y + (bannerH - cb.textH()) * 0.5f;
        dl.addText(bx + 8f, badgeTy, cb.col(cb.getAccentR(), cb.getAccentG(), cb.getAccentB(), 245), activeBadge);

        cb.useBodyFont();
        float maxMsgW = Math.max(80f, bx - (x + 26f) - 10f);
        String msg = cb.clipText(statusMessage, maxMsgW);
        float textY = y + (bannerH - cb.textH()) * 0.5f;
        dl.addText(x + 25f, textY, cb.col(236, 232, 246, 255), msg);
        return y + bannerH;
    }

    private static float renderProfilesCard(
        ImDrawList dl,
        float x,
        float y,
        float w,
        DrawCallbacks cb
    ) {
        List<String> profiles = listProfiles();
        int cols = 2;
        int rows = Math.max(1, (profiles.size() + cols - 1) / cols);
        float cardH = 116f + rows * 34f;

        dl.addRectFilled(x, y, x + w, y + cardH, cb.col(22, 18, 33, 230), 8f);
        dl.addRect(x, y, x + w, y + cardH, cb.col(255, 255, 255, 20), 8f, 0, 1f);

        cb.useTitleFont();
        dl.addText(x + 12f, y + 9f, cb.col(242, 238, 252, 255), "Local Config Profiles");
        cb.useSubFont();
        dl.addText(
            x + 12f,
            y + 28f,
            cb.col(154, 146, 178, 230),
            "Save named presets to disk or switch between PvP setups with one click."
        );

        cb.useBodyFont();
        float inputY = y + 50f;
        float saveNewW = 98f;
        float quickSaveW = 96f;
        float inputW = Math.max(120f, w - 24f - saveNewW - quickSaveW - 14f);

        if (drawTextInputBox(dl, "profile_name_input", "Profile name (e.g. Crystal)...", profileInputBuf, x + 12f, inputY, inputW, 26f, cb)) {
            profileNameDraft = profileInputBuf.get();
        }

        float saveNewX = x + 18f + inputW;
        if (drawActionButton(dl, "save_new_prof", "+ Save New", saveNewX, inputY, saveNewW, 26f, true, cb)) {
            String targetName = profileNameDraft.trim().isEmpty() ? activeProfileName : profileNameDraft;
            if (saveProfile(targetName)) {
                profileNameDraft = "";
                profileInputBuf.set("");
            }
        }

        float quickSaveX = saveNewX + saveNewW + 6f;
        if (drawActionButton(dl, "save_curr_prof", "Save Current", quickSaveX, inputY, quickSaveW, 26f, false, cb)) {
            saveProfile(activeProfileName);
        }

        float gridY = inputY + 36f;
        float colGap = 8f;
        float cellW = (w - 24f - colGap) * 0.5f;
        float cellH = 28f;

        for (int i = 0; i < profiles.size(); i++) {
            String prof = profiles.get(i);
            int colIdx = i % cols;
            int rowIdx = i / cols;
            float cx = x + 12f + colIdx * (cellW + colGap);
            float cy = gridY + rowIdx * 34f;
            boolean isActive = prof.equalsIgnoreCase(activeProfileName);
            boolean canDelete = !"Default".equalsIgnoreCase(prof);
            float loadW = canDelete ? (cellW - 28f) : cellW;

            ImGui.setCursorScreenPos(cx, cy);
            if (ImGui.invisibleButton("##load_prof_" + i, loadW, cellH)) {
                loadProfile(prof);
            }
            float hov = cb.animateHover("prof_load_" + i, ImGui.isItemHovered());

            int baseFill = isActive
                ? cb.col(cb.getAccentR(), cb.getAccentG(), cb.getAccentB(), 62)
                : cb.col(30, 24, 44, 225);
            int hovFill = isActive
                ? cb.col(cb.getAccentR(), cb.getAccentG(), cb.getAccentB(), 88)
                : cb.col(46, 36, 66, 240);
            int fill = cb.lerpColor(baseFill, hovFill, hov);
            int border = isActive
                ? cb.col(cb.getAccentR(), cb.getAccentG(), cb.getAccentB(), 205)
                : cb.lerpColor(cb.col(255, 255, 255, 22), cb.col(cb.getAccentR(), cb.getAccentG(), cb.getAccentB(), 120), hov);

            dl.addRectFilled(cx, cy, cx + loadW, cy + cellH, fill, 6f);
            dl.addRect(cx, cy, cx + loadW, cy + cellH, border, 6f, 0, 1f);

            String label = isActive ? ("* " + prof) : prof;
            String clipped = cb.clipText(label, loadW - 16f);
            float ty = cy + (cellH - cb.textH()) * 0.5f;
            dl.addText(cx + 9f + hov * 1.5f, ty, cb.col(238, 234, 248, 255), clipped);

            if (canDelete) {
                float dx = cx + loadW + 4f;
                ImGui.setCursorScreenPos(dx, cy);
                if (ImGui.invisibleButton("##del_prof_" + i, 24f, cellH)) {
                    deleteProfile(prof);
                }
                float delHov = cb.animateHover("prof_del_" + i, ImGui.isItemHovered());
                int delBg = cb.lerpColor(cb.col(38, 26, 44, 215), cb.col(195, 58, 78, 225), delHov);
                dl.addRectFilled(dx, cy, dx + 24f, cy + cellH, delBg, 5f);
                float xw = cb.textW("×");
                dl.addText(dx + (24f - xw) * 0.5f, ty, cb.col(248, 220, 228, 255), "×");
            }
        }

        return y + cardH;
    }

    private static float renderClipboardAndFilterCard(
        ImDrawList dl,
        float x,
        float y,
        float w,
        DrawCallbacks cb
    ) {
        List<Module> allVisible = getVisibleModules();
        List<Module> filtered = new ArrayList<>();
        String query = exportSearchDraft.trim().toLowerCase(Locale.ROOT);
        for (Module m : allVisible) {
            String mn = m.getName().toLowerCase(Locale.ROOT);
            if (query.isEmpty() || mn.contains(query)) {
                filtered.add(m);
            }
        }

        int cols = 2;
        int rows = Math.max(1, (filtered.size() + cols - 1) / cols);
        float rowStride = 28f;
        float listH = Math.min(196f, rows * rowStride + 8f);
        float cardH = 136f + listH;

        dl.addRectFilled(x, y, x + w, y + cardH, cb.col(22, 18, 33, 230), 8f);
        dl.addRect(x, y, x + w, y + cardH, cb.col(255, 255, 255, 20), 8f, 0, 1f);

        cb.useTitleFont();
        dl.addText(x + 12f, y + 9f, cb.col(242, 238, 252, 255), "Clipboard Share & Export Filter");
        cb.useSubFont();
        dl.addText(
            x + 12f,
            y + 28f,
            cb.col(154, 146, 178, 230),
            "Copy or paste Base64 config strings and choose which modules are included."
        );

        cb.useBodyFont();
        float btnY = y + 48f;
        float btnGap = 8f;
        float halfW = (w - 24f - btnGap) * 0.5f;
        if (drawActionButton(dl, "copy_clip_cfg", "Copy Config to Clipboard", x + 12f, btnY, halfW, 28f, true, cb)) {
            copyConfigToClipboard();
        }
        if (drawActionButton(dl, "import_clip_cfg", "Import Config from Clipboard", x + 12f + halfW + btnGap, btnY, halfW, 28f, false, cb)) {
            importConfigFromClipboard();
        }

        float filterHeaderY = btnY + 38f;
        cb.useSubFont();
        String countHeader = "Included (" + selectedForExport.size() + "/" + allVisible.size() + ")";
        dl.addText(x + 12f, filterHeaderY + (22f - cb.textH()) * 0.5f, cb.col(228, 222, 242, 255), countHeader);

        cb.useBodyFont();
        float pillW = 48f;
        float rightEdge = x + w - 12f;
        if (drawSmallPill(dl, "exp_inv", "Invert", rightEdge - pillW, filterHeaderY, pillW, 22f, cb)) {
            invertExportSelection();
        }
        if (drawSmallPill(dl, "exp_none", "None", rightEdge - pillW * 2f - 5f, filterHeaderY, pillW, 22f, cb)) {
            selectNoneForExport();
        }
        if (drawSmallPill(dl, "exp_all", "All", rightEdge - pillW * 3f - 10f, filterHeaderY, pillW, 22f, cb)) {
            selectAllForExport();
        }

        float searchW = 128f;
        float searchX = rightEdge - pillW * 3f - 16f - searchW;
        cb.useSubFont();
        if (drawTextInputBox(dl, "exp_mod_filter", "Filter...", exportSearchBuf, searchX, filterHeaderY, searchW, 22f, cb)) {
            exportSearchDraft = exportSearchBuf.get();
        }
        cb.useBodyFont();

        float listY = filterHeaderY + 28f;
        ImGui.setCursorScreenPos(x + 12f, listY);
        int gridFlags = ImGuiWindowFlags.NoScrollWithMouse;
        ImGui.beginChild("##export_mod_grid", w - 24f, listH, false, gridFlags);
        cb.useSubFont();
        cb.applySmoothScroll("export_mod_grid");
        ImDrawList gdl = ImGui.getWindowDrawList();
        float cellGap = 6f;
        float cellW = (w - 24f - cellGap) * 0.5f;
        float cellH = 24f;
        float gridScrollY = ImGui.getScrollY();

        for (int i = 0; i < filtered.size(); i++) {
            Module mod = filtered.get(i);
            int cIdx = i % cols;
            int rIdx = i / cols;
            float cx = x + 12f + cIdx * (cellW + cellGap);
            float cy = listY + rIdx * rowStride - gridScrollY;
            ImGui.setCursorScreenPos(cx, cy);
            if (ImGui.invisibleButton("##exp_mod_" + i, cellW, cellH)) {
                toggleExportSelection(mod);
            }
            boolean checked = selectedForExport.contains(mod);
            float hov = cb.animateHover("exp_mod_hov_" + mod.getName(), ImGui.isItemHovered());
            float chkAnim = cb.animateHover("exp_mod_chk_" + mod.getName(), checked);

            int bgOff = cb.lerpColor(cb.col(16, 13, 24, 215), cb.col(28, 22, 42, 235), hov);
            int bgOn = cb.col(cb.getAccentR(), cb.getAccentG(), cb.getAccentB(), 48);
            int bg = cb.lerpColor(bgOff, bgOn, chkAnim);
            int bd = cb.lerpColor(
                cb.col(255, 255, 255, 18),
                cb.col(cb.getAccentR(), cb.getAccentG(), cb.getAccentB(), 170),
                Math.max(chkAnim, hov * 0.5f)
            );
            gdl.addRectFilled(cx, cy, cx + cellW, cy + cellH, bg, 5f);
            gdl.addRect(cx, cy, cx + cellW, cy + cellH, bd, 5f, 0, 1f);

            float boxX = cx + 7f;
            float boxY = cy + (cellH - 12f) * 0.5f;
            int boxFill = cb.lerpColor(
                cb.col(28, 22, 42, 225),
                cb.col(cb.getAccentR(), cb.getAccentG(), cb.getAccentB(), 235),
                chkAnim
            );
            gdl.addRectFilled(boxX, boxY, boxX + 12f, boxY + 12f, boxFill, 3f);
            if (chkAnim > 0.1f) {
                int chkCol = cb.col(20, 14, 28, (int) (255 * chkAnim));
                gdl.addLine(boxX + 2.5f, boxY + 6.2f, boxX + 5.2f, boxY + 8.8f, chkCol, 1.6f);
                gdl.addLine(boxX + 5.2f, boxY + 8.8f, boxX + 9.5f, boxY + 3.4f, chkCol, 1.6f);
            }

            float ty = cy + (cellH - cb.textH()) * 0.5f;
            int tc = cb.lerpColor(cb.col(148, 140, 170, 225), cb.col(240, 235, 250, 255), chkAnim);
            String clippedMod = cb.clipText(mod.getName(), cellW - 28f);
            gdl.addText(cx + 24f, ty, tc, clippedMod);
        }
        ImGui.setCursorPosY(rows * rowStride + 4f);
        ImGui.dummy(1f, 1f);
        ImGui.endChild();
        cb.useBodyFont();

        return y + cardH;
    }

    private static float renderMacroCard(
        ImDrawList dl,
        float x,
        float y,
        float w,
        long frameCounter,
        DrawCallbacks cb
    ) {
        List<Macro> macros = backend.getMacros();
        Macro active = backend.getActiveMacro();
        int macroRows = Math.max(1, macros.size());
        float cardH = 224f + macroRows * 34f;

        dl.addRectFilled(x, y, x + w, y + cardH, cb.col(22, 18, 33, 230), 8f);
        dl.addRect(x, y, x + w, y + cardH, cb.col(255, 255, 255, 20), 8f, 0, 1f);

        cb.useTitleFont();
        dl.addText(x + 12f, y + 9f, cb.col(242, 238, 252, 255), "Keystroke & Mouse Macro Manager");
        cb.useSubFont();
        dl.addText(
            x + 12f,
            y + 28f,
            cb.col(154, 146, 178, 230),
            "Create input sequences, bind them to a hotkey (or middle-click), and share via clipboard."
        );

        cb.useBodyFont();
        float createY = y + 50f;
        float inputW = Math.max(110f, w - 280f);
        if (drawTextInputBox(dl, "macro_name_input", "New macro name...", macroInputBuf, x + 12f, createY, inputW, 26f, cb)) {
            macroNameDraft = macroInputBuf.get();
        }

        float btnX = x + 18f + inputW;
        if (drawActionButton(dl, "create_macro_btn", "+ Create", btnX, createY, 78f, 26f, true, cb)) {
            String clean = sanitizeProfileName(macroNameDraft);
            if (clean.isEmpty()) {
                clean = "Macro " + (macros.size() + 1);
            }
            Macro created = backend.createMacro(clean);
            backend.setActiveMacro(created);
            macroNameDraft = "";
            macroInputBuf.set("");
            setStatus("Created macro '" + clean + "'", false);
        }
        if (drawActionButton(dl, "copy_macro_btn", "Copy Macro", btnX + 84f, createY, 84f, 26f, false, cb)) {
            if (active != null) {
                backend.exportMacroToClipboard(active);
                setStatus("Copied macro '" + active.getName() + "' to clipboard", false);
            }
        }
        if (drawActionButton(dl, "import_macro_btn", "Import", btnX + 174f, createY, 70f, 26f, false, cb)) {
            backend.importMacroFromClipboard();
            setStatus("Imported macro from clipboard", false);
        }

        float listY = createY + 36f;
        for (int i = 0; i < macros.size(); i++) {
            Macro m = macros.get(i);
            float ry = listY + i * 34f;
            boolean isSelected = (m == active);
            float rowHov = cb.animateHover("macro_row_" + i, false);
            int rowFill = isSelected
                ? cb.col(cb.getAccentR(), cb.getAccentG(), cb.getAccentB(), 52)
                : cb.lerpColor(cb.col(29, 23, 42, 225), cb.col(40, 32, 58, 235), rowHov);
            dl.addRectFilled(x + 12f, ry, x + w - 12f, ry + 28f, rowFill, 6f);

            ImGui.setCursorScreenPos(x + 12f, ry);
            if (ImGui.invisibleButton("##sel_macro_" + i, w * 0.36f, 28f)) {
                backend.setActiveMacro(m);
                active = m;
            }
            cb.animateHover("macro_row_" + i, ImGui.isItemHovered());
            if (ImGui.isItemHovered() && ImGui.isMouseClicked(ImGuiMouseButton.Middle)) {
                bindingMacro = (bindingMacro == m) ? null : m;
                macroBindConsumedFrame = frameCounter;
            }

            float ty = ry + (28f - cb.textH()) * 0.5f;
            List<MacroAction> actions = m.getActions();
            int stepCount = actions != null ? actions.size() : 0;
            String macroTitle = cb.clipText(m.getName() + " (" + stepCount + " steps)", w * 0.36f - 14f);
            dl.addText(x + 20f, ty, cb.col(238, 234, 248, 255), macroTitle);

            float modeX = x + w - 266f;
            String modeTxt = m.isHoldMode() ? "Mode: Hold" : "Mode: Toggle";
            if (drawSmallPill(dl, "mac_mode_" + i, modeTxt, modeX, ry + 3f, 86f, 22f, cb)) {
                m.setHoldMode(!m.isHoldMode());
                backend.saveMacros();
            }

            float bindX = modeX + 92f;
            boolean isBindingThis = (bindingMacro == m);
            int key = m.getKeybind();
            String bindTxt = isBindingThis
                ? "Press Key..."
                : (key == 0 ? "+ Bind" : cb.keyName(key));
            if (drawSmallPill(dl, "mac_bind_" + i, bindTxt, bindX, ry + 3f, 78f, 22f, cb)) {
                if (macroBindConsumedFrame != frameCounter) {
                    bindingMacro = isBindingThis ? null : m;
                    macroBindConsumedFrame = frameCounter;
                }
            }

            if (key != 0 && !isBindingThis) {
                float clrX = bindX + 82f;
                if (drawSmallPill(dl, "mac_clr_" + i, "×", clrX, ry + 3f, 22f, 22f, cb)) {
                    m.setKeybind(0);
                    backend.saveMacros();
                    setStatus("Cleared keybind for '" + m.getName() + "'", false);
                }
            }

            if (macros.size() > 1) {
                float delX = x + w - 42f;
                if (drawSmallPill(dl, "mac_del_" + i, "Del", delX, ry + 3f, 28f, 22f, cb)) {
                    backend.deleteMacro(m);
                    setStatus("Deleted macro '" + m.getName() + "'", false);
                    break;
                }
            }
        }

        float ctrlY = listY + macroRows * 34f + 10f;
        boolean inWorld = backend.isInWorld();
        boolean recording = backend.isMacroRecording();
        boolean playing = backend.isMacroPlaying();

        if (!inWorld) {
            cb.useSubFont();
            dl.addText(
                x + 12f,
                ctrlY,
                cb.col(235, 185, 120, 235),
                "Tip: Join a world to record or play live input macros."
            );
            cb.useBodyFont();
            ctrlY += 20f;
        }

        String recLabel = recording ? "Stop Recording" : "Record Actions";
        if (drawActionButton(dl, "mac_rec_btn", recLabel, x + 12f, ctrlY, 144f, 28f, recording, cb)) {
            if (recording) {
                backend.stopMacroRecord();
                setStatus("Stopped macro recording", false);
            } else if (active != null) {
                backend.startMacroRecord(active);
                setStatus("Recording macro '" + active.getName() + "'...", false);
            }
        }

        String playLabel = playing ? "Stop Playback" : "Play Macro";
        if (drawActionButton(dl, "mac_play_btn", playLabel, x + 164f, ctrlY, 134f, 28f, playing, cb)) {
            if (playing) {
                backend.stopMacroPlay();
                setStatus("Stopped macro playback", false);
            } else if (active != null) {
                backend.startMacroPlay(active);
                setStatus("Playing macro '" + active.getName() + "'", false);
            }
        }

        if (active != null && drawActionButton(dl, "mac_clear_steps", "Clear Steps", x + 306f, ctrlY, 102f, 28f, false, cb)) {
            active.clear();
            backend.saveMacros();
            setStatus("Cleared recorded steps for '" + active.getName() + "'", false);
        }

        return y + cardH;
    }

    private static boolean drawTextInputBox(
        ImDrawList dl,
        String id,
        String hint,
        ImString buf,
        float x,
        float y,
        float w,
        float h,
        DrawCallbacks cb
    ) {
        dl.addRectFilled(x, y, x + w, y + h, cb.col(14, 11, 22, 242), 6f);
        float padY = Math.max(1f, (h - cb.textH()) * 0.5f);
        ImGui.setCursorScreenPos(x, y);
        ImGui.pushItemWidth(w);
        ImGui.pushStyleVar(ImGuiStyleVar.FramePadding, 8f, padY);
        ImGui.pushStyleColor(ImGuiCol.FrameBg, cb.col(0, 0, 0, 0));
        ImGui.pushStyleColor(ImGuiCol.Text, cb.col(240, 236, 250, 255));
        ImGui.pushStyleColor(ImGuiCol.TextDisabled, cb.col(146, 138, 168, 200));
        boolean changed = ImGui.inputTextWithHint("##" + id, hint, buf);
        boolean activeOrHov = ImGui.isItemActive() || ImGui.isItemHovered();
        ImGui.popStyleColor(3);
        ImGui.popStyleVar();
        ImGui.popItemWidth();

        float focusAnim = cb.animateHover("inp_box_" + id, activeOrHov);
        int bd = cb.lerpColor(
            cb.col(255, 255, 255, 22),
            cb.col(cb.getAccentR(), cb.getAccentG(), cb.getAccentB(), 185),
            focusAnim
        );
        dl.addRect(x, y, x + w, y + h, bd, 6f, 0, 1f);
        return changed;
    }

    private static boolean drawActionButton(
        ImDrawList dl,
        String id,
        String label,
        float x,
        float y,
        float w,
        float h,
        boolean primary,
        DrawCallbacks cb
    ) {
        ImGui.setCursorScreenPos(x, y);
        boolean clicked = ImGui.invisibleButton("##" + id, w, h);
        float hov = cb.animateHover("act_btn_" + id, ImGui.isItemHovered());

        int bg;
        int border;
        if (primary) {
            bg = cb.lerpColor(
                cb.col(cb.getAccentR(), cb.getAccentG(), cb.getAccentB(), 180),
                cb.col(cb.getAccentR(), cb.getAccentG(), cb.getAccentB(), 230),
                hov
            );
            border = cb.lerpColor(cb.col(255, 255, 255, 55), cb.col(255, 255, 255, 130), hov);
        } else {
            bg = cb.lerpColor(cb.col(32, 26, 48, 225), cb.col(48, 38, 70, 245), hov);
            border = cb.lerpColor(
                cb.col(cb.getAccentR(), cb.getAccentG(), cb.getAccentB(), 90),
                cb.col(cb.getAccentR(), cb.getAccentG(), cb.getAccentB(), 180),
                hov
            );
        }
        dl.addRectFilled(x, y, x + w, y + h, bg, 6f);
        dl.addRect(x, y, x + w, y + h, border, 6f, 0, 1f);

        float tw = cb.textW(label);
        float tx = x + (w - tw) * 0.5f;
        float ty = y + (h - cb.textH()) * 0.5f;
        int textCol = primary ? cb.col(18, 12, 24, 255) : cb.col(238, 234, 248, 255);
        dl.addText(tx, ty, textCol, label);
        return clicked;
    }

    private static boolean drawSmallPill(
        ImDrawList dl,
        String id,
        String label,
        float x,
        float y,
        float w,
        float h,
        DrawCallbacks cb
    ) {
        ImGui.setCursorScreenPos(x, y);
        boolean clicked = ImGui.invisibleButton("##" + id, w, h);
        float hov = cb.animateHover("sml_pill_" + id, ImGui.isItemHovered());
        int bg = cb.lerpColor(cb.col(34, 27, 50, 220), cb.col(54, 42, 78, 245), hov);
        int bd = cb.lerpColor(
            cb.col(255, 255, 255, 24),
            cb.col(cb.getAccentR(), cb.getAccentG(), cb.getAccentB(), 175),
            hov
        );
        dl.addRectFilled(x, y, x + w, y + h, bg, 5f);
        dl.addRect(x, y, x + w, y + h, bd, 5f, 0, 1f);
        cb.useSubFont();
        float tw = cb.textW(label);
        float tx = x + (w - tw) * 0.5f;
        float ty = y + (h - cb.textH()) * 0.5f;
        dl.addText(tx, ty, cb.col(234, 228, 246, 255), label);
        cb.useBodyFont();
        return clicked;
    }

    private static void ensureImStrings() {
        if (profileInputBuf == null) {
            profileInputBuf = new ImString(64);
        }
        if (exportSearchBuf == null) {
            exportSearchBuf = new ImString(64);
        }
        if (macroInputBuf == null) {
            macroInputBuf = new ImString(64);
        }
    }

    private static void setStatus(String message, boolean isError) {
        statusMessage = message;
        statusIsError = isError;
        statusTimestampMs = System.currentTimeMillis();
    }

    private static boolean containsIgnoreCase(List<String> list, String target) {
        for (String s : list) {
            if (s.equalsIgnoreCase(target)) {
                return true;
            }
        }
        return false;
    }

    private static final class DefaultBackend implements Backend {

        @Override
        public List<Module> getModules() {
            ImnotcheatingyouareClient client = ImnotcheatingyouareClient.INSTANCE;
            if (client == null || client.moduleManager == null) {
                return Collections.emptyList();
            }
            ModuleManager mm = client.moduleManager;
            return mm.modules;
        }

        @Override
        public String exportModules(List<Module> modules) {
            return ConfigManager.exportSpecific(modules);
        }

        @Override
        public void importConfigString(String encoded) {
            ConfigManager.importString(encoded);
        }

        @Override
        public void saveActiveConfig() {
            ConfigManager.save();
        }

        @Override
        public void loadActiveConfig() {
            ConfigManager.load();
        }

        @Override
        public File getProfilesDir() {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.gameDirectory == null) {
                return new File("config/imnotcheatingyouare/profiles");
            }
            return new File(mc.gameDirectory, "config/imnotcheatingyouare/profiles");
        }

        @Override
        public String getClipboard() {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.keyboardHandler == null) {
                return "";
            }
            KeyboardHandler kh = mc.keyboardHandler;
            return kh.getClipboard();
        }

        @Override
        public void setClipboard(String text) {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.keyboardHandler == null) {
                return;
            }
            KeyboardHandler kh = mc.keyboardHandler;
            kh.setClipboard(text);
        }

        @Override
        public List<Macro> getMacros() {
            return MacroManager.getMacros();
        }

        @Override
        public Macro getActiveMacro() {
            return MacroManager.getActiveMacro();
        }

        @Override
        public void setActiveMacro(Macro macro) {
            MacroManager.setActiveMacro(macro);
        }

        @Override
        public Macro createMacro(String name) {
            return MacroManager.createNewMacro(name);
        }

        @Override
        public void deleteMacro(Macro macro) {
            MacroManager.deleteMacro(macro);
        }

        @Override
        public void saveMacros() {
            MacroManager.save();
        }

        @Override
        public boolean isMacroRecording() {
            return MacroManager.isRecording();
        }

        @Override
        public boolean isMacroPlaying() {
            return MacroManager.isPlaying();
        }

        @Override
        public void startMacroRecord(Macro macro) {
            MacroManager.startRecord(macro);
        }

        @Override
        public void stopMacroRecord() {
            MacroManager.stopRecord();
        }

        @Override
        public void startMacroPlay(Macro macro) {
            MacroManager.startPlay(macro);
        }

        @Override
        public void stopMacroPlay() {
            MacroManager.stopPlay();
        }

        @Override
        public void exportMacroToClipboard(Macro macro) {
            MacroManager.exportToClipboard(macro);
        }

        @Override
        public void importMacroFromClipboard() {
            MacroManager.importFromClipboard();
        }

        @Override
        public boolean isInWorld() {
            Minecraft mc = Minecraft.getInstance();
            return mc != null && mc.player != null && mc.level != null;
        }
    }
}
