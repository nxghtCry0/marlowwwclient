package com.eclipseware.imnotcheatingyouare.client.clickgui;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.ui.*;
import com.eclipseware.imnotcheatingyouare.client.utils.RenderUtils;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NewClickgui extends Screen {
    private Category selectedCategory = Category.Combat;
    private Module expandedModule = null;
    
    private final Map<Module, GlassyToggle> moduleToggles = new HashMap<>();
    private final List<GlassyButton> categoryButtons = new ArrayList<>();
    private final List<AbstractWidget> settingWidgets = new ArrayList<>();
    private final List<Setting> activeSettings = new ArrayList<>();
    private final Map<Module, Float> moduleConfigHeights = new HashMap<>();
    
    private EditBox searchBox;
    
    private float scrollOffset = 0;
    private float targetScrollOffset = 0;
    private float animatedCatY = -1;
    
    private float moduleListSlideX = 50f;
    private float moduleListAlpha = 0f;
    private float categorySlideX = -50f;
    private float categoryAlpha = 0f;

    public NewClickgui() {
        super(Component.literal("Marlowww Client"));
    }

    @Override
    protected void init() {
        this.clearWidgets();
        categoryButtons.clear();
        moduleToggles.clear();
        settingWidgets.clear();
        activeSettings.clear();

        int panelWidth = 550;
        int panelHeight = 320;
        int startX = (this.width - panelWidth) / 2;
        int startY = (this.height - panelHeight) / 2;

        int catX = startX + 10;
        int catY = startY + 30;

        for (Category category : Category.values()) {
            GlassyButton btn = new GlassyButton(catX, catY, 100, 24, Component.literal(category.name()), () -> {
                if (this.selectedCategory != category) {
                    this.selectedCategory = category;
                    this.expandedModule = null;
                    this.targetScrollOffset = 0;
                    this.moduleListSlideX = 30f;
                    this.moduleListAlpha = 0f;
                    this.init();
                }
            }, true, category == selectedCategory ? GlassyButton.Style.PRIMARY : GlassyButton.Style.NORMAL);
            
            categoryButtons.add(btn);
            this.addRenderableWidget(btn);
            catY += 28;
        }

        for (Module m : ImnotcheatingyouareClient.INSTANCE.moduleManager.modules) {
            if (m.isHidden()) continue;
            GlassyToggle toggle = new GlassyToggle(0, 0, 45, 20, m::isToggled, val -> m.toggle());
            moduleToggles.put(m, toggle);
            this.addRenderableWidget(toggle);
        }
        
        searchBox = new EditBox(this.font, startX + 130, startY + panelHeight - 30, panelWidth - 140, 20, Component.literal("Search Modules"));
        searchBox.setMaxLength(50);
        this.addRenderableWidget(searchBox);
        
        if (expandedModule != null) {
            rebuildSettingWidgets();
        }
    }

    private void rebuildSettingWidgets() {
        for (AbstractWidget w : settingWidgets) {
            this.removeWidget(w);
        }
        settingWidgets.clear();
        activeSettings.clear();
        
        if (expandedModule == null) return;
        
        List<Setting> settings = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingsByMod(expandedModule);
        if (settings == null) return;
        
        for (Setting s : settings) {
            activeSettings.add(s);
            if (s.isCheck()) {
                GlassyToggle t = new GlassyToggle(0, 0, 45, 20, s::getValBoolean, s::setValBoolean);
                settingWidgets.add(t);
                this.addRenderableWidget(t);
            } else if (s.isSlider()) {
                GlassyIntSlider sl = new GlassyIntSlider(0, 0, 150, 20, 
                    () -> (int) s.getValDouble(), 
                    val -> s.setValDouble(val), 
                    (int) s.getMin(), 
                    (int) s.getMax(), 
                    1, 
                    val -> Component.literal(String.valueOf(val)), 
                    (int) s.getValDouble());
                settingWidgets.add(sl);
                this.addRenderableWidget(sl);
            } else if (s.isCombo()) {
                GlassyDropdown<String> dd = new GlassyDropdown<>(0, 0, 150, 20, 
                    s.getOptions(), 
                    s::getValString, 
                    s::setValString, 
                    Component::literal);
                settingWidgets.add(dd);
                this.addRenderableWidget(dd);
            }
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
        
        if (button == 1) {
            int panelWidth = 550;
            int panelHeight = 320;
            int startX = (this.width - panelWidth) / 2;
            int startY = (this.height - panelHeight) / 2;
            
            int listX = startX + 121;
            int listY = startY + 20;
            int listWidth = panelWidth - 121;
            int listHeight = panelHeight - 20 - 40; 
            
            if (mouseX >= listX && mouseX <= listX + listWidth && mouseY >= listY && mouseY <= listY + listHeight) {
                int modY = listY - (int) scrollOffset;
                List<Module> modules = new ArrayList<>();
                String query = searchBox.getValue().toLowerCase();
                List<Module> sourceModules = query.isEmpty() ? ImnotcheatingyouareClient.INSTANCE.moduleManager.getModules(selectedCategory) : ImnotcheatingyouareClient.INSTANCE.moduleManager.modules;
                for(Module m : sourceModules) {
                    if(!m.isHidden() && m.getName().toLowerCase().contains(query)) modules.add(m);
                }
                
                for (Module m : modules) {
                if (mouseY >= modY && mouseY <= modY + 35) {
                    expandedModule = (expandedModule == m) ? null : m;
                    rebuildSettingWidgets();
                    return true;
                }
                modY += 35;
                if (m == expandedModule && activeSettings != null) {
                    modY += activeSettings.size() * 30;
                }
            }
        }
    }
    return super.mouseClicked(event, doubleClick);
}

@Override
public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
    if (verticalAmount != 0) {
        targetScrollOffset -= (float) (verticalAmount * 25);
        if (targetScrollOffset < 0) targetScrollOffset = 0;
        return true;
    }
    return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
}

    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractBackground(context, mouseX, mouseY, delta);
        context.fill(0, 0, this.width, this.height, 0x88000000);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        GlassyTheme.updateColors(RenderUtils.getThemeAccentColor().getRGB());
        
        scrollOffset += (targetScrollOffset - scrollOffset) * 0.3f * delta;
        moduleListSlideX += (0f - moduleListSlideX) * 0.2f * delta;
        moduleListAlpha += (1f - moduleListAlpha) * 0.2f * delta;
        categorySlideX += (0f - categorySlideX) * 0.2f * delta;
        categoryAlpha += (1f - categoryAlpha) * 0.2f * delta;
        
        GuiGraphics graphics = new GuiGraphics(context);
        
        int panelWidth = 550;
        int panelHeight = 320;
        int startX = (this.width - panelWidth) / 2;
        int startY = (this.height - panelHeight) / 2;

        graphics.fill(startX, startY, startX + panelWidth, startY + panelHeight, GlassyTheme.PANEL_BG);
        graphics.renderOutline(startX, startY, panelWidth, panelHeight, GlassyTheme.PANEL_BORDER);

        graphics.fill(startX, startY, startX + panelWidth, startY + 20, GlassyTheme.PANEL_HEADER_BG);
        graphics.drawString(this.font, Component.literal("\u00a7b\u00a7lMarlowww Client \u00a7f| \u00a77Modules"), startX + 10, startY + 6, GlassyTheme.TEXT, false);

        graphics.fill(startX + 120, startY + 20, startX + 121, startY + panelHeight, 0x44FFFFFF);
        
        for (GlassyButton btn : categoryButtons) {
            btn.setX((int)(startX + 10 + categorySlideX));
        }

        float targetY = startY + 30 + (selectedCategory.ordinal() * 28);
        if (animatedCatY == -1) animatedCatY = targetY;
        animatedCatY += (targetY - animatedCatY) * 0.3f * delta;
        graphics.fill((int)(startX + 10 + categorySlideX), (int) animatedCatY, (int)(startX + 12 + categorySlideX), (int) animatedCatY + 24, GlassyTheme.ACCENT);

        int listX = startX + 121 + (int)moduleListSlideX;
        int listY = startY + 20;
        int listWidth = panelWidth - 121;
        int listHeight = panelHeight - 20 - 40;
        
        List<Module> modules = new ArrayList<>();
        String query = searchBox.getValue().toLowerCase();
        List<Module> sourceModules = query.isEmpty() ? ImnotcheatingyouareClient.INSTANCE.moduleManager.getModules(selectedCategory) : ImnotcheatingyouareClient.INSTANCE.moduleManager.modules;
        for(Module m : sourceModules) {
            if(!m.isHidden() && m.getName().toLowerCase().contains(query)) modules.add(m);
        }
        
        int rowHeight = 35;
        int totalHeight = modules.size() * rowHeight;
        
        for (Module m : modules) {
            float currentH = moduleConfigHeights.getOrDefault(m, 0f);
            float targetH = (m == expandedModule && activeSettings != null) ? (activeSettings.size() * 30) : 0f;
            currentH += (targetH - currentH) * 0.3f * delta;
            moduleConfigHeights.put(m, currentH);
            totalHeight += (int)currentH;
        }
        
        if (targetScrollOffset > totalHeight - listHeight && totalHeight > listHeight) {
            targetScrollOffset = totalHeight - listHeight;
        }
        if (totalHeight <= listHeight) {
            targetScrollOffset = 0;
        }

        int modY = listY - (int) scrollOffset;
        
        for (int i = 0; i < modules.size(); i++) {
            Module m = modules.get(i);
            GlassyToggle toggle = moduleToggles.get(m);
            if (toggle == null) continue;
            
            float currentH = moduleConfigHeights.getOrDefault(m, 0f);
            
            boolean visible = (modY + rowHeight + currentH > listY && modY < listY + listHeight);
            
            if (visible) {
                boolean hovered = mouseX >= listX && mouseX <= listX + listWidth && mouseY >= modY && mouseY <= modY + rowHeight;
                int bg = hovered ? 0x22FFFFFF : ((i % 2 == 0) ? 0x08FFFFFF : 0x00000000);
                graphics.fill(listX, Math.max(listY, modY), listX + listWidth, Math.min(listY + listHeight, modY + rowHeight), bg);
                
                if (modY + rowHeight <= listY + listHeight) {
                    graphics.fill(listX, modY + rowHeight - 1, listX + listWidth, modY + rowHeight, 0x11FFFFFF);
                }

                if (modY >= listY && modY < listY + listHeight) {
                    graphics.drawString(this.font, Component.literal(m.getName() + (m == expandedModule ? " \u00a77[-]" : " \u00a77[+]")), listX + 10, modY + 6, GlassyTheme.TEXT, false);
                    String desc = m.getDescription();
                    if (desc.length() > 60) desc = desc.substring(0, 57) + "...";
                    graphics.drawString(this.font, Component.literal(desc), listX + 10, modY + 18, GlassyTheme.TEXT_MUTED, false);
                }
                
                toggle.setX(listX + listWidth - 55);
                toggle.setY(modY + (rowHeight - 20) / 2);
                toggle.visible = (toggle.getY() >= listY && toggle.getY() + 20 <= listY + listHeight);
            } else {
                toggle.visible = false;
                toggle.setY(-100);
            }
            
            modY += rowHeight;
            
            if (currentH > 0.5f) {
                graphics.fill(listX, Math.max(listY, modY), listX + listWidth, Math.min(listY + listHeight, modY + (int)currentH), 0x1A000000);
                
                if (m == expandedModule) {
                    float subY = modY;
                    for (int sIdx = 0; sIdx < activeSettings.size(); sIdx++) {
                        Setting s = activeSettings.get(sIdx);
                        AbstractWidget w = settingWidgets.get(sIdx);
                        
                        if (subY + 30 > listY && subY < listY + listHeight && subY + 30 <= modY + currentH) {
                            if (subY >= listY) {
                                graphics.drawString(this.font, Component.literal(s.getName()), listX + 25, (int)subY + 10, GlassyTheme.TEXT_MUTED, false);
                            }
                            w.setX(listX + listWidth - 165);
                            w.setY((int)subY + 5);
                            w.visible = true;
                            if (s.isCheck()) w.setX(listX + listWidth - 55);
                        } else {
                            w.visible = false;
                            w.setY(-100);
                        }
                        subY += 30;
                    }
                }
                modY += (int)currentH;
            } else if (m == expandedModule) {
                for (AbstractWidget w : settingWidgets) {
                    w.visible = false;
                    w.setY(-100);
                }
            }
        }
        
        for (Map.Entry<Module, GlassyToggle> entry : moduleToggles.entrySet()) {
            if (!modules.contains(entry.getKey())) {
                entry.getValue().visible = false;
                entry.getValue().setY(-100);
            }
        }
        
        graphics.fill(startX + 120, startY + panelHeight - 40, startX + panelWidth, startY + panelHeight - 39, 0x44FFFFFF);

        super.extractRenderState(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
