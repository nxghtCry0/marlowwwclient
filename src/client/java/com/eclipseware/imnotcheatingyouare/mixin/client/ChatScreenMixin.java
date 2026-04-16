package com.eclipseware.imnotcheatingyouare.mixin.client;

import com.eclipseware.imnotcheatingyouare.client.clickgui.ConfigGui;
import com.eclipseware.imnotcheatingyouare.client.setting.ConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatScreen.class)
public class ChatScreenMixin {

@Inject(method = "handleChatInput", at = @At("HEAD"), cancellable = true)
private void onHandleChatInput(String message, boolean addToHistory, CallbackInfo ci) {
    if (message.toLowerCase().startsWith("/config")) {
        if (message.toLowerCase().contains("gui")) {
            Minecraft.getInstance().setScreen(new ConfigGui());
        } else if (message.toLowerCase().contains("export")) {
            String exp = ConfigManager.exportSpecific(com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient.INSTANCE.moduleManager.modules);
            Minecraft.getInstance().keyboardHandler.setClipboard(exp);
        }
        ci.cancel(); 
    }
}

}