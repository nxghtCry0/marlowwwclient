package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class InventoryClean extends Module {
    private long lastDropMs = 0L;
    private long nextDelay = 0L;
    private boolean running = false;
    private boolean autoOpenedInventory = false;

    public InventoryClean() {
        super("InventoryClean", Category.Farming, "Automatically removes unnecessary items from your inventory, keeping it clean and manageable. Especially useful for gamemodes with rapid item acquisition.");
        setSubCategory("Inventory");

        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Activation", this, "Toggle", new ArrayList<>(Arrays.asList("On Key", "Toggle"))));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Open Inventory", this, true));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Inventory Only", this, false));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Min Delay (ms)", this, 150.0, 0.0, 2000.0, true));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Max Delay (ms)", this, 400.0, 0.0, 2000.0, true));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Best Items", this, true));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Remove Negative Potions", this, true));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Remove Food", this, false));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Blacklist", this,
                "stick,string,flint,compass,feather,glass_bottle,enchanting_table,chest,anvil", true));
    }

    @Override
    public void onDisable() {
        running = false;
        if (autoOpenedInventory && mc.gui != null && mc.gui.screen() instanceof InventoryScreen) {
            mc.setScreenAndShow((Screen) null);
        }
        autoOpenedInventory = false;
    }

    @Override
    public void onKeybind() {
        Setting activationSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Activation");
        String activation = activationSetting != null ? activationSetting.getValString() : "Toggle";

        if (!activation.equals("On Key")) {
            toggle();
            return;
        }

        if (running || mc.player == null) return;
        running = true;
        lastDropMs = 0L;
        nextDelay = 0L;

        Setting openInventorySetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Open Inventory");
        if (openInventorySetting != null && openInventorySetting.getValBoolean() && mc.gui.screen() == null) {
            mc.setScreenAndShow(new InventoryScreen(mc.player));
            autoOpenedInventory = true;
        }
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.gameMode == null) return;

        Setting activationSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Activation");
        String activation = activationSetting != null ? activationSetting.getValString() : "Toggle";

        boolean shouldRun;
        if (activation.equals("On Key")) {
            shouldRun = running;
        } else {
            if (!isToggled()) return;
            Setting inventoryOnlySetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Inventory Only");
            boolean inventoryOnly = inventoryOnlySetting != null && inventoryOnlySetting.getValBoolean();
            shouldRun = !inventoryOnly || mc.gui.screen() instanceof InventoryScreen;
        }
        if (!shouldRun) return;

        if (System.currentTimeMillis() - lastDropMs < nextDelay) return;

        int slot = findNextRemovableSlot();
        if (slot == -1) {
            finishRun(activation);
            return;
        }

        int containerSlot = slot < 9 ? slot + 36 : slot;
        mc.gameMode.handleContainerInput(mc.player.inventoryMenu.containerId, containerSlot, 1, ContainerInput.THROW, mc.player);

        lastDropMs = System.currentTimeMillis();
        Setting minSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Min Delay (ms)");
        Setting maxSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Max Delay (ms)");
        int min = minSetting != null ? (int) minSetting.getValDouble() : 150;
        int max = maxSetting != null ? (int) maxSetting.getValDouble() : 400;
        if (min > max) { int t = min; min = max; max = t; }
        nextDelay = min + (long) (Math.random() * ((max - min) + 1));
    }

    private void finishRun(String activation) {
        if (activation.equals("On Key") && running) {
            running = false;
            if (autoOpenedInventory) {
                mc.setScreenAndShow((Screen) null);
                autoOpenedInventory = false;
            }
        }
    }

    private int findNextRemovableSlot() {
        Setting blacklistSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Blacklist");
        Set<String> blacklist = parseBlacklist(blacklistSetting != null ? blacklistSetting.getValText() : "");

        Setting removePotionsSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Remove Negative Potions");
        boolean removeNegativePotions = removePotionsSetting != null && removePotionsSetting.getValBoolean();

        Setting removeFoodSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Remove Food");
        boolean removeFood = removeFoodSetting != null && removeFoodSetting.getValBoolean();

        Setting bestItemsSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Best Items");
        boolean protectBestItems = bestItemsSetting == null || bestItemsSetting.getValBoolean();

        Map<String, Integer> bestSlotByCategory = protectBestItems ? findBestItemSlots() : new HashMap<>();

        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;

            String category = categorize(stack);
            if (category != null) {
                Integer bestSlot = bestSlotByCategory.get(category);
                if (bestSlot != null) {
                    if (bestSlot == i) continue;
                    return i;
                }
            }

            if (isBlacklisted(stack, blacklist)) return i;
            if (removeNegativePotions && isNegativePotion(stack)) return i;
            if (removeFood && isRemovableFood(stack)) return i;
        }
        return -1;
    }

    private Map<String, Integer> findBestItemSlots() {
        Map<String, Integer> bestSlot = new HashMap<>();
        Map<String, Float> bestValue = new HashMap<>();

        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;

            String category = categorize(stack);
            if (category == null) continue;

            float value = itemValue(stack);
            Float current = bestValue.get(category);
            if (current == null || value > current) {
                bestValue.put(category, value);
                bestSlot.put(category, i);
            }
        }
        return bestSlot;
    }

    private String categorize(ItemStack stack) {
        Item item = stack.getItem();
        String path = BuiltInRegistries.ITEM.getKey(item).getPath();

        if (path.contains("pickaxe")) return "pickaxe";
        if (path.contains("sword")) return "sword";
        if (path.contains("axe")) return "axe";
        if (path.contains("bow") && !path.contains("crossbow")) return "bow";

        net.minecraft.world.item.equipment.Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        if (equippable != null) {
            return "armor_" + equippable.slot().name();
        }
        return null;
    }

    private float itemValue(ItemStack stack) {
        double attack = 0.0;
        double armor = 0.0;
        double toughness = 0.0;

        ItemAttributeModifiers modifiers = stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
        if (modifiers != null) {
            for (ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
                if (entry.attribute().value() == Attributes.ATTACK_DAMAGE.value()) {
                    attack += entry.modifier().amount();
                } else if (entry.attribute().value() == Attributes.ARMOR.value()) {
                    armor += entry.modifier().amount();
                } else if (entry.attribute().value() == Attributes.ARMOR_TOUGHNESS.value()) {
                    toughness += entry.modifier().amount();
                }
            }
        }

        float durabilityFraction = 0f;
        int maxDamage = stack.getMaxDamage();
        if (maxDamage > 0) {
            durabilityFraction = 1.0f - ((float) stack.getDamageValue() / (float) maxDamage);
        }

        return (float) (attack + armor + toughness * 0.5) + durabilityFraction * 0.01f;
    }

    private boolean isBlacklisted(ItemStack stack, Set<String> blacklist) {
        if (blacklist.isEmpty()) return false;
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        return blacklist.contains(path);
    }

    private boolean isNegativePotion(ItemStack stack) {
        if (!stack.is(Items.POTION) && !stack.is(Items.SPLASH_POTION) && !stack.is(Items.LINGERING_POTION)) return false;
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        if (contents == null) return false;

        for (net.minecraft.world.effect.MobEffectInstance effect : contents.getAllEffects()) {
            if (!effect.getEffect().value().isBeneficial()) return true;
        }
        return false;
    }

    private boolean isRemovableFood(ItemStack stack) {
        if (stack.is(Items.GOLDEN_APPLE) || stack.is(Items.ENCHANTED_GOLDEN_APPLE)) return false;
        return stack.get(DataComponents.FOOD) != null;
    }

    private Set<String> parseBlacklist(String raw) {
        Set<String> result = new HashSet<>();
        if (raw == null || raw.isBlank()) return result;
        for (String entry : raw.split(",")) {
            String trimmed = entry.trim().toLowerCase();
            if (!trimmed.isEmpty()) result.add(trimmed);
        }
        return result;
    }
}
