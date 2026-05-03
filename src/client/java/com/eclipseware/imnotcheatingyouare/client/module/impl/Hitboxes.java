package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.utils.RenderUtils;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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
        
        // HudRenderCallback.EVENT.register((guiGraphics, tickDeltaObj) -> {
        //     if (isToggled() && visual.getValBoolean()) {
        //         if (mc.level == null || mc.player == null) return;
        //         Color renderCol = new Color(255, 255, 255, 100);
        //         float partialTick = getTickDelta(tickDeltaObj);
        //         
        //         double expand = size.getValDouble();
        //         if (expand <= 0) return;
        //         
        //         for (Entity entity : mc.level.entitiesForRendering()) {
        //             if (entity == mc.player || !(entity instanceof Player)) continue;
        //             
        //             double x = net.minecraft.util.Mth.lerp(partialTick, entity.xo, entity.getX());
        //             double y = net.minecraft.util.Mth.lerp(partialTick, entity.yo, entity.getY());
        //             double z = net.minecraft.util.Mth.lerp(partialTick, entity.zo, entity.getZ());
        //             
        //             float hw = (entity.getBbWidth() / 2.0f) + (float) expand;
        //             float h = entity.getBbHeight() + (float) expand * 2;
        //             y -= expand; // adjust base to match inflate
        //             
        //             double[][] corners = {
        //                 {x - hw, y,     z - hw},
        //                 {x + hw, y,     z - hw},
        //                 {x - hw, y + h, z - hw},
        //                 {x + hw, y + h, z - hw},
        //                 {x - hw, y,     z + hw},
        //                 {x + hw, y,     z + hw},
        //                 {x - hw, y + h, z + hw},
        //                 {x + hw, y + h, z + hw},
        //             };
        //             
        //             double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
        //             double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        //             boolean valid = false;
        // 
        //             for (double[] c : corners) {
        //                 Vector3d proj = RenderUtils.project2D(c[0], c[1], c[2], partialTick);
        //                 if (proj == null) continue;
        //                 valid = true;
        //                 if (proj.x < minX) minX = proj.x;
        //                 if (proj.x > maxX) maxX = proj.x;
        //                 if (proj.y < minY) minY = proj.y;
        //                 if (proj.y > maxY) maxY = proj.y;
        //             }
        //             if (!valid) continue;
        //             
        //             // guiGraphics.fill((int) minX, (int) minY, (int) maxX, (int) minY + 1, renderCol.getRGB()); // Top
        //             // guiGraphics.fill((int) minX, (int) maxY, (int) maxX, (int) maxY + 1, renderCol.getRGB()); // Bot
        //             // guiGraphics.fill((int) minX, (int) minY, (int) minX + 1, (int) maxY, renderCol.getRGB()); // L
        //             // guiGraphics.fill((int) maxX, (int) minY, (int) maxX + 1, (int) maxY, renderCol.getRGB()); // R
        //         }
        //     }
        // });
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
