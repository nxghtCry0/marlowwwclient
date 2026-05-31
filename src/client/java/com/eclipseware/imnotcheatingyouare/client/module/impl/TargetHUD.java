package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.utils.AnimationUtil;
import com.eclipseware.imnotcheatingyouare.client.utils.RenderUtils;
import com.eclipseware.imnotcheatingyouare.client.utils.FontUtils;
import com.eclipseware.imnotcheatingyouare.client.utils.NanoVGManager;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.LivingEntity;
import com.mojang.blaze3d.platform.Window;
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

    public TargetHUD() {
        super("TargetHUD", Category.HUD, "Displays your combat target's info in a sleek card.");
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
        boolean inEditor = mc.screen instanceof com.eclipseware.imnotcheatingyouare.client.clickgui.HudEditorScreen;
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

        float x = (float) xVal;
        float y = (float) yVal;
        
        float width = 175.0f;
        float height = 42.0f;
        float radius = 10.0f;

        float centerX = x + width / 2.0f;
        float centerY = y + height / 2.0f;
        float scale = (active && activeTarget != null) ? AnimationUtil.easeOutBack(animationProgress) : animationProgress;
        float alpha = animationProgress;

        Color themeColor = syncTheme ? RenderUtils.getThemeAccentColor() : new Color(155, 60, 255);
        Color themeSecondary = syncTheme ? RenderUtils.getThemeSecondaryColor() : new Color(80, 20, 160);
        
        int fullAlpha = (int)(255 * alpha) & 0xFF;

        float hp = lastTarget.getHealth();
        float maxHp = lastTarget.getMaxHealth();
        if (maxHp <= 0) maxHp = 20.0f;
        float hpPct = AnimationUtil.clamp(hp / maxHp, 0.0f, 1.0f);

        animatedHealthFraction = AnimationUtil.animate(animatedHealthFraction, hpPct, 0.18f);
        damageHealthFraction = AnimationUtil.animate(damageHealthFraction, hpPct, 0.04f);
        if (damageHealthFraction < animatedHealthFraction) {
            damageHealthFraction = animatedHealthFraction;
        }

        float finalWidth = width;
        float finalHeight = height;
        float finalScale = scale;
        float finalCenterX = centerX;
        float finalCenterY = centerY;
        float finalAlpha = alpha;
        
        int rStart = (int) (themeColor.getRed() * 0.15f);
        int gStart = (int) (themeColor.getGreen() * 0.15f);
        int bStart = (int) (themeColor.getBlue() * 0.15f);

        int rEnd = (int) (themeSecondary.getRed() * 0.15f);
        int gEnd = (int) (themeSecondary.getGreen() * 0.15f);
        int bEnd = (int) (themeSecondary.getBlue() * 0.15f);

        int borderStartColor = themeColor.getRGB() & 0xFFFFFF;
        int borderEndColor = themeSecondary.getRGB() & 0xFFFFFF;

        float finalBarW = width - 28.0f - (comboCount > 0 ? 26.0f : 0.0f);
        float finalDamageW = finalBarW * damageHealthFraction;
        float finalProgressW = finalBarW * animatedHealthFraction;

        Color startColor;
        Color endColor;
        if ("Dynamic Health".equalsIgnoreCase(colorMode)) {
            Color healthColor = RenderUtils.getHealthColor(animatedHealthFraction);
            startColor = healthColor;
            endColor = healthColor.darker();
        } else {
            startColor = themeColor;
            endColor = themeSecondary;
        }
        int healthStartColor = startColor.getRGB() & 0xFFFFFF;
        int healthEndColor = endColor.getRGB() & 0xFFFFFF;

        int badgeColor = themeColor.getRGB() & 0xFFFFFF;

        Window window = mc.getWindow();
        int scaledWidth = window.getGuiScaledWidth();
        int scaledHeight = window.getGuiScaledHeight();
        float ratio = (float) window.getGuiScale();

        int cardBgAlpha = (int) (200 * finalAlpha) & 0xFF;
        int cardBorderAlpha = (int) (180 * finalAlpha) & 0xFF;
        int barBgAlpha = (int) (60 * finalAlpha) & 0xFF;
        int dmgAlpha = (int) (160 * finalAlpha) & 0xFF;
        int progressAlpha = (int) (255 * finalAlpha) & 0xFF;

        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate(centerX, centerY);
        guiGraphics.pose().scale(scale, scale);
        guiGraphics.pose().translate(-width / 2.0f, -height / 2.0f);

        int bgStart = (cardBgAlpha << 24) | (rStart << 16) | (gStart << 8) | bStart;
        int bgEnd = (cardBgAlpha << 24) | (rEnd << 16) | (gEnd << 8) | bEnd;
        int borderStart = (cardBorderAlpha << 24) | borderStartColor;
        int borderEnd = (cardBorderAlpha << 24) | borderEndColor;

        AnimationUtil.drawSquircleVerticalGradient(guiGraphics, 0.0f, 0.0f, width, height, radius, bgStart, bgEnd);
        AnimationUtil.drawSquircleOutlineGradient(guiGraphics, 0.0f, 0.0f, width, height, radius, 1.0f, borderStart, borderEnd);

        AnimationUtil.drawRoundedRect(guiGraphics, 14, 26, (int) finalBarW, 5, 2, (barBgAlpha << 24) | 0x16161C);

        if (finalDamageW > 0) {
            AnimationUtil.drawRoundedRect(guiGraphics, 14, 26, (int) finalDamageW, 5, 2, (dmgAlpha << 24) | 0xE24C4C);
        }

        if (finalProgressW > 0) {
            AnimationUtil.drawRoundedHorizontalGradient(guiGraphics, 14, 26, (int) finalProgressW, 5, 2, (progressAlpha << 24) | healthStartColor, (progressAlpha << 24) | healthEndColor);
        }

        if (comboCount > 0) {
            AnimationUtil.drawRoundedRect(guiGraphics, (int) (width - 36.0f), 22, 22, 13, 3, (progressAlpha << 24) | badgeColor);
        }

        String name = inEditor ? "Preview" : lastTarget.getName().getString();
        if (name.length() > 14) {
            name = name.substring(0, 12) + "..";
        }
        int textColor = (fullAlpha << 24) | 0xF5F5FA;
        FontUtils.drawString(guiGraphics, name, 14, 10, textColor, false);

        String hpStr = String.format("%.1f", lastTarget.getHealth());
        int subTextColor = (fullAlpha << 24) | 0xA0A0AB;
        int hpTextW = FontUtils.width(hpStr);
        FontUtils.drawString(guiGraphics, hpStr, (int) (width - 14.0f - hpTextW), 10, subTextColor, false);

        if (comboCount > 0) {
            String comboStr = "+" + comboCount;
            float badgeCX = width - 36.0f + 11.0f;
            float badgeCY = 22.0f + 6.5f;
            int textW = FontUtils.width(comboStr);
            int badgeTextColor = (fullAlpha << 24) | 0xFFFFFF;

            FontUtils.drawString(guiGraphics, comboStr, (int) (badgeCX - textW / 2.0f), (int) (badgeCY - 4.5f), badgeTextColor, false);
        }

        guiGraphics.pose().popMatrix();
    }
}
