package com.eclipseware.imnotcheatingyouare.client.glassui.render;

import com.eclipseware.imnotcheatingyouare.client.glassui.asset.UiAssets;
import com.eclipseware.imnotcheatingyouare.client.glassui.asset.UiIcon;
import com.mojang.blaze3d.platform.Window;
import com.sun.jna.Function;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.awt.image.WritableRaster;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.lwjgl.sdl.SDLProperties;
import org.lwjgl.sdl.SDLVideo;

public final class StreamproofOverlay {
    public interface User32Direct extends StdCallLibrary {
        short RegisterClassExW(Pointer lpWndClass);
        Pointer CreateWindowExW(int dwExStyle, Pointer lpClassName, Pointer lpWindowName, int dwStyle, int x, int y, int nWidth, int nHeight, Pointer hWndParent, Pointer hMenu, Pointer hInstance, Pointer lpParam);
        boolean DestroyWindow(Pointer hWnd);
        boolean IsWindow(Pointer hWnd);
        boolean ShowWindow(Pointer hWnd, int nCmdShow);
        boolean SetWindowPos(Pointer hWnd, Pointer hWndInsertAfter, int X, int Y, int cx, int cy, int uFlags);
        boolean SetLayeredWindowAttributes(Pointer hWnd, int crKey, byte bAlpha, int dwFlags);
        boolean SetWindowDisplayAffinity(Pointer hWnd, int dwAffinity);
        boolean GetWindowDisplayAffinity(Pointer hWnd, IntByReference pdwAffinity);
        boolean GetClientRect(Pointer hWnd, Pointer lpRect);
        boolean ClientToScreen(Pointer hWnd, Pointer lpPoint);
        Pointer GetForegroundWindow();
        Pointer GetDC(Pointer hWnd);
        int ReleaseDC(Pointer hWnd, Pointer hDC);
        boolean PeekMessageW(Pointer lpMsg, Pointer hWnd, int wMsgFilterMin, int wMsgFilterMax, int wRemoveMsg);
        boolean TranslateMessage(Pointer lpMsg);
        Pointer DispatchMessageW(Pointer lpMsg);
    }

    public interface Gdi32Direct extends StdCallLibrary {
        Pointer CreateCompatibleDC(Pointer hdc);
        boolean DeleteDC(Pointer hdc);
        Pointer CreateDIBSection(Pointer hdc, Pointer pbmi, int usage, PointerByReference ppvBits, Pointer hSection, int offset);
        Pointer SelectObject(Pointer hdc, Pointer h);
        Pointer CreateRectRgn(int x1, int y1, int x2, int y2);
        boolean DeleteObject(Pointer ho);
        boolean BitBlt(Pointer hdc, int x, int y, int cx, int cy, Pointer hdcSrc, int x1, int y1, int rop);
    }

    public interface DwmApiDirect extends StdCallLibrary {
        int DwmExtendFrameIntoClientArea(Pointer hWnd, Pointer pMarInset);
        int DwmEnableBlurBehindWindow(Pointer hWnd, Pointer pBlurBehind);
        int DwmSetWindowAttribute(Pointer hWnd, int dwAttribute, Pointer pvAttribute, int cbAttribute);
    }

    public interface Kernel32Direct extends StdCallLibrary {
        Pointer GetModuleHandleW(String lpModuleName);
    }

    private sealed interface DrawOp {
        record PushClip(int x0, int y0, int x1, int y1) implements DrawOp {}
        record PopClip() implements DrawOp {}
        record GlassCard(float x, float y, float w, float h, float radius, int topFill, int bottomFill, int topBorder, int bottomBorder) implements DrawOp {}
        record RoundedGrad(float x, float y, float w, float h, float radius, int topColor, int bottomColor) implements DrawOp {}
        record RoundedBorder(float x, float y, float w, float h, float radius, float thickness, int topColor, int bottomColor) implements DrawOp {}
        record RoundedShadow(float x, float y, float w, float h, float radius, float spread, int shadowColor) implements DrawOp {}
        record Icon(Identifier texture, float x, float y, float size, int color) implements DrawOp {}
        record Avatar(Identifier skinId, float cx, float cy, float radius, int tintColor) implements DrawOp {}
        record BlockIcon(Identifier blockId, int fallbackColor, float x, float y, float size, float alpha) implements DrawOp {}
        record TextRun(String text, float x, float y, float sizePx, TextGradient gradient, SatoshiWeight weight, List<SatoshiFont.PlacedGlyph> glyphs) implements DrawOp {}
    }

    private record FramePacket(
        long mcHwndPeer,
        int guiWidth,
        int guiHeight,
        float centerX,
        float centerY,
        float renderScale,
        float panelW,
        float panelH,
        StreamproofMode mode,
        List<DrawOp> ops
    ) {}

    private record IconTintKey(Identifier texture, int rgb24) {}
    private record GlassCardKey(int wPx, int hPx, int rPx, int topFill, int botFill, int topBorder, int botBorder) {}
    private record TextRunKey(String text, SatoshiWeight weight, int quantizedPxSize, int tlRgb, int brRgb) {}
    private record CachedTextSprite(BufferedImage image, float drawOffsetX, float drawOffsetY, float logicalW, float logicalH) {}

