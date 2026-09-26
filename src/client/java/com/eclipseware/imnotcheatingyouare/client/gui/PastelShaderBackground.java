package com.eclipseware.imnotcheatingyouare.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

public final class PastelShaderBackground {
    public static final Identifier TEXTURE_ID = Identifier.parse("imnotcheatingyouare:dynamic/menu_background");

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
            uniform float uTime;
            uniform vec2 uResolution;

            float hash(vec2 p) {
                return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
            }

            float noise(vec2 p) {
                vec2 i = floor(p);
                vec2 f = fract(p);
                vec2 u = f * f * (3.0 - 2.0 * f);
                return mix(mix(hash(i), hash(i + vec2(1.0, 0.0)), u.x),
                           mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), u.x), u.y);
            }

            float fbm(vec2 p) {
                float v = 0.0;
                float a = 0.5;
                for (int i = 0; i < 5; i++) {
                    v += a * noise(p);
                    p = p * 2.03 + vec2(1.7, 9.2);
                    a *= 0.5;
                }
                return v;
            }

            void main() {
                vec2 p = vec2(uv.x * uResolution.x / uResolution.y, 1.0 - uv.y) * 2.2;
                float t = uTime * 0.11;

                vec2 q = vec2(fbm(p + vec2(0.0, t)), fbm(p + vec2(5.2, 1.3 - t)));
                vec2 r = vec2(fbm(p + 3.0 * q + vec2(1.7, 9.2) + t * 1.5), fbm(p + 3.0 * q + vec2(8.3, 2.8) - t));
                float f = fbm(p + 3.0 * r);

                vec3 lavender = vec3(0.78, 0.66, 0.98);
                vec3 lilac = vec3(0.92, 0.72, 0.95);
                vec3 periwinkle = vec3(0.62, 0.64, 0.97);
                vec3 deep = vec3(0.34, 0.24, 0.55);

                vec3 col = mix(periwinkle, lavender, clamp(f * 1.6, 0.0, 1.0));
                col = mix(col, lilac, clamp(length(q) * 0.9, 0.0, 1.0));
                col = mix(col, deep, clamp(r.x * r.y * 1.4, 0.0, 0.55));

                float vignette = smoothstep(1.35, 0.25, length(uv - 0.5) * 1.6);
                col *= mix(0.72, 1.0, vignette);
                col += (hash(uv * uResolution + uTime) - 0.5) * 0.015;

                fragColor = vec4(col, 1.0);
            }
            """;

    private static int program;
    private static int vao;
    private static int framebuffer;
    private static int timeLocation;
    private static int resolutionLocation;
    private static boolean failed;
    private static DynamicTexture texture;
    private static long lastRenderNanos;
    private static int textureWidth;
    private static int textureHeight;
    private static final long START = System.nanoTime();

    private PastelShaderBackground() {
    }

    public static boolean render() {
        if (failed) return false;
        long now = System.nanoTime();
        if (texture != null && now - lastRenderNanos < 4_000_000L) return true;
        lastRenderNanos = now;
        Minecraft mc = Minecraft.getInstance();
        int width = Math.max(1, mc.getWindow().getWidth() / 2);
        int height = Math.max(1, mc.getWindow().getHeight() / 2);

        try {
            if (program == 0 && !createProgram()) {
                failed = true;
                return false;
            }
            if (texture == null || width != textureWidth || height != textureHeight) {
                if (texture != null) mc.getTextureManager().release(TEXTURE_ID);
                texture = new DynamicTexture(() -> "Marlow menu background", width, height, true);
                mc.getTextureManager().register(TEXTURE_ID, texture);
                textureWidth = width;
                textureHeight = height;
            }
            if (!(texture.getTexture() instanceof com.mojang.blaze3d.opengl.GlTexture glTexture)) {
                failed = true;
                return false;
            }
            draw(glTexture.glId(), width, height);
            return true;
        } catch (Throwable t) {
            failed = true;
            return false;
        }
    }

    public static int glTextureId() {
        if (failed || texture == null) return 0;
        if (texture.getTexture() instanceof com.mojang.blaze3d.opengl.GlTexture glTexture)
            return com.eclipseware.imnotcheatingyouare.client.utils.ImGuiTextures.prepare(glTexture.glId(), true);
        return 0;
    }

    private static void draw(int textureId, int width, int height) {
        int prevFramebuffer = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
        int prevProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        int prevVao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        int prevArrayBuffer = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
        int[] viewport = new int[4];
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
        boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean scissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        int activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);

        try {
            if (framebuffer == 0) framebuffer = GL30.glGenFramebuffers();
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebuffer);
            GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, textureId, 0);
            GL11.glViewport(0, 0, width, height);
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glColorMask(true, true, true, true);

            GL20.glUseProgram(program);
            GL20.glUniform1f(timeLocation, (System.nanoTime() - START) / 1_000_000_000f);
            GL20.glUniform2f(resolutionLocation, width, height);
            GL30.glBindVertexArray(vao);
            GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 3);
        } finally {
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, prevFramebuffer);
            GL20.glUseProgram(prevProgram);
            GL30.glBindVertexArray(prevVao);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, prevArrayBuffer);
            GL11.glViewport(viewport[0], viewport[1], viewport[2], viewport[3]);
            toggle(GL11.GL_BLEND, blend);
            toggle(GL11.GL_DEPTH_TEST, depth);
            toggle(GL11.GL_SCISSOR_TEST, scissor);
            toggle(GL11.GL_CULL_FACE, cull);
            GL13.glActiveTexture(activeTexture);
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
            System.err.println("[Marlow] Menu shader link failed: " + GL20.glGetProgramInfoLog(p));
            GL20.glDeleteProgram(p);
            return false;
        }
        program = p;
        timeLocation = GL20.glGetUniformLocation(p, "uTime");
        resolutionLocation = GL20.glGetUniformLocation(p, "uResolution");
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
            System.err.println("[Marlow] Menu shader compile failed: " + GL20.glGetShaderInfoLog(shader));
            GL20.glDeleteShader(shader);
            return 0;
        }
        return shader;
    }
}
