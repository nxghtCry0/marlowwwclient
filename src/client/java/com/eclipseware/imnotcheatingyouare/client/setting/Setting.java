package com.eclipseware.imnotcheatingyouare.client.setting;

import com.eclipseware.imnotcheatingyouare.client.module.Module;
import java.util.ArrayList;

public class Setting {
    private String name;
    private Module parent;
    private String mode;
    private String sval;
    private ArrayList<String> options;
    private boolean bval;
    private double dval;
    private double min;
    private double max;
    private boolean onlyint = false;

    public Setting(String name, Module parent, String sval, ArrayList<String> options){
        this.name = name; this.parent = parent; this.sval = sval; this.options = options; this.mode = "Combo";
    }
    
    public Setting(String name, Module parent, boolean bval){
        this.name = name; this.parent = parent; this.bval = bval; this.mode = "Check";
    }
    
    public Setting(String name, Module parent, double dval, double min, double max, boolean onlyint){
        this.name = name; this.parent = parent; this.min = min; this.max = max; this.onlyint = onlyint; this.mode = "Slider";
        this.dval = clamp(dval);
    }
    
    public String getName() { return name; }
    public Module getParentMod() { return parent; }
    public String getValString() { return this.sval; }
    public void setValString(String in) { this.sval = in; }
    public ArrayList<String> getOptions() { return this.options; }
    public boolean getValBoolean() { return this.bval; }
    public void setValBoolean(boolean in) { this.bval = in; }
    public double getValDouble(){ return this.onlyint ? (int)dval : this.dval; }
    public void setValDouble(double in) { this.dval = clamp(in); }
    public double getMin() { return this.min; }
    public double getMax() { return this.max; }
    public boolean isCombo() { return this.mode.equalsIgnoreCase("Combo"); }
    public boolean isCheck() { return this.mode.equalsIgnoreCase("Check"); }
    public boolean isSlider() { return this.mode.equalsIgnoreCase("Slider"); }
    public boolean onlyInt() { return this.onlyint; }

    private double clamp(double value) {
        return Math.max(this.min, Math.min(this.max, value));
    }
}