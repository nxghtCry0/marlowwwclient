package com.eclipseware.imnotcheatingyouare.client.clickgui;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.ui.GuiGraphics;
import com.eclipseware.imnotcheatingyouare.client.ui.reworked.*;
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

public class ReworkedClickgui extends Screen {

    private static final int PANEL_WIDTH = 680;
    private static final int PANEL_HEIGHT = 400;

    private Category selectedCategory = Category.Combat;
    private Module expandedModule = null;
    private Module bindingModule = null;

    private final List<RoundedButton> categoryButtons = new ArrayList<>();
    private final Map<Module, RoundedToggle> moduleToggles = new HashMap<>();
    private final List<AbstractWidget> settingWidgets = new ArrayList<>();
    private final List<Setting> activeSettings = new ArrayList<>();
    private final Map<Module, Float> moduleConfigHeights = new HashMap<>();
    private final List<Module> filteredModules = new ArrayList<>();
    private String lastSearchQuery = null;
    private Category lastSelectedCategory = null;

    private EditBox searchBox;

    private float scrollOffset = 0;
    private float targetScrollOffset = 0;
    private float categoryScroll = 0;
    private float targetCategoryScroll = 0;
    private boolean draggingScrollbar = false;
    private boolean draggingCategoryScrollbar = false;

    private float entryAnim = 0f;
    private float exitAnim = 0f;
    private boolean closing = false;

    private float indicatorY = -1f;

    private float moduleListSlideX = 30f;

    private long lastRenderTime = 0;

    public ReworkedClickgui() {
        super(Component.literal("Marlowww Client — Reworked"));
    }

    private float getScaleFactor() {
        float s = 1.0f;
        if (this.width < PANEL_WIDTH + 40f) s = Math.min(s, this.width / (PANEL_WIDTH + 40f));
        if (this.height < PANEL_HEIGHT + 40f) s = Math.min(s, this.height / (PANEL_HEIGHT + 40f));
        return s;
    }

    @Override
    protected void init() {
        this.clearWidgets();
        categoryButtons.clear();
        moduleToggles.clear();
        settingWidgets.clear();
        activeSettings.clear();

        ReworkedTheme.refresh();

        float scale = getScaleFactor();
        int virtualWidth = (int) (this.width / scale);
        int virtualHeight = (int) (this.height / scale);
        int startX = (virtualWidth - PANEL_WIDTH) / 2;
        int startY = (virtualHeight - PANEL_HEIGHT) / 2;

        int catX = startX + 12;
        int catY = startY + 40;
        for (Category category : Category.values()) {
            RoundedButton btn = new RoundedButton(catX, catY, 120, 26, category.name(), () -> {
                if (this.selectedCategory != category) {
                    this.selectedCategory = category;
                    this.expandedModule = null;
                    this.targetScrollOffset = 0;
                    this.moduleListSlideX = 30f;
                    rebuildSettingWidgets();
                }
            }, category == selectedCategory ? RoundedButton.Style.PRIMARY : RoundedButton.Style.SUBTLE);
            categoryButtons.add(btn);
            this.addRenderableWidget(btn);
            catY += 30;
        }

        for (Module m : ImnotcheatingyouareClient.INSTANCE.moduleManager.modules) {
            if (m.isHidden()) continue;
            RoundedToggle toggle = new RoundedToggle(0, 0, 45, 20, m::isToggled, val -> m.toggle());
            moduleToggles.put(m, toggle);
            this.addRenderableWidget(toggle);
        }

        searchBox = new EditBox(this.font, 0, 0, 200, 20, Component.literal("Search Modules"));
        searchBox.setMaxLength(50);
        this.addRenderableWidget(searchBox);

        if (expandedModule != null) rebuildSettingWidgets();
    }

