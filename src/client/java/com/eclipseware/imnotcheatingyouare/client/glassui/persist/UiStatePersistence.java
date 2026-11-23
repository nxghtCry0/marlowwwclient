package com.eclipseware.imnotcheatingyouare.client.glassui.persist;

import com.eclipseware.imnotcheatingyouare.client.glassui.model.UiCard;
import com.eclipseware.imnotcheatingyouare.client.glassui.model.UiCatalog;
import com.eclipseware.imnotcheatingyouare.client.glassui.model.UiChoice;
import com.eclipseware.imnotcheatingyouare.client.glassui.model.UiControl;
import java.util.List;
import java.util.Map;

public final class UiStatePersistence {
    private UiStatePersistence() {
    }

    public static UiCatalogSnapshot capture(UiCatalog catalog) {
        UiCatalogSnapshot snapshot = new UiCatalogSnapshot();
        for (UiCard card : catalog.allCards()) {
            if (card.toggleable()) {
                snapshot.cards.put(card.id(), card.enabled());
            }
            if (!card.choices().isEmpty()) {
                snapshot.choices.put(card.id(), card.selectedChoice());
            }
        }
        for (UiControl.Toggle toggle : catalog.allToggles()) {
            snapshot.toggles.put(toggle.id(), toggle.enabled());
        }
        for (UiControl.Slider slider : catalog.allSliders()) {
            snapshot.sliders.put(slider.id(), slider.value());
        }
        return snapshot;
    }

    public static void apply(UiCatalog catalog, UiCatalogSnapshot snapshot) {
        for (Map.Entry<String, Boolean> entry : snapshot.cards.entrySet()) {
            UiCard card = catalog.findCard(entry.getKey());
            if (card != null && card.toggleable()) {
                boolean enabled = Boolean.TRUE.equals(entry.getValue());
                card.setEnabled(enabled);
                card.animToggle = enabled ? 1f : 0f;
                var cb = card.onToggleChanged();
                if (cb != null) {
                    cb.accept(enabled);
                }
            }
        }
        for (UiControl.Toggle toggle : catalog.allToggles()) {
            Boolean saved = snapshot.toggles.get(toggle.id());
            if (saved != null) {
                toggle.setValue(saved);
                toggle.animToggle = saved ? 1f : 0f;
            }
        }
        for (UiControl.Slider slider : catalog.allSliders()) {
            Float saved = snapshot.sliders.get(slider.id());
            if (saved != null) {
                slider.setClampedValue(saved);
                slider.animNorm = slider.normalized();
            }
        }
        for (Map.Entry<String, Integer> entry : snapshot.choices.entrySet()) {
            UiCard card = catalog.findCard(entry.getKey());
            if (card != null) {
                List<UiChoice> choices = card.choices();
                if (!choices.isEmpty() && entry.getValue() != null) {
                    int idx = Math.max(0, Math.min(choices.size() - 1, entry.getValue()));
                    card.setSelectedChoice(idx);
                    for (int i = 0; i < choices.size(); i++) {
                        choices.get(i).animSelected = i == idx ? 1f : 0f;
                    }
                    var cb = card.onChoiceChanged();
                    if (cb != null) {
                        cb.accept(idx);
                    }
                }
            }
        }
    }
}
