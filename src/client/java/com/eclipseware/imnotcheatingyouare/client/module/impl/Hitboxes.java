package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.utils.RenderUtils;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.joml.Vector3d;
import java.awt.Color;

public class Hitboxes extends Module {
    private Setting size;
    private Setting visual;

    public Hitboxes() {
        super("Hitboxes", Category.Blatant, "Expands entity hitboxes. Visual option draws it on UI layer.");
        setSubCategory("Semi-Blatant"); 
        size = new Setting("Expand Size", this, 0.2, 0.0, 3.0, false);
        visual = new Setting("Visual", this, true);
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(size);
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(visual);
        
    }
    
    private float getTickDelta(Object tickDeltaObj) {
        if (tickDeltaObj instanceof Float) return (Float) tickDeltaObj;
        for (java.lang.reflect.Method m : tickDeltaObj.getClass().getMethods()) {
            if (m.getReturnType() == float.class) {
                if (m.getParameterCount() == 1 && m.getParameterTypes()[0] == boolean.class) {
                    try { return (float) m.invoke(tickDeltaObj, true); } catch (Exception e) {}
                } else if (m.getParameterCount() == 0) {
                    String name = m.getName().toLowerCase();
                    if (name.contains("tick") || name.contains("delta") || name.contains("frame")) {
                        try { return (float) m.invoke(tickDeltaObj); } catch (Exception e) {}
                    }
                }
            }
        }
        return 1.0f;
    }
}
