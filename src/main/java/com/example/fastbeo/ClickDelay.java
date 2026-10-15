package com.example.fastbeo;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

/**
 * Removes the built-in click delays of the 1.8.9 client:
 *  - leftClickCounter: the 10 tick cooldown after swinging at air / starting to break
 *  - rightClickDelayTimer: the 4 tick cooldown between right clicks (placing, using items)
 */
public class ClickDelay {
    public boolean enabled = false;

    private final Minecraft mc = Minecraft.getMinecraft();

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !enabled) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;

        // Same lock as the other modules: multiplayer needs 'Allow Multiplayer'
        if (!FarReach.canRun()) {
            enabled = false;
            return;
        }

        try {
            if (Settings.bool("ClickDelay", "Left click")) {
                ReflectionHelper.setPrivateValue(Minecraft.class, mc, 0,
                        "leftClickCounter", "field_71429_W");
            }
            if (Settings.bool("ClickDelay", "Right click")) {
                ReflectionHelper.setPrivateValue(Minecraft.class, mc, 0,
                        "rightClickDelayTimer", "field_71467_ac");
            }
        } catch (Exception ex) {
            enabled = false;
            mc.thePlayer.addChatMessage(
                    new ChatComponentText("[" + Client.NAME + "] ClickDelay failed: " + ex.getClass().getSimpleName()));
        }
    }
}
