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

    public static String apply(String text) {
        if (text == null || text.isEmpty() || INSTANCE == null || !INSTANCE.isToggled() || mc == null || mc.getUser() == null) return text;
        String name = mc.getUser().getName();
        if (name == null || name.length() < 2) return text;
        if (!containsIgnoreCase(text, name)) return text;
        if (!name.equals(cachedName)) {
            cachedName = name;
            cachedPattern = Pattern.compile(Pattern.quote(name), Pattern.CASE_INSENSITIVE);
        }
        return cachedPattern.matcher(text).replaceAll(Matcher.quoteReplacement(alias()));
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
