package com.eclipseware.imnotcheatingyouare.client.clickgui;

import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import org.joml.Vector2f;


















































































































































































































































































































































































































































































































































































































































































































































































































































































class Panel
{
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


