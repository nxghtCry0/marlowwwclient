package com.eclipseware.imnotcheatingyouare.client.glassui.persist;

import java.util.LinkedHashMap;
import java.util.Map;

public final class UiCatalogSnapshot {
    public final Map<String, Boolean> cards = new LinkedHashMap<>();
    public final Map<String, Boolean> toggles = new LinkedHashMap<>();
    public final Map<String, Float> sliders = new LinkedHashMap<>();
    public final Map<String, Integer> choices = new LinkedHashMap<>();
}
