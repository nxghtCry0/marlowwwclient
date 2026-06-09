# Marlow Client v3.0 Changelog

Welcome to the official release of Marlow Client v3.0! This release brings a complete overhaul of the HUD layout editor, significant updates to combat and macro engines, and advanced network-level bypasses to keep your movements and actions undetected on top-tier anticheats.

---

## 🎨 ImGui & NanoVG HUD System

We have completely retired the old UI configuration structure and rebuilt the HUD display engine from the ground up using a hybrid **ImGui** and **NanoVG** framework. This introduces fluid animations, hardware-accelerated vector drawing, and native window-docking support.

### **Advanced HUD Editor**
- **HUD Editor Screen:** A brand new, interactive layout editor. Press the Menu key (default: `Right Shift`) to open the click GUI and configure your on-screen modules dynamically.
- **Dynamic Snap-to-Grid & Docking:** Drag and drop elements anywhere on your viewport with automatic alignment and snapping features.
- **NanoVG Rendering Engine:** Custom vector graphics API wrapping modern OpenGL/LWJGL bindings. All interface shapes, borders, and graphs are drawn using high-fidelity antialiasing for maximum sharpness at any resolution.
- **Fluid Animation System:** Hover states, scale triggers, and visibility transitions utilize sophisticated interpolation curves for smooth visual feedback.

### **TargetHUD Update**
- **Sleek Target Cards:** Reworked the player-focused target card. Displays health bars, armor durability alerts, distance, and 3D head previews.
- **Damage Splashes:** Integrated custom shader-based scale and fade transitions when the target takes damage.
- **Interactive Filtering:** Refined target prioritization options under the hood to ensure precise selection in chaotic PvP environments.

### **ArmorHUD Integration**
- **Real-Time Indicators:** Visual status indicators for your currently equipped armor items.
- **Durability Warnings:** Explicitly flags when an armor piece falls below critical threshold values to prevent unexpected breakages.

---

## 🛡️ Combat, Macros & Bypasses

We have implemented key upgrades to major PvP modules and macro systems to improve customizability, reaction speed, and overall anticheat bypass capability.

### **AnchorMacro Defensive Shielding**
- **Auto Shield Algorithm:** AnchorMacro now calculates the player's 3D orientation relative to the targeted Respawn Anchor. It automatically selects defensive blocks (such as Obsidian, Crying Obsidian, or any full block in the hotbar) and places them to block incoming explosion damage.
- **Collision Protection:** Features real-time player bounding-box collision checks to prevent accidentally trapping yourself inside your own shield blocks.
- **3D Angle Evaluation:** Computes directional vectors along all three axes (including Y UP/DOWN) for perfect shield placements in vertical terrain.

### **Network-Level Silent Swap & Placement Engine**
- **Bypass Jitter Rotations:** Integrated micro-GCD jitter rotations (+/- 0.005 degrees) to serverbound packets, successfully bypassing strict look-duplicate checks (like those found on GrimAC, Vulcan, and Intave).
- **Synchronized Packet Placement:** Designed a robust state-machine scheduler (`placeBlockSilent`) that holds, swaps, and reverts main-hand slots in synchronization with client ticks, eliminating desyncs.
- **Vanilla Override:** Patched internal client attack loops to prevent vanilla left-click mining/attacking while placing crystals or obsidian, ensuring smooth combat flow.

### **CrystalHelper Enhancements**
- **Granular Controls:** Added a suite of hotbar-aware settings including:
  - `On Sword`, `On Totem`, `On Glowstone`, `On Anchor` targeting criteria.
  - Custom cooldowns configured in milliseconds.
  - Optional `Hold Trigger` setting to repeat placement.
  - Customizable bedrock exclusion toggles.

### **AutoClicker & SafeWalk**
- **Right Clicker Support:** Isolated right-mouse button autoclicking with dedicated consistency engines.
- **Blatant SafeWalk:** Movement-delta bounding clip engine that prevents falling off blocks without requiring standard crouching speed.

---

## 🛠️ General Improvements & Fixes

- **Menu Key Closing:** Pressing your configured Menu keybind inside the ClickGUI now closes the window immediately, matching expected sandbox behavior.
- **Accurate Build Outputs:** Corrected jar compilation naming configurations to output `marlowwwclient-3.0.jar` across all settings files.
