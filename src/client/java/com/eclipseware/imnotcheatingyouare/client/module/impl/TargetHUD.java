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
    public static final float WIDTH = 190f;
    public static final float HEIGHT = 50f;

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
        float k = guiScale / xyz.breadloaf.imguimc.imgui.ImguiLoader.getUiScale();

        float slide = (1.0f - animationProgress) * 12f * k;
        float x = (float) xVal * k;
        float y = (float) yVal * k + slide;
        float width = WIDTH * k;
        float height = HEIGHT * k;
        float alpha = animationProgress;
        int a255 = (int) (alpha * 255.0f);

        Color accent = syncTheme ? RenderUtils.getThemeAccentColor() : new Color(190, 150, 255);

        float hp = lastTarget.getHealth();
        float maxHp = lastTarget.getMaxHealth();
        if (maxHp <= 0) maxHp = 20.0f;
        float hpPct = AnimationUtil.clamp(hp / maxHp, 0.0f, 1.0f);

        animatedHealthFraction = AnimationUtil.animate(animatedHealthFraction, hpPct, 0.18f);
        damageHealthFraction = AnimationUtil.animate(damageHealthFraction, hpPct, 0.04f);
        if (damageHealthFraction < animatedHealthFraction) damageHealthFraction = animatedHealthFraction;

        Color hpColor = "Dynamic Health".equalsIgnoreCase(colorMode) ? RenderUtils.getHealthColor(animatedHealthFraction) : accent;
        float hurt = lastTarget.hurtTime > 0 ? lastTarget.hurtTime / 10.0f : 0f;

        ImDrawList dl = ImGui.getBackgroundDrawList();
        float r = 8f * k;

        dl.addRectFilled(x + 2f * k, y + 3f * k, x + width + 2f * k, y + height + 3f * k, RenderUtils.toImGuiColor(0, 0, 0, (int) (a255 * 0.35f)), r);
        int bgTop = RenderUtils.toImGuiColor(22, 18, 34, (int) (a255 * 0.94f));
        int bgBottom = RenderUtils.toImGuiColor(14, 11, 22, (int) (a255 * 0.94f));
        dl.addRectFilled(x, y, x + width, y + height, bgBottom, r);
        dl.addRectFilledMultiColor(x + r * 0.5f, y, x + width - r * 0.5f, y + height * 0.5f, bgTop, bgTop, bgBottom, bgBottom);
        dl.addRect(x, y, x + width, y + height, RenderUtils.toImGuiColor(accent, alpha * 0.35f), r, 0, 1.0f * k);
        dl.addLine(x + r, y + 0.5f * k, x + width - r, y + 0.5f * k, RenderUtils.toImGuiColor(accent, alpha * 0.9f), 1.5f * k);

        float pad = 8f * k;
        float avatar = height - pad * 2f;
        float ax = x + pad;
        float ay = y + pad;
        String name = inEditor ? "Target Preview" : lastTarget.getName().getString();
        if (name.length() > 16) name = name.substring(0, 14) + "..";

        com.eclipseware.imnotcheatingyouare.client.utils.ImGuiTextures.Region face = null;
        com.eclipseware.imnotcheatingyouare.client.utils.ImGuiTextures.Region hat = null;
        boolean isPlayer = lastTarget instanceof net.minecraft.world.entity.player.Player;
        if (isPlayer) {
            face = com.eclipseware.imnotcheatingyouare.client.utils.ImGuiTextures.playerFace((net.minecraft.world.entity.player.Player) lastTarget);
            hat = com.eclipseware.imnotcheatingyouare.client.utils.ImGuiTextures.playerHat((net.minecraft.world.entity.player.Player) lastTarget);
        } else {
            face = com.eclipseware.imnotcheatingyouare.client.utils.ImGuiTextures.entityIcon(lastTarget);
        }

        float squash = 1f - hurt * 0.08f;
        float cx = ax + avatar / 2f;
        float cy = ay + avatar / 2f;
        float half = avatar / 2f * squash;
        int avatarBgTop = RenderUtils.toImGuiColor(accent, alpha * 0.45f);
        int avatarBgBottom = RenderUtils.toImGuiColor(accent.darker().darker(), alpha * 0.6f);
        dl.addRectFilled(ax, ay, ax + avatar, ay + avatar, avatarBgBottom, 7f * k);
        dl.addRectFilledMultiColor(ax + 3f * k, ay, ax + avatar - 3f * k, ay + avatar * 0.5f, avatarBgTop, avatarBgTop, avatarBgBottom, avatarBgBottom);

        int imgTint = RenderUtils.toImGuiColor(255, (int) (255 - hurt * 140), (int) (255 - hurt * 140), a255);
        if (face != null && isPlayer) {
            float inset = 3f * k;
            dl.addImageRounded(face.texture(), cx - half + inset, cy - half + inset, cx + half - inset, cy + half - inset, face.u0(), face.v0(), face.u1(), face.v1(), imgTint, 5f * k, imgui.flag.ImDrawFlags.RoundCornersAll);
            if (hat != null) {
                float hatOut = inset * 0.4f;
                dl.addImageRounded(hat.texture(), cx - half + hatOut, cy - half + hatOut, cx + half - hatOut, cy + half - hatOut, hat.u0(), hat.v0(), hat.u1(), hat.v1(), imgTint, 6f * k, imgui.flag.ImDrawFlags.RoundCornersAll);
            }
        } else if (face != null) {
            float inset = avatar * 0.14f;
            dl.addImage(face.texture(), cx - half + inset, cy - half + inset, cx + half - inset, cy + half - inset, face.u0(), face.v0(), face.u1(), face.v1(), imgTint);
        } else {
            String initial = name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase();
            ImGui.calcTextSize(comboSizeBuf, initial);
            dl.addText(cx - comboSizeBuf.x / 2f, cy - comboSizeBuf.y / 2f, RenderUtils.toImGuiColor(255, 255, 255, a255), initial);
        }
        dl.addRect(ax, ay, ax + avatar, ay + avatar, RenderUtils.toImGuiColor(accent, alpha * (0.35f + hurt * 0.5f)), 7f * k, 0, 1.2f * k);

        float tx = ax + avatar + pad;
        float right = x + width - pad;
        dl.addText(tx, y + pad - 1f * k, RenderUtils.toImGuiColor(245, 242, 252, a255), name);

        String hpStr = String.format("%.1f", hp);
        ImGui.calcTextSize(hpSizeBuf, hpStr);
        dl.addText(right - hpSizeBuf.x, y + pad - 1f * k, RenderUtils.toImGuiColor(hpColor, alpha), hpStr);

        String info;
        int infoCol;
        if (mc.player != null && lastTarget != mc.player) {
            float mine = mc.player.getHealth() + mc.player.getAbsorptionAmount();
            float theirs = hp + lastTarget.getAbsorptionAmount();
            String dist = String.format("%.1fm", mc.player.distanceTo(lastTarget));
            String armor = lastTarget.getArmorValue() + " armor";
            if (mine > theirs + 0.5f) {
                info = "Winning  " + dist + "  " + armor;
                infoCol = RenderUtils.toImGuiColor(120, 230, 150, a255);
            } else if (theirs > mine + 0.5f) {
                info = "Losing  " + dist + "  " + armor;
                infoCol = RenderUtils.toImGuiColor(255, 110, 110, a255);
            } else {
                info = "Even  " + dist + "  " + armor;
                infoCol = RenderUtils.toImGuiColor(255, 205, 90, a255);
            }
        } else {
            info = "Drag in the HUD editor";
            infoCol = RenderUtils.toImGuiColor(150, 145, 170, a255);
        }
        dl.addText(tx, y + pad + 12f * k, infoCol, info);

        float barH = 5f * k;
        float barX = tx;
        float barY = y + height - pad - barH;
        float barW = right - tx - (comboCount > 0 ? 26f * k : 0f);
        dl.addRectFilled(barX, barY, barX + barW, barY + barH, RenderUtils.toImGuiColor(8, 6, 14, (int) (a255 * 0.9f)), barH / 2f);
        float dmgW = barW * damageHealthFraction;
        if (dmgW > 0) dl.addRectFilled(barX, barY, barX + dmgW, barY + barH, RenderUtils.toImGuiColor(255, 235, 245, (int) (a255 * 0.45f)), barH / 2f);
        float hpW = barW * animatedHealthFraction;
        if (hpW > barH) {
            int c1 = RenderUtils.toImGuiColor(hpColor.brighter(), alpha);
            int c2 = RenderUtils.toImGuiColor(hpColor, alpha);
            dl.addRectFilled(barX, barY, barX + hpW, barY + barH, c2, barH / 2f);
            dl.addRectFilledMultiColor(barX + barH / 2f, barY, barX + hpW - barH / 2f, barY + barH * 0.5f, c1, c1, c2, c2);
        }

        if (comboCount > 0) {
            String comboStr = "x" + comboCount;
            ImGui.calcTextSize(comboSizeBuf, comboStr);
            float bw = Math.max(20f * k, comboSizeBuf.x + 8f * k) * comboPulseScale;
            float bh = 13f * k;
            float bx = right - bw;
            float by = barY + barH / 2f - bh / 2f;
            dl.addRectFilled(bx, by, bx + bw, by + bh, RenderUtils.toImGuiColor(accent, alpha * 0.9f), bh / 2f);
            dl.addText(bx + (bw - comboSizeBuf.x) / 2f, by + (bh - comboSizeBuf.y) / 2f, RenderUtils.toImGuiColor(255, 255, 255, a255), comboStr);
        }
    }
}
