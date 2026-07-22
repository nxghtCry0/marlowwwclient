package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.utils.ModuleUtils;
import com.eclipseware.imnotcheatingyouare.mixin.client.MinecraftAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class LungeAssist extends Module {
  public static LungeAssist INSTANCE;
  
  private boolean isSwapping = false;
  private int originalSlot = -1;
  private int swapTicks = 0;
  private int targetTicks = 3;
  
  public LungeAssist() {
    super("LungeSwap", Category.Mace, "Automatically swaps to lunge spear and attacks.");
    INSTANCE = this;
  }
  
  @Override
  public void onEnable() {
    if (mc == null || mc.player == null || mc.level == null) {
      setToggled(false);
      return;
    } 
    int currentSlot = mc.player.getInventory().getSelectedSlot();
    int lungeSlot = findLungeSlot((Player)mc.player);
    if (lungeSlot == -1) {
      setToggled(false);
      return;
    } 
    if (currentSlot == lungeSlot) {
      Minecraft minecraft = mc;
      if (minecraft instanceof MinecraftAccessor) {
        MinecraftAccessor accessor = (MinecraftAccessor)minecraft;
        accessor.invokeStartAttack();
      } 
      setToggled(false);
      return;
    } 
    this.originalSlot = currentSlot;
    this.targetTicks = 3;
    this.isSwapping = true;
    this.swapTicks = 0;
    equip(lungeSlot);
    if (mc.player.getAttackStrengthScale(0.0F) >= 1.0F) {
      Minecraft minecraft = mc;
      if (minecraft instanceof MinecraftAccessor) {
        MinecraftAccessor accessor = (MinecraftAccessor)minecraft;
        accessor.invokeStartAttack();
      } 
      this.swapTicks = 1;
    } 
  }
  
  public boolean onPlayerAttack() {
    return false;
  }
  
  @Override
  public void onTick() {
    if (!this.isSwapping || mc == null || mc.player == null)
      return; 
    this.swapTicks++;
    if (this.swapTicks >= this.targetTicks) {
      if (this.originalSlot != -1 && this.originalSlot != mc.player.getInventory().getSelectedSlot())
        equip(this.originalSlot); 
      setToggled(false);
    } 
  }
  
  @Override
  public void onDisable() {
    this.isSwapping = false;
    this.originalSlot = -1;
    this.swapTicks = 0;
  }
  
  private void equip(int slot) {
    if (mc.player == null)
      return; 
    if (mc.player.getInventory().getSelectedSlot() != slot) {
      ModuleUtils.switchToSlot(slot);
      mc.player.getInventory().setSelectedSlot(slot);
    } 
  }
  
  private int findLungeSlot(Player player) {
    int fallback = -1;
    for (int i = 0; i < 9; i++) {
      ItemStack stack = player.getInventory().getItem(i);
      if (isLungeSpear(stack))
        return i; 
      if (fallback == -1 && !stack.isEmpty() && isSpearItem(stack))
        fallback = i; 
    } 
    return fallback;
  }
  
  private boolean isLungeSpear(ItemStack stack) {
    if (stack.isEmpty())
      return false; 
    if (!isSpearItem(stack))
      return false; 
    for (Holder<?> enchant : stack.getEnchantments().keySet()) {
      if (enchant.unwrapKey().isPresent() && ((ResourceKey)enchant.unwrapKey().get()).toString().toLowerCase().contains("lunge"))
        return true; 
    } 
    return false;
  }
  
  private boolean isSpearItem(ItemStack stack) {
    String itemName = stack.getItem().toString().toLowerCase();
    String registryName = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().toLowerCase();
    return (itemName.contains("spear") || registryName.contains("spear") || itemName.contains("trident") || registryName.contains("trident"));
  }
}