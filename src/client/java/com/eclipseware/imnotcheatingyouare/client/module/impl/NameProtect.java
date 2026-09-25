package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NameProtect extends Module {
    public static NameProtect INSTANCE;
    private static String cachedName;
    private static Pattern cachedPattern;

    public NameProtect() {
        super("NameProtect", Category.Misc, "Hides your real username everywhere text is drawn: chat, tab list, scoreboard, nametags and menus.");
        INSTANCE = this;
    }

    private static boolean logged;

    private static String mode() {
        Setting s = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(INSTANCE, "Mode");
        return s != null ? s.getValString() : "Self";
    }

    public static String apply(String text) {
        if (text == null || text.length() < 2 || INSTANCE == null || !INSTANCE.isToggled() || mc == null || mc.getUser() == null) return text;
        String alias = alias();
        String out = replace(text, mc.getUser().getName(), alias);
        if (mode().equals("Everyone") && mc.level != null) {
            for (var p : mc.level.players()) {
                String n = p.getGameProfile().name();
                if (n != null && !n.equals(alias)) out = replace(out, n, alias);
            }
        }
        if (!logged && out != text && !out.equals(text)) {
            logged = true;
            System.out.println("[Marlow] NameProtect is replacing names");
        }
        return out;
    }

    private static String replace(String text, String name, String alias) {
        if (name == null || name.length() < 2 || !containsIgnoreCase(text, name)) return text;
        Pattern p = name.equals(cachedName) ? cachedPattern : Pattern.compile("(?<![A-Za-z0-9_])" + Pattern.quote(name) + "(?![A-Za-z0-9_])", Pattern.CASE_INSENSITIVE);
        if (!name.equals(cachedName) && name.equals(mc.getUser().getName())) {
            cachedName = name;
            cachedPattern = p;
        }
        return p.matcher(text).replaceAll(Matcher.quoteReplacement(alias));
    }

    private static boolean containsIgnoreCase(String text, String name) {
        int n = name.length();
        for (int i = 0; i + n <= text.length(); i++) {
            if (text.regionMatches(true, i, name, 0, n)) return true;
        }
        return false;
    }

    private static String alias() {
        Setting s = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(INSTANCE, "Name");
        String a = s != null ? (s.isText() ? s.getValText() : s.getValString()) : null;
        return a == null || a.isEmpty() ? "Marlowww" : a;
    }
}
