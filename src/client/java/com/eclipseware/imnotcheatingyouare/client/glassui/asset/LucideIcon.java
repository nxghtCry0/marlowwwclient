package com.eclipseware.imnotcheatingyouare.client.glassui.asset;

import net.minecraft.resources.Identifier;

public enum LucideIcon implements UiIcon {
    PORTAL_ARROW("portal_arrow"),
    CROSSHAIR("crosshair"),
    LOG_IN("log_in"),
    USER("user"),
    EYE("eye"),
    PALETTE("palette"),
    SETTINGS("settings"),
    POWER("power"),
    SEARCH("search"),
    SAVE("save"),
    CHECK("check"),
    MOVE_DIAGONAL("move_diagonal"),
    FOCUS("focus"),
    ZAP("zap"),
    BOX("box"),
    WIND("wind"),
    FEATHER("feather"),
    FOOTPRINTS("footprints"),
    SHIELD("shield"),
    SPARKLES("sparkles"),
    SUN("sun"),
    LAYERS("layers"),
    COMPASS("compass"),
    GLOBE("globe"),
    CLOCK("clock"),
    SLIDERS_HORIZONTAL("sliders_horizontal"),
    CPU("cpu"),
    GAUGE("gauge"),
    BELL("bell"),
    SWORDS("swords"),
    SWORD("sword"),
    GAVEL("gavel"),
    REPEAT("repeat"),
    MOUSE("mouse"),
    FLAME("flame"),
    ARROW_LEFT("arrow_left"),
    X_CLOSE("x"),
    DROPLET("droplet"),
    PACKAGE("package"),
    HEART_PULSE("heart_pulse"),
    HAND("hand"),
    DANGER("triangle_alert"),
    SCAN("scan"),
    NAVIGATION("navigation"),
    TAG("tag"),
    CUBOID("cuboid"),
    PLUS("plus");

    private final String fileName;

    LucideIcon(String fileName) {
        this.fileName = fileName;
    }

    @Override
    public Identifier texture() {
        return UiAssets.iconTexture(fileName);
    }
}
