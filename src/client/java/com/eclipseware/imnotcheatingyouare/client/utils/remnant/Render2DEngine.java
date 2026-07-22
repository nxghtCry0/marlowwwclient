package com.eclipseware.imnotcheatingyouare.client.utils.remnant;

import net.minecraft.client.gui.GuiGraphicsExtractor;

public class Render2DEngine {
    public static GuiGraphicsExtractor activeContext;

    private static final net.minecraft.resources.Identifier DYNAMIC_TEXTURE_ID = net.minecraft.resources.Identifier.parse("imnotcheatingyouare:rounded_corner_atlas");
    private static boolean textureRegistered = false;

    private static class UVCoords {
        float u, v;
        float uw, vh;
        UVCoords(float u, float v, float uw, float vh) {
            this.u = u;
            this.v = v;
            this.uw = uw;
            this.vh = vh;
        }
    }

    private static UVCoords getUV(int r, boolean hollow, int quadrant) {
        int cx, cy;
        if (r == 3) {
            cx = hollow ? 24 : 8;
            cy = 8;
        } else if (r == 4) {
            cx = hollow ? 56 : 40;
            cy = 8;
        } else if (r == 5) {
            cx = hollow ? 24 : 8;
            cy = 32;
        } else {
            cx = hollow ? 56 : 40;
            cy = 32;
            r = 6;
        }

        float uStart = cx - r;
        float vStart = cy - r;
        
        if (quadrant == 0) {
            return new UVCoords(uStart, vStart, r, r);
        } else if (quadrant == 1) {
            return new UVCoords(cx, vStart, r, r);
        } else if (quadrant == 2) {
            return new UVCoords(uStart, cy, r, r);
        } else {
            return new UVCoords(cx, cy, r, r);
        }
    }

    private static void drawCircle(com.mojang.blaze3d.platform.NativeImage image, int cx, int cy, int r, boolean hollow) {
        for (int y = cy - r; y < cy + r; y++) {
            for (int x = cx - r; x < cx + r; x++) {
                if (x < 0 || x >= 128 || y < 0 || y >= 64) continue;
                double dx = x + 0.5 - cx;
                double dy = y + 0.5 - cy;
                double d = Math.sqrt(dx * dx + dy * dy);
                double alpha;
                if (hollow) {
                    double dist = Math.abs(d - (r - 0.5));
                    alpha = (dist <= 0.55) ? 1.0 : 0.0;
                } else {
                    alpha = (d <= r) ? 1.0 : 0.0;
                }
                int a = (int)(alpha * 255);
                image.setPixelABGR(x, y, (a << 24) | 0x00FFFFFF);
            }
        }
    }

