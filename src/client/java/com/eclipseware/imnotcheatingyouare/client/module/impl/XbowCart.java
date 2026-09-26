package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.utils.CartHelper;
import com.eclipseware.imnotcheatingyouare.client.utils.ModuleUtils;
import com.eclipseware.imnotcheatingyouare.client.utils.RotationManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;

public class XbowCart extends Module {
    private enum Stage { LOAD, AIM, PLACE, FIRE, SHOOT }

    private Stage stage = Stage.LOAD;
    private BlockPos spot;
    private BlockPos fireGround;
    private int originalSlot = -1;
    private int ticks;

    public XbowCart() {
        super("XbowCart", Category.CartPvP, "Pre-loads the crossbow, places rail and TNT cart, lights fire in the arrow path and shoots through it, all with silent aim.");
        var sm = ImnotcheatingyouareClient.INSTANCE.settingsManager;
        ArrayList<String> modes = new ArrayList<>();
        modes.add("Auto");
        modes.add("Crosshair");
        modes.add("Target");
        sm.rSetting(new Setting("Spot", this, "Auto", modes));
        sm.rSetting(new Setting("Target Range", this, 6.0, 2.0, 10.0, false));
        sm.rSetting(new Setting("Use Fire", this, true));
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
        if (CartHelper.railSlot() == -1 || CartHelper.slot(Items.TNT_MINECART) == -1 || CartHelper.slot(Items.CROSSBOW) == -1) {
            toggle();
            return;
        }
        spot = CartHelper.findSpot(s("Spot").getValString(), s("Target Range").getValDouble());
        if (spot == null) {
            toggle();
            return;
        }
        originalSlot = ModuleUtils.getSelectedSlot();
        fireGround = null;
        ticks = 0;
        stage = charged() ? Stage.AIM : Stage.LOAD;
    }

    private ItemStack crossbow() {
        int slot = CartHelper.slot(Items.CROSSBOW);
        return slot == -1 ? ItemStack.EMPTY : mc.player.getInventory().getItem(slot);
    }

    private boolean charged() {
        return CrossbowItem.isCharged(crossbow());
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.level == null || spot == null) {
            finish();
            return;
        }
        switch (stage) {
            case LOAD -> {
                if (charged()) {
                    mc.options.keyUse.setDown(false);
                    if (mc.player.isUsingItem()) mc.gameMode.releaseUsingItem(mc.player);
                    stage = Stage.AIM;
                    return;
                }
                if (ticks == 0) {
                    ModuleUtils.switchToSlot(CartHelper.slot(Items.CROSSBOW));
                    mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
                }
                mc.options.keyUse.setDown(true);
                if (++ticks > 40) finish();
            }
            case AIM -> {
                CartHelper.aim(CartHelper.railAim(spot));
                stage = Stage.PLACE;
            }
            case PLACE -> {
                CartHelper.aim(CartHelper.railAim(spot));
                if (!CartHelper.placeRail(spot) || !CartHelper.placeCart(spot)) {
                    finish();
                    return;
                }
                fireGround = s("Use Fire").getValBoolean() && CartHelper.slot(Items.FLINT_AND_STEEL) != -1 ? fireSpot() : null;
                if (fireGround != null) {
                    CartHelper.aim(CartHelper.railAim(fireGround));
                    stage = Stage.FIRE;
                } else {
                    aimCart();
                    stage = Stage.SHOOT;
                }
            }
            case FIRE -> {
                CartHelper.aim(CartHelper.railAim(fireGround));
                CartHelper.useOn(CartHelper.slot(Items.FLINT_AND_STEEL), new BlockHitResult(CartHelper.railAim(fireGround), Direction.UP, fireGround, false));
                aimCart();
                stage = Stage.SHOOT;
            }
            case SHOOT -> {
                aimCart();
                ModuleUtils.switchToSlot(CartHelper.slot(Items.CROSSBOW));
                mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
                finish();
            }
        }
    }

    private BlockPos fireSpot() {
        Vec3 eye = mc.player.getEyePosition();
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos ground = spot.relative(dir);
            BlockPos cell = ground.above();
            if (!mc.level.getBlockState(cell).isAir()) continue;
            if (!mc.level.getBlockState(ground).isFaceSturdy(mc.level, ground, Direction.UP)) continue;
            if (!CartHelper.inReach(ground)) continue;
            double d = eye.distanceToSqr(Vec3.atCenterOf(cell));
            if (d < bestDist) {
                bestDist = d;
                best = ground;
            }
        }
        return best;
    }

    private void aimCart() {
        BlockPos rail = spot.above();
        CartHelper.aim(new Vec3(rail.getX() + 0.5, rail.getY() + 0.35, rail.getZ() + 0.5));
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
