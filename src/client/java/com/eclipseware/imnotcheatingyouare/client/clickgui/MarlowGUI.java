package com.eclipseware.imnotcheatingyouare.client.clickgui;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.clickgui.HudEditorScreen;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.utils.remnant.FontAtlas;
import com.eclipseware.imnotcheatingyouare.client.utils.remnant.Fonts;
import com.eclipseware.imnotcheatingyouare.client.utils.remnant.Render2DEngine;
import java.awt.Color;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.joml.Matrix3x2fStack;
import org.joml.Vector2f;
import org.lwjgl.glfw.GLFW;

public class MarlowGUI extends Screen {
    private static final float PANEL_WIDTH = 115.0f;
    private static final float PANEL_HEIGHT = 18.0f;
    private static final float ANIMATION_DURATION = 500.0f;
    private static final float TOGGLE_WIDTH = 18.0f;
    private static final float TOGGLE_HEIGHT = 9.0f;
    private static final float KNOB_SIZE = 6.0f;
    private static final float PANEL_SPACING = 10.0f;
    private static final float INITIAL_Y = 15.0f;
    private static final float SETTING_PADDING = 6.0f;
    private static final float SETTING_TEXT_SIZE = 6.0f;
    private static final float TITLE_TEXT_SIZE = 7.5f;
    private static final float SLIDER_THICKNESS = 2.0f;
    private static final float OUTLINE_THICKNESS = 1.0f;
    private static final float TOGGLE_KNOB_OFFSET = 1.5f;
    private static final float TOOLTIP_DELAY = 1000.0f;
    private static final float TOOLTIP_PADDING = 4.0f;
    private static final float TOOLTIP_TEXT_SIZE = 7.0f;

    private static final Color HIGHLIGHT_COLOR = new Color(138, 75, 255);
    private static final Color DISABLED_COLOR = new Color(40, 44, 52, 220);
    private static final Color BACKGROUND_COLOR = new Color(33, 37, 43, 230);
    private static final Color OUTLINE_COLOR = new Color(24, 26, 31, 200);
    private static final Color TOGGLE_KNOB_COLOR = new Color(255, 255, 255, 220);
    private static final int TEXT_PRIMARY = 0xFFFFFFFF;
    private static final int TEXT_SECONDARY = 0xFF9DA5B4;
    private static final int TEXT_DISABLED = 0xFF5C6370;

    private Module selectedModule;
    private Panel selectedPanel;
    private Module hoveredModule;
    private long hoverStartTime;
    private boolean showingTooltip;
    private Setting hoveredSetting;
    private long settingHoverStartTime;
    private boolean showingSettingTooltip;
    private Setting draggingSetting = null;
    private Module bindingModule = null;
    private static final Map<Category, Panel> panels = new HashMap<>();
    private int lastScreenWidth = -1;
    private int lastScreenHeight = -1;
    private long openTime = 0L;

    public Module getSelectedModule() { return this.selectedModule; }
    public void setSelectedModule(Module val) { this.selectedModule = val; }
    public Panel getSelectedPanel() { return this.selectedPanel; }
    public void setSelectedPanel(Panel val) { this.selectedPanel = val; }

    public MarlowGUI() {
        super(Component.literal("ClickGui"));
        this.layoutPanels();
    }

    @Override
    protected void init() {
        super.init();
        this.openTime = System.currentTimeMillis();
        this.selectedModule = null;
        this.selectedPanel = null;
        this.hoveredSetting = null;
        this.showingSettingTooltip = false;
        for (Panel panel : panels.values()) {
            panel.setShowingSettings(false);
            panel.setSettingsAnimationProgress(0.0f);
            panel.setOpenSetting(null);
        }
    }

    private void layoutPanels() {
        if (!panels.isEmpty()) return;
        float x = 10.0f;
        float y = INITIAL_Y;
        float screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        if (screenWidth <= 0.0f) screenWidth = 960.0f;
        for (Category category : Category.values()) {
            List<Module> modules = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModules(category);
            if (modules.isEmpty()) continue;
            if (x + PANEL_WIDTH > screenWidth - 10.0f) {
                x = 10.0f;
                y += 240.0f;
            }
            Panel panel = new Panel(category, true, new Vector2f(x, y));
            panel.setModules(modules);
            panels.put(category, panel);
            x += PANEL_WIDTH + PANEL_SPACING;
        }
    }

    private void recenterPanels() {}
    private Color getHighlightColor() { return HIGHLIGHT_COLOR; }
    private Color getOutlineColor() { return OUTLINE_COLOR; }

    private String getCategoryIcon(Category category) {
        return switch (category) {
            case Combat, Crystal, Blatant -> "\ue2b3";
            case Movement -> "\ue424";
            case Render, HUD -> "\ue0be";
            case Utility, Exploit -> "\ue4f5";
            case Client, Configs, Macros -> "\ue3bc";
            default -> "\ue18d";
        };
    }

    private float getSettingHeight(Setting setting, Panel panel) {
        float height = PANEL_HEIGHT;
        if (setting.isCombo()) {
            if (panel.getOpenSetting() == setting && setting.getOptions() != null) {
                height += (PANEL_HEIGHT - 2.0f) * (float)setting.getOptions().size();
            }
        } else if (setting.isSlider()) {
            height += 4.0f;
        }
        return height;
    }

