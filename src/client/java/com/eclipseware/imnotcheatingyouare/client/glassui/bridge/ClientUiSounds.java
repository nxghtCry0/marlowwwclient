package com.eclipseware.imnotcheatingyouare.client.glassui.bridge;

import com.eclipseware.imnotcheatingyouare.client.glassui.asset.UiAssets;
import com.eclipseware.imnotcheatingyouare.client.glassui.audio.UiSoundSink;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

public final class ClientUiSounds implements UiSoundSink {
    public static final ClientUiSounds INSTANCE = new ClientUiSounds();

    private static SoundEvent uiClick;
    private static SoundEvent uiTick;
    private static SoundEvent uiType;
    private static SoundEvent moduleOn;
    private static SoundEvent moduleOff;
    private static SoundEvent notifyPop;
    private static SoundEvent guiOpen;
    private static SoundEvent guiClose;
    private static boolean initialized = false;

    private long lastTypeSoundMs = 0L;
    private long lastSliderTickMs = 0L;

    private ClientUiSounds() {
    }

    public static synchronized void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        uiClick = registerSound("ui_click");
        uiTick = registerSound("ui_tick");
        uiType = registerSound("ui_type");
        moduleOn = registerSound("module_on");
        moduleOff = registerSound("module_off");
        notifyPop = registerSound("notify_pop");
        guiOpen = registerSound("gui_open");
        guiClose = registerSound("gui_close");
    }

    private static SoundEvent registerSound(String name) {
        Identifier id = UiAssets.id(name);
        SoundEvent created = SoundEvent.createVariableRangeEvent(id);
        try {
            return Registry.register(BuiltInRegistries.SOUND_EVENT, id, created);
        } catch (Throwable ignored) {
            return created;
        }
    }

    private static float masterGain() {
        if (!ClientGuiRegistry.isSoundEnabled()) {
            return 0f;
        }
        float raw = ClientGuiRegistry.getSlider("sound_volume", 100f) / 100f;
        return Math.max(0f, Math.min(1.5f, raw));
    }

    private static int soundPackMode() {
        return ClientGuiRegistry.getChoice("sound_alerts", 0);
    }

    private static void playUi(SoundEvent event, float pitch, float baseVolume) {
        if (event == null) {
            return;
        }
        float gain = masterGain();
        if (gain <= 0.01f) {
            return;
        }
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.getSoundManager() == null) {
                return;
            }
            SoundManager sm = mc.getSoundManager();
            float clampedPitch = Math.max(0.5f, Math.min(2.0f, pitch));
            float vol = Math.max(0.02f, Math.min(2.0f, baseVolume * gain));
            sm.play(SimpleSoundInstance.forUI(event, clampedPitch, vol));
        } catch (Throwable ignored) {
        }
    }

    private static SoundEvent vanillaClick() {
        var holder = SoundEvents.UI_BUTTON_CLICK;
        return holder.value();
    }

    @Override
    public void playOpen() {
        init();
        switch (soundPackMode()) {
            case 2 -> playUi(uiTick, 1.15f, 0.52f);
            case 3 -> {
            }
            default -> playUi(guiOpen, 1.0f, 0.62f);
        }
    }

    @Override
    public void playClose() {
        init();
        switch (soundPackMode()) {
            case 2 -> playUi(uiTick, 0.88f, 0.48f);
            case 3 -> playUi(vanillaClick(), 0.92f, 0.20f);
            default -> playUi(guiClose, 1.0f, 0.58f);
        }
    }

    @Override
    public void playClick(float pitchScale) {
        init();
        switch (soundPackMode()) {
            case 1 -> playUi(uiType, 0.96f * pitchScale, 0.72f);
            case 2 -> playUi(uiTick, 1.02f * pitchScale, 0.55f);
            case 3 -> playUi(vanillaClick(), pitchScale, 0.25f);
            default -> playUi(uiClick, pitchScale, 0.78f);
        }
    }

    @Override
    public void playTab(float pitchScale) {
        init();
        if (soundPackMode() == 3) {
            playUi(vanillaClick(), 1.12f * pitchScale, 0.20f);
        } else {
            playUi(uiTick, pitchScale, 0.68f);
        }
    }

    @Override
    public void playSliderScrub(float normalized) {
        init();
        long now = System.currentTimeMillis();
        if (now - lastSliderTickMs < 42L) {
            return;
        }
        lastSliderTickMs = now;
        float clamped = Math.max(0f, Math.min(1f, normalized));
        playUi(uiTick, 0.86f + clamped * 0.36f, 0.38f);
    }

    @Override
    public void playTypeKey() {
        init();
        long now = System.currentTimeMillis();
        if (now - lastTypeSoundMs < 24L) {
            return;
        }
        lastTypeSoundMs = now;
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        float jitter = (rng.nextFloat() - 0.5f) * 0.12f;
        switch (soundPackMode()) {
            case 2 -> playUi(uiTick, 1.08f + jitter, 0.42f);
            case 3 -> playUi(vanillaClick(), 1.35f + jitter, 0.14f);
            default -> playUi(uiType, 1.0f + jitter, 0.62f);
        }
    }

    @Override
    public void playToggle(boolean enabled) {
        init();
        if (!ClientGuiRegistry.getBool("toggle_chimes", true)) {
            playClick(enabled ? 1.12f : 0.92f);
            return;
        }
        switch (soundPackMode()) {
            case 1 -> playUi(enabled ? moduleOn : moduleOff, enabled ? 0.88f : 0.86f, 0.68f);
            case 2 -> playUi(uiTick, enabled ? 1.24f : 0.84f, 0.64f);
            case 3 -> playUi(vanillaClick(), enabled ? 1.2f : 0.9f, 0.26f);
            default -> playUi(enabled ? moduleOn : moduleOff, 1.0f, 0.76f);
        }
    }

    @Override
    public void playNotify() {
        init();
        switch (soundPackMode()) {
            case 2 -> playUi(uiTick, 1.28f, 0.62f);
            case 3 -> playUi(vanillaClick(), 1.25f, 0.25f);
            default -> playUi(notifyPop, 1.0f, 0.72f);
        }
    }
}
