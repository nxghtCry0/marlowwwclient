package com.eclipseware.imnotcheatingyouare.client.clickgui.components;

import com.eclipseware.imnotcheatingyouare.client.clickgui.Clickgui;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;

public class BindButton extends Button {
    private final Module module;
    public boolean isListening;

    public BindButton(Module module) {
        super("Bind");
        this.module = module;
        this.width = 15;
    }

    private String getKeyName(int key) {
        if (key == -1) return "NONE";
        
        if (key >= 0 && key <= 7) {
            if (key == 1) return "RMB";
            if (key == 2) return "MMB";
            return "MB" + (key + 1);
        }

        switch (key) {
            case GLFW.GLFW_KEY_RIGHT_SHIFT: return "RSHIFT";
            case GLFW.GLFW_KEY_LEFT_SHIFT: return "LSHIFT";
            case GLFW.GLFW_KEY_RIGHT_CONTROL: return "RCTRL";
            case GLFW.GLFW_KEY_LEFT_CONTROL: return "LCTRL";
            case GLFW.GLFW_KEY_RIGHT_ALT: return "RALT";
            case GLFW.GLFW_KEY_LEFT_ALT: return "LALT";
            case GLFW.GLFW_KEY_TAB: return "TAB";
            case GLFW.GLFW_KEY_SPACE: return "SPACE";
            case GLFW.GLFW_KEY_ENTER: return "ENTER";
            case GLFW.GLFW_KEY_ESCAPE: return "NONE";
        }

        String str = GLFW.glfwGetKeyName(key, 0);
        if (str == null) return "UNKNOWN: " + key;
        return str.toUpperCase();
    }

    @Override
    public void drawScreen(DrawContext context, int mouseX, int mouseY, float partialTicks) {
        int dark = 0x22000000;
        int hoverDark = 0x44222222;
        int fill = this.isHovering(mouseX, mouseY) ? hoverDark : dark;

        context.fill((int)this.x, (int)this.y, (int)(this.x + this.width), (int)(this.y + this.height), fill);
        
        if (this.isListening) {
            drawString("Listening...", this.x + 2.3f, this.y - 1.7f + 6, -1);
        } else {
            drawString("Bind " + ChatFormatting.GRAY + getKeyName(this.module.getKeyBind()), this.x + 2.3f, this.y - 1.7f + 6, -1);
        }
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        boolean wasListening = this.isListening;
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (wasListening) {
            if (mouseButton != 0 && mouseButton != 1) { 
                this.module.setKeyBind(mouseButton);
            }
            this.isListening = false;
        } else if (this.isHovering(mouseX, mouseY)) {
            Clickgui.playSound();
        }
    }

    @Override
    public void onKeyPressed(int key) {
        if (this.isListening) {
            int targetKey = key;
            if (key == GLFW.GLFW_KEY_DELETE || key == GLFW.GLFW_KEY_BACKSPACE || key == GLFW.GLFW_KEY_ESCAPE) {
                targetKey = -1;
            }
            this.module.setKeyBind(targetKey);
            this.isListening = false;
        }
    }

    @Override
    public void toggle() {
        this.isListening = !this.isListening;
    }

    @Override
    public boolean getState() {
        return !this.isListening;
    }
}
