package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.utils.ModuleUtils;
import com.eclipseware.imnotcheatingyouare.mixin.client.MinecraftAccessor;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class CrystalHelper extends Module {

    private boolean wasDown = false;
    private long lastPlaceTime = 0;

    public CrystalHelper() {
        super("CrystalHelper", Category.Crystal, "Silent swaps to obsidian or crystal on LMB depending on the targeted block.");
    }

    @Override
    public void onTick() {
        if (mc == null || mc.player == null || mc.level == null) return;
        if (mc.gui.screen() != null) return;

        Setting onCrystalSet = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "On Crystal");
        Setting onObiSet = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "On Obsidian");
        Setting excludeBedrockSet = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Exclude Bedrock");
        Setting onlySelectedSet = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Only Selected");
        Setting onAnySet = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "On Any");
        Setting onEmptySet = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "On Empty");
        Setting onSwordSet = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "On Sword");
        Setting onCrystalItemSet = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "On Crystal Item");
        Setting onObiItemSet = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "On Obsidian Item");
        Setting onTotemSet = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "On Totem");
        Setting onGlowstoneSet = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "On Glowstone");
        Setting onAnchorSet = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "On Anchor");
        Setting holdTriggerSet = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Hold Trigger");
        Setting cooldownSet = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Cooldown (ms)");

        boolean onCrystal = onCrystalSet != null ? onCrystalSet.getValBoolean() : true;
        boolean onObi = onObiSet != null ? onObiSet.getValBoolean() : true;
        boolean excludeBedrock = excludeBedrockSet != null ? excludeBedrockSet.getValBoolean() : false;
        boolean onlySelected = onlySelectedSet != null ? onlySelectedSet.getValBoolean() : true;
        boolean onAny = onAnySet != null ? onAnySet.getValBoolean() : false;
        boolean onEmpty = onEmptySet != null ? onEmptySet.getValBoolean() : true;
        boolean onSword = onSwordSet != null ? onSwordSet.getValBoolean() : true;
        boolean onCrystalItem = onCrystalItemSet != null ? onCrystalItemSet.getValBoolean() : true;
        boolean onObiItem = onObiItemSet != null ? onObiItemSet.getValBoolean() : true;
        boolean onTotem = onTotemSet != null ? onTotemSet.getValBoolean() : true;
        boolean onGlowstone = onGlowstoneSet != null ? onGlowstoneSet.getValBoolean() : true;
        boolean onAnchor = onAnchorSet != null ? onAnchorSet.getValBoolean() : true;
        boolean holdTrigger = holdTriggerSet != null ? holdTriggerSet.getValBoolean() : false;
        double cooldownMs = cooldownSet != null ? cooldownSet.getValDouble() : 200.0;

        boolean isDown = mc.options.keyAttack.isDown();
        boolean shouldTrigger = holdTrigger ? isDown : (isDown && !wasDown);
        
        wasDown = isDown;

        if (shouldTrigger) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastPlaceTime < cooldownMs) return;

            if (!canUse(onlySelected, onAny, onEmpty, onSword, onCrystalItem, onObiItem, onTotem, onGlowstone, onAnchor)) return;

            if (mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.BLOCK) {
                BlockHitResult blockHit = (BlockHitResult) mc.hitResult;
                Block block = mc.level.getBlockState(blockHit.getBlockPos()).getBlock();

                boolean isCrystallable = (block == Blocks.OBSIDIAN) || (!excludeBedrock && (block == Blocks.BEDROCK));

                if (isCrystallable) {
                    if (onCrystal && blockHit.getDirection() == net.minecraft.core.Direction.UP) {
                        int crystalSlot = ModuleUtils.getCrystalSlot();
                        if (crystalSlot != -1) {
                            silentUseItem(crystalSlot, blockHit);
                            lastPlaceTime = currentTime;
                        }
                    }
                } 
                else {
                    if (onObi) {
                        int obiSlot = ModuleUtils.getObsidianSlot();
                        if (obiSlot != -1) {
                            silentUseItem(obiSlot, blockHit);
                            lastPlaceTime = currentTime;
                        }
                    }
                }
            }
        }
    }

    private boolean canUse(boolean onlySelected, boolean onAny, boolean onEmpty, boolean onSword, boolean onCrystalItem, boolean onObiItem, boolean onTotem, boolean onGlowstone, boolean onAnchor) {
        if (!onlySelected) return true;
        if (onAny) return true;
        if (mc.player == null) return false;

        net.minecraft.world.item.ItemStack held = mc.player.getMainHandItem();
        if (held.isEmpty()) return onEmpty;

        if (onSword && ModuleUtils.isHoldingWeapon(held)) return true;
        if (onCrystalItem && held.is(Items.END_CRYSTAL)) return true;
        if (onObiItem && held.is(Items.OBSIDIAN)) return true;
        if (onTotem && (held.is(Items.TOTEM_OF_UNDYING) || held.getItem().getDescriptionId().toLowerCase().contains("totem"))) return true;
        if (onGlowstone && held.is(Items.GLOWSTONE)) return true;
        if (onAnchor && held.is(Items.RESPAWN_ANCHOR)) return true;

        return false;
    }

    private void silentUseItem(int targetSlot, BlockHitResult hitResult) {
        ModuleUtils.runSilentSwap(targetSlot, () -> {
            mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hitResult);
            mc.player.swing(InteractionHand.MAIN_HAND);
        });
    }

    private int findItem(Item item) {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getItem(i).is(item)) return i;
        }
        return -1;
    }
}
