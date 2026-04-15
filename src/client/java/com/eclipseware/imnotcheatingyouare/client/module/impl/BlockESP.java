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
    private final Set<String> selectedBlocks = new HashSet<>();
    
    public BlockESP() {
        super("BlockESP", Category.Render, "Highlights specific blocks with customizable colors and tracers.");
        HudRenderCallback.EVENT.register((guiGraphics, tickCounter) -> onRenderHUD(guiGraphics, tickCounter));
    }

    private void onRenderHUD(GuiGraphics guiGraphics, Object tickCounterObj) {
        if (!isToggled() || mc.player == null || mc.level == null) return;

        Setting fpsSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "FPS");
        double targetFPS = fpsSetting != null ? fpsSetting.getValDouble() : 30;
        int interval = Math.max(1, (int)(60.0 / targetFPS));
        if (mc.player.tickCount % interval != 0) return;

        Setting rangeSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Range");
        int range = rangeSetting != null ? (int) rangeSetting.getValDouble() : 32;
        
        Setting tracersSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Tracers");
        Setting fillSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Fill");
        Setting outlineSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Outline");
        
        boolean showTracers = tracersSetting != null && tracersSetting.getValBoolean();
        boolean doFill = fillSetting != null && fillSetting.getValBoolean();
        boolean doOutline = outlineSetting != null && outlineSetting.getValBoolean();
        
        if (!showTracers && !doFill && !doOutline) return;

        BlockPos playerPos = mc.player.blockPosition();
        
        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    BlockPos pos = playerPos.offset(x, y, z);
                    BlockState state = mc.level.getBlockState(pos);
                    Block block = state.getBlock();
                    String blockName = BuiltInRegistries.BLOCK.getKey(block).getPath();
                    
                    if (!selectedBlocks.contains(blockName)) continue;
                    
                    Color color = getColorForBlock(blockName);
                    Vector3d screenPos = RenderUtils.project2D(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 1.0f);
                    
                    if (screenPos != null && screenPos.z > 0 && screenPos.z < 1.0) {
                        if (showTracers) {
                            RenderUtils.drawLine2D(guiGraphics,
                                mc.getWindow().getGuiScaledWidth() / 2.0,
                                mc.getWindow().getGuiScaledHeight() / 2.0,
                                screenPos.x, screenPos.y, color);
                        }
                        if (doFill || doOutline) {
                            drawBlockBox(guiGraphics, pos, color, doFill, doOutline);
                        }
                    }
                }
            }
        }
    }

    private void drawBlockBox(GuiGraphics guiGraphics, BlockPos pos, Color color, boolean fill, boolean outline) {
        Vector3d[] corners = new Vector3d[8];
        corners[0] = RenderUtils.project2D(pos.getX(), pos.getY(), pos.getZ(), 1.0f);
        corners[1] = RenderUtils.project2D(pos.getX() + 1, pos.getY(), pos.getZ(), 1.0f);
        corners[2] = RenderUtils.project2D(pos.getX(), pos.getY() + 1, pos.getZ(), 1.0f);
        corners[3] = RenderUtils.project2D(pos.getX() + 1, pos.getY() + 1, pos.getZ(), 1.0f);
        corners[4] = RenderUtils.project2D(pos.getX(), pos.getY(), pos.getZ() + 1, 1.0f);
        corners[5] = RenderUtils.project2D(pos.getX() + 1, pos.getY(), pos.getZ() + 1, 1.0f);
        corners[6] = RenderUtils.project2D(pos.getX(), pos.getY() + 1, pos.getZ() + 1, 1.0f);
        corners[7] = RenderUtils.project2D(pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1, 1.0f);

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

    public void addBlock(String blockName) {
        if (selectedBlocks.add(blockName)) {
            registerBlockSettings(blockName);
        }
    }

    public void removeBlock(String blockName) {
        selectedBlocks.remove(blockName);
    }

    public boolean hasBlock(String blockName) {
        return selectedBlocks.contains(blockName);
    }

    private void registerBlockSettings(String blockName) {
        SettingsManager sm = ImnotcheatingyouareClient.INSTANCE.settingsManager;
        if (sm.getSettingByName(this, blockName + " R") == null) {
            sm.rSetting(new Setting(blockName + " R", this, 255.0, 0.0, 255.0, true));
            sm.rSetting(new Setting(blockName + " G", this, 255.0, 0.0, 255.0, true));
            sm.rSetting(new Setting(blockName + " B", this, 255.0, 0.0, 255.0, true));
        }
    }

    @Override
    public void onDisable() {
        selectedBlocks.clear();
    }

    public Set<String> getSelectedBlocks() {
        return new HashSet<>(selectedBlocks);
    }
}