    public static void checkRegisterTexture() {
        if (textureRegistered) return;
        try {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.getTextureManager() == null) return;

            com.mojang.blaze3d.platform.NativeImage image = new com.mojang.blaze3d.platform.NativeImage(128, 64, true);
            
            drawCircle(image, 8, 8, 3, false);
            drawCircle(image, 24, 8, 3, true);
            drawCircle(image, 40, 8, 4, false);
            drawCircle(image, 56, 8, 4, true);
            drawCircle(image, 8, 32, 5, false);
            drawCircle(image, 24, 32, 5, true);
            drawCircle(image, 40, 32, 6, false);
            drawCircle(image, 56, 32, 6, true);

            net.minecraft.client.renderer.texture.DynamicTexture dynamicTexture = new net.minecraft.client.renderer.texture.DynamicTexture(() -> "marlow_rounded_corners", image);
            mc.getTextureManager().register(DYNAMIC_TEXTURE_ID, dynamicTexture);
            textureRegistered = true;
            System.out.println("[Marlow] AA ROUNDED CORNERS ATLAS REGISTERED SUCCESSFULLY!");
        } catch (Exception e) {
            System.out.println("[Marlow] Failed to register dynamic AA corner texture: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void drawRoundedRect(org.joml.Matrix3x2fStack matrices, float x, float y, float width, float height, float radius, java.awt.Color color) {
        if (activeContext == null) return;
        checkRegisterTexture();

        int ix = (int)x;
        int iy = (int)y;
        int iw = (int)width;
        int ih = (int)height;
        int ir = (int)radius;
        int c = color.getRGB();

        if (ir <= 0) {
            activeContext.fill(ix, iy, ix + iw, iy + ih, c);
            return;
        }

        ir = Math.min(ir, Math.min(iw / 2, ih / 2));

        UVCoords tl = getUV(ir, false, 0);
        UVCoords tr = getUV(ir, false, 1);
        UVCoords bl = getUV(ir, false, 2);
        UVCoords br = getUV(ir, false, 3);

        activeContext.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, DYNAMIC_TEXTURE_ID, ix, iy, tl.u, tl.v, ir, ir, (int)tl.uw, (int)tl.vh, 128, 64, c);
        activeContext.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, DYNAMIC_TEXTURE_ID, ix + iw - ir, iy, tr.u, tr.v, ir, ir, (int)tr.uw, (int)tr.vh, 128, 64, c);
        activeContext.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, DYNAMIC_TEXTURE_ID, ix, iy + ih - ir, bl.u, bl.v, ir, ir, (int)bl.uw, (int)bl.vh, 128, 64, c);
        activeContext.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, DYNAMIC_TEXTURE_ID, ix + iw - ir, iy + ih - ir, br.u, br.v, ir, ir, (int)br.uw, (int)br.vh, 128, 64, c);

        activeContext.fill(ix + ir, iy, ix + iw - ir, iy + ih, c);
        activeContext.fill(ix, iy + ir, ix + ir, iy + ih - ir, c);
        activeContext.fill(ix + iw - ir, iy + ir, ix + iw, iy + ih - ir, c);
    }

    public static void drawRoundedOutline(org.joml.Matrix3x2fStack matrices, float x, float y, float width, float height, float radius, float thickness, java.awt.Color color) {
        if (activeContext == null) return;
        checkRegisterTexture();

        int ix = (int)x;
        int iy = (int)y;
        int iw = (int)width;
        int ih = (int)height;
        int ir = (int)radius;
        int t = (int)Math.max(1, thickness);
        int c = color.getRGB();

        ir = Math.min(ir, Math.min(iw / 2, ih / 2));

        UVCoords tl = getUV(ir, true, 0);
        UVCoords tr = getUV(ir, true, 1);
        UVCoords bl = getUV(ir, true, 2);
        UVCoords br = getUV(ir, true, 3);

        activeContext.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, DYNAMIC_TEXTURE_ID, ix, iy, tl.u, tl.v, ir, ir, (int)tl.uw, (int)tl.vh, 128, 64, c);
        activeContext.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, DYNAMIC_TEXTURE_ID, ix + iw - ir, iy, tr.u, tr.v, ir, ir, (int)tr.uw, (int)tr.vh, 128, 64, c);
        activeContext.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, DYNAMIC_TEXTURE_ID, ix, iy + ih - ir, bl.u, bl.v, ir, ir, (int)bl.uw, (int)bl.vh, 128, 64, c);
        activeContext.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, DYNAMIC_TEXTURE_ID, ix + iw - ir, iy + ih - ir, br.u, br.v, ir, ir, (int)br.uw, (int)br.vh, 128, 64, c);

        activeContext.fill(ix + ir, iy, ix + iw - ir, iy + t, c);
        activeContext.fill(ix + ir, iy + ih - t, ix + iw - ir, iy + ih, c);
        activeContext.fill(ix, iy + ir, ix + t, iy + ih - ir, c);
        activeContext.fill(ix + iw - t, iy + ir, ix + iw, iy + ih - ir, c);
    }

    public static void drawRect(org.joml.Matrix3x2fStack matrices, float x, float y, float width, float height, java.awt.Color color) {
        if (activeContext != null) {
            activeContext.fill((int)x, (int)y, (int)(x + width), (int)(y + height), color.getRGB());
        }
    }

    public static void drawLine(org.joml.Matrix3x2fStack matrices, float x, float y, float x1, float y1, float width, java.awt.Color color) {
        if (activeContext != null) {
            activeContext.fill((int)x, (int)y, (int)x1, (int)y1, color.getRGB());
        }
    }

    public static void drawRoundedGradient(org.joml.Matrix3x2fStack matrices, float x, float y, float width, float height, float radius, java.awt.Color startColor, java.awt.Color endColor) {
        if (activeContext != null) {
            activeContext.fill((int)x, (int)y, (int)(x + width), (int)(y + height), startColor.getRGB());
        }
    }
}
