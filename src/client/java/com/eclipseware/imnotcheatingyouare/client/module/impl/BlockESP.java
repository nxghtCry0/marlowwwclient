package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.setting.SettingsManager;
import com.eclipseware.imnotcheatingyouare.client.utils.RenderUtils;
import imgui.ImDrawList;
import imgui.ImGui;
import imgui.ImGuiIO;
import java.awt.Color;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Predicate;
import net.minecraft.client.Camera;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

public class BlockESP extends Module {
    public record TrackedBlockInfo(String blockId, String displayName, Block block, int color) {
    }

    public record CatalogBlockInfo(String blockId, String path, String displayName, Block block, int defaultColor) {
    }

    private static final int[] COLOR_CYCLE = new int[]{
        0xFF00DCFF,
        0xFFA855F7,
        0xFF2ED89A,
        0xFFFF5C8A,
        0xFFFFAA2B,
        0xFF5CE1E6,
        0xFFFF5555,
        0xFF7CFC00
    };

    private final Set<Block> selectedBlocks = new CopyOnWriteArraySet<>();
    private final Map<Block, Color> blockColorMap = new ConcurrentHashMap<>();
    private volatile List<CatalogBlockInfo> cachedCatalog;

    private record Hit(BlockPos pos, int color, int openFaces) {
    }

