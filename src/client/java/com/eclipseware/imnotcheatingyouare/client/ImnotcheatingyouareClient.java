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
import net.minecraft.client.multiplayer.ServerData;
import org.lwjgl.glfw.GLFW;

public class ImnotcheatingyouareClient implements ClientModInitializer {
    public static ImnotcheatingyouareClient INSTANCE;

    public ModuleManager moduleManager;
    public SettingsManager settingsManager;
    public Clickgui clickGui;

    private KeyMapping guiBind;

    private void addColorSettings(SettingsManager sm, Module module, String colorName, int r, int g, int b) {
    sm.rSetting(new Setting(colorName + " R", module, (double) r, 0.0, 255.0, true));
    sm.rSetting(new Setting(colorName + " G", module, (double) g, 0.0, 255.0, true));
    sm.rSetting(new Setting(colorName + " B", module, (double) b, 0.0, 255.0, true));
}

@Override
@SuppressWarnings("deprecation")
public void onInitializeClient() {
        Module.mc = net.minecraft.client.Minecraft.getInstance();
        INSTANCE = this;
        moduleManager = new ModuleManager();
        settingsManager = new SettingsManager();

Module autoSprint = new Module("AutoSprint", Category.Movement);
Module noJumpDelay = new Module("NoJumpDelay", Category.Movement);
Module aimAssist = new com.eclipseware.imnotcheatingyouare.client.module.impl.AimAssist();
Module triggerbot = new com.eclipseware.imnotcheatingyouare.client.module.impl.Triggerbot();
Module autoWeb = new com.eclipseware.imnotcheatingyouare.client.module.impl.AutoWeb();
Module shieldDrain = new com.eclipseware.imnotcheatingyouare.client.module.impl.ShieldDrain();
Module attributeSwap = new com.eclipseware.imnotcheatingyouare.client.module.impl.AttributeSwap();
Module autoMaceCounter = new com.eclipseware.imnotcheatingyouare.client.module.impl.AutoMaceCounter();
Module autoDrain = new com.eclipseware.imnotcheatingyouare.client.module.impl.AutoDrain();
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

Module autoClicker = new com.eclipseware.imnotcheatingyouare.client.module.impl.AutoClicker();
Module fastPlace = new com.eclipseware.imnotcheatingyouare.client.module.impl.FastPlace();
Module bridgeAssist = new com.eclipseware.imnotcheatingyouare.client.module.impl.BridgeAssist();
Module scaffold = new com.eclipseware.imnotcheatingyouare.client.module.impl.Scaffold();

Module xray = new com.eclipseware.imnotcheatingyouare.client.module.impl.Xray();
Module elytraBoost = new com.eclipseware.imnotcheatingyouare.client.module.impl.ElytraBoost();

Module configurator = new com.eclipseware.imnotcheatingyouare.client.module.impl.Configurator();
Module fullbright = new com.eclipseware.imnotcheatingyouare.client.module.impl.Fullbright();
Module reach = new com.eclipseware.imnotcheatingyouare.client.module.impl.Reach();
Module handView = new com.eclipseware.imnotcheatingyouare.client.module.impl.HandView();
Module esp = new com.eclipseware.imnotcheatingyouare.client.module.impl.ESP();
Module noParticles = new com.eclipseware.imnotcheatingyouare.client.module.impl.NoParticles();
Module lowFire = new com.eclipseware.imnotcheatingyouare.client.module.impl.LowFire();
Module noTotemPop = new com.eclipseware.imnotcheatingyouare.client.module.impl.NoTotemPop();
Module tracers = new com.eclipseware.imnotcheatingyouare.client.module.impl.Tracers();
Module nametags = new com.eclipseware.imnotcheatingyouare.client.module.impl.Nametags();
Module silentAim = new com.eclipseware.imnotcheatingyouare.client.module.impl.SilentAim();
Module storageESP = new com.eclipseware.imnotcheatingyouare.client.module.impl.StorageESP();
Module blockESP = new com.eclipseware.imnotcheatingyouare.client.module.impl.BlockESP();
Module killAura = new com.eclipseware.imnotcheatingyouare.client.module.impl.KillAura();
Module detectionAlert = new com.eclipseware.imnotcheatingyouare.client.module.impl.DetectionAlert();
Module backtrack = new com.eclipseware.imnotcheatingyouare.client.module.impl.Backtrack();
Module pearlBind = new com.eclipseware.imnotcheatingyouare.client.module.impl.PearlBind();
Module autoTotem = new com.eclipseware.imnotcheatingyouare.client.module.impl.AutoTotem();
Module hitboxes = new com.eclipseware.imnotcheatingyouare.client.module.impl.Hitboxes();
Module anchorMacro = new com.eclipseware.imnotcheatingyouare.client.module.impl.AnchorMacro();
Module crystalAura = new com.eclipseware.imnotcheatingyouare.client.module.impl.CrystalAura();
Module antibot = new com.eclipseware.imnotcheatingyouare.client.module.impl.AntiBot();
Module teams = new com.eclipseware.imnotcheatingyouare.client.module.impl.Teams();
Module blink = new com.eclipseware.imnotcheatingyouare.client.module.impl.BlinkModule();


moduleManager.modules.add(autoSprint);
moduleManager.modules.add(noJumpDelay);
moduleManager.modules.add(aimAssist);
moduleManager.modules.add(wTap);
moduleManager.modules.add(triggerbot);
moduleManager.modules.add(autoWeb);
moduleManager.modules.add(shieldDrain);
moduleManager.modules.add(attributeSwap);
moduleManager.modules.add(autoMaceCounter);
moduleManager.modules.add(autoDrain);
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

moduleManager.modules.add(autoClicker);
moduleManager.modules.add(fastPlace);
moduleManager.modules.add(bridgeAssist);
moduleManager.modules.add(scaffold);
moduleManager.modules.add(xray);
moduleManager.modules.add(elytraBoost);
moduleManager.modules.add(configurator);

moduleManager.modules.add(fullbright);
moduleManager.modules.add(reach);
moduleManager.modules.add(handView);
moduleManager.modules.add(noParticles);
moduleManager.modules.add(lowFire);
moduleManager.modules.add(noTotemPop);
moduleManager.modules.add(detectionAlert);
moduleManager.modules.add(esp);
moduleManager.modules.add(tracers);
moduleManager.modules.add(nametags);
moduleManager.modules.add(silentAim);
moduleManager.modules.add(storageESP);
moduleManager.modules.add(blockESP);
moduleManager.modules.add(killAura);
moduleManager.modules.add(backtrack);
moduleManager.modules.add(pearlBind);
moduleManager.modules.add(autoTotem);
moduleManager.modules.add(hitboxes);
moduleManager.modules.add(anchorMacro);
moduleManager.modules.add(crystalAura);
Module hitSwap = new com.eclipseware.imnotcheatingyouare.client.module.impl.HitSwap();
moduleManager.modules.add(hitSwap);
moduleManager.modules.add(antibot);
moduleManager.modules.add(teams);
Module renderOptimizer = new com.eclipseware.imnotcheatingyouare.client.module.impl.RenderOptimizer();
moduleManager.modules.add(renderOptimizer);
Module automine = new com.eclipseware.imnotcheatingyouare.client.module.impl.Automine();
Module autowalk = new com.eclipseware.imnotcheatingyouare.client.module.impl.AutoWalk();
Module guimove = new com.eclipseware.imnotcheatingyouare.client.module.impl.GUIMove();
Module autosign = new com.eclipseware.imnotcheatingyouare.client.module.impl.AutoSign();
Module freecam = new com.eclipseware.imnotcheatingyouare.client.module.impl.Freecam();
Module friendProtector = new com.eclipseware.imnotcheatingyouare.client.module.impl.FriendProtector();
moduleManager.modules.add(automine);
moduleManager.modules.add(autowalk);
moduleManager.modules.add(guimove);
moduleManager.modules.add(autosign);
moduleManager.modules.add(freecam);
moduleManager.modules.add(friendProtector);
moduleManager.modules.add(new com.eclipseware.imnotcheatingyouare.client.module.impl.AntiTranslationKey());

moduleManager.modules.add(blink);


Module theme = new Module("Theme", Category.Render, "Customizes the client's UI colors and animations.");
        moduleManager.modules.add(theme);

        settingsManager.rSetting(new Setting("Accent R", theme, 155.0, 0.0, 255.0, true));
        settingsManager.rSetting(new Setting("Accent G", theme, 60.0, 0.0, 255.0, true));
        settingsManager.rSetting(new Setting("Accent B", theme, 255.0, 0.0, 255.0, true));
        settingsManager.rSetting(new Setting("Background Alpha", theme, 240.0, 0.0, 255.0, true));
        settingsManager.rSetting(new Setting("Anim Speed", theme, 5.0, 1.0, 10.0, false));

        java.util.ArrayList<String> aimModes = new java.util.ArrayList<>();
aimModes.add("Spring"); aimModes.add("Smooth"); aimModes.add("Linear");
settingsManager.rSetting(new Setting("Mode", aimAssist, "Spring", aimModes));
settingsManager.rSetting(new Setting("Smoothness", aimAssist, 7.0, 1.0, 10.0, false));
settingsManager.rSetting(new Setting("Speed", aimAssist, 3.0, 1.0, 10.0, false));
settingsManager.rSetting(new Setting("Randomness", aimAssist, 3.0, 0.0, 10.0, false));
settingsManager.rSetting(new Setting("Overflick", aimAssist, true));
settingsManager.rSetting(new Setting("Overflick Power", aimAssist, 3.0, 1.0, 10.0, false));
settingsManager.rSetting(new Setting("Reaction Delay (ms)", aimAssist, 50.0, 0.0, 300.0, true));
settingsManager.rSetting(new Setting("Attack Only", aimAssist, true));
settingsManager.rSetting(new Setting("Stop On Target", aimAssist, true));
settingsManager.rSetting(new Setting("FOV", aimAssist, 60.0, 0.0, 360.0, true));
settingsManager.rSetting(new Setting("Range", aimAssist, 4.0, 1.0, 8.0, false));
settingsManager.rSetting(new Setting("Silent Aim", aimAssist, false));

        java.util.ArrayList<String> acButtons = new java.util.ArrayList<>();
        acButtons.add("Left"); acButtons.add("Right");
        settingsManager.rSetting(new Setting("Button", autoClicker, "Left", acButtons));
        settingsManager.rSetting(new Setting("Min CPS", autoClicker, 9.0, 1.0, 20.0, true));
        settingsManager.rSetting(new Setting("Max CPS", autoClicker, 14.0, 1.0, 20.0, true));
        settingsManager.rSetting(new Setting("Require Click", autoClicker, true));

        settingsManager.rSetting(new Setting("Delay (Ticks)", fastPlace, 0.0, 0.0, 3.0, true));
        
        settingsManager.rSetting(new Setting("Edge Distance", bridgeAssist, 0.25, 0.00, 0.30, false));
settingsManager.rSetting(new Setting("Pitch Check", bridgeAssist, true));

settingsManager.rSetting(new Setting("Range", blockESP, 32.0, 8.0, 64.0, true));
settingsManager.rSetting(new Setting("FPS", blockESP, 30.0, 1.0, 60.0, true));
settingsManager.rSetting(new Setting("Tracers", blockESP, true));
settingsManager.rSetting(new Setting("Fill", blockESP, true));
settingsManager.rSetting(new Setting("Outline", blockESP, true));

settingsManager.rSetting(new Setting("Chest", storageESP, true));
settingsManager.rSetting(new Setting("Barrel", storageESP, true));
settingsManager.rSetting(new Setting("Shulker Box", storageESP, true));
settingsManager.rSetting(new Setting("Ender Chest", storageESP, true));
settingsManager.rSetting(new Setting("Trapped Chest", storageESP, true));
settingsManager.rSetting(new Setting("Hopper", storageESP, false));
settingsManager.rSetting(new Setting("Dispenser", storageESP, false));
settingsManager.rSetting(new Setting("Dropper", storageESP, false));
settingsManager.rSetting(new Setting("Furnaces", storageESP, false));
settingsManager.rSetting(new Setting("Range", storageESP, 64.0, 8.0, 128.0, true));
settingsManager.rSetting(new Setting("FPS", storageESP, 30.0, 1.0, 60.0, true));
settingsManager.rSetting(new Setting("Tracers", storageESP, true));
settingsManager.rSetting(new Setting("Fill", storageESP, true));
settingsManager.rSetting(new Setting("Outline", storageESP, true));

addColorSettings(settingsManager, storageESP, "Chest Color", 255, 165, 0);
addColorSettings(settingsManager, storageESP, "Barrel Color", 139, 90, 43);
addColorSettings(settingsManager, storageESP, "Shulker Color", 130, 90, 130);
addColorSettings(settingsManager, storageESP, "Ender Chest Color", 138, 43, 226);
addColorSettings(settingsManager, storageESP, "Trapped Chest Color", 255, 0, 0);
addColorSettings(settingsManager, storageESP, "Hopper Color", 100, 100, 100);
addColorSettings(settingsManager, storageESP, "Dispenser Color", 128, 128, 128);
addColorSettings(settingsManager, storageESP, "Dropper Color", 169, 169, 169);
addColorSettings(settingsManager, storageESP, "Furnace Color", 160, 160, 160);

        settingsManager.rSetting(new Setting("Firework Level", elytraBoost, 1.0, 0.0, 3.0, true));
        settingsManager.rSetting(new Setting("Play Sound", elytraBoost, true));
        settingsManager.rSetting(new Setting("Players", aimAssist, true));
        settingsManager.rSetting(new Setting("Hostile Mobs", aimAssist, true));
        settingsManager.rSetting(new Setting("Passive Mobs", aimAssist, false));

        settingsManager.rSetting(new Setting("Wait Ticks", wTap, 0.0, 0.0, 10.0, true));
        settingsManager.rSetting(new Setting("Action Ticks", wTap, 1.0, 1.0, 5.0, true));

        java.util.ArrayList<String> tbClickStyles = new java.util.ArrayList<>();
        tbClickStyles.add("Virtual"); tbClickStyles.add("Direct");
        settingsManager.rSetting(new Setting("Click Style", triggerbot, "Virtual", tbClickStyles));
        settingsManager.rSetting(new Setting("Range", triggerbot, 4.25, 1.0, 6.0, false));
        settingsManager.rSetting(new Setting("Min Delay (Ticks)", triggerbot, 1.0, 0.0, 20.0, true));
        settingsManager.rSetting(new Setting("Max Delay (Ticks)", triggerbot, 4.0, 0.0, 20.0, true));
        settingsManager.rSetting(new Setting("Inventory Fix", triggerbot, true));
        settingsManager.rSetting(new Setting("Require Click", triggerbot, false));
        settingsManager.rSetting(new Setting("Weapons Only", triggerbot, true));
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
        jrModes.add("Smart"); jrModes.add("Classic"); jrModes.add("Blatant");
        settingsManager.rSetting(new Setting("Mode", jumpReset, "Smart", jrModes));
        settingsManager.rSetting(new Setting("Reset Hit", jumpReset, 2.0, 1.0, 10.0, true));
        settingsManager.rSetting(new Setting("Max Trade Length", jumpReset, 4.0, 1.0, 20.0, true));
        settingsManager.rSetting(new Setting("Short Trade Reset", jumpReset, true));
        settingsManager.rSetting(new Setting("Trade Timeout (ms)", jumpReset, 500.0, 100.0, 2000.0, true));
        settingsManager.rSetting(new Setting("Delay (Ticks)", jumpReset, 0.0, 0.0, 5.0, true));
        settingsManager.rSetting(new Setting("Chance (%)", jumpReset, 100.0, 0.0, 100.0, true));
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
        espModes.add("Outline"); espModes.add("2D"); espModes.add("Hybrid"); espModes.add("Glow");
        settingsManager.rSetting(new Setting("Mode", esp, "Outline", espModes));
        settingsManager.rSetting(new Setting("Show Mobs", esp, false));
        settingsManager.rSetting(new Setting("Fill", esp, true));
        settingsManager.rSetting(new Setting("Health", esp, true));
        settingsManager.rSetting(new Setting("Names", esp, true));
        settingsManager.rSetting(new Setting("Outline Thickness", esp, 1.0, 1.0, 5.0, true));
        settingsManager.rSetting(new Setting("Corner Gap", esp, 50.0, 10.0, 100.0, true));
        settingsManager.rSetting(new Setting("Border", esp, true));

        settingsManager.rSetting(new Setting("Range", backtrack, 3.0, 1.0, 6.0, false));
        settingsManager.rSetting(new Setting("Delay", backtrack, 150.0, 50.0, 500.0, true));
        settingsManager.rSetting(new Setting("Chance", backtrack, 50.0, 0.0, 100.0, true));
        settingsManager.rSetting(new Setting("Attack Timeout", backtrack, 1000.0, 100.0, 5000.0, true));
        settingsManager.rSetting(new Setting("Visualizer", backtrack, true));

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

settingsManager.rSetting(new Setting("Show Mobs", tracers, false));

    settingsManager.rSetting(new Setting("Players", nametags, true));
settingsManager.rSetting(new Setting("Show Mobs", nametags, false));

settingsManager.rSetting(new Setting("Range", silentAim, 4.5, 1.0, 8.0, false));
settingsManager.rSetting(new Setting("FOV", silentAim, 120.0, 10.0, 360.0, true));
settingsManager.rSetting(new Setting("Players", silentAim, true));
settingsManager.rSetting(new Setting("Hostile Mobs", silentAim, true));
settingsManager.rSetting(new Setting("Passive Mobs", silentAim, false));

settingsManager.rSetting(new Setting("Chest", storageESP, true));
settingsManager.rSetting(new Setting("Barrel", storageESP, true));
settingsManager.rSetting(new Setting("Shulker Box", storageESP, true));
settingsManager.rSetting(new Setting("Ender Chest", storageESP, true));
settingsManager.rSetting(new Setting("Trapped Chest", storageESP, true));
settingsManager.rSetting(new Setting("Hopper", storageESP, false));
settingsManager.rSetting(new Setting("Dispenser", storageESP, false));
settingsManager.rSetting(new Setting("Dropper", storageESP, false));
settingsManager.rSetting(new Setting("Furnaces", storageESP, false));
settingsManager.rSetting(new Setting("Range", storageESP, 64.0, 8.0, 128.0, true));
settingsManager.rSetting(new Setting("FPS", storageESP, 30.0, 1.0, 60.0, true));
settingsManager.rSetting(new Setting("Tracers", storageESP, true));
settingsManager.rSetting(new Setting("Fill", storageESP, true));
settingsManager.rSetting(new Setting("Outline", storageESP, true));

addColorSettings(settingsManager, storageESP, "Chest Color", 255, 165, 0);
addColorSettings(settingsManager, storageESP, "Barrel Color", 139, 90, 43);
addColorSettings(settingsManager, storageESP, "Shulker Color", 130, 90, 130);
addColorSettings(settingsManager, storageESP, "Ender Chest Color", 138, 43, 226);
addColorSettings(settingsManager, storageESP, "Trapped Chest Color", 255, 0, 0);
addColorSettings(settingsManager, storageESP, "Hopper Color", 100, 100, 100);
addColorSettings(settingsManager, storageESP, "Dispenser Color", 128, 128, 128);
addColorSettings(settingsManager, storageESP, "Dropper Color", 169, 169, 169);
addColorSettings(settingsManager, storageESP, "Furnace Color", 160, 160, 160);

settingsManager.rSetting(new Setting("Range", blockESP, 32.0, 8.0, 64.0, true));
settingsManager.rSetting(new Setting("FPS", blockESP, 30.0, 1.0, 60.0, true));
settingsManager.rSetting(new Setting("Tracers", blockESP, true));
settingsManager.rSetting(new Setting("Fill", blockESP, true));
settingsManager.rSetting(new Setting("Outline", blockESP, true));


        Module menu = new com.eclipseware.imnotcheatingyouare.client.module.impl.Menu();
        moduleManager.modules.add(menu);

        addColorSettings(settingsManager, menu, "Primary", 155, 60, 255);
        addColorSettings(settingsManager, menu, "Secondary", 20, 20, 20);

        net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback.EVENT.register((guiGraphics, tickDelta) -> {
            com.eclipseware.imnotcheatingyouare.client.ui.ArrayListHud.INSTANCE.render(guiGraphics, tickDelta.getGameTimeDeltaTicks());
        });

        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            for (Module m : moduleManager.modules) {
                m.tickKeybind();
                if (m.isToggled()) {
                    m.onTick();
                }
            }

            com.eclipseware.imnotcheatingyouare.client.utils.RotationManager.tick();
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            com.eclipseware.imnotcheatingyouare.client.utils.RotationManager.visualTick();
        });

        com.eclipseware.imnotcheatingyouare.client.setting.ConfigManager.load();

        com.eclipseware.imnotcheatingyouare.client.utils.FriendManager.load();

        Thread saveHook = new Thread(com.eclipseware.imnotcheatingyouare.client.setting.ConfigManager::save, "ConfigSaveHook");
        saveHook.setDaemon(true);
        Runtime.getRuntime().addShutdownHook(saveHook);

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