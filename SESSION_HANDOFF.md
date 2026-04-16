## 🧠 Codebase Intelligence
- The project environment uses Official Mojang Mappings for 1.21.11, dropping "get" prefixes on records/frequently used methods (e.g., `camera.position()` instead of `camera.getPosition()`).
- The previous String-based reflection setup for `Camera` bypassed Fabric Loom's remapper, resulting in field lookup crashes during obfuscated production releases and defaulting to `getEyePosition`. Native `camera.position()` correctly remaps. 
- `ConnectionMixin` acts as the primary movement packet firewall. It is safer to spoof rotations by overriding vanilla's 1-tick `ServerboundMovePlayerPacket` instead of forcing out separate `.Rot` packets through a utility class. This prevents `TickTimer` and `PacketOrder` Grim AC flags.

## 🚀 Future Roadmap
- Address potential pitch spoofing for the client's visual 3rd person rotations. Currently, we update the server's body/head yaw, but syncing visual pitch requires a custom mixin without breaking the `Camera` rotation reference point.
- Implement specialized AC checks in `RotationManager` to dynamically slow rotation smoothing based on the sever's violation tolerance if an anti-cheat is active.

## 🤖 AI Context & Handoff
- Successfully overhauled `RotationManager` and `SilentAimUtil` to pipeline identically through `ConnectionMixin` instead of sending direct packets.
- Fixed the cached ESP references lingering between singleplayer/servers.
- The `cat << EOF` multiline syntax had powershell parsing issues directly through the `run_command` interface; using standard file writes is preferred.
- Built without errors using Gradle.
