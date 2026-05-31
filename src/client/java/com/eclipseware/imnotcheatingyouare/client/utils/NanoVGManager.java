package com.eclipseware.imnotcheatingyouare.client.utils;

import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.nanovg.NVGPaint;
import org.lwjgl.nanovg.NanoVG;
import org.lwjgl.system.MemoryStack;
import java.nio.ByteBuffer;
import org.lwjgl.system.MemoryUtil;
import java.io.InputStream;

import static org.lwjgl.nanovg.NanoVG.*;
import static org.lwjgl.nanovg.NanoVGGL3.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.*;

public class NanoVGManager {
    private static long vg = 0;
    private static boolean initialized = false;
    private static boolean failed = false;

    private static int fontId = -1;
    private static ByteBuffer fontBuffer = null;

    private static int activeProgram;
    private static int activeTexture;
    private static int activeTextureId;
    private static int activeArrayBuffer;
    private static int activeElementArrayBuffer;
    private static int activeVertexArray;
    private static boolean blendEnabled;
    private static boolean cullFaceEnabled;
    private static boolean depthTestEnabled;
    private static boolean scissorTestEnabled;
    private static boolean stencilTestEnabled;
    private static final int[] scissorBox = new int[4];
    private static int blendSrcRgb;
    private static int blendDstRgb;
    private static int blendSrcAlpha;
    private static int blendDstAlpha;
    private static int unpackAlignment;
    private static int unpackRowLength;
    private static int unpackSkipRows;
    private static int unpackSkipPixels;
    private static boolean primitiveRestartEnabled;

    public static long getContext() {
        if (failed) return 0;
        if (!initialized) {
            try {
                vg = nvgCreate(NVG_ANTIALIAS | NVG_STENCIL_STROKES);
                if (vg == 0) {
                    failed = true;
                    System.err.println("[EclipseWare] Failed to initialize NanoVG Context.");
                } else {
                    initialized = true;
                }
            } catch (Throwable t) {
                failed = true;
                t.printStackTrace();
            }
        }
        return vg;
    }