    private void rebuildSettingWidgets() {
        for (AbstractWidget w : settingWidgets) this.removeWidget(w);
        settingWidgets.clear();
        activeSettings.clear();
        if (expandedModule == null) return;

        List<Setting> settings = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingsByMod(expandedModule);
        if (settings == null) return;

        for (Setting s : settings) {
            activeSettings.add(s);
            if (s.isCheck()) {
                RoundedToggle t = new RoundedToggle(0, 0, 45, 20, s::getValBoolean, s::setValBoolean);
                settingWidgets.add(t);
                this.addRenderableWidget(t);
            } else if (s.isSlider()) {
                RoundedSlider sl = new RoundedSlider(0, 0, 150, 20,
                        s::getValDouble, s::setValDouble,
                        s.getMin(), s.getMax(), s.onlyInt(),
                        val -> Component.literal(s.onlyInt() ? String.valueOf(val.intValue()) : String.valueOf(val)).getString());
                settingWidgets.add(sl);
                this.addRenderableWidget(sl);
            } else if (s.isCombo()) {
                RoundedDropdown<String> dd = new RoundedDropdown<>(0, 0, 150, 20,
                        s.getOptions(), s::getValString, s::setValString, str -> str);
                settingWidgets.add(dd);
                this.addRenderableWidget(dd);
            }
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        float scale = getScaleFactor();
        double mx = event.x() / scale;
        double my = event.y() / scale;
        int button = event.button();

        if (bindingModule != null) {
            if (button != 0 && button != 1) {
                bindingModule.setKeyBind(button);
                try { com.eclipseware.imnotcheatingyouare.client.setting.ConfigManager.save(); } catch (Exception ignored) {}
                bindingModule = null;
                return true;
            }
        }

        int panelWidth = PANEL_WIDTH;
        int panelHeight = PANEL_HEIGHT;
        int virtualWidth = (int) (this.width / scale);
        int virtualHeight = (int) (this.height / scale);
        int startX = (virtualWidth - panelWidth) / 2;
        int startY = (virtualHeight - panelHeight) / 2;

        int listX = startX + 145;
        int listY = startY + 35;
        int listWidth = panelWidth - 145;
        int listHeight = panelHeight - 35 - 50;

        if (button == 0) {
            if (mx >= listX && mx <= listX + listWidth && my >= listY && my <= listY + listHeight) {
                int modY = listY - (int) scrollOffset;
                int rowHeight = 36;
                for (int i = 0; i < filteredModules.size(); i++) {
                    Module m = filteredModules.get(i);
                    float currentH = moduleConfigHeights.getOrDefault(m, 0f);
                    int bindX = listX + listWidth - 115;
                    int bindY = modY + (rowHeight - 18) / 2;
                    if (mx >= bindX && mx <= bindX + 55 && my >= bindY && my <= bindY + 18) {
                        bindingModule = (bindingModule == m) ? null : m;
                        Clickgui.playSound();
                        return true;
                    }
                    modY += rowHeight;
                    if (m == expandedModule) modY += (int) currentH;
                }
            }
            bindingModule = null;

            int sbX = startX + panelWidth - 12;
            if (mx >= sbX && mx <= startX + panelWidth && my >= listY && my <= listY + listHeight) {
                int rowHeight = 36;
                int totalHeight = filteredModules.size() * rowHeight;
                for (Module m : filteredModules) totalHeight += moduleConfigHeights.getOrDefault(m, 0f).intValue();
                if (totalHeight > listHeight) {
                    this.draggingScrollbar = true;
                    float pct = (float) ((my - listY) / listHeight);
                    pct = Math.max(0f, Math.min(1f, pct));
                    targetScrollOffset = pct * (totalHeight - listHeight);
                }
                return true;
            }

            int catContentH = Category.values().length * 30 + 10;
            int catSidebarH = panelHeight - 50;
            if (catContentH > catSidebarH) {
                int catSbX = startX + 134;
                int catSbY = startY + 40;
                if (mx >= catSbX && mx <= startX + 142 && my >= catSbY && my <= catSbY + catSidebarH) {
                    this.draggingCategoryScrollbar = true;
                    float pct = (float) ((my - catSbY) / catSidebarH);
                    pct = Math.max(0f, Math.min(1f, pct));
                    targetCategoryScroll = pct * (catContentH - catSidebarH);
                    return true;
                }
            }
        } else if (button == 1) {
            bindingModule = null;
            if (mx >= listX && mx <= listX + listWidth && my >= listY && my <= listY + listHeight) {
                int modY = listY - (int) scrollOffset;
                for (int i = 0; i < filteredModules.size(); i++) {
                    Module m = filteredModules.get(i);
                    if (my >= modY && my <= modY + 36) {
                        List<Setting> mSets = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingsByMod(m);
                        if (mSets != null && !mSets.isEmpty()) {
                            expandedModule = (expandedModule == m) ? null : m;
                            rebuildSettingWidgets();
                        }
                        return true;
                    }
                    modY += 36;
                    if (m == expandedModule) modY += activeSettings.size() * 30;
                }
            }
        }

        if (button == 0) {
            for (AbstractWidget w : settingWidgets) {
                if (w instanceof RoundedDropdown<?> dd && dd.isMenuOpen()) {
                    if (dd.mouseClicked(event, doubleClick)) return true;
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
        float scale = getScaleFactor();
        double my = event.y() / scale;

        if (this.draggingScrollbar) {
            int panelWidth = PANEL_WIDTH;
            int panelHeight = PANEL_HEIGHT;
            int virtualHeight = (int) (this.height / scale);
            int startY = (virtualHeight - panelHeight) / 2;
            int listY = startY + 35;
            int listHeight = panelHeight - 35 - 50;
            int rowHeight = 36;
            int totalHeight = filteredModules.size() * rowHeight;
            for (Module m : filteredModules) totalHeight += moduleConfigHeights.getOrDefault(m, 0f).intValue();
            if (totalHeight > listHeight) {
                float pct = (float) ((my - listY) / listHeight);
                pct = Math.max(0f, Math.min(1f, pct));
                targetScrollOffset = pct * (totalHeight - listHeight);
            }
            return true;
        }
        if (this.draggingCategoryScrollbar) {
            int panelHeight = PANEL_HEIGHT;
            int virtualHeight = (int) (this.height / scale);
            int startY = (virtualHeight - panelHeight) / 2;
            int catSbY = startY + 40;
            int catSidebarH = panelHeight - 50;
            int catContentH = Category.values().length * 30 + 10;
            if (catContentH > catSidebarH) {
                float pct = (float) ((my - catSbY) / catSidebarH);
                pct = Math.max(0f, Math.min(1f, pct));
                targetCategoryScroll = pct * (catContentH - catSidebarH);
            }
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        float scale = getScaleFactor();
        double sMx = mouseX / scale;
        int panelWidth = PANEL_WIDTH;
        int virtualWidth = (int) (this.width / scale);
        int startX = (virtualWidth - panelWidth) / 2;

        if (sMx < startX + 140) {
            int contentH = Category.values().length * 30 + 10;
            int maxScroll = Math.max(0, contentH - (PANEL_HEIGHT - 50));
            targetCategoryScroll -= (float) (verticalAmount * 20);
            targetCategoryScroll = Math.max(0, Math.min(maxScroll, targetCategoryScroll));
            return true;
        }

        if (verticalAmount != 0) {
            targetScrollOffset -= (float) (verticalAmount * 30);
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

    private float lerpDecay(float current, float target, float speed, float dt) {
        float factor = 1f - (float) Math.exp(-speed * dt);
        return current + (target - current) * factor;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        ReworkedTheme.refresh();

        long now = System.currentTimeMillis();
        if (lastRenderTime == 0) lastRenderTime = now;
        float dt = Math.min(0.1f, (now - lastRenderTime) / 1000f);
        lastRenderTime = now;

        if (!closing) {
            entryAnim = lerpDecay(entryAnim, 1f, 10f * ReworkedTheme.animSpeed, dt);
        } else {
            exitAnim = lerpDecay(exitAnim, 1f, 14f * ReworkedTheme.animSpeed, dt);
            if (exitAnim > 0.985f) {
                try {
                    net.minecraft.client.Minecraft.getInstance().execute(this::onClose);
                } catch (Throwable t) {
                    this.onClose();
                }
                return;
            }
        }

        scrollOffset = lerpDecay(scrollOffset, targetScrollOffset, 12f * ReworkedTheme.animSpeed, dt);
        categoryScroll = lerpDecay(categoryScroll, targetCategoryScroll, 12f * ReworkedTheme.animSpeed, dt);
        moduleListSlideX = lerpDecay(moduleListSlideX, 0f, 10f * ReworkedTheme.animSpeed, dt);

        float scale = getScaleFactor();
        int scaledMouseX = (int) (mouseX / scale);
        int scaledMouseY = (int) (mouseY / scale);

        context.pose().pushMatrix();
        context.pose().scale(scale, scale);

        GuiGraphics g = new GuiGraphics(context);

        int panelWidth = PANEL_WIDTH;
        int panelHeight = PANEL_HEIGHT;
        int virtualWidth = (int) (this.width / scale);
        int virtualHeight = (int) (this.height / scale);
        int startX = (virtualWidth - panelWidth) / 2;
        int startY = (virtualHeight - panelHeight) / 2;

        float entryEase = easeOutBack(entryAnim);
        float exitEase = 1f - exitAnim;
        float combined = entryEase * exitEase;
        int scaledPanelW = (int) (panelWidth * (0.92f + 0.08f * combined));
        int scaledPanelH = (int) (panelHeight * (0.92f + 0.08f * combined));
        int panelX = startX + (panelWidth - scaledPanelW) / 2;
        int panelY = startY + (panelHeight - scaledPanelH) / 2;
        int panelR = ReworkedTheme.radius + 4;

        if (combined < 1f) {
            int overlayAlpha = (int) (0x88 * combined);
            context.fill(0, 0, this.width, this.height, (overlayAlpha << 24));
        }

        Rounded.shadow(g, panelX, panelY, scaledPanelW, scaledPanelH, panelR);
        Rounded.fill(g, panelX, panelY, scaledPanelW, scaledPanelH, panelR, ReworkedTheme.background);
        Rounded.outline(g, panelX, panelY, scaledPanelW, scaledPanelH, panelR, 1f, 0x33FFFFFF);

        int headerH = 28;
        int headerR = panelR;
        Rounded.fill(g, panelX, panelY, scaledPanelW, headerH, headerR, ReworkedTheme.withAlpha(ReworkedTheme.brighten(ReworkedTheme.bgRgb, 0.20f), 0xFF));
        context.fill(panelX, panelY + headerH - headerR, panelX + scaledPanelW, panelY + headerH, ReworkedTheme.withAlpha(ReworkedTheme.brighten(ReworkedTheme.bgRgb, 0.20f), 0xFF));

        String headerTitle = "§bMarlowww Client §f| §7Reworked UI";
        ReworkedFont.drawString(g, headerTitle, panelX + 14, panelY + 10, ReworkedTheme.text, false);

        String subtitle = "v3.1 Reworked";
        ReworkedFont.drawRightAlignedString(g, subtitle, panelX + scaledPanelW - 14, panelY + 10, ReworkedTheme.textSubtle);

        context.fill(panelX + 140, panelY + headerH, panelX + 141, panelY + scaledPanelH, 0x33FFFFFF);

        for (int i = 0; i < categoryButtons.size(); i++) {
            RoundedButton btn = categoryButtons.get(i);
            int catY = panelY + 40 + (i * 30) - (int) categoryScroll;
            if (catY >= panelY + 35 && catY + 26 <= panelY + scaledPanelH - 10) {
                btn.visible = true;
                btn.setX(panelX + 12);
                btn.setY(catY);
            } else {
                btn.visible = false;
                btn.setY(-100);
            }
        }

        float targetIndicatorY = panelY + 40 + (selectedCategory.ordinal() * 30) - categoryScroll;
        if (indicatorY == -1f) indicatorY = targetIndicatorY;
        indicatorY = lerpDecay(indicatorY, targetIndicatorY, 15f * ReworkedTheme.animSpeed, dt);
        if (indicatorY >= panelY + 35 && indicatorY + 26 <= panelY + scaledPanelH - 10) {
            int indR = 2;
            Rounded.fill(g, panelX + 8, (int) indicatorY, 3, 26, indR, ReworkedTheme.accent);
        }

        int catContentH = Category.values().length * 30 + 10;
        int catSidebarH = panelHeight - 50;
        if (catContentH > catSidebarH) {
            int sbX = panelX + 138;
            int sbY = panelY + 40;
            int sbW = 2;
            int sbH = catSidebarH;
            context.fill(sbX - 1, sbY, sbX + sbW + 1, sbY + sbH, 0x10FFFFFF);
            int thumbH = Math.max(10, (int) ((double) sbH / catContentH * sbH));
            int maxScroll = catContentH - catSidebarH;
            double pct = categoryScroll / maxScroll;
            int thumbY = sbY + (int) (pct * (sbH - thumbH));
            boolean sbHovered = scaledMouseX >= sbX - 3 && scaledMouseX <= sbX + sbW + 3 && scaledMouseY >= sbY && scaledMouseY <= sbY + sbH;
            int thumbColor = (draggingCategoryScrollbar || sbHovered) ? ReworkedTheme.accent : 0x55FFFFFF;
            context.fill(sbX, thumbY, sbX + sbW, thumbY + thumbH, thumbColor);
        }

        int listX = panelX + 145 + (int) moduleListSlideX;
        int listY = panelY + 35;
        int listWidth = scaledPanelW - 145;
        int listHeight = scaledPanelH - 35 - 50;

        String query = searchBox != null ? searchBox.getValue() : "";
        if (lastSelectedCategory != selectedCategory || !query.equals(lastSearchQuery)) {
            lastSelectedCategory = selectedCategory;
            lastSearchQuery = query;
            filteredModules.clear();
            String ql = query.toLowerCase().replace(" ", "");
            List<Module> source = ql.isEmpty()
                    ? ImnotcheatingyouareClient.INSTANCE.moduleManager.getModules(selectedCategory)
                    : ImnotcheatingyouareClient.INSTANCE.moduleManager.modules;
            for (Module m : source) {
                if (!m.isHidden() && m.getName().toLowerCase().replace(" ", "").contains(ql)) {
                    filteredModules.add(m);
                }
            }
            for (Map.Entry<Module, RoundedToggle> e : moduleToggles.entrySet()) {
                if (!filteredModules.contains(e.getKey())) {
                    e.getValue().visible = false;
                    e.getValue().setY(-100);
                }
            }
        }

        int rowHeight = 36;
        int totalHeight = filteredModules.size() * rowHeight;
        for (Module m : filteredModules) {
            float currentH = moduleConfigHeights.getOrDefault(m, 0f);
            float targetH = (m == expandedModule) ? (activeSettings.size() * 30) : 0f;
            if (Math.abs(targetH - currentH) > 0.01f) {
                currentH = lerpDecay(currentH, targetH, 12f * ReworkedTheme.animSpeed, dt);
                if (Math.abs(targetH - currentH) <= 0.01f) currentH = targetH;
                moduleConfigHeights.put(m, currentH);
            }
            totalHeight += (int) currentH;
        }
        if (targetScrollOffset > totalHeight - listHeight && totalHeight > listHeight) {
            targetScrollOffset = totalHeight - listHeight;
        }
        if (totalHeight <= listHeight) targetScrollOffset = 0;

        g.extractor().enableScissor(listX, listY, listX + listWidth, listY + listHeight);

        int modY = listY - (int) scrollOffset;
        int rowR = Math.min(ReworkedTheme.radius, Math.min(listWidth - 4, rowHeight - 4) / 2);

        for (int i = 0; i < filteredModules.size(); i++) {
            Module m = filteredModules.get(i);
            RoundedToggle toggle = moduleToggles.get(m);
            if (toggle == null) continue;

            float currentH = moduleConfigHeights.getOrDefault(m, 0f);
            boolean visible = (modY + rowHeight + currentH > listY && modY < listY + listHeight);

            if (visible) {
                boolean hovered = scaledMouseX >= listX && scaledMouseX <= listX + listWidth
                        && scaledMouseY >= modY && scaledMouseY <= modY + rowHeight;

                int rowBg = hovered ? ReworkedTheme.rowHover : ((i % 2 == 0) ? 0x08FFFFFF : 0x00000000);
                Rounded.fill(g, listX + 2, modY, listWidth - 4, rowHeight, rowR, rowBg);

                List<Setting> mSets = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingsByMod(m);
                String suffix = (mSets != null && !mSets.isEmpty()) ? (m == expandedModule ? " §7[-]" : " §7[+]") : "";
                ReworkedFont.drawString(g, m.getName() + suffix, listX + 14, modY + 7, ReworkedTheme.text, false);

                String desc = m.getDescription();
                if (desc != null && !desc.isEmpty()) {
                    if (desc.length() > 75) desc = desc.substring(0, 72) + "...";
                    ReworkedFont.drawString(g, desc, listX + 14, modY + 19, ReworkedTheme.textMuted, false);
                }

                toggle.setX(listX + listWidth - 55);
                toggle.setY(modY + (rowHeight - 20) / 2);
                toggle.visible = (toggle.getY() + 20 > listY && toggle.getY() < listY + listHeight);

                int bindX = listX + listWidth - 115;
                int bindY = modY + (rowHeight - 18) / 2;
                boolean bindHovered = scaledMouseX >= bindX && scaledMouseX <= bindX + 55 && scaledMouseY >= bindY && scaledMouseY <= bindY + 18;
                int bindR = Math.min(ReworkedTheme.radius, Math.min(55, 18) / 2);
                int bindBg = (bindingModule == m) ? ReworkedTheme.accent
                        : (bindHovered ? ReworkedTheme.controlBgHover : ReworkedTheme.controlBg);
                int bindBorder = (bindingModule == m || bindHovered) ? ReworkedTheme.controlBorderHover : ReworkedTheme.controlBorder;
                Rounded.fill(g, bindX, bindY, 55, 18, bindR, bindBg);
                Rounded.outline(g, bindX, bindY, 55, 18, bindR, 1f, bindBorder);

                String bindText = (bindingModule == m) ? "..." : getKeyName(m.getKeyBind());
                int bindTextCol = (bindingModule == m) ? 0xFFFFFFFF : ReworkedTheme.text;
                int textW = ReworkedFont.width(bindText);
                ReworkedFont.drawString(g, bindText, bindX + (55 - textW) / 2, bindY + 5, bindTextCol, false);
            } else {
                toggle.visible = false;
                toggle.setY(-100);
            }

            modY += rowHeight;
            if (currentH > 0.5f) {
                int cardH = (int) currentH;
                Rounded.fill(g, listX + 14, modY, listWidth - 28, cardH,
                        rowR, ReworkedTheme.withAlpha(ReworkedTheme.bgRgb, 0xA0));

                if (m == expandedModule) {
                    float subY = modY;
                    for (int sIdx = 0; sIdx < activeSettings.size(); sIdx++) {
                        Setting s = activeSettings.get(sIdx);
                        AbstractWidget w = settingWidgets.get(sIdx);

                        ReworkedFont.drawString(g, s.getName(), listX + 28, (int) subY + 10, ReworkedTheme.textMuted, false);
                        if (w instanceof RoundedSlider) {
                            w.setX(listX + listWidth - 165);
                            w.setY((int) subY + 5);
                        } else if (w instanceof RoundedDropdown) {
                            w.setX(listX + listWidth - 165);
                            w.setY((int) subY + 5);
                        } else {
                            w.setX(listX + listWidth - 55);
                            w.setY((int) subY + 5);
                        }
                        w.visible = (subY + 20 > listY && subY < listY + listHeight);
                        subY += 30;
                    }
                }
                modY += (int) currentH;
            } else if (m == expandedModule) {
                for (AbstractWidget w : settingWidgets) {
                    w.visible = false;
                    w.setY(-100);
                }
            }
        }

        if (totalHeight > listHeight) {
            int sbX = panelX + scaledPanelW - 12;
            int sbY = listY + 2;
            int sbW = 5;
            int sbH = listHeight - 4;
            context.fill(sbX, sbY, sbX + sbW, sbY + sbH, 0x10FFFFFF);
            int thumbH = Math.max(10, (int) ((float) listHeight / totalHeight * sbH));
            float maxScroll = totalHeight - listHeight;
            float scrollPct = scrollOffset / maxScroll;
            int thumbY = sbY + (int) (scrollPct * (sbH - thumbH));
            boolean sbHovered = scaledMouseX >= sbX - 2 && scaledMouseX <= sbX + sbW + 2 && scaledMouseY >= sbY && scaledMouseY <= sbY + sbH;
            int thumbColor = (draggingScrollbar || sbHovered) ? ReworkedTheme.accent : 0x55FFFFFF;
            context.fill(sbX, thumbY, sbX + sbW, thumbY + thumbH, thumbColor);
        }

        boolean oldSearchVisible = searchBox != null && searchBox.visible;
        List<Boolean> oldCatVis = new ArrayList<>();
        for (RoundedButton b : categoryButtons) { oldCatVis.add(b.visible); b.visible = false; }
        if (searchBox != null) searchBox.visible = false;

        super.extractRenderState(context, scaledMouseX, scaledMouseY, delta);

        g.extractor().disableScissor();

        if (searchBox != null) {
            searchBox.visible = oldSearchVisible;
            if (searchBox.visible) {
                searchBox.setX(panelX + 150);
                searchBox.setY(panelY + scaledPanelH - 30);
                searchBox.setWidth(scaledPanelW - 160);
                searchBox.extractRenderState(context, scaledMouseX, scaledMouseY, delta);
            }
        }
        for (int i = 0; i < categoryButtons.size(); i++) {
            RoundedButton b = categoryButtons.get(i);
            b.visible = oldCatVis.get(i);
            if (b.visible) b.extractRenderState(context, scaledMouseX, scaledMouseY, delta);
        }

        context.fill(panelX + 140, panelY + scaledPanelH - 40, panelX + scaledPanelW, panelY + scaledPanelH - 39, 0x33FFFFFF);

        if (expandedModule != null) {
            for (AbstractWidget w : settingWidgets) {
                if (w instanceof RoundedDropdown<?> dropdown) {
                    dropdown.renderOverlay(g, scaledMouseX, scaledMouseY);
                }
            }
        }

        if (searchBox != null && searchBox.visible && (searchBox.getValue() == null || searchBox.getValue().isEmpty())) {
            ReworkedFont.drawString(g, "Search modules...", panelX + 155, panelY + scaledPanelH - 25, ReworkedTheme.textSubtle, false);
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
        if (bindingModule != null) {
            int key = input.input();
            int targetKey = key;
            if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_DELETE || key == org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE || key == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
                targetKey = -1;
            }
            bindingModule.setKeyBind(targetKey);
            try { com.eclipseware.imnotcheatingyouare.client.setting.ConfigManager.save(); } catch (Exception ignored) {}
            bindingModule = null;
            if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) return true;
        }

        Module menuMod = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("Menu");
        int menuKey = menuMod != null ? menuMod.getKeyBind() : org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT;
        if (input.input() == menuKey) {
            boolean anyFocused = (searchBox != null && searchBox.isFocused());
            if (!anyFocused) {
                closing = true;
                return true;
            }
        }
        if (input.input() == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE && !closing) {
            closing = true;
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public void removed() {
        bindingModule = null;
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static float easeOutBack(float t) {
        t = Math.max(0f, Math.min(1f, t));
        float c1 = 1.70158f;
        float c3 = c1 + 1f;
        return 1f + c3 * (float) Math.pow(t - 1f, 3f) + c1 * (float) Math.pow(t - 1f, 2f);
    }
}
