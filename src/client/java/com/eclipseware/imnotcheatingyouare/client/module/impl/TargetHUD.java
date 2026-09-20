package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.utils.AnimationUtil;
import com.eclipseware.imnotcheatingyouare.client.utils.RenderUtils;
import imgui.ImDrawList;
import imgui.ImGui;
import imgui.ImVec2;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.LivingEntity;

import java.awt.Color;
import java.util.ArrayList;

public class TargetHUD extends Module {
    public static TargetHUD INSTANCE;

    private LivingEntity target = null;
    private LivingEntity lastTarget = null;

    private float animationProgress = 0.0f;
    private float animatedHealthFraction = 1.0f;
    private float damageHealthFraction = 1.0f;

    private int comboCount = 0;
    private float comboPulseScale = 1.0f;
    private long lastAttackTime = 0;
    private int lastPlayerHurtTime = 0;

    private static final ImVec2 nameSizeBuf = new ImVec2();
    private static final ImVec2 hpSizeBuf = new ImVec2();
    private static final ImVec2 comboSizeBuf = new ImVec2();

    public TargetHUD() {
        super("TargetHUD", Category.HUD, "Displays combat target info in an ultra-sleek ImGui card.");
        INSTANCE = this;

        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("X", this, 200.0, 0.0, 2000.0, true));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Y", this, 200.0, 0.0, 2000.0, true));
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Sync Theme", this, true));

