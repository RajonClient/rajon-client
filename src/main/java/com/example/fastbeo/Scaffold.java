package com.example.fastbeo;

import java.util.UUID;

import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Vec3;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import org.lwjgl.input.Keyboard;

public class Scaffold {
    public static final KeyBinding KEY =
            new KeyBinding("Toggle Scaffold", Keyboard.KEY_G, Client.NAME);

    // How far ahead (in ticks of current movement) to place the block.
    // Raise it if you still fall, lower it if blocks are placed too far ahead.
    private double lookahead = 3.0;
    // Max blocks placed per tick
    private int maxPlaces = 2;

    private final Minecraft mc = Minecraft.getMinecraft();
    public boolean enabled = false;

    private boolean solo;
    private IntegratedServer server;
    private int slot;
    private int placed;

    @SubscribeEvent
    public void onKey(InputEvent.KeyInputEvent e) {
        if (KEY.isPressed()) {
            if (!FarReach.canRun()) {
                msg("Multiplayer is locked. Enable 'Allow Multiplayer' in the ClickGUI first.");
                return;
            }
            enabled = !enabled;
            msg("Scaffold: " + (enabled ? "ON" : "OFF"));
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !enabled) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;

        server = mc.getIntegratedServer();
        solo = server != null && mc.isSingleplayer();
        if (!solo && !FarReach.allowMultiplayer) {
            enabled = false;
            return;
        }
        if (mc.currentScreen != null) return;

        lookahead = Settings.get("Scaffold", "Lookahead");
        maxPlaces = (int) Settings.get("Scaffold", "Blocks per tick");

        slot = findBlockSlot();
        if (slot == -1) return;

        double y = Math.floor(mc.thePlayer.getEntityBoundingBox().minY) - 1;
        BlockPos current = new BlockPos(
                Math.floor(mc.thePlayer.posX), y, Math.floor(mc.thePlayer.posZ));
        BlockPos ahead = new BlockPos(
                Math.floor(mc.thePlayer.posX + mc.thePlayer.motionX * lookahead), y,
                Math.floor(mc.thePlayer.posZ + mc.thePlayer.motionZ * lookahead));

        placed = 0;
        fill(current);
        if (!ahead.equals(current)) fill(ahead);
    }

    // Make sure the block at target is filled, bridging from a neighbour if needed
    private void fill(BlockPos target) {
        if (placed >= maxPlaces || !isEmpty(target)) return;

        if (placeAgainstNeighbour(target)) return;

        // No solid neighbour: build a connecting block first, then the target
        for (EnumFacing f : EnumFacing.Plane.HORIZONTAL) {
            BlockPos n = target.offset(f);
            if (isEmpty(n) && placeAgainstNeighbour(n)) {
                if (isEmpty(target)) placeAgainstNeighbour(target);
                return;
            }
        }
    }

    private boolean placeAgainstNeighbour(BlockPos target) {
        if (placed >= maxPlaces) return false;
        for (EnumFacing f : EnumFacing.values()) {
            BlockPos n = target.offset(f);
            Material m = mc.theWorld.getBlockState(n).getBlock().getMaterial();
            if (m.isSolid()) {
                place(n, f.getOpposite());
                placed++;
                return true;
            }
        }
        return false;
    }

    private boolean isEmpty(BlockPos p) {
        return mc.theWorld.getBlockState(p).getBlock().getMaterial().isReplaceable();
    }

    private int findBlockSlot() {
        int cur = mc.thePlayer.inventory.currentItem;
        if (isBlock(mc.thePlayer.inventory.mainInventory[cur])) return cur;
        for (int i = 0; i < 9; i++) {
            if (isBlock(mc.thePlayer.inventory.mainInventory[i])) return i;
        }
        return -1;
    }

    private void place(final BlockPos anchor, final EnumFacing side) {
        // Rotations module: turn the camera toward the face we are placing against
        if (FarReach.rotationsEnabled) {
            Vec3 aim = new Vec3(
                    anchor.getX() + 0.5 + side.getFrontOffsetX() * 0.5,
                    anchor.getY() + 0.5 + side.getFrontOffsetY() * 0.5,
                    anchor.getZ() + 0.5 + side.getFrontOffsetZ() * 0.5);
            Rotations.lookAt(aim, (float) Settings.get("Rotations", "Speed"));
        }

        if (!solo) {
            // Multiplayer: normal client-side placement, the server decides
            if (mc.thePlayer.inventory.currentItem != slot) {
                mc.thePlayer.inventory.currentItem = slot;
                mc.playerController.updateController();
            }
            Vec3 hit = new Vec3(
                    anchor.getX() + 0.5 + side.getFrontOffsetX() * 0.5,
                    anchor.getY() + 0.5 + side.getFrontOffsetY() * 0.5,
                    anchor.getZ() + 0.5 + side.getFrontOffsetZ() * 0.5);
            if (mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld,
                    mc.thePlayer.inventory.getCurrentItem(), anchor, side, hit)) {
                mc.thePlayer.swingItem();
            }
            return;
        }

        // Singleplayer: place through the integrated server, blocks are not consumed
        final UUID uuid = mc.thePlayer.getUniqueID();
        final int fslot = slot;
        final IntegratedServer srv = server;
        srv.addScheduledTask(new Runnable() {
            public void run() {
                EntityPlayerMP p = srv.getConfigurationManager().getPlayerByUUID(uuid);
                if (p == null) return;
                ItemStack stack = p.inventory.mainInventory[fslot];
                if (!isBlock(stack)) return;

                int count = stack.stackSize;
                int meta = stack.getMetadata();

                stack.onItemUse(p, p.worldObj, anchor, side, 0.5F, 0.5F, 0.5F);

                stack.stackSize = count;
                stack.setItemDamage(meta);
                p.inventoryContainer.detectAndSendChanges();
            }
        });
    }

    private static boolean isBlock(ItemStack s) {
        return s != null && s.stackSize > 0 && s.getItem() instanceof ItemBlock;
    }

    private void msg(String t) {
        if (mc.thePlayer != null) {
            mc.thePlayer.addChatMessage(new ChatComponentText("[" + Client.NAME + "] " + t));
        }
    }
}
