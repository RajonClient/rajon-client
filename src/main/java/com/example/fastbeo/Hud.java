package com.example.fastbeo;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import org.lwjgl.opengl.GL11;

public class Hud {
    private static final int ACCENT = 0xFF4FC3F7;
    private static final int BG = 0xB0101010;
    private static final long TARGET_TIME = 3000L;

    private final Minecraft mc = Minecraft.getMinecraft();

    private EntityLivingBase target;
    private long targetSeen;
    private float shownHealth = -1f;

    private double bps = 0.0;

    @SubscribeEvent
    public void onAttack(AttackEntityEvent e) {
        if (e.entityPlayer == mc.thePlayer && e.target instanceof EntityLivingBase) {
            setTarget((EntityLivingBase) e.target);
        }
    }

    private void setTarget(EntityLivingBase t) {
        if (t != target) shownHealth = -1f;
        target = t;
        targetSeen = System.currentTimeMillis();
    }

    @SubscribeEvent
    public void onRender(RenderGameOverlayEvent.Post e) {
        if (e.type != RenderGameOverlayEvent.ElementType.ALL) return;
        if (!FarReach.hudEnabled) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (mc.gameSettings.showDebugInfo) return;

        ScaledResolution sr = e.resolution;
        if (Settings.bool("HUD", "Watermark")) drawWatermark(sr);
        if (Settings.bool("HUD", "Info")) drawInfo(sr);
        if (Settings.bool("HUD", "TargetHUD")) drawTarget(sr);
        drawArrayList(sr);
        if (Settings.bool("HUD", "Notifications")) drawNotifications(sr);
    }

    // ---------------------------------------------------------------- sizes (also used by the HUD editor)

    /** {width, height} of an element's box. */
    public static int[] size(int element) {
        FontRenderer fr = Minecraft.getMinecraft().fontRendererObj;
        switch (element) {
            case HudLayout.WATERMARK:
                return new int[] { 12 + fr.getStringWidth(Client.NAME) + fr.getStringWidth(watermarkRest()), 16 };
            case HudLayout.INFO:
                return new int[] { 110, 34 };
            case HudLayout.TARGET:
                return new int[] { 150, 40 };
            case HudLayout.ARRAYLIST:
                return ArrayListRenderer.size();
            default:
                return new int[] { 130, 24 };
        }
    }

    private static String watermarkRest() {
        Minecraft mc = Minecraft.getMinecraft();
        String user = mc.getSession().getUsername();
        String server = mc.getCurrentServerData() != null
                ? mc.getCurrentServerData().serverIP : "singleplayer";
        int ping = 0;
        if (mc.getNetHandler() != null && mc.thePlayer != null) {
            NetworkPlayerInfo info = mc.getNetHandler().getPlayerInfo(mc.thePlayer.getUniqueID());
            if (info != null) ping = info.getResponseTime();
        }
        return "  |  " + user + "  |  " + server + "  |  " + ping + "ms";
    }

    // ---------------------------------------------------------------- watermark

    private void drawWatermark(ScaledResolution sr) {
        FontRenderer fr = mc.fontRendererObj;
        int[] s = size(HudLayout.WATERMARK);
        int x = HudLayout.left(HudLayout.WATERMARK, s[0], sr);
        int y = HudLayout.top(HudLayout.WATERMARK, s[1], sr);
        String rest = watermarkRest();

        rounded(x, y, s[0], s[1], BG);
        Gui.drawRect(x + 1, y + s[1] - 1, x + s[0] - 1, y + s[1], ACCENT);

        String first = Client.NAME.substring(0, 1);
        String others = Client.NAME.substring(1);
        fr.drawStringWithShadow(first, x + 6, y + 4, ACCENT);
        fr.drawStringWithShadow(others, x + 6 + fr.getStringWidth(first), y + 4, 0xFFFFFF);
        fr.drawStringWithShadow(rest, x + 6 + fr.getStringWidth(Client.NAME), y + 4, 0xAAAAAA);
    }

    // ---------------------------------------------------------------- player info

