package com.eclipseware.imnotcheatingyouare.client;

import com.eclipseware.imnotcheatingyouare.client.clickgui.Clickgui;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.module.ModuleManager;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.setting.SettingsManager;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public class ImnotcheatingyouareClient implements ClientModInitializer {
    public static ImnotcheatingyouareClient INSTANCE;

    public ModuleManager moduleManager;
    public SettingsManager settingsManager;
    public Clickgui clickGui;

    private KeyMapping guiBind;

    @Override
    public void onInitializeClient() {
        INSTANCE = this;
        moduleManager = new ModuleManager();
        settingsManager = new SettingsManager();
        clickGui = new Clickgui();

        // 1. Register Modules
        Module fullbright = new com.eclipseware.imnotcheatingyouare.client.module.impl.Fullbright();
        Module autoSprint = new Module("AutoSprint", Category.Movement);
        Module noJumpDelay = new Module("NoJumpDelay", Category.Movement);
        Module aimAssist = new com.eclipseware.imnotcheatingyouare.client.module.impl.AimAssist();
        Module triggerbot = new com.eclipseware.imnotcheatingyouare.client.module.impl.Triggerbot();
        Module hitSelect = new com.eclipseware.imnotcheatingyouare.client.module.impl.HitSelect();
        Module autoShieldBreaker = new com.eclipseware.imnotcheatingyouare.client.module.impl.AutoShieldBreaker();
Module arrayListMod = new com.eclipseware.imnotcheatingyouare.client.module.impl.ArrayListMod();
Module nameProtect = new com.eclipseware.imnotcheatingyouare.client.module.impl.NameProtect();
Module breachSwap = new com.eclipseware.imnotcheatingyouare.client.module.impl.BreachSwap();
Module lungeAssist = new com.eclipseware.imnotcheatingyouare.client.module.impl.LungeAssist();

    moduleManager.modules.add(fullbright);
    moduleManager.modules.add(autoSprint);
    moduleManager.modules.add(noJumpDelay);
    moduleManager.modules.add(aimAssist);
    moduleManager.modules.add(triggerbot);
    moduleManager.modules.add(hitSelect);
    moduleManager.modules.add(autoShieldBreaker);
    moduleManager.modules.add(arrayListMod);
    moduleManager.modules.add(nameProtect);
    moduleManager.modules.add(breachSwap);
    moduleManager.modules.add(lungeAssist);

        // --- REGISTER SETTINGS ---
        java.util.ArrayList<String> fbModes = new java.util.ArrayList<>();
        fbModes.add("Night Vision");
        fbModes.add("Gamma");
        settingsManager.rSetting(new Setting("Mode", fullbright, "Night Vision", fbModes));

        java.util.ArrayList<String> aimModes = new java.util.ArrayList<>();
        aimModes.add("Wind");
        aimModes.add("Smooth");
        aimModes.add("Snap");
        settingsManager.rSetting(new Setting("Mode", aimAssist, "Wind", aimModes));
        settingsManager.rSetting(new Setting("Smoothness", aimAssist, 5.0, 1.0, 10.0, false));
        settingsManager.rSetting(new Setting("Speed", aimAssist, 5.0, 1.0, 10.0, false));
        settingsManager.rSetting(new Setting("Ignore Mouse %", aimAssist, 20.0, 0.0, 100.0, true));
        settingsManager.rSetting(new Setting("Stop On Target", aimAssist, true));
        settingsManager.rSetting(new Setting("FOV", aimAssist, 90.0, 10.0, 360.0, true));
        settingsManager.rSetting(new Setting("Range", aimAssist, 4.0, 1.0, 8.0, false));
        settingsManager.rSetting(new Setting("Players", aimAssist, true));
        settingsManager.rSetting(new Setting("Hostile Mobs", aimAssist, true));
        settingsManager.rSetting(new Setting("Passive Mobs", aimAssist, false));

        settingsManager.rSetting(new Setting("Range", triggerbot, 4.25, 1.0, 6.0, false));
        settingsManager.rSetting(new Setting("Min Delay (Ticks)", triggerbot, 1.0, 0.0, 20.0, true));
        settingsManager.rSetting(new Setting("Max Delay (Ticks)", triggerbot, 4.0, 0.0, 20.0, true));
        settingsManager.rSetting(new Setting("Inventory Fix", triggerbot, true));
        settingsManager.rSetting(new Setting("Simulate Mouse Click", triggerbot, true)); 
        settingsManager.rSetting(new Setting("Packet Bypass", triggerbot, false)); 
        settingsManager.rSetting(new Setting("Players", triggerbot, true));
        settingsManager.rSetting(new Setting("Hostile Mobs", triggerbot, true));
        settingsManager.rSetting(new Setting("Passive Mobs", triggerbot, false));

        java.util.ArrayList<String> hsModes = new java.util.ArrayList<>();
        hsModes.add("HurtTime");
        hsModes.add("Criticals");
        settingsManager.rSetting(new Setting("Mode", hitSelect, "HurtTime", hsModes));
        settingsManager.rSetting(new Setting("Max HurtTime", hitSelect, 5.0, 0.0, 10.0, true));

        java.util.ArrayList<String> asbModes = new java.util.ArrayList<>();
        asbModes.add("Swap");
        asbModes.add("Silent");
        settingsManager.rSetting(new Setting("Mode", autoShieldBreaker, "Swap", asbModes));
        settingsManager.rSetting(new Setting("Delay (ms)", autoShieldBreaker, 50.0, 0.0, 1000.0, true));
        settingsManager.rSetting(new Setting("Swap Back", autoShieldBreaker, true));
        settingsManager.rSetting(new Setting("Swap Back Delay (ms)", autoShieldBreaker, 100.0, 0.0, 1000.0, true));

        java.util.ArrayList<String> alAlignments = new java.util.ArrayList<>();
        alAlignments.add("Left");
        alAlignments.add("Right");
        settingsManager.rSetting(new Setting("Alignment", arrayListMod, "Left", alAlignments));
        settingsManager.rSetting(new Setting("Red", arrayListMod, 230.0, 0.0, 255.0, true));
settingsManager.rSetting(new Setting("Green", arrayListMod, 10.0, 0.0, 255.0, true));
settingsManager.rSetting(new Setting("Blue", arrayListMod, 230.0, 0.0, 255.0, true));

    java.util.ArrayList<String> npNames = new java.util.ArrayList<>();
    npNames.add("Marlowww");
    npNames.add("Hidden");
    npNames.add("You");
    settingsManager.rSetting(new Setting("Name", nameProtect, "Marlowww", npNames));

    // 2. Register Category
        net.minecraft.client.KeyMapping.Category guiCategory = net.minecraft.client.KeyMapping.Category.register(
            net.minecraft.resources.Identifier.fromNamespaceAndPath("imnotcheatingyouare", "main")
        );

        // Register Right Shift Keybind
        guiBind = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.imnotcheatingyouare.clickgui",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                guiCategory
        ));

        // Register HUD
        net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback.EVENT.register((guiGraphics, tickDelta) -> {
            com.eclipseware.imnotcheatingyouare.client.ui.ArrayListHud.INSTANCE.render(guiGraphics, tickDelta.getGameTimeDeltaTicks());
        });

        // 3. Listen for Key Press and Tick Modules
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (guiBind.consumeClick()) {
                if (!(client.screen instanceof Clickgui)) {
                    client.setScreen(clickGui);
                }
            }

            for (Module m : moduleManager.modules) {
m.tickKeybind(); // Listen for keys universally
if (m.isToggled()) {
m.onTick();
}
}
});
}
}