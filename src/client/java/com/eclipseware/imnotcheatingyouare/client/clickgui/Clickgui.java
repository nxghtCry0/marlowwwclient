package com.eclipseware.imnotcheatingyouare.client.clickgui;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.clickgui.comp.*;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.awt.Color;
import java.util.ArrayList;

import com.eclipseware.imnotcheatingyouare.client.ui.ArrayListHud;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

public class Clickgui extends Screen {
public double posX, posY, windowWidth, windowHeight, dragX, dragY;
public boolean dragging;
public Category selectedCategory;
public Module selectedModule;
public int modeIndex;

public float openAnim = 0f;

public ArrayList<Comp> comps = new ArrayList<>();

public Clickgui() {
    super(Component.literal("ClickGUI"));
    dragging = false;
    selectedCategory = Category.values()[0]; 
    posX = -1; // Default unset position
    posY = -1;
}

@Override
public boolean isPauseScreen() {
    return false; // Don't pause the game when ClickGUI is open
}

public static void playSound() {
    Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
}

@Override
protected void init() {
    super.init();
    windowWidth = 450;
    windowHeight = 300;
    openAnim = 0f; // Reset animation on open
    
    // Only center if it hasn't been moved yet
    if (posX == -1 && posY == -1) {
        posX = (this.width - windowWidth) / 2.0;
        posY = (this.height - windowHeight) / 2.0;
    }
}

@Override
public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    super.render(guiGraphics, mouseX, mouseY, partialTick);
    
    // Smooth scaling pop-in animation
    openAnim += (1.0f - openAnim) * 0.2f;

    if (dragging) {
        posX = mouseX - dragX;
        posY = mouseY - dragY;
    }
    
    if (ArrayListHud.INSTANCE.dragging) {
        ArrayListHud.INSTANCE.x = mouseX - ArrayListHud.INSTANCE.dragX;
        ArrayListHud.INSTANCE.y = mouseY - ArrayListHud.INSTANCE.dragY;
    }

    guiGraphics.pose().pushMatrix();

    // Apply scaling at the center of the window
    float centerX = (float) (posX + windowWidth / 2.0);
    float centerY = (float) (posY + windowHeight / 2.0);
    guiGraphics.pose().translate(centerX, centerY);
    guiGraphics.pose().scale(openAnim, openAnim);
    guiGraphics.pose().translate(-centerX, -centerY);

    guiGraphics.fill((int)posX, (int)posY, (int)(posX + windowWidth), (int)(posY + windowHeight), new Color(25, 25, 25, 240).getRGB());
    guiGraphics.fill((int)posX, (int)posY, (int)(posX + windowWidth), (int)(posY + 30), new Color(15, 15, 15, 255).getRGB());

    int tabWidth = (int) (windowWidth / Category.values().length);
    int tabOffset = 0;

    for (Category category : Category.values()) {
        boolean isSelected = category.equals(selectedCategory);
        if (isSelected) {
            guiGraphics.fill((int)posX + tabOffset, (int)posY + 28, (int)posX + tabOffset + tabWidth, (int)posY + 30, new Color(230, 10, 230).getRGB());
        }
        int textWidth = Minecraft.getInstance().font.width(category.name());
        guiGraphics.drawString(Minecraft.getInstance().font, category.name(), (int)posX + tabOffset + (tabWidth / 2) - (textWidth / 2), (int)posY + 10, isSelected ? -1 : new Color(170, 170, 170).getRGB(), false);
        tabOffset += tabWidth;
    }

    guiGraphics.fill((int)posX, (int)posY + 30, (int)posX + 120, (int)(posY + windowHeight), new Color(20, 20, 20, 255).getRGB());
    
    int modY = (int)posY + 35;
    for (Module m : ImnotcheatingyouareClient.INSTANCE.moduleManager.getModules(selectedCategory)) {
        if (m.equals(selectedModule)) {
            guiGraphics.fill((int)posX, modY - 2, (int)posX + 120, modY + 12, new Color(40, 40, 40).getRGB());
        }
        guiGraphics.drawString(Minecraft.getInstance().font, m.getName(), (int)posX + 10, modY, m.isToggled() ? new Color(230, 10, 230).getRGB() : new Color(170, 170, 170).getRGB(), false);
        modY += 18;
    }

    if (selectedModule != null) {
guiGraphics.drawString(Minecraft.getInstance().font, selectedModule.getName() + " Settings", (int)posX + 130, (int)posY + 40, -1, false);

    if (comps.isEmpty()) {
        guiGraphics.drawString(Minecraft.getInstance().font, "Nothing to config here!", (int)posX + 130, (int)posY + 65, new Color(170, 170, 170).getRGB(), false);
    } else {
        for (Comp comp : comps) {
comp.render(guiGraphics, mouseX, mouseY);
}
}
}

guiGraphics.pose().popMatrix(); // End of scaling

}

@Override
public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
double mouseX = event.x();
double mouseY = event.y();
int button = event.button();

