package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
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

        Setting onCrystalSet = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "On Crystal");
        Setting onObiSet = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "On Obsidian");
        Setting excludeBedrockSet = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Exclude Bedrock");
        Setting onlySelectedSet = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Only Selected");
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

            if (!canUse(onlySelected, onSword, onCrystalItem, onObiItem, onTotem, onGlowstone, onAnchor)) return;

            if (mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.BLOCK) {
                BlockHitResult blockHit = (BlockHitResult) mc.hitResult;
                Block block = mc.level.getBlockState(blockHit.getBlockPos()).getBlock();

                boolean isCrystallable = (block == Blocks.OBSIDIAN) || (!excludeBedrock && (block == Blocks.BEDROCK));

                if (isCrystallable && blockHit.getDirection() == net.minecraft.core.Direction.UP) {
                    if (onCrystal) {
                        int crystalSlot = findItem(Items.END_CRYSTAL);
                        if (crystalSlot != -1) {
                            silentUseItem(crystalSlot, blockHit);
                            lastPlaceTime = currentTime;
                        }
                    }
                } 
                else if (!isCrystallable) {
                    if (onObi) {
                        int obiSlot = findItem(Items.OBSIDIAN);
                        if (obiSlot != -1) {
                            silentUseItem(obiSlot, blockHit);
                            lastPlaceTime = currentTime;
                        }
                    }
                }
            }
        }
    }

    private boolean canUse(boolean onlySelected, boolean onSword, boolean onCrystalItem, boolean onObiItem, boolean onTotem, boolean onGlowstone, boolean onAnchor) {
        if (!onlySelected) return true;
        if (mc.player == null) return false;

        net.minecraft.world.item.ItemStack held = mc.player.getMainHandItem();
        if (held.isEmpty()) return false;

        String name = held.getItem().getDescriptionId().toLowerCase();

        if (onSword && name.contains("sword")) return true;
        if (onCrystalItem && held.is(Items.END_CRYSTAL)) return true;
        if (onObiItem && held.is(Items.OBSIDIAN)) return true;
        if (onTotem && name.contains("totem")) return true;
        if (onGlowstone && held.is(Items.GLOWSTONE)) return true;
        if (onAnchor && held.is(Items.RESPAWN_ANCHOR)) return true;

        return false;
    }

    private void silentUseItem(int targetSlot, BlockHitResult hitResult) {
        com.eclipseware.imnotcheatingyouare.client.utils.ModuleUtils.placeBlockSilent(hitResult, targetSlot);
    }

    private int findItem(Item item) {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getItem(i).is(item)) return i;
        }
        return -1;
    }
}
