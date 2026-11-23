package com.eclipseware.imnotcheatingyouare.client.module;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.glassui.bridge.ClientNotificationService;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.setting.SettingsManager;
import com.eclipseware.imnotcheatingyouare.client.utils.InputUtil;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class Module {
    private String name;
    private Category category;
    private String subCategory;
    private String description;
    private int keyBind;
    private boolean toggled;
    private boolean wasKeyPressed;
    private boolean hidden;
    private boolean holdMode;
    private boolean expanded = false;

    public static Minecraft mc;

    public Module(String name, Category category, String description) {
        this.name = name;
        this.category = category;
        this.subCategory = "";
        this.description = description;
        this.keyBind = 0;
        this.toggled = false;
        this.hidden = false;
    }

    public Module(String name, Category category) {
        this(name, category, "");
    }

    public Module(String name, Category category, String description, boolean hidden) {
        this.name = name;
        this.category = category;
        this.subCategory = "";
        this.description = description;
        this.keyBind = 0;
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
        ClientNotificationService.onModuleToggled(this, this.toggled);
    }

    public void onEnable() {
    }

    public void onDisable() {
    }

    public void onTick() {
    }

    public void onRenderHUD(GuiGraphicsExtractor guiGraphics, Object tickDelta) {
    }

    public boolean needsTick() {
        return false;
    }

    public void onKeybind() {
        this.toggle();
    }

    public void tickKeybind() {
        if (this.keyBind == 0 || mc == null || mc.getWindow() == null) {
            return;
        }

        boolean isPressed = InputUtil.isDown(this.keyBind);
        var currentScreen = mc.gui.screen();

        if (holdMode) {
            if (isPressed && !wasKeyPressed && currentScreen == null && !this.toggled) {
                toggle();
            } else if (!isPressed && wasKeyPressed && this.toggled) {
                toggle();
            }
            wasKeyPressed = isPressed;
            return;
        }

        if (currentScreen != null) {
            wasKeyPressed = isPressed;
            return;
        }

        if (isPressed && !wasKeyPressed) {
            onKeybind();
        }
        wasKeyPressed = isPressed;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public String getSubCategory() {
        return subCategory;
    }

    public void setSubCategory(String subCategory) {
        this.subCategory = subCategory;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isHoldMode() {
        return holdMode;
    }

    public void setHoldMode(boolean holdMode) {
        this.holdMode = holdMode;
    }

    public int getKeyBind() {
        return keyBind;
    }

    public void setKeyBind(int keyBind) {
        this.keyBind = keyBind;
    }

    public boolean isToggled() {
        return toggled;
    }

    public void setToggled(boolean toggled) {
        this.toggled = toggled;
    }

    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }

    public List<Setting> getSettings() {
        ImnotcheatingyouareClient client = ImnotcheatingyouareClient.INSTANCE;
        SettingsManager sm = client.settingsManager;
        return sm.getSettingsByMod(this);
    }

    public boolean isEnabled() {
        return isToggled();
    }
}
