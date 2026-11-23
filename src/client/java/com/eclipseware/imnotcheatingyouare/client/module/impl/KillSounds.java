package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.ConfigManager;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.setting.SettingsManager;
import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public class KillSounds extends Module {
    public static KillSounds INSTANCE;

    private final Map<UUID, Long> attackedPlayers = new HashMap<>();
    private final Set<UUID> triggeredDeaths = new HashSet<>();

    public KillSounds() {
        super("KillSounds", Category.Utility, "Plays a custom audio file whenever a player you are attacking dies.");
        INSTANCE = this;
        ImnotcheatingyouareClient client = ImnotcheatingyouareClient.INSTANCE;
        if (client == null || client.settingsManager == null) {
            return;
        }
        SettingsManager sm = client.settingsManager;
        sm.rSetting(new Setting("Volume (%)", this, 100.0, 0.0, 200.0, true));
        sm.rSetting(new Setting("Credit Window (s)", this, 10.0, 2.0, 30.0, false));
        sm.rSetting(new Setting("Sound File", this, "", true));
    }

    public static void recordAttack(Entity target) {
        KillSounds inst = INSTANCE;
        if (inst == null || target == null || mc == null || target == mc.player) {
            return;
        }
        if (target instanceof Player victim) {
            inst.attackedPlayers.put(victim.getUUID(), System.currentTimeMillis());
        }
    }

    @Override
    public void onEnable() {
        attackedPlayers.clear();
        triggeredDeaths.clear();
        if (mc != null && mc.getWindow() != null && !hasValidSoundFile()) {
            promptForSoundFile(null);
        }
    }

    @Override
    public void onDisable() {
        attackedPlayers.clear();
        triggeredDeaths.clear();
    }

    @Override
    public void onTick() {
        if (mc == null || mc.level == null || mc.player == null) {
            attackedPlayers.clear();
            triggeredDeaths.clear();
            return;
        }

        long now = System.currentTimeMillis();
        long windowMs = Math.round(getVolumeOrWindow("Credit Window (s)", 10.0) * 1000.0);

        Iterator<Map.Entry<UUID, Long>> expIt = attackedPlayers.entrySet().iterator();
        while (expIt.hasNext()) {
            Map.Entry<UUID, Long> entry = expIt.next();
            if (now - entry.getValue() > windowMs) {
                expIt.remove();
            }
        }

        for (Player player : mc.level.players()) {
            if (player == mc.player) {
                continue;
            }
            UUID uuid = player.getUUID();
            boolean dead = player.isDeadOrDying() || player.getHealth() <= 0.0f;
            if (!dead) {
                triggeredDeaths.remove(uuid);
                continue;
            }
            if (attackedPlayers.containsKey(uuid) && triggeredDeaths.add(uuid)) {
                attackedPlayers.remove(uuid);
                playConfiguredSound();
            }
        }
    }

    public boolean hasValidSoundFile() {
        String path = getSoundFilePath();
        if (path.isBlank()) {
            return false;
        }
        File file = new File(path);
        return file.isFile();
    }

    public String getSoundFilePath() {
        Setting s = setting("Sound File");
        if (s == null || s.getValText() == null) {
            return "";
        }
        return s.getValText().trim();
    }

    public String getSoundDisplayName() {
        String path = getSoundFilePath();
        if (path.isBlank()) {
            return "No file selected";
        }
        File f = new File(path);
        return f.getName();
    }

    public void setSoundFilePath(String path) {
        Setting s = setting("Sound File");
        if (s != null) {
            s.setValText(path != null ? path.trim() : "");
            ConfigManager.save();
        }
    }

    public void promptForSoundFile(Consumer<File> callback) {
        String currentPath = getSoundFilePath();
        KillSoundPlayer.openAudioFileChooserAsync(currentPath, selected -> {
            if (selected != null && selected.isFile()) {
                setSoundFilePath(selected.getAbsolutePath());
            }
            if (callback != null) {
                callback.accept(selected);
            }
        });
    }

    public boolean playConfiguredSound() {
        String path = getSoundFilePath();
        if (path.isBlank()) {
            return false;
        }
        float volScale = (float) (getVolumeOrWindow("Volume (%)", 100.0) / 100.0);
        return KillSoundPlayer.playSoundAsync(path, volScale);
    }

    private double getVolumeOrWindow(String settingName, double fallback) {
        Setting s = setting(settingName);
        return s != null ? s.getValDouble() : fallback;
    }

    private Setting setting(String name) {
        ImnotcheatingyouareClient client = ImnotcheatingyouareClient.INSTANCE;
        if (client == null || client.settingsManager == null) {
            return null;
        }
        SettingsManager sm = client.settingsManager;
        return sm.getSettingByName(this, name);
    }
}
