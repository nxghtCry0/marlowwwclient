package com.eclipseware.imnotcheatingyouare.client.ui;

public final class GlassyTheme {
    public static final int OVERLAY_DIM = 0x8B000000;
    
    public static final int PANEL_BG = 0xB0101010;
    public static final int PANEL_HEADER_BG = 0x80202020;
    public static final int PANEL_BORDER = 0x31000000;
    public static final int PANEL_BORDER_STRONG = 0x55FFFFFF;
    
    public static int ROW_HOVER_BG = 0x1FFFFFFF;
    public static int ROW_SELECTED_BG = 0x2233AAFF;
    public static int ROW_SELECTED_STRIP = 0xFF4CD1FF;
    
    public static int TEXT = 0xFFEAEAEA;
    public static int TEXT_MUTED = 0xFFBFBFBF;
    public static int TEXT_SUBTLE = 0xFF8F8F8F;
    
    public static int ACCENT = 0xFF4CD1FF;
    public static int ACCENT_DIM = 0x804CD1FF;
    
    public static final int CONTROL_BG = 0x40181818;
    public static final int CONTROL_BG_HOVER = 0x50222222;
    public static final int CONTROL_BG_DISABLED = 0x20101010;
    public static final int CONTROL_BORDER = 0x31000000;
    public static final int CONTROL_BORDER_HOVER = 0x55FFFFFF;
    
    public static final int CONTROL_BG_STRONG = 0x70303030;
    public static final int CONTROL_BG_STRONG_HOVER = 0x8A404040;
    public static final int CONTROL_BG_STRONG_PRESSED = 0xA0404040;
    public static final int CONTROL_BORDER_STRONG = 0x55FFFFFF;
    public static final int CONTROL_BORDER_STRONG_HOVER = 0x81000000;
    
    public static void updateColors(int rgb) {
        int color = rgb & 0xFFFFFF;
        ACCENT = 0xFF000000 | color;
        ACCENT_DIM = 0x80000000 | color;
        ROW_SELECTED_BG = 0x22000000 | color;
        ROW_SELECTED_STRIP = ACCENT;
    }
}
