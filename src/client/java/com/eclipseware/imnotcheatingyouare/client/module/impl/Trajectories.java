package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.utils.RenderUtils;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class Trajectories extends Module {

    public Trajectories() {
        super("Trajectories", Category.Render, "Draws the flight path of thrown or shot projectiles.");
    }

    @Override
    public void onRenderHUD(GuiGraphicsExtractor guiGraphics, Object tickDeltaObj) {
        if (!isToggled() || mc.player == null || mc.level == null) return;

        float partialTick = getTickDelta(tickDeltaObj);

        ItemStack stack = mc.player.getMainHandItem();
        if (stack.isEmpty()) {
            stack = mc.player.getOffhandItem();
        }
        if (stack.isEmpty()) return;

        double velocity;
        double gravity;
        double drag;

        if (stack.is(Items.BOW)) {
            gravity = 0.05;
            drag = 0.99;
            int useTicks = mc.player.getTicksUsingItem();
            float charge = useTicks / 20.0f;
            charge = (charge * charge + charge * 2.0f) / 3.0f;
            if (charge > 1.0f) charge = 1.0f;
            if (charge < 0.1f) charge = 1.0f;
            velocity = charge * 3.0;
        } else if (stack.is(Items.CROSSBOW)) {
            gravity = 0.05;
            drag = 0.99;
            velocity = 3.15;
        } else if (stack.is(Items.TRIDENT)) {
            gravity = 0.05;
            drag = 0.99;
            velocity = 2.5;
        } else if (stack.is(Items.SPLASH_POTION) || stack.is(Items.LINGERING_POTION)) {
            gravity = 0.05;
            drag = 0.99;
            velocity = 0.5;
        } else if (stack.is(Items.EXPERIENCE_BOTTLE)) {
            gravity = 0.07;
            drag = 0.99;
            velocity = 0.7;
        } else if (stack.is(Items.ENDER_PEARL) || stack.is(Items.SNOWBALL) || stack.is(Items.EGG)) {
            gravity = 0.03;
            drag = 0.99;
            velocity = 1.5;
        } else if (stack.is(Items.WIND_CHARGE)) {
            gravity = 0.0;
            drag = 1.0;
            velocity = 1.5;
        } else {
            return;
        }

        double yaw = mc.player.getYRot();
        double pitch = mc.player.getXRot();

        boolean hasMultishot = stack.is(Items.CROSSBOW) && hasEnchantment(stack, net.minecraft.world.item.enchantment.Enchantments.MULTISHOT);
        int paths = hasMultishot ? 3 : 1;

        Color themeColor = RenderUtils.getThemeAccentColor();

        for (int p = 0; p < paths; p++) {
            double currentYaw = yaw;
            if (p == 1) currentYaw = yaw - 10.0;
            if (p == 2) currentYaw = yaw + 10.0;

            double motionX = -Math.sin(Math.toRadians(currentYaw)) * Math.cos(Math.toRadians(pitch));
            double motionY = -Math.sin(Math.toRadians(pitch));
            double motionZ = Math.cos(Math.toRadians(currentYaw)) * Math.cos(Math.toRadians(pitch));

            Vec3 motion = new Vec3(motionX, motionY, motionZ).normalize().scale(velocity);

            double startX = net.minecraft.util.Mth.lerp(partialTick, mc.player.xo, mc.player.getX());
            double startY = net.minecraft.util.Mth.lerp(partialTick, mc.player.yo, mc.player.getY()) + mc.player.getEyeHeight();
            double startZ = net.minecraft.util.Mth.lerp(partialTick, mc.player.zo, mc.player.getZ());
            Vec3 pos = new Vec3(startX, startY, startZ);

            Vec3 currentPos = pos;
            Vec3 nextPos;
            List<Vec3> points = new ArrayList<>();
            points.add(currentPos);

            for (int step = 0; step < 300; step++) {
                BlockPos bp = new BlockPos((int) Math.floor(currentPos.x), (int) Math.floor(currentPos.y), (int) Math.floor(currentPos.z));
                net.minecraft.world.level.material.FluidState fluid = mc.level.getFluidState(bp);
                double currentDrag = drag;
                double currentGravity = gravity;

                if (!fluid.isEmpty()) {
                    if (fluid.is(net.minecraft.tags.FluidTags.WATER)) {
                        if (stack.is(Items.TRIDENT)) {
                            currentDrag = 0.99;
                        } else {
                            currentDrag = 0.80;
                        }
                    } else if (fluid.is(net.minecraft.tags.FluidTags.LAVA)) {
                        currentDrag = 0.50;
                    }
                }

                nextPos = currentPos.add(motion);

                BlockHitResult bhr = mc.level.clip(new net.minecraft.world.level.ClipContext(
                    currentPos, nextPos,
                    net.minecraft.world.level.ClipContext.Block.COLLIDER,
                    net.minecraft.world.level.ClipContext.Fluid.NONE,
                    mc.player
                ));

                if (bhr != null && bhr.getType() != net.minecraft.world.phys.HitResult.Type.MISS) {
                    points.add(bhr.getLocation());
                    break;
                }

                motion = motion.scale(currentDrag);
                motion = new Vec3(motion.x, motion.y - currentGravity, motion.z);

                currentPos = nextPos;
                points.add(currentPos);
            }

            Vector3d startProj = new Vector3d();
            Vector3d endProj = new Vector3d();

            for (int i = 0; i < points.size() - 1; i++) {
                Vec3 p1 = points.get(i);
                Vec3 p2 = points.get(i + 1);

                if (RenderUtils.project2D(p1.x, p1.y, p1.z, partialTick, startProj) &&
                    RenderUtils.project2D(p2.x, p2.y, p2.z, partialTick, endProj)) {

                    if (startProj.z > 0 && startProj.z < 1.0 && endProj.z > 0 && endProj.z < 1.0) {
                        RenderUtils.drawLine2D(guiGraphics, startProj.x, startProj.y, endProj.x, endProj.y, themeColor);
                    }
                }
            }

            Vec3 hitPos = points.get(points.size() - 1);
            double minX = hitPos.x - 0.25;
            double minY = hitPos.y;
            double minZ = hitPos.z - 0.25;
            double maxX = hitPos.x + 0.25;
            double maxY = hitPos.y + 0.5;
            double maxZ = hitPos.z + 0.25;
            Color faceColor = new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), 35);
            RenderUtils.draw3DBox(guiGraphics, minX, minY, minZ, maxX, maxY, maxZ, faceColor, themeColor, partialTick);
        }
    }

    private boolean hasEnchantment(ItemStack stack, net.minecraft.resources.ResourceKey<net.minecraft.world.item.enchantment.Enchantment> key) {
        if (stack == null || stack.isEmpty()) return false;
        for (net.minecraft.core.Holder<net.minecraft.world.item.enchantment.Enchantment> holder : stack.getEnchantments().keySet()) {
            if (holder.is(key)) {
                return true;
            }
        }
        return false;
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
