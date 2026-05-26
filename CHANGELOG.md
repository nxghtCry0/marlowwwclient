# Marlow Client v2.5.0 - UI & Combat Stability Rework

## 🚀 Major Features & Fixes

### **Config UI & Keybinds Redesign**
- **Glassy Theme:** Completely redesigned the Config UI into a modern glassy dialog with a clean 2-column modules list.
- **Bulk Action Controls:** Added bulk action buttons to quickly enable/disable/invert modules (All, None, Invert) with live selection statistics.
- **Keybind Binding in NewClickgui:** Implemented interactive keybind binding next to module toggle controls supporting both standard keys and mouse buttons.

### **Combat Module Polish & Anti-Cheat Bypasses**
- **AimAssist Customization:** Split master speed control into independent Horizontal and Vertical Speeds. Added target sorting (Distance, Angle), target bones (Head, Body, Arms, Legs, Nearest Part based on crosshair 2D angle), Jitter, and motion Prediction.
- **JumpReset Timing & Thread Safety:** Ported network-thread motion listeners to the main client tick render thread inside ClientGamePacketListenerMixin. Implemented a 250ms jump-trigger expiration window to completely resolve random or delayed jumping after landing.
- **Backtrack Rework:** Re-engineered the packet delaying logic to properly simulate latency, allowing reliable hits at players' historic positions (ghost positions) without causing client-side desync.
- **WTap & KillAura:** Fixed and refined detection bypasses to prevent flags on recent anti-cheat updates.

### **Command & Input Stability**
- **Command Autofill Bypass:** Added a setting under the Bypass module to block incoming clientbound commands packets and outgoing command suggestion requests.
- **Input Compatibility:** Restructured keyboard and mouse input handling to support record-based KeyEvent and MouseButtonInfo structures in Minecraft 26.1.2.
- **Slider Dragging:** Fixed coordinate scale mapping in MouseButtonEventMixin so clicking and dragging sliders matches screen scale factors perfectly.
- **Custom Input Macro System:** Implemented input macro recording and playback with JSON storage and Base64 clipboard sharing.
