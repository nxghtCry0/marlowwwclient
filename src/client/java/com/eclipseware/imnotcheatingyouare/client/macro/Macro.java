package com.eclipseware.imnotcheatingyouare.client.macro;

import java.util.ArrayList;
import java.util.List;

public class Macro {
    private String name;
    private List<MacroAction> actions = new ArrayList<>();
    private int keybind;
    private boolean enabled;
    private boolean holdMode;

    public Macro(String name) {
        this.name = name;
        this.enabled = true;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<MacroAction> getActions() { return actions; }
    public void addAction(MacroAction action) { actions.add(action); }
    public void removeAction(int index) { if (index >= 0 && index < actions.size()) actions.remove(index); }
    public void clear() { actions.clear(); }
    public int getKeybind() { return keybind; }
    public void setKeybind(int keybind) { this.keybind = keybind; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isHoldMode() { return holdMode; }
    public void setHoldMode(boolean holdMode) { this.holdMode = holdMode; }
}