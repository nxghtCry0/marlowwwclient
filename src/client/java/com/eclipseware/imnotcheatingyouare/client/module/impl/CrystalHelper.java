package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.setting.SettingsManager;
import com.eclipseware.imnotcheatingyouare.client.utils.CrystalHelperRules;
import com.eclipseware.imnotcheatingyouare.client.utils.CrystalHelperRules.Held;
import com.eclipseware.imnotcheatingyouare.client.utils.CrystalHelperRules.HeldFilter;
import com.eclipseware.imnotcheatingyouare.client.utils.ModuleUtils;
import net.minecraft.client.KeyMapping;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import java.util.concurrent.ThreadLocalRandom;

public class CrystalHelper extends Module {

    private boolean wasDown = false;
    private long lastPlaceTime = 0;
    private double nextDelayMs = 0;

    public CrystalHelper() {
        super("CrystalHelper", Category.Crystal, "Silent swaps to obsidian or crystal on LMB depending on the targeted block.");
    }

    @Override
    public void onTick() {
        if (mc == null || mc.player == null || mc.level == null) {
            wasDown = false;
            return;
        }

        KeyMapping attackKey = mc.options.keyAttack;
        boolean isDown = attackKey.isDown() && mc.screen == null;
        boolean shouldTrigger = CrystalHelperRules.shouldTrigger(readBool("Hold Trigger", false), isDown, wasDown);
        wasDown = isDown;

        if (!shouldTrigger) return;

        long now = System.currentTimeMillis();
        if (!CrystalHelperRules.cooldownElapsed(now, lastPlaceTime, nextDelayMs)) return;
        if (!readFilter().allows(heldKind())) return;
        if (mc.hitResult == null || mc.hitResult.getType() != HitResult.Type.BLOCK) return;

        BlockHitResult blockHit = (BlockHitResult) mc.hitResult;
        if (placeFor(blockHit)) rearm(now);
    }

    private void rearm(long now) {
        lastPlaceTime = now;
        double base = readNumber("Cooldown (ms)", 200.0);
        nextDelayMs = CrystalHelperRules.jitteredCooldown(base, ThreadLocalRandom.current().nextDouble());
    }

    private boolean placeFor(BlockHitResult blockHit) {
        Block block = blockAt(blockHit.getBlockPos());
        boolean crystallable = CrystalHelperRules.isCrystallable(
                block == Blocks.OBSIDIAN,
                block == Blocks.BEDROCK,
                readBool("Exclude Bedrock", false));

        if (crystallable) return placeCrystal(blockHit);
        return placeObsidian(blockHit);
    }

    private boolean placeCrystal(BlockHitResult blockHit) {
        if (!readBool("On Crystal", true)) return false;
        if (blockHit.getDirection() != Direction.UP) return false;
        if (!hasCrystalRoom(blockHit.getBlockPos())) return false;

        int slot = ModuleUtils.getCrystalSlot();
        if (slot == -1) return false;

        silentUseItem(slot, blockHit);
        return true;
    }

    private boolean placeObsidian(BlockHitResult blockHit) {
        if (!readBool("On Obsidian", true)) return false;
        if (!canPlaceObsidianAt(blockHit.getBlockPos().relative(blockHit.getDirection()))) return false;

        int slot = ModuleUtils.getObsidianSlot();
        if (slot == -1) return false;

        silentUseItem(slot, blockHit);
        return true;
    }

    private boolean canPlaceObsidianAt(BlockPos target) {
        boolean replaceable = mc.level.getBlockState(target).canBeReplaced();
        boolean unobstructed = mc.level.isUnobstructed(Blocks.OBSIDIAN.defaultBlockState(), target, CollisionContext.empty());
        return replaceable && unobstructed;
    }

    private boolean hasCrystalRoom(BlockPos base) {
        BlockPos first = base.above();
        BlockPos second = first.above();
        return mc.level.isEmptyBlock(first) && mc.level.isEmptyBlock(second);
    }

    private Block blockAt(BlockPos pos) {
        return mc.level.getBlockState(pos).getBlock();
    }

    private Held heldKind() {
        ItemStack held = mc.player.getMainHandItem();
        if (held.isEmpty()) return Held.EMPTY;
        if (ModuleUtils.isHoldingWeapon(held)) return Held.WEAPON;
        if (held.is(Items.END_CRYSTAL)) return Held.CRYSTAL;
        if (held.is(Items.OBSIDIAN)) return Held.OBSIDIAN;
        if (held.is(Items.TOTEM_OF_UNDYING)) return Held.TOTEM;
        if (held.is(Items.GLOWSTONE)) return Held.GLOWSTONE;
        if (held.is(Items.RESPAWN_ANCHOR)) return Held.ANCHOR;
        return Held.OTHER;
    }

    private HeldFilter readFilter() {
        return new HeldFilter(
                readBool("Only Selected", true),
                readBool("On Any", false),
                readBool("On Empty", true),
                readBool("On Sword", true),
                readBool("On Crystal Item", true),
                readBool("On Obsidian Item", true),
                readBool("On Totem", true),
                readBool("On Glowstone", true),
                readBool("On Anchor", true));
    }

    private Setting setting(String name) {
        SettingsManager manager = ImnotcheatingyouareClient.INSTANCE.settingsManager;
        return manager.getSettingByName(this, name);
    }

    private boolean readBool(String name, boolean fallback) {
        Setting found = setting(name);
        return found != null ? found.getValBoolean() : fallback;
    }

    private double readNumber(String name, double fallback) {
        Setting found = setting(name);
        return found != null ? found.getValDouble() : fallback;
    }

    private void silentUseItem(int targetSlot, BlockHitResult hitResult) {
        ModuleUtils.runSilentSwap(targetSlot, () -> {
            mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hitResult);
            mc.player.swing(InteractionHand.MAIN_HAND);
        });
    }
}
