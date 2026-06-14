# Marlow Client v3.1 Changelog

This is a minor release (v3.1) adding the new WebStun module and improving the LungeAssist module.

## 🚀 New Features

### **WebStun**
- **Automatic Cobweb Placement:** Places a cobweb directly under target player's feet when their shield is disabled by AutoShieldBreaker or when hit.
- **Anti-Cheat Safe:** Spans actions across multiple ticks using a multi-tick state machine.
- **Silent Rotations & Placements:** Uses exact GCD-snapped rotations and post-movement callbacks to place blocks against solid support blocks, preventing GrimAC alerts.

## 🛠️ Bug Fixes & Improvements

### **LungeAssist**
- **Jumping Apex Logic:** Fixed LungeAssist so that if enabled while jumping/falling, it waits for the apex of the jump before initiating the spear lunge attack.
- **GPL-3 Compliance:** Updated base implementation to align with project licensing constraints.

### **Bypass Infrastructure**
- **MultiPlayerGameMode client-carried sync:** Prevented duplicate selection packets (BadPacketsA bypass) by reflectively syncing carried index fields.
- **Mixin Optimization:** Replaced static mixin fields with localized manager references to prevent loader crashes.
