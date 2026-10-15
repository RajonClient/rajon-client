package com.example.fastbeo;

import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Raises the brightness setting so everything is fully lit.
 * Your original brightness is restored when the module is turned off.
 */
public class Fullbright {
    public boolean enabled = false;

    private final Minecraft mc = Minecraft.getMinecraft();
    private boolean active = false;
    private float saved = 0f;

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END || mc.gameSettings == null) return;

        if (enabled) {
            if (!active) {
                saved = mc.gameSettings.gammaSetting;
                active = true;
            }
            mc.gameSettings.gammaSetting = (float) Settings.get("Fullbright", "Gamma");
        } else if (active) {
            mc.gameSettings.gammaSetting = saved;
            active = false;
        }
    }
}
