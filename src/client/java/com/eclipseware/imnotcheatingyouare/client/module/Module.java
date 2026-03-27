package com.eclipseware.imnotcheatingyouare.client.module;

import net.minecraft.client.Minecraft;

public class Module {
private String name;
private Category category;
private boolean toggled;
protected Minecraft mc = Minecraft.getInstance();

public Module(String name, Category category) {
    this.name = name;
    this.category = category;
}

public String getName() { return name; }
public Category getCategory() { return category; }
public boolean isToggled() { return toggled; }

public float arrayListAnim = 0f;

private int keyBind = -1;
private boolean keyWasDown = false;

public int getKeyBind() { return keyBind; }
public void setKeyBind(int keyBind) { this.keyBind = keyBind; }

public void toggle() {
toggled = !toggled;
if (toggled) {
onEnable();
} else {
onDisable();
}
}

public void onKeybind() {
toggle(); // Default behavior is to toggle. Macros can override this!
}

public void tickKeybind() {
if (keyBind == -1 || mc.getWindow() == null) return;

long windowHandle = 0L;
try {
    for (java.lang.reflect.Field f : mc.getWindow().getClass().getDeclaredFields()) {
        if (f.getType() == long.class) {
            f.setAccessible(true);
            windowHandle = f.getLong(mc.getWindow());
            break;
        }
    }
} catch (Exception e) {}

if (windowHandle == 0L) return;

boolean isDown = com.mojang.blaze3d.platform.InputConstants.isKeyDown(windowHandle, keyBind);
if (isDown && !keyWasDown) {
    onKeybind();
}
keyWasDown = isDown;

}

// --- Module Lifecycle Hooks ---
public void onEnable() {}
public void onDisable() {}
public void onTick() {}

}