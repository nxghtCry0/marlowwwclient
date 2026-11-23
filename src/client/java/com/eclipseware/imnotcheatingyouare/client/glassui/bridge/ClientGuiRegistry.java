package com.eclipseware.imnotcheatingyouare.client.glassui.bridge;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.clickgui.GuiTaxonomy;
import com.eclipseware.imnotcheatingyouare.client.clickgui.HudEditorScreen;
import com.eclipseware.imnotcheatingyouare.client.glassui.asset.LucideIcon;
import com.eclipseware.imnotcheatingyouare.client.glassui.asset.UiIcon;
import com.eclipseware.imnotcheatingyouare.client.glassui.bind.UiInputBind;
import com.eclipseware.imnotcheatingyouare.client.glassui.model.UiCard;
import com.eclipseware.imnotcheatingyouare.client.glassui.model.UiCatalog;
import com.eclipseware.imnotcheatingyouare.client.glassui.model.UiCategory;
import com.eclipseware.imnotcheatingyouare.client.glassui.model.UiChoice;
import com.eclipseware.imnotcheatingyouare.client.glassui.model.UiControl;
import com.eclipseware.imnotcheatingyouare.client.glassui.model.UiCustomSection;
import com.eclipseware.imnotcheatingyouare.client.glassui.model.UiHeaderAccessory;
import com.eclipseware.imnotcheatingyouare.client.glassui.model.UiTab;
import com.eclipseware.imnotcheatingyouare.client.glassui.persist.UiCatalogSnapshot;
import com.eclipseware.imnotcheatingyouare.client.glassui.persist.UiStatePersistence;
import com.eclipseware.imnotcheatingyouare.client.glassui.render.StreamproofMode;
import com.eclipseware.imnotcheatingyouare.client.glassui.render.StreamproofOverlay;
import com.eclipseware.imnotcheatingyouare.client.glassui.render.StreamproofSettings;
import com.eclipseware.imnotcheatingyouare.client.glassui.theme.AccentPreset;
import com.eclipseware.imnotcheatingyouare.client.glassui.theme.BorderStyle;
import com.eclipseware.imnotcheatingyouare.client.glassui.theme.FontGradientStyle;
import com.eclipseware.imnotcheatingyouare.client.glassui.theme.GlassSurfacePreset;
import com.eclipseware.imnotcheatingyouare.client.glassui.theme.MotionCurve;
import com.eclipseware.imnotcheatingyouare.client.glassui.theme.MutableUiTheme;
import com.eclipseware.imnotcheatingyouare.client.glassui.theme.UiThemeContext;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.module.ModuleManager;
import com.eclipseware.imnotcheatingyouare.client.module.impl.BlockESP;
import com.eclipseware.imnotcheatingyouare.client.module.impl.KillSounds;
import com.eclipseware.imnotcheatingyouare.client.setting.ConfigManager;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.setting.SettingsManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;

public final class ClientGuiRegistry {
    public record CardLocation(int categoryIndex, int tabIndex, String cardId) {
    }

    private static final class PersistedBindDto {
        boolean mouse;
        int code;
    }

    private static final class PersistedGuiPrefs {
        int version = 1;
        Boolean sidebarExpanded;
        Map<String, Boolean> cards = new LinkedHashMap<>();
        Map<String, Boolean> toggles = new LinkedHashMap<>();
        Map<String, Float> sliders = new LinkedHashMap<>();
        Map<String, Integer> choices = new LinkedHashMap<>();
        Map<String, PersistedBindDto> systemBinds = new LinkedHashMap<>();
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final MutableUiTheme THEME = new MutableUiTheme();
    private static final Map<String, Module> CARD_TO_MODULE = new LinkedHashMap<>();
    private static final Map<Module, CardLocation> MODULE_LOCATIONS = new HashMap<>();
    private static final Set<String> SYSTEM_CARD_IDS = Set.of(
        "accent_palette",
        "glass_theme",
        "font_gradient",
        "border_style",
        "layout_scale",
        "open_curve",
        "notifications",
        "sound_alerts",
        "streamproof_menu",
        "client_prefs"
    );

    private static volatile UiCatalog catalog;
    private static volatile boolean initialized = false;
    private static boolean persistedSidebarExpanded = false;
    private static int clientCategoryIndex = 5;
    private static int configCenterTabIndex = 2;
    private static int macroCenterTabIndex = 3;
    private static int settingsCategoryIndex = 6;

    private ClientGuiRegistry() {
    }

    public static MutableUiTheme theme() {
        return THEME;
    }

    public static boolean isInitialized() {
        return initialized && catalog != null;
    }

    public static UiCatalog catalog() {
        if (catalog == null) {
            init();
        }
        return catalog;
    }

    public static int clientCategoryIndex() {
        return clientCategoryIndex;
    }

    public static int configCenterTabIndex() {
        return configCenterTabIndex;
    }

    public static int macroCenterTabIndex() {
        return macroCenterTabIndex;
    }

    public static int settingsCategoryIndex() {
        return settingsCategoryIndex;
    }

    public static boolean isSidebarExpanded() {
        return persistedSidebarExpanded;
    }

    public static void setSidebarExpanded(boolean expanded) {
        persistedSidebarExpanded = expanded;
    }