    private void renderSetting(Setting setting, GuiGraphicsExtractor context, Vector2f position, float moduleY, FontAtlas font, int mouseX, int mouseY) {
        float posX = position.x();
        float maxTextWidth = PANEL_WIDTH - SETTING_PADDING * 2.0f - 10.0f;
        if (setting.isCheck()) {
            maxTextWidth = PANEL_WIDTH - TOGGLE_WIDTH - SETTING_PADDING * 2.0f - 6.0f;
        } else if (setting.isSlider()) {
            DecimalFormat format = new DecimalFormat("#.##");
            String valStr = format.format(setting.getValDouble());
            float valWidth = font.getWidth(valStr, SETTING_TEXT_SIZE);
            maxTextWidth = PANEL_WIDTH - SETTING_PADDING * 2.0f - valWidth - 8.0f;
        } else if (setting.isCombo()) {
            String valStr = setting.getValString();
            float valWidth = font.getWidth(valStr, 6.0f);
            maxTextWidth = PANEL_WIDTH - SETTING_PADDING * 2.0f - valWidth - 10.0f;
        }
        Object result = setting.getName();
        if (result != null && font.getWidth((String)result, SETTING_TEXT_SIZE) > maxTextWidth) {
            while (((String)result).length() > 0 && font.getWidth((String)result + "...", SETTING_TEXT_SIZE) > maxTextWidth) {
                result = ((String)result).substring(0, ((String)result).length() - 1);
            }
            result = (String)result + "...";
        }
        font.render(context.pose(), (String)result, posX + SETTING_PADDING, moduleY + (PANEL_HEIGHT / 2.0f) - font.getLineHeight(SETTING_TEXT_SIZE) / 2.0f, SETTING_TEXT_SIZE, TEXT_SECONDARY);
        if (setting.isCheck()) {
            float smoothProgress;
            float toggleY = moduleY + (PANEL_HEIGHT - TOGGLE_HEIGHT) / 2.0f;
            Panel panel = this.selectedPanel;
            float animationProgress = panel.booleanAnimations.computeIfAbsent(setting, k -> Float.valueOf(setting.getValBoolean() ? 1.0f : 0.0f)).floatValue();
            long lastAnimationTime = panel.booleanAnimationTimes.computeIfAbsent(setting, k -> System.currentTimeMillis());
            long currentTime = System.currentTimeMillis();
            float elapsed = Math.min(1.0f, (float)(currentTime - lastAnimationTime) / 500.0f * 3.0f);
            animationProgress = this.setAnimationDuration(setting.getValBoolean(), animationProgress, elapsed, -1.0f);
            if (animationProgress < 0.5f) {
                smoothProgress = 2.0f * animationProgress * animationProgress;
            } else {
                float t = -2.0f * animationProgress + 2.0f;
                smoothProgress = 1.0f - t * t / 2.0f;
            }
            panel.booleanAnimations.put(setting, Float.valueOf(animationProgress));
            panel.booleanAnimationTimes.put(setting, currentTime);
            Color accent = this.getHighlightColor();
            Color toggleColor = new Color(DISABLED_COLOR.getRed() + (int)((float)(accent.getRed() - DISABLED_COLOR.getRed()) * smoothProgress), DISABLED_COLOR.getGreen() + (int)((float)(accent.getGreen() - DISABLED_COLOR.getGreen()) * smoothProgress), DISABLED_COLOR.getBlue() + (int)((float)(accent.getBlue() - DISABLED_COLOR.getBlue()) * smoothProgress));
            float pillX = posX + PANEL_WIDTH - TOGGLE_WIDTH - SETTING_PADDING - 2.0f;
            org.joml.Matrix4f matrix = new org.joml.Matrix4f(
                context.pose().m00, context.pose().m01, 0.0f, 0.0f,
                context.pose().m10, context.pose().m11, 0.0f, 0.0f,
                0.0f, 0.0f, 1.0f, 0.0f,
                context.pose().m20, context.pose().m21, 0.0f, 1.0f
            );
            com.eclipseware.imnotcheatingyouare.client.utils.ShaderManager.drawRoundedRect(matrix, pillX, toggleY, TOGGLE_WIDTH, TOGGLE_HEIGHT, TOGGLE_HEIGHT / 2.3f, toggleColor.getRGB());
            float knobStartX = pillX + 1.0f;
            float knobEndX = pillX + TOGGLE_WIDTH - KNOB_SIZE - 2.0f;
            float knobX = knobStartX + (knobEndX - knobStartX) * smoothProgress;
            float knobY = toggleY + TOGGLE_KNOB_OFFSET;
            com.eclipseware.imnotcheatingyouare.client.utils.ShaderManager.drawRoundedRect(matrix, knobX, knobY, KNOB_SIZE, KNOB_SIZE, KNOB_SIZE / 2.0f, TOGGLE_KNOB_COLOR.getRGB());
        } else if (setting.isSlider()) {
            float sliderY = moduleY + PANEL_HEIGHT - 1.5f;
            DecimalFormat format = new DecimalFormat("#.##");
            String value = format.format(setting.getValDouble());
            float valueWidth = font.getWidth(value, SETTING_TEXT_SIZE);
            font.render(context.pose(), value, posX + PANEL_WIDTH - SETTING_PADDING - 2.0f - valueWidth, moduleY + (PANEL_HEIGHT / 2.0f) - font.getLineHeight(SETTING_TEXT_SIZE) / 2.0f, SETTING_TEXT_SIZE, TEXT_DISABLED);
            org.joml.Matrix4f matrix = new org.joml.Matrix4f(
                context.pose().m00, context.pose().m01, 0.0f, 0.0f,
                context.pose().m10, context.pose().m11, 0.0f, 0.0f,
                0.0f, 0.0f, 1.0f, 0.0f,
                context.pose().m20, context.pose().m21, 0.0f, 1.0f
            );
            com.eclipseware.imnotcheatingyouare.client.utils.ShaderManager.drawRoundedRect(matrix, posX + SETTING_PADDING, sliderY, PANEL_WIDTH - SETTING_PADDING * 2.0f, SLIDER_THICKNESS, 1.0f, DISABLED_COLOR.getRGB());
            double progress = (setting.getValDouble() - setting.getMin()) / (setting.getMax() - setting.getMin());
            float targetProgress = (float)progress;
            Panel panel = this.selectedPanel;
            float animationProgress = panel.numberAnimations.computeIfAbsent(setting, k -> Float.valueOf(targetProgress)).floatValue();
            long lastAnimationTime = panel.numberAnimationTimes.computeIfAbsent(setting, k -> System.currentTimeMillis());
            long currentTime = System.currentTimeMillis();
            float elapsed = Math.min(1.0f, (float)(currentTime - lastAnimationTime) / 500.0f * 3.0f);
            animationProgress = this.setAnimationDuration(animationProgress < targetProgress, animationProgress, elapsed, targetProgress);
            panel.numberAnimations.put(setting, Float.valueOf(animationProgress));
            panel.numberAnimationTimes.put(setting, currentTime);
            com.eclipseware.imnotcheatingyouare.client.utils.ShaderManager.drawRoundedRect(matrix, posX + SETTING_PADDING, sliderY, (PANEL_WIDTH - SETTING_PADDING * 2.0f) * animationProgress, SLIDER_THICKNESS, 1.0f, this.getHighlightColor().getRGB());
            float knobX = posX + SETTING_PADDING + (PANEL_WIDTH - SETTING_PADDING * 2.0f) * animationProgress - 3.0f;
            float knobY = sliderY - 2.0f;
            com.eclipseware.imnotcheatingyouare.client.utils.ShaderManager.drawRoundedRect(matrix, knobX, knobY, 6.0f, 6.0f, 3.0f, TOGGLE_KNOB_COLOR.getRGB());
        } else if (setting.isCombo()) {
            float smoothProgress;
            Panel panel = this.selectedPanel;
            boolean isOpen = panel.getOpenSetting() == setting;
            float animationProgress = panel.modeAnimations.computeIfAbsent(setting, k -> Float.valueOf(isOpen ? 1.0f : 0.0f)).floatValue();
            long lastAnimationTime = panel.modeAnimationTimes.computeIfAbsent(setting, k -> System.currentTimeMillis());
            long currentTime = System.currentTimeMillis();
            float elapsed = Math.min(1.0f, (float)(currentTime - lastAnimationTime) / 500.0f * 3.0f);
            if ((animationProgress = this.setAnimationDuration(isOpen, animationProgress, elapsed, -1.0f)) < 0.5f) {
                smoothProgress = 2.0f * animationProgress * animationProgress;
            } else {
                float t = -2.0f * animationProgress + 2.0f;
                smoothProgress = 1.0f - t * t / 2.0f;
            }
            panel.modeAnimations.put(setting, Float.valueOf(animationProgress));
            panel.modeAnimationTimes.put(setting, currentTime);
            String value = setting.getValString();
            float valueWidth = font.getWidth(value, 6.0f);
            font.render(context.pose(), value, posX + PANEL_WIDTH - SETTING_PADDING - 2.0f - valueWidth, moduleY + (PANEL_HEIGHT / 2.0f) - font.getLineHeight(6.0f) / 2.0f, 6.0f, TEXT_DISABLED);
            if (animationProgress > 0.0f && setting.getOptions() != null) {
                float optionY = moduleY + PANEL_HEIGHT;
                for (String mode : setting.getOptions()) {
                    boolean isSelected = mode.equals(setting.getValString());
                    float alpha = (int)(smoothProgress * 255.0f);
                    org.joml.Matrix4f matrix = new org.joml.Matrix4f(
                        context.pose().m00, context.pose().m01, 0.0f, 0.0f,
                        context.pose().m10, context.pose().m11, 0.0f, 0.0f,
                        0.0f, 0.0f, 1.0f, 0.0f,
                        context.pose().m20, context.pose().m21, 0.0f, 1.0f
                    );
                    if (isSelected) {
                        com.eclipseware.imnotcheatingyouare.client.utils.ShaderManager.drawRoundedRect(matrix, posX + 4.0f, optionY, PANEL_WIDTH - 8.0f, PANEL_HEIGHT - 2.0f, 3.5f, new Color(HIGHLIGHT_COLOR.getRed(), HIGHLIGHT_COLOR.getGreen(), HIGHLIGHT_COLOR.getBlue(), (int)(60.0f * smoothProgress)).getRGB());
                    } else {
                        boolean isOptHovered = (float)mouseX >= posX + 4.0f && (float)mouseX <= posX + PANEL_WIDTH - 4.0f && (float)mouseY >= optionY && (float)mouseY <= optionY + PANEL_HEIGHT - 2.0f;
                        if (isOptHovered) {
                            com.eclipseware.imnotcheatingyouare.client.utils.ShaderManager.drawRoundedRect(matrix, posX + 4.0f, optionY, PANEL_WIDTH - 8.0f, PANEL_HEIGHT - 2.0f, 3.5f, new Color(255, 255, 255, (int)(25.0f * smoothProgress)).getRGB());
                        }
                    }
                    font.render(context.pose(), mode, posX + 8.0f, optionY + (PANEL_HEIGHT - 2.0f) / 2.0f - font.getLineHeight() / 2.0f + 1.0f, 6.0f, isSelected ? 0xFFFFFFFF & 0xFFFFFF | (int)alpha << 24 : TEXT_DISABLED & 0xFFFFFF | (int)alpha << 24);
                    optionY += PANEL_HEIGHT - 2.0f;
                }
            }
        }
    }

