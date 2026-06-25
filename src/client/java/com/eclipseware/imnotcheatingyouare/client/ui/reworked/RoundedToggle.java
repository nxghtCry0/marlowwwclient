package com.eclipseware.imnotcheatingyouare.client.ui.reworked;

import com.eclipseware.imnotcheatingyouare.client.ui.CompatAbstractWidget;
import com.eclipseware.imnotcheatingyouare.client.ui.GuiGraphics;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public final class RoundedToggle extends CompatAbstractWidget {

    private final BooleanSupplier getter;
    private final Consumer<Boolean> setter;
    private float anim = Float.NaN;
    private long lastRenderTime = 0;

    public RoundedToggle(int x, int y, int w, int h, BooleanSupplier getter, Consumer<Boolean> setter) {
        super(x, y, w, h, Component.empty());
        this.getter = getter;
        this.setter = setter;
    }

    private boolean value() {
        return this.getter != null && this.getter.getAsBoolean();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event != null && this.active && this.visible
                && event.button() == 0 && isMouseOver(event.x(), event.y())) {
            if (this.setter != null) this.setter.accept(!value());
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        long now = System.currentTimeMillis();
        if (lastRenderTime == 0) lastRenderTime = now;
        float dt = Math.min(0.1f, (now - lastRenderTime) / 1000f);
        lastRenderTime = now;

        boolean on = value();
        float target = on ? 1f : 0f;
        if (!Float.isFinite(this.anim)) {
            this.anim = target;
        } else {
            float k = 16f * ReworkedTheme.animSpeed;
            float factor = 1f - (float) Math.exp(-k * dt);
            this.anim = this.anim + (target - this.anim) * factor;
        }

        int x = getX(), y = getY(), w = getWidth(), h = getHeight();
        int r = Math.min(ReworkedTheme.radius, Math.min(w, h) / 2);

        int trackOff = this.active ? ReworkedTheme.controlBg : 0x20101010;
        int trackOn  = ReworkedTheme.accent;
        int track = Rounded.lerpArgb(trackOff, trackOn, this.anim);
        Rounded.fill(g, x, y, w, h, r, track);

        int border = (this.isHovered && this.active)
                ? Rounded.lerpArgb(ReworkedTheme.controlBorder, ReworkedTheme.controlBorderHover, 0.6f)
                : ReworkedTheme.controlBorder;
        Rounded.outline(g, x, y, w, h, r, 1f, border);

        int knobSize = Math.max(8, h - 6);
        int knobR = Math.min(knobSize / 2, r - 2);
        float eased = easeOutBack(this.anim);
        int knobX = Math.round(x + 3 + eased * (w - knobSize - 6));
        int knobY = y + (h - knobSize) / 2;

        int knobOff = this.active ? ReworkedTheme.textMuted : 0xFF505050;
        int knobOn  = 0xFFFFFFFF;
        int knob = Rounded.lerpArgb(knobOff, knobOn, this.anim);

        if (this.anim > 0.05f) {
            int haloAlpha = (int) (this.anim * 80);
            Rounded.fill(g, knobX - 2, knobY - 2, knobSize + 4, knobSize + 4, knobR + 2, (haloAlpha << 24) | (ReworkedTheme.accent & 0x00FFFFFF));
        }

        Rounded.fill(g, knobX, knobY, knobSize, knobSize, knobR, knob);
        Rounded.outline(g, knobX, knobY, knobSize, knobSize, knobR, 1f, 0x33FFFFFF);
    }

    private static float easeOutBack(float t) {
        t = Math.max(0f, Math.min(1f, t));
        float c1 = 1.70158f;
        float c3 = c1 + 1f;
        return 1f + c3 * (float) Math.pow(t - 1f, 3f) + c1 * (float) Math.pow(t - 1f, 2f);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput out) {
        out.add(NarratedElementType.TITLE, Component.literal(value() ? "Enabled" : "Disabled"));
    }
}
