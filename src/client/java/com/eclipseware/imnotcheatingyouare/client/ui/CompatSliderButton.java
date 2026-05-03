 package com.eclipseware.imnotcheatingyouare.client.ui;
 import net.minecraft.client.gui.DrawContext;
 import net.minecraft.client.gui.DrawContext;
 import net.minecraft.client.gui.components.AbstractSliderButton;
 import net.minecraft.network.chat.Component;
 public abstract class CompatSliderButton extends AbstractSliderButton {
   private DrawContext currentExtractor;
   private int currentMouseX;
   private int currentMouseY;
   private float currentPartialTick;
   protected CompatSliderButton(int x, int y, int width, int height, Component message, double value) {
     super(x, y, width, height, message, value);
   }
   public void extractWidgetRenderState(DrawContext extractor, int mouseX, int mouseY, float partialTick) {
     this.currentExtractor = extractor;
     this.currentMouseX = mouseX;
     this.currentMouseY = mouseY;
     this.currentPartialTick = partialTick;
     try {
       renderWidget(new DrawContext(extractor), mouseX, mouseY, partialTick);
     } finally {
       this.currentExtractor = null;
     } 
   }
   public void renderWidget(DrawContext DrawContext, int mouseX, int mouseY, float partialTick) {
     if (this.currentExtractor != null)
       super.extractWidgetRenderState(this.currentExtractor, this.currentMouseX, this.currentMouseY, this.currentPartialTick); 
   }
 }