    private static final DrawOp.PopClip POP_CLIP = new DrawOp.PopClip();
    private static final int WS_POPUP = 0x80000000;
    private static final int WS_EX_TOPMOST = 0x00000008;
    private static final int WS_EX_TRANSPARENT = 0x00000020;
    private static final int WS_EX_TOOLWINDOW = 0x00000080;
    private static final int WS_EX_LAYERED = 0x00080000;
    private static final int WS_EX_NOACTIVATE = 0x08000000;
    private static final int SW_HIDE = 0;
    private static final int SW_SHOWNOACTIVATE = 4;
    private static final int SWP_NOSIZE = 0x0001;
    private static final int SWP_NOMOVE = 0x0002;
    private static final int SWP_NOACTIVATE = 0x0010;
    private static final int SWP_SHOWWINDOW = 0x0040;
    private static final int LWA_ALPHA = 0x00000002;
    private static final int SRCCOPY = 0x00CC0020;
    private static final int PM_REMOVE = 0x0001;
    private static final int DWM_BB_ENABLE = 0x00000001;
    private static final int DWM_BB_BLURREGION = 0x00000002;
    private static final int DWMWA_NCRENDERING_POLICY = 2;
    private static final int DWMNCRP_DISABLED = 1;
    private static final int DWMWA_WINDOW_CORNER_PREFERENCE = 33;
    private static final int DWMWCP_DONOTROUND = 1;
    private static final int DWMWA_BORDER_COLOR = 34;
    private static final int DWMWA_COLOR_NONE = -2;
    private static final int WDA_NONE = 0x00000000;
    private static final int WDA_MONITOR = 0x00000001;
    private static final int WDA_EXCLUDEFROMCAPTURE = 0x00000011;

    private static volatile StreamproofSettings settings = StreamproofSettings.defaults();
    private static final Pointer HWND_TOPMOST = Pointer.createConstant(-1L);
    private static final boolean IS_WINDOWS = detectWindows();

    private static boolean detectWindows() {
        String os = System.getProperty("os.name", "");
        return os.toLowerCase(Locale.ROOT).contains("win");
    }

    private static final User32Direct USER32 = loadLib("user32", User32Direct.class);
    private static final Gdi32Direct GDI32 = loadLib("gdi32", Gdi32Direct.class);
    private static final DwmApiDirect DWMAPI = loadLib("dwmapi", DwmApiDirect.class);
    private static final Kernel32Direct KERNEL32 = loadLib("kernel32", Kernel32Direct.class);

    private static volatile boolean overlayDisabledAfterFailure = false;
    private static volatile long cachedSdlHandle = 0L;
    private static volatile long cachedMcHwndPeer = 0L;
    private static final AtomicBoolean visibleDesired = new AtomicBoolean(false);
    private static final AtomicReference<FramePacket> pendingFrame = new AtomicReference<>(null);
    private static final AtomicBoolean workerStarted = new AtomicBoolean(false);
    private static volatile Thread presenterThread;

    private static ArrayList<DrawOp> recordingOps;
    private static long recHwndPeer;
    private static int recGuiW;
    private static int recGuiH;
    private static float recCenterX;
    private static float recCenterY;
    private static float recRenderScale = 1f;
    private static float recPanelW = 576f;
    private static float recPanelH = 340f;
    private static StreamproofMode recMode = StreamproofMode.OVERLAY;
    private static volatile boolean capturingFrame = false;

    private static final Map<Identifier, BufferedImage> sharedSkinCache = new ConcurrentHashMap<>();
    private static final Set<Identifier> attemptedSkinLoads = ConcurrentHashMap.newKeySet();
    private static final Map<Identifier, BufferedImage> blockIsoIconCache = new ConcurrentHashMap<>();
    private static final Set<Identifier> attemptedBlockIconLoads = ConcurrentHashMap.newKeySet();
    private static volatile BufferedImage cachedFontAtlas;
    private static volatile boolean fontAtlasLoaded = false;

    private StreamproofOverlay() {
    }

