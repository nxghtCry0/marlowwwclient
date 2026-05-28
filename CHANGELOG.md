# Marlow Client v3.0 - AutoClicker & SafeWalk Update

## 🚀 Major Features & Fixes

### **AutoClicker Rework**
- **Right Clicker Integration:** Added a "Right Clicker" setting to allow autoclicking the right-mouse (Use) key independently.
- **Double Click Support:** Implemented a new, isolated click consistency engine specifically for right-clicking, mimicking realistic human click distributions.

### **SafeWalk (BridgeAssist) Update**
- **Blatant Mode:** Added a "Blatant" mode that dynamically clips player movement delta coordinates at block edges. This prevents the player from falling off blocks while moving at normal, standing speed instead of crouching.

### **Gamma Fullbright Mode**
- **Gamma Setting:** Added a "Gamma" mode to Fullbright that overrides rendering brightness calculations via Lightmap, lighting up the screen without changing system configuration or printing illegal option error spam to client logs.
