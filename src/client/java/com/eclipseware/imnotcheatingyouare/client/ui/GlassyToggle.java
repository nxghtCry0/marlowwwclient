package com.eclipseware.imnotcheatingyouare.client.ui;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public final class GlassyToggle extends CompatAbstractWidget {
    private final BooleanSupplier getter;
    private final Consumer<Boolean> setter;
    private float anim = Float.NaN;
    private long lastRenderTime = 0;

    public GlassyToggle(int x, int y, int w, int h, BooleanSupplier getter, Consumer<Boolean> setter) {
        super(x, y, w, h, Component.empty());
        this.getter = getter;
        this.setter = setter;
    }

    private boolean value() {
        return (this.getter != null && this.getter.getAsBoolean());
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event != null && this.active && this.visible && event.button() == 0 && isMouseOver(event.x(), event.y())) {
            if (this.setter != null) {
                this.setter.accept(!value());
            }
            return true;
        } 
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int x = getX();
        int y = getY();
        int w = getWidth();
        int h = getHeight();

        long now = System.currentTimeMillis();
        if (lastRenderTime == 0) lastRenderTime = now;
        float timeDelta = Math.min(0.1f, (now - lastRenderTime) / 1000f);
        lastRenderTime = now;

        boolean on = value();
        float target = on ? 1.0F : 0.0F;
        if (!Float.isFinite(this.anim)) {
            this.anim = target;
        } else {
            float speed = (this.isHovered && this.active) ? 18.0F : 12.0F;
            float factor = 1f - (float)Math.exp(-speed * timeDelta);
            this.anim = this.anim + (target - this.anim) * factor;
        } 
        
        int trackOff = this.active ? GlassyTheme.CONTROL_BG : GlassyTheme.CONTROL_BG_DISABLED;
        int trackOn = GlassyTheme.ACCENT_DIM;
        int track = lerpRgb(trackOff, trackOn, this.anim);
        
        int border = (this.isHovered && this.active) ? GlassyTheme.CONTROL_BORDER_HOVER : GlassyTheme.CONTROL_BORDER;
        int knob = on ? GlassyTheme.TEXT : GlassyTheme.TEXT_MUTED;
        
        guiGraphics.fill(x, y, x + w, y + h, track);
        guiGraphics.renderOutline(x, y, w, h, border);
        
        int knobSize = Math.max(8, h - 6);
        int knobX = Math.round(Mth.lerp(this.anim, (x + 3), (x + w - knobSize - 3)));
        int knobY = y + (h - knobSize) / 2;
        guiGraphics.fill(knobX, knobY, knobX + knobSize, knobY + knobSize, knob);
        guiGraphics.renderOutline(knobX, knobY, knobSize, knobSize, 0x60FFFFFF);
    }

    private static int lerpRgb(int a, int b, float t) {
        t = Mth.clamp(t, 0.0F, 1.0F);
        int ar = a >> 16 & 0xFF;
        int ag = a >> 8 & 0xFF;
        int ab = a & 0xFF;
        int br = b >> 16 & 0xFF;
        int bg = b >> 8 & 0xFF;
        int bb = b & 0xFF;
        int r = Math.round(Mth.lerp(t, ar, br));
        int g = Math.round(Mth.lerp(t, ag, bg));
        int bl = Math.round(Mth.lerp(t, ab, bb));
        int alpha = a >>> 24;
        if (alpha == 0) {
            alpha = 64;
        }
        int beta = b >>> 24;
        if (beta == 0) {
            beta = 64;
        }
        int al = Math.round(Mth.lerp(t, alpha, beta));
        return al << 24 | r << 16 | g << 8 | bl;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        narrationElementOutput.add(NarratedElementType.TITLE, Component.literal(value() ? "Enabled" : "Disabled"));
    }
}
