package xyz.breadloaf.imguimc.screen;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import xyz.breadloaf.imguimc.imgui.ImguiLoader;

public class EmptyScreen extends Screen {

    public EmptyScreen() {
        super(Component.literal("EmptyScreen"));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor drawContext, int mouseX, int mouseY, float delta) {
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor drawContext, int mouseX, int mouseY, float delta) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (com.eclipseware.imnotcheatingyouare.client.clickgui.ImGuiClickGui.isBinding()) {
            com.eclipseware.imnotcheatingyouare.client.clickgui.ImGuiClickGui.handleBindingKey(event.input());
            return true;
        }

        if (ImguiLoader.wantsTextInput()) {
            return true;
        }

        int key = event.input();
        int menuKey = InputConstants.KEY_RSHIFT;
        if (ImnotcheatingyouareClient.INSTANCE != null && ImnotcheatingyouareClient.INSTANCE.moduleManager != null) {
            Module menuMod = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("Menu");
            if (menuMod != null) menuKey = menuMod.getKeyBind();
        }

        if (key == InputConstants.KEY_ESCAPE || (menuKey != 0 && key == menuKey)) {
            this.onClose();
            return true;
        }

        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        com.eclipseware.imnotcheatingyouare.client.clickgui.ImGuiClickGui.markClosed();
        com.eclipseware.imnotcheatingyouare.client.module.impl.WeakDevice weakDevice =
                com.eclipseware.imnotcheatingyouare.client.module.impl.WeakDevice.INSTANCE;
        if (weakDevice != null) weakDevice.markClosed();
        super.onClose();
    }
}
