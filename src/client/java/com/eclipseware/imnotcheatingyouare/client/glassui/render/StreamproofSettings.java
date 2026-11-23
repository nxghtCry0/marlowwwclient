package com.eclipseware.imnotcheatingyouare.client.glassui.render;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public record StreamproofSettings(
    BooleanSupplier enabled,
    Supplier<StreamproofMode> mode,
    String windowClassName
) {
    public static StreamproofSettings defaults() {
        return new StreamproofSettings(() -> false, () -> StreamproofMode.IN_GAME, "MarlowClientAsyncDwmShield");
    }
}
