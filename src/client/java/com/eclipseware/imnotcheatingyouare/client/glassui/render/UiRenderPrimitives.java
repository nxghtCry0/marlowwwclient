package com.eclipseware.imnotcheatingyouare.client.glassui.render;

import com.eclipseware.imnotcheatingyouare.client.glassui.asset.UiIcon;
import com.eclipseware.imnotcheatingyouare.client.glassui.theme.BorderStyle;
import com.eclipseware.imnotcheatingyouare.client.glassui.theme.UiTheme;
import com.eclipseware.imnotcheatingyouare.client.glassui.theme.UiThemeContext;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;

public final class UiRenderPrimitives {
    private static final int CORNER_SEGMENTS = 10;
    private static final int CIRCLE_SEGMENTS = 28;
    private static final float PI_F = (float) Math.PI;

    private UiRenderPrimitives() {
    }

    public static float approachExp(float current, float target, float speed, float dt) {
        float clampedDt = clamp(dt, 0f, 0.1f);
        float factor = 1f - (float) Math.exp(-speed * clampedDt);
        float next = current + (target - current) * factor;
        return Math.abs(target - next) < 0.0005f ? target : next;
    }

    public static float easeOutCubic(float t) {
        float clamped = clamp(t, 0f, 1f);
        float inv = 1f - clamped;
        return 1f - inv * inv * inv;
    }

    public static float easeOutBack(float t, float overshoot) {
        float clamped = clamp(t, 0f, 1f) - 1f;
        return 1f + (overshoot + 1f) * clamped * clamped * clamped + overshoot * clamped * clamped;
    }

    public static int argb(int a, int r, int g, int b) {
        return (clampInt(a, 0, 255) << 24)
            | (clampInt(r, 0, 255) << 16)
            | (clampInt(g, 0, 255) << 8)
            | clampInt(b, 0, 255);
    }

