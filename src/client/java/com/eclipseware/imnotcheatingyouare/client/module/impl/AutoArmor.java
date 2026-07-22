package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;

public class AutoArmor extends Module {
    private long lastSwapTime = 0;
    private long currentRandomDelay = 150L;

    public AutoArmor() {
        super("AutoArmor", Category.Combat);
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Delay (ms)", this, 200.0, 50.0, 1000.0, true));
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.gameMode == null || mc.level == null) return;
        
        if (mc.gui.screen() != null && !(mc.gui.screen() instanceof net.minecraft.client.gui.screens.inventory.InventoryScreen)) {
            return;
        }

        if (System.currentTimeMillis() - lastSwapTime < currentRandomDelay) {
            return;
        }

        for (int i = 0; i < 4; i++) {
            EquipmentSlot slotType = getSlotType(i);
            int bestSlot = getBestArmorSlot(slotType);
            
            if (bestSlot != -1) {
                ItemStack currentArmor = mc.player.getItemBySlot(slotType);
                
                if (!currentArmor.isEmpty()) {
                    int armorContainerSlot = 8 - i; 
                    mc.gameMode.handleContainerInput(mc.player.inventoryMenu.containerId, armorContainerSlot, 0, ContainerInput.QUICK_MOVE, mc.player);
                    lastSwapTime = System.currentTimeMillis();
                    currentRandomDelay = 120 + (long) (Math.random() * 130);
                    return; 
                } else {
                    int containerSlot = bestSlot < 9 ? bestSlot + 36 : bestSlot;
                    mc.gameMode.handleContainerInput(mc.player.inventoryMenu.containerId, containerSlot, 0, ContainerInput.QUICK_MOVE, mc.player);
                    lastSwapTime = System.currentTimeMillis();
                    currentRandomDelay = 120 + (long) (Math.random() * 130);
                    return; 
                }
            }
        }
    }

    private EquipmentSlot getSlotType(int index) {
        switch (index) {
            case 0: return EquipmentSlot.FEET;
            case 1: return EquipmentSlot.LEGS;
            case 2: return EquipmentSlot.CHEST;
            case 3: return EquipmentSlot.HEAD;
            default: return EquipmentSlot.FEET;
        }
    }

    private int getBestArmorSlot(EquipmentSlot slotType) {
        int bestSlot = -1;
        float bestValue = getArmorValue(mc.player.getItemBySlot(slotType));

        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (!stack.isEmpty()) {
                net.minecraft.world.item.equipment.Equippable equippable = stack.get(net.minecraft.core.component.DataComponents.EQUIPPABLE);
                if (equippable != null && equippable.slot() == slotType) {
                    float value = getArmorValue(stack);
                    if (value > bestValue) {
                        bestValue = value;
                        bestSlot = i;
                    }
                }
            }
        }
        return bestSlot;
    }

    private float getArmorValue(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return -1.0f;
        }

        double armor = 0.0;
        double toughness = 0.0;

        net.minecraft.world.item.component.ItemAttributeModifiers attributeModifiers = stack.get(net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS);
        if (attributeModifiers != null) {
            for (net.minecraft.world.item.component.ItemAttributeModifiers.Entry entry : attributeModifiers.modifiers()) {
                if (entry.attribute().value() == net.minecraft.world.entity.ai.attributes.Attributes.ARMOR.value()) {
                    armor += entry.modifier().amount();
                } else if (entry.attribute().value() == net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS.value()) {
                    toughness += entry.modifier().amount();
                }
            }
        }
        
        return (float) (armor + toughness * 0.5);
    }
}