    private final ExecutorService scanner = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Marlow BlockESP");
        t.setDaemon(true);
        return t;
    });
    private volatile List<Hit> hits = List.of();
    private volatile boolean scanning = false;
    private long lastScan = 0L;
    private final Matrix4f matrix = new Matrix4f();
    private final Vector4f[] clip = new Vector4f[8];

    private static final int[][] EDGES = {
        {0, 1}, {2, 3}, {4, 5}, {6, 7},
        {0, 2}, {1, 3}, {4, 6}, {5, 7},
        {0, 4}, {1, 5}, {2, 6}, {3, 7}
    };
    private static final int[][] FACES = {
        {0, 2, 6, 4}, {1, 5, 7, 3}, {0, 1, 5, 4},
        {2, 6, 7, 3}, {0, 1, 3, 2}, {4, 6, 7, 5}
    };
    private static final Direction[] FACE_DIRS = {
        Direction.WEST, Direction.EAST,
        Direction.DOWN, Direction.UP,
        Direction.NORTH, Direction.SOUTH
    };

    public BlockESP() {
        super("BlockESP", Category.Render, "Highlights selected blocks through terrain with custom colors and presets.");
        for (int i = 0; i < 8; i++) {
            clip[i] = new Vector4f();
        }

        ImnotcheatingyouareClient client = ImnotcheatingyouareClient.INSTANCE;
        SettingsManager sm = client.settingsManager;

        sm.rSetting(new Setting("Range", this, 48.0, 8.0, 128.0, true));
        sm.rSetting(new Setting("Max Blocks", this, 1500.0, 100.0, 5000.0, true));
        sm.rSetting(new Setting("Fill Opacity", this, 18.0, 0.0, 60.0, true));
        sm.rSetting(new Setting("Line Width", this, 1.5, 0.5, 4.0, false));
        sm.rSetting(new Setting("Fill", this, true));
        sm.rSetting(new Setting("Outline", this, true));
        sm.rSetting(new Setting("Tracers", this, false));
        sm.rSetting(new Setting("Block Color", this, new Color(0, 220, 255)));

        addDefaultBlock("minecraft:diamond_ore", new Color(0, 220, 255));
        addDefaultBlock("minecraft:deepslate_diamond_ore", new Color(0, 220, 255));
        addDefaultBlock("minecraft:ancient_debris", new Color(200, 120, 80));
        addDefaultBlock("minecraft:spawner", new Color(255, 60, 60));
        addDefaultBlock("minecraft:end_portal_frame", new Color(50, 205, 50));

        loadSelectedBlocksFromFile();
    }

    private void addDefaultBlock(String idStr, Color color) {
        Block block = resolveBlock(idStr);
        if (block != null && block != Blocks.AIR) {
            selectedBlocks.add(block);
            blockColorMap.put(block, color);
        }
    }

    public static Block resolveBlock(String idStr) {
        try {
            String trimmed = idStr.trim().toLowerCase(Locale.ROOT);
            String full = trimmed.indexOf(':') >= 0 ? trimmed : "minecraft:" + trimmed;
            Identifier id = Identifier.tryParse(full);
            if (id == null) {
                return null;
            }
            Optional<Holder.Reference<Block>> holderOpt = BuiltInRegistries.BLOCK.get(id);
            if (holderOpt.isPresent()) {
                Holder.Reference<Block> ref = holderOpt.get();
                return ref.value();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    public boolean isBlockSelected(Block block) {
        return selectedBlocks.contains(block);
    }

    public boolean isBlockIdSelected(String blockId) {
        Block block = resolveBlock(blockId);
        return block != null && selectedBlocks.contains(block);
    }

    public void setBlockSelected(Block block, boolean select) {
        if (block == null || block == Blocks.AIR) {
            return;
        }
        if (select) {
            selectedBlocks.add(block);
            blockColorMap.computeIfAbsent(block, b -> new Color(defaultColorForBlock(b), true));
        } else {
            selectedBlocks.remove(block);
        }
        lastScan = 0L;
        saveSelectedBlocksToFile();
    }

    public int getSelectedBlockCount() {
        return selectedBlocks.size();
    }

    public List<TrackedBlockInfo> getTrackedEntries() {
        List<TrackedBlockInfo> out = new ArrayList<>(selectedBlocks.size());
        int fallback = defaultBlockColorSetting();
        for (Block b : selectedBlocks) {
            Identifier id = BuiltInRegistries.BLOCK.getKey(b);
            if (id == null) {
                continue;
            }
            String idStr = id.toString();
            String disp = displayNameForPath(id.getPath());
            Color mapped = blockColorMap.get(b);
            int argb = mapped != null ? (mapped.getRGB() | 0xFF000000) : (fallback | 0xFF000000);
            out.add(new TrackedBlockInfo(idStr, disp, b, argb));
        }
        out.sort(Comparator.comparing(TrackedBlockInfo::displayName));
        return out;
    }

    public int cycleBlockColor(String blockId) {
        Block b = resolveBlock(blockId);
        if (b == null) {
            return 0xFF00DCFF;
        }
        Color cur = blockColorMap.get(b);
        int curRgb = cur != null ? (cur.getRGB() | 0xFF000000) : COLOR_CYCLE[0];
        int nextIdx = 0;
        for (int i = 0; i < COLOR_CYCLE.length; i++) {
            if (COLOR_CYCLE[i] == curRgb) {
                nextIdx = (i + 1) % COLOR_CYCLE.length;
                break;
            }
        }
        int nextArgb = COLOR_CYCLE[nextIdx];
        blockColorMap.put(b, new Color(nextArgb, true));
        lastScan = 0L;
        saveSelectedBlocksToFile();
        return nextArgb;
    }

    public void removeBlockById(String blockId) {
        Block b = resolveBlock(blockId);
        if (b != null) {
            selectedBlocks.remove(b);
            lastScan = 0L;
            saveSelectedBlocksToFile();
        }
    }

    public boolean toggleCatalogBlock(CatalogBlockInfo info) {
        Block b = info.block();
        if (selectedBlocks.contains(b)) {
            selectedBlocks.remove(b);
            lastScan = 0L;
            saveSelectedBlocksToFile();
            return false;
        }
        selectedBlocks.add(b);
        blockColorMap.putIfAbsent(b, new Color(info.defaultColor(), true));
        lastScan = 0L;
        saveSelectedBlocksToFile();
        return true;
    }

    public void clearSelectedBlocks() {
        selectedBlocks.clear();
        hits = List.of();
        lastScan = 0L;
        saveSelectedBlocksToFile();
    }

    public void applyPreset(String presetId) {
        selectedBlocks.clear();
        if ("pvp".equalsIgnoreCase(presetId)) {
            addDefaultBlock("minecraft:obsidian", new Color(168, 85, 247));
            addDefaultBlock("minecraft:crying_obsidian", new Color(192, 110, 255));
            addDefaultBlock("minecraft:respawn_anchor", new Color(255, 170, 43));
            addDefaultBlock("minecraft:cobweb", new Color(240, 244, 255));
            addDefaultBlock("minecraft:bedrock", new Color(140, 150, 165));
        } else if ("ores".equalsIgnoreCase(presetId)) {
            addDefaultBlock("minecraft:diamond_ore", new Color(0, 220, 255));
            addDefaultBlock("minecraft:deepslate_diamond_ore", new Color(0, 220, 255));
            addDefaultBlock("minecraft:ancient_debris", new Color(200, 120, 80));
            addDefaultBlock("minecraft:emerald_ore", new Color(46, 216, 154));
            addDefaultBlock("minecraft:deepslate_emerald_ore", new Color(46, 216, 154));
            addDefaultBlock("minecraft:gold_ore", new Color(255, 205, 60));
            addDefaultBlock("minecraft:deepslate_gold_ore", new Color(255, 205, 60));
        } else if ("base".equalsIgnoreCase(presetId)) {
            addDefaultBlock("minecraft:spawner", new Color(255, 60, 60));
            addDefaultBlock("minecraft:end_portal_frame", new Color(50, 205, 50));
            addDefaultBlock("minecraft:beacon", new Color(92, 225, 230));
            addDefaultBlock("minecraft:enchanting_table", new Color(168, 85, 247));
            addDefaultBlock("minecraft:ender_chest", new Color(138, 43, 226));
        }
        lastScan = 0L;
        saveSelectedBlocksToFile();
    }

    public List<CatalogBlockInfo> searchBlockCatalog(String query, int limit) {
        List<CatalogBlockInfo> all = ensureCatalogBuilt();
        String q = query != null ? query.trim().toLowerCase(Locale.ROOT) : "";
        List<CatalogBlockInfo> out = new ArrayList<>(Math.min(limit, 96));
        for (CatalogBlockInfo entry : all) {
            String lowerName = entry.displayName().toLowerCase(Locale.ROOT);
            if (q.isEmpty()
                || entry.path().contains(q)
                || entry.blockId().contains(q)
                || lowerName.contains(q)) {
                out.add(entry);
                if (out.size() >= limit) {
                    break;
                }
            }
        }
        return out;
    }

    private List<CatalogBlockInfo> ensureCatalogBuilt() {
        List<CatalogBlockInfo> local = cachedCatalog;
        if (local != null) {
            return local;
        }
        List<CatalogBlockInfo> built = new ArrayList<>(1024);
        for (Block block : BuiltInRegistries.BLOCK) {
            if (block == Blocks.AIR || block == Blocks.CAVE_AIR || block == Blocks.VOID_AIR) {
                continue;
            }
            Identifier id = BuiltInRegistries.BLOCK.getKey(block);
            if (id == null) {
                continue;
            }
            String idStr = id.toString();
            String path = id.getPath();
            String display = displayNameForPath(path);
            built.add(new CatalogBlockInfo(idStr, path, display, block, defaultColorForBlock(block)));
        }
        cachedCatalog = built;
        return built;
    }

    private static int defaultColorForBlock(Block block) {
        try {
            MapColor mcCol = block.defaultMapColor();
            if (mcCol != null && mcCol.col != 0) {
                return 0xFF000000 | (mcCol.col & 0x00FFFFFF);
            }
        } catch (Throwable ignored) {
        }
        return 0xFF00DCFF;
    }

    public static String displayNameForId(String blockId) {
        int colon = blockId.indexOf(':');
        String path = colon >= 0 ? blockId.substring(colon + 1) : blockId;
        return displayNameForPath(path);
    }

    private static String displayNameForPath(String path) {
        String spaced = path.replace('_', ' ');
        String[] words = spaced.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (w.isEmpty()) {
                continue;
            }
            sb.append(Character.toUpperCase(w.charAt(0)));
            sb.append(w.substring(1));
            sb.append(' ');
        }
        return sb.toString().trim();
    }

    private int defaultBlockColorSetting() {
        ImnotcheatingyouareClient client = ImnotcheatingyouareClient.INSTANCE;
        SettingsManager sm = client.settingsManager;
        Setting colorSetting = sm.getSettingByName(this, "Block Color");
        return colorSetting != null ? colorSetting.getValColor() : 0xFF00DCFF;
    }

    private double num(String name, double fallback) {
        ImnotcheatingyouareClient client = ImnotcheatingyouareClient.INSTANCE;
        SettingsManager sm = client.settingsManager;
        Setting s = sm.getSettingByName(this, name);
        return s != null ? s.getValDouble() : fallback;
    }

    private boolean bool(String name, boolean fallback) {
        ImnotcheatingyouareClient client = ImnotcheatingyouareClient.INSTANCE;
        SettingsManager sm = client.settingsManager;
        Setting s = sm.getSettingByName(this, name);
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
        if (scanning || now - lastScan < 400L) {
            return;
        }
        lastScan = now;
        scanning = true;
        final ClientLevel level = mc.level;
        final BlockPos center = mc.player.blockPosition();
        final int range = (int) num("Range", 48);
        final int max = (int) num("Max Blocks", 1500);
        final int defaultColor = defaultBlockColorSetting();
        final Set<Block> wanted = new HashSet<>(selectedBlocks);
        final Map<Block, Integer> colors = new HashMap<>();
        for (Map.Entry<Block, Color> e : blockColorMap.entrySet()) {
            Color c = e.getValue();
            colors.put(e.getKey(), c.getRGB());
        }
        scanner.execute(() -> {
            try {
                hits = scan(level, center, range, max, wanted, colors, defaultColor);
            } catch (Throwable ignored) {
            } finally {
                scanning = false;
            }
        });
    }

    private static List<Hit> scan(
        ClientLevel level,
        BlockPos center,
        int range,
        int max,
        Set<Block> wanted,
        Map<Block, Integer> colors,
        int defaultColor
    ) {
        List<Hit> found = new ArrayList<>();
        int rsq = range * range;
        int cx0 = (center.getX() - range) >> 4;
        int cx1 = (center.getX() + range) >> 4;
        int cz0 = (center.getZ() - range) >> 4;
        int cz1 = (center.getZ() + range) >> 4;
        Predicate<BlockState> pred = st -> wanted.contains(st.getBlock());
        BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos();
        ClientChunkCache chunkSource = level.getChunkSource();

        for (int cx = cx0; cx <= cx1; cx++) {
            for (int cz = cz0; cz <= cz1; cz++) {
                LevelChunk chunk = chunkSource.getChunk(cx, cz, ChunkStatus.FULL, false);
                if (chunk == null) {
                    continue;
                }
                LevelChunkSection[] sections = chunk.getSections();
                for (int i = 0; i < sections.length; i++) {
                    LevelChunkSection section = sections[i];
                    if (section == null || section.hasOnlyAir() || !section.maybeHas(pred)) {
                        continue;
                    }
                    int sy = chunk.getSectionYFromSectionIndex(i) << 4;
                    if (sy + 15 < center.getY() - range || sy > center.getY() + range) {
                        continue;
                    }
                    for (int y = 0; y < 16; y++) {
                        for (int z = 0; z < 16; z++) {
                            for (int x = 0; x < 16; x++) {
                                BlockState st = section.getBlockState(x, y, z);
                                Block b = st.getBlock();
                                if (!wanted.contains(b)) {
                                    continue;
                                }
                                int wx = (cx << 4) + x;
                                int wy = sy + y;
                                int wz = (cz << 4) + z;
                                int dx = wx - center.getX();
                                int dy = wy - center.getY();
                                int dz = wz - center.getZ();
                                if (dx * dx + dy * dy + dz * dz > rsq) {
                                    continue;
                                }
                                BlockPos pos = new BlockPos(wx, wy, wz);
                                int open = 0;
                                for (int d = 0; d < 6; d++) {
                                    probe.setWithOffset(pos, FACE_DIRS[d]);
                                    BlockState neighbor = level.getBlockState(probe);
                                    if (neighbor.getBlock() != b) {
                                        open |= 1 << d;
                                    }
                                }
                                if (open == 0) {
                                    continue;
                                }
                                found.add(new Hit(pos, colors.getOrDefault(b, defaultColor), open));
                            }
                        }
                    }
                }
            }
        }
        if (found.size() > max) {
            found.sort(Comparator.comparingDouble(h -> h.pos().distSqr(center)));
            found = new ArrayList<>(found.subList(0, max));
        }
        return found;
    }

    public void renderImGuiOverlay() {
        if (!isToggled() || mc.player == null || mc.level == null || mc.gameRenderer == null) {
            return;
        }
        scheduleScan();
        List<Hit> list = hits;
        if (list.isEmpty()) {
            return;
        }

        boolean fill = bool("Fill", true);
        boolean outline = bool("Outline", true);
        boolean tracers = bool("Tracers", false);
        float width = (float) num("Line Width", 1.5);
        int fillAlpha = (int) (num("Fill Opacity", 18) * 2.55);

        Camera camera = mc.gameRenderer.mainCamera();
        Vec3 cam = camera.position();
        camera.getViewRotationProjectionMatrix(matrix);
        ImGuiIO io = ImGui.getIO();
        float dw = io.getDisplaySizeX();
        float dh = io.getDisplaySizeY();
        ImDrawList dl = ImGui.getBackgroundDrawList();
        Vector4f center = new Vector4f();

        for (Hit h : list) {
            BlockPos p = h.pos();
            float bx = (float) (p.getX() - cam.x);
            float by = (float) (p.getY() - cam.y);
            float bz = (float) (p.getZ() - cam.z);
            boolean anyFront = false;
            for (int i = 0; i < 8; i++) {
                clip[i].set(bx + (i & 1), by + ((i >> 1) & 1), bz + ((i >> 2) & 1), 1f);
                matrix.transform(clip[i]);
                if (clip[i].w > 0.05f) {
                    anyFront = true;
                }
            }
            if (!anyFront) {
                continue;
            }
            int argb = h.color();
            int r = (argb >> 16) & 0xFF;
            int g = (argb >> 8) & 0xFF;
            int b = argb & 0xFF;
            int line = RenderUtils.toImGuiColor(r, g, b, 230);
            int shade = RenderUtils.toImGuiColor(r, g, b, fillAlpha);

            if (fill && fillAlpha > 0) {
                for (int f = 0; f < 6; f++) {
                    if ((h.openFaces() & (1 << f)) == 0) {
                        continue;
                    }
                    int[] q = FACES[f];
                    if (clip[q[0]].w <= 0.05f || clip[q[1]].w <= 0.05f || clip[q[2]].w <= 0.05f || clip[q[3]].w <= 0.05f) {
                        continue;
                    }
                    dl.addQuadFilled(
                        sx(q[0], dw), sy(q[0], dh),
                        sx(q[1], dw), sy(q[1], dh),
                        sx(q[2], dw), sy(q[2], dh),
                        sx(q[3], dw), sy(q[3], dh),
                        shade
                    );
                }
            }
            if (outline) {
                for (int[] e : EDGES) {
                    if (!edgeVisible(h.openFaces(), e[0], e[1])) {
                        continue;
                    }
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
            if ((open & (1 << f)) == 0) {
                continue;
            }
            boolean ha = false;
            boolean hb = false;
            for (int v : FACES[f]) {
                if (v == a) {
                    ha = true;
                }
                if (v == b) {
                    hb = true;
                }
            }
            if (ha && hb) {
                return true;
            }
        }
        return false;
    }

    private float sx(int i, float dw) {
        return (clip[i].x / clip[i].w + 1f) * 0.5f * dw;
    }

    private float sy(int i, float dh) {
        return (1f - clip[i].y / clip[i].w) * 0.5f * dh;
    }

    private static void drawEdge(ImDrawList dl, Vector4f a, Vector4f b, float dw, float dh, int color, float width) {
        float near = 0.05f;
        float ax = a.x;
        float ay = a.y;
        float aw = a.w;
        float bx = b.x;
        float by = b.y;
        float bw = b.w;
        if (aw < near && bw < near) {
            return;
        }
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
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File file = new File(dir, "blockesp_meteor_selected.txt");
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                for (Block b : selectedBlocks) {
                    Identifier id = BuiltInRegistries.BLOCK.getKey(b);
                    if (id != null) {
                        Color col = blockColorMap.get(b);
                        if (col != null) {
                            writer.write(id + "|" + (col.getRGB() & 0x00FFFFFF));
                        } else {
                            writer.write(id.toString());
                        }
                        writer.newLine();
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    public void loadSelectedBlocksFromFile() {
        try {
            File file = new File("config", "blockesp_meteor_selected.txt");
            if (!file.exists()) {
                return;
            }
            selectedBlocks.clear();
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    if (trimmed.isEmpty()) {
                        continue;
                    }
                    int pipe = trimmed.indexOf('|');
                    String idPart = pipe > 0 ? trimmed.substring(0, pipe) : trimmed;
                    Block b = resolveBlock(idPart);
                    if (b != null && b != Blocks.AIR) {
                        selectedBlocks.add(b);
                        if (pipe > 0) {
                            try {
                                int rgb = Integer.parseInt(trimmed.substring(pipe + 1));
                                blockColorMap.put(b, new Color((0xFF << 24) | (rgb & 0x00FFFFFF), true));
                            } catch (NumberFormatException ignored) {
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }
}
