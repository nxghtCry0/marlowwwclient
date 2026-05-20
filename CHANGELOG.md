# Marlow Client v2.3.0 - The Performance & Combat Update

## 🚀 Major Features & Fixes

### **KillAura Functionality Restored**
- **Settings Registration:** Resolved the critical bug where `KillAura` would not attack any targets. Registered all missing settings (`Range`, `Combat System`, `Criticals Only`, `Turn Speed`, `Movement Correction`, `Target Players`, `Target Mobs`, `Target Animals`, `Modern Delay`, and `1.8.9 CPS` options) in the client's `SettingsManager`.
- **Target Filtering & Range Validation:** Corrected fallback values that were returning `0` range and `false` for target acquisition, restoring full target tracking, rotation synchronization, and attack routines.

### **ESP Sprinting Projection & Performance Rewrite**
- **Native Projection Matrix Integration:** Completely refactored the 2D projection math in `RenderUtils.project2D` to utilize the native `camera.getViewRotationProjectionMatrix(new Matrix4f())`. This replaces the previous reflection-based retrieval of private `getFov` fields which was causing severe rendering lag and desynchronization on sprint transitions.
- **Visual FOV Synchronization:** ESP boxes now update in real-time instantly during FOV transitions (such as starting or stopping a sprint) without screen-jitter or rendering misalignment.
- **Obsolete Code Purge:** Cleaned up and deleted the obsolete `GameRendererAccessor` mixin and associated dummy testing calls from the `ESP` module, significantly reducing codebase footprint and enhancing cross-version stability.

### **Build & Versioning**
- Migrated hardcoded versioning in `fabric.mod.json` to a dynamic gradle variable placeholder (`${version}`) and bumped `gradle.properties` mod version to `2.3.0`.

---

# Marlow Client v2.2.0 - The Stealth Update

## 🚀 Major Features & Bypasses

### **100% Undetected Hotbar Spoofing (Vulcan Bypass)**
- **SpoofManager Queue System:** Eradicated the dreaded `BadPackets (Hotbar) Type V` flag completely. Item selection and interaction packets now use an artificial human-like latency queue (1-tick delay) to perfectly spoof Vulcan's strict packet time-check heuristics.
- **Native Syncing via `setClientSlot()`:** Refactored `ModuleUtils` to prevent double-packet signatures. We now use pure local reflection to manipulate the client slot visually, allowing vanilla's `LocalPlayer.tick()` to natively handle network synchronization without redundant transmission.
- **CrystalHelper Rewrite:** The `CrystalHelper` module is now fully integrated with `SpoofManager`, granting 100% stealthy crystal placements and breaking sequences.

### **Combat & Movement Stability**
- **Shield State Recognition:** Overhauled `JumpReset`, `KillAura`, and `SilentAim` modules to properly identify and respect `mc.player.isBlocking()`. This fixes the bot-attack/jump-reset bugs where the client would conflict with server states when a shield was hit.
- **PearlGrapple Tracking:** Deployed a brand new target-tracking entity module specifically for pearls. Fully automated rotation smoothing, nearest-player targeting, and undetectable hotbar syncing.

## 🎨 UI & UX Improvements

### **NewClickgui Enhancements**
- **Stealth Bypass Activation:** Modified the Bypass module activation to be highly discrete: requires exactly 5 key presses within a 3-second window instead of the old "look down" detection vector.
- **Infinite Scroll & Negative Slider Fix:** Stabilized `GlassyIntSlider` to clamp values properly, eliminating infinite dragging and negative value overflows.
- **Scroll Isolation:** Component scrolling is now dynamically restricted exclusively to the panel you are actively hovering over.
- **Smart Filtering:** Removed the generic MapPlus/expander icon from modules that don't have any configurable settings to clean up visual clutter.

## ⚙️ Core Architecture
- Hardcoded semantic version `2.2.0` across `fabric.mod.json`, `gradle.properties`, and `README.md` to prevent IDE `runClient` crashes during development loops.
- Registered the `SpoofManager` execution loop directly into `START_CLIENT_TICK` to guarantee pristine execution ordering.
- Integrated dynamic real-time commit activity charts directly into the `README.md`.
