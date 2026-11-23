package com.eclipseware.imnotcheatingyouare.mixin.client;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.module.ModuleManager;
import com.eclipseware.imnotcheatingyouare.client.module.impl.AutoTotem;
import com.eclipseware.imnotcheatingyouare.client.module.impl.Hitboxes;
import com.eclipseware.imnotcheatingyouare.client.module.impl.STap;
import com.eclipseware.imnotcheatingyouare.client.module.impl.WTap;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.setting.SettingsManager;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityMixin {

    @Inject(method = "getPickRadius", at = @At("RETURN"), cancellable = true)
    private void onGetPickRadius(CallbackInfoReturnable<Float> cir) {
        Entity self = (Entity) (Object) this;
        float extra = Hitboxes.getExtraMarginFor(self);
        if (extra > 0.0f) {
            cir.setReturnValue(cir.getReturnValue() + extra);
        }
    }

    @Inject(method = "isInvisibleTo(Lnet/minecraft/world/entity/player/Player;)Z", at = @At("HEAD"), cancellable = true)
    private void onIsInvisibleTo(Player player, CallbackInfoReturnable<Boolean> cir) {
        ImnotcheatingyouareClient client = ImnotcheatingyouareClient.INSTANCE;
        ModuleManager mm = client != null ? client.moduleManager : null;
        Module trueSight = mm != null ? mm.getModule("TrueSight") : null;
        if (trueSight != null && trueSight.isToggled()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "isSprinting", at = @At("HEAD"), cancellable = true)
    private void onIsSprinting(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof LocalPlayer) {
            if (WTap.shouldSilentStopSprint() || STap.shouldSilentStopSprint() || AutoTotem.shouldPauseInputs()) {
                cir.setReturnValue(false);
            }
        }
    }

    @ModifyVariable(method = "move", at = @At("HEAD"), argsOnly = true)
    private Vec3 modifyMove(Vec3 vec) {
        if ((Object) this instanceof LocalPlayer player) {
            ImnotcheatingyouareClient client = ImnotcheatingyouareClient.INSTANCE;
            ModuleManager mm = client != null ? client.moduleManager : null;
            SettingsManager sm = client != null ? client.settingsManager : null;
            Module bridgeAssist = mm != null ? mm.getModule("BridgeAssist") : null;
            if (bridgeAssist != null && bridgeAssist.isToggled() && sm != null) {
                Setting modeSet = sm.getSettingByName(bridgeAssist, "Mode");
                if (modeSet != null && "Blatant".equals(modeSet.getValString()) && player.onGround()) {
                    Level level = player.level();
                    AABB box = player.getBoundingBox();
                    double step = -player.maxUpStep();
                    double d = vec.x;
                    double e = vec.z;

                    while (d != 0.0 && level.noCollision(player, box.move(d, step, 0.0))) {
                        if (d < 0.05 && d >= -0.05) {
                            d = 0.0;
                        } else if (d > 0.0) {
                            d -= 0.05;
                        } else {
                            d += 0.05;
                        }
                    }

                    while (e != 0.0 && level.noCollision(player, box.move(0.0, step, e))) {
                        if (e < 0.05 && e >= -0.05) {
                            e = 0.0;
                        } else if (e > 0.0) {
                            e -= 0.05;
                        } else {
                            e += 0.05;
                        }
                    }

                    while (d != 0.0 && e != 0.0 && level.noCollision(player, box.move(d, step, e))) {
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

                    return new Vec3(d, vec.y, e);
                }
            }
        }
        return vec;
    }
}
