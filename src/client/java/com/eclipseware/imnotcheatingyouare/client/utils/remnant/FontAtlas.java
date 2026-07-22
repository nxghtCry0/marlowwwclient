package com.eclipseware.imnotcheatingyouare.client.utils.remnant;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.resources.Identifier;
import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

public class FontAtlas {
    private final String name;
    private int width;
    private int height;
    private FontMetrics fontMetrics;
    private final Glyph[] glyphs = new Glyph[2048 * 2048];
    private DynamicTexture tex;
    private Identifier textureId;
    private boolean registered = false;

    public FontAtlas(final ResourceManager manager, final String name) throws IOException {
        this.name = name;
        try {
            Identifier jsonId = Identifier.parse("imnotcheatingyouare:fonts/" + name + ".json");
            System.out.println("[Marlow] Loading font atlas JSON: " + jsonId);
            var resourceJson = manager.getResource(jsonId);
            if (resourceJson.isPresent()) {
                try (Reader reader = new InputStreamReader(resourceJson.get().open())) {
                    final JsonObject atlasJson = JsonParser.parseReader(reader).getAsJsonObject();
                    this.width = atlasJson.getAsJsonObject("atlas").get("width").getAsInt();
                    this.height = atlasJson.getAsJsonObject("atlas").get("height").getAsInt();
                    this.fontMetrics = FontMetrics.parse(atlasJson.getAsJsonObject("metrics"));

                    for (final JsonElement glyphElement : atlasJson.getAsJsonArray("glyphs")) {
                        final JsonObject glyphObject = glyphElement.getAsJsonObject();
                        final Glyph glyph = Glyph.parse(glyphObject);
                        this.glyphs[glyph.getUnicode()] = glyph;
                    }
                }
            }

            Identifier pngId = Identifier.parse("imnotcheatingyouare:fonts/" + name + ".png");
            var resourcePng = manager.getResource(pngId);
            if (resourcePng.isPresent()) {
                this.tex = new DynamicTexture(() -> "font_atlas_" + name, NativeImage.read(resourcePng.get().open()));
                this.textureId = Identifier.parse("imnotcheatingyouare:dynamic_fonts/" + name);
                try {
                    var textureManager = Minecraft.getInstance().getTextureManager();
                    if (textureManager != null) {
                        textureManager.register(this.textureId, this.tex);
                        System.out.println("[Marlow] Successfully loaded and registered dynamic font atlas: " + name + " (" + width + "x" + height + ")");
                        this.registered = true;
                    } else {
                        System.out.println("[Marlow] TextureManager not available yet for " + name + ", registration deferred.");
                    }
                } catch (Throwable ignore) {
                    System.out.println("[Marlow] TextureManager access failed for " + name + ", registration deferred.");
                }
            }
        } catch (Throwable t) {
            System.out.println("[Marlow] ERROR loading font atlas " + name + ":");
            t.printStackTrace();
        }
    }

    public float getWidth(String text) {
        if (text == null) return 0;
        if (name.equals("lucide") || name.equals("icons")) {
            if (width <= 0 || height <= 0) return 0;
            float widthSum = 0;
            for (int i = 0; i < text.length(); i++) {
                int unicode = text.codePointAt(i);
                Glyph glyph = this.glyphs[unicode];
                if (glyph != null) {
                    widthSum += glyph.getAdvance();
                }
            }
            return widthSum * 9.0f;
        }
        net.minecraft.network.chat.Component component = com.eclipseware.imnotcheatingyouare.client.utils.FontUtils.get(text);
        return Minecraft.getInstance().font.width(component);
    }

    public float getWidth(String text, float size) {
        if (text == null) return 0;
        if (name.equals("lucide") || name.equals("icons")) {
            if (width <= 0 || height <= 0) return 0;
            float widthSum = 0;
            for (int i = 0; i < text.length(); i++) {
                int unicode = text.codePointAt(i);
                Glyph glyph = this.glyphs[unicode];
                if (glyph != null) {
                    widthSum += glyph.getAdvance();
                }
            }
            return widthSum * size;
        }
        net.minecraft.network.chat.Component component = com.eclipseware.imnotcheatingyouare.client.utils.FontUtils.get(text);
        return Minecraft.getInstance().font.width(component) * (size / 9.0f);
    }

    public float getLineHeight() {
        if (name.equals("lucide") || name.equals("icons")) {
            return (fontMetrics != null ? fontMetrics.getLineHeight() : 1.0f) * 9.0f;
        }
        return Minecraft.getInstance().font.lineHeight;
    }

    public float getLineHeight(float size) {
        if (name.equals("lucide") || name.equals("icons")) {
            return (fontMetrics != null ? fontMetrics.getLineHeight() : 1.0f) * size;
        }
        return Minecraft.getInstance().font.lineHeight * (size / 9.0f);
    }

