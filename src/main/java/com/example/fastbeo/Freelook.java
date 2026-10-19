package com.example.fastbeo;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.MathHelper;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Look around with the mouse while your character keeps facing (and walking) the same way.
 *
 * How it works: only while a frame is being drawn, your player's rotation is swapped for the
 * camera angles; right after the frame the real rotation is put back. Game ticks (movement,
 * packets sent to the server) therefore always use your real facing, so this is a purely
 * visual feature and the server never sees anything different.
 *
 * Note: while Freelook is on, clicks follow where the camera points. Do not combine it
 * with AimAssist.
 */
public class Freelook {
    public boolean enabled = false;

    private final Minecraft mc = Minecraft.getMinecraft();

    private boolean active = false;
    private boolean changedView = false;
    private int savedView = 0;

    private float camYaw, camPitch;

    // Real rotation while it is swapped out
    private EntityPlayerSP swappedPlayer = null;
    private float realYaw, realPitch, realPrevYaw, realPrevPitch;

    @SubscribeEvent
    public void onRender(TickEvent.RenderTickEvent e) {
        EntityPlayerSP p = mc.thePlayer;

        if (e.phase == TickEvent.Phase.START) {
            if (p == null) {
                active = false;
                swappedPlayer = null;
                return;
            }

            if (enabled && !active) {
                // Start: the camera begins exactly where you are looking
                active = true;
                camYaw = p.rotationYaw;
                camPitch = p.rotationPitch;
                if (Settings.bool("Freelook", "Third person")) {
                    savedView = mc.gameSettings.thirdPersonView;
                    mc.gameSettings.thirdPersonView = 1;
                    changedView = true;
                }
            } else if (!enabled && active) {
                // Stop: give the view back
                active = false;
                if (changedView) {
                    mc.gameSettings.thirdPersonView = savedView;
                    changedView = false;
                }
                return;
            }

            if (!active) return;

            // Swap the camera angles in for this frame
            swappedPlayer = p;
            realYaw = p.rotationYaw;
            realPitch = p.rotationPitch;
            realPrevYaw = p.prevRotationYaw;
            realPrevPitch = p.prevRotationPitch;

            p.rotationYaw = camYaw;
            p.prevRotationYaw = camYaw;
            p.rotationPitch = camPitch;
            p.prevRotationPitch = camPitch;

        } else {
            if (swappedPlayer == null) return;

            // The mouse moved the (swapped in) camera angles during this frame: keep them
            if (p == swappedPlayer) {
                camYaw = p.rotationYaw;
                camPitch = MathHelper.clamp_float(p.rotationPitch, -90.0F, 90.0F);

                // Put the real rotation back
                p.rotationYaw = realYaw;
                p.prevRotationYaw = realPrevYaw;
                p.rotationPitch = realPitch;
                p.prevRotationPitch = realPrevPitch;
            }
            swappedPlayer = null;
        }
    }
}
