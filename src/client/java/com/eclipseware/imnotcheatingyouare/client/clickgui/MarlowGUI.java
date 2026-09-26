package com.eclipseware.imnotcheatingyouare.client.clickgui;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.utils.RenderUtils;
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
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.joml.Matrix3x2fStack;
import org.joml.Vector2f;
import org.lwjgl.glfw.GLFW;

public class MarlowGUI extends Screen {
  private static final float PANEL_WIDTH = 120.0F;
  private static final float PANEL_HEIGHT = 17.5F;
  private static final float ANIMATION_DURATION = 500.0F;
  private static final float TOGGLE_WIDTH = 18.0F;
  private static final float TOGGLE_HEIGHT = 9.0F;
  private static final float KNOB_SIZE = 6.0F;
  private static final float PANEL_SPACING = 8.0F;
  private static final float INITIAL_Y = 35.0F;
  private static final float SETTING_PADDING = 6.0F;
  private static final float SETTING_TEXT_SIZE = 6.0F;
  private static final float TITLE_TEXT_SIZE = 8.0F;
  private static final float SLIDER_THICKNESS = 2.0F;
  private static final float OUTLINE_THICKNESS = 1.0F;
  private static final float TOGGLE_KNOB_OFFSET = 1.5F;
  private static final float TOOLTIP_DELAY = 1000.0F;
  private static final float TOOLTIP_PADDING = 4.0F;
  private static final float TOOLTIP_TEXT_SIZE = 7.0F;
  private static final Color HIGHLIGHT_COLOR = new Color(180, 140, 115);
  private static final Color DISABLED_COLOR = new Color(48, 40, 36, 200);
  private static final Color BACKGROUND_COLOR = new Color(25, 21, 19, 230);
  private static final Color OUTLINE_COLOR = new Color(60, 48, 42, 180);
  private static final Color TOGGLE_KNOB_COLOR = new Color(255, 255, 255, 200);
  
  private static final int TEXT_PRIMARY = -1;
  private static final int TEXT_SECONDARY = -5592406;
  private static final int TEXT_DISABLED = -7303024;
  private Module selectedModule;
  private Panel selectedPanel;
  private Setting focusedSetting = null;
  private Setting editingSlider = null;
  private String sliderInputBuffer = "";
  private long cursorBlinkTime = 0L;
  private Module hoveredModule;
  private long hoverStartTime;
  private boolean showingTooltip;
  private Setting hoveredSetting;
  private long settingHoverStartTime;
  private boolean showingSettingTooltip;
  private Setting draggingSetting = null;
  private int draggingColorBar = -1;
  private Module bindingModule = null;
  public static final Map<Category, Panel> panels = new HashMap<>();
  private int lastScreenWidth = -1;
  private int lastScreenHeight = -1;
  private long openTime = 0L;
  private String searchQuery = "";
  private boolean searchActive = false;
  
  public Module getSelectedModule() { return this.selectedModule; }
  public void setSelectedModule(Module val) { this.selectedModule = val; }
  public Panel getSelectedPanel() { return this.selectedPanel; } public void setSelectedPanel(Panel val) {
    this.selectedPanel = val;
  }
  public MarlowGUI() {
    super((Component)Component.literal("ClickGui"));
    layoutPanels();
  }

  
  protected void init() {
    super.init();
    layoutPanels();
    this.openTime = System.currentTimeMillis();
    this.selectedModule = null;
    this.selectedPanel = null;
    this.hoveredSetting = null;
    this.editingSlider = null;
    this.sliderInputBuffer = "";
    this.showingSettingTooltip = false;
    for (Panel panel : panels.values()) {
      panel.setShowingSettings(false);
      panel.setSettingsAnimationProgress(0.0F);
      panel.setOpenSetting(null);
    } 
  }
  
  private void layoutPanels() {
    float screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
    if (screenWidth <= 0.0F) screenWidth = 960.0F; 

    float panelWidth = 120.0F;
    float spacing = 8.0F;
    float startX = 10.0F;
    float startY = 35.0F;

    int maxCols = Math.max(1, (int) ((screenWidth - 20.0F) / (panelWidth + spacing)));

    int col = 0;
    int row = 0;

    for (Category category : Category.values()) {
      List<Module> modules = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModules(category);
      if (!modules.isEmpty()) {
        Panel panel = panels.get(category);
        if (panel == null) {
          float x = startX + col * (panelWidth + spacing);
          float y = startY + (row * 30.0F);
          panel = new Panel(category, true, new Vector2f(x, y));
          panels.put(category, panel);

          col++;
          if (col >= maxCols) {
            col = 0;
            row++;
          }
        }
        panel.setModules(modules);
      }
    }
  }
  private List<Module> getFilteredModules(Panel panel) {
    if (this.searchQuery == null || this.searchQuery.isEmpty()) {
      return panel.getModules();
    }
    List<Module> filtered = new ArrayList<>();
    String query = this.searchQuery.toLowerCase();
    for (Module m : panel.getModules()) {
      if (m.getName().toLowerCase().contains(query) || (m
        .getDescription() != null && m.getDescription().toLowerCase().contains(query))) {
        filtered.add(m);
      }
    } 
    return filtered;
  }
  private void recenterPanels() {}
  
  private Color getHighlightColor() {
    Color c = RenderUtils.getThemeAccentColor();
    if (c.getRed() == 155 && c.getGreen() == 60 && c.getBlue() == 255) {
      return HIGHLIGHT_COLOR;
    }
    return c;
  }
  private Color getOutlineColor() {
    Color c = RenderUtils.getThemeAccentColor();
    if (c.getRed() == 155 && c.getGreen() == 60 && c.getBlue() == 255) {
      return OUTLINE_COLOR;
    }
    return c;
  }
  
  private String getCategoryIcon(Category category) {
    switch (category) { case Combat: case Crystal: case Movement: case Render: case HUD: case Utility: case Client:  }  return 




      
      "";
  }

  
  private float getSettingHeight(Setting setting, Panel panel) {
    float height = 17.5F;
    if (setting.isCombo()) {
      if (panel.getOpenSetting() == setting && setting.getOptions() != null) {
        height += 15.5F * setting.getOptions().size();
      }
    } else if (setting.isSlider()) {
      height += 4.0F;
    } else if (setting.isColor()) {
      if (panel.getOpenSetting() == setting) {
        height += 30.0F;
      }
    }
    return height;
  }
  
