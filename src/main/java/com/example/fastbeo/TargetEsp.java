package com.example.fastbeo;

import java.awt.Color;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import org.lwjgl.opengl.GL11;

/**
 * TargetESP: draws a marker on your current target only (the one you hit or aim at),
 * for a few seconds after the last hit. It does not show anything you could not already see.
 *
 *  Ring   - a ring that glides up and down the target with a fading trail
 *  Box    - a translucent box with an outline
 *  Circle - a pulsing disc on the ground under the target
 */
public class TargetEsp {
    private static final String M = "TargetESP";

    private static final int ACCENT = 0x4FC3F7;
    private static final int ACCENT2 = 0xB388FF;

    public boolean enabled = false;

    private final Minecraft mc = Minecraft.getMinecraft();

    private EntityLivingBase target = null;
    private long seen = 0L;

    @SubscribeEvent
    public void onAttack(AttackEntityEvent e) {
        if (enabled && e.entityPlayer == mc.thePlayer && e.target instanceof EntityLivingBase) {
            setTarget((EntityLivingBase) e.target);
        }
    }

    private void setTarget(EntityLivingBase t) {
        target = t;
        seen = System.currentTimeMillis();
    }

    @SubscribeEvent
    public void onRender(RenderWorldLastEvent e) {
        if (!enabled || mc.thePlayer == null || mc.theWorld == null) return;

        // Also pick up whatever you are aiming at
        MovingObjectPosition mop = mc.objectMouseOver;
        if (mop != null && mop.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY
                && mop.entityHit instanceof EntityLivingBase) {
            setTarget((EntityLivingBase) mop.entityHit);
        }

        if (target == null) return;
        long duration = (long) (Settings.get(M, "Duration") * 1000.0);
        if (target.isDead || target == mc.thePlayer || System.currentTimeMillis() - seen > duration) {
            target = null;
            return;
        }
        if (Settings.bool(M, "Players only") && !(target instanceof EntityPlayer)) return;

        Entity view = mc.getRenderViewEntity();
        if (view == null) return;

        float pt = e.partialTicks;
        double vx = view.lastTickPosX + (view.posX - view.lastTickPosX) * pt;
        double vy = view.lastTickPosY + (view.posY - view.lastTickPosY) * pt;
        double vz = view.lastTickPosZ + (view.posZ - view.lastTickPosZ) * pt;

        // Target position relative to the camera, smoothed between ticks
        double x = target.lastTickPosX + (target.posX - target.lastTickPosX) * pt - vx;
        double y = target.lastTickPosY + (target.posY - target.lastTickPosY) * pt - vy;
        double z = target.lastTickPosZ + (target.posZ - target.lastTickPosZ) * pt - vz;

        double w = target.width / 2.0;
        double h = target.height;
        int rgb = pickColor();
        double speed = Settings.get(M, "Speed");

        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.depthMask(false);

        int mode = (int) Settings.get(M, "Mode");
        if (mode == 1) {
            drawBox(x, y, z, w, h, rgb);
        } else if (mode == 2) {
            drawCircle(x, y, z, w, rgb, speed);
        } else {
            drawRing(x, y, z, w * 1.5, h, rgb, speed);
        }

        GlStateManager.depthMask(true);
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    // ---------------------------------------------------------------- colors

    private int pickColor() {
        int c = (int) Settings.get(M, "Color");
        if (c == 1) { // Theme: slow blend between two colors
            float f = (float) ((Math.sin(System.currentTimeMillis() / 700.0) + 1.0) / 2.0);
            int r = (int) (((ACCENT >> 16) & 255) * (1f - f) + ((ACCENT2 >> 16) & 255) * f);
            int g = (int) (((ACCENT >> 8) & 255) * (1f - f) + ((ACCENT2 >> 8) & 255) * f);
            int b = (int) ((ACCENT & 255) * (1f - f) + (ACCENT2 & 255) * f);
            return (r << 16) | (g << 8) | b;
        }
        if (c == 2) return 0xFFFFFF; // White

        // Health: green when healthy, red when low
        float max = Math.max(1f, target.getMaxHealth());
        float f = Math.max(0f, Math.min(1f, target.getHealth() / max));
        return Color.HSBtoRGB(f * 0.33f, 0.8f, 1f) & 0xFFFFFF;
    }

    private static void color(int rgb, float alpha) {
        GL11.glColor4f(((rgb >> 16) & 255) / 255f, ((rgb >> 8) & 255) / 255f, (rgb & 255) / 255f, alpha);
    }

    // ---------------------------------------------------------------- modes

    private void drawRing(double x, double y, double z, double radius, double h, int rgb, double speed) {
        double t = System.currentTimeMillis() / 1000.0;
        int tail = 8;
        GL11.glLineWidth(2.5f);

        for (int k = 0; k < tail; k++) {
            // Each ring follows the same up/down motion a little later, which makes the trail
            double phase = t * speed * 1.6 - k * 0.07;
            double f = (Math.sin(phase * Math.PI) + 1.0) / 2.0;
            double ry = y + f * h;
            float alpha = (1f - k / (float) tail) * 0.9f;

            color(rgb, alpha);
            GL11.glBegin(GL11.GL_LINE_LOOP);
            for (int i = 0; i < 48; i++) {
                double a = Math.PI * 2.0 * i / 48.0;
                GL11.glVertex3d(x + Math.cos(a) * radius, ry, z + Math.sin(a) * radius);
            }
            GL11.glEnd();
        }
    }

    private void drawCircle(double x, double y, double z, double w, int rgb, double speed) {
        double pulse = 0.9 + 0.1 * Math.sin(System.currentTimeMillis() / 1000.0 * speed * 6.0);
        double radius = w * 1.6 * pulse;
        double cy = y + 0.02;

        // Filled disc
        color(rgb, 0.25f);
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        GL11.glVertex3d(x, cy, z);
        for (int i = 0; i <= 48; i++) {
            double a = Math.PI * 2.0 * i / 48.0;
            GL11.glVertex3d(x + Math.cos(a) * radius, cy, z + Math.sin(a) * radius);
        }
        GL11.glEnd();

        // Outline
        GL11.glLineWidth(2.5f);
        color(rgb, 0.95f);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        for (int i = 0; i < 48; i++) {
            double a = Math.PI * 2.0 * i / 48.0;
            GL11.glVertex3d(x + Math.cos(a) * radius, cy, z + Math.sin(a) * radius);
        }
        GL11.glEnd();
    }

    private void drawBox(double x, double y, double z, double w, double h, int rgb) {
        double p = 0.05;
        double x1 = x - w - p, x2 = x + w + p;
        double y1 = y - p, y2 = y + h + p;
        double z1 = z - w - p, z2 = z + w + p;

        // Translucent fill (six faces)
        color(rgb, 0.22f);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex3d(x1, y1, z1); GL11.glVertex3d(x2, y1, z1); GL11.glVertex3d(x2, y1, z2); GL11.glVertex3d(x1, y1, z2);
        GL11.glVertex3d(x1, y2, z1); GL11.glVertex3d(x1, y2, z2); GL11.glVertex3d(x2, y2, z2); GL11.glVertex3d(x2, y2, z1);
        GL11.glVertex3d(x1, y1, z1); GL11.glVertex3d(x1, y2, z1); GL11.glVertex3d(x2, y2, z1); GL11.glVertex3d(x2, y1, z1);
        GL11.glVertex3d(x1, y1, z2); GL11.glVertex3d(x2, y1, z2); GL11.glVertex3d(x2, y2, z2); GL11.glVertex3d(x1, y2, z2);
        GL11.glVertex3d(x1, y1, z1); GL11.glVertex3d(x1, y1, z2); GL11.glVertex3d(x1, y2, z2); GL11.glVertex3d(x1, y2, z1);
        GL11.glVertex3d(x2, y1, z1); GL11.glVertex3d(x2, y2, z1); GL11.glVertex3d(x2, y2, z2); GL11.glVertex3d(x2, y1, z2);
        GL11.glEnd();

        // Outline
        GL11.glLineWidth(2.0f);
        RenderGlobal.drawOutlinedBoundingBox(new AxisAlignedBB(x1, y1, z1, x2, y2, z2),
                (rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255, 255);
    }
}
