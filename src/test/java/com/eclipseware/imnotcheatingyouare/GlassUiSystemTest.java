package com.eclipseware.imnotcheatingyouare;

import com.eclipseware.imnotcheatingyouare.client.glassui.asset.LucideIcon;
import com.eclipseware.imnotcheatingyouare.client.glassui.asset.UiAssets;
import com.eclipseware.imnotcheatingyouare.client.glassui.bind.UiInputBind;
import com.eclipseware.imnotcheatingyouare.client.glassui.model.UiCard;
import com.eclipseware.imnotcheatingyouare.client.glassui.model.UiCatalog;
import com.eclipseware.imnotcheatingyouare.client.glassui.model.UiCategory;
import com.eclipseware.imnotcheatingyouare.client.glassui.model.UiChoice;
import com.eclipseware.imnotcheatingyouare.client.glassui.model.UiControl;
import com.eclipseware.imnotcheatingyouare.client.glassui.model.UiTab;
import com.eclipseware.imnotcheatingyouare.client.glassui.persist.UiCatalogSnapshot;
import com.eclipseware.imnotcheatingyouare.client.glassui.persist.UiStatePersistence;
import com.eclipseware.imnotcheatingyouare.client.glassui.render.SatoshiFont;
import com.eclipseware.imnotcheatingyouare.client.glassui.render.SatoshiWeight;
import com.eclipseware.imnotcheatingyouare.client.glassui.render.TextGradient;
import com.eclipseware.imnotcheatingyouare.client.glassui.screen.GlassUiChrome;
import com.eclipseware.imnotcheatingyouare.client.glassui.screen.GlassUiConfig;
import com.eclipseware.imnotcheatingyouare.client.glassui.screen.UiNavigationMemory;
import com.eclipseware.imnotcheatingyouare.client.glassui.screen.UiScreenState;
import net.minecraft.network.chat.Component;
import com.eclipseware.imnotcheatingyouare.client.glassui.theme.AccentPreset;
import com.eclipseware.imnotcheatingyouare.client.glassui.theme.BorderStyle;
import com.eclipseware.imnotcheatingyouare.client.glassui.theme.FontGradientStyle;
import com.eclipseware.imnotcheatingyouare.client.glassui.theme.GlassSurfacePreset;
import com.eclipseware.imnotcheatingyouare.client.glassui.theme.MotionCurve;
import com.eclipseware.imnotcheatingyouare.client.glassui.theme.MutableUiTheme;
import com.eclipseware.imnotcheatingyouare.client.utils.InputUtil;
import com.mojang.blaze3d.platform.InputConstants;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class GlassUiSystemTest {

    @Test
    void mutableThemePresetsCustomHsbAndReset() {
        MutableUiTheme theme = new MutableUiTheme();
        int oceanColor = theme.accentColor();

        theme.setAccentPreset(AccentPreset.EMERALD);
        int emeraldColor = theme.accentColor();
        Assertions.assertNotEquals(oceanColor, emeraldColor);

        theme.setAccentPreset(AccentPreset.ROSE_PINK);
        int roseColor = theme.accentColor();
        Assertions.assertNotEquals(emeraldColor, roseColor);

        theme.setAccentPreset(AccentPreset.CUSTOM);
        theme.setCustomHue(280f);
        theme.setCustomSaturation(90f);
        theme.setCustomBrightness(95f);
        theme.setDualToneShift(36f);
        int customPrimary = theme.accentColor();
        int customSecondary = theme.secondaryAccentColor();
        Assertions.assertNotEquals(customPrimary, customSecondary);

        theme.setDualToneShift(0f);
        Assertions.assertEquals(customPrimary, theme.secondaryAccentColor());

        theme.setGlassSurface(GlassSurfacePreset.OBSIDIAN);
        theme.setFontGradientStyle(FontGradientStyle.AURORA);
        theme.setBorderStyle(BorderStyle.ACCENT);
        theme.setMotionCurve(MotionCurve.SNAPPY);
        TextGradient aurora = TextGradient.pearl(0.9f, customPrimary, 0.4f, theme);
        Assertions.assertTrue(((aurora.topLeft() >>> 24) & 0xFF) > 200);

        theme.resetDefaults();
        Assertions.assertEquals(AccentPreset.OCEAN_SKY, theme.accentPreset());
        Assertions.assertEquals(GlassSurfacePreset.STONE, theme.glassSurface());
        Assertions.assertEquals(FontGradientStyle.PEARL, theme.fontGradientStyle());
        Assertions.assertEquals(BorderStyle.SUBTLE, theme.borderStyle());
        Assertions.assertEquals(MotionCurve.SPRING, theme.motionCurve());
        Assertions.assertEquals(oceanColor, theme.accentColor());
    }

    @Test
    void satoshiFontMetricsTrimAndWrap() {
        Assertions.assertEquals("imnotcheatingyouare", UiAssets.getNamespace());
        float smallWidth = SatoshiFont.width("Marlow Client", 8f, SatoshiWeight.MEDIUM);
        float largeWidth = SatoshiFont.width("Marlow Client", 12f, SatoshiWeight.SEMIBOLD);
        Assertions.assertTrue(smallWidth > 20f);
        Assertions.assertTrue(largeWidth > smallWidth);
        Assertions.assertEquals(0f, SatoshiFont.width("", 10f, SatoshiWeight.MEDIUM));

        String longInput = "Right-click any module card to configure options and keybinds";
        String trimmed = SatoshiFont.trimTailToWidth(longInput, 60f, 8f, SatoshiWeight.MEDIUM);
        Assertions.assertTrue(trimmed.length() < longInput.length());
        Assertions.assertTrue(SatoshiFont.width(trimmed, 8f, SatoshiWeight.MEDIUM) <= 60.5f);

        List<String> wrapped = SatoshiFont.wrapLines(longInput, 90f, 8f, SatoshiWeight.MEDIUM);
        Assertions.assertTrue(wrapped.size() >= 2);
    }

    @Test
    void catalogControlsSearchAndPersistenceRoundTrip() {
        AtomicBoolean backingFlag = new AtomicBoolean(true);
        AtomicReference<Float> backingSlider = new AtomicReference<>(85f);
        AtomicReference<String> backingMode = new AtomicReference<>("Normal");
        AtomicInteger actionRuns = new AtomicInteger(0);
        AtomicBoolean showAdvanced = new AtomicBoolean(false);

        UiControl.Toggle freshHits = new UiControl.Toggle(
            "wtap__fresh_hits",
            "Fresh Hits Only",
            true,
            "Only reset sprint on charged hits.",
            LucideIcon.CHECK,
            backingFlag::set
        );
        freshHits.withSync(() -> freshHits.setEnabledSilent(backingFlag.get()));

        UiControl.Slider actionDelay = new UiControl.Slider(
            "wtap__action_delay",
            "Action Delay (ms)",
            85f,
            20f,
            350f,
            "ms",
            0,
            "Hold release duration.",
            LucideIcon.SLIDERS_HORIZONTAL,
            backingSlider::set
        );
        actionDelay.withSync(() -> actionDelay.setValueSilent(backingSlider.get()));

        UiControl.Cycle cycleCtrl = new UiControl.Cycle(
            "wtap__submode",
            "Timing Curve",
            List.of("Linear", "Humanized", "Burst"),
            0,
            "Cycle timing profile.",
            LucideIcon.LAYERS,
            backingMode::set
        );

        UiControl.RangeSlider rangeCtrl = new UiControl.RangeSlider(
            "wtap__cps_window",
            "CPS Window",
            9f,
            14f,
            1f,
            20f,
            0,
            "Range control test.",
            LucideIcon.SLIDERS_HORIZONTAL,
            (lo, hi) -> {
            }
        );

        UiControl.ColorPicker colorCtrl = new UiControl.ColorPicker(
            "esp__player_color",
            "Player Color",
            0xFF2AA8FF,
            "Color control test.",
            LucideIcon.PALETTE,
            col -> {
            }
        );

        UiControl.TextField textCtrl = new UiControl.TextField(
            "spoof__brand",
            "Custom Brand",
            "vanilla",
            "Text control test.",
            LucideIcon.TAG,
            val -> {
            }
        );
        textCtrl.visibleWhen(showAdvanced::get);

        UiControl.Action actionCtrl = new UiControl.Action(
            "wtap__reset",
            "Reset Counter",
            "Reset",
            "Counter cleared",
            "Action control test.",
            LucideIcon.REPEAT,
            actionRuns::incrementAndGet
        );

        List<UiChoice> choices = List.of(
            new UiChoice("m_normal", "Normal", LucideIcon.FOCUS, "Physical key release"),
            new UiChoice("m_silent", "Silent", LucideIcon.FEATHER, "Packet sprint state"),
            new UiChoice("m_auto", "Auto", LucideIcon.SPARKLES, "Adaptive reset")
        );

        AtomicBoolean cardBacking = new AtomicBoolean(false);
        UiCard wtapCard = new UiCard(
            "mod_wtap",
            "WTap",
            "Combat module",
            "Releases forward input on hit.",
            LucideIcon.FOOTPRINTS,
            false,
            "wtap sprint reset knockback combat",
            0,
            choices,
            actionDelay,
            List.of(freshHits, actionDelay, cycleCtrl, rangeCtrl, colorCtrl, textCtrl, actionCtrl),
            true,
            true,
            "Open",
            "Options",
            null,
            cardBacking::set,
            idx -> {
            },
            null,
            null
        );

        UiTab tab = new UiTab("combat_sprint", "Sprint Reset", List.of(wtapCard), null, null);
        UiCategory category = new UiCategory("combat", "Combat", LucideIcon.SWORDS, List.of(tab));
        UiCatalog catalog = new UiCatalog(List.of(category), Map.of(), Map.of());

        Assertions.assertEquals(6, wtapCard.visibleControls().size());
        showAdvanced.set(true);
        Assertions.assertEquals(7, wtapCard.visibleControls().size());

        cycleCtrl.step(1);
        Assertions.assertEquals("Humanized", cycleCtrl.currentOption());
        Assertions.assertEquals("Humanized", backingMode.get());
        cycleCtrl.step(-1);
        Assertions.assertEquals("Linear", cycleCtrl.currentOption());

        rangeCtrl.setHandleFromNormalized(false, 0.5f);
        rangeCtrl.setHandleFromNormalized(true, 0.8f);
        Assertions.assertTrue(rangeCtrl.low() <= rangeCtrl.high());

        String initialHex = colorCtrl.hexLabel();
        colorCtrl.setHueNormalized(0.15f);
        Assertions.assertNotEquals(initialHex, colorCtrl.hexLabel());

        textCtrl.setValue("lunarclient");
        Assertions.assertEquals("lunarclient", textCtrl.value());

        actionCtrl.trigger();
        Assertions.assertEquals(1, actionRuns.get());

        Assertions.assertEquals(1, catalog.searchCards("knockback").size());
        Assertions.assertEquals(1, catalog.searchCards("Fresh Hits").size());
        Assertions.assertTrue(catalog.searchCards("nonexistent_xyz").isEmpty());

        catalog.setEnabled("mod_wtap", true, false);
        catalog.setChoice("mod_wtap", 2, false);
        catalog.setBool("wtap__fresh_hits", false);
        catalog.setSlider("wtap__action_delay", 140f);
        Assertions.assertTrue(cardBacking.get());
        Assertions.assertFalse(backingFlag.get());
        Assertions.assertEquals(140f, backingSlider.get(), 0.01f);

        UiCatalogSnapshot saved = UiStatePersistence.capture(catalog);

        catalog.setEnabled("mod_wtap", false, false);
        catalog.setChoice("mod_wtap", 0, false);
        catalog.setBool("wtap__fresh_hits", true);
        catalog.setSlider("wtap__action_delay", 45f);
        Assertions.assertFalse(catalog.isEnabled("mod_wtap"));

        UiStatePersistence.apply(catalog, saved);
        Assertions.assertTrue(catalog.isEnabled("mod_wtap"));
        Assertions.assertEquals(2, catalog.getChoice("mod_wtap", 0));
        Assertions.assertFalse(catalog.getBool("wtap__fresh_hits", true));
        Assertions.assertEquals(140f, catalog.getSlider("wtap__action_delay", 0f), 0.01f);

        backingFlag.set(true);
        backingSlider.set(95f);
        catalog.syncAllFromBacking();
        Assertions.assertTrue(catalog.getBool("wtap__fresh_hits", false));
        Assertions.assertEquals(95f, catalog.getSlider("wtap__action_delay", 0f), 0.01f);
    }

    @Test
    void inputBindKeyboardAndMouseRoundTrip() {
        Assertions.assertFalse(UiInputBind.NONE.isBound());
        Assertions.assertEquals(0, UiInputBind.NONE.toModuleKeyBind());
        Assertions.assertEquals("Bind", UiInputBind.NONE.formatLabel());

        UiInputBind rShift = UiInputBind.key(InputConstants.KEY_RSHIFT);
        Assertions.assertTrue(rShift.isBound());
        Assertions.assertFalse(rShift.isMouse());
        Assertions.assertEquals("RSHF", rShift.formatLabel());
        int rawKey = rShift.toModuleKeyBind();
        Assertions.assertEquals(rShift, UiInputBind.fromModuleKeyBind(rawKey));

        int[] sdlButtons = new int[]{
            InputConstants.MOUSE_BUTTON_LEFT,
            InputConstants.MOUSE_BUTTON_RIGHT,
            InputConstants.MOUSE_BUTTON_MIDDLE,
            InputConstants.MOUSE_BUTTON_4,
            InputConstants.MOUSE_BUTTON_5
        };
        int[] expectedModuleCodes = new int[]{
            InputUtil.MOUSE_LEFT,
            InputUtil.MOUSE_RIGHT,
            InputUtil.MOUSE_MIDDLE,
            InputUtil.MOUSE_BUTTON_4,
            InputUtil.MOUSE_BUTTON_5
        };
        String[] expectedLabels = new String[]{"MB1", "MB2", "MB3", "MB4", "MB5"};

        for (int idx = 0; idx < sdlButtons.length; idx++) {
            UiInputBind mouseBind = UiInputBind.mouse(sdlButtons[idx]);
            Assertions.assertTrue(mouseBind.isBound());
            Assertions.assertTrue(mouseBind.isMouse());
            Assertions.assertEquals(expectedLabels[idx], mouseBind.formatLabel());
            int moduleCode = mouseBind.toModuleKeyBind();
            Assertions.assertEquals(expectedModuleCodes[idx], moduleCode);
            UiInputBind restored = UiInputBind.fromModuleKeyBind(moduleCode);
            Assertions.assertEquals(mouseBind, restored);
        }
    }

    @Test
    void zeroTemplateReferencesInSourceTree() throws IOException {
        List<String> globalForbidden = List.of(
            "te" + "jas-utility",
            "te" + "jas_utility",
            "te" + "jasutility",
            "janar" + "dhan",
            "ui" + "lib",
            "/t" + "mc"
        );
        String glassForbidden = "te" + "jas";
        Path glassRoot = Path.of("src/client/java/com/eclipseware/imnotcheatingyouare/client/glassui");
        Path assetsRoot = Path.of("src/main/resources/assets/imnotcheatingyouare");
        List<Path> roots = List.of(
            Path.of("src/main"),
            Path.of("src/client"),
            Path.of("src/test")
        );
        Map<String, List<String>> hits = new LinkedHashMap<>();
        for (Path root : roots) {
            if (!Files.exists(root)) {
                continue;
            }
            try (Stream<Path> walk = Files.walk(root)) {
                List<Path> files = walk.filter(Files::isRegularFile).toList();
                for (Path file : files) {
                    boolean inMigratedUi = file.startsWith(glassRoot) || file.startsWith(assetsRoot);
                    String pathStr = file.toString().toLowerCase(Locale.ROOT);
                    for (String needle : globalForbidden) {
                        if (pathStr.contains(needle)) {
                            hits.computeIfAbsent(needle, k -> new ArrayList<>()).add("PATH:" + file);
                        }
                    }
                    if (inMigratedUi && pathStr.contains(glassForbidden)) {
                        hits.computeIfAbsent(glassForbidden, k -> new ArrayList<>()).add("PATH:" + file);
                    }
                    if (!isTextAsset(pathStr)) {
                        continue;
                    }
                    String content = Files.readString(file, StandardCharsets.UTF_8);
                    String lowerContent = content.toLowerCase(Locale.ROOT);
                    for (String needle : globalForbidden) {
                        if (lowerContent.contains(needle)) {
                            hits.computeIfAbsent(needle, k -> new ArrayList<>()).add(file.toString());
                        }
                    }
                    if (inMigratedUi && lowerContent.contains(glassForbidden)) {
                        hits.computeIfAbsent(glassForbidden, k -> new ArrayList<>()).add(file.toString());
                    }
                }
            }
        }
        Assertions.assertTrue(hits.isEmpty(), () -> "Unexpected template references found: " + hits);
    }

    private static boolean isTextAsset(String lowerPath) {
        return lowerPath.endsWith(".java")
            || lowerPath.endsWith(".json")
            || lowerPath.endsWith(".mcmeta")
            || lowerPath.endsWith(".md")
            || lowerPath.endsWith(".txt")
            || lowerPath.endsWith(".properties");
    }

    @Test
    void noHalloweenThemeRemnantsExist() throws IOException {
        List<String> halloweenTokens = List.of(
            "pump" + "kin",
            "hall" + "oween",
            "spook" + "low",
            "crypt" + "night",
            "witch" + "violet",
            "ember" + "gold",
            "drawhud" + "cobweb",
            "255, 118, 26",
            "ff761a"
        );
        List<Path> roots = List.of(
            Path.of("src/main"),
            Path.of("src/client")
        );
        Map<String, List<String>> hits = new LinkedHashMap<>();
        for (Path root : roots) {
            if (!Files.exists(root)) {
                continue;
            }
            try (Stream<Path> walk = Files.walk(root)) {
                List<Path> files = walk.filter(Files::isRegularFile).toList();
                for (Path file : files) {
                    String pathStr = file.toString().toLowerCase(Locale.ROOT);
                    if (!isTextAsset(pathStr)) {
                        continue;
                    }
                    String content = Files.readString(file, StandardCharsets.UTF_8);
                    String lowerContent = content.toLowerCase(Locale.ROOT);
                    for (String token : halloweenTokens) {
                        if (lowerContent.contains(token)) {
                            hits.computeIfAbsent(token, k -> new ArrayList<>()).add(file.toString());
                        }
                    }
                }
            }
        }
        Assertions.assertTrue(hits.isEmpty(), () -> "Halloween theme remnants found: " + hits);
    }

    @Test
    void sidebarExpandMenuStateAndLayoutGeometry() {
        UiNavigationMemory nav = new UiNavigationMemory();
        Assertions.assertFalse(nav.sidebarExpanded());
        Assertions.assertTrue(nav.toggleSidebarExpanded());
        Assertions.assertTrue(nav.sidebarExpanded());
        Assertions.assertFalse(nav.toggleSidebarExpanded());

        List<UiCategory> categories = List.of(
            new UiCategory("combat", "Combat", LucideIcon.SWORDS, List.of()),
            new UiCategory("movement", "Movement", LucideIcon.WIND, List.of()),
            new UiCategory("render", "Render", LucideIcon.EYE, List.of()),
            new UiCategory("utility", "Utility", LucideIcon.SHIELD, List.of()),
            new UiCategory("world", "World", LucideIcon.CUBOID, List.of()),
            new UiCategory("client", "Client", LucideIcon.CPU, List.of()),
            new UiCategory("settings", "Settings", LucideIcon.SETTINGS, List.of())
        );
        UiCatalog catalog = new UiCatalog(categories, Map.of(), Map.of());
        GlassUiConfig config = new GlassUiConfig(
            catalog,
            Component.empty(),
            656f,
            424f,
            false,
            new MutableUiTheme(),
            null,
            null,
            null,
            null,
            true,
            null,
            null,
            null,
            false,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            nav,
            null
        );

        float expandedWidth = GlassUiChrome.expandedSidebarWidth(config);
        Assertions.assertTrue(expandedWidth >= 92f);
        for (UiCategory cat : categories) {
            float titleW = SatoshiFont.width(cat.title(), 6.1f, SatoshiWeight.SEMIBOLD);
            Assertions.assertTrue(30.5f + titleW + 10f <= expandedWidth);
        }

        UiScreenState state = new UiScreenState();
        Assertions.assertEquals(656f, GlassUiChrome.effectivePanelWidth(config, state), 0.01f);
        Assertions.assertEquals(56f, GlassUiChrome.contentAreaX(config, state), 0.01f);
        Assertions.assertEquals(50, GlassUiChrome.contentScissorLeft(config, state));

        state.sidebarExpanded = true;
        state.sidebarExpandProgress = 1f;
        float extra = expandedWidth - GlassUiChrome.SIDEBAR_COLLAPSED_WIDTH;
        Assertions.assertEquals(656f + extra, GlassUiChrome.effectivePanelWidth(config, state), 0.01f);
        Assertions.assertEquals(56f + extra, GlassUiChrome.contentAreaX(config, state), 0.01f);
        Assertions.assertEquals(Math.round(50f + extra), GlassUiChrome.contentScissorLeft(config, state));

        AtomicInteger autoSaves = new AtomicInteger(0);
        GlassUiConfig toggleCfg = new GlassUiConfig(
            catalog,
            Component.empty(),
            656f,
            424f,
            false,
            new MutableUiTheme(),
            null,
            null,
            null,
            null,
            true,
            null,
            null,
            null,
            false,
            null,
            null,
            autoSaves::incrementAndGet,
            null,
            null,
            null,
            null,
            null,
            null,
            nav,
            null
        );
        UiScreenState toggleState = new UiScreenState();
        Assertions.assertFalse(toggleState.sidebarExpanded);
        Assertions.assertTrue(GlassUiChrome.toggleSidebar(toggleCfg, toggleState));
        Assertions.assertTrue(toggleState.sidebarExpanded);
        Assertions.assertTrue(nav.sidebarExpanded());
        Assertions.assertEquals(1, autoSaves.get());
        Assertions.assertFalse(GlassUiChrome.toggleSidebar(toggleCfg, toggleState));
        Assertions.assertFalse(toggleState.sidebarExpanded);
        Assertions.assertFalse(nav.sidebarExpanded());
        Assertions.assertEquals(2, autoSaves.get());
    }
}
