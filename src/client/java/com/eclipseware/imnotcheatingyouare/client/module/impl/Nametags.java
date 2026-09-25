package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.utils.RenderUtils;
import imgui.ImDrawList;
import imgui.ImGui;
import imgui.ImVec2;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3d;

import java.awt.Color;

public class Nametags extends Module {

    public Nametags() {
        super("Nametags", Category.Render, "Renders high-fidelity ImGui nametags for entities.");
    }

    private static final Vector3d projVec = new Vector3d();
    private static final ImVec2 nameSizeBuf = new ImVec2();
    private static final ImVec2 hpSizeBuf = new ImVec2();
    private static final ImVec2 distSizeBuf = new ImVec2();
    private static final ImVec2 itemSizeBuf = new ImVec2();

    @Override
    public void onRenderHUD(GuiGraphicsExtractor guiGraphics, Object tickDeltaObj) {
    }

    public void renderImGuiOverlay() {
        if (!isToggled() || mc.player == null || mc.level == null) return;

        float partialTick = mc.getDeltaTracker() != null ? mc.getDeltaTracker().getGameTimeDeltaPartialTick(true) : 1.0f;

        Setting playersSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Players");
        boolean showPlayers = playersSetting == null || playersSetting.getValBoolean();

        Setting mobsSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Show Mobs");
        boolean showMobs = mobsSetting != null && mobsSetting.getValBoolean();

        Color themeColor = RenderUtils.getThemeAccentColor();
        ImDrawList drawList = ImGui.getBackgroundDrawList();

        double maxDist = mc.options != null ? Math.max(256.0, mc.options.getEffectiveRenderDistance() * 16.0) : 256.0;
        float displayWidth = ImGui.getIO().getDisplaySizeX();
        float displayHeight = ImGui.getIO().getDisplaySizeY();

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity == mc.player || !entity.isAlive()) continue;

            boolean isPlayer = entity instanceof Player;
            boolean isMob = entity instanceof Mob;
            if (!(isPlayer && showPlayers) && !(isMob && showMobs)) continue;

            double dist = mc.player.distanceTo(entity);
            if (dist > maxDist) continue;

            double ex = net.minecraft.util.Mth.lerp(partialTick, entity.xo, entity.getX());
            double ey = net.minecraft.util.Mth.lerp(partialTick, entity.yo, entity.getY()) + entity.getBbHeight() + 0.4;
            double ez = net.minecraft.util.Mth.lerp(partialTick, entity.zo, entity.getZ());

            if (!RenderUtils.project2DImGui(ex, ey, ez, partialTick, projVec)) continue;
            if (projVec.z <= 0 || projVec.z >= 1.0) continue;
            if (!Double.isFinite(projVec.x) || !Double.isFinite(projVec.y)) continue;

            float px = (float) projVec.x;
            float py = (float) projVec.y;
            if (px < -500f || px > displayWidth + 500f || py < -500f || py > displayHeight + 500f) continue;

            float alpha = Math.max(0.4f, 1.0f - (float)(dist / maxDist));
            int oa = (int)(alpha * 255);

            String name = entity.getName().getString();
            String hpStr = "";
            if (entity instanceof LivingEntity living) {
                hpStr = " " + (int) Math.ceil(living.getHealth()) + "HP";
            }
            String distStr = " " + (Math.round(dist * 10.0) / 10.0) + "m";

            ImGui.calcTextSize(nameSizeBuf, name);
            ImGui.calcTextSize(hpSizeBuf, hpStr);
            ImGui.calcTextSize(distSizeBuf, distStr);

            float totalWidth = nameSizeBuf.x + hpSizeBuf.x + distSizeBuf.x;
            float cardHeight = Math.max(nameSizeBuf.y, Math.max(hpSizeBuf.y, distSizeBuf.y)) + 6f;
            float paddingX = 6f;
            float cardWidth = totalWidth + (paddingX * 2);

            float drawX = px - cardWidth / 2f;
            float drawY = py - cardHeight;

            int cardBgColor = RenderUtils.toImGuiColor(14, 14, 18, (int)(alpha * 255.0f * 0.85f));
            int cardBorderColor = RenderUtils.toImGuiColor(isPlayer ? themeColor : new Color(255, 95, 95), alpha * 0.6f);
            int nameTextColor = RenderUtils.toImGuiColor(255, 255, 255, oa);
            int distTextColor = RenderUtils.toImGuiColor(170, 210, 255, oa);

            drawList.addRectFilled(drawX, drawY, drawX + cardWidth, drawY + cardHeight, cardBgColor, 4.0f);
            drawList.addRect(drawX, drawY, drawX + cardWidth, drawY + cardHeight, cardBorderColor, 4.0f, 0, 1.2f);

            float curX = drawX + paddingX;
            float textY = drawY + (cardHeight - nameSizeBuf.y) / 2f;

            drawList.addText(curX, textY, nameTextColor, name);
            curX += nameSizeBuf.x;

            if (!hpStr.isEmpty() && entity instanceof LivingEntity living) {
                Color hpColor = RenderUtils.getHealthColor(living.getHealth() / Math.max(1f, living.getMaxHealth()));
                int hpTextColor = RenderUtils.toImGuiColor(hpColor, alpha);
                drawList.addText(curX, textY, hpTextColor, hpStr);
                curX += hpSizeBuf.x;
            }

            drawList.addText(curX, textY, distTextColor, distStr);

            if (entity instanceof LivingEntity living) {
                ItemStack mainHand = living.getMainHandItem();
                ItemStack offHand = living.getOffhandItem();

                if ((mainHand != null && !mainHand.isEmpty()) || (offHand != null && !offHand.isEmpty())) {
                    StringBuilder itemSb = new StringBuilder();
                    if (mainHand != null && !mainHand.isEmpty()) {
                        itemSb.append(mainHand.getHoverName().getString());
                        if (mainHand.getCount() > 1) itemSb.append(" x").append(mainHand.getCount());
                    }
                    if (offHand != null && !offHand.isEmpty()) {
                        if (itemSb.length() > 0) itemSb.append(" | ");
                        itemSb.append(offHand.getHoverName().getString());
                        if (offHand.getCount() > 1) itemSb.append(" x").append(offHand.getCount());
                    }

                    String itemText = itemSb.toString();
                    ImGui.calcTextSize(itemSizeBuf, itemText);

                    float itemPaddingX = 5f;
                    float itemCardW = itemSizeBuf.x + (itemPaddingX * 2);
                    float itemCardH = itemSizeBuf.y + 4f;
                    float itemCardX = px - itemCardW / 2f;
                    float itemCardY = drawY - itemCardH - 3f;

                    int itemBgColor = RenderUtils.toImGuiColor(10, 10, 14, (int)(alpha * 255.0f * 0.8f));
                    int itemBorderColor = RenderUtils.toImGuiColor(120, 120, 140, (int)(alpha * 255.0f * 0.4f));
                    int itemTextColor = RenderUtils.toImGuiColor(220, 220, 230, oa);

                    drawList.addRectFilled(itemCardX, itemCardY, itemCardX + itemCardW, itemCardY + itemCardH, itemBgColor, 3.0f);
                    drawList.addRect(itemCardX, itemCardY, itemCardX + itemCardW, itemCardY + itemCardH, itemBorderColor, 3.0f, 0, 1.0f);
                    drawList.addText(itemCardX + itemPaddingX, itemCardY + (itemCardH - itemSizeBuf.y) / 2f, itemTextColor, itemText);
                }
            }
        }
    }
}