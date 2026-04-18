package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.setting.SettingsManager;
import com.eclipseware.imnotcheatingyouare.client.utils.RenderUtils;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3d;

import java.awt.*;
import java.util.HashSet;
import java.util.Set;

public class BlockESP extends Module {

    private final Set<String> defaultBlocks = new HashSet<>();

    public BlockESP() {
        super("BlockESP", Category.Render, "Highlights target blocks.");
        HudRenderCallback.EVENT.register((guiGraphics, tickCounter) -> onRenderHUD(guiGraphics, tickCounter));

        addDefault("obsidian");
        addDefault("bedrock");
        addDefault("diamond_ore");
        addDefault("ancient_debris");
        addDefault("spawner");
        addDefault("end_portal_frame");
    }

    private void addDefault(String id) {
        defaultBlocks.add(id);
        SettingsManager sm = ImnotcheatingyouareClient.INSTANCE.settingsManager;
        if (sm.getSettingByName(this, "Find " + id) == null) {
            sm.rSetting(new Setting("Find " + id, this, false));
            sm.rSetting(new Setting(id + " R", this, 255.0, 0.0, 255.0, true));
            sm.rSetting(new Setting(id + " G", this, 255.0, 0.0, 255.0, true));
            sm.rSetting(new Setting(id + " B", this, 255.0, 0.0, 255.0, true));
        }
    }

    private boolean isBlockEnabled(String id) {
        Setting s = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Find " + id);
        return s != null && s.getValBoolean();
    }

    private final java.util.List<BlockPos> cachedBlocks = new java.util.concurrent.CopyOnWriteArrayList<>();
    private long lastCacheTick = 0;

    @Override
    public void onTick() {
        if (!isToggled() || mc.player == null || mc.level == null) return;
        
        Setting fpsSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "FPS");
        double targetFPS = fpsSetting != null ? fpsSetting.getValDouble() : 30;
        int interval = Math.max(1, (int)(20.0 / targetFPS)); 
        
        if (mc.player.tickCount - lastCacheTick < 10) return;
        lastCacheTick = mc.player.tickCount;

        Setting rangeSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Range");
        int range = rangeSetting != null ? (int) rangeSetting.getValDouble() : 32;

        BlockPos playerPos = mc.player.blockPosition();
        java.util.List<BlockPos> newCache = new java.util.ArrayList<>();

        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    BlockPos pos = playerPos.offset(x, y, z);
                    BlockState state = mc.level.getBlockState(pos);
                    Block block = state.getBlock();
                    String blockName = BuiltInRegistries.BLOCK.getKey(block).getPath();
                    
                    if (defaultBlocks.contains(blockName) && isBlockEnabled(blockName)) {
                        newCache.add(pos);
                    }
                }
            }
        }
        
        cachedBlocks.clear();
        cachedBlocks.addAll(newCache);
    }

    private void onRenderHUD(GuiGraphics guiGraphics, Object tickCounterObj) {
        if (!isToggled() || mc.player == null || mc.level == null) return;
        
        float partialTick = getTickDelta(tickCounterObj);
        
        Setting tracersSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Tracers");
        Setting fillSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Fill");
        Setting outlineSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Outline");
        
        boolean showTracers = tracersSetting != null && tracersSetting.getValBoolean();
        boolean doFill = fillSetting != null && fillSetting.getValBoolean();
        boolean doOutline = outlineSetting != null && outlineSetting.getValBoolean();
        
        if (!showTracers && !doFill && !doOutline) return;
        
        for (BlockPos pos : cachedBlocks) {
            BlockState state = mc.level.getBlockState(pos);
            String blockName = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
            if (!defaultBlocks.contains(blockName) || !isBlockEnabled(blockName)) continue;
            
            Color color = getColorForBlock(blockName);
            Vector3d screenPos = RenderUtils.project2D(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, partialTick);
            
            if (screenPos != null && screenPos.z > 0 && screenPos.z < 1.0) {
                if (showTracers) {
                    RenderUtils.drawLine2D(guiGraphics,
                        mc.getWindow().getGuiScaledWidth() / 2.0,
                        mc.getWindow().getGuiScaledHeight() / 2.0,
                        screenPos.x, screenPos.y, color);
                }
                if (doFill || doOutline) {
                    drawBlockBox(guiGraphics, pos, color, doFill, doOutline, partialTick);
                }
            }
        }
    }

    private void drawBlockBox(GuiGraphics guiGraphics, BlockPos pos, Color color, boolean fill, boolean outline, float partialTick) {
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

    private Color getColorForBlock(String blockName) {
        Setting rSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, blockName + " R");
        Setting gSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, blockName + " G");
        Setting bSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, blockName + " B");
        
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