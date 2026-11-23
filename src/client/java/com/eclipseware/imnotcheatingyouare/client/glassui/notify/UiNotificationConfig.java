package com.eclipseware.imnotcheatingyouare.client.glassui.notify;

import com.eclipseware.imnotcheatingyouare.client.glassui.audio.UiSoundSink;
import com.eclipseware.imnotcheatingyouare.client.glassui.theme.UiTheme;
import com.eclipseware.imnotcheatingyouare.client.glassui.theme.UiThemeContext;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.client.gui.screens.Screen;

public record UiNotificationConfig(
    BooleanSupplier enabled,
    Supplier<UiNotificationLayout> layout,
    Supplier<Float> durationSeconds,
    BooleanSupplier typingAnimation,
    BooleanSupplier hideWhenStreamproof,
    Supplier<UiTheme> theme,
    UiSoundSink sounds,
    Predicate<Screen> suppressHudForScreen
) {
    public static UiNotificationConfig defaults() {
        return new UiNotificationConfig(
            () -> true,
            () -> UiNotificationLayout.BOTTOM_RIGHT,
            () -> 2.2f,
            () -> true,
            () -> false,
            UiThemeContext::current,
            UiSoundSink.SILENT,
            s -> false
        );
    }
}
