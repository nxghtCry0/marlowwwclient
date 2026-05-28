package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.macro.MacroManager;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;

public class MacroModule extends Module {
    public MacroModule() {
        super("Macro", Category.Utility, "Records and plays back inputs.");
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Record", this, false));
    }

    @Override
    public void onEnable() {
        Setting recordSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Record");
        boolean isRecordMode = recordSetting != null && recordSetting.getValBoolean();
        if (isRecordMode) {
            MacroManager.startRecord();
        } else {
            MacroManager.startPlay();
        }
    }

    @Override
    public void onDisable() {
        MacroManager.stopRecord();
        MacroManager.stopPlay();
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.level == null) return;

        Setting recordSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Record");
        boolean isRecordMode = recordSetting != null && recordSetting.getValBoolean();

        if (isRecordMode) {
            if (MacroManager.isPlaying()) {
                MacroManager.stopPlay();
            }
            if (!MacroManager.isRecording()) {
                MacroManager.startRecord();
            }
        } else {
            if (MacroManager.isRecording()) {
                MacroManager.stopRecord();
            }
            if (!MacroManager.isPlaying() && isToggled()) {
                toggle();
                return;
            }
        }

    }
}
