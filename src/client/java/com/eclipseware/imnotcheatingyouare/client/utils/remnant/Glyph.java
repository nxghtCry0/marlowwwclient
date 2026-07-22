package com.eclipseware.imnotcheatingyouare.client.utils.remnant;

import com.google.gson.JsonObject;

public class Glyph {
    private int unicode;
    private float advance;
    private float planeLeft, planeBottom, planeRight, planeTop;
    private float atlasLeft, atlasBottom, atlasRight, atlasTop;

    public int getUnicode() { return unicode; }
    public float getAdvance() { return advance; }
    public float getPlaneLeft() { return planeLeft; }
    public float getPlaneBottom() { return planeBottom; }
    public float getPlaneRight() { return planeRight; }
    public float getPlaneTop() { return planeTop; }
    public float getAtlasLeft() { return atlasLeft; }
    public float getAtlasBottom() { return atlasBottom; }
    public float getAtlasRight() { return atlasRight; }
    public float getAtlasTop() { return atlasTop; }

    public static Glyph parse(final JsonObject object) {
        final Glyph glyph = new Glyph();
        glyph.unicode = object.get("unicode").getAsInt();
        glyph.advance = object.get("advance").getAsFloat();

        if (object.has("planeBounds")) {
            glyph.planeLeft = object.get("planeBounds").getAsJsonObject().get("left").getAsFloat();
            glyph.planeBottom = object.get("planeBounds").getAsJsonObject().get("bottom").getAsFloat();
            glyph.planeRight = object.get("planeBounds").getAsJsonObject().get("right").getAsFloat();
            glyph.planeTop = object.get("planeBounds").getAsJsonObject().get("top").getAsFloat();
        }

        if (object.has("atlasBounds")) {
            glyph.atlasLeft = object.get("atlasBounds").getAsJsonObject().get("left").getAsFloat();
            glyph.atlasBottom = object.get("atlasBounds").getAsJsonObject().get("bottom").getAsFloat();
            glyph.atlasRight = object.get("atlasBounds").getAsJsonObject().get("right").getAsFloat();
            glyph.atlasTop = object.get("atlasBounds").getAsJsonObject().get("top").getAsFloat();
        }

        return glyph;
    }
}
