/*    */ package com.eclipseware.imnotcheatingyouare.client.ui;
/*    */ 
/*    */ import net.minecraft.client.gui.GuiGraphicsExtractor;
/*    */ import net.minecraft.client.gui.GuiGraphicsExtractor;
/*    */ import net.minecraft.client.gui.components.AbstractSliderButton;
/*    */ import net.minecraft.network.chat.Component;
/*    */ 
/*    */ public abstract class CompatSliderButton extends AbstractSliderButton {
/*    */   private GuiGraphicsExtractor currentExtractor;
/*    */   private int currentMouseX;
/*    */   private int currentMouseY;
/*    */   private float currentPartialTick;
/*    */   
/*    */   protected CompatSliderButton(int x, int y, int width, int height, Component message, double value) {
/* 15 */     super(x, y, width, height, message, value);
/*    */   }
/*    */ 
/*    */   
/*    */   public void extractWidgetRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
/* 20 */     this.currentExtractor = extractor;
/* 21 */     this.currentMouseX = mouseX;
/* 22 */     this.currentMouseY = mouseY;
/* 23 */     this.currentPartialTick = partialTick;
/*    */     try {
/* 25 */       renderWidget(new GuiGraphics(extractor), mouseX, mouseY, partialTick);
/*    */     } finally {
/* 27 */       this.currentExtractor = null;
/*    */     } 
/*    */   }
/*    */   
/*    */   public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
/* 32 */     if (this.currentExtractor != null)
/* 33 */       super.extractWidgetRenderState(this.currentExtractor, this.currentMouseX, this.currentMouseY, this.currentPartialTick); 
/*    */   }
/*    */ }


/* Location:              C:\Users\teeja\Downloads\wWaypoints 0.5.1 26.1.x.jar!\com\wwaypoints\client\screen\CompatSliderButton.class
 * Java compiler version: 25 (69.0)
 * JD-Core Version:       1.1.3
 */
