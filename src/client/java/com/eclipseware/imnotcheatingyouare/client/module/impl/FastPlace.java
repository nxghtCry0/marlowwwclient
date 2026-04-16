package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;

public class FastPlace extends Module {
    public static FastPlace INSTANCE;

    public FastPlace() {
        super("FastPlace", Category.World, "Removes the right-click delay for placing blocks.");
        INSTANCE = this;
    }
}