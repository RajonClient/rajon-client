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
        public final String[] options;  // not null = a choice, clicking cycles through it
        public double value;

        Setting(int module, String name, boolean toggle, double min, double max, double step, double def, String[] options) {
            this.options = options;
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
            if (options != null) return options[(int) Math.round(value)];
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
        add(new Setting(module, name, false, min, max, step, def, null));
    }

    private static void toggle(int module, String name, boolean def) {
        add(new Setting(module, name, true, 0, 1, 1, def ? 1 : 0, null));
    }

    private static void choice(int module, String name, int def, String... options) {
        add(new Setting(module, name, false, 0, options.length - 1, 1, def, options));
    }

    // Module indexes follow Modules.NAMES
    static {
        slider(0, "Distance", 3.35, 10, 0.05, 4.0);         // Reach 20

        choice(1, "Mode", 0, "Normal", "Expand", "Telly");   // Scaffold
        slider(1, "Expand length", 1, 6, 1, 3);
        slider(1, "Clutch", 0, 4, 1, 2);                    // how far around you it looks for a spot
        slider(1, "Lookahead", 0, 5, 0.5, 2);               // place ahead of you (higher = further)
        slider(1, "Min delay", 0, 500, 10, 0);              // ms between blocks
        slider(1, "Max delay", 0, 500, 10, 0);
        choice(1, "AutoBlock", 1, "Off", "Switch");
        toggle(1, "Sprint", true);
        toggle(1, "Swing", true);
        toggle(1, "Same Y", false);
        toggle(1, "Down", true);                            // hold sneak to build downward
        choice(1, "Eagle", 0, "Off", "Normal");
        choice(1, "Tower", 0, "Off", "Motion", "Legit");
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

        choice(8, "Color", 0, "Gradient", "Rainbow", "Static"); // ArrayList
        toggle(8, "Tags", true);
        toggle(8, "Background", true);
        toggle(8, "Animations", true);

        slider(10, "Gamma", 5, 1000, 5, 1000);              // Fullbright

        toggle(11, "Players", true);                        // ESP
        toggle(11, "Mobs", false);
        slider(11, "Line width", 1, 5, 0.5, 2);

        slider(12, "Range", 1, 8, 0.5, 4.5);                // AimAssist
        slider(12, "FOV", 10, 180, 5, 90);
        slider(12, "Speed", 1, 30, 1, 8);
        slider(12, "Smoothness", 1, 10, 1, 5);
        toggle(12, "Players only", true);
        toggle(12, "Require click", true);
        toggle(12, "Vertical", true);
        toggle(12, "Visible only", true);

        choice(13, "Mode", 0, "Face block", "Backwards");   // Rotations (scaffold)
        toggle(13, "Silent", true);                         // send the rotation to the server only, camera stays
        slider(13, "Speed", 10, 180, 5, 90);                // max degrees per tick (maxStep of SilentRotations.aim)
        slider(13, "Pitch", 30, 90, 1, 80);                 // used by Backwards
        toggle(13, "Movement fix", true);                   // keeps your walking direction

        slider(15, "Min CPS", 1, 20, 1, 8);                 // TriggerBot
        slider(15, "Max CPS", 1, 20, 1, 12);
        toggle(15, "Players only", true);
        toggle(15, "Require click", false);

        toggle(16, "Third person", true);                   // Freelook

        choice(17, "Style", 1, "Vanilla", "1.7");           // Animations: 1.7 = swing while blocking
        choice(17, "Block", 0, "Vanilla", "Push", "Slide", "Smooth", "Spin"); // extra motion
        slider(17, "X", -1, 1, 0.05, 0);
        slider(17, "Y", -1, 1, 0.05, 0);
        slider(17, "Z", -1, 1, 0.05, 0);
        slider(17, "Scale", 0.5, 1.5, 0.05, 1);
        slider(17, "Duration", 100, 600, 50, 300);          // ms, length of the block motion
        toggle(17, "Loop", false);                          // repeat the block motion while blocking

        slider(18, "Range", 2, 6, 0.1, 4.0);                // KillAura
        slider(18, "Min CPS", 1, 20, 1, 8);
        slider(18, "Max CPS", 1, 20, 1, 12);
        choice(18, "Mode", 0, "Single", "Switch");
        choice(18, "Priority", 0, "Distance", "Health", "Angle");
        slider(18, "FOV", 30, 360, 10, 360);
        toggle(18, "Players only", true);
        toggle(18, "Visible only", true);
        toggle(18, "Require weapon", false);
        toggle(18, "Swing", true);
        toggle(18, "Rotate", true);
        toggle(18, "Silent", true);                         // on = server-side rotation only, off = turns your camera
        slider(18, "Rotation speed", 5, 180, 5, 180);       // max degrees per tick (maxStep of SilentRotations.aim)
        choice(18, "AutoBlock", 0, "Off", "Fake");          // Fake = block pose on your screen only

        choice(19, "Mode", 0, "Ring", "Box", "Circle");     // TargetESP
        choice(19, "Color", 0, "Health", "Theme", "White");
        slider(19, "Duration", 1, 10, 1, 3);               // seconds after the last hit
        slider(19, "Speed", 0.5, 3, 0.1, 1);
        toggle(19, "Players only", false);

        toggle(20, "Switch back", true);                    // AutoTool
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

    /** Text of a setting as shown in the ClickGUI (for example "8" or "Face block"). */
    public static String display(String module, String name) {
        Setting s = find(module, name);
        return s == null ? "" : s.display();
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
