## 🧠 Codebase Intelligence
- The project leverages robust Mixins (e.g., `MultiPlayerGameModeMixin`) for granular packet monitoring and interaction injection.
- Minecraft's `handleInventoryMouseClick` with container ID `0` allows absolute silent manipulation of items without rendering `InventoryScreen` on the client.
- Client-side chunk iteration (`chunk.getBlockEntities().values()`) is the only performant method for wide-radius scanning compared to `mc.level.getBlockEntity(pos)`.

## 🚀 Future Roadmap
- `HitSwap` logic currently reads simple heuristic mappings (Netherite > Diamond > Iron). It may benefit from calculating exact attributes (e.g. Sharpness) using `EnchantmentHelper`.
- Implement interpolation for entity ESPs (Tracer lines can stutter if FPS is uncapped and entity ticks differ from render ticks).

## 🤖 AI Context & Handoff
- Modified `AutoTotem` to use direct container manipulation rather than triggering an active screen overlay, minimizing detectability.
- Updated `CrystalAura` damage heuristics to approximate target armor and explicitly override Anti-Suicide protections when enemies are below the `Face Place Threshold`.
- Created `HitSwap` module that injects before network packets send on standard UI attack to ensure peak damage. The agent should know Caveman logic is active for terse outputs.
