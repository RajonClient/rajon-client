package com.example.fastbeo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import net.minecraft.client.Minecraft;

/**
 * Saves module states, key binds and HUD positions.
 *
 *  - "autosave": written automatically every time something changes and loaded at startup.
 *  - Named configs: saved/loaded from the Configs screen (ClickGUI -> Configs).
 *
 * Files live in .minecraft/config/farreach/<name>.properties
 * 'Allow Multiplayer' is never saved as ON, so multiplayer is always a deliberate choice.
 */
public class ConfigManager {
    public static final String AUTO = "autosave";

    private static boolean loading = false;

    private static File dir() {
        File d = new File(Minecraft.getMinecraft().mcDataDir, "config/farreach");
        d.mkdirs();
        return d;
    }

    private static File file(String name) {
        return new File(dir(), name + ".properties");
    }

    public static boolean validName(String n) {
        return n != null && n.matches("[A-Za-z0-9_-]{1,24}");
    }

    // ---------------------------------------------------------------- snapshot / apply

    private static Properties snapshot() {
        Properties p = new Properties();
        for (int i = 0; i < Modules.NAMES.length; i++) {
            String n = Modules.NAMES[i];
            if (i != 6) { // 6 = Allow Multiplayer
                p.setProperty("module." + n, String.valueOf(Modules.isOn(i)));
            }
            p.setProperty("bind." + n, String.valueOf(Binds.get(i)));
        }
        for (int j = 0; j < HudLayout.NAMES.length; j++) {
            String n = HudLayout.NAMES[j];
            p.setProperty("hud." + n + ".x", String.valueOf(HudLayout.getRawX(j)));
            p.setProperty("hud." + n + ".y", String.valueOf(HudLayout.getRawY(j)));
        }
        for (Settings.Setting s : Settings.all()) {
            p.setProperty("setting." + s.key(), String.valueOf(s.value));
        }
        return p;
    }

    private static int parse(String v, int fallback) {
        if (v == null) return fallback;
        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private static void apply(Properties p) {
        loading = true;
        try {
            for (int i = 0; i < Modules.NAMES.length; i++) {
                String n = Modules.NAMES[i];
                if (i != 6) {
                    String v = p.getProperty("module." + n);
                    if (v != null) Modules.setState(i, Boolean.parseBoolean(v.trim()));
                }
                String b = p.getProperty("bind." + n);
                if (b != null) Binds.set(i, parse(b, Binds.get(i)));
            }
            for (int j = 0; j < HudLayout.NAMES.length; j++) {
                String n = HudLayout.NAMES[j];
                HudLayout.setRaw(j,
                        parse(p.getProperty("hud." + n + ".x"), HudLayout.getRawX(j)),
                        parse(p.getProperty("hud." + n + ".y"), HudLayout.getRawY(j)));
            }
            for (Settings.Setting s : Settings.all()) {
                String v = p.getProperty("setting." + s.key());
                if (v != null) {
                    try {
                        Settings.set(s, Double.parseDouble(v.trim()));
                    } catch (NumberFormatException ex) {
                        // keep the current value
                    }
                }
            }
        } finally {
            loading = false;
        }
        HudLayout.save(); // also refreshes the autosave
    }

    // ---------------------------------------------------------------- files

    private static boolean write(String name) {
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(file(name));
            snapshot().store(out, "FarReach config: " + name);
            return true;
        } catch (Exception ex) {
            return false;
        } finally {
            if (out != null) {
                try { out.close(); } catch (Exception ignored) { }
            }
        }
    }

    private static Properties read(String name) {
        File f = file(name);
        if (!f.exists()) return null;
        FileInputStream in = null;
        try {
            Properties p = new Properties();
            in = new FileInputStream(f);
            p.load(in);
            return p;
        } catch (Exception ex) {
            return null;
        } finally {
            if (in != null) {
                try { in.close(); } catch (Exception ignored) { }
            }
        }
    }

    // ---------------------------------------------------------------- public API

    /** Called whenever a module, bind or HUD position changes. */
    public static void autoSave() {
        if (loading) return;
        write(AUTO);
    }

    /** Called once at startup. */
    public static void loadAuto() {
        Properties p = read(AUTO);
        if (p != null) apply(p);
    }

    public static boolean save(String name) {
        if (!validName(name) || AUTO.equals(name)) return false;
        return write(name);
    }

    public static boolean load(String name) {
        if (!validName(name)) return false;
        Properties p = read(name);
        if (p == null) return false;
        apply(p);
        return true;
    }

    public static boolean delete(String name) {
        if (!validName(name) || AUTO.equals(name)) return false;
        return file(name).delete();
    }

    /** Names of the saved configs (without the autosave). */
    public static List<String> list() {
        List<String> names = new ArrayList<String>();
        File[] files = dir().listFiles();
        if (files != null) {
            for (File f : files) {
                String n = f.getName();
                if (n.endsWith(".properties")) {
                    n = n.substring(0, n.length() - ".properties".length());
                    if (!AUTO.equals(n)) names.add(n);
                }
            }
        }
        Collections.sort(names);
        return names;
    }
}
