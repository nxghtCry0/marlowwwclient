package com.eclipseware.imnotcheatingyouare.client.glassui.bridge;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.glassui.render.StreamproofOverlay;
import com.eclipseware.imnotcheatingyouare.client.glassui.screen.GlassUiConfig;
import com.eclipseware.imnotcheatingyouare.client.glassui.screen.GlassUiScreen;
import com.eclipseware.imnotcheatingyouare.client.glassui.screen.UiNavigationMemory;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.module.ModuleManager;
import com.eclipseware.imnotcheatingyouare.client.setting.ConfigManager;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ClientGuiScreen extends GlassUiScreen {
    private static final UiNavigationMemory NAVIGATION_MEMORY = new UiNavigationMemory();
    private static boolean navSyncedFromPrefs = false;

    private static final List<String> SEARCH_PROMPTS = List.of(
        "Search modules...",
        "Try WTap or STap...",
        "Try BlockESP...",
        "Try Triggerbot...",
        "Try Accent Palette...",
        "Try Streamproof..."
    );

    public ClientGuiScreen() {
        this(null);
    }

    public ClientGuiScreen(Screen parentScreen) {
        super(buildConfig(parentScreen));
    }

    public static GlassUiConfig buildConfig(Screen parentScreen) {
        var catalog = ClientGuiRegistry.catalog();
        if (!navSyncedFromPrefs) {
            NAVIGATION_MEMORY.setSidebarExpanded(ClientGuiRegistry.isSidebarExpanded());
            navSyncedFromPrefs = true;
        }
        return new GlassUiConfig(
            catalog,
            Component.literal("Marlow Client"),
            656f,
            424f,
            false,
            ClientGuiRegistry.theme(),
            ClientUiSounds.INSTANCE,
            ClientKeybindBridge.INSTANCE,
            ClientDashboardProvider.INSTANCE,
            ClientNotificationService.overlay(),
            true,
            SEARCH_PROMPTS,
            "Right-click any card for full settings • Click avatar for live stats • Click Bind for key or MB4/MB5",
            () -> ClientGuiRegistry.getBool("esc_unfocus", true),
            true,
            "Saved configuration",
            () -> {
                ClientGuiRegistry.setSidebarExpanded(NAVIGATION_MEMORY.sidebarExpanded());
                ConfigManager.save();
                ClientGuiRegistry.saveGuiPrefs();
            },
            () -> {
                ClientGuiRegistry.setSidebarExpanded(NAVIGATION_MEMORY.sidebarExpanded());
                ClientGuiRegistry.saveGuiPrefs();
                if (ClientGuiRegistry.isAutoSaveEnabled()) {
                    ConfigManager.save();
                }
            },
            event -> event.key() == resolveGuiOpenKey(),
            keyCode -> false,
            (event, actions) -> {
                if (event.key() == InputConstants.KEY_F8) {
                    boolean next = !ClientGuiRegistry.isFeatureEnabled("streamproof_menu");
                    ClientGuiRegistry.setFeatureEnabled("streamproof_menu", next, true);
                    if (!next) {
                        StreamproofOverlay.hide();
                    }
                    actions.showToast(next ? "Streamproof GUI enabled" : "In-game GUI rendering enabled");
                    return true;
                }
                return false;
            },
            null,
            null,
            null,
            NAVIGATION_MEMORY,
            parentScreen
        );
    }

    private static int resolveGuiOpenKey() {
        ImnotcheatingyouareClient client = ImnotcheatingyouareClient.INSTANCE;
        if (client != null && client.moduleManager != null) {
            ModuleManager mm = client.moduleManager;
            Module guiMod = mm.getModule("GUI");
            if (guiMod != null && guiMod.getKeyBind() > 0) {
                return guiMod.getKeyBind();
            }
        }
        return InputConstants.KEY_RSHIFT;
    }

    public static void open() {
        Minecraft mc = Minecraft.getInstance();
        Screen current = mc.gui.screen();
        if (current instanceof ClientGuiScreen) {
            return;
        }
        mc.setScreenAndShow(new ClientGuiScreen(null));
    }

    public static void openWithParent(Screen parent) {
        Minecraft mc = Minecraft.getInstance();
        mc.setScreenAndShow(new ClientGuiScreen(parent));
    }

    public static void openConfigCenter() {
        Minecraft mc = Minecraft.getInstance();
        ClientGuiRegistry.catalog();
        ClientGuiScreen screen = new ClientGuiScreen(null);
        screen.selectCategoryAndTab(ClientGuiRegistry.clientCategoryIndex(), ClientGuiRegistry.configCenterTabIndex());
        mc.setScreenAndShow(screen);
    }

    public static void openSettingsTheme() {
        Minecraft mc = Minecraft.getInstance();
        ClientGuiRegistry.catalog();
        Screen current = mc.gui.screen();
        if (current instanceof GlassUiScreen gs) {
            gs.selectCategoryAndTab(ClientGuiRegistry.settingsCategoryIndex(), 0);
            return;
        }
        ClientGuiScreen screen = new ClientGuiScreen(null);
        screen.selectCategoryAndTab(ClientGuiRegistry.settingsCategoryIndex(), 0);
        mc.setScreenAndShow(screen);
    }

    public static void openModuleDetail(Module module) {
        if (module == null) {
            open();
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ClientGuiRegistry.catalog();
        ClientGuiScreen screen = new ClientGuiScreen(null);
        ClientGuiRegistry.CardLocation loc = ClientGuiRegistry.findLocationForModule(module);
        if (loc != null) {
            screen.selectCategoryAndTab(loc.categoryIndex(), loc.tabIndex());
            screen.openCardDetailImmediate(loc.cardId());
        } else {
            screen.openCardDetailImmediate(ClientGuiRegistry.cardIdForModule(module));
        }
        mc.setScreenAndShow(screen);
    }
}
