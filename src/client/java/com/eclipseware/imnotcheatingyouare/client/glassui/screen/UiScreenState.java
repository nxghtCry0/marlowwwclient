package com.eclipseware.imnotcheatingyouare.client.glassui.screen;

import com.eclipseware.imnotcheatingyouare.client.glassui.model.UiCard;
import com.eclipseware.imnotcheatingyouare.client.glassui.model.UiControl;
import com.eclipseware.imnotcheatingyouare.client.glassui.model.UiDashboardAction;
import com.eclipseware.imnotcheatingyouare.client.glassui.model.UiHitRect;
import com.eclipseware.imnotcheatingyouare.client.glassui.render.SmoothScroller;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class UiScreenState {
    public record IndexedHit(int index, UiHitRect rect) {
    }

    public record IdHit(String id, UiHitRect rect) {
    }

    public record CardHit(UiCard card, UiHitRect rect) {
    }

    public record CardChoiceHit(UiCard card, int choiceIndex, UiHitRect rect) {
    }

    public record DetailModeHit(String cardId, int choiceIndex, UiHitRect rect) {
    }

    public record ActionHit(UiDashboardAction action, UiHitRect rect) {
    }

    public record ControlHit<T extends UiControl>(T control, UiHitRect rect) {
    }

    public record TrackHit<T extends UiControl>(T control, UiHitRect rect, float trackX, float trackW) {
    }

    public float elapsedSeconds = 0f;
    public float openProgress = 0f;
    public boolean closing = false;
    public float contentCascadeProgress = 0f;
    public boolean playedOpenSound = false;

    public String openDetailCardId = null;
    public String renderedDetailCardId = null;
    public float detailProgress = 0f;
    public float detailBackHover = 0f;
    public float detailMasterToggleAnim = 0f;
    public float detailMasterHover = 0f;
    public float detailOpenTime = 0f;

    public boolean dashboardOpen = false;
    public float dashboardProgress = 0f;
    public float dashboardOpenTime = 0f;
    public float dashboardBackHover = 0f;
    public final Map<Integer, Float> dashboardBtnHovers = new HashMap<>();

    public final SmoothScroller subtabScroller = new SmoothScroller();
    public final SmoothScroller detailScroller = new SmoothScroller();
    public final SmoothScroller searchScroller = new SmoothScroller();
    public SmoothScroller activeDraggingScroller = null;

    public float railIndicatorY = -1f;
    public float subtabUnderlineX = -1f;
    public float subtabUnderlineW = 40f;

    public boolean sidebarExpanded = false;
    public float sidebarExpandProgress = 0f;
    public float sidebarToggleHover = 0f;

    public float powerHover = 0f;
    public float saveHover = 0f;
    public float saveConfirmTimer = 0f;
    public float avatarHover = 0f;

    public boolean searchFocused = false;
    public float searchFocusAnim = 0f;
    public float searchHover = 0f;
    public String searchQuery = "";
    public float searchClearHover = 0f;
    public float searchCaretX = -1f;
    public float searchKeyPulse = 0f;

    public String lastHoveredModeKey = "";
    public float modeHoverStartSec = 0f;

    public String listeningBindId = null;

    public UiControl.Slider activeDraggingSlider = null;
    public UiControl.RangeSlider activeDraggingRange = null;
    public boolean activeDraggingRangeHigh = false;
    public UiControl.ColorPicker activeDraggingColor = null;
    public float activeSliderTrackX = 0f;
    public float activeSliderTrackW = 1f;

    public UiControl.TextField focusedTextField = null;

    public String toastText = "";
    public float toastTimer = 0f;
    public float toastAnim = 0f;
    public float toastCreatedSec = 0f;

    public final List<UiRipple> ripples = new ArrayList<>();

    public final List<IndexedHit> categoryHitboxes = new ArrayList<>();
    public final List<IndexedHit> subtabHitboxes = new ArrayList<>();
    public UiHitRect sidebarToggleHitbox = UiHitRect.EMPTY;
    public UiHitRect powerHitbox = UiHitRect.EMPTY;
    public UiHitRect searchHitbox = UiHitRect.EMPTY;
    public UiHitRect searchClearHitbox = UiHitRect.EMPTY;
    public UiHitRect saveHitbox = UiHitRect.EMPTY;
    public UiHitRect avatarHitbox = UiHitRect.EMPTY;

    public UiHitRect dashboardBackHitbox = UiHitRect.EMPTY;
    public final List<ActionHit> dashboardActionHitboxes = new ArrayList<>();

    public UiHitRect detailBackHitbox = UiHitRect.EMPTY;
    public UiHitRect detailMasterHitbox = UiHitRect.EMPTY;
    public final List<DetailModeHit> detailModeHitboxes = new ArrayList<>();

    public final List<CardHit> searchResultActions = new ArrayList<>();
    public final List<IdHit> bindBadgeHitboxes = new ArrayList<>();
    public final List<IdHit> detailOpenHitboxes = new ArrayList<>();
    public final List<IdHit> openOptionsBtnHitboxes = new ArrayList<>();
    public final List<CardHit> instantActionHitboxes = new ArrayList<>();
    public final List<CardHit> cardBodyHitboxes = new ArrayList<>();
    public final List<ControlHit<UiControl.Toggle>> toggleRowActions = new ArrayList<>();
    public final List<TrackHit<UiControl.Slider>> sliderRowHitboxes = new ArrayList<>();
    public final List<ControlHit<UiControl.Action>> actionButtonHitboxes = new ArrayList<>();
    public final List<ControlHit<UiControl.Cycle>> cycleRowHitboxes = new ArrayList<>();
    public final List<TrackHit<UiControl.RangeSlider>> rangeRowHitboxes = new ArrayList<>();
    public final List<TrackHit<UiControl.ColorPicker>> colorHueHitboxes = new ArrayList<>();
    public final List<ControlHit<UiControl.ColorPicker>> colorSwatchHitboxes = new ArrayList<>();
    public final List<ControlHit<UiControl.TextField>> textFieldHitboxes = new ArrayList<>();
    public final List<CardHit> cardToggleHitboxes = new ArrayList<>();
    public final List<CardChoiceHit> cardChoiceHitboxes = new ArrayList<>();

    public void clearHitboxes() {
        categoryHitboxes.clear();
        subtabHitboxes.clear();
        sidebarToggleHitbox = UiHitRect.EMPTY;
        powerHitbox = UiHitRect.EMPTY;
        searchHitbox = UiHitRect.EMPTY;
        searchClearHitbox = UiHitRect.EMPTY;
        saveHitbox = UiHitRect.EMPTY;
        avatarHitbox = UiHitRect.EMPTY;
        dashboardBackHitbox = UiHitRect.EMPTY;
        dashboardActionHitboxes.clear();
        detailBackHitbox = UiHitRect.EMPTY;
        detailMasterHitbox = UiHitRect.EMPTY;
        detailModeHitboxes.clear();
        searchResultActions.clear();
        bindBadgeHitboxes.clear();
        detailOpenHitboxes.clear();
        openOptionsBtnHitboxes.clear();
        instantActionHitboxes.clear();
        cardBodyHitboxes.clear();
        toggleRowActions.clear();
        sliderRowHitboxes.clear();
        actionButtonHitboxes.clear();
        cycleRowHitboxes.clear();
        rangeRowHitboxes.clear();
        colorHueHitboxes.clear();
        colorSwatchHitboxes.clear();
        textFieldHitboxes.clear();
        cardToggleHitboxes.clear();
        cardChoiceHitboxes.clear();
    }
}
