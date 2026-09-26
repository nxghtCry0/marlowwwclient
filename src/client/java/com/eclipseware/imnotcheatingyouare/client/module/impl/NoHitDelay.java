package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.mixin.client.MinecraftAccessor;
import net.minecraft.world.entity.Entity;

public class NoHitDelay extends Module {

    public NoHitDelay() {
        super("NoHitDelay", Category.Combat);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.level == null) return;
        
        if (mc instanceof MinecraftAccessor accessor) {
            accessor.setMissTime(0);
        }

        for (Entity entity : mc.level.entitiesForRendering()) {
            entity.invulnerableTime = 0;
        }
    }
}
