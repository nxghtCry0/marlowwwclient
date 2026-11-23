package com.eclipseware.imnotcheatingyouare.mixin.client;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.glassui.bridge.ClientNotificationService;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.module.ModuleManager;
import com.eclipseware.imnotcheatingyouare.client.module.impl.RenderOptimizer;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.setting.SettingsManager;
import com.eclipseware.imnotcheatingyouare.client.ui.ArrayListHud;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.world.scores.Objective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public class GuiMixin {

    @Inject(method = "displayScoreboardSidebar", at = @At("HEAD"), cancellable = true)
    private void onDisplayScoreboardSidebar(GuiGraphicsExtractor guiGraphics, Objective objective, CallbackInfo ci) {
        RenderOptimizer ro = RenderOptimizer.INSTANCE;
        ImnotcheatingyouareClient client = ImnotcheatingyouareClient.INSTANCE;
        if (ro != null && ro.isToggled() && client != null && client.settingsManager != null) {
            SettingsManager sm = client.settingsManager;
            Setting noScoreboard = sm.getSettingByName(ro, "No Scoreboard");
            if (noScoreboard != null && noScoreboard.getValBoolean()) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void onExtractRenderState(GuiGraphicsExtractor guiGraphics, DeltaTracker tickDelta, CallbackInfo ci) {
        ImnotcheatingyouareClient client = ImnotcheatingyouareClient.INSTANCE;
        if (client != null && client.moduleManager != null) {
            ModuleManager mm = client.moduleManager;
            for (Module m : mm.modules) {
                if (m.isToggled()) {
                    m.onRenderHUD(guiGraphics, tickDelta);
                }
            }
        }
        ArrayListHud.INSTANCE.render(guiGraphics, tickDelta.getGameTimeDeltaTicks());
        ClientNotificationService.renderHud(guiGraphics);
    }
}
