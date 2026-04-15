package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;

public class HandView extends Module {
    public static HandView INSTANCE;

    public HandView() {
        super("HandView", Category.Render, "Alters the scale and position of items in your hand.");
        INSTANCE = this;
    }
}