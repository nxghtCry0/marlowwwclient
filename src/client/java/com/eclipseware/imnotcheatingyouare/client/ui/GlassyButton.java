package com.eclipseware.imnotcheatingyouare.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;

import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public final class GlassyButton extends CompatAbstractWidget {
    public enum Style {
        NORMAL, PRIMARY, DANGER
    }

    private final Runnable onPress;
    private final boolean defaultActive;
    private final Style style;
    private boolean pressedVisual;
    
    private float hoverAnim = 0f;
    private long lastRenderTime = 0;

    public GlassyButton(int x, int y, int w, int h, Component message, Runnable onPress) {
        this(x, y, w, h, message, onPress, true, Style.NORMAL);
    }

    public GlassyButton(int x, int y, int w, int h, Component message, Runnable onPress, boolean defaultActive, Style style) {
        super(x, y, w, h, message);
        this.onPress = onPress;
        this.defaultActive = defaultActive;
        this.active = defaultActive;
        this.style = style;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event != null && this.active && this.visible && event.button() == 0 && isMouseOver(event.x(), event.y())) {
            this.pressedVisual = true;
            if (this.onPress != null) {
                this.onPress.run();
            }
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event != null && event.button() == 0) {
            this.pressedVisual = false;
        }
        return super.mouseReleased(event);
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

        float targetHover = (this.isHovered && this.active) ? 1f : 0f;
        float factor = 1f - (float)Math.exp(-15f * timeDelta);
        hoverAnim = hoverAnim + (targetHover - hoverAnim) * factor;

        int bg, border, text;

        if (!this.active) {
            bg = GlassyTheme.CONTROL_BG_DISABLED;
            border = GlassyTheme.CONTROL_BORDER;
            text = GlassyTheme.TEXT_MUTED;
        } else if (this.style == Style.PRIMARY) {
            int bgNormal = GlassyTheme.CONTROL_BG_STRONG;
            int bgHover = GlassyTheme.CONTROL_BG_STRONG_HOVER;
            bg = this.pressedVisual ? GlassyTheme.CONTROL_BG_STRONG_PRESSED : lerpRgb(bgNormal, bgHover, hoverAnim);
            
            int borderNormal = GlassyTheme.CONTROL_BORDER_STRONG;
            int borderHover = GlassyTheme.CONTROL_BORDER_STRONG_HOVER;
            border = lerpRgb(borderNormal, borderHover, hoverAnim);
            text = GlassyTheme.TEXT;
        } else if (this.style == Style.DANGER) {
            bg = this.pressedVisual ? 0xA0FF3333 : (this.isHovered ? 0x8AFF3333 : 0x70FF3333);
            border = this.isHovered ? 0xFFFFAAAA : 0xFFFF5555;
            text = GlassyTheme.TEXT;
        } else {
            int bgNormal = GlassyTheme.CONTROL_BG;
            int bgHover = GlassyTheme.CONTROL_BG_HOVER;
            bg = this.pressedVisual ? GlassyTheme.CONTROL_BG_STRONG_PRESSED : lerpRgb(bgNormal, bgHover, hoverAnim);
            
            int borderNormal = GlassyTheme.CONTROL_BORDER;
            int borderHover = GlassyTheme.CONTROL_BORDER_HOVER;
            border = lerpRgb(borderNormal, borderHover, hoverAnim);
            
            text = lerpRgb(GlassyTheme.TEXT_SUBTLE, GlassyTheme.TEXT, hoverAnim);
        }

        guiGraphics.fill(x, y, x + w, y + h, bg);
        guiGraphics.renderOutline(x, y, w, h, border);

        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        Component msg = getMessage();
        
        int msgW = font.width(msg);
        int textX = x + (w - msgW) / 2;
        int textY = y + (h - 9) / 2;
        
        guiGraphics.drawString(font, msg, textX, textY, text, false);
    }

    private static int lerpRgb(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int ar = a >> 16 & 0xFF;
        int ag = a >> 8 & 0xFF;
        int ab = a & 0xFF;
        int br = b >> 16 & 0xFF;
        int bg = b >> 8 & 0xFF;
        int bb = b & 0xFF;
        int r = Math.round(ar + (br - ar) * t);
        int g = Math.round(ag + (bg - ag) * t);
        int bl = Math.round(ab + (bb - ab) * t);
        int alpha = Math.round((a >>> 24) + ((b >>> 24) - (a >>> 24)) * t);
        return alpha << 24 | r << 16 | g << 8 | bl;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        narrationElementOutput.add(NarratedElementType.TITLE, getMessage());
    }
}
