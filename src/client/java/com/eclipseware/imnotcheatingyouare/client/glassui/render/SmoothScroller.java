package com.eclipseware.imnotcheatingyouare.client.glassui.render;

import com.eclipseware.imnotcheatingyouare.client.glassui.theme.UiTheme;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class SmoothScroller {
    private float target = 0f;
    private float current = 0f;
    private float maxScroll = 0f;
    private float overscroll = 0f;
    private float velocity = 0f;
    private float activity = 0f;
    public float trackHover = 0f;
    public boolean dragging = false;

    public float trackX = 0f;
    public float trackY = 0f;
    public float trackW = 0f;
    public float trackH = 0f;
    private float lastViewportH = 1f;
    private float lastContentH = 1f;

    public float current() {
        return current;
    }

    public float maxScroll() {
        return maxScroll;
    }

    public float velocity() {
        return velocity;
    }

    public void reset() {
        target = 0f;
        current = 0f;
        maxScroll = 0f;
        overscroll = 0f;
        velocity = 0f;
        activity = 0f;
        trackHover = 0f;
        dragging = false;
    }

    public void updateBounds(float newMaxScroll, float viewportH, float contentH) {
        maxScroll = Math.max(0f, newMaxScroll);
        lastViewportH = Math.max(1f, viewportH);
        lastContentH = Math.max(1f, contentH);
        target = clamp(target, 0f, maxScroll);
        if (maxScroll <= 0.1f) {
            current = Math.min(current, 2f);
        }
    }

    public void onWheel(float deltaPx) {
        activity = 1f;
        if (maxScroll <= 0.5f) {
            overscroll = clamp(overscroll + deltaPx * 0.16f, -10f, 10f);
            return;
        }
        float raw = target + deltaPx;
        if (raw < 0f) {
            target = 0f;
            overscroll = clamp(overscroll + raw * 0.24f, -16f, 0f);
        } else if (raw > maxScroll) {
            overscroll = clamp(overscroll + (raw - maxScroll) * 0.24f, 0f, 16f);
            target = maxScroll;
        } else {
            target = raw;
        }
    }

    public void dragToLocalY(float localY) {
        if (maxScroll <= 0.5f || trackH <= 4f) {
            return;
        }
        float thumbH = computeThumbHeight();
        float usable = Math.max(1f, trackH - thumbH);
        float frac = clamp((localY - trackY - thumbH * 0.5f) / usable, 0f, 1f);
        target = frac * maxScroll;
        current = target;
        overscroll = 0f;
        activity = 1f;
    }

    public void update(float animDt, float dt) {
        float prev = current;
        float speed = dragging ? 30f : 17.5f;
        current = UiRenderPrimitives.approachExp(current, target, speed, animDt);
        overscroll = UiRenderPrimitives.approachExp(overscroll, 0f, 13.5f, animDt);
        float instVel = (current - prev) / Math.max(0.001f, dt);
        velocity = UiRenderPrimitives.approachExp(velocity, instVel, 22f, dt);
        boolean isMoving = Math.abs(current - target) > 0.25f || Math.abs(overscroll) > 0.2f || dragging || trackHover > 0.08f;
        activity = UiRenderPrimitives.approachExp(activity, isMoving ? 1f : 0f, isMoving ? 18f : 3.2f, dt);
    }

    public float visualOffset() {
        return current + overscroll;
    }

    public float accordionShift(int index, int totalItems) {
        if (Math.abs(overscroll) < 0.05f || totalItems <= 1) {
            return 0f;
        }
        if (overscroll < 0f) {
            return index * (-overscroll * 0.12f);
        }
        return -((totalItems - 1 - index) * (overscroll * 0.08f));
    }

    public float edgeFadeAlpha(float itemTop, float itemH, float viewTop, float viewBottom) {
        float denom = Math.min(itemH, 26f);
        if (denom <= 0.01f) {
            return 1f;
        }
        float topRatio = clamp((itemTop + itemH - viewTop) / denom, 0f, 1f);
        float botRatio = clamp((viewBottom - itemTop) / denom, 0f, 1f);
        float minFade = Math.min(UiRenderPrimitives.easeOutCubic(topRatio), UiRenderPrimitives.easeOutCubic(botRatio));
        return clamp(minFade, 0.15f, 1f);
    }

    public float computeThumbHeight() {
        float ratio = clamp(lastViewportH / Math.max(lastViewportH, lastContentH), 0.14f, 1f);
        return Math.max(20f, trackH * ratio);
    }

    public void drawScrollbar(
        GuiGraphicsExtractor graphics,
        float x,
        float viewTop,
        float viewBottom,
        float lx,
        float ly,
        float animDt,
        float alpha,
        boolean interactive,
        UiTheme theme
    ) {
        if (maxScroll <= 1f || alpha <= 0.02f) {
            trackW = 0f;
            trackH = 0f;
            return;
        }
        float padY = 4f;
        trackX = x;
        trackY = viewTop + padY;
        trackW = 4.2f;
        trackH = Math.max(24f, (viewBottom - viewTop) - padY * 2f);

        boolean hit = interactive && lx >= trackX - 5f && lx <= trackX + trackW + 5f && ly >= trackY - 4f && ly <= trackY + trackH + 4f;
        trackHover = UiRenderPrimitives.approachExp(trackHover, (hit || dragging) ? 1f : 0f, 16f, animDt);

        int accent = theme.accentColor();
        int secondary = theme.secondaryAccentColor();
        float visBoost = (0.34f + 0.66f * Math.max(activity, trackHover)) * alpha;

        UiRenderPrimitives.drawRoundedRect(
            graphics, trackX, trackY, trackW, trackH, trackW * 0.5f,
            UiRenderPrimitives.argb(Math.round((26f + 16f * trackHover) * alpha), 255, 255, 255)
        );

        float thumbH = computeThumbHeight();
        float frac = clamp(current / Math.max(0.01f, maxScroll), 0f, 1f);
        float travel = Math.max(0f, trackH - thumbH);
        float thumbY = trackY + travel * frac + clamp(overscroll * 0.28f, -4f, 4f);
        float clampedThumbY = clamp(thumbY, trackY, trackY + trackH - thumbH);

        int topCol = UiRenderPrimitives.multiplyAlpha(accent, (0.70f + 0.30f * trackHover) * visBoost);
        int botCol = UiRenderPrimitives.multiplyAlpha(secondary, (0.58f + 0.32f * trackHover) * visBoost);
        UiRenderPrimitives.drawRoundedGradient(graphics, trackX, clampedThumbY, trackW, thumbH, trackW * 0.5f, topCol, botCol);
    }

    private static float clamp(float val, float min, float max) {
        return Math.max(min, Math.min(max, val));
    }
}
