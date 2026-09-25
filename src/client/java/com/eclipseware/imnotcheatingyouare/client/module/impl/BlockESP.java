package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.setting.SettingsManager;
import com.eclipseware.imnotcheatingyouare.client.utils.RenderUtils;
import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.type.ImBoolean;
import imgui.type.ImString;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3d;

import java.awt.Color;
import java.io.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;

public class BlockESP extends Module {

    private final Set<Block> selectedBlocks = new CopyOnWriteArraySet<>();
    private final Map<Block, Color> blockColorMap = new HashMap<>();

    private static final record CachedBlock(BlockPos pos, Color color) {}
    private final List<CachedBlock> cachedBlocks = new java.util.concurrent.CopyOnWriteArrayList<>();
    private long lastCacheTick = 0;

    private static final Vector3d projVec = new Vector3d();
    private static final Vector3d[] boxProjBuffer = new Vector3d[8];
    static {
        for (int i = 0; i < 8; i++) {
            boxProjBuffer[i] = new Vector3d();
        }
    }

    private final ImString searchFilter = new ImString(256);
    private final ImBoolean showSelectorWindow = new ImBoolean(false);
    private String selectedCategory = "All";

    public BlockESP() {
        super("BlockESP", Category.Render, "Highlights selected blocks via native ImGui block selector.");

        SettingsManager sm = ImnotcheatingyouareClient.INSTANCE.settingsManager;

        sm.rSetting(new Setting("Range", this, 48.0, 8.0, 128.0, true));
        sm.rSetting(new Setting("Max Blocks", this, 1500.0, 100.0, 5000.0, true));
        sm.rSetting(new Setting("Fill Opacity", this, 18.0, 0.0, 60.0, true));
        sm.rSetting(new Setting("Line Width", this, 1.5, 0.5, 4.0, false));
        sm.rSetting(new Setting("Fill", this, true));
        sm.rSetting(new Setting("Outline", this, true));
        sm.rSetting(new Setting("Tracers", this, false));
        sm.rSetting(new Setting("Block Color", this, new Color(0, 220, 255)));
        sm.rSetting(new Setting("Open Block Selector", this, false));

        addDefaultBlock("minecraft:diamond_ore", new Color(0, 220, 255));
        addDefaultBlock("minecraft:deepslate_diamond_ore", new Color(0, 220, 255));
        addDefaultBlock("minecraft:ancient_debris", new Color(200, 120, 80));
        addDefaultBlock("minecraft:spawner", new Color(255, 60, 60));
        addDefaultBlock("minecraft:end_portal_frame", new Color(50, 205, 50));

        loadSelectedBlocksFromFile();
    }

    private void addDefaultBlock(String idStr, Color color) {
        try {
            Block block = BuiltInRegistries.BLOCK.get(Identifier.parse(idStr))
                    .map(net.minecraft.core.Holder::value)
                    .orElse(null);
            if (block != null && block != Blocks.AIR) {
                selectedBlocks.add(block);
                blockColorMap.put(block, color);
            }
        } catch (Exception ignored) {}
    }

    public boolean isBlockSelected(Block block) {
        return selectedBlocks.contains(block);
    }

    public void setBlockSelected(Block block, boolean select) {
        if (select) {
            selectedBlocks.add(block);
        } else {
            selectedBlocks.remove(block);
        }
        saveSelectedBlocksToFile();
    }

    public int getSelectedBlockCount() {
        return selectedBlocks.size();
    }

    @Override
    public void onTick() {
        Setting openSet = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Open Block Selector");
        if (openSet != null && openSet.getValBoolean()) {
            openSet.setValBoolean(false);
            showSelectorWindow.set(true);
        }
    }

