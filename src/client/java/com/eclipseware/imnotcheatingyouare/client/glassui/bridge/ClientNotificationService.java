package com.eclipseware.imnotcheatingyouare.client.glassui.bridge;

import com.eclipseware.imnotcheatingyouare.client.glassui.asset.LucideIcon;
import com.eclipseware.imnotcheatingyouare.client.glassui.asset.UiIcon;
import com.eclipseware.imnotcheatingyouare.client.glassui.bind.UiInputBind;
import com.eclipseware.imnotcheatingyouare.client.glassui.model.UiCard;
import com.eclipseware.imnotcheatingyouare.client.glassui.notify.UiNotificationConfig;
import com.eclipseware.imnotcheatingyouare.client.glassui.notify.UiNotificationKind;
import com.eclipseware.imnotcheatingyouare.client.glassui.notify.UiNotificationLayout;
import com.eclipseware.imnotcheatingyouare.client.glassui.notify.UiNotificationOverlay;
import com.eclipseware.imnotcheatingyouare.client.glassui.screen.GlassUiScreen;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class ClientNotificationService {
    private static final UiNotificationOverlay OVERLAY = new UiNotificationOverlay(
        new UiNotificationConfig(
            () -> ClientGuiRegistry.isFeatureEnabled("notifications"),
            () -> UiNotificationLayout.fromIndex(ClientGuiRegistry.getChoice("notifications", 0)),
            () -> ClientGuiRegistry.getSlider("notif_duration", 2.2f),
            () -> ClientGuiRegistry.getBool("notif_typing", true),
            () -> ClientGuiRegistry.getBool("notif_streamproof_hide", false),
            ClientGuiRegistry::theme,
            ClientUiSounds.INSTANCE,
            screen -> screen instanceof GlassUiScreen
        )
    );

    private static volatile boolean suppressModuleHook = false;

    private ClientNotificationService() {
    }

    public static UiNotificationOverlay overlay() {
        return OVERLAY;
    }

    public static boolean hasActiveNotifications() {
        return OVERLAY.hasActiveNotifications();
    }

    public static void setSuppressModuleHook(boolean suppress) {
        suppressModuleHook = suppress;
    }

    public static void onModuleToggled(Module module, boolean enabled) {
        if (suppressModuleHook || !ClientGuiRegistry.isInitialized() || module == null || module.isHidden()) {
            return;
        }
        String name = module.getName();
        if ("GUI".equalsIgnoreCase(name)
            || "Config Menu".equalsIgnoreCase(name)
            || "HUDEditor".equalsIgnoreCase(name)
            || "RecommendedConfigs".equalsIgnoreCase(name)) {
            return;
        }
        ClientDashboardProvider.recordModuleToggle();
        ClientUiSounds.INSTANCE.playToggle(enabled);
        if (!ClientGuiRegistry.isFeatureEnabled("notifications")) {
            return;
        }
        UiInputBind bind = UiInputBind.fromModuleKeyBind(module.getKeyBind());
        String suffix = bind.isBound() ? (" • " + bind.formatLabel()) : "";
        String subtitle = enabled ? ("Module enabled" + suffix) : ("Module disabled" + suffix);
        UiIcon icon = ClientGuiRegistry.resolveModuleIcon(module);
        OVERLAY.push(
            name,
            enabled ? "ON" : "OFF",
            subtitle,
            icon,
            enabled ? UiNotificationKind.ENABLED : UiNotificationKind.DISABLED,
            true
        );
    }

    public static void notifySystemCardToggle(UiCard card, boolean enabled) {
        ClientDashboardProvider.recordModuleToggle();
        ClientUiSounds.INSTANCE.playToggle(enabled);
        if (!ClientGuiRegistry.isFeatureEnabled("notifications")) {
            return;
        }
        OVERLAY.push(
            card.title(),
            enabled ? "ON" : "OFF",
            enabled ? "System feature enabled" : "System feature disabled",
            card.icon(),
            enabled ? UiNotificationKind.ENABLED : UiNotificationKind.DISABLED,
            true
        );
    }

    public static void notifyMacro(String title, String detail) {
        ClientDashboardProvider.recordMacroRun();
        ClientUiSounds.INSTANCE.playNotify();
        if (!ClientGuiRegistry.isFeatureEnabled("notifications") || !ClientGuiRegistry.getBool("notif_macros", true)) {
            return;
        }
        OVERLAY.push(title, "MACRO", detail, LucideIcon.REPEAT, UiNotificationKind.ACTION, false);
    }

    public static void notifyInfo(String title, String detail, UiIcon icon, boolean playSound) {
        if (playSound) {
            ClientUiSounds.INSTANCE.playNotify();
        }
        if (!ClientGuiRegistry.isFeatureEnabled("notifications")) {
            return;
        }
        OVERLAY.push(title, "INFO", detail, icon != null ? icon : LucideIcon.BELL, UiNotificationKind.INFO, false);
    }

    public static void renderHud(GuiGraphicsExtractor graphics) {
        OVERLAY.renderHud(graphics);
    }
}
