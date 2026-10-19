package com.example.fastbeo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * KillAura: attacks targets around you automatically, without you aiming at them.
 *
 *  - Range: how far it reaches (a vanilla server accepts hits up to about 6 blocks,
 *    and only about 3 through walls).
 *  - Mode: Single keeps hitting the chosen target, Switch goes through every target in range.
 *  - Priority: Distance, Health or Angle decides who is chosen first.
 *  - Rotate: turns your real camera toward the target (smooth). Nothing is hidden from the server.
 *  - AutoBlock Fake: shows the sword block pose on your screen while you fight. It is only a
 *    visual effect, nothing is sent to the server and you are not really blocking.
 *
 * Same lock as the other combat modules: on a server it needs 'Allow Multiplayer'.
 */
public class KillAura {
    private static final String M = "KillAura";

    public boolean enabled = false;

    private final Minecraft mc = Minecraft.getMinecraft();
    private final Random rand = new Random();

    private double budget = 1.0; // starts "full" so the first hit is immediate
    private EntityLivingBase current = null;
    private int switchIndex = 0;
    private long last = 0L;
    private long lastActive = 0L; // last time a target was in range

    // ---------------------------------------------------------------- targets

    // Aim point of an entity, smoothed between ticks: {x, y, z}
    private static double[] aimPoint(EntityLivingBase en, float pt) {
        double x = en.lastTickPosX + (en.posX - en.lastTickPosX) * pt;
        double y = en.lastTickPosY + (en.posY - en.lastTickPosY) * pt;
        double z = en.lastTickPosZ + (en.posZ - en.lastTickPosZ) * pt;
        return new double[] { x, y + en.getEyeHeight() - 0.2, z };
    }

    private boolean valid(EntityPlayerSP p, EntityLivingBase en) {
        if (en == p || en.isDead || en.getHealth() <= 0f || en instanceof EntityArmorStand) return false;
        if (Settings.bool(M, "Players only") && !(en instanceof EntityPlayer)) return false;
        if (p.getDistanceToEntity(en) > Settings.get(M, "Range")) return false;
        if (Settings.bool(M, "Visible only") && !p.canEntityBeSeen(en)) return false;

        double fov = Settings.get(M, "FOV");
        if (fov < 360.0) {
            double[] a = aimPoint(en, 1.0F);
            float[] angles = Rotations.anglesTo(a[0], a[1], a[2]);
            if (Math.abs(Rotations.yawDiff(angles[0])) > fov / 2.0) return false;
        }
        return true;
    }

    private EntityLivingBase chooseTarget(final EntityPlayerSP p) {
        List<EntityLivingBase> list = new ArrayList<EntityLivingBase>();
        for (Object o : mc.theWorld.loadedEntityList) {
            if (o instanceof EntityLivingBase && valid(p, (EntityLivingBase) o)) {
                list.add((EntityLivingBase) o);
            }
        }
        if (list.isEmpty()) return null;

        final int priority = (int) Settings.get(M, "Priority"); // 0 distance, 1 health, 2 angle
        Collections.sort(list, new Comparator<EntityLivingBase>() {
            public int compare(EntityLivingBase a, EntityLivingBase b) {
                return Double.compare(score(a), score(b));
            }

            private double score(EntityLivingBase en) {
                if (priority == 1) return en.getHealth();
                if (priority == 2) {
                    double[] pt = aimPoint(en, 1.0F);
                    float[] ang = Rotations.anglesTo(pt[0], pt[1], pt[2]);
                    float dy = Rotations.yawDiff(ang[0]);
                    float dp = Rotations.pitchDiff(ang[1]);
                    return Math.sqrt(dy * dy + dp * dp);
                }
                return p.getDistanceToEntity(en);
            }
        });

        if ((int) Settings.get(M, "Mode") == 1) {
            return list.get(switchIndex % list.size()); // Switch: go through all of them
        }
        return list.get(0);
    }

    private static boolean holdingWeapon(EntityPlayerSP p) {
        ItemStack held = p.getHeldItem();
        return held != null && (held.getItem() instanceof ItemSword || held.getItem() instanceof ItemAxe);
    }

    // ---------------------------------------------------------------- attacking (every tick)

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;

        if (!enabled) {
            current = null;
            return;
        }

        EntityPlayerSP p = mc.thePlayer;
        if (p == null || mc.theWorld == null) {
            current = null;
            return;
        }

        // Same lock as the other combat modules
        if (!FarReach.canRun()) {
            enabled = false;
            current = null;
            return;
        }

        if (mc.currentScreen != null || p.getHealth() <= 0f) return;

        if (Settings.bool(M, "Require weapon") && !holdingWeapon(p)) {
            current = null;
            return;
        }

        current = chooseTarget(p);
        if (current == null) {
            budget = 1.0;
            return;
        }
        lastActive = System.currentTimeMillis();

        if ((int) Settings.get(M, "AutoBlock") == 1) {
            AnimRenderer.install();
        }

        double a = Settings.get(M, "Min CPS");
        double b = Settings.get(M, "Max CPS");
        double lo = Math.min(a, b);
        double hi = Math.max(a, b);
        budget += (lo + rand.nextDouble() * (hi - lo)) / 20.0;

        if (budget >= 1.0) {
            budget -= 1.0;

            if (Settings.bool(M, "Swing")) {
                p.swingItem();
            } else if (mc.getNetHandler() != null) {
                mc.getNetHandler().addToSendQueue(new C0APacketAnimation());
            }
            mc.playerController.attackEntity(p, current);

            switchIndex++;
        }
    }

    // ---------------------------------------------------------------- fake autoblock

    /** True while the fake block pose should be shown (used by AnimRenderer). */
    public boolean isFakeBlocking() {
        if (!enabled || mc.thePlayer == null) return false;
        if ((int) Settings.get(M, "AutoBlock") != 1) return false;
        // A short moment after the last target keeps the pose from flickering
        return System.currentTimeMillis() - lastActive < 400L;
    }

    // ---------------------------------------------------------------- camera (every frame, smooth)

    @SubscribeEvent
    public void onRender(TickEvent.RenderTickEvent e) {
        if (e.phase != TickEvent.Phase.START) return;

        long now = System.nanoTime();
        double dt = last == 0L ? 0.0 : (now - last) / 1.0e9;
        last = now;

        if (!enabled || current == null || !Settings.bool(M, "Rotate")) return;
        if (mc.thePlayer == null || mc.theWorld == null || mc.currentScreen != null) return;
        if (current.isDead || dt <= 0.0 || dt > 0.25) return;

        float pt = e.renderTickTime;
        double[] a = aimPoint(current, pt);
        float[] angles = Rotations.anglesTo(a[0], a[1], a[2], pt);
        float dy = Rotations.yawDiff(angles[0]);
        float dp = Rotations.pitchDiff(angles[1]);

        // Exponential easing, capped by the rotation speed (degrees per tick).
        // A higher speed also makes the easing faster, so 180 turns almost instantly.
        double speed = Settings.get(M, "Rotation speed");
        double alpha = 1.0 - Math.exp(-(6.0 + speed * 0.4) * dt);
        float maxStep = (float) (Settings.get(M, "Rotation speed") * dt * 20.0);

        Rotations.turn(clampStep(dy, alpha, maxStep), clampStep(dp, alpha, maxStep));
    }

    private static float clampStep(float diff, double alpha, float maxStep) {
        if (Math.abs(diff) < 0.1f) return 0f;
        float s = (float) (diff * alpha);
        return Math.max(-maxStep, Math.min(maxStep, s));
    }
}
