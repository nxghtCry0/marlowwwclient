package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;

public class AntiTranslationKey extends Module {
    public AntiTranslationKey() {
        super("AntiTranslationKey", Category.Misc, "Prevents servers from crashing you with malformed translation strings.");
    }
}