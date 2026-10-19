package com.example.fastbeo;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;

/**
 * Shared rotation helpers (used by AimAssist and by Scaffold when the Rotations module is on).
 * These turn your real camera, so you can see exactly where you are looking.
 */
public class Rotations {

    /** {yaw, pitch} needed to look from the player's eyes at a point in the world. */
    public static float[] anglesTo(double x, double y, double z) {
        return anglesTo(x, y, z, 1.0F);
    }

    /** Same, but uses the smoothed (between ticks) eye position for the given partial tick. */
    public static float[] anglesTo(double x, double y, double z, float partialTicks) {
        EntityPlayerSP p = Minecraft.getMinecraft().thePlayer;
        Vec3 eye = p.getPositionEyes(partialTicks);
        double dx = x - eye.xCoord;
        double dy = y - eye.yCoord;
        double dz = z - eye.zCoord;
        double flat = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, flat));
        return new float[] { yaw, pitch };
    }

    /** Signed yaw difference (-180..180) from the current view to the given yaw. */
    public static float yawDiff(float targetYaw) {
        EntityPlayerSP p = Minecraft.getMinecraft().thePlayer;
        return MathHelper.wrapAngleTo180_float(targetYaw - p.rotationYaw);
    }

    /** Signed pitch difference from the current view to the given pitch. */
    public static float pitchDiff(float targetPitch) {
        EntityPlayerSP p = Minecraft.getMinecraft().thePlayer;
        return targetPitch - p.rotationPitch;
    }

    /** Turns the camera by the given amounts (previous rotation moves too, so it stays smooth). */
    public static void turn(float yawStep, float pitchStep) {
        EntityPlayerSP p = Minecraft.getMinecraft().thePlayer;
        p.rotationYaw += yawStep;
        p.prevRotationYaw += yawStep;

        float newPitch = MathHelper.clamp_float(p.rotationPitch + pitchStep, -90.0F, 90.0F);
        float applied = newPitch - p.rotationPitch;
        p.rotationPitch = newPitch;
        p.prevRotationPitch += applied;
    }

    // ---- Tracked rotation (used by Scaffold) ----
    // How far Scaffold has turned the view away from where your mouse had it.
    // MovementFix uses this to keep your walking direction and to hand the view back.
    public static float offsetYaw = 0f;
    public static float offsetPitch = 0f;
    public static int lastUsed = -1000; // player tick of the last tracked turn

    /** Like turn(), but remembers how much the view was moved. */
    public static void turnTracked(float yawStep, float pitchStep) {
        EntityPlayerSP p = Minecraft.getMinecraft().thePlayer;
        float pitchBefore = p.rotationPitch;
        turn(yawStep, pitchStep);
        offsetYaw += yawStep;
        offsetPitch += p.rotationPitch - pitchBefore;
        lastUsed = p.ticksExisted;
    }

    /** Smoothly turns the view back toward where the mouse had it. */
    public static void release(float maxStep) {
        if (offsetYaw == 0f && offsetPitch == 0f) return;
        EntityPlayerSP p = Minecraft.getMinecraft().thePlayer;

        offsetYaw = MathHelper.wrapAngleTo180_float(offsetYaw);
        float yawStep = limit(-offsetYaw, maxStep);
        float pitchStep = limit(-offsetPitch, maxStep);

        float pitchBefore = p.rotationPitch;
        turn(yawStep, pitchStep);
        offsetYaw += yawStep;
        offsetPitch += p.rotationPitch - pitchBefore;

        if (Math.abs(offsetYaw) < 0.01f) offsetYaw = 0f;
        if (Math.abs(offsetPitch) < 0.01f) offsetPitch = 0f;
    }

    public static void clearOffsets() {
        offsetYaw = 0f;
        offsetPitch = 0f;
    }

    private static float limit(float v, float max) {
        return Math.max(-max, Math.min(max, v));
    }

    /** Turns toward fixed yaw/pitch angles, at most maxStep degrees on each axis. */
    public static void turnTo(float yaw, float pitch, float maxStep) {
        turnTracked(limit(yawDiff(yaw), maxStep), limit(pitchDiff(pitch), maxStep));
    }

    /** Turns toward a point, at most maxStep degrees on each axis. */
    public static void lookAt(Vec3 point, float maxStep) {
        float[] a = anglesTo(point.xCoord, point.yCoord, point.zCoord);
        turnTracked(limit(yawDiff(a[0]), maxStep), limit(pitchDiff(a[1]), maxStep));
    }
}
