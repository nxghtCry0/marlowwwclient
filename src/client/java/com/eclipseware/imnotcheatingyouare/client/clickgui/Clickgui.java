package com.eclipseware.imnotcheatingyouare.client.clickgui;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.clickgui.components.ModuleButton;
import com.eclipseware.imnotcheatingyouare.client.clickgui.components.Widget;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;

public class Clickgui extends Screen {
    private static Clickgui INSTANCE;

    private final ArrayList<Widget> widgets = new ArrayList<>();

    public Clickgui() {
        super(Component.literal("ClickGui"));
        setInstance();
        load();
    }

    public static Clickgui getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new Clickgui();
        }
        return INSTANCE;
    }

    private void setInstance() {
        INSTANCE = this;
    }

    private void load() {
        int x = 20;
        for (Category category : Category.values()) {
            Widget panel = new Widget(category.name(), x, 20, true);
            for (Module m : ImnotcheatingyouareClient.INSTANCE.moduleManager.getModules(category)) {
                if (!m.isHidden()) {
                    panel.addButton(new ModuleButton(m));
                }
            }
            this.widgets.add(panel);
            x += 120;
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        com.eclipseware.imnotcheatingyouare.client.clickgui.components.Item.context = context;
        context.fill(0, 0, context.guiWidth(), context.guiHeight(), 0x55000000);
        this.widgets.forEach(components -> components.drawScreen(context, mouseX, mouseY, delta));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        this.widgets.forEach(components -> components.mouseClicked((int) click.x(), (int) click.y(), click.button()));
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        this.widgets.forEach(components -> components.mouseReleased((int) click.x(), (int) click.y(), click.button()));
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        long windowHandle = 0;
        try {
            for (java.lang.reflect.Field f : Minecraft.getInstance().getWindow().getClass().getDeclaredFields()) {
                if (f.getType() == long.class) {
                    f.setAccessible(true);
                    windowHandle = f.getLong(Minecraft.getInstance().getWindow());
                    break;
                }
            }
        } catch (Exception e) {}
        
        boolean shiftDown = false;
        if (windowHandle != 0) {
            shiftDown = org.lwjgl.glfw.GLFW.glfwGetKey(windowHandle, org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT) == org.lwjgl.glfw.GLFW.GLFW_PRESS || org.lwjgl.glfw.GLFW.glfwGetKey(windowHandle, org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
        }

        if (shiftDown) {
            if (verticalAmount < 0) {
                this.widgets.forEach(component -> component.setX(component.getX() - 30));
            } else if (verticalAmount > 0) {
                this.widgets.forEach(component -> component.setX(component.getX() + 30));
            }
        } else {
            if (verticalAmount < 0) {
                this.widgets.forEach(component -> component.setY(component.getY() - 15));
            } else if (verticalAmount > 0) {
                this.widgets.forEach(component -> component.setY(component.getY() + 15));
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        boolean wasBinding = false;
        for (Widget widget : this.widgets) {
            for (com.eclipseware.imnotcheatingyouare.client.clickgui.components.Item item : widget.getItems()) {
                if (item instanceof ModuleButton) {
                    for (com.eclipseware.imnotcheatingyouare.client.clickgui.components.Item subItem : ((ModuleButton)item).getItems()) {
                        if (subItem instanceof com.eclipseware.imnotcheatingyouare.client.clickgui.components.BindButton) {
                            if (((com.eclipseware.imnotcheatingyouare.client.clickgui.components.BindButton)subItem).isListening) {
                                wasBinding = true;
                            }
                        }
                    }
                }
            }
        }

        this.widgets.forEach(component -> component.onKeyPressed(input.input()));
        
        if (wasBinding && input.input() == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            return true;
        }
        
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharacterEvent input) {
        this.widgets.forEach(component -> component.onKeyTyped(input.codepointAsString(), 0));
        return super.charTyped(input);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
    
    @Override
    public void extractBackground(net.minecraft.client.gui.GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
    }

    public final ArrayList<Widget> getComponents() {
        return this.widgets;
    }

    public static void playSound() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getSoundManager() != null) {
            mc.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1f));
        }
    }
}