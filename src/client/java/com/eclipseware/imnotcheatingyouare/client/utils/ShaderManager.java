package com.eclipseware.imnotcheatingyouare.client.utils;

import net.minecraft.client.Minecraft;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class ShaderManager {
    private static int programId = 0;
    private static int vao = 0;
    private static int vbo = 0;

    private static int uScreenSize = -1;
    private static int uRectMin = -1;
    private static int uRectMax = -1;
    private static int uRadius = -1;
    private static int uColor = -1;
    private static int uColor2 = -1;
    private static int uGradientType = -1;
    private static int uThickness = -1;
    private static int uSoftness = -1;
    private static int uModelViewMat = -1;

    private static final String VERTEX_SHADER = 
        "#version 150\n" +
        "in vec2 Position;\n" +
        "out vec2 v_texCoord;\n" +
        "uniform vec2 u_screenSize;\n" +
        "uniform mat4 u_modelViewMat;\n" +
        "void main() {\n" +
        "    vec4 transformed = u_modelViewMat * vec4(Position, 0.0, 1.0);\n" +
        "    gl_Position = vec4((transformed.x / u_screenSize.x) * 2.0 - 1.0, 1.0 - (transformed.y / u_screenSize.y) * 2.0, transformed.z, 1.0);\n" +
        "    v_texCoord = Position;\n" +
        "}\n";

    private static final String FRAGMENT_SHADER = 
        "#version 150\n" +
        "in vec2 v_texCoord;\n" +
        "out vec4 fragColor;\n" +
        "uniform vec2 u_rectMin;\n" +
        "uniform vec2 u_rectMax;\n" +
        "uniform float u_radius;\n" +
        "uniform vec4 u_color;\n" +
        "uniform vec4 u_color2;\n" +
        "uniform int u_gradientType;\n" +
        "uniform float u_thickness;\n" +
        "uniform float u_softness;\n" +
        "float squircleSDF(vec2 p, vec2 b, float r) {\n" +
        "    vec2 q = abs(p) - b + vec2(r);\n" +
        "    float n = 4.0;\n" +
        "    return min(max(q.x, q.y), 0.0) + pow(pow(max(q.x, 0.0), n) + pow(max(q.y, 0.0), n), 1.0 / n) - r;\n" +
        "}\n" +
        "void main() {\n" +
        "    vec2 rectSize = u_rectMax - u_rectMin;\n" +
        "    vec2 halfSize = rectSize * 0.5;\n" +
        "    vec2 rectCenter = u_rectMin + halfSize;\n" +
        "    vec2 p = v_texCoord - rectCenter;\n" +
        "    float distance = squircleSDF(p, halfSize, u_radius);\n" +
        "    float alphaFactor;\n" +
        "    if (u_thickness > 0.0) {\n" +
        "        float borderDist = abs(distance + u_thickness * 0.5) - u_thickness * 0.5;\n" +
        "        alphaFactor = 1.0 - smoothstep(-u_softness, u_softness, borderDist);\n" +
        "    } else {\n" +
        "        alphaFactor = 1.0 - smoothstep(-u_softness, u_softness, distance);\n" +
        "    }\n" +
        "    vec4 finalColor = u_color;\n" +
        "    if (u_gradientType == 1) {\n" +
        "        float t = (v_texCoord.x - u_rectMin.x) / rectSize.x;\n" +
        "        finalColor = mix(u_color, u_color2, clamp(t, 0.0, 1.0));\n" +
        "    } else if (u_gradientType == 2) {\n" +
        "        float t = (v_texCoord.y - u_rectMin.y) / rectSize.y;\n" +
        "        finalColor = mix(u_color, u_color2, clamp(t, 0.0, 1.0));\n" +
        "    }\n" +
        "    fragColor = vec4(finalColor.rgb, finalColor.a * alphaFactor);\n" +
        "}\n";

    public static void init() {
        if (programId != 0) return;

        int vertexShader = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vertexShader, VERTEX_SHADER);
        glCompileShader(vertexShader);
        if (glGetShaderi(vertexShader, GL_COMPILE_STATUS) == GL_FALSE) {
            System.err.println("Failed to compile vertex shader: " + glGetShaderInfoLog(vertexShader));
        }

        int fragmentShader = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fragmentShader, FRAGMENT_SHADER);
        glCompileShader(fragmentShader);
        if (glGetShaderi(fragmentShader, GL_COMPILE_STATUS) == GL_FALSE) {
            System.err.println("Failed to compile fragment shader: " + glGetShaderInfoLog(fragmentShader));
        }

        programId = glCreateProgram();
        glAttachShader(programId, vertexShader);
        glAttachShader(programId, fragmentShader);
        glBindAttribLocation(programId, 0, "Position");
        glLinkProgram(programId);
        if (glGetProgrami(programId, GL_LINK_STATUS) == GL_FALSE) {
            System.err.println("Failed to link shader program: " + glGetProgramInfoLog(programId));
        }

        glDeleteShader(vertexShader);
        glDeleteShader(fragmentShader);

        uScreenSize = glGetUniformLocation(programId, "u_screenSize");
        uRectMin = glGetUniformLocation(programId, "u_rectMin");
        uRectMax = glGetUniformLocation(programId, "u_rectMax");
        uRadius = glGetUniformLocation(programId, "u_radius");
        uColor = glGetUniformLocation(programId, "u_color");
        uColor2 = glGetUniformLocation(programId, "u_color2");
        uGradientType = glGetUniformLocation(programId, "u_gradientType");
        uThickness = glGetUniformLocation(programId, "u_thickness");
        uSoftness = glGetUniformLocation(programId, "u_softness");
        uModelViewMat = glGetUniformLocation(programId, "u_modelViewMat");

        vao = glGenVertexArrays();
        vbo = glGenBuffers();

        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 8, 0);
        glBindVertexArray(0);
    }

    public static void drawSquircle(org.joml.Matrix4f matrix, float x, float y, float width, float height, float radius, float thickness, float softness, int color, int color2, int gradientType) {
        init();

        int activeProgram = glGetInteger(GL_CURRENT_PROGRAM);
        int activeArrayBuffer = glGetInteger(GL_ARRAY_BUFFER_BINDING);
        int activeVertexArray = glGetInteger(GL_VERTEX_ARRAY_BINDING);
        boolean blendEnabled = glIsEnabled(GL_BLEND);
        int blendSrcRgb = glGetInteger(GL_BLEND_SRC_RGB);
        int blendDstRgb = glGetInteger(GL_BLEND_DST_RGB);
        int blendSrcAlpha = glGetInteger(GL_BLEND_SRC_ALPHA);
        int blendDstAlpha = glGetInteger(GL_BLEND_DST_ALPHA);
        boolean depthEnabled = glIsEnabled(GL_DEPTH_TEST);
        boolean cullEnabled = glIsEnabled(GL_CULL_FACE);

        glEnable(GL_BLEND);
        glBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
        glDisable(GL_DEPTH_TEST);
        glDisable(GL_CULL_FACE);

        glUseProgram(programId);

        Minecraft mc = Minecraft.getInstance();
        float screenW = (float) mc.getWindow().getGuiScaledWidth();
        float screenH = (float) mc.getWindow().getGuiScaledHeight();

        glUniform2f(uScreenSize, screenW, screenH);
        glUniform2f(uRectMin, x, y);
        glUniform2f(uRectMax, x + width, y + height);
        glUniform1f(uRadius, radius);
        glUniform1f(uThickness, thickness);
        glUniform1f(uSoftness, softness);
        glUniform1i(uGradientType, gradientType);

        if (uModelViewMat != -1) {
            java.nio.FloatBuffer buffer = org.lwjgl.BufferUtils.createFloatBuffer(16);
            if (matrix != null) {
                matrix.get(buffer);
            } else {
                buffer.put(new float[]{
                    1, 0, 0, 0,
                    0, 1, 0, 0,
                    0, 0, 1, 0,
                    0, 0, 0, 1
                });
            }
            buffer.flip();
            glUniformMatrix4fv(uModelViewMat, false, buffer);
        }

        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        float a = ((color >> 24) & 0xFF) / 255.0f;
        glUniform4f(uColor, r, g, b, a);

        float r2 = ((color2 >> 16) & 0xFF) / 255.0f;
        float g2 = ((color2 >> 8) & 0xFF) / 255.0f;
        float b2 = (color2 & 0xFF) / 255.0f;
        float a2 = ((color2 >> 24) & 0xFF) / 255.0f;
        glUniform4f(uColor2, r2, g2, b2, a2);

        float[] vertices = {
            x, y,
            x, y + height,
            x + width, y + height,
            x + width, y
        };

        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_DYNAMIC_DRAW);

        glDrawArrays(GL_TRIANGLE_FAN, 0, 4);

        glBindVertexArray(activeVertexArray);
        glBindBuffer(GL_ARRAY_BUFFER, activeArrayBuffer);
        glUseProgram(activeProgram);
        if (!blendEnabled) glDisable(GL_BLEND);
        glBlendFuncSeparate(blendSrcRgb, blendDstRgb, blendSrcAlpha, blendDstAlpha);
        if (depthEnabled) glEnable(GL_DEPTH_TEST);
        if (cullEnabled) glEnable(GL_CULL_FACE);
    }

    public static void drawRoundedRect(org.joml.Matrix4f matrix, float x, float y, float width, float height, float radius, int color) {
        drawSquircle(matrix, x, y, width, height, radius, 0.0f, 1.0f, color, color, 0);
    }

    public static void drawRoundedRect(org.joml.Matrix4f matrix, float x, float y, float width, float height, float radius, int color, float softness) {
        drawSquircle(matrix, x, y, width, height, radius, 0.0f, softness, color, color, 0);
    }

    public static void drawRoundedOutline(org.joml.Matrix4f matrix, float x, float y, float width, float height, float radius, float thickness, int color) {
        drawSquircle(matrix, x, y, width, height, radius, thickness, 1.0f, color, color, 0);
    }

    public static void drawHorizontalGradient(org.joml.Matrix4f matrix, float x, float y, float width, float height, float radius, int startColor, int endColor) {
        drawSquircle(matrix, x, y, width, height, radius, 0.0f, 1.0f, startColor, endColor, 1);
    }

    public static void drawRoundedRect(float x, float y, float width, float height, float radius, int color) {
        drawSquircle(null, x, y, width, height, radius, 0.0f, 1.0f, color, color, 0);
    }

    public static void drawRoundedRect(float x, float y, float width, float height, float radius, int color, float softness) {
        drawSquircle(null, x, y, width, height, radius, 0.0f, softness, color, color, 0);
    }

    public static void drawRoundedOutline(float x, float y, float width, float height, float radius, float thickness, int color) {
        drawSquircle(null, x, y, width, height, radius, thickness, 1.0f, color, color, 0);
    }

    public static void drawHorizontalGradient(float x, float y, float width, float height, float radius, int startColor, int endColor) {
        drawSquircle(null, x, y, width, height, radius, 0.0f, 1.0f, startColor, endColor, 1);
    }
}
