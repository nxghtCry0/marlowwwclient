package com.eclipseware.imnotcheatingyouare.client.glassui.model;

import com.eclipseware.imnotcheatingyouare.client.glassui.asset.UiIcon;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

public final class UiCard {
    private final String id;
    private final String title;
    private final String subtitle;
    private final String summary;
    private final UiIcon icon;
    private boolean enabled;
    private final String keywords;
    private int selectedChoice;
    private final List<UiChoice> choices;
    private final UiControl quickControl;
    private final List<UiControl> controls;
    private final boolean bindable;
    private final boolean toggleable;
    private final String actionLabel;
    private final String optionsLabel;
    private final Runnable onAction;
    private final Consumer<Boolean> onToggleChanged;
    private final IntConsumer onChoiceChanged;
    private final UiHeaderAccessory headerAccessory;
    private final UiCustomSection customSection;
    private Runnable syncHook;

    public float animToggle;
    public float animHover = 0f;
    public float animToggleHover = 0f;

    public UiCard(
        String id,
        String title,
        String subtitle,
        String summary,
        UiIcon icon,
        boolean enabled,
        String keywords,
        int selectedChoice,
        List<UiChoice> choices,
        UiControl quickControl,
        List<UiControl> controls,
        boolean bindable,
        boolean toggleable,
        String actionLabel,
        String optionsLabel,
        Runnable onAction,
        Consumer<Boolean> onToggleChanged,
        IntConsumer onChoiceChanged,
        UiHeaderAccessory headerAccessory,
        UiCustomSection customSection
    ) {
        this.id = id;
        this.title = title;
        this.subtitle = subtitle != null ? subtitle : "";
        this.summary = summary != null ? summary : "";
        this.icon = icon;
        this.enabled = enabled;
        this.keywords = keywords != null ? keywords : "";
        this.selectedChoice = selectedChoice;
        this.choices = choices != null ? List.copyOf(choices) : List.of();
        this.quickControl = quickControl;
        this.controls = controls != null ? List.copyOf(controls) : List.of();
        this.bindable = bindable;
        this.toggleable = toggleable;
        this.actionLabel = actionLabel != null ? actionLabel : "Cast";
        this.optionsLabel = optionsLabel != null ? optionsLabel : "Options";
        this.onAction = onAction;
        this.onToggleChanged = onToggleChanged;
        this.onChoiceChanged = onChoiceChanged;
        this.headerAccessory = headerAccessory;
        this.customSection = customSection;
        this.animToggle = enabled ? 1f : 0f;
        for (int idx = 0; idx < this.choices.size(); idx++) {
            UiChoice choice = this.choices.get(idx);
            choice.animSelected = idx == selectedChoice ? 1f : 0f;
        }
    }

    public UiCard withSync(Runnable syncHook) {
        this.syncHook = syncHook;
        return this;
    }

    public void syncFromBacking() {
        if (syncHook != null) {
            syncHook.run();
        }
        if (quickControl != null) {
            quickControl.syncFromBacking();
        }
        for (UiControl control : controls) {
            control.syncFromBacking();
        }
    }

    public String id() {
        return id;
    }

    public String title() {
        return title;
    }

    public String subtitle() {
        return subtitle;
    }

    public String summary() {
        return summary;
    }

    public UiIcon icon() {
        return icon;
    }

    public boolean enabled() {
        return enabled;
    }

    public void setEnabledSilent(boolean next) {
        this.enabled = next;
    }

    public void setEnabled(boolean next) {
        this.enabled = next;
    }

    public String keywords() {
        return keywords;
    }

    public int selectedChoice() {
        return selectedChoice;
    }

    public void setSelectedChoiceSilent(int idx) {
        if (choices.isEmpty()) {
            return;
        }
        this.selectedChoice = Math.max(0, Math.min(choices.size() - 1, idx));
    }

    public void setSelectedChoice(int idx) {
        this.selectedChoice = idx;
    }

    public List<UiChoice> choices() {
        return choices;
    }

    public UiControl quickControl() {
        return quickControl;
    }

    public List<UiControl> controls() {
        return controls;
    }

    public List<UiControl> visibleControls() {
        java.util.ArrayList<UiControl> out = new java.util.ArrayList<>(controls.size());
        for (UiControl control : controls) {
            if (control.isVisible()) {
                out.add(control);
            }
        }
        return out;
    }

    public boolean bindable() {
        return bindable;
    }

    public boolean toggleable() {
        return toggleable;
    }

    public String actionLabel() {
        return actionLabel;
    }

    public String optionsLabel() {
        return optionsLabel;
    }

    public Runnable onAction() {
        return onAction;
    }

    public Consumer<Boolean> onToggleChanged() {
        return onToggleChanged;
    }

    public IntConsumer onChoiceChanged() {
        return onChoiceChanged;
    }

    public UiHeaderAccessory headerAccessory() {
        return headerAccessory;
    }

    public UiCustomSection customSection() {
        return customSection;
    }
}