    public static synchronized void init() {
        UiThemeContext.setCurrent(THEME);
        StreamproofOverlay.configure(
            new StreamproofSettings(
                () -> isFeatureEnabled("streamproof_menu"),
                () -> StreamproofMode.fromIndex(getChoice("streamproof_menu", 0)),
                "MarlowClientAsyncDwmShield"
            )
        );
        CARD_TO_MODULE.clear();
        MODULE_LOCATIONS.clear();

        Map<String, UiControl.Toggle> sharedToggles = new LinkedHashMap<>();
        Map<String, UiControl.Slider> sharedSliders = new LinkedHashMap<>();
        List<UiCategory> categories = new ArrayList<>();

        ImnotcheatingyouareClient client = ImnotcheatingyouareClient.INSTANCE;
        List<Module> allModules = (client != null && client.moduleManager != null) ? client.moduleManager.modules : List.of();

        GuiTaxonomy.Group[] groups = GuiTaxonomy.Group.values();
        for (int gIdx = 0; gIdx < groups.length; gIdx++) {
            GuiTaxonomy.Group group = groups[gIdx];
            if (group == GuiTaxonomy.Group.CLIENT) {
                clientCategoryIndex = categories.size();
            }
            List<UiTab> tabs = new ArrayList<>();
            List<String> subNames = group.getSubCategories();
            for (String sub : subNames) {
                if ("All".equalsIgnoreCase(sub)) {
                    continue;
                }
                if (group == GuiTaxonomy.Group.CLIENT && "Config Center".equalsIgnoreCase(sub)) {
                    configCenterTabIndex = tabs.size();
                    tabs.add(new UiTab("client_configs", "Config Center", List.of(), ConfigCenterSection.INSTANCE, null));
                    continue;
                }
                if (group == GuiTaxonomy.Group.CLIENT && "Macros".equalsIgnoreCase(sub)) {
                    macroCenterTabIndex = tabs.size();
                    List<Module> macroMods = GuiTaxonomy.filterModules(allModules, group, sub, "");
                    List<UiCard> macroCards = buildModuleCards(macroMods, categories.size(), tabs.size());
                    tabs.add(new UiTab("client_macros", "Macros", macroCards, MacroCenterSection.INSTANCE, null));
                    continue;
                }
                List<Module> subMods = GuiTaxonomy.filterModules(allModules, group, sub, "");
                if (subMods.isEmpty()) {
                    continue;
                }
                int targetTabIdx = tabs.size();
                List<UiCard> cards = buildModuleCards(subMods, categories.size(), targetTabIdx);
                String tabId = (group.name() + "_" + sub).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_");
                tabs.add(new UiTab(tabId, sub, cards, null, null));
            }
            if (!tabs.isEmpty()) {
                categories.add(new UiCategory(group.name().toLowerCase(Locale.ROOT), group.getLabel(), iconForGroup(group), tabs));
            }
        }

        settingsCategoryIndex = categories.size();
        categories.add(buildSettingsCategory(sharedToggles, sharedSliders));

        UiCatalog built = new UiCatalog(categories, sharedToggles, sharedSliders);
        built.setOnCardToggleListener((card, enabled, notify) -> {
            if (SYSTEM_CARD_IDS.contains(card.id())) {
                saveGuiPrefs();
                if (notify) {
                    ClientNotificationService.notifySystemCardToggle(card, enabled);
                }
            } else if (isAutoSaveEnabled()) {
                ConfigManager.save();
            }
        });
        built.setOnChoiceChangeListener((card, idx) -> {
            if (SYSTEM_CARD_IDS.contains(card.id())) {
                syncThemeFromCatalog(built);
                saveGuiPrefs();
            } else if (isAutoSaveEnabled()) {
                ConfigManager.save();
            }
        });
        built.setOnControlChangeListener(ctrl -> {
            if (ctrl.id().startsWith("sys_")) {
                syncThemeFromCatalog(built);
            }
        });

        catalog = built;
        loadGuiPrefs();
        syncThemeFromCatalog(built);
        initialized = true;
    }

    private static List<UiCard> buildModuleCards(List<Module> modules, int catIdx, int tabIdx) {
        List<UiCard> out = new ArrayList<>(modules.size());
        for (Module mod : modules) {
            UiCard card = buildCardForModule(mod);
            CARD_TO_MODULE.put(card.id(), mod);
            MODULE_LOCATIONS.put(mod, new CardLocation(catIdx, tabIdx, card.id()));
            out.add(card);
        }
        return out;
    }

    private static UiCard buildCardForModule(Module mod) {
        String id = cardIdForModule(mod);
        String name = mod.getName();
        String rawDesc = mod.getDescription();
        String subCat = GuiTaxonomy.resolveSubCategory(mod);
        String summary = (rawDesc != null && !rawDesc.isBlank())
            ? rawDesc
            : ("Configure " + name + " behavior and settings.");
        String subtitle = (rawDesc != null && !rawDesc.isBlank())
            ? rawDesc
            : (subCat + " module • Right-click for options");
        UiIcon icon = resolveModuleIcon(mod);

        ImnotcheatingyouareClient client = ImnotcheatingyouareClient.INSTANCE;
        SettingsManager sm = client != null ? client.settingsManager : null;
        List<Setting> rawSettings = sm != null ? sm.getSettingsByMod(mod) : null;
        List<Setting> uniqueSettings = deduplicateSettings(rawSettings, mod);

        Setting primaryCombo = null;
        for (Setting s : uniqueSettings) {
            if (s.isCombo() && s.getOptions() != null) {
                int optCount = s.getOptions().size();
                if (optCount >= 2 && optCount <= 4) {
                    primaryCombo = s;
                    break;
                }
            }
        }

        List<UiChoice> choices = new ArrayList<>();
        int initialChoiceIdx = 0;
        if (primaryCombo != null) {
            List<String> opts = primaryCombo.getOptions();
            String cur = primaryCombo.getValString();
            for (int i = 0; i < opts.size(); i++) {
                String optLabel = opts.get(i);
                if (optLabel.equalsIgnoreCase(cur)) {
                    initialChoiceIdx = i;
                }
                choices.add(new UiChoice(
                    id + "_mode_" + i,
                    optLabel,
                    choiceIconForLabel(optLabel, i),
                    primaryCombo.getName() + " • " + optLabel
                ));
            }
        }

        List<UiControl> controls = new ArrayList<>();
        for (Setting s : uniqueSettings) {
            if (s == primaryCombo) {
                continue;
            }
            UiControl ctrl = buildControlForSetting(id, s);
            if (ctrl != null) {
                controls.add(ctrl);
            }
        }

        if (mod.getKeyBind() != 0 || !"GUI".equalsIgnoreCase(name)) {
            UiControl.Toggle holdToggle = new UiControl.Toggle(
                id + "__hold_mode",
                "Hold Keybind Mode",
                mod.isHoldMode(),
                "Only keep " + name + " active while its bound key is held down.",
                LucideIcon.HAND,
                val -> {
                    mod.setHoldMode(val);
                    if (isAutoSaveEnabled()) {
                        ConfigManager.save();
                    }
                }
            );
            holdToggle.withSync(() -> holdToggle.setEnabledSilent(mod.isHoldMode()));
            controls.add(holdToggle);
        }

        UiControl quickControl = null;
        if (choices.isEmpty()) {
            for (UiControl ctrl : controls) {
                if ((ctrl instanceof UiControl.Slider || ctrl instanceof UiControl.Toggle) && !ctrl.id().endsWith("__hold_mode")) {
                    quickControl = ctrl;
                    break;
                }
            }
        }

        boolean isHudEditor = "HUDEditor".equalsIgnoreCase(name);
        boolean isRecConfigs = "RecommendedConfigs".equalsIgnoreCase(name);
        boolean isGuiMod = "GUI".equalsIgnoreCase(name);
        boolean toggleable = !isHudEditor && !isRecConfigs && !isGuiMod;
        String actionLabel = "Open";
        Runnable onAction = null;
        if (isHudEditor) {
            onAction = () -> {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null) {
                    mc.setScreenAndShow(new HudEditorScreen());
                }
            };
        } else if (isRecConfigs) {
            actionLabel = "Scan";
            onAction = mod::toggle;
        } else if (isGuiMod) {
            actionLabel = "Theme";
            onAction = () -> ClientGuiScreen.openSettingsTheme();
        }

        UiCustomSection customSection = null;
        if (mod instanceof BlockESP) {
            customSection = BlockEspPickerSection.INSTANCE;
        } else if (mod instanceof KillSounds) {
            customSection = KillSoundsSection.INSTANCE;
        }
        final Setting boundCombo = primaryCombo;

