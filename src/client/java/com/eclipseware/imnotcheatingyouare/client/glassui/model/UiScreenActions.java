package com.eclipseware.imnotcheatingyouare.client.glassui.model;

import com.eclipseware.imnotcheatingyouare.client.glassui.audio.UiSoundSink;
import com.eclipseware.imnotcheatingyouare.client.glassui.theme.UiTheme;

public interface UiScreenActions {
    UiTheme theme();

    UiSoundSink sounds();

    float elapsedSeconds();

    void showToast(String message);

    void spawnRipple(float x, float y, float maxRadius, int color);

    default void spawnRipple(float x, float y) {
        UiTheme activeTheme = theme();
        spawnRipple(x, y, 20f, activeTheme.accentColor());
    }

    void requestClose();

    void closeOverlays();

    void openCardDetail(String cardId, float localX, float localY);

    void refreshDashboardAnimation();

    void syncTextInput();
}
