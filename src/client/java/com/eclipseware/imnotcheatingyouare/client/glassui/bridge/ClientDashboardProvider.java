package com.eclipseware.imnotcheatingyouare.client.glassui.bridge;

import com.eclipseware.imnotcheatingyouare.client.clickgui.HudEditorScreen;
import com.eclipseware.imnotcheatingyouare.client.glassui.asset.LucideIcon;
import com.eclipseware.imnotcheatingyouare.client.glassui.audio.UiSoundSink;
import com.eclipseware.imnotcheatingyouare.client.glassui.model.UiDashboardAction;
import com.eclipseware.imnotcheatingyouare.client.glassui.model.UiDashboardProvider;
import com.eclipseware.imnotcheatingyouare.client.glassui.model.UiDashboardSnapshot;
import com.eclipseware.imnotcheatingyouare.client.glassui.model.UiStatCard;
import com.eclipseware.imnotcheatingyouare.client.glassui.render.StreamproofOverlay;
import com.eclipseware.imnotcheatingyouare.client.glassui.theme.UiTheme;
import com.eclipseware.imnotcheatingyouare.client.macro.MacroManager;
import com.eclipseware.imnotcheatingyouare.client.setting.ConfigManager;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.ServerData;

public final class ClientDashboardProvider implements UiDashboardProvider {
    public static final ClientDashboardProvider INSTANCE = new ClientDashboardProvider();

    private static final long SESSION_START_MS = System.currentTimeMillis();
    private static int moduleSwitches = 0;
    private static int macroRuns = 0;

    private ClientDashboardProvider() {
    }

    public static void recordModuleToggle() {
        moduleSwitches++;
    }

    public static void recordMacroRun() {
        macroRuns++;
    }

    public static void resetCounters() {
        moduleSwitches = 0;
        macroRuns = 0;
    }

    @Override
    public boolean hasActiveStatus() {
        return ClientGuiRegistry.activeModuleCount() > 0;
    }