        UiCard card = new UiCard(
            id,
            name,
            subtitle,
            summary,
            icon,
            toggleable ? mod.isToggled() : true,
            name + " " + subCat + " " + mod.getCategory().name(),
            initialChoiceIdx,
            choices,
            quickControl,
            controls,
            true,
            toggleable,
            actionLabel,
            "Options",
            onAction,
            enabled -> {
                if (toggleable && mod.isToggled() != enabled) {
                    mod.toggle();
                }
            },
            choiceIdx -> {
                if (boundCombo != null) {
                    List<String> opts = boundCombo.getOptions();
                    if (choiceIdx >= 0 && choiceIdx < opts.size()) {
                        boundCombo.setValString(opts.get(choiceIdx));
                    }
                }
            },
            null,
            customSection
        );

        card.withSync(() -> {
            if (toggleable) {
                card.setEnabledSilent(mod.isToggled());
            }
            if (boundCombo != null) {
                List<String> opts = boundCombo.getOptions();
                String cur = boundCombo.getValString();
                for (int i = 0; i < opts.size(); i++) {
                    if (opts.get(i).equalsIgnoreCase(cur)) {
                        card.setSelectedChoiceSilent(i);
                        break;
                    }
                }
            }
        });

        return card;
    }

    private static List<Setting> deduplicateSettings(List<Setting> rawSettings, Module mod) {
        if (rawSettings == null || rawSettings.isEmpty()) {
            return List.of();
        }
        List<Setting> unique = new ArrayList<>(rawSettings.size());
        Set<String> seen = new HashSet<>();
        for (Setting s : rawSettings) {
            if (s == null || s.getName() == null) {
                continue;
            }
            String trimmedName = s.getName().trim();
            String key = trimmedName.toLowerCase(Locale.ROOT);
            if (mod instanceof BlockESP && "open block selector".equals(key)) {
                continue;
            }
            if (seen.add(key)) {
                unique.add(s);
            }
        }
        return unique;
    }

    private static UiControl buildControlForSetting(String cardId, Setting s) {
        String sName = s.getName();
        String ctrlId = cardId + "__" + sName.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_");
        if (s.isCheck()) {
            UiControl.Toggle t = new UiControl.Toggle(
                ctrlId,
                sName,
                s.getValBoolean(),
                "Toggle " + sName + " on or off.",
                LucideIcon.CHECK,
                val -> s.setValBoolean(val)
            );
            t.visibleWhen(s::isVisible);
            t.withSync(() -> t.setEnabledSilent(s.getValBoolean()));
            return t;
        }
        if (s.isSlider()) {
            String suffix = inferUnitSuffix(sName);
            int decimals = s.onlyInt() ? 0 : ((s.getMax() - s.getMin()) <= 4.0 ? 2 : 1);
            UiControl.Slider sl = new UiControl.Slider(
                ctrlId,
                sName,
                (float) s.getValDouble(),
                (float) s.getMin(),
                (float) s.getMax(),
                suffix,
                decimals,
                "Adjust " + sName + " (" + trimNum(s.getMin(), s.onlyInt()) + " to " + trimNum(s.getMax(), s.onlyInt()) + ").",
                LucideIcon.SLIDERS_HORIZONTAL,
                val -> s.setValDouble(val)
            );
            sl.visibleWhen(s::isVisible);
            sl.withSync(() -> sl.setValueSilent((float) s.getValDouble()));
            return sl;
        }
        if (s.isCombo()) {
            List<String> opts = s.getOptions() != null ? s.getOptions() : List.of();
            int initIdx = 0;
            String cur = s.getValString();
            for (int i = 0; i < opts.size(); i++) {
                if (opts.get(i).equalsIgnoreCase(cur)) {
                    initIdx = i;
                    break;
                }
            }
            UiControl.Cycle cyc = new UiControl.Cycle(
                ctrlId,
                sName,
                opts,
                initIdx,
                "Left-click or right-click to cycle " + sName + ".",
                LucideIcon.LAYERS,
                s::setValString
            );
            cyc.visibleWhen(s::isVisible);
            cyc.withSync(() -> cyc.setSelectedSilent(s.getValString()));
            return cyc;
        }
        if (s.isRange()) {
            int decimals = s.onlyInt() ? 0 : 2;
            UiControl.RangeSlider rng = new UiControl.RangeSlider(
                ctrlId,
                sName,
                (float) s.getRangeLow(),
                (float) s.getRangeHigh(),
                (float) s.getMin(),
                (float) s.getMax(),
                decimals,
                "Drag low and high handles for " + sName + ".",
                LucideIcon.SLIDERS_HORIZONTAL,
                (lo, hi) -> s.setRange(lo, hi)
            );
            rng.visibleWhen(s::isVisible);
            rng.withSync(() -> rng.setRangeSilent((float) s.getRangeLow(), (float) s.getRangeHigh()));
            return rng;
        }
        if (s.isColor()) {
            UiControl.ColorPicker cp = new UiControl.ColorPicker(
                ctrlId,
                sName,
                s.getValColor(),
                "Drag hue spectrum or click swatch for " + sName + ".",
                LucideIcon.PALETTE,
                col -> s.setValColor(col)
            );
            cp.visibleWhen(s::isVisible);
            cp.withSync(() -> cp.setColorSilent(s.getValColor()));
            return cp;
        }
        if (s.isText()) {
            UiControl.TextField tf = new UiControl.TextField(
                ctrlId,
                sName,
                s.getValText(),
                "Custom text value for " + sName + ".",
                LucideIcon.TAG,
                s::setValText
            );
            tf.visibleWhen(s::isVisible);
            tf.withSync(() -> {
                if (!tf.focused) {
                    tf.setValueSilent(s.getValText());
                }
            });
            return tf;
        }
        if (s.isCurve()) {
            List<String> presets = List.of("Humanized S-Curve", "Bell Sweep", "Ease Out", "Linear");
            int initCurveIdx = resolveCurvePresetIndex(s.getCurve());
            UiControl.Cycle curveCycle = new UiControl.Cycle(
                ctrlId,
                sName,
                presets,
                initCurveIdx,
                "Select Bezier acceleration curve for " + sName + ".",
                LucideIcon.GAUGE,
                selected -> applyCurvePreset(s, selected)
            );
            curveCycle.visibleWhen(s::isVisible);
            curveCycle.withSync(() -> {
                int idx = resolveCurvePresetIndex(s.getCurve());
                curveCycle.setSelectedSilent(presets.get(idx));
            });
            return curveCycle;
        }
        return null;
    }

    private static int resolveCurvePresetIndex(double[] c) {
        if (c == null || c.length < 4) {
            return 0;
        }
        if (Math.abs(c[0] - 0.42) < 0.08 && Math.abs(c[2] - 0.58) < 0.08) {
            return 1;
        }
        if (c[0] < 0.18 && c[1] > 0.65) {
            return 2;
        }
        if (Math.abs(c[0] - c[1]) < 0.06 && Math.abs(c[2] - c[3]) < 0.06) {
            return 3;
        }
        return 0;
    }

    private static void applyCurvePreset(Setting s, String preset) {
        if ("Bell Sweep".equalsIgnoreCase(preset)) {
            s.setCurve(0.42, 0.0, 0.58, 1.0);
        } else if ("Ease Out".equalsIgnoreCase(preset)) {
            s.setCurve(0.12, 0.82, 0.28, 1.0);
        } else if ("Linear".equalsIgnoreCase(preset)) {
            s.setCurve(0.25, 0.25, 0.75, 0.75);
        } else {
            s.setCurve(0.25, 0.10, 0.25, 1.00);
        }
    }

