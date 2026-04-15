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

    @Inject(method = "skipRendering", at = @At("HEAD"), cancellable = true)
    private void onSkipRendering(BlockState state, BlockState adjacentBlockState, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (Xray.INSTANCE != null && Xray.INSTANCE.isToggled()) {
            boolean isOre = isImportant(state);
            boolean isAdjOre = isImportant(adjacentBlockState);
            
            if (!isOre) {
                // If it's dirt/stone/air, skip rendering it completely
                cir.setReturnValue(true);
            } else if (isAdjOre) {
                // If it IS an ore, but the block right next to it is the SAME type of ore, skip the face between them to save FPS
                cir.setReturnValue(true); 
            } else {
                // It's an ore, and the face is exposed to air/dirt -> Draw it!
                cir.setReturnValue(false); 
            }
        }
    }

    // Forces ambient occlusion to act as if everything is fully lit so ores aren't pitched black inside the earth
    @Inject(method = "getShadeBrightness", at = @At("HEAD"), cancellable = true)
    private void onGetShadeBrightness(BlockState state, net.minecraft.world.level.BlockGetter level, net.minecraft.core.BlockPos pos, CallbackInfoReturnable<Float> cir) {
        if (Xray.INSTANCE != null && Xray.INSTANCE.isToggled()) {
            cir.setReturnValue(1.0f);
        }
    }
    
    // Forces the renderer to allow skylight to propagate through the invisible dirt/stone
    @Inject(method = "propagatesSkylightDown", at = @At("HEAD"), cancellable = true)
    private void onPropagatesSkylightDown(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (Xray.INSTANCE != null && Xray.INSTANCE.isToggled()) {
            cir.setReturnValue(true);
        }
    }

    private boolean isImportant(BlockState state) {
        String name = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
        return name.contains("ore") || name.equals("ancient_debris") || name.contains("chest") || name.contains("spawner");
    }
}