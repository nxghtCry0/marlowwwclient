package com.eclipseware.imnotcheatingyouare.client.ui;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;

import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public final class GlassyDropdown<T> extends CompatAbstractWidget {
    public enum Style {
        NORMAL, PRIMARY
    }

    private final List<T> options;
    private final Supplier<T> getter;
    private final Consumer<T> setter;
    private final java.util.function.Function<T, Component> labeler;
    private final Style style;

    private boolean menuOpen;
    public boolean isMenuOpen() { return this.menuOpen; }
    private int hoveredIndex = -1;

    public GlassyDropdown(int x, int y, int w, int h, List<T> options, Supplier<T> getter, Consumer<T> setter, java.util.function.Function<T, Component> labeler) {
        this(x, y, w, h, options, getter, setter, labeler, Style.NORMAL);
    }

    public GlassyDropdown(int x, int y, int w, int h, List<T> options, Supplier<T> getter, Consumer<T> setter, java.util.function.Function<T, Component> labeler, Style style) {
        super(x, y, w, h, Component.empty());
        this.options = options;
        this.getter = getter;
        this.setter = setter;
        this.labeler = labeler;
        this.style = style;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event != null && this.active && this.visible && event.button() == 0) {
            double mx = event.x();
            double my = event.y();
            
            if (this.menuOpen) {
                if (this.hoveredIndex >= 0 && this.hoveredIndex < this.options.size()) {
                    if (this.setter != null) {
                        this.setter.accept(this.options.get(this.hoveredIndex));
                    }
                    this.menuOpen = false;
                    return true;
                } else {
                    this.menuOpen = false;
                    return true;
                }
            } else if (isMouseOver(mx, my)) {
                this.menuOpen = true;
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
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
            bg = this.menuOpen ? GlassyTheme.CONTROL_BG_STRONG_PRESSED : (this.isHovered ? GlassyTheme.CONTROL_BG_STRONG_HOVER : GlassyTheme.CONTROL_BG_STRONG);
            border = this.isHovered ? GlassyTheme.CONTROL_BORDER_STRONG_HOVER : GlassyTheme.CONTROL_BORDER_STRONG;
            text = GlassyTheme.TEXT;
        } else {
            bg = this.menuOpen ? GlassyTheme.CONTROL_BG_STRONG_PRESSED : (this.isHovered ? GlassyTheme.CONTROL_BG_HOVER : GlassyTheme.CONTROL_BG);
            border = this.isHovered ? GlassyTheme.CONTROL_BORDER_HOVER : GlassyTheme.CONTROL_BORDER;
            text = this.isHovered ? GlassyTheme.TEXT : GlassyTheme.TEXT_SUBTLE;
        }

        guiGraphics.fill(x, y, x + w, y + h, bg);
        guiGraphics.renderOutline(x, y, w, h, border);

        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        
        T current = (this.getter != null) ? this.getter.get() : null;
        Component msg = (current != null && this.labeler != null) ? this.labeler.apply(current) : Component.literal("-");
        
        int textX = x + 8;
        int textY = y + (h - 9) / 2;
        
        guiGraphics.drawString(font, msg, textX, textY, text, false);
        
        WwUiStyle.drawDropdownCaret(guiGraphics, x + w - 16, y, 16, h, this.active);

        if (this.menuOpen) {
            // The overlay will be rendered by renderOverlay.
            // We just need to update the hovered index here to ensure clicks work if not handled by renderOverlay.
            int menuY = y + h;
            int itemH = 14;
            this.hoveredIndex = -1;
            for (int i = 0; i < this.options.size(); i++) {
                int itemY = menuY + i * itemH;
                boolean isHovered = mouseX >= x && mouseX < x + w && mouseY >= itemY && mouseY < itemY + itemH;
                if (isHovered) {
                    this.hoveredIndex = i;
                }
            }
        } else {
            this.hoveredIndex = -1;
        }
    }

    public void renderOverlay(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (!this.menuOpen) return;
        int x = getX();
        int y = getY();
        int w = getWidth();
        int h = getHeight();
        
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        int textX = x + 8;
        
        T current = (this.getter != null) ? this.getter.get() : null;

        int menuY = y + h;
        int itemH = 14;
        int menuH = this.options.size() * itemH;
        
        guiGraphics.fill(x, menuY, x + w, menuY + menuH, GlassyTheme.PANEL_BG);
        guiGraphics.renderOutline(x, menuY, w, menuH, GlassyTheme.PANEL_BORDER_STRONG);
        
        this.hoveredIndex = -1;
        for (int i = 0; i < this.options.size(); i++) {
            T option = this.options.get(i);
            int itemY = menuY + i * itemH;
            
            boolean isHovered = mouseX >= x && mouseX < x + w && mouseY >= itemY && mouseY < itemY + itemH;
            if (isHovered) {
                this.hoveredIndex = i;
                guiGraphics.fill(x + 1, itemY, x + w - 1, itemY + itemH, GlassyTheme.ROW_HOVER_BG);
            }
            
            Component optMsg = (this.labeler != null) ? this.labeler.apply(option) : Component.literal("-");
            int optText = Objects.equals(current, option) ? GlassyTheme.ACCENT : GlassyTheme.TEXT;
            
            guiGraphics.drawString(font, optMsg, textX, itemY + (itemH - 9) / 2, optText, false);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        T current = (this.getter != null) ? this.getter.get() : null;
        Component msg = (current != null && this.labeler != null) ? this.labeler.apply(current) : Component.literal("-");
        narrationElementOutput.add(NarratedElementType.TITLE, msg);
    }
}
