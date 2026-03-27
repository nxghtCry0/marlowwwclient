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

public AutoShieldBreaker() {
    super("AutoShieldBreaker", Category.Combat);
}

public boolean handleAttack(Entity target, Player player) {
    if (!this.isToggled() || !(target instanceof LivingEntity)) return false;

    LivingEntity livingTarget = (LivingEntity) target;

    // Check if the target is actively blocking with a shield
    if (!livingTarget.isBlocking()) return false;
    
    // Math check: Are we actually hitting the front of the shield?
    if (!isShieldBlockingUs(livingTarget, player)) return false;

        Setting delaySetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Delay (ms)");
        long delay = delaySetting != null ? (long) delaySetting.getValDouble() : 0;

        // If we are still within the delay cooldown, don't attempt to break, let standard hit proceed
        if (System.currentTimeMillis() - lastBreakTime < delay) {
            return false;
        }

        int axeSlot = findAxeInHotbar(player);
        if (axeSlot == -1) return false; // No axe found in hotbar

        Setting modeSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Mode");
        String mode = modeSetting != null ? modeSetting.getValString() : "Swap";

        int oldSlot = player.getInventory().getSelectedSlot();

    // If we are already holding the axe, just let the attack go through normally
    if (oldSlot == axeSlot) {
        lastBreakTime = System.currentTimeMillis();
        return false; 
    }

    if (mode.equals("Silent")) {
        // Silent Mode: Send packets directly without visually changing your hand client-side
        mc.getConnection().send(new ServerboundSetCarriedItemPacket(axeSlot));
        mc.getConnection().send(ServerboundInteractPacket.createAttackPacket(target, player.isShiftKeyDown()));
        player.swing(InteractionHand.MAIN_HAND);
        mc.getConnection().send(new ServerboundSetCarriedItemPacket(oldSlot));
        
        lastBreakTime = System.currentTimeMillis();
        return true; // Cancel the physical client-side attack since we spoofed it via packets
    } else {
// Swap Mode: Physically change the hotbar slot and let the normal attack proceed
player.getInventory().setSelectedSlot(axeSlot);

        // CRITICAL FIX: Force the server to acknowledge the axe swap IMMEDIATELY before the attack packet sends
        if (mc.getConnection() != null) {
            mc.getConnection().send(new ServerboundSetCarriedItemPacket(axeSlot));
        }

        lastBreakTime = System.currentTimeMillis();
        
        Setting swapBackSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Swap Back");
        if (swapBackSetting != null && swapBackSetting.getValBoolean()) {
            Setting swapDelaySetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Swap Back Delay (ms)");
            long swapDelay = swapDelaySetting != null ? (long) swapDelaySetting.getValDouble() : 0;
            
            needsSwapBack = true;
            swapBackTime = System.currentTimeMillis() + swapDelay;
            originalSlot = oldSlot;
        }
        
        return false; // Don't cancel, let the game handle the attack with the newly selected axe
    }
}

@Override
public void onTick() {
    if (needsSwapBack && mc.player != null) {
        if (System.currentTimeMillis() >= swapBackTime) {
            mc.player.getInventory().setSelectedSlot(originalSlot);
            // Sync the swap-back to the server as well
            if (mc.getConnection() != null) {
                mc.getConnection().send(new ServerboundSetCarriedItemPacket(originalSlot));
            }
            needsSwapBack = false;
        }
    }
}

@Override
public void onDisable() {
    needsSwapBack = false;
}

private int findAxeInHotbar(Player player) {
// Hotbar slots are indices 0-8 in the main inventory
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
    
    // Vector FROM attacker TO defender (ignoring Y axis for horizontal angle check)
    Vec3 attackerToDefender = defenderPos.subtract(attackerPos).normalize();
    attackerToDefender = new Vec3(attackerToDefender.x, 0.0, attackerToDefender.z);
    
    // Defender's look vector (ignoring Y axis)
    Vec3 defenderLook = target.getViewVector(1.0F);
    defenderLook = new Vec3(defenderLook.x, 0.0, defenderLook.z);
    
    // If the dot product is less than 0, their look vector is opposing our attack vector
    // Meaning they are facing us, and the shield will successfully block the hit.
    return attackerToDefender.dot(defenderLook) < 0.0;
}

}