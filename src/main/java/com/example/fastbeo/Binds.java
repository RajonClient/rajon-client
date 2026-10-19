package com.example.fastbeo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;

import org.lwjgl.input.Keyboard;

/**
 * Your own key binds for the modules. Set them in the ClickGUI (right click a module),
 * they are saved to .minecraft/config/farreach_binds.properties.
 */
public class Binds {
    // Same order as Modules.NAMES: Reach, Scaffold, FastPlace, LegitScaffold,
    // AutoClicker, ClickDelay, Allow Multiplayer, HUD, ArrayList, AutoSprint, Fullbright, ESP, AimAssist, Rotations, NoHurtCam, TriggerBot, Freelook, Animations (0 = not bound)
    private static final int[] DEFAULTS = {
        Keyboard.KEY_Y, Keyboard.KEY_G, Keyboard.KEY_H, Keyboard.KEY_J,
        Keyboard.KEY_K, Keyboard.KEY_N, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
    };

    private static final int[] keys = new int[Modules.NAMES.length];

    private static File file() {
        return new File(Minecraft.getMinecraft().mcDataDir, "config/farreach_binds.properties");
    }

    public static int get(int module) {
        return keys[module];
    }

    public static void set(int module, int key) {
        keys[module] = key;
        save();
        ConfigManager.autoSave();
    }

    public static void load() {
        for (int i = 0; i < keys.length; i++) keys[i] = DEFAULTS[i];

        File f = file();
        if (!f.exists()) return;

        FileInputStream in = null;
        try {
            Properties p = new Properties();
            in = new FileInputStream(f);
            p.load(in);
            for (int i = 0; i < keys.length; i++) {
                String v = p.getProperty(Modules.NAMES[i]);
                if (v != null) keys[i] = Integer.parseInt(v.trim());
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
        File f = file();
        FileOutputStream out = null;
        try {
            if (f.getParentFile() != null) f.getParentFile().mkdirs();
            Properties p = new Properties();
            for (int i = 0; i < keys.length; i++) {
                p.setProperty(Modules.NAMES[i], String.valueOf(keys[i]));
            }
            out = new FileOutputStream(f);
            p.store(out, "FarReach binds (key codes, 0 = unbound)");
        } catch (Exception ex) {
            // Could not save: binds still work until the game closes
        } finally {
            if (out != null) {
                try { out.close(); } catch (Exception ignored) { }
            }
        }
    }

    @SubscribeEvent
    public void onKey(InputEvent.KeyInputEvent e) {
        if (!Keyboard.getEventKeyState()) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.currentScreen != null || mc.thePlayer == null) return;

        int key = Keyboard.getEventKey();
        if (key == 0) return;

        for (int i = 0; i < keys.length; i++) {
            if (keys[i] == key) Modules.toggle(i);
        }
    }
}