    public float setAnimationDuration(boolean condition, float progress, float elapsed, float targetProgress) {
        progress = condition ? Math.min(targetProgress != -1.0f ? targetProgress : 1.0f, progress + elapsed) : Math.max(targetProgress != -1.0f ? targetProgress : 0.0f, progress - elapsed);
        return progress;
    }

    private String getKeyName(int key) {
        if (key == -1) return "NONE";
        if (key >= 0 && key <= 7) {
            if (key == 1) return "RMB";
            if (key == 2) return "MMB";
            return "MB" + (key + 1);
        }
        switch (key) {
            case 344: return "RSHIFT";
            case 340: return "LSHIFT";
            case 345: return "RCTRL";
            case 341: return "LCTRL";
            case 346: return "RALT";
            case 342: return "LALT";
            case 258: return "TAB";
            case 32: return "SPACE";
            case 257: return "ENTER";
            case 256: return "NONE";
        }
        String str = GLFW.glfwGetKeyName(key, 0);
        if (str == null) return "KEY " + key;
        return str.toUpperCase();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        Render2DEngine.activeContext = context;
        int currentWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int currentHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        if (currentWidth != this.lastScreenWidth || currentHeight != this.lastScreenHeight) {
            this.recenterPanels();
            this.lastScreenWidth = currentWidth;
            this.lastScreenHeight = currentHeight;
        }
        if (Minecraft.getInstance().level == null) {
            super.extractBackground(context, mouseX, mouseY, delta);
        }
        FontAtlas inter = Fonts.getInstance().getInterSemiBold();
        FontAtlas icons = Fonts.getInstance().getLucide();
        Matrix3x2fStack matrices = context.pose();
        if (Minecraft.getInstance().player != null) {
            inter.render(matrices, "HUD Editor", (float)Minecraft.getInstance().getWindow().getGuiScaledWidth() - inter.getWidth("HUD Editor", 8.0f) - 10.0f, 10.0f, 8.0f, Color.WHITE.getRGB());
        }
        context.fill(0, 0, context.guiWidth(), context.guiHeight(), 856164364);
        if (this.draggingSetting != null && this.selectedPanel != null) {
            Vector2f pos = this.selectedPanel.getPosition();
            float settingsOffset = (1.0f - this.selectedPanel.getSettingsAnimationProgress()) * PANEL_WIDTH;
            float settingsX = pos.x() + settingsOffset;
            float relativeX = (float)mouseX - (settingsX + SETTING_PADDING);
            float totalWidth = PANEL_WIDTH - SETTING_PADDING * 2.0f;
            float percentage = Math.max(0.0f, Math.min(1.0f, relativeX / totalWidth));
            double range = this.draggingSetting.getMax() - this.draggingSetting.getMin();
            double newValue = this.draggingSetting.getMin() + range * (double)percentage;
            this.draggingSetting.setValDouble(newValue);
        }
        boolean foundHoveredModule = false;
        boolean foundHoveredSetting = false;
        Setting nextHoveredSetting = null;
        long currentTime = System.currentTimeMillis();
        for (Panel panel : panels.values()) {
            if (panel.isDragging()) {
                panel.updatePosition(mouseX, mouseY);
            }
            if (!panel.isOpen()) continue;
            Vector2f pos = panel.getPosition();
            float moduleY = pos.y() + PANEL_HEIGHT + 2.0f - panel.getModuleScrollOffset();
            float moduleXOffset = -panel.getSettingsAnimationProgress() * PANEL_WIDTH;
            for (Module module : panel.getModules()) {
                if ((float)mouseX >= pos.x() + moduleXOffset && (float)mouseX <= pos.x() + PANEL_WIDTH + moduleXOffset && (float)mouseY >= moduleY && (float)mouseY <= moduleY + PANEL_HEIGHT && (float)mouseY >= pos.y() + PANEL_HEIGHT + 2.0f && (float)mouseY <= pos.y() + panel.getCurrentHeight()) {
                    foundHoveredModule = true;
                    if (this.hoveredModule != module) {
                        this.hoveredModule = module;
                        this.hoverStartTime = currentTime;
                        this.showingTooltip = false;
                    } else if (currentTime - this.hoverStartTime >= 1000L && !this.showingTooltip) {
                        this.showingTooltip = true;
                    }
                    break;
                }
                moduleY += PANEL_HEIGHT + 2.0f;
            }
        }
        if (!foundHoveredModule) {
            this.hoveredModule = null;
            this.showingTooltip = false;
        }
        Color themeHighlight = this.getHighlightColor();
        Color themeOutline = this.getOutlineColor();
        for (Panel panel : panels.values()) {
            if (panel.isOpen()) {
                float screenHeight;
                float contentHeight = (PANEL_HEIGHT + 2.0f) * (float)panel.getModules().size();
                float panelBottom = panel.getPosition().y() + contentHeight + PANEL_HEIGHT + 4.0f;
                if (panelBottom > (screenHeight = (float)Minecraft.getInstance().getWindow().getGuiScaledHeight())) {
                    float visibleHeight = screenHeight - panel.getPosition().y() - PANEL_HEIGHT - 4.0f;
                    panel.maxModuleScrollOffset = Math.max(0.0f, contentHeight - visibleHeight);
                } else {
                    panel.maxModuleScrollOffset = 0.0f;
                }
                panel.setModuleScrollOffset(Math.max(0.0f, Math.min(panel.getModuleScrollOffset(), panel.getMaxModuleScrollOffset())));
                if (panel.isShowingSettings() && this.selectedModule != null && this.selectedPanel == panel) {
                    float settingsContentHeight = PANEL_HEIGHT + 4.0f;
                    List<Setting> settings = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingsByMod(this.selectedModule);
                    for (Setting setting : settings) {
                        float settingHeight = this.getSettingHeight(setting, panel);
                        settingsContentHeight += settingHeight;
                    }
                    float visibleSettingsHeight = screenHeight - panel.getPosition().y() - PANEL_HEIGHT - 4.0f;
                    panel.maxSettingScrollOffset = Math.max(0.0f, settingsContentHeight - visibleSettingsHeight);
                    panel.settingScrollOffset = Math.max(0.0f, Math.min(panel.settingScrollOffset, panel.maxSettingScrollOffset));
                }
            }
            if (panel.isShowingSettings()) {
                if (panel.getSettingsAnimationProgress() < 1.0f) {
                    float elapsed = (float)(currentTime - panel.lastSettingsAnimationTime) / 100.0f;
                    panel.setSettingsAnimationProgress(Math.min(1.0f, panel.getSettingsAnimationProgress() + elapsed));
                    panel.lastSettingsAnimationTime = currentTime;
                }
            } else if (panel.getSettingsAnimationProgress() > 0.0f) {
                float elapsed = (float)(currentTime - panel.lastSettingsAnimationTime) / 100.0f;
                panel.setSettingsAnimationProgress(Math.max(0.0f, panel.getSettingsAnimationProgress() - elapsed));
                panel.lastSettingsAnimationTime = currentTime;
            }
            float targetHeight = PANEL_HEIGHT;
            if (panel.isOpen()) {
                if (panel.isShowingSettings() && this.selectedModule != null && this.selectedPanel == panel) {
                    targetHeight += PANEL_HEIGHT + 2.0f;
                    List<Setting> settings = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingsByMod(this.selectedModule);
                    targetHeight += PANEL_HEIGHT + 4.0f;
                    for (Setting setting : settings) {
                        float extraHeight = this.getSettingHeight(setting, panel);
                        targetHeight += extraHeight;
                    }
                } else {
                    targetHeight += (PANEL_HEIGHT + 2.0f) * (float)panel.getModules().size();
                }
            }
            panel.updateHeight(targetHeight += 2.0f);
            panel.animateHeight();
            Vector2f position = panel.getPosition();
            String icon = this.getCategoryIcon(panel.getCategory());
            float posX = position.x();
            float posY = position.y();
            float panelHeight = panel.getCurrentHeight();
            org.joml.Matrix4f matrix = new org.joml.Matrix4f(
                context.pose().m00, context.pose().m01, 0.0f, 0.0f,
                context.pose().m10, context.pose().m11, 0.0f, 0.0f,
                0.0f, 0.0f, 1.0f, 0.0f,
                context.pose().m20, context.pose().m21, 0.0f, 1.0f
            );
            com.eclipseware.imnotcheatingyouare.client.utils.ShaderManager.drawRoundedRect(matrix, posX, posY, PANEL_WIDTH, panelHeight, 5.0f, BACKGROUND_COLOR.getRGB());
            com.eclipseware.imnotcheatingyouare.client.utils.ShaderManager.drawRoundedRect(matrix, posX, posY, PANEL_WIDTH, 6.0f, 5.0f, HIGHLIGHT_COLOR.getRGB());
            context.fill((int)posX, (int)(posY + 3.0f), (int)(posX + PANEL_WIDTH), (int)(posY + 6.0f), BACKGROUND_COLOR.getRGB());
            com.eclipseware.imnotcheatingyouare.client.utils.ShaderManager.drawRoundedOutline(matrix, posX, posY, PANEL_WIDTH, panelHeight, 5.0f, 1.0f, themeOutline.getRGB());
            inter.render(matrices, panel.getCategory().name(), posX + 6.0f, posY + (PANEL_HEIGHT / 2.0f) - inter.getLineHeight(TITLE_TEXT_SIZE) / 2.5f, TITLE_TEXT_SIZE, -1);
            float iconWidth = icons.getWidth(icon, 8.0f);
            icons.render(matrices, icon, posX + PANEL_WIDTH - 6.0f - iconWidth, posY + (PANEL_HEIGHT / 2.0f) - inter.getLineHeight(TITLE_TEXT_SIZE) / 2.5f, TITLE_TEXT_SIZE, -1);
            float moduleY = posY + PANEL_HEIGHT + 2.0f - panel.getModuleScrollOffset();
            if (panel.isOpen()) {
                context.enableScissor((int)posX + 1, (int)(posY + PANEL_HEIGHT + 2.0f), (int)(posX + PANEL_WIDTH - 1.0f), (int)(posY + panelHeight));
                float settingsOffset = (1.0f - this.easeInOutCubic(panel.getSettingsAnimationProgress())) * PANEL_WIDTH;
                float moduleXOffset = -this.easeInOutCubic(panel.getSettingsAnimationProgress()) * PANEL_WIDTH;
                if (!panel.isShowingSettings() || panel.getSettingsAnimationProgress() < 1.0f) {
                    for (Module module : panel.getModules()) {
                        if (moduleY + PANEL_HEIGHT >= posY + PANEL_HEIGHT + 2.0f && moduleY <= posY + panelHeight) {
                            String bind;
                            boolean isHovered = (float)mouseX >= posX + 2.0f + moduleXOffset && (float)mouseX <= posX + PANEL_WIDTH - 2.0f + moduleXOffset && (float)mouseY >= moduleY && (float)mouseY <= moduleY + PANEL_HEIGHT;
                            if (isHovered) {
                                com.eclipseware.imnotcheatingyouare.client.utils.ShaderManager.drawRoundedRect(matrix, posX + 1.0f + moduleXOffset, moduleY, PANEL_WIDTH - 2.0f, PANEL_HEIGHT, 3.0f, new Color(255, 255, 255, 12).getRGB());
                            }
                            bind = this.bindingModule == module ? "..." : this.getKeyName(module.getKeyBind());
                            List<Setting> settings = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingsByMod(module);
                            boolean hasSettings = !settings.isEmpty();
                            if (!bind.equals("NONE") || this.bindingModule == module) {
                                float bindLength = inter.getWidth(bind, 6.0f);
                                float bindX = posX + moduleXOffset + PANEL_WIDTH - bindLength - (hasSettings ? 16.0f : 8.0f);
                                inter.render(matrices, bind, bindX, moduleY + (PANEL_HEIGHT / 2.0f) - inter.getLineHeight(6.0f) / 2.0f, 6.0f, TEXT_SECONDARY);
                            }
                            inter.render(matrices, module.getName(), posX + 6.0f + moduleXOffset, moduleY + (PANEL_HEIGHT / 2.0f) - inter.getLineHeight() / 2.0f, 8.0f, module.isEnabled() ? TEXT_PRIMARY : TEXT_SECONDARY);
                            if (hasSettings) {
                                icons.render(matrices, "\ue071", posX + moduleXOffset + PANEL_WIDTH - 14.0f, moduleY + (PANEL_HEIGHT / 2.0f) - inter.getLineHeight() / 2.0f, 10.0f, module.isEnabled() ? TEXT_PRIMARY : TEXT_SECONDARY);
                            }
                        }
                        moduleY += PANEL_HEIGHT + 2.0f;
                    }
                }
                if (panel.isShowingSettings() && this.selectedModule != null && this.selectedPanel == panel) {
                    moduleY = posY + PANEL_HEIGHT + 2.0f - panel.settingScrollOffset;
                    float settingsX = posX + settingsOffset;
                    inter.render(matrices, this.selectedModule.getName(), settingsX + 6.0f, moduleY + (PANEL_HEIGHT / 2.0f) - inter.getLineHeight() / 2.0f, 7.0f, this.selectedModule.isEnabled() ? TEXT_PRIMARY : TEXT_SECONDARY);
                    String backArrow = "\ue0ab";
                    float arrowWidth = icons.getWidth(backArrow, 8.0f);
                    icons.render(matrices, backArrow, settingsX + PANEL_WIDTH - 10.0f - arrowWidth, moduleY + (PANEL_HEIGHT / 2.0f) - inter.getLineHeight() / 2.0f, 8.0f, TEXT_DISABLED);
                    context.fill((int)(settingsX + 4.0f), (int)(moduleY + PANEL_HEIGHT), (int)(settingsX + PANEL_WIDTH - 4.0f), (int)(moduleY + PANEL_HEIGHT + 1.0f), new Color(100, 100, 100, 100).getRGB());
                    moduleY += PANEL_HEIGHT + 4.0f;
                    List<Setting> settings = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingsByMod(this.selectedModule);
                    context.enableScissor((int)settingsX + 1, (int)(posY + PANEL_HEIGHT + 2.0f), (int)(settingsX + PANEL_WIDTH - 1.0f), (int)(posY + panelHeight));
                    for (Setting setting : settings) {
                        float settingHeight = this.getSettingHeight(setting, panel);
                        if (moduleY + settingHeight >= posY + PANEL_HEIGHT + 2.0f && moduleY <= posY + panelHeight) {
                            this.renderSetting(setting, context, new Vector2f(settingsX, posY), moduleY, inter, mouseX, mouseY);
                            boolean isSettingHovered = (float)mouseX >= settingsX + 4.0f && (float)mouseX <= settingsX + PANEL_WIDTH - 4.0f && (float)mouseY >= moduleY && (float)mouseY <= moduleY + settingHeight && (float)mouseY >= posY + PANEL_HEIGHT + 2.0f && (float)mouseY <= posY + panelHeight;
                            if (isSettingHovered) {
                                foundHoveredSetting = true;
                                nextHoveredSetting = setting;
                            }
                        }
                        moduleY += settingHeight;
                    }
                    context.disableScissor();
                }
                context.disableScissor();
            }
        }
        if (foundHoveredSetting && nextHoveredSetting != null) {
            if (this.hoveredSetting != nextHoveredSetting) {
                this.hoveredSetting = nextHoveredSetting;
                this.settingHoverStartTime = currentTime;
                this.showingSettingTooltip = false;
            } else if (currentTime - this.settingHoverStartTime >= 500L && !this.showingSettingTooltip) {
                this.showingSettingTooltip = true;
            }
        } else {
            this.hoveredSetting = null;
            this.showingSettingTooltip = false;
        }
        if (this.showingTooltip && this.hoveredModule != null && this.hoveredModule.getDescription() != null) {
            String description = this.hoveredModule.getDescription();
            float tooltipWidth = inter.getWidth(description, 7.0f) + 12.0f;
            float tooltipHeight = inter.getLineHeight(7.0f) + 8.0f;
            float tooltipX = Math.min((float)(mouseX + 10), (float)Minecraft.getInstance().getWindow().getGuiScaledWidth() - tooltipWidth - 5.0f);
            float tooltipY = Math.min((float)(mouseY + 10), (float)Minecraft.getInstance().getWindow().getGuiScaledHeight() - tooltipHeight - 5.0f);
            org.joml.Matrix4f matrix = new org.joml.Matrix4f(
                context.pose().m00, context.pose().m01, 0.0f, 0.0f,
                context.pose().m10, context.pose().m11, 0.0f, 0.0f,
                0.0f, 0.0f, 1.0f, 0.0f,
                context.pose().m20, context.pose().m21, 0.0f, 1.0f
            );
            com.eclipseware.imnotcheatingyouare.client.utils.ShaderManager.drawRoundedRect(matrix, tooltipX, tooltipY, tooltipWidth, tooltipHeight, 4.0f, BACKGROUND_COLOR.getRGB());
            com.eclipseware.imnotcheatingyouare.client.utils.ShaderManager.drawRoundedOutline(matrix, tooltipX, tooltipY, tooltipWidth, tooltipHeight, 4.0f, 1.0f, themeOutline.getRGB());
            inter.render(matrices, description, tooltipX + 4.0f, tooltipY + 4.0f, 7.0f, -1);
        }
        if (this.showingSettingTooltip && this.hoveredSetting != null) {
            String text = this.hoveredSetting.getName();
            float tooltipWidth = inter.getWidth(text, 7.0f) + 12.0f;
            float tooltipHeight = inter.getLineHeight(7.0f) + 8.0f;
            float tooltipX = Math.min((float)(mouseX + 10), (float)Minecraft.getInstance().getWindow().getGuiScaledWidth() - tooltipWidth - 5.0f);
            float tooltipY = Math.min((float)(mouseY + 10), (float)Minecraft.getInstance().getWindow().getGuiScaledHeight() - tooltipHeight - 5.0f);
            org.joml.Matrix4f matrix = new org.joml.Matrix4f(
                context.pose().m00, context.pose().m01, 0.0f, 0.0f,
                context.pose().m10, context.pose().m11, 0.0f, 0.0f,
                0.0f, 0.0f, 1.0f, 0.0f,
                context.pose().m20, context.pose().m21, 0.0f, 1.0f
            );
            com.eclipseware.imnotcheatingyouare.client.utils.ShaderManager.drawRoundedRect(matrix, tooltipX, tooltipY, tooltipWidth, tooltipHeight, 4.0f, BACKGROUND_COLOR.getRGB());
            com.eclipseware.imnotcheatingyouare.client.utils.ShaderManager.drawRoundedOutline(matrix, tooltipX, tooltipY, tooltipWidth, tooltipHeight, 4.0f, 1.0f, themeOutline.getRGB());
            inter.render(matrices, text, tooltipX + 4.0f, tooltipY + 4.0f, 7.0f, -1);
        }
        Render2DEngine.activeContext = null;
    }

    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();
        if (this.bindingModule != null) {
            this.bindingModule.setKeyBind(button);
            this.bindingModule = null;
            return true;
        }
        if (button == 0) {
            if (Minecraft.getInstance().player != null) {
                FontAtlas font = Fonts.getInstance().getInterSemiBold();
                String hudEditorText = "HUD Editor";
                float hudEditorWidth = font.getWidth(hudEditorText, 8.0f);
                float hudEditorX = (float)Minecraft.getInstance().getWindow().getGuiScaledWidth() - hudEditorWidth - 10.0f;
                float hudEditorY = 10.0f;
                if (mouseX >= (double)hudEditorX && mouseX <= (double)(hudEditorX + hudEditorWidth) && mouseY >= (double)hudEditorY && mouseY <= (double)(hudEditorY + font.getLineHeight(8.0f))) {
                    Minecraft.getInstance().setScreenAndShow((Screen)new HudEditorScreen());
                    Module menu = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("Menu");
                    if (menu != null) menu.onDisable();
                    return true;
                }
            }
            for (Panel panel : panels.values()) {
                Vector2f pos = panel.getPosition();
                if (mouseX >= (double)pos.x() && mouseX <= (double)(pos.x() + PANEL_WIDTH) && mouseY >= (double)pos.y() && mouseY <= (double)(pos.y() + PANEL_HEIGHT)) {
                    panel.startDragging(mouseX, mouseY);
                    return true;
                }
                if (!panel.isOpen()) continue;
                float moduleY = pos.y() + PANEL_HEIGHT + 2.0f;
                float adjustedMouseY = (float)mouseY + panel.getModuleScrollOffset();
                if (!panel.isShowingSettings()) {
                    float moduleXOffset = -panel.getSettingsAnimationProgress() * PANEL_WIDTH;
                    for (Module module : panel.getModules()) {
                        if (mouseX >= (double)(pos.x() + moduleXOffset) && mouseX <= (double)(pos.x() + PANEL_WIDTH + moduleXOffset) && adjustedMouseY >= moduleY && adjustedMouseY <= moduleY + PANEL_HEIGHT && mouseY >= (double)(pos.y() + PANEL_HEIGHT + 2.0f) && mouseY <= (double)(pos.y() + panel.getCurrentHeight())) {
                            module.toggle();
                            return true;
                        }
                        moduleY += PANEL_HEIGHT + 2.0f;
                    }
                } else if (this.selectedModule != null && this.selectedPanel == panel) {
                    float settingsOffset = (1.0f - panel.getSettingsAnimationProgress()) * PANEL_WIDTH;
                    float settingsX = pos.x() + settingsOffset;
                    float adjustedSettingsMouseY = (float)mouseY + panel.settingScrollOffset;
                    if (mouseX >= (double)settingsX && mouseX <= (double)(settingsX + PANEL_WIDTH) && mouseY >= (double)moduleY && mouseY <= (double)(moduleY + PANEL_HEIGHT)) {
                        panel.setShowingSettings(false);
                        panel.lastSettingsAnimationTime = System.currentTimeMillis();
                        this.selectedModule = null;
                        this.selectedPanel = null;
                        return true;
                    }
                    moduleY += PANEL_HEIGHT + 4.0f;
                    List<Setting> settings = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingsByMod(this.selectedModule);
                    for (Setting setting : settings) {
                        float settingHeight = this.getSettingHeight(setting, panel);
                        if (mouseX >= (double)(settingsX + 4.0f) && mouseX <= (double)(settingsX + PANEL_WIDTH - 4.0f) && adjustedSettingsMouseY >= moduleY && adjustedSettingsMouseY <= moduleY + settingHeight && mouseY >= (double)(pos.y() + PANEL_HEIGHT + 2.0f) && mouseY <= (double)(pos.y() + panel.getCurrentHeight())) {
                            if (setting.isCheck()) {
                                float toggleY = moduleY + (PANEL_HEIGHT - TOGGLE_HEIGHT) / 2.0f;
                                if (mouseX >= (double)(settingsX + PANEL_WIDTH - TOGGLE_WIDTH - SETTING_PADDING - 2.0f) && mouseX <= (double)(settingsX + PANEL_WIDTH - SETTING_PADDING + 2.0f) && adjustedSettingsMouseY >= toggleY && adjustedSettingsMouseY <= toggleY + TOGGLE_HEIGHT) {
                                    setting.setValBoolean(!setting.getValBoolean());
                                    panel.booleanAnimationTimes.put(setting, System.currentTimeMillis());
                                    return true;
                                }
                            } else if (setting.isSlider()) {
                                float sliderY = moduleY + PANEL_HEIGHT - 1.5f;
                                if (adjustedSettingsMouseY >= sliderY - 5.0f && adjustedSettingsMouseY <= sliderY + 7.0f) {
                                    this.draggingSetting = setting;
                                    float relativeX = (float)(mouseX - (double)(settingsX + SETTING_PADDING));
                                    float totalWidth = PANEL_WIDTH - SETTING_PADDING * 2.0f;
                                    float percentage = Math.max(0.0f, Math.min(1.0f, relativeX / totalWidth));
                                    double range = setting.getMax() - setting.getMin();
                                    double newValue = setting.getMin() + range * (double)percentage;
                                    setting.setValDouble(newValue);
                                    return true;
                                }
                            } else if (setting.isCombo() && adjustedSettingsMouseY <= moduleY + PANEL_HEIGHT) {
                                panel.setOpenSetting(panel.getOpenSetting() == setting ? null : setting);
                                return true;
                            }
                        }
                        if (setting.isCombo() && panel.getOpenSetting() == setting && setting.getOptions() != null) {
                            float optionY = moduleY + PANEL_HEIGHT;
                            for (String mode : setting.getOptions()) {
                                if (mouseX >= (double)(settingsX + 6.0f) && mouseX <= (double)(settingsX + PANEL_WIDTH - 6.0f) && adjustedSettingsMouseY >= optionY && adjustedSettingsMouseY <= optionY + PANEL_HEIGHT - 2.0f && mouseY >= (double)(pos.y() + PANEL_HEIGHT + 2.0f) && mouseY <= (double)(pos.y() + panel.getCurrentHeight())) {
                                    setting.setValString(mode);
                                    return true;
                                }
                                optionY += PANEL_HEIGHT - 2.0f;
                            }
                        }
                        moduleY += settingHeight;
                    }
                }
            }
        } else if (button == 1) {
            for (Panel panel : panels.values()) {
                Vector2f pos = panel.getPosition();
                if (mouseX >= (double)pos.x() && mouseX <= (double)(pos.x() + PANEL_WIDTH) && mouseY >= (double)pos.y() && mouseY <= (double)(pos.y() + PANEL_HEIGHT)) {
                    panel.setOpen(!panel.isOpen());
                    if (!panel.isOpen()) {
                        panel.setShowingSettings(false);
                        if (this.selectedPanel == panel) {
                            this.selectedModule = null;
                            this.selectedPanel = null;
                        }
                    }
                    return true;
                }
                if (!panel.isOpen()) continue;
                float moduleY = pos.y() + PANEL_HEIGHT + 2.0f;
                float moduleXOffset = -panel.getSettingsAnimationProgress() * PANEL_WIDTH;
                float adjustedMouseY = (float)mouseY + panel.getModuleScrollOffset();
                if (panel.isShowingSettings() && this.selectedModule != null && this.selectedPanel == panel) {
                    float settingsOffset = (1.0f - panel.getSettingsAnimationProgress()) * PANEL_WIDTH;
                    float settingsX = pos.x() + settingsOffset;
                    if (mouseX >= (double)settingsX && mouseX <= (double)(settingsX + PANEL_WIDTH) && mouseY >= (double)moduleY && mouseY <= (double)(moduleY + PANEL_HEIGHT)) {
                        panel.setShowingSettings(false);
                        panel.lastSettingsAnimationTime = System.currentTimeMillis();
                        this.selectedModule = null;
                        this.selectedPanel = null;
                        return true;
                    }
                }
                for (Module module : panel.getModules()) {
                    if (mouseX >= (double)(pos.x() + moduleXOffset) && mouseX <= (double)(pos.x() + PANEL_WIDTH + moduleXOffset) && adjustedMouseY >= moduleY && adjustedMouseY <= moduleY + PANEL_HEIGHT && mouseY >= (double)(pos.y() + PANEL_HEIGHT + 2.0f) && mouseY <= (double)(pos.y() + panel.getCurrentHeight())) {
                        List<Setting> settings = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingsByMod(module);
                        if (!settings.isEmpty()) {
                            for (Panel otherPanel : panels.values()) {
                                if (otherPanel == panel) continue;
                                otherPanel.setShowingSettings(false);
                            }
                            panel.setShowingSettings(true);
                            panel.lastSettingsAnimationTime = System.currentTimeMillis();
                            this.selectedModule = module;
                            this.selectedPanel = panel;
                        }
                        return true;
                    }
                    moduleY += PANEL_HEIGHT + 2.0f;
                }
            }
        } else if (button == 2) {
            for (Panel panel : panels.values()) {
                Vector2f pos = panel.getPosition();
                if (!panel.isOpen() || panel.isShowingSettings()) continue;
                float moduleY = pos.y() + PANEL_HEIGHT + 2.0f;
                float moduleXOffset = -panel.getSettingsAnimationProgress() * PANEL_WIDTH;
                float adjustedMouseY = (float)mouseY + panel.getModuleScrollOffset();
                for (Module module : panel.getModules()) {
                    if (mouseX >= (double)(pos.x() + moduleXOffset) && mouseX <= (double)(pos.x() + PANEL_WIDTH + moduleXOffset) && adjustedMouseY >= moduleY && adjustedMouseY <= moduleY + PANEL_HEIGHT && mouseY >= (double)(pos.y() + PANEL_HEIGHT + 2.0f) && mouseY <= (double)(pos.y() + panel.getCurrentHeight())) {
                        this.bindingModule = module;
                        return true;
                    }
                    moduleY += PANEL_HEIGHT + 2.0f;
                }
            }
        }
        return super.mouseClicked(click, doubled);
    }

    public boolean mouseReleased(MouseButtonEvent click) {
        if (click.button() == 0) {
            for (Panel panel : panels.values()) {
                if (panel.isDragging()) {
                    panel.setDragging(false);
                    return true;
                }
            }
            if (this.draggingSetting != null) {
                this.draggingSetting = null;
                return true;
            }
        }
        return super.mouseReleased(click);
    }

    public boolean keyPressed(KeyEvent event) {
        int keyCode = event.input();
        if (this.bindingModule != null) {
            if (keyCode == 256) {
                this.bindingModule.setKeyBind(-1);
            } else {
                this.bindingModule.setKeyBind(keyCode);
            }
            this.bindingModule = null;
            return true;
        }
        if (keyCode == 256) {
            this.onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        for (Panel panel : panels.values()) {
            Vector2f pos = panel.getPosition();
            if (!panel.isOpen() || !(mouseX >= (double)pos.x()) || !(mouseX <= (double)(pos.x() + PANEL_WIDTH)) || !(mouseY >= (double)pos.y()) || !(mouseY <= (double)(pos.y() + panel.getCurrentHeight()))) continue;
            if (panel.isShowingSettings() && this.selectedModule != null && this.selectedPanel == panel) {
                float settingsOffset = (1.0f - panel.getSettingsAnimationProgress()) * PANEL_WIDTH;
                float settingsX = pos.x() + settingsOffset;
                if (mouseX >= (double)settingsX && mouseX <= (double)(settingsX + PANEL_WIDTH)) {
                    float newSettingScrollOffset = panel.settingScrollOffset - (float)(verticalAmount * 10.0);
                    panel.settingScrollOffset = Math.max(0.0f, Math.min(panel.maxSettingScrollOffset, newSettingScrollOffset));
                    return true;
                }
            }
            if (panel.maxModuleScrollOffset > 0.0f) {
                float newModuleScrollOffset = panel.getModuleScrollOffset() - (float)(verticalAmount * 10.0);
                panel.setModuleScrollOffset(Math.max(0.0f, Math.min(panel.getMaxModuleScrollOffset(), newModuleScrollOffset)));
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    public void removed() {
        this.selectedModule = null;
        this.selectedPanel = null;
        for (Panel panel : panels.values()) {
            panel.setShowingSettings(false);
            panel.setSettingsAnimationProgress(0.0f);
            panel.setOpenSetting(null);
        }
        Module menu = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("Menu");
        if (menu != null && menu.isToggled()) {
            menu.onDisable();
        }
        super.removed();
    }

    public boolean isPauseScreen() { return false; }
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {}
    private float easeInOutCubic(float t) {
        return (float)((double)t < 0.5 ? (double)(4.0f * t * t * t) : 1.0 - Math.pow(-2.0f * t + 2.0f, 3.0) / 2.0);
    }

    public static class Panel {
        private final Category category;
        private Vector2f position;
        private boolean isOpen;
        private boolean isDragging;
        private double dragX;
        private double dragY;
        private List<Module> modules;
        private boolean showingSettings;
        private Setting openSetting;
        private float settingsAnimationProgress = 0.0f;
        private long lastSettingsAnimationTime = 0L;
        private float currentHeight = PANEL_HEIGHT;
        private float targetHeight = PANEL_HEIGHT;
        private long lastHeightAnimationTime = System.currentTimeMillis();
        private float animationProgress = 0.0f;
        private float moduleScrollOffset = 0.0f;
        private float maxModuleScrollOffset = 0.0f;
        private float settingScrollOffset = 0.0f;
        private float maxSettingScrollOffset = 0.0f;
        private final Map<Setting, Float> booleanAnimations = new HashMap<>();
        private final Map<Setting, Long> booleanAnimationTimes = new HashMap<>();
        private final Map<Setting, Float> modeAnimations = new HashMap<>();
        private final Map<Setting, Long> modeAnimationTimes = new HashMap<>();
        private final Map<Setting, Float> numberAnimations = new HashMap<>();
        private final Map<Setting, Long> numberAnimationTimes = new HashMap<>();

        public Category getCategory() { return this.category; }
        public Vector2f getPosition() { return this.position; }
        public void setPosition(Vector2f pos) { this.position = pos; }
        public boolean isOpen() { return this.isOpen; }
        public void setOpen(boolean val) { this.isOpen = val; }
        public boolean isDragging() { return this.isDragging; }
        public void setDragging(boolean val) { this.isDragging = val; }
        public List<Module> getModules() { return this.modules; }
        public void setModules(List<Module> val) { this.modules = val; }
        public boolean isShowingSettings() { return this.showingSettings; }
        public Setting getOpenSetting() { return this.openSetting; }
        public void setOpenSetting(Setting val) { this.openSetting = val; }
        public float getSettingsAnimationProgress() { return this.settingsAnimationProgress; }
        public void setSettingsAnimationProgress(float val) { this.settingsAnimationProgress = val; }
        public float getCurrentHeight() { return this.currentHeight; }
        public void setCurrentHeight(float val) { this.currentHeight = val; }
        public float getModuleScrollOffset() { return this.moduleScrollOffset; }
        public void setModuleScrollOffset(float val) { this.moduleScrollOffset = val; }
        public float getMaxModuleScrollOffset() { return this.maxModuleScrollOffset; }

        public Panel(Category category, boolean isOpen, Vector2f position) {
            this.category = category;
            this.position = position;
            this.isOpen = isOpen;
            this.isDragging = false;
            this.showingSettings = false;
            this.openSetting = null;
            this.modules = new ArrayList<>();
            this.moduleScrollOffset = 0.0f;
            this.maxModuleScrollOffset = 0.0f;
            this.settingScrollOffset = 0.0f;
            this.maxSettingScrollOffset = 0.0f;
        }

        public void updateHeight(float newTargetHeight) {
            if (this.targetHeight != newTargetHeight) {
                this.targetHeight = newTargetHeight;
                this.lastHeightAnimationTime = System.currentTimeMillis();
                this.animationProgress = 0.0f;
            }
        }

        public void animateHeight() {
            if (this.currentHeight != this.targetHeight) {
                long currentTime = System.currentTimeMillis();
                float elapsed = Math.min(1.0f, (float)(currentTime - this.lastHeightAnimationTime) / 500.0f);
                this.lastHeightAnimationTime = currentTime;
                this.animationProgress = Math.min(1.0f, this.animationProgress + elapsed);
                float smoothProgress = (float)(1.0 - Math.pow(1.0f - this.animationProgress, 3.0));
                this.currentHeight += (this.targetHeight - this.currentHeight) * smoothProgress;
                if (Math.abs(this.currentHeight - this.targetHeight) < 0.01f) {
                    this.currentHeight = this.targetHeight;
                }
            }
        }

        public void setShowingSettings(boolean showingSettings) {
            if (this.showingSettings != showingSettings) {
                this.showingSettings = showingSettings;
                this.lastSettingsAnimationTime = System.currentTimeMillis();
                this.animationProgress = 0.0f;
                this.lastHeightAnimationTime = System.currentTimeMillis();
                this.settingScrollOffset = 0.0f;
                this.maxSettingScrollOffset = 0.0f;
            }
        }

        public void startDragging(double mouseX, double mouseY) {
            this.isDragging = true;
            this.dragX = mouseX - (double)this.position.x();
            this.dragY = mouseY - (double)this.position.y();
        }

        public void updatePosition(double mouseX, double mouseY) {
            if (this.isDragging) {
                float newX = (float)(mouseX - this.dragX);
                float newY = (float)(mouseY - this.dragY);
                float screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
                float screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
                newX = Math.max(0.0f, Math.min(screenWidth - PANEL_WIDTH, newX));
                newY = Math.max(0.0f, Math.min(screenHeight - PANEL_HEIGHT, newY));
                this.position = new Vector2f(newX, newY);
            }
        }
    }
}
