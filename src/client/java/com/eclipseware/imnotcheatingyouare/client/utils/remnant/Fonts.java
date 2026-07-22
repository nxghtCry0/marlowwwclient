package com.eclipseware.imnotcheatingyouare.client.utils.remnant;

import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.PackType;
import net.minecraft.resources.Identifier;

import java.io.IOException;

public class Fonts implements SimpleSynchronousResourceReloadListener {
    private static Fonts INSTANCE;

    private FontAtlas interBold;
    private FontAtlas interSemiBold;
    private FontAtlas interMedium;
    private FontAtlas proggyClean;
    private FontAtlas poppins;
    private FontAtlas icons;
    private FontAtlas lucide;

    public FontAtlas getInterBold() { return interBold; }
    public FontAtlas getInterSemiBold() { return interSemiBold; }
    public FontAtlas getInterMedium() { return interMedium; }
    public FontAtlas getProggyClean() { return proggyClean; }
    public FontAtlas getPoppins() { return poppins; }
    public FontAtlas getIcons() { return icons; }
    public FontAtlas getLucide() { return lucide; }

    public Fonts() {
        INSTANCE = this;
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(this);
    }

    public static Fonts getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new Fonts();
            try {
                net.minecraft.server.packs.resources.ResourceManager manager = net.minecraft.client.Minecraft.getInstance().getResourceManager();
                if (manager != null) {
                    INSTANCE.onResourceManagerReload(manager);
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
        return INSTANCE;
    }

    @Override
    public Identifier getFabricId() {
        return Identifier.parse("imnotcheatingyouare:reload_fonts");
    }

    @Override
    public void onResourceManagerReload(final ResourceManager manager) {
        try {
            this.interBold = new FontAtlas(manager, "inter-bold");
            this.interMedium = new FontAtlas(manager, "inter-medium");
            this.interSemiBold = new FontAtlas(manager, "inter-semibold");
            this.proggyClean = new FontAtlas(manager, "proggy-clean");
            this.poppins = new FontAtlas(manager, "poppins");
            this.icons = new FontAtlas(manager, "icons");
            this.lucide = new FontAtlas(manager, "lucide");
        } catch (final IOException exception) {
            throw new RuntimeException("Couldn't load fonts", exception);
        }
    }
}
