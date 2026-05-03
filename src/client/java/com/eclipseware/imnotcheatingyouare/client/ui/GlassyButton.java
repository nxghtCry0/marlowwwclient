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
    protected void renderWidget(DrawContext DrawContext, int mouseX, int mouseY, float partialTick) {
        int x = getX();
        int y = getY();
        int w = getWidth();
        int h = getHeight();

        int bg, border, text;

        if (!this.active) {
            bg = GlassyTheme.CONTROL_BG_DISABLED;
            border = GlassyTheme.CONTROL_BORDER;
            text = GlassyTheme.TEXT_MUTED;
        } else if (this.style == Style.PRIMARY) {
            bg = this.pressedVisual ? GlassyTheme.CONTROL_BG_STRONG_PRESSED : (this.isHovered ? GlassyTheme.CONTROL_BG_STRONG_HOVER : GlassyTheme.CONTROL_BG_STRONG);
            border = this.isHovered ? GlassyTheme.CONTROL_BORDER_STRONG_HOVER : GlassyTheme.CONTROL_BORDER_STRONG;
            text = GlassyTheme.TEXT;
        } else if (this.style == Style.DANGER) {
            bg = this.pressedVisual ? 0xA0FF3333 : (this.isHovered ? 0x8AFF3333 : 0x70FF3333);
            border = this.isHovered ? 0xFFFFAAAA : 0xFFFF5555;
            text = GlassyTheme.TEXT;
        } else {
            bg = this.pressedVisual ? GlassyTheme.CONTROL_BG_STRONG_PRESSED : (this.isHovered ? GlassyTheme.CONTROL_BG_HOVER : GlassyTheme.CONTROL_BG);
            border = this.isHovered ? GlassyTheme.CONTROL_BORDER_HOVER : GlassyTheme.CONTROL_BORDER;
            text = this.isHovered ? GlassyTheme.TEXT : GlassyTheme.TEXT_SUBTLE;
        }

        DrawContext.fill(x, y, x + w, y + h, bg);
        DrawContext.renderOutline(x, y, w, h, border);

        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        Component msg = getMessage();
        
        int msgW = font.width(msg);
        int textX = x + (w - msgW) / 2;
        int textY = y + (h - 9) / 2;
        
        DrawContext.drawString(font, msg, textX, textY, text, false);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        narrationElementOutput.add(NarratedElementType.TITLE, getMessage());
    }
}
