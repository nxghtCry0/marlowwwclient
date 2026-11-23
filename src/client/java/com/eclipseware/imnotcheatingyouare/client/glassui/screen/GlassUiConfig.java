package com.eclipseware.imnotcheatingyouare.client.glassui.screen;

import com.eclipseware.imnotcheatingyouare.client.glassui.audio.UiSoundSink;
import com.eclipseware.imnotcheatingyouare.client.glassui.bind.UiKeybindProvider;
import com.eclipseware.imnotcheatingyouare.client.glassui.model.UiCard;
import com.eclipseware.imnotcheatingyouare.client.glassui.model.UiCatalog;
import com.eclipseware.imnotcheatingyouare.client.glassui.model.UiControl;
import com.eclipseware.imnotcheatingyouare.client.glassui.model.UiDashboardProvider;
import com.eclipseware.imnotcheatingyouare.client.glassui.model.UiScreenActions;
import com.eclipseware.imnotcheatingyouare.client.glassui.notify.UiNotificationOverlay;
import com.eclipseware.imnotcheatingyouare.client.glassui.theme.UiTheme;
import com.eclipseware.imnotcheatingyouare.client.glassui.theme.UiThemeContext;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

public record GlassUiConfig(
    UiCatalog catalog,
    Component title,
    float panelWidth,
    float panelHeight,
    boolean pauseGame,
    UiTheme theme,
    UiSoundSink sounds,
    UiKeybindProvider keybinds,
    UiDashboardProvider dashboard,
    UiNotificationOverlay notifications,
    boolean searchEnabled,
    List<String> searchPrompts,
    String defaultFooterHint,
    BooleanSupplier smartEscape,
    boolean showSaveButton,
    String saveToastMessage,
    Runnable onSave,
    Runnable onAutoSave,
    Predicate<KeyEvent> isToggleCloseKey,
    IntPredicate isCancelBindKey,
    BiPredicate<KeyEvent, UiScreenActions> onUnhandledKeyPressed,
    BiConsumer<UiCard, Boolean> onCardToggle,
    BiConsumer<UiCard, Integer> onChoiceChange,
    Consumer<UiControl> onControlChange,
    UiNavigationMemory navigationMemory,
    Screen parentScreen
) {
    public GlassUiConfig {
        if (title == null) {
            title = Component.literal("Marlow Client");
        }
        if (panelWidth <= 0f) {
            panelWidth = 656f;
        }
        if (panelHeight <= 0f) {
            panelHeight = 424f;
        }
        if (theme == null) {
            theme = UiThemeContext.current();
        }
        if (sounds == null) {
            sounds = UiSoundSink.SILENT;
        }
        if (searchPrompts == null || searchPrompts.isEmpty()) {
            searchPrompts = List.of("Search modules...");
        }
        if (smartEscape == null) {
            smartEscape = () -> true;
        }
        if (saveToastMessage == null) {
            saveToastMessage = "Saved configuration";
        }
        if (isToggleCloseKey == null) {
            isToggleCloseKey = ev -> false;
        }
        if (isCancelBindKey == null) {
            isCancelBindKey = key -> false;
        }
        if (navigationMemory == null) {
            navigationMemory = new UiNavigationMemory();
        }
    }
}
