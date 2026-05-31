package com.eclipseware.imnotcheatingyouare.client.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.pipeline.RenderTarget;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.nanovg.NanoVGGL3;
import org.lwjgl.opengl.*;
import org.lwjgl.system.MemoryUtil;

import java.io.InputStream;
import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.*;

public class NanoVGRenderer {
    private static long vg = MemoryUtil.NULL;
    private static boolean initialized = false;
    private static ByteBuffer fontBuffer = null;

    public static void initialize() {
        if (initialized) return;
        if (GLFW.glfwGetCurrentContext() == MemoryUtil.NULL) return;

        vg = NanoVGGL3.nvgCreate(NanoVGGL3.NVG_ANTIALIAS | NanoVGGL3.NVG_STENCIL_STROKES);
        if (vg == MemoryUtil.NULL) {
            throw new RuntimeException("Failed to initialize NanoVG context.");
        }

        int unpackAlignment = glGetInteger(GL_UNPACK_ALIGNMENT);
        int unpackRowLength = glGetInteger(GL_UNPACK_ROW_LENGTH);
        int unpackSkipRows = glGetInteger(GL_UNPACK_SKIP_ROWS);
        int unpackSkipPixels = glGetInteger(GL_UNPACK_SKIP_PIXELS);
        int unpackImageHeight = glGetInteger(GL_UNPACK_IMAGE_HEIGHT);
        int unpackSkipImages = glGetInteger(GL_UNPACK_SKIP_IMAGES);

        glPixelStorei(GL_UNPACK_ALIGNMENT, 4);
        glPixelStorei(GL_UNPACK_ROW_LENGTH, 0);
        glPixelStorei(GL_UNPACK_SKIP_ROWS, 0);
        glPixelStorei(GL_UNPACK_SKIP_PIXELS, 0);
        glPixelStorei(GL_UNPACK_IMAGE_HEIGHT, 0);
        glPixelStorei(GL_UNPACK_SKIP_IMAGES, 0);

        loadFont("roboto", Identifier.parse("imnotcheatingyouare:font/verdana.ttf"));
        loadFont("sans", Identifier.parse("imnotcheatingyouare:font/verdana.ttf"));

        glPixelStorei(GL_UNPACK_ALIGNMENT, unpackAlignment);
        glPixelStorei(GL_UNPACK_ROW_LENGTH, unpackRowLength);
        glPixelStorei(GL_UNPACK_SKIP_ROWS, unpackSkipRows);
        glPixelStorei(GL_UNPACK_SKIP_PIXELS, unpackSkipPixels);
        glPixelStorei(GL_UNPACK_IMAGE_HEIGHT, unpackImageHeight);
        glPixelStorei(GL_UNPACK_SKIP_IMAGES, unpackSkipImages);

        initialized = true;
    }