    private void drawInfo(ScaledResolution sr) {
        ItemStack held = mc.thePlayer.getHeldItem();
        if (held == null || !(held.getItem() instanceof ItemBlock)) return;

        FontRenderer fr = mc.fontRendererObj;

        // Speed in blocks per second, smoothed
        double dx = mc.thePlayer.posX - mc.thePlayer.prevPosX;
        double dz = mc.thePlayer.posZ - mc.thePlayer.prevPosZ;
        double inst = Math.sqrt(dx * dx + dz * dz) * 20.0;
        bps += (inst - bps) * 0.1;

        int[] s = size(HudLayout.INFO);
        int w = s[0];
        int x = HudLayout.left(HudLayout.INFO, w, sr);
        int y = HudLayout.top(HudLayout.INFO, s[1], sr);
        int count = held.stackSize;

        rounded(x, y, w, s[1], BG);
        fr.drawStringWithShadow(count + " blocks", x + 6, y + 5, 0xFFFFFF);

        int barW = w - 12;
        Gui.drawRect(x + 6, y + 16, x + 6 + barW, y + 18, 0x40FFFFFF);
        int fill = (int) (barW * Math.min(1f, count / 64f));
        Gui.drawRect(x + 6, y + 16, x + 6 + fill, y + 18, ACCENT);

        String line = Math.round(mc.thePlayer.rotationPitch) + "\u00b0  |  "
                + String.format("%.2f", bps) + " bps";
        fr.drawStringWithShadow(line, x + 6, y + 22, 0xAAAAAA);
    }

    // ---------------------------------------------------------------- target HUD

    private void drawTarget(ScaledResolution sr) {
        // Also pick up whatever you are looking at
        MovingObjectPosition mop = mc.objectMouseOver;
        if (mop != null && mop.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY
                && mop.entityHit instanceof EntityLivingBase) {
            setTarget((EntityLivingBase) mop.entityHit);
        }

        if (target == null) return;
        if (target.isDead || System.currentTimeMillis() - targetSeen > TARGET_TIME) {
            target = null;
            return;
        }

        FontRenderer fr = mc.fontRendererObj;

        int[] s = size(HudLayout.TARGET);
        int w = s[0], h = s[1];
        int x = HudLayout.left(HudLayout.TARGET, w, sr);
        int y = HudLayout.top(HudLayout.TARGET, h, sr);

        rounded(x, y, w, h, BG);

        // Head
        int hx = x + 6, hy = y + 8;
        if (target instanceof AbstractClientPlayer) {
            mc.getTextureManager().bindTexture(((AbstractClientPlayer) target).getLocationSkin());
            GlStateManager.enableBlend();
            GlStateManager.color(1f, 1f, 1f, 1f);
            Gui.drawScaledCustomSizeModalRect(hx, hy, 8f, 8f, 8, 8, 24, 24, 64f, 64f);
            Gui.drawScaledCustomSizeModalRect(hx, hy, 40f, 8f, 8, 8, 24, 24, 64f, 64f);
        } else {
            Gui.drawRect(hx, hy, hx + 24, hy + 24, 0xFF555555);
        }

        // Name and distance
        fr.drawStringWithShadow(target.getName(), x + 36, y + 6, 0xFFFFFF);
        String dist = String.format("%.1fm", mc.thePlayer.getDistanceToEntity(target));
        fr.drawStringWithShadow(dist, x + 36, y + 17, 0xAAAAAA);

        // Armor icons (players only)
        if (target instanceof EntityPlayer) {
            ItemStack[] armor = ((EntityPlayer) target).inventory.armorInventory;
            int ax = x + 36;
            for (int i = 3; i >= 0; i--) {
                if (armor[i] != null) {
                    GlStateManager.pushMatrix();
                    GlStateManager.translate(ax, y + 27, 0);
                    GlStateManager.scale(0.5f, 0.5f, 0.5f);
                    RenderHelper.enableGUIStandardItemLighting();
                    mc.getRenderItem().renderItemAndEffectIntoGUI(armor[i], 0, 0);
                    RenderHelper.disableStandardItemLighting();
                    GlStateManager.popMatrix();
                    ax += 10;
                }
            }
            GlStateManager.disableLighting();
            GlStateManager.enableBlend();
        }

        // Health ring
        float max = Math.max(1f, target.getMaxHealth());
        float hp = Math.max(0f, target.getHealth());
        if (shownHealth < 0f) shownHealth = hp;
        shownHealth += (hp - shownHealth) * 0.15f;
        double frac = Math.min(1.0, shownHealth / max);

        double cx = x + w - 20, cy = y + h / 2.0;
        ring(cx, cy, 11, 1.0, 0x60FFFFFF, 3f);
        int color = frac > 0.5 ? 0xFF66BB6A : (frac > 0.25 ? 0xFFFFCA28 : 0xFFEF5350);
        ring(cx, cy, 11, frac, color, 3f);

        String hpText = String.valueOf(Math.round(hp));
        fr.drawStringWithShadow(hpText, (int) (cx - fr.getStringWidth(hpText) / 2.0), (int) (cy - 4), 0xFFFFFF);
    }

