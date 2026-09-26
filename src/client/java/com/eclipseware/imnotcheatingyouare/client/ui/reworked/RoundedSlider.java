package com.eclipseware.imnotcheatingyouare.client.ui.reworked;

import com.eclipseware.imnotcheatingyouare.client.ui.CompatAbstractWidget;
import com.eclipseware.imnotcheatingyouare.client.ui.GuiGraphics;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.Function;

public final class RoundedSlider extends CompatAbstractWidget {

    private final DoubleSupplier getter;
    private final DoubleConsumer setter;
    private final double min, max;
    private final boolean onlyInt;
    private final Function<Double, String> labeler;

    private float animValue = 0f;
    private float hoverAnim = 0f;
    private boolean dragging = false;
    private long lastRenderTime = 0;

    public RoundedSlider(int x, int y, int w, int h,
                         DoubleSupplier getter, DoubleConsumer setter,
                         double min, double max, boolean onlyInt,
                         Function<Double, String> labeler) {
        super(x, y, w, h, Component.empty());
        this.getter = getter;
        this.setter = setter;
        this.min = min;
        this.max = max;
        this.onlyInt = onlyInt;
        this.labeler = labeler;
        this.animValue = normalizedValue();
        this.active = true;
    }

    private float normalizedValue() {
        double v = (getter != null) ? getter.getAsDouble() : min;
        double n = (v - min) / Math.max(1.0, (max - min));
        return (float) Mth.clamp(n, 0.0, 1.0);
    }

    private void applyFromMouse(double mouseX) {
        int x = getX(), w = getWidth();
        double pad = 8.0;
        double knobW = 8.0;
        double trackX0 = x + pad;
        double trackX1 = x + w - pad;
        double centerMin = trackX0 + knobW / 2.0;
        double centerMax = trackX1 - knobW / 2.0;
        double range = centerMax - centerMin;
        double n = range <= 0 ? 0 : (mouseX - centerMin) / range;
        n = Mth.clamp(n, 0.0, 1.0);
        double raw = min + (max - min) * n;
        double stepped = onlyInt ? Math.round(raw) : Math.round(raw * 10.0) / 10.0;
        stepped = Mth.clamp(stepped, min, max);
        if (setter != null) setter.accept(stepped);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event != null && this.active && this.visible
                && event.button() == 0 && isMouseOver(event.x(), event.y())) {
            this.dragging = true;
            applyFromMouse(event.x());
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        this.dragging = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (this.dragging && this.active && this.visible) {
            applyFromMouse(event.x());
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        long now = System.currentTimeMillis();
        if (lastRenderTime == 0) lastRenderTime = now;
        float dt = Math.min(0.1f, (now - lastRenderTime) / 1000f);
        lastRenderTime = now;

        float target = normalizedValue();
        float k = (this.isHovered || this.dragging) ? 22f : 14f;
        k *= ReworkedTheme.animSpeed;
        float factor = 1f - (float) Math.exp(-k * dt);
        animValue = animValue + (target - animValue) * factor;

        float hoverTarget = (this.isHovered || this.dragging) && this.active ? 1f : 0f;
        hoverAnim = hoverAnim + (hoverTarget - hoverAnim) * (1f - (float) Math.exp(-14f * ReworkedTheme.animSpeed * dt));

        int x = getX(), y = getY(), w = getWidth(), h = getHeight();
        int r = Math.min(ReworkedTheme.radius, Math.min(w, h) / 2);

        int body = this.active
                ? Rounded.lerpArgb(ReworkedTheme.controlBg, ReworkedTheme.controlBgHover, hoverAnim)
                : 0x20101010;
        Rounded.fill(g, x, y, w, h, r, body);

        int border = this.active
                ? Rounded.lerpArgb(ReworkedTheme.controlBorder, ReworkedTheme.controlBorderHover, hoverAnim)
                : ReworkedTheme.controlBorder;
        Rounded.outline(g, x, y, w, h, r, 1f, border);

        int pad = 8;
        int trackH = 3;
        int trackX0 = x + pad;
        int trackX1 = x + w - pad;
        int trackY = y + h - 7;
        Rounded.fill(g, trackX0, trackY, trackX1 - trackX0, trackH, trackH / 2, ReworkedTheme.controlBorder);

        float eased = animValue;
        int knobCenter = (int) (trackX0 + 4 + eased * (trackX1 - trackX0 - 8));
        int fillEnd = knobCenter;
        if (fillEnd > trackX0) {
            int fill = this.active ? ReworkedTheme.accent : 0x40808080;
            Rounded.fill(g, trackX0, trackY, fillEnd - trackX0, trackH, trackH / 2, fill);
        }

        int knobBase = 8;
        int knobSize = (int) (knobBase + hoverAnim * 2f);
        int knobR = knobSize / 2;
        int knobX = knobCenter - knobR;
        int knobY = trackY + trackH / 2 - knobR;

        if (this.active && hoverAnim > 0.05f) {
            int haloAlpha = (int) (hoverAnim * 90);
            Rounded.fill(g, knobX - 2, knobY - 2, knobSize + 4, knobSize + 4, knobR + 2,
                    (haloAlpha << 24) | (ReworkedTheme.accent & 0x00FFFFFF));
        }

        int knobCol = this.active
                ? Rounded.lerpRgb(ReworkedTheme.accent, 0xFFFFFFFF, hoverAnim * 0.3f)
                : 0xFF606060;
        Rounded.fill(g, knobX, knobY, knobSize, knobSize, knobR, knobCol);
        Rounded.outline(g, knobX, knobY, knobSize, knobSize, knobR, 1f, 0x33FFFFFF);

        double cur = (getter != null) ? getter.getAsDouble() : min;
        String label = labeler != null ? labeler.apply(cur) : (onlyInt ? Integer.toString((int) cur) : Double.toString(cur));
        int tx = x + (w - ReworkedFont.width(label)) / 2;
        int ty = y + 5;
        ReworkedFont.drawString(g, label, tx, ty,
                this.active ? ReworkedTheme.text : ReworkedTheme.textSubtle, false);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput out) {
        double cur = (getter != null) ? getter.getAsDouble() : min;
        out.add(NarratedElementType.TITLE, Component.literal(labeler != null ? labeler.apply(cur) : String.valueOf(cur)));
    }
}
