package com.example.fastbeo;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * AutoTool: while you hold the attack button on a block, it selects the hotbar slot whose item
 * mines that block fastest (counting Efficiency). When you stop mining it goes back to the slot
 * you had before (the "Switch back" setting).
 */
public class AutoTool {
    private static final String M = "AutoTool";

    public boolean enabled = false;

    private final Minecraft mc = Minecraft.getMinecraft();

    // Slot you were holding before AutoTool switched, -1 = nothing to give back
    private int previousSlot = -1;

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;

        EntityPlayerSP p = mc.thePlayer;
        if (p == null || mc.theWorld == null) {
            previousSlot = -1;
            return;
        }

        if (!enabled) {
            restore(p);
            return;
        }

        // Same lock as the other gameplay modules
        if (!FarReach.canRun()) {
            enabled = false;
            restore(p);
            return;
        }

        if (mc.currentScreen != null) return;
        // Blocks break instantly in creative, nothing to speed up
        if (mc.playerController.isInCreativeMode()) return;

        MovingObjectPosition mop = mc.objectMouseOver;
        boolean mining = mc.gameSettings.keyBindAttack.isKeyDown()
                && mop != null
                && mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK
                && !p.isUsingItem();

        if (!mining) {
            if (Settings.bool(M, "Switch back")) restore(p);
            else previousSlot = -1;
            return;
        }

        BlockPos pos = mop.getBlockPos();
        Block block = mc.theWorld.getBlockState(pos).getBlock();
        if (block.getBlockHardness(mc.theWorld, pos) < 0f) return; // unbreakable

        int cur = p.inventory.currentItem;
        int best = cur;
        float bestSpeed = speed(p.inventory.mainInventory[cur], block);

        for (int i = 0; i < 9; i++) {
            float s = speed(p.inventory.mainInventory[i], block);
            if (s > bestSpeed + 0.01f) { // only switch for a real improvement (no flickering)
                bestSpeed = s;
                best = i;
            }
        }

        if (best != cur) {
            if (previousSlot == -1) previousSlot = cur;
            p.inventory.currentItem = best;
            mc.playerController.updateController();
        }
    }

    // How fast this item mines the block (an empty hand counts as 1)
    private static float speed(ItemStack stack, Block block) {
        if (stack == null) return 1.0f;
        float f = stack.getStrVsBlock(block);
        if (f > 1.0f) {
            int level = EnchantmentHelper.getEnchantmentLevel(Enchantment.efficiency.effectId, stack);
            if (level > 0) f += level * level + 1;
        }
        return f;
    }

    private void restore(EntityPlayerSP p) {
        if (previousSlot != -1) {
            p.inventory.currentItem = previousSlot;
            mc.playerController.updateController();
            previousSlot = -1;
        }
    }
}
