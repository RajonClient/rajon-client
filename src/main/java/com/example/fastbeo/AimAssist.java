package com.example.fastbeo;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import org.lwjgl.input.Mouse;

/**
 * Gently pulls your camera toward the closest target inside your field of view.
 * You can always override it by moving the mouse. Settings are in the ClickGUI.
 */
public class AimAssist {
    public boolean enabled = false;

    private final Minecraft mc = Minecraft.getMinecraft();
    private long last = 0L;

    @SubscribeEvent
    public void onRender(TickEvent.RenderTickEvent e) {
        if (e.phase != TickEvent.Phase.START) return;

        // Time since the last frame, so the speed does not depend on FPS
        long now = System.nanoTime();
        double dt = last == 0L ? 0.0 : (now - last) / 1.0e9;
        last = now;

        if (!enabled) return;
        if (mc.thePlayer == null || mc.theWorld == null || mc.currentScreen != null) return;

        // Same lock as the other gameplay modules
        if (!FarReach.canRun()) {
            enabled = false;
            return;
        }

        if (dt <= 0.0 || dt > 0.25) return;
        if (Settings.bool("AimAssist", "Require click") && !Mouse.isButtonDown(0)) return;

        double range = Settings.get("AimAssist", "Range");
        double halfFov = Settings.get("AimAssist", "FOV") / 2.0;
        boolean playersOnly = Settings.bool("AimAssist", "Players only");
        boolean visibleOnly = Settings.bool("AimAssist", "Visible only");

        // Find the target closest to the crosshair
        float bestYaw = 0f, bestPitch = 0f;
        double bestAngle = Double.MAX_VALUE;
        boolean found = false;

        for (Object o : mc.theWorld.loadedEntityList) {
            if (!(o instanceof EntityLivingBase)) continue;
            EntityLivingBase en = (EntityLivingBase) o;
            if (en == mc.thePlayer || en.isDead || en.getHealth() <= 0f) continue;
            if (playersOnly && !(en instanceof EntityPlayer)) continue;
            if (mc.thePlayer.getDistanceToEntity(en) > range) continue;
            if (visibleOnly && !mc.thePlayer.canEntityBeSeen(en)) continue;

            // Aim at the upper body
            float[] a = Rotations.anglesTo(en.posX, en.posY + en.getEyeHeight() - 0.2, en.posZ);
            float dy = Rotations.yawDiff(a[0]);
            float dp = Rotations.pitchDiff(a[1]);
            if (Math.abs(dy) > halfFov || Math.abs(dp) > halfFov) continue;

            double angle = Math.sqrt(dy * dy + dp * dp);
            if (angle < bestAngle) {
                bestAngle = angle;
                bestYaw = a[0];
                bestPitch = a[1];
                found = true;
            }
        }

        if (!found) return;

        // Degrees we may turn this frame (speed is in degrees per tick)
        float maxStep = (float) (Settings.get("AimAssist", "Speed") * dt * 20.0);

        float dy = Rotations.yawDiff(bestYaw);
        float dp = Settings.bool("AimAssist", "Vertical") ? Rotations.pitchDiff(bestPitch) : 0f;

        // Slow down when close so it settles instead of snapping
        float yawStep = ease(dy, maxStep);
        float pitchStep = ease(dp, maxStep);
        Rotations.turn(yawStep, pitchStep);
    }

    private static float ease(float diff, float maxStep) {
        float abs = Math.abs(diff);
        float allowed = maxStep * Math.min(1f, 0.25f + abs / 20f);
        float step = Math.min(abs, allowed);
        return diff < 0 ? -step : step;
    }
}
