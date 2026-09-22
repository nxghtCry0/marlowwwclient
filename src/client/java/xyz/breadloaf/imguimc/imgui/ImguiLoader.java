package xyz.breadloaf.imguimc.imgui;

import com.eclipseware.imnotcheatingyouare.client.utils.Initializer;
import com.mojang.logging.LogUtils;
import imgui.*;
import imgui.flag.*;
import imgui.gl3.ImGuiImplGl3;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL33;
import org.slf4j.Logger;
import xyz.breadloaf.imguimc.font.FontExtractor;
import xyz.breadloaf.imguimc.interfaces.Renderable;
import xyz.breadloaf.imguimc.interfaces.Theme;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import imgui.type.ImInt;
import org.lwjgl.opengl.GL12;
import java.nio.ByteBuffer;

public class ImguiLoader {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final long CONTENT_SCALE_REFRESH_INTERVAL_NANOS = 250_000_000L;

    private static SdlImGuiPlatform imGuiPlatform = null;
    private static ImGuiImplGl3 imGuiGl3 = null;

    private static long windowHandle;
    private static int customFontTextureId = 0;
    private static float appliedUiScale = 1.0f;
    private static float loadedFontScale = -1.0f;
    private static float cachedWindowContentScale = 1.0f;
    private static long lastContentScaleRefreshNanos;
    private static long lastFrameFailureLogNanos;
    private static final Map<Class<?>, Long> componentFailureLogTimes = new HashMap<>();

    private static boolean fontLoaded = false;
    private static boolean initialized = false;
    private static boolean contextCreated = false;
    private static boolean customFontAvailable = false;
    private static boolean renderedComponentsLastFrame = false;

    public static void onGlfwInit(long handle) {
        if (initialized)
            return;
        initialize(handle);
    }

    private static void initialize(long handle) {
        if (initialized)
            return;

        try {
            FontExtractor.extractFont();
            customFontAvailable = true;
        } catch (IOException exception) {
            customFontAvailable = false;
            LOGGER.warn("Could not extract the custom ImGui font; using the default font", exception);
        }
        windowHandle = handle;
        try {
            initializeImGui();
            imGuiPlatform = new SdlImGuiPlatform();
            imGuiPlatform.init(handle);
            imGuiGl3 = new ImGuiImplGl3();
            imGuiGl3.init();
            rebuildCustomFont(getWindowContentScale());
            initialized = true;
        } catch (RuntimeException | Error error) {
            shutdown();
            throw error;
        }
    }

    private static void rebuildCustomFont(float scale) {
        if (imGuiGl3 == null)
            return;

        ImGuiIO io = ImGui.getIO();
        ImFontAtlas fontAtlas = io.getFonts();
        float fontSize = Math.max(10.0f, Math.round(16.0f * scale));

        fontAtlas.clear();

        ImFontConfig fontConfig = new ImFontConfig();
        ImFont customFont = null;

        try {
            fontConfig.setPixelSnapH(true);
            fontConfig.setOversampleH(3);
            fontConfig.setOversampleV(2);
            fontConfig.setFontDataOwnedByAtlas(true);

            String fontPath = FontExtractor.getFontPath("font.ttf");
            File file = new File(fontPath);
            if (file.exists() && file.length() > 0) {
                customFont = fontAtlas.addFontFromFileTTF(fontPath, fontSize, fontConfig);
            }

            if (customFont == null || !customFont.isValidPtr()) {
                byte[] fontBytes = FontExtractor.getFontBytes();
                if (fontBytes != null && fontBytes.length > 0) {
                    customFont = fontAtlas.addFontFromMemoryTTF(fontBytes, fontSize, fontConfig);
                }
            }

            if (customFont != null && customFont.isValidPtr()) {
                io.setFontDefault(customFont);
                customFontAvailable = true;
            } else {
                customFontAvailable = false;
                io.setFontDefault(fontAtlas.addFontDefault());
                LOGGER.warn("Could not load custom ImGui font; using default font");
            }
        } catch (Throwable t) {
            customFontAvailable = false;
            try {
                io.setFontDefault(fontAtlas.addFontDefault());
            } catch (Throwable ignored) {}
            LOGGER.warn("Failed to add custom ImGui font; using default font", t);
        } finally {
            fontConfig.destroy();
        }

        try {
            fontAtlas.build();
            updateFontsTextureDirect();
            fontLoaded = true;
            loadedFontScale = scale;
        } catch (Throwable t) {
            LOGGER.error("Failed to build and upload ImGui font texture", t);
        }
    }

