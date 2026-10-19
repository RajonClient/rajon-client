package com.example.fastbeo;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

/**
 * First-person item animations (visual only, apart from the normal swing animation packet).
 *
 *  - Style: "1.7" keeps the sword swinging while you block (like the old 1.7 / Lunar block hit);
 *    left click while blocking plays the swing. "Vanilla" leaves the game as it is.

 *  - X / Y / Z / Scale: move and resize the item in your hand at all times.
 *  - Block: extra motion that plays when you left click while blocking with a sword
 *    (or all the time while blocking if Loop is on).
 *      Vanilla = nothing extra
 *      Push    = the sword thrusts forward
 *      Slide   = the sword dips down
 *      Smooth  = the sword tilts forward and back
 *      Spin    = the sword spins while you block
 *
 * The extra transforms are applied right before the game draws the hand, so they stack on top
 * of the normal blocking pose. They move/rotate/scale the whole item, they do not replace the
 * blocking pose itself.
 */
public class Animations {
    public boolean enabled = false;

    // Where the held item roughly sits in first person (view space)
    private static final float PX = 0.56f, PY = -0.52f, PZ = -0.72f;

    private final Minecraft mc = Minecraft.getMinecraft();

    // When the last left click happened while blocking (the 1.8 game ignores swings while
    // blocking, so the motion is driven by this timer instead of the real swing)
    private long animStart = 0L;

    // The 1.7 style needs our own item renderer; it is installed the first time it is needed
    private boolean installed = false;
    private boolean failed = false;

    @SubscribeEvent
    public void onMouse(MouseEvent e) {
        if (!enabled || mc.thePlayer == null || mc.currentScreen != null) return;
        if (e.button == 0 && e.buttonstate && mc.thePlayer.isBlocking()) {
            animStart = System.currentTimeMillis();
            // 1.7 style: the swing is allowed while blocking (1.8 ignores the click)
            if ((int) Settings.get("Animations", "Style") == 1) {
                mc.thePlayer.swingItem();
            }
        }
    }

    @SubscribeEvent
    public void onHand(RenderHandEvent e) {
        if (!enabled || mc.thePlayer == null) return;

        installRenderer();

        // Position and size of the item
        GlStateManager.translate((float) Settings.get("Animations", "X"),
                (float) Settings.get("Animations", "Y"),
                (float) Settings.get("Animations", "Z"));

        float scale = (float) Settings.get("Animations", "Scale");
        if (scale != 1.0f) {
            GlStateManager.translate(PX, PY, PZ);
            GlStateManager.scale(scale, scale, scale);
            GlStateManager.translate(-PX, -PY, -PZ);
        }

        if (!mc.thePlayer.isBlocking()) return;

        // Progress of the block motion (0..1): from our own timer or from a real swing
        long now = System.currentTimeMillis();
        long duration = Math.max(1L, (long) Settings.get("Animations", "Duration"));
        float own = 0f;
        if (Settings.bool("Animations", "Loop")) {
            own = (now % duration) / (float) duration;
        } else if (animStart > 0L && now - animStart < duration) {
            own = (now - animStart) / (float) duration;
        }
        float swing = Math.max(mc.thePlayer.getSwingProgress(e.partialTicks), own);

        // 0 -> 1 -> 0 while the motion plays
        float arc = (float) Math.sin(swing * Math.PI);

        switch ((int) Settings.get("Animations", "Block")) {
            case 1: // Push
                GlStateManager.translate(0f, 0f, -0.35f * arc);
                break;
            case 2: // Slide
                GlStateManager.translate(0f, -0.15f * arc, 0f);
                break;
            case 3: // Smooth
                pivot(-45f * arc, 1f, 0f, 0f);
                break;
            case 4: // Spin
                pivot((System.currentTimeMillis() % 1500L) / 1500f * 360f, 0f, 1f, 0f);
                break;
            default:
                break;
        }
    }

    // The 1.7 style needs our own item renderer
    private void installRenderer() {
        AnimRenderer.install();
    }

    // Rotate around the spot where the item is, instead of around the camera
    private static void pivot(float angle, float x, float y, float z) {
        GlStateManager.translate(PX, PY, PZ);
        GlStateManager.rotate(angle, x, y, z);
        GlStateManager.translate(-PX, -PY, -PZ);
    }
}
