package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.utils.RenderUtils;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ChestType;
import org.joml.Vector3d;

import java.awt.*;
import java.util.List;

public class StorageESP extends Module {
    private static final record CachedBlock(BlockPos pos, Color color) {}
    private final List<CachedBlock> cache = new java.util.ArrayList<>();
    private int lastUpdateTick = -999;
    public StorageESP() {
        super("StorageESP", Category.Render, "Highlights storage blocks like chests, barrels, and shulker boxes.");
        HudRenderCallback.EVENT.register((guiGraphics, tickCounter) -> onRenderHUD(guiGraphics, tickCounter));
    }

    private void onRenderHUD(GuiGraphics guiGraphics, Object tickDeltaObj) {
        if (!isToggled() || mc.player == null || mc.level == null) {
            cache.clear();
            return;
        }

        float partialTick = getTickDelta(tickDeltaObj);

        Setting tracersSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Tracers");
        Setting fillSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Fill");
        Setting outlineSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Outline");
        boolean showTracers = tracersSetting != null && tracersSetting.getValBoolean();
        boolean doFill = fillSetting != null && fillSetting.getValBoolean();
        boolean doOutline = outlineSetting != null && outlineSetting.getValBoolean();
        if (!showTracers && !doFill && !doOutline) return;

        Setting fpsSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "FPS");
        double targetFPS = fpsSetting != null ? fpsSetting.getValDouble() : 30;
        int interval = Math.max(1, (int)(60.0 / targetFPS));

        if (mc.player.tickCount - lastUpdateTick >= interval) {
            lastUpdateTick = mc.player.tickCount;
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
            net.minecraft.world.level.ChunkPos playerChunk = new net.minecraft.world.level.ChunkPos(playerPos);
            int chunkRange = (range >> 4) + 1;

            if (mc.level.getChunkSource() != null) {
                for (int cx = playerChunk.x - chunkRange; cx <= playerChunk.x + chunkRange; cx++) {
                    for (int cz = playerChunk.z - chunkRange; cz <= playerChunk.z + chunkRange; cz++) {
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

        double screenCenterX = mc.getWindow().getGuiScaledWidth() / 2.0;
        double screenCenterY = mc.getWindow().getGuiScaledHeight() / 2.0;

        for (CachedBlock cb : cache) {
            Vector3d screenPos = RenderUtils.project2D(cb.pos.getX() + 0.5, cb.pos.getY() + 0.5, cb.pos.getZ() + 0.5, partialTick);
            if (screenPos != null && screenPos.z > 0 && screenPos.z < 1.0) {
                if (showTracers) RenderUtils.drawLine2D(guiGraphics, screenCenterX, screenCenterY, screenPos.x, screenPos.y, cb.color);
                if (doFill || doOutline) drawStorageBox(guiGraphics, cb.pos, cb.color, doFill, doOutline, partialTick);
            }
        }
    }

    private Color getColorForEntity(BlockEntity entity, Setting chestSetting, Setting barrelSetting, Setting shulkerSetting,
                                    Setting enderChestSetting, Setting trappedChestSetting, Setting hopperSetting,
                                    Setting dispenserSetting, Setting dropperSetting, Setting furnacesSetting) {
        if (entity instanceof ChestBlockEntity chest) {
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

    private void drawStorageBox(GuiGraphics guiGraphics, BlockPos pos, Color color, boolean fill, boolean outline, float partialTick) {
        Vector3d[] corners = new Vector3d[8];
        corners[0] = RenderUtils.project2D(pos.getX(), pos.getY(), pos.getZ(), partialTick);
        corners[1] = RenderUtils.project2D(pos.getX() + 1, pos.getY(), pos.getZ(), partialTick);
        corners[2] = RenderUtils.project2D(pos.getX(), pos.getY() + 1, pos.getZ(), partialTick);
        corners[3] = RenderUtils.project2D(pos.getX() + 1, pos.getY() + 1, pos.getZ(), partialTick);
        corners[4] = RenderUtils.project2D(pos.getX(), pos.getY(), pos.getZ() + 1, partialTick);
        corners[5] = RenderUtils.project2D(pos.getX() + 1, pos.getY(), pos.getZ() + 1, partialTick);
        corners[6] = RenderUtils.project2D(pos.getX(), pos.getY() + 1, pos.getZ() + 1, partialTick);
        corners[7] = RenderUtils.project2D(pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1, partialTick);

        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE, maxY = Double.MIN_VALUE;
        boolean behind = true;
        for (Vector3d v : corners) {
            if (v != null && v.z > 0 && v.z < 1.0) {
                behind = false;
                minX = Math.min(minX, v.x); minY = Math.min(minY, v.y);
                maxX = Math.max(maxX, v.x); maxY = Math.max(maxY, v.y);
            }
        }
        if (behind) return;

        if (fill) {
            guiGraphics.fill((int)minX, (int)minY, (int)maxX, (int)maxY,
                new Color(color.getRed(), color.getGreen(), color.getBlue(), 35).getRGB());
        }
        if (outline) {
            RenderUtils.drawCornerMarks(guiGraphics, minX, minY, maxX, maxY, color);
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