    public static int getFont(long ctx) {
        if (fontId == -1 && ctx != 0) {
            try {
                InputStream is = NanoVGManager.class.getResourceAsStream("/assets/imnotcheatingyouare/font/verdana.ttf");
                if (is == null) {
                    is = NanoVGManager.class.getClassLoader().getResourceAsStream("assets/imnotcheatingyouare/font/verdana.ttf");
                }
                if (is != null) {
                    byte[] bytes = is.readAllBytes();
                    fontBuffer = MemoryUtil.memAlloc(bytes.length);
                    fontBuffer.put(bytes);
                    fontBuffer.flip();
                    fontId = nvgCreateFontMem(ctx, "sans", fontBuffer, false);
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
        return fontId;
    }

    public static void saveGLState() {
        activeProgram = glGetInteger(GL_CURRENT_PROGRAM);
        activeTexture = glGetInteger(GL_ACTIVE_TEXTURE);
        activeTextureId = glGetInteger(GL_TEXTURE_BINDING_2D);
        activeArrayBuffer = glGetInteger(GL_ARRAY_BUFFER_BINDING);
        activeElementArrayBuffer = glGetInteger(GL_ELEMENT_ARRAY_BUFFER_BINDING);
        activeVertexArray = glGetInteger(GL_VERTEX_ARRAY_BINDING);
        
        blendEnabled = glIsEnabled(GL_BLEND);
        cullFaceEnabled = glIsEnabled(GL_CULL_FACE);
        depthTestEnabled = glIsEnabled(GL_DEPTH_TEST);
        scissorTestEnabled = glIsEnabled(GL_SCISSOR_TEST);
        stencilTestEnabled = glIsEnabled(GL_STENCIL_TEST);

        glGetIntegerv(GL_SCISSOR_BOX, scissorBox);
        blendSrcRgb = glGetInteger(GL_BLEND_SRC_RGB);
        blendDstRgb = glGetInteger(GL_BLEND_DST_RGB);
        blendSrcAlpha = glGetInteger(GL_BLEND_SRC_ALPHA);
        blendDstAlpha = glGetInteger(GL_BLEND_DST_ALPHA);

        unpackAlignment = glGetInteger(GL_UNPACK_ALIGNMENT);
        unpackRowLength = glGetInteger(GL_UNPACK_ROW_LENGTH);
        unpackSkipRows = glGetInteger(GL_UNPACK_SKIP_ROWS);
        unpackSkipPixels = glGetInteger(GL_UNPACK_SKIP_PIXELS);
        primitiveRestartEnabled = glIsEnabled(GL_PRIMITIVE_RESTART);
    }

    public static void restoreGLState() {
        glUseProgram(activeProgram);
        glActiveTexture(activeTexture);
        glBindTexture(GL_TEXTURE_2D, activeTextureId);
        glBindVertexArray(activeVertexArray);
        glBindBuffer(GL_ARRAY_BUFFER, activeArrayBuffer);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, activeElementArrayBuffer);

        if (blendEnabled) glEnable(GL_BLEND); else glDisable(GL_BLEND);
        if (cullFaceEnabled) glEnable(GL_CULL_FACE); else glDisable(GL_CULL_FACE);
        if (depthTestEnabled) glEnable(GL_DEPTH_TEST); else glDisable(GL_DEPTH_TEST);
        if (scissorTestEnabled) glEnable(GL_SCISSOR_TEST); else glDisable(GL_SCISSOR_TEST);
        if (stencilTestEnabled) glEnable(GL_STENCIL_TEST); else glDisable(GL_STENCIL_TEST);

        glScissor(scissorBox[0], scissorBox[1], scissorBox[2], scissorBox[3]);
        glBlendFuncSeparate(blendSrcRgb, blendDstRgb, blendSrcAlpha, blendDstAlpha);

        glPixelStorei(GL_UNPACK_ALIGNMENT, unpackAlignment);
        glPixelStorei(GL_UNPACK_ROW_LENGTH, unpackRowLength);
        glPixelStorei(GL_UNPACK_SKIP_ROWS, unpackSkipRows);
        glPixelStorei(GL_UNPACK_SKIP_PIXELS, unpackSkipPixels);

        if (primitiveRestartEnabled) {
            glEnable(GL_PRIMITIVE_RESTART);
        } else {
            glDisable(GL_PRIMITIVE_RESTART);
        }
    }

    public static boolean begin(int width, int height, float devicePixelRatio) {
        long ctx = getContext();
        if (ctx == 0) return false;
        
        saveGLState();

        glDisable(GL_DEPTH_TEST);
        glDisable(GL_CULL_FACE);
        glDisable(GL_SCISSOR_TEST);
        glDisable(GL_PRIMITIVE_RESTART);
        glEnable(GL_BLEND);
        glBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE_MINUS_SRC_ALPHA);

        glPixelStorei(GL_UNPACK_ALIGNMENT, 4);
        glPixelStorei(GL_UNPACK_ROW_LENGTH, 0);
        glPixelStorei(GL_UNPACK_SKIP_ROWS, 0);
        glPixelStorei(GL_UNPACK_SKIP_PIXELS, 0);

        nvgBeginFrame(ctx, width, height, devicePixelRatio);
        return true;
    }

    public static void end() {
        long ctx = getContext();
        if (ctx == 0) return;

        nvgEndFrame(ctx);
        restoreGLState();
    }

    public static void drawRoundedRect(float x, float y, float w, float h, float radius, int color) {
        long ctx = getContext();
        if (ctx == 0) return;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            NVGColor nvgColor = NVGColor.malloc(stack);
            rgbaToNVGColor(color, nvgColor);

            nvgBeginPath(ctx);
            nvgRoundedRect(ctx, x, y, w, h, radius);
            nvgFillColor(ctx, nvgColor);
            nvgFill(ctx);
        }
    }