    public static int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (clampInt(alpha, 0, 255) << 24);
    }

    public static int multiplyAlpha(int color, float factor) {
        int baseA = (color >>> 24) & 0xFF;
        int nextA = clampInt(Math.round(baseA * clamp(factor, 0f, 1f)), 0, 255);
        return (color & 0x00FFFFFF) | (nextA << 24);
    }

    public static int lerpColor(int c0, int c1, float t) {
        float f = clamp(t, 0f, 1f);
        int a0 = (c0 >>> 24) & 0xFF;
        int r0 = (c0 >>> 16) & 0xFF;
        int g0 = (c0 >>> 8) & 0xFF;
        int b0 = c0 & 0xFF;

        int a1 = (c1 >>> 24) & 0xFF;
        int r1 = (c1 >>> 16) & 0xFF;
        int g1 = (c1 >>> 8) & 0xFF;
        int b1 = c1 & 0xFF;

        return argb(
            Math.round(a0 + (a1 - a0) * f),
            Math.round(r0 + (r1 - r0) * f),
            Math.round(g0 + (g1 - g0) * f),
            Math.round(b0 + (b1 - b0) * f)
        );
    }

    public static String ellipsize(String text, float maxWidthPx, float sizePx, SatoshiWeight weight) {
        if (text == null) {
            return "";
        }
        if (SatoshiFont.width(text, sizePx, weight) <= maxWidthPx) {
            return text;
        }
        String suffix = "...";
        int end = text.length();
        while (end > 0 && SatoshiFont.width(text.substring(0, end) + suffix, sizePx, weight) > maxWidthPx) {
            end--;
        }
        return text.substring(0, end).stripTrailing() + suffix;
    }

    public static void enableScissor(GuiGraphicsExtractor graphics, int x0, int y0, int x1, int y1) {
        if (StreamproofOverlay.isCapturingFrame()) {
            StreamproofOverlay.pushScissor(x0, y0, x1, y1);
            return;
        }
        graphics.enableScissor(x0, y0, x1, y1);
    }

    public static void disableScissor(GuiGraphicsExtractor graphics) {
        if (StreamproofOverlay.isCapturingFrame()) {
            StreamproofOverlay.popScissor();
            return;
        }
        graphics.disableScissor();
    }

    public static void drawRoundedRect(GuiGraphicsExtractor graphics, float x, float y, float w, float h, float radius, int color) {
        drawRoundedGradient(graphics, x, y, w, h, radius, color, color);
    }

    public static void drawRoundedGradient(
        GuiGraphicsExtractor graphics,
        float x,
        float y,
        float w,
        float h,
        float radius,
        int topColor,
        int bottomColor
    ) {
        if (w <= 0.5f || h <= 0.5f) {
            return;
        }
        if (((topColor >>> 24) & 0xFF) == 0 && ((bottomColor >>> 24) & 0xFF) == 0) {
            return;
        }
        if (StreamproofOverlay.isCapturingFrame()) {
            StreamproofOverlay.drawRoundedGradient(x, y, w, h, radius, topColor, bottomColor);
            return;
        }
        Matrix3x2f pose = new Matrix3x2f(graphics.pose());
        ScreenRectangle scissor = graphics.scissorStack.peek();
        ScreenRectangle bounds = computeBounds(x - 1f, y - 1f, x + w + 1f, y + h + 1f, pose, scissor);
        if (bounds == null) {
            return;
        }
        float clampedRadius = clamp(radius, 0f, Math.min(w, h) * 0.5f);
        graphics.guiRenderState.addGuiElement(new SmoothRoundedRectState(pose, x, y, w, h, clampedRadius, topColor, bottomColor, scissor, bounds));
    }

    public static void drawRoundedBorder(
        GuiGraphicsExtractor graphics,
        float x,
        float y,
        float w,
        float h,
        float radius,
        float thickness,
        int topColor,
        int bottomColor
    ) {
        if (w <= 1f || h <= 1f || thickness <= 0.05f) {
            return;
        }
        if (((topColor >>> 24) & 0xFF) == 0 && ((bottomColor >>> 24) & 0xFF) == 0) {
            return;
        }
        if (StreamproofOverlay.isCapturingFrame()) {
            StreamproofOverlay.drawRoundedBorder(x, y, w, h, radius, thickness, topColor, bottomColor);
            return;
        }
        Matrix3x2f pose = new Matrix3x2f(graphics.pose());
        ScreenRectangle scissor = graphics.scissorStack.peek();
        ScreenRectangle bounds = computeBounds(x - 2f, y - 2f, x + w + 2f, y + h + 2f, pose, scissor);
        if (bounds == null) {
            return;
        }
        float clampedRadius = clamp(radius, 0f, Math.min(w, h) * 0.5f);
        graphics.guiRenderState.addGuiElement(new SmoothRoundedBorderState(pose, x, y, w, h, clampedRadius, thickness, topColor, bottomColor, scissor, bounds));
    }

    public static void drawRoundedBorder(GuiGraphicsExtractor graphics, float x, float y, float w, float h, float radius, float thickness, int color) {
        drawRoundedBorder(graphics, x, y, w, h, radius, thickness, color, color);
    }

    public static void drawRoundedShadow(
        GuiGraphicsExtractor graphics,
        float x,
        float y,
        float w,
        float h,
        float radius,
        float spread,
        int shadowColor
    ) {
        if (w <= 1f || h <= 1f || spread <= 0.5f || ((shadowColor >>> 24) & 0xFF) == 0) {
            return;
        }
        if (StreamproofOverlay.isCapturingFrame()) {
            StreamproofOverlay.drawRoundedShadow(x, y, w, h, radius, spread, shadowColor);
            return;
        }
        Matrix3x2f pose = new Matrix3x2f(graphics.pose());
        ScreenRectangle scissor = graphics.scissorStack.peek();
        ScreenRectangle bounds = computeBounds(x - spread - 1f, y - spread - 1f, x + w + spread + 1f, y + h + spread + 1f, pose, scissor);
        if (bounds == null) {
            return;
        }
        float clampedRadius = clamp(radius, 0f, Math.min(w, h) * 0.5f);
        graphics.guiRenderState.addGuiElement(new SmoothRoundedShadowState(pose, x, y, w, h, clampedRadius, spread, shadowColor, scissor, bounds));
    }

    public static void drawGlassCard(
        GuiGraphicsExtractor graphics,
        float x,
        float y,
        float w,
        float h,
        float radius,
        int topFill,
        int bottomFill,
        int topBorder,
        int bottomBorder
    ) {
        if (StreamproofOverlay.isCapturingFrame()) {
            StreamproofOverlay.recordGlassCard(x, y, w, h, radius, topFill, bottomFill, topBorder, bottomBorder);
            return;
        }
        drawRoundedGradient(graphics, x, y, w, h, radius, topFill, bottomFill);
        drawRoundedBorder(graphics, x, y, w, h, radius, 1.0f, topBorder, bottomBorder);
    }

    public static void drawThemedGlassCard(
        GuiGraphicsExtractor graphics,
        float x,
        float y,
        float w,
        float h,
        float radius,
        float baseTopAlpha,
        float baseBotAlpha,
        float baseRimTopAlpha,
        float baseRimBotAlpha,
        float alpha,
        float activeTintAmount,
        float hoverAmount,
        UiTheme theme
    ) {
        float frost = theme.cardFrostScale();
        float glowScale = theme.activeGlowScale();
        BorderStyle borderStyle = theme.borderStyle();
        int accent = theme.accentColor();
        int secondary = theme.secondaryAccentColor();

        float rimMult = switch (borderStyle) {
            case CRISP -> 1.42f;
            case MINIMAL -> 0.44f;
            default -> 1.0f;
        };

        int topFillWhite = argb(clampInt(Math.round((baseTopAlpha * frost) * alpha), 0, 245), 255, 255, 255);
        int botFillWhite = argb(clampInt(Math.round((baseBotAlpha * frost) * alpha), 0, 235), 255, 255, 255);

        float tintBlend = clamp(activeTintAmount * glowScale * 0.42f, 0f, 0.55f);
        int topFill = tintBlend > 0.01f
            ? lerpColor(topFillWhite, multiplyAlpha(accent, ((baseTopAlpha * frost + 28f) / 255f) * alpha), tintBlend)
            : topFillWhite;
        int botFill = tintBlend > 0.01f
            ? lerpColor(botFillWhite, multiplyAlpha(secondary, ((baseBotAlpha * frost + 18f) / 255f) * alpha), tintBlend * 0.85f)
            : botFillWhite;

        int whiteRimTop = argb(clampInt(Math.round((baseRimTopAlpha * rimMult) * alpha), 0, 255), 255, 255, 255);
        int whiteRimBot = argb(clampInt(Math.round((baseRimBotAlpha * rimMult) * alpha), 0, 255), 255, 255, 255);

        float rimAccentBlend = borderStyle == BorderStyle.ACCENT
            ? clamp(Math.max(activeTintAmount * 0.88f, hoverAmount * 0.48f), 0f, 0.92f)
            : clamp(activeTintAmount * glowScale * 0.45f, 0f, 0.65f);

        int topRim = rimAccentBlend > 0.01f
            ? lerpColor(whiteRimTop, multiplyAlpha(accent, clamp(0.78f * alpha, 0f, 1f)), rimAccentBlend)
            : whiteRimTop;
        int botRim = rimAccentBlend > 0.01f
            ? lerpColor(whiteRimBot, multiplyAlpha(secondary, clamp(0.42f * alpha, 0f, 1f)), rimAccentBlend)
            : whiteRimBot;

        drawGlassCard(graphics, x, y, w, h, radius, topFill, botFill, topRim, botRim);
    }

    public static void drawIconBadge(
        GuiGraphicsExtractor graphics,
        UiIcon icon,
        float bx,
        float by,
        float size,
        float activeBlend,
        float alpha,
        UiTheme theme
    ) {
        int accent = theme.accentColor();
        float r = Math.min(size * 0.45f, 6.2f * theme.cornerRadiusScale());
        int bg = lerpColor(argb(Math.round(26f * alpha), 255, 255, 255), multiplyAlpha(accent, 0.24f * alpha), activeBlend);
        int rim = lerpColor(argb(Math.round(42f * alpha), 255, 255, 255), multiplyAlpha(accent, 0.65f * alpha), activeBlend);
        drawRoundedRect(graphics, bx, by, size, size, r, bg);
        drawRoundedBorder(graphics, bx, by, size, size, r, 0.8f, rim, rim);
        float iconSz = size * 0.54f;
        int iconTint = lerpColor(argb(Math.round(210f * alpha), 255, 255, 255), multiplyAlpha(accent, alpha), activeBlend);
        drawIcon(graphics, icon, bx + (size - iconSz) * 0.5f, by + (size - iconSz) * 0.5f, iconSz, iconTint);
    }

    public static void drawSwitch(
        GuiGraphicsExtractor graphics,
        float x,
        float y,
        float w,
        float h,
        float t,
        float hover,
        float alpha,
        UiTheme theme
    ) {
        int accent = theme.accentColor();
        int offTrack = argb(Math.round((56f + 22f * hover) * alpha), 255, 255, 255);
        int onTrack = multiplyAlpha(accent, alpha);
        int trackColor = lerpColor(offTrack, onTrack, t);
        float r = h * 0.5f;
        drawRoundedRect(graphics, x, y, w, h, r, trackColor);

        float pad = 1.6f;
        float baseKnobD = h - pad * 2f;
        float midStretch = (1f - Math.abs(t * 2f - 1f)) * 2.2f;
        float knobW = baseKnobD + midStretch;
        float travel = Math.max(0f, w - pad * 2f - knobW);
        float knobX = x + pad + travel * t;
        float knobY = y + pad;

        drawRoundedShadow(graphics, knobX, knobY + 0.6f, knobW, baseKnobD, baseKnobD * 0.5f, 2.5f, argb(Math.round(75f * alpha), 0, 0, 0));
        drawRoundedRect(graphics, knobX, knobY, knobW, baseKnobD, baseKnobD * 0.5f, argb(Math.round(255f * alpha), 255, 255, 255));
    }

    public static void drawIcon(GuiGraphicsExtractor graphics, UiIcon icon, float x, float y, float size, int color) {
        if (StreamproofOverlay.isCapturingFrame()) {
            StreamproofOverlay.drawIcon(icon, x, y, size, color);
            return;
        }
        drawTexturedQuad(graphics, icon.texture(), x, y, size, size, 0f, 0f, 1f, 1f, color);
    }

    public static void drawTexturedQuad(
        GuiGraphicsExtractor graphics,
        Identifier textureId,
        float x,
        float y,
        float w,
        float h,
        float u0,
        float v0,
        float u1,
        float v1,
        int color
    ) {
        if (w <= 0.2f || h <= 0.2f || ((color >>> 24) & 0xFF) == 0) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        TextureManager tm = mc.getTextureManager();
        AbstractTexture tex = tm.getTexture(textureId);
        TextureSetup setup = TextureSetup.singleTexture(tex.getTextureView(), tex.getSampler());
        Matrix3x2f pose = new Matrix3x2f(graphics.pose());
        ScreenRectangle scissor = graphics.scissorStack.peek();
        ScreenRectangle bounds = computeBounds(x, y, x + w, y + h, pose, scissor);
        if (bounds == null) {
            return;
        }
        graphics.guiRenderState.addGuiElement(
            new SmoothTexturedQuadState(setup, pose, x, y, x + w, y + h, u0, v0, u1, v1, color, scissor, bounds)
        );
    }

    public static float screenPixelSize() {
        Minecraft mc = Minecraft.getInstance();
        Window window = mc.getWindow();
        int scale = Math.max(1, window.getGuiScale());
        return 1f / scale;
    }

    public static void drawCircleOutline(GuiGraphicsExtractor graphics, float cx, float cy, float radius, float thickness, int color) {
        if (radius <= 0.5f || thickness <= 0.05f || ((color >>> 24) & 0xFF) == 0) {
            return;
        }
        Matrix3x2f pose = new Matrix3x2f(graphics.pose());
        ScreenRectangle scissor = graphics.scissorStack.peek();
        float pad = radius + thickness + 1.5f;
        ScreenRectangle bounds = computeBounds(cx - pad, cy - pad, cx + pad, cy + pad, pose, scissor);
        if (bounds == null) {
            return;
        }
        graphics.guiRenderState.addGuiElement(new CircleRingState(pose, cx, cy, radius, thickness, color, scissor, bounds));
    }

    public static void drawCircleFilled(GuiGraphicsExtractor graphics, float cx, float cy, float radius, int color) {
        if (radius <= 0.25f || ((color >>> 24) & 0xFF) == 0) {
            return;
        }
        Matrix3x2f pose = new Matrix3x2f(graphics.pose());
        ScreenRectangle scissor = graphics.scissorStack.peek();
        float pad = radius + 1.5f;
        ScreenRectangle bounds = computeBounds(cx - pad, cy - pad, cx + pad, cy + pad, pose, scissor);
        if (bounds == null) {
            return;
        }
        graphics.guiRenderState.addGuiElement(new CircleFillState(pose, cx, cy, radius, color, scissor, bounds));
    }

    public static void drawCircularPlayerAvatar(
        GuiGraphicsExtractor graphics,
        Identifier skinTexture,
        float cx,
        float cy,
        float radius,
        int tintColor
    ) {
        if (radius <= 1f || ((tintColor >>> 24) & 0xFF) == 0) {
            return;
        }
        if (StreamproofOverlay.isCapturingFrame()) {
            StreamproofOverlay.drawCircularAvatar(skinTexture, cx, cy, radius, tintColor);
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        TextureManager tm = mc.getTextureManager();
        AbstractTexture tex = tm.getTexture(skinTexture);
        TextureSetup setup = TextureSetup.singleTexture(tex.getTextureView(), tex.getSampler());
        Matrix3x2f pose = new Matrix3x2f(graphics.pose());
        ScreenRectangle scissor = graphics.scissorStack.peek();
        ScreenRectangle bounds = computeBounds(cx - radius - 1f, cy - radius - 1f, cx + radius + 1f, cy + radius + 1f, pose, scissor);
        if (bounds == null) {
            return;
        }
        graphics.guiRenderState.addGuiElement(
            new CircularAvatarState(setup, pose, cx, cy, radius, 8f / 64f, 8f / 64f, 16f / 64f, 16f / 64f, tintColor, scissor, bounds)
        );
        graphics.guiRenderState.addGuiElement(
            new CircularAvatarState(setup, pose, cx, cy, radius, 40f / 64f, 8f / 64f, 48f / 64f, 16f / 64f, tintColor, scissor, bounds)
        );
    }

    private static ScreenRectangle computeBounds(float x0, float y0, float x1, float y1, Matrix3x2fc pose, ScreenRectangle scissor) {
        int ix0 = (int) Math.floor(x0);
        int iy0 = (int) Math.floor(y0);
        int iw = Math.max(1, (int) Math.ceil(x1) - ix0);
        int ih = Math.max(1, (int) Math.ceil(y1) - iy0);
        ScreenRectangle raw = new ScreenRectangle(ix0, iy0, iw, ih);
        ScreenRectangle transformed = raw.transformMaxBounds(pose);
        return scissor != null ? scissor.intersection(transformed) : transformed;
    }

    static void emitQuad(
        VertexConsumer vc,
        Matrix3x2fc pose,
        float ax, float ay, int ac,
        float bx, float by, int bc,
        float cx, float cy, int cc,
        float dx, float dy, int dc
    ) {
        float cross1 = (bx - ax) * (dy - ay) - (by - ay) * (dx - ax);
        float cross2 = (bx - ax) * (cy - ay) - (by - ay) * (cx - ax);
        boolean flip = Math.abs(cross1) > 1e-5f ? cross1 > 0f : cross2 > 0f;
        if (!flip) {
            emitColorVertex(vc, pose, ax, ay, ac);
            emitColorVertex(vc, pose, bx, by, bc);
            emitColorVertex(vc, pose, cx, cy, cc);
            emitColorVertex(vc, pose, dx, dy, dc);
        } else {
            emitColorVertex(vc, pose, ax, ay, ac);
            emitColorVertex(vc, pose, dx, dy, dc);
            emitColorVertex(vc, pose, cx, cy, cc);
            emitColorVertex(vc, pose, bx, by, bc);
        }
    }

    private static void emitColorVertex(VertexConsumer vc, Matrix3x2fc pose, float vx, float vy, int col) {
        VertexConsumer step = vc.addVertexWith2DPose(pose, vx, vy);
        step.setColor(col);
    }

    record PerimeterSample(float x, float y, float nx, float ny) {
    }

    static List<PerimeterSample> buildPerimeter(float x, float y, float w, float h, float r) {
        List<PerimeterSample> out = new ArrayList<>((CORNER_SEGMENTS + 1) * 4);
        float[][] corners = new float[][]{
            {x + w - r, y + r, -PI_F * 0.5f, 0f},
            {x + w - r, y + h - r, 0f, PI_F * 0.5f},
            {x + r, y + h - r, PI_F * 0.5f, PI_F},
            {x + r, y + r, PI_F, PI_F * 1.5f}
        };
        for (float[] corner : corners) {
            float cx = corner[0];
            float cy = corner[1];
            float a0 = corner[2];
            float a1 = corner[3];
            for (int i = 0; i <= CORNER_SEGMENTS; i++) {
                float t = (float) i / CORNER_SEGMENTS;
                float ang = a0 + (a1 - a0) * t;
                float nx = (float) Math.cos(ang);
                float ny = (float) Math.sin(ang);
                out.add(new PerimeterSample(cx + nx * r, cy + ny * r, nx, ny));
            }
        }
        return out;
    }

    private static float clamp(float val, float min, float max) {
        return Math.max(min, Math.min(max, val));
    }

    private static int clampInt(int val, int min, int max) {
        return Math.max(min, Math.min(max, val));
    }

    private record SmoothRoundedRectState(
        Matrix3x2fc pose,
        float x,
        float y,
        float w,
        float h,
        float radius,
        int topColor,
        int bottomColor,
        ScreenRectangle scissorArea,
        ScreenRectangle bounds
    ) implements GuiElementRenderState {
        @Override
        public RenderPipeline pipeline() {
            return RenderPipelines.GUI;
        }

        @Override
        public TextureSetup textureSetup() {
            return TextureSetup.noTexture();
        }

        private int colorAtY(float py) {
            float t = clamp((py - y) / Math.max(1f, h), 0f, 1f);
            return lerpColor(topColor, bottomColor, t);
        }

        @Override
        public void buildVertices(VertexConsumer vc) {
            float aa = 0.45f;
            float ix = x + aa;
            float iy = y + aa;
            float iw = Math.max(0.1f, w - aa * 2f);
            float ih = Math.max(0.1f, h - aa * 2f);
            float ir = Math.max(0f, Math.min(radius - aa, Math.min(iw, ih) * 0.5f));

            if (ir <= 0.1f) {
                int cTop = colorAtY(iy);
                int cBot = colorAtY(iy + ih);
                emitQuad(vc, pose, ix, iy, cTop, ix, iy + ih, cBot, ix + iw, iy + ih, cBot, ix + iw, iy, cTop);
            } else {
                float prevY = iy;
                float prevInset = ir;
                for (int i = 1; i <= CORNER_SEGMENTS; i++) {
                    float theta = ((float) i / CORNER_SEGMENTS) * (PI_F * 0.5f);
                    float curY = iy + ir * (1f - (float) Math.cos(theta));
                    float curInset = ir * (1f - (float) Math.sin(theta));
                    int c0 = colorAtY(prevY);
                    int c1 = colorAtY(curY);
                    emitQuad(vc, pose, ix + prevInset, prevY, c0, ix + curInset, curY, c1, ix + iw - curInset, curY, c1, ix + iw - prevInset, prevY, c0);
                    prevY = curY;
                    prevInset = curInset;
                }
                float midTop = iy + ir;
                float midBot = iy + ih - ir;
                if (midBot > midTop + 0.05f) {
                    int c0 = colorAtY(midTop);
                    int c1 = colorAtY(midBot);
                    emitQuad(vc, pose, ix, midTop, c0, ix, midBot, c1, ix + iw, midBot, c1, ix + iw, midTop, c0);
                }
                prevY = midBot;
                prevInset = 0f;
                for (int i = 1; i <= CORNER_SEGMENTS; i++) {
                    float theta = ((float) i / CORNER_SEGMENTS) * (PI_F * 0.5f);
                    float curY = midBot + ir * (float) Math.sin(theta);
                    float curInset = ir * (1f - (float) Math.cos(theta));
                    int c0 = colorAtY(prevY);
                    int c1 = colorAtY(curY);
                    emitQuad(vc, pose, ix + prevInset, prevY, c0, ix + curInset, curY, c1, ix + iw - curInset, curY, c1, ix + iw - prevInset, prevY, c0);
                    prevY = curY;
                    prevInset = curInset;
                }
            }

            List<PerimeterSample> ring = buildPerimeter(x, y, w, h, radius);
            int count = ring.size();
            for (int i = 0; i < count; i++) {
                PerimeterSample p0 = ring.get(i);
                PerimeterSample p1 = ring.get((i + 1) % count);
                int c0 = colorAtY(p0.y);
                int c1 = colorAtY(p1.y);
                emitQuad(
                    vc, pose,
                    p0.x - p0.nx * aa, p0.y - p0.ny * aa, c0,
                    p0.x + p0.nx * aa, p0.y + p0.ny * aa, withAlpha(c0, 0),
                    p1.x + p1.nx * aa, p1.y + p1.ny * aa, withAlpha(c1, 0),
                    p1.x - p1.nx * aa, p1.y - p1.ny * aa, c1
                );
            }
        }
    }

    private record SmoothRoundedBorderState(
        Matrix3x2fc pose,
        float x,
        float y,
        float w,
        float h,
        float radius,
        float thickness,
        int topColor,
        int bottomColor,
        ScreenRectangle scissorArea,
        ScreenRectangle bounds
    ) implements GuiElementRenderState {
        @Override
        public RenderPipeline pipeline() {
            return RenderPipelines.GUI;
        }

        @Override
        public TextureSetup textureSetup() {
            return TextureSetup.noTexture();
        }

        private int colorAtY(float py) {
            float t = clamp((py - y) / Math.max(1f, h), 0f, 1f);
            return lerpColor(topColor, bottomColor, t);
        }

        @Override
        public void buildVertices(VertexConsumer vc) {
            float aa = 0.55f;
            List<PerimeterSample> ring = buildPerimeter(x, y, w, h, radius);
            int count = ring.size();
            for (int i = 0; i < count; i++) {
                PerimeterSample p0 = ring.get(i);
                PerimeterSample p1 = ring.get((i + 1) % count);
                int c0 = colorAtY(p0.y);
                int c1 = colorAtY(p1.y);
                int c0Zero = withAlpha(c0, 0);
                int c1Zero = withAlpha(c1, 0);

                float out0x = p0.x + p0.nx * aa;
                float out0y = p0.y + p0.ny * aa;
                float out1x = p1.x + p1.nx * aa;
                float out1y = p1.y + p1.ny * aa;

                float inCore0x = p0.x - p0.nx * thickness;
                float inCore0y = p0.y - p0.ny * thickness;
                float inCore1x = p1.x - p1.nx * thickness;
                float inCore1y = p1.y - p1.ny * thickness;

                float inF0x = p0.x - p0.nx * (thickness + aa);
                float inF0y = p0.y - p0.ny * (thickness + aa);
                float inF1x = p1.x - p1.nx * (thickness + aa);
                float inF1y = p1.y - p1.ny * (thickness + aa);

                emitQuad(vc, pose, p0.x, p0.y, c0, out0x, out0y, c0Zero, out1x, out1y, c1Zero, p1.x, p1.y, c1);
                emitQuad(vc, pose, inCore0x, inCore0y, c0, p0.x, p0.y, c0, p1.x, p1.y, c1, inCore1x, inCore1y, c1);
                emitQuad(vc, pose, inF0x, inF0y, c0Zero, inCore0x, inCore0y, c0, inCore1x, inCore1y, c1, inF1x, inF1y, c1Zero);
            }
        }
    }

    private record SmoothRoundedShadowState(
        Matrix3x2fc pose,
        float x,
        float y,
        float w,
        float h,
        float radius,
        float spread,
        int shadowColor,
        ScreenRectangle scissorArea,
        ScreenRectangle bounds
    ) implements GuiElementRenderState {
        @Override
        public RenderPipeline pipeline() {
            return RenderPipelines.GUI;
        }

        @Override
        public TextureSetup textureSetup() {
            return TextureSetup.noTexture();
        }

        @Override
        public void buildVertices(VertexConsumer vc) {
            List<PerimeterSample> ring = buildPerimeter(x, y, w, h, radius);
            int count = ring.size();
            int midColor = multiplyAlpha(shadowColor, 0.38f);
            int zeroColor = withAlpha(shadowColor, 0);
            float midSpread = spread * 0.42f;
            for (int i = 0; i < count; i++) {
                PerimeterSample p0 = ring.get(i);
                PerimeterSample p1 = ring.get((i + 1) % count);
                float m0x = p0.x + p0.nx * midSpread;
                float m0y = p0.y + p0.ny * midSpread;
                float m1x = p1.x + p1.nx * midSpread;
                float m1y = p1.y + p1.ny * midSpread;
                float o0x = p0.x + p0.nx * spread;
                float o0y = p0.y + p0.ny * spread;
                float o1x = p1.x + p1.nx * spread;
                float o1y = p1.y + p1.ny * spread;
                emitQuad(vc, pose, p0.x, p0.y, shadowColor, m0x, m0y, midColor, m1x, m1y, midColor, p1.x, p1.y, shadowColor);
                emitQuad(vc, pose, m0x, m0y, midColor, o0x, o0y, zeroColor, o1x, o1y, zeroColor, m1x, m1y, midColor);
            }
        }
    }

    private record SmoothTexturedQuadState(
        TextureSetup textureSetup,
        Matrix3x2fc pose,
        float x0,
        float y0,
        float x1,
        float y1,
        float u0,
        float v0,
        float u1,
        float v1,
        int color,
        ScreenRectangle scissorArea,
        ScreenRectangle bounds
    ) implements GuiElementRenderState {
        @Override
        public RenderPipeline pipeline() {
            return RenderPipelines.GUI_TEXTURED;
        }

        @Override
        public void buildVertices(VertexConsumer vc) {
            emitTex(vc, pose, x0, y0, u0, v0, color);
            emitTex(vc, pose, x0, y1, u0, v1, color);
            emitTex(vc, pose, x1, y1, u1, v1, color);
            emitTex(vc, pose, x1, y0, u1, v0, color);
        }

        private static void emitTex(VertexConsumer vc, Matrix3x2fc pose, float vx, float vy, float u, float v, int col) {
            VertexConsumer step = vc.addVertexWith2DPose(pose, vx, vy);
            step.setUv(u, v);
            step.setColor(col);
        }
    }

    private record CircularAvatarState(
        TextureSetup textureSetup,
        Matrix3x2fc pose,
        float cx,
        float cy,
        float radius,
        float u0,
        float v0,
        float u1,
        float v1,
        int color,
        ScreenRectangle scissorArea,
        ScreenRectangle bounds
    ) implements GuiElementRenderState {
        @Override
        public RenderPipeline pipeline() {
            return RenderPipelines.GUI_TEXTURED;
        }

        @Override
        public void buildVertices(VertexConsumer vc) {
            float uc = (u0 + u1) * 0.5f;
            float vcMid = (v0 + v1) * 0.5f;
            float ur = (u1 - u0) * 0.5f;
            float vr = (v1 - v0) * 0.5f;
            for (int i = 0; i < CIRCLE_SEGMENTS; i += 2) {
                float a0 = ((float) i / CIRCLE_SEGMENTS) * (PI_F * 2f);
                float a1 = ((float) (i + 1) / CIRCLE_SEGMENTS) * (PI_F * 2f);
                float a2 = ((float) Math.min(CIRCLE_SEGMENTS, i + 2) / CIRCLE_SEGMENTS) * (PI_F * 2f);
                float c0x = (float) Math.cos(a0);
                float s0y = (float) Math.sin(a0);
                float c1x = (float) Math.cos(a1);
                float s1y = (float) Math.sin(a1);
                float c2x = (float) Math.cos(a2);
                float s2y = (float) Math.sin(a2);

                SmoothTexturedQuadState.emitTex(vc, pose, cx, cy, uc, vcMid, color);
                SmoothTexturedQuadState.emitTex(vc, pose, cx + c2x * radius, cy + s2y * radius, uc + c2x * ur, vcMid + s2y * vr, color);
                SmoothTexturedQuadState.emitTex(vc, pose, cx + c1x * radius, cy + s1y * radius, uc + c1x * ur, vcMid + s1y * vr, color);
                SmoothTexturedQuadState.emitTex(vc, pose, cx + c0x * radius, cy + s0y * radius, uc + c0x * ur, vcMid + s0y * vr, color);
            }
        }
    }

    private record CircleRingState(
        Matrix3x2fc pose,
        float cx,
        float cy,
        float radius,
        float thickness,
        int color,
        ScreenRectangle scissorArea,
        ScreenRectangle bounds
    ) implements GuiElementRenderState {
        @Override
        public RenderPipeline pipeline() {
            return RenderPipelines.GUI;
        }

        @Override
        public TextureSetup textureSetup() {
            return TextureSetup.noTexture();
        }

        @Override
        public void buildVertices(VertexConsumer vc) {
            float aa = 0.55f;
            float rIn0 = Math.max(0f, radius - thickness * 0.5f - aa);
            float rIn1 = Math.max(0f, radius - thickness * 0.5f);
            float rOut1 = radius + thickness * 0.5f;
            float rOut0 = rOut1 + aa;
            int zero = withAlpha(color, 0);
            for (int i = 0; i < CIRCLE_SEGMENTS; i++) {
                float a0 = ((float) i / CIRCLE_SEGMENTS) * (PI_F * 2f);
                float a1 = ((float) (i + 1) / CIRCLE_SEGMENTS) * (PI_F * 2f);
                float c0 = (float) Math.cos(a0);
                float s0 = (float) Math.sin(a0);
                float c1 = (float) Math.cos(a1);
                float s1 = (float) Math.sin(a1);
                emitQuad(vc, pose, cx + c0 * rIn0, cy + s0 * rIn0, zero, cx + c0 * rIn1, cy + s0 * rIn1, color, cx + c1 * rIn1, cy + s1 * rIn1, color, cx + c1 * rIn0, cy + s1 * rIn0, zero);
                emitQuad(vc, pose, cx + c0 * rIn1, cy + s0 * rIn1, color, cx + c0 * rOut1, cy + s0 * rOut1, color, cx + c1 * rOut1, cy + s1 * rOut1, color, cx + c1 * rIn1, cy + s1 * rIn1, color);
                emitQuad(vc, pose, cx + c0 * rOut1, cy + s0 * rOut1, color, cx + c0 * rOut0, cy + s0 * rOut0, zero, cx + c1 * rOut0, cy + s1 * rOut0, zero, cx + c1 * rOut1, cy + s1 * rOut1, color);
            }
        }
    }

    private record CircleFillState(
        Matrix3x2fc pose,
        float cx,
        float cy,
        float radius,
        int color,
        ScreenRectangle scissorArea,
        ScreenRectangle bounds
    ) implements GuiElementRenderState {
        @Override
        public RenderPipeline pipeline() {
            return RenderPipelines.GUI;
        }

        @Override
        public TextureSetup textureSetup() {
            return TextureSetup.noTexture();
        }

        @Override
        public void buildVertices(VertexConsumer vc) {
            float aa = 0.6f;
            float rCore = Math.max(0.1f, radius - aa * 0.5f);
            float rOuter = radius + aa;
            int zero = withAlpha(color, 0);
            for (int i = 0; i < CIRCLE_SEGMENTS; i++) {
                float a0 = ((float) i / CIRCLE_SEGMENTS) * (PI_F * 2f);
                float a1 = ((float) (i + 1) / CIRCLE_SEGMENTS) * (PI_F * 2f);
                float c0 = (float) Math.cos(a0);
                float s0 = (float) Math.sin(a0);
                float c1 = (float) Math.cos(a1);
                float s1 = (float) Math.sin(a1);
                emitQuad(vc, pose, cx, cy, color, cx + c0 * rCore, cy + s0 * rCore, color, cx + c1 * rCore, cy + s1 * rCore, color, cx, cy, color);
                emitQuad(vc, pose, cx + c0 * rCore, cy + s0 * rCore, color, cx + c0 * rOuter, cy + s0 * rOuter, zero, cx + c1 * rOuter, cy + s1 * rOuter, zero, cx + c1 * rCore, cy + s1 * rCore, color);
            }
        }
    }
}