    private static void loadFont(String name, Identifier fontPath) {
        try (InputStream stream = Minecraft.getInstance().getResourceManager()
                .getResourceOrThrow(fontPath).open()) {
            byte[] bytes = stream.readAllBytes();
            fontBuffer = MemoryUtil.memAlloc(bytes.length);
            fontBuffer.put(bytes);
            fontBuffer.flip();

            if (org.lwjgl.nanovg.NanoVG.nvgCreateFontMem(vg, name, fontBuffer, false) == -1) {
                System.err.println("Failed to load NanoVG font: " + name);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void render(Runnable drawCalls) {
        if (!initialized) {
            initialize();
            return;
        }

        Window window = Minecraft.getInstance().getWindow();
        int scaledWidth = window.getGuiScaledWidth();
        int scaledHeight = window.getGuiScaledHeight();
        float ratio = (float) window.getGuiScale();

        int activeTex = glGetInteger(GL_ACTIVE_TEXTURE);
        int currentTex = glGetInteger(GL_TEXTURE_BINDING_2D);
        int currentVao = glGetInteger(GL_VERTEX_ARRAY_BINDING);
        int currentProgram = glGetInteger(GL_CURRENT_PROGRAM);
        int currentEbo = glGetInteger(GL_ELEMENT_ARRAY_BUFFER_BINDING);
        int currentVbo = glGetInteger(GL_ARRAY_BUFFER_BINDING);

        boolean depth = glIsEnabled(GL_DEPTH_TEST);
        boolean stencil = glIsEnabled(GL_STENCIL_TEST);
        boolean blend = glIsEnabled(GL_BLEND);
        boolean cull = glIsEnabled(GL_CULL_FACE);
        boolean scissor = glIsEnabled(GL_SCISSOR_TEST);

        int unpackAlignment = glGetInteger(GL_UNPACK_ALIGNMENT);
        int unpackRowLength = glGetInteger(GL_UNPACK_ROW_LENGTH);
        int unpackSkipRows = glGetInteger(GL_UNPACK_SKIP_ROWS);
        int unpackSkipPixels = glGetInteger(GL_UNPACK_SKIP_PIXELS);
        int unpackImageHeight = glGetInteger(GL_UNPACK_IMAGE_HEIGHT);
        int unpackSkipImages = glGetInteger(GL_UNPACK_SKIP_IMAGES);
        boolean primitiveRestartEnabled = glIsEnabled(GL_PRIMITIVE_RESTART);

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
        glPixelStorei(GL_UNPACK_IMAGE_HEIGHT, 0);
        glPixelStorei(GL_UNPACK_SKIP_IMAGES, 0);

        int currentFbo = glGetInteger(GL_FRAMEBUFFER_BINDING);
        int stencilBits = glGetInteger(GL_STENCIL_BITS);
        int stencilRbo = 0;
        if (stencilBits == 0 && currentFbo != 0) {
            int[] viewport = new int[4];
            glGetIntegerv(GL_VIEWPORT, viewport);
            int width = viewport[2];
            int height = viewport[3];

            stencilRbo = glGenRenderbuffers();
            glBindRenderbuffer(GL_RENDERBUFFER, stencilRbo);
            glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH24_STENCIL8, width, height);
            glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_STENCIL_ATTACHMENT, GL_RENDERBUFFER, stencilRbo);

            if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
                glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_STENCIL_ATTACHMENT, GL_RENDERBUFFER, 0);
                glRenderbufferStorage(GL_RENDERBUFFER, GL_STENCIL_INDEX8, width, height);
                glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_STENCIL_ATTACHMENT, GL_RENDERBUFFER, stencilRbo);
            }
        }

        org.lwjgl.nanovg.NanoVG.nvgBeginFrame(vg, scaledWidth, scaledHeight, ratio);
        drawCalls.run();
        org.lwjgl.nanovg.NanoVG.nvgEndFrame(vg);

        if (stencilRbo != 0) {
            glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_STENCIL_ATTACHMENT, GL_RENDERBUFFER, 0);
            glDeleteRenderbuffers(stencilRbo);
        }

        glUseProgram(currentProgram);
        glActiveTexture(activeTex);
        glBindTexture(GL_TEXTURE_2D, currentTex);
        glBindVertexArray(currentVao);
        glBindBuffer(GL_ARRAY_BUFFER, currentVbo);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, currentEbo);

        if (depth) glEnable(GL_DEPTH_TEST); else glDisable(GL_DEPTH_TEST);
        if (stencil) glEnable(GL_STENCIL_TEST); else glDisable(GL_STENCIL_TEST);
        if (blend) glEnable(GL_BLEND); else glDisable(GL_BLEND);
        if (cull) glEnable(GL_CULL_FACE); else glDisable(GL_CULL_FACE);
        if (scissor) glEnable(GL_SCISSOR_TEST); else glDisable(GL_SCISSOR_TEST);

        glPixelStorei(GL_UNPACK_ALIGNMENT, unpackAlignment);
        glPixelStorei(GL_UNPACK_ROW_LENGTH, unpackRowLength);
        glPixelStorei(GL_UNPACK_SKIP_ROWS, unpackSkipRows);
        glPixelStorei(GL_UNPACK_SKIP_PIXELS, unpackSkipPixels);
        glPixelStorei(GL_UNPACK_IMAGE_HEIGHT, unpackImageHeight);
        glPixelStorei(GL_UNPACK_SKIP_IMAGES, unpackSkipImages);

        if (primitiveRestartEnabled) {
            glEnable(GL_PRIMITIVE_RESTART);
        } else {
            glDisable(GL_PRIMITIVE_RESTART);
        }
    }

    public static long getContext() { return vg; }

    public static void shutdown() {
        if (vg != MemoryUtil.NULL) {
            NanoVGGL3.nvgDelete(vg);
            vg = MemoryUtil.NULL;
        }
        if (fontBuffer != null) {
            MemoryUtil.memFree(fontBuffer);
            fontBuffer = null;
        }
    }
}
