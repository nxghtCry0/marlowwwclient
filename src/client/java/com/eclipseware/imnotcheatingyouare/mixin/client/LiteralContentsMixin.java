package com.eclipseware.imnotcheatingyouare.mixin.client;

import com.eclipseware.imnotcheatingyouare.client.module.impl.NameProtect;
import net.minecraft.network.chat.contents.PlainTextContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlainTextContents.LiteralContents.class)
public class LiteralContentsMixin {

    @Inject(method = "text", at = @At("RETURN"), cancellable = true)
    private void protectText(CallbackInfoReturnable<String> cir) {
        String replaced = NameProtect.apply(cir.getReturnValue());
        if (replaced != cir.getReturnValue()) cir.setReturnValue(replaced);
    }
}