    private static UiCategory buildSettingsCategory(
        Map<String, UiControl.Toggle> sharedToggles,
        Map<String, UiControl.Slider> sharedSliders
    ) {
        UiHeaderAccessory swatchAccessory = UiHeaderAccessory.dualColorSwatch(
            th -> new int[]{th.accentColor(), th.secondaryAccentColor()}
        );

        UiControl.Slider themeHue = sysSlider(sharedSliders, "theme_hue", "Custom Hue", 205f, 0f, 360f, "°", 0, "Primary accent hue when Custom palette is active.", LucideIcon.PALETTE);
        UiControl.Slider themeSat = sysSlider(sharedSliders, "theme_sat", "Saturation", 82f, 25f, 100f, "%", 0, "Color richness of your custom accent.", LucideIcon.DROPLET);
        UiControl.Slider themeBri = sysSlider(sharedSliders, "theme_bri", "Brightness", 100f, 55f, 100f, "%", 0, "Overall luminance of your primary accent.", LucideIcon.SUN);
        UiControl.Slider themeShift = sysSlider(sharedSliders, "theme_shift", "Dual-Tone Shift", 24f, -90f, 90f, "°", 0, "Hue offset used for secondary gradient highlights.", LucideIcon.SPARKLES);

        UiControl.Slider glassAlpha = sysSlider(sharedSliders, "glass_alpha", "Window Opacity", 100f, 65f, 115f, "%", 0, "Frosted glass window surface opacity.", LucideIcon.LAYERS);
        UiControl.Slider themeCardBright = sysSlider(sharedSliders, "theme_card_brightness", "Card Frost", 100f, 60f, 145f, "%", 0, "Inner glass card frost brightness.", LucideIcon.SUN);
        UiControl.Toggle blurOpt = sysToggle(sharedToggles, "blur_bg_opt", "World Blur", true, "Blur the in-game viewport behind the Glass UI.", LucideIcon.EYE);
        UiControl.Slider themeScrim = sysSlider(sharedSliders, "theme_scrim", "Backdrop Dim", 72f, 0f, 150f, "", 0, "Dark vignette scrim behind the menu.", LucideIcon.BOX);

        UiControl.Slider themeGradStrength = sysSlider(sharedSliders, "theme_grad_strength", "Gradient Tint", 100f, 0f, 150f, "%", 0, "Accent blend intensity on Satoshi headings.", LucideIcon.SPARKLES);
        UiControl.Slider cornerScale = sysSlider(sharedSliders, "corner_scale", "Corner Curvature", 100f, 50f, 140f, "%", 0, "Roundness of window, cards, and pills.", LucideIcon.BOX);
        UiControl.Slider themeCardGlow = sysSlider(sharedSliders, "theme_card_glow", "Active Card Glow", 65f, 0f, 125f, "%", 0, "Accent tint glow on enabled module cards.", LucideIcon.FLAME);

        UiControl.Slider uiScale = sysSlider(sharedSliders, "ui_scale", "Menu Scale", 96f, 70f, 112f, "%", 0, "Overall scale of the Glass UI window.", LucideIcon.MOVE_DIAGONAL);
        UiControl.Slider animSpeed = sysSlider(sharedSliders, "anim_speed", "Animation Speed", 100f, 50f, 180f, "%", 0, "Transition speed across springs, tabs, and ripples.", LucideIcon.GAUGE);
        UiControl.Toggle typingFx = sysToggle(sharedToggles, "typing_fx", "Kinetic Typing", true, "Cipher-decode typewriter animation on subtitles and search.", LucideIcon.ZAP);

        UiControl.Slider notifDuration = sysSlider(sharedSliders, "notif_duration", "Alert Duration", 2.2f, 1.0f, 5.0f, "s", 1, "How long HUD notification cards stay visible.", LucideIcon.CLOCK);
        UiControl.Toggle notifMacros = sysToggle(sharedToggles, "notif_macros", "Macro Alerts", true, "Show HUD notifications when macros record or play.", LucideIcon.REPEAT);
        UiControl.Toggle notifTyping = sysToggle(sharedToggles, "notif_typing", "Alert Typing FX", true, "Animate notification text with kinetic cipher reveal.", LucideIcon.SPARKLES);
        UiControl.Toggle notifStreamproofHide = sysToggle(sharedToggles, "notif_streamproof_hide", "Mute on Streamproof", false, "Hide HUD alerts completely while Streamproof overlay is active.", LucideIcon.EYE);

        UiControl.Toggle uiSounds = sysToggle(sharedToggles, "ui_sounds", "Enable UI Audio", true, "Play synthesized clicks, typing thocks, and chimes.", LucideIcon.BELL);
        UiControl.Slider soundVolume = sysSlider(sharedSliders, "sound_volume", "Master Volume", 100f, 0f, 150f, "%", 0, "Master volume for all Glass UI sound effects.", LucideIcon.SLIDERS_HORIZONTAL);
        UiControl.Toggle toggleChimes = sysToggle(sharedToggles, "toggle_chimes", "Module Chimes", true, "Play harmonic on/off chimes when modules toggle.", LucideIcon.SPARKLES);
        UiControl.Action testSoundAction = new UiControl.Action(
            "sys_test_sound",
            "Preview Audio",
            "Play",
            "Played audio sample",
            "Preview the active UI sound profile.",
            LucideIcon.SPARKLES,
            ClientUiSounds.INSTANCE::playNotify
        );

        UiControl.Toggle autoSaveOpt = sysToggle(sharedToggles, "auto_save_opt", "Auto-Save Config", true, "Automatically save module and UI changes to disk.", LucideIcon.SAVE);
        UiControl.Toggle toastOpt = sysToggle(sharedToggles, "toast_alerts_opt", "In-Menu Toasts", true, "Show confirmation toast pills at the bottom of the menu.", LucideIcon.CHECK);
        UiControl.Toggle smartEsc = sysToggle(sharedToggles, "esc_unfocus", "Smart Escape", true, "First Escape clears search or closes detail view before closing GUI.", LucideIcon.ARROW_LEFT);
        UiControl.Toggle hideServerIp = sysToggle(sharedToggles, "hide_server_ip", "Hide Server IP", false, "Mask multiplayer server IP inside the Profile dashboard.", LucideIcon.SHIELD);
        UiControl.Action resetThemeAction = new UiControl.Action(
            "sys_reset_theme",
            "Reset Theme Defaults",
            "Reset",
            "Restored default Glass UI theme",
            "Restore default Glass UI colors, glass surface, and layout.",
            LucideIcon.REPEAT,
            ClientGuiRegistry::resetThemeDefaults
        );

        UiCard accentCard = new UiCard(
            "accent_palette",
            "Accent Palette",
            "Primary accent & custom HSB • Right-click for sliders",
            "Choose a signature accent color or dial in Custom HSB with dual-tone gradient shift.",
            LucideIcon.PALETTE,
            true,
            "theme color accent custom hue saturation brightness sky emerald rose pink",
            0,
            List.of(
                new UiChoice("acc_sky", "Ocean Sky", LucideIcon.DROPLET, "Electric sky-blue primary accent across switches and sliders."),
                new UiChoice("acc_emerald", "Emerald", LucideIcon.SPARKLES, "Crisp emerald-mint accent with cool botanical highlights."),
                new UiChoice("acc_rose", "Rose Pink", LucideIcon.HEART_PULSE, "Vibrant rose-crimson accent with warm contrast."),
                new UiChoice("acc_custom", "Custom", LucideIcon.SLIDERS_HORIZONTAL, "Uses your Custom Hue, Saturation, and Brightness sliders.")
            ),
            null,
            List.of(themeHue, themeSat, themeBri, themeShift),
            false,
            false,
            "Apply",
            "Options",
            null,
            null,
            idx -> THEME.setAccentPreset(AccentPreset.fromIndex(idx)),
            swatchAccessory,
            null
        );

        UiCard glassCard = new UiCard(
            "glass_theme",
            "Glass Surface",
            "Window material, frost brightness, and world blur",
            "Customize the frosted glass surface tint, card brightness, world blur, and backdrop dimming.",
            LucideIcon.LAYERS,
            true,
            "glass surface theme stone obsidian glacier midnight opacity blur frost",
            0,
            List.of(
                new UiChoice("gl_stone", "Stone", LucideIcon.SUN, "Signature warm charcoal-stone frosted glass surface."),
                new UiChoice("gl_obsidian", "Obsidian", LucideIcon.BOX, "Deep neutral carbon-black glass with high contrast."),
                new UiChoice("gl_glacier", "Glacier", LucideIcon.WIND, "Cool slate-blue tinted glass with icy reflections."),
                new UiChoice("gl_midnight", "Midnight", LucideIcon.SPARKLES, "Dark indigo-violet glass built for night sessions.")
            ),
            null,
            List.of(glassAlpha, themeCardBright, blurOpt, themeScrim),
            false,
            false,
            "Apply",
            "Options",
            null,
            null,
            idx -> THEME.setGlassSurface(GlassSurfacePreset.fromIndex(idx)),
            null,
            null
        );

        UiCard fontCard = new UiCard(
            "font_gradient",
            "Typography Tint",
            "Satoshi font gradient style across titles and labels",
            "Select how Satoshi headings and labels blend white, silver, gold, and accent gradients.",
            LucideIcon.SPARKLES,
            true,
            "font gradient text pearl aurora champagne platinum satoshi",
            0,
            List.of(
                new UiChoice("fg_pearl", "Pearl", LucideIcon.SPARKLES, "Soft ivory-to-accent gradient across headings and labels."),
                new UiChoice("fg_aurora", "Aurora", LucideIcon.PALETTE, "Blends your primary and dual-tone accent into titles."),
                new UiChoice("fg_champagne", "Champagne", LucideIcon.SUN, "Warm gold and cream finish across menu typography."),
                new UiChoice("fg_platinum", "Platinum", LucideIcon.SHIELD, "Clean silver-ice monochrome gradient with minimal tint.")
            ),
            null,
            List.of(themeGradStrength, themeShift),
            false,
            false,
            "Apply",
            "Options",
            null,
            null,
            idx -> THEME.setFontGradientStyle(FontGradientStyle.fromIndex(idx)),
            swatchAccessory,
            null
        );

        UiCard borderCard = new UiCard(
            "border_style",
            "Card Borders",
            "Rim lighting, active glow, and corner curvature",
            "Configure card border sharpness, active module accent glow, and corner roundness.",
            LucideIcon.BOX,
            true,
            "card border rim glow roundness corner style crisp minimal",
            0,
            List.of(
                new UiChoice("bd_subtle", "Subtle", LucideIcon.LAYERS, "Balanced top-lit frosted rim around cards and pills."),
                new UiChoice("bd_accent", "Accent", LucideIcon.PALETTE, "Tints active module borders with your accent color."),
                new UiChoice("bd_crisp", "Crisp", LucideIcon.FOCUS, "Bright high-contrast glass edges for sharp separation."),
                new UiChoice("bd_minimal", "Minimal", LucideIcon.FEATHER, "Low-contrast hairline borders for a flat clean look.")
            ),
            null,
            List.of(cornerScale, themeCardGlow),
            false,
            false,
            "Apply",
            "Options",
            null,
            null,
            idx -> THEME.setBorderStyle(BorderStyle.fromIndex(idx)),
            null,
            null
        );

        UiCard scaleCard = new UiCard(
            "layout_scale",
            "Menu Scale",
            "Resize GUI window and corner proportions",
            "Scale the entire menu interface to fit compact laptops or high-resolution monitors.",
            LucideIcon.MOVE_DIAGONAL,
            true,
            "menu scale size zoom layout roundness",
            0,
            List.of(),
            uiScale,
            List.of(uiScale, cornerScale),
            false,
            false,
            "Apply",
            "Options",
            null,
            null,
            null,
            null,
            null
        );

        UiCard motionCard = new UiCard(
            "open_curve",
            "Motion & Typing",
            "Transition physics and typewriter text animations",
            "Choose the easing curve, transition speed, and kinetic typing animations across the menu.",
            LucideIcon.GAUGE,
            true,
            "motion physics curve spring ease snappy animation speed typing typewriter",
            1,
            List.of(
                new UiChoice("mc_linear", "Linear", LucideIcon.ARROW_LEFT, "Direct constant-rate transition without overshoot."),
                new UiChoice("mc_spring", "Spring", LucideIcon.WIND, "Responsive elastic bounce when opening panels."),
                new UiChoice("mc_ease", "Ease", LucideIcon.FEATHER, "Smooth cubic deceleration for calm transitions."),
                new UiChoice("mc_snappy", "Snappy", LucideIcon.ZAP, "Fast low-latency transitions for quick mid-game edits.")
            ),
            null,
            List.of(animSpeed, typingFx),
            false,
            false,
            "Apply",
            "Options",
            null,
            null,
            idx -> THEME.setMotionCurve(MotionCurve.fromIndex(idx)),
            null,
            null
        );

        UiCard notifCard = new UiCard(
            "notifications",
            "Notifications",
            "HUD alerts when modules toggle or macros run",
            "Displays sleek glass HUD cards with typing animation whenever modules toggle on or off.",
            LucideIcon.BELL,
            true,
            "notifications hud alerts toggle toast popup",
            0,
            List.of(
                new UiChoice("nf_br", "Bot Right", LucideIcon.BELL, "Stacks notification cards in the bottom-right corner."),
                new UiChoice("nf_tr", "Top Right", LucideIcon.COMPASS, "Stacks notification cards in the top-right corner."),
                new UiChoice("nf_center", "Center", LucideIcon.FOCUS, "Displays notifications centered above your hotbar."),
                new UiChoice("nf_compact", "Compact", LucideIcon.FEATHER, "Slim single-line notification pills in bottom-right.")
            ),
            null,
            List.of(notifDuration, notifMacros, notifTyping, notifStreamproofHide),
            true,
            true,
            "Apply",
            "Options",
            null,
            null,
            null,
            null,
            null
        );

        UiCard soundCard = new UiCard(
            "sound_alerts",
            "Custom Sounds",
            "Synthesized studio clicks, typing thocks, and chimes",
            "Select your custom audio profile and master volume for clicks, typing, and module toggles.",
            LucideIcon.SPARKLES,
            true,
            "custom sounds audio click chime thock volume",
            0,
            List.of(
                new UiChoice("snd_glass", "Glass", LucideIcon.SPARKLES, "Crisp glass clicks and harmonic bell chimes."),
                new UiChoice("snd_thock", "Thock", LucideIcon.BOX, "Warm mechanical key-switch thock and deeper chimes."),
                new UiChoice("snd_min", "Minimal", LucideIcon.FEATHER, "Ultra-quiet micro-ticks for distraction-free play."),
                new UiChoice("snd_van", "Vanilla", LucideIcon.CPU, "Classic Minecraft UI button click audio.")
            ),
            null,
            List.of(uiSounds, soundVolume, toggleChimes, testSoundAction),
            false,
            false,
            "Apply",
            "Options",
            null,
            null,
            null,
            null,
            null
        );

        UiCard streamproofCard = new UiCard(
            "streamproof_menu",
            "Streamproof GUI",
            "Hides menu & alerts on Parsec, OBS, and Discord",
            "Renders the GUI and HUD notifications onto an async DWM capture-excluded surface.",
            LucideIcon.EYE,
            false,
            "streamproof parsec obs discord desktop capture hide menu",
            0,
            List.of(
                new UiChoice("sp_overlay", "Overlay", LucideIcon.EYE, "Capture-excluded DWM glass invisible to Parsec and OBS."),
                new UiChoice("sp_window", "Window", LucideIcon.LAYERS, "Window-synced capture-excluded surface for windowed play."),
                new UiChoice("sp_crisp", "Crisp", LucideIcon.FOCUS, "Lighter streamproof glass surface with sharper text contrast."),
                new UiChoice("sp_ingame", "In-Game", LucideIcon.CPU, "Standard in-game GPU rendering visible on screen shares.")
            ),
            null,
            List.of(glassAlpha, uiScale),
            true,
            true,
            "Apply",
            "Options",
            null,
            enabled -> {
                if (!enabled) {
                    StreamproofOverlay.hide();
                }
            },
            null,
            null,
            null
        );

        UiCard prefsCard = new UiCard(
            "client_prefs",
            "Preferences",
            "Auto-save, privacy IP mask, Escape key, and reset",
            "Configure auto-save, streamer IP masking in Profile, Escape behavior, and theme reset.",
            LucideIcon.SETTINGS,
            true,
            "preferences autosave config escape privacy ip reset",
            0,
            List.of(),
            autoSaveOpt,
            List.of(autoSaveOpt, toastOpt, smartEsc, hideServerIp, resetThemeAction),
            false,
            false,
            "Apply",
            "Options",
            null,
            null,
            null,
            null,
            null
        );

        List<UiTab> tabs = List.of(
            new UiTab("settings_theme", "Theme", List.of(accentCard, glassCard, fontCard, borderCard), null, null),
            new UiTab("settings_layout", "Layout", List.of(scaleCard, motionCard), null, null),
            new UiTab("settings_system", "System", List.of(notifCard, soundCard, streamproofCard, prefsCard), null, null)
        );
        return new UiCategory("settings", "Settings", LucideIcon.SETTINGS, tabs);
    }

