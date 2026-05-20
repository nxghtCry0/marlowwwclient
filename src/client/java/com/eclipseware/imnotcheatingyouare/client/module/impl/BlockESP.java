package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.setting.SettingsManager;
import com.eclipseware.imnotcheatingyouare.client.utils.RenderUtils;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3d;

import java.awt.*;
import java.util.HashSet;
import java.util.Set;

public class BlockESP extends Module {

    private static final record CachedBlock(BlockPos pos, Color color, String name) {}
    private final Set<String> defaultBlocks = new HashSet<>();
    private final Set<Block> resolvedTargetBlocks = new HashSet<>();
    private final java.util.List<CachedBlock> cachedBlocks = new java.util.concurrent.CopyOnWriteArrayList<>();
    private long lastCacheTick = 0;

    private static final Vector3d projVec = new Vector3d();
    private static final Vector3d[] boxProjBuffer = new Vector3d[8];
    static {
        for (int i = 0; i < 8; i++) {
            boxProjBuffer[i] = new Vector3d();
        }
    }

    public BlockESP() {
        super("BlockESP", Category.Render, "Highlights target blocks.");

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

    private void updateResolvedBlocks() {
        resolvedTargetBlocks.clear();
        for (String id : defaultBlocks) {
            if (isBlockEnabled(id)) {
                Block block = BuiltInRegistries.BLOCK.get(Identifier.parse("minecraft:" + id))
                        .map(net.minecraft.core.Holder::value)
                        .orElse(null);
                if (block != null) {
                    resolvedTargetBlocks.add(block);
                }
            }
        }
    }

    @Override
    public void onTick() {
        if (!isToggled() || mc.player == null || mc.level == null) return;
        
        Setting fpsSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "FPS");
        double targetFPS = fpsSetting != null ? fpsSetting.getValDouble() : 30;
        int interval = Math.max(1, (int)(20.0 / targetFPS)); 
        
        if (mc.player.tickCount - lastCacheTick < 10) return;
        lastCacheTick = mc.player.tickCount;

        updateResolvedBlocks();
        if (resolvedTargetBlocks.isEmpty()) {
            cachedBlocks.clear();
            return;
        }

        Setting rangeSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Range");
        int range = rangeSetting != null ? (int) rangeSetting.getValDouble() : 32;

        BlockPos playerPos = mc.player.blockPosition();
        java.util.List<CachedBlock> newCache = new java.util.ArrayList<>();

        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    BlockPos pos = playerPos.offset(x, y, z);
                    BlockState state = mc.level.getBlockState(pos);
                    Block block = state.getBlock();
                    
                    if (resolvedTargetBlocks.contains(block)) {
                        String blockName = BuiltInRegistries.BLOCK.getKey(block).getPath();
                        Color color = getColorForBlock(blockName);
                        newCache.add(new CachedBlock(pos, color, blockName));
                    }
                }
            }
        }
        
        cachedBlocks.clear();
        cachedBlocks.addAll(newCache);
    }

    @Override
    public void onRenderHUD(GuiGraphicsExtractor guiGraphics, Object tickCounterObj) {
        if (!isToggled() || mc.player == null || mc.level == null) return;
        
        float partialTick = getTickDelta(tickCounterObj);
        
        Setting tracersSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Tracers");
        Setting fillSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Fill");
        Setting outlineSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Outline");
        
        boolean showTracers = tracersSetting != null && tracersSetting.getValBoolean();
        boolean doFill = fillSetting != null && fillSetting.getValBoolean();
        boolean doOutline = outlineSetting != null && outlineSetting.getValBoolean();
        
        if (!showTracers && !doFill && !doOutline) return;
        
        for (CachedBlock cb : cachedBlocks) {
            if (!isBlockEnabled(cb.name)) continue;
            
            if (RenderUtils.project2D(cb.pos.getX() + 0.5, cb.pos.getY() + 0.5, cb.pos.getZ() + 0.5, partialTick, projVec)) {
                if (projVec.z > 0 && projVec.z < 1.0) {
                    if (showTracers) {
                        RenderUtils.drawLine2D(guiGraphics,
                            mc.getWindow().getGuiScaledWidth() / 2.0,
                            mc.getWindow().getGuiScaledHeight() / 2.0,
                            projVec.x, projVec.y, cb.color);
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