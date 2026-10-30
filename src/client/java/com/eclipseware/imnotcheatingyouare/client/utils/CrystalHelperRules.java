package com.eclipseware.imnotcheatingyouare.client.utils;

public final class CrystalHelperRules {

    public enum Held {
        EMPTY, WEAPON, CRYSTAL, OBSIDIAN, TOTEM, GLOWSTONE, ANCHOR, OTHER
    }

    public record HeldFilter(boolean onlySelected, boolean onAny, boolean onEmpty, boolean onSword,
                             boolean onCrystalItem, boolean onObiItem, boolean onTotem,
                             boolean onGlowstone, boolean onAnchor) {

        public boolean allows(Held held) {
            if (!onlySelected || onAny) return true;
            return switch (held) {
                case EMPTY -> onEmpty;
                case WEAPON -> onSword;
                case CRYSTAL -> onCrystalItem;
                case OBSIDIAN -> onObiItem;
                case TOTEM -> onTotem;
                case GLOWSTONE -> onGlowstone;
                case ANCHOR -> onAnchor;
                case OTHER -> false;
            };
        }
    }

    private CrystalHelperRules() {
    }

    public static boolean shouldTrigger(boolean holdTrigger, boolean isDown, boolean wasDown) {
        if (holdTrigger) return isDown;
        return isDown && !wasDown;
    }

    public static boolean cooldownElapsed(long now, long lastPlace, double cooldownMs) {
        return now - lastPlace >= cooldownMs;
    }

    public static double jitteredCooldown(double baseMs, double random01) {
        return baseMs * (1.0 + 0.25 * random01);
    }

    public static boolean isCrystallable(boolean obsidian, boolean bedrock, boolean excludeBedrock) {
        if (obsidian) return true;
        return bedrock && !excludeBedrock;
    }
}
