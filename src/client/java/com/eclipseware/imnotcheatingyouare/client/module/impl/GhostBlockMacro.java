package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.utils.ModuleUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Arrays;

public class GhostBlockMacro extends Module {
    public GhostBlockMacro() {
        super("GhostBlockMacro", Category.Farming, "Triggers a client-side placement desync window.");
        setSubCategory("Macro");
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Mode", this, "Keybind Hold", new ArrayList<>(Arrays.asList("Keybind Hold", "Single Press"))));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Auto Find Block", this, true));
    }

    @Override
    public void onEnable() {
        Setting modeSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Mode");
        String mode = modeSetting != null ? modeSetting.getValString() : "Keybind Hold";
        if (mode.equalsIgnoreCase("Single Press")) {
            executeGhostBlock();
            setToggled(false);
        }
    }

    @Override
    public void onTick() {
        if (!isToggled()) return;
        Setting modeSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Mode");
        String mode = modeSetting != null ? modeSetting.getValString() : "Keybind Hold";
        if (mode.equalsIgnoreCase("Keybind Hold")) {
            executeGhostBlock();
        }
    }

    private void executeGhostBlock() {
        if (mc.player == null || mc.gameMode == null || mc.getConnection() == null) {
            return;
        }

        BlockHitResult blockHit = null;
        if (mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.BLOCK) {
            blockHit = (BlockHitResult) mc.hitResult;
        } else {
            Vec3 eyePos = mc.player.getEyePosition(1.0F);
            Vec3 viewVec = mc.player.getViewVector(1.0F);
            Vec3 reachVec = eyePos.add(viewVec.scale(5.0));
            HitResult ray = mc.level.clip(new net.minecraft.world.level.ClipContext(
                eyePos, reachVec, net.minecraft.world.level.ClipContext.Block.OUTLINE, net.minecraft.world.level.ClipContext.Fluid.NONE, mc.player
            ));
            if (ray.getType() == HitResult.Type.BLOCK) {
                blockHit = (BlockHitResult) ray;
            }
        }

        if (blockHit == null) return;

        Setting autoFind = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Auto Find Block");
        boolean findBlock = autoFind != null && autoFind.getValBoolean();

        boolean offhandHasBlock = mc.player.getOffhandItem().getItem() instanceof BlockItem;
        if (offhandHasBlock) {
            mc.getConnection().send(new ServerboundPlayerActionPacket(
                ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND,
                BlockPos.ZERO,
                Direction.DOWN
            ));

            ItemStack offhandItem = mc.player.getOffhandItem();
            ItemStack mainItem = mc.player.getMainHandItem();
            mc.player.setItemInHand(InteractionHand.MAIN_HAND, offhandItem);
            mc.player.setItemInHand(InteractionHand.OFF_HAND, mainItem);
        }

        if (!(mc.player.getMainHandItem().getItem() instanceof BlockItem)) {
            if (findBlock) {
                int blockSlot = findBlockInHotbar();
                if (blockSlot != -1) {
                    ModuleUtils.switchToSlot(blockSlot);
                    mc.player.getInventory().setSelectedSlot(blockSlot);
                }
            }
        }

        if (!(mc.player.getMainHandItem().getItem() instanceof BlockItem)) {
            return;
        }

        mc.getConnection().send(new ServerboundPlayerActionPacket(
            ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND,
            BlockPos.ZERO,
            Direction.DOWN
        ));

        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, blockHit);
        mc.player.swing(InteractionHand.MAIN_HAND);
    }

    private int findBlockInHotbar() {
        int iceSlot = -1;
        int anyBlockSlot = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() instanceof BlockItem) {
                String name = stack.getItem().toString().toLowerCase();
                if (name.contains("ice")) {
                    return i;
                }
                if (anyBlockSlot == -1) {
                    anyBlockSlot = i;
                }
            }
        }
        return anyBlockSlot != -1 ? anyBlockSlot : iceSlot;
    }
}


