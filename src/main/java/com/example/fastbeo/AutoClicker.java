package com.example.fastbeo;

import java.lang.reflect.Method;
import java.util.Random;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import org.lwjgl.input.Mouse;

/**
 * Hold left mouse button to click repeatedly.
 * Mining blocks is left alone so it behaves like vanilla.
 */
public class AutoClicker {
    // Clicks per second, randomised between these two values (max 20)
    public static final double MIN_CPS = 8.0;
    public static final double MAX_CPS = 12.0;

    public boolean enabled = false;

    private final Minecraft mc = Minecraft.getMinecraft();
    private final Random rand = new Random();
    private Method clickMouse;
    private double budget = 0.0;

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !enabled) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;

        // Same lock as the other modules: multiplayer needs 'Allow Multiplayer'
        if (!FarReach.canRun()) {
            enabled = false;
            return;
        }

        if (mc.currentScreen != null || !Mouse.isButtonDown(0)) {
            budget = 0.0;
            return;
        }

        // Do not interfere with mining
        if (mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectType.BLOCK) {
            budget = 0.0;
            return;
        }

        double a = Settings.get("AutoClicker", "Min CPS");
        double b = Settings.get("AutoClicker", "Max CPS");
        double lo = Math.min(a, b);
        double hi = Math.max(a, b);
        budget += (lo + rand.nextDouble() * (hi - lo)) / 20.0;
        if (budget >= 1.0) {
            budget -= 1.0;
            click();
        }
    }

    private void click() {
        try {
            if (clickMouse == null) {
                clickMouse = ReflectionHelper.findMethod(Minecraft.class, mc,
                        new String[] { "clickMouse", "func_147116_af" });
                clickMouse.setAccessible(true);
            }
            clickMouse.invoke(mc);
        } catch (Exception ex) {
            enabled = false;
            if (mc.thePlayer != null) {
                mc.thePlayer.addChatMessage(
                        new ChatComponentText("[" + Client.NAME + "] AutoClicker failed: " + ex.getClass().getSimpleName()));
            }
        }
    }
}
