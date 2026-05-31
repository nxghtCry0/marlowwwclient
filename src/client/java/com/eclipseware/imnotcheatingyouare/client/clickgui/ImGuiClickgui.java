package com.eclipseware.imnotcheatingyouare.client.clickgui;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.utils.ImGuiManager;
import imgui.ImGui;
import imgui.ImGuiStyle;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;
import imgui.type.ImInt;
import imgui.type.ImString;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class ImGuiClickgui extends Screen {
    private Category selectedCategory = Category.Combat;
    private Module bindingModule = null;
    private final ImString searchVal = new ImString(64);
    private boolean themeApplied = false;

    public ImGuiClickgui() {
        super(Component.literal("ImGui ClickGUI"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void applyTheme() {
        if (themeApplied) return;
        ImGuiStyle style = ImGui.getStyle();
        style.setFrameRounding(4.0f);
        style.setGrabRounding(4.0f);
        style.setWindowRounding(6.0f);
        
        style.setColor(ImGuiCol.WindowBg, 20, 20, 25, 240);
        style.setColor(ImGuiCol.TitleBgActive, 45, 20, 90, 255);
        style.setColor(ImGuiCol.CheckMark, 155, 60, 255, 255);
        style.setColor(ImGuiCol.SliderGrabActive, 155, 60, 255, 255);
        style.setColor(ImGuiCol.SliderGrab, 120, 45, 200, 255);
        style.setColor(ImGuiCol.HeaderActive, 155, 60, 255, 255);
        style.setColor(ImGuiCol.HeaderHovered, 120, 45, 200, 255);
        style.setColor(ImGuiCol.ButtonActive, 155, 60, 255, 255);
        style.setColor(ImGuiCol.ButtonHovered, 120, 45, 200, 255);
        themeApplied = true;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0x88000000);

        if (!ImGuiManager.isInitialized()) {
            ImGuiManager.init();
        }

        applyTheme();

        ImGuiManager.getImplGlfw().newFrame();
        ImGui.newFrame();

        ImGui.setNextWindowSize(700, 450, ImGuiCond.FirstUseEver);
        
        if (ImGui.begin("Marlowww Client ClickGUI", ImGuiWindowFlags.NoCollapse)) {
            ImGui.columns(2, "ClickGUIColumns", true);
            ImGui.setColumnWidth(0, 150);

            ImGui.beginChild("CategoriesColumn");
            for (Category category : Category.values()) {
                if (ImGui.selectable(category.name(), selectedCategory == category)) {
                    selectedCategory = category;
                }
            }
            ImGui.endChild();

            ImGui.nextColumn();

            ImGui.beginChild("ModulesColumn");
            
            if (ImGui.inputText("Search", searchVal)) {
            }
            
            ImGui.separator();

            String searchQuery = searchVal.get().trim().toLowerCase();

            List<Module> modules = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModules(selectedCategory);
            for (Module m : modules) {
                if (m.isHidden()) continue;
                if (!searchQuery.isEmpty() && !m.getName().toLowerCase().contains(searchQuery)) {
                    continue;
                }

                ImBoolean toggled = new ImBoolean(m.isToggled());
                if (ImGui.checkbox("##toggle_" + m.getName(), toggled)) {
                    m.toggle();
                    try {
                        com.eclipseware.imnotcheatingyouare.client.setting.ConfigManager.save();
                    } catch (Exception e) {}
                }
                ImGui.sameLine();

                boolean open = ImGui.treeNode(m.getName());
                if (open) {
                    String bindText = m.getKeyBind() == -1 ? "None" : GLFW.glfwGetKeyName(m.getKeyBind(), 0);
                    if (bindText == null) {
                        bindText = "Key " + m.getKeyBind();
                    }
                    if (m.getKeyBind() == -1) bindText = "None";
                    
                    if (bindingModule == m) {
                        bindText = "Press a key...";
                    }
                    
                    if (ImGui.button("Bind: " + bindText + "##bind_" + m.getName())) {
                        bindingModule = m;
                    }
                    
                    List<Setting> settings = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingsByMod(m);
                    if (settings != null) {
                        for (Setting setting : settings) {
                            if (setting.isCheck()) {
                                ImBoolean val = new ImBoolean(setting.getValBoolean());
                                if (ImGui.checkbox(setting.getName() + "##" + m.getName(), val)) {
                                    setting.setValBoolean(val.get());
                                    try { com.eclipseware.imnotcheatingyouare.client.setting.ConfigManager.save(); } catch (Exception e) {}
                                }
                            } else if (setting.isSlider()) {
                                float[] val = new float[]{(float) setting.getValDouble()};
                                if (ImGui.sliderFloat(setting.getName() + "##" + m.getName(), val, (float) setting.getMin(), (float) setting.getMax())) {
                                    if (setting.onlyInt()) {
                                        setting.setValDouble(Math.round(val[0]));
                                    } else {
                                        setting.setValDouble(Math.round(val[0] * 100.0) / 100.0);
                                    }
                                    try { com.eclipseware.imnotcheatingyouare.client.setting.ConfigManager.save(); } catch (Exception e) {}
                                }
                            } else if (setting.isCombo()) {
                                List<String> options = setting.getOptions();
                                String[] optionsArray = options.toArray(new String[0]);
                                int currentIndex = options.indexOf(setting.getValString());
                                ImInt selected = new ImInt(currentIndex >= 0 ? currentIndex : 0);
                                if (ImGui.combo(setting.getName() + "##" + m.getName(), selected, optionsArray)) {
                                    setting.setValString(options.get(selected.get()));
                                    try { com.eclipseware.imnotcheatingyouare.client.setting.ConfigManager.save(); } catch (Exception e) {}
                                }
                            }
                        }
                    }
                    ImGui.treePop();
                }
            }
            ImGui.endChild();
            
            ImGui.columns(1);
        }
        ImGui.end();

        ImGui.render();
        ImGuiManager.getImplGl3().renderDrawData(ImGui.getDrawData());
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        int key = input.input();
        if (bindingModule != null) {
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                bindingModule.setKeyBind(-1);
            } else {
                bindingModule.setKeyBind(key);
            }
            try {
                com.eclipseware.imnotcheatingyouare.client.setting.ConfigManager.save();
            } catch (Exception e) {}
            bindingModule = null;
            return true;
        }

        if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            this.onClose();
            return true;
        }
        return true;
    }
}
