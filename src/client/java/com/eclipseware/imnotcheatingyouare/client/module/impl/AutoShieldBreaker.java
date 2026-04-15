package com.eclipseware.imnotcheatingyouare.client.module.impl;
import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
public class AutoShieldBreaker extends Module {
private long lastBreakTime = 0;
private boolean needsSwapBack = false;
private long swapBackTime = 0;
private int originalSlot = -1;
private int axeSlot = -1;
private int ticksWaited = 0;
public AutoShieldBreaker() {
super("AutoShieldBreaker", Category.Combat);
}
public boolean shouldCancelAttack(Entity target) {
if (!this.isToggled() || !(target instanceof LivingEntity)) return false;
LivingEntity livingTarget = (LivingEntity) target;
if (!livingTarget.isBlocking()) return false;
if (!isShieldBlockingUs(livingTarget, mc.player)) return false;

Setting delaySetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Delay (ms)");
long delay = delaySetting != null ? (long) delaySetting.getValDouble() : 0;
if (System.currentTimeMillis() - lastBreakTime < delay) {
return true;
}
axeSlot = findAxeInHotbar(mc.player);
if (axeSlot == -1) return false;

Setting modeSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Mode");
String mode = modeSetting != null ? modeSetting.getValString() : "Swap";
int oldSlot = mc.player.getInventory().getSelectedSlot();
if (oldSlot == axeSlot) {
lastBreakTime = System.currentTimeMillis();
return false;
}
if (mode.equals("Silent")) {
if (mc.getConnection() != null) {
mc.getConnection().send(new ServerboundSetCarriedItemPacket(axeSlot));
mc.getConnection().send(ServerboundInteractPacket.createAttackPacket(target, mc.player.isShiftKeyDown()));
}
mc.player.getInventory().setSelectedSlot(axeSlot);
mc.player.swing(InteractionHand.MAIN_HAND);
needsSwapBack = true;
originalSlot = oldSlot;
ticksWaited = 0;
swapBackTime = 0;
lastBreakTime = System.currentTimeMillis();
return true;
} else {
mc.player.getInventory().setSelectedSlot(axeSlot);
if (mc.getConnection() != null) {
mc.getConnection().send(new ServerboundSetCarriedItemPacket(axeSlot));
}
Setting swapBackSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Swap Back");
if (swapBackSetting != null && swapBackSetting.getValBoolean()) {
Setting swapDelaySetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Swap Back Delay (ms)");
long swapDelay = swapDelaySetting != null ? (long) swapDelaySetting.getValDouble() : 150;
needsSwapBack = true;
swapBackTime = System.currentTimeMillis() + swapDelay;
originalSlot = oldSlot;
}
lastBreakTime = System.currentTimeMillis();
return false;
}
}
public boolean handleAttack(Entity target, Player player) {
if (!this.isToggled() || !(target instanceof LivingEntity)) return false;
LivingEntity livingTarget = (LivingEntity) target;
if (!livingTarget.isBlocking()) return false;
if (!isShieldBlockingUs(livingTarget, player)) return false;
Setting delaySetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Delay (ms)");
long delay = delaySetting != null ? (long) delaySetting.getValDouble() : 0;
if (System.currentTimeMillis() - lastBreakTime < delay) {
return false;
}
axeSlot = findAxeInHotbar(player);
if (axeSlot == -1) return false;
Setting modeSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Mode");
String mode = modeSetting != null ? modeSetting.getValString() : "Swap";
int oldSlot = player.getInventory().getSelectedSlot();
if (oldSlot == axeSlot) {
lastBreakTime = System.currentTimeMillis();
return false;
}
if (mode.equals("Silent")) {
if (mc.getConnection() != null) {
mc.getConnection().send(new ServerboundSetCarriedItemPacket(axeSlot));
mc.getConnection().send(ServerboundInteractPacket.createAttackPacket(target, player.isShiftKeyDown()));
}
// Hotbar desync fix
player.getInventory().setSelectedSlot(axeSlot);
player.swing(InteractionHand.MAIN_HAND);
needsSwapBack = true;
originalSlot = oldSlot;
ticksWaited = 0;
swapBackTime = 0;
lastBreakTime = System.currentTimeMillis();
return true;
} else {
player.getInventory().setSelectedSlot(axeSlot);
if (mc.getConnection() != null) {
mc.getConnection().send(new ServerboundSetCarriedItemPacket(axeSlot));
}
Setting swapBackSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Swap Back");
if (swapBackSetting != null && swapBackSetting.getValBoolean()) {
Setting swapDelaySetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Swap Back Delay (ms)");
long swapDelay = swapDelaySetting != null ? (long) swapDelaySetting.getValDouble() : 150;
needsSwapBack = true;
swapBackTime = System.currentTimeMillis() + swapDelay;
originalSlot = oldSlot;
}
lastBreakTime = System.currentTimeMillis();
return false;
}
}
@Override
public void onTick() {
if (needsSwapBack && mc.player != null && mc.getConnection() != null) {
if (swapBackTime == 0) {
ticksWaited++;
if (ticksWaited >= 1) {
mc.player.getInventory().setSelectedSlot(originalSlot);
mc.getConnection().send(new ServerboundSetCarriedItemPacket(originalSlot));
needsSwapBack = false;
axeSlot = -1;
}
} else if (System.currentTimeMillis() >= swapBackTime) {
mc.player.getInventory().setSelectedSlot(originalSlot);
mc.getConnection().send(new ServerboundSetCarriedItemPacket(originalSlot));
needsSwapBack = false;
swapBackTime = 0;
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
swapBackTime = 0;
axeSlot = -1;
}
private int findAxeInHotbar(Player player) {
for (int i = 0; i < 9; i++) {
ItemStack stack = player.getInventory().getItem(i);
if (stack.getItem() instanceof AxeItem) {
return i;
}
}
return -1;
}
private boolean isShieldBlockingUs(LivingEntity target, Player player) {
Vec3 attackerPos = player.position();
Vec3 defenderPos = target.position();
Vec3 attackerToDefender = defenderPos.subtract(attackerPos).normalize();
attackerToDefender = new Vec3(attackerToDefender.x, 0.0, attackerToDefender.z);
Vec3 defenderLook = target.getViewVector(1.0F);
defenderLook = new Vec3(defenderLook.x, 0.0, defenderLook.z);
return attackerToDefender.dot(defenderLook) < 0.0;
}
}