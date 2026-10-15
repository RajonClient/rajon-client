package com.example.fastbeo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Adjustable parameters of the modules (sliders and on/off toggles).
 * Edit them in the ClickGUI: right click a module to expand its settings.
 * Values are saved by ConfigManager (autosave and named configs).
 */
public class Settings {

    public static class Setting {
        public final int module;      // index in Modules.NAMES
        public final String name;
        public final boolean toggle;  // true = on/off switch, false = slider
        public final double min, max, step, def;
        public double value;

        Setting(int module, String name, boolean toggle, double min, double max, double step, double def) {
            this.module = module;
            this.name = name;
            this.toggle = toggle;
            this.min = min;
            this.max = max;
            this.step = step;
            this.def = def;
            this.value = def;
        }

        public String key() {
            return Modules.NAMES[module] + "." + name;
        }

        public boolean on() {
            return value >= 0.5;
        }

        public String display() {
            if (toggle) return on() ? "ON" : "OFF";
            if (step >= 1.0) return String.valueOf((int) Math.round(value));
            return String.format(step >= 0.1 ? "%.1f" : "%.2f", value);
        }
    }

    private static final List<Setting> ALL = new ArrayList<Setting>();
    private static final Map<String, Setting> BY_KEY = new HashMap<String, Setting>();

    private static void add(Setting s) {
        ALL.add(s);
        BY_KEY.put(s.key(), s);
    }

    private static void slider(int module, String name, double min, double max, double step, double def) {
        add(new Setting(module, name, false, min, max, step, def));
    }

    private static void toggle(int module, String name, boolean def) {
        add(new Setting(module, name, true, 0, 1, 1, def ? 1 : 0));
    }

    // Module indexes follow Modules.NAMES
    static {
        slider(0, "Distance", 3.35, 10, 0.05, 4.0);         // Reach 20

        slider(1, "Lookahead", 0, 6, 0.5, 3);               // Scaffold
        slider(1, "Blocks per tick", 1, 3, 1, 2);

        toggle(3, "Require block", false);                  // LegitScaffold (off = works with any item)

        slider(4, "Min CPS", 1, 20, 1, 8);                  // AutoClicker
        slider(4, "Max CPS", 1, 20, 1, 12);

        toggle(5, "Left click", true);                      // ClickDelay
        toggle(5, "Right click", true);

        toggle(7, "Watermark", true);                       // HUD
        toggle(7, "Info", true);
        toggle(7, "TargetHUD", true);
        toggle(7, "Notifications", true);

        toggle(8, "Rainbow", true);                         // ArrayList

        slider(10, "Gamma", 5, 1000, 5, 1000);              // Fullbright

        toggle(11, "Players", true);                        // ESP
        toggle(11, "Mobs", false);
        slider(11, "Line width", 1, 5, 0.5, 2);

        slider(12, "Range", 1, 8, 0.5, 4.5);                // AimAssist
        slider(12, "FOV", 10, 180, 5, 90);
        slider(12, "Speed", 1, 30, 1, 8);
        toggle(12, "Players only", true);
        toggle(12, "Require click", true);
        toggle(12, "Vertical", true);
        toggle(12, "Visible only", true);

        slider(13, "Speed", 10, 180, 5, 90);                // Rotations
    }

    public static List<Setting> all() {
        return ALL;
    }

    /** Settings of one module, in display order. */
    public static List<Setting> of(int module) {
        List<Setting> out = new ArrayList<Setting>();
        for (Setting s : ALL) {
            if (s.module == module) out.add(s);
        }
        return out;
    }

    private static Setting find(String module, String name) {
        return BY_KEY.get(module + "." + name);
    }

    public static double get(String module, String name) {
        Setting s = find(module, name);
        return s == null ? 0.0 : s.value;
    }

    public static boolean bool(String module, String name) {
        Setting s = find(module, name);
        return s != null && s.on();
    }

    /** Sets a value, snapped to the step and kept inside min/max, then autosaves. */
    public static void set(Setting s, double v) {
        double snapped = Math.round((v - s.min) / s.step) * s.step + s.min;
        s.value = Math.max(s.min, Math.min(s.max, snapped));
        ConfigManager.autoSave();
    }
}
