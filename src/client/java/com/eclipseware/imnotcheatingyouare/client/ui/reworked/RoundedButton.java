package com.eclipseware.imnotcheatingyouare.client.ui.reworked;

import com.eclipseware.imnotcheatingyouare.client.ui.CompatAbstractWidget;
import com.eclipseware.imnotcheatingyouare.client.ui.GuiGraphics;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public final class RoundedButton extends CompatAbstractWidget {

    public enum Style { NORMAL, PRIMARY, DANGER, SUBTLE }

    private final Runnable onPress;
    private final Style style;

    private float hoverAnim = 0f;
    private float pressAnim = 0f;
    private boolean pressedVisual;
    private long lastRenderTime = 0;

    public RoundedButton(int x, int y, int w, int h, String label, Runnable onPress) {
        this(x, y, w, h, label, onPress, Style.NORMAL);
    }

    public RoundedButton(int x, int y, int w, int h, String label, Runnable onPress, Style style) {
        super(x, y, w, h, Component.literal(label));
        this.onPress = onPress;
        this.style = style;
        this.active = true;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event != null && this.active && this.visible
                && event.button() == 0 && isMouseOver(event.x(), event.y())) {
            this.pressedVisual = true;
            this.pressAnim = 1f;
            if (this.onPress != null) this.onPress.run();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event != null && event.button() == 0) this.pressedVisual = false;
        return super.mouseReleased(event);
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        long now = System.currentTimeMillis();
        if (lastRenderTime == 0) lastRenderTime = now;
        float dt = Math.min(0.1f, (now - lastRenderTime) / 1000f);
        lastRenderTime = now;

        float k = 14f * ReworkedTheme.animSpeed;
        float hoverTarget = (this.isHovered && this.active) ? 1f : 0f;
        hoverAnim = approach(hoverAnim, hoverTarget, k, dt);
        float pressTarget = this.pressedVisual ? 1f : 0f;
        pressAnim = approach(pressAnim, pressTarget, 22f * ReworkedTheme.animSpeed, dt);

        int x = getX(), y = getY(), w = getWidth(), h = getHeight();
        int r = Math.min(ReworkedTheme.radius, Math.min(w, h) / 2);

        int bodyNormal, bodyHover, text;
        if (!this.active) {
            bodyNormal = bodyHover = ReworkedTheme.controlBg;
            text = ReworkedTheme.textSubtle;
        } else if (style == Style.PRIMARY) {
            bodyNormal = ReworkedTheme.accent;
            bodyHover  = Rounded.lerpArgb(ReworkedTheme.accent, 0xFFFFFFFF, 0.18f);
            text = 0xFFFFFFFF;
        } else if (style == Style.DANGER) {
            bodyNormal = 0x80FF3333;
            bodyHover  = 0xB0FF3333;
            text = 0xFFFFFFFF;
        } else if (style == Style.SUBTLE) {
            bodyNormal = 0x10FFFFFF;
            bodyHover  = 0x20FFFFFF;
            text = ReworkedTheme.text;
        } else {
            bodyNormal = ReworkedTheme.controlBg;
            bodyHover  = ReworkedTheme.controlBgHover;
            text = Rounded.lerpArgb(ReworkedTheme.textSubtle, ReworkedTheme.text, hoverAnim);
        }

        int body = Rounded.lerpArgb(bodyNormal, bodyHover, hoverAnim);
        body = Rounded.lerpArgb(body, 0xFF000000, pressAnim * 0.18f);

        float scale = 1f - pressAnim * 0.03f;
        int drawW = (int) Math.max(1, w * scale);
        int drawH = (int) Math.max(1, h * scale);
        int drawX = x + (w - drawW) / 2;
        int drawY = y + (h - drawH) / 2;
        int drawR = Math.min(r, Math.min(drawW, drawH) / 2);

        if (this.active && (style == Style.PRIMARY || style == Style.DANGER)) {
            Rounded.shadow(g, drawX, drawY, drawW, drawH, drawR);
        }

        Rounded.fill(g, drawX, drawY, drawW, drawH, drawR, body);

        if (this.active && hoverAnim > 0.01f && ReworkedTheme.hoverGlow > 0.01f) {
            int glowAlpha = (int) (ReworkedTheme.hoverGlow * hoverAnim * 60);
            Rounded.fill(g, drawX, drawY, drawW, drawH, drawR, (glowAlpha << 24) | 0x00FFFFFF);
        }

        int border = this.active
                ? Rounded.lerpArgb(ReworkedTheme.controlBorder, ReworkedTheme.controlBorderHover, hoverAnim)
                : ReworkedTheme.controlBorder;
        Rounded.outline(g, drawX, drawY, drawW, drawH, drawR, 1f, border);

        String label = getMessage().getString();
        int tw = ReworkedFont.width(label);
        int tx = drawX + (drawW - tw) / 2;
        int ty = drawY + (drawH - 8) / 2;
        ReworkedFont.drawString(g, label, tx, ty, text, false);
    }

    private static float approach(float current, float target, float k, float dt) {
        float factor = 1f - (float) Math.exp(-k * dt);
        return current + (target - current) * factor;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput out) {
        out.add(NarratedElementType.TITLE, getMessage());
    }
}