    private static void updateFontsTextureDirect() {
        ImGuiIO io = ImGui.getIO();
        ImFontAtlas fontAtlas = io.getFonts();

        ImInt width = new ImInt();
        ImInt height = new ImInt();
        ByteBuffer buffer = fontAtlas.getTexDataAsRGBA32(width, height);

        int w = width.get();
        int h = height.get();
        if (w <= 0 || h <= 0 || buffer == null) {
            LOGGER.error("ImGui font atlas generated invalid texture data ({}x{})", w, h);
            return;
        }

        int prevPixelUnpack = GL11.glGetInteger(org.lwjgl.opengl.GL21.GL_PIXEL_UNPACK_BUFFER_BINDING);
        int prevPixelPack = GL11.glGetInteger(org.lwjgl.opengl.GL21.GL_PIXEL_PACK_BUFFER_BINDING);
        int prevSampler = GL11.glGetInteger(GL33.GL_SAMPLER_BINDING);
        int prevActiveTex = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        int prevTex2D = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);

        int prevAlignment = GL11.glGetInteger(GL11.GL_UNPACK_ALIGNMENT);
        int prevRowLength = GL11.glGetInteger(GL11.GL_UNPACK_ROW_LENGTH);
        int prevSkipPixels = GL11.glGetInteger(GL11.GL_UNPACK_SKIP_PIXELS);
        int prevSkipRows = GL11.glGetInteger(GL11.GL_UNPACK_SKIP_ROWS);

        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        org.lwjgl.opengl.GL15.glBindBuffer(org.lwjgl.opengl.GL21.GL_PIXEL_UNPACK_BUFFER, 0);
        org.lwjgl.opengl.GL15.glBindBuffer(org.lwjgl.opengl.GL21.GL_PIXEL_PACK_BUFFER, 0);
        GL33.glBindSampler(0, 0);

        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);
        GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, 0);
        GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, 0);
        GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, 0);

        try {
            if (customFontTextureId != 0) {
                GL11.glDeleteTextures(customFontTextureId);
                customFontTextureId = 0;
            }

            customFontTextureId = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, customFontTextureId);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);

            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, w, h, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);

            fontAtlas.setTexID(customFontTextureId);
        } finally {
            GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, prevAlignment);
            GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, prevRowLength);
            GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, prevSkipPixels);
            GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, prevSkipRows);

            org.lwjgl.opengl.GL15.glBindBuffer(org.lwjgl.opengl.GL21.GL_PIXEL_UNPACK_BUFFER, prevPixelUnpack);
            org.lwjgl.opengl.GL15.glBindBuffer(org.lwjgl.opengl.GL21.GL_PIXEL_PACK_BUFFER, prevPixelPack);
            GL33.glBindSampler(0, prevSampler);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, prevTex2D);
            GL13.glActiveTexture(prevActiveTex);
        }
    }

    public static void onFrameRender() {
        if (!initialized) {
            try {
                net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                if (mc != null && mc.getWindow() != null) {
                    initialize(mc.getWindow().handle());
                }
            } catch (Throwable ignored) {}
        }
        if (!shouldRenderFrame())
            return;
        renderFrame();
    }

    public static boolean shouldRenderFrame() {
        return initialized && (!Initializer.renderstack.isEmpty() || renderedComponentsLastFrame);
    }

    private static void renderFrame() {
        if (!initialized) return;

        boolean hasComponents = !Initializer.renderstack.isEmpty();
        if (!hasComponents && !renderedComponentsLastFrame)
            return;
        if (hasComponents)
            renderedComponentsLastFrame = true;

        boolean frameStarted = false;
        try {
            applyDisplayScale();
            imGuiPlatform.newFrame();
            ImGui.newFrame();
            frameStarted = true;

            setupDocking();
            try {
                for (Renderable renderable: Initializer.renderstack) {
                    if (renderable == null)
                        continue;

                    Theme theme = null;
                    try {
                        theme = renderable.getTheme();
                        if (theme == null)
                            throw new IllegalStateException("ImGui renderable returned a null theme");
                        theme.preRender();
                        renderable.render();
                    } catch (RuntimeException exception) {
                        logComponentFailure(renderable, exception);
                    } finally {
                        if (theme != null) {
                            try {
                                theme.postRender();
                            } catch (RuntimeException exception) {
                                logComponentFailure(renderable, exception);
                            }
                        }
                    }
                }
            } finally {
                finishDocking();
            }

            syncTextInputState();

            ImGui.render();
            frameStarted = false;
            endFrame(windowHandle);
            if (!hasComponents)
                renderedComponentsLastFrame = false;
        } catch (RuntimeException exception) {
            if (frameStarted) {
                try {
                    ImGui.endFrame();
                } catch (RuntimeException endFrameException) {
                    exception.addSuppressed(endFrameException);
                }
            }
            logFrameFailure(exception);
        }
    }

    private static final Object TEXT_INPUT_OWNER = new Object();
    private static boolean sdlTextInputActive = false;

    private static void syncTextInputState() {
        boolean wantsText = ImGui.getIO().getWantTextInput();
        if (wantsText == sdlTextInputActive)
            return;

        sdlTextInputActive = wantsText;
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc == null)
            return;

        if (wantsText) {
            mc.textInputManager().startTextInput(TEXT_INPUT_OWNER);
        } else {
            mc.textInputManager().stopTextInput(TEXT_INPUT_OWNER);
        }
    }

    private static void logFrameFailure(RuntimeException exception) {
        long now = System.nanoTime();
        if (lastFrameFailureLogNanos != 0L && now - lastFrameFailureLogNanos < 10_000_000_000L)
            return;

        lastFrameFailureLogNanos = now;
        LOGGER.error("Failed to render the ImGui frame", exception);
    }

    private static void logComponentFailure(Renderable renderable, RuntimeException exception) {
        Class<?> renderableClass = renderable.getClass();
        long now = System.nanoTime();
        Long previousLog = componentFailureLogTimes.get(renderableClass);
        if (previousLog != null && now - previousLog < 10_000_000_000L)
            return;

        componentFailureLogTimes.put(renderableClass, now);
        LOGGER.error("Failed to render ImGui component {}", renderableClass.getName(), exception);
    }

    private static void setupDocking() {
    }

    private static void finishDocking() {
    }

    private static void initializeImGui() {
        ImGui.createContext();
        contextCreated = true;

        final ImGuiIO io = ImGui.getIO();

        io.setIniFilename(null);                               
        io.removeConfigFlags(ImGuiConfigFlags.NavEnableKeyboard);
        io.removeConfigFlags(ImGuiConfigFlags.DockingEnable);

        final ImFontAtlas fontAtlas = io.getFonts();
        fontAtlas.addFontDefault();
    }

    private static void applyDisplayScale() {
        float scale = getWindowContentScale();
        ImGuiIO io = ImGui.getIO();
        io.setFontGlobalScale(scale > 0f ? 1.0f / scale : 1.0f);

        if (!fontLoaded || (customFontAvailable && Math.abs(scale - loadedFontScale) > 0.15f)) {
            rebuildCustomFont(scale);
        }

        if (Math.abs(scale - appliedUiScale) > 0.05f) {
            ImGui.getStyle().scaleAllSizes(scale / appliedUiScale);
            appliedUiScale = scale;
        }
    }

    private static float getWindowContentScale() {
        long now = System.nanoTime();
        if (lastContentScaleRefreshNanos != 0L
                && now - lastContentScaleRefreshNanos < CONTENT_SCALE_REFRESH_INTERVAL_NANOS)
            return cachedWindowContentScale;

        float scale = 1.0f;

        try {
            com.mojang.blaze3d.platform.Window window = net.minecraft.client.Minecraft.getInstance().getWindow();
            int guiScaledWidth = window.getGuiScaledWidth();
            if (guiScaledWidth > 0) {
                scale = window.queryFramebufferSize().width() / (float) guiScaledWidth;
            }
        } catch (Throwable ignored) {
            scale = 1.0f;
        }

        if (!Float.isFinite(scale) || scale <= 0.0f)
            scale = 1.0f;

        scale = Math.round(scale / 0.05f) * 0.05f;

        cachedWindowContentScale = Math.max(1.0f, scale);
        lastContentScaleRefreshNanos = now;
        return cachedWindowContentScale;
    }

    private static void endFrame(long windowPtr) {
        int framebufferBinding = GL11.glGetInteger(org.lwjgl.opengl.GL30.GL_FRAMEBUFFER_BINDING);
        int activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        int texture0Binding = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        int sampler0Binding = GL11.glGetInteger(GL33.GL_SAMPLER_BINDING);
        int pixelUnpackBinding = GL11.glGetInteger(org.lwjgl.opengl.GL21.GL_PIXEL_UNPACK_BUFFER_BINDING);
        int pixelPackBinding = GL11.glGetInteger(org.lwjgl.opengl.GL21.GL_PIXEL_PACK_BUFFER_BINDING);

        boolean scissorTest = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        boolean depthTest = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean cullFace = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean stencilTest = GL11.glIsEnabled(GL11.GL_STENCIL_TEST);

        int blendSrcRgb = GL11.glGetInteger(org.lwjgl.opengl.GL14.GL_BLEND_SRC_RGB);
        int blendDstRgb = GL11.glGetInteger(org.lwjgl.opengl.GL14.GL_BLEND_DST_RGB);
        int blendSrcAlpha = GL11.glGetInteger(org.lwjgl.opengl.GL14.GL_BLEND_SRC_ALPHA);
        int blendDstAlpha = GL11.glGetInteger(org.lwjgl.opengl.GL14.GL_BLEND_DST_ALPHA);
        int blendEquationRgb = GL11.glGetInteger(org.lwjgl.opengl.GL20.GL_BLEND_EQUATION_RGB);
        int blendEquationAlpha = GL11.glGetInteger(org.lwjgl.opengl.GL20.GL_BLEND_EQUATION_ALPHA);

        int program = GL11.glGetInteger(org.lwjgl.opengl.GL20.GL_CURRENT_PROGRAM);
        int vao = GL11.glGetInteger(org.lwjgl.opengl.GL30.GL_VERTEX_ARRAY_BINDING);
        int arrayBuffer = GL11.glGetInteger(org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER_BINDING);
        int elementArrayBuffer = GL11.glGetInteger(org.lwjgl.opengl.GL15.GL_ELEMENT_ARRAY_BUFFER_BINDING);

        GL13.glActiveTexture(activeTexture);
        int currentActiveTextureBinding = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);

        try {
            prepareImGuiGlState();
            imGuiGl3.renderDrawData(ImGui.getDrawData());
        } finally {
            org.lwjgl.opengl.GL30.glBindFramebuffer(org.lwjgl.opengl.GL30.GL_FRAMEBUFFER, framebufferBinding);
            org.lwjgl.opengl.GL20.glUseProgram(program);
            org.lwjgl.opengl.GL30.glBindVertexArray(vao);
            org.lwjgl.opengl.GL15.glBindBuffer(org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER, arrayBuffer);
            org.lwjgl.opengl.GL15.glBindBuffer(org.lwjgl.opengl.GL15.GL_ELEMENT_ARRAY_BUFFER, elementArrayBuffer);
            org.lwjgl.opengl.GL15.glBindBuffer(org.lwjgl.opengl.GL21.GL_PIXEL_UNPACK_BUFFER, pixelUnpackBinding);
            org.lwjgl.opengl.GL15.glBindBuffer(org.lwjgl.opengl.GL21.GL_PIXEL_PACK_BUFFER, pixelPackBinding);

            restoreCapability(GL11.GL_SCISSOR_TEST, scissorTest);
            restoreCapability(GL11.GL_DEPTH_TEST, depthTest);
            restoreCapability(GL11.GL_CULL_FACE, cullFace);
            restoreCapability(GL11.GL_BLEND, blend);
            restoreCapability(GL11.GL_STENCIL_TEST, stencilTest);

            org.lwjgl.opengl.GL14.glBlendFuncSeparate(blendSrcRgb, blendDstRgb, blendSrcAlpha, blendDstAlpha);
            org.lwjgl.opengl.GL20.glBlendEquationSeparate(blendEquationRgb, blendEquationAlpha);

            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            GL33.glBindSampler(0, sampler0Binding);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture0Binding);

            GL13.glActiveTexture(activeTexture);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, currentActiveTextureBinding);
        }
    }

    private static void prepareImGuiGlState() {
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL33.glBindSampler(0, 0);
        org.lwjgl.opengl.GL15.glBindBuffer(org.lwjgl.opengl.GL21.GL_PIXEL_UNPACK_BUFFER, 0);
        org.lwjgl.opengl.GL15.glBindBuffer(org.lwjgl.opengl.GL21.GL_PIXEL_PACK_BUFFER, 0);
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_STENCIL_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    }

    private static void restoreCapability(int capability, boolean enabled) {
        if (enabled) {
            GL11.glEnable(capability);
        } else {
            GL11.glDisable(capability);
        }
    }

    public static boolean wantsCaptureKeyboard() {
        return initialized && ImGui.getIO().getWantCaptureKeyboard();
    }

    public static boolean wantsTextInput() {
        return initialized && ImGui.getIO().getWantTextInput();
    }

    public static boolean wantsCaptureMouse() {
        return initialized && ImGui.getIO().getWantCaptureMouse();
    }

    public static void shutdown() {
        shutdownInternal();
    }

    private static void shutdownInternal() {
        if (sdlTextInputActive) {
            sdlTextInputActive = false;
            try {
                net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                if (mc != null) mc.textInputManager().stopTextInput(TEXT_INPUT_OWNER);
            } catch (Throwable ignored) {}
        }
        initialized = false;
        fontLoaded = false;
        customFontAvailable = false;
        renderedComponentsLastFrame = false;
        loadedFontScale = -1.0f;
        appliedUiScale = 1.0f;
        cachedWindowContentScale = 1.0f;
        lastContentScaleRefreshNanos = 0L;
        lastFrameFailureLogNanos = 0L;
        componentFailureLogTimes.clear();
        windowHandle = 0L;

        if (customFontTextureId != 0) {
            try {
                GL11.glDeleteTextures(customFontTextureId);
            } catch (Throwable ignored) {}
            customFontTextureId = 0;
        }

        if (imGuiGl3 != null) {
            try {
                imGuiGl3.dispose();
            } catch (Throwable exception) {
                LOGGER.warn("Failed to dispose the ImGui OpenGL backend", exception);
            } finally {
                imGuiGl3 = null;
            }
        }

        if (imGuiPlatform != null) {
            try {
                imGuiPlatform.dispose();
            } catch (Throwable exception) {
                LOGGER.warn("Failed to dispose the ImGui SDL platform backend", exception);
            } finally {
                imGuiPlatform = null;
            }
        }

        if (contextCreated) {
            try {
                ImGui.destroyContext();
            } catch (Throwable exception) {
                LOGGER.warn("Failed to destroy the ImGui context", exception);
            } finally {
                contextCreated = false;
            }
        }
    }
}