    private static UiControl.Toggle sysToggle(
        Map<String, UiControl.Toggle> map,
        String shortId,
        String label,
        boolean def,
        String desc,
        LucideIcon icon
    ) {
        String id = "sys_" + shortId;
        UiControl.Toggle t = new UiControl.Toggle(id, label, def, desc, icon, val -> {
            if (catalog != null) {
                syncThemeFromCatalog(catalog);
                saveGuiPrefs();
            }
        });
        map.put(shortId, t);
        map.put(id, t);
        return t;
    }

    private static UiControl.Slider sysSlider(
        Map<String, UiControl.Slider> map,
        String shortId,
        String label,
        float def,
        float min,
        float max,
        String suffix,
        int decimals,
        String desc,
        LucideIcon icon
    ) {
        String id = "sys_" + shortId;
        UiControl.Slider s = new UiControl.Slider(id, label, def, min, max, suffix, decimals, desc, icon, val -> {
            if (catalog != null) {
                syncThemeFromCatalog(catalog);
            }
        });
        map.put(shortId, s);
        map.put(id, s);
        return s;
    }

    public static void syncThemeFromCatalog() {
        if (catalog != null) {
            syncThemeFromCatalog(catalog);
        }
    }

    private static void syncThemeFromCatalog(UiCatalog cat) {
        THEME.setAccentPreset(AccentPreset.fromIndex(cat.getChoice("accent_palette", 0)));
        THEME.setGlassSurface(GlassSurfacePreset.fromIndex(cat.getChoice("glass_theme", 0)));
        THEME.setFontGradientStyle(FontGradientStyle.fromIndex(cat.getChoice("font_gradient", 0)));
        THEME.setBorderStyle(BorderStyle.fromIndex(cat.getChoice("border_style", 0)));
        THEME.setMotionCurve(MotionCurve.fromIndex(cat.getChoice("open_curve", 1)));
        THEME.setCustomHue(cat.getSlider("theme_hue", 205f));
        THEME.setCustomSaturation(cat.getSlider("theme_sat", 82f));
        THEME.setCustomBrightness(cat.getSlider("theme_bri", 100f));
        THEME.setDualToneShift(cat.getSlider("theme_shift", 24f));
        THEME.setGradientStrength(clamp(cat.getSlider("theme_grad_strength", 100f) / 100f, 0f, 1.5f));
        THEME.setCardFrostScale(clamp(cat.getSlider("theme_card_brightness", 100f) / 100f, 0.60f, 1.45f));
        THEME.setBackdropScrimBase(clamp(cat.getSlider("theme_scrim", 72f), 0f, 150f));
        THEME.setActiveGlowScale(clamp(cat.getSlider("theme_card_glow", 65f) / 100f, 0f, 1.25f));
        THEME.setUiSizeScale(clamp(cat.getSlider("ui_scale", 96f) / 100f, 0.70f, 1.12f));
        THEME.setCornerRadiusScale(clamp(cat.getSlider("corner_scale", 100f) / 100f, 0.5f, 1.4f));
        THEME.setGlassOpacityScale(clamp(cat.getSlider("glass_alpha", 100f) / 100f, 0.65f, 1.15f));
        THEME.setAnimationSpeedScale(clamp(cat.getSlider("anim_speed", 100f) / 100f, 0.5f, 1.8f));
        THEME.setBlurEnabled(cat.getBool("blur_bg_opt", true));
        THEME.setTypingAnimationEnabled(cat.getBool("typing_fx", true));
        THEME.setToastsEnabled(cat.getBool("toast_alerts_opt", true));

        ImnotcheatingyouareClient client = ImnotcheatingyouareClient.INSTANCE;
        if (client != null && client.moduleManager != null && client.settingsManager != null) {
            ModuleManager mm = client.moduleManager;
            SettingsManager sm = client.settingsManager;
            Module themeMod = mm.getModule("Theme");
            if (themeMod != null) {
                Setting accSet = sm.getSettingByName(themeMod, "Accent Color");
                if (accSet != null && accSet.isColor()) {
                    accSet.setValColor(THEME.accentColor());
                }
            }
        }
    }

