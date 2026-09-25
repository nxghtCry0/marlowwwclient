package com.eclipseware.imnotcheatingyouare.mixin.client;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.impl.ESP;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityGlowMixin {

    @Unique
    private static ESP activeGlowEsp() {
        if (ImnotcheatingyouareClient.INSTANCE == null || ImnotcheatingyouareClient.INSTANCE.moduleManager == null) return null;
        if (!(ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("ESP") instanceof ESP esp) || !esp.isToggled()) return null;
        Setting mode = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(esp, "Mode");
        if (mode == null) return null;
        String modeStr = mode.getValString();
        if (modeStr.equalsIgnoreCase("Glow") || modeStr.equalsIgnoreCase("Hybrid") || modeStr.equalsIgnoreCase("Shader")) return esp;
        return null;
    }

    @Unique
    private boolean isSelfShaded() {
        return (Object) this == net.minecraft.client.Minecraft.getInstance().player
                && com.eclipseware.imnotcheatingyouare.client.module.impl.SelfShader.INSTANCE != null
                && com.eclipseware.imnotcheatingyouare.client.module.impl.SelfShader.INSTANCE.body();
    }

    @Inject(method = "isCurrentlyGlowing", at = @At("HEAD"), cancellable = true)
    private void forceGlowForESP(CallbackInfoReturnable<Boolean> cir) {
        if (isSelfShaded()) {
            cir.setReturnValue(true);
            return;
        }
        ESP esp = activeGlowEsp();
        if (esp != null && esp.shouldHighlight((Entity) (Object) this)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "getTeamColor", at = @At("HEAD"), cancellable = true)
    private void forceGlowColor(CallbackInfoReturnable<Integer> cir) {
        if (isSelfShaded()) {
            cir.setReturnValue(com.eclipseware.imnotcheatingyouare.client.module.impl.SelfShader.INSTANCE.color().getRGB() & 0xFFFFFF);
            return;
        }
        ESP esp = activeGlowEsp();
        Entity entity = (Entity) (Object) this;
        if (esp != null && esp.shouldHighlight(entity)) {
            cir.setReturnValue(esp.getEntityColor(entity).getRGB() & 0xFFFFFF);
        }
    }
}
