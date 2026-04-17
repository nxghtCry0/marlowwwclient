package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.utils.TimerUtil;
import com.eclipseware.imnotcheatingyouare.mixin.client.MinecraftAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class AutoMace extends Module {

    private final TimerUtil attackTimer = new TimerUtil();
    private int savedSlot = -1;
    private double fallStartY = -1;
    private boolean isFalling = false;
    private boolean slamExecuted = false;
    private boolean maceHit = false;
    private int slamTick = 0;
    private int attackDelayOverride = -1;

    public AutoMace() {
        super("AutoMace", Category.Combat, "Automatically attacks with mace");
        
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Min Fall Distance", this, 3.0, 1.0, 10.0, false));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Attack Delay", this, 100.0, 0.0, 500.0, true));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Density Threshold", this, 7.0, 1.0, 20.0, false));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Target Players", this, true));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Target Mobs", this, false));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Stun Slam", this, false));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Only Axe", this, false));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Auto Switch Mace", this, true));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Stay On Mace", this, false));
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.level == null)
            return;

        updateFall();
        attack();
    }

    private void updateFall() {
        boolean onGround = mc.player.onGround();
        boolean falling = mc.player.getDeltaMovement().y < -0.1;
        boolean rising = mc.player.getDeltaMovement().y > 0.1;
        double currentY = mc.player.getY();

        if (onGround) {
            if (isFalling) {
                resetFall();
            }
            if (savedSlot != -1 && !getBoolSetting("Stay On Mace")) {
                switchToSlot(savedSlot);
                savedSlot = -1;
            }
            return;
        }

        if (rising && maceHit) {
            maceHit = false;
            fallStartY = currentY;
        }

        if (!isFalling) {
            isFalling = true;
            fallStartY = currentY;
            slamExecuted = false;
            maceHit = false;
            slamTick = 0;
        } else if (falling && fallStartY != -1 && currentY > fallStartY) {
            fallStartY = currentY;
        }
    }

    private void attack() {
        if (!isFalling || mc.player.getDeltaMovement().y >= -0.1)
            return;

        double fallDist = fallStartY == -1 ? 0 : Math.max(0, fallStartY - mc.player.getY());
        if (fallDist < getDoubleSetting("Min Fall Distance"))
            return;

        Entity target = mc.hitResult != null && mc.hitResult.getType() == net.minecraft.world.phys.HitResult.Type.ENTITY 
            ? ((net.minecraft.world.phys.EntityHitResult) mc.hitResult).getEntity() 
            : null;
        
        if (!isValidTarget(target))
            return;

        if (getBoolSetting("Stun Slam")) {
            handleSlam(target, fallDist);
        }

        if (!getBoolSetting("Stun Slam") || slamExecuted || slamTick == 0) {
            handleMaceAttack(target);
        }
    }

    private void handleSlam(Entity target, double fallDist) {
        boolean targetBlocking = target instanceof Player player &&
                player.isBlocking() &&
                net.minecraft.world.item.Items.SHIELD.equals(player.getUseItem().getItem());

        if (getBoolSetting("Only Axe") && !isAxe(mc.player.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND))) {
            return;
        }

        if (targetBlocking && fallDist > getDoubleSetting("Min Fall Distance") && !slamExecuted && slamTick == 0) {
            if (savedSlot == -1)
                savedSlot = mc.player.getInventory().getSelectedSlot();
            slamTick = 1;
        }

        if (slamTick == 1) {
            int axeSlot = getBoolSetting("Only Axe") ? mc.player.getInventory().getSelectedSlot() : getAxeSlotId();
            if (axeSlot != -1) {
                mc.player.getInventory().setSelectedSlot(axeSlot);
                ((MinecraftAccessor) mc).invokeStartAttack();
            }
            slamTick = 2;
        } else if (slamTick == 2) {
            switchToMace();
            slamExecuted = true;
            slamTick = 0;
        }
    }

    private void handleMaceAttack(Entity target) {
        if (maceHit) return;
        
        double fallDist = fallStartY == -1 ? 0 : Math.max(0, fallStartY - mc.player.getY());

        if (!hasMace()) {
            if (savedSlot == -1)
                savedSlot = mc.player.getInventory().getSelectedSlot();
            if (getBoolSetting("Auto Switch Mace")) {
                switchToAppropriateMace(fallDist);
            } else {
                switchToMace();
            }
        } else if (getBoolSetting("Auto Switch Mace")) {
            switchToAppropriateMace(fallDist);
        }

        if (hasMace() && attackTimer.hasElapsedTime(getEffectiveAttackDelay(), true)) {
            ((MinecraftAccessor) mc).invokeStartAttack();
            maceHit = true;
        }
    }

    private boolean isValidTarget(Entity entity) {
        if (entity == null || entity == mc.player)
            return false;
        if (!(entity instanceof LivingEntity livingEntity))
            return false;
        if (!livingEntity.isAlive())
            return false;

        if (entity instanceof Player) {
            return getBoolSetting("Target Players");
        } else {
            return getBoolSetting("Target Mobs");
        }
    }

    private int getAxeSlotId() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (isAxe(stack))
                return i;
        }
        return -1;
    }

    private boolean isAxe(ItemStack stack) {
        return stack.getItem() instanceof AxeItem;
    }

    private boolean hasMace() {
        return mc.player.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND).getItem() == Items.MACE;
    }

    private void switchToMace() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.getItem() == Items.MACE) {
                mc.player.getInventory().setSelectedSlot(i);
                return;
            }
        }
    }

    private void switchToAppropriateMace(double fallDistance) {
        boolean useDensity = fallDistance >= getDoubleSetting("Density Threshold");

        int targetSlot = useDensity ? findDensityMaceSlot() : findBreachMaceSlot();

        if (targetSlot == -1) {
            targetSlot = findAnyMaceSlot();
        }

        if (targetSlot != -1) {
            mc.player.getInventory().setSelectedSlot(targetSlot);
        }
    }

    private int findDensityMaceSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.getItem() == Items.MACE && hasDensityEnchantment(stack)) {
                return i;
            }
        }
        return -1;
    }

    private int findBreachMaceSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.getItem() == Items.MACE && hasBreachEnchantment(stack)) {
                return i;
            }
        }
        return -1;
    }

    private int findAnyMaceSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.getItem() == Items.MACE) {
                return i;
            }
        }
        return -1;
    }

    private boolean hasDensityEnchantment(ItemStack stack) {
        for (var enchant : stack.getEnchantments().keySet()) {
            if (enchant.unwrapKey().isPresent() && enchant.unwrapKey().get().toString().contains("density")) {
                return true;
            }
        }
        return false;
    }

    private boolean hasBreachEnchantment(ItemStack stack) {
        for (var enchant : stack.getEnchantments().keySet()) {
            if (enchant.unwrapKey().isPresent() && enchant.unwrapKey().get().toString().contains("breach")) {
                return true;
            }
        }
        return false;
    }

    private void switchToSlot(int slot) {
        if (slot >= 0 && slot < 9) {
            mc.player.getInventory().setSelectedSlot(slot);
        }
    }

    private void resetFall() {
        isFalling = false;
        fallStartY = -1;
        slamExecuted = false;
        maceHit = false;
        slamTick = 0;
    }

    private double getDoubleSetting(String name) {
        Setting s = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, name);
        return s != null ? s.getValDouble() : 0;
    }

    private boolean getBoolSetting(String name) {
        Setting s = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, name);
        return s != null && s.getValBoolean();
    }

    @Override
    public void onEnable() {
        savedSlot = -1;
        fallStartY = -1;
        isFalling = false;
        slamExecuted = false;
        maceHit = false;
        slamTick = 0;
        attackTimer.reset();
    }

    @Override
    public void onDisable() {
        if (savedSlot != -1) {
            switchToSlot(savedSlot);
        }
        resetAll();
    }

    private void resetAll() {
        savedSlot = -1;
        fallStartY = -1;
        isFalling = false;
        slamExecuted = false;
        maceHit = false;
        slamTick = 0;
        attackTimer.reset();
    }

    public void setAttackDelayOverride(int delay) {
        this.attackDelayOverride = delay;
    }

    public void clearAttackDelayOverride() {
        this.attackDelayOverride = -1;
    }

    private int getEffectiveAttackDelay() {
        return attackDelayOverride >= 0 ? attackDelayOverride : (int) getDoubleSetting("Attack Delay");
    }
}