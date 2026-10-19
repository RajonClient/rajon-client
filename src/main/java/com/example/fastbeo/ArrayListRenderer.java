package com.example.fastbeo;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;

/**
 * ArrayList in the style of Rise: right aligned, sorted by width, every module slides in
 * from the side, rows move smoothly when the list changes, a dark translucent background
 * with a soft colored glow, a colored bar on the right edge and a gray tag after the name
 * (for example "AutoClicker 8-12"). Options are in the ClickGUI under ArrayList.
 */
public class ArrayListRenderer {
    private static final int ROW_H = 13;
    private static final int PAD = 4;
    private static final int BAR = 2;

    private static final int ACCENT = 0x4FC3F7;
    private static final int ACCENT2 = 0xB388FF;

    // Per-module animation state
    private static final float[] slide = new float[Modules.NAMES.length]; // 0 = hidden, 1 = shown
    private static final float[] ypos = new float[Modules.NAMES.length];  // current row offset
    private static final boolean[] shown = new boolean[Modules.NAMES.length];
    private static long last = 0L;

    // Allow Multiplayer, HUD and ArrayList themselves are not listed
    private static boolean skip(int i) {
        return i == 6 || i == 7 || i == 8;
    }

    private static String tagOf(int i) {
        if (!Settings.bool("ArrayList", "Tags")) return null;
        return Modules.tag(i);
    }

    private static int textWidth(int i) {
        FontRenderer fr = Minecraft.getMinecraft().fontRendererObj;
        int w = fr.getStringWidth(Modules.NAMES[i]);
        String tag = tagOf(i);
        if (tag != null) w += fr.getStringWidth(" " + tag);
        return w;
    }

    /** {width, height} of the list for the modules that are on right now (used by the HUD editor). */
    public static int[] size() {
        int n = 0, max = 0;
        for (int i = 0; i < Modules.NAMES.length; i++) {
            if (skip(i) || !Modules.isOn(i)) continue;
            n++;
            max = Math.max(max, textWidth(i));
        }
        if (n == 0) return new int[] { 70, 24 };
        return new int[] { max + PAD * 2 + BAR, n * ROW_H };
    }

    private static int mix(int c1, int c2, float f) {
        int r = (int) (((c1 >> 16) & 255) * (1f - f) + ((c2 >> 16) & 255) * f);
        int g = (int) (((c1 >> 8) & 255) * (1f - f) + ((c2 >> 8) & 255) * f);
        int b = (int) ((c1 & 255) * (1f - f) + (c2 & 255) * f);
        return (r << 16) | (g << 8) | b;
    }

    private static int argb(int rgb, float alpha) {
        int a = Math.max(0, Math.min(255, (int) (alpha * 255f)));
        return (a << 24) | (rgb & 0xFFFFFF);
    }

    // Color of the row at a position in the list
    private static int themeColor(int mode, int index) {
        long t = System.currentTimeMillis();
        if (mode == 1) { // Rainbow
            float hue = ((t / 20 + index * 12) % 360) / 360f;
            return Color.HSBtoRGB(hue, 0.55f, 1f) & 0xFFFFFF;
        }
        if (mode == 2) return ACCENT; // Static
        // Gradient: slowly flowing between two colors
        float f = (float) ((Math.sin(t / 1000.0 * 1.2 + index * 0.45) + 1.0) / 2.0);
        return mix(ACCENT, ACCENT2, f);
    }

    public static void draw(ScaledResolution sr) {
        Minecraft mc = Minecraft.getMinecraft();
        final FontRenderer fr = mc.fontRendererObj;

        // Frame time, so the animation speed does not depend on FPS
        long now = System.nanoTime();
        double dt = last == 0L ? 0.0 : Math.min(0.1, (now - last) / 1.0e9);
        last = now;

        boolean animate = Settings.bool("ArrayList", "Animations");
        boolean background = Settings.bool("ArrayList", "Background");
        int mode = (int) Settings.get("ArrayList", "Color");

        // Update slide animation and collect the rows that are still visible
        List<Integer> visible = new ArrayList<Integer>();
        double slideRate = 1.0 - Math.exp(-14.0 * dt);
        for (int i = 0; i < Modules.NAMES.length; i++) {
            if (skip(i)) continue;
            float target = Modules.isOn(i) ? 1f : 0f;
            if (!animate) {
                slide[i] = target;
            } else {
                slide[i] += (target - slide[i]) * (float) slideRate;
                if (Math.abs(target - slide[i]) < 0.01f) slide[i] = target;
            }
            if (slide[i] > 0f) {
                visible.add(i);
            } else {
                shown[i] = false;
            }
        }
        if (visible.isEmpty()) return;

        // Longest first
        Collections.sort(visible, new Comparator<Integer>() {
            public int compare(Integer a, Integer b) {
                return textWidth(b) - textWidth(a);
            }
        });

        int[] sz = size();
        int left = HudLayout.left(HudLayout.ARRAYLIST, sz[0], sr);
        int top = HudLayout.top(HudLayout.ARRAYLIST, sz[1], sr);
        int right = left + sz[0];

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        double moveRate = 1.0 - Math.exp(-16.0 * dt);
        for (int k = 0; k < visible.size(); k++) {
            int i = visible.get(k);

            // Smooth vertical movement when the list changes
            float targetY = k * ROW_H;
            if (!shown[i]) {
                ypos[i] = targetY;
                shown[i] = true;
            } else if (animate) {
                ypos[i] += (targetY - ypos[i]) * (float) moveRate;
            } else {
                ypos[i] = targetY;
            }

            float sl = slide[i];
            float ease = 1f - (1f - sl) * (1f - sl) * (1f - sl); // ease out
            float a = Math.min(1f, sl * 1.5f);

            String name = Modules.NAMES[i];
            String tag = tagOf(i);
            int w = textWidth(i) + PAD * 2 + BAR;

            int x2 = right + (int) ((1f - ease) * (w + 6)); // slides in from the right
            int x1 = x2 - w;
            int y = top + Math.round(ypos[i]);
            int color = themeColor(mode, k);

            if (background) {
                // Soft colored glow, then the dark panel
                Gui.drawRect(x1 - 2, y - 1, x2 + 1, y + ROW_H + 1, argb(color, 0.12f * a));
                Gui.drawRect(x1, y, x2 - BAR, y + ROW_H, argb(0x000000, 0.6f * a));
            }
            Gui.drawRect(x2 - BAR, y, x2, y + ROW_H, argb(color, a));

            // Very faint text is skipped (the font renderer treats near-zero alpha as opaque)
            if (a * 255f >= 6f) {
                int tx = x1 + PAD;
                fr.drawStringWithShadow(name, tx, y + 3, argb(color, a));
                if (tag != null) {
                    fr.drawStringWithShadow(" " + tag, tx + fr.getStringWidth(name), y + 3,
                            argb(0xAAAAAA, a));
                }
            }
        }

        GlStateManager.color(1f, 1f, 1f, 1f);
    }
}