    @Override
    public UiDashboardSnapshot buildSnapshot(Minecraft mc) {
        String srvText = serverLabel(mc, ClientGuiRegistry.isServerIpHidden());
        String statusLine = srvText + " • Session " + formattedUptime() + " • Press RSHIFT for Marlow UI";

        int activeMods = ClientGuiRegistry.activeModuleCount();
        int totalMods = ClientGuiRegistry.totalModuleCount();
        int bindsCount = ClientKeybindBridge.INSTANCE.boundCount();
        String badgeText = activeMods + "/" + totalMods + " Active • " + bindsCount + " Binds";

        int fpsVal = mc.getFps();
        int pingVal = pingMs(mc);
        int profiles = ConfigCenterSection.profileCount();
        int macros = MacroManager.getMacros().size();

        List<UiStatCard> stats = List.of(
            new UiStatCard(
                "Frame Rate",
                fpsVal + " FPS",
                StreamproofOverlay.shouldUseExternalOverlay() ? "Async DWM overlay active" : "Native GPU render pass",
                clamp(fpsVal / 240f, 0.06f, 1f),
                LucideIcon.CPU
            ),
            new UiStatCard(
                "Network Ping",
                mc.isLocalServer() ? "0 ms" : (pingVal + " ms"),
                mc.isLocalServer() ? "Integrated local world" : srvText,
                mc.isLocalServer() ? 1.0f : clamp(1f - (pingVal / 250f), 0.08f, 1f),
                LucideIcon.GLOBE
            ),
            new UiStatCard(
                "Active Modules",
                activeMods + " Enabled",
                totalMods + " modules available",
                totalMods <= 0 ? 0.08f : clamp((float) activeMods / totalMods, 0.08f, 1f),
                LucideIcon.SWORDS
            ),
            new UiStatCard(
                "Saved Profiles",
                profiles + " Profiles",
                "Encrypted .enc configs",
                clamp((profiles + 1) / 8f, 0.12f, 1f),
                LucideIcon.SHIELD
            ),
            new UiStatCard(
                "Client Macros",
                macros + " Macros",
                macroRuns + " macro runs this session",
                clamp((macros + macroRuns) / 10f, 0.10f, 1f),
                LucideIcon.REPEAT
            ),
            new UiStatCard(
                "Session Activity",
                formattedUptime(),
                moduleSwitches + " module switches",
                clamp((moduleSwitches + 2) / 30f, 0.10f, 1f),
                LucideIcon.CLOCK
            )
        );

        boolean spOn = ClientGuiRegistry.isFeatureEnabled("streamproof_menu");
        boolean notifOn = ClientGuiRegistry.isFeatureEnabled("notifications");

        List<UiDashboardAction> actions = List.of(
            new UiDashboardAction(
                "Streamproof",
                spOn ? "Active (F8)" : "Off (In-Game)",
                LucideIcon.EYE,
                spOn,
                false,
                (ui, lx, ly) -> {
                    boolean next = !ClientGuiRegistry.isFeatureEnabled("streamproof_menu");
                    ClientGuiRegistry.setFeatureEnabled("streamproof_menu", next, true);
                    if (!next) {
                        StreamproofOverlay.hide();
                    }
                    UiTheme th = ui.theme();
                    ui.spawnRipple(lx, ly, 22f, th.accentColor());
                }
            ),
            new UiDashboardAction(
                "HUD Alerts",
                notifOn ? "Enabled" : "Muted",
                LucideIcon.BELL,
                notifOn,
                false,
                (ui, lx, ly) -> {
                    boolean next = !ClientGuiRegistry.isFeatureEnabled("notifications");
                    ClientGuiRegistry.setFeatureEnabled("notifications", next, false);
                    UiSoundSink snd = ui.sounds();
                    snd.playToggle(next);
                    ui.showToast(next ? "HUD notifications enabled" : "HUD notifications muted");
                    UiTheme th = ui.theme();
                    ui.spawnRipple(lx, ly, 22f, th.accentColor());
                }
            ),
            new UiDashboardAction(
                "HUD Editor",
                "Drag overlays",
                LucideIcon.MOVE_DIAGONAL,
                false,
                false,
                (ui, lx, ly) -> {
                    if (mc.player != null) {
                        mc.setScreenAndShow(new HudEditorScreen());
                    } else {
                        ui.showToast("Join a world to arrange HUD elements");
                    }
                }
            ),
            new UiDashboardAction(
                "Reload Config",
                "Read disk cfg",
                LucideIcon.REPEAT,
                false,
                false,
                (ui, lx, ly) -> {
                    ConfigManager.load();
                    ClientGuiRegistry.loadGuiPrefs();
                    UiSoundSink snd = ui.sounds();
                    snd.playNotify();
                    ui.showToast("Reloaded configuration from disk");
                    UiTheme th = ui.theme();
                    ui.spawnRipple(lx, ly, 22f, th.accentColor());
                }
            ),
            new UiDashboardAction(
                "Disable All",
                activeMods + " active now",
                LucideIcon.POWER,
                false,
                true,
                (ui, lx, ly) -> {
                    int disabled = ClientGuiRegistry.disableAllGameplayModules();
                    UiSoundSink snd = ui.sounds();
                    snd.playToggle(false);
                    if (disabled > 0) {
                        ClientNotificationService.notifyInfo("Panic Disable", "Disabled " + disabled + " active modules", LucideIcon.POWER, false);
                        ui.showToast("Disabled " + disabled + " active modules");
                    } else {
                        ui.showToast("No active modules to disable");
                    }
                    ui.spawnRipple(lx, ly, 24f, 0xFFFF6470);
                }
            )
        );

        User user = mc.getUser();
        return new UiDashboardSnapshot(
            user.getName(),
            statusLine,
            badgeText,
            stats,
            actions
        );
    }

    private static String serverLabel(Minecraft mc, boolean hideIp) {
        if (mc.level == null) {
            return "Main Menu";
        }
        if (mc.isLocalServer()) {
            return "Singleplayer";
        }
        if (hideIp) {
            return "Server IP Hidden";
        }
        ServerData data = mc.getCurrentServer();
        if (data != null && data.ip != null && !data.ip.isBlank()) {
            return data.ip;
        }
        return "Multiplayer";
    }

    private static int pingMs(Minecraft mc) {
        if (mc.player == null) {
            return 0;
        }
        ClientPacketListener conn = mc.getConnection();
        if (conn == null) {
            return 0;
        }
        UUID uuid = mc.player.getUUID();
        PlayerInfo info = conn.getPlayerInfo(uuid);
        return info != null ? Math.max(0, info.getLatency()) : 0;
    }

    private static String formattedUptime() {
        long sec = Math.max(0L, (System.currentTimeMillis() - SESSION_START_MS) / 1000L);
        long mins = sec / 60L;
        long remSec = sec % 60L;
        if (mins < 60L) {
            return String.format(Locale.ROOT, "%02d:%02d", mins, remSec);
        }
        long hrs = mins / 60L;
        long remMin = mins % 60L;
        return String.format(Locale.ROOT, "%d:%02d:%02d", hrs, remMin, remSec);
    }

    private static float clamp(float val, float min, float max) {
        return Math.max(min, Math.min(max, val));
    }
}
