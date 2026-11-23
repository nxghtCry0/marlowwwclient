package com.eclipseware.imnotcheatingyouare.client.glassui.model;

import com.eclipseware.imnotcheatingyouare.client.glassui.render.UiRenderPrimitives;
import com.eclipseware.imnotcheatingyouare.client.glassui.theme.UiTheme;
import java.util.function.Function;
import net.minecraft.client.gui.GuiGraphicsExtractor;

@FunctionalInterface
public interface UiHeaderAccessory {
    float render(
        GuiGraphicsExtractor graphics,
        float rightEdgeX,
        float headerY,
        float headerH,
        float alpha,
        UiTheme theme
    );

    static UiHeaderAccessory dualColorSwatch(Function<UiTheme, int[]> colors) {
        return (graphics, rightEdgeX, headerY, headerH, alpha, theme) -> {
            int[] pair = colors.apply(theme);
            int primary = pair[0];
            int secondary = pair[1];
            float sw2X = rightEdgeX - 16f;
            float sw1X = sw2X - 18f;
            float swY = headerY + 11.5f;
            int borderCol = UiRenderPrimitives.argb(Math.round(190f * alpha), 255, 255, 255);
            int c1 = UiRenderPrimitives.multiplyAlpha(primary, alpha);
            int c2 = UiRenderPrimitives.multiplyAlpha(secondary, alpha);
            UiRenderPrimitives.drawRoundedRect(graphics, sw1X, swY, 15f, 15f, 7.5f, c1);
            UiRenderPrimitives.drawRoundedBorder(graphics, sw1X, swY, 15f, 15f, 7.5f, 0.9f, borderCol, borderCol);
            UiRenderPrimitives.drawRoundedRect(graphics, sw2X, swY, 15f, 15f, 7.5f, c2);
            UiRenderPrimitives.drawRoundedBorder(graphics, sw2X, swY, 15f, 15f, 7.5f, 0.9f, borderCol, borderCol);
            return 42f;
        };
    }
}
