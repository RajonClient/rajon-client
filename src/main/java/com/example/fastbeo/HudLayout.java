package com.example.fastbeo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;

/**
 * Positions of the HUD elements. Edited with the HUD Editor (ClickGUI -> HUD Editor)
 * and saved to .minecraft/config/farreach_hud.properties.
 *
 * Everything is stored in scaled GUI coordinates. -1 means "use the default spot".
 * The ArrayList stores its RIGHT edge, all other elements store their LEFT edge.
 */
public class HudLayout {
    public static final int WATERMARK = 0, INFO = 1, TARGET = 2, ARRAYLIST = 3, NOTIFS = 4;
    public static final String[] NAMES = { "Watermark", "Info", "TargetHUD", "ArrayList", "Notifications" };

    private static final int[] x = { -1, -1, -1, -1, -1 };
    private static final int[] y = { -1, -1, -1, -1, -1 };

    private static int defX(int i, ScaledResolution sr) {
        switch (i) {
            case INFO: return sr.getScaledWidth() / 2 - 55;
            case TARGET: return sr.getScaledWidth() / 2 + 20;
            case ARRAYLIST: return sr.getScaledWidth() - 4;
            case NOTIFS: return sr.getScaledWidth() - 136;
            default: return 4;
        }
    }

    private static int defY(int i, ScaledResolution sr) {
        switch (i) {
            case INFO: return sr.getScaledHeight() / 2 + 30;
            case TARGET: return sr.getScaledHeight() / 2 + 20;
            case NOTIFS: return sr.getScaledHeight() - 50;
            default: return 4;
        }
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(v, Math.max(min, max)));
    }

    /** Left edge of the element's box, kept inside the screen. */
    public static int left(int i, int w, ScaledResolution sr) {
        int raw = x[i] >= 0 ? x[i] : defX(i, sr);
        int l = (i == ARRAYLIST) ? raw - w : raw;
        return clamp(l, 0, sr.getScaledWidth() - w);
    }

    /** Top edge of the element's box, kept inside the screen. */
    public static int top(int i, int h, ScaledResolution sr) {
        int raw = y[i] >= 0 ? y[i] : defY(i, sr);
        return clamp(raw, 0, sr.getScaledHeight() - h);
    }

    /** Move the element so its box has the given left/top corner. */
    public static void moveTo(int i, int left, int top, int w) {
        x[i] = (i == ARRAYLIST) ? left + w : left;
        y[i] = top;
    }

    public static int getRawX(int i) {
        return x[i];
    }

    public static int getRawY(int i) {
        return y[i];
    }

    public static void setRaw(int i, int nx, int ny) {
        x[i] = nx;
        y[i] = ny;
    }

    public static void reset() {
        for (int i = 0; i < x.length; i++) {
            x[i] = -1;
            y[i] = -1;
        }
        save();
    }

    private static File file() {
        return new File(Minecraft.getMinecraft().mcDataDir, "config/farreach_hud.properties");
    }

    public static void load() {
        File f = file();
        if (!f.exists()) return;

        FileInputStream in = null;
        try {
            Properties p = new Properties();
            in = new FileInputStream(f);
            p.load(in);
            for (int i = 0; i < NAMES.length; i++) {
                String sx = p.getProperty(NAMES[i] + ".x");
                String sy = p.getProperty(NAMES[i] + ".y");
                if (sx != null) x[i] = Integer.parseInt(sx.trim());
                if (sy != null) y[i] = Integer.parseInt(sy.trim());
            }
        } catch (Exception ex) {
            // Broken file: keep the defaults
        } finally {
            if (in != null) {
                try { in.close(); } catch (Exception ignored) { }
            }
        }
    }

    public static void save() {
        ConfigManager.autoSave();
        File f = file();
        FileOutputStream out = null;
        try {
            if (f.getParentFile() != null) f.getParentFile().mkdirs();
            Properties p = new Properties();
            for (int i = 0; i < NAMES.length; i++) {
                p.setProperty(NAMES[i] + ".x", String.valueOf(x[i]));
                p.setProperty(NAMES[i] + ".y", String.valueOf(y[i]));
            }
            out = new FileOutputStream(f);
            p.store(out, "FarReach HUD positions (-1 = default)");
        } catch (Exception ex) {
            // Could not save: positions still apply until the game closes
        } finally {
            if (out != null) {
                try { out.close(); } catch (Exception ignored) { }
            }
        }
    }
}
