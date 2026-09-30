package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.utils.ModuleUtils;
import com.eclipseware.imnotcheatingyouare.client.utils.RotationManager;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class AutoTrade extends Module {
    private enum State {
        IDLE,
        WAIT_BLOCK,
        WAIT_VILLAGER,
        BREAKING,
        WAIT_CLEAR,
        PLACING,
        WAIT_PROFESSION,
        WAIT_TRADES
    }

    private static final class Want {
        final String id;
        final int minLevel;
        final int maxCost;
        final String raw;

        Want(String id, int minLevel, int maxCost, String raw) {
            this.id = id;
            this.minLevel = minLevel;
            this.maxCost = maxCost;
            this.raw = raw;
        }
    }

    private State state = State.IDLE;
    private Villager villager;
    private BlockPos lecternPos;
    private long lastPlaceFailMs;
    private long lastHintMs;
    private long professionStartMs;
    private int tradeWaitTicks;
    private int ignoreScreenTicks;
    private boolean rotatedForPlace;

    public AutoTrade() {
        super("AutoTrade", Category.Utility, "Rolls a librarian's trades by breaking and replacing its lectern until the book you want shows up.");
        setSubCategory("Villagers");
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Wishlist", this, "mending", true));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Default Min Level", this, 0.0, 0.0, 10.0, true));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Default Max Cost", this, 0.0, 0.0, 64.0, true));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Remove Found From Wishlist", this, true));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Disconnect When Found", this, false));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Play Sound", this, true));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Pause On Screens", this, true));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Rotate To Place", this, true));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Place Fail Delay (ms)", this, 1500.0, 0.0, 10000.0, true));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Disable On Place Fail", this, false));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Max Profession Wait (ms)", this, 0.0, 0.0, 10000.0, true));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Interact Retry (ticks)", this, 0.0, 0.0, 200.0, true));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("CivBreak", this, false));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Chat Feedback", this, true));
    }

    @Override
    public void onEnable() {
        state = State.WAIT_BLOCK;
        villager = null;
        lecternPos = null;
        rotatedForPlace = false;
        ignoreScreenTicks = 0;
        say("Hit the lectern you want to roll.");
    }

    @Override
    public void onDisable() {
        state = State.IDLE;
        villager = null;
        rotatedForPlace = false;
    }

    private Setting setting(String name) {
        return ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, name);
    }

    private boolean flag(String name) {
        Setting s = setting(name);
        return s != null && s.getValBoolean();
    }

    private int number(String name) {
        Setting s = setting(name);
        return s == null ? 0 : (int) s.getValDouble();
    }

    private void say(String message) {
        if (mc.player == null || !flag("Chat Feedback")) return;
        mc.player.sendSystemMessage(Component.literal("§d[AutoTrade] §f" + message));
    }

    private void hint(String message) {
        long now = System.currentTimeMillis();
        if (now - lastHintMs < 1500L) return;
        lastHintMs = now;
        say(message);
    }

    private List<Want> parseWishlist() {
        List<Want> out = new ArrayList<>();
        Setting list = setting("Wishlist");
        if (list == null) return out;
        int defaultLevel = number("Default Min Level");
        int defaultCost = number("Default Max Cost");
        for (String part : list.getValText().split("[,;]")) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) continue;
            String[] tokens = trimmed.toLowerCase(Locale.ROOT).split("\\s+");
            String id = tokens[0];
            if (!id.contains(":")) id = "minecraft:" + id;
            int level = tokens.length > 1 ? parseInt(tokens[1], defaultLevel) : defaultLevel;
            int cost = tokens.length > 2 ? parseInt(tokens[2], defaultCost) : defaultCost;
            out.add(new Want(id, level, cost, trimmed));
        }
        return out;
    }

    private static int parseInt(String text, int fallback) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.level == null || mc.gameMode == null) return;

        if (ignoreScreenTicks > 0) {
            ignoreScreenTicks--;
            if (mc.gui.screen() instanceof MerchantScreen) {
                mc.player.closeContainer();
                ignoreScreenTicks = 0;
            }
        }

        switch (state) {
            case WAIT_BLOCK -> tickWaitBlock();
            case WAIT_VILLAGER -> tickWaitVillager();
            case BREAKING -> tickBreaking();
            case WAIT_CLEAR -> tickWaitClear();
            case PLACING -> tickPlacing();
            case WAIT_PROFESSION -> tickWaitProfession();
            case WAIT_TRADES -> tickWaitTrades();
            default -> {
            }
        }
    }

    private void tickWaitBlock() {
        if (mc.gui.screen() != null || !mc.options.keyAttack.isDown()) return;
        if (!(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return;
        BlockPos pos = hit.getBlockPos();
        if (!mc.level.getBlockState(pos).is(Blocks.LECTERN)) {
            hint("That block is not a lectern.");
            return;
        }
        lecternPos = pos;
        if (flag("CivBreak")) {
            mc.getConnection().send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, lecternPos, Direction.UP));
        }
        state = State.WAIT_VILLAGER;
        say("Lectern selected. Right click the villager you want to roll.");
    }

    private void tickWaitVillager() {
        if (mc.gui.screen() != null || !mc.options.keyUse.isDown()) return;
        if (!(mc.hitResult instanceof EntityHitResult hit) || !(hit.getEntity() instanceof Villager target)) return;
        villager = target;
        ignoreScreenTicks = 40;
        state = State.BREAKING;
        say("Villager selected. Rolling.");
    }

    private void tickBreaking() {
        if (!villagerValid()) return;
        if (flag("CivBreak")) {
            mc.getConnection().send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, lecternPos, Direction.DOWN));
        }
        if (mc.level.getBlockState(lecternPos).isAir()) {
            state = State.WAIT_CLEAR;
            return;
        }
        if (flag("CivBreak")) return;
        if (mc.player.distanceToSqr(Vec3.atCenterOf(lecternPos)) > 36.0) {
            say("The lectern is out of reach. Move closer and turn the module back on.");
            toggle();
            return;
        }
        mc.gameMode.continueDestroyBlock(lecternPos, Direction.UP);
        mc.player.swing(InteractionHand.MAIN_HAND);
    }

    private void tickWaitClear() {
        if (!villagerValid()) return;
        if (mc.level.getBlockState(lecternPos).is(Blocks.LECTERN)) {
            say("The server put the lectern back. Breaking it again.");
            state = State.BREAKING;
            return;
        }
        if (villager.getVillagerData().profession().is(VillagerProfession.NONE)) {
            state = State.PLACING;
            rotatedForPlace = false;
        }
    }

    private void tickPlacing() {
        if (!villagerValid()) return;
        if (mc.level.getBlockState(lecternPos).is(Blocks.LECTERN)) {
            state = State.WAIT_PROFESSION;
            professionStartMs = System.currentTimeMillis();
            return;
        }
        int slot = ModuleUtils.findItemInHotbar(Items.LECTERN);
        if (slot < 0) {
            placeFailed("There is no lectern in your hotbar.");
            return;
        }
        BlockState current = mc.level.getBlockState(lecternPos);
        if (!current.canBeReplaced()) {
            placeFailed("A lectern can't be placed there.");
            return;
        }
        BlockHitResult hit = findPlaceHit(lecternPos);
        if (hit == null) {
            placeFailed("There is nothing next to that spot to place against.");
            return;
        }
        if (flag("Rotate To Place") && !rotatedForPlace) {
            Vec3 eye = mc.player.getEyePosition();
            float[] rotation = ModuleUtils.getRotations(eye, hit.getLocation());
            RotationManager.queueRotation(rotation[0], rotation[1], 0, 2, 0, false, null);
            rotatedForPlace = true;
            return;
        }
        if (ModuleUtils.getSelectedSlot() != slot) {
            ModuleUtils.switchToSlot(slot);
        }
        InteractionResult result = mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
        rotatedForPlace = false;
        if (!result.consumesAction()) {
            placeFailed("The lectern placement was refused.");
            return;
        }
        mc.player.swing(InteractionHand.MAIN_HAND);
        state = State.WAIT_PROFESSION;
        professionStartMs = System.currentTimeMillis();
    }

    private BlockHitResult findPlaceHit(BlockPos pos) {
        for (Direction side : Direction.values()) {
            BlockPos neighbor = pos.relative(side);
            BlockState neighborState = mc.level.getBlockState(neighbor);
            if (neighborState.isAir() || neighborState.canBeReplaced()) continue;
            Direction face = side.getOpposite();
            Vec3 point = Vec3.atCenterOf(neighbor).add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
            return new BlockHitResult(point, face, neighbor, false);
        }
        return null;
    }

    private void placeFailed(String message) {
        long now = System.currentTimeMillis();
        if (now - lastPlaceFailMs >= number("Place Fail Delay (ms)")) {
            say(message);
            lastPlaceFailMs = now;
        }
        if (flag("Disable On Place Fail")) toggle();
    }

    private void tickWaitProfession() {
        if (!villagerValid()) return;
        int maxWait = number("Max Profession Wait (ms)");
        if (maxWait > 0 && System.currentTimeMillis() - professionStartMs >= maxWait) {
            say("The villager did not take a profession in time. Trying again.");
            state = State.BREAKING;
            return;
        }
        BlockState at = mc.level.getBlockState(lecternPos);
        if (at.isAir()) {
            say("The server removed the lectern. Placing it again.");
            state = State.PLACING;
            return;
        }
        if (!at.is(Blocks.LECTERN)) {
            say("Something other than a lectern is in that spot.");
            state = State.BREAKING;
            return;
        }
        if (!villager.getVillagerData().profession().is(VillagerProfession.NONE)) {
            state = State.WAIT_TRADES;
            tradeWaitTicks = 0;
            interactWithVillager();
        }
    }

    private void tickWaitTrades() {
        if (!villagerValid()) return;
        if (mc.gui.screen() instanceof MerchantScreen && mc.player.containerMenu instanceof MerchantMenu menu) {
            MerchantOffers offers = menu.getOffers();
            if (offers != null && !offers.isEmpty()) {
                checkOffers(offers);
                return;
            }
        }
        int retry = number("Interact Retry (ticks)");
        if (retry > 0) {
            if (tradeWaitTicks >= retry) {
                say("No reply from the server, interacting again.");
                interactWithVillager();
            } else {
                tradeWaitTicks++;
            }
        }
    }

    private boolean villagerValid() {
        if (villager == null || villager.isRemoved() || lecternPos == null) {
            say("The villager is gone. Turning off.");
            toggle();
            return false;
        }
        return true;
    }

    private void interactWithVillager() {
        if (flag("Pause On Screens") && mc.gui.screen() != null) {
            say("Paused while a screen is open. Interact with the villager to continue.");
            return;
        }
        Vec3 eye = mc.player.getEyePosition();
        AABB box = villager.getBoundingBox();
        Optional<Vec3> clip = box.clip(eye, villager.getEyePosition());
        Vec3 point = clip.orElse(villager.getEyePosition());
        EntityHitResult hit = new EntityHitResult(villager, point);
        mc.gameMode.interact(mc.player, villager, hit, InteractionHand.MAIN_HAND);
        tradeWaitTicks = 0;
    }

    private void checkOffers(MerchantOffers offers) {
        List<Want> wants = parseWishlist();
        for (MerchantOffer offer : offers) {
            ItemStack result = offer.getResult();
            if (!result.is(Items.ENCHANTED_BOOK)) continue;
            ItemEnchantments stored = result.get(DataComponents.STORED_ENCHANTMENTS);
            if (stored == null) continue;
            for (Holder<Enchantment> enchant : stored.keySet()) {
                int level = stored.getLevel(enchant);
                String id = enchant.getRegisteredName();
                int cost = offer.getBaseCostA().getCount();
                Want match = null;
                boolean listed = false;
                for (Want want : wants) {
                    if (!want.id.equals(id)) continue;
                    listed = true;
                    if (want.minLevel <= 0) {
                        int max = enchant.value().getMaxLevel();
                        if (level < max) {
                            say("Found " + id + " but it is level " + level + " and the max is " + max + ".");
                            continue;
                        }
                    } else if (level < want.minLevel) {
                        say("Found " + id + " but it is level " + level + " and you asked for " + want.minLevel + ".");
                        continue;
                    }
                    if (want.maxCost > 0 && cost > want.maxCost) {
                        say("Found " + id + " but it costs " + cost + " emeralds and your limit is " + want.maxCost + ".");
                        continue;
                    }
                    match = want;
                    break;
                }
                if (match != null) {
                    onFound(match, id, level, cost);
                    return;
                }
                if (!listed) {
                    say("Found " + id + " but it is not on your wishlist.");
                }
            }
        }
        mc.player.closeContainer();
        state = State.BREAKING;
    }

    private void onFound(Want match, String id, int level, int cost) {
        say("Found " + id + " level " + level + " for " + cost + " emeralds. Stopping.");
        if (flag("Remove Found From Wishlist")) {
            removeFromWishlist(match);
        }
        if (flag("Play Sound")) {
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.AMETHYST_CLUSTER_BREAK, 1.0f, 1.0f));
        }
        toggle();
        if (flag("Disconnect When Found")) {
            mc.getConnection().getConnection().disconnect(Component.literal("[AutoTrade] Found " + id + " level " + level + " for " + cost + " emeralds and disconnected."));
        }
    }

    private void removeFromWishlist(Want match) {
        Setting list = setting("Wishlist");
        if (list == null) return;
        List<String> kept = new ArrayList<>();
        boolean removed = false;
        for (String part : list.getValText().split("[,;]")) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) continue;
            if (!removed && trimmed.equals(match.raw)) {
                removed = true;
                continue;
            }
            kept.add(trimmed);
        }
        list.setValText(String.join(", ", kept));
    }
}