    private static <T extends StdCallLibrary> T loadLib(String name, Class<T> cls) {
        if (!IS_WINDOWS) {
            return null;
        }
        try {
            return Native.load(name, cls, W32APIOptions.DEFAULT_OPTIONS);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static BufferedImage fontAtlasImage() {
        if (!fontAtlasLoaded) {
            fontAtlasLoaded = true;
            try (InputStream stream = StreamproofOverlay.class.getResourceAsStream(UiAssets.fontAtlasClasspath())) {
                if (stream != null) {
                    cachedFontAtlas = ImageIO.read(stream);
                }
            } catch (Exception ignored) {
            }
        }
        return cachedFontAtlas;
    }

    public static void configure(StreamproofSettings newSettings) {
        if (newSettings != null) {
            settings = newSettings;
        }
    }

    public static boolean isCapturingFrame() {
        return capturingFrame;
    }

    public static boolean isSupported() {
        return IS_WINDOWS && USER32 != null && GDI32 != null && DWMAPI != null && !overlayDisabledAfterFailure;
    }

    public static StreamproofMode currentMode() {
        return settings.mode().get();
    }

    public static boolean shouldUseExternalOverlay() {
        if (!isSupported()) {
            return false;
        }
        if (!settings.enabled().getAsBoolean()) {
            return false;
        }
        return settings.mode().get() != StreamproofMode.IN_GAME;
    }

    public static float glassOpacityMultiplier() {
        return (capturingFrame && settings.mode().get() == StreamproofMode.CRISP) ? 0.78f : 1.0f;
    }

    public static boolean beginFrame(
        Minecraft mc,
        int guiWidth,
        int guiHeight,
        float centerX,
        float centerY,
        float renderScale,
        float panelW,
        float panelH
    ) {
        if (!shouldUseExternalOverlay()) {
            hide();
            return false;
        }
        Window window = mc.getWindow();
        if (window.isIconified() || !mc.isWindowActive() || guiWidth <= 0 || guiHeight <= 0) {
            hide();
            return false;
        }
        if (window.isExclusiveFullscreen()) {
            window.setExclusiveFullscreen(false);
        }
        long hwndPeer = resolveMinecraftHwndPeerFast(window);
        if (hwndPeer == 0L) {
            hide();
            return false;
        }
        ensureWorkerRunning();
        recordingOps = new ArrayList<>(96);
        recHwndPeer = hwndPeer;
        recGuiW = guiWidth;
        recGuiH = guiHeight;
        recCenterX = centerX;
        recCenterY = centerY;
        recRenderScale = renderScale;
        recPanelW = panelW;
        recPanelH = panelH;
        recMode = settings.mode().get();
        capturingFrame = true;
        return true;
    }

    public static void endFrame() {
        if (!capturingFrame) {
            return;
        }
        capturingFrame = false;
        ArrayList<DrawOp> ops = recordingOps;
        recordingOps = null;
        if (ops == null) {
            return;
        }
        FramePacket packet = new FramePacket(
            recHwndPeer, recGuiW, recGuiH, recCenterX, recCenterY,
            recRenderScale, recPanelW, recPanelH, recMode, ops
        );
        visibleDesired.set(true);
        pendingFrame.set(packet);
        Thread t = presenterThread;
        if (t != null) {
            LockSupport.unpark(t);
        }
    }

    public static void hide() {
        capturingFrame = false;
        recordingOps = null;
        pendingFrame.set(null);
        if (visibleDesired.compareAndSet(true, false)) {
            Thread t = presenterThread;
            if (t != null) {
                LockSupport.unpark(t);
            }
        }
    }

    public static void pushScissor(int x0, int y0, int x1, int y1) {
        if (recordingOps != null) {
            recordingOps.add(new DrawOp.PushClip(x0, y0, x1, y1));
        }
    }

    public static void popScissor() {
        if (recordingOps != null) {
            recordingOps.add(POP_CLIP);
        }
    }

    public static void recordGlassCard(float x, float y, float w, float h, float radius, int topFill, int bottomFill, int topBorder, int bottomBorder) {
        if (recordingOps != null) {
            recordingOps.add(new DrawOp.GlassCard(x, y, w, h, radius, topFill, bottomFill, topBorder, bottomBorder));
        }
    }

    public static void drawRoundedGradient(float x, float y, float w, float h, float radius, int topColor, int bottomColor) {
        if (recordingOps != null) {
            recordingOps.add(new DrawOp.RoundedGrad(x, y, w, h, radius, topColor, bottomColor));
        }
    }

    public static void drawRoundedBorder(float x, float y, float w, float h, float radius, float thickness, int topColor, int bottomColor) {
        if (recordingOps != null) {
            recordingOps.add(new DrawOp.RoundedBorder(x, y, w, h, radius, thickness, topColor, bottomColor));
        }
    }

    public static void drawRoundedShadow(float x, float y, float w, float h, float radius, float spread, int shadowColor) {
        if (recordingOps != null) {
            recordingOps.add(new DrawOp.RoundedShadow(x, y, w, h, radius, spread, shadowColor));
        }
    }

    public static void drawIcon(UiIcon icon, float x, float y, float size, int color) {
        if (recordingOps != null) {
            recordingOps.add(new DrawOp.Icon(icon.texture(), x, y, size, color));
        }
    }

    public static void drawCircularAvatar(Identifier skinTexture, float cx, float cy, float radius, int tintColor) {
        if (attemptedSkinLoads.add(skinTexture)) {
            try {
                Minecraft mc = Minecraft.getInstance();
                ResourceManager rm = mc.getResourceManager();
                Optional<Resource> resOpt = rm.getResource(skinTexture);
                if (resOpt.isPresent()) {
                    Resource res = resOpt.get();
                    try (InputStream stream = res.open()) {
                        BufferedImage img = ImageIO.read(stream);
                        if (img != null) {
                            sharedSkinCache.put(skinTexture, img);
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }
        if (recordingOps != null) {
            recordingOps.add(new DrawOp.Avatar(skinTexture, cx, cy, radius, tintColor));
        }
    }

    public static void drawBlockIcon(Identifier blockId, int fallbackColor, float x, float y, float size, float alpha) {
        if (attemptedBlockIconLoads.add(blockId)) {
            BufferedImage thumb = buildBlockFallbackThumbnail(fallbackColor);
            blockIsoIconCache.put(blockId, thumb);
        }
        if (recordingOps != null) {
            recordingOps.add(new DrawOp.BlockIcon(blockId, fallbackColor, x, y, size, alpha));
        }
    }

    private static BufferedImage buildBlockFallbackThumbnail(int fallbackColor) {
        BufferedImage out = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D ig = out.createGraphics();
        ig.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int rgb = fallbackColor & 0x00FFFFFF;
        Color base = new Color((0xFF << 24) | rgb, true);
        ig.setColor(base.brighter());
        ig.fillPolygon(new Polygon(new int[]{16, 27, 16, 5}, new int[]{3, 9, 16, 9}, 4));
        ig.setColor(base);
        ig.fillPolygon(new Polygon(new int[]{5, 16, 16, 5}, new int[]{9, 16, 28, 22}, 4));
        ig.setColor(base.darker());
        ig.fillPolygon(new Polygon(new int[]{16, 27, 27, 16}, new int[]{16, 9, 22, 28}, 4));
        ig.setColor(new Color(255, 255, 255, 95));
        ig.setStroke(new BasicStroke(1.2f));
        ig.drawPolygon(new Polygon(new int[]{16, 27, 27, 16, 5, 5}, new int[]{3, 9, 22, 28, 22, 9}, 6));
        ig.dispose();
        return out;
    }

    static void drawSatoshiRun(
        String text,
        float x,
        float y,
        float sizePx,
        TextGradient gradient,
        SatoshiWeight weight,
        List<SatoshiFont.PlacedGlyph> glyphs
    ) {
        if (recordingOps != null) {
            recordingOps.add(new DrawOp.TextRun(text, x, y, sizePx, gradient, weight, glyphs));
        }
    }

    private static long resolveMinecraftHwndPeerFast(Window window) {
        long sdlHandle = window.handle();
        if (sdlHandle != 0L && sdlHandle == cachedSdlHandle && cachedMcHwndPeer != 0L) {
            return cachedMcHwndPeer;
        }
        try {
            if (sdlHandle != 0L) {
                int props = SDLVideo.SDL_GetWindowProperties(sdlHandle);
                if (props != 0) {
                    long rawHwnd = SDLProperties.SDL_GetPointerProperty(props, SDLVideo.SDL_PROP_WINDOW_WIN32_HWND_POINTER, 0L);
                    if (rawHwnd != 0L) {
                        cachedSdlHandle = sdlHandle;
                        cachedMcHwndPeer = rawHwnd;
                        return rawHwnd;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return 0L;
    }

    private static void ensureWorkerRunning() {
        if (!workerStarted.compareAndSet(false, true)) {
            return;
        }
        Thread thread = new Thread(StreamproofOverlay::presenterLoop, "Marlow-Streamproof-Presenter");
        thread.setDaemon(true);
        thread.setPriority(Thread.NORM_PRIORITY + 1);
        presenterThread = thread;
        thread.start();
    }

    private static void presenterLoop() {
        if (USER32 == null || GDI32 == null || DWMAPI == null || KERNEL32 == null) {
            return;
        }
        boolean classRegistered = false;
        Memory classNameMem = null;
        Pointer overlayHwnd = null;
        boolean overlayVisible = false;
        int lastWinX = Integer.MIN_VALUE;
        int lastWinY = Integer.MIN_VALUE;
        int lastWinW = -1;
        int lastWinH = -1;
        long lastTopmostRefreshMs = 0L;
        boolean gameAffinityApplied = false;
        Pointer gameAffinityHwnd = null;
        int failures = 0;

        Pointer memDc = null;
        Pointer dibBitmap = null;
        Pointer oldBitmap = null;
        Pointer dibBits = null;
        int dibW = 0;
        int dibH = 0;
        BufferedImage frameImage = null;

        Memory rectBuf = new Memory(16);
        Memory ptTopLeft = new Memory(8);
        Memory msgBuf = new Memory(48);
        IntByReference affinityOut = new IntByReference();
        Memory dwmMargins = new Memory(16);
        dwmMargins.setInt(0, -1);
        dwmMargins.setInt(4, -1);
        dwmMargins.setInt(8, -1);
        dwmMargins.setInt(12, -1);
        Memory dwmAttr = new Memory(4);

        Map<Identifier, BufferedImage> rawIconCache = new HashMap<>();
        LinkedHashMap<IconTintKey, BufferedImage> tintedIconCache = new LinkedHashMap<>(96, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<IconTintKey, BufferedImage> eldest) {
                return size() > 128;
            }
        };
        LinkedHashMap<GlassCardKey, BufferedImage> glassCardCache = new LinkedHashMap<>(64, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<GlassCardKey, BufferedImage> eldest) {
                return size() > 72;
            }
        };
        LinkedHashMap<TextRunKey, CachedTextSprite> textRunCache = new LinkedHashMap<>(256, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<TextRunKey, CachedTextSprite> eldest) {
                return size() > 320;
            }
        };
        ArrayList<Shape> clipStack = new ArrayList<>(8);

        while (true) {
            if (!visibleDesired.get()) {
                if (gameAffinityApplied) {
                    if (gameAffinityHwnd != null) {
                        USER32.SetWindowDisplayAffinity(gameAffinityHwnd, WDA_NONE);
                    }
                    gameAffinityApplied = false;
                    gameAffinityHwnd = null;
                }
                if (overlayVisible && overlayHwnd != null) {
                    USER32.ShowWindow(overlayHwnd, SW_HIDE);
                    pumpMessages(overlayHwnd, msgBuf);
                    overlayVisible = false;
                    lastWinX = Integer.MIN_VALUE;
                    lastWinY = Integer.MIN_VALUE;
                    lastWinW = -1;
                    lastWinH = -1;
                }
                if (overlayHwnd != null) {
                    pumpMessages(overlayHwnd, msgBuf);
                }
                LockSupport.parkNanos(25_000_000L);
                continue;
            }

            FramePacket packet = pendingFrame.getAndSet(null);
            if (packet == null) {
                if (overlayHwnd != null) {
                    pumpMessages(overlayHwnd, msgBuf);
                }
                LockSupport.parkNanos(4_000_000L);
                continue;
            }

            Pointer mcHwnd = new Pointer(packet.mcHwndPeer());
            Pointer fg = USER32.GetForegroundWindow();
            if (fg != null && !fg.equals(mcHwnd) && !fg.equals(overlayHwnd)) {
                if (overlayVisible && overlayHwnd != null) {
                    USER32.ShowWindow(overlayHwnd, SW_HIDE);
                    overlayVisible = false;
                }
                continue;
            }

            if (packet.mode() == StreamproofMode.WINDOW) {
                if (!gameAffinityApplied || !mcHwnd.equals(gameAffinityHwnd)) {
                    if (!USER32.SetWindowDisplayAffinity(mcHwnd, WDA_EXCLUDEFROMCAPTURE)) {
                        USER32.SetWindowDisplayAffinity(mcHwnd, WDA_MONITOR);
                    }
                    gameAffinityApplied = true;
                    gameAffinityHwnd = mcHwnd;
                }
            } else if (gameAffinityApplied) {
                if (gameAffinityHwnd != null) {
                    USER32.SetWindowDisplayAffinity(gameAffinityHwnd, WDA_NONE);
                }
                gameAffinityApplied = false;
                gameAffinityHwnd = null;
            }

            if (!USER32.GetClientRect(mcHwnd, rectBuf)) {
                continue;
            }
            int clientW = rectBuf.getInt(8) - rectBuf.getInt(0);
            int clientH = rectBuf.getInt(12) - rectBuf.getInt(4);
            if (clientW <= 32 || clientH <= 32) {
                continue;
            }

            ptTopLeft.setInt(0, 0);
            ptTopLeft.setInt(4, 0);
            if (!USER32.ClientToScreen(mcHwnd, ptTopLeft)) {
                continue;
            }
            int clientScreenX = ptTopLeft.getInt(0);
            int clientScreenY = ptTopLeft.getInt(4);

            float pxPerGuiX = (float) clientW / packet.guiWidth();
            float pxPerGuiY = (float) clientH / packet.guiHeight();
            float halfW = (packet.panelW() * 0.5f + 18f) * packet.renderScale();
            float halfH = (packet.panelH() * 0.5f + 18f) * packet.renderScale();

            int leftPx = clampInt((int) Math.floor((packet.centerX() - halfW) * pxPerGuiX), 0, clientW - 16);
            int topPx = clampInt((int) Math.floor((packet.centerY() - halfH) * pxPerGuiY), 0, clientH - 16);
            int rightPx = clampInt((int) Math.ceil((packet.centerX() + halfW) * pxPerGuiX), leftPx + 16, clientW);
            int bottomPx = clampInt((int) Math.ceil((packet.centerY() + halfH) * pxPerGuiY), topPx + 16, clientH);

            int ovW = clampInt(rightPx - leftPx, 16, 3840);
            int ovH = clampInt(bottomPx - topPx, 16, 2160);

            if (overlayHwnd == null || !USER32.IsWindow(overlayHwnd)) {
                overlayVisible = false;
                Pointer hInst = KERNEL32.GetModuleHandleW(null);
                if (!classRegistered) {
                    Function defProc = Function.getFunction("user32", "DefWindowProcW");
                    byte[] bytes = (settings.windowClassName() + "\u0000").getBytes(StandardCharsets.UTF_16LE);
                    classNameMem = new Memory(bytes.length);
                    classNameMem.write(0L, bytes, 0, bytes.length);
                    Memory wc = new Memory(80L);
                    wc.clear();
                    wc.setInt(0L, 80);
                    wc.setPointer(8L, defProc);
                    wc.setPointer(24L, hInst);
                    wc.setPointer(64L, classNameMem);
                    USER32.RegisterClassExW(wc);
                    classRegistered = true;
                }
                int ex = WS_EX_LAYERED | WS_EX_TRANSPARENT | WS_EX_TOPMOST | WS_EX_TOOLWINDOW | WS_EX_NOACTIVATE;
                overlayHwnd = USER32.CreateWindowExW(ex, classNameMem, null, WS_POPUP, 0, 0, 64, 64, null, null, hInst, null);
                if (overlayHwnd == null) {
                    continue;
                }
                USER32.SetLayeredWindowAttributes(overlayHwnd, 0, (byte) -1, LWA_ALPHA);
                DWMAPI.DwmExtendFrameIntoClientArea(overlayHwnd, dwmMargins);
                Pointer emptyRgn = GDI32.CreateRectRgn(0, 0, -1, -1);
                if (emptyRgn != null) {
                    Memory bb = new Memory(24L);
                    bb.clear();
                    bb.setInt(0L, DWM_BB_ENABLE | DWM_BB_BLURREGION);
                    bb.setInt(4L, 1);
                    bb.setPointer(8L, emptyRgn);
                    DWMAPI.DwmEnableBlurBehindWindow(overlayHwnd, bb);
                    GDI32.DeleteObject(emptyRgn);
                }
                dwmAttr.setInt(0L, DWMNCRP_DISABLED);
                DWMAPI.DwmSetWindowAttribute(overlayHwnd, DWMWA_NCRENDERING_POLICY, dwmAttr, 4);
                dwmAttr.setInt(0L, DWMWCP_DONOTROUND);
                DWMAPI.DwmSetWindowAttribute(overlayHwnd, DWMWA_WINDOW_CORNER_PREFERENCE, dwmAttr, 4);
                dwmAttr.setInt(0L, DWMWA_COLOR_NONE);
                DWMAPI.DwmSetWindowAttribute(overlayHwnd, DWMWA_BORDER_COLOR, dwmAttr, 4);
                if (!USER32.SetWindowDisplayAffinity(overlayHwnd, WDA_EXCLUDEFROMCAPTURE)) {
                    USER32.SetWindowDisplayAffinity(overlayHwnd, WDA_MONITOR);
                }
            }

            if (dibBitmap == null || memDc == null || dibBits == null || frameImage == null || dibW != ovW || dibH != ovH) {
                if (memDc == null) {
                    memDc = GDI32.CreateCompatibleDC(null);
                    if (memDc == null) {
                        continue;
                    }
                }
                if (dibBitmap != null) {
                    if (oldBitmap != null) {
                        GDI32.SelectObject(memDc, oldBitmap);
                        oldBitmap = null;
                    }
                    GDI32.DeleteObject(dibBitmap);
                    dibBitmap = null;
                    dibBits = null;
                }
                Memory bmi = new Memory(44L);
                bmi.clear();
                bmi.setInt(0L, 40);
                bmi.setInt(4L, ovW);
                bmi.setInt(8L, -ovH);
                bmi.setShort(12L, (short) 1);
                bmi.setShort(14L, (short) 32);
                bmi.setInt(20L, ovW * ovH * 4);
                PointerByReference ref = new PointerByReference();
                Pointer bmp = GDI32.CreateDIBSection(memDc, bmi, 0, ref, null, 0);
                if (bmp == null || ref.getValue() == null) {
                    if (bmp != null) {
                        GDI32.DeleteObject(bmp);
                    }
                    continue;
                }
                oldBitmap = GDI32.SelectObject(memDc, bmp);
                dibBitmap = bmp;
                dibBits = ref.getValue();
                dibW = ovW;
                dibH = ovH;
                frameImage = new BufferedImage(ovW, ovH, BufferedImage.TYPE_INT_ARGB_PRE);
            }

            WritableRaster raster = frameImage.getRaster();
            DataBuffer db = raster.getDataBuffer();
            int[] pixels = ((DataBufferInt) db).getData();
            Arrays.fill(pixels, 0);

            float scaleX = packet.renderScale() * pxPerGuiX;
            float scaleY = packet.renderScale() * pxPerGuiY;
            float pxScale = Math.max(1f, Math.min(2.5f, scaleY));

            Graphics2D g = frameImage.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            AffineTransform baseTx = new AffineTransform();
            baseTx.translate(packet.centerX() * pxPerGuiX - leftPx, packet.centerY() * pxPerGuiY - topPx);
            baseTx.scale(scaleX, scaleY);
            baseTx.translate(-packet.panelW() * 0.5f, -packet.panelH() * 0.5f);
            g.setTransform(baseTx);

            clipStack.clear();
            renderCommandList(g, packet.ops(), scaleX, scaleY, pxScale, clipStack, rawIconCache, tintedIconCache, glassCardCache, textRunCache);
            g.dispose();

            dibBits.write(0L, pixels, 0, Math.min(pixels.length, dibW * dibH));

            int targetX = clientScreenX + leftPx;
            int targetY = clientScreenY + topPx;
            long nowMs = System.currentTimeMillis();
            boolean posChanged = !overlayVisible || targetX != lastWinX || targetY != lastWinY;
            boolean sizeChanged = !overlayVisible || dibW != lastWinW || dibH != lastWinH;

            if (posChanged || sizeChanged || nowMs - lastTopmostRefreshMs >= 300L) {
                int flags = SWP_NOACTIVATE | SWP_SHOWWINDOW;
                if (!posChanged) flags |= SWP_NOMOVE;
                if (!sizeChanged) flags |= SWP_NOSIZE;
                USER32.SetWindowPos(overlayHwnd, HWND_TOPMOST, targetX, targetY, dibW, dibH, flags);
                if (!overlayVisible) {
                    USER32.ShowWindow(overlayHwnd, SW_SHOWNOACTIVATE);
                    if (!USER32.GetWindowDisplayAffinity(overlayHwnd, affinityOut) || affinityOut.getValue() != WDA_EXCLUDEFROMCAPTURE) {
                        USER32.SetWindowDisplayAffinity(overlayHwnd, WDA_EXCLUDEFROMCAPTURE);
                    }
                    overlayVisible = true;
                }
                lastWinX = targetX;
                lastWinY = targetY;
                lastWinW = dibW;
                lastWinH = dibH;
                lastTopmostRefreshMs = nowMs;
            }

            Pointer winDc = USER32.GetDC(overlayHwnd);
            boolean ok = false;
            if (winDc != null) {
                try {
                    ok = GDI32.BitBlt(winDc, 0, 0, dibW, dibH, memDc, 0, 0, SRCCOPY);
                } finally {
                    USER32.ReleaseDC(overlayHwnd, winDc);
                }
            }
            pumpMessages(overlayHwnd, msgBuf);
            if (!ok) {
                failures++;
                if (failures >= 4) {
                    overlayDisabledAfterFailure = true;
                }
            } else {
                failures = 0;
            }
        }
    }

    private static void pumpMessages(Pointer hwnd, Memory msgBuf) {
        int count = 0;
        while (count < 16 && USER32.PeekMessageW(msgBuf, hwnd, 0, 0, PM_REMOVE)) {
            USER32.TranslateMessage(msgBuf);
            USER32.DispatchMessageW(msgBuf);
            count++;
        }
    }

    private static BufferedImage getTintedIcon(
        Identifier texture,
        int rgb24,
        Map<Identifier, BufferedImage> rawIconCache,
        Map<IconTintKey, BufferedImage> tintedIconCache
    ) {
        IconTintKey key = new IconTintKey(texture, rgb24);
        BufferedImage hit = tintedIconCache.get(key);
        if (hit != null) {
            return hit;
        }
        BufferedImage raw = rawIconCache.get(texture);
        if (raw == null) {
            String path = "/assets/" + texture.getNamespace() + "/" + texture.getPath();
            try (InputStream stream = StreamproofOverlay.class.getResourceAsStream(path)) {
                if (stream == null) {
                    return null;
                }
                raw = ImageIO.read(stream);
                if (raw == null) {
                    return null;
                }
                rawIconCache.put(texture, raw);
            } catch (Exception e) {
                return null;
            }
        }
        BufferedImage out = new BufferedImage(raw.getWidth(), raw.getHeight(), BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D ig = out.createGraphics();
        ig.drawImage(raw, 0, 0, null);
        ig.setComposite(AlphaComposite.SrcIn);
        ig.setColor(new Color((0xFF << 24) | rgb24, true));
        ig.fillRect(0, 0, raw.getWidth(), raw.getHeight());
        ig.dispose();
        tintedIconCache.put(key, out);
        return out;
    }

    private static BufferedImage getGlassCardSprite(
        int wPx,
        int hPx,
        int rPx,
        int topFill,
        int botFill,
        int topBorder,
        int botBorder,
        float borderStrokePx,
        Map<GlassCardKey, BufferedImage> glassCardCache
    ) {
        GlassCardKey key = new GlassCardKey(wPx, hPx, rPx, topFill, botFill, topBorder, botBorder);
        BufferedImage hit = glassCardCache.get(key);
        if (hit != null) {
            return hit;
        }
        BufferedImage img = new BufferedImage(wPx, hPx, BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D cg = img.createGraphics();
        cg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        cg.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        float arc = rPx * 2f;
        RoundRectangle2D.Float fillShape = new RoundRectangle2D.Float(0f, 0f, wPx, hPx, arc, arc);
        if (topFill == botFill) {
            cg.setColor(new Color(topFill, true));
        } else {
            cg.setPaint(new GradientPaint(0f, 0f, new Color(topFill, true), 0f, hPx, new Color(botFill, true)));
        }
        cg.fill(fillShape);
        float halfB = borderStrokePx * 0.5f;
        float bArc = Math.max(0f, arc - borderStrokePx);
        RoundRectangle2D.Float borderShape = new RoundRectangle2D.Float(halfB, halfB, Math.max(0.5f, wPx - borderStrokePx), Math.max(0.5f, hPx - borderStrokePx), bArc, bArc);
        cg.setStroke(new BasicStroke(borderStrokePx));
        if (topBorder == botBorder) {
            cg.setColor(new Color(topBorder, true));
        } else {
            cg.setPaint(new GradientPaint(0f, 0f, new Color(topBorder, true), 0f, hPx, new Color(botBorder, true)));
        }
        cg.draw(borderShape);
        cg.dispose();
        glassCardCache.put(key, img);
        return img;
    }

    private static void renderCommandList(
        Graphics2D g,
        List<DrawOp> ops,
        float scaleX,
        float scaleY,
        float pxScale,
        ArrayList<Shape> clipStack,
        Map<Identifier, BufferedImage> rawIconCache,
        Map<IconTintKey, BufferedImage> tintedIconCache,
        Map<GlassCardKey, BufferedImage> glassCardCache,
        Map<TextRunKey, CachedTextSprite> textRunCache
    ) {
        BufferedImage atlas = fontAtlasImage();
        for (DrawOp rawOp : ops) {
            if (rawOp instanceof DrawOp.PushClip op) {
                clipStack.add(g.getClip());
                g.clipRect(op.x0(), op.y0(), Math.max(0, op.x1() - op.x0()), Math.max(0, op.y1() - op.y0()));
            } else if (rawOp instanceof DrawOp.PopClip) {
                g.setClip(clipStack.isEmpty() ? null : clipStack.remove(clipStack.size() - 1));
            } else if (rawOp instanceof DrawOp.GlassCard op) {
                int wPx = Math.max(4, Math.round(op.w() * scaleX));
                int hPx = Math.max(4, Math.round(op.h() * scaleY));
                int rPx = Math.round(clamp(op.radius(), 0f, Math.min(op.w(), op.h()) * 0.5f) * scaleY);
                BufferedImage sprite = getGlassCardSprite(wPx, hPx, rPx, op.topFill(), op.bottomFill(), op.topBorder(), op.bottomBorder(), Math.max(1f, scaleY), glassCardCache);
                AffineTransform prev = g.getTransform();
                g.translate(op.x(), op.y());
                g.scale(op.w() / wPx, op.h() / hPx);
                g.drawImage(sprite, 0, 0, null);
                g.setTransform(prev);
            } else if (rawOp instanceof DrawOp.RoundedShadow op) {
                int baseA = (op.shadowColor() >>> 24) & 0xFF;
                if (baseA > 3) {
                    int rgb = op.shadowColor() & 0x00FFFFFF;
                    float ex1 = op.spread() * 0.55f;
                    float r1 = (op.radius() + ex1 * 0.7f) * 2f;
                    int a1 = clampInt(Math.round(baseA * 0.42f), 1, 255);
                    g.setColor(new Color((a1 << 24) | rgb, true));
                    g.fill(new RoundRectangle2D.Float(op.x() - ex1, op.y() - ex1, op.w() + ex1 * 2f, op.h() + ex1 * 2f, r1, r1));
                }
            } else if (rawOp instanceof DrawOp.RoundedGrad op) {
                float r = clamp(op.radius(), 0f, Math.min(op.w(), op.h()) * 0.5f) * 2f;
                int col = op.topColor() == op.bottomColor() ? op.topColor() : UiRenderPrimitives.lerpColor(op.topColor(), op.bottomColor(), 0.5f);
                g.setColor(new Color(col, true));
                g.fill(new RoundRectangle2D.Float(op.x(), op.y(), op.w(), op.h(), r, r));
            } else if (rawOp instanceof DrawOp.RoundedBorder op) {
                float halfT = op.thickness() * 0.5f;
                float r = Math.max(0f, op.radius() - halfT) * 2f;
                java.awt.Stroke prevStroke = g.getStroke();
                g.setStroke(new BasicStroke(op.thickness()));
                g.setColor(new Color(UiRenderPrimitives.lerpColor(op.topColor(), op.bottomColor(), 0.5f), true));
                g.draw(new RoundRectangle2D.Float(op.x() + halfT, op.y() + halfT, Math.max(0.2f, op.w() - op.thickness()), Math.max(0.2f, op.h() - op.thickness()), r, r));
                g.setStroke(prevStroke);
            } else if (rawOp instanceof DrawOp.Icon op) {
                float alpha = ((op.color() >>> 24) & 0xFF) / 255f;
                if (alpha > 0.01f) {
                    BufferedImage tinted = getTintedIcon(op.texture(), op.color() & 0x00FFFFFF, rawIconCache, tintedIconCache);
                    if (tinted != null) {
                        var oldComp = g.getComposite();
                        if (alpha < 0.995f) {
                            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, clamp(alpha, 0f, 1f)));
                        }
                        AffineTransform prevTx = g.getTransform();
                        g.translate(op.x(), op.y());
                        g.scale(op.size() / tinted.getWidth(), op.size() / tinted.getHeight());
                        g.drawImage(tinted, 0, 0, null);
                        g.setTransform(prevTx);
                        g.setComposite(oldComp);
                    }
                }
            } else if (rawOp instanceof DrawOp.Avatar op) {
                float alpha = ((op.tintColor() >>> 24) & 0xFF) / 255f;
                if (alpha > 0.01f) {
                    float d = op.radius() * 2f;
                    Ellipse2D.Float circle = new Ellipse2D.Float(op.cx() - op.radius(), op.cy() - op.radius(), d, d);
                    Shape oldClip = g.getClip();
                    var oldComp = g.getComposite();
                    g.clip(circle);
                    if (alpha < 0.995f) {
                        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, clamp(alpha, 0f, 1f)));
                    }
                    BufferedImage skinImg = sharedSkinCache.get(op.skinId());
                    if (skinImg != null && skinImg.getWidth() >= 64 && skinImg.getHeight() >= 16) {
                        AffineTransform prevTx = g.getTransform();
                        g.translate(op.cx() - op.radius(), op.cy() - op.radius());
                        g.scale(d / 8f, d / 8f);
                        g.drawImage(skinImg, 0, 0, 8, 8, 8, 8, 16, 16, null);
                        g.drawImage(skinImg, 0, 0, 8, 8, 40, 8, 48, 16, null);
                        g.setTransform(prevTx);
                    } else {
                        g.setColor(new Color(0xFF3A5570, true));
                        g.fill(circle);
                    }
                    g.setComposite(oldComp);
                    g.setClip(oldClip);
                }
            } else if (rawOp instanceof DrawOp.BlockIcon op) {
                if (op.alpha() > 0.01f) {
                    BufferedImage iconImg = blockIsoIconCache.get(op.blockId());
                    if (iconImg != null) {
                        var oldComp = g.getComposite();
                        if (op.alpha() < 0.995f) {
                            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, clamp(op.alpha(), 0f, 1f)));
                        }
                        AffineTransform prevTx = g.getTransform();
                        g.translate(op.x(), op.y());
                        g.scale(op.size() / iconImg.getWidth(), op.size() / iconImg.getHeight());
                        g.drawImage(iconImg, 0, 0, null);
                        g.setTransform(prevTx);
                        g.setComposite(oldComp);
                    }
                }
            } else if (rawOp instanceof DrawOp.TextRun op) {
                if (atlas != null && !op.glyphs().isEmpty()) {
                    TextGradient grad = op.gradient();
                    int a1 = Math.max((grad.topLeft() >>> 24) & 0xFF, (grad.topRight() >>> 24) & 0xFF);
                    int a2 = Math.max((grad.bottomLeft() >>> 24) & 0xFF, (grad.bottomRight() >>> 24) & 0xFF);
                    float maxAlpha = Math.max(a1, a2) / 255f;
                    if (maxAlpha > 0.01f) {
                        int quantized = Math.max(8, Math.round(op.sizePx() * pxScale * 3f));
                        TextRunKey key = new TextRunKey(op.text(), op.weight(), quantized, grad.topLeft() & 0x00FFFFFF, grad.bottomRight() & 0x00FFFFFF);
                        CachedTextSprite sprite = textRunCache.get(key);
                        if (sprite == null) {
                            sprite = rasterizeTextSprite(atlas, op.x(), op.y(), pxScale, grad, op.glyphs());
                            textRunCache.put(key, sprite);
                        }
                        var oldComp = g.getComposite();
                        if (maxAlpha < 0.995f) {
                            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, clamp(maxAlpha, 0f, 1f)));
                        }
                        AffineTransform prevTx = g.getTransform();
                        g.translate(op.x() + sprite.drawOffsetX(), op.y() + sprite.drawOffsetY());
                        g.scale(sprite.logicalW() / sprite.image().getWidth(), sprite.logicalH() / sprite.image().getHeight());
                        g.drawImage(sprite.image(), 0, 0, null);
                        g.setTransform(prevTx);
                        g.setComposite(oldComp);
                    }
                }
            }
        }
    }

    private static CachedTextSprite rasterizeTextSprite(
        BufferedImage atlas,
        float originX,
        float originY,
        float pxScale,
        TextGradient gradient,
        List<SatoshiFont.PlacedGlyph> glyphs
    ) {
        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        for (SatoshiFont.PlacedGlyph gl : glyphs) {
            if (gl.x0() < minX) minX = gl.x0();
            if (gl.y0() < minY) minY = gl.y0();
            if (gl.x1() > maxX) maxX = gl.x1();
            if (gl.y1() > maxY) maxY = gl.y1();
        }
        float logW = Math.max(1f, maxX - minX);
        float logH = Math.max(1f, maxY - minY);
        int imgW = clampInt((int) Math.ceil(logW * pxScale), 2, 2048);
        int imgH = clampInt((int) Math.ceil(logH * pxScale), 2, 512);
        BufferedImage out = new BufferedImage(imgW, imgH, BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D sg = out.createGraphics();
        sg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        float aw = atlas.getWidth();
        float ah = atlas.getHeight();
        float sxScale = imgW / logW;
        float syScale = imgH / logH;
        for (SatoshiFont.PlacedGlyph gl : glyphs) {
            int dx0 = Math.round((gl.x0() - minX) * sxScale);
            int dy0 = Math.round((gl.y0() - minY) * syScale);
            int dx1 = Math.round((gl.x1() - minX) * sxScale);
            int dy1 = Math.round((gl.y1() - minY) * syScale);
            int su0 = clampInt(Math.round(gl.u0() * aw), 0, atlas.getWidth());
            int sv0 = clampInt(Math.round(gl.v0() * ah), 0, atlas.getHeight());
            int su1 = clampInt(Math.round(gl.u1() * aw), 0, atlas.getWidth());
            int sv1 = clampInt(Math.round(gl.v1() * ah), 0, atlas.getHeight());
            if (dx1 > dx0 && dy1 > dy0 && su1 > su0 && sv1 > sv0) {
                sg.drawImage(atlas, dx0, dy0, dx1, dy1, su0, sv0, su1, sv1, null);
            }
        }
        sg.setComposite(AlphaComposite.SrcIn);
        Color c0 = new Color((0xFF << 24) | (gradient.topLeft() & 0x00FFFFFF), true);
        Color c1 = new Color((0xFF << 24) | (gradient.bottomRight() & 0x00FFFFFF), true);
        sg.setPaint(new GradientPaint(0f, 0f, c0, imgW, imgH, c1));
        sg.fill(new Rectangle2D.Float(0f, 0f, imgW, imgH));
        sg.dispose();
        return new CachedTextSprite(out, minX - originX, minY - originY, logW, logH);
    }

    private static float clamp(float val, float min, float max) {
        return Math.max(min, Math.min(max, val));
    }

    private static int clampInt(int val, int min, int max) {
        return Math.max(min, Math.min(max, val));
    }
}