    public static void resetThemeDefaults() {
        UiCatalog cat = catalog();
        cat.setChoice("accent_palette", 0, true);
        cat.setChoice("glass_theme", 0, true);
        cat.setChoice("font_gradient", 0, true);
        cat.setChoice("border_style", 0, true);
        cat.setChoice("open_curve", 1, true);
        cat.setSlider("sys_theme_hue", 205f);
        cat.setSlider("sys_theme_sat", 82f);
        cat.setSlider("sys_theme_bri", 100f);
        cat.setSlider("sys_theme_shift", 24f);
        cat.setSlider("sys_glass_alpha", 100f);
        cat.setSlider("sys_theme_card_brightness", 100f);
        cat.setSlider("sys_theme_scrim", 72f);
        cat.setSlider("sys_theme_grad_strength", 100f);
        cat.setSlider("sys_corner_scale", 100f);
        cat.setSlider("sys_theme_card_glow", 65f);
        cat.setSlider("sys_ui_scale", 96f);
        cat.setSlider("sys_anim_speed", 100f);
        cat.setBool("sys_blur_bg_opt", true);
        syncThemeFromCatalog(cat);
        saveGuiPrefs();
    }

    private static File prefsFile() {
        Minecraft mc = Minecraft.getInstance();
        File root = (mc != null && mc.gameDirectory != null) ? mc.gameDirectory : new File(".");
        File dir = new File(root, "config/imnotcheatingyouare");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return new File(dir, "glass_ui.json");
    }

