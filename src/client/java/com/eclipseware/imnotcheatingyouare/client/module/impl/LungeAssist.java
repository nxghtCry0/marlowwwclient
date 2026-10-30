package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.setting.SettingsManager;
import com.eclipseware.imnotcheatingyouare.client.utils.ModuleUtils;
import com.eclipseware.imnotcheatingyouare.mixin.client.MinecraftAccessor;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.PiercingWeapon;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

public class LungeAssist extends Module {
    public static LungeAssist INSTANCE;

    private static final int SERVER_GRACE_TICKS = 5;
    private static final int MAX_WAIT_TICKS = 14;

    private boolean isSwapping = false;
    private boolean waitingForCharge = false;
    private boolean enabledThisTick = false;
    private int originalSlot = -1;
    private int pendingLungeSlot = -1;
    private int swapTicks = 0;
    private int waitTicks = 0;
    private int targetTicks = 0;

    public LungeAssist() {
        super("LungeSwap", Category.Mace, "Instantly swaps to your best Lunge spear, lunges, and swaps back.");
        INSTANCE = this;
        registerCoreSettings();
    }

    public void registerCoreSettings() {
        ImnotcheatingyouareClient client = ImnotcheatingyouareClient.INSTANCE;
        if (client == null || client.settingsManager == null) {
            return;
        }
        SettingsManager sm = client.settingsManager;
        if (sm.getSettingByName(this, "Mode") == null) {
            ArrayList<String> modes = new ArrayList<>();
            modes.add("Silent");
            modes.add("Swap");
            sm.rSetting(new Setting("Mode", this, "Silent", modes));
        }
        if (sm.getSettingByName(this, "Swap Delay") == null) {
            sm.rSetting(new Setting("Swap Delay", this, 0.0, 0.0, 4.0, true));
        }
        if (sm.getSettingByName(this, "Early Charge") == null) {
            sm.rSetting(new Setting("Early Charge", this, true));
        }
        if (sm.getSettingByName(this, "Wait For Charge") == null) {
            sm.rSetting(new Setting("Wait For Charge", this, true));
        }
    }

    public void registerSettingsIfReady() {
        registerCoreSettings();
        ImnotcheatingyouareClient client = ImnotcheatingyouareClient.INSTANCE;
        if (client == null || client.settingsManager == null) {
            return;
        }
        SettingsManager sm = client.settingsManager;
        if (sm.getSettingByName(this, "AutoJump") == null) {
            sm.rSetting(new Setting("AutoJump", this, true));
        }
    }

    @Override
    public void onEnable() {
        registerSettingsIfReady();
        if (mc == null || mc.player == null || mc.level == null) {
            setToggled(false);
            return;
        }
        Inventory inv = mc.player.getInventory();
        int currentSlot = inv.getSelectedSlot();
        int lungeSlot = findLungeSlot(mc.player);
        if (lungeSlot == -1) {
            setToggled(false);
            return;
        }
        ItemStack spearStack = inv.getItem(lungeSlot);
        if (currentSlot == lungeSlot) {
            if (canAttackWithSpear(spearStack)) {
                triggerAutoJump();
                performSpearAttack(spearStack);
            }
            setToggled(false);
            return;
        }

        this.originalSlot = currentSlot;
        this.pendingLungeSlot = lungeSlot;
        this.targetTicks = resolveSwapDelayTicks();
        this.swapTicks = 0;
        this.waitTicks = 0;
        this.enabledThisTick = true;

        if (canAttackWithSpear(spearStack)) {
            executeSwapAndLunge(lungeSlot, spearStack);
            return;
        }

        if (getBoolSetting("Wait For Charge", true)) {
            this.waitingForCharge = true;
            this.isSwapping = false;
        } else {
            executeSwapAndLunge(lungeSlot, spearStack);
        }
    }

    public boolean onPlayerAttack() {
        return false;
    }

    @Override
    public void onTick() {
        if (mc == null || mc.player == null || mc.level == null) {
            setToggled(false);
            return;
        }
        if (this.enabledThisTick) {
            this.enabledThisTick = false;
            return;
        }

        if (this.waitingForCharge) {
            this.waitTicks++;
            int lungeSlot = findLungeSlot(mc.player);
            if (lungeSlot == -1 || this.waitTicks > MAX_WAIT_TICKS) {
                setToggled(false);
                return;
            }
            Inventory inv = mc.player.getInventory();
            ItemStack spearStack = inv.getItem(lungeSlot);
            if (canAttackWithSpear(spearStack)) {
                this.waitingForCharge = false;
                executeSwapAndLunge(lungeSlot, spearStack);
            }
            return;
        }

        if (!this.isSwapping) {
            setToggled(false);
            return;
        }
        this.swapTicks++;
        if (this.swapTicks >= this.targetTicks) {
            restoreOriginalSlot();
            setToggled(false);
        }
    }

    @Override
    public void onDisable() {
        if (this.isSwapping) {
            restoreOriginalSlot();
        }
        this.isSwapping = false;
        this.waitingForCharge = false;
        this.enabledThisTick = false;
        this.originalSlot = -1;
        this.pendingLungeSlot = -1;
        this.swapTicks = 0;
        this.waitTicks = 0;
    }

    private void executeSwapAndLunge(int lungeSlot, ItemStack spearStack) {
        triggerAutoJump();
        equip(lungeSlot);
        performSpearAttack(spearStack);
        if (this.targetTicks <= 0) {
            restoreOriginalSlot();
            this.isSwapping = false;
            setToggled(false);
        } else {
            this.isSwapping = true;
            this.swapTicks = 0;
        }
    }

