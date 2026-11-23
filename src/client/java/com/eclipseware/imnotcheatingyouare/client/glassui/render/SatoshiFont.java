package com.eclipseware.imnotcheatingyouare.client.glassui.render;

import com.eclipseware.imnotcheatingyouare.client.glassui.asset.UiAssets;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;

public final class SatoshiFont {
    private static final class GlyphInfo {
        final float advance;
        final boolean empty;
        final float xOff;
        final float yOff;
        final float w;
        final float h;
        final float u0;
        final float v0;
        final float u1;
        final float v1;

        GlyphInfo(float advance, boolean empty, float xOff, float yOff, float w, float h, float u0, float v0, float u1, float v1) {
            this.advance = advance;
            this.empty = empty;
            this.xOff = xOff;
            this.yOff = yOff;
            this.w = w;
            this.h = h;
            this.u0 = u0;
            this.v0 = v0;
            this.u1 = u1;
            this.v1 = v1;
        }
    }

    private record WeightData(float lineHeight, Map<Integer, GlyphInfo> glyphs, Map<Long, Float> kerning) {
    }

    public record PlacedGlyph(
        float x0,
        float y0,
        float x1,
        float y1,
        float u0,
        float v0,
        float u1,
        float v1,
        int cTopLeft,
        int cBotLeft,
        int cBotRight,
        int cTopRight
    ) {
    }

    private static volatile Map<SatoshiWeight, WeightData> cachedWeights;

    private SatoshiFont() {
    }

    private static Map<SatoshiWeight, WeightData> weights() {
        Map<SatoshiWeight, WeightData> local = cachedWeights;
        if (local != null) {
            return local;
        }
        synchronized (SatoshiFont.class) {
            if (cachedWeights == null) {
                cachedWeights = loadMetrics();
            }
            return cachedWeights;
        }
    }

