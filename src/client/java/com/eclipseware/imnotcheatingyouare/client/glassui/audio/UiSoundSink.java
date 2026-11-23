package com.eclipseware.imnotcheatingyouare.client.glassui.audio;

public interface UiSoundSink {
    UiSoundSink SILENT = new UiSoundSink() {};

    default void playOpen() {
    }

    default void playClose() {
    }

    default void playClick(float pitchScale) {
    }

    default void playClick() {
        playClick(1.0f);
    }

    default void playTab(float pitchScale) {
    }

    default void playTab() {
        playTab(1.0f);
    }

    default void playSliderScrub(float normalized) {
    }

    default void playTypeKey() {
    }

    default void playToggle(boolean enabled) {
    }

    default void playNotify() {
    }
}
