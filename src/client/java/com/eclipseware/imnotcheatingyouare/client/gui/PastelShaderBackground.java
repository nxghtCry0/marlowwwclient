package com.eclipseware.imnotcheatingyouare.client.gui;

import com.eclipseware.imnotcheatingyouare.client.utils.ImGuiTextures;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.platform.Window;
import java.nio.ByteBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL33;

public final class PastelShaderBackground {
    public static final Identifier TEXTURE_ID = Identifier.parse("imnotcheatingyouare:dynamic/menu_background");

    private static final String[] SHADER_HEADERS = {
        "#version 330 core\n",
        "#version 150\n",
        "#version 300 es\nprecision highp float;\n",
        "#version 130\n"
    };

    private static final String VERTEX_BODY =
        "out vec2 uv;\n"
        + "void main() {\n"
        + "    vec2 pos = vec2((gl_VertexID << 1) & 2, gl_VertexID & 2);\n"
        + "    uv = pos;\n"
        + "    gl_Position = vec4(pos * 2.0 - 1.0, 0.0, 1.0);\n"
        + "}\n";

    private static final String FRAGMENT_BODY =
        "in vec2 uv;\n"
        + "out vec4 fragColor;\n"
        + "uniform float uTime;\n"
        + "uniform vec2 uResolution;\n"
        + "float hash(vec2 p) {\n"
        + "    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);\n"
        + "}\n"
        + "float noise(vec2 p) {\n"
        + "    vec2 i = floor(p);\n"
        + "    vec2 f = fract(p);\n"
        + "    vec2 u = f * f * (3.0 - 2.0 * f);\n"
        + "    return mix(mix(hash(i), hash(i + vec2(1.0, 0.0)), u.x),\n"
        + "               mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), u.x), u.y);\n"
        + "}\n"
        + "float fbm(vec2 p) {\n"
        + "    float v = 0.0;\n"
        + "    float a = 0.5;\n"
        + "    mat2 rot = mat2(0.80, -0.60, 0.60, 0.80);\n"
        + "    for (int i = 0; i < 5; i++) {\n"
        + "        v += a * noise(p);\n"
        + "        p = rot * p * 2.02 + vec2(1.7, 9.2);\n"
        + "        a *= 0.5;\n"
        + "    }\n"
        + "    return v;\n"
        + "}\n"
        + "void main() {\n"
        + "    float aspect = uResolution.x / max(uResolution.y, 1.0);\n"
        + "    vec2 screenUv = vec2(uv.x, 1.0 - uv.y);\n"
        + "    vec2 p = vec2(screenUv.x * aspect, screenUv.y) * 2.2;\n"
        + "    float t = uTime * 0.16;\n"
        + "    vec2 q = vec2(fbm(p + vec2(0.0, t)), fbm(p + vec2(5.2, 1.3 - t)));\n"
        + "    vec2 r = vec2(fbm(p + 2.5 * q + vec2(1.7, 9.2) + t * 1.1), fbm(p + 2.5 * q + vec2(8.3, 2.8) - t));\n"
        + "    float f = fbm(p + 2.5 * r);\n"
        + "    vec3 cryptNight = vec3(0.05, 0.02, 0.09);\n"
        + "    vec3 witchViolet = vec3(0.38, 0.11, 0.62);\n"
        + "    vec3 pumpkinFlame = vec3(0.98, 0.38, 0.06);\n"
        + "    vec3 emberGold = vec3(1.00, 0.66, 0.14);\n"
        + "    vec3 col = mix(cryptNight, witchViolet, clamp(f * 1.45, 0.0, 1.0));\n"
        + "    float emberCloud = clamp(length(q) * 0.95, 0.0, 1.0);\n"
        + "    col = mix(col, pumpkinFlame, pow(emberCloud, 1.4) * 0.78);\n"
        + "    col = mix(col, cryptNight, clamp(r.x * r.y * 1.65, 0.0, 0.68));\n"
        + "    float ribbon = sin((p.x * 1.9 - p.y * 1.5) + t * 1.8 + f * 3.1) * 0.5 + 0.5;\n"
        + "    col = mix(col, emberGold, pow(ribbon, 3.2) * 0.42);\n"
        + "    vec2 moonPos = vec2(0.76 * aspect, 0.24);\n"
        + "    vec2 moonDelta = vec2(screenUv.x * aspect, screenUv.y) - moonPos;\n"
        + "    float moonDist = length(moonDelta);\n"
        + "    float moonGlow = exp(-moonDist * 4.2) * 0.55;\n"
        + "    float moonDisc = smoothstep(0.115, 0.102, moonDist);\n"
        + "    float craters = fbm(moonDelta * 18.0 + vec2(2.1, 4.7));\n"
        + "    vec3 moonSurface = mix(vec3(1.0, 0.48, 0.12), vec3(1.0, 0.82, 0.42), 0.65 - craters * 0.35);\n"
        + "    col += vec3(1.0, 0.42, 0.08) * moonGlow;\n"
        + "    col = mix(col, moonSurface, moonDisc * (0.85 - f * 0.25));\n"
        + "    vec2 sparkUv = p * 4.5 + vec2(sin(t * 2.0 + p.y * 2.0) * 0.4, t * 3.2);\n"
        + "    float sparkCell = hash(floor(sparkUv));\n"
        + "    vec2 sparkLocal = fract(sparkUv) - 0.5;\n"
        + "    float spark = smoothstep(0.18, 0.02, length(sparkLocal)) * step(0.93, sparkCell);\n"
        + "    col += emberGold * spark * (0.5 + 0.5 * sin(uTime * 4.0 + sparkCell * 20.0));\n"
        + "    float vignette = smoothstep(1.38, 0.22, length(uv - 0.5) * 1.65);\n"
        + "    col *= mix(0.52, 1.0, vignette);\n"
        + "    col += (hash(uv * uResolution + uTime) - 0.5) * 0.014;\n"
        + "    fragColor = vec4(clamp(col, 0.0, 1.0), 1.0);\n"
        + "}\n";

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
        if (failed) {
            return false;
        }
        long now = System.nanoTime();
        if (texture != null && now - lastRenderNanos < 4_000_000L) {
            return true;
        }
        lastRenderNanos = now;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getWindow() == null) {
            return false;
        }
        Window win = mc.getWindow();
        int fbWidth = win.getWidth();
        int fbHeight = win.getHeight();
        if (fbWidth < 64 || fbHeight < 64) {
            return texture != null;
        }
        int width = Math.max(1, fbWidth / 2);
        int height = Math.max(1, fbHeight / 2);

        try {
            if (program == 0 && !createProgram()) {
                failed = true;
                return false;
            }
            TextureManager tm = mc.getTextureManager();
            if (texture == null || width != textureWidth || height != textureHeight) {
                if (texture != null) {
                    tm.release(TEXTURE_ID);
                }
                texture = new DynamicTexture(() -> "Marlow menu background", width, height, true);
                tm.register(TEXTURE_ID, texture);
                textureWidth = width;
                textureHeight = height;
                if (texture.getTexture() instanceof GlTexture created) {
                    ImGuiTextures.invalidate(created.glId());
                }
            }
            if (!(texture.getTexture() instanceof GlTexture glTexture)) {
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
        if (failed || texture == null) {
            return 0;
        }
        if (texture.getTexture() instanceof GlTexture glTexture) {
            return ImGuiTextures.prepare(glTexture.glId(), true);
        }
        return 0;
    }

    private static void draw(int textureId, int width, int height) {
        int prevDrawFbo = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        int prevReadFbo = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int prevProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        int prevVao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        int prevArrayBuffer = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
        int activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        int prevTex2D = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        int prevSampler = GL11.glGetInteger(GL33.GL_SAMPLER_BINDING);

        ByteBuffer maskBuf = BufferUtils.createByteBuffer(16);
        GL11.glGetBooleanv(GL11.GL_COLOR_WRITEMASK, maskBuf);
        boolean maskR = maskBuf.get(0) != 0;
        boolean maskG = maskBuf.get(1) != 0;
        boolean maskB = maskBuf.get(2) != 0;
        boolean maskA = maskBuf.get(3) != 0;

        int[] viewport = new int[4];
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
        boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean scissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);

        try {
            if (framebuffer == 0) {
                framebuffer = GL30.glGenFramebuffers();
            }
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebuffer);
            GL30.glFramebufferTexture2D(
                GL30.GL_FRAMEBUFFER,
                GL30.GL_COLOR_ATTACHMENT0,
                GL11.GL_TEXTURE_2D,
                textureId,
                0
            );
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
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, prevDrawFbo);
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, prevReadFbo);
            GL20.glUseProgram(prevProgram);
            GL30.glBindVertexArray(prevVao);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, prevArrayBuffer);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, prevTex2D);
            GL33.glBindSampler(0, prevSampler);
            GL13.glActiveTexture(activeTexture);
            GL11.glColorMask(maskR, maskG, maskB, maskA);
            GL11.glViewport(viewport[0], viewport[1], viewport[2], viewport[3]);
            toggle(GL11.GL_BLEND, blend);
            toggle(GL11.GL_DEPTH_TEST, depth);
            toggle(GL11.GL_SCISSOR_TEST, scissor);
            toggle(GL11.GL_CULL_FACE, cull);
        }
    }

    private static void toggle(int cap, boolean enabled) {
        if (enabled) {
            GL11.glEnable(cap);
        } else {
            GL11.glDisable(cap);
        }
    }

    private static boolean createProgram() {
        for (String header : SHADER_HEADERS) {
            if (tryCreateProgramWithHeader(header)) {
                return true;
            }
        }
        return false;
    }

    private static boolean tryCreateProgramWithHeader(String header) {
        int vs = compile(GL20.GL_VERTEX_SHADER, header + VERTEX_BODY);
        if (vs == 0) {
            return false;
        }
        int fs = compile(GL20.GL_FRAGMENT_SHADER, header + FRAGMENT_BODY);
        if (fs == 0) {
            GL20.glDeleteShader(vs);
            return false;
        }
        int p = GL20.glCreateProgram();
        GL20.glAttachShader(p, vs);
        GL20.glAttachShader(p, fs);
        GL20.glLinkProgram(p);
        GL20.glDeleteShader(vs);
        GL20.glDeleteShader(fs);
        if (GL20.glGetProgrami(p, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
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
            GL20.glDeleteShader(shader);
            return 0;
        }
        return shader;
    }
}
