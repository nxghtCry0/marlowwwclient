package com.eclipseware.imnotcheatingyouare.client.module.impl;
import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import net.minecraft.client.KeyMapping;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
public class HitSelect extends Module {
private boolean wasHit = false;
private int punishDelayTicks = 0;
private int lastHurtTime = 0;
public HitSelect() {
super("HitSelect", Category.Combat);
}
@Override
public void onTick() {
if (!isToggled() || mc.player == null) return;
if (mc.player.hurtTime > 0 && lastHurtTime == 0) {
wasHit = true;
punishDelayTicks = 0;
}
lastHurtTime = mc.player.hurtTime;
if (wasHit) {
punishDelayTicks++;
}
}
public boolean canAttack(Entity target) {
if (!this.isToggled() || mc.player == null) return true;
Setting modeSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Mode");
String mode = modeSetting != null ? modeSetting.getValString() : "HurtTime";
Setting autoPunishSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Auto Punish");
        if (autoPunishSetting != null && autoPunishSetting.getValBoolean()) {
            if (wasHit) {
                Setting delaySetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Punish Delay (Ticks)");
                int punishDelay = delaySetting != null ? (int) delaySetting.getValDouble() : 3;
                if (punishDelayTicks < punishDelay) {
                    return false;
                }
                wasHit = false;
                return true;
            }
        }
if (target instanceof LivingEntity) {
LivingEntity livingTarget = (LivingEntity) target;
if (mode.equals("Criticals")) {
return !mc.player.onGround() && mc.player.getDeltaMovement().y < 0.0 && !mc.player.onClimbable() && !mc.player.isInWater();
} else if (mode.equals("HurtTime")) {
Setting hurtTimeSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Max HurtTime");
int maxHurtTime = hurtTimeSetting != null ? (int) hurtTimeSetting.getValDouble() : 5;
return livingTarget.hurtTime <= maxHurtTime;
}
}
return true;
}
public void performPunishAttack() {
KeyMapping attackKey = mc.options.keyAttack;
KeyMapping.click(attackKey.getDefaultKey());
}
@Override
public void onDisable() {
wasHit = false;
punishDelayTicks = 0;
lastHurtTime = 0;
}
}