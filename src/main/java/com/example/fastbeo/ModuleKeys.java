package com.example.fastbeo;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;

import org.lwjgl.input.Keyboard;

/**
 * Toggle keys for every module. Change them in Options > Controls > FarReach.
 * (Scaffold keeps its own key, G, and the ClickGUI uses Right Shift.)
 */
public class ModuleKeys {
    private static final String CAT = "FarReach";

    public static final KeyBinding REACH       = new KeyBinding("Toggle Reach 20",     Keyboard.KEY_Y, CAT);
    public static final KeyBinding FAST_PLACE  = new KeyBinding("Toggle FastPlace",    Keyboard.KEY_V, CAT);
    public static final KeyBinding LEGIT       = new KeyBinding("Toggle LegitScaffold", Keyboard.KEY_F, CAT);
    public static final KeyBinding AUTOCLICKER = new KeyBinding("Toggle AutoClicker",  Keyboard.KEY_R, CAT);
    // Unbound by default so multiplayer is always a deliberate choice
    public static final KeyBinding MULTIPLAYER = new KeyBinding("Toggle Allow Multiplayer", Keyboard.KEY_NONE, CAT);

    private final Minecraft mc = Minecraft.getMinecraft();

    public static void register() {
        ClientRegistry.registerKeyBinding(REACH);
        ClientRegistry.registerKeyBinding(FAST_PLACE);
        ClientRegistry.registerKeyBinding(LEGIT);
        ClientRegistry.registerKeyBinding(AUTOCLICKER);
        ClientRegistry.registerKeyBinding(MULTIPLAYER);
    }

    @SubscribeEvent
    public void onKey(InputEvent.KeyInputEvent e) {
        if (mc.thePlayer == null) return;

        if (MULTIPLAYER.isPressed()) {
            FarReach.allowMultiplayer = !FarReach.allowMultiplayer;
            msg("Allow Multiplayer: " + onOff(FarReach.allowMultiplayer)
                    + (FarReach.allowMultiplayer ? " (only use where hacked clients are allowed)" : ""));
        }

        if (REACH.isPressed()) {
            if (!mc.isSingleplayer()) {
                msg("Reach 20 is singleplayer only.");
            } else {
                FarReach.reachEnabled = !FarReach.reachEnabled;
                msg("Reach 20: " + onOff(FarReach.reachEnabled));
            }
        }

        if (FAST_PLACE.isPressed()) {
            if (locked()) return;
            FarReach.fastPlace.enabled = !FarReach.fastPlace.enabled;
            msg("FastPlace: " + onOff(FarReach.fastPlace.enabled));
        }

        if (LEGIT.isPressed()) {
            if (locked()) return;
            FarReach.legitScaffold.enabled = !FarReach.legitScaffold.enabled;
            msg("LegitScaffold: " + onOff(FarReach.legitScaffold.enabled));
        }

        if (AUTOCLICKER.isPressed()) {
            if (locked()) return;
            FarReach.autoClicker.enabled = !FarReach.autoClicker.enabled;
            msg("AutoClicker: " + onOff(FarReach.autoClicker.enabled));
        }
    }

    private boolean locked() {
        if (!FarReach.canRun()) {
            msg("Locked in multiplayer. Turn on 'Allow Multiplayer' first.");
            return true;
        }
        return false;
    }

    private static String onOff(boolean b) {
        return b ? "ON" : "OFF";
    }

    private void msg(String t) {
        mc.thePlayer.addChatMessage(new ChatComponentText("[FarReach] " + t));
    }
}
