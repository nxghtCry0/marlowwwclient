# IMPORTANT!

Join the Discord!
https://discord.gg/marlowwwclient
Download releases or get early access here!
❤️

We also have a GitLab!

# Marlow Client V4 (Minecraft 1.21.11)

Marlow Client is an open-source Fabric client focused on combat automation, movement utilities, rendering tools, and quality-of-life systems for PvP-oriented gameplay. The click GUI, ESP, nametags, and HUD overlays are rendered natively with Dear ImGui rather than vanilla widgets. This is the maintained **Minecraft 1.21.11** branch (`mc-1.21.11`), downported from the 26.3 codebase.

<a href="https://www.star-history.com/?repos=nxghtCry0%2Fmarlowwwclient&type=date&legend=top-left">
 <picture>
   <source media="(prefers-color-scheme: dark)" srcset="https://api.star-history.com/chart?repos=nxghtCry0/marlowwwclient&type=date&theme=dark&legend=top-left" />
   <source media="(prefers-color-scheme: light)" srcset="https://api.star-history.com/chart?repos=nxghtCry0/marlowwwclient&type=date&legend=top-left" />
   <img alt="Star History Chart" src="https://api.star-history.com/chart?repos=nxghtCry0/marlowwwclient&type=date&legend=top-left" />
 </picture>
</a>

<a href="https://github.com/nxghtCry0/marlowwwclient/commits/main">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="https://github-readme-activity-graph.vercel.app/graph?username=nxghtCry0&theme=react-dark&hide_border=true&area=true&custom_title=Marlowww%20Client%20Commit%20Activity" />
    <source media="(prefers-color-scheme: light)" srcset="https://github-readme-activity-graph.vercel.app/graph?username=nxghtCry0&theme=github&hide_border=true&area=true&custom_title=Marlowww%20Client%20Commit%20Activity" />
    <img alt="Marlowww Client Commit Activity" src="https://github-readme-activity-graph.vercel.app/graph?username=nxghtCry0&theme=react-dark&hide_border=true&area=true&custom_title=Marlowww%20Client%20Commit%20Activity" />
  </picture>
</a>

## Technology Stack

