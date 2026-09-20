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

        sm.rSetting(new Setting("Range", this, 32.0, 8.0, 128.0, true));
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
        addDefaultBlock("minecraft:obsidian", new Color(160, 32, 240));

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

    @Override
    public void onRenderHUD(GuiGraphicsExtractor guiGraphics, Object tickCounterObj) {
        if (!isToggled() || mc.player == null || mc.level == null) {
            cachedBlocks.clear();
            return;
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCacheTick >= 250) {
            lastCacheTick = currentTime;
            if (selectedBlocks.isEmpty()) {
                cachedBlocks.clear();
            } else {
                Setting rangeSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Range");
                Setting colorSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Block Color");
                int range = rangeSetting != null ? (int) rangeSetting.getValDouble() : 32;
                Color defaultColor = colorSetting != null ? new Color(colorSetting.getValColor(), true) : new Color(0, 220, 255);

                BlockPos playerPos = mc.player.blockPosition();
                List<CachedBlock> newCache = new ArrayList<>();

                for (int x = -range; x <= range; x++) {
                    for (int y = -range; y <= range; y++) {
                        for (int z = -range; z <= range; z++) {
                            BlockPos pos = playerPos.offset(x, y, z);
                            BlockState state = mc.level.getBlockState(pos);
                            Block block = state.getBlock();

                            if (selectedBlocks.contains(block)) {
                                Color color = blockColorMap.getOrDefault(block, defaultColor);
                                newCache.add(new CachedBlock(pos, color));
                            }
                        }
                    }
                }

                cachedBlocks.clear();
                cachedBlocks.addAll(newCache);
            }
        }

        float partialTick = getTickDelta(tickCounterObj);

        Setting tracersSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Tracers");
        Setting fillSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Fill");
        Setting outlineSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Outline");

        boolean showTracers = tracersSetting != null && tracersSetting.getValBoolean();
        boolean doFill = fillSetting == null || fillSetting.getValBoolean();
        boolean doOutline = outlineSetting == null || outlineSetting.getValBoolean();

        if (!showTracers && !doFill && !doOutline) return;

        double screenCenterX = mc.getWindow().getGuiScaledWidth() / 2.0;
        double screenCenterY = mc.getWindow().getGuiScaledHeight() / 2.0;

        for (CachedBlock cb : cachedBlocks) {
            if (RenderUtils.project2D(cb.pos.getX() + 0.5, cb.pos.getY() + 0.5, cb.pos.getZ() + 0.5, partialTick, projVec)) {
                if (projVec.z > 0 && projVec.z < 1.0) {
                    if (showTracers) {
                        RenderUtils.drawLine2D(guiGraphics, screenCenterX, screenCenterY, projVec.x, projVec.y, cb.color);
                    }
                    if (doFill || doOutline) {
                        drawBlockBox(guiGraphics, cb.pos, cb.color, doFill, doOutline, partialTick);
                    }
                }
            }
        }
    }

    private void drawBlockBox(GuiGraphicsExtractor guiGraphics, BlockPos pos, Color color, boolean fill, boolean outline, float partialTick) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();

        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        boolean behind = true;

        for (int i = 0; i < 8; i++) {
            double cx = x + ((i & 1) == 0 ? 0 : 1);
            double cy = y + ((i & 2) == 0 ? 0 : 1);
            double cz = z + ((i & 4) == 0 ? 0 : 1);

            if (RenderUtils.project2D(cx, cy, cz, partialTick, boxProjBuffer[i])) {
                if (boxProjBuffer[i].z > 0 && boxProjBuffer[i].z < 1.0) {
                    behind = false;
                    double px = boxProjBuffer[i].x;
                    double py = boxProjBuffer[i].y;
                    minX = Math.min(minX, px);
                    minY = Math.min(minY, py);
                    maxX = Math.max(maxX, px);
                    maxY = Math.max(maxY, py);
                }
            }
        }
        if (behind) return;

        if (fill) {
            guiGraphics.fill((int)minX, (int)minY, (int)maxX, (int)maxY, new Color(color.getRed(), color.getGreen(), color.getBlue(), 40).getRGB());
        }
        if (outline) {
            int c = color.getRGB();
            guiGraphics.fill((int)minX, (int)minY, (int)maxX, (int)minY + 1, c);
            guiGraphics.fill((int)minX, (int)maxY, (int)maxX, (int)maxY + 1, c);
            guiGraphics.fill((int)minX, (int)minY, (int)minX + 1, (int)maxY, c);
            guiGraphics.fill((int)maxX, (int)minY, (int)maxX + 1, (int)maxY + 1, c);
        }
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