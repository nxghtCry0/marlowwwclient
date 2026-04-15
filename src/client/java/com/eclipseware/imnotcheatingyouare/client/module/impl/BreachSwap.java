package com.eclipseware.imnotcheatingyouare.client.module.impl;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MaceItem;
import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
public class BreachSwap extends Module {
private boolean needsSwapBack = false;
private int originalSlot = -1;
private int maceSlot = -1;
private int ticksWaited = 0;
public BreachSwap() {
super("BreachSwap", Category.Combat);
}
public boolean handleAttack(Entity target, Player player) {
if (!this.isToggled()) return false;
maceSlot = findBreachMace(player);
if (maceSlot == -1) return false;
int oldSlot = player.getInventory().getSelectedSlot();
if (oldSlot == maceSlot) return false;
Setting modeSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Mode");
String mode = modeSetting != null ? modeSetting.getValString() : "Swap";
if (mode.equals("Silent")) {
if (mc.getConnection() != null) {
mc.getConnection().send(new ServerboundSetCarriedItemPacket(maceSlot));
mc.getConnection().send(ServerboundInteractPacket.createAttackPacket(target, player.isShiftKeyDown()));
}
player.swing(InteractionHand.MAIN_HAND);
player.resetAttackStrengthTicker();
needsSwapBack = true;
originalSlot = oldSlot;
ticksWaited = 0;
return true;
} else {
player.getInventory().setSelectedSlot(maceSlot);
if (mc.getConnection() != null) {
mc.getConnection().send(new ServerboundSetCarriedItemPacket(maceSlot));
}
needsSwapBack = true;
originalSlot = oldSlot;
ticksWaited = 0;
return false;
}
}
@Override
public void onTick() {
if (needsSwapBack && mc.player != null && mc.getConnection() != null) {
ticksWaited++;
if (ticksWaited >= 1) {
mc.player.getInventory().setSelectedSlot(originalSlot);
mc.getConnection().send(new ServerboundSetCarriedItemPacket(originalSlot));
needsSwapBack = false;
maceSlot = -1;
}
}
}
@Override
public void onDisable() {
if (needsSwapBack && mc.player != null && mc.getConnection() != null) {
mc.player.getInventory().setSelectedSlot(originalSlot);
mc.getConnection().send(new ServerboundSetCarriedItemPacket(originalSlot));
}
needsSwapBack = false;
maceSlot = -1;
}
private int findBreachMace(Player player) {
for (int i = 0; i < 9; i++) {
ItemStack stack = player.getInventory().getItem(i);
if (stack.getItem() instanceof MaceItem) {
for (var enchant : stack.getEnchantments().keySet()) {
if (enchant.unwrapKey().isPresent() && enchant.unwrapKey().get().toString().contains("breach")) {
return i;
}
}
}
}
return -1;
}
}