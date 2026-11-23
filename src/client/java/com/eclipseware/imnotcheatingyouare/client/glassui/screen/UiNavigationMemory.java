package com.eclipseware.imnotcheatingyouare.client.glassui.screen;

import java.util.HashMap;
import java.util.Map;

public final class UiNavigationMemory {
    private int selectedCategoryIndex = 0;
    private boolean sidebarExpanded = false;
    private final Map<Integer, Integer> selectedTabByCategory = new HashMap<>();

    public int selectedCategoryIndex() {
        return selectedCategoryIndex;
    }

    public void setSelectedCategoryIndex(int selectedCategoryIndex) {
        this.selectedCategoryIndex = Math.max(0, selectedCategoryIndex);
    }

    public boolean sidebarExpanded() {
        return sidebarExpanded;
    }

    public void setSidebarExpanded(boolean sidebarExpanded) {
        this.sidebarExpanded = sidebarExpanded;
    }

    public boolean toggleSidebarExpanded() {
        this.sidebarExpanded = !this.sidebarExpanded;
        return this.sidebarExpanded;
    }

    public int tabForCategory(int categoryIndex, int tabCount) {
        if (tabCount <= 0) {
            return 0;
        }
        int saved = selectedTabByCategory.getOrDefault(categoryIndex, 0);
        return Math.max(0, Math.min(tabCount - 1, saved));
    }

    public void rememberTab(int categoryIndex, int tabIndex) {
        selectedTabByCategory.put(categoryIndex, Math.max(0, tabIndex));
    }
}
