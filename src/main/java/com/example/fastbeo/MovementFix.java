package com.example.fastbeo;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.util.MovementInputFromOptions;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Fixes the movement problem of the Rotations module.
 *
 * Minecraft moves you relative to your yaw, so when Scaffold turned the camera
 * (for example 180 degrees in Backwards mode) pressing W suddenly walked you
 * the other way. This class:
 *  1. replaces the player's movement input with one that rotates your W/A/S/D
 *     by the same angle in the opposite direction, so you keep walking exactly
 *     where you meant to, and
 *  2. smoothly hands the camera back to where your mouse had it once Scaffold
 *     stops using the rotation.
 */
public class MovementFix {
    private final Minecraft mc = Minecraft.getMinecraft();

    /** Normal keyboard input, plus the correction for the turned view. */
    private static class FixedInput extends MovementInputFromOptions {
        FixedInput(GameSettings settings) {
            super(settings);
        }

        @Override
        public void updatePlayerMoveState() {
            super.updatePlayerMoveState();

            if (!Settings.bool("Rotations", "Movement fix")) return;

            float offset = Rotations.offsetYaw;
            if (offset == 0f) return;

            double rad = Math.toRadians(offset);
            double cos = Math.cos(rad);
            double sin = Math.sin(rad);

            float strafe = this.moveStrafe;
            float forward = this.moveForward;

            this.moveStrafe = (float) (strafe * cos + forward * sin);
            this.moveForward = (float) (-strafe * sin + forward * cos);
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (mc.thePlayer == null) {
            Rotations.clearOffsets();
            return;
        }

        if (e.phase == TickEvent.Phase.START) {
            // The game creates a fresh input for every new player (join, respawn): reinstall ours
            if (!(mc.thePlayer.movementInput instanceof FixedInput)) {
                mc.thePlayer.movementInput = new FixedInput(mc.gameSettings);
            }
            return;
        }

        // Scaffold has not used the rotation for a moment: give the view back smoothly
        if (mc.thePlayer.ticksExisted - Rotations.lastUsed > 2) {
            Rotations.release((float) Settings.get("Rotations", "Speed"));
        }
    }
}