  private void renderSetting(Setting setting, GuiGraphicsExtractor context, Vector2f position, float moduleY, FontAtlas font, int mouseX, int mouseY) {
    float posX = position.x();
    float maxTextWidth = 98.0F;
    if (setting.isCheck()) {
      maxTextWidth = 84.0F;
    } else if (setting.isSlider()) {
      DecimalFormat format = new DecimalFormat("#.##");
      String valStr = format.format(setting.getValDouble());
      float valWidth = font.getWidth(valStr, 6.0F);
      maxTextWidth = 108.0F - valWidth - 8.0F;
    } else if (setting.isCombo()) {
      String valStr = setting.getValString();
      float valWidth = font.getWidth(valStr, 6.0F);
      maxTextWidth = 108.0F - valWidth - 10.0F;
    } else if (setting.isText()) {
      String valStr = setting.getValText();
      float valWidth = font.getWidth(valStr, 6.0F);
      maxTextWidth = 108.0F - valWidth - 10.0F;
    } 
    Object result = setting.getName();
    if (result != null && font.getWidth((String)result, 6.0F) > maxTextWidth) {
      while (((String)result).length() > 0 && font.getWidth((String)result + "...", 6.0F) > maxTextWidth) {
        result = ((String)result).substring(0, ((String)result).length() - 1);
      }
      result = (String)result + "...";
    } 
    font.render(context.pose(), (String)result, posX + 6.0F, moduleY + 8.75F - font.getLineHeight(6.0F) / 2.0F, 6.0F, -5592406);
    if (setting.isCheck()) {
      
      float smoothProgress, toggleY = moduleY + 4.25F;
      Panel panel = this.selectedPanel;
      float animationProgress = ((Float)panel.booleanAnimations.computeIfAbsent(setting, k -> Float.valueOf(setting.getValBoolean() ? 1.0F : 0.0F))).floatValue();
      long lastAnimationTime = ((Long)panel.booleanAnimationTimes.computeIfAbsent(setting, k -> Long.valueOf(System.currentTimeMillis()))).longValue();
      long currentTime = System.currentTimeMillis();
      float elapsed = Math.min(1.0F, (float)(currentTime - lastAnimationTime) / 500.0F * 3.0F);
      animationProgress = setAnimationDuration(setting.getValBoolean(), animationProgress, elapsed, -1.0F);
      if (animationProgress < 0.5F) {
        smoothProgress = 2.0F * animationProgress * animationProgress;
      } else {
        float t = -2.0F * animationProgress + 2.0F;
        smoothProgress = 1.0F - t * t / 2.0F;
      } 
      panel.booleanAnimations.put(setting, Float.valueOf(animationProgress));
      panel.booleanAnimationTimes.put(setting, Long.valueOf(currentTime));
      Color accent = getHighlightColor();
      Color toggleColor = new Color(DISABLED_COLOR.getRed() + (int)((accent.getRed() - DISABLED_COLOR.getRed()) * smoothProgress), DISABLED_COLOR.getGreen() + (int)((accent.getGreen() - DISABLED_COLOR.getGreen()) * smoothProgress), DISABLED_COLOR.getBlue() + (int)((accent.getBlue() - DISABLED_COLOR.getBlue()) * smoothProgress));
      float pillX = posX + 120.0F - 18.0F - 6.0F - 2.0F;
      Render2DEngine.drawRoundedRect(context.pose(), pillX, toggleY, 18.0F, 9.0F, 3.9130435F, toggleColor);
      float knobStartX = pillX + 1.0F;
      float knobEndX = pillX + 18.0F - 6.0F - 2.0F;
      float knobX = knobStartX + (knobEndX - knobStartX) * smoothProgress;
      float knobY = toggleY + 1.5F;
      Render2DEngine.drawRoundedRect(context.pose(), knobX, knobY, 6.0F, 6.0F, 3.0F, TOGGLE_KNOB_COLOR);
    } else if (setting.isSlider()) {
      float sliderY = moduleY + 17.5F - 1.5F;
      DecimalFormat format = new DecimalFormat("#.##");
      boolean isEditingThis = (this.editingSlider == setting);
      String value = isEditingThis ? this.sliderInputBuffer : format.format(setting.getValDouble());
      if (isEditingThis && (System.currentTimeMillis() / 400) % 2 == 0) {
        value += "|";
      }
      float valueWidth = font.getWidth(value, 6.0F);
      float textX = posX + 120.0F - 6.0F - 2.0F - valueWidth;
      float textY = moduleY + 8.75F - font.getLineHeight(6.0F) / 2.0F;

      if (isEditingThis) {
        Render2DEngine.drawRoundedRect(context.pose(), textX - 2.0F, textY - 1.0F, valueWidth + 4.0F, font.getLineHeight(6.0F) + 2.0F, 2.0F, new Color(180, 140, 115, 100));
      }
      font.render(context.pose(), value, textX, textY, 6.0F, isEditingThis ? -1 : -7303024);
      Render2DEngine.drawRoundedRect(context.pose(), posX + 6.0F, sliderY, 108.0F, 2.0F, 1.0F, DISABLED_COLOR);
      double progress = (setting.getValDouble() - setting.getMin()) / (setting.getMax() - setting.getMin());
      float targetProgress = Math.max(0.0F, Math.min(1.0F, (float)progress));
      Panel panel = this.selectedPanel;
      float animationProgress = ((Float)panel.numberAnimations.computeIfAbsent(setting, k -> Float.valueOf(targetProgress))).floatValue();
      long lastAnimationTime = ((Long)panel.numberAnimationTimes.computeIfAbsent(setting, k -> Long.valueOf(System.currentTimeMillis()))).longValue();
      long currentTime = System.currentTimeMillis();
      float elapsed = Math.min(1.0F, (float)(currentTime - lastAnimationTime) / 500.0F * 3.0F);
      animationProgress = setAnimationDuration((animationProgress < targetProgress), animationProgress, elapsed, targetProgress);
      panel.numberAnimations.put(setting, Float.valueOf(animationProgress));
      panel.numberAnimationTimes.put(setting, Long.valueOf(currentTime));
      Render2DEngine.drawRoundedRect(context.pose(), posX + 6.0F, sliderY, 108.0F * animationProgress, 2.0F, 1.0F, getHighlightColor());
      float knobX = posX + 6.0F + 108.0F * animationProgress - 3.0F;
      float knobY = sliderY - 2.0F;
      Render2DEngine.drawRoundedRect(context.pose(), knobX, knobY, 6.0F, 6.0F, 3.0F, TOGGLE_KNOB_COLOR);
    } else if (setting.isCombo()) {
      float smoothProgress;
      Panel panel = this.selectedPanel;
      boolean isOpen = (panel.getOpenSetting() == setting);
      float animationProgress = ((Float)panel.modeAnimations.computeIfAbsent(setting, k -> Float.valueOf(isOpen ? 1.0F : 0.0F))).floatValue();
      long lastAnimationTime = ((Long)panel.modeAnimationTimes.computeIfAbsent(setting, k -> Long.valueOf(System.currentTimeMillis()))).longValue();
      long currentTime = System.currentTimeMillis();
      float elapsed = Math.min(1.0F, (float)(currentTime - lastAnimationTime) / 500.0F * 3.0F);
      if ((animationProgress = setAnimationDuration(isOpen, animationProgress, elapsed, -1.0F)) < 0.5F) {
        smoothProgress = 2.0F * animationProgress * animationProgress;
      } else {
        float t = -2.0F * animationProgress + 2.0F;
        smoothProgress = 1.0F - t * t / 2.0F;
      } 
      panel.modeAnimations.put(setting, Float.valueOf(animationProgress));
      panel.modeAnimationTimes.put(setting, Long.valueOf(currentTime));
      String value = setting.getValString();
      float valueWidth = font.getWidth(value, 6.0F);
      font.render(context.pose(), value, posX + 120.0F - 6.0F - 2.0F - valueWidth, moduleY + 8.75F - font.getLineHeight(6.0F) / 2.0F, 6.0F, -7303024);
      if (animationProgress > 0.0F && setting.getOptions() != null) {
        float optionY = moduleY + 17.5F;
        for (String mode : setting.getOptions()) {
          boolean isSelected = mode.equals(setting.getValString());
          float alpha = (int)(smoothProgress * 255.0F);
          if (isSelected) {
            Render2DEngine.drawRoundedRect(context.pose(), posX + 4.0F, optionY, 112.0F, 15.5F, 3.5F, new Color(HIGHLIGHT_COLOR.getRed(), HIGHLIGHT_COLOR.getGreen(), HIGHLIGHT_COLOR.getBlue(), (int)(60.0F * smoothProgress)));
          } else {
            boolean isOptHovered = (mouseX >= posX + 4.0F && mouseX <= posX + 120.0F - 4.0F && mouseY >= optionY && mouseY <= optionY + 17.5F - 2.0F);
            if (isOptHovered) {
              Render2DEngine.drawRoundedRect(context.pose(), posX + 4.0F, optionY, 112.0F, 15.5F, 3.5F, new Color(255, 255, 255, (int)(25.0F * smoothProgress)));
            }
          } 
          font.render(context.pose(), mode, posX + 8.0F, optionY + 7.75F - font.getLineHeight() / 2.0F + 1.0F, 6.0F, isSelected ? (0xFFFFFF | (int)alpha << 24) : (0x909090 | (int)alpha << 24));
          optionY += 15.5F;
        } 
      } 
    } else if (setting.isText()) {
      String valStr = setting.getValText();
      if (this.focusedSetting == setting) {
        valStr = (System.currentTimeMillis() / 500L % 2L == 0L) ? (valStr + "_") : (valStr + " ");
      }
      float valueWidth = font.getWidth(valStr, 6.0F);
      font.render(context.pose(), valStr, posX + 120.0F - 6.0F - 2.0F - valueWidth, moduleY + 8.75F - font.getLineHeight(6.0F) / 2.0F, 6.0F, (this.focusedSetting == setting) ? getHighlightColor().getRGB() : -5592406);
    } else if (setting.isColor()) {
      int curColor = setting.getValColor();
      float rectX = posX + 120.0F - 6.0F - 2.0F - 10.0F;
      float rectY = moduleY + 8.75F - 4.0F;
      Render2DEngine.drawRoundedRect(context.pose(), rectX, rectY, 10.0F, 8.0F, 2.0F, new Color(curColor, true));
      
      Panel panel = this.selectedPanel;
      if (panel.getOpenSetting() == setting) {
        float pickerY = moduleY + 17.5F;
        float pickerW = 120.0F - 12.0F;
        float pickerH = 6.0F;
        float[] hsv = Color.RGBtoHSB((curColor >> 16) & 0xFF, (curColor >> 8) & 0xFF, curColor & 0xFF, null);
        
        for (int i = 0; i < (int)pickerW; i++) {
          float pct = (float)i / pickerW;
          int rgb = Color.HSBtoRGB(pct, 1.0F, 1.0F);
          context.fill((int)(posX + 6.0F + i), (int)pickerY, (int)(posX + 7.0F + i), (int)(pickerY + pickerH), rgb);
        }
        float hueKnobX = posX + 6.0F + hsv[0] * pickerW;
        context.fill((int)(hueKnobX - 1.0F), (int)(pickerY - 1.0F), (int)(hueKnobX + 1.0F), (int)(pickerY + pickerH + 1.0F), -1);
        
        pickerY += pickerH + 4.0F;
        for (int i = 0; i < (int)pickerW; i++) {
          float pct = (float)i / pickerW;
          int rgb = Color.HSBtoRGB(hsv[0], pct, hsv[2]);
          context.fill((int)(posX + 6.0F + i), (int)pickerY, (int)(posX + 7.0F + i), (int)(pickerY + pickerH), rgb);
        }
        float satKnobX = posX + 6.0F + hsv[1] * pickerW;
        context.fill((int)(satKnobX - 1.0F), (int)(pickerY - 1.0F), (int)(satKnobX + 1.0F), (int)(pickerY + pickerH + 1.0F), -1);
        
        pickerY += pickerH + 4.0F;
        for (int i = 0; i < (int)pickerW; i++) {
          float pct = (float)i / pickerW;
          int rgb = Color.HSBtoRGB(hsv[0], hsv[1], pct);
          context.fill((int)(posX + 6.0F + i), (int)pickerY, (int)(posX + 7.0F + i), (int)(pickerY + pickerH), rgb);
        }
        float valKnobX = posX + 6.0F + hsv[2] * pickerW;
        context.fill((int)(valKnobX - 1.0F), (int)(pickerY - 1.0F), (int)(valKnobX + 1.0F), (int)(pickerY + pickerH + 1.0F), -1);
      }
    }
  }
  
