package com.eclipseware.imnotcheatingyouare.client.glassui.screen;

import com.eclipseware.imnotcheatingyouare.client.glassui.audio.UiSoundSink;
import com.eclipseware.imnotcheatingyouare.client.glassui.bind.UiInputBind;
import com.eclipseware.imnotcheatingyouare.client.glassui.bind.UiKeybindProvider;
import com.eclipseware.imnotcheatingyouare.client.glassui.model.UiCard;
import com.eclipseware.imnotcheatingyouare.client.glassui.model.UiCatalog;
import com.eclipseware.imnotcheatingyouare.client.glassui.model.UiCategory;
import com.eclipseware.imnotcheatingyouare.client.glassui.model.UiChoice;
import com.eclipseware.imnotcheatingyouare.client.glassui.model.UiControl;
import com.eclipseware.imnotcheatingyouare.client.glassui.model.UiCustomSection;
import com.eclipseware.imnotcheatingyouare.client.glassui.model.UiDashboardAction;
import com.eclipseware.imnotcheatingyouare.client.glassui.model.UiHitRect;
import com.eclipseware.imnotcheatingyouare.client.glassui.model.UiScreenActions;
import com.eclipseware.imnotcheatingyouare.client.glassui.model.UiTab;
import com.eclipseware.imnotcheatingyouare.client.glassui.notify.UiNotificationOverlay;
import com.eclipseware.imnotcheatingyouare.client.glassui.render.SmoothScroller;
import com.eclipseware.imnotcheatingyouare.client.glassui.render.StreamproofOverlay;
import com.eclipseware.imnotcheatingyouare.client.glassui.render.UiRenderPrimitives;
import com.eclipseware.imnotcheatingyouare.client.glassui.theme.MotionCurve;
import com.eclipseware.imnotcheatingyouare.client.glassui.theme.UiTheme;
import com.eclipseware.imnotcheatingyouare.client.gui.MarlowTitleScreen;
import com.eclipseware.imnotcheatingyouare.client.gui.PastelShaderBackground;
import com.eclipseware.imnotcheatingyouare.client.module.impl.GUI;
import com.eclipseware.imnotcheatingyouare.client.module.impl.Menu;
import com.mojang.blaze3d.platform.InputConstants;
import java.awt.Color;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import org.joml.Matrix3x2fStack;

public class GlassUiScreen extends Screen implements UiScreenActions {
    protected final GlassUiConfig config;
    protected final UiScreenState state = new UiScreenState();

    private int selectedCategoryIndex;
    private int selectedTabIndex;

    private long lastFrameNanos = System.nanoTime();
    private float currentUiScale = 0.96f;
    private float currentRenderScale = 0.96f;
    private float currentCenterX = 0f;
    private float currentCenterY = 0f;
    private float currentPanelWidth = 656f;

    public GlassUiScreen(GlassUiConfig config) {
        super(config.title());
        this.config = config;
        UiCatalog catalog = config.catalog();
        List<UiCategory> categories = catalog.categories();
        UiNavigationMemory nav = config.navigationMemory();
        int maxCat = Math.max(0, categories.size() - 1);
        this.selectedCategoryIndex = clampInt(nav.selectedCategoryIndex(), 0, maxCat);
        int tabCount = 0;
        if (!categories.isEmpty()) {
            UiCategory initialCat = categories.get(this.selectedCategoryIndex);
            tabCount = initialCat.tabs().size();
        }
        this.selectedTabIndex = nav.tabForCategory(this.selectedCategoryIndex, tabCount);
        state.sidebarExpanded = nav.sidebarExpanded();
        state.sidebarExpandProgress = state.sidebarExpanded ? 1f : 0f;
        this.currentPanelWidth = GlassUiChrome.effectivePanelWidth(config, state);
    }

    public GlassUiConfig config() {
        return config;
    }

    public boolean isSidebarExpanded() {
        return state.sidebarExpanded;
    }

    public void toggleSidebar() {
        GlassUiChrome.toggleSidebar(config, state);
    }

    public void selectCategoryAndTab(int categoryIndex, int tabIndex) {
        UiCatalog catalog = config.catalog();
        List<UiCategory> categories = catalog.categories();
        if (categories.isEmpty()) {
            return;
        }
        int clampedCat = clampInt(categoryIndex, 0, categories.size() - 1);
        List<UiTab> tabs = categories.get(clampedCat).tabs();
        int clampedTab = tabs.isEmpty() ? 0 : clampInt(tabIndex, 0, tabs.size() - 1);
        this.selectedCategoryIndex = clampedCat;
        this.selectedTabIndex = clampedTab;
        UiNavigationMemory nav = config.navigationMemory();
        nav.setSelectedCategoryIndex(clampedCat);
        nav.rememberTab(clampedCat, clampedTab);
        state.dashboardOpen = false;
        state.openDetailCardId = null;
        state.contentCascadeProgress = 0f;
    }

    public void openCardDetailImmediate(String cardId) {
        UiCatalog catalog = config.catalog();
        if (catalog.findCard(cardId) == null) {
            return;
        }
        state.dashboardOpen = false;
        state.openDetailCardId = cardId;
        state.renderedDetailCardId = cardId;
        state.detailProgress = 1f;
        state.detailOpenTime = state.elapsedSeconds;
        state.detailScroller.reset();
        state.detailMasterToggleAnim = catalog.isEnabled(cardId) ? 1f : 0f;
    }

    @Override
    public UiTheme theme() {
        return config.theme();
    }

    @Override
    public UiSoundSink sounds() {
        return config.sounds();
    }

    @Override
    public float elapsedSeconds() {
        return state.elapsedSeconds;
    }

