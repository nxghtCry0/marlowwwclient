package com.eclipseware.imnotcheatingyouare.client.clickgui;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.clickgui.comp.*;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.utils.AnimationUtil;
import com.eclipseware.imnotcheatingyouare.client.utils.FontUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Clickgui extends Screen {
    public double posX, posY, windowWidth, windowHeight, dragX, dragY;
    public boolean dragging;
    public Category selectedCategory;
    public String selectedSubCategory;
    public Module selectedModule;

    public float openAnim = 0f;
    public boolean closing = false;
    public float settingsSlideAnim = 0f;
    private final Map<Module, Float> moduleToggleAnims = new HashMap<>();

    public double scrollOffset = 0;       
    public double targetScrollOffset = 0; 
    public double moduleScrollOffset = 0; 
    public double targetModuleScrollOffset = 0;

    public ArrayList<Comp> comps = new ArrayList<>();

    public Clickgui() {
        super(Component.literal("ClickGUI"));
        dragging = false;
        selectedCategory = Category.values()[0]; 
        selectedSubCategory = null;
        posX = -1; 
        posY = -1;
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {}

    public static void playSound() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    @Override
    protected void init() {
        super.init();
        windowWidth = 560;
        windowHeight = 380;
        openAnim = 0f; 
        closing = false;
        
        if (posX == -1 && posY == -1) {
            posX = (this.width - windowWidth) / 2.0;
            posY = (this.height - windowHeight) / 2.0;
        }
    }

    @Override
    public void onClose() {
        super.onClose();
        com.eclipseware.imnotcheatingyouare.client.setting.ConfigManager.save();
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        if (selectedModule != null) {
            for (Comp comp : comps) {
                if (comp instanceof BindComp bindComp && bindComp.isBinding) {
                    bindComp.keyPressed(event.key(), 0, event.modifiers());
                    return true;
                }
            }
        }

        if (searchFocused) {
            if (event.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE && !searchText.isEmpty()) {
                searchText = searchText.substring(0, searchText.length() - 1);
                playSound();
                return true;
            } else if (event.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE || event.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER) {
                searchFocused = false;
                return true;
            } else if (event.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE) {
                searchText += " ";
                return true;
            } else if (event.key() >= org.lwjgl.glfw.GLFW.GLFW_KEY_A && event.key() <= org.lwjgl.glfw.GLFW.GLFW_KEY_Z) {
                char c = (char) event.key();
                if ((event.modifiers() & org.lwjgl.glfw.GLFW.GLFW_MOD_SHIFT) == 0) {
                    c = Character.toLowerCase(c);
                }
                searchText += c;
                return true;
            }
        }

        if (event.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_H) {
            float uiScale = getScale();
            posX = (this.width / uiScale - windowWidth) / 2.0;
            posY = (this.height / uiScale - windowHeight) / 2.0;
            return true;
        }

        if (event.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            closing = true;
            return true;
        }
        return super.keyPressed(event);
    }

    private float getScale() {
        return Math.max(0.5f, Math.min(2.0f, Math.min((float) this.width / 800f, (float) this.height / 500f)));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int rawMouseX, int rawMouseY, float partialTick) {
        float uiScale = getScale();
        int mouseX = (int) (rawMouseX / uiScale);
        int mouseY = (int) (rawMouseY / uiScale);

        super.render(guiGraphics, rawMouseX, rawMouseY, partialTick);
        
        Module theme = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("Theme");
        
        int r = 155, g = 60, b = 255, a = 240;
        float animSpeed = 0.15f;
        if (theme != null) {
            r = (int) ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(theme, "Accent R").getValDouble();
            g = (int) ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(theme, "Accent G").getValDouble();
            b = (int) ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(theme, "Accent B").getValDouble();
            a = (int) ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(theme, "Background Alpha").getValDouble();
            animSpeed = (float) ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(theme, "Anim Speed").getValDouble() * 0.03f;
        }

        scrollOffset += (targetScrollOffset - scrollOffset) * (animSpeed * 1.5f);
        moduleScrollOffset += (targetModuleScrollOffset - moduleScrollOffset) * (animSpeed * 1.5f);

        if (closing) {
            openAnim += (0f - openAnim) * (animSpeed * 1.5f); 
            if (openAnim < 0.05f) {
                super.onClose(); 
                return;
            }
        } else {
            openAnim += (1.0f - openAnim) * animSpeed; 
        }

        float targetSlide = (selectedModule != null) ? 1f : 0f;
        settingsSlideAnim += (targetSlide - settingsSlideAnim) * (animSpeed * 1.2f);

        if (dragging) {
            posX = mouseX - dragX;
            posY = mouseY - dragY;
        }

        int screenDim = new Color(5, 5, 5, (int)(140 * openAnim)).getRGB();
        guiGraphics.fill(0, 0, this.width, this.height, screenDim);

        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().scale(uiScale, uiScale);
        
        float centerX = (float) (posX + windowWidth / 2.0);
        float centerY = (float) (posY + windowHeight / 2.0);
        
        guiGraphics.pose().translate(centerX, centerY);
        guiGraphics.pose().scale(openAnim, openAnim);
        guiGraphics.pose().translate(-centerX, -centerY);

        int bgDark = new Color(16, 16, 18, a).getRGB();
        int sidebarColor = new Color(22, 22, 25, Math.min(255, a + 15)).getRGB();
        int accent = new Color(r, g, b).getRGB(); 

        AnimationUtil.drawRoundedRect(guiGraphics, (int)posX, (int)posY, (int)windowWidth, (int)windowHeight, 10, bgDark);
        AnimationUtil.drawRoundedRect(guiGraphics, (int)posX, (int)posY, 130, (int)windowHeight, 10, sidebarColor);
        guiGraphics.fill((int)posX + 120, (int)posY, (int)posX + 130, (int)(posY + windowHeight), sidebarColor); 

        FontUtils.drawString(guiGraphics, "MARLOWWW", (int)posX + 15, (int)posY + 15, -1, true);
        FontUtils.drawString(guiGraphics, "CLIENT", (int)posX + 15 + FontUtils.width("MARLOWWW "), (int)posY + 15, accent, true);

        int catY = (int)posY + 60;
        for (Category cat : Category.values()) {
            boolean sel = cat == selectedCategory;
            if (sel) {
                AnimationUtil.drawRoundedRect(guiGraphics, (int)posX + 10, catY, 110, 30, 6, new Color(255, 255, 255, 15).getRGB());
                AnimationUtil.drawRoundedRect(guiGraphics, (int)posX + 10, catY + 6, 4, 18, 2, accent);
            }
            boolean hasMatch = catHasMatch(cat);
            int catColor = sel ? -1 : (hasMatch ? accent : new Color(160, 160, 160).getRGB());
            FontUtils.drawString(guiGraphics, cat.name(), (int)posX + 28, catY + 11, catColor, false);
            catY += 36;
        }

        if (settingsSlideAnim < 0.99f) {
            guiGraphics.pose().pushMatrix();
            guiGraphics.pose().translate(-(settingsSlideAnim * 400), 0f); 

            FontUtils.drawString(guiGraphics, selectedCategory.name().toUpperCase(), (int)posX + 150, (int)posY + 20, accent, false);
            FontUtils.drawString(guiGraphics, "Select a module to configure", (int)posX + 150, (int)posY + 32, new Color(140, 140, 140).getRGB(), false);
            guiGraphics.fill((int)posX + 150, (int)posY + 50, (int)(posX + windowWidth - 20), (int)posY + 51, new Color(255, 255, 255, 20).getRGB());

            guiGraphics.enableScissor((int)posX + 130, (int)posY + 51, (int)(posX + windowWidth), (int)(posY + windowHeight));
            guiGraphics.pose().pushMatrix();
            guiGraphics.pose().translate(0f, (float) moduleScrollOffset);

            int modY = (int)posY + 60;
            java.util.List<Module> modsToRender = new ArrayList<>();
            for (Module m : ImnotcheatingyouareClient.INSTANCE.moduleManager.getModules(selectedCategory)) {
                if (selectedSubCategory == null && m != selectedModule) continue;
                if (selectedSubCategory != null && !selectedSubCategory.equals("ALL") && m != selectedModule) {
                    String sub = m.getSubCategory() == null || m.getSubCategory().isEmpty() ? "General" : m.getSubCategory();
                    if (!sub.equals(selectedSubCategory)) continue;
                }
                modsToRender.add(m);
            }
            if (modsToRender.isEmpty() && selectedModule != null) modsToRender.add(selectedModule);
            
            for (Module m : modsToRender) {
                float currentModAnim = moduleToggleAnims.getOrDefault(m, m.isToggled() ? 1f : 0f);
                currentModAnim += ((m.isToggled() ? 1f : 0f) - currentModAnim) * 0.15f;
                moduleToggleAnims.put(m, currentModAnim);

                int cardColor = interpolateColor(new Color(30, 30, 33), new Color(r, g, b, 60), currentModAnim).getRGB();
                if (isMatch(m)) {
                    cardColor = new Color(r, g, b, 120).getRGB();
                }
                int textColor = interpolateColor(new Color(170, 170, 170), new Color(255, 255, 255), currentModAnim).getRGB();

                AnimationUtil.drawRoundedRect(guiGraphics, (int)posX + 150, modY, (int)(windowWidth - 170), 36, 6, cardColor);
                if (currentModAnim > 0.05f) {
                    AnimationUtil.drawRoundedRect(guiGraphics, (int)posX + 150, modY, (int)(4 * currentModAnim), 36, 6, accent);
                }

                FontUtils.drawString(guiGraphics, m.getName(), (int)posX + 165, modY + 14, textColor, false);
                FontUtils.drawRightAlignedString(guiGraphics, "Right-Click", (int)(posX + windowWidth - 30), modY + 14, new Color(100, 100, 100).getRGB());

                modY += 44;
            }
            guiGraphics.pose().popMatrix();
            guiGraphics.disableScissor();
            guiGraphics.pose().popMatrix();
        }

        if (settingsSlideAnim > 0.01f) {
            guiGraphics.pose().pushMatrix();
            guiGraphics.pose().translate((1f - settingsSlideAnim) * 400, 0f);

            boolean backHover = isInside(mouseX, mouseY, posX + 150, posY + 20, posX + 220, posY + 35);
            FontUtils.drawString(guiGraphics, "< Back", (int)posX + 150, (int)posY + 22, backHover ? -1 : new Color(160, 160, 160).getRGB(), false);

            if (selectedModule != null) {
                FontUtils.drawString(guiGraphics, selectedModule.getName() + " Settings", (int)posX + 150, (int)posY + 44, accent, false);
            }
            guiGraphics.fill((int)posX + 150, (int)posY + 66, (int)(posX + windowWidth - 20), (int)posY + 67, new Color(255, 255, 255, 20).getRGB());

            guiGraphics.enableScissor((int)posX + 130, (int)posY + 67, (int)(posX + windowWidth), (int)(posY + windowHeight));
            guiGraphics.pose().pushMatrix();
            guiGraphics.pose().translate(0f, (float) scrollOffset);

            for (Comp comp : comps) {
                comp.render(guiGraphics, mouseX, (int)(mouseY - scrollOffset));
            }

            guiGraphics.pose().popMatrix();
            guiGraphics.disableScissor();
            guiGraphics.pose().popMatrix();
        }

        if (selectedModule == null) {
            if (selectedSubCategory == null) {
                int modY = (int)posY + 60 + (int)moduleScrollOffset;
                java.util.Set<String> subCats = new java.util.LinkedHashSet<>();
                for (Module m : ImnotcheatingyouareClient.INSTANCE.moduleManager.getModules(selectedCategory)) {
                    String sub = m.getSubCategory() == null || m.getSubCategory().isEmpty() ? "General" : m.getSubCategory();
                    subCats.add(sub);
                }
                for (String sub : subCats) {
                    AnimationUtil.drawRoundedRect(guiGraphics, (int)posX + 150, modY, (int)(windowWidth - 170), 36, 6, new Color(30, 30, 33).getRGB());
                    FontUtils.drawString(guiGraphics, "📁 " + sub, (int)posX + 165, modY + 14, new Color(220, 220, 220).getRGB(), false);
                    FontUtils.drawRightAlignedString(guiGraphics, "Click to open", (int)(posX + windowWidth - 30), modY + 14, new Color(100, 100, 100).getRGB());
                    modY += 44;
                }
            } else {
                int modY = (int)posY + 60 + (int)moduleScrollOffset;
                for (Module m : ImnotcheatingyouareClient.INSTANCE.moduleManager.getModules(selectedCategory)) {
                    if (!selectedSubCategory.equals("ALL")) {
                        String sub = m.getSubCategory() == null || m.getSubCategory().isEmpty() ? "General" : m.getSubCategory();
                        if (!sub.equals(selectedSubCategory)) continue;
                    }
                    if (isInside(mouseX, mouseY, posX + 150, modY, posX + windowWidth - 20, modY + 36)) {
                        String desc = m.getDescription();
                        boolean detected = false;
                        String srv = net.minecraft.client.Minecraft.getInstance().getCurrentServer() != null ? net.minecraft.client.Minecraft.getInstance().getCurrentServer().ip : null;
                        if (srv != null) {
                            com.eclipseware.imnotcheatingyouare.client.module.impl.DetectionAlert da = (com.eclipseware.imnotcheatingyouare.client.module.impl.DetectionAlert) ImnotcheatingyouareClient.INSTANCE.moduleManager.getModule("DetectionDB");
                            if (da != null && da.isToggled() && da.isModuleDetected(m, srv)) {
                                detected = true;
                            }
                        }

                        int tw = (desc != null && !desc.isEmpty()) ? FontUtils.width(desc) : 0;
                        String detStr = detected ? "DETECTED ON " + srv.toUpperCase() : null;
                        int tw2 = detected ? FontUtils.width(detStr) : 0;
                        int maxTw = Math.max(tw, tw2);

                        if (maxTw > 0) {
                            int h = detected && desc != null && !desc.isEmpty() ? 32 : 20;
                            AnimationUtil.drawRoundedRect(guiGraphics, mouseX + 12, mouseY + 4, maxTw + 10, h, 4, new Color(15, 15, 15, 240).getRGB());
                            
                            int textY = mouseY + 10;
                            if (desc != null && !desc.isEmpty()) {
                                FontUtils.drawString(guiGraphics, desc, mouseX + 17, textY, -1, false);
                                textY += 12;
                            }
                            if (detected) {
                                FontUtils.drawString(guiGraphics, detStr, mouseX + 17, textY, new Color(255, 80, 80).getRGB(), false);
                            }
                        }
                        break;
                    }
                    modY += 44;
                }
            }
        }

        int searchY = (int)posY + (int)windowHeight + 10;
        AnimationUtil.drawRoundedRect(guiGraphics, (int)posX, searchY, (int)windowWidth, 30, 8, bgDark);
        if (searchFocused) {
            AnimationUtil.drawRoundedRect(guiGraphics, (int)posX, searchY, (int)windowWidth, 30, 8, new Color(r, g, b, 80).getRGB());
        }
        FontUtils.drawString(guiGraphics, "Search: " + searchText + (searchFocused && System.currentTimeMillis() % 1000 < 500 ? "_" : ""), (int)posX + 15, searchY + 11, -1, false);

        guiGraphics.pose().popMatrix(); 
    }

    private Color interpolateColor(Color color1, Color color2, float fraction) {
        int red = (int) (color1.getRed() + (color2.getRed() - color1.getRed()) * fraction);
        int green = (int) (color1.getGreen() + (color2.getGreen() - color1.getGreen()) * fraction);
        int blue = (int) (color1.getBlue() + (color2.getBlue() - color1.getBlue()) * fraction);
        int alpha = (int) (color1.getAlpha() + (color2.getAlpha() - color1.getAlpha()) * fraction);
        return new Color(red, green, blue, alpha);
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        if (closing) return false;

        float uiScale = getScale();
        double mouseX = event.x() / uiScale;
        double mouseY = event.y() / uiScale;
        int button = event.button();

        if (selectedModule != null) {
            for (Comp comp : comps) {
                if (comp instanceof BindComp bindComp && bindComp.isBinding) {
                    bindComp.mouseClicked(mouseX, mouseY - scrollOffset, button);
                    return true;
                }
            }
        }

        boolean searchHover = isInside(mouseX, mouseY, posX, posY + windowHeight + 10, posX + windowWidth, posY + windowHeight + 40);
        if (searchHover && button == 0) {
            searchFocused = true;
            playSound();
            return true;
        } else if (button == 0 || button == 1) {
            searchFocused = false;
        }

        boolean headerHover = isInside(mouseX, mouseY, posX, posY, posX + windowWidth, posY + 20);
        boolean footerHover = isInside(mouseX, mouseY, posX, posY + windowHeight - 20, posX + windowWidth, posY + windowHeight);
        
        if ((headerHover || footerHover) && button == 0) {
            dragging = true;
            dragX = mouseX - posX;
            dragY = mouseY - posY;
        }

        int catY = (int)posY + 60;
        for (Category cat : Category.values()) {
            if (isInside(mouseX, mouseY, posX + 10, catY, posX + 120, catY + 30)) {
                if (button == 0) {
                    selectedCategory = cat;
                    selectedModule = null;
                    targetModuleScrollOffset = 0;
                    selectedSubCategory = null; 
                    playSound();
                    return true;
                } else if (button == 1) {
                    selectedCategory = cat;
                    selectedModule = null;
                    targetModuleScrollOffset = 0;
                    selectedSubCategory = "ALL";
                    playSound();
                    return true;
                }
            }
            catY += 36;
        }

        if (selectedModule == null) {
            if (mouseY > posY + 58) {
                int modY = (int)posY + 70 + (int)moduleScrollOffset;
                if (selectedSubCategory == null) {
                    java.util.Set<String> subCats = new java.util.LinkedHashSet<>();
                    for (Module m : ImnotcheatingyouareClient.INSTANCE.moduleManager.getModules(selectedCategory)) {
                        String sub = m.getSubCategory() == null || m.getSubCategory().isEmpty() ? "General" : m.getSubCategory();
                        subCats.add(sub);
                    }
                    for (String sub : subCats) {
                        if (isInside(mouseX, mouseY, posX + 150, modY, posX + windowWidth - 20, modY + 36)) {
                            playSound();
                            if (button == 0) {
                                selectedSubCategory = sub;
                                targetModuleScrollOffset = 0;
                            }
                            return true;
                        }
                        modY += 44;
                    }
                } else {
                    for (Module m : ImnotcheatingyouareClient.INSTANCE.moduleManager.getModules(selectedCategory)) {
                        if (!selectedSubCategory.equals("ALL")) {
                            String sub = m.getSubCategory() == null || m.getSubCategory().isEmpty() ? "General" : m.getSubCategory();
                            if (!sub.equals(selectedSubCategory)) continue;
                        }
                        if (isInside(mouseX, mouseY, posX + 150, modY, posX + windowWidth - 20, modY + 36)) {
                            playSound();
                            if (button == 0) m.toggle();
                            else if (button == 1) {
                                selectedModule = m;
                                loadComponents(m);
                            }
                            return true;
                        }
                        modY += 44;
                    }
                }
            }
        } else {
            if (isInside(mouseX, mouseY, posX + 150, posY + 20, posX + 220, posY + 35) && button == 0) {
                selectedModule = null;
                playSound();
                return true;
            }

            if (isInside(mouseX, mouseY, posX + 130, posY + 66, posX + windowWidth, posY + windowHeight)) {
                ArrayList<Comp> compsCopy = new ArrayList<>(comps);
                for (Comp comp : compsCopy) {
                    comp.mouseClicked(mouseX, mouseY - scrollOffset, button);
                }
            }
        }
        return false;
    }

    @Override
    public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent event) {
        dragging = false;
        if (selectedModule != null) {
            float uiScale = getScale();
            for (Comp comp : new ArrayList<>(comps)) {
                comp.mouseReleased(event.x() / uiScale, (event.y() / uiScale) - scrollOffset, event.button());
            }
        }
        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return handleScroll(scrollY);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDelta) {
        return handleScroll(scrollDelta);
    }

    private boolean handleScroll(double scrollDelta) {
        if (selectedModule == null) {
            targetModuleScrollOffset += scrollDelta * 30;
            if (targetModuleScrollOffset > 0) targetModuleScrollOffset = 0; 
            
            int totalMods = ImnotcheatingyouareClient.INSTANCE.moduleManager.getModules(selectedCategory).size();
            double maxScroll = -((totalMods * 44) + 30 - (windowHeight - 70));
            if (maxScroll > 0) maxScroll = 0;
            if (targetModuleScrollOffset < maxScroll) targetModuleScrollOffset = maxScroll;
        } else {
            targetScrollOffset += scrollDelta * 30;
            if (targetScrollOffset > 0) targetScrollOffset = 0; 
            
            double maxH = 0;
            for (Comp c : comps) {
                if (c.y > maxH) maxH = c.y;
            }
            double maxScroll = -((maxH + 50) - (windowHeight - 66));
            if (maxScroll > 0) maxScroll = 0;
            if (targetScrollOffset < maxScroll) targetScrollOffset = maxScroll;
        }
        return true;
    }

    public void loadComponents(Module m) {
        comps.clear();
        scrollOffset = 0; 
        targetScrollOffset = 0;
        int sY = 80; 
        
        comps.add(new BindComp(150, sY, this, m));
        sY += 34;

        if (ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingsByMod(m) != null) {
            for (Setting setting : ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingsByMod(m)) {
                if (setting.isCombo()) { comps.add(new Combo(150, sY, this, m, setting)); sY += 32; }
                if (setting.isCheck()) { comps.add(new CheckBox(150, sY, this, m, setting)); sY += 30; }
                if (setting.isSlider()) { comps.add(new Slider(150, sY, this, m, setting)); sY += 40; }
            }
        }
    }

    public boolean isInside(double mouseX, double mouseY, double x, double y, double x2, double y2) {
        return (mouseX >= x && mouseX <= x2) && (mouseY >= y && mouseY <= y2);
    }
    
    public String searchText = "";
    public boolean searchFocused = false;

    private boolean isMatch(Module m) {
        if (searchText.isEmpty()) return false;
        String query = searchText.toLowerCase();
        return m.getName().toLowerCase().contains(query) || 
               (m.getDescription() != null && m.getDescription().toLowerCase().contains(query));
    }

    private boolean catHasMatch(Category cat) {
        if (searchText.isEmpty()) return false;
        for (Module m : ImnotcheatingyouareClient.INSTANCE.moduleManager.getModules(cat)) {
            if (isMatch(m)) return true;
        }
        return false;
    }


}