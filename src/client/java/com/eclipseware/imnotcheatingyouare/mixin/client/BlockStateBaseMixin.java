package com.eclipseware.imnotcheatingyouare.mixin.client;

import com.eclipseware.imnotcheatingyouare.client.module.impl.Xray;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.world.level.block.state.BlockBehaviour$BlockStateBase")
public class BlockStateBaseMixin {

    @Inject(method = "canOcclude", at = @At("HEAD"), cancellable = true)
    private void onCanOcclude(CallbackInfoReturnable<Boolean> cir) {
        if (Xray.INSTANCE != null && Xray.INSTANCE.isToggled()) {
            BlockState state = (BlockState) (Object) this;
            String name = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
            if (!Xray.INSTANCE.isImportantBlock(name)) {
                cir.setReturnValue(false);
            }
        }
    }
}
