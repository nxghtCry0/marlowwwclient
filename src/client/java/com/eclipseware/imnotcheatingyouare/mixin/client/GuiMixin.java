package com.eclipseware.imnotcheatingyouare.mixin.client;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.impl.RenderOptimizer;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.world.scores.Objective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class GuiMixin {

    @Inject(method = "displayScoreboardSidebar", at = @At("HEAD"), cancellable = true)
    private void onDisplayScoreboardSidebar(DrawContext DrawContext, Objective objective, CallbackInfo ci) {
        if (RenderOptimizer.INSTANCE != null && RenderOptimizer.INSTANCE.isToggled()) {
            Setting noScoreboard = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(RenderOptimizer.INSTANCE, "No Scoreboard");
            if (noScoreboard != null && noScoreboard.getValBoolean()) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void onExtractRenderState(DrawContext DrawContext, DeltaTracker tickDelta, CallbackInfo ci) {
        com.eclipseware.imnotcheatingyouare.client.ui.ArrayListHud.INSTANCE.render(DrawContext, tickDelta.getGameTimeDeltaTicks());
    }
}
