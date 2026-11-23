package com.eclipseware.imnotcheatingyouare.client.glassui.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

public final class UiCatalog {
    @FunctionalInterface
    public interface CardToggleListener {
        void onToggle(UiCard card, boolean enabled, boolean notify);
    }

    private final List<UiCategory> categories;
    private final Map<String, UiCard> cardById;
    private final Map<String, UiControl.Toggle> togglesById;
    private final Map<String, UiControl.Slider> slidersById;

    private CardToggleListener onCardToggleListener;
    private BiConsumer<UiCard, Integer> onChoiceChangeListener;
    private Consumer<UiControl> onControlChangeListener;

    public UiCatalog(
        List<UiCategory> categories,
        Map<String, UiControl.Toggle> sharedToggles,
        Map<String, UiControl.Slider> sharedSliders
    ) {
        this.categories = categories != null ? List.copyOf(categories) : List.of();
        Map<String, UiCard> cards = new LinkedHashMap<>();
        Map<String, UiControl.Toggle> toggles = new LinkedHashMap<>();
        Map<String, UiControl.Slider> sliders = new LinkedHashMap<>();
        if (sharedToggles != null) {
            toggles.putAll(sharedToggles);
        }
        if (sharedSliders != null) {
            sliders.putAll(sharedSliders);
        }
        for (UiCategory category : this.categories) {
            for (UiTab tab : category.tabs()) {
                for (UiCard card : tab.cards()) {
                    cards.put(card.id(), card);
                    if (card.quickControl() != null) {
                        registerControl(card.quickControl(), toggles, sliders);
                    }
                    for (UiControl control : card.controls()) {
                        registerControl(control, toggles, sliders);
                    }
                }
            }
        }
        this.cardById = Collections.unmodifiableMap(cards);
        this.togglesById = Collections.unmodifiableMap(toggles);
        this.slidersById = Collections.unmodifiableMap(sliders);
    }

    private static void registerControl(
        UiControl control,
        Map<String, UiControl.Toggle> toggles,
        Map<String, UiControl.Slider> sliders
    ) {
        if (control instanceof UiControl.Toggle toggle) {
            toggles.putIfAbsent(toggle.id(), toggle);
        } else if (control instanceof UiControl.Slider slider) {
            sliders.putIfAbsent(slider.id(), slider);
        }
    }

    public List<UiCategory> categories() {
        return categories;
    }

    public void setOnCardToggleListener(CardToggleListener listener) {
        this.onCardToggleListener = listener;
    }

    public void setOnChoiceChangeListener(BiConsumer<UiCard, Integer> listener) {
        this.onChoiceChangeListener = listener;
    }

    public void setOnControlChangeListener(Consumer<UiControl> listener) {
        this.onControlChangeListener = listener;
    }

    public void syncAllFromBacking() {
        for (UiCard card : cardById.values()) {
            card.syncFromBacking();
        }
    }

    public Collection<UiCard> allCards() {
        return cardById.values();
    }

    public Collection<UiControl.Toggle> allToggles() {
        return togglesById.values();
    }

    public Collection<UiControl.Slider> allSliders() {
        return slidersById.values();
    }

    public UiCard findCard(String id) {
        return cardById.get(id);
    }

    public UiControl.Toggle findToggle(String id) {
        return togglesById.get(id);
    }

    public UiControl.Slider findSlider(String id) {
        return slidersById.get(id);
    }

    public boolean isEnabled(String id) {
        UiCard card = cardById.get(id);
        return card != null && card.enabled();
    }

    public boolean setEnabled(String id, boolean enabled, boolean notify) {
        UiCard card = cardById.get(id);
        if (card == null || !card.toggleable()) {
            return false;
        }
        boolean changed = card.enabled() != enabled;
        card.setEnabled(enabled);
        if (changed) {
            Consumer<Boolean> cb = card.onToggleChanged();
            if (cb != null) {
                cb.accept(enabled);
            }
            if (onCardToggleListener != null) {
                onCardToggleListener.onToggle(card, enabled, notify);
            }
        }
        return changed;
    }