    public static void saveGuiPrefs() {
        if (catalog == null) {
            return;
        }
        try {
            UiCatalogSnapshot full = UiStatePersistence.capture(catalog);
            PersistedGuiPrefs dto = new PersistedGuiPrefs();
            dto.sidebarExpanded = persistedSidebarExpanded;
            for (String sysId : SYSTEM_CARD_IDS) {
                if (full.cards.containsKey(sysId)) {
                    dto.cards.put(sysId, full.cards.get(sysId));
                }
                if (full.choices.containsKey(sysId)) {
                    dto.choices.put(sysId, full.choices.get(sysId));
                }
            }
            for (Map.Entry<String, Boolean> e : full.toggles.entrySet()) {
                if (e.getKey().startsWith("sys_")) {
                    dto.toggles.put(e.getKey(), e.getValue());
                }
            }
            for (Map.Entry<String, Float> e : full.sliders.entrySet()) {
                if (e.getKey().startsWith("sys_")) {
                    dto.sliders.put(e.getKey(), e.getValue());
                }
            }
            Map<String, UiInputBind> sysBinds = ClientKeybindBridge.INSTANCE.systemBindsSnapshot();
            for (Map.Entry<String, UiInputBind> e : sysBinds.entrySet()) {
                UiInputBind b = e.getValue();
                if (b != null && b.isBound()) {
                    PersistedBindDto bDto = new PersistedBindDto();
                    bDto.mouse = b.isMouse();
                    bDto.code = b.code();
                    dto.systemBinds.put(e.getKey(), bDto);
                }
            }
            Files.writeString(prefsFile().toPath(), GSON.toJson(dto), StandardCharsets.UTF_8);
        } catch (Exception ignored) {
        }
    }

    public static void loadGuiPrefs() {
        if (catalog == null) {
            return;
        }
        try {
            File file = prefsFile();
            if (!file.exists()) {
                return;
            }
            String raw = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            PersistedGuiPrefs dto = GSON.fromJson(raw, PersistedGuiPrefs.class);
            if (dto == null) {
                return;
            }
            if (dto.sidebarExpanded != null) {
                persistedSidebarExpanded = dto.sidebarExpanded;
            }
            UiCatalogSnapshot snap = new UiCatalogSnapshot();
            if (dto.cards != null) {
                snap.cards.putAll(dto.cards);
            }
            if (dto.toggles != null) {
                snap.toggles.putAll(dto.toggles);
            }
            if (dto.sliders != null) {
                snap.sliders.putAll(dto.sliders);
            }
            if (dto.choices != null) {
                snap.choices.putAll(dto.choices);
            }
            UiStatePersistence.apply(catalog, snap);
            if (dto.systemBinds != null) {
                Map<String, UiInputBind> binds = new LinkedHashMap<>();
                for (Map.Entry<String, PersistedBindDto> e : dto.systemBinds.entrySet()) {
                    PersistedBindDto pb = e.getValue();
                    if (pb != null) {
                        UiInputBind parsed = pb.mouse ? UiInputBind.mouse(pb.code) : UiInputBind.key(pb.code);
                        if (parsed.isBound()) {
                            binds.put(e.getKey(), parsed);
                        }
                    }
                }
                ClientKeybindBridge.INSTANCE.loadSystemBinds(binds);
            }
            syncThemeFromCatalog(catalog);
        } catch (Exception ignored) {
        }
    }

    public static boolean isFeatureEnabled(String id) {
        return catalog != null && catalog.isEnabled(id);
    }

    public static void setFeatureEnabled(String id, boolean enabled, boolean notify) {
        catalog().setEnabled(id, enabled, notify);
    }

    public static boolean getBool(String shortOrFullId, boolean def) {
        if (catalog == null) {
            return def;
        }
        return catalog.getBool(shortOrFullId, catalog.getBool("sys_" + shortOrFullId, def));
    }

    public static float getSlider(String shortOrFullId, float def) {
        if (catalog == null) {
            return def;
        }
        return catalog.getSlider(shortOrFullId, catalog.getSlider("sys_" + shortOrFullId, def));
    }

    public static int getChoice(String id, int def) {
        return catalog != null ? catalog.getChoice(id, def) : def;
    }

    public static boolean isSoundEnabled() {
        return getBool("ui_sounds", true);
    }

    public static boolean isAutoSaveEnabled() {
        return getBool("auto_save_opt", true);
    }

    public static boolean isServerIpHidden() {
        return getBool("hide_server_ip", false);
    }

    public static Module findModuleByCardId(String cardId) {
        return CARD_TO_MODULE.get(cardId);
    }

    public static CardLocation findLocationForModule(Module mod) {
        return MODULE_LOCATIONS.get(mod);
    }

    public static int activeModuleCount() {
        ImnotcheatingyouareClient client = ImnotcheatingyouareClient.INSTANCE;
        if (client == null || client.moduleManager == null) {
            return 0;
        }
        int count = 0;
        ModuleManager mm = client.moduleManager;
        for (Module m : mm.modules) {
            if (!GuiTaxonomy.shouldHideFromList(m) && m.isToggled() && !isUtilityPassiveModule(m)) {
                count++;
            }
        }
        return count;
    }

