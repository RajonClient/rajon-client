package com.example.fastbeo;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;

/** One place that knows every module, its state, and how to toggle it. */
public class Modules {
    public static final String[] NAMES = {
        "Reach", "Scaffold", "FastPlace", "LegitScaffold",
        "AutoClicker", "ClickDelay", "Allow Multiplayer", "HUD", "ArrayList",
        "AutoSprint", "Fullbright", "ESP",
        "AimAssist", "Rotations", "NoHurtCam", "TriggerBot", "Freelook", "Animations"
    };

    // Panels shown in the ClickGUI, in this order
    public static final String[] CATEGORY_NAMES = { "Combat", "Movement", "Player", "Render", "Misc" };

    // Category of each module (same order as NAMES): index into CATEGORY_NAMES
    public static final int[] CATEGORY = {
        0, // Reach 20
        2, // Scaffold
        2, // FastPlace
        1, // LegitScaffold
        0, // AutoClicker
        0, // ClickDelay
        4, // Allow Multiplayer
        3, // HUD
        3, // ArrayList
        1, // AutoSprint
        3, // Fullbright
        3, // ESP
        0, // AimAssist
        2, // Rotations
        3, // NoHurtCam
        0, // TriggerBot
        3, // Freelook
        3  // Animations
    };

    // Short description of each module, shown when you hover it in the ClickGUI
    public static final String[] DESCRIPTIONS = {
        "Hit and place from further away (3.35 to 10 blocks).",
        "Automatically places blocks under your feet.",
        "Removes the delay between placing blocks.",
        "Sneaks at block edges so you do not fall.",
        "Hold left click to click repeatedly.",
        "Removes the left and right click cooldowns.",
        "Unlocks modules on servers. Only where hacked clients are allowed.",
        "Shows the watermark, info panel, target panel and notifications.",
        "Lists the modules you have turned on.",
        "Holds the sprint key while you walk forward.",
        "Makes everything fully lit.",
        "Draws boxes around players, visible through walls.",
        "Gently turns your camera toward the closest target in view.",
        "Scaffold turns your camera toward the block it places.",
        "Removes the camera shake when you take damage.",
        "Attacks automatically while your crosshair is on a target.",
        "Look around freely while your character keeps facing the same way.",
        "Block animations and item position/size in first person."
    };

    public static boolean isOn(int i) {
        switch (i) {
            case 0: return FarReach.reachEnabled;
            case 1: return FarReach.scaffold.enabled;
            case 2: return FarReach.fastPlace.enabled;
            case 3: return FarReach.legitScaffold.enabled;
            case 4: return FarReach.autoClicker.enabled;
            case 5: return FarReach.clickDelay.enabled;
            case 6: return FarReach.allowMultiplayer;
            case 7: return FarReach.hudEnabled;
            case 8: return FarReach.arrayListEnabled;
            case 9: return FarReach.autoSprint.enabled;
            case 10: return FarReach.fullbright.enabled;
            case 11: return FarReach.esp.enabled;
            case 12: return FarReach.aimAssist.enabled;
            case 13: return FarReach.rotationsEnabled;
            case 14: return FarReach.noHurtCam.enabled;
            case 15: return FarReach.triggerBot.enabled;
            case 16: return FarReach.freelook.enabled;
            case 17: return FarReach.animations.enabled;
            default: return false;
        }
    }

    private static void set(int i, boolean v) {
        switch (i) {
            case 0: FarReach.reachEnabled = v; break;
            case 1: FarReach.scaffold.enabled = v; break;
            case 2: FarReach.fastPlace.enabled = v; break;
            case 3: FarReach.legitScaffold.enabled = v; break;
            case 4: FarReach.autoClicker.enabled = v; break;
            case 5: FarReach.clickDelay.enabled = v; break;
            case 6: FarReach.allowMultiplayer = v; break;
            case 7: FarReach.hudEnabled = v; break;
            case 8: FarReach.arrayListEnabled = v; break;
            case 9: FarReach.autoSprint.enabled = v; break;
            case 10: FarReach.fullbright.enabled = v; break;
            case 11: FarReach.esp.enabled = v; break;
            case 12: FarReach.aimAssist.enabled = v; break;
            case 13: FarReach.rotationsEnabled = v; break;
            case 14: FarReach.noHurtCam.enabled = v; break;
            case 15: FarReach.triggerBot.enabled = v; break;
            case 16: FarReach.freelook.enabled = v; break;
            case 17: FarReach.animations.enabled = v; break;
            default: break;
        }
        ConfigManager.autoSave();
    }

    /** Sets a module without messages (used when loading configs). */
    public static void setState(int i, boolean v) {
        set(i, v);
    }

    public static void toggle(int i) {
        if (i < 0 || i >= NAMES.length) return;
        Minecraft mc = Minecraft.getMinecraft();

        // Allow Multiplayer, HUD, ArrayList, Fullbright, NoHurtCam, Freelook and Animations (visual only) and Rotations (a flag) are never locked
        if (i == 6 || i == 7 || i == 8 || i == 10 || i == 13 || i == 14 || i == 16 || i == 17) {
            set(i, !isOn(i));
            msg(NAMES[i] + ": " + onOff(isOn(i))
                    + (i == 6 && isOn(i) ? " (only use where hacked clients are allowed)" : ""));
            Notifications.add(NAMES[i], onOff(isOn(i)), isOn(i));
            return;
        }

        if (!FarReach.canRun()) {
            msg("Locked in multiplayer. Turn on 'Allow Multiplayer' first.");
            Notifications.add(NAMES[i], "Locked in multiplayer", false);
            return;
        }

        set(i, !isOn(i));
        msg(NAMES[i] + ": " + onOff(isOn(i)));
        Notifications.add(NAMES[i], onOff(isOn(i)), isOn(i));
    }

    /** Short extra info shown after the module name in the ArrayList (null = none). */
    public static String tag(int i) {
        switch (i) {
            case 0: return String.format("%.2f", Settings.get("Reach", "Distance"));
            case 1: return Settings.display("Scaffold", "Mode");
            case 4: return cps("AutoClicker");
            case 11: return "Box";
            case 12: return "Smooth " + Settings.display("AimAssist", "Smoothness");
            case 13: return Settings.display("Rotations", "Mode");
            case 15: return cps("TriggerBot");
            case 17: return Settings.display("Animations", "Style");
            default: return null;
        }
    }

    private static String cps(String module) {
        int a = (int) Settings.get(module, "Min CPS");
        int b = (int) Settings.get(module, "Max CPS");
        return Math.min(a, b) + "-" + Math.max(a, b);
    }

    private static String onOff(boolean b) {
        return b ? "ON" : "OFF";
    }

    private static void msg(String t) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer != null) {
            mc.thePlayer.addChatMessage(new ChatComponentText("[" + Client.NAME + "] " + t));
        }
    }
}
