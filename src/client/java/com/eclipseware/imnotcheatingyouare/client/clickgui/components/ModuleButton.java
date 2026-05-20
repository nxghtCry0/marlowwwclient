package com.eclipseware.imnotcheatingyouare.client.clickgui.components;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.clickgui.Clickgui;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.utils.RenderUtils;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.List;

public class ModuleButton extends Button {
    private final Module module;
    private List<Item> items = new ArrayList<>();
    private boolean subOpen;

    public ModuleButton(Module module) {
        super(module.getName());
        this.module = module;
        this.initSettings();
    }

    public void initSettings() {
        ArrayList<Item> newItems = new ArrayList<>();
        List<Setting> settings = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingsByMod(this.module);
        
        if (settings != null && !settings.isEmpty()) {
            for (Setting setting : settings) {
                if (setting.isCheck()) {
                    newItems.add(new BooleanButton(setting));
                } else if (setting.isSlider()) {
                    newItems.add(new Slider(setting));
                } else if (setting.isCombo()) {
                    newItems.add(new EnumButton(setting));
                }
            }
        }
        newItems.add(new BindButton(this.module));
        this.items = newItems;
    }

    @Override
    public void drawScreen(GuiGraphicsExtractor context, int mouseX, int mouseY, float partialTicks) {
        java.awt.Color theme = RenderUtils.getThemeAccentColor();
        int accent = (theme.getRGB() & 0x00FFFFFF) | (200 << 24);
        int dark = 0xAA111111;
        int hoverDark = 0xCC222222;

        int fill = this.getState() ? accent : (this.isHovering(mouseX, mouseY) ? hoverDark : dark);
        context.fill((int)this.x, (int)this.y, (int)(this.x + this.width), (int)(this.y + this.height), fill);
        
        drawString(this.getName(), this.x + 4, this.y + 4, this.getState() ? -1 : 0xFFAAAAAA);

        if (!this.items.isEmpty()) {
            drawString(this.subOpen ? "-" : "+", this.x + this.width - 12, this.y + 4, 0xFFAAAAAA);

            if (this.subOpen) {
                float height = 14.0f;
                context.fill((int)this.x + 3, (int)(this.y + 14), (int)this.x + 4, (int)(this.y + this.getHeight()), accent);

                for (Item item : this.items) {
                    if (!item.isHidden()) {
                        item.setLocation(this.x + 8.0f, this.y + height);
                        item.setWidth(this.width - 8);
                        item.drawScreen(context, mouseX, mouseY, partialTicks);
                        height += item.getHeight();
                    }
                    item.update();
                }
            }
        }
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (!this.items.isEmpty()) {
            if (mouseButton == 1 && this.isHovering(mouseX, mouseY)) {
                this.subOpen = !this.subOpen;
                Clickgui.playSound();
            }
            if (this.subOpen) {
                for (Item item : this.items) {
                    if (item.isHidden()) continue;
                    item.mouseClicked(mouseX, mouseY, mouseButton);
                }
            }
        }
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int releaseButton) {
        super.mouseReleased(mouseX, mouseY, releaseButton);
        if (!this.items.isEmpty() && this.subOpen) {
            for (Item item : this.items) {
                if (item.isHidden()) continue;
                item.mouseReleased(mouseX, mouseY, releaseButton);
            }
        }
    }

    @Override
    public void onKeyTyped(String typedChar, int keyCode) {
        super.onKeyTyped(typedChar, keyCode);
        if (!this.items.isEmpty() && this.subOpen) {
            for (Item item : this.items) {
                if (item.isHidden()) continue;
                item.onKeyTyped(typedChar, keyCode);
            }
        }
    }

    @Override
    public void onKeyPressed(int key) {
        super.onKeyPressed(key);
        if (!this.items.isEmpty() && this.subOpen) {
            for (Item item : this.items) {
                if (item.isHidden()) continue;
                item.onKeyPressed(key);
            }
        }
    }

    @Override
    public int getHeight() {
        if (this.subOpen) {
            int height = 14;
            for (Item item : this.items) {
                if (item.isHidden()) continue;
                height += item.getHeight();
            }
            return height;
        }
        return 14;
    }

    public Module getModule() {
        return this.module;
    }

    public java.util.List<Item> getItems() {
        return this.items;
    }

    @Override
    public boolean isHovering(int mouseX, int mouseY) {
        return mouseX >= this.x && mouseX <= this.x + this.width && mouseY >= this.y && mouseY <= this.y + 14.0f;
    }

    @Override
    public void toggle() {
        this.module.toggle();
    }

    @Override
    public boolean getState() {
        return this.module.isToggled();
    }
}
