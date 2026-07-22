package com.eclipseware.imnotcheatingyouare.client.utils.remnant;

import com.google.gson.JsonObject;

public class FontMetrics {
    private int emSize;
    private float lineHeight;
    private float ascender;
    private float descender;
    private float underlineY;
    private float underlineThickness;

    public int getEmSize() { return emSize; }
    public float getLineHeight() { return lineHeight; }
    public float getAscender() { return ascender; }
    public float getDescender() { return descender; }
    public float getUnderlineY() { return underlineY; }
    public float getUnderlineThickness() { return underlineThickness; }

    public FontMetrics() {}
    public FontMetrics(final int emSize, final float lineHeight, final float ascender, final float descender, final float underlineY, final float underlineThickness) {
        this.emSize = emSize;
        this.lineHeight = lineHeight;
        this.ascender = ascender;
        this.descender = descender;
        this.underlineY = underlineY;
        this.underlineThickness = underlineThickness;
    }

    public static FontMetrics parse(final JsonObject object) {
        final FontMetrics metrics = new FontMetrics();
        metrics.emSize = object.get("emSize").getAsInt();
        metrics.lineHeight = object.get("lineHeight").getAsFloat();
        metrics.ascender = object.get("ascender").getAsFloat();
        metrics.descender = object.get("descender").getAsFloat();
        metrics.underlineY = object.get("underlineY").getAsFloat();
        metrics.underlineThickness = object.get("underlineThickness").getAsFloat();
        return metrics;
    }
}
