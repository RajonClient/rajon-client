package com.example.fastbeo;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import org.lwjgl.input.Keyboard;

/**
 * Bridging practice aid: automatically sneaks when you reach the edge of a
 * block while holding blocks. You still aim and place the blocks yourself.
 */
public class LegitScaffold {
    public boolean enabled = false;

    private final Minecraft mc = Minecraft.getMinecraft();
    private boolean forcing = false;

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        if (mc.thePlayer == null || mc.theWorld == null) {
            forcing = false;
            return;
        }

        if (!enabled) {
            release();
            return;
        }

        // Singleplayer only
        if (!FarReach.canRun()) {
            enabled = false;
            release();
            return;
        }

        if (mc.currentScreen != null) return;

        ItemStack held = mc.thePlayer.getHeldItem();
        boolean holdingBlock = held != null && held.getItem() instanceof ItemBlock;

        boolean atEdge = mc.thePlayer.onGround
                && mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer,
                        mc.thePlayer.getEntityBoundingBox()
                                .offset(mc.thePlayer.motionX, -1.0, mc.thePlayer.motionZ)).isEmpty();

        int code = mc.gameSettings.keyBindSneak.getKeyCode();
        // Works with any item, unless "Require block" is turned on
        boolean allowed = !Settings.bool("LegitScaffold", "Require block") || holdingBlock;
        if (allowed && atEdge) {
            KeyBinding.setKeyBindState(code, true);
            forcing = true;
        } else {
            release();
        }
    }

    // Give sneak back to the real keyboard state
    private void release() {
        if (!forcing) return;
        int code = mc.gameSettings.keyBindSneak.getKeyCode();
        boolean down = code > 0 && Keyboard.isKeyDown(code);
        KeyBinding.setKeyBindState(code, down);
        forcing = false;
    }
}
