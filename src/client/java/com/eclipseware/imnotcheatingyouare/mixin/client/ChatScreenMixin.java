package com.eclipseware.imnotcheatingyouare.mixin.client;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.glassui.bridge.ClientGuiScreen;
import com.eclipseware.imnotcheatingyouare.client.module.ModuleManager;
import com.eclipseware.imnotcheatingyouare.client.setting.ConfigManager;
import java.util.Locale;
import net.minecraft.client.KeyboardHandler;
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
        String lower = message.toLowerCase(Locale.ROOT);
        if (lower.startsWith("/config")) {
            if (lower.contains("gui")) {
                ClientGuiScreen.openConfigCenter();
            } else if (lower.contains("export")) {
                ImnotcheatingyouareClient client = ImnotcheatingyouareClient.INSTANCE;
                if (client != null && client.moduleManager != null) {
                    ModuleManager mm = client.moduleManager;
                    String exp = ConfigManager.exportSpecific(mm.modules);
                    Minecraft mc = Minecraft.getInstance();
                    KeyboardHandler kh = mc.keyboardHandler;
                    kh.setClipboard(exp);
                }
            }
            ci.cancel();
        }
    }
}
