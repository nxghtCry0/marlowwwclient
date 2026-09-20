package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.utils.ModuleUtils;
import com.eclipseware.imnotcheatingyouare.mixin.client.MinecraftAccessor;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;

public class KeybindLava extends Module {
    private int originalSlot = -1;
    private int step = 0;
    private long lastExecuteMs = 0L;

    public KeybindLava() {
        super("KeybindLava", Category.UHC, "Silently places or picks up lava with a lava/empty bucket.");
    }

    @Override
    public void onEnable() {
        if (step != 0 || System.currentTimeMillis() - lastExecuteMs < 150L) {
            this.setToggled(false);
            return;
        }
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.level == null || mc.gameMode == null) {
            resetState();
            this.setToggled(false);
            return;
        }

        if (step == 0) {
            int targetSlot = ModuleUtils.findItemInHotbar(Items.LAVA_BUCKET);
            if (targetSlot == -1) {
                targetSlot = ModuleUtils.findItemInHotbar(Items.BUCKET);
            }

            if (targetSlot == -1) {
                resetState();
                this.setToggled(false);
                return;
            }

            originalSlot = ModuleUtils.getSelectedSlot();
            if (targetSlot != originalSlot) {
                ModuleUtils.switchToSlot(targetSlot);
            }
            step = 1;
        } else if (step == 1) {
            ((MinecraftAccessor) mc).invokeStartUseItem();
            mc.player.swing(InteractionHand.MAIN_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT, true);
            step = 2;
        } else if (step == 2) {
            if (originalSlot >= 0 && originalSlot < 9 && originalSlot != ModuleUtils.getSelectedSlot()) {
                ModuleUtils.switchToSlot(originalSlot);
            }
            lastExecuteMs = System.currentTimeMillis();
            resetState();
            this.setToggled(false);
        }
    }

    private void resetState() {
        originalSlot = -1;
        step = 0;
    }

    @Override
    public void onDisable() {
        if (originalSlot >= 0 && originalSlot < 9 && mc.player != null && originalSlot != ModuleUtils.getSelectedSlot()) {
            ModuleUtils.switchToSlot(originalSlot);
        }
        resetState();
    }
}






