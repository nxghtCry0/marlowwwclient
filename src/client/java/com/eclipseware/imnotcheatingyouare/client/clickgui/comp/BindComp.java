package com.eclipseware.imnotcheatingyouare.client.clickgui.comp;

import com.eclipseware.imnotcheatingyouare.client.clickgui.Clickgui;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;

public class BindComp extends Comp {
    public boolean listening = false;

    public BindComp(double x, double y, Clickgui parent, Module module) {
        this.x = x; this.y = y; this.parent = parent; this.module = module;
    }

    @Override
public void render(GuiGraphics guiGraphics, int mouseX, int mouseY) {
guiGraphics.fill((int)(parent.posX + x), (int)(parent.posY + y), (int)(parent.posX + x + 110), (int)(parent.posY + y + 15), new Color(30, 30, 30).getRGB());

    String keyName = module.getKeyBind() == -1 ? "NONE" : GLFW.glfwGetKeyName(module.getKeyBind(), GLFW.glfwGetKeyScancode(module.getKeyBind()));
    if (keyName == null) keyName = String.valueOf(module.getKeyBind());
    if (module.getKeyBind() == GLFW.GLFW_KEY_RIGHT_SHIFT) keyName = "RSHIFT";
    
    String text = listening ? "Listening..." : "Bind: " + keyName.toUpperCase();
    guiGraphics.drawString(Minecraft.getInstance().font, text, (int)(parent.posX + x + 4), (int)(parent.posY + y + 4), listening ? new Color(230, 10, 230).getRGB() : new Color(200, 200, 200).getRGB(), false);

    if (listening) {
        long window = 0L;
        try {
            for (java.lang.reflect.Field f : Minecraft.getInstance().getWindow().getClass().getDeclaredFields()) {
                if (f.getType() == long.class) {
                    f.setAccessible(true);
                    window = f.getLong(Minecraft.getInstance().getWindow());
                    break;
                }
            }
        } catch (Exception e) {}

        if (window != 0L) {
            for (int i = 32; i <= 348; i++) {
                if (com.mojang.blaze3d.platform.InputConstants.isKeyDown(window, i)) {
                    if (i == GLFW.GLFW_KEY_ESCAPE || i == GLFW.GLFW_KEY_BACKSPACE || i == GLFW.GLFW_KEY_DELETE) {
                        module.setKeyBind(-1);
                    } else {
                        module.setKeyBind(i);
                    }
                    listening = false;
                    Clickgui.playSound();
                    break;
                }
            }
        }
    }
}

@Override
public void mouseClicked(double mouseX, double mouseY, int mouseButton) {
    if (isInside(mouseX, mouseY, parent.posX + x, parent.posY + y, parent.posX + x + 110, parent.posY + y + 15) && mouseButton == 0) {
        listening = !listening;
        Clickgui.playSound();
    }
}

}