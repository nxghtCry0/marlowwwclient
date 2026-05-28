# Marlow Client v2.6.0 - WTap & Customization Update

## 🚀 Major Features & Fixes

### **WTap Rework & Customization**
- **WTap Modes:** Added customizable modes (`Auto`, `Silent`, and `Normal`) supporting both packet-level and physical key-release methods.
- **Rebound Key Compatibility:** Restructured movement checks to dynamically resolve the bound key code rather than assuming the default 'W' key, supporting custom keybinds out-of-the-box.
- **Chance & Jitter Settings:** Added a "Chance (%)" slider setting (0.0 to 100.0) to randomize whether the sprint reset triggers on hit, along with a customizable "Jitter Ticks" slider (0.0 to 5.0) to randomize the timing windows.
- **Target Filtering:** Added an "Only Players" checkbox setting to prevent WTap from triggering when hitting non-player entities (mobs, items, etc.).
