package xyz.breadloaf.imguimc.theme;

import xyz.breadloaf.imguimc.interfaces.Theme;

public class ImGuiClassicTheme implements Theme {
    @Override
    public void preRender() {
        BaseImGuiTheme.applyClassic();
    }

    @Override
    public void postRender() {

    }
}