    private static Map<SatoshiWeight, WeightData> loadMetrics() {
        Map<SatoshiWeight, WeightData> out = new LinkedHashMap<>();
        String resourcePath = UiAssets.fontMetricsClasspath();
        InputStream stream = SatoshiFont.class.getResourceAsStream(resourcePath);
        if (stream == null) {
            return out;
        }
        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            JsonElement parsed = JsonParser.parseReader(reader);
            JsonObject root = parsed.getAsJsonObject();
            JsonObject weightsObj = root.getAsJsonObject("weights");
            for (SatoshiWeight weight : SatoshiWeight.values()) {
                JsonObject wObj = weightsObj.getAsJsonObject(weight.key());
                if (wObj == null) {
                    continue;
                }
                JsonElement lhElem = wObj.get("lineHeight");
                float lineHeight = lhElem.getAsFloat();
                Map<Integer, GlyphInfo> glyphMap = new HashMap<>(128);
                JsonObject gObj = wObj.getAsJsonObject("glyphs");
                for (Map.Entry<String, JsonElement> entry : gObj.entrySet()) {
                    Integer code = parseIntSafe(entry.getKey());
                    if (code == null) {
                        continue;
                    }
                    JsonObject obj = entry.getValue().getAsJsonObject();
                    float adv = obj.get("adv").getAsFloat();
                    JsonElement emptyElem = obj.get("empty");
                    boolean isEmpty = emptyElem != null && emptyElem.getAsBoolean();
                    if (isEmpty) {
                        glyphMap.put(code, new GlyphInfo(adv, true, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f));
                    } else {
                        glyphMap.put(code, new GlyphInfo(
                            adv,
                            false,
                            obj.get("x").getAsFloat(),
                            obj.get("y").getAsFloat(),
                            obj.get("w").getAsFloat(),
                            obj.get("h").getAsFloat(),
                            obj.get("u0").getAsFloat(),
                            obj.get("v0").getAsFloat(),
                            obj.get("u1").getAsFloat(),
                            obj.get("v1").getAsFloat()
                        ));
                    }
                }
                Map<Long, Float> kernMap = new HashMap<>(512);
                JsonObject kObj = wObj.getAsJsonObject("kerning");
                if (kObj != null) {
                    for (Map.Entry<String, JsonElement> entry : kObj.entrySet()) {
                        String pairKey = entry.getKey();
                        int sep = pairKey.indexOf(':');
                        if (sep <= 0) {
                            continue;
                        }
                        Integer left = parseIntSafe(pairKey.substring(0, sep));
                        Integer right = parseIntSafe(pairKey.substring(sep + 1));
                        if (left != null && right != null) {
                            kernMap.put(packPair(left, right), entry.getValue().getAsFloat());
                        }
                    }
                }
                out.put(weight, new WeightData(lineHeight, glyphMap, kernMap));
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    private static Integer parseIntSafe(String raw) {
        try {
            return Integer.valueOf(raw);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static long packPair(int a, int b) {
        return (((long) a) << 32) | (((long) b) & 0xFFFFFFFFL);
    }

    public static float width(String text, float sizePx, SatoshiWeight weight) {
        if (text == null || text.isEmpty() || sizePx <= 0f) {
            return 0f;
        }
        WeightData data = weights().get(weight);
        if (data == null) {
            return text.length() * sizePx * 0.55f;
        }
        GlyphInfo fallback = data.glyphs().getOrDefault((int) '?', new GlyphInfo(0.5f, true, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f));
        float advSum = 0f;
        int prevCode = -1;
        Map<Integer, GlyphInfo> glyphs = data.glyphs();
        Map<Long, Float> kerning = data.kerning();
        for (int i = 0; i < text.length(); i++) {
            int code = text.charAt(i);
            if (prevCode >= 0) {
                Float kern = kerning.get(packPair(prevCode, code));
                if (kern != null) {
                    advSum += kern;
                }
            }
            GlyphInfo g = glyphs.getOrDefault(code, fallback);
            advSum += g.advance;
            prevCode = code;
        }
        return advSum * sizePx;
    }

    public static float width(String text, float sizePx) {
        return width(text, sizePx, SatoshiWeight.MEDIUM);
    }

    public static String trimTailToWidth(String text, float maxWidthPx, float sizePx, SatoshiWeight weight) {
        if (text == null || width(text, sizePx, weight) <= maxWidthPx) {
            return text != null ? text : "";
        }
        int start = text.length();
        while (start > 0 && width(text.substring(start - 1), sizePx, weight) <= maxWidthPx) {
            start--;
        }
        return text.substring(start);
    }

    public static List<String> wrapLines(String text, float maxWidthPx, float sizePx, SatoshiWeight weight) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        String trimmed = text.trim();
        String[] words = trimmed.split("\\s+");
        List<String> lines = new ArrayList<>(3);
        String current = "";
        for (String word : words) {
            String candidate = current.isEmpty() ? word : current + " " + word;
            if (current.isEmpty() || width(candidate, sizePx, weight) <= maxWidthPx) {
                current = candidate;
            } else {
                lines.add(current);
                current = word;
            }
        }
        if (!current.isEmpty()) {
            lines.add(current);
        }
        return lines;
    }

    public static void draw(
        GuiGraphicsExtractor graphics,
        String text,
        float x,
        float y,
        float sizePx,
        TextGradient gradient,
        SatoshiWeight weight
    ) {
        if (text == null || text.isEmpty() || sizePx <= 0.5f) {
            return;
        }
        int maxA1 = Math.max((gradient.topLeft() >>> 24) & 0xFF, (gradient.topRight() >>> 24) & 0xFF);
        int maxA2 = Math.max((gradient.bottomLeft() >>> 24) & 0xFF, (gradient.bottomRight() >>> 24) & 0xFF);
        if (Math.max(maxA1, maxA2) < 3) {
            return;
        }

        WeightData data = weights().get(weight);
        if (data == null) {
            return;
        }
        Map<Integer, GlyphInfo> glyphMap = data.glyphs();
        Map<Long, Float> kerning = data.kerning();
        GlyphInfo fallback = glyphMap.get((int) '?');
        if (fallback == null) {
            return;
        }

        float totalW = Math.max(1f, width(text, sizePx, weight));
        List<PlacedGlyph> quads = new ArrayList<>(text.length());
        float penX = x;
        float minX = x;
        float minY = y;
        float maxX = x + totalW;
        float maxY = y + sizePx;
        int prevCode = -1;

        for (int i = 0; i < text.length(); i++) {
            int code = text.charAt(i);
            if (prevCode >= 0) {
                Float kern = kerning.get(packPair(prevCode, code));
                if (kern != null) {
                    penX += kern * sizePx;
                }
            }
            GlyphInfo g = glyphMap.getOrDefault(code, fallback);
            if (!g.empty) {
                float gx0 = penX + g.xOff * sizePx;
                float gy0 = y + g.yOff * sizePx;
                float gx1 = gx0 + g.w * sizePx;
                float gy1 = gy0 + g.h * sizePx;
                if (gx0 < minX) minX = gx0;
                if (gy0 < minY) minY = gy0;
                if (gx1 > maxX) maxX = gx1;
                if (gy1 > maxY) maxY = gy1;

                float hx0 = clamp((gx0 - x) / totalW, 0f, 1f);
                float hx1 = clamp((gx1 - x) / totalW, 0f, 1f);
                float vy0 = clamp((gy0 - y) / sizePx, 0f, 1f);
                float vy1 = clamp((gy1 - y) / sizePx, 0f, 1f);

                quads.add(new PlacedGlyph(
                    gx0, gy0, gx1, gy1,
                    g.u0, g.v0, g.u1, g.v1,
                    gradient.sample(hx0, vy0),
                    gradient.sample(hx0, vy1),
                    gradient.sample(hx1, vy1),
                    gradient.sample(hx1, vy0)
                ));
            }
            penX += g.advance * sizePx;
            prevCode = code;
        }

        if (quads.isEmpty()) {
            return;
        }

        if (StreamproofOverlay.isCapturingFrame()) {
            StreamproofOverlay.drawSatoshiRun(text, x, y, sizePx, gradient, weight, quads);
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        TextureManager tm = mc.getTextureManager();
        AbstractTexture tex = tm.getTexture(UiAssets.fontAtlasTexture());
        TextureSetup setup = TextureSetup.singleTexture(tex.getTextureView(), tex.getSampler());
        Matrix3x2f pose = new Matrix3x2f(graphics.pose());
        ScreenRectangle scissor = graphics.scissorStack.peek();
        int ix0 = (int) Math.floor(minX);
        int iy0 = (int) Math.floor(minY);
        int iw = Math.max(1, (int) Math.ceil(maxX) - ix0);
        int ih = Math.max(1, (int) Math.ceil(maxY) - iy0);
        ScreenRectangle rawRect = new ScreenRectangle(ix0, iy0, iw, ih);
        ScreenRectangle transformed = rawRect.transformMaxBounds(pose);
        ScreenRectangle bounds = scissor != null ? scissor.intersection(transformed) : transformed;
        if (bounds == null) {
            return;
        }

        graphics.guiRenderState.addGuiElement(new SatoshiTextRunState(setup, pose, quads, scissor, bounds));
    }

    public static void drawCentered(
        GuiGraphicsExtractor graphics,
        String text,
        float centerX,
        float y,
        float sizePx,
        TextGradient gradient,
        SatoshiWeight weight
    ) {
        float w = width(text, sizePx, weight);
        draw(graphics, text, centerX - w * 0.5f, y, sizePx, gradient, weight);
    }

    private static float clamp(float val, float min, float max) {
        return Math.max(min, Math.min(max, val));
    }

    private record SatoshiTextRunState(
        TextureSetup textureSetup,
        Matrix3x2fc pose,
        List<PlacedGlyph> glyphs,
        ScreenRectangle scissorArea,
        ScreenRectangle bounds
    ) implements GuiElementRenderState {
        @Override
        public RenderPipeline pipeline() {
            return RenderPipelines.GUI_TEXTURED;
        }

        @Override
        public void buildVertices(VertexConsumer vc) {
            for (PlacedGlyph g : glyphs) {
                emitTexVertex(vc, pose, g.x0, g.y0, g.u0, g.v0, g.cTopLeft);
                emitTexVertex(vc, pose, g.x0, g.y1, g.u0, g.v1, g.cBotLeft);
                emitTexVertex(vc, pose, g.x1, g.y1, g.u1, g.v1, g.cBotRight);
                emitTexVertex(vc, pose, g.x1, g.y0, g.u1, g.v0, g.cTopRight);
            }
        }

        private static void emitTexVertex(VertexConsumer vc, Matrix3x2fc pose, float vx, float vy, float u, float v, int col) {
            VertexConsumer step = vc.addVertexWith2DPose(pose, vx, vy);
            step.setUv(u, v);
            step.setColor(col);
        }
    }
}