  public float setAnimationDuration(boolean condition, float progress, float elapsed, float targetProgress) {
    progress = condition ? Math.min((targetProgress != -1.0F) ? targetProgress : 1.0F, progress + elapsed) : Math.max((targetProgress != -1.0F) ? targetProgress : 0.0F, progress - elapsed);
    return progress;
  }
  
  private String getKeyName(int key) {
    if (key == -1) return "NONE"; 
    if (key >= 0 && key <= 7) {
      if (key == 1) return "RMB"; 
      if (key == 2) return "MMB"; 
      return "MB" + key + 1;
    } 
    switch (key) { case 344:
        return "RSHIFT";
      case 340: return "LSHIFT";
      case 345: return "RCTRL";
      case 341: return "LCTRL";
      case 346: return "RALT";
      case 342: return "LALT";
      case 258: return "TAB";
      case 32: return "SPACE";
      case 257: return "ENTER";
      case 256: return "NONE"; }
    
    String str = GLFW.glfwGetKeyName(key, 0);
    if (str == null) return "KEY " + key; 
    return str.toUpperCase();
  }

  
  public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
    Render2DEngine.activeContext = context;
    int currentWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
    int currentHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
    if (currentWidth != this.lastScreenWidth || currentHeight != this.lastScreenHeight) {
      recenterPanels();
      this.lastScreenWidth = currentWidth;
      this.lastScreenHeight = currentHeight;
    } 
    if ((Minecraft.getInstance()).level == null) {
      super.extractBackground(context, mouseX, mouseY, delta);
    }
    FontAtlas inter = Fonts.getInstance().getInterSemiBold();
    FontAtlas icons = Fonts.getInstance().getLucide();
    Matrix3x2fStack matrices = context.pose();
    if ((Minecraft.getInstance()).player != null) {
      inter.render(matrices, "HUD Editor", Minecraft.getInstance().getWindow().getGuiScaledWidth() - inter.getWidth("HUD Editor", 8.0F) - 10.0F, 10.0F, 8.0F, Color.WHITE.getRGB());
    }
    context.fill(0, 0, context.guiWidth(), context.guiHeight(), 856164364);
    
    Color themeHighlight = getHighlightColor();
    Color themeOutline = getOutlineColor();
    float searchX = 10.0F;
    float searchY = 10.0F;
    float searchWidth = 150.0F;
    float searchHeight = 15.0F;
    
    Render2DEngine.drawRoundedRect(matrices, searchX, searchY, searchWidth, searchHeight, 4.0F, BACKGROUND_COLOR);
    Render2DEngine.drawRoundedOutline(matrices, searchX, searchY, searchWidth, searchHeight, 4.0F, 1.0F, this.searchActive ? themeHighlight : themeOutline);
    
