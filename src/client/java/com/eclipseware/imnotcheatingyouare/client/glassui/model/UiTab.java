package com.eclipseware.imnotcheatingyouare.client.glassui.model;

import java.util.List;

public final class UiTab {
    private final String id;
    private final String title;
    private final List<UiCard> cards;
    private final UiCustomSection customSection;
    private final String footerHint;
    public float animHover = 0f;
    public float animActive = 0f;

    public UiTab(String id, String title, List<UiCard> cards, UiCustomSection customSection, String footerHint) {
        this.id = id;
        this.title = title;
        this.cards = cards != null ? List.copyOf(cards) : List.of();
        this.customSection = customSection;
        this.footerHint = footerHint;
    }

    public String id() {
        return id;
    }

    public String title() {
        return title;
    }

    public List<UiCard> cards() {
        return cards;
    }

    public UiCustomSection customSection() {
        return customSection;
    }

    public String footerHint() {
        return footerHint;
    }
}
