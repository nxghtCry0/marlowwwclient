package com.eclipseware.imnotcheatingyouare.mixin.client;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.module.ModuleManager;
import com.eclipseware.imnotcheatingyouare.client.module.impl.AttributeSwap;
import com.eclipseware.imnotcheatingyouare.client.module.impl.AutoShieldBreaker;
import com.eclipseware.imnotcheatingyouare.client.module.impl.Backtrack;
import com.eclipseware.imnotcheatingyouare.client.module.impl.BreachSwap;
import com.eclipseware.imnotcheatingyouare.client.module.impl.HitSelect;
import com.eclipseware.imnotcheatingyouare.client.module.impl.HitSwap;
import com.eclipseware.imnotcheatingyouare.client.module.impl.KillSounds;
import com.eclipseware.imnotcheatingyouare.client.module.impl.KnockbackDisplacement;
import com.eclipseware.imnotcheatingyouare.client.module.impl.MaceDTap;
import com.eclipseware.imnotcheatingyouare.client.module.impl.SpearMaceDTap;
import com.eclipseware.imnotcheatingyouare.client.module.impl.TargetHUD;
import com.eclipseware.imnotcheatingyouare.client.module.impl.Triggerbot;
import com.eclipseware.imnotcheatingyouare.client.module.impl.WebStun;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.setting.SettingsManager;
import com.eclipseware.imnotcheatingyouare.client.utils.RotationManager;
import com.eclipseware.imnotcheatingyouare.client.utils.SilentAimUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.PiercingWeapon;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {

    @Shadow
    private float destroyProgress;

    @Unique
    private boolean kbShouldRevert = false;

    @Unique
    private float eclipseSavedUseYRot;

    @Unique
    private float eclipseSavedUseXRot;

    @Unique
    private boolean eclipseWasUseSpoofing = false;

    @Unique
    private void eclipseApplyUseRotationSpoof(Player player) {
        eclipseWasUseSpoofing = false;
        if (player == null) {
            return;
        }
        boolean silentUtil = SilentAimUtil.isActive();
        boolean rotMgr = RotationManager.isActive();
        if (!silentUtil && !rotMgr) {
            return;
        }

        eclipseSavedUseYRot = player.getYRot();
        eclipseSavedUseXRot = player.getXRot();
        eclipseWasUseSpoofing = true;

        float rawYaw = silentUtil ? SilentAimUtil.getYaw() : RotationManager.getServerYaw();
        float rawPitch = silentUtil ? SilentAimUtil.getPitch() : RotationManager.getServerPitch();
        float gcd = RotationManager.getGCD();
        if (gcd < 0.001f) {
            gcd = 0.15f;
        }

        float refYaw = Float.isNaN(RotationManager.lastSentYaw) ? eclipseSavedUseYRot : RotationManager.lastSentYaw;
        float refPitch = Float.isNaN(RotationManager.lastSentPitch) ? eclipseSavedUseXRot : RotationManager.lastSentPitch;
        int yawSteps = Math.round(Mth.wrapDegrees(rawYaw - refYaw) / gcd);
        int pitchSteps = Math.round((rawPitch - refPitch) / gcd);

        float finalYaw = refYaw + yawSteps * gcd;
        float finalPitch = Mth.clamp(refPitch + pitchSteps * gcd, -90.0f, 90.0f);
        player.setYRot(finalYaw);
        player.setXRot(finalPitch);
        RotationManager.lastSentYaw = finalYaw;
        RotationManager.lastSentPitch = finalPitch;
    }

    @Unique
    private void eclipseRestoreUseRotationSpoof(Player player) {
        if (!eclipseWasUseSpoofing || player == null) {
            return;
        }
        player.setYRot(eclipseSavedUseYRot);
        player.setXRot(eclipseSavedUseXRot);
        eclipseWasUseSpoofing = false;
    }

    @Inject(method = "useItem", at = @At("HEAD"))
    private void onUseItemHead(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        eclipseApplyUseRotationSpoof(player);
    }

    @Inject(method = "useItem", at = @At("RETURN"))
    private void onUseItemReturn(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        eclipseRestoreUseRotationSpoof(player);
    }

    @Inject(method = "useItemOn", at = @At("HEAD"))
    private void onUseItemOnHead(
        LocalPlayer player,
        InteractionHand hand,
        BlockHitResult hitResult,
        CallbackInfoReturnable<InteractionResult> cir
    ) {
        eclipseApplyUseRotationSpoof(player);
    }

    @Inject(method = "useItemOn", at = @At("RETURN"))
    private void onUseItemOnReturn(
        LocalPlayer player,
        InteractionHand hand,
        BlockHitResult hitResult,
        CallbackInfoReturnable<InteractionResult> cir
    ) {
        eclipseRestoreUseRotationSpoof(player);
    }

    @Inject(method = "continueDestroyBlock", at = @At("RETURN"))
    private void onContinueDestroyBlock(BlockPos pos, Direction directionFace, CallbackInfoReturnable<Boolean> cir) {
        ImnotcheatingyouareClient client = ImnotcheatingyouareClient.INSTANCE;
        ModuleManager mm = client != null ? client.moduleManager : null;
        SettingsManager sm = client != null ? client.settingsManager : null;
        if (mm == null || sm == null) {
            return;
        }
        Module fastBreak = mm.getModule("FastBreak");
        if (fastBreak != null && fastBreak.isToggled()) {
            Setting speedSetting = sm.getSettingByName(fastBreak, "Speed Multiplier");
            if (speedSetting != null) {
                float mult = (float) speedSetting.getValDouble();
                this.destroyProgress *= mult;
            }
        }
    }

    @Inject(method = "destroyBlock", at = @At("HEAD"))
    private void onDestroyBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        ImnotcheatingyouareClient client = ImnotcheatingyouareClient.INSTANCE;
        ModuleManager mm = client != null ? client.moduleManager : null;
        if (mm == null) {
            return;
        }
        Module attributeSwapMod = mm.getModule("AttributeSwap");
        if (attributeSwapMod != null && attributeSwapMod.isToggled() && attributeSwapMod instanceof AttributeSwap as) {
            as.handleBlockBreak();
        }
    }

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void onAttack(Player player, Entity target, CallbackInfo ci) {
        HitSwap hitSwap = HitSwap.INSTANCE;
        if (hitSwap != null) {
            hitSwap.onPreAttack(target);
        }

        ImnotcheatingyouareClient client = ImnotcheatingyouareClient.INSTANCE;
        ModuleManager mm = client != null ? client.moduleManager : null;
        if (mm == null) {
            return;
        }

        Module triggerBotMod = mm.getModule("Triggerbot");
        if (triggerBotMod != null && triggerBotMod.isToggled() && triggerBotMod instanceof Triggerbot tb) {
            if (tb.shouldBlock(target)) {
                ci.cancel();
                return;
            }
        }

        Module shieldBreakerMod = mm.getModule("AutoShieldBreaker");
        if (shieldBreakerMod != null && shieldBreakerMod.isToggled() && shieldBreakerMod instanceof AutoShieldBreaker asb) {
            long prevBreakTime = asb.lastBreakTime;
            if (asb.shouldCancelAttack(target)) {
                ci.cancel();
                return;
            }
            if (asb.lastBreakTime != prevBreakTime) {
                WebStun.trigger(target);
            }
        }

        Module hitSelectMod = mm.getModule("HitSelect");
        if (hitSelectMod != null && hitSelectMod.isToggled() && hitSelectMod instanceof HitSelect hs) {
            if (!hs.canAttack(target)) {
                ci.cancel();
                return;
            }
        }

        Module breachSwapMod = mm.getModule("BreachSwap");
        if (breachSwapMod != null && breachSwapMod instanceof BreachSwap bs) {
            if (bs.handleAttack(target, player)) {
                ci.cancel();
                return;
            }
        }

        Module attributeSwapMod = mm.getModule("AttributeSwap");
        if (attributeSwapMod != null && attributeSwapMod.isToggled() && attributeSwapMod instanceof AttributeSwap as) {
            if (as.handleAttack(target, player)) {
                ci.cancel();
                return;
            }
        }

        if (!ci.isCancelled()) {
            Module kbMod = mm.getModule("KBDisplacement");
            if (kbMod != null && kbMod.isToggled() && kbMod instanceof KnockbackDisplacement kbd) {
                float[] flip = kbd.getFlipRotation(target);
                if (flip != null) {
                    RotationManager.queueRotation(flip[0], flip[1], 0, 1, 1, false, null);
                }
            }
        }

        if (!ci.isCancelled() && !kbShouldRevert) {
            Module silentAim = mm.getModule("SilentAim");
            if (silentAim != null && silentAim.isToggled() && SilentAimUtil.isActive()) {
                SilentAimUtil.consume();
            }
        }
    }

    @Inject(method = "attack", at = @At("RETURN"))
    private void afterAttack(Player player, Entity target, CallbackInfo ci) {
        HitSwap hitSwap = HitSwap.INSTANCE;
        if (hitSwap != null) {
            hitSwap.onPostAttack(target);
        }

        TargetHUD targetHud = TargetHUD.INSTANCE;
        if (targetHud != null) {
            targetHud.onPostAttack(target);
        }

        Backtrack backtrack = Backtrack.INSTANCE;
        if (backtrack != null) {
            backtrack.onAttack(target);
        }

        KillSounds.recordAttack(target);

        SpearMaceDTap spearDTap = SpearMaceDTap.INSTANCE;
        if (spearDTap != null) {
            spearDTap.onManualPlayerAttack(target);
        }

        MaceDTap maceDTap = MaceDTap.INSTANCE;
        if (maceDTap != null) {
            maceDTap.onManualPlayerAttack(target);
        }
    }

    @Inject(method = "piercingAttack", at = @At("RETURN"))
    private void afterPiercingAttack(SwingAnimation swingAnim, PiercingWeapon piercing, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) {
            return;
        }
        Entity hitTarget = mc.crosshairPickEntity;
        if (hitTarget == null && mc.hitResult instanceof EntityHitResult ehr) {
            hitTarget = ehr.getEntity();
        }
        if (hitTarget == null) {
            return;
        }
        KillSounds.recordAttack(hitTarget);
        SpearMaceDTap spearDTap = SpearMaceDTap.INSTANCE;
        if (spearDTap != null) {
            spearDTap.onManualPlayerAttack(hitTarget);
        }
    }
}
