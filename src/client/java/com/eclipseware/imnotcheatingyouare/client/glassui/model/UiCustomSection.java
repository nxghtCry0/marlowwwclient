package com.eclipseware.imnotcheatingyouare.client.glassui.model;

import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;

public interface UiCustomSection {
    float measureHeight(UiSectionMeasureContext context);

    void render(UiSectionRenderContext context);

    default boolean onMouseClicked(UiScreenActions actions, float localX, float localY, int button) {
        return false;
    }

    default boolean onKeyPressed(UiScreenActions actions, KeyEvent event) {
        return false;
    }

    default boolean onCharTyped(UiScreenActions actions, CharacterEvent event) {
        return false;
    }

    default boolean isTextInputFocused() {
        return false;
    }

    default void clearFocus(UiScreenActions actions) {
    }

    default void clearHitboxes() {
    }
}
