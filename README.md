# IMPORTANT!

Join the Discord!
https://discord.gg/marlowwwclient
Download releases or get early access here!
❤️

My main account, @nxght_Cry0 on discord was TERMINATED!
The only official account of mine is now @firefox.lol

# Marlow Client V4

Marlow Client is an open-source Fabric client focused on combat automation, movement utilities, rendering tools, and quality-of-life systems for PvP-oriented gameplay. Newly rewritten and rearchitected for Minecraft 26.2.

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

[![Java](https://img.shields.io/badge/Java-25-ED8B00?logo=openjdk&logoColor=white)](https://adoptium.net/)
[![Gradle](https://img.shields.io/badge/Gradle-Build-02303A?logo=gradle&logoColor=white)](https://gradle.org/)
[![Fabric Loader](https://img.shields.io/badge/Fabric_Loader-0.19.3-DBD0B4?logo=fabric&logoColor=black)](https://fabricmc.net/)
[![Fabric API](https://img.shields.io/badge/Fabric_API-0.155.2%2B26.2-DBD0B4?logo=fabric&logoColor=black)](https://modrinth.com/mod/fabric-api)
[![Minecraft](https://img.shields.io/badge/Minecraft-26.2-62B47A?logo=minecraft&logoColor=white)](https://www.minecraft.net/)
[![Sponge Mixin](https://img.shields.io/badge/SpongePowered-Mixin-1E1E1E)](https://github.com/SpongePowered/Mixin)
[![LWJGL](https://img.shields.io/badge/LWJGL-Input%20%26%20Rendering-FFFFFF?logo=lwjgl&logoColor=black)](https://www.lwjgl.org/)

## Compatibility

- Minecraft: `26.2` _(Legacy `1.21.11` port available on the `port-1.21.11` branch)_
- Java: `25+`
- Fabric Loader: `0.19.3+`
- Fabric API: `0.155.2+26.2`

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

## Feature Catalog (69 Modules)

### Combat

- `AimAssist` | `AttributeSwap` | `AutoDrain` | `AutoMace`
- `AutoMaceCounter` | `AutoShieldBreaker` | `AutoTotem` | `AutoWeb`
- `AutoWindcharge` | `BreachSwap` | `FastThrowables` | `HitSelect`
- `HitSwap` | `JumpReset` | `LungeSwap`
- `PearlBind` | `PearlCatch` | `STap` | `ShieldDrain`
- `SilentAim` | `SilentAimbot` | `Triggerbot` | `WTap`
- `Weapons`

### Crystal

- `AnchorMacro` | `AutoDHand` | `CrystalAura` | `CrystalHelper` | `Surround`

### Blatant

- `Backtrack` | `BlinkModule` (`Blink`) | `BoatFly` | `GUIMove`
- `KBDisplacement` (`KnockbackDisplacement`) | `KillAura` | `Reach`

### Movement

- `AutoWalk` | `AutoSprint` | `ElytraBoost` | `Flight`
- `Freecam` | `NoJumpDelay` | `Scaffold`

### Render

- `ArrayListMod` | `BlockESP` | `DetectionAlert` | `ESP`
- `Fullbright` | `HandView` | `LowFire` | `Menu`
- `NameProtect` | `Nametags` | `NoParticles` | `NoTotemPop`
- `RenderOptimizer` | `StorageESP` | `Theme` | `Trajectories` | `Xray`

### Misc & World

- `AntiBot` | `AntiTranslationKey` | `AutoClicker` | `Automine`
- `AutoSign` | `BridgeAssist` | `Bypass` | `ChestStealer`
- `Configurator` | `FastPlace` | `FriendProtector` | `NPC`
- `Teams`

## Additional Systems

- Click GUI and config GUI screens
- Persistent config save/load via `ConfigManager`
- Friend list persistence via `FriendManager`
- HUD array list rendering for active modules powered by `HudElementRegistry`
- Module keybind toggling and per-tick module lifecycle hooks
- Client command support:
  - `/config gui`
  - `/config export` (copies serialized config to clipboard)

## License

This repository currently includes both `LICENSE.txt` and `GNU-LICENSE.md`, and `fabric.mod.json` declares `All-Rights-Reserved`. Review licensing files and metadata together before redistribution.