    public void render(org.joml.Matrix3x2fStack matrices, String text, float x, float y, float size, int color) {
        GuiGraphicsExtractor context = Render2DEngine.activeContext;
        if (context == null || text == null) return;

        if (name.equals("lucide") || name.equals("icons")) {
            if (width <= 0 || height <= 0 || tex == null) {
                return;
            }
            if (!registered) {
                try {
                    var textureManager = Minecraft.getInstance().getTextureManager();
                    if (textureManager != null && textureId != null) {
                        textureManager.register(this.textureId, this.tex);
                        registered = true;
                    }
                } catch (Throwable ignore) {}
            }

            float currentX = x;
            
            for (int i = 0; i < text.length(); i++) {
                int unicode = text.codePointAt(i);
                Glyph glyph = this.glyphs[unicode];
                if (glyph == null) continue;
                
                if (glyph.getPlaneRight() - glyph.getPlaneLeft() != 0) {
                    float x0 = currentX + glyph.getPlaneLeft() * size;
                    float x1 = currentX + glyph.getPlaneRight() * size;
                    float y0 = y + fontMetrics.getAscender() * size - glyph.getPlaneTop() * size;
                    float y1 = y + fontMetrics.getAscender() * size - glyph.getPlaneBottom() * size;
                    
                    float u0 = glyph.getAtlasLeft();
                    float v0 = height - glyph.getAtlasTop();
                    int w = (int)Math.ceil(x1 - x0);
                    int h = (int)Math.ceil(y1 - y0);
                    int rw = (int)(glyph.getAtlasRight() - glyph.getAtlasLeft());
                    int rh = (int)(glyph.getAtlasTop() - glyph.getAtlasBottom());
                    
                    context.blit(
                        net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,
                        textureId,
                        (int)x0,
                        (int)y0,
                        u0,
                        v0,
                        w,
                        h,
                        rw,
                        rh,
                        width,
                        height,
                        color
                    );
                }
                currentX += size * glyph.getAdvance();
            }
        } else {
            net.minecraft.network.chat.Component component = com.eclipseware.imnotcheatingyouare.client.utils.FontUtils.get(text);
            if (size != 9.0f && size > 0) {
                float scale = size / 9.0f;
                matrices.pushMatrix();
                matrices.translate(x, y);
                matrices.scale(scale, scale);
                context.text(Minecraft.getInstance().font, component, 0, 0, color, false);
                matrices.popMatrix();
            } else {
                context.text(Minecraft.getInstance().font, component, (int)x, (int)y, color, false);
            }
        }
    }

    public void render(org.joml.Matrix3x2fStack matrices, String text, float x, float y, int color) {
        this.render(matrices, text, x, y, 9.0f, color);
    }

    public void renderRightString(org.joml.Matrix3x2fStack matrices, String text, float x, float y, float size, int color) {
        this.render(matrices, text, x - getWidth(text, size), y, size, color);
    }

    public void renderRightString(org.joml.Matrix3x2fStack matrices, String text, float x, float y, int color) {
        this.render(matrices, text, x - getWidth(text), y, 9.0f, color);
    }

    public void renderCenteredString(org.joml.Matrix3x2fStack matrices, String text, float x, float y, int color) {
        this.render(matrices, text, x - getWidth(text) / 2.0f, y, 9.0f, color);
    }

    public void renderCenteredString(org.joml.Matrix3x2fStack matrices, String text, float x, float y, float size, int color) {
        this.render(matrices, text, x - getWidth(text, size) / 2.0f, y, size, color);
    }

    public void renderHorizontalGradient(org.joml.Matrix3x2fStack matrices, String text, float x, float y, float size, java.awt.Color primaryColor, java.awt.Color secondaryColor, int speed) {
        this.render(matrices, text, x, y, size, primaryColor.getRGB());
    }

    public void renderDiagonalGradient(org.joml.Matrix3x2fStack matrices, String text, float x, float y, float size, java.awt.Color primaryColor, java.awt.Color secondaryColor, int speed, float verticalStrength) {
        this.render(matrices, text, x, y, size, primaryColor.getRGB());
    }

    public void renderWithShadow(org.joml.Matrix3x2fStack matrices, String text, float x, float y, int color) {
        this.renderWithShadow(matrices, text, x, y, 9.0f, color);
    }

    public void renderWithShadow(org.joml.Matrix3x2fStack matrices, String text, float x, float y, float size, int color) {
        GuiGraphicsExtractor context = Render2DEngine.activeContext;
        if (context == null || text == null) return;
        net.minecraft.network.chat.Component component = com.eclipseware.imnotcheatingyouare.client.utils.FontUtils.get(text);
        if (size != 9.0f && size > 0) {
            float scale = size / 9.0f;
            matrices.pushMatrix();
            matrices.translate(x, y);
            matrices.scale(scale, scale);
            context.text(Minecraft.getInstance().font, component, 0, 0, color, true);
            matrices.popMatrix();
        } else {
            context.text(Minecraft.getInstance().font, component, (int)x, (int)y, color, true);
        }
    }
}
