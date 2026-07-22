package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.utils.ModuleUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;

public class NoFall extends Module {
    public NoFall() {
        super("NoFall", Category.Blatant, "Prevents fall damage by spoofing ground state packets.");
        
        ArrayList<String> modes = new ArrayList<>();
        modes.add("Packet");
        modes.add("AirPlace");
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Mode", this, "Packet", modes));
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.level == null) return;
        if (mc.player.getAbilities().instabuild) return;

        Setting modeSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Mode");
        String mode = modeSetting != null ? modeSetting.getValString() : "Packet";

        if (mode.equalsIgnoreCase("AirPlace")) {
            if (mc.player.fallDistance > 2.0f) {
                BlockHitResult result = getBlockBelow(5.0);
                if (result != null && result.getType() == HitResult.Type.BLOCK) {
                    int blockSlot = findBlockInHotbar();
                    if (blockSlot != -1) {
                        ModuleUtils.placeBlockSilent(result.getBlockPos(), Direction.UP, blockSlot);
                    }
                }
            }
        }
    }

    private BlockHitResult getBlockBelow(double maxDist) {
        Vec3 start = mc.player.position();
        Vec3 end = start.subtract(0, maxDist, 0);
        return mc.level.clip(new ClipContext(start, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, mc.player));
    }

    private int findBlockInHotbar() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() instanceof BlockItem) {
                return i;
            }
        }
        return -1;
    }
}
