package com.example.fastbeo;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import org.lwjgl.input.Keyboard;

/**
 * Holds the sprint key for you while you walk forward. The game itself still checks
 * hunger, collisions and sneaking, so it behaves like pressing sprint by hand.
 */
public class AutoSprint {
    public boolean enabled = false;

    private final Minecraft mc = Minecraft.getMinecraft();
    private boolean forcing = false;

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        if (mc.thePlayer == null || mc.theWorld == null) {
            forcing = false;
            return;
        }

        if (!enabled) {
            release();
            return;
        }

        // Same lock as the other gameplay modules
        if (!FarReach.canRun()) {
            enabled = false;
            release();
            return;
        }

        if (mc.currentScreen != null) return;

        boolean walking = mc.gameSettings.keyBindForward.isKeyDown()
                && !mc.gameSettings.keyBindSneak.isKeyDown();

        if (walking) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), true);
            forcing = true;
        } else {
            release();
        }
    }

    // Give the sprint key back to the real keyboard state
    private void release() {
        if (!forcing) return;
        int code = mc.gameSettings.keyBindSprint.getKeyCode();
        boolean down = code > 0 && Keyboard.isKeyDown(code);
        KeyBinding.setKeyBindState(code, down);
        forcing = false;
    }
}
