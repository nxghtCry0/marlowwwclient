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
    private static final int PANEL_WIDTH = 660;
    private static final int PANEL_HEIGHT = 380;

    private Category selectedCategory = Category.Combat;
    private Module expandedModule = null;
    
    private final Map<Module, GlassyToggle> moduleToggles = new HashMap<>();
    private final List<GlassyButton> categoryButtons = new ArrayList<>();
    private final List<AbstractWidget> settingWidgets = new ArrayList<>();
    private final List<Setting> activeSettings = new ArrayList<>();
    private final Map<Module, Float> moduleConfigHeights = new HashMap<>();
    private final List<Module> filteredModules = new ArrayList<>();
    private String lastSearchQuery = null;
    private Category lastSelectedCategory = null;
    
    private EditBox searchBox;
    
    private float scrollOffset = 0;
    private float targetScrollOffset = 0;
    private float animatedCatY = -1;
    
    private float moduleListSlideX = 50f;
    private float moduleListAlpha = 0f;
    private float categorySlideX = -50f;
    private float categoryAlpha = 0f;

    private long lastRenderTime = 0;
    private boolean draggingScrollbar = false;

    public NewClickgui() {
        super(Component.literal("Marlowww Client"));
    }

    public float getScaleFactor() {
        float scale = 1.0f;
        if (this.width < 680f) {
            scale = Math.min(scale, this.width / 680f);
        }
        if (this.height < 400f) {
            scale = Math.min(scale, this.height / 400f);
        }
        return scale;
    }

    @Override
    protected void init() {
        this.clearWidgets();
        categoryButtons.clear();
        moduleToggles.clear();
        settingWidgets.clear();
        activeSettings.clear();

        float scale = getScaleFactor();
        int virtualWidth = (int) (this.width / scale);
        int virtualHeight = (int) (this.height / scale);

        int panelWidth = PANEL_WIDTH;
        int panelHeight = PANEL_HEIGHT;
        int startX = (virtualWidth - panelWidth) / 2;
        int startY = (virtualHeight - panelHeight) / 2;

        int catX = startX + 10;
        int catY = startY + 30;

        for (Category category : Category.values()) {
            GlassyButton btn = new GlassyButton(catX, catY, 110, 24, Component.literal(category.name()), () -> {
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

        try (java.io.PrintWriter writer = new java.io.PrintWriter(new java.io.FileWriter("C:\\Users\\teeja\\.gemini\\antigravity\\worktrees\\im not cheating you are\\optimize-rendering-performance-system\\dev.txt", false))) {
            for (Module m : ImnotcheatingyouareClient.INSTANCE.moduleManager.modules) {
                writer.println("Module: " + m.getName() + " | Category: " + m.getCategory() + " | Hidden: " + m.isHidden());
            }
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }

        for (Module m : ImnotcheatingyouareClient.INSTANCE.moduleManager.modules) {
            if (m.isHidden()) continue;
            GlassyToggle toggle = new GlassyToggle(0, 0, 45, 20, m::isToggled, val -> m.toggle());
            moduleToggles.put(m, toggle);
            this.addRenderableWidget(toggle);
        }
        
        searchBox = new EditBox(this.font, startX + 140, startY + panelHeight - 32, panelWidth - 150, 20, Component.literal("Search Modules"));
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
                    s::getValDouble, 
                    s::setValDouble, 
                    s.getMin(), 
                    s.getMax(), 
                    s.onlyInt(), 
                    val -> Component.literal(s.onlyInt() ? String.valueOf((int) val) : String.valueOf(val)), 
                    s.getValDouble());
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
        
        if (button == 0) {
            for (AbstractWidget w : settingWidgets) {
                if (w instanceof GlassyDropdown<?> dd && dd.isMenuOpen()) {
                    if (dd.mouseClicked(event, doubleClick)) {
                        return true;
                    }
                }
            }
        }
        
        int panelWidth = PANEL_WIDTH;
        int panelHeight = PANEL_HEIGHT;
        int startX = (this.width - panelWidth) / 2;
        int startY = (this.height - panelHeight) / 2;
        
        int listX = startX + 131;
        int listY = startY + 20;
        int listWidth = panelWidth - 131;
        int listHeight = panelHeight - 20 - 45; 
        
        if (button == 0) {
            // Scrollbar track click check:
            int sbX = startX + panelWidth - 12;
            if (mouseX >= sbX && mouseX <= startX + panelWidth && mouseY >= listY && mouseY <= listY + listHeight) {
                // Calculate total height of modules
                int rowHeight = 35;
                int totalHeight = filteredModules.size() * rowHeight;
                for (int i = 0; i < filteredModules.size(); i++) {
                    Module m = filteredModules.get(i);
                    float currentH = moduleConfigHeights.getOrDefault(m, 0f);
                    totalHeight += (int)currentH;
                }
                
                if (totalHeight > listHeight) {
                    this.draggingScrollbar = true;
                    float pct = (float) ((mouseY - listY) / (float) listHeight);
                    pct = Math.max(0f, Math.min(1f, pct));
                    targetScrollOffset = pct * (totalHeight - listHeight);
                }
                return true;
            }
        }
        
        if (button == 1) {
            if (mouseX >= listX && mouseX <= listX + listWidth && mouseY >= listY && mouseY <= listY + listHeight) {
                int modY = listY - (int) scrollOffset;
                for (int i = 0; i < filteredModules.size(); i++) {
                    Module m = filteredModules.get(i);
                    if (mouseY >= modY && mouseY <= modY + 35) {
                        List<Setting> mSets = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingsByMod(m);
                        if (mSets != null && !mSets.isEmpty()) {
                            expandedModule = (expandedModule == m) ? null : m;
                            rebuildSettingWidgets();
                        }
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
    public boolean mouseReleased(MouseButtonEvent event) {
        this.draggingScrollbar = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (this.draggingScrollbar) {
            int panelWidth = PANEL_WIDTH;
            int panelHeight = PANEL_HEIGHT;
            int startX = (this.width - panelWidth) / 2;
            int startY = (this.height - panelHeight) / 2;
            int listY = startY + 20;
            int listHeight = panelHeight - 20 - 45;
            
            // Calculate total height of modules
            int rowHeight = 35;
            int totalHeight = filteredModules.size() * rowHeight;
            for (int i = 0; i < filteredModules.size(); i++) {
                Module m = filteredModules.get(i);
                float currentH = moduleConfigHeights.getOrDefault(m, 0f);
                totalHeight += (int)currentH;
            }
            
            if (totalHeight > listHeight) {
                double relativeY = event.y() - listY;
                float pct = (float) (relativeY / listHeight);
                pct = Math.max(0f, Math.min(1f, pct));
                targetScrollOffset = pct * (totalHeight - listHeight);
            }
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        float scale = getScaleFactor();
        if (verticalAmount != 0) {
            targetScrollOffset -= (float) (verticalAmount * 25);
            if (targetScrollOffset < 0) targetScrollOffset = 0;
            return true;
        }
        return super.mouseScrolled(mouseX / scale, mouseY / scale, horizontalAmount, verticalAmount);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractBackground(context, mouseX, mouseY, delta);
        context.fill(0, 0, this.width, this.height, 0x88000000);
    }

    private float lerpDecay(float current, float target, float speed, float timeDelta) {
        float factor = 1f - (float)Math.exp(-speed * timeDelta);
        return current + (target - current) * factor;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        GlassyTheme.updateColors(RenderUtils.getThemeAccentColor().getRGB());
        
        long now = System.currentTimeMillis();
        if (lastRenderTime == 0) lastRenderTime = now;
        float timeDelta = Math.min(0.1f, (now - lastRenderTime) / 1000f);
        lastRenderTime = now;
        
        scrollOffset = lerpDecay(scrollOffset, targetScrollOffset, 12f, timeDelta);
        moduleListSlideX = lerpDecay(moduleListSlideX, 0f, 10f, timeDelta);
        moduleListAlpha = lerpDecay(moduleListAlpha, 1f, 10f, timeDelta);
        categorySlideX = lerpDecay(categorySlideX, 0f, 10f, timeDelta);
        categoryAlpha = lerpDecay(categoryAlpha, 1f, 10f, timeDelta);
        
        float scale = getScaleFactor();
        int scaledMouseX = (int) (mouseX / scale);
        int scaledMouseY = (int) (mouseY / scale);
        
        context.pose().pushMatrix();
        context.pose().scale(scale, scale);
        
        GuiGraphics graphics = new GuiGraphics(context);
        
        int panelWidth = PANEL_WIDTH;
        int panelHeight = PANEL_HEIGHT;
        int virtualWidth = (int) (this.width / scale);
        int virtualHeight = (int) (this.height / scale);
        int startX = (virtualWidth - panelWidth) / 2;
        int startY = (virtualHeight - panelHeight) / 2;

        graphics.fill(startX, startY, startX + panelWidth, startY + panelHeight, GlassyTheme.PANEL_BG);
        graphics.renderOutline(startX, startY, panelWidth, panelHeight, GlassyTheme.PANEL_BORDER);

        graphics.fill(startX, startY, startX + panelWidth, startY + 20, GlassyTheme.PANEL_HEADER_BG);
        graphics.drawString(this.font, Component.literal("\u00a7b\u00a7lMarlowww Client \u00a7f| \u00a77Modules"), startX + 10, startY + 6, GlassyTheme.TEXT, false);

        graphics.fill(startX + 130, startY + 20, startX + 131, startY + panelHeight, 0x44FFFFFF);
        
        for (GlassyButton btn : categoryButtons) {
            btn.setX((int)(startX + 10 + categorySlideX));
        }

        float targetY = startY + 30 + (selectedCategory.ordinal() * 28);
        if (animatedCatY == -1) animatedCatY = targetY;
        animatedCatY = lerpDecay(animatedCatY, targetY, 15f, timeDelta);
        graphics.fill((int)(startX + 10 + categorySlideX), (int) animatedCatY, (int)(startX + 12 + categorySlideX), (int) animatedCatY + 24, GlassyTheme.ACCENT);

        int listX = startX + 131 + (int)moduleListSlideX;
        int listY = startY + 20;
        int listWidth = panelWidth - 131;
        int listHeight = panelHeight - 20 - 45;
        
        String query = searchBox.getValue();
        if (lastSelectedCategory != selectedCategory || !query.equals(lastSearchQuery)) {
            lastSelectedCategory = selectedCategory;
            lastSearchQuery = query;
            filteredModules.clear();
            String queryLower = query.toLowerCase();
            String queryLowerClean = queryLower.replace(" ", "");
            List<Module> sourceModules = queryLowerClean.isEmpty() ? ImnotcheatingyouareClient.INSTANCE.moduleManager.getModules(selectedCategory) : ImnotcheatingyouareClient.INSTANCE.moduleManager.modules;
            for (Module m : sourceModules) {
                if (!m.isHidden() && m.getName().toLowerCase().replace(" ", "").contains(queryLowerClean)) {
                    filteredModules.add(m);
                }
            }
            
            // Hide all toggles that are not in the filtered list
            for (Map.Entry<Module, GlassyToggle> entry : moduleToggles.entrySet()) {
                if (!filteredModules.contains(entry.getKey())) {
                    entry.getValue().visible = false;
                    entry.getValue().setY(-100);
                }
            }
        }
        
        int rowHeight = 35;
        int totalHeight = filteredModules.size() * rowHeight;
        
        for (int i = 0; i < filteredModules.size(); i++) {
            Module m = filteredModules.get(i);
            float currentH = moduleConfigHeights.getOrDefault(m, 0f);
            float targetH = (m == expandedModule && activeSettings != null) ? (activeSettings.size() * 30) : 0f;
            if (Math.abs(targetH - currentH) > 0.01f) {
                currentH = lerpDecay(currentH, targetH, 12f, timeDelta);
                if (Math.abs(targetH - currentH) <= 0.01f) {
                    currentH = targetH;
                }
                moduleConfigHeights.put(m, currentH);
            }
            totalHeight += (int)currentH;
        }
        
        if (targetScrollOffset > totalHeight - listHeight && totalHeight > listHeight) {
            targetScrollOffset = totalHeight - listHeight;
        }
        if (totalHeight <= listHeight) {
            targetScrollOffset = 0;
        }

        int modY = listY - (int) scrollOffset;
        
        for (int i = 0; i < filteredModules.size(); i++) {
            Module m = filteredModules.get(i);
            GlassyToggle toggle = moduleToggles.get(m);
            if (toggle == null) continue;
            
            float currentH = moduleConfigHeights.getOrDefault(m, 0f);
            
            boolean visible = (modY + rowHeight + currentH > listY && modY < listY + listHeight);
            
            if (visible) {
                boolean hovered = scaledMouseX >= listX && scaledMouseX <= listX + listWidth && scaledMouseY >= modY && scaledMouseY <= modY + rowHeight;
                int bg = hovered ? 0x22FFFFFF : ((i % 2 == 0) ? 0x08FFFFFF : 0x00000000);
                graphics.fill(listX, Math.max(listY, modY), listX + listWidth, Math.min(listY + listHeight, modY + rowHeight), bg);
                
                if (modY + rowHeight <= listY + listHeight) {
                    graphics.fill(listX, modY + rowHeight - 1, listX + listWidth, modY + rowHeight, 0x11FFFFFF);
                }

                if (modY >= listY && modY < listY + listHeight) {
                    List<Setting> mSets = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingsByMod(m);
                    String suffix = (mSets != null && !mSets.isEmpty()) ? (m == expandedModule ? " \u00a77[-]" : " \u00a77[+]") : "";
                    graphics.drawString(this.font, Component.literal(m.getName() + suffix), listX + 10, modY + 6, GlassyTheme.TEXT, false);
                    String desc = m.getDescription();
                    if (desc.length() > 80) desc = desc.substring(0, 77) + "...";
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
        
        // Draw Scrollbar
        if (totalHeight > listHeight) {
            int sbX = startX + panelWidth - 8;
            int sbY = listY + 2;
            int sbWidth = 4;
            int sbHeight = listHeight - 4;
            
            graphics.fill(sbX, sbY, sbX + sbWidth, sbY + sbHeight, 0x15FFFFFF);
            
            int thumbHeight = (int) ((float) listHeight / totalHeight * sbHeight);
            thumbHeight = Math.max(10, thumbHeight);
            
            float maxScroll = totalHeight - listHeight;
            float scrollPct = scrollOffset / maxScroll;
            int thumbY = sbY + (int) (scrollPct * (sbHeight - thumbHeight));
            
            boolean sbHovered = scaledMouseX >= sbX - 2 && scaledMouseX <= sbX + sbWidth + 2 && scaledMouseY >= sbY && scaledMouseY <= sbY + sbHeight;
            int thumbColor = (draggingScrollbar || sbHovered) ? GlassyTheme.ACCENT : 0x44FFFFFF;
            
            graphics.fill(sbX, thumbY, sbX + sbWidth, thumbY + thumbHeight, thumbColor);
        }

        graphics.fill(startX + 130, startY + panelHeight - 40, startX + panelWidth, startY + panelHeight - 39, 0x44FFFFFF);

        super.extractRenderState(context, scaledMouseX, scaledMouseY, delta);
        
        if (expandedModule != null) {
            for (AbstractWidget w : settingWidgets) {
                if (w instanceof GlassyDropdown<?> dropdown) {
                    dropdown.renderOverlay(graphics, scaledMouseX, scaledMouseY);
                }
            }
        }
        
        context.pose().popMatrix();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
