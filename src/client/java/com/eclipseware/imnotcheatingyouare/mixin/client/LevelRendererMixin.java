package com.eclipseware.imnotcheatingyouare.mixin.client;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.impl.ESP;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ShaderManager;getPostChain(Lnet/minecraft/resources/Identifier;Ljava/util/Set;)Lnet/minecraft/client/renderer/PostChain;"), index = 0)
    private Identifier swapOutlineChain(Identifier id) {
        if (ImnotcheatingyouareClient.INSTANCE == null || ImnotcheatingyouareClient.INSTANCE.moduleManager == null) return id;
        if (ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("ESP") instanceof ESP esp && esp.isShaderMode()) {
            return esp.getShaderChainId();
        }
        if (com.eclipseware.imnotcheatingyouare.client.module.impl.SelfShader.INSTANCE != null
                && com.eclipseware.imnotcheatingyouare.client.module.impl.SelfShader.INSTANCE.body()) {
            return com.eclipseware.imnotcheatingyouare.client.module.impl.SelfShader.INSTANCE.shaderChainId();
        }
        return id;
    }
}