        ArrayList<String> colorModes = new ArrayList<>();
        colorModes.add("Theme Sync");
        colorModes.add("Dynamic Health");
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(new Setting("Health Color Mode", this, "Theme Sync", colorModes));
    }

    public void onPostAttack(net.minecraft.world.entity.Entity hitTarget) {
        if (!isToggled() || !(hitTarget instanceof LivingEntity le)) return;

        target = le;
        long now = System.currentTimeMillis();
        if (lastTarget != le || now - lastAttackTime > 2500) {
            comboCount = 1;
        } else {
            comboCount++;
        }
        lastAttackTime = now;
        comboPulseScale = 1.3f;
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.level == null) {
            target = null;
            comboCount = 0;
            return;
        }

        if (comboCount > 0 && System.currentTimeMillis() - lastAttackTime > 2500) {
            comboCount = 0;
        }

        if (target != null && mc.player.hurtTime > 0 && lastPlayerHurtTime == 0) {
            comboCount = Math.max(0, comboCount - 1);
            comboPulseScale = 1.2f;
        }
        lastPlayerHurtTime = mc.player.hurtTime;

        LivingEntity newTarget = null;
        Module killAura = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("KillAura");
        if (killAura != null && killAura.isToggled()) {
            newTarget = ((KillAura) killAura).getTarget();
        }

        if (newTarget != null) {
            target = newTarget;
            lastAttackTime = System.currentTimeMillis();
        } else if (target != null && System.currentTimeMillis() - lastAttackTime > 2500) {
            target = null;
            comboCount = 0;
        }
    }

    @Override
    public void onRenderHUD(GuiGraphicsExtractor guiGraphics, Object tickDelta) {
        // High-performance ImGui overlay handles rendering on frame render
    }

    public void renderImGuiOverlay() {
        boolean inEditor = mc.gui.screen() instanceof com.eclipseware.imnotcheatingyouare.client.clickgui.HudEditorScreen;
        boolean active = isToggled() || inEditor;

        LivingEntity activeTarget = target;
        if (activeTarget == null || activeTarget.isDeadOrDying()) {
            if (inEditor) {
                activeTarget = mc.player;
            } else {
                active = false;
            }
        }

        if (active && activeTarget != null) {
            lastTarget = activeTarget;
            animationProgress = AnimationUtil.animate(animationProgress, 1.0f, 0.12f);
        } else {
            animationProgress = AnimationUtil.animate(animationProgress, 0.0f, 0.12f);
        }

        if (animationProgress <= 0.005f || lastTarget == null) {
            return;
        }

        comboPulseScale = AnimationUtil.animate(comboPulseScale, 1.0f, 0.1f);

        double xVal = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "X").getValDouble();
        double yVal = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Y").getValDouble();
        boolean syncTheme = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Sync Theme").getValBoolean();
        String colorMode = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Health Color Mode").getValString();

        float guiScale = (float) mc.getWindow().getGuiScale();
        if (guiScale <= 0) guiScale = 1.0f;

        float x = (float) xVal * guiScale;
        float y = (float) yVal * guiScale;
        float width = 175.0f * guiScale;
        float height = 42.0f * guiScale;

        float alpha = animationProgress;
        Color themeColor = syncTheme ? RenderUtils.getThemeAccentColor() : new Color(239, 142, 172);

        float hp = lastTarget.getHealth();
        float maxHp = lastTarget.getMaxHealth();
        if (maxHp <= 0) maxHp = 20.0f;
        float hpPct = AnimationUtil.clamp(hp / maxHp, 0.0f, 1.0f);

        animatedHealthFraction = AnimationUtil.animate(animatedHealthFraction, hpPct, 0.18f);
        damageHealthFraction = AnimationUtil.animate(damageHealthFraction, hpPct, 0.04f);
        if (damageHealthFraction < animatedHealthFraction) {
            damageHealthFraction = animatedHealthFraction;
        }

        Color hpColor;
        if ("Dynamic Health".equalsIgnoreCase(colorMode)) {
            hpColor = RenderUtils.getHealthColor(animatedHealthFraction);
        } else {
            hpColor = themeColor;
        }

        ImDrawList drawList = ImGui.getForegroundDrawList();

        int cardBgColor = RenderUtils.toImGuiColor(18, 18, 24, (int)(alpha * 255.0f * 0.88f));
        int cardBorderColor = RenderUtils.toImGuiColor(themeColor, alpha * 0.7f);
        int trackBgColor = RenderUtils.toImGuiColor(10, 10, 14, (int)(alpha * 255.0f * 0.9f));
        int dmgBarColor = RenderUtils.toImGuiColor(226, 76, 76, (int)(alpha * 255.0f * 0.7f));
        int hpBarColor = RenderUtils.toImGuiColor(hpColor, alpha);

        // 1. Card Container & Glow Border
        drawList.addRectFilled(x, y, x + width, y + height, cardBgColor, 6.0f * guiScale);
        drawList.addRect(x, y, x + width, y + height, cardBorderColor, 6.0f * guiScale, 0, 1.2f * guiScale);

        // 2. Target Name
        String name = inEditor ? "Target Preview" : lastTarget.getName().getString();
        if (name.length() > 16) {
            name = name.substring(0, 14) + "..";
        }
        int nameTextColor = RenderUtils.toImGuiColor(255, 255, 255, (int)(alpha * 255.0f));
        drawList.addText(x + 10f * guiScale, y + 8f * guiScale, nameTextColor, name);

        // 3. Health Numbers
        String hpStr = String.format("%.1f / %.1f", hp, maxHp);
        ImGui.calcTextSize(hpSizeBuf, hpStr);
        int hpTextColor = RenderUtils.toImGuiColor(170, 170, 185, (int)(alpha * 255.0f));
        drawList.addText(x + width - 10f * guiScale - hpSizeBuf.x, y + 8f * guiScale, hpTextColor, hpStr);

        // 4. Smooth Health Track & Damage Catch-up Bar
        float barX = x + 10f * guiScale;
        float barY = y + 26f * guiScale;
        float barW = width - 20f * guiScale - (comboCount > 0 ? 30f * guiScale : 0f);
        float barH = 6f * guiScale;

        drawList.addRectFilled(barX, barY, barX + barW, barY + barH, trackBgColor, 3.0f * guiScale);

        float dmgW = barW * damageHealthFraction;
        if (dmgW > 0) {
            drawList.addRectFilled(barX, barY, barX + dmgW, barY + barH, dmgBarColor, 3.0f * guiScale);
        }

        float animatedW = barW * animatedHealthFraction;
        if (animatedW > 0) {
            drawList.addRectFilled(barX, barY, barX + animatedW, barY + barH, hpBarColor, 3.0f * guiScale);
        }

        // 5. Combo Hits Badge
        if (comboCount > 0) {
            String comboStr = "+" + comboCount;
            ImGui.calcTextSize(comboSizeBuf, comboStr);

            float badgeW = comboSizeBuf.x + 8f * guiScale;
            float badgeH = 12f * guiScale;
            float badgeX = x + width - 10f * guiScale - badgeW;
            float badgeY = barY - 3f * guiScale;

            int badgeBgColor = RenderUtils.toImGuiColor(themeColor, alpha * 0.9f);
            int badgeTextColor = RenderUtils.toImGuiColor(255, 255, 255, (int)(alpha * 255.0f));

            drawList.addRectFilled(badgeX, badgeY, badgeX + badgeW, badgeY + badgeH, badgeBgColor, 3.0f * guiScale);
            drawList.addText(badgeX + 4f * guiScale, badgeY + (badgeH - comboSizeBuf.y) / 2f, badgeTextColor, comboStr);
        }
    }
}
