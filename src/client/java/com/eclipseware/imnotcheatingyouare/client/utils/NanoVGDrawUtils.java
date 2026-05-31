package com.eclipseware.imnotcheatingyouare.client.utils;

import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.nanovg.NVGPaint;
import org.lwjgl.nanovg.NanoVG;
import org.lwjgl.system.MemoryStack;

public class NanoVGDrawUtils {

    public static void drawRoundedRect(long vg, float x, float y, float w, float h, float r, int rgb, int alpha) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            NVGColor fill = color(stack, rgb, alpha);
            NanoVG.nvgBeginPath(vg);
            NanoVG.nvgRoundedRect(vg, x, y, w, h, r);
            NanoVG.nvgFillColor(vg, fill);
            NanoVG.nvgFill(vg);
        }
    }

    public static void drawRoundedOutline(long vg, float x, float y, float w, float h, float r, float thickness, int rgb, int alpha) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            NVGColor stroke = color(stack, rgb, alpha);
            NanoVG.nvgBeginPath(vg);
            NanoVG.nvgRoundedRect(vg, x, y, w, h, r);
            NanoVG.nvgStrokeColor(vg, stroke);
            NanoVG.nvgStrokeWidth(vg, thickness);
            NanoVG.nvgStroke(vg);
        }
    }

    public static void drawSmoothCircle(long vg, float cx, float cy, float r, int rgb, int alpha) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            NVGColor fill = color(stack, rgb, alpha);
            NanoVG.nvgBeginPath(vg);
            NanoVG.nvgCircle(vg, cx, cy, r);
            NanoVG.nvgFillColor(vg, fill);
            NanoVG.nvgFill(vg);
        }
    }

    public static void drawGlassPanel(long vg, float x, float y, float w, float h, float r, float blur) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            NVGColor inside = NVGColor.malloc(stack).r(1.0f).g(1.0f).b(1.0f).a(0.08f);
            NVGColor shadow = NVGColor.malloc(stack).r(0.0f).g(0.0f).b(0.0f).a(0.40f);
            NVGPaint paint = NVGPaint.malloc(stack);

            NanoVG.nvgBoxGradient(vg, x, y, w, h, r, blur, inside, shadow, paint);
            NanoVG.nvgBeginPath(vg);
            NanoVG.nvgRoundedRect(vg, x, y, w, h, r);
            NanoVG.nvgFillPaint(vg, paint);
            NanoVG.nvgFill(vg);
        }
    }

    public static void drawCurve(long vg, float x1, float y1, float x2, float y2, float thickness, int rgb, int alpha) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            NVGColor stroke = color(stack, rgb, alpha);
            float cx1 = x1 + (x2 - x1) * 0.5f;
            float cx2 = cx1;

            NanoVG.nvgBeginPath(vg);
            NanoVG.nvgMoveTo(vg, x1, y1);
            NanoVG.nvgBezierTo(vg, cx1, y1, cx2, y2, x2, y2);
            NanoVG.nvgStrokeColor(vg, stroke);
            NanoVG.nvgStrokeWidth(vg, thickness);
            NanoVG.nvgLineCap(vg, NanoVG.NVG_ROUND);
            NanoVG.nvgStroke(vg);
        }
    }

    public static void drawRoundedRectGradient(long vg, float x, float y, float w, float h, float r, int startRgb, int startAlpha, int endRgb, int endAlpha) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            NVGColor startColor = color(stack, startRgb, startAlpha);
            NVGColor endColor = color(stack, endRgb, endAlpha);
            
            NVGPaint paint = NVGPaint.malloc(stack);
            NanoVG.nvgLinearGradient(vg, x, y, x, y + h, startColor, endColor, paint);
            
            NanoVG.nvgBeginPath(vg);
            NanoVG.nvgRoundedRect(vg, x, y, w, h, r);
            NanoVG.nvgFillPaint(vg, paint);
            NanoVG.nvgFill(vg);
        }
    }

    public static void drawRoundedRectHorizontalGradient(long vg, float x, float y, float w, float h, float r, int startRgb, int startAlpha, int endRgb, int endAlpha) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            NVGColor startColor = color(stack, startRgb, startAlpha);
            NVGColor endColor = color(stack, endRgb, endAlpha);
            
            NVGPaint paint = NVGPaint.malloc(stack);
            NanoVG.nvgLinearGradient(vg, x, y, x + w, y, startColor, endColor, paint);
            
            NanoVG.nvgBeginPath(vg);
            NanoVG.nvgRoundedRect(vg, x, y, w, h, r);
            NanoVG.nvgFillPaint(vg, paint);
            NanoVG.nvgFill(vg);
        }
    }

    public static void drawRoundedOutlineGradient(long vg, float x, float y, float w, float h, float r, float thickness, int startRgb, int startAlpha, int endRgb, int endAlpha) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            NVGColor startColor = color(stack, startRgb, startAlpha);
            NVGColor endColor = color(stack, endRgb, endAlpha);
            
            NVGPaint paint = NVGPaint.malloc(stack);
            NanoVG.nvgLinearGradient(vg, x, y, x, y + h, startColor, endColor, paint);
            
            NanoVG.nvgBeginPath(vg);
            NanoVG.nvgRoundedRect(vg, x, y, w, h, r);
            NanoVG.nvgStrokePaint(vg, paint);
            NanoVG.nvgStrokeWidth(vg, thickness);
            NanoVG.nvgStroke(vg);
        }
    }

    private static NVGColor color(MemoryStack stack, int rgb, int alpha) {
        return NVGColor.malloc(stack)
                .r(((rgb >> 16) & 0xFF) / 255.0f)
                .g(((rgb >> 8) & 0xFF) / 255.0f)
                .b((rgb & 0xFF) / 255.0f)
                .a(alpha / 255.0f);
    }
}
