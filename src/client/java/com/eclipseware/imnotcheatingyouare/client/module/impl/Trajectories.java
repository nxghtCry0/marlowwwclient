package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.utils.RenderUtils;
import imgui.ImDrawList;
import imgui.ImGui;
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

    private static final Vector3d startProj = new Vector3d();
    private static final Vector3d endProj = new Vector3d();
    private static final Vector3d[] boxProjBuffer = new Vector3d[8];
    static {
        for (int i = 0; i < 8; i++) {
            boxProjBuffer[i] = new Vector3d();
        }
    }

    public Trajectories() {
        super("Trajectories", Category.Render, "Draws the flight path of thrown or shot projectiles via ImGui.");
    }

    @Override
    public void onRenderHUD(GuiGraphicsExtractor guiGraphics, Object tickDeltaObj) {
    }

    public void renderImGuiOverlay() {
        if (!isToggled() || mc.player == null || mc.level == null) return;

        Module bypassMod = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("Bypass");
        if (bypassMod != null && bypassMod.isToggled()) return;

        float partialTick = mc.getDeltaTracker() != null ? mc.getDeltaTracker().getGameTimeDeltaPartialTick(true) : 1.0f;

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
        int lineColor = RenderUtils.toImGuiColor(themeColor, 1.0f);
        int boxFillColor = RenderUtils.toImGuiColor(themeColor, 0.25f);
        int boxOutlineColor = RenderUtils.toImGuiColor(themeColor, 0.9f);

        ImDrawList drawList = ImGui.getForegroundDrawList();

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

            for (int i = 0; i < points.size() - 1; i++) {
                Vec3 p1 = points.get(i);
                Vec3 p2 = points.get(i + 1);

                if (RenderUtils.project2DImGui(p1.x, p1.y, p1.z, partialTick, startProj) &&
                    RenderUtils.project2DImGui(p2.x, p2.y, p2.z, partialTick, endProj)) {

                    if (startProj.z > 0 && startProj.z < 1.0 && endProj.z > 0 && endProj.z < 1.0) {
                        drawList.addLine((float) startProj.x, (float) startProj.y, (float) endProj.x, (float) endProj.y, lineColor, 2.0f);
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

            Vec3[] corners = new Vec3[]{
                new Vec3(minX, minY, minZ),
                new Vec3(maxX, minY, minZ),
                new Vec3(minX, maxY, minZ),
                new Vec3(maxX, maxY, minZ),
                new Vec3(minX, minY, maxZ),
                new Vec3(maxX, minY, maxZ),
                new Vec3(minX, maxY, maxZ),
                new Vec3(maxX, maxY, maxZ)
            };

            double min2dX = Double.MAX_VALUE, min2dY = Double.MAX_VALUE;
            double max2dX = -Double.MAX_VALUE, max2dY = -Double.MAX_VALUE;
            int validCount = 0;

            for (int i = 0; i < 8; i++) {
                if (RenderUtils.project2DImGui(corners[i].x, corners[i].y, corners[i].z, partialTick, boxProjBuffer[i])) {
                    if (boxProjBuffer[i].z > 0 && boxProjBuffer[i].z < 1.0) {
                        validCount++;
                        min2dX = Math.min(min2dX, boxProjBuffer[i].x);
                        min2dY = Math.min(min2dY, boxProjBuffer[i].y);
                        max2dX = Math.max(max2dX, boxProjBuffer[i].x);
                        max2dY = Math.max(max2dY, boxProjBuffer[i].y);
                    }
                }
            }

            if (validCount > 0 && Double.isFinite(min2dX) && Double.isFinite(min2dY) && Double.isFinite(max2dX) && Double.isFinite(max2dY)) {
                float ix = (float) Math.floor(min2dX);
                float iy = (float) Math.floor(min2dY);
                float ix2 = (float) Math.ceil(max2dX);
                float iy2 = (float) Math.ceil(max2dY);

                if (ix2 > ix && iy2 > iy) {
                    drawList.addRectFilled(ix, iy, ix2, iy2, boxFillColor, 2.0f);
                    drawList.addRect(ix, iy, ix2, iy2, boxOutlineColor, 2.0f, 0, 1.5f);
                }
            }
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
}
