package com.eclipseware.imnotcheatingyouare.mixin.client;

import com.eclipseware.imnotcheatingyouare.client.module.impl.Xray;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.class)
public class BlockBehaviourMixin {

    @Inject(method = "getRenderShape", at = @At("HEAD"), cancellable = true)
    private void onGetRenderShape(BlockState state, CallbackInfoReturnable<net.minecraft.world.level.block.RenderShape> cir) {
        if (Xray.INSTANCE != null && Xray.INSTANCE.isToggled()) {
            if (!isImportant(state)) {
                cir.setReturnValue(net.minecraft.world.level.block.RenderShape.INVISIBLE);
            }
        }
    }

    @Inject(method = "getShadeBrightness", at = @At("HEAD"), cancellable = true)
    private void onGetShadeBrightness(BlockState state, net.minecraft.world.level.BlockGetter level, net.minecraft.core.BlockPos pos, CallbackInfoReturnable<Float> cir) {
        if (Xray.INSTANCE != null && Xray.INSTANCE.isToggled()) {
            cir.setReturnValue(1.0f);
        }
    }
    
    @Inject(method = "propagatesSkylightDown", at = @At("HEAD"), cancellable = true)
    private void onPropagatesSkylightDown(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (Xray.INSTANCE != null && Xray.INSTANCE.isToggled()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "skipRendering", at = @At("RETURN"), cancellable = true)
    private void onSkipRendering(BlockState state, BlockState adjacentBlockState, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (Xray.INSTANCE != null && Xray.INSTANCE.isToggled()) {
            if (isImportant(state)) {
                cir.setReturnValue(false); 
            }
        }
    }

    private boolean isImportant(BlockState state) {
        String name = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
        if (Xray.INSTANCE != null) {
            return Xray.INSTANCE.isImportantBlock(name);
        }
        return false;
    }
}