    @Override
    protected void init() {
        super.init();
        if (!state.playedOpenSound) {
            state.playedOpenSound = true;
            UiSoundSink soundSink = config.sounds();
            soundSink.playOpen();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return config.pauseGame();
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    public boolean isListeningForBind() {
        return state.listeningBindId != null;
    }

    @Override
    public boolean isInputCaptured() {
        UiCustomSection custom = activeCustomSection();
        boolean customFocused = custom != null && custom.isTextInputFocused();
        return state.searchFocused || customFocused || state.focusedTextField != null || state.listeningBindId != null;
    }

    public boolean onRawKeyForBind(int keyCode) {
        String activeBindId = state.listeningBindId;
        if (activeBindId == null) {
            return false;
        }
        UiKeybindProvider provider = config.keybinds();
        if (provider == null) {
            state.listeningBindId = null;
            return false;
        }
        UiCatalog catalog = config.catalog();
        UiSoundSink soundSink = config.sounds();
        if (keyCode == InputConstants.KEY_ESCAPE || keyCode == InputConstants.KEY_BACKSPACE || keyCode == InputConstants.KEY_DELETE) {
            provider.setBind(activeBindId, UiInputBind.NONE);
            state.listeningBindId = null;
            showToast("Cleared " + catalog.cardTitle(activeBindId) + " bind");
            soundSink.playClick(0.92f);
            triggerAutoSave();
            return true;
        }
        if (config.isCancelBindKey().test(keyCode)) {
            state.listeningBindId = null;
            soundSink.playClick(0.95f);
            return true;
        }
        if (keyCode > 0) {
            UiInputBind bind = UiInputBind.key(keyCode);
            provider.setBind(activeBindId, bind);
            state.listeningBindId = null;
            showToast("Bound " + catalog.cardTitle(activeBindId) + " to " + bind.formatLabel());
            soundSink.playNotify();
            triggerAutoSave();
            return true;
        }
        return true;
    }

    public boolean onRawMouseButtonForBind(int button) {
        String activeBindId = state.listeningBindId;
        if (activeBindId == null) {
            return false;
        }
        UiKeybindProvider provider = config.keybinds();
        if (provider == null) {
            state.listeningBindId = null;
            return false;
        }
        if (button == InputConstants.MOUSE_BUTTON_MIDDLE || button >= InputConstants.MOUSE_BUTTON_4) {
            UiInputBind bind = UiInputBind.mouse(button);
            provider.setBind(activeBindId, bind);
            state.listeningBindId = null;
            UiCatalog catalog = config.catalog();
            showToast("Bound " + catalog.cardTitle(activeBindId) + " to " + bind.formatLabel());
            UiSoundSink soundSink = config.sounds();
            soundSink.playNotify();
            triggerAutoSave();
            return true;
        }
        return false;
    }

    @Override
    public void syncTextInput() {
        if (this.minecraft == null) {
            return;
        }
        UiCustomSection custom = activeCustomSection();
        boolean customFocused = custom != null && custom.isTextInputFocused();
        if ((customFocused || state.focusedTextField != null) && state.searchFocused) {
            state.searchFocused = false;
        }
        var tim = this.minecraft.textInputManager();
        if (state.searchFocused || customFocused || state.focusedTextField != null) {
            tim.startTextInput(this);
        } else {
            tim.stopTextInput(this);
        }
    }

    private void updateSearchFocus(boolean focused) {
        if (state.searchFocused == focused) {
            return;
        }
        state.searchFocused = focused;
        if (focused) {
            clearAllCustomSectionFocus();
            clearFocusedTextField();
        }
        syncTextInput();
    }

    private void clearFocusedTextField() {
        if (state.focusedTextField != null) {
            state.focusedTextField.focused = false;
            state.focusedTextField = null;
        }
    }

    private void clearAllCustomSectionFocus() {
        UiCustomSection custom = activeCustomSection();
        if (custom != null) {
            custom.clearFocus(this);
        }
    }

    private UiCustomSection activeCustomSection() {
        if (state.dashboardOpen) {
            return null;
        }
        UiCatalog catalog = config.catalog();
        String detailId = state.openDetailCardId;
        if (detailId != null) {
            UiCard detailCard = catalog.findCard(detailId);
            return detailCard != null ? detailCard.customSection() : null;
        }
        if (!state.searchQuery.isBlank()) {
            return null;
        }
        List<UiCategory> categories = catalog.categories();
        if (selectedCategoryIndex < 0 || selectedCategoryIndex >= categories.size()) {
            return null;
        }
        List<UiTab> tabs = categories.get(selectedCategoryIndex).tabs();
        if (selectedTabIndex < 0 || selectedTabIndex >= tabs.size()) {
            return null;
        }
        return tabs.get(selectedTabIndex).customSection();
    }

    public void requestAnimatedClose() {
        if (!state.closing) {
            state.closing = true;
            state.listeningBindId = null;
            state.searchFocused = false;
            clearFocusedTextField();
            clearAllCustomSectionFocus();
            syncTextInput();
            UiSoundSink soundSink = config.sounds();
            soundSink.playClose();
            triggerAutoSave();
            GUI.notifyGuiClosed();
        }
    }

    @Override
    public void requestClose() {
        requestAnimatedClose();
    }

    @Override
    public void closeOverlays() {
        state.dashboardOpen = false;
        state.openDetailCardId = null;
        clearFocusedTextField();
        clearAllCustomSectionFocus();
        syncTextInput();
    }

    @Override
    public void refreshDashboardAnimation() {
        state.dashboardOpenTime = state.elapsedSeconds;
    }

    @Override
    public void openCardDetail(String cardId, float localX, float localY) {
        UiCatalog catalog = config.catalog();
        if (catalog.findCard(cardId) == null) {
            return;
        }
        state.dashboardOpen = false;
        state.openDetailCardId = cardId;
        state.detailOpenTime = state.elapsedSeconds;
        state.detailScroller.reset();
        updateSearchFocus(false);
        clearFocusedTextField();
        clearAllCustomSectionFocus();
        state.detailMasterToggleAnim = catalog.isEnabled(cardId) ? 1f : 0f;
        UiTheme uiTheme = config.theme();
        spawnRipple(localX, localY, 26f, uiTheme.accentColor());
        UiSoundSink soundSink = config.sounds();
        soundSink.playClick(1.10f);
    }

    @Override
    public void showToast(String message) {
        UiTheme uiTheme = config.theme();
        if (!uiTheme.toastsEnabled() || message == null || message.isEmpty()) {
            return;
        }
        state.toastText = message;
        state.toastTimer = 2.1f;
        state.toastCreatedSec = state.elapsedSeconds;
    }

    @Override
    public void spawnRipple(float x, float y, float maxRadius, int color) {
        if (state.ripples.size() < 16) {
            state.ripples.add(new UiRipple(x, y, maxRadius, color, 0f));
        }
    }

    protected Screen resolveCloseTargetScreen() {
        Screen parent = config.parentScreen();
        if (parent instanceof TitleScreen) {
            return Menu.isCustomMenuEnabled() ? new MarlowTitleScreen() : new TitleScreen();
        }
        return parent;
    }

    @Override
    public void onClose() {
        state.listeningBindId = null;
        state.searchFocused = false;
        clearFocusedTextField();
        clearAllCustomSectionFocus();
        syncTextInput();
        UiNotificationOverlay notifs = config.notifications();
        if (notifs == null || !notifs.hasActiveNotifications()) {
            StreamproofOverlay.hide();
        }
        triggerAutoSave();
        GUI.notifyGuiClosed();
        if (this.minecraft != null) {
            var gui = this.minecraft.gui;
            gui.setScreen(resolveCloseTargetScreen());
        }
    }

    @Override
    public void removed() {
        if (this.minecraft != null) {
            var tim = this.minecraft.textInputManager();
            tim.stopTextInput(this);
        }
        UiNotificationOverlay notifs = config.notifications();
        if (notifs == null || !notifs.hasActiveNotifications()) {
            StreamproofOverlay.hide();
        }
        super.removed();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        if (StreamproofOverlay.shouldUseExternalOverlay()) {
            return;
        }
        UiTheme uiTheme = config.theme();
        if (this.minecraft != null && this.minecraft.level == null) {
            if (!Menu.isCustomMenuEnabled() || "Vanilla Panorama".equalsIgnoreCase(Menu.getBackgroundMode())) {
                extractPanorama(graphics, a);
            } else if (Menu.shouldReplacePanorama() && PastelShaderBackground.render()) {
                graphics.blit(PastelShaderBackground.TEXTURE_ID, 0, 0, this.width, this.height, 0.0f, 1.0f, 1.0f, 0.0f);
            } else {
                Color top = Menu.getPrimaryColor();
                Color bot = Menu.getSecondaryColor();
                graphics.fillGradient(0, 0, this.width, this.height, top.getRGB(), bot.getRGB());
            }
        } else if (uiTheme.blurEnabled()) {
            extractBlurredBackground(graphics);
        }
        float eased = UiRenderPrimitives.easeOutCubic(state.openProgress);
        int scrimAlpha = uiTheme.backdropScrimAlpha(eased);
        if (scrimAlpha > 0) {
            graphics.fill(0, 0, this.width, this.height, UiRenderPrimitives.argb(scrimAlpha, 12, 12, 14));
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        UiCatalog catalog = config.catalog();
        catalog.syncAllFromBacking();

        long now = System.nanoTime();
        float rawDt = Math.max(0L, now - lastFrameNanos) / 1_000_000_000f;
        lastFrameNanos = now;
        UiTheme uiTheme = config.theme();
        float speedScale = uiTheme.animationSpeedScale();
        float dt = clamp(rawDt, 0.0005f, 0.05f);
        float animDt = dt * speedScale;
        state.elapsedSeconds += dt;

        if (!state.closing) {
            state.openProgress = Math.min(1f, state.openProgress + animDt * 4.8f);
        } else {
            state.openProgress = Math.max(0f, state.openProgress - animDt * 7.2f);
            if (state.openProgress <= 0.01f) {
                UiNotificationOverlay notifs = config.notifications();
                if (notifs == null || !notifs.hasActiveNotifications()) {
                    StreamproofOverlay.hide();
                }
                if (this.minecraft != null) {
                    var gui = this.minecraft.gui;
                    gui.setScreen(resolveCloseTargetScreen());
                }
                return;
            }
        }

        if (state.dashboardOpen) {
            state.dashboardProgress = Math.min(1f, state.dashboardProgress + animDt * 6.4f);
        } else {
            state.dashboardProgress = Math.max(0f, state.dashboardProgress - animDt * 7.0f);
        }

        if (state.openDetailCardId != null && !state.dashboardOpen) {
            state.renderedDetailCardId = state.openDetailCardId;
            state.detailProgress = Math.min(1f, state.detailProgress + animDt * 6.2f);
        } else {
            state.detailProgress = Math.max(0f, state.detailProgress - animDt * 6.8f);
            if (state.detailProgress <= 0.01f) {
                state.renderedDetailCardId = null;
            }
        }

        state.subtabScroller.update(animDt, dt);
        state.detailScroller.update(animDt, dt);
        state.searchScroller.update(animDt, dt);
        state.searchKeyPulse = UiRenderPrimitives.approachExp(state.searchKeyPulse, 0f, 12f, dt);

        state.contentCascadeProgress = Math.min(1f, state.contentCascadeProgress + animDt * 3.6f);
        if (state.saveConfirmTimer > 0f) {
            state.saveConfirmTimer = Math.max(0f, state.saveConfirmTimer - dt);
        }
        if (state.toastTimer > 0f) {
            state.toastTimer = Math.max(0f, state.toastTimer - dt);
        }
        state.toastAnim = UiRenderPrimitives.approachExp(state.toastAnim, state.toastTimer > 0f ? 1f : 0f, 14f, animDt);
        state.sidebarExpandProgress = UiRenderPrimitives.approachExp(
            state.sidebarExpandProgress,
            state.sidebarExpanded ? 1f : 0f,
            16f,
            animDt
        );

        float panelW = GlassUiChrome.effectivePanelWidth(config, state);
        currentPanelWidth = panelW;
        float panelH = config.panelHeight();
        float fitW = (this.width - 14f) / panelW;
        float fitH = (this.height - 12f) / panelH;
        float maxFit = Math.min(fitW, fitH);
        float baseFit = Math.min(1.22f, maxFit);
        currentUiScale = clamp(baseFit * uiTheme.uiSizeScale(), 0.42f, Math.max(0.42f, maxFit));

        float openScale;
        if (state.closing) {
            openScale = 0.94f + 0.06f * UiRenderPrimitives.easeOutCubic(state.openProgress);
        } else {
            MotionCurve curve = uiTheme.motionCurve();
            openScale = switch (curve) {
                case LINEAR -> 0.95f + 0.05f * state.openProgress;
                case EASE -> 0.93f + 0.07f * UiRenderPrimitives.easeOutCubic(state.openProgress);
                case SNAPPY -> 0.96f + 0.04f * UiRenderPrimitives.easeOutCubic(Math.min(1f, state.openProgress * 1.25f));
                case SPRING -> 0.92f + 0.08f * UiRenderPrimitives.easeOutBack(state.openProgress, 0.95f);
            };
        }
        float panelAlpha = UiRenderPrimitives.easeOutCubic(state.openProgress);
        float slideOffsetY = (1f - panelAlpha) * 9f;

        currentRenderScale = currentUiScale * openScale;
        currentCenterX = this.width * 0.5f;
        currentCenterY = this.height * 0.5f + slideOffsetY;

        float lx = toLocalX(mouseX);
        float ly = toLocalY(mouseY);

        state.clearHitboxes();
        List<UiCategory> categories = catalog.categories();
        if (selectedCategoryIndex >= 0 && selectedCategoryIndex < categories.size()) {
            List<UiTab> tabs = categories.get(selectedCategoryIndex).tabs();
            if (selectedTabIndex >= 0 && selectedTabIndex < tabs.size()) {
                UiCustomSection tabSection = tabs.get(selectedTabIndex).customSection();
                if (tabSection != null) {
                    tabSection.clearHitboxes();
                }
            }
        }
        if (state.renderedDetailCardId != null) {
            UiCard detCard = catalog.findCard(state.renderedDetailCardId);
            if (detCard != null && detCard.customSection() != null) {
                UiCustomSection detSection = detCard.customSection();
                detSection.clearHitboxes();
            }
        }

        boolean streamproofCaptured = StreamproofOverlay.beginFrame(
            this.minecraft,
            this.width,
            this.height,
            currentCenterX,
            currentCenterY,
            currentRenderScale,
            panelW,
            panelH
        );

        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(currentCenterX, currentCenterY);
        pose.scale(currentRenderScale, currentRenderScale);
        pose.translate(-panelW * 0.5f, -panelH * 0.5f);

        try {
            GlassUiChrome.renderWindowSurface(graphics, config, state, panelAlpha);
            GlassUiChrome.renderLeftRail(graphics, config, state, selectedCategoryIndex, lx, ly, animDt, panelAlpha);
            GlassUiChrome.renderHeader(graphics, this.minecraft, config, state, selectedCategoryIndex, selectedTabIndex, lx, ly, animDt, panelAlpha);

            int scissorLeft = GlassUiChrome.contentScissorLeft(config, state);
            UiRenderPrimitives.enableScissor(graphics, scissorLeft, 44, (int) (panelW - 6f), (int) (panelH - 8f));
            String fsId = state.renderedDetailCardId;
            if (state.dashboardProgress > 0.01f && config.dashboard() != null) {
                float pAlpha = UiRenderPrimitives.easeOutCubic(state.dashboardProgress);
                if (pAlpha < 0.99f) {
                    float bgAlpha = panelAlpha * (1f - pAlpha);
                    if (!state.searchQuery.isBlank()) {
                        GlassUiCards.renderSearchResults(graphics, config, state, -999f, -999f, animDt, bgAlpha, false);
                    } else {
                        GlassUiCards.renderActiveTab(graphics, config, state, this, selectedCategoryIndex, selectedTabIndex, -999f, -999f, animDt, bgAlpha, false);
                    }
                }
                GlassUiDashboardView.render(graphics, this.minecraft, config, state, lx, ly, animDt, panelAlpha * pAlpha, state.dashboardOpen);
            } else if (fsId != null && state.detailProgress > 0.01f) {
                float fsAlpha = UiRenderPrimitives.easeOutCubic(state.detailProgress);
                if (fsAlpha < 0.99f) {
                    float bgAlpha = panelAlpha * (1f - fsAlpha);
                    if (!state.searchQuery.isBlank()) {
                        GlassUiCards.renderSearchResults(graphics, config, state, -999f, -999f, animDt, bgAlpha, false);
                    } else {
                        GlassUiCards.renderActiveTab(graphics, config, state, this, selectedCategoryIndex, selectedTabIndex, -999f, -999f, animDt, bgAlpha, false);
                    }
                }
                GlassUiDetailView.render(graphics, config, state, this, fsId, lx, ly, animDt, panelAlpha * fsAlpha, state.openDetailCardId != null);
            } else if (!state.searchQuery.isBlank()) {
                GlassUiCards.renderSearchResults(graphics, config, state, lx, ly, animDt, panelAlpha, true);
            } else {
                GlassUiCards.renderActiveTab(graphics, config, state, this, selectedCategoryIndex, selectedTabIndex, lx, ly, animDt, panelAlpha, true);
            }
            UiRenderPrimitives.disableScissor(graphics);

            GlassUiChrome.renderRipples(graphics, state, dt, panelAlpha);
            UiNotificationOverlay notifs = config.notifications();
            if (notifs != null) {
                notifs.renderInsidePanel(graphics, panelW, panelH, panelAlpha);
            }
            GlassUiChrome.renderToast(graphics, config, state, panelAlpha);
        } finally {
            pose.popMatrix();
            if (streamproofCaptured) {
                StreamproofOverlay.endFrame();
            }
        }
    }

    private SmoothScroller activeViewScroller() {
        if (state.dashboardOpen) {
            return null;
        }
        if (state.openDetailCardId != null) {
            return state.detailScroller;
        }
        if (!state.searchQuery.isBlank()) {
            return state.searchScroller;
        }
        return state.subtabScroller;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        SmoothScroller scroller = activeViewScroller();
        if (scroller != null) {
            scroller.onWheel((float) (-scrollY * 28.0));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (state.closing) {
            return true;
        }
        int btn = event.button();
        float lx = toLocalX((float) event.x());
        float ly = toLocalY((float) event.y());
        UiCatalog catalog = config.catalog();
        UiSoundSink soundSink = config.sounds();
        UiTheme uiTheme = config.theme();

        String activeBindId = state.listeningBindId;
        if (activeBindId != null) {
            UiKeybindProvider provider = config.keybinds();
            if (provider != null && (btn == InputConstants.MOUSE_BUTTON_MIDDLE || btn >= InputConstants.MOUSE_BUTTON_4)) {
                UiInputBind bind = UiInputBind.mouse(btn);
                provider.setBind(activeBindId, bind);
                state.listeningBindId = null;
                showToast("Bound " + catalog.cardTitle(activeBindId) + " to " + bind.formatLabel());
                soundSink.playNotify();
                triggerAutoSave();
                return true;
            }
            if (provider != null && btn == InputConstants.MOUSE_BUTTON_RIGHT) {
                provider.setBind(activeBindId, UiInputBind.NONE);
                state.listeningBindId = null;
                showToast("Cleared " + catalog.cardTitle(activeBindId) + " bind");
                soundSink.playClick(0.92f);
                triggerAutoSave();
                return true;
            }
            if (btn == InputConstants.MOUSE_BUTTON_LEFT) {
                for (int i = state.bindBadgeHitboxes.size() - 1; i >= 0; i--) {
                    UiScreenState.IdHit entry = state.bindBadgeHitboxes.get(i);
                    if (entry.rect().contains(lx, ly)) {
                        if (entry.id().equals(activeBindId)) {
                            state.listeningBindId = null;
                            soundSink.playClick(0.95f);
                        } else {
                            state.listeningBindId = entry.id();
                            spawnRipple(lx, ly, 18f, uiTheme.accentColor());
                            showToast("Press key or MB4/MB5 for " + catalog.cardTitle(entry.id()));
                            soundSink.playClick(1.14f);
                        }
                        return true;
                    }
                }
            }
            state.listeningBindId = null;
            soundSink.playClick(0.95f);
            return true;
        }

        if (btn == InputConstants.MOUSE_BUTTON_RIGHT) {
            UiKeybindProvider provider = config.keybinds();
            if (provider != null) {
                for (int i = state.bindBadgeHitboxes.size() - 1; i >= 0; i--) {
                    UiScreenState.IdHit entry = state.bindBadgeHitboxes.get(i);
                    if (entry.rect().contains(lx, ly)) {
                        provider.setBind(entry.id(), UiInputBind.NONE);
                        showToast("Cleared " + catalog.cardTitle(entry.id()) + " bind");
                        soundSink.playClick(0.92f);
                        triggerAutoSave();
                        return true;
                    }
                }
            }
            for (UiScreenState.ControlHit<UiControl.Cycle> entry : state.cycleRowHitboxes) {
                if (entry.rect().contains(lx, ly)) {
                    UiControl.Cycle cycle = entry.control();
                    cycle.step(-1);
                    cycle.animPress = 1f;
                    notifyControlChanged(cycle);
                    soundSink.playTab(0.96f);
                    triggerAutoSave();
                    return true;
                }
            }
            for (UiScreenState.ControlHit<UiControl.ColorPicker> entry : state.colorSwatchHitboxes) {
                if (entry.rect().contains(lx, ly)) {
                    UiControl.ColorPicker cp = entry.control();
                    cp.cycleVibrance();
                    notifyControlChanged(cp);
                    soundSink.playTab(1.06f);
                    triggerAutoSave();
                    return true;
                }
            }
            UiCustomSection customRight = activeCustomSection();
            if (customRight != null && customRight.onMouseClicked(this, lx, ly, btn)) {
                return true;
            }
            if (state.dashboardOpen) {
                state.dashboardOpen = false;
                soundSink.playTab(0.94f);
                return true;
            }
            if (state.openDetailCardId != null) {
                state.openDetailCardId = null;
                clearFocusedTextField();
                clearAllCustomSectionFocus();
                syncTextInput();
                soundSink.playTab(0.96f);
                return true;
            }
            for (int i = state.detailOpenHitboxes.size() - 1; i >= 0; i--) {
                UiScreenState.IdHit entry = state.detailOpenHitboxes.get(i);
                if (entry.rect().contains(lx, ly) && catalog.findCard(entry.id()) != null) {
                    openCardDetail(entry.id(), lx, ly);
                    return true;
                }
            }
            return super.mouseClicked(event, doubleClick);
        }

        if (btn != InputConstants.MOUSE_BUTTON_LEFT) {
            UiCustomSection customOther = activeCustomSection();
            if (customOther != null && customOther.onMouseClicked(this, lx, ly, btn)) {
                return true;
            }
            return super.mouseClicked(event, doubleClick);
        }

        SmoothScroller activeScroller = activeViewScroller();
        if (activeScroller != null
            && activeScroller.maxScroll() > 1f
            && activeScroller.trackW > 0f
            && lx >= activeScroller.trackX - 5f
            && lx <= activeScroller.trackX + activeScroller.trackW + 5f
            && ly >= activeScroller.trackY - 4f
            && ly <= activeScroller.trackY + activeScroller.trackH + 4f) {
            state.activeDraggingScroller = activeScroller;
            activeScroller.dragging = true;
            activeScroller.dragToLocalY(ly);
            soundSink.playClick(1.05f);
            return true;
        }

        if (config.dashboard() != null && state.avatarHitbox.contains(lx, ly)) {
            state.dashboardOpen = !state.dashboardOpen;
            if (state.dashboardOpen) {
                state.openDetailCardId = null;
                updateSearchFocus(false);
                clearFocusedTextField();
                clearAllCustomSectionFocus();
                state.dashboardOpenTime = state.elapsedSeconds;
                soundSink.playNotify();
            } else {
                soundSink.playTab(0.95f);
            }
            spawnRipple(lx, ly, 22f, uiTheme.accentColor());
            return true;
        }

        if (config.searchEnabled()) {
            if (!state.searchQuery.isEmpty() && state.searchClearHitbox.contains(lx, ly)) {
                state.searchQuery = "";
                state.searchScroller.reset();
                state.contentCascadeProgress = 0f;
                soundSink.playClick(1.05f);
                return true;
            }
            if (state.searchHitbox.contains(lx, ly)) {
                state.dashboardOpen = false;
                state.openDetailCardId = null;
                updateSearchFocus(true);
                soundSink.playTab(1.08f);
                return true;
            } else if (state.searchFocused) {
                updateSearchFocus(false);
            }
        }

        if (state.sidebarToggleHitbox.contains(lx, ly)) {
            toggleSidebar();
            spawnRipple(lx, ly, 18f, uiTheme.accentColor());
            soundSink.playTab(state.sidebarExpanded ? 1.08f : 0.94f);
            return true;
        }

        if (state.powerHitbox.contains(lx, ly)) {
            spawnRipple(lx, ly, 18f, 0xFFFF8880);
            requestAnimatedClose();
            return true;
        }

        if (config.showSaveButton() && config.onSave() != null && state.saveHitbox.contains(lx, ly)) {
            Runnable saveHook = config.onSave();
            saveHook.run();
            state.saveConfirmTimer = 1.4f;
            spawnRipple(lx, ly, 18f, uiTheme.accentColor());
            showToast(config.saveToastMessage());
            soundSink.playNotify();
            return true;
        }

        if (config.keybinds() != null) {
            for (int i = state.bindBadgeHitboxes.size() - 1; i >= 0; i--) {
                UiScreenState.IdHit entry = state.bindBadgeHitboxes.get(i);
                if (entry.rect().contains(lx, ly)) {
                    state.listeningBindId = entry.id();
                    spawnRipple(lx, ly, 18f, uiTheme.accentColor());
                    showToast("Press key or MB4/MB5 for " + catalog.cardTitle(entry.id()));
                    soundSink.playClick(1.14f);
                    return true;
                }
            }
        }

        for (UiScreenState.IndexedHit entry : state.categoryHitboxes) {
            if (entry.rect().contains(lx, ly)) {
                int idx = entry.index();
                state.dashboardOpen = false;
                state.openDetailCardId = null;
                state.searchQuery = "";
                clearFocusedTextField();
                clearAllCustomSectionFocus();
                syncTextInput();
                state.subtabScroller.reset();
                selectedCategoryIndex = idx;
                UiNavigationMemory nav = config.navigationMemory();
                nav.setSelectedCategoryIndex(idx);
                List<UiCategory> cats = catalog.categories();
                int tabCount = 0;
                if (idx >= 0 && idx < cats.size()) {
                    UiCategory picked = cats.get(idx);
                    tabCount = picked.tabs().size();
                }
                selectedTabIndex = nav.tabForCategory(idx, tabCount);
                state.contentCascadeProgress = 0f;
                soundSink.playTab(1.02f);
                return true;
            }
        }

        for (UiScreenState.IndexedHit entry : state.subtabHitboxes) {
            if (entry.rect().contains(lx, ly)) {
                int idx = entry.index();
                state.dashboardOpen = false;
                state.openDetailCardId = null;
                state.searchQuery = "";
                clearFocusedTextField();
                clearAllCustomSectionFocus();
                syncTextInput();
                state.subtabScroller.reset();
                selectedTabIndex = idx;
                UiNavigationMemory nav = config.navigationMemory();
                nav.rememberTab(selectedCategoryIndex, idx);
                state.contentCascadeProgress = 0f;
                soundSink.playTab(1.10f);
                return true;
            }
        }

        if (state.dashboardOpen) {
            if (state.dashboardBackHitbox.contains(lx, ly)) {
                state.dashboardOpen = false;
                soundSink.playTab(0.95f);
                return true;
            }
            for (UiScreenState.ActionHit entry : state.dashboardActionHitboxes) {
                if (entry.rect().contains(lx, ly)) {
                    UiDashboardAction act = entry.action();
                    UiDashboardAction.ClickHandler handler = act.onClick();
                    if (handler != null) {
                        handler.handle(this, lx, ly);
                    }
                    return true;
                }
            }
            return true;
        }

        String activeFsId = state.openDetailCardId;
        if (activeFsId != null) {
            if (state.detailBackHitbox.contains(lx, ly)) {
                state.openDetailCardId = null;
                clearFocusedTextField();
                clearAllCustomSectionFocus();
                syncTextInput();
                soundSink.playTab(0.96f);
                return true;
            }
            if (state.detailMasterHitbox.contains(lx, ly)) {
                UiCard card = catalog.findCard(activeFsId);
                if (card != null) {
                    if (card.onAction() != null && !card.toggleable()) {
                        Runnable act = card.onAction();
                        act.run();
                        showToast("Triggered " + card.title());
                    } else if (card.toggleable()) {
                        boolean next = !card.enabled();
                        catalog.setEnabled(activeFsId, next, true);
                        notifyCardToggled(card, next);
                        spawnRipple(lx, ly, 20f, next ? uiTheme.accentColor() : 0xFFFFFFFF);
                    }
                }
                return true;
            }
            for (UiScreenState.DetailModeHit entry : state.detailModeHitboxes) {
                if (entry.rect().contains(lx, ly)) {
                    catalog.setChoice(entry.cardId(), entry.choiceIndex(), true);
                    UiCard card = catalog.findCard(entry.cardId());
                    if (card != null) {
                        notifyChoiceChanged(card, entry.choiceIndex());
                    }
                    spawnRipple(lx, ly, 18f, 0xFFFFFFFF);
                    soundSink.playTab(1.14f);
                    triggerAutoSave();
                    return true;
                }
            }
            if (handleInlineControlClick(lx, ly)) {
                return true;
            }
            if (state.focusedTextField != null) {
                clearFocusedTextField();
                syncTextInput();
            }
            UiCustomSection detSection = activeCustomSection();
            if (detSection != null && detSection.onMouseClicked(this, lx, ly, btn)) {
                return true;
            }
            return true;
        }

        UiCustomSection tabSection = activeCustomSection();
        if (tabSection != null && tabSection.onMouseClicked(this, lx, ly, btn)) {
            return true;
        }

        for (UiScreenState.CardHit entry : state.searchResultActions) {
            if (entry.rect().contains(lx, ly)) {
                UiCard card = entry.card();
                if (card.toggleable()) {
                    boolean next = !card.enabled();
                    catalog.setEnabled(card.id(), next, true);
                    notifyCardToggled(card, next);
                    spawnRipple(lx, ly, 22f, next ? uiTheme.accentColor() : 0xFFFFFFFF);
                } else {
                    openCardDetail(card.id(), lx, ly);
                }
                return true;
            }
        }

        for (UiScreenState.CardHit entry : state.cardToggleHitboxes) {
            if (entry.rect().contains(lx, ly)) {
                UiCard card = entry.card();
                boolean next = !card.enabled();
                catalog.setEnabled(card.id(), next, true);
                notifyCardToggled(card, next);
                spawnRipple(lx, ly, 18f, next ? uiTheme.accentColor() : 0xFFFFFFFF);
                return true;
            }
        }

        for (UiScreenState.CardHit entry : state.instantActionHitboxes) {
            if (entry.rect().contains(lx, ly)) {
                UiCard card = entry.card();
                Runnable act = card.onAction();
                if (act != null) {
                    act.run();
                }
                spawnRipple(lx, ly, 20f, uiTheme.accentColor());
                showToast("Triggered " + card.title());
                return true;
            }
        }

        for (UiScreenState.IdHit entry : state.openOptionsBtnHitboxes) {
            if (entry.rect().contains(lx, ly)) {
                openCardDetail(entry.id(), lx, ly);
                return true;
            }
        }

        for (UiScreenState.CardChoiceHit entry : state.cardChoiceHitboxes) {
            if (entry.rect().contains(lx, ly)) {
                UiCard card = entry.card();
                int idx = entry.choiceIndex();
                catalog.setChoice(card.id(), idx, true);
                List<UiChoice> choices = card.choices();
                if (idx >= 0 && idx < choices.size()) {
                    choices.get(idx).animPress = 1f;
                }
                notifyChoiceChanged(card, idx);
                UiHitRect rect = entry.rect();
                spawnRipple(rect.x() + rect.w() * 0.5f, rect.y() + 13f, 20f, 0xFFFFFFFF);
                soundSink.playTab(1.14f);
                triggerAutoSave();
                return true;
            }
        }

        if (handleInlineControlClick(lx, ly)) {
            return true;
        }
        if (state.focusedTextField != null) {
            clearFocusedTextField();
            syncTextInput();
        }

        for (UiScreenState.CardHit entry : state.cardBodyHitboxes) {
            UiHitRect rect = entry.rect();
            if (rect.contains(lx, ly) && ly <= rect.y() + 40f) {
                UiCard card = entry.card();
                if (card.toggleable()) {
                    boolean next = !card.enabled();
                    catalog.setEnabled(card.id(), next, true);
                    notifyCardToggled(card, next);
                    spawnRipple(lx, ly, 20f, next ? uiTheme.accentColor() : 0xFFFFFFFF);
                } else {
                    openCardDetail(card.id(), lx, ly);
                }
                return true;
            }
        }

        return super.mouseClicked(event, doubleClick);
    }

    private boolean handleInlineControlClick(float lx, float ly) {
        UiSoundSink soundSink = config.sounds();
        UiTheme uiTheme = config.theme();

        for (UiScreenState.ControlHit<UiControl.TextField> entry : state.textFieldHitboxes) {
            if (entry.rect().contains(lx, ly)) {
                clearAllCustomSectionFocus();
                state.searchFocused = false;
                state.focusedTextField = entry.control();
                state.focusedTextField.focused = true;
                syncTextInput();
                soundSink.playTab(1.06f);
                return true;
            }
        }

        for (UiScreenState.ControlHit<UiControl.Toggle> entry : state.toggleRowActions) {
            if (entry.rect().contains(lx, ly)) {
                UiControl.Toggle toggle = entry.control();
                toggle.setValue(!toggle.enabled());
                toggle.animPress = 1f;
                notifyControlChanged(toggle);
                spawnRipple(lx, ly, 20f, toggle.enabled() ? uiTheme.accentColor() : 0xFFFFFFFF);
                soundSink.playClick(toggle.enabled() ? 1.14f : 0.92f);
                triggerAutoSave();
                return true;
            }
        }

        for (UiScreenState.TrackHit<UiControl.Slider> entry : state.sliderRowHitboxes) {
            if (entry.rect().contains(lx, ly)) {
                UiControl.Slider slider = entry.control();
                state.activeDraggingSlider = slider;
                state.activeSliderTrackX = entry.trackX();
                state.activeSliderTrackW = Math.max(1f, entry.trackW());
                float norm = clamp((lx - state.activeSliderTrackX) / state.activeSliderTrackW, 0f, 1f);
                slider.setFromNormalized(norm);
                notifyControlChanged(slider);
                soundSink.playTab(0.95f + norm * 0.25f);
                return true;
            }
        }

        for (UiScreenState.ControlHit<UiControl.Action> entry : state.actionButtonHitboxes) {
            if (entry.rect().contains(lx, ly)) {
                UiControl.Action action = entry.control();
                action.animPress = 1f;
                action.trigger();
                UiHitRect rect = entry.rect();
                spawnRipple(rect.x() + rect.w() * 0.5f, rect.y() + rect.h() * 0.5f, 22f, uiTheme.accentColor());
                if (!action.toastMessage().isEmpty()) {
                    showToast(action.toastMessage());
                }
                soundSink.playClick(1.12f);
                return true;
            }
        }

        for (UiScreenState.ControlHit<UiControl.Cycle> entry : state.cycleRowHitboxes) {
            if (entry.rect().contains(lx, ly)) {
                UiControl.Cycle cycle = entry.control();
                cycle.step(1);
                cycle.animPress = 1f;
                notifyControlChanged(cycle);
                spawnRipple(lx, ly, 18f, uiTheme.accentColor());
                soundSink.playTab(1.10f);
                triggerAutoSave();
                return true;
            }
        }

        for (UiScreenState.TrackHit<UiControl.RangeSlider> entry : state.rangeRowHitboxes) {
            if (entry.rect().contains(lx, ly)) {
                UiControl.RangeSlider range = entry.control();
                state.activeDraggingRange = range;
                state.activeSliderTrackX = entry.trackX();
                state.activeSliderTrackW = Math.max(1f, entry.trackW());
                float norm = clamp((lx - state.activeSliderTrackX) / state.activeSliderTrackW, 0f, 1f);
                float distLow = Math.abs(norm - range.lowNormalized());
                float distHigh = Math.abs(norm - range.highNormalized());
                state.activeDraggingRangeHigh = distHigh < distLow;
                range.setHandleFromNormalized(state.activeDraggingRangeHigh, norm);
                notifyControlChanged(range);
                soundSink.playTab(0.95f + norm * 0.25f);
                return true;
            }
        }

        for (UiScreenState.ControlHit<UiControl.ColorPicker> entry : state.colorSwatchHitboxes) {
            if (entry.rect().contains(lx, ly)) {
                UiControl.ColorPicker cp = entry.control();
                cp.cycleVibrance();
                notifyControlChanged(cp);
                spawnRipple(lx, ly, 16f, cp.color());
                soundSink.playTab(1.08f);
                triggerAutoSave();
                return true;
            }
        }

        for (UiScreenState.TrackHit<UiControl.ColorPicker> entry : state.colorHueHitboxes) {
            if (entry.rect().contains(lx, ly)) {
                UiControl.ColorPicker cp = entry.control();
                state.activeDraggingColor = cp;
                state.activeSliderTrackX = entry.trackX();
                state.activeSliderTrackW = Math.max(1f, entry.trackW());
                float norm = clamp((lx - state.activeSliderTrackX) / state.activeSliderTrackW, 0f, 1f);
                cp.setHueNormalized(norm);
                notifyControlChanged(cp);
                soundSink.playTab(0.95f + norm * 0.25f);
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        SmoothScroller draggingScroll = state.activeDraggingScroller;
        if (draggingScroll != null) {
            float ly = toLocalY((float) event.y());
            draggingScroll.dragToLocalY(ly);
            return true;
        }
        UiSoundSink soundSink = config.sounds();
        if (state.activeDraggingSlider != null) {
            float lx = toLocalX((float) event.x());
            float norm = clamp((lx - state.activeSliderTrackX) / Math.max(1f, state.activeSliderTrackW), 0f, 1f);
            state.activeDraggingSlider.setFromNormalized(norm);
            notifyControlChanged(state.activeDraggingSlider);
            soundSink.playSliderScrub(norm);
            return true;
        }
        if (state.activeDraggingRange != null) {
            float lx = toLocalX((float) event.x());
            float norm = clamp((lx - state.activeSliderTrackX) / Math.max(1f, state.activeSliderTrackW), 0f, 1f);
            state.activeDraggingRange.setHandleFromNormalized(state.activeDraggingRangeHigh, norm);
            notifyControlChanged(state.activeDraggingRange);
            soundSink.playSliderScrub(norm);
            return true;
        }
        if (state.activeDraggingColor != null) {
            float lx = toLocalX((float) event.x());
            float norm = clamp((lx - state.activeSliderTrackX) / Math.max(1f, state.activeSliderTrackW), 0f, 1f);
            state.activeDraggingColor.setHueNormalized(norm);
            notifyControlChanged(state.activeDraggingColor);
            soundSink.playSliderScrub(norm);
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (state.activeDraggingScroller != null) {
            state.activeDraggingScroller.dragging = false;
            state.activeDraggingScroller = null;
            return true;
        }
        if (state.activeDraggingSlider != null) {
            state.activeDraggingSlider = null;
            triggerAutoSave();
            return true;
        }
        if (state.activeDraggingRange != null) {
            state.activeDraggingRange = null;
            triggerAutoSave();
            return true;
        }
        if (state.activeDraggingColor != null) {
            state.activeDraggingColor = null;
            triggerAutoSave();
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        UiSoundSink soundSink = config.sounds();

        if (state.listeningBindId != null) {
            return onRawKeyForBind(key);
        }

        UiCustomSection activeCustom = activeCustomSection();

        if (key == InputConstants.KEY_ESCAPE) {
            if (state.focusedTextField != null) {
                clearFocusedTextField();
                syncTextInput();
                soundSink.playTab(0.96f);
                triggerAutoSave();
                return true;
            }
            if (activeCustom != null && activeCustom.isTextInputFocused()) {
                activeCustom.clearFocus(this);
                soundSink.playTab(0.96f);
                return true;
            }
            if (state.dashboardOpen) {
                state.dashboardOpen = false;
                soundSink.playTab(0.95f);
                return true;
            }
            if (state.openDetailCardId != null) {
                state.openDetailCardId = null;
                clearAllCustomSectionFocus();
                syncTextInput();
                soundSink.playTab(0.96f);
                return true;
            }
            if (config.smartEscape().getAsBoolean() && (state.searchFocused || !state.searchQuery.isEmpty())) {
                if (!state.searchQuery.isEmpty()) {
                    state.searchQuery = "";
                    state.searchScroller.reset();
                    state.contentCascadeProgress = 0f;
                }
                updateSearchFocus(false);
                soundSink.playTab(0.96f);
                return true;
            }
            requestAnimatedClose();
            return true;
        }

        if (state.focusedTextField != null) {
            UiControl.TextField tf = state.focusedTextField;
            String cur = tf.value();
            if (key == InputConstants.KEY_BACKSPACE) {
                if (!cur.isEmpty()) {
                    String next = (event.modifiers() & InputConstants.MOD_CONTROL) != 0 ? "" : cur.substring(0, cur.length() - 1);
                    tf.setValue(next);
                    tf.keyPulse = 1f;
                    notifyControlChanged(tf);
                    soundSink.playTypeKey();
                }
                return true;
            }
            if (key == InputConstants.KEY_DELETE) {
                tf.setValue("");
                tf.keyPulse = 1f;
                notifyControlChanged(tf);
                soundSink.playTypeKey();
                return true;
            }
            if (key == InputConstants.KEY_V && (event.modifiers() & InputConstants.MOD_CONTROL) != 0 && this.minecraft != null) {
                KeyboardHandler kh = this.minecraft.keyboardHandler;
                String clip = kh.getClipboard();
                if (clip != null && !clip.isEmpty()) {
                    String step1 = clip.replace("\r", "");
                    String step2 = step1.replace("\n", "");
                    String sanitized = step2.trim();
                    String merged = cur + sanitized;
                    tf.setValue(merged.length() > 64 ? merged.substring(0, 64) : merged);
                    tf.keyPulse = 1f;
                    notifyControlChanged(tf);
                    soundSink.playTypeKey();
                }
                return true;
            }
            if (key == InputConstants.KEY_RETURN || key == InputConstants.KEY_NUMPADENTER) {
                clearFocusedTextField();
                syncTextInput();
                soundSink.playClick(1.06f);
                triggerAutoSave();
                return true;
            }
            return true;
        }

        if (activeCustom != null && activeCustom.onKeyPressed(this, event)) {
            return true;
        }

        if (state.searchFocused) {
            if (key == InputConstants.KEY_BACKSPACE) {
                if (!state.searchQuery.isEmpty()) {
                    state.searchQuery = (event.modifiers() & InputConstants.MOD_CONTROL) != 0
                        ? ""
                        : state.searchQuery.substring(0, state.searchQuery.length() - 1);
                    state.searchScroller.reset();
                    state.searchKeyPulse = 1f;
                    soundSink.playTypeKey();
                    if (state.searchQuery.isEmpty()) {
                        state.contentCascadeProgress = 0f;
                    }
                }
                return true;
            }
            if (key == InputConstants.KEY_DELETE) {
                state.searchQuery = "";
                state.searchScroller.reset();
                state.searchKeyPulse = 1f;
                state.contentCascadeProgress = 0f;
                soundSink.playTypeKey();
                return true;
            }
            if (key == InputConstants.KEY_RETURN || key == InputConstants.KEY_NUMPADENTER) {
                updateSearchFocus(false);
                soundSink.playClick(1.06f);
                return true;
            }
            return true;
        }

        SmoothScroller activeScroll = activeViewScroller();
        if (activeScroll != null
            && (key == InputConstants.KEY_DOWN
                || key == InputConstants.KEY_UP
                || key == InputConstants.KEY_PAGEDOWN
                || key == InputConstants.KEY_PAGEUP)) {
            float step = switch (key) {
                case InputConstants.KEY_DOWN -> 44f;
                case InputConstants.KEY_UP -> -44f;
                case InputConstants.KEY_PAGEDOWN -> 140f;
                default -> -140f;
            };
            activeScroll.onWheel(step);
            return true;
        }

        BiPredicate<KeyEvent, UiScreenActions> unhandledHook = config.onUnhandledKeyPressed();
        if (unhandledHook != null && unhandledHook.test(event, this)) {
            return true;
        }

        if (config.isToggleCloseKey().test(event)) {
            requestAnimatedClose();
            return true;
        }

        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (state.listeningBindId != null) {
            return true;
        }
        int cp = event.codepoint();
        if (cp < 32 || cp == 127) {
            return false;
        }
        UiSoundSink soundSink = config.sounds();
        if (state.focusedTextField != null) {
            String ch = event.codepointAsString();
            String cur = state.focusedTextField.value();
            if (cur.length() < 64) {
                state.focusedTextField.setValue(cur + ch);
                state.focusedTextField.keyPulse = 1f;
                notifyControlChanged(state.focusedTextField);
                soundSink.playTypeKey();
                return true;
            }
            return false;
        }
        UiCustomSection activeCustom = activeCustomSection();
        if (activeCustom != null && activeCustom.onCharTyped(this, event)) {
            return true;
        }
        if (!state.searchFocused) {
            return super.charTyped(event);
        }
        String ch = event.codepointAsString();
        if ("`".equals(ch) && state.searchQuery.isEmpty()) {
            return true;
        }
        if (state.searchQuery.length() < 32) {
            state.dashboardOpen = false;
            state.openDetailCardId = null;
            state.searchQuery = state.searchQuery + ch;
            state.searchScroller.reset();
            state.searchKeyPulse = 1f;
            soundSink.playTypeKey();
            return true;
        }
        return false;
    }

    private void notifyCardToggled(UiCard card, boolean enabled) {
        BiConsumer<UiCard, Boolean> cb = config.onCardToggle();
        if (cb != null) {
            cb.accept(card, enabled);
        }
    }

    private void notifyChoiceChanged(UiCard card, int choiceIndex) {
        BiConsumer<UiCard, Integer> cb = config.onChoiceChange();
        if (cb != null) {
            cb.accept(card, choiceIndex);
        }
    }

    private void notifyControlChanged(UiControl control) {
        Consumer<UiControl> cb = config.onControlChange();
        if (cb != null) {
            cb.accept(control);
        }
    }

    private void triggerAutoSave() {
        Runnable hook = config.onAutoSave();
        if (hook != null) {
            hook.run();
        }
    }

    private float toLocalX(float screenX) {
        float scale = Math.max(0.01f, currentRenderScale);
        return (screenX - currentCenterX) / scale + currentPanelWidth * 0.5f;
    }

    private float toLocalY(float screenY) {
        float scale = Math.max(0.01f, currentRenderScale);
        return (screenY - currentCenterY) / scale + config.panelHeight() * 0.5f;
    }

    private static float clamp(float val, float min, float max) {
        return Math.max(min, Math.min(max, val));
    }

    private static int clampInt(int val, int min, int max) {
        return Math.max(min, Math.min(max, val));
    }
}
