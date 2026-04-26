# Marlow Client

Marlow Client is an open-source Fabric client focused on combat automation, movement utilities, rendering tools, and quality-of-life systems for PvP-oriented gameplay.

<a href="https://www.star-history.com/?repos=nxghtCry0%2Fmarlowwwclient&type=date&legend=top-left">
 <picture>
   <source media="(prefers-color-scheme: dark)" srcset="https://api.star-history.com/chart?repos=nxghtCry0/marlowwwclient&type=date&theme=dark&legend=top-left" />
   <source media="(prefers-color-scheme: light)" srcset="https://api.star-history.com/chart?repos=nxghtCry0/marlowwwclient&type=date&legend=top-left" />
   <img alt="Star History Chart" src="https://api.star-history.com/chart?repos=nxghtCry0/marlowwwclient&type=date&legend=top-left" />
 </picture>
</a>

## Technology Stack

[![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)](https://adoptium.net/)
[![Gradle](https://img.shields.io/badge/Gradle-Build-02303A?logo=gradle&logoColor=white)](https://gradle.org/)
[![Fabric Loader](https://img.shields.io/badge/Fabric_Loader-0.18.4-DBD0B4?logo=fabric&logoColor=black)](https://fabricmc.net/)
[![Fabric API](https://img.shields.io/badge/Fabric_API-0.141.3%2B1.21.11-DBD0B4?logo=fabric&logoColor=black)](https://modrinth.com/mod/fabric-api)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.11-62B47A?logo=minecraft&logoColor=white)](https://www.minecraft.net/)
[![Sponge Mixin](https://img.shields.io/badge/SpongePowered-Mixin-1E1E1E)](https://github.com/SpongePowered/Mixin)
[![LWJGL](https://img.shields.io/badge/LWJGL-Input%20%26%20Rendering-FFFFFF?logo=lwjgl&logoColor=black)](https://www.lwjgl.org/)

## Compatibility

- Minecraft: `1.21.11`
- Java: `21+`
- Fabric Loader: `0.18.4+`
- Fabric API: `0.141.3+1.21.11`

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

## Feature Catalog

The following modules are registered by the client.

### Combat

- `AimAssist`
- `WTap`
- `Triggerbot`
- `AutoWeb`
- `ShieldDrain`
- `AttributeSwap`
- `AutoMaceCounter`
- `AutoDrain`
- `HitSelect`
- `AutoShieldBreaker`
- `KnockbackDisplacement`
- `BreachSwap`
- `LungeAssist`
- `AutoMace`
- `PearlCatch`
- `JumpReset`
- `SilentAim`
- `KillAura`
- `Backtrack`
- `PearlBind`
- `AutoTotem`
- `Hitboxes`
- `AnchorMacro`
- `CrystalAura`
- `HitSwap`
- `AntiBot`
- `Teams`
- `BlinkModule` (`Blink`)

### Movement

- `AutoSprint`
- `NoJumpDelay`
- `Scaffold`
- `ElytraBoost`
- `AutoWalk`
- `GUIMove`
- `Freecam`

### World and Utility

- `AutoClicker`
- `FastPlace`
- `BridgeAssist`
- `Xray`
- `Configurator`
- `Automine`
- `AutoSign`

### Render and Visuals

- `ArrayListMod`
- `NameProtect`
- `Fullbright`
- `Reach`
- `HandView`
- `ESP`
- `Tracers`
- `Nametags`
- `StorageESP`
- `BlockESP`
- `DetectionAlert`
- `RenderOptimizer`
- `Theme`
- `Menu`

### Client and Safety

- `FriendProtector`
- `AntiTranslationKey`

## Additional Systems

- Click GUI and config GUI screens
- Persistent config save/load via `ConfigManager`
- Friend list persistence via `FriendManager`
- HUD array list rendering for active modules
- Module keybind toggling and per-tick module lifecycle hooks
- Client command support:
  - `/config gui`
  - `/config export` (copies serialized config to clipboard)

## License

This repository currently includes both `LICENSE.txt` and `GNU-LICENSE.md`, and `fabric.mod.json` declares `All-Rights-Reserved`. Review licensing files and metadata together before redistribution.
