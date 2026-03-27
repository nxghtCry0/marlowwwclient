package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;

public class ArrayListMod extends Module {
    public ArrayListMod() {
        super("ArrayList", Category.Render);
        this.toggle(); // Default to ON when the client launches
    }
}