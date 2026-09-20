package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.utils.RenderUtils;
import imgui.ImDrawList;
import imgui.ImGui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ChestType;
import org.joml.Vector3d;

import java.awt.Color;
import java.util.List;

public class StorageESP extends Module {
    private static final record CachedBlock(BlockPos pos, Color color) {}
    private final List<CachedBlock> cache = new java.util.ArrayList<>();
    private long lastUpdateTimeMs = 0L;

    public StorageESP() {
        super("StorageESP", Category.Render, "Highlights storage blocks like chests, barrels, and shulker boxes via ImGui.");
    }

    private static final Vector3d projVec = new Vector3d();
    private static final Vector3d[] storageProjBuffer = new Vector3d[8];
    static {
        for (int i = 0; i < 8; i++) {
            storageProjBuffer[i] = new Vector3d();
        }
    }

    @Override
    public void onRenderHUD(GuiGraphicsExtractor guiGraphics, Object tickDeltaObj) {
    }

    public void renderImGuiOverlay() {
        if (!isToggled() || mc.player == null || mc.level == null) {
            cache.clear();
            return;
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastUpdateTimeMs >= 200) {
            lastUpdateTimeMs = currentTime;
            cache.clear();

            Setting chestSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Chest");
            Setting barrelSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Barrel");
            Setting shulkerSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Shulker Box");
            Setting enderChestSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Ender Chest");
            Setting trappedChestSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Trapped Chest");
            Setting hopperSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Hopper");
            Setting dispenserSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Dispenser");
            Setting dropperSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Dropper");
            Setting furnacesSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Furnaces");

            Setting rangeSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Range");
            int range = rangeSetting != null ? (int) rangeSetting.getValDouble() : 32;

            BlockPos playerPos = mc.player.blockPosition();
            net.minecraft.world.level.ChunkPos playerChunk = new net.minecraft.world.level.ChunkPos(playerPos.getX() >> 4, playerPos.getZ() >> 4);
            int chunkRange = (range >> 4) + 1;

            if (mc.level.getChunkSource() != null) {
                for (int cx = playerChunk.x() - chunkRange; cx <= playerChunk.x() + chunkRange; cx++) {
                    for (int cz = playerChunk.z() - chunkRange; cz <= playerChunk.z() + chunkRange; cz++) {
                        net.minecraft.world.level.chunk.LevelChunk chunk = mc.level.getChunkSource().getChunk(cx, cz, false);
                        if (chunk == null || chunk.isEmpty()) continue;

                        for (BlockEntity be : chunk.getBlockEntities().values()) {
                            BlockPos pos = be.getBlockPos();
                            if (mc.player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= range * range) {
                                Color color = getColorForEntity(be, chestSetting, barrelSetting, shulkerSetting,
                                    enderChestSetting, trappedChestSetting, hopperSetting, dispenserSetting, dropperSetting, furnacesSetting);
                                if (color != null) cache.add(new CachedBlock(pos, color));
                            }
                        }
                    }
                }
            }
        }

        float partialTick = mc.getDeltaTracker() != null ? mc.getDeltaTracker().getGameTimeDeltaPartialTick(true) : 1.0f;

        Setting tracersSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Tracers");
        Setting fillSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Fill");
        Setting outlineSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Outline");
        boolean showTracers = tracersSetting != null && tracersSetting.getValBoolean();
        boolean doFill = fillSetting == null || fillSetting.getValBoolean();
        boolean doOutline = outlineSetting == null || outlineSetting.getValBoolean();

        if (!showTracers && !doFill && !doOutline) return;

        float displayWidth = ImGui.getIO().getDisplaySizeX();
        float displayHeight = ImGui.getIO().getDisplaySizeY();
        float centerX = displayWidth / 2.0f;
        float centerY = displayHeight / 2.0f;
        ImDrawList drawList = ImGui.getForegroundDrawList();

        for (CachedBlock cb : cache) {
            if (RenderUtils.project2DImGui(cb.pos.getX() + 0.5, cb.pos.getY() + 0.5, cb.pos.getZ() + 0.5, partialTick, projVec)) {
                if (projVec.z > 0 && projVec.z < 1.0 && Double.isFinite(projVec.x) && Double.isFinite(projVec.y)) {
                    int colInt = RenderUtils.toImGuiColor(cb.color, 1.0f);
                    if (showTracers) drawList.addLine(centerX, centerY, (float) projVec.x, (float) projVec.y, colInt, 1.2f);
                    if (doFill || doOutline) drawImGuiStorageBox(drawList, cb.pos, cb.color, doFill, doOutline, partialTick);
                }
            }
        }
    }