    public static int totalModuleCount() {
        ImnotcheatingyouareClient client = ImnotcheatingyouareClient.INSTANCE;
        if (client == null || client.moduleManager == null) {
            return 0;
        }
        int count = 0;
        ModuleManager mm = client.moduleManager;
        for (Module m : mm.modules) {
            if (!GuiTaxonomy.shouldHideFromList(m)) {
                count++;
            }
        }
        return count;
    }

    public static int disableAllGameplayModules() {
        ImnotcheatingyouareClient client = ImnotcheatingyouareClient.INSTANCE;
        if (client == null || client.moduleManager == null) {
            return 0;
        }
        int count = 0;
        ClientNotificationService.setSuppressModuleHook(true);
        try {
            ModuleManager mm = client.moduleManager;
            for (Module m : mm.modules) {
                if (m.isToggled() && !isUtilityPassiveModule(m)) {
                    m.toggle();
                    count++;
                }
            }
        } finally {
            ClientNotificationService.setSuppressModuleHook(false);
        }
        if (count > 0 && isAutoSaveEnabled()) {
            ConfigManager.save();
        }
        return count;
    }

    private static boolean isUtilityPassiveModule(Module m) {
        String n = m.getName();
        return "CustomMenu".equalsIgnoreCase(n)
            || "Menu".equalsIgnoreCase(n)
            || "Theme".equalsIgnoreCase(n)
            || "GUI".equalsIgnoreCase(n)
            || "ArrayList".equalsIgnoreCase(n)
            || "KeybindList".equalsIgnoreCase(n);
    }

    public static String cardIdForModule(Module mod) {
        String lowerName = mod.getName().toLowerCase(Locale.ROOT);
        String clean = lowerName.replaceAll("[^a-z0-9]+", "_");
        return "mod_" + clean;
    }

    public static UiIcon iconForGroup(GuiTaxonomy.Group group) {
        return switch (group) {
            case COMBAT -> LucideIcon.SWORDS;
            case MOVEMENT -> LucideIcon.WIND;
            case RENDER -> LucideIcon.EYE;
            case UTILITY -> LucideIcon.SHIELD;
            case WORLD -> LucideIcon.CUBOID;
            case CLIENT -> LucideIcon.CPU;
        };
    }

    public static UiIcon resolveModuleIcon(Module mod) {
        if (mod == null) {
            return LucideIcon.SPARKLES;
        }
        String n = mod.getName().toLowerCase(Locale.ROOT);
        Category cat = mod.getCategory();
        if (n.contains("killsound")) return LucideIcon.BELL;
        if (n.contains("hitbox")) return LucideIcon.SCAN;
        if (n.contains("aim") || n.contains("bow")) return LucideIcon.CROSSHAIR;
        if (n.contains("trigger") || n.contains("clicker")) return LucideIcon.MOUSE;
        if (n.contains("mace") || n.contains("breach")) return LucideIcon.GAVEL;
        if (n.contains("crystal") || n.contains("anchor")) return LucideIcon.FLAME;
        if (n.contains("totem") || n.contains("dhand") || n.contains("health")) return LucideIcon.HEART_PULSE;
        if (n.contains("shield") || n.contains("surround") || n.contains("armor") || n.contains("friend")) return LucideIcon.SHIELD;
        if (n.contains("wtap") || n.contains("stap") || n.contains("sprint") || n.contains("jump") || n.contains("kb") || n.contains("knockback")) return LucideIcon.FOOTPRINTS;
        if (n.contains("elytra") || n.contains("flight") || n.contains("fly") || n.contains("wind")) return LucideIcon.FEATHER;
        if (n.contains("blockesp") || n.contains("xray") || n.contains("scaffold") || n.contains("bridge")) return LucideIcon.CUBOID;
        if (n.contains("storage") || n.contains("loot") || n.contains("inventory") || n.contains("cart")) return LucideIcon.PACKAGE;
        if (n.contains("tracer") || n.contains("trajectory") || n.contains("pearl")) return LucideIcon.NAVIGATION;
        if (n.contains("nametag") || n.contains("sign") || n.contains("nameprotect")) return LucideIcon.TAG;
        if (n.contains("esp") || n.contains("chams") || n.contains("sight") || n.contains("freecam")) return LucideIcon.EYE;
        if (n.contains("fullbright") || n.contains("shader")) return LucideIcon.SUN;
        if (n.contains("theme") || n.contains("menu")) return LucideIcon.PALETTE;
        if (n.contains("macro") || n.contains("swap")) return LucideIcon.REPEAT;
        if (n.contains("alert") || n.contains("suspicious") || n.contains("spawner")) return LucideIcon.DANGER;
        if (n.contains("blink") || n.contains("lag") || n.contains("backtrack") || n.contains("fast")) return LucideIcon.ZAP;
        if (cat == Category.Combat || cat == Category.Blatant) return LucideIcon.SWORD;
        if (cat == Category.Movement) return LucideIcon.WIND;
        if (cat == Category.Render || cat == Category.HUD) return LucideIcon.SCAN;
        return LucideIcon.SLIDERS_HORIZONTAL;
    }

    private static UiIcon choiceIconForLabel(String label, int index) {
        String l = label.toLowerCase(Locale.ROOT);
        if (l.contains("silent") || l.contains("ghost") || l.contains("legit")) return LucideIcon.FEATHER;
        if (l.contains("blatant") || l.contains("rage") || l.contains("fast") || l.contains("pulse") || l.contains("aggro")) return LucideIcon.ZAP;
        if (l.contains("bind")) return LucideIcon.FOCUS;
        if (l.contains("smart") || l.contains("auto") || l.contains("dynamic")) return LucideIcon.SPARKLES;
        if (l.contains("shader") || l.contains("glow")) return LucideIcon.PALETTE;
        if (l.contains("outline") || l.contains("2d") || l.contains("box")) return LucideIcon.SCAN;
        return switch (index % 4) {
            case 0 -> LucideIcon.FOCUS;
            case 1 -> LucideIcon.SPARKLES;
            case 2 -> LucideIcon.LAYERS;
            default -> LucideIcon.ZAP;
        };
    }

    private static String inferUnitSuffix(String settingName) {
        String lower = settingName.toLowerCase(Locale.ROOT);
        if (lower.contains("(%)") || lower.contains("chance") || lower.contains("opacity")) return "%";
        if (lower.contains("(ms)")) return "ms";
        if (lower.contains("(s)")) return "s";
        if (lower.contains("cps")) return " CPS";
        if (lower.contains("fov") || lower.contains("yaw") || lower.contains("pitch")) return "°";
        if (lower.contains("range") || lower.contains("distance")) return "m";
        return "";
    }

    private static String trimNum(double v, boolean onlyInt) {
        if (onlyInt || Math.abs(v - Math.rint(v)) < 0.001) {
            return String.valueOf((int) Math.round(v));
        }
        return String.format(Locale.ROOT, "%.2f", v);
    }

    private static float clamp(float val, float min, float max) {
        return Math.max(min, Math.min(max, val));
    }
}
