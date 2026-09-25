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

import net.minecraft.client.KeyMapping;
import net.minecraft.client.multiplayer.ServerData;

public class ImnotcheatingyouareClient implements ClientModInitializer {
    public static ImnotcheatingyouareClient INSTANCE;

    public ModuleManager moduleManager;
    public SettingsManager settingsManager;
    public Clickgui clickGui;

    private KeyMapping guiBind;

    private void addColorSettings(SettingsManager sm, Module module, String colorName, int r, int g, int b) {
        sm.rSetting(new Setting(colorName, module, new java.awt.Color(r, g, b)));
    }

    @Override
    public void onInitializeClient() {
        Module.mc = net.minecraft.client.Minecraft.getInstance();
        INSTANCE = this;
        new com.eclipseware.imnotcheatingyouare.client.utils.remnant.Fonts();
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
Module webStun = new com.eclipseware.imnotcheatingyouare.client.module.impl.WebStun();
Module autoMace = new com.eclipseware.imnotcheatingyouare.client.module.impl.AutoMace();
Module elytraSwapMacro = new com.eclipseware.imnotcheatingyouare.client.module.impl.ElytraSwapMacro();
Module elytraBounce = new com.eclipseware.imnotcheatingyouare.client.module.impl.ElytraBounce();
Module pearlCatch = new com.eclipseware.imnotcheatingyouare.client.module.impl.PearlCatch();
Module jumpReset = new com.eclipseware.imnotcheatingyouare.client.module.impl.JumpReset();
Module pearlGrapple = new com.eclipseware.imnotcheatingyouare.client.module.impl.PearlGrapple();

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
Module anchorMacro = new com.eclipseware.imnotcheatingyouare.client.module.impl.AnchorMacro();
Module crystalAura = new com.eclipseware.imnotcheatingyouare.client.module.impl.CrystalAura();
Module crystalHelper = new com.eclipseware.imnotcheatingyouare.client.module.impl.CrystalHelper();
Module antibot = new com.eclipseware.imnotcheatingyouare.client.module.impl.AntiBot();
Module teams = new com.eclipseware.imnotcheatingyouare.client.module.impl.Teams();
Module blink = new com.eclipseware.imnotcheatingyouare.client.module.impl.BlinkModule();
Module packetAuditor = new com.eclipseware.imnotcheatingyouare.client.module.impl.PacketAuditor();
Module autoHitCrystal = new com.eclipseware.imnotcheatingyouare.client.module.impl.AutoHitCrystal();
Module autoPlaceCrystal = new com.eclipseware.imnotcheatingyouare.client.module.impl.AutoPlaceCrystal();
Module fastThrowables = new com.eclipseware.imnotcheatingyouare.client.module.impl.FastThrowables();
Module sTap = new com.eclipseware.imnotcheatingyouare.client.module.impl.STap();
Module chams = new com.eclipseware.imnotcheatingyouare.client.module.impl.Chams();
Module trajectories = new com.eclipseware.imnotcheatingyouare.client.module.impl.Trajectories();
Module surround = new com.eclipseware.imnotcheatingyouare.client.module.impl.Surround();


moduleManager.modules.add(autoSprint);
moduleManager.modules.add(noJumpDelay);
moduleManager.modules.add(aimAssist);
moduleManager.modules.add(wTap);
moduleManager.modules.add(sTap);
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
moduleManager.modules.add(webStun);
moduleManager.modules.add(autoMace);
moduleManager.modules.add(elytraSwapMacro);
moduleManager.modules.add(elytraBounce);
moduleManager.modules.add(pearlCatch);
moduleManager.modules.add(jumpReset);
moduleManager.modules.add(pearlGrapple);

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
moduleManager.modules.add(chams);
moduleManager.modules.add(tracers);
moduleManager.modules.add(nametags);
moduleManager.modules.add(silentAim);
moduleManager.modules.add(storageESP);
moduleManager.modules.add(blockESP);
moduleManager.modules.add(killAura);
moduleManager.modules.add(backtrack);
moduleManager.modules.add(pearlBind);
moduleManager.modules.add(autoTotem);
moduleManager.modules.add(anchorMacro);
        moduleManager.modules.add(crystalAura);
        moduleManager.modules.add(crystalHelper);
        moduleManager.modules.add(autoHitCrystal);
        moduleManager.modules.add(autoPlaceCrystal);
        moduleManager.modules.add(fastThrowables);
        moduleManager.modules.add(blink);
        moduleManager.modules.add(packetAuditor);
        moduleManager.modules.add(trajectories);
        moduleManager.modules.add(surround);
        Module hitSwap = new com.eclipseware.imnotcheatingyouare.client.module.impl.HitSwap();
        moduleManager.modules.add(hitSwap);
        moduleManager.modules.add(antibot);
        moduleManager.modules.add(teams);
        Module renderOptimizer = new com.eclipseware.imnotcheatingyouare.client.module.impl.RenderOptimizer();
        moduleManager.modules.add(renderOptimizer);
        Module automine = new com.eclipseware.imnotcheatingyouare.client.module.impl.Automine();
        Module autowalk = new com.eclipseware.imnotcheatingyouare.client.module.impl.AutoWalk();
        Module antiAfk = new com.eclipseware.imnotcheatingyouare.client.module.impl.AntiAFK();
        Module guimove = new com.eclipseware.imnotcheatingyouare.client.module.impl.GUIMove();
        Module autosign = new com.eclipseware.imnotcheatingyouare.client.module.impl.AutoSign();
        Module freecam = new com.eclipseware.imnotcheatingyouare.client.module.impl.Freecam();
        Module friendProtector = new com.eclipseware.imnotcheatingyouare.client.module.impl.FriendProtector();
        moduleManager.modules.add(automine);
        moduleManager.modules.add(autowalk);
        moduleManager.modules.add(antiAfk);
        moduleManager.modules.add(guimove);
        moduleManager.modules.add(autosign);
        moduleManager.modules.add(freecam);
        moduleManager.modules.add(friendProtector);
        moduleManager.modules.add(new com.eclipseware.imnotcheatingyouare.client.module.impl.AntiTranslationKey());

        Module autoDHand = new com.eclipseware.imnotcheatingyouare.client.module.impl.AutoDHand();
        Module autoWindcharge = new com.eclipseware.imnotcheatingyouare.client.module.impl.AutoWindcharge();
        Module boatFly = new com.eclipseware.imnotcheatingyouare.client.module.impl.BoatFly();
        Module flight = new com.eclipseware.imnotcheatingyouare.client.module.impl.Flight();
        Module weapons = new com.eclipseware.imnotcheatingyouare.client.module.impl.Weapons();
        Module bypassModule = new com.eclipseware.imnotcheatingyouare.client.module.impl.Bypass();
        Module npcModule = new com.eclipseware.imnotcheatingyouare.client.module.impl.NPC();
        Module weakDevice = new com.eclipseware.imnotcheatingyouare.client.module.impl.WeakDevice();
        moduleManager.modules.add(weakDevice);

        Module autoMLG = new com.eclipseware.imnotcheatingyouare.client.module.impl.AutoMLG();
        moduleManager.modules.add(autoMLG);
        moduleManager.modules.add(autoDHand);
        moduleManager.modules.add(autoWindcharge);
        moduleManager.modules.add(boatFly);
        moduleManager.modules.add(flight);
        moduleManager.modules.add(weapons);
        moduleManager.modules.add(bypassModule);
        moduleManager.modules.add(npcModule);
        Module recommendedConfigs = new com.eclipseware.imnotcheatingyouare.client.module.impl.RecommendedConfigs();
        moduleManager.modules.add(recommendedConfigs);

Module keybindWater = new com.eclipseware.imnotcheatingyouare.client.module.impl.KeybindWater();
Module noFall = new com.eclipseware.imnotcheatingyouare.client.module.impl.NoFall();
moduleManager.modules.add(noFall);
Module keybindLava = new com.eclipseware.imnotcheatingyouare.client.module.impl.KeybindLava();
Module keybindWeb = new com.eclipseware.imnotcheatingyouare.client.module.impl.KeybindWeb();
Module vapeMacro = new com.eclipseware.imnotcheatingyouare.client.module.impl.VapeMacro();
Module keybindList = new com.eclipseware.imnotcheatingyouare.client.module.impl.KeybindList();

moduleManager.modules.add(keybindWater);
moduleManager.modules.add(keybindLava);
moduleManager.modules.add(keybindWeb);
moduleManager.modules.add(vapeMacro);
moduleManager.modules.add(keybindList);

settingsManager.rSetting(new Setting("Scale", keybindList, 0.75, 0.3, 1.5, false));
settingsManager.rSetting(new Setting("Only Enabled", keybindList, false));

Module loot = new com.eclipseware.imnotcheatingyouare.client.module.impl.Loot();
moduleManager.modules.add(loot);

Module instaCart = new com.eclipseware.imnotcheatingyouare.client.module.impl.InstaCart();
Module cartRefill = new com.eclipseware.imnotcheatingyouare.client.module.impl.CartRefill();
Module xbowCart = new com.eclipseware.imnotcheatingyouare.client.module.impl.XbowCart();
moduleManager.modules.add(instaCart);
moduleManager.modules.add(cartRefill);
moduleManager.modules.add(xbowCart);

Module targetHUD = new com.eclipseware.imnotcheatingyouare.client.module.impl.TargetHUD();
Module armorHUD = new com.eclipseware.imnotcheatingyouare.client.module.impl.ArmorHUD();
Module hudEditor = new com.eclipseware.imnotcheatingyouare.client.module.impl.HUDEditor();
Module ghostBlockMacro = new com.eclipseware.imnotcheatingyouare.client.module.impl.GhostBlockMacro();
moduleManager.modules.add(targetHUD);
moduleManager.modules.add(armorHUD);
moduleManager.modules.add(hudEditor);
moduleManager.modules.add(ghostBlockMacro);
        moduleManager.modules.add(new com.eclipseware.imnotcheatingyouare.client.module.impl.AutoFish());
        moduleManager.modules.add(new com.eclipseware.imnotcheatingyouare.client.module.impl.InventoryClean());
        moduleManager.modules.add(new com.eclipseware.imnotcheatingyouare.client.module.impl.InventoryFill());


        Module clientSpoof = new com.eclipseware.imnotcheatingyouare.client.module.impl.ClientSpoof();
        moduleManager.modules.add(clientSpoof);
        java.util.ArrayList<String> csModes = new java.util.ArrayList<>();
        csModes.add("Lunar"); csModes.add("Vanilla"); csModes.add("Fabric"); csModes.add("Custom");
        settingsManager.rSetting(new Setting("Mode", clientSpoof, "Lunar", csModes));
        settingsManager.rSetting(new Setting("Custom Brand", clientSpoof, "lunarclient:v2.12.0-2629", true));

        Module theme = new Module("Theme", Category.Render, "Customizes the client's UI colors and animations.");
        moduleManager.modules.add(theme);
        settingsManager.rSetting(new Setting("Accent Color", theme, new java.awt.Color(239, 142, 172)));
        settingsManager.rSetting(new Setting("Background Alpha", theme, 240.0, 0.0, 255.0, true));
        settingsManager.rSetting(new Setting("Anim Speed", theme, 5.0, 1.0, 10.0, false));

        settingsManager.rSetting(new Setting("Target Item", vapeMacro, "pearl", true));



        java.util.ArrayList<String> acButtons = new java.util.ArrayList<>();
        acButtons.add("Left"); acButtons.add("Right");
        settingsManager.rSetting(new Setting("Button", autoClicker, "Left", acButtons));
        settingsManager.rSetting(new Setting("Min CPS", autoClicker, 9.0, 1.0, 20.0, true));
        settingsManager.rSetting(new Setting("Max CPS", autoClicker, 14.0, 1.0, 20.0, true));
        settingsManager.rSetting(new Setting("Require Click", autoClicker, true));
        settingsManager.rSetting(new Setting("Right Clicker", autoClicker, false));

        settingsManager.rSetting(new Setting("Delay (Ticks)", fastPlace, 0.0, 0.0, 3.0, true));
        
        java.util.ArrayList<String> safewalkModes = new java.util.ArrayList<>();
        safewalkModes.add("Normal");
        safewalkModes.add("Blatant");
        settingsManager.rSetting(new Setting("Mode", bridgeAssist, "Normal", safewalkModes));
        settingsManager.rSetting(new Setting("Edge Distance", bridgeAssist, 0.25, 0.00, 0.30, false));
        settingsManager.rSetting(new Setting("Pitch Check", bridgeAssist, true));



        settingsManager.rSetting(new Setting("Firework Level", elytraBoost, 1.0, 0.0, 3.0, true));
        settingsManager.rSetting(new Setting("Play Sound", elytraBoost, true));

        java.util.ArrayList<String> fbModes = new java.util.ArrayList<>();
        fbModes.add("Night Vision");
        fbModes.add("Gamma");
        settingsManager.rSetting(new Setting("Mode", fullbright, "Night Vision", fbModes));


        settingsManager.rSetting(new Setting("Wait Delay (ms)", wTap, 0.0, 0.0, 500.0, true));
        settingsManager.rSetting(new Setting("Action Delay (ms)", wTap, 100.0, 10.0, 500.0, true));

        java.util.ArrayList<String> wtapModes = new java.util.ArrayList<>();
        wtapModes.add("Auto");
        wtapModes.add("Silent");
        wtapModes.add("Normal");
        settingsManager.rSetting(new Setting("WTap Mode", wTap, "Auto", wtapModes));
        settingsManager.rSetting(new Setting("Chance (%)", wTap, 100.0, 0.0, 100.0, true));
        settingsManager.rSetting(new Setting("Only Players", wTap, true));
        settingsManager.rSetting(new Setting("Jitter (ms)", wTap, 20.0, 0.0, 200.0, true));

        settingsManager.rSetting(new Setting("Range", triggerbot, 4.25, 1.0, 6.0, false));
        settingsManager.rSetting(new Setting("Min Delay (ms)", triggerbot, 50.0, 0.0, 500.0, true));
        settingsManager.rSetting(new Setting("Max Delay (ms)", triggerbot, 150.0, 0.0, 500.0, true));
        settingsManager.rSetting(new Setting("Weapons Only", triggerbot, true));
        settingsManager.rSetting(new Setting("Players", triggerbot, true));
        settingsManager.rSetting(new Setting("Hostile Mobs", triggerbot, true));
        settingsManager.rSetting(new Setting("Passive Mobs", triggerbot, false));

        java.util.ArrayList<String> hsModes = new java.util.ArrayList<>();
        hsModes.add("Pause"); hsModes.add("Dynamic");
        settingsManager.rSetting(new Setting("Mode", hitSelect, "Pause", hsModes));
        settingsManager.rSetting(new Setting("Auto Punish", hitSelect, false));
        settingsManager.rSetting(new Setting("Punish Delay (Ticks)", hitSelect, 3.0, 0.0, 10.0, true));

        java.util.ArrayList<String> asbModes = new java.util.ArrayList<>();
        asbModes.add("Swap"); asbModes.add("Silent");
        settingsManager.rSetting(new Setting("Mode", autoShieldBreaker, "Swap", asbModes));
        settingsManager.rSetting(new Setting("Delay (ms)", autoShieldBreaker, 50.0, 0.0, 1000.0, true));
        settingsManager.rSetting(new Setting("Swap Back", autoShieldBreaker, true));
        settingsManager.rSetting(new Setting("Swap Back Delay (ms)", autoShieldBreaker, 100.0, 0.0, 1000.0, true));
        settingsManager.rSetting(new Setting("Reaction Time (ms)", autoShieldBreaker, 100.0, 0.0, 1000.0, true));

        settingsManager.rSetting(new Setting("Delay (ms)", autoWeb, 250.0, 0.0, 1000.0, true));
        settingsManager.rSetting(new Setting("Movement Correction", autoWeb, true));
        settingsManager.rSetting(new Setting("Smooth Rotation", autoWeb, 2.0, 1.0, 10.0, false));
        settingsManager.rSetting(new Setting("Swap Back", autoWeb, true));

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
        settingsManager.rSetting(new Setting("Text Color", arrayListMod, new java.awt.Color(230, 10, 230)));
        settingsManager.rSetting(new Setting("Y Offset", arrayListMod, 5.0, 0.0, 100.0, true));
        settingsManager.rSetting(new Setting("Scale", arrayListMod, 1.0, 0.5, 1.5, false));

        java.util.ArrayList<String> npNames = new java.util.ArrayList<>();
        npNames.add("Marlowww"); npNames.add("Hidden"); npNames.add("You");
        settingsManager.rSetting(new Setting("Name", nameProtect, "Marlowww", npNames));
        java.util.ArrayList<String> npModes = new java.util.ArrayList<>();
        npModes.add("Self"); npModes.add("Everyone");
        settingsManager.rSetting(new Setting("Mode", nameProtect, "Self", npModes));
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

        java.util.ArrayList<String> afkActions = new java.util.ArrayList<>();
        afkActions.add("Cycle");
        afkActions.add("Jump");
        afkActions.add("Swing");
        afkActions.add("Sneak");
        settingsManager.rSetting(new Setting("Action", antiAfk, "Cycle", afkActions));
        settingsManager.rSetting(new Setting("Interval (s)", antiAfk, 5.0, 1.0, 300.0, true));
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
        espModes.add("Outline"); espModes.add("2D"); espModes.add("Hybrid"); espModes.add("Glow"); espModes.add("Shader");
        settingsManager.rSetting(new Setting("Mode", esp, "Outline", espModes));
        settingsManager.rSetting(new Setting("Show Mobs", esp, false));
        settingsManager.rSetting(new Setting("Fill", esp, true));
        settingsManager.rSetting(new Setting("Health", esp, true));
        settingsManager.rSetting(new Setting("Names", esp, true));
        settingsManager.rSetting(new Setting("Outline Thickness", esp, 1.0, 1.0, 5.0, true));
        settingsManager.rSetting(new Setting("Corner Gap", esp, 50.0, 10.0, 100.0, true));
        settingsManager.rSetting(new Setting("Border", esp, true));
        settingsManager.rSetting(new Setting("Feather", esp, 3.0, 1.0, 5.0, true));
        settingsManager.rSetting(new Setting("Fill Opacity", esp, 20.0, 0.0, 50.0, true));
        java.util.ArrayList<String> espColorModes = new java.util.ArrayList<>();
        espColorModes.add("Theme"); espColorModes.add("Custom"); espColorModes.add("Per Type");
        settingsManager.rSetting(new Setting("Color Mode", esp, "Theme", espColorModes));
        addColorSettings(settingsManager, esp, "Custom Color", 190, 150, 255);
        addColorSettings(settingsManager, esp, "Player Color", 190, 150, 255);
        addColorSettings(settingsManager, esp, "Hostile Color", 255, 95, 95);
        addColorSettings(settingsManager, esp, "Passive Color", 120, 230, 150);
        addColorSettings(settingsManager, esp, "Neutral Color", 255, 205, 90);
        settingsManager.rSetting(new Setting("Name Tags", esp, true));
        settingsManager.rSetting(new Setting("Tag Distance", esp, true));
        settingsManager.rSetting(new Setting("Tag Health", esp, true));
        settingsManager.rSetting(new Setting("Tag Item", esp, true));

        settingsManager.rSetting(new Setting("Range", backtrack, 3.0, 1.0, 6.0, false));
        settingsManager.rSetting(new Setting("Delay", backtrack, 150.0, 50.0, 500.0, true));
        settingsManager.rSetting(new Setting("Chance", backtrack, 50.0, 0.0, 100.0, true));
        settingsManager.rSetting(new Setting("Attack Timeout", backtrack, 1000.0, 100.0, 5000.0, true));
        settingsManager.rSetting(new Setting("Visualizer", backtrack, false));

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
settingsManager.rSetting(new Setting("FPS", storageESP, 144.0, 1.0, 1000.0, true));
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
settingsManager.rSetting(new Setting("FPS", blockESP, 144.0, 1.0, 1000.0, true));
settingsManager.rSetting(new Setting("Tracers", blockESP, true));
settingsManager.rSetting(new Setting("Fill", blockESP, true));
settingsManager.rSetting(new Setting("Outline", blockESP, true));

        java.util.ArrayList<String> combatSystems = new java.util.ArrayList<>();
        combatSystems.add("Modern (1.9+)");
        combatSystems.add("1.8.9");
        settingsManager.rSetting(new Setting("Combat System", killAura, "Modern (1.9+)", combatSystems));
        java.util.ArrayList<String> rotModes = new java.util.ArrayList<>();
        rotModes.add("Smooth");
        rotModes.add("Bypass");
        settingsManager.rSetting(new Setting("Rotation Mode", killAura, "Smooth", rotModes));
        settingsManager.rSetting(new Setting("Range", killAura, 4.0, 1.0, 6.0, false));
        settingsManager.rSetting(new Setting("Criticals Only", killAura, false));
        settingsManager.rSetting(new Setting("Turn Speed", killAura, 30.0, 1.0, 180.0, false));
        settingsManager.rSetting(new Setting("Movement Correction", killAura, true));
        settingsManager.rSetting(new Setting("Target Players", killAura, true));
        settingsManager.rSetting(new Setting("Target Mobs", killAura, false));
        settingsManager.rSetting(new Setting("Target Animals", killAura, false));
        settingsManager.rSetting(new Setting("Modern Delay (Ticks)", killAura, 0.0, 0.0, 5.0, true));
        settingsManager.rSetting(new Setting("Silent", killAura, true));
        settingsManager.rSetting(new Setting("1.8.9 Min CPS", killAura, 8.0, 1.0, 20.0, true));
        settingsManager.rSetting(new Setting("1.8.9 Max CPS", killAura, 12.0, 1.0, 20.0, true));

        moduleManager.modules.add(new com.eclipseware.imnotcheatingyouare.client.module.impl.SelfShader());

        Module menu = new com.eclipseware.imnotcheatingyouare.client.module.impl.Menu();
        moduleManager.modules.add(menu);

        Module guiModule = new Module("GUI", Category.Client, "Layout, size and look of the click GUI.");
        moduleManager.modules.add(guiModule);
        java.util.ArrayList<String> guiLayouts = new java.util.ArrayList<>();
        guiLayouts.add("Compact"); guiLayouts.add("Vertical");
        settingsManager.rSetting(new Setting("Layout", guiModule, "Compact", guiLayouts));
        java.util.ArrayList<String> guiScaleModes = new java.util.ArrayList<>();
        guiScaleModes.add("Auto"); guiScaleModes.add("Custom");
        settingsManager.rSetting(new Setting("Scale Mode", guiModule, "Auto", guiScaleModes));
        settingsManager.rSetting(new Setting("Scale", guiModule, 1.0, 0.5, 3.0, false));
        settingsManager.rSetting(new Setting("Glass Opacity", guiModule, 70.0, 20.0, 100.0, true));
        settingsManager.rSetting(new Setting("Glow", guiModule, true));

        addColorSettings(settingsManager, menu, "Primary", 239, 142, 172);
        addColorSettings(settingsManager, menu, "Secondary", 24, 24, 37);


        settingsManager.rSetting(new Setting("Use Verdana Font", menu, true));

        addColorSettings(settingsManager, menu, "Background", 30, 30, 46);
        settingsManager.rSetting(new Setting("Background Alpha", menu, 240.0, 0.0, 255.0, true));

        settingsManager.rSetting(new Setting("Corner Radius", menu, 12.0, 0.0, 30.0, true));
        settingsManager.rSetting(new Setting("Animation Speed", menu, 5.0, 1.0, 10.0, false));
        settingsManager.rSetting(new Setting("Hover Glow", menu, 35.0, 0.0, 100.0, true));

        Module legacyUI = new com.eclipseware.imnotcheatingyouare.client.module.impl.LegacyUI();
        moduleManager.modules.add(legacyUI);
        settingsManager.rSetting(new Setting("On Crystal", crystalHelper, true));
        settingsManager.rSetting(new Setting("On Obsidian", crystalHelper, true));
        settingsManager.rSetting(new Setting("Exclude Bedrock", crystalHelper, false));
        settingsManager.rSetting(new Setting("Only Selected", crystalHelper, true));
        settingsManager.rSetting(new Setting("On Any", crystalHelper, false));
        settingsManager.rSetting(new Setting("On Empty", crystalHelper, true));
        settingsManager.rSetting(new Setting("On Sword", crystalHelper, true));
        settingsManager.rSetting(new Setting("On Crystal Item", crystalHelper, true));
        settingsManager.rSetting(new Setting("On Obsidian Item", crystalHelper, true));
        settingsManager.rSetting(new Setting("On Totem", crystalHelper, true));
        settingsManager.rSetting(new Setting("On Glowstone", crystalHelper, true));
        settingsManager.rSetting(new Setting("On Anchor", crystalHelper, true));
        settingsManager.rSetting(new Setting("Hold Trigger", crystalHelper, false));
        settingsManager.rSetting(new Setting("Cooldown (ms)", crystalHelper, 200.0, 0.0, 1000.0, true));


        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            com.eclipseware.imnotcheatingyouare.client.utils.ModuleUtils.onClientTickStart();
            com.eclipseware.imnotcheatingyouare.client.utils.SpoofManager.onTick();
            com.eclipseware.imnotcheatingyouare.client.utils.ModuleUtils.tickSilentRevert();
            com.eclipseware.imnotcheatingyouare.client.clickgui.ImGuiClickGui.tick();

            for (Module m : moduleManager.modules) {
                m.tickKeybind();
                if (m.isToggled()) {
                    m.onTick();
                }
            }

            com.eclipseware.imnotcheatingyouare.client.utils.RotationManager.tick();
            com.eclipseware.imnotcheatingyouare.client.macro.MacroManager.tick();
            com.eclipseware.imnotcheatingyouare.client.macro.MacroManager.tickKeybinds();
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            com.eclipseware.imnotcheatingyouare.client.utils.RotationManager.visualTick();
            com.eclipseware.imnotcheatingyouare.client.utils.ModuleUtils.onClientTickEnd();
        });

        com.eclipseware.imnotcheatingyouare.client.setting.ConfigManager.load();
        com.eclipseware.imnotcheatingyouare.client.macro.MacroManager.load();

        com.eclipseware.imnotcheatingyouare.client.utils.FriendManager.load();
        com.eclipseware.imnotcheatingyouare.client.utils.TargetFilterManager.load();

        Thread saveHook = new Thread(com.eclipseware.imnotcheatingyouare.client.setting.ConfigManager::save, "ConfigSaveHook");
        saveHook.setDaemon(true);
        Runtime.getRuntime().addShutdownHook(saveHook);

        net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal("config")
                .then(net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal("gui")
                    .executes(context -> {
                        net.minecraft.client.Minecraft.getInstance().execute(() ->
                            net.minecraft.client.Minecraft.getInstance().setScreenAndShow(new com.eclipseware.imnotcheatingyouare.client.clickgui.ConfigGui())
                        );
                        return 1;
                    })
                )
                .then(net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal("export")
                    .executes(context -> {
                        String exp = com.eclipseware.imnotcheatingyouare.client.setting.ConfigManager.exportSpecific(moduleManager.modules);
                        net.minecraft.client.Minecraft.getInstance().keyboardHandler.setClipboard(exp);
                        context.getSource().sendFeedback(net.minecraft.network.chat.Component.literal("§d[EclipseWare] §7Config exported to clipboard!"));
                        return 1;
                    })
                )
            );
            dispatcher.register(net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal("mc")
                .then(net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal("macro")
                    .then(net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal("export")
                        .executes(context -> {
                            com.eclipseware.imnotcheatingyouare.client.macro.MacroManager.exportToClipboard();
                            context.getSource().sendFeedback(net.minecraft.network.chat.Component.literal("§d[EclipseWare] §7Macro exported to clipboard!"));
                            return 1;
                        })
                    )
                    .then(net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal("import")
                        .executes(context -> {
                            com.eclipseware.imnotcheatingyouare.client.macro.MacroManager.importFromClipboard();
                            context.getSource().sendFeedback(net.minecraft.network.chat.Component.literal("§d[EclipseWare] §7Macro imported from clipboard!"));
                            return 1;
                        })
                    )
                )
                .then(net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal("enable")
                    .then(net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument("module", com.mojang.brigadier.arguments.StringArgumentType.word())
                        .executes(context -> {
                            String modName = com.mojang.brigadier.arguments.StringArgumentType.getString(context, "module");
                            Module mod = moduleManager.getModule(modName);
                            if (mod != null) {
                                if (!mod.isToggled()) mod.toggle();
                                context.getSource().sendFeedback(net.minecraft.network.chat.Component.literal("§d[EclipseWare] §aEnabled " + mod.getName()));
                            } else {
                                context.getSource().sendFeedback(net.minecraft.network.chat.Component.literal("§d[EclipseWare] §cModule not found!"));
                            }
                            return 1;
                        })
                    )
                )
                .then(net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal("disable")
                    .then(net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument("module", com.mojang.brigadier.arguments.StringArgumentType.word())
                        .executes(context -> {
                            String modName = com.mojang.brigadier.arguments.StringArgumentType.getString(context, "module");
                            Module mod = moduleManager.getModule(modName);
                            if (mod != null) {
                                if (mod.isToggled()) mod.toggle();
                                context.getSource().sendFeedback(net.minecraft.network.chat.Component.literal("§d[EclipseWare] §cDisabled " + mod.getName()));
                            } else {
                                context.getSource().sendFeedback(net.minecraft.network.chat.Component.literal("§d[EclipseWare] §cModule not found!"));
                            }
                            return 1;
                        })
                    )
                )
                .then(net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal("config")
                    .then(net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument("module", com.mojang.brigadier.arguments.StringArgumentType.word())
                        .executes(context -> {
                            String modName = com.mojang.brigadier.arguments.StringArgumentType.getString(context, "module");
                            Module mod = moduleManager.getModule(modName);
                            if (mod != null) {
                                net.minecraft.client.Minecraft.getInstance().execute(() ->
                                    net.minecraft.client.Minecraft.getInstance().setScreenAndShow(new com.eclipseware.imnotcheatingyouare.client.clickgui.ConfigGui())
                                );
                            } else {
                                context.getSource().sendFeedback(net.minecraft.network.chat.Component.literal("§d[EclipseWare] §cModule not found!"));
                            }
                            return 1;
                        })
                    )
                )
            );
        });
    }
}