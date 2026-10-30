package xyz.breadloaf.imguimc.screen;

import com.eclipseware.imnotcheatingyouare.client.clickgui.ImGuiClickGui;
import com.eclipseware.imnotcheatingyouare.client.utils.InputUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class EmptyScreen extends Screen {

    public EmptyScreen() {
        super(Component.literal("EclipseWare ClickGUI"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor drawContext, int mouseX, int mouseY, float delta) {
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor drawContext, int mouseX, int mouseY, float delta) {
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        Runnable closeAction = () -> ImGuiClickGui.setOpen(false);
        if (ImGuiClickGui.onScreenKeyPressed(key, closeAction)) {
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        if (ImGuiClickGui.onScreenMouseClicked(InputUtil.toLegacyOrdinal(event.button()))) {
            return true;
        }
        return super.mouseClicked(event, doubled);
    }

    @Override
    public void removed() {
        super.removed();
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.gui.screen() != this) {
            ImGuiClickGui.markClosed();
        }
    }
}
