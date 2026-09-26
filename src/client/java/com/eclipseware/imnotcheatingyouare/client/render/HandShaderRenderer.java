package com.eclipseware.imnotcheatingyouare.client.render;

import com.eclipseware.imnotcheatingyouare.client.module.impl.SelfShader;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.opengl.GlTexture;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL33;

import java.awt.Color;

public final class HandShaderRenderer {
    private static final String VERTEX = """
            #version 330 core
            out vec2 uv;
            void main() {
                vec2 pos = vec2((gl_VertexID << 1) & 2, gl_VertexID & 2);
                uv = pos;
                gl_Position = vec4(pos * 2.0 - 1.0, 0.0, 1.0);
            }
            """;

    private static final String FRAGMENT = """
            #version 330 core
            in vec2 uv;
            out vec4 fragColor;
            uniform sampler2D uHand;
            uniform vec2 uTexel;
            uniform vec3 uColor;
            uniform float uFill;
            uniform float uRadius;
            uniform float uGlow;
            uniform float uTime;
            uniform int uPastel;

            vec3 pastel(vec2 p) {
                float t = uTime * 0.6;
                float a = sin(p.x * 6.0 + t) * 0.5 + 0.5;
                float b = sin(p.y * 5.0 - t * 1.3 + a * 2.0) * 0.5 + 0.5;
                vec3 lavender = vec3(0.78, 0.66, 0.98);
                vec3 lilac = vec3(0.95, 0.72, 0.95);
                vec3 periwinkle = vec3(0.62, 0.66, 1.0);
                return mix(mix(periwinkle, lavender, a), lilac, b * 0.7);
            }

            void main() {
                vec4 hand = texture(uHand, uv);
                vec3 tint = uPastel == 1 ? pastel(uv) : uColor;

                float edge = 0.0;
                float glow = 0.0;
                for (int ring = 1; ring <= 8; ring++) {
                    float r = float(ring) * uRadius * 0.5;
                    for (int i = 0; i < 16; i++) {
                        float ang = 6.2831853 * float(i) / 16.0;
                        float a = texture(uHand, uv + vec2(cos(ang), sin(ang)) * r * uTexel).a;
                        if (r <= uRadius) edge = max(edge, a);
                        glow = max(glow, a * (1.0 - float(ring - 1) / 8.0));
                    }
                }

                if (hand.a > 0.01) {
                    float shimmer = 0.08 * sin(uTime * 2.0 + uv.y * 12.0);
                    vec3 filled = mix(hand.rgb, tint, clamp(uFill + shimmer * uFill, 0.0, 1.0));
                    fragColor = vec4(filled, hand.a);
                    return;
                }

                float outline = edge;
                float halo = glow * glow * uGlow;
                float alpha = max(outline, halo);
                if (alpha <= 0.001) discard;
                fragColor = vec4(tint * (outline > 0.0 ? 1.0 : 0.9), alpha);
            }
            """;

    private static TextureTarget target;
    private static int program;
    private static int vao;
    private static int framebuffer;
    private static boolean failed;
    private static boolean capturing;
    private static final long START = System.nanoTime();

    private HandShaderRenderer() {
    }

    public static boolean shouldCapture() {
        if (failed || SelfShader.INSTANCE == null || !SelfShader.INSTANCE.hands()) return false;
        if (program == 0) {
            try {
                if (!createProgram()) failed = true;
            } catch (Throwable t) {
                failed = true;
            }
        }
        return !failed && program != 0;
    }

    public static RenderTarget captureTarget(RenderTarget main) {
        if (target == null) {
            target = new TextureTarget("Marlow Hand Shader", main.width, main.height, false, com.mojang.blaze3d.GpuFormat.RGBA8_UNORM);
        } else if (target.width != main.width || target.height != main.height) {
            target.resize(main.width, main.height);
        }
        capturing = true;
        return target;
    }

