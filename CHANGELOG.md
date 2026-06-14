# Marlow Client v3.0.1 Changelog

This is a hotfix release (v3.0.1) addressing issues with client modules and block breaking actions.

## 🛠️ Bug Fixes & Improvements

### **AttributeSwap & AutoShieldBreaker**
- **Restored AutoShieldBreaker:** Restored AutoShieldBreaker to its working old codebase.
- **Fixed AttributeSwap Block Breaking:** Modified block-breaking swap triggers in AttributeSwap to execute strictly when the block is destroyed (`destroyBlock` hook), resolving conflicts and preventing it from swapping all the time.
- **Cleaned Up Settings:** Removed the redundant Auto Lunge setting and its hooks from AttributeSwap since it is already covered by LungeAssist.
- **Swap / Silent Modes:** Cleaned up Swap and Silent behaviors for combat attacks and block breaking.

---