[![Java](https://img.shields.io/badge/Java-21%2B-ED8B00?logo=openjdk&logoColor=white)](https://adoptium.net/)
[![Gradle](https://img.shields.io/badge/Gradle-Build-02303A?logo=gradle&logoColor=white)](https://gradle.org/)
[![Fabric Loader](https://img.shields.io/badge/Fabric_Loader-0.19.5-DBD0B4?logo=fabric&logoColor=black)](https://fabricmc.net/)
[![Fabric API](https://img.shields.io/badge/Fabric_API-0.141.6%2B1.21.11-DBD0B4?logo=fabric&logoColor=black)](https://modrinth.com/mod/fabric-api)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.11-62B47A?logo=minecraft&logoColor=white)](https://www.minecraft.net/)
[![Sponge Mixin](https://img.shields.io/badge/SpongePowered-Mixin-1E1E1E)](https://github.com/SpongePowered/Mixin)
[![MixinExtras](https://img.shields.io/badge/MixinExtras-0.5.5-1E1E1E)](https://github.com/LlamaLad7/MixinExtras)
[![LWJGL](https://img.shields.io/badge/LWJGL-Input%20%26%20Rendering-FFFFFF?logo=lwjgl&logoColor=black)](https://www.lwjgl.org/)
[![Dear ImGui](https://img.shields.io/badge/Dear_ImGui-imgui--java-8A2BE2)](https://github.com/SpaiR/imgui-java)

## Compatibility

- Minecraft: `1.21.11`
- Java: `21+` to run the game. Gradle itself runs on the JDK set by `org.gradle.java.home` in `gradle.properties` (JDK 25 by default), and the mod is compiled to Java 21 bytecode
- Fabric Loader: `0.19.5+`
- Fabric API: `0.141.6+1.21.11`
- Mappings: Mojang official mappings (`loom.officialMojangMappings()`), built with the `net.fabricmc.fabric-loom-remap` Loom plugin

## Branches

| Branch | Minecraft | Notes |
| --- | --- | --- |
| `mc-1.21.11` | 1.21.11 | This branch, maintained |
| `main` | 26.3 | Current development |
| `mc-26.2` | 26.2 | Port of `main` |
| `port-1.21.11` | 1.21.11 | Old one-off port, superseded by `mc-1.21.11` |

Because 1.21.11 is the last obfuscated Minecraft release, this branch differs from `main` in a few places:

- Windowing is GLFW. The ImGui overlay uses a polling platform backend (`GlfwImGuiPlatform`) and `InputUtil` reads keys and mouse buttons through GLFW. `main` uses SDL3.
- Keybinds are stored as GLFW key codes (`ConfigVersion` 1). Configs saved by the 26.x builds use SDL scancodes, so their keybinds are ignored on load and the defaults are kept. Module settings still load.
- GUI code uses vanilla `GuiGraphics` (`render`, `renderBackground`, `renderContents`) instead of `GuiGraphicsExtractor`.
- The first-person hand shader redirects rendering through `RenderSystem.outputColorTextureOverride` instead of hooking the render pass.
- Nothing reflects on Minecraft members by name, since names are obfuscated at runtime in a real install. Private members are reached through accessor mixins.
- The game runs from `run-1.21.11/` so it does not share a game directory with the 26.x builds.

## Build and Run

Clone the repository:

```powershell
git clone https://github.com/nxghtCry0/marlowwwclient.git
cd marlowwwclient
```

Run the client in a development environment:

```powershell
.\gradlew.bat runClient
```

Build a release jar:

```powershell
.\gradlew.bat build
```

Build artifacts are generated in `build/libs/`. Use the main jar artifact, not the `-sources` jar.

## Feature Catalog (109 Modules)

### Combat

- `AimAssist` | `AttributeSwap` | `AutoArmor` | `AutoClicker`
- `AutoShieldBreaker` | `BowAimbot` | `FastThrowables` | `HitSelect`
- `NoHitDelay` | `STap` | `Triggerbot` | `WTap` | `Weapons`

### Crystal

- `AutoHitCrystal` | `AutoPlaceCrystal` | `CrystalAura` | `CrystalHelper` | `Surround`

### Mace

- `AutoMace` | `AutoMaceCounter` | `AutoWindcharge` | `LungeSwap` | `PearlCatch`

### CartPvP

- `CartRefill` | `InstaCart` | `XbowCart`

### UHC

- `KeybindLava` | `KeybindWater` | `KeybindWeb`

### Movement

- `AntiAFK` | `AutoWalk` | `ElytraBoost` | `ElytraBounce` | `FakeLag` | `NoSlow`

### Render

- `BlockESP` | `Chams` | `ESP` | `Freecam` | `Fullbright` | `HandView`
- `LowFire` | `Nametags` | `NoParticles` | `NoTotemPop` | `RenderOptimizer`
- `StorageESP` | `Tracers` | `Trajectories` | `TrueSight` | `Xray`

### HUD

- `ArmorHUD` | `ArrayList` | `HUDEditor` | `KeybindList` | `TargetHUD`

### World

- `Automine` | `AutoSign` | `FastBreak` | `FastPlace` | `ChestStealer` | `Scaffold`

### Exploit

- `AttributeSwap` | `Backtrack` | `Blink` | `BreachSwap` | `HitSwap` | `Reach`

### Utility

- `AutoDHand` | `AutoDrain` | `AutoMLG` | `AutoTool` | `AutoTotem` | `AutoTrade` | `AutoWeb`
- `BridgeAssist` | `JumpReset` | `PearlBind` | `PearlGrapple` | `ShieldDrain` | `WebStun`

### Client

- `Bypass` | `LegacyUI` | `Menu` | `WeakDevice`

### Configs

- `Config Menu` (`Configurator`)

### Misc

- `AntiBot` | `AntiTranslationKey` | `ClientSpoof` | `DetectionDB`
- `Filter` (`FriendProtector`) | `NameProtect` | `NPC` | `PacketAuditor`
- `RecommendedConfigs` | `Teams`

### Blatant

- `BoatFly` | `Flight` | `GUIMove` | `KillAura`
- `KBDisplacement` | `NoFall` | `SilentAim`

### Farming

- `AnchorMacro` | `AutoFish` | `ElytraSwapMacro` | `GhostBlockMacro`
- `InventoryClean` | `InventoryFill` | `VapeMacro`

## Additional Systems

- ImGui click GUI (category sidebar, per-module settings panel, live search) and a legacy vanilla-widget click GUI, config GUI, and HUD editor screen
- ImGui-rendered ESP, nametags, tracers, trajectories, block/storage ESP, and target HUD
- Persistent config save/load via `ConfigManager`
- Friend list persistence via `FriendManager`
- Target filter persistence via `TargetFilterManager`
- Macro recording/playback via `MacroManager`
- HUD array list rendering for active modules
- Module keybind toggling (keyboard and mouse buttons) and per-tick module lifecycle hooks
- Client command support:
  - `/config gui`
  - `/config export` (copies serialized config to clipboard)

## License

This repository currently includes both `LICENSE.txt` and `GNU-LICENSE.md`, and `fabric.mod.json` declares `GPL-3.0`. Review licensing files and metadata together before redistribution.
