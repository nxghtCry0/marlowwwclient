package com.eclipseware.imnotcheatingyouare.mixin.client;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.PlainTextContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerTabOverlay.class)
public class PlayerTabOverlayMixin {

    @Inject(method = "getNameForDisplay", at = @At("RETURN"), cancellable = true)
    private void onGetNameForDisplay(PlayerInfo playerInfo, CallbackInfoReturnable<Component> cir) {
        Module np = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("NameProtect");
        if (np != null && np.isToggled() && Minecraft.getInstance().getUser() != null) {
            String myName = Minecraft.getInstance().getUser().getName();
            
            // Only process if this is OUR player entry in the tab list
if (playerInfo.getProfile().name().equals(myName)) {
Component current = cir.getReturnValue();
Setting nameSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(np, "Name");
                String alias = nameSetting != null ? nameSetting.getValString() : "Marlowww";

                if (current == null) {
                    // Default fallback if no styling exists
                    cir.setReturnValue(Component.literal(alias));
                } else if (current.getString().contains(myName)) {
                    // Rebuild to preserve prefixes/suffixes (like Scoreboard teams)
                    cir.setReturnValue(replaceName(current, myName, alias));
                }
            }
        }
    }

    private Component replaceName(Component original, String target, String replacement) {
        if (original == null) return null;

        MutableComponent newComponent;
        if (original.getContents() instanceof PlainTextContents) {
            PlainTextContents plainText = (PlainTextContents) original.getContents();
            String text = plainText.text();
            if (text.contains(target)) {
                newComponent = Component.literal(text.replace(target, replacement));
            } else {
                newComponent = original.plainCopy();
            }
        } else {
            newComponent = original.plainCopy();
        }

        newComponent.setStyle(original.getStyle());

        for (Component sibling : original.getSiblings()) {
            newComponent.append(replaceName(sibling, target, replacement));
        }

        return newComponent;
    }
}