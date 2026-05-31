package com.eclipseware.imnotcheatingyouare.client.clickgui;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.ui.GuiGraphics;
import com.eclipseware.imnotcheatingyouare.client.utils.FontUtils;
import com.eclipseware.imnotcheatingyouare.client.utils.RenderUtils;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class HudEditorScreen extends Screen {
    private Module draggingModule = null;
    private double dragStartX, dragStartY;
    private double dragStartValX, dragStartValY;

    public HudEditorScreen() {
        super(Component.literal("HUD Editor"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        // Draw translucent dark background
        context.fill(0, 0, this.width, this.height, 0x80000000);

        // Render the active HUD elements so the user sees where they are
        for (Module m : ImnotcheatingyouareClient.INSTANCE.moduleManager.modules) {
            if (m.isToggled() || m.getName().equals("TargetHUD") || m.getName().equals("ArmorHUD")) {
                m.onRenderHUD(context, delta);
            }
        }

        // Draw HUD outline guides
        Color themeColor = RenderUtils.getThemeAccentColor();
        int accent = themeColor.getRGB();

        for (Module m : getHudModules()) {
            Setting xSet = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(m, "X");
            Setting ySet = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(m, "Y");
            if (xSet == null || ySet == null) continue;

            int x = (int) xSet.getValDouble();
            int y = (int) ySet.getValDouble();
            int w = getModuleWidth(m);
            int h = getModuleHeight(m);

            boolean hovered = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
            int outlineColor = (hovered || draggingModule == m) ? accent : 0x44FFFFFF;

            drawBorder(context, x - 2, y - 2, w + 4, h + 4, outlineColor);
            FontUtils.drawString(context, m.getName(), x, y - 10, outlineColor, true);
        }

        // Draw instructions
        FontUtils.drawCenteredString(context, "HUD Editor", this.width / 2, 10, accent);
        FontUtils.drawCenteredString(context, "Drag boxes with left-click to move. Press ESC to save & exit.", this.width / 2, 22, 0xFFBBBBBB);
    }

    private void drawBorder(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int color) {
        graphics.fill(x, y, x + w, y + 1, color);
        graphics.fill(x, y + h - 1, x + w, y + h, color);
        graphics.fill(x, y, x + 1, y + h, color);
        graphics.fill(x + w - 1, y, x + w, y + h, color);
    }

    private List<Module> getHudModules() {
        List<Module> list = new ArrayList<>();
        Module targetHUD = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("TargetHUD");
        Module armorHUD = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("ArmorHUD");
        if (targetHUD != null) list.add(targetHUD);
        if (armorHUD != null) list.add(armorHUD);
        return list;
    }

    private int getModuleWidth(Module m) {
        if (m.getName().equals("TargetHUD")) return 175;
        if (m.getName().equals("ArmorHUD")) {
            Setting layoutSet = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(m, "Layout");
            String layout = layoutSet != null ? layoutSet.getValString() : "Horizontal";
            return layout.equals("Vertical") ? 20 : 96;
        }
        return 100;
    }

    private int getModuleHeight(Module m) {
        if (m.getName().equals("TargetHUD")) return 42;
        if (m.getName().equals("ArmorHUD")) {
            Setting layoutSet = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(m, "Layout");
            String layout = layoutSet != null ? layoutSet.getValString() : "Horizontal";
            return layout.equals("Vertical") ? 112 : 20;
        }
        return 20;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();

        if (event.button() == 0) {
            for (Module m : getHudModules()) {
                Setting xSet = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(m, "X");
                Setting ySet = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(m, "Y");
                if (xSet == null || ySet == null) continue;

                int x = (int) xSet.getValDouble();
                int y = (int) ySet.getValDouble();
                int w = getModuleWidth(m);
                int h = getModuleHeight(m);

                if (mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h) {
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
            Setting xSet = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(draggingModule, "X");
            Setting ySet = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(draggingModule, "Y");
            if (xSet != null && ySet != null) {
                double deltaX = event.x() - dragStartX;
                double deltaY = event.y() - dragStartY;
                xSet.setValDouble(dragStartValX + deltaX);
                ySet.setValDouble(dragStartValY + deltaY);
                return true;
            }
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0 && draggingModule != null) {
            draggingModule = null;
            com.eclipseware.imnotcheatingyouare.client.setting.ConfigManager.save();
            return true;
        }
        return super.mouseReleased(event);
    }
}