    private Color getColorForEntity(BlockEntity entity, Setting chestSetting, Setting barrelSetting, Setting shulkerSetting,
                                    Setting enderChestSetting, Setting trappedChestSetting, Setting hopperSetting,
                                    Setting dispenserSetting, Setting dropperSetting, Setting furnacesSetting) {
        if (entity instanceof ChestBlockEntity) {
            BlockState state = mc.level.getBlockState(entity.getBlockPos());
            if (trappedChestSetting != null && trappedChestSetting.getValBoolean() &&
                state.hasProperty(BlockStateProperties.CHEST_TYPE) &&
                state.getValue(BlockStateProperties.CHEST_TYPE) != ChestType.SINGLE) {
                return getColorFromSetting("Trapped Chest Color");
            } else if (chestSetting != null && chestSetting.getValBoolean()) {
                return getColorFromSetting("Chest Color");
            }
        } else if (entity instanceof BarrelBlockEntity && barrelSetting != null && barrelSetting.getValBoolean()) {
            return getColorFromSetting("Barrel Color");
        } else if (entity instanceof ShulkerBoxBlockEntity && shulkerSetting != null && shulkerSetting.getValBoolean()) {
            return getColorFromSetting("Shulker Color");
        } else if (entity instanceof EnderChestBlockEntity && enderChestSetting != null && enderChestSetting.getValBoolean()) {
            return getColorFromSetting("Ender Chest Color");
        } else if (entity instanceof HopperBlockEntity && hopperSetting != null && hopperSetting.getValBoolean()) {
            return getColorFromSetting("Hopper Color");
        } else if (entity instanceof DispenserBlockEntity && dispenserSetting != null && dispenserSetting.getValBoolean()) {
            return getColorFromSetting("Dispenser Color");
        } else if (entity instanceof DropperBlockEntity && dropperSetting != null && dropperSetting.getValBoolean()) {
            return getColorFromSetting("Dropper Color");
        } else if ((entity instanceof FurnaceBlockEntity || entity instanceof BlastFurnaceBlockEntity ||
                   entity instanceof SmokerBlockEntity) && furnacesSetting != null && furnacesSetting.getValBoolean()) {
            return getColorFromSetting("Furnace Color");
        }
        return null;
    }

    private void drawImGuiStorageBox(ImDrawList drawList, BlockPos pos, Color color, boolean fill, boolean outline, float partialTick) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();

        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        int validCount = 0;

        for (int i = 0; i < 8; i++) {
            double cx = x + ((i & 1) == 0 ? 0 : 1);
            double cy = y + ((i & 2) == 0 ? 0 : 1);
            double cz = z + ((i & 4) == 0 ? 0 : 1);

            if (RenderUtils.project2DImGui(cx, cy, cz, partialTick, storageProjBuffer[i])) {
                validCount++;
                double px = storageProjBuffer[i].x;
                double py = storageProjBuffer[i].y;
                minX = Math.min(minX, px);
                minY = Math.min(minY, py);
                maxX = Math.max(maxX, px);
                maxY = Math.max(maxY, py);
            }
        }
        if (validCount == 0) return;
        if (!Double.isFinite(minX) || !Double.isFinite(minY) || !Double.isFinite(maxX) || !Double.isFinite(maxY)) return;

        float ix = (float) Math.floor(minX);
        float iy = (float) Math.floor(minY);
        float ix2 = (float) Math.ceil(maxX);
        float iy2 = (float) Math.ceil(maxY);

        if (ix2 <= ix || iy2 <= iy) return;

        if (fill) {
            int fillColor = RenderUtils.toImGuiColor(color, 0.25f);
            drawList.addRectFilled(ix, iy, ix2, iy2, fillColor, 2.0f);
        }
        if (outline) {
            int outlineColor = RenderUtils.toImGuiColor(color, 0.9f);
            drawList.addRect(ix, iy, ix2, iy2, outlineColor, 2.0f, 0, 1.2f);
        }
    }

    private Color getColorFromSetting(String settingName) {
        Setting rSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, settingName + " R");
        Setting gSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, settingName + " G");
        Setting bSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, settingName + " B");

        int r = rSetting != null ? (int) rSetting.getValDouble() : 255;
        int g = gSetting != null ? (int) gSetting.getValDouble() : 255;
        int b = bSetting != null ? (int) bSetting.getValDouble() : 255;

        return new Color(r, g, b);
    }
}