package com.eclipseware.imnotcheatingyouare.mixin.client;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityMixin {

    @Inject(method = "isInvisibleTo(Lnet/minecraft/world/entity/player/Player;)Z", at = @At("HEAD"), cancellable = true)
    private void onIsInvisibleTo(net.minecraft.world.entity.player.Player player, CallbackInfoReturnable<Boolean> cir) {
        Module trueSight = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("TrueSight");
        if (trueSight != null && trueSight.isToggled()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "isSprinting", at = @At("HEAD"), cancellable = true)
    private void onIsSprinting(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof net.minecraft.client.player.LocalPlayer) {
            if (com.eclipseware.imnotcheatingyouare.client.module.impl.WTap.shouldSilentStopSprint() || com.eclipseware.imnotcheatingyouare.client.module.impl.AutoTotem.shouldPauseInputs()) {
                cir.setReturnValue(false);
            }
        }
    }

    @Inject(method = "isInWall", at = @At("HEAD"), cancellable = true)
    private void onIsInWall(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof net.minecraft.client.player.LocalPlayer) {
            Module freecam = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("Freecam");
            if (freecam != null && freecam.isToggled()) {
                cir.setReturnValue(false);
            }
        }
    }

    @ModifyVariable(method = "move", at = @At("HEAD"), argsOnly = true)
    private net.minecraft.world.phys.Vec3 modifyMove(net.minecraft.world.phys.Vec3 vec) {
        if ((Object) this instanceof net.minecraft.client.player.LocalPlayer player) {
            Module bridgeAssist = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("BridgeAssist");
            if (bridgeAssist != null && bridgeAssist.isToggled()) {
                Setting modeSet = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(bridgeAssist, "Mode");
                if (modeSet != null && modeSet.getValString().equals("Blatant")) {
                    if (player.onGround()) {
                        double d = vec.x;
                        double e = vec.z;
                        while (d != 0.0 && player.level().noCollision(player, player.getBoundingBox().move(d, (double)(-player.maxUpStep()), 0.0))) {
                            if (d < 0.05 && d >= -0.05) {
                                d = 0.0;
                            } else if (d > 0.0) {
                                d -= 0.05;
                            } else {
                                d += 0.05;
                            }
                        }

                        while (e != 0.0 && player.level().noCollision(player, player.getBoundingBox().move(0.0, (double)(-player.maxUpStep()), e))) {
                            if (e < 0.05 && e >= -0.05) {
                                e = 0.0;
                            } else if (e > 0.0) {
                                e -= 0.05;
                            } else {
                                e += 0.05;
                            }
                        }

                        while (d != 0.0 && e != 0.0 && player.level().noCollision(player, player.getBoundingBox().move(d, (double)(-player.maxUpStep()), e))) {
                            if (d < 0.05 && d >= -0.05) {
                                d = 0.0;
                            } else if (d > 0.0) {
                                d -= 0.05;
                            } else {
                                d += 0.05;
                            }

                            if (e < 0.05 && e >= -0.05) {
                                e = 0.0;
                            } else if (e > 0.0) {
                                e -= 0.05;
                            } else {
                                e += 0.05;
                            }
                        }

                        return new net.minecraft.world.phys.Vec3(d, vec.y, e);
                    }
                }
            }
        }
        return vec;
    }
}
