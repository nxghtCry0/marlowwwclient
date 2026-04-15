package com.eclipseware.imnotcheatingyouare.mixin.client;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.impl.ESP;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityGlowMixin {

    @Inject(method = "isCurrentlyGlowing", at = @At("HEAD"), cancellable = true)
    private void forceGlowForESP(CallbackInfoReturnable<Boolean> cir) {
        if (ImnotcheatingyouareClient.INSTANCE == null || ImnotcheatingyouareClient.INSTANCE.moduleManager == null) return;
        
        ESP esp = (ESP) ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("ESP");
        if (esp != null && esp.isToggled()) {
            Setting mode = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(esp, "Mode");
            if (mode != null && (mode.getValString().equals("Outline") || mode.getValString().equals("Hybrid"))) {
                
                Entity entity = (Entity) (Object) this;
                Setting showMobs = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(esp, "Show Mobs");
                boolean mobs = showMobs != null && showMobs.getValBoolean();

                if (entity instanceof Player && entity != Minecraft.getInstance().player) {
                    cir.setReturnValue(true);
                } else if (entity instanceof Mob && mobs) {
                    cir.setReturnValue(true);
                }
            }
        }
    }

    @Inject(method = "getTeamColor", at = @At("HEAD"), cancellable = true)
    private void forceGlowColor(CallbackInfoReturnable<Integer> cir) {
        if (ImnotcheatingyouareClient.INSTANCE == null || ImnotcheatingyouareClient.INSTANCE.moduleManager == null) return;

        ESP esp = (ESP) ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("ESP");
        if (esp != null && esp.isToggled()) {
            Setting mode = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(esp, "Mode");
            if (mode != null && (mode.getValString().equals("Outline") || mode.getValString().equals("Hybrid"))) {
                
                Entity entity = (Entity) (Object) this;
                Setting showMobs = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(esp, "Show Mobs");
                boolean mobs = showMobs != null && showMobs.getValBoolean();

                if (entity instanceof Player && entity != Minecraft.getInstance().player) {
                    cir.setReturnValue(0x9B3CFF);
                } else if (entity instanceof Mob && mobs) {
                    cir.setReturnValue(0xFF6432); 
                }
            }
        }
    }
}