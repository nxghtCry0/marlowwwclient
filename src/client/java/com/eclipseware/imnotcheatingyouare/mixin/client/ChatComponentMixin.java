package com.eclipseware.imnotcheatingyouare.mixin.client;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.PlainTextContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ChatComponent.class)
public class ChatComponentMixin {

    @ModifyVariable(method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/gui/components/ChatComponent$GuiMessageTag;)V", at = @At("HEAD"), argsOnly = true)
    private Component modifyChat(Component message) {
        Module np = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("NameProtect");
        if (np != null && np.isToggled() && message != null && Minecraft.getInstance().getUser() != null) {
            String myName = Minecraft.getInstance().getUser().getName();
            if (message.getString().contains(myName)) {
                Setting nameSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(np, "Name");
                String alias = nameSetting != null ? nameSetting.getValString() : "Marlowww";
                return replaceName(message, myName, alias);
            }
        }
        return message;
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
    } else if (original.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents) {
        net.minecraft.network.chat.contents.TranslatableContents trans = (net.minecraft.network.chat.contents.TranslatableContents) original.getContents();
        Object[] args = trans.getArgs();
        Object[] newArgs = new Object[args.length];
        
        // Recursively dig into the translation arguments to find and replace the name
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof Component) {
                newArgs[i] = replaceName((Component) args[i], target, replacement);
            } else if (args[i] instanceof String && ((String) args[i]).contains(target)) {
                newArgs[i] = ((String) args[i]).replace(target, replacement);
            } else {
                newArgs[i] = args[i];
            }
        }
        newComponent = Component.translatable(trans.getKey(), newArgs);
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