    String displayText = this.searchQuery.isEmpty() ? (this.searchActive ? "" : "Search...") : this.searchQuery;
    int displayColor = (this.searchQuery.isEmpty() && !this.searchActive) ? -7303024 : -1;
    inter.render(matrices, displayText, searchX + 6.0F, searchY + searchHeight / 2.0F - inter.getLineHeight(8.0F) / 2.5F, 8.0F, displayColor);
    
    if (this.searchActive && System.currentTimeMillis() / 500L % 2L == 0L) {
      float cursorX = searchX + 6.0F + inter.getWidth(displayText, 8.0F);
      float cursorY = searchY + searchHeight / 2.0F - inter.getLineHeight(8.0F) / 2.0F;
      Render2DEngine.drawRect(matrices, cursorX, cursorY, 1.0F, inter.getLineHeight(8.0F), Color.WHITE);
    } 
    if (this.draggingSetting != null && this.selectedPanel != null) {
      Vector2f pos = this.selectedPanel.getPosition();
      float settingsOffset = (1.0F - this.selectedPanel.getSettingsAnimationProgress()) * 120.0F;
      float settingsX = pos.x() + settingsOffset;
      float relativeX = (float)mouseX - (settingsX + 6.0F);
      float totalWidth = 108.0F;
      float percentage = Math.max(0.0F, Math.min(1.0F, relativeX / totalWidth));
      if (this.draggingSetting.isColor()) {
        int curColor = this.draggingSetting.getValColor();
        float[] hsv = Color.RGBtoHSB((curColor >> 16) & 0xFF, (curColor >> 8) & 0xFF, curColor & 0xFF, null);
        if (this.draggingColorBar == 0) {
          this.draggingSetting.setValColor(Color.HSBtoRGB(Math.min(0.99F, percentage), hsv[1], hsv[2]));
        } else if (this.draggingColorBar == 1) {
          this.draggingSetting.setValColor(Color.HSBtoRGB(hsv[0], percentage, hsv[2]));
        } else if (this.draggingColorBar == 2) {
          this.draggingSetting.setValColor(Color.HSBtoRGB(hsv[0], hsv[1], percentage));
        }
      } else {
        double range = this.draggingSetting.getMax() - this.draggingSetting.getMin();
        double newValue = this.draggingSetting.getMin() + range * (double)percentage;
        this.draggingSetting.setValDouble(newValue);
      }
    } 
    boolean foundHoveredModule = false;
    boolean foundHoveredSetting = false;
    Setting nextHoveredSetting = null;
    long currentTime = System.currentTimeMillis();
    for (Panel panel : panels.values()) {
      if (panel.isDragging()) {
        panel.updatePosition(mouseX, mouseY);
      }
      if (!panel.isOpen())
        continue;  Vector2f pos = panel.getPosition();
      float moduleY = pos.y() + 17.5F + 2.0F - panel.getModuleScrollOffset();
      float moduleXOffset = -panel.getSettingsAnimationProgress() * 120.0F;
      for (Module module : getFilteredModules(panel)) {
        if (mouseX >= pos.x() + moduleXOffset && mouseX <= pos.x() + 120.0F + moduleXOffset && mouseY >= moduleY && mouseY <= moduleY + 17.5F && mouseY >= pos.y() + 17.5F + 2.0F && mouseY <= pos.y() + panel.getCurrentHeight()) {
          foundHoveredModule = true;
          if (this.hoveredModule != module) {
            this.hoveredModule = module;
            this.hoverStartTime = currentTime;
            this.showingTooltip = false; break;
          }  if (currentTime - this.hoverStartTime >= 1000L && !this.showingTooltip) {
            this.showingTooltip = true;
          }
          break;
        } 
        moduleY += 19.5F;
      } 
    } 
    if (!foundHoveredModule) {
      this.hoveredModule = null;
      this.showingTooltip = false;
    } 
    for (Panel panel : panels.values()) {
      if (panel.isOpen()) {
        
        float contentHeight = 19.5F * getFilteredModules(panel).size();
        float panelBottom = panel.getPosition().y() + contentHeight + 17.5F + 4.0F; float screenHeight;
        if (panelBottom > (screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight())) {
          float visibleHeight = screenHeight - panel.getPosition().y() - 17.5F - 4.0F;
          panel.maxModuleScrollOffset = Math.max(0.0F, contentHeight - visibleHeight);
        } else {
          panel.maxModuleScrollOffset = 0.0F;
        } 
        panel.setModuleScrollOffset(Math.max(0.0F, Math.min(panel.getModuleScrollOffset(), panel.getMaxModuleScrollOffset())));
        if (panel.isShowingSettings() && this.selectedModule != null && this.selectedPanel == panel) {
          float settingsContentHeight = 21.5F;
          List<Setting> settings = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingsByMod(this.selectedModule);
          for (Setting setting : settings) {
            float settingHeight = getSettingHeight(setting, panel);
            settingsContentHeight += settingHeight;
          } 
          float visibleSettingsHeight = screenHeight - panel.getPosition().y() - 17.5F - 4.0F;
          panel.maxSettingScrollOffset = Math.max(0.0F, settingsContentHeight - visibleSettingsHeight);
          panel.settingScrollOffset = Math.max(0.0F, Math.min(panel.settingScrollOffset, panel.maxSettingScrollOffset));
        } 
      } 
      if (panel.isShowingSettings()) {
        if (panel.getSettingsAnimationProgress() < 1.0F) {
          float elapsed = (float)(currentTime - panel.lastSettingsAnimationTime) / 100.0F;
          panel.setSettingsAnimationProgress(Math.min(1.0F, panel.getSettingsAnimationProgress() + elapsed));
          panel.lastSettingsAnimationTime = currentTime;
        } 
      } else if (panel.getSettingsAnimationProgress() > 0.0F) {
        float elapsed = (float)(currentTime - panel.lastSettingsAnimationTime) / 100.0F;
        panel.setSettingsAnimationProgress(Math.max(0.0F, panel.getSettingsAnimationProgress() - elapsed));
        panel.lastSettingsAnimationTime = currentTime;
      } 
      float targetHeight = 17.5F;
      if (panel.isOpen()) {
        if (panel.isShowingSettings() && this.selectedModule != null && this.selectedPanel == panel) {
          targetHeight += 19.5F;
          List<Setting> settings = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingsByMod(this.selectedModule);
          targetHeight += 21.5F;
          for (Setting setting : settings) {
            float extraHeight = getSettingHeight(setting, panel);
            targetHeight += extraHeight;
          } 
        } else {
          targetHeight += 19.5F * getFilteredModules(panel).size();
        } 
      }
      panel.updateHeight(targetHeight += 2.0F);
      panel.animateHeight();
      Vector2f position = panel.getPosition();
      float posX = position.x();
      float posY = position.y();
      float panelHeight = panel.getCurrentHeight();
      Render2DEngine.drawRoundedRect(matrices, posX + 0.5F, posY + 0.5F, 119.0F, panelHeight - 1.0F, 4.0F, BACKGROUND_COLOR);
      Render2DEngine.drawRoundedOutline(matrices, posX, posY, 120.0F, panelHeight, 5.0F, 1.0F, themeOutline);
      inter.render(matrices, panel.getCategory().name(), posX + 6.0F, posY + 8.75F - inter.getLineHeight(8.0F) / 2.5F, 8.0F, -1);
      float moduleY = posY + 17.5F + 2.0F - panel.getModuleScrollOffset();
      if (panel.isOpen()) {
        context.enableScissor((int)posX + 1, (int)(posY + 17.5F + 2.0F), (int)(posX + 120.0F - 1.0F), (int)(posY + panelHeight));
        float settingsOffset = (1.0F - easeInOutCubic(panel.getSettingsAnimationProgress())) * 120.0F;
        float moduleXOffset = -easeInOutCubic(panel.getSettingsAnimationProgress()) * 120.0F;
        if (!panel.isShowingSettings() || panel.getSettingsAnimationProgress() < 1.0F) {
          for (Module module : getFilteredModules(panel)) {
            if (moduleY + 17.5F >= posY + 17.5F + 2.0F && moduleY <= posY + panelHeight) {
              
              boolean isHovered = (mouseX >= posX + 2.0F + moduleXOffset && mouseX <= posX + 120.0F - 2.0F + moduleXOffset && mouseY >= moduleY && mouseY <= moduleY + 17.5F);
              if (module.isEnabled()) {
                Render2DEngine.drawRoundedRect(matrices, posX + 2.0F + moduleXOffset, moduleY, 116.0F, 17.5F, 4.0F, new Color(HIGHLIGHT_COLOR.getRed(), HIGHLIGHT_COLOR.getGreen(), HIGHLIGHT_COLOR.getBlue(), 60));
              } else if (isHovered) {
                Render2DEngine.drawRoundedRect(matrices, posX + 2.0F + moduleXOffset, moduleY, 116.0F, 17.5F, 4.0F, new Color(255, 255, 255, 25));
              } 
              String bind = (this.bindingModule == module) ? "..." : getKeyName(module.getKeyBind());
              if (!bind.equals("NONE") || this.bindingModule == module) {
                float bindLength = inter.getWidth(bind, 7.0F);
                float bindX = posX + 2.0F + moduleXOffset + 120.0F - bindLength - 20.0F;
                float bindY = moduleY + 8.75F - inter.getLineHeight(7.0F) / 2.0F;
                float bindW = bindLength + 5.0F;
                float bindH = 8.75F;
                float textY = bindY + (bindH - inter.getLineHeight(7.0F)) / 2.0F - 0.5F;
                Render2DEngine.drawRoundedRect(matrices, bindX, bindY, bindW, bindH, 3.0F, new Color(20, 20, 26, 230));
                Render2DEngine.drawRoundedOutline(matrices, bindX, bindY, bindW, bindH, 3.0F, 1.0F, new Color(HIGHLIGHT_COLOR.getRed(), HIGHLIGHT_COLOR.getGreen(), HIGHLIGHT_COLOR.getBlue(), 200));
                inter.render(matrices, bind, bindX + 2.5F, textY, 7.0F, -1);
              } 
              inter.render(matrices, module.getName(), posX + 6.0F + moduleXOffset, moduleY + 8.75F - inter.getLineHeight() / 2.0F, 8.0F, module.isEnabled() ? -1 : -5592406);
              List<Setting> settings = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingsByMod(module);
              if (!settings.isEmpty()) {
                float gearX = posX + moduleXOffset + 120.0F - 14.0F;
                float gearY = moduleY + 8.75F - inter.getLineHeight(8.0F) / 2.5F;
                Color gearColor = module.isEnabled() ? Color.WHITE : Color.LIGHT_GRAY;
                inter.render(matrices, ">", gearX, gearY, 8.0F, gearColor.getRGB());
              } 
            } 
            moduleY += 19.5F;
          } 
        }
        if (panel.isShowingSettings() && this.selectedModule != null && this.selectedPanel == panel) {
          moduleY = posY + 17.5F + 2.0F - panel.settingScrollOffset;
          float settingsX = posX + settingsOffset;
          inter.render(matrices, this.selectedModule.getName(), settingsX + 6.0F, moduleY + 8.75F - inter.getLineHeight() / 2.0F, 7.0F, this.selectedModule.isEnabled() ? -1 : -5592406);
          
          float arrowX = settingsX + 120.0F - 14.0F;
          float arrowY = moduleY + 8.75F - inter.getLineHeight(8.0F) / 2.5F;
          inter.render(matrices, "<", arrowX, arrowY, 8.0F, Color.GRAY.getRGB());
          
          Render2DEngine.drawLine(matrices, settingsX + 4.0F, moduleY + 17.5F, settingsX + 120.0F - 4.0F, moduleY + 17.5F, 1.0F, new Color(100, 100, 100, 100));
          moduleY += 21.5F;
          List<Setting> settings = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingsByMod(this.selectedModule);
          context.enableScissor((int)settingsX + 1, (int)(posY + 17.5F + 2.0F), (int)(settingsX + 120.0F - 1.0F), (int)(posY + panelHeight));
          for (Setting setting : settings) {
            float settingHeight = getSettingHeight(setting, panel);
            if (moduleY + settingHeight >= posY + 17.5F + 2.0F && moduleY <= posY + panelHeight) {
              renderSetting(setting, context, new Vector2f(settingsX, posY), moduleY, inter, mouseX, mouseY);
              boolean isSettingHovered = (mouseX >= settingsX + 4.0F && mouseX <= settingsX + 120.0F - 4.0F && mouseY >= moduleY && mouseY <= moduleY + settingHeight && mouseY >= posY + 17.5F + 2.0F && mouseY <= posY + panelHeight);
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
      float tooltipWidth = inter.getWidth(description, 7.0F) + 12.0F;
      float tooltipHeight = inter.getLineHeight(7.0F) + 8.0F;
      float tooltipX = Math.min((mouseX + 10), Minecraft.getInstance().getWindow().getGuiScaledWidth() - tooltipWidth - 5.0F);
      float tooltipY = Math.min((mouseY + 10), Minecraft.getInstance().getWindow().getGuiScaledHeight() - tooltipHeight - 5.0F);
      Render2DEngine.drawRoundedRect(matrices, tooltipX, tooltipY, tooltipWidth, tooltipHeight, 4.0F, BACKGROUND_COLOR);
      Render2DEngine.drawRoundedOutline(matrices, tooltipX, tooltipY, tooltipWidth, tooltipHeight, 4.0F, 1.0F, themeOutline);
      inter.render(matrices, description, tooltipX + 4.0F, tooltipY + 4.0F, 7.0F, -1);
    } 
    if (this.showingSettingTooltip && this.hoveredSetting != null) {
      String text = this.hoveredSetting.getName();
      float tooltipWidth = inter.getWidth(text, 7.0F) + 12.0F;
      float tooltipHeight = inter.getLineHeight(7.0F) + 8.0F;
      float tooltipX = Math.min((mouseX + 10), Minecraft.getInstance().getWindow().getGuiScaledWidth() - tooltipWidth - 5.0F);
      float tooltipY = Math.min((mouseY + 10), Minecraft.getInstance().getWindow().getGuiScaledHeight() - tooltipHeight - 5.0F);
      Render2DEngine.drawRoundedRect(matrices, tooltipX, tooltipY, tooltipWidth, tooltipHeight, 4.0F, BACKGROUND_COLOR);
      Render2DEngine.drawRoundedOutline(matrices, tooltipX, tooltipY, tooltipWidth, tooltipHeight, 4.0F, 1.0F, themeOutline);
      inter.render(matrices, text, tooltipX + 4.0F, tooltipY + 4.0F, 7.0F, -1);
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
      float searchX = 10.0F;
      float searchY = 10.0F;
      float searchWidth = 150.0F;
      float searchHeight = 15.0F;
      if (mouseX >= searchX && mouseX <= (searchX + searchWidth) && mouseY >= searchY && mouseY <= (searchY + searchHeight)) {
        this.searchActive = true;
        return true;
      } 
      this.searchActive = false;
      
      if ((Minecraft.getInstance()).player != null) {
        FontAtlas font = Fonts.getInstance().getInterSemiBold();
        String hudEditorText = "HUD Editor";
        float hudEditorWidth = font.getWidth(hudEditorText, 8.0F);
        float hudEditorX = Minecraft.getInstance().getWindow().getGuiScaledWidth() - hudEditorWidth - 10.0F;
        float hudEditorY = 10.0F;
        if (mouseX >= hudEditorX && mouseX <= (hudEditorX + hudEditorWidth) && mouseY >= hudEditorY && mouseY <= (hudEditorY + font.getLineHeight(8.0F))) {
          Minecraft.getInstance().setScreenAndShow(new HudEditorScreen());
          Module menu = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("Menu");
          if (menu != null) menu.onDisable(); 
          return true;
        } 
      } 
      for (Panel panel : panels.values()) {
        Vector2f pos = panel.getPosition();
        if (mouseX >= pos.x() && mouseX <= (pos.x() + 120.0F) && mouseY >= pos.y() && mouseY <= (pos.y() + 17.5F)) {
          panel.startDragging(mouseX, mouseY);
          return true;
        } 
        if (!panel.isOpen())
          continue;  float moduleY = pos.y() + 17.5F + 2.0F;
        float adjustedMouseY = (float)mouseY + panel.getModuleScrollOffset();
        if (!panel.isShowingSettings()) {
          float moduleXOffset = -panel.getSettingsAnimationProgress() * 120.0F;
          for (Module module : getFilteredModules(panel)) {
            if (mouseX >= (pos.x() + moduleXOffset) && mouseX <= (pos.x() + 120.0F + moduleXOffset) && adjustedMouseY >= moduleY && adjustedMouseY <= moduleY + 17.5F && mouseY >= (pos.y() + 17.5F + 2.0F) && mouseY <= (pos.y() + panel.getCurrentHeight())) {
              module.toggle();
              return true;
            } 
            moduleY += 19.5F;
          }  continue;
        }  if (this.selectedModule != null && this.selectedPanel == panel) {
          float settingsOffset = (1.0F - panel.getSettingsAnimationProgress()) * 120.0F;
          float settingsX = pos.x() + settingsOffset;
          float adjustedSettingsMouseY = (float)mouseY + panel.settingScrollOffset;
          if (mouseX >= settingsX && mouseX <= (settingsX + 120.0F) && mouseY >= moduleY && mouseY <= (moduleY + 17.5F)) {
            panel.setShowingSettings(false);
            panel.lastSettingsAnimationTime = System.currentTimeMillis();
            this.selectedModule = null;
            this.selectedPanel = null;
            return true;
          } 
          moduleY += 21.5F;
          List<Setting> settings = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingsByMod(this.selectedModule);
          for (Setting setting : settings) {
            float settingHeight = getSettingHeight(setting, panel);
            if (mouseX >= (settingsX + 4.0F) && mouseX <= (settingsX + 120.0F - 4.0F) && adjustedSettingsMouseY >= moduleY && adjustedSettingsMouseY <= moduleY + settingHeight && mouseY >= (pos.y() + 17.5F + 2.0F) && mouseY <= (pos.y() + panel.getCurrentHeight()))
              if (setting.isCheck())
              { float toggleY = moduleY + 4.25F;
                if (mouseX >= (settingsX + 120.0F - 18.0F - 6.0F - 2.0F) && mouseX <= (settingsX + 120.0F - 6.0F + 2.0F) && adjustedSettingsMouseY >= toggleY && adjustedSettingsMouseY <= toggleY + 9.0F) {
                  setting.setValBoolean(!setting.getValBoolean());
                  panel.booleanAnimationTimes.put(setting, Long.valueOf(System.currentTimeMillis()));
                  return true;
                }  }
              else if (setting.isSlider())
              { float sliderY = moduleY + 17.5F - 1.5F;
                DecimalFormat format = new DecimalFormat("#.##");
                String valStr = format.format(setting.getValDouble());
                FontAtlas font = Fonts.getInstance().getInterSemiBold();
                float valWidth = font.getWidth(valStr, 6.0F);
                float textMinX = settingsX + 120.0F - 6.0F - 6.0F - valWidth;

                if (mouseX >= textMinX && mouseX <= (settingsX + 120.0F - 2.0F) && adjustedSettingsMouseY >= moduleY && adjustedSettingsMouseY <= moduleY + 14.0F) {
                  this.editingSlider = setting;
                  this.sliderInputBuffer = format.format(setting.getValDouble());
                  this.focusedSetting = null;
                  return true;
                }

                if (adjustedSettingsMouseY >= sliderY - 5.0F && adjustedSettingsMouseY <= sliderY + 7.0F) {
                  this.editingSlider = null;
                  this.draggingSetting = setting;
                  float relativeX = (float)(mouseX - (settingsX + 6.0F));
                  float totalWidth = 108.0F;
                  float percentage = Math.max(0.0F, Math.min(1.0F, relativeX / totalWidth));
                  double range = setting.getMax() - setting.getMin();
                  double newValue = setting.getMin() + range * percentage;
                  setting.setValDouble(newValue);
                  return true;
                }  }
              else { if (setting.isCombo() && adjustedSettingsMouseY <= moduleY + 17.5F) {
                  panel.setOpenSetting((panel.getOpenSetting() == setting) ? null : setting);
                  return true;
                }  if (setting.isText()) {
                  this.focusedSetting = setting;
                  return true;
                }  if (setting.isColor()) {
                  if (adjustedSettingsMouseY <= moduleY + 17.5F) {
                    panel.setOpenSetting((panel.getOpenSetting() == setting) ? null : setting);
                    return true;
                  } else if (panel.getOpenSetting() == setting) {
                    float pickerY = moduleY + 17.5F;
                    float pickerW = 120.0F - 12.0F;
                    float pickerH = 6.0F;
                    float relativeX = (float)(mouseX - (settingsX + 6.0F));
                    float percentage = Math.max(0.0F, Math.min(1.0F, relativeX / pickerW));
                    int curColor = setting.getValColor();
                    float[] hsv = Color.RGBtoHSB((curColor >> 16) & 0xFF, (curColor >> 8) & 0xFF, curColor & 0xFF, null);
                    if (adjustedSettingsMouseY >= pickerY && adjustedSettingsMouseY <= pickerY + pickerH) {
                      setting.setValColor(Color.HSBtoRGB(Math.min(0.99F, percentage), hsv[1], hsv[2]));
                      this.draggingSetting = setting;
                      this.draggingColorBar = 0;
                      return true;
                    } else if (adjustedSettingsMouseY >= pickerY + pickerH + 4.0F && adjustedSettingsMouseY <= pickerY + pickerH * 2.0F + 4.0F) {
                      setting.setValColor(Color.HSBtoRGB(hsv[0], percentage, hsv[2]));
                      this.draggingSetting = setting;
                      this.draggingColorBar = 1;
                      return true;
                    } else if (adjustedSettingsMouseY >= pickerY + pickerH * 2.0F + 8.0F && adjustedSettingsMouseY <= pickerY + pickerH * 3.0F + 8.0F) {
                      setting.setValColor(Color.HSBtoRGB(hsv[0], hsv[1], percentage));
                      this.draggingSetting = setting;
                      this.draggingColorBar = 2;
                      return true;
                    }
                  }
                }  }
               
            if (this.focusedSetting == setting && (mouseX < (settingsX + 4.0F) || mouseX > (settingsX + 120.0F - 4.0F) || adjustedSettingsMouseY < moduleY || adjustedSettingsMouseY > moduleY + settingHeight)) {
              this.focusedSetting = null;
            }
            if (setting.isCombo() && panel.getOpenSetting() == setting && setting.getOptions() != null) {
              float optionY = moduleY + 17.5F;
              for (String mode : setting.getOptions()) {
                if (mouseX >= (settingsX + 6.0F) && mouseX <= (settingsX + 120.0F - 6.0F) && adjustedSettingsMouseY >= optionY && adjustedSettingsMouseY <= optionY + 17.5F - 2.0F && mouseY >= (pos.y() + 17.5F + 2.0F) && mouseY <= (pos.y() + panel.getCurrentHeight())) {
                  setting.setValString(mode);
                  return true;
                } 
                optionY += 15.5F;
              } 
            } 
            moduleY += settingHeight;
          } 
        } 
      } 
    } else if (button == 1) {
      for (Panel panel : panels.values()) {
        Vector2f pos = panel.getPosition();
        if (mouseX >= pos.x() && mouseX <= (pos.x() + 120.0F) && mouseY >= pos.y() && mouseY <= (pos.y() + 17.5F)) {
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
        if (!panel.isOpen())
          continue;  float moduleY = pos.y() + 17.5F + 2.0F;
        float moduleXOffset = -panel.getSettingsAnimationProgress() * 120.0F;
        float adjustedMouseY = (float)mouseY + panel.getModuleScrollOffset();
        if (panel.isShowingSettings() && this.selectedModule != null && this.selectedPanel == panel) {
          float settingsOffset = (1.0F - panel.getSettingsAnimationProgress()) * 120.0F;
          float settingsX = pos.x() + settingsOffset;
          if (mouseX >= settingsX && mouseX <= (settingsX + 120.0F) && mouseY >= moduleY && mouseY <= (moduleY + 17.5F)) {
            panel.setShowingSettings(false);
            panel.lastSettingsAnimationTime = System.currentTimeMillis();
            this.selectedModule = null;
            this.selectedPanel = null;
            return true;
          } 
        } 
        for (Module module : getFilteredModules(panel)) {
          if (mouseX >= (pos.x() + moduleXOffset) && mouseX <= (pos.x() + 120.0F + moduleXOffset) && adjustedMouseY >= moduleY && adjustedMouseY <= moduleY + 17.5F && mouseY >= (pos.y() + 17.5F + 2.0F) && mouseY <= (pos.y() + panel.getCurrentHeight())) {
            List<Setting> settings = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingsByMod(module);
            if (!settings.isEmpty()) {
              for (Panel otherPanel : panels.values()) {
                if (otherPanel == panel)
                  continue;  otherPanel.setShowingSettings(false);
              } 
              panel.setShowingSettings(true);
              panel.lastSettingsAnimationTime = System.currentTimeMillis();
              this.selectedModule = module;
              this.selectedPanel = panel;
            } 
            return true;
          } 
          moduleY += 19.5F;
        } 
      } 
    } else if (button == 2) {
      for (Panel panel : panels.values()) {
        Vector2f pos = panel.getPosition();
        if (!panel.isOpen() || panel.isShowingSettings())
          continue;  float moduleY = pos.y() + 17.5F + 2.0F;
        float moduleXOffset = -panel.getSettingsAnimationProgress() * 120.0F;
        float adjustedMouseY = (float)mouseY + panel.getModuleScrollOffset();
        for (Module module : getFilteredModules(panel)) {
          if (mouseX >= (pos.x() + moduleXOffset) && mouseX <= (pos.x() + 120.0F + moduleXOffset) && adjustedMouseY >= moduleY && adjustedMouseY <= moduleY + 17.5F && mouseY >= (pos.y() + 17.5F + 2.0F) && mouseY <= (pos.y() + panel.getCurrentHeight())) {
            this.bindingModule = module;
            return true;
          } 
          moduleY += 19.5F;
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
        this.draggingColorBar = -1;
        return true;
      } 
    } 
    return super.mouseReleased(click);
  }
  
  public boolean keyPressed(KeyEvent event) {
    int keyCode = event.input();
    if (this.searchActive) {
      if (keyCode == 259) {
        if (!this.searchQuery.isEmpty()) {
          this.searchQuery = this.searchQuery.substring(0, this.searchQuery.length() - 1);
          checkSearchEasterEgg();
        }
        return true;
      } 
      if (keyCode == 256 || keyCode == 257) {
        this.searchActive = false;
        return true;
      } 
      if (keyCode != 256) {
        return true;
      }
    } 
    if (this.editingSlider != null) {
      if (keyCode == 259) {
        if (!this.sliderInputBuffer.isEmpty()) {
          this.sliderInputBuffer = this.sliderInputBuffer.substring(0, this.sliderInputBuffer.length() - 1);
        }
        return true;
      }
      if (keyCode == 256 || keyCode == 257 || keyCode == 335) {
        try {
          if (!this.sliderInputBuffer.isEmpty()) {
            double parsed = Double.parseDouble(this.sliderInputBuffer);
            if (this.editingSlider.onlyInt()) {
              this.editingSlider.setValDoubleUnclamped(Math.round(parsed));
            } else {
              this.editingSlider.setValDoubleUnclamped(parsed);
            }
          }
        } catch (NumberFormatException ignored) {}
        this.editingSlider = null;
        this.sliderInputBuffer = "";
        return true;
      }
      return true;
    }
    if (this.focusedSetting != null) {
      if (keyCode == 259) {
        String val = this.focusedSetting.getValText();
        if (!val.isEmpty()) {
          this.focusedSetting.setValText(val.substring(0, val.length() - 1));
        }
        return true;
      } 
      if (keyCode == 256 || keyCode == 257) {
        this.focusedSetting = null;
        return true;
      } 
      return true;
    } 
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
      onClose();
      return true;
    } 
    return super.keyPressed(event);
  }

  
  public boolean charTyped(CharacterEvent event) {
    if (this.searchActive) {
      this.searchQuery += event.codepointAsString();
      checkSearchEasterEgg();
      return true;
    } 
    if (this.editingSlider != null) {
      String typedStr = event.codepointAsString();
      if (typedStr.matches("[0-9.-]")) {
        this.sliderInputBuffer += typedStr;
      }
      return true;
    }
    if (this.focusedSetting != null) {
      this.focusedSetting.setValText(this.focusedSetting.getValText() + event.codepointAsString());
      return true;
    } 
    return super.charTyped(event);
  }

  private void checkSearchEasterEgg() {
    if (this.searchQuery.equalsIgnoreCase("protien powder")) {
      try {
        java.lang.reflect.Field field = net.minecraft.client.Options.class.getDeclaredField("soundSourceVolumes");
        field.setAccessible(true);
        java.util.Map<net.minecraft.sounds.SoundSource, net.minecraft.client.OptionInstance<Double>> map = 
          (java.util.Map<net.minecraft.sounds.SoundSource, net.minecraft.client.OptionInstance<Double>>) field.get(net.minecraft.client.Minecraft.getInstance().options);
        map.get(net.minecraft.sounds.SoundSource.MASTER).set(500.0);
        net.minecraft.client.Minecraft.getInstance().options.save();
      } catch (Exception ignored) {}
    } else if (this.searchQuery.equalsIgnoreCase("sweet powder")) {
      try {
        java.lang.reflect.Field field = net.minecraft.client.Options.class.getDeclaredField("soundSourceVolumes");
        field.setAccessible(true);
        java.util.Map<net.minecraft.sounds.SoundSource, net.minecraft.client.OptionInstance<Double>> map = 
          (java.util.Map<net.minecraft.sounds.SoundSource, net.minecraft.client.OptionInstance<Double>>) field.get(net.minecraft.client.Minecraft.getInstance().options);
        map.get(net.minecraft.sounds.SoundSource.MASTER).set(1.0);
        net.minecraft.client.Minecraft.getInstance().options.save();
      } catch (Exception ignored) {}
    }
  }
  
  public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
    for (Panel panel : panels.values()) {
      Vector2f pos = panel.getPosition();
      if (!panel.isOpen() || mouseX < pos.x() || mouseX > (pos.x() + 120.0F) || mouseY < pos.y() || mouseY > (pos.y() + panel.getCurrentHeight()))
        continue;  if (panel.isShowingSettings() && this.selectedModule != null && this.selectedPanel == panel) {
        float settingsOffset = (1.0F - panel.getSettingsAnimationProgress()) * 120.0F;
        float settingsX = pos.x() + settingsOffset;
        if (mouseX >= settingsX && mouseX <= (settingsX + 120.0F)) {
          float newSettingScrollOffset = panel.settingScrollOffset - (float)(verticalAmount * 10.0D);
          panel.settingScrollOffset = Math.max(0.0F, Math.min(panel.maxSettingScrollOffset, newSettingScrollOffset));
          return true;
        } 
      } 
      if (panel.maxModuleScrollOffset > 0.0F) {
        float newModuleScrollOffset = panel.getModuleScrollOffset() - (float)(verticalAmount * 10.0D);
        panel.setModuleScrollOffset(Math.max(0.0F, Math.min(panel.getMaxModuleScrollOffset(), newModuleScrollOffset)));
        return true;
      } 
    } 
    return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
  }
  
  public void removed() {
    this.selectedModule = null;
    this.selectedPanel = null;
    this.hoveredSetting = null;
    this.showingSettingTooltip = false;
    for (Panel panel : panels.values()) {
      panel.setShowingSettings(false);
      panel.setSettingsAnimationProgress(0.0F);
      panel.setOpenSetting(null);
    } 
    Module menu = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("Menu");
    if (menu != null && menu.isToggled()) {
      menu.onDisable();
    }
    try {
      com.eclipseware.imnotcheatingyouare.client.setting.ConfigManager.save();
    } catch (Exception ignored) {}
    super.removed();
  }
  public boolean isPauseScreen() {
    return false;
  } public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {}
  private float easeInOutCubic(float t) {
    return (float)((t < 0.5D) ? (4.0F * t * t * t) : (1.0D - Math.pow((-2.0F * t + 2.0F), 3.0D) / 2.0D));
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
    private float settingsAnimationProgress = 0.0F;
    private long lastSettingsAnimationTime = 0L;
    private float currentHeight = 17.5F;
    private float targetHeight = 17.5F;
    private long lastHeightAnimationTime = System.currentTimeMillis();
    private float animationProgress = 0.0F;
    private float moduleScrollOffset = 0.0F;
    private float maxModuleScrollOffset = 0.0F;
    private float settingScrollOffset = 0.0F;
    private float maxSettingScrollOffset = 0.0F;
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
    public void setModuleScrollOffset(float val) { this.moduleScrollOffset = val; } public float getMaxModuleScrollOffset() {
      return this.maxModuleScrollOffset;
    }
    public Panel(Category category, boolean isOpen, Vector2f position) {
      this.category = category;
      this.position = position;
      this.isOpen = isOpen;
      this.isDragging = false;
      this.showingSettings = false;
      this.openSetting = null;
      this.modules = new ArrayList<>();
      this.moduleScrollOffset = 0.0F;
      this.maxModuleScrollOffset = 0.0F;
      this.settingScrollOffset = 0.0F;
      this.maxSettingScrollOffset = 0.0F;
    }
    
    public void updateHeight(float newTargetHeight) {
      if (this.targetHeight != newTargetHeight) {
        this.targetHeight = newTargetHeight;
        this.lastHeightAnimationTime = System.currentTimeMillis();
        this.animationProgress = 0.0F;
      } 
    }
    
    public void animateHeight() {
      if (this.currentHeight != this.targetHeight) {
        long currentTime = System.currentTimeMillis();
        float elapsed = Math.min(1.0F, (float)(currentTime - this.lastHeightAnimationTime) / 500.0F);
        this.lastHeightAnimationTime = currentTime;
        this.animationProgress = Math.min(1.0F, this.animationProgress + elapsed);
        float smoothProgress = (float)(1.0D - Math.pow((1.0F - this.animationProgress), 3.0D));
        this.currentHeight += (this.targetHeight - this.currentHeight) * smoothProgress;
        if (Math.abs(this.currentHeight - this.targetHeight) < 0.01F) {
          this.currentHeight = this.targetHeight;
        }
      } 
    }
    
    public void setShowingSettings(boolean showingSettings) {
      if (this.showingSettings != showingSettings) {
        this.showingSettings = showingSettings;
        this.lastSettingsAnimationTime = System.currentTimeMillis();
        this.animationProgress = 0.0F;
        this.lastHeightAnimationTime = System.currentTimeMillis();
        this.settingScrollOffset = 0.0F;
        this.maxSettingScrollOffset = 0.0F;
      } 
    }
    
    public void startDragging(double mouseX, double mouseY) {
      this.isDragging = true;
      this.dragX = mouseX - this.position.x();
      this.dragY = mouseY - this.position.y();
    }
    
    public void updatePosition(double mouseX, double mouseY) {
      if (this.isDragging) {
        float newX = (float)(mouseX - this.dragX);
        float newY = (float)(mouseY - this.dragY);
        float screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        float screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        newX = Math.max(0.0F, Math.min(screenWidth - 120.0F, newX));
        newY = Math.max(0.0F, Math.min(screenHeight - 17.5F, newY));
        this.position = new Vector2f(newX, newY);
      } 
    }
  }
}