    public static void composite(RenderTarget main) {
        if (!capturing) return;
        capturing = false;
        if (target == null) return;
        if (!(target.getColorTexture() instanceof GlTexture hand) || !(main.getColorTexture() instanceof GlTexture dest)) return;

        if (program == 0) return;

        int prevFramebuffer = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
        int prevProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        int prevVao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        int prevArray = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
        int prevActive = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        int[] viewport = new int[4];
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
        boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean scissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        int srcRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
        int dstRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        int srcA = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
        int dstA = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        boolean depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);

        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        int prevTex = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        int prevSampler = GL11.glGetInteger(GL33.GL_SAMPLER_BINDING);

        try {
            if (framebuffer == 0) framebuffer = GL30.glGenFramebuffers();
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebuffer);
            GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, dest.glId(), 0);

            GL11.glViewport(0, 0, main.width, main.height);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glDepthMask(false);
            GL11.glColorMask(true, true, true, true);
            GL11.glEnable(GL11.GL_BLEND);
            GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);

            GL33.glBindSampler(0, 0);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, hand.glId());
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE);

            SelfShader s = SelfShader.INSTANCE;
            Color c = s.color();
            GL20.glUseProgram(program);
            GL20.glUniform1i(GL20.glGetUniformLocation(program, "uHand"), 0);
            GL20.glUniform2f(GL20.glGetUniformLocation(program, "uTexel"), 1f / main.width, 1f / main.height);
            GL20.glUniform3f(GL20.glGetUniformLocation(program, "uColor"), c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f);
            GL20.glUniform1f(GL20.glGetUniformLocation(program, "uFill"), s.fill());
            GL20.glUniform1f(GL20.glGetUniformLocation(program, "uRadius"), 1.0f + s.outlineWidth() * 1.2f);
            GL20.glUniform1f(GL20.glGetUniformLocation(program, "uGlow"), s.glow());
            GL20.glUniform1f(GL20.glGetUniformLocation(program, "uTime"), (System.nanoTime() - START) / 1_000_000_000f);
            GL20.glUniform1i(GL20.glGetUniformLocation(program, "uPastel"), s.colorMode() == 0 ? 1 : 0);
            GL30.glBindVertexArray(vao);
            GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 3);
        } finally {
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, prevFramebuffer);
            GL20.glUseProgram(prevProgram);
            GL30.glBindVertexArray(prevVao);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, prevArray);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, prevTex);
            GL33.glBindSampler(0, prevSampler);
            GL13.glActiveTexture(prevActive);
            GL11.glViewport(viewport[0], viewport[1], viewport[2], viewport[3]);
            toggle(GL11.GL_BLEND, blend);
            toggle(GL11.GL_DEPTH_TEST, depth);
            toggle(GL11.GL_SCISSOR_TEST, scissor);
            toggle(GL11.GL_CULL_FACE, cull);
            GL14.glBlendFuncSeparate(srcRgb, dstRgb, srcA, dstA);
            GL11.glDepthMask(depthMask);
        }
    }

    private static void toggle(int cap, boolean enabled) {
        if (enabled) GL11.glEnable(cap);
        else GL11.glDisable(cap);
    }

    private static boolean createProgram() {
        int vs = compile(GL20.GL_VERTEX_SHADER, VERTEX);
        int fs = compile(GL20.GL_FRAGMENT_SHADER, FRAGMENT);
        if (vs == 0 || fs == 0) return false;
        int p = GL20.glCreateProgram();
        GL20.glAttachShader(p, vs);
        GL20.glAttachShader(p, fs);
        GL20.glLinkProgram(p);
        GL20.glDeleteShader(vs);
        GL20.glDeleteShader(fs);
        if (GL20.glGetProgrami(p, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            System.err.println("[Marlow] Hand shader link failed: " + GL20.glGetProgramInfoLog(p));
            GL20.glDeleteProgram(p);
            return false;
        }
        program = p;
        int prevVao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        vao = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(prevVao);
        return true;
    }

    private static int compile(int type, String source) {
        int shader = GL20.glCreateShader(type);
        GL20.glShaderSource(shader, source);
        GL20.glCompileShader(shader);
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            System.err.println("[Marlow] Hand shader compile failed: " + GL20.glGetShaderInfoLog(shader));
            GL20.glDeleteShader(shader);
            return 0;
        }
        return shader;
    }
}
