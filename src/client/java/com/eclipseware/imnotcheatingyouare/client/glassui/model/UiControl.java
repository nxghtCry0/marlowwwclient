package com.eclipseware.imnotcheatingyouare.client.glassui.model;

import com.eclipseware.imnotcheatingyouare.client.glassui.asset.LucideIcon;
import com.eclipseware.imnotcheatingyouare.client.glassui.asset.UiIcon;
import java.awt.Color;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

public abstract sealed class UiControl permits
    UiControl.Toggle,
    UiControl.Slider,
    UiControl.Action,
    UiControl.Cycle,
    UiControl.RangeSlider,
    UiControl.ColorPicker,
    UiControl.TextField {

    private final String id;
    private final String label;
    private final String description;
    private final UiIcon icon;
    private BooleanSupplier visibleSupplier;
    private Runnable syncHook;
    public float animHover = 0f;

    protected UiControl(String id, String label, String description, UiIcon icon) {
        this.id = id;
        this.label = label;
        this.description = description != null ? description : "";
        this.icon = icon != null ? icon : LucideIcon.SLIDERS_HORIZONTAL;
    }

    public String id() {
        return id;
    }

    public String label() {
        return label;
    }

    public String description() {
        return description;
    }

    public UiIcon icon() {
        return icon;
    }

    public UiControl visibleWhen(BooleanSupplier supplier) {
        this.visibleSupplier = supplier;
        return this;
    }

    public UiControl withSync(Runnable syncHook) {
        this.syncHook = syncHook;
        return this;
    }

    public boolean isVisible() {
        return visibleSupplier == null || visibleSupplier.getAsBoolean();
    }

    public void syncFromBacking() {
        if (syncHook != null) {
            syncHook.run();
        }
    }

    public static final class Toggle extends UiControl {
        private boolean enabled;
        private final Consumer<Boolean> onChanged;
        public float animToggle;
        public float animPress = 0f;

        public Toggle(String id, String label, boolean enabled, String description, UiIcon icon, Consumer<Boolean> onChanged) {
            super(id, label, description, icon);
            this.enabled = enabled;
            this.onChanged = onChanged;
            this.animToggle = enabled ? 1f : 0f;
        }

        public boolean enabled() {
            return enabled;
        }

        public void setEnabledSilent(boolean value) {
            this.enabled = value;
        }

        public void setValue(boolean newValue) {
            this.enabled = newValue;
            if (onChanged != null) {
                onChanged.accept(newValue);
            }
        }
    }

    public static final class Slider extends UiControl {
        private float value;
        private final float min;
        private final float max;
        private final String suffix;
        private final int decimals;
        private final Consumer<Float> onChanged;
        public float animNorm;
        public float animFocus = 0f;

        public Slider(
            String id,
            String label,
            float value,
            float min,
            float max,
            String suffix,
            int decimals,
            String description,
            UiIcon icon,
            Consumer<Float> onChanged
        ) {
            super(id, label, description, icon);
            this.min = min;
            this.max = max;
            this.suffix = suffix != null ? suffix : "";
            this.decimals = decimals;
            this.onChanged = onChanged;
            this.value = clamp(value, min, max);
            this.animNorm = normalized();
        }

        public float value() {
            return value;
        }

        public float min() {
            return min;
        }

        public float max() {
            return max;
        }

        public String suffix() {
            return suffix;
        }

        public int decimals() {
            return decimals;
        }

        public void setValueSilent(float next) {
            this.value = clamp(next, min, max);
        }

        public float normalized() {
            float span = max - min;
            if (span <= 0f) {
                return 0f;
            }
            return clamp((value - min) / span, 0f, 1f);
        }

        public void setFromNormalized(float norm) {
            float clamped = clamp(norm, 0f, 1f);
            float next = min + (max - min) * clamped;
            if (decimals <= 0) {
                next = Math.round(next);
            }
            this.value = clamp(next, min, max);
            if (onChanged != null) {
                onChanged.accept(this.value);
            }
        }

        public void setClampedValue(float newValue) {
            float clamped = clamp(newValue, min, max);
            this.value = clamped;
            if (onChanged != null) {
                onChanged.accept(clamped);
            }
        }

        public String formattedValue() {
            if (decimals <= 0) {
                return Math.round(value) + suffix;
            }
            String fmt = "%." + decimals + "f%s";
            return String.format(Locale.ROOT, fmt, value, suffix);
        }
    }

    public static final class Action extends UiControl {
        private final String buttonLabel;
        private final String toastMessage;
        private final Runnable onTrigger;
        public float animBtnHover = 0f;
        public float animPress = 0f;

        public Action(
            String id,
            String label,
            String buttonLabel,
            String toastMessage,
            String description,
            UiIcon icon,
            Runnable onTrigger
        ) {
            super(id, label, description, icon != null ? icon : LucideIcon.SPARKLES);
            this.buttonLabel = buttonLabel;
            this.toastMessage = toastMessage != null ? toastMessage : "";
            this.onTrigger = onTrigger;
        }

        public String buttonLabel() {
            return buttonLabel;
        }

        public String toastMessage() {
            return toastMessage;
        }

        public void trigger() {
            if (onTrigger != null) {
                onTrigger.run();
            }
        }
    }

    public static final class Cycle extends UiControl {
        private final List<String> options;
        private int index;
        private final Consumer<String> onChanged;
        public float animBtnHover = 0f;
        public float animPress = 0f;

        public Cycle(
            String id,
            String label,
            List<String> options,
            int initialIndex,
            String description,
            UiIcon icon,
            Consumer<String> onChanged
        ) {
            super(id, label, description, icon != null ? icon : LucideIcon.LAYERS);
            this.options = options != null ? List.copyOf(options) : List.of();
            this.index = this.options.isEmpty() ? 0 : Math.max(0, Math.min(this.options.size() - 1, initialIndex));
            this.onChanged = onChanged;
        }

        public List<String> options() {
            return options;
        }

        public int index() {
            return index;
        }

        public String currentOption() {
            if (options.isEmpty()) {
                return "";
            }
            return options.get(index);
        }

        public void setSelectedSilent(String val) {
            if (val == null || options.isEmpty()) {
                return;
            }
            for (int i = 0; i < options.size(); i++) {
                if (options.get(i).equalsIgnoreCase(val)) {
                    this.index = i;
                    return;
                }
            }
        }

        public void step(int delta) {
            if (options.isEmpty()) {
                return;
            }
            int size = options.size();
            this.index = ((this.index + delta) % size + size) % size;
            if (onChanged != null) {
                onChanged.accept(options.get(this.index));
            }
        }
    }

    public static final class RangeSlider extends UiControl {
        private float low;
        private float high;
        private final float min;
        private final float max;
        private final int decimals;
        private final RangeConsumer onChanged;
        public float animLowNorm;
        public float animHighNorm;
        public float animFocus = 0f;

        @FunctionalInterface
        public interface RangeConsumer {
            void accept(float low, float high);
        }

        public RangeSlider(
            String id,
            String label,
            float low,
            float high,
            float min,
            float max,
            int decimals,
            String description,
            UiIcon icon,
            RangeConsumer onChanged
        ) {
            super(id, label, description, icon);
            this.min = min;
            this.max = max;
            this.decimals = decimals;
            this.onChanged = onChanged;
            float a = clamp(low, min, max);
            float b = clamp(high, min, max);
            this.low = Math.min(a, b);
            this.high = Math.max(a, b);
            this.animLowNorm = lowNormalized();
            this.animHighNorm = highNormalized();
        }

        public float low() {
            return low;
        }

        public float high() {
            return high;
        }

        public float lowNormalized() {
            float span = max - min;
            return span <= 0f ? 0f : clamp((low - min) / span, 0f, 1f);
        }

        public float highNormalized() {
            float span = max - min;
            return span <= 0f ? 1f : clamp((high - min) / span, 0f, 1f);
        }

        public void setRangeSilent(float lo, float hi) {
            float a = clamp(lo, min, max);
            float b = clamp(hi, min, max);
            this.low = Math.min(a, b);
            this.high = Math.max(a, b);
        }

        public void setHandleFromNormalized(boolean draggingHigh, float norm) {
            float v = min + (max - min) * clamp(norm, 0f, 1f);
            if (decimals <= 0) {
                v = Math.round(v);
            }
            if (draggingHigh) {
                this.high = Math.max(this.low, clamp(v, min, max));
            } else {
                this.low = Math.min(this.high, clamp(v, min, max));
            }
            if (onChanged != null) {
                onChanged.accept(this.low, this.high);
            }
        }

        public String formattedValue() {
            if (decimals <= 0) {
                return Math.round(low) + " - " + Math.round(high);
            }
            String fmt = "%." + decimals + "f - %." + decimals + "f";
            return String.format(Locale.ROOT, fmt, low, high);
        }
    }

    public static final class ColorPicker extends UiControl {
        private int argbColor;
        private float hue;
        private float saturation;
        private float brightness;
        private final IntConsumer onChanged;
        public float animFocus = 0f;

        public ColorPicker(
            String id,
            String label,
            int argbColor,
            String description,
            UiIcon icon,
            IntConsumer onChanged
        ) {
            super(id, label, description, icon != null ? icon : LucideIcon.PALETTE);
            this.onChanged = onChanged;
            setColorSilent(argbColor);
        }

        public int color() {
            return argbColor;
        }

        public float hue() {
            return hue;
        }

        public float saturation() {
            return saturation;
        }

        public float brightness() {
            return brightness;
        }

        public void setColorSilent(int color) {
            this.argbColor = color | 0xFF000000;
            int r = (this.argbColor >>> 16) & 0xFF;
            int g = (this.argbColor >>> 8) & 0xFF;
            int b = this.argbColor & 0xFF;
            float[] hsb = Color.RGBtoHSB(r, g, b, null);
            this.hue = hsb[0];
            this.saturation = Math.max(0.15f, hsb[1]);
            this.brightness = Math.max(0.25f, hsb[2]);
        }

        public void setHueNormalized(float norm) {
            this.hue = clamp(norm, 0f, 1f);
            this.argbColor = Color.HSBtoRGB(this.hue, this.saturation, this.brightness);
            if (onChanged != null) {
                onChanged.accept(this.argbColor);
            }
        }

        public void cycleVibrance() {
            if (this.saturation > 0.85f && this.brightness > 0.85f) {
                this.saturation = 0.55f;
                this.brightness = 1.0f;
            } else if (this.saturation <= 0.60f) {
                this.saturation = 0.95f;
                this.brightness = 0.72f;
            } else {
                this.saturation = 0.92f;
                this.brightness = 1.0f;
            }
            this.argbColor = Color.HSBtoRGB(this.hue, this.saturation, this.brightness);
            if (onChanged != null) {
                onChanged.accept(this.argbColor);
            }
        }

        public String hexLabel() {
            int rgb = argbColor & 0x00FFFFFF;
            return String.format(Locale.ROOT, "#%06X", rgb);
        }
    }

    public static final class TextField extends UiControl {
        private String value;
        private final Consumer<String> onChanged;
        public boolean focused = false;
        public float animFocus = 0f;
        public float caretX = -1f;
        public float keyPulse = 0f;

        public TextField(
            String id,
            String label,
            String value,
            String description,
            UiIcon icon,
            Consumer<String> onChanged
        ) {
            super(id, label, description, icon != null ? icon : LucideIcon.TAG);
            this.value = value != null ? value : "";
            this.onChanged = onChanged;
        }

        public String value() {
            return value;
        }

        public void setValueSilent(String next) {
            this.value = next != null ? next : "";
        }

        public void setValue(String next) {
            this.value = next != null ? next : "";
            if (onChanged != null) {
                onChanged.accept(this.value);
            }
        }
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }
}