    public void renderImGuiSelectorWindow() {
        if (!showSelectorWindow.get()) return;

        ImGui.setNextWindowSize(540, 420, ImGuiCond.FirstUseEver);
        if (ImGui.begin("BlockESP Block Selector", showSelectorWindow)) {
            ImGui.textColored(0.0f, 0.85f, 1.0f, 1.0f, "Select blocks to highlight (" + selectedBlocks.size() + " selected)");
            ImGui.separator();

            ImGui.inputText("Search", searchFilter);

            if (ImGui.button("All")) selectedCategory = "All";
            ImGui.sameLine();
            if (ImGui.button("Ores")) selectedCategory = "Ores";
            ImGui.sameLine();
            if (ImGui.button("Storage")) selectedCategory = "Storage";
            ImGui.sameLine();
            if (ImGui.button("Utility")) selectedCategory = "Utility";
            ImGui.sameLine();
            if (ImGui.button("Selected Only")) selectedCategory = "Selected";

            ImGui.sameLine();
            if (ImGui.button("Clear All")) {
                selectedBlocks.clear();
                saveSelectedBlocksToFile();
            }

            ImGui.separator();

            if (ImGui.beginChild("BlockListRegion", 0, 0, true)) {
                String filter = searchFilter.get().trim().toLowerCase();

                for (Block block : BuiltInRegistries.BLOCK) {
                    if (block == Blocks.AIR || block == Blocks.CAVE_AIR || block == Blocks.VOID_AIR) continue;
                    Identifier id = BuiltInRegistries.BLOCK.getKey(block);
                    if (id == null) continue;

                    String idStr = id.toString();
                    String path = id.getPath();
                    String displayName = capitalizeWords(path.replace('_', ' '));

                    if (!filter.isEmpty() && !path.contains(filter) && !idStr.contains(filter) && !displayName.toLowerCase().contains(filter)) {
                        continue;
                    }

                    if ("Selected".equals(selectedCategory) && !selectedBlocks.contains(block)) continue;
                    if ("Ores".equals(selectedCategory) && !path.contains("ore") && !path.contains("debris") && !path.contains("raw_")) continue;
                    if ("Storage".equals(selectedCategory) && !path.contains("chest") && !path.contains("barrel") && !path.contains("shulker") && !path.contains("hopper")) continue;
                    if ("Utility".equals(selectedCategory) && !path.contains("spawner") && !path.contains("portal") && !path.contains("beacon") && !path.contains("conduit") && !path.contains("table") && !path.contains("furnace")) continue;

                    boolean isSel = selectedBlocks.contains(block);
                    if (ImGui.checkbox(displayName + "##" + idStr, isSel)) {
                        setBlockSelected(block, !isSel);
                    }
                    ImGui.sameLine(280);
                    ImGui.textDisabled(idStr);
                }

                ImGui.endChild();
            }
        }
        ImGui.end();
    }

