package com.eclipseware.imnotcheatingyouare.client.glassui.bind;

public interface UiKeybindProvider {
    UiInputBind getBind(String itemId);

    void setBind(String itemId, UiInputBind bind);

    default int boundCount() {
        return 0;
    }
}