    public static void drawRoundedOutline(float x, float y, float w, float h, float radius, float thickness, int color) {
        long ctx = getContext();
        if (ctx == 0) return;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            NVGColor nvgColor = NVGColor.malloc(stack);
            rgbaToNVGColor(color, nvgColor);

            nvgBeginPath(ctx);
            nvgRoundedRect(ctx, x, y, w, h, radius);
            nvgStrokeWidth(ctx, thickness);
            nvgStrokeColor(ctx, nvgColor);
            nvgStroke(ctx);
        }
    }

    public static void drawDropShadow(float x, float y, float w, float h, float radius, float feather, int color) {
        long ctx = getContext();
        if (ctx == 0) return;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            NVGColor shadowColor = NVGColor.malloc(stack);
            rgbaToNVGColor(color, shadowColor);

            NVGColor transparentColor = NVGColor.malloc(stack);
            rgbaToNVGColor(0, transparentColor);

            NVGPaint paint = NVGPaint.malloc(stack);
            nvgBoxGradient(ctx, x, y, w, h, radius, feather, shadowColor, transparentColor, paint);

            nvgBeginPath(ctx);
            nvgRect(ctx, x - feather, y - feather, w + feather * 2, h + feather * 2);
            nvgRoundedRect(ctx, x, y, w, h, radius);
            nvgPathWinding(ctx, NVG_HOLE);
            nvgFillPaint(ctx, paint);
            nvgFill(ctx);
        }
    }

    public static void drawGradientRoundedRect(float x, float y, float w, float h, float radius, int colorStart, int colorEnd, boolean horizontal) {
        long ctx = getContext();
        if (ctx == 0) return;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            NVGColor start = NVGColor.malloc(stack);
            rgbaToNVGColor(colorStart, start);

            NVGColor endColor = NVGColor.malloc(stack);
            rgbaToNVGColor(colorEnd, endColor);

            NVGPaint paint = NVGPaint.malloc(stack);
            float sx = x;
            float sy = y;
            float ex = horizontal ? (x + w) : x;
            float ey = horizontal ? y : (y + h);

            nvgLinearGradient(ctx, sx, sy, ex, ey, start, endColor, paint);
            nvgBeginPath(ctx);
            nvgRoundedRect(ctx, x, y, w, h, radius);
            nvgFillPaint(ctx, paint);
            nvgFill(ctx);
        }
    }

    public static void drawGradientRoundedOutline(float x, float y, float w, float h, float radius, float thickness, int colorStart, int colorEnd, boolean horizontal) {
        long ctx = getContext();
        if (ctx == 0) return;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            NVGColor start = NVGColor.malloc(stack);
            rgbaToNVGColor(colorStart, start);

            NVGColor endColor = NVGColor.malloc(stack);
            rgbaToNVGColor(colorEnd, endColor);

            NVGPaint paint = NVGPaint.malloc(stack);
            float sx = x;
            float sy = y;
            float ex = horizontal ? (x + w) : x;
            float ey = horizontal ? y : (y + h);

            nvgLinearGradient(ctx, sx, sy, ex, ey, start, endColor, paint);
            nvgBeginPath(ctx);
            nvgRoundedRect(ctx, x, y, w, h, radius);
            nvgStrokePaint(ctx, paint);
            nvgStrokeWidth(ctx, thickness);
            nvgStroke(ctx);
        }
    }

    private static void rgbaToNVGColor(int rgba, NVGColor out) {
        float r = ((rgba >> 16) & 0xFF) / 255.0f;
        float g = ((rgba >> 8) & 0xFF) / 255.0f;
        float b = (rgba & 0xFF) / 255.0f;
        float a = ((rgba >> 24) & 0xFF) / 255.0f;
        out.r(r);
        out.g(g);
        out.b(b);
        out.a(a);
    }
}
