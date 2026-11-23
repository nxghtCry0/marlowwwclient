package com.eclipseware.imnotcheatingyouare.client.clickgui;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.glassui.render.SatoshiFont;
import com.eclipseware.imnotcheatingyouare.client.glassui.render.SatoshiWeight;
import com.eclipseware.imnotcheatingyouare.client.glassui.render.TextGradient;
import com.eclipseware.imnotcheatingyouare.client.glassui.render.UiRenderPrimitives;
import com.eclipseware.imnotcheatingyouare.client.glassui.theme.UiTheme;
import com.eclipseware.imnotcheatingyouare.client.glassui.theme.UiThemeContext;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.module.ModuleManager;
import com.eclipseware.imnotcheatingyouare.client.module.impl.TargetHUD;
import com.eclipseware.imnotcheatingyouare.client.setting.ConfigManager;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.setting.SettingsManager;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class HudEditorScreen extends Screen {
    private Module draggingModule = null;
    private double dragStartX;
    private double dragStartY;
    private double dragStartValX;
    private double dragStartValY;

    public HudEditorScreen() {
        super(Component.literal("HUD Editor"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        UiTheme theme = UiThemeContext.current();
        int accent = theme.accentColor();

        context.fill(0, 0, this.width, this.height, UiRenderPrimitives.argb(120, 8, 9, 12));

        ImnotcheatingyouareClient client = ImnotcheatingyouareClient.INSTANCE;
        if (client != null && client.moduleManager != null) {
            ModuleManager mm = client.moduleManager;
            for (Module m : mm.modules) {
                String mName = m.getName();
                if (m.isToggled() || "TargetHUD".equals(mName) || "ArmorHUD".equals(mName)) {
                    m.onRenderHUD(context, delta);
                }
            }
        }

        SettingsManager sm = client != null ? client.settingsManager : null;
        if (sm != null) {
            for (Module m : getHudModules()) {
                Setting xSet = sm.getSettingByName(m, "X");
                Setting ySet = sm.getSettingByName(m, "Y");
                if (xSet == null || ySet == null) {
                    continue;
                }

                float x = (float) xSet.getValDouble();
                float y = (float) ySet.getValDouble();
                float w = getModuleWidth(sm, m);
                float h = getModuleHeight(sm, m);

                boolean hovered = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
                boolean active = hovered || draggingModule == m;
                float r = 7f * theme.cornerRadiusScale();

                UiRenderPrimitives.drawThemedGlassCard(
                    context,
                    x - 3f,
                    y - 3f,
                    w + 6f,
                    h + 6f,
                    r,
                    active ? 44f : 24f,
                    active ? 26f : 14f,
                    active ? 78f : 42f,
                    active ? 36f : 18f,
                    1f,
                    active ? 0.85f : 0.15f,
                    hovered ? 1f : 0f,
                    theme
                );
                SatoshiFont.draw(
                    context,
                    m.getName(),
                    x,
                    y - 11f,
                    5.8f,
                    active ? TextGradient.accent(1f, accent, theme) : TextGradient.silver(0.88f, theme),
                    SatoshiWeight.SEMIBOLD
                );
            }
        }

        float bannerW = 286f;
        float bannerH = 34f;
        float bx = (this.width - bannerW) * 0.5f;
        float by = 12f;
        UiRenderPrimitives.drawRoundedShadow(context, bx, by + 1f, bannerW, bannerH, 11f, 8f, UiRenderPrimitives.argb(95, 0, 0, 0));
        UiRenderPrimitives.drawGlassCard(
            context,
            bx,
            by,
            bannerW,
            bannerH,
            11f,
            theme.windowTopColor(228),
            theme.windowBottomColor(238),
            UiRenderPrimitives.argb(74, 255, 255, 255),
            UiRenderPrimitives.argb(28, 255, 255, 255)
        );
        SatoshiFont.drawCentered(context, "HUD Overlay Editor", this.width * 0.5f, by + 7.5f, 7.2f, TextGradient.pearl(1f, accent, 0.34f, theme), SatoshiWeight.SEMIBOLD);
        SatoshiFont.drawCentered(context, "Drag overlays with Left Click • Press ESC to save & exit", this.width * 0.5f, by + 20.5f, 4.9f, TextGradient.mist(0.84f, theme), SatoshiWeight.MEDIUM);
    }

    private List<Module> getHudModules() {
        List<Module> list = new ArrayList<>();
        ImnotcheatingyouareClient client = ImnotcheatingyouareClient.INSTANCE;
        if (client == null || client.moduleManager == null) {
            return list;
        }
        ModuleManager mm = client.moduleManager;
        Module targetHUD = mm.getModule("TargetHUD");
        Module armorHUD = mm.getModule("ArmorHUD");
        if (targetHUD != null) {
            list.add(targetHUD);
        }
        if (armorHUD != null) {
            list.add(armorHUD);
        }
        return list;
    }

    private int getModuleWidth(SettingsManager sm, Module m) {
        if ("TargetHUD".equals(m.getName())) {
            return (int) TargetHUD.WIDTH;
        }
        if ("ArmorHUD".equals(m.getName())) {
            Setting layoutSet = sm.getSettingByName(m, "Layout");
            String layout = layoutSet != null ? layoutSet.getValString() : "Horizontal";
            return "Vertical".equals(layout) ? 20 : 96;
        }
        return 100;
    }

    private int getModuleHeight(SettingsManager sm, Module m) {
        if ("TargetHUD".equals(m.getName())) {
            return (int) TargetHUD.HEIGHT;
        }
        if ("ArmorHUD".equals(m.getName())) {
            Setting layoutSet = sm.getSettingByName(m, "Layout");
            String layout = layoutSet != null ? layoutSet.getValString() : "Horizontal";
            return "Vertical".equals(layout) ? 112 : 20;
        }
        return 20;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        ImnotcheatingyouareClient client = ImnotcheatingyouareClient.INSTANCE;
        SettingsManager sm = client != null ? client.settingsManager : null;

        if (sm != null && (event.button() == InputConstants.MOUSE_BUTTON_LEFT || event.button() == InputConstants.MOUSE_BUTTON_RIGHT)) {
            for (Module m : getHudModules()) {
                Setting xSet = sm.getSettingByName(m, "X");
                Setting ySet = sm.getSettingByName(m, "Y");
                if (xSet == null || ySet == null) {
                    continue;
                }

                int x = (int) xSet.getValDouble();
                int y = (int) ySet.getValDouble();
                int w = getModuleWidth(sm, m);
                int h = getModuleHeight(sm, m);

                if (mouseX >= x - 3 && mouseX <= x + w + 3 && mouseY >= y - 3 && mouseY <= y + h + 3) {
                    draggingModule = m;
                    dragStartX = mouseX;
                    dragStartY = mouseY;
                    dragStartValX = xSet.getValDouble();
                    dragStartValY = ySet.getValDouble();
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (draggingModule != null) {
            ImnotcheatingyouareClient client = ImnotcheatingyouareClient.INSTANCE;
            SettingsManager sm = client != null ? client.settingsManager : null;
            if (sm != null) {
                Setting xSet = sm.getSettingByName(draggingModule, "X");
                Setting ySet = sm.getSettingByName(draggingModule, "Y");
                if (xSet != null && ySet != null) {
                    double deltaX = event.x() - dragStartX;
                    double deltaY = event.y() - dragStartY;
                    xSet.setValDouble(dragStartValX + deltaX);
                    ySet.setValDouble(dragStartValY + deltaY);
                    return true;
                }
            }
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (draggingModule != null) {
            draggingModule = null;
            ConfigManager.save();
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public void onClose() {
        ConfigManager.save();
        super.onClose();
    }
}
