package com.eclipseware.imnotcheatingyouare.client.clickgui.comp;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.clickgui.Clickgui;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.utils.AnimationUtil;
import com.eclipseware.imnotcheatingyouare.client.utils.FontUtils;
import net.minecraft.client.gui.GuiGraphics;
import java.awt.Color;

public class BindComp extends Comp {
    public boolean isBinding;

    public BindComp(double x, double y, Clickgui parent, Module module) {
        this.x = x; this.y = y; this.parent = parent; this.module = module;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        String bindName = getBindDisplayName(module.getKeyBind());
        
        Module theme = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("Theme");
        int r = 155, g = 60, b = 255;
        if (theme != null) {
            r = (int) ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(theme, "Accent R").getValDouble();
            g = (int) ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(theme, "Accent G").getValDouble();
            b = (int) ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(theme, "Accent B").getValDouble();
        }

        String label = isBinding ? "Listening..." : "Keybind: ";
        String value = isBinding ? "" : bindName;
        int totalWidth = FontUtils.width(label + value) + 12;
        
        boolean hovered = isInside(mouseX, mouseY, parent.posX + x, parent.posY + y - 2, parent.posX + x + totalWidth, parent.posY + y + 18);
        AnimationUtil.drawRoundedRect(guiGraphics, (int)(parent.posX + x), (int)(parent.posY + y - 2), totalWidth, 20, 4, hovered ? new Color(45, 45, 50).getRGB() : new Color(30, 30, 35).getRGB());

        FontUtils.drawString(guiGraphics, label, (int)(parent.posX + x + 6), (int)(parent.posY + y + 4), new Color(200, 200, 200).getRGB(), false);
        if (!isBinding) {
            FontUtils.drawString(guiGraphics, value, (int)(parent.posX + x + 6) + FontUtils.width(label), (int)(parent.posY + y + 4), new Color(r, g, b).getRGB(), false);
        }
    }

    private String getBindDisplayName(int keyCode) {
        if (keyCode == -1) return "None";

        if (keyCode >= 0 && keyCode < 8) {
            return switch (keyCode) {
                case 0 -> "MOUSE 1";
                case 1 -> "MOUSE 2";
                case 2 -> "MOUSE 3";
                case 3 -> "MOUSE 4";
                case 4 -> "MOUSE 5";
                case 5 -> "MOUSE 6";
                case 6 -> "MOUSE 7";
                case 7 -> "MOUSE 8";
                default -> "MOUSE " + (keyCode + 1);
            };
        }

        String name = org.lwjgl.glfw.GLFW.glfwGetKeyName(keyCode, org.lwjgl.glfw.GLFW.glfwGetKeyScancode(keyCode));
        if (name == null) {
            return switch (keyCode) {
                case org.lwjgl.glfw.GLFW.GLFW_KEY_INSERT -> "INS";
                case org.lwjgl.glfw.GLFW.GLFW_KEY_DELETE -> "DEL";
                case org.lwjgl.glfw.GLFW.GLFW_KEY_HOME -> "HOME";
                case org.lwjgl.glfw.GLFW.GLFW_KEY_END -> "END";
                case org.lwjgl.glfw.GLFW.GLFW_KEY_PAGE_UP -> "PG UP";
                case org.lwjgl.glfw.GLFW.GLFW_KEY_PAGE_DOWN -> "PG DN";
                case org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT -> "LSHIFT";
                case org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT -> "RSHIFT";
                case org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_CONTROL -> "LCTRL";
                case org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_CONTROL -> "RCTRL";
                case org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_ALT -> "LALT";
                case org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_ALT -> "RALT";
                case org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE -> "SPACE";
                default -> "UNK";
            };
        }
        return name.toUpperCase();
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (isBinding) {
            if (!isInside(mouseX, mouseY, parent.posX + x, parent.posY + y - 2, parent.posX + x + 120, parent.posY + y + 18) || mouseButton != 0) {
                module.setKeyBind(mouseButton);
                isBinding = false;
                Clickgui.playSound();
                return;
            }
        }

        String label = isBinding ? "Listening..." : "Keybind: ";
        String value = isBinding ? "" : getBindDisplayName(module.getKeyBind());
        int totalWidth = FontUtils.width(label + value) + 12;

        if (isInside(mouseX, mouseY, parent.posX + x, parent.posY + y - 2, parent.posX + x + totalWidth, parent.posY + y + 18) && mouseButton == 0) {
            isBinding = !isBinding;
            Clickgui.playSound();
        }
    }

    public void keyPressed(int key, int scanCode, int modifiers) {
        if (isBinding) {
            if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_DELETE || key == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE || key == org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE) {
                module.setKeyBind(-1);
            } else {
                module.setKeyBind(key);
            }
            isBinding = false;
            Clickgui.playSound();
        }
    }
}