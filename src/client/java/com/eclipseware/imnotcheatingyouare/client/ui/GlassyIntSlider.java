package com.eclipseware.imnotcheatingyouare.client.ui;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public final class GlassyIntSlider extends CompatSliderButton {
    private final IntSupplier getter;
    private final IntConsumer setter;
    private final int min;
    private final int max;
    private final int step;
    private final IntFunction<Component> label;
    private double animValue;
    private final double defaultValue01;
    private final int defaultValue;

    public GlassyIntSlider(int x, int y, int width, int height, IntSupplier getter, IntConsumer setter, int min, int max, int step, IntFunction<Component> label, int defaultValue) {
        super(x, y, width, height, Component.empty(), 0.0D);
        this.getter = getter;
        this.setter = setter;
        this.min = min;
        this.max = max;
        this.step = step;
        this.label = label;
        this.defaultValue = defaultValue;
        this.defaultValue01 = (clamp(defaultValue, min, max) - min) / Math.max(1.0D, (max - min));
        
        int got = (getter != null) ? getter.getAsInt() : min;
        int knob = clamp(got, min, max);
        this.value = (knob - min) / Math.max(1.0D, (max - min));
        this.animValue = this.value;
        updateMessage();
    }

    public void refreshFromConfig() {
        updateMessage();
    }

    @Override
    protected void updateMessage() {
        int got = (this.getter != null) ? this.getter.getAsInt() : this.min;
        int knob = clamp(got, this.min, this.max);
        this.value = (knob - this.min) / Math.max(1.0D, (this.max - this.min));
        if (this.label != null) {
            setMessage(this.label.apply(knob));
        } else {
            setMessage(Component.literal(Integer.toString(knob)));
        } 
    }

    @Override
    protected void applyValue() {
        int raw = (int)Math.round(this.min + (this.max - this.min) * this.value);
        int stepped = (this.step <= 1) ? raw : ((int)Math.round((double)raw / this.step) * this.step);
        int clamped = clamp(stepped, this.min, this.max);
        this.value = (clamped - this.min) / Math.max(1.0D, (this.max - this.min));
        if (this.setter != null) {
            this.setter.accept(clamped);
        }
        updateMessage();
    }

    @Override
    public void renderWidget(DrawContext DrawContext, int mouseX, int mouseY, float partialTick) {
        double speed = this.isHovered ? 0.55D : 0.3D;
        this.animValue = Mth.lerp(speed, this.animValue, this.value);
        renderGlassySlider(DrawContext, mouseX, mouseY, getX(), getY(), getWidth(), getHeight(), this.animValue, this.defaultValue01, getMessage(), this.active, this.isHovered);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event != null && this.active && this.visible && event.button() == 1 && isMouseOver(event.x(), event.y())) {
            return true;
        }
        if (event != null && this.active && this.visible && event.button() == 0 && isMouseOver(event.x(), event.y()) && isShiftDown()) {
            this.value = this.defaultValue01;
            applyValue();
            updateMessage();
            return true;
        } 
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (event != null && this.active && this.visible && event.button() == 0 && isShiftDown()) {
            this.value = this.defaultValue01;
            applyValue();
            updateMessage();
            return true;
        } 
        return super.mouseDragged(event, dragX, dragY);
    }
    
    private static boolean isShiftDown() {
        Window window = Minecraft.getInstance().getWindow();
        return false;
    }
    
    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private static void renderGlassySlider(DrawContext DrawContext, int mouseX, int mouseY, int x, int y, int w, int h, double value, double defaultValue01, Component message, boolean active, boolean hovered) {
        int bg, border, knob;
        if (!active) {
            bg = GlassyTheme.CONTROL_BG_DISABLED;
        } else if (hovered) {
            bg = GlassyTheme.CONTROL_BG_HOVER;
        } else {
            bg = GlassyTheme.CONTROL_BG;
        } 
        
        if (!active) {
            border = GlassyTheme.CONTROL_BORDER;
        } else {
            border = hovered ? GlassyTheme.CONTROL_BORDER_HOVER : GlassyTheme.CONTROL_BORDER;
        } 
        DrawContext.fill(x, y, x + w, y + h, bg);
        DrawContext.renderOutline(x, y, w, h, border);
        
        int pad = 6;
        int trackH = 3;
        int trackX0 = x + pad;
        int trackX1 = x + w - pad;
        int trackY = y + h - 7;
        int trackBg = active ? GlassyTheme.CONTROL_BORDER : 0x15000000;
        DrawContext.fill(trackX0, trackY, trackX1, trackY + trackH, trackBg);
        
        int knobW = 6;
        int knobH = 7;
        int knobHalf = knobW / 2;
        double clampedValue = Mth.clamp(value, 0.0D, 1.0D);
        int centerMin = trackX0 + knobHalf;
        int centerMax = trackX1 - knobHalf;
        int centerX = (int)Math.round(Mth.lerp(clampedValue, centerMin, Math.max(centerMin, centerMax)));
        
        if (Double.isFinite(defaultValue01)) {
            int notchX = (int)Math.round(Mth.lerp(Mth.clamp(defaultValue01, 0.0D, 1.0D), centerMin, Math.max(centerMin, centerMax)));
            DrawContext.fill(notchX, trackY - 4, notchX + 1, trackY + trackH + 4, active ? GlassyTheme.ACCENT_DIM : 0x41000000);
        } 
        
        int knobX = centerX - knobHalf;
        int knobY = trackY - 2;
        int fillX = knobX + knobW / 2;
        if (fillX > trackX0) {
            int fill = active ? GlassyTheme.ACCENT_DIM : 0x2055AAFF;
            DrawContext.fill(trackX0, trackY, Math.min(fillX, trackX1), trackY + trackH, fill);
        } 

        if (!active) {
            knob = 0x41000000;
        } else if (hovered) {
            knob = 0xFF69DAFF;
        } else {
            knob = GlassyTheme.ACCENT;
        } 
        DrawContext.fill(knobX, knobY, knobX + knobW, knobY + knobH, knob);
        DrawContext.renderOutline(knobX, knobY, knobW, knobH, active ? GlassyTheme.ACCENT_DIM : GlassyTheme.CONTROL_BORDER);
        
        if (message != null) {
            int color = active ? GlassyTheme.TEXT : GlassyTheme.TEXT_SUBTLE;
            Font font = Minecraft.getInstance().font;
            DrawContext.drawCenteredString(font, message, x + w / 2, y + 4, color);
        } 
    }
}
