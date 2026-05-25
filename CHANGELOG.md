# Marlow Client v2.3.1 - The UI & Combat Stability Rework

## 🚀 Major Features & Fixes

### **Config UI Redesign**
- **Glassy Theme:** Completely redesigned the Config UI into a modern glassy dialog with a clean 2-column modules list.
- **Bulk Action Controls:** Added bulk action buttons to quickly enable/disable/invert modules (All, None, Invert) with live selection statistics.
- **Improved Navigation:** Implemented smooth lerped scrolling with custom stylized scrollbars.

### **Slider Dragging & Mouse Scaling**
- **MouseButtonEvent Scaling:** Fixed coordinate scale mapping in MouseButtonEventMixin so clicking and dragging sliders matches screen scale factors perfectly.
- **Float/Double Settings Support:** Updated GlassyIntSlider to support both integer and double/floating-point settings natively.

### **Combat Module Fixes & Bypasses**
- **Backtrack Rework:** Re-engineered the packet delaying logic to properly simulate latency, allowing reliable hits at players' historic positions (ghost positions) without causing client-side desync.
- **Jump Reset:** Fixed the chance slider check to correctly respect configuration settings instead of constantly triggering.
- **WTap & KillAura:** Fixed and refined detection bypasses to prevent flags on recent anti-cheat updates.
