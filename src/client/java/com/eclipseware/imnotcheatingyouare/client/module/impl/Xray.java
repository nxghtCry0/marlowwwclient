package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.setting.SettingsManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.Shapes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Xray extends Module {
    public static Xray INSTANCE;

    public static final List<Block> DEFAULT_ORES = List.of(
            Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE,
            Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE,
            Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE,
            Blocks.LAPIS_ORE, Blocks.DEEPSLATE_LAPIS_ORE,
            Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE,
            Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE,
            Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE,
            Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE,
            Blocks.NETHER_GOLD_ORE, Blocks.NETHER_QUARTZ_ORE,
            Blocks.ANCIENT_DEBRIS, Blocks.CHEST, Blocks.TRAPPED_CHEST,
            Blocks.ENDER_CHEST, Blocks.SPAWNER
    );

    private final Set<Block> whitelistedBlocks = new HashSet<>(DEFAULT_ORES);
    private final Set<String> customBlocks = new HashSet<>();

    public Xray() {
        super("Xray", Category.Render, "Only renders specified blocks. Meteor-style ore and fluid filtering.");
        INSTANCE = this;

        SettingsManager sm = ImnotcheatingyouareClient.INSTANCE.settingsManager;
        if (sm.getSettingByName(this, "Exposed Only") == null) {
            sm.rSetting(new Setting("Exposed Only", this, false));
        }
        if (sm.getSettingByName(this, "Opacity") == null) {
            sm.rSetting(new Setting("Opacity", this, 25, 0, 255, true));
        }
        if (sm.getSettingByName(this, "Fluid Opacity Mode") == null) {
            sm.rSetting(new Setting("Fluid Opacity Mode", this, "Both", new ArrayList<>(Arrays.asList("None", "Water", "Lava", "Both"))));
        }

        for (Block block : DEFAULT_ORES) {
            String id = BuiltInRegistries.BLOCK.getKey(block).getPath();
            customBlocks.add(id);
            if (sm.getSettingByName(this, "Show " + id) == null) {
                sm.rSetting(new Setting("Show " + id, this, true));
            }
        }
    }

    public boolean isExposedOnly() {
        Setting s = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Exposed Only");
        return s != null && s.getValBoolean();
    }

    public int getOpacity() {
        Setting s = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Opacity");
        return s != null ? (int) s.getValDouble() : 25;
    }

    public String getFluidOpacityMode() {
        Setting s = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Fluid Opacity Mode");
        return s != null ? s.getValString() : "Both";
    }

    public boolean isWhitelisted(Block block) {
        String id = BuiltInRegistries.BLOCK.getKey(block).getPath();
        Setting s = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Show " + id);
        if (s != null) {
            return s.getValBoolean();
        }
        return whitelistedBlocks.contains(block) || id.contains("ore");
    }

    public boolean isImportantBlock(String blockName) {
        Setting s = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Show " + blockName);
        if (s != null) return s.getValBoolean();
        return blockName.contains("ore") || customBlocks.contains(blockName);
    }

    public boolean isImportantBlock(BlockState state, BlockGetter level, BlockPos pos) {
        if (!isWhitelisted(state.getBlock())) {
            return false;
        }
        if (isExposedOnly()) {
            return isExposed(level, pos);
        }
        return true;
    }

    public boolean isBlocked(Block block, BlockPos pos, BlockGetter level) {
        if (!isWhitelisted(block)) {
            return true;
        }
        if (isExposedOnly() && pos != null && level != null && !isExposed(level, pos)) {
            return true;
        }
        return false;
    }

    public static final ThreadLocal<Boolean> IS_CHECKING_EXPOSED = ThreadLocal.withInitial(() -> false);

    public boolean isExposed(BlockGetter level, BlockPos pos) {
        if (level == null || pos == null) return true;
        IS_CHECKING_EXPOSED.set(true);
        try {
            for (Direction dir : Direction.values()) {
                BlockPos neighbor = pos.relative(dir);
                BlockState neighborState = level.getBlockState(neighbor);
                if (neighborState != null && (neighborState.isAir() || !neighborState.isSolidRender())) {
                    return true;
                }
            }
            return false;
        } finally {
            IS_CHECKING_EXPOSED.set(false);
        }
    }

    public boolean shouldApplyFluidOpacity(FluidState state) {
        String mode = getFluidOpacityMode();
        switch (mode) {
            case "Water":
                return state.is(FluidTags.WATER);
            case "Lava":
                return state.is(FluidTags.LAVA);
            case "Both":
                return state.is(FluidTags.WATER) || state.is(FluidTags.LAVA);
            case "None":
            default:
                return false;
        }
    }

    public int getAlpha(BlockState state, BlockPos pos, BlockGetter level) {
        if (!isToggled()) return -1;
        if (isBlocked(state.getBlock(), pos, level)) {
            return getOpacity();
        }
        return 255;
    }

    public int getFluidAlpha(FluidState state, BlockPos pos, BlockGetter level) {
        if (!isToggled()) return -1;
        Block block = state.createLegacyBlock().getBlock();
        if (shouldApplyFluidOpacity(state) && isBlocked(block, pos, level)) {
            return getOpacity();
        }
        return -1;
    }

    public boolean modifyDrawSide(BlockState state, BlockGetter view, BlockPos pos, Direction facing, boolean returns) {
        if (!returns && isWhitelisted(state.getBlock())) {
            BlockPos adjPos = pos.relative(facing);
            BlockState adjState = view.getBlockState(adjPos);
            return adjState.getFaceOcclusionShape(facing.getOpposite()) != Shapes.block()
                    || adjState.getBlock() != state.getBlock()
                    || !adjState.isSolidRender()
                    || isBlocked(adjState.getBlock(), adjPos, view);
        }
        return returns;
    }

    public boolean shouldRenderBlockEntity(BlockEntity blockEntity) {
        if (!isToggled()) return true;
        if (blockEntity == null || blockEntity.getBlockState() == null) return true;
        return !isBlocked(blockEntity.getBlockState().getBlock(), blockEntity.getBlockPos(), blockEntity.getLevel());
    }

    @Override
    public void onEnable() {
        if (mc != null && mc.levelExtractor != null) {
            mc.levelExtractor.allChanged();
        }
    }

    @Override
    public void onDisable() {
        if (mc != null && mc.levelExtractor != null) {
            mc.levelExtractor.allChanged();
        }
    }
}