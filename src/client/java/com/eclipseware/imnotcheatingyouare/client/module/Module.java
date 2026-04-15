package com.eclipseware.imnotcheatingyouare.client.module;

import net.minecraft.client.Minecraft;

public class Module {
    private String name;
    private Category category;
    private String description;
    private int keyBind;
    private boolean toggled;
    private boolean wasKeyPressed;
    private boolean hidden;
    
    public static Minecraft mc;

    public Module(String name, Category category, String description) {
        this.name = name;
        this.category = category;
        this.description = description;
        this.keyBind = -1;
        this.toggled = false;
        this.hidden = false;
    }

    public Module(String name, Category category) {
        this.name = name;
        this.category = category;
        this.description = "";
        this.keyBind = -1;
        this.toggled = false;
        this.hidden = false;
    }

    public Module(String name, Category category, String description, boolean hidden) {
        this.name = name;
        this.category = category;
        this.description = description;
        this.keyBind = -1;
        this.toggled = false;
        this.hidden = hidden;
    }

    public void toggle() {
        this.toggled = !this.toggled;
        if (this.toggled) {
            onEnable();
        } else {
            onDisable();
        }
    }

    public void onEnable() {}
    public void onDisable() {}
    
    public void onTick() {}

    public void onKeybind() {
        this.toggle();
    }

    public void tickKeybind() {
        if (this.keyBind == -1 || mc == null || mc.getWindow() == null) return;

        // Don't toggle modules while any screen is open (chat, inventory, GUIs).
        // Reset latching so releasing the key inside a screen doesn't cause
        // an instant toggle the moment the screen closes.
        if (mc.screen != null) {
            wasKeyPressed = false;
            return;
        }

        long windowHandle = 0;
        try {
            for (java.lang.reflect.Field f : mc.getWindow().getClass().getDeclaredFields()) {
                if (f.getType() == long.class) {
                    f.setAccessible(true);
                    windowHandle = f.getLong(mc.getWindow());
                    break;
                }
            }
        } catch (Exception e) {}

        if (windowHandle == 0) return;

        boolean isPressed = org.lwjgl.glfw.GLFW.glfwGetKey(windowHandle, this.keyBind) == org.lwjgl.glfw.GLFW.GLFW_PRESS;

        if (isPressed && !wasKeyPressed) {
            onKeybind();
        }
        wasKeyPressed = isPressed;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public int getKeyBind() { return keyBind; }
    public void setKeyBind(int keyBind) { this.keyBind = keyBind; }
    
    public boolean isToggled() { return toggled; }
    public void setToggled(boolean toggled) { this.toggled = toggled; }
    
    public boolean isHidden() { return hidden; }
    public void setHidden(boolean hidden) { this.hidden = hidden; }
}