    // ---------------------------------------------------------------- arraylist

    private void drawArrayList(ScaledResolution sr) {
        if (!FarReach.arrayListEnabled) return;
        ArrayListRenderer.draw(sr);
    }

    // ---------------------------------------------------------------- notifications

    private void drawNotifications(ScaledResolution sr) {
        FontRenderer fr = mc.fontRendererObj;
        List<Notifications.Entry> list = Notifications.active();
        long now = System.currentTimeMillis();

        int[] s = size(HudLayout.NOTIFS);
        int w = s[0], h = s[1];
        int baseX = HudLayout.left(HudLayout.NOTIFS, w, sr);
        int baseY = HudLayout.top(HudLayout.NOTIFS, h, sr);

        int n = list.size();
        for (int i = 0; i < n; i++) {
            Notifications.Entry en = list.get(n - 1 - i); // newest at the base, older ones above it
            long age = now - en.time;

            double slide = 0.0;
            if (age < 250) slide = 1.0 - age / 250.0;
            else if (age > Notifications.LIFETIME - 250) {
                slide = (age - (Notifications.LIFETIME - 250)) / 250.0;
            }

            int x = baseX + (int) (slide * (sr.getScaledWidth() - baseX + 4));
            int y = baseY - i * (h + 4);

            rounded(x, y, w, h, BG);
            Gui.drawRect(x, y + 1, x + 2, y + h - 1, en.good ? 0xFF66BB6A : 0xFFEF5350);
            fr.drawStringWithShadow(en.title, x + 8, y + 4, 0xFFFFFF);
            fr.drawStringWithShadow(en.text, x + 8, y + 14, en.good ? 0xFF66BB6A : 0xFFEF5350);
        }
    }

    // ---------------------------------------------------------------- helpers

    private static void rounded(int x, int y, int w, int h, int color) {
        Gui.drawRect(x + 1, y, x + w - 1, y + h, color);
        Gui.drawRect(x, y + 1, x + w, y + h - 1, color);
    }

    private static void ring(double cx, double cy, double radius, double frac, int argb, float width) {
        float a = (argb >> 24 & 255) / 255f;
        float r = (argb >> 16 & 255) / 255f;
        float g = (argb >> 8 & 255) / 255f;
        float b = (argb & 255) / 255f;

        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GL11.glColor4f(r, g, b, a);
        GL11.glLineWidth(width);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);

        int steps = Math.max(1, (int) (frac * 60));
        GL11.glBegin(GL11.GL_LINE_STRIP);
        for (int i = 0; i <= steps; i++) {
            double ang = -Math.PI / 2 + 2 * Math.PI * frac * i / steps;
            GL11.glVertex2d(cx + Math.cos(ang) * radius, cy + Math.sin(ang) * radius);
        }
        GL11.glEnd();

        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GlStateManager.enableTexture2D();
        GlStateManager.color(1f, 1f, 1f, 1f);
    }
}
