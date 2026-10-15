package com.example.fastbeo;

import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Removes the camera tilt you get when you take damage.
 * It only changes how your own camera looks, nothing is sent to the server.
 */
public class NoHurtCam {
    public boolean enabled = false;

    private final Minecraft mc = Minecraft.getMinecraft();

    @SubscribeEvent
    public void onRender(TickEvent.RenderTickEvent e) {
        // Right before each frame is drawn, clear the hurt timer the camera effect reads
        if (e.phase != TickEvent.Phase.START || !enabled) return;
        if (mc.thePlayer == null) return;
        mc.thePlayer.hurtTime = 0;
    }
}
