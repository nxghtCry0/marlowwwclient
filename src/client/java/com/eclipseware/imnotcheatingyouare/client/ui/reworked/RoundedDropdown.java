package com.eclipseware.imnotcheatingyouare.client.ui.reworked;

import com.eclipseware.imnotcheatingyouare.client.ui.CompatAbstractWidget;
import com.eclipseware.imnotcheatingyouare.client.ui.GuiGraphics;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public final class RoundedDropdown<T> extends CompatAbstractWidget {

    private final List<T> options;
    private final Supplier<T> getter;
    private final Consumer<T> setter;
    private final Function<T, String> labeler;

    private boolean menuOpen = false;
    private float openAnim = 0f;
    private float caretRot = 0f;
    private int hoveredIndex = -1;
    private long lastRenderTime = 0;

    public RoundedDropdown(int x, int y, int w, int h,
                           List<T> options, Supplier<T> getter, Consumer<T> setter,
                           Function<T, String> labeler) {
        super(x, y, w, h, Component.empty());
        this.options = options;
        this.getter = getter;
        this.setter = setter;
        this.labeler = labeler;
        this.active = true;
    }

    public boolean isMenuOpen() { return menuOpen; }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event != null && this.active && this.visible && event.button() == 0) {
            double mx = event.x(), my = event.y();

            if (this.menuOpen) {
                int itemH = 18;
                int menuY = getY() + getHeight() + 2;
                int menuH = options.size() * itemH;
                if (mx >= getX() && mx < getX() + getWidth() && my >= menuY && my < menuY + menuH) {
                    int idx = (int) ((my - menuY) / itemH);
                    if (idx >= 0 && idx < options.size()) {
                        if (setter != null) setter.accept(options.get(idx));
                        menuOpen = false;
                        return true;
                    }
                }
                menuOpen = false;
                return true;
            } else if (isMouseOver(mx, my)) {
                menuOpen = true;
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        long now = System.currentTimeMillis();
        if (lastRenderTime == 0) lastRenderTime = now;
        float dt = Math.min(0.1f, (now - lastRenderTime) / 1000f);
        lastRenderTime = now;

        float openTarget = menuOpen ? 1f : 0f;
        float k = 18f * ReworkedTheme.animSpeed;
        float factor = 1f - (float) Math.exp(-k * dt);
        openAnim = openAnim + (openTarget - openAnim) * factor;
        caretRot = caretRot + (openTarget - caretRot) * factor;

        int x = getX(), y = getY(), w = getWidth(), h = getHeight();
        int r = Math.min(ReworkedTheme.radius, Math.min(w, h) / 2);

        int body = this.active
                ? (this.isHovered || menuOpen ? ReworkedTheme.controlBgHover : ReworkedTheme.controlBg)
                : 0x20101010;
        Rounded.fill(g, x, y, w, h, r, body);

        int border = this.active
                ? Rounded.lerpArgb(ReworkedTheme.controlBorder, ReworkedTheme.controlBorderHover, Math.max(this.isHovered ? 1f : 0f, openAnim))
                : ReworkedTheme.controlBorder;
        Rounded.outline(g, x, y, w, h, r, 1f, border);

        T current = getter != null ? getter.get() : null;
        String label = (current != null && labeler != null) ? labeler.apply(current) : "-";
        ReworkedFont.drawString(g, label, x + 10, y + (h - 8) / 2,
                this.active ? ReworkedTheme.text : ReworkedTheme.textSubtle, false);

        int cx = x + w - 14;
        int cy = y + h / 2;
        int caretCol = this.active ? ReworkedTheme.accent : ReworkedTheme.textSubtle;
        if (caretRot < 0.5f) {
            g.extractor().fill(cx, cy - 1, cx + 7, cy, caretCol);
            g.extractor().fill(cx + 1, cy, cx + 6, cy + 1, caretCol);
            g.extractor().fill(cx + 2, cy + 1, cx + 5, cy + 2, caretCol);
        } else {
            g.extractor().fill(cx + 2, cy - 2, cx + 5, cy - 1, caretCol);
            g.extractor().fill(cx + 1, cy - 1, cx + 6, cy, caretCol);
            g.extractor().fill(cx, cy, cx + 7, cy + 1, caretCol);
        }
    }

    public void renderOverlay(GuiGraphics g, int mouseX, int mouseY) {
        if (openAnim < 0.001f) return;

        int x = getX(), y = getY(), w = getWidth(), h = getHeight();
        int r = Math.min(ReworkedTheme.radius, Math.min(w, h) / 2);

        int itemH = 18;
        int menuY = y + h + 2;
        int fullMenuH = options.size() * itemH;
        int menuH = (int) (fullMenuH * easeOutCubic(openAnim));
        if (menuH < 2) return;

        Rounded.shadow(g, x, menuY, w, menuH, r);
        Rounded.fill(g, x, menuY, w, menuH, r, ReworkedTheme.background);

        g.extractor().enableScissor(x, menuY, x + w, menuY + menuH);

        hoveredIndex = -1;
        for (int i = 0; i < options.size(); i++) {
            T option = options.get(i);
            int itemY = menuY + i * itemH;

            boolean isHover = mouseX >= x && mouseX < x + w && mouseY >= itemY && mouseY < itemY + itemH;
            if (isHover && mouseY < menuY + menuH) hoveredIndex = i;

            T current = getter != null ? getter.get() : null;
            boolean isCurrent = Objects.equals(current, option);

            int rowR = Math.min(r, Math.min(w, itemH) / 2);
            if (isHover && mouseY < menuY + menuH) {
                Rounded.fill(g, x + 2, itemY, w - 4, itemH, rowR, ReworkedTheme.rowHover);
            }
            if (isCurrent) {
                Rounded.fill(g, x + 2, itemY + 2, 3, itemH - 4, 1, ReworkedTheme.accent);
            }

            String optLabel = labeler != null ? labeler.apply(option) : String.valueOf(option);
            int textColor = isCurrent ? ReworkedTheme.accent : (isHover ? ReworkedTheme.text : ReworkedTheme.textMuted);
            ReworkedFont.drawString(g, optLabel, x + 10, itemY + (itemH - 8) / 2, textColor, false);
        }

        g.extractor().disableScissor();

        Rounded.outline(g, x, menuY, w, menuH, r, 1f, ReworkedTheme.controlBorderHover);
    }

    private static float easeOutCubic(float t) {
        t = Math.max(0f, Math.min(1f, t));
        return 1f - (float) Math.pow(1f - t, 3f);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput out) {
        T current = getter != null ? getter.get() : null;
        out.add(NarratedElementType.TITLE, Component.literal(current != null ? String.valueOf(current) : "-"));
    }
}