    private void triggerAutoJump() {
        if (mc == null || mc.player == null) {
            return;
        }
        if (getBoolSetting("AutoJump", true) && mc.player.onGround()) {
            mc.player.jumpFromGround();
        }
    }

    private void performSpearAttack(ItemStack spearStack) {
        if (mc == null || mc.player == null) {
            return;
        }
        Minecraft minecraft = mc;
        if (minecraft instanceof MinecraftAccessor accessor) {
            accessor.setMissTime(0);
        }
        PiercingWeapon piercing = spearStack.get(DataComponents.PIERCING_WEAPON);
        MultiPlayerGameMode gm = mc.gameMode;
        if (piercing != null && gm != null) {
            gm.piercingAttack(piercing);
            mc.player.swing(InteractionHand.MAIN_HAND);
            return;
        }
        if (minecraft instanceof MinecraftAccessor accessor) {
            accessor.invokeStartAttack();
        }
    }

    private boolean canAttackWithSpear(ItemStack spearStack) {
        if (mc == null || mc.player == null || spearStack == null || spearStack.isEmpty()) {
            return false;
        }
        int grace = getBoolSetting("Early Charge", true) ? SERVER_GRACE_TICKS : 0;
        return !mc.player.cannotAttackWithItem(spearStack, grace);
    }

    public int resolveSwapDelayTicks() {
        String mode = getStringSetting("Mode", "Silent");
        int configured = (int) Math.round(getDoubleSetting("Swap Delay", 0.0));
        if ("Silent".equalsIgnoreCase(mode)) {
            return Math.max(0, configured);
        }
        return Math.max(1, configured);
    }

    private void restoreOriginalSlot() {
        if (mc == null || mc.player == null || this.originalSlot < 0 || this.originalSlot > 8) {
            return;
        }
        Inventory inv = mc.player.getInventory();
        if (inv.getSelectedSlot() != this.originalSlot) {
            equip(this.originalSlot);
        }
    }

    private void equip(int slot) {
        if (mc == null || mc.player == null) {
            return;
        }
        Inventory inv = mc.player.getInventory();
        if (inv.getSelectedSlot() != slot) {
            ModuleUtils.switchToSlot(slot);
            inv.setSelectedSlot(slot);
        }
    }

    public int findLungeSlot(Player player) {
        if (player == null) {
            return -1;
        }
        Inventory inv = player.getInventory();
        int bestSlot = -1;
        int bestScore = 0;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inv.getItem(i);
            int score = scoreSpearStack(stack);
            if (score > bestScore) {
                bestScore = score;
                bestSlot = i;
            }
        }
        return bestSlot;
    }

    public int scoreSpearStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }
        if (!isSpearItem(stack)) {
            return 0;
        }
        int lungeLevel = getLungeEnchantLevel(stack);
        if (lungeLevel > 0) {
            return 10 + lungeLevel;
        }
        return 1;
    }

    public int getLungeEnchantLevel(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }
        ItemEnchantments enchantments = stack.getEnchantments();
        Set<Holder<Enchantment>> keys = enchantments.keySet();
        int maxLevel = 0;
        for (Holder<Enchantment> enchant : keys) {
            Optional<ResourceKey<Enchantment>> optKey = enchant.unwrapKey();
            if (optKey.isPresent()) {
                ResourceKey<Enchantment> resKey = optKey.get();
                String keyText = resKey.toString().toLowerCase();
                if (keyText.contains("lunge")) {
                    int lvl = enchantments.getLevel(enchant);
                    if (lvl > maxLevel) {
                        maxLevel = Math.max(1, lvl);
                    }
                }
            }
        }
        return maxLevel;
    }

    public boolean isLungeSpear(ItemStack stack) {
        return scoreSpearStack(stack) > 10;
    }

    public boolean isSpearItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (stack.get(DataComponents.PIERCING_WEAPON) != null) {
            return true;
        }
        Item item = stack.getItem();
        String itemName = item.toString().toLowerCase();
        Identifier regId = BuiltInRegistries.ITEM.getKey(item);
        String registryName = regId.getPath().toLowerCase();
        return itemName.contains("spear")
            || registryName.contains("spear")
            || itemName.contains("trident")
            || registryName.contains("trident");
    }

    private boolean getBoolSetting(String name, boolean fallback) {
        ImnotcheatingyouareClient client = ImnotcheatingyouareClient.INSTANCE;
        if (client == null || client.settingsManager == null) {
            return fallback;
        }
        SettingsManager sm = client.settingsManager;
        Setting s = sm.getSettingByName(this, name);
        return s != null ? s.getValBoolean() : fallback;
    }

    private double getDoubleSetting(String name, double fallback) {
        ImnotcheatingyouareClient client = ImnotcheatingyouareClient.INSTANCE;
        if (client == null || client.settingsManager == null) {
            return fallback;
        }
        SettingsManager sm = client.settingsManager;
        Setting s = sm.getSettingByName(this, name);
        return s != null ? s.getValDouble() : fallback;
    }

    private String getStringSetting(String name, String fallback) {
        ImnotcheatingyouareClient client = ImnotcheatingyouareClient.INSTANCE;
        if (client == null || client.settingsManager == null) {
            return fallback;
        }
        SettingsManager sm = client.settingsManager;
        Setting s = sm.getSettingByName(this, name);
        return s != null ? s.getValString() : fallback;
    }
}