    public boolean getBool(String id, boolean defaultValue) {
        UiControl.Toggle toggle = togglesById.get(id);
        return toggle != null ? toggle.enabled() : defaultValue;
    }

    public void setBool(String id, boolean enabled) {
        UiControl.Toggle toggle = togglesById.get(id);
        if (toggle == null) {
            return;
        }
        toggle.setValue(enabled);
        if (onControlChangeListener != null) {
            onControlChangeListener.accept(toggle);
        }
    }

    public float getSlider(String id, float defaultValue) {
        UiControl.Slider slider = slidersById.get(id);
        return slider != null ? slider.value() : defaultValue;
    }

    public void setSlider(String id, float value) {
        UiControl.Slider slider = slidersById.get(id);
        if (slider == null) {
            return;
        }
        slider.setClampedValue(value);
        if (onControlChangeListener != null) {
            onControlChangeListener.accept(slider);
        }
    }

    public int getChoice(String id, int defaultValue) {
        UiCard card = cardById.get(id);
        return card != null ? card.selectedChoice() : defaultValue;
    }

    public void setChoice(String id, int choiceIdx, boolean animate) {
        UiCard card = cardById.get(id);
        if (card == null) {
            return;
        }
        List<UiChoice> choices = card.choices();
        if (choices.isEmpty()) {
            return;
        }
        int clamped = Math.max(0, Math.min(choices.size() - 1, choiceIdx));
        card.setSelectedChoice(clamped);
        if (!animate) {
            for (int i = 0; i < choices.size(); i++) {
                choices.get(i).animSelected = i == clamped ? 1f : 0f;
            }
        }
        var cb = card.onChoiceChanged();
        if (cb != null) {
            cb.accept(clamped);
        }
        if (onChoiceChangeListener != null) {
            onChoiceChangeListener.accept(card, clamped);
        }
    }

    public int activeCardCount() {
        int count = 0;
        for (UiCard card : cardById.values()) {
            if (card.toggleable() && card.enabled()) {
                count++;
            }
        }
        return count;
    }

    public int toggleableCardCount() {
        int count = 0;
        for (UiCard card : cardById.values()) {
            if (card.toggleable()) {
                count++;
            }
        }
        return count;
    }

    public String cardTitle(String id) {
        UiCard card = cardById.get(id);
        return card != null ? card.title() : id;
    }

    public int disableMatching(Predicate<UiCard> predicate) {
        int count = 0;
        for (UiCard card : cardById.values()) {
            if (card.toggleable() && card.enabled() && predicate.test(card)) {
                card.setEnabled(false);
                Consumer<Boolean> cb = card.onToggleChanged();
                if (cb != null) {
                    cb.accept(false);
                }
                count++;
            }
        }
        return count;
    }

    public List<UiCard> searchCards(String query) {
        if (query == null) {
            return List.of();
        }
        String q = query.trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) {
            return List.of();
        }
        List<UiCard> matches = new ArrayList<>();
        for (UiCard card : cardById.values()) {
            if (matchesCard(card, q)) {
                matches.add(card);
            }
        }
        return matches;
    }

    private static boolean matchesCard(UiCard card, String q) {
        if (lowerContains(card.title(), q)
            || lowerContains(card.subtitle(), q)
            || lowerContains(card.summary(), q)
            || lowerContains(card.keywords(), q)) {
            return true;
        }
        for (UiChoice ch : card.choices()) {
            if (lowerContains(ch.label(), q) || lowerContains(ch.description(), q)) {
                return true;
            }
        }
        for (UiControl ctrl : card.controls()) {
            if (lowerContains(ctrl.label(), q) || lowerContains(ctrl.description(), q)) {
                return true;
            }
        }
        return false;
    }

    private static boolean lowerContains(String text, String needle) {
        return text != null && text.toLowerCase(Locale.ROOT).contains(needle);
    }
}
