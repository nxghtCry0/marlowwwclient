package com.eclipseware.imnotcheatingyouare.client.clickgui;

import com.eclipseware.imnotcheatingyouare.client.utils.FontUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.File;

public class PSAScreen extends Screen {
    private final Screen parentScreen;
    private final long startTime;
    private Button exitButton;

    public PSAScreen(Screen parentScreen) {
        super(Component.literal("PSA Warning"));
        this.parentScreen = parentScreen;
        this.startTime = System.currentTimeMillis();
    }

    @Override
    protected void init() {
        super.init();

        int btnWidth = 200;
        int btnHeight = 20;
        int btnX = this.width / 2 - btnWidth / 2;
        int btnY = this.height - 40;

        this.exitButton = this.addRenderableWidget(Button.builder(Component.literal("Exit (10s)"), btn -> {
            try {
                File dir = new File(this.minecraft.gameDirectory, "config/imnotcheatingyouare");
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                File psaFile = new File(dir, "psa_accepted");
                psaFile.createNewFile();
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (this.minecraft != null) {
                this.minecraft.setScreen(this.parentScreen);
            }
        }).bounds(btnX, btnY, btnWidth, btnHeight).build());

        this.exitButton.active = false;
    }

    @Override
    public void tick() {
        super.tick();
        long elapsed = System.currentTimeMillis() - this.startTime;
        long secondsRemaining = 10 - (elapsed / 1000);
        if (secondsRemaining > 0) {
            this.exitButton.active = false;
            this.exitButton.setMessage(Component.literal("Exit (" + secondsRemaining + "s)"));
        } else {
            this.exitButton.active = true;
            this.exitButton.setMessage(Component.literal("Exit"));
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.fill(0, 0, this.width, this.height, 0x80000000);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);

        int titleY = 30;
        FontUtils.drawCenteredString(guiGraphics, "\u00a7c\u00a7lPSA: Marlow Client Reuploads!", this.width / 2, titleY, -1);

        int textY = titleY + 30;
        int spacing = 12;

        FontUtils.drawCenteredString(guiGraphics, "\u00a7e- Marlowww client only has TWO official sources! \u00a7f", this.width / 2, textY, -1);
        textY += spacing;
        FontUtils.drawCenteredString(guiGraphics, "\u00a7cIf you are NOT running a version from one of these, IT IS MALWARE!\u00a7f", this.width / 2, textY, -1);
        textY += spacing * 2;

        FontUtils.drawCenteredString(guiGraphics, "\u00a7bHere are the only two sources:\u00a7f", this.width / 2, textY, -1);
        textY += spacing;
        FontUtils.drawCenteredString(guiGraphics, "\u00a7a- https://discord.gg/zgurRAZNte\u00a7f", this.width / 2, textY, -1);
        textY += spacing;
        FontUtils.drawCenteredString(guiGraphics, "\u00a7a- https://github.com/nxghtCry0/marlowwwclient\u00a7f", this.width / 2, textY, -1);
        textY += spacing * 2;

        FontUtils.drawCenteredString(guiGraphics, "\u00a77I know this client is open source and I cannot stop people from changing this in public forks,\u00a7f", this.width / 2, textY, -1);
        textY += spacing;
        FontUtils.drawCenteredString(guiGraphics, "\u00a77but if you have privately changed this, and I figure out,\u00a7f", this.width / 2, textY, -1);
        textY += spacing;
        FontUtils.drawCenteredString(guiGraphics, "\u00a77you will be hit with a cease and desist faster than you can react.\u00a7f", this.width / 2, textY, -1);
        textY += spacing * 2;

        FontUtils.drawCenteredString(guiGraphics, "\u00a7dThanks for using Marlowww Client,", this.width / 2, textY, -1);
        textY += spacing;
        FontUtils.drawCenteredString(guiGraphics, "\u00a7d-nxght", this.width / 2, textY, -1);
    }
}
