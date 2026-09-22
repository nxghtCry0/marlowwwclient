# IMPORTANT!

Join the Discord!
https://discord.gg/marlowwwclient
Download releases or get early access here!
❤️

We also have a GitLab!

# Marlow Client V4

Marlow Client is an open-source Fabric client focused on combat automation, movement utilities, rendering tools, and quality-of-life systems for PvP-oriented gameplay. The click GUI, ESP, nametags, and HUD overlays are rendered natively with Dear ImGui rather than vanilla widgets. Newly rewritten and rearchitected for Minecraft 26.3.

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

[![Java](https://img.shields.io/badge/Java-25%2F26-ED8B00?logo=openjdk&logoColor=white)](https://adoptium.net/)
[![Gradle](https://img.shields.io/badge/Gradle-Build-02303A?logo=gradle&logoColor=white)](https://gradle.org/)
[![Fabric Loader](https://img.shields.io/badge/Fabric_Loader-0.19.5-DBD0B4?logo=fabric&logoColor=black)](https://fabricmc.net/)
[![Fabric API](https://img.shields.io/badge/Fabric_API-0.161.0%2B26.3-DBD0B4?logo=fabric&logoColor=black)](https://modrinth.com/mod/fabric-api)
[![Minecraft](https://img.shields.io/badge/Minecraft-26.3-62B47A?logo=minecraft&logoColor=white)](https://www.minecraft.net/)
[![Sponge Mixin](https://img.shields.io/badge/SpongePowered-Mixin-1E1E1E)](https://github.com/SpongePowered/Mixin)
[![MixinExtras](https://img.shields.io/badge/MixinExtras-0.5.5-1E1E1E)](https://github.com/LlamaLad7/MixinExtras)
[![LWJGL](https://img.shields.io/badge/LWJGL-Input%20%26%20Rendering-FFFFFF?logo=lwjgl&logoColor=black)](https://www.lwjgl.org/)
[![Dear ImGui](https://img.shields.io/badge/Dear_ImGui-imgui--java-8A2BE2)](https://github.com/SpaiR/imgui-java)

## Compatibility

- Minecraft: `26.3` _(Legacy `1.21.11` port available on the `port-1.21.11` branch)_
- Java: `25+` to run Gradle itself, `26` to actually launch the client (Loom's toolchain will fetch/select this automatically)
- Fabric Loader: `0.19.5+`
- Fabric API: `0.161.0+26.3`

This build targets Minecraft 26.3's SDL3-based windowing (it replaced the previous GLFW window), so the ImGui overlay talks to input and rendering through a custom SDL bridge rather than imgui-java's stock GLFW backend.

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

## Feature Catalog (108 Modules)

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

- `AutoDHand` | `AutoDrain` | `AutoMLG` | `AutoTool` | `AutoTotem` | `AutoWeb`
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
