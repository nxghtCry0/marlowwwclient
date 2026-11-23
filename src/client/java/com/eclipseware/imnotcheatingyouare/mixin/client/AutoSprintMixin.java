package com.eclipseware.imnotcheatingyouare.mixin.client;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.module.ModuleManager;
import com.eclipseware.imnotcheatingyouare.client.module.impl.AutoTotem;
import com.eclipseware.imnotcheatingyouare.client.module.impl.STap;
import com.eclipseware.imnotcheatingyouare.client.module.impl.SprintResetController;
import com.eclipseware.imnotcheatingyouare.client.module.impl.WTap;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public class AutoSprintMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        ImnotcheatingyouareClient client = ImnotcheatingyouareClient.INSTANCE;
        if (client == null || client.moduleManager == null) {
            return;
        }
        ModuleManager mm = client.moduleManager;
        Module sprintMod = mm.getModule("AutoSprint");
        if (sprintMod == null || !sprintMod.isToggled()) {
            return;
        }

        LocalPlayer player = (LocalPlayer) (Object) this;
        if (player.zza > 0 && !player.isShiftKeyDown() && !player.isUsingItem()) {
            if (!SprintResetController.isSuppressingSprint()
                && !WTap.shouldSilentStopSprint()
                && !STap.shouldSilentStopSprint()
                && !AutoTotem.shouldPauseInputs()) {
                player.setSprinting(true);
            }
        }
    }
}
