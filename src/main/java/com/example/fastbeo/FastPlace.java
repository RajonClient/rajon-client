package com.example.fastbeo;

import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

public class FastPlace {
    public boolean enabled = false;

    private final Minecraft mc = Minecraft.getMinecraft();

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !enabled) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;

        // Singleplayer only
        if (!FarReach.canRun()) {
            enabled = false;
            return;
        }

        ItemStack held = mc.thePlayer.getHeldItem();
        if (held == null || !(held.getItem() instanceof ItemBlock)) return;

        try {
            // Removes the delay between block placements
            ReflectionHelper.setPrivateValue(Minecraft.class, mc, 0,
                    "rightClickDelayTimer", "field_71467_ac");
        } catch (Exception ex) {
            enabled = false;
        }
    }
}
