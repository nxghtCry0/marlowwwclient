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
import net.minecraft.client.input.KeyEvent;
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
    private Module bindingModule = null;
    
    private final Map<Module, GlassyToggle> moduleToggles = new HashMap<>();
    private final List<GlassyButton> categoryButtons = new ArrayList<>();
    private final List<AbstractWidget> settingWidgets = new ArrayList<>();
    private final List<Setting> activeSettings = new ArrayList<>();
    private final Map<Module, Float> moduleConfigHeights = new HashMap<>();
    private final List<Module> filteredModules = new ArrayList<>();
    private String lastSearchQuery = null;
    private Category lastSelectedCategory = null;
    
    private EditBox searchBox;
    private EditBox macroNameBox;
    private EditBox filterPlayerBox;
    private EditBox filterEntityBox;
    
    private float macroListScroll = 0;
    private float targetMacroListScroll = 0;
    private float actionListScroll = 0;
    private float targetActionListScroll = 0;
    private float playerListScroll = 0;
    private float targetPlayerListScroll = 0;
    private float entityListScroll = 0;
    private float targetEntityListScroll = 0;
    private float categoryScroll = 0;
    private float targetCategoryScroll = 0;
    private com.eclipseware.imnotcheatingyouare.client.macro.Macro bindingMacro = null;
    
    private float scrollOffset = 0;
    private float targetScrollOffset = 0;
    private float animatedCatY = -1;
    
    private float moduleListSlideX = 50f;
    private float moduleListAlpha = 0f;
    private float categorySlideX = -50f;
    private float categoryAlpha = 0f;

    private long lastRenderTime = 0;
    private boolean draggingScrollbar = false;
    private boolean draggingCategoryScrollbar = false;

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

        macroNameBox = new EditBox(this.font, startX + 131 + 225, startY + 30, 150, 18, Component.literal("Macro Name"));
        macroNameBox.setMaxLength(20);
        macroNameBox.setValue(com.eclipseware.imnotcheatingyouare.client.macro.MacroManager.getActiveMacro() != null ? com.eclipseware.imnotcheatingyouare.client.macro.MacroManager.getActiveMacro().getName() : "Default");
        this.addRenderableWidget(macroNameBox);

        filterPlayerBox = new EditBox(this.font, startX + 145, startY + 45, 150, 18, Component.literal("Player Name"));
        filterPlayerBox.setMaxLength(16);
        this.addRenderableWidget(filterPlayerBox);

        filterEntityBox = new EditBox(this.font, startX + 360, startY + 45, 285, 18, Component.literal("Search Entity"));
        filterEntityBox.setMaxLength(50);
        this.addRenderableWidget(filterEntityBox);
        
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
        float scale = getScaleFactor();
        double mouseX = event.x() / scale;
        double mouseY = event.y() / scale;
        int button = event.button();
        
        if (bindingMacro != null) {
            if (button != 0 && button != 1) {
                bindingMacro.setKeybind(button);
                com.eclipseware.imnotcheatingyouare.client.macro.MacroManager.save();
                bindingMacro = null;
                com.eclipseware.imnotcheatingyouare.client.clickgui.Clickgui.playSound();
                return true;
            }
        }
        
        if (selectedCategory == Category.Filters) {
            int panelWidth = PANEL_WIDTH;
            int panelHeight = PANEL_HEIGHT;
            int virtualWidth = (int) (this.width / scale);
            int virtualHeight = (int) (this.height / scale);
            int startX = (virtualWidth - panelWidth) / 2;
            int startY = (virtualHeight - panelHeight) / 2;

            int leftX = startX + 145;
            int leftY = startY + 70;
            int leftW = 200;
            int leftH = 270;

            int rightX = startX + 360;
            int rightY = startY + 70;
            int rightW = 285;
            int rightH = 270;

            if (button == 0) {
                int addBtnX = leftX + 155;
                int addBtnY = startY + 45;
                int addBtnW = 45;
                int addBtnH = 18;
                if (mouseX >= addBtnX && mouseX <= addBtnX + addBtnW && mouseY >= addBtnY && mouseY <= addBtnY + addBtnH) {
                    String name = filterPlayerBox.getValue().trim();
                    if (!name.isEmpty()) {
                        com.eclipseware.imnotcheatingyouare.client.utils.TargetFilterManager.addFilteredPlayer(name);
                        filterPlayerBox.setValue("");
                        com.eclipseware.imnotcheatingyouare.client.clickgui.Clickgui.playSound();
                    }
                    return true;
                }

                if (mouseX >= leftX && mouseX <= leftX + leftW && mouseY >= leftY && mouseY <= leftY + leftH) {
                    List<String> players = new ArrayList<>(com.eclipseware.imnotcheatingyouare.client.utils.TargetFilterManager.getFilteredPlayers());
                    players.sort(String.CASE_INSENSITIVE_ORDER);
                    for (int i = 0; i < players.size(); i++) {
                        String name = players.get(i);
                        int itemY = leftY + 5 + i * 22 - (int) playerListScroll;
                        int delX = leftX + leftW - 20;
                        int delY = itemY + 4;
                        if (mouseX >= delX && mouseX <= delX + 12 && mouseY >= delY && mouseY <= delY + 12 && itemY >= leftY && itemY + 20 <= leftY + leftH) {
                            com.eclipseware.imnotcheatingyouare.client.utils.TargetFilterManager.removeFilteredPlayer(name);
                            com.eclipseware.imnotcheatingyouare.client.clickgui.Clickgui.playSound();
                            return true;
                        }
                    }
                }

                if (mouseX >= rightX && mouseX <= rightX + rightW && mouseY >= rightY && mouseY <= rightY + rightH) {
                    List<net.minecraft.world.entity.EntityType<?>> sortedEntities = new ArrayList<>();
                    String query = filterEntityBox.getValue().toLowerCase().replace(" ", "");
                    for (net.minecraft.world.entity.EntityType<?> type : net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE) {
                        String name = type.getDescription().getString();
                        String id = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(type).toString();
                        if (name.toLowerCase().replace(" ", "").contains(query) || id.toLowerCase().replace(" ", "").contains(query)) {
                            sortedEntities.add(type);
                        }
                    }
                    sortedEntities.sort((t1, t2) -> t1.getDescription().getString().compareToIgnoreCase(t2.getDescription().getString()));

                    for (int i = 0; i < sortedEntities.size(); i++) {
                        net.minecraft.world.entity.EntityType<?> type = sortedEntities.get(i);
                        String id = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(type).toString();
                        int itemY = rightY + 5 + i * 22 - (int) entityListScroll;

                        if (mouseX >= rightX + 4 && mouseX <= rightX + rightW - 4 && mouseY >= itemY && mouseY <= itemY + 18 && itemY >= rightY && itemY + 18 <= rightY + rightH) {
                            com.eclipseware.imnotcheatingyouare.client.utils.TargetFilterManager.toggleFilteredEntityType(id);
                            com.eclipseware.imnotcheatingyouare.client.clickgui.Clickgui.playSound();
                            return true;
                        }
                    }
                }
            }

            if (super.mouseClicked(event, doubleClick)) {
                return true;
            }
            return true;
        }

        if (selectedCategory == Category.Macros) {
            int panelWidth = PANEL_WIDTH;
            int panelHeight = PANEL_HEIGHT;
            int virtualWidth = (int) (this.width / scale);
            int virtualHeight = (int) (this.height / scale);
            int startX = (virtualWidth - panelWidth) / 2;
            int startY = (virtualHeight - panelHeight) / 2;

            int listX = startX + 145;
            int listY = startY + 30;
            int listW = 180;
            int listH = 320;
            int editX = startX + 345;

            if (button == 0) {
                // [+ Create New Macro] click
                if (mouseX >= listX && mouseX <= listX + listW && mouseY >= startY + PANEL_HEIGHT - 35 && mouseY <= startY + PANEL_HEIGHT - 15) {
                    com.eclipseware.imnotcheatingyouare.client.macro.Macro m = com.eclipseware.imnotcheatingyouare.client.macro.MacroManager.createNewMacro("Macro " + (com.eclipseware.imnotcheatingyouare.client.macro.MacroManager.getMacros().size() + 1));
                    com.eclipseware.imnotcheatingyouare.client.macro.MacroManager.setActiveMacro(m);
                    macroNameBox.setValue(m.getName());
                    Clickgui.playSound();
                    return true;
                }

                // Macros List item selection
                List<com.eclipseware.imnotcheatingyouare.client.macro.Macro> macrosList = com.eclipseware.imnotcheatingyouare.client.macro.MacroManager.getMacros();
                int listAreaY = listY + 15;
                int listAreaH = 260;
                if (mouseX >= listX && mouseX <= listX + listW && mouseY >= listAreaY && mouseY <= listAreaY + listAreaH) {
                    for (int i = 0; i < macrosList.size(); i++) {
                        com.eclipseware.imnotcheatingyouare.client.macro.Macro m = macrosList.get(i);
                        int itemY = listAreaY + 5 + i * 32 - (int)macroListScroll;
                        
                        // Check click on delete button [X]
                        if (mouseX >= listX + listW - 20 && mouseX <= listX + listW - 8 && mouseY >= itemY + 8 && mouseY <= itemY + 20) {
                            com.eclipseware.imnotcheatingyouare.client.macro.MacroManager.deleteMacro(m);
                            com.eclipseware.imnotcheatingyouare.client.macro.Macro active = com.eclipseware.imnotcheatingyouare.client.macro.MacroManager.getActiveMacro();
                            macroNameBox.setValue(active != null ? active.getName() : "Default");
                            Clickgui.playSound();
                            return true;
                        }
                        
                        // Check click on item card
                        if (mouseX >= listX + 4 && mouseX <= listX + listW - 4 && mouseY >= itemY && mouseY <= itemY + 28) {
                            com.eclipseware.imnotcheatingyouare.client.macro.MacroManager.setActiveMacro(m);
                            macroNameBox.setValue(m.getName());
                            Clickgui.playSound();
                            return true;
                        }
                    }
                }

                // Active macro editor clicks
                com.eclipseware.imnotcheatingyouare.client.macro.Macro active = com.eclipseware.imnotcheatingyouare.client.macro.MacroManager.getActiveMacro();
                if (active != null) {
                    // Bind button
                    if (mouseX >= editX + 160 && mouseX <= editX + 260 && mouseY >= startY + 30 && mouseY <= startY + 48) {
                        bindingMacro = active;
                        Clickgui.playSound();
                        return true;
                    }

                    // Hold Mode toggle
                    if (mouseX >= editX && mouseX <= editX + 100 && mouseY >= startY + 56 && mouseY <= startY + 71) {
                        active.setHoldMode(!active.isHoldMode());
                        com.eclipseware.imnotcheatingyouare.client.macro.MacroManager.save();
                        Clickgui.playSound();
                        return true;
                    }

                    // Enabled toggle
                    if (mouseX >= editX + 110 && mouseX <= editX + 210 && mouseY >= startY + 56 && mouseY <= startY + 71) {
                        active.setEnabled(!active.isEnabled());
                        com.eclipseware.imnotcheatingyouare.client.macro.MacroManager.save();
                        Clickgui.playSound();
                        return true;
                    }

                    // Record button
                    if (mouseX >= editX && mouseX <= editX + 80 && mouseY >= startY + 78 && mouseY <= startY + 98) {
                        if (com.eclipseware.imnotcheatingyouare.client.macro.MacroManager.isRecording()) {
                            com.eclipseware.imnotcheatingyouare.client.macro.MacroManager.stopRecord();
                        } else {
                            com.eclipseware.imnotcheatingyouare.client.macro.MacroManager.startRecord(active);
                        }
                        Clickgui.playSound();
                        return true;
                    }

                    // Play button
                    if (mouseX >= editX + 85 && mouseX <= editX + 165 && mouseY >= startY + 78 && mouseY <= startY + 98) {
                        if (com.eclipseware.imnotcheatingyouare.client.macro.MacroManager.isPlaying()) {
                            com.eclipseware.imnotcheatingyouare.client.macro.MacroManager.stopPlay();
                        } else {
                            com.eclipseware.imnotcheatingyouare.client.macro.MacroManager.startPlay(active);
                        }
                        Clickgui.playSound();
                        return true;
                    }

                    // Clear button
                    if (mouseX >= editX + 170 && mouseX <= editX + 250 && mouseY >= startY + 78 && mouseY <= startY + 98) {
                        active.clear();
                        com.eclipseware.imnotcheatingyouare.client.macro.MacroManager.save();
                        Clickgui.playSound();
                        return true;
                    }

                    // Export Macro button
                    if (mouseX >= editX && mouseX <= editX + 145 && mouseY >= startY + PANEL_HEIGHT - 35 && mouseY <= startY + PANEL_HEIGHT - 15) {
                        com.eclipseware.imnotcheatingyouare.client.macro.MacroManager.exportToClipboard(active);
                        Clickgui.playSound();
                        return true;
                    }

                    // Import Macro button
                    if (mouseX >= editX + 155 && mouseX <= editX + 300 && mouseY >= startY + PANEL_HEIGHT - 35 && mouseY <= startY + PANEL_HEIGHT - 15) {
                        com.eclipseware.imnotcheatingyouare.client.macro.MacroManager.importFromClipboard();
                        com.eclipseware.imnotcheatingyouare.client.macro.Macro activeNew = com.eclipseware.imnotcheatingyouare.client.macro.MacroManager.getActiveMacro();
                        macroNameBox.setValue(activeNew != null ? activeNew.getName() : "Default");
                        Clickgui.playSound();
                        return true;
                    }
                }
            }
            if (super.mouseClicked(event, doubleClick)) {
                return true;
            }
            return true;
        }

        if (bindingModule != null) {
            if (button != 0 && button != 1) {
                bindingModule.setKeyBind(button);
                try {
                    com.eclipseware.imnotcheatingyouare.client.setting.ConfigManager.save();
                } catch (Exception e) {}
                bindingModule = null;
                return true;
            }
        }
        
        int panelWidth = PANEL_WIDTH;
        int panelHeight = PANEL_HEIGHT;
        int startX = ((int)(this.width / scale) - panelWidth) / 2;
        int startY = ((int)(this.height / scale) - panelHeight) / 2;
        
        int listX = startX + 131;
        int listY = startY + 20;
        int listWidth = panelWidth - 131;
        int listHeight = panelHeight - 20 - 45;
        
        if (button == 0) {
            if (mouseX >= listX && mouseX <= listX + listWidth && mouseY >= listY && mouseY <= listY + listHeight) {
                int modY = listY - (int) scrollOffset;
                int rowHeight = 35;
                for (int i = 0; i < filteredModules.size(); i++) {
                    Module m = filteredModules.get(i);
                    float currentH = moduleConfigHeights.getOrDefault(m, 0f);
                    
                    int bindX = listX + listWidth - 110;
                    int bindY = modY + (rowHeight - 16) / 2;
                    if (mouseX >= bindX && mouseX <= bindX + 50 && mouseY >= bindY && mouseY <= bindY + 16) {
                        bindingModule = (bindingModule == m) ? null : m;
                        com.eclipseware.imnotcheatingyouare.client.clickgui.Clickgui.playSound();
                        return true;
                    }
                    
                    modY += rowHeight;
                    if (m == expandedModule && activeSettings != null) {
                        modY += (int) currentH;
                    }
                }
            }
            bindingModule = null;
        } else if (button == 1) {
            bindingModule = null;
        }
        
        if (button == 0) {
            for (AbstractWidget w : settingWidgets) {
                if (w instanceof GlassyDropdown<?> dd && dd.isMenuOpen()) {
                    if (dd.mouseClicked(event, doubleClick)) {
                        return true;
                    }
                }
            }
        } 
        
        if (button == 0) {
            // Category scrollbar track click check:
            int catContentH = Category.values().length * 28 + 10;
            int catSidebarH = panelHeight - 40;
            if (catContentH > catSidebarH) {
                int catSbX = startX + 120;
                int catSbY = startY + 30;
                if (mouseX >= catSbX && mouseX <= startX + 128 && mouseY >= catSbY && mouseY <= catSbY + catSidebarH) {
                    this.draggingCategoryScrollbar = true;
                    float pct = (float) ((mouseY - catSbY) / (float) catSidebarH);
                    pct = Math.max(0f, Math.min(1f, pct));
                    targetCategoryScroll = pct * (catContentH - catSidebarH);
                    return true;
                }
            }
        }
        
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
        this.draggingCategoryScrollbar = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (this.draggingCategoryScrollbar) {
            float scale = getScaleFactor();
            int panelHeight = PANEL_HEIGHT;
            int startY = ((int)(this.height / scale) - panelHeight) / 2;
            int catSbY = startY + 30;
            int catSidebarH = panelHeight - 40;
            int catContentH = Category.values().length * 28 + 10;
            
            if (catContentH > catSidebarH) {
                double relativeY = (event.y() / scale) - catSbY;
                float pct = (float) (relativeY / catSidebarH);
                pct = Math.max(0f, Math.min(1f, pct));
                targetCategoryScroll = pct * (catContentH - catSidebarH);
            }
            return true;
        }
        
        if (this.draggingScrollbar) {
            float scale = getScaleFactor();
            int panelWidth = PANEL_WIDTH;
            int panelHeight = PANEL_HEIGHT;
            int startX = ((int)(this.width / scale) - panelWidth) / 2;
            int startY = ((int)(this.height / scale) - panelHeight) / 2;
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
                double relativeY = (event.y() / scale) - listY;
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
        double sMouseX = mouseX / scale;
        int panelWidth = PANEL_WIDTH;
        int virtualWidth = (int) (this.width / scale);
        int startX = (virtualWidth - panelWidth) / 2;

        if (sMouseX < startX + 130) {
            int contentH = Category.values().length * 28 + 10;
            int maxScroll = Math.max(0, contentH - (PANEL_HEIGHT - 40));
            targetCategoryScroll -= (float) (verticalAmount * 20);
            if (targetCategoryScroll < 0) targetCategoryScroll = 0;
            if (targetCategoryScroll > maxScroll) targetCategoryScroll = maxScroll;
            return true;
        }

        if (selectedCategory == Category.Filters) {
            int leftX = startX + 145;

            if (sMouseX < leftX + 210) {
                List<String> players = new ArrayList<>(com.eclipseware.imnotcheatingyouare.client.utils.TargetFilterManager.getFilteredPlayers());
                int contentH = players.size() * 22 + 10;
                int maxScroll = Math.max(0, contentH - 270);
                targetPlayerListScroll -= (float) (verticalAmount * 20);
                if (targetPlayerListScroll < 0) targetPlayerListScroll = 0;
                if (targetPlayerListScroll > maxScroll) targetPlayerListScroll = maxScroll;
            } else {
                List<net.minecraft.world.entity.EntityType<?>> sortedEntities = new ArrayList<>();
                String query = filterEntityBox.getValue().toLowerCase().replace(" ", "");
                for (net.minecraft.world.entity.EntityType<?> type : net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE) {
                    String name = type.getDescription().getString();
                    String id = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(type).toString();
                    if (name.toLowerCase().replace(" ", "").contains(query) || id.toLowerCase().replace(" ", "").contains(query)) {
                        sortedEntities.add(type);
                    }
                }
                int contentH = sortedEntities.size() * 22 + 10;
                int maxScroll = Math.max(0, contentH - 270);
                targetEntityListScroll -= (float) (verticalAmount * 20);
                if (targetEntityListScroll < 0) targetEntityListScroll = 0;
                if (targetEntityListScroll > maxScroll) targetEntityListScroll = maxScroll;
            }
            return true;
        }

        if (selectedCategory == Category.Macros) {
            int listX = startX + 145;
            
            if (sMouseX < listX + 190) {
                // Scroll macros list
                List<com.eclipseware.imnotcheatingyouare.client.macro.Macro> macros = com.eclipseware.imnotcheatingyouare.client.macro.MacroManager.getMacros();
                int contentH = macros.size() * 32 + 10;
                int maxScroll = Math.max(0, contentH - 260);
                targetMacroListScroll -= (float) (verticalAmount * 20);
                if (targetMacroListScroll < 0) targetMacroListScroll = 0;
                if (targetMacroListScroll > maxScroll) targetMacroListScroll = maxScroll;
            } else {
                // Scroll actions list
                com.eclipseware.imnotcheatingyouare.client.macro.Macro active = com.eclipseware.imnotcheatingyouare.client.macro.MacroManager.getActiveMacro();
                if (active != null) {
                    int contentH = active.getActions().size() * 15 + 10;
                    int maxScroll = Math.max(0, contentH - 195);
                    targetActionListScroll -= (float) (verticalAmount * 20);
                    if (targetActionListScroll < 0) targetActionListScroll = 0;
                    if (targetActionListScroll > maxScroll) targetActionListScroll = maxScroll;
                }
            }
            return true;
        }

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

        categoryScroll = lerpDecay(categoryScroll, targetCategoryScroll, 12f, timeDelta);

        graphics.fill(startX + 130, startY + 20, startX + 131, startY + panelHeight, 0x44FFFFFF);
        
        for (int i = 0; i < categoryButtons.size(); i++) {
            GlassyButton btn = categoryButtons.get(i);
            btn.setX((int)(startX + 10 + categorySlideX));
            int btnY = startY + 30 + (i * 28) - (int)categoryScroll;
            if (btnY >= startY + 25 && btnY + 24 <= startY + panelHeight - 5) {
                btn.visible = true;
                btn.setY(btnY);
            } else {
                btn.visible = false;
                btn.setY(-100);
            }
        }

        int catContentH = Category.values().length * 28 + 10;
        int catSidebarH = panelHeight - 40;
        if (catContentH > catSidebarH) {
            int sbX = startX + 125;
            int sbY = startY + 30;
            int sbW = 2;
            int sbH = catSidebarH;
            graphics.fill(sbX - 1, sbY, sbX + sbW + 1, sbY + sbH, 0x10FFFFFF);
            int thumbH = Math.max(10, (int) ((double) sbH / catContentH * sbH));
            int maxScroll = catContentH - catSidebarH;
            double pct = categoryScroll / maxScroll;
            int thumbY = sbY + (int) (pct * (sbH - thumbH));
            
            boolean sbHovered = scaledMouseX >= sbX - 3 && scaledMouseX <= sbX + sbW + 3 && scaledMouseY >= sbY && scaledMouseY <= sbY + sbH;
            int thumbColor = (draggingCategoryScrollbar || sbHovered) ? GlassyTheme.ACCENT : 0x55FFFFFF;
            graphics.fill(sbX, thumbY, sbX + sbW, thumbY + thumbH, thumbColor);
        }

        float targetY = startY + 30 + (selectedCategory.ordinal() * 28) - categoryScroll;
        if (animatedCatY == -1) animatedCatY = targetY;
        animatedCatY = lerpDecay(animatedCatY, targetY, 15f, timeDelta);
        if (animatedCatY >= startY + 25 && animatedCatY + 24 <= startY + panelHeight - 5) {
            graphics.fill((int)(startX + 10 + categorySlideX), (int) animatedCatY, (int)(startX + 12 + categorySlideX), (int) animatedCatY + 24, GlassyTheme.ACCENT);
        }

        if (selectedCategory == Category.Macros) {
            searchBox.visible = false;
            macroNameBox.visible = true;
            filterPlayerBox.visible = false;
            filterEntityBox.visible = false;
            for (GlassyToggle toggle : moduleToggles.values()) {
                toggle.visible = false;
                toggle.setY(-100);
            }
            
            com.eclipseware.imnotcheatingyouare.client.macro.Macro activeMac = com.eclipseware.imnotcheatingyouare.client.macro.MacroManager.getActiveMacro();
            if (activeMac != null && !macroNameBox.getValue().isEmpty() && !macroNameBox.getValue().equals(activeMac.getName())) {
                activeMac.setName(macroNameBox.getValue());
                com.eclipseware.imnotcheatingyouare.client.macro.MacroManager.save();
            }

            renderMacroUI(graphics, scaledMouseX, scaledMouseY, timeDelta, startX, startY);
            super.extractRenderState(context, scaledMouseX, scaledMouseY, delta);
            context.pose().popMatrix();
            return;
        } else if (selectedCategory == Category.Filters) {
            searchBox.visible = false;
            macroNameBox.visible = false;
            filterPlayerBox.visible = true;
            filterEntityBox.visible = true;
            for (GlassyToggle toggle : moduleToggles.values()) {
                toggle.visible = false;
                toggle.setY(-100);
            }

            renderFiltersUI(graphics, scaledMouseX, scaledMouseY, timeDelta, startX, startY);
            super.extractRenderState(context, scaledMouseX, scaledMouseY, delta);
            context.pose().popMatrix();
            return;
        } else {
            searchBox.visible = true;
            macroNameBox.visible = false;
            filterPlayerBox.visible = false;
            filterEntityBox.visible = false;
        }

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

        // Enable scissor for modules list area
        graphics.extractor().enableScissor(listX, listY, listX + listWidth, listY + listHeight);

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

                List<Setting> mSets = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingsByMod(m);
                String suffix = (mSets != null && !mSets.isEmpty()) ? (m == expandedModule ? " \u00a77[-]" : " \u00a77[+]") : "";
                graphics.drawString(this.font, Component.literal(m.getName() + suffix), listX + 10, modY + 6, GlassyTheme.TEXT, false);
                String desc = m.getDescription();
                if (desc.length() > 80) desc = desc.substring(0, 77) + "...";
                graphics.drawString(this.font, Component.literal(desc), listX + 10, modY + 18, GlassyTheme.TEXT_MUTED, false);
                
                toggle.setX(listX + listWidth - 55);
                toggle.setY(modY + (rowHeight - 20) / 2);
                toggle.visible = (toggle.getY() + 20 > listY && toggle.getY() < listY + listHeight);
                
                int bindX = listX + listWidth - 110;
                int bindY = modY + (rowHeight - 16) / 2;
                boolean bindHovered = scaledMouseX >= bindX && scaledMouseX <= bindX + 50 && scaledMouseY >= bindY && scaledMouseY <= bindY + 16;
                int bindBg = (bindingModule == m) ? GlassyTheme.ACCENT : (bindHovered ? 0x30FFFFFF : 0x15FFFFFF);
                graphics.fill(bindX, bindY, bindX + 50, bindY + 16, bindBg);
                
                String bindText = (bindingModule == m) ? "..." : getKeyName(m.getKeyBind());
                int textWidth = this.font.width(bindText);
                int textX = bindX + (50 - textWidth) / 2;
                int textY = bindY + (16 - 9) / 2;
                graphics.drawString(this.font, Component.literal(bindText), textX, textY, GlassyTheme.TEXT, false);
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
                        
                        graphics.drawString(this.font, Component.literal(s.getName()), listX + 25, (int)subY + 10, GlassyTheme.TEXT_MUTED, false);
                        w.setX(listX + listWidth - 165);
                        w.setY((int)subY + 5);
                        w.visible = (subY + 20 > listY && subY < listY + listHeight);
                        if (s.isCheck()) w.setX(listX + listWidth - 55);
                        
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
        
        // Draw Scrollbar (with styling improvements)
        if (totalHeight > listHeight) {
            int sbX = startX + panelWidth - 10;
            int sbY = listY + 2;
            int sbWidth = 5;
            int sbHeight = listHeight - 4;
            
            graphics.fill(sbX, sbY, sbX + sbWidth, sbY + sbHeight, 0x10FFFFFF);
            
            int thumbHeight = (int) ((float) listHeight / totalHeight * sbHeight);
            thumbHeight = Math.max(10, thumbHeight);
            
            float maxScroll = totalHeight - listHeight;
            float scrollPct = scrollOffset / maxScroll;
            int thumbY = sbY + (int) (scrollPct * (sbHeight - thumbHeight));
            
            boolean sbHovered = scaledMouseX >= sbX - 2 && scaledMouseX <= sbX + sbWidth + 2 && scaledMouseY >= sbY && scaledMouseY <= sbY + sbHeight;
            int thumbColor = (draggingScrollbar || sbHovered) ? GlassyTheme.ACCENT : 0x55FFFFFF;
            
            graphics.fill(sbX, thumbY, sbX + sbWidth, thumbY + thumbHeight, thumbColor);
        }

        // Temporarily hide non-list widgets during super.extractRenderState so they don't get scissored/clipped
        boolean oldSearchVisible = searchBox.visible;
        boolean oldMacroVisible = macroNameBox.visible;
        boolean oldFilterPlayerVisible = filterPlayerBox.visible;
        boolean oldFilterEntityVisible = filterEntityBox.visible;
        
        List<Boolean> oldCategoryVisibilities = new ArrayList<>();
        for (GlassyButton btn : categoryButtons) {
            oldCategoryVisibilities.add(btn.visible);
            btn.visible = false;
        }
        searchBox.visible = false;
        macroNameBox.visible = false;
        filterPlayerBox.visible = false;
        filterEntityBox.visible = false;

        super.extractRenderState(context, scaledMouseX, scaledMouseY, delta);

        // Disable scissor after rendering list items
        graphics.extractor().disableScissor();

        // Restore visibilities of non-list widgets
        searchBox.visible = oldSearchVisible;
        macroNameBox.visible = oldMacroVisible;
        filterPlayerBox.visible = oldFilterPlayerVisible;
        filterEntityBox.visible = oldFilterEntityVisible;
        for (int i = 0; i < categoryButtons.size(); i++) {
            categoryButtons.get(i).visible = oldCategoryVisibilities.get(i);
        }

        // Render non-list widgets manually outside the scissor
        for (GlassyButton btn : categoryButtons) {
            if (btn.visible) {
                btn.extractRenderState(context, scaledMouseX, scaledMouseY, delta);
            }
        }
        if (searchBox.visible) searchBox.extractRenderState(context, scaledMouseX, scaledMouseY, delta);
        if (macroNameBox.visible) macroNameBox.extractRenderState(context, scaledMouseX, scaledMouseY, delta);
        if (filterPlayerBox.visible) filterPlayerBox.extractRenderState(context, scaledMouseX, scaledMouseY, delta);
        if (filterEntityBox.visible) filterEntityBox.extractRenderState(context, scaledMouseX, scaledMouseY, delta);

        graphics.fill(startX + 130, startY + panelHeight - 40, startX + panelWidth, startY + panelHeight - 39, 0x44FFFFFF);
        
        if (expandedModule != null) {
            for (AbstractWidget w : settingWidgets) {
                if (w instanceof GlassyDropdown<?> dropdown) {
                    dropdown.renderOverlay(graphics, scaledMouseX, scaledMouseY);
                }
            }
        }
        
        context.pose().popMatrix();
    }

    private String getKeyName(int key) {
        if (key == -1) return "NONE";
        
        if (key >= 0 && key <= 7) {
            if (key == 1) return "RMB";
            if (key == 2) return "MMB";
            return "MB" + (key + 1);
        }

        switch (key) {
            case org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT: return "RSHIFT";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT: return "LSHIFT";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_CONTROL: return "RCTRL";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_CONTROL: return "LCTRL";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_ALT: return "RALT";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_ALT: return "LALT";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_TAB: return "TAB";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE: return "SPACE";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER: return "ENTER";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE: return "NONE";
        }

        String str = org.lwjgl.glfw.GLFW.glfwGetKeyName(key, 0);
        if (str == null) return "KEY " + key;
        return str.toUpperCase();
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        if (selectedCategory == Category.Filters && filterPlayerBox.isFocused() && input.input() == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER) {
            String name = filterPlayerBox.getValue().trim();
            if (!name.isEmpty()) {
                com.eclipseware.imnotcheatingyouare.client.utils.TargetFilterManager.addFilteredPlayer(name);
                filterPlayerBox.setValue("");
                com.eclipseware.imnotcheatingyouare.client.clickgui.Clickgui.playSound();
            }
            return true;
        }
        if (bindingMacro != null) {
            int key = input.input();
            int targetKey = key;
            if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_DELETE || key == org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE || key == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
                targetKey = -1;
            }
            bindingMacro.setKeybind(targetKey);
            com.eclipseware.imnotcheatingyouare.client.macro.MacroManager.save();
            bindingMacro = null;
            if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
                return true;
            }
        }
        if (bindingModule != null) {
            int key = input.input();
            int targetKey = key;
            if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_DELETE || key == org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE || key == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
                targetKey = -1;
            }
            bindingModule.setKeyBind(targetKey);
            try {
                com.eclipseware.imnotcheatingyouare.client.setting.ConfigManager.save();
            } catch (Exception e) {}
            bindingModule = null;
            if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
                return true;
            }
        }
        
        Module menuMod = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("Menu");
        int menuKey = menuMod != null ? menuMod.getKeyBind() : org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT;
        if (input.input() == menuKey) {
            boolean anyFocused = (searchBox != null && searchBox.isFocused()) ||
                                 (macroNameBox != null && macroNameBox.isFocused()) ||
                                 (filterPlayerBox != null && filterPlayerBox.isFocused()) ||
                                 (filterEntityBox != null && filterEntityBox.isFocused());
            if (!anyFocused) {
                this.onClose();
                return true;
            }
        }
        
        return super.keyPressed(input);
    }

    @Override
    public void removed() {
        bindingModule = null;
        bindingMacro = null;
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void renderMacroUI(GuiGraphics graphics, int mouseX, int mouseY, float timeDelta, int startX, int startY) {
        int listX = startX + 145;
        int listY = startY + 30;
        int listW = 180;
        int listH = 320;
        
        macroListScroll = lerpDecay(macroListScroll, targetMacroListScroll, 12f, timeDelta);
        actionListScroll = lerpDecay(actionListScroll, targetActionListScroll, 12f, timeDelta);
        
        graphics.drawString(this.font, Component.literal("\u00a7b\u00a7lMacros"), listX, listY, -1, false);
        
        int listAreaY = listY + 15;
        int listAreaH = 260;
        
        graphics.fill(listX, listAreaY, listX + listW, listAreaY + listAreaH, 0x10FFFFFF);
        graphics.renderOutline(listX, listAreaY, listW, listAreaH, 0x22FFFFFF);
        
        List<com.eclipseware.imnotcheatingyouare.client.macro.Macro> macros = com.eclipseware.imnotcheatingyouare.client.macro.MacroManager.getMacros();
        com.eclipseware.imnotcheatingyouare.client.macro.Macro active = com.eclipseware.imnotcheatingyouare.client.macro.MacroManager.getActiveMacro();
        
        graphics.extractor().enableScissor(listX + 2, listAreaY + 2, listX + listW - 2, listAreaY + listAreaH - 2);
        graphics.extractor().pose().pushMatrix();
        graphics.extractor().pose().translate(0f, -macroListScroll);
        
        int r = 155, g = 60, b = 255;
        Module theme = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("Theme");
        if (theme != null) {
            r = (int) ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(theme, "Accent R").getValDouble();
            g = (int) ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(theme, "Accent G").getValDouble();
            b = (int) ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(theme, "Accent B").getValDouble();
        }
        int accent = new java.awt.Color(r, g, b).getRGB();
        
        for (int i = 0; i < macros.size(); i++) {
            com.eclipseware.imnotcheatingyouare.client.macro.Macro m = macros.get(i);
            int itemY = listAreaY + 5 + i * 32;
            boolean isSelected = (m == active);
            boolean hovered = mouseX >= listX + 4 && mouseX <= listX + listW - 4 && mouseY >= itemY - (int)macroListScroll && mouseY <= itemY + 28 - (int)macroListScroll && mouseY >= listAreaY && mouseY <= listAreaY + listAreaH;
            
            graphics.fill(listX + 4, itemY, listX + listW - 4, itemY + 28, isSelected ? (accent & 0x00FFFFFF) | 0x25000000 : (hovered ? 0x25FFFFFF : 0x15FFFFFF));
            graphics.renderOutline(listX + 4, itemY, listW - 8, 28, isSelected ? accent : (hovered ? 0x44FFFFFF : 0x22FFFFFF));
            
            graphics.drawString(this.font, Component.literal(m.getName()), listX + 10, itemY + 6, m.isEnabled() ? -1 : 0xFF8F8F8F, false);
            
            String bindText = m.getKeybind() != -1 && m.getKeybind() != 0 ? getKeyName(m.getKeybind()) : "NONE";
            graphics.drawString(this.font, Component.literal("\u00a77Bind: \u00a7b" + bindText), listX + 10, itemY + 16, 0xFF8F8F8F, false);
            
            boolean delHovered = mouseX >= listX + listW - 20 && mouseX <= listX + listW - 8 && mouseY >= itemY + 8 - (int)macroListScroll && mouseY <= itemY + 20 - (int)macroListScroll && mouseY >= listAreaY && mouseY <= listAreaY + listAreaH;
            graphics.drawString(this.font, Component.literal("X"), listX + listW - 18, itemY + 8, delHovered ? 0xFFFF5555 : 0xFF8F8F8F, false);
        }
        
        graphics.extractor().pose().popMatrix();
        graphics.extractor().disableScissor();
        
        int totalLeftHeight = macros.size() * 32 + 10;
        if (totalLeftHeight > listAreaH) {
            int sbX = listX + listW - 6;
            int sbY = listAreaY + 2;
            int sbW = 3;
            int sbH = listAreaH - 4;
            graphics.fill(sbX, sbY, sbX + sbW, sbY + sbH, 0x15FFFFFF);
            int thumbH = Math.max(15, (int) ((double) sbH / totalLeftHeight * sbH));
            int maxScroll = totalLeftHeight - listAreaH;
            double pct = macroListScroll / maxScroll;
            int thumbY = sbY + (int) (pct * (sbH - thumbH));
            graphics.fill(sbX, thumbY, sbX + sbW, thumbY + thumbH, accent);
        }
        
        boolean createHovered = mouseX >= listX && mouseX <= listX + listW && mouseY >= startY + PANEL_HEIGHT - 35 && mouseY <= startY + PANEL_HEIGHT - 15;
        graphics.fill(listX, startY + PANEL_HEIGHT - 35, listX + listW, startY + PANEL_HEIGHT - 15, createHovered ? accent : 0x14FFFFFF);
        graphics.renderOutline(listX, startY + PANEL_HEIGHT - 35, listW, 20, createHovered ? accent : 0x22FFFFFF);
        graphics.drawCenteredString(this.font, Component.literal("Create New Macro"), listX + listW / 2, startY + PANEL_HEIGHT - 29, createHovered ? -1 : accent);
        
        graphics.fill(startX + 145 + 190, startY + 20, startX + 145 + 191, startY + PANEL_HEIGHT, 0x44FFFFFF);
        
        if (active == null) {
            graphics.drawCenteredString(this.font, Component.literal("No active macro selected"), startX + 345 + 155, startY + PANEL_HEIGHT / 2, 0xFF8F8F8F);
            return;
        }
        
        int editX = startX + 345;
        
        macroNameBox.setX(editX);
        macroNameBox.setY(startY + 30);
        
        String bindBtnText = (bindingMacro == active) ? "..." : "Bind: " + (active.getKeybind() != -1 && active.getKeybind() != 0 ? getKeyName(active.getKeybind()) : "NONE");
        boolean bindHovered = mouseX >= editX + 160 && mouseX <= editX + 260 && mouseY >= startY + 30 && mouseY <= startY + 48;
        graphics.fill(editX + 160, startY + 30, editX + 260, startY + 48, bindHovered ? 0x2EFFFFFF : 0x14FFFFFF);
        graphics.renderOutline(editX + 160, startY + 30, 100, 18, bindHovered ? 0x60FFFFFF : 0x22FFFFFF);
        graphics.drawCenteredString(this.font, Component.literal(bindBtnText), editX + 210, startY + 35, -1);
        
        boolean holdHovered = mouseX >= editX && mouseX <= editX + 100 && mouseY >= startY + 56 && mouseY <= startY + 71;
        graphics.fill(editX, startY + 56, editX + 100, startY + 71, active.isHoldMode() ? accent : (holdHovered ? 0x2EFFFFFF : 0x14FFFFFF));
        graphics.renderOutline(editX, startY + 56, 100, 15, active.isHoldMode() ? accent : (holdHovered ? 0x60FFFFFF : 0x22FFFFFF));
        graphics.drawCenteredString(this.font, Component.literal("Hold Mode"), editX + 50, startY + 60, -1);
        
        boolean enabledHovered = mouseX >= editX + 110 && mouseX <= editX + 210 && mouseY >= startY + 56 && mouseY <= startY + 71;
        graphics.fill(editX + 110, startY + 56, editX + 210, startY + 71, active.isEnabled() ? accent : (enabledHovered ? 0x2EFFFFFF : 0x14FFFFFF));
        graphics.renderOutline(editX + 110, startY + 56, 100, 15, active.isEnabled() ? accent : (enabledHovered ? 0x60FFFFFF : 0x22FFFFFF));
        graphics.drawCenteredString(this.font, Component.literal("Enabled"), editX + 160, startY + 60, -1);
        
        boolean rec = com.eclipseware.imnotcheatingyouare.client.macro.MacroManager.isRecording();
        boolean recHovered = mouseX >= editX && mouseX <= editX + 80 && mouseY >= startY + 78 && mouseY <= startY + 98;
        graphics.fill(editX, startY + 78, editX + 80, startY + 98, rec ? 0xFFCC2222 : (recHovered ? 0x2EFFFFFF : 0x14FFFFFF));
        graphics.renderOutline(editX, startY + 78, 80, 20, rec ? 0xFFEE4444 : (recHovered ? 0x60FFFFFF : 0x22FFFFFF));
        graphics.drawCenteredString(this.font, Component.literal(rec ? "Stop Rec" : "Record"), editX + 40, startY + 84, -1);
        
        boolean play = com.eclipseware.imnotcheatingyouare.client.macro.MacroManager.isPlaying();
        boolean playHovered = mouseX >= editX + 85 && mouseX <= editX + 165 && mouseY >= startY + 78 && mouseY <= startY + 98;
        graphics.fill(editX + 85, startY + 78, editX + 165, startY + 98, play ? 0xFF22AA22 : (playHovered ? 0x2EFFFFFF : 0x14FFFFFF));
        graphics.renderOutline(editX + 85, startY + 78, 80, 20, play ? 0xFF44CC44 : (playHovered ? 0x60FFFFFF : 0x22FFFFFF));
        graphics.drawCenteredString(this.font, Component.literal(play ? "Stop Play" : "Play"), editX + 125, startY + 84, -1);
        
        boolean clearHovered = mouseX >= editX + 170 && mouseX <= editX + 250 && mouseY >= startY + 78 && mouseY <= startY + 98;
        graphics.fill(editX + 170, startY + 78, editX + 250, startY + 98, clearHovered ? 0x2EFFFFFF : 0x14FFFFFF);
        graphics.renderOutline(editX + 170, startY + 78, 80, 20, clearHovered ? 0x60FFFFFF : 0x22FFFFFF);
        graphics.drawCenteredString(this.font, Component.literal("Clear"), editX + 210, startY + 84, -1);
        
        int actionAreaY = startY + 105;
        int actionAreaH = 195;
        int actionAreaW = 300;
        
        graphics.fill(editX, actionAreaY, editX + actionAreaW, actionAreaY + actionAreaH, 0x10FFFFFF);
        graphics.renderOutline(editX, actionAreaY, actionAreaW, actionAreaH, 0x22FFFFFF);
        
        List<com.eclipseware.imnotcheatingyouare.client.macro.MacroAction> actions = active.getActions();
        
        graphics.extractor().enableScissor(editX + 2, actionAreaY + 2, editX + actionAreaW - 2, actionAreaY + actionAreaH - 2);
        graphics.extractor().pose().pushMatrix();
        graphics.extractor().pose().translate(0f, -actionListScroll);
        
        for (int idx = 0; idx < actions.size(); idx++) {
            com.eclipseware.imnotcheatingyouare.client.macro.MacroAction action = actions.get(idx);
            int rowY = actionAreaY + 5 + idx * 16;
            
            String label = "";
            int color = 0xFFCCCCCC;
            switch (action.getType()) {
                case DELAY -> {
                    label = "Delay: " + action.getDelayMs() + "ms";
                    color = 0xFF7F7F7F;
                }
                case KEY_PRESS -> {
                    label = "Pressed Key: " + getKeyName(action.getKeyCode());
                    color = 0xFF55FF55;
                }
                case KEY_RELEASE -> {
                    label = "Released Key: " + getKeyName(action.getKeyCode());
                    color = 0xFFFF5555;
                }
                case MOUSE_CLICK -> {
                    label = "Mouse Pressed: Button " + action.getKeyCode();
                    color = 0xFF55FFFF;
                }
                case MOUSE_RELEASE -> {
                    label = "Mouse Released: Button " + action.getKeyCode();
                    color = 0xFFFF55FF;
                }
            }
            graphics.drawString(this.font, Component.literal(label), editX + 10, rowY, color, false);
        }
        
        graphics.extractor().pose().popMatrix();
        graphics.extractor().disableScissor();
        
        int totalRightHeight = actions.size() * 15 + 10;
        if (totalRightHeight > actionAreaH) {
            int sbX = editX + actionAreaW - 6;
            int sbY = actionAreaY + 2;
            int sbW = 3;
            int sbH = actionAreaH - 4;
            graphics.fill(sbX, sbY, sbX + sbW, sbY + sbH, 0x15FFFFFF);
            int thumbH = Math.max(15, (int) ((double) sbH / totalRightHeight * sbH));
            int maxScroll = totalRightHeight - actionAreaH;
            double pct = actionListScroll / maxScroll;
            int thumbY = sbY + (int) (pct * (sbH - thumbH));
            graphics.fill(sbX, thumbY, sbX + sbW, thumbY + thumbH, accent);
        }
        
        boolean expHovered = mouseX >= editX && mouseX <= editX + 145 && mouseY >= startY + PANEL_HEIGHT - 35 && mouseY <= startY + PANEL_HEIGHT - 15;
        graphics.fill(editX, startY + PANEL_HEIGHT - 35, editX + 145, startY + PANEL_HEIGHT - 15, expHovered ? 0x2EFFFFFF : 0x14FFFFFF);
        graphics.renderOutline(editX, startY + PANEL_HEIGHT - 35, 145, 20, expHovered ? 0x60FFFFFF : 0x22FFFFFF);
        graphics.drawCenteredString(this.font, Component.literal("Export Macro"), editX + 72, startY + PANEL_HEIGHT - 29, -1);
        
        boolean impHovered = mouseX >= editX + 155 && mouseX <= editX + 300 && mouseY >= startY + PANEL_HEIGHT - 35 && mouseY <= startY + PANEL_HEIGHT - 15;
        graphics.fill(editX + 155, startY + PANEL_HEIGHT - 35, editX + 300, startY + PANEL_HEIGHT - 15, impHovered ? accent : 0x14FFFFFF);
        graphics.renderOutline(editX + 155, startY + PANEL_HEIGHT - 35, 145, 20, impHovered ? accent : 0x22FFFFFF);
        graphics.drawCenteredString(this.font, Component.literal("Import Macro"), editX + 227, startY + PANEL_HEIGHT - 29, impHovered ? -1 : accent);
    }

    private void renderFiltersUI(GuiGraphics graphics, int mouseX, int mouseY, float timeDelta, int startX, int startY) {
        int leftX = startX + 145;
        int leftY = startY + 70;
        int leftW = 200;
        int leftH = 270;

        int rightX = startX + 360;
        int rightY = startY + 70;
        int rightW = 285;
        int rightH = 270;

        playerListScroll = lerpDecay(playerListScroll, targetPlayerListScroll, 12f, timeDelta);
        entityListScroll = lerpDecay(entityListScroll, targetEntityListScroll, 12f, timeDelta);

        int r = 155, g = 60, b = 255;
        Module theme = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("Theme");
        if (theme != null) {
            r = (int) ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(theme, "Accent R").getValDouble();
            g = (int) ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(theme, "Accent G").getValDouble();
            b = (int) ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(theme, "Accent B").getValDouble();
        }
        int accent = new java.awt.Color(r, g, b).getRGB();

        // 1. Players Panel
        graphics.drawString(this.font, Component.literal("§b§lFiltered Players"), leftX, startY + 30, -1, false);

        // Edit box layout
        filterPlayerBox.setX(leftX);
        filterPlayerBox.setY(startY + 45);

        // Add button [+]
        boolean addHovered = mouseX >= leftX + 155 && mouseX <= leftX + 200 && mouseY >= startY + 45 && mouseY <= startY + 63;
        graphics.fill(leftX + 155, startY + 45, leftX + 200, startY + 63, addHovered ? accent : 0x14FFFFFF);
        graphics.renderOutline(leftX + 155, startY + 45, 45, 18, addHovered ? accent : 0x22FFFFFF);
        graphics.drawCenteredString(this.font, Component.literal("+"), leftX + 177, startY + 49, addHovered ? -1 : accent);

        graphics.fill(leftX, leftY, leftX + leftW, leftY + leftH, 0x10FFFFFF);
        graphics.renderOutline(leftX, leftY, leftW, leftH, 0x22FFFFFF);

        List<String> players = new ArrayList<>(com.eclipseware.imnotcheatingyouare.client.utils.TargetFilterManager.getFilteredPlayers());
        players.sort(String.CASE_INSENSITIVE_ORDER);

        graphics.extractor().enableScissor(leftX + 2, leftY + 2, leftX + leftW - 2, leftY + leftH - 2);
        graphics.extractor().pose().pushMatrix();
        graphics.extractor().pose().translate(0f, -playerListScroll);

        for (int i = 0; i < players.size(); i++) {
            String name = players.get(i);
            int itemY = leftY + 5 + i * 22;
            boolean hovered = mouseX >= leftX + 4 && mouseX <= leftX + leftW - 4 && mouseY >= itemY - (int) playerListScroll && mouseY <= itemY + 18 - (int) playerListScroll && mouseY >= leftY && mouseY <= leftY + leftH;

            graphics.fill(leftX + 4, itemY, leftX + leftW - 4, itemY + 18, hovered ? 0x25FFFFFF : 0x15FFFFFF);
            graphics.renderOutline(leftX + 4, itemY, leftW - 8, 18, hovered ? 0x44FFFFFF : 0x22FFFFFF);

            graphics.drawString(this.font, Component.literal(name), leftX + 10, itemY + 5, -1, false);

            boolean delHovered = mouseX >= leftX + leftW - 20 && mouseX <= leftX + leftW - 8 && mouseY >= itemY + 4 - (int) playerListScroll && mouseY <= itemY + 16 - (int) playerListScroll && mouseY >= leftY && mouseY <= leftY + leftH;
            graphics.drawString(this.font, Component.literal("X"), leftX + leftW - 18, itemY + 4, delHovered ? 0xFFFF5555 : 0xFF8F8F8F, false);
        }

        graphics.extractor().pose().popMatrix();
        graphics.extractor().disableScissor();

        int totalLeftHeight = players.size() * 22 + 10;
        if (totalLeftHeight > leftH) {
            int sbX = leftX + leftW - 6;
            int sbY = leftY + 2;
            int sbW = 3;
            int sbH = leftH - 4;
            graphics.fill(sbX, sbY, sbX + sbW, sbY + sbH, 0x15FFFFFF);
            int thumbH = Math.max(15, (int) ((double) sbH / totalLeftHeight * sbH));
            int maxScroll = totalLeftHeight - leftH;
            double pct = playerListScroll / maxScroll;
            int thumbY = sbY + (int) (pct * (sbH - thumbH));
            graphics.fill(sbX, thumbY, sbX + sbW, thumbY + thumbH, accent);
        }

        // 2. Entities Panel
        graphics.drawString(this.font, Component.literal("§b§lFiltered Entities"), rightX, startY + 30, -1, false);

        filterEntityBox.setX(rightX);
        filterEntityBox.setY(startY + 45);

        graphics.fill(rightX, rightY, rightX + rightW, rightY + rightH, 0x10FFFFFF);
        graphics.renderOutline(rightX, rightY, rightW, rightH, 0x22FFFFFF);

        List<net.minecraft.world.entity.EntityType<?>> sortedEntities = new ArrayList<>();
        String query = filterEntityBox.getValue().toLowerCase().replace(" ", "");
        for (net.minecraft.world.entity.EntityType<?> type : net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE) {
            String name = type.getDescription().getString();
            String id = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(type).toString();
            if (name.toLowerCase().replace(" ", "").contains(query) || id.toLowerCase().replace(" ", "").contains(query)) {
                sortedEntities.add(type);
            }
        }
        sortedEntities.sort((t1, t2) -> t1.getDescription().getString().compareToIgnoreCase(t2.getDescription().getString()));

        graphics.extractor().enableScissor(rightX + 2, rightY + 2, rightX + rightW - 2, rightY + rightH - 2);
        graphics.extractor().pose().pushMatrix();
        graphics.extractor().pose().translate(0f, -entityListScroll);

        for (int i = 0; i < sortedEntities.size(); i++) {
            net.minecraft.world.entity.EntityType<?> type = sortedEntities.get(i);
            String id = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(type).toString();
            String name = type.getDescription().getString();
            int itemY = rightY + 5 + i * 22;

            boolean isFiltered = com.eclipseware.imnotcheatingyouare.client.utils.TargetFilterManager.isEntityTypeFiltered(id);
            boolean hovered = mouseX >= rightX + 4 && mouseX <= rightX + rightW - 4 && mouseY >= itemY - (int) entityListScroll && mouseY <= itemY + 18 - (int) entityListScroll && mouseY >= rightY && mouseY <= rightY + rightH;

            graphics.fill(rightX + 4, itemY, rightX + rightW - 4, itemY + 18, isFiltered ? (accent & 0x00FFFFFF) | 0x25000000 : (hovered ? 0x25FFFFFF : 0x15FFFFFF));
            graphics.renderOutline(rightX + 4, itemY, rightW - 8, 18, isFiltered ? accent : (hovered ? 0x44FFFFFF : 0x22FFFFFF));

            graphics.drawString(this.font, Component.literal(name), rightX + 10, itemY + 5, -1, false);

            int nameW = this.font.width(name);
            graphics.drawString(this.font, Component.literal("§7" + id), rightX + 10 + nameW + 5, itemY + 5, 0xFF8F8F8F, false);

            graphics.renderOutline(rightX + rightW - 16, itemY + 3, 12, 12, isFiltered ? accent : 0x44FFFFFF);
            if (isFiltered) {
                graphics.fill(rightX + rightW - 14, itemY + 5, rightX + rightW - 10, itemY + 9, accent);
            }
        }

        graphics.extractor().pose().popMatrix();
        graphics.extractor().disableScissor();

        int totalRightHeight = sortedEntities.size() * 22 + 10;
        if (totalRightHeight > rightH) {
            int sbX = rightX + rightW - 6;
            int sbY = rightY + 2;
            int sbW = 3;
            int sbH = rightH - 4;
            graphics.fill(sbX, sbY, sbX + sbW, sbY + sbH, 0x15FFFFFF);
            int thumbH = Math.max(15, (int) ((double) sbH / totalRightHeight * sbH));
            int maxScroll = totalRightHeight - rightH;
            double pct = entityListScroll / maxScroll;
            int thumbY = sbY + (int) (pct * (sbH - thumbH));
            graphics.fill(sbX, thumbY, sbX + sbW, thumbY + thumbH, accent);
        }
    }
}
