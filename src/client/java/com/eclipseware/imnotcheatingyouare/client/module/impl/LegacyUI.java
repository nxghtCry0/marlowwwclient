package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;

public class LegacyUI extends Module {
    public static LegacyUI INSTANCE;

    public LegacyUI() {
        super("LegacyUI", Category.Client, "Enables the old click GUI instead of the new glassy UI.");
        INSTANCE = this;
    }
}
