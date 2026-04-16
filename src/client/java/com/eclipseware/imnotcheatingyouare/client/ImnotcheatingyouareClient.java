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
    @SuppressWarnings("deprecation")
    public void onInitializeClient() {
        INSTANCE = this;
        moduleManager = new ModuleManager();
        settingsManager = new SettingsManager();
        clickGui = new Clickgui();

        // 1. Register Modules
        Module autoSprint = new Module("AutoSprint", Category.Movement);
        Module noJumpDelay = new Module("NoJumpDelay", Category.Movement);
        Module aimAssist = new com.eclipseware.imnotcheatingyouare.client.module.impl.AimAssist();
        Module triggerbot = new com.eclipseware.imnotcheatingyouare.client.module.impl.Triggerbot();
        Module wTap = new com.eclipseware.imnotcheatingyouare.client.module.impl.WTap();
Module hitSelect = new com.eclipseware.imnotcheatingyouare.client.module.impl.HitSelect();
        Module autoShieldBreaker = new com.eclipseware.imnotcheatingyouare.client.module.impl.AutoShieldBreaker();
        Module kbDisplacement = new com.eclipseware.imnotcheatingyouare.client.module.impl.KnockbackDisplacement();
        Module arrayListMod = new com.eclipseware.imnotcheatingyouare.client.module.impl.ArrayListMod();
        Module nameProtect = new com.eclipseware.imnotcheatingyouare.client.module.impl.NameProtect();
        Module breachSwap = new com.eclipseware.imnotcheatingyouare.client.module.impl.BreachSwap();
Module lungeAssist = new com.eclipseware.imnotcheatingyouare.client.module.impl.LungeAssist();
Module autoMace = new com.eclipseware.imnotcheatingyouare.client.module.impl.AutoMace();
Module pearlCatch = new com.eclipseware.imnotcheatingyouare.client.module.impl.PearlCatch();
Module jumpReset = new com.eclipseware.imnotcheatingyouare.client.module.impl.JumpReset();
        Module configurator = new com.eclipseware.imnotcheatingyouare.client.module.impl.Configurator();
        Module fullbright = new com.eclipseware.imnotcheatingyouare.client.module.impl.Fullbright();
        Module reach = new com.eclipseware.imnotcheatingyouare.client.module.impl.Reach();
        Module handView = new com.eclipseware.imnotcheatingyouare.client.module.impl.HandView();
        Module esp = new com.eclipseware.imnotcheatingyouare.client.module.impl.ESP();
        Module tracers = new com.eclipseware.imnotcheatingyouare.client.module.impl.Tracers();
        Module nametags = new com.eclipseware.imnotcheatingyouare.client.module.impl.Nametags();

        moduleManager.modules.add(autoSprint);
        moduleManager.modules.add(noJumpDelay);
        moduleManager.modules.add(aimAssist);
        moduleManager.modules.add(wTap);
        moduleManager.modules.add(triggerbot);
        moduleManager.modules.add(hitSelect);
        moduleManager.modules.add(autoShieldBreaker);
        moduleManager.modules.add(kbDisplacement);
        moduleManager.modules.add(arrayListMod);
        moduleManager.modules.add(nameProtect);
        moduleManager.modules.add(breachSwap);
moduleManager.modules.add(lungeAssist);
moduleManager.modules.add(autoMace);
moduleManager.modules.add(pearlCatch);
moduleManager.modules.add(jumpReset);
moduleManager.modules.add(configurator);
        moduleManager.modules.add(fullbright);
        moduleManager.modules.add(reach);
        moduleManager.modules.add(handView);
        moduleManager.modules.add(esp);
        moduleManager.modules.add(tracers);
        moduleManager.modules.add(nametags);

        Module automine = new com.eclipseware.imnotcheatingyouare.client.module.impl.Automine();
        Module autowalk = new com.eclipseware.imnotcheatingyouare.client.module.impl.AutoWalk();
        Module guimove = new com.eclipseware.imnotcheatingyouare.client.module.impl.GUIMove();
        Module autosign = new com.eclipseware.imnotcheatingyouare.client.module.impl.AutoSign();
        Module freecam = new com.eclipseware.imnotcheatingyouare.client.module.impl.Freecam();

        moduleManager.modules.add(automine);
        moduleManager.modules.add(autowalk);
        moduleManager.modules.add(guimove);
        moduleManager.modules.add(autosign);
        moduleManager.modules.add(freecam);
        moduleManager.modules.add(new com.eclipseware.imnotcheatingyouare.client.module.impl.AntiTranslationKey());

        Module backtrack = new com.eclipseware.imnotcheatingyouare.client.module.impl.Backtrack();
        Module blink = new com.eclipseware.imnotcheatingyouare.client.module.impl.BlinkModule();
        moduleManager.modules.add(backtrack);
        moduleManager.modules.add(blink);

        Module theme = new Module("Theme", Category.Render, "Customizes the client's UI colors and animations.");
        moduleManager.modules.add(theme);

        // --- REGISTER SETTINGS ---
        settingsManager.rSetting(new Setting("Accent R", theme, 155.0, 0.0, 255.0, true));
        settingsManager.rSetting(new Setting("Accent G", theme, 60.0, 0.0, 255.0, true));
        settingsManager.rSetting(new Setting("Accent B", theme, 255.0, 0.0, 255.0, true));
        settingsManager.rSetting(new Setting("Background Alpha", theme, 240.0, 0.0, 255.0, true));
        settingsManager.rSetting(new Setting("Anim Speed", theme, 5.0, 1.0, 10.0, false));

        java.util.ArrayList<String> aimModes = new java.util.ArrayList<>();
        aimModes.add("Human"); aimModes.add("Smooth"); aimModes.add("Linear");
        settingsManager.rSetting(new Setting("Mode", aimAssist, "Human", aimModes));
        settingsManager.rSetting(new Setting("Smoothness", aimAssist, 7.0, 1.0, 10.0, false));
        settingsManager.rSetting(new Setting("Speed", aimAssist, 3.0, 1.0, 10.0, false));
        settingsManager.rSetting(new Setting("Ignore Mouse %", aimAssist, 40.0, 0.0, 100.0, true));
        settingsManager.rSetting(new Setting("Attack Only", aimAssist, true));
        settingsManager.rSetting(new Setting("Stop On Target", aimAssist, true));
        settingsManager.rSetting(new Setting("FOV", aimAssist, 60.0, 10.0, 360.0, true));
        settingsManager.rSetting(new Setting("Range", aimAssist, 4.0, 1.0, 8.0, false));
        settingsManager.rSetting(new Setting("Players", aimAssist, true));
        settingsManager.rSetting(new Setting("Hostile Mobs", aimAssist, true));
        settingsManager.rSetting(new Setting("Passive Mobs", aimAssist, false));

        settingsManager.rSetting(new Setting("Wait Ticks", wTap, 0.0, 0.0, 10.0, true));
        settingsManager.rSetting(new Setting("Action Ticks", wTap, 1.0, 1.0, 5.0, true));

        java.util.ArrayList<String> tbModes = new java.util.ArrayList<>();
        tbModes.add("Legit"); tbModes.add("Blatant");
        settingsManager.rSetting(new Setting("Mode", triggerbot, "Legit", tbModes));
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
        hsModes.add("HurtTime"); hsModes.add("Criticals");
        settingsManager.rSetting(new Setting("Mode", hitSelect, "HurtTime", hsModes));
        settingsManager.rSetting(new Setting("Max HurtTime", hitSelect, 5.0, 0.0, 10.0, true));
        settingsManager.rSetting(new Setting("Auto Punish", hitSelect, false));
        settingsManager.rSetting(new Setting("Punish Delay (Ticks)", hitSelect, 3.0, 0.0, 10.0, true));

        java.util.ArrayList<String> asbModes = new java.util.ArrayList<>();
        asbModes.add("Swap"); asbModes.add("Silent");
        settingsManager.rSetting(new Setting("Mode", autoShieldBreaker, "Swap", asbModes));
        settingsManager.rSetting(new Setting("Delay (ms)", autoShieldBreaker, 50.0, 0.0, 1000.0, true));
        settingsManager.rSetting(new Setting("Swap Back", autoShieldBreaker, true));
        settingsManager.rSetting(new Setting("Swap Back Delay (ms)", autoShieldBreaker, 100.0, 0.0, 1000.0, true));

        java.util.ArrayList<String> kbModes = new java.util.ArrayList<>();
        kbModes.add("Pull"); kbModes.add("Upward"); kbModes.add("Horizontal"); kbModes.add("Custom");
        settingsManager.rSetting(new Setting("Mode", kbDisplacement, "Pull", kbModes));
        settingsManager.rSetting(new Setting("Auto Sprint", kbDisplacement, true));
        settingsManager.rSetting(new Setting("Delay (Ticks)", kbDisplacement, 2.0, 0.0, 5.0, true));
        settingsManager.rSetting(new Setting("Cooldown (Ticks)", kbDisplacement, 15.0, 0.0, 40.0, true));
        settingsManager.rSetting(new Setting("Custom Yaw", kbDisplacement, 0.0, -180.0, 180.0, true));
        settingsManager.rSetting(new Setting("Custom Pitch", kbDisplacement, 0.0, -90.0, 90.0, true));

        java.util.ArrayList<String> alAlignments = new java.util.ArrayList<>();
        alAlignments.add("Left"); alAlignments.add("Right");
        settingsManager.rSetting(new Setting("Alignment", arrayListMod, "Left", alAlignments));
        settingsManager.rSetting(new Setting("Sync Theme", arrayListMod, true));
        settingsManager.rSetting(new Setting("Red", arrayListMod, 230.0, 0.0, 255.0, true));
        settingsManager.rSetting(new Setting("Green", arrayListMod, 10.0, 0.0, 255.0, true));
        settingsManager.rSetting(new Setting("Blue", arrayListMod, 230.0, 0.0, 255.0, true));
        settingsManager.rSetting(new Setting("Y Offset", arrayListMod, 5.0, 0.0, 100.0, true));

        java.util.ArrayList<String> npNames = new java.util.ArrayList<>();
        npNames.add("Marlowww"); npNames.add("Hidden"); npNames.add("You");
        settingsManager.rSetting(new Setting("Name", nameProtect, "Marlowww", npNames));
        settingsManager.rSetting(new Setting("TIP: Hide chat in Accessibility Settings", nameProtect, false));

        java.util.ArrayList<String> bsModes = new java.util.ArrayList<>();
bsModes.add("Swap"); bsModes.add("Silent");
        settingsManager.rSetting(new Setting("Mode", breachSwap, "Swap", bsModes));
        settingsManager.rSetting(new Setting("Swap Back", breachSwap, true));
        settingsManager.rSetting(new Setting("Swap Back Delay (ms)", breachSwap, 100.0, 0.0, 1000.0, true));
        
        settingsManager.rSetting(new Setting("AutoJump", lungeAssist, true));

        java.util.ArrayList<String> jrModes = new java.util.ArrayList<>();
jrModes.add("Legit"); jrModes.add("Blatant");
settingsManager.rSetting(new Setting("Mode", jumpReset, "Legit", jrModes));
settingsManager.rSetting(new Setting("Delay (Ticks)", jumpReset, 0.0, 0.0, 5.0, true));
settingsManager.rSetting(new Setting("Velocity Threshold", jumpReset, 0.1, 0.0, 1.0, false));
settingsManager.rSetting(new Setting("Delay (Ticks)", pearlCatch, 4.0, 0.0, 20.0, true));

        settingsManager.rSetting(new Setting("Distance", reach, 0.5, 0.0, 1.0, false));

        settingsManager.rSetting(new Setting("Main Scale X", handView, 1.0, 0.1, 3.0, false));
        settingsManager.rSetting(new Setting("Main Scale Y", handView, 1.0, 0.1, 3.0, false));
        settingsManager.rSetting(new Setting("Main Scale Z", handView, 1.0, 0.1, 3.0, false));
        settingsManager.rSetting(new Setting("Main Pos X", handView, 0.0, -2.0, 2.0, false));
        settingsManager.rSetting(new Setting("Main Pos Y", handView, 0.0, -2.0, 2.0, false));
        settingsManager.rSetting(new Setting("Main Pos Z", handView, 0.0, -2.0, 2.0, false));
        settingsManager.rSetting(new Setting("Off Scale X", handView, 1.0, 0.1, 3.0, false));
        settingsManager.rSetting(new Setting("Off Scale Y", handView, 1.0, 0.1, 3.0, false));
        settingsManager.rSetting(new Setting("Off Scale Z", handView, 1.0, 0.1, 3.0, false));
        settingsManager.rSetting(new Setting("Off Pos X", handView, 0.0, -2.0, 2.0, false));
        settingsManager.rSetting(new Setting("Off Pos Y", handView, 0.0, -2.0, 2.0, false));
        settingsManager.rSetting(new Setting("Off Pos Z", handView, 0.0, -2.0, 2.0, false));

        java.util.ArrayList<String> espModes = new java.util.ArrayList<>();
        espModes.add("Outline"); espModes.add("3D"); espModes.add("2D"); espModes.add("Hybrid");
        settingsManager.rSetting(new Setting("Mode", esp, "Outline", espModes));
        settingsManager.rSetting(new Setting("Show Mobs", esp, false));

        settingsManager.rSetting(new Setting("Crosshair Attach", tracers, true));
        settingsManager.rSetting(new Setting("Show Mobs", tracers, false));

        settingsManager.rSetting(new Setting("Players", nametags, true));
        settingsManager.rSetting(new Setting("Show Mobs", nametags, false));

        java.util.ArrayList<String> btModes = new java.util.ArrayList<>();
        btModes.add("Latency"); btModes.add("Pulse");
        settingsManager.rSetting(new Setting("Mode", backtrack, "Latency", btModes));
        settingsManager.rSetting(new Setting("Delay Min (ms)", backtrack, 100.0, 0.0, 2000.0, true));
        settingsManager.rSetting(new Setting("Delay Max (ms)", backtrack, 500.0, 0.0, 2000.0, true));
        settingsManager.rSetting(new Setting("Through Walls", backtrack, false));

        java.util.ArrayList<String> blinkModes = new java.util.ArrayList<>();
        blinkModes.add("Pulse"); blinkModes.add("Latency");
        settingsManager.rSetting(new Setting("Mode", blink, "Pulse", blinkModes));
        settingsManager.rSetting(new Setting("Delay Min (ms)", blink, 100.0, 0.0, 2000.0, true));
        settingsManager.rSetting(new Setting("Delay Max (ms)", blink, 500.0, 0.0, 2000.0, true));

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

        // 4. Load Configuration
        com.eclipseware.imnotcheatingyouare.client.setting.ConfigManager.load();

        // 5. Save config if the user closes the game unexpectedly (with daemon thread patch)
        Thread saveHook = new Thread(com.eclipseware.imnotcheatingyouare.client.setting.ConfigManager::save, "ConfigSaveHook");
        saveHook.setDaemon(true);
        Runtime.getRuntime().addShutdownHook(saveHook);

        // 6. Register Real Client Commands
        net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("config")
                .then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("gui")
                    .executes(context -> {
                        net.minecraft.client.Minecraft.getInstance().execute(() ->
                            net.minecraft.client.Minecraft.getInstance().setScreen(new com.eclipseware.imnotcheatingyouare.client.clickgui.ConfigGui())
                        );
                        return 1;
                    })
                )
                .then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("export")
                    .executes(context -> {
                        String exp = com.eclipseware.imnotcheatingyouare.client.setting.ConfigManager.exportSpecific(moduleManager.modules);
                        net.minecraft.client.Minecraft.getInstance().keyboardHandler.setClipboard(exp);
                        context.getSource().sendFeedback(net.minecraft.network.chat.Component.literal("§d[EclipseWare] §7Config exported to clipboard!"));
                        return 1;
                    })
                )
            );
        });
    }
}