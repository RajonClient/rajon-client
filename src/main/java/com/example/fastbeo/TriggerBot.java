package com.example.fastbeo;

import java.util.Random;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import org.lwjgl.input.Mouse;

/**
 * Attacks automatically while your crosshair is on a target.
 * It only fires at what you are really aiming at, inside normal reach.
 */
public class TriggerBot {
    public boolean enabled = false;

    private final Minecraft mc = Minecraft.getMinecraft();
    private final Random rand = new Random();
    private double budget = 1.0; // starts "full" so the first hit is immediate

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !enabled) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;

        // Same lock as the other gameplay modules
        if (!FarReach.canRun()) {
            enabled = false;
            return;
        }

        if (mc.currentScreen != null) return;

        if (Settings.bool("TriggerBot", "Require click") && !Mouse.isButtonDown(0)) {
            budget = 1.0;
            return;
        }

        MovingObjectPosition mop = mc.objectMouseOver;
        if (mop == null || mop.typeOfHit != MovingObjectType.ENTITY
                || !(mop.entityHit instanceof EntityLivingBase)) {
            budget = 1.0;
            return;
        }

        EntityLivingBase target = (EntityLivingBase) mop.entityHit;
        if (target.isDead || target.getHealth() <= 0f) return;
        if (Settings.bool("TriggerBot", "Players only") && !(target instanceof EntityPlayer)) return;

        double a = Settings.get("TriggerBot", "Min CPS");
        double b = Settings.get("TriggerBot", "Max CPS");
        double lo = Math.min(a, b);
        double hi = Math.max(a, b);
        budget += (lo + rand.nextDouble() * (hi - lo)) / 20.0;

        if (budget >= 1.0) {
            budget -= 1.0;
            mc.thePlayer.swingItem();
            mc.playerController.attackEntity(mc.thePlayer, target);
        }
    }
}