// ArrayList Dragging Check
Module arrayListMod = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("ArrayList");
if (arrayListMod != null && arrayListMod.isToggled()) {
// Because we updated ArrayListHud to anchor to 'x' regardless of alignment, we just check a standard box
if (isInside(mouseX, mouseY, ArrayListHud.INSTANCE.x, ArrayListHud.INSTANCE.y, ArrayListHud.INSTANCE.x + 100, ArrayListHud.INSTANCE.y + 100) && button == 0) {
ArrayListHud.INSTANCE.dragging = true;
ArrayListHud.INSTANCE.dragX = mouseX - ArrayListHud.INSTANCE.x;
ArrayListHud.INSTANCE.dragY = mouseY - ArrayListHud.INSTANCE.y;
}
}

if (isInside(mouseX, mouseY, posX, posY, posX + windowWidth, posY + 30) && button == 0) {
    dragging = true;
    dragX = mouseX - posX;
    dragY = mouseY - posY;
}

    int tabWidth = (int) (windowWidth / Category.values().length);
int tabOffset = 0;
for (Category category : Category.values()) {
if (isInside(mouseX, mouseY, posX + tabOffset, posY, posX + tabOffset + tabWidth, posY + 30) && button == 0) {
selectedCategory = category;
selectedModule = null;
comps.clear();
playSound();
}
tabOffset += tabWidth;
}

int modY = (int)posY + 35;
for (Module m : ImnotcheatingyouareClient.INSTANCE.moduleManager.getModules(selectedCategory)) {
    if (isInside(mouseX, mouseY, posX, modY - 2, posX + 120, modY + 12)) {
        playSound();
        if (button == 0) m.toggle();
        else if (button == 1) {
            selectedModule = m;
            loadComponents(m);
        }
    }
    modY += 18;
}

    // Create a copy to avoid ConcurrentModificationException if a setting reloads the layout mid-click
    ArrayList<Comp> compsCopy = new ArrayList<>(comps);
    for (Comp comp : compsCopy) {
        comp.mouseClicked(mouseX, mouseY, button);
    }
    return false;
}

@Override
public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent event) {
double mouseX = event.x();
double mouseY = event.y();
int state = event.button();

    dragging = false;
    ArrayListHud.INSTANCE.dragging = false;

    ArrayList<Comp> compsCopy = new ArrayList<>(comps);
    for (Comp comp : compsCopy) {
        comp.mouseReleased(mouseX, mouseY, state);
    }
    return false;
}

public void loadComponents(Module m) {
    comps.clear();
    int sY = 65; 
    
    // Always add a Bind component at the top of the settings page!
    comps.add(new com.eclipseware.imnotcheatingyouare.client.clickgui.comp.BindComp(130, sY, this, m));
    sY += 20;

    if (ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingsByMod(m) != null) {
        
        // Get the current mode string for conditional checks
        Setting modeSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(m, "Mode");
        String currentMode = modeSetting != null ? modeSetting.getValString() : "";

        for (Setting setting : ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingsByMod(m)) {
            
            // --- CONDITIONAL VISIBILITY LOGIC ---
            // Triggerbot: Show Legit settings only in Legit mode
            if (m.getName().equals("Triggerbot")) {
                if (currentMode.equals("Blatant")) {
                    // Hide Legit-specific settings in Blatant
if (setting.getName().equals("Range") || setting.getName().equals("Min Delay (Ticks)") ||
setting.getName().equals("Max Delay (Ticks)") || setting.getName().equals("Inventory Fix") ||
setting.getName().equals("Simulate Mouse Click")) continue;
                } else if (currentMode.equals("Legit")) {
                    // Hide Blatant-specific settings in Legit
                    if (setting.getName().equals("Packet Bypass")) continue;
                }
            }
            // AimAssist: Hide smoothness settings in Snap mode
if (m.getName().equals("AimAssist") && currentMode.equals("Snap")) {
if (setting.getName().equals("Smoothness") || setting.getName().equals("Ignore Mouse %")) continue;
}
// HitSelect: Hide Max HurtTime in Criticals mode
if (m.getName().equals("HitSelect") && currentMode.equals("Criticals")) {
if (setting.getName().equals("Max HurtTime")) continue;
}
// AutoShieldBreaker: Dynamic Swap Back settings
if (m.getName().equals("AutoShieldBreaker")) {
if (currentMode.equals("Silent")) {
if (setting.getName().equals("Swap Back") || setting.getName().equals("Swap Back Delay (ms)")) continue;
} else if (currentMode.equals("Swap")) {
Setting swapBackSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(m, "Swap Back");
boolean swapBack = swapBackSetting != null && swapBackSetting.getValBoolean();
if (!swapBack && setting.getName().equals("Swap Back Delay (ms)")) continue;
}
}
// ------------------------------------

        if (setting.isCombo()) { comps.add(new Combo(130, sY, this, m, setting)); sY += 20; }
            if (setting.isCheck()) { comps.add(new CheckBox(130, sY, this, m, setting)); sY += 20; }
            if (setting.isSlider()) { comps.add(new Slider(130, sY, this, m, setting)); sY += 25; }
        }
    }
}

public boolean isInside(double mouseX, double mouseY, double x, double y, double x2, double y2) {
    return (mouseX >= x && mouseX <= x2) && (mouseY >= y && mouseY <= y2);
}

}