    private String capitalizeWords(String input) {
        String[] words = input.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (w.isEmpty()) continue;
            sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(" ");
        }
        return sb.toString().trim();
    }

    private record Hit(BlockPos pos, int color, int openFaces) {}

    private final java.util.concurrent.ExecutorService scanner = java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Marlow BlockESP");
        t.setDaemon(true);
        return t;
    });
    private volatile List<Hit> hits = List.of();
    private volatile boolean scanning = false;
    private long lastScan = 0;
    private final org.joml.Matrix4f matrix = new org.joml.Matrix4f();
    private final org.joml.Vector4f[] clip = new org.joml.Vector4f[8];
    {
        for (int i = 0; i < 8; i++) clip[i] = new org.joml.Vector4f();
    }
    private static final int[][] EDGES = {{0, 1}, {2, 3}, {4, 5}, {6, 7}, {0, 2}, {1, 3}, {4, 6}, {5, 7}, {0, 4}, {1, 5}, {2, 6}, {3, 7}};
    private static final int[][] FACES = {{0, 2, 6, 4}, {1, 5, 7, 3}, {0, 1, 5, 4}, {2, 6, 7, 3}, {0, 1, 3, 2}, {4, 6, 7, 5}};
    private static final net.minecraft.core.Direction[] FACE_DIRS = {
            net.minecraft.core.Direction.WEST, net.minecraft.core.Direction.EAST,
            net.minecraft.core.Direction.DOWN, net.minecraft.core.Direction.UP,
            net.minecraft.core.Direction.NORTH, net.minecraft.core.Direction.SOUTH};

    private double num(String name, double fallback) {
        Setting s = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, name);
        return s != null ? s.getValDouble() : fallback;
    }

    private boolean bool(String name, boolean fallback) {
        Setting s = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, name);
        return s != null ? s.getValBoolean() : fallback;
    }

    @Override
    public void onDisable() {
        hits = List.of();
    }

    @Override
    public void onRenderHUD(GuiGraphicsExtractor guiGraphics, Object tickCounterObj) {
    }

    private void scheduleScan() {
        if (selectedBlocks.isEmpty()) {
            hits = List.of();
            return;
        }
        long now = System.currentTimeMillis();
        if (scanning || now - lastScan < 400) return;
        lastScan = now;
        scanning = true;
        final net.minecraft.client.multiplayer.ClientLevel level = mc.level;
        final BlockPos center = mc.player.blockPosition();
        final int range = (int) num("Range", 48);
        final int max = (int) num("Max Blocks", 1500);
        Setting colorSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Block Color");
        final int defaultColor = colorSetting != null ? colorSetting.getValColor() : 0xFF00DCFF;
        final Set<Block> wanted = new HashSet<>(selectedBlocks);
        final Map<Block, Integer> colors = new HashMap<>();
        for (Map.Entry<Block, Color> e : blockColorMap.entrySet()) colors.put(e.getKey(), e.getValue().getRGB());
        scanner.execute(() -> {
            try {
                hits = scan(level, center, range, max, wanted, colors, defaultColor);
            } catch (Throwable ignored) {
            } finally {
                scanning = false;
            }
        });
    }

    private static List<Hit> scan(net.minecraft.client.multiplayer.ClientLevel level, BlockPos center, int range, int max,
                                  Set<Block> wanted, Map<Block, Integer> colors, int defaultColor) {
        List<Hit> found = new ArrayList<>();
        int rsq = range * range;
        int cx0 = (center.getX() - range) >> 4, cx1 = (center.getX() + range) >> 4;
        int cz0 = (center.getZ() - range) >> 4, cz1 = (center.getZ() + range) >> 4;
        java.util.function.Predicate<BlockState> pred = st -> wanted.contains(st.getBlock());
        BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos();
        for (int cx = cx0; cx <= cx1; cx++) {
            for (int cz = cz0; cz <= cz1; cz++) {
                net.minecraft.world.level.chunk.LevelChunk chunk = level.getChunkSource().getChunk(cx, cz, net.minecraft.world.level.chunk.status.ChunkStatus.FULL, false);
                if (chunk == null) continue;
                net.minecraft.world.level.chunk.LevelChunkSection[] sections = chunk.getSections();
                for (int i = 0; i < sections.length; i++) {
                    net.minecraft.world.level.chunk.LevelChunkSection section = sections[i];
                    if (section == null || section.hasOnlyAir() || !section.maybeHas(pred)) continue;
                    int sy = chunk.getSectionYFromSectionIndex(i) << 4;
                    if (sy + 15 < center.getY() - range || sy > center.getY() + range) continue;
                    for (int y = 0; y < 16; y++) {
                        for (int z = 0; z < 16; z++) {
                            for (int x = 0; x < 16; x++) {
                                Block b = section.getBlockState(x, y, z).getBlock();
                                if (!wanted.contains(b)) continue;
                                int wx = (cx << 4) + x, wy = sy + y, wz = (cz << 4) + z;
                                int dx = wx - center.getX(), dy = wy - center.getY(), dz = wz - center.getZ();
                                if (dx * dx + dy * dy + dz * dz > rsq) continue;
                                BlockPos pos = new BlockPos(wx, wy, wz);
                                int open = 0;
                                for (int d = 0; d < 6; d++) {
                                    probe.setWithOffset(pos, FACE_DIRS[d]);
                                    if (level.getBlockState(probe).getBlock() != b) open |= 1 << d;
                                }
                                if (open == 0) continue;
                                found.add(new Hit(pos, colors.getOrDefault(b, defaultColor), open));
                            }
                        }
                    }
                }
            }
        }
        if (found.size() > max) {
            found.sort(java.util.Comparator.comparingDouble(h -> h.pos().distSqr(center)));
            found = new ArrayList<>(found.subList(0, max));
        }
        return found;
    }

    public void renderImGuiOverlay() {
        if (!isToggled() || mc.player == null || mc.level == null || mc.gameRenderer == null) return;
        scheduleScan();
        List<Hit> list = hits;
        if (list.isEmpty()) return;

        boolean fill = bool("Fill", true);
        boolean outline = bool("Outline", true);
        boolean tracers = bool("Tracers", false);
        float width = (float) num("Line Width", 1.5);
        int fillAlpha = (int) (num("Fill Opacity", 18) * 2.55);

        net.minecraft.client.Camera camera = mc.gameRenderer.mainCamera();
        net.minecraft.world.phys.Vec3 cam = camera.position();
        camera.getViewRotationProjectionMatrix(matrix);
        float dw = ImGui.getIO().getDisplaySizeX();
        float dh = ImGui.getIO().getDisplaySizeY();
        imgui.ImDrawList dl = ImGui.getBackgroundDrawList();
        org.joml.Vector4f center = new org.joml.Vector4f();

        for (Hit h : list) {
            BlockPos p = h.pos();
            float bx = (float) (p.getX() - cam.x), by = (float) (p.getY() - cam.y), bz = (float) (p.getZ() - cam.z);
            boolean anyFront = false;
            for (int i = 0; i < 8; i++) {
                clip[i].set(bx + (i & 1), by + ((i >> 1) & 1), bz + ((i >> 2) & 1), 1f);
                matrix.transform(clip[i]);
                if (clip[i].w > 0.05f) anyFront = true;
            }
            if (!anyFront) continue;
            int argb = h.color();
            int r = (argb >> 16) & 0xFF, g = (argb >> 8) & 0xFF, b = argb & 0xFF;
            int line = RenderUtils.toImGuiColor(r, g, b, 230);
            int shade = RenderUtils.toImGuiColor(r, g, b, fillAlpha);

            if (fill && fillAlpha > 0) {
                for (int f = 0; f < 6; f++) {
                    if ((h.openFaces() & (1 << f)) == 0) continue;
                    int[] q = FACES[f];
                    if (clip[q[0]].w <= 0.05f || clip[q[1]].w <= 0.05f || clip[q[2]].w <= 0.05f || clip[q[3]].w <= 0.05f) continue;
                    dl.addQuadFilled(sx(q[0], dw), sy(q[0], dh), sx(q[1], dw), sy(q[1], dh), sx(q[2], dw), sy(q[2], dh), sx(q[3], dw), sy(q[3], dh), shade);
                }
            }
            if (outline) {
                for (int[] e : EDGES) {
                    if (!edgeVisible(h.openFaces(), e[0], e[1])) continue;
                    drawEdge(dl, clip[e[0]], clip[e[1]], dw, dh, line, width);
                }
            }
            if (tracers) {
                center.set(bx + 0.5f, by + 0.5f, bz + 0.5f, 1f);
                matrix.transform(center);
                if (center.w > 0.05f) {
                    dl.addLine(dw / 2f, dh / 2f, (center.x / center.w + 1f) * 0.5f * dw, (1f - center.y / center.w) * 0.5f * dh, line, 1f);
                }
            }
        }
    }

    private static boolean edgeVisible(int open, int a, int b) {
        for (int f = 0; f < 6; f++) {
            if ((open & (1 << f)) == 0) continue;
            boolean ha = false, hb = false;
            for (int v : FACES[f]) {
                if (v == a) ha = true;
                if (v == b) hb = true;
            }
            if (ha && hb) return true;
        }
        return false;
    }

    private float sx(int i, float dw) {
        return (clip[i].x / clip[i].w + 1f) * 0.5f * dw;
    }

    private float sy(int i, float dh) {
        return (1f - clip[i].y / clip[i].w) * 0.5f * dh;
    }

    private static void drawEdge(imgui.ImDrawList dl, org.joml.Vector4f a, org.joml.Vector4f b, float dw, float dh, int color, float width) {
        float near = 0.05f;
        float ax = a.x, ay = a.y, aw = a.w, bx = b.x, by = b.y, bw = b.w;
        if (aw < near && bw < near) return;
        if (aw < near) {
            float t = (near - aw) / (bw - aw);
            ax += (bx - ax) * t;
            ay += (by - ay) * t;
            aw = near;
        } else if (bw < near) {
            float t = (near - bw) / (aw - bw);
            bx += (ax - bx) * t;
            by += (ay - by) * t;
            bw = near;
        }
        dl.addLine((ax / aw + 1f) * 0.5f * dw, (1f - ay / aw) * 0.5f * dh, (bx / bw + 1f) * 0.5f * dw, (1f - by / bw) * 0.5f * dh, color, width);
    }

    public void saveSelectedBlocksToFile() {
        try {
            File dir = new File("config");
            if (!dir.exists()) dir.mkdirs();
            File file = new File(dir, "blockesp_meteor_selected.txt");
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                for (Block b : selectedBlocks) {
                    Identifier id = BuiltInRegistries.BLOCK.getKey(b);
                    if (id != null) {
                        writer.write(id.toString());
                        writer.newLine();
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    public void loadSelectedBlocksFromFile() {
        try {
            File file = new File("config", "blockesp_meteor_selected.txt");
            if (!file.exists()) return;
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String idStr = line.trim();
                    if (!idStr.isEmpty()) {
                        try {
                            Block b = BuiltInRegistries.BLOCK.get(Identifier.parse(idStr))
                                    .map(net.minecraft.core.Holder::value)
                                    .orElse(null);
                            if (b != null && b != Blocks.AIR) {
                                selectedBlocks.add(b);
                            }
                        } catch (Exception ignored) {}
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    private float getTickDelta(Object tickDeltaObj) {
        if (tickDeltaObj instanceof Float) return (Float) tickDeltaObj;
        for (java.lang.reflect.Method m : tickDeltaObj.getClass().getMethods()) {
            if (m.getReturnType() == float.class) {
                if (m.getParameterCount() == 1 && m.getParameterTypes()[0] == boolean.class) {
                    try { return (float) m.invoke(tickDeltaObj, true); } catch (Exception e) {}
                } else if (m.getParameterCount() == 0) {
                    String name = m.getName().toLowerCase();
                    if (name.contains("tick") || name.contains("delta") || name.contains("frame")) {
                        try { return (float) m.invoke(tickDeltaObj); } catch (Exception e) {}
                    }
                }
            }
        }
        return 1.0f;
    }
}