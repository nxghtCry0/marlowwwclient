package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.utils.CartHelper;
import com.eclipseware.imnotcheatingyouare.client.utils.ModuleUtils;
import com.eclipseware.imnotcheatingyouare.client.utils.RotationManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;

public class InstaCart extends Module {
    private enum Stage { AIM, RAIL, PLACE, DRAW }

    private Stage stage = Stage.AIM;
    private BlockPos spot;
    private int originalSlot = -1;
    private int ticks;

    public InstaCart() {
        super("InstaCart", Category.CartPvP, "Places a rail and TNT minecart with silent aim, then fires a flame bow into it.");
        var sm = ImnotcheatingyouareClient.INSTANCE.settingsManager;
        ArrayList<String> modes = new ArrayList<>();
        modes.add("Auto");
        modes.add("Crosshair");
        modes.add("Target");
        sm.rSetting(new Setting("Spot", this, "Auto", modes));
        sm.rSetting(new Setting("Target Range", this, 6.0, 2.0, 10.0, false));
        sm.rSetting(new Setting("Draw Ticks", this, 5.0, 3.0, 20.0, true));
        sm.rSetting(new Setting("Shoot", this, true));
        sm.rSetting(new Setting("Swap Back", this, true));
    }

    private Setting s(String name) {
        return ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, name);
    }

    @Override
    public void onEnable() {
        if (mc.player == null || mc.level == null || mc.gameMode == null) {
            toggle();
            return;
        }
        if (CartHelper.railSlot() == -1 || CartHelper.slot(Items.TNT_MINECART) == -1) {
            toggle();
            return;
        }
        spot = CartHelper.findSpot(s("Spot").getValString(), s("Target Range").getValDouble());
        if (spot == null) {
            toggle();
            return;
        }
        originalSlot = ModuleUtils.getSelectedSlot();
        stage = Stage.AIM;
        ticks = 0;
        CartHelper.aim(CartHelper.railAim(spot));
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.level == null || spot == null) {
            finish();
            return;
        }
        switch (stage) {
            case AIM -> {
                CartHelper.aim(CartHelper.railAim(spot));
                stage = Stage.RAIL;
            }
            case RAIL -> {
                CartHelper.aim(CartHelper.railAim(spot));
                if (!CartHelper.placeRail(spot)) {
                    finish();
                    return;
                }
                stage = Stage.PLACE;
            }
            case PLACE -> {
                CartHelper.aim(CartHelper.railAim(spot));
                if (!CartHelper.placeCart(spot)) {
                    finish();
                    return;
                }
                int bow = CartHelper.slot(Items.BOW);
                if (!s("Shoot").getValBoolean() || bow == -1 || !hasArrows()) {
                    finish();
                    return;
                }
                ModuleUtils.switchToSlot(bow);
                aimCart();
                mc.options.keyUse.setDown(true);
                mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
                ticks = 0;
                stage = Stage.DRAW;
            }
            case DRAW -> {
                aimCart();
                mc.options.keyUse.setDown(true);
                if (++ticks >= (int) s("Draw Ticks").getValDouble()) {
                    mc.options.keyUse.setDown(false);
                    mc.gameMode.releaseUsingItem(mc.player);
                    finish();
                }
            }
        }
    }

    private void aimCart() {
        BlockPos rail = spot.above();
        CartHelper.aim(new Vec3(rail.getX() + 0.5, rail.getY() + 0.35, rail.getZ() + 0.5));
    }

    private boolean hasArrows() {
        if (mc.player.hasInfiniteMaterials()) return true;
        for (int i = 0; i < mc.player.getInventory().getContainerSize(); i++) {
            if (mc.player.getInventory().getItem(i).is(net.minecraft.tags.ItemTags.ARROWS)) return true;
        }
        return false;
    }

    private void finish() {
        if (mc.options != null) mc.options.keyUse.setDown(false);
        if (mc.player != null && s("Swap Back").getValBoolean() && originalSlot >= 0) ModuleUtils.switchToSlot(originalSlot);
        originalSlot = -1;
        spot = null;
        RotationManager.requestReturn();
        if (isToggled()) toggle();
    }

    @Override
    public void onDisable() {
        if (spot != null) finish();
    }
}
