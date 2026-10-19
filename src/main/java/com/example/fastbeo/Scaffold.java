package com.example.fastbeo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Vec3;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import org.lwjgl.input.Keyboard;

/**
 * Scaffold: places blocks under you while you move.
 *
 * Modes
 *   Normal : fills the block under your feet (and clutches to nearby spots)
 *   Expand : also places several blocks ahead of you in the direction you face
 *   Telly  : jumps and places while moving (sprint-jump bridging)
 *
 * Extras: AutoBlock, place delay, Same Y, Down (hold sneak), Eagle, Tower (Motion or Legit),
 * Lookahead, Sprint control,
 * and the Rotations module (turns the camera while placing, with the movement fix).
 *
 * Singleplayer places through the integrated server (blocks are not used up).
 * On a server it uses the normal client placement, so the server decides what is valid.
 */
public class Scaffold {
    private static final String M = "Scaffold";

    private final Minecraft mc = Minecraft.getMinecraft();
    private final Random rand = new Random();

    public boolean enabled = false;

    private long nextPlaceAt = 0L;
    private int launchY = -999;
    private boolean sneakForced = false;
    private boolean sprintForced = false;

    // A place to click: the block we click on and which face of it
    private static class Spot {
        final BlockPos anchor;
        final EnumFacing side;

        Spot(BlockPos anchor, EnumFacing side) {
            this.anchor = anchor;
            this.side = side;
        }
    }

    // ---------------------------------------------------------------- main loop

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;

        EntityPlayerSP p = mc.thePlayer;
        if (p == null || mc.theWorld == null) {
            sneakForced = false;
            sprintForced = false;
            launchY = -999;
            return;
        }

        if (!enabled) {
            releaseKeys();
            launchY = -999;
            return;
        }

        final IntegratedServer server = mc.getIntegratedServer();
        final boolean solo = server != null && mc.isSingleplayer();
        if (!solo && !FarReach.allowMultiplayer) {
            enabled = false;
            releaseKeys();
            return;
        }
        if (mc.currentScreen != null) return;

        int mode = (int) Settings.get(M, "Mode"); // 0 Normal, 1 Expand, 2 Telly
        boolean moving = p.movementInput.moveForward != 0f || p.movementInput.moveStrafe != 0f;
        boolean jumpDown = physicalDown(mc.gameSettings.keyBindJump);
        boolean goDown = Settings.bool(M, "Down") && physicalDown(mc.gameSettings.keyBindSneak)
                && p.onGround && mode != 2;

        // Level to build on
        int feetY = (int) Math.floor(p.getEntityBoundingBox().minY + 0.001);
        boolean sameY = Settings.bool(M, "Same Y");
        boolean towering = (int) Settings.get(M, "Tower") != 0 && jumpDown && !moving;
        if (!sameY || p.onGround || launchY == -999 || towering) launchY = feetY;
        int baseY = launchY - 1 - (goDown ? 1 : 0);

        handleSprint(p, mode);
        handleEagle(p, mode, goDown);

        // Telly: keep hopping while moving
        if (mode == 2 && p.onGround && moving && !p.isSneaking()) {
            p.jump();
        }

        handleTower(p, moving, jumpDown);

        // Which hotbar slot holds blocks
        int slot = pickSlot(p);
        if (slot == -1) return;

        long now = System.currentTimeMillis();
        if (now < nextPlaceAt) return;

        int perTick = solo ? 1 : (int) Settings.get(M, "Blocks per tick");
        for (int n = 0; n < perTick; n++) {
            Spot spot = findSpot(p, baseY, mode);
            if (spot == null) break;

            place(p, spot, slot, solo, server);

            double a = Settings.get(M, "Min delay");
            double b = Settings.get(M, "Max delay");
            double lo = Math.min(a, b);
            double hi = Math.max(a, b);
            long delay = (long) (lo + rand.nextDouble() * (hi - lo));
            nextPlaceAt = System.currentTimeMillis() + delay;
            if (delay > 0) break;
        }
    }

    // ---------------------------------------------------------------- movement helpers

    private void handleSprint(EntityPlayerSP p, int mode) {
        int code = mc.gameSettings.keyBindSprint.getKeyCode();
        if (mode == 2) {
            // Telly needs to sprint
            KeyBinding.setKeyBindState(code, true);
            sprintForced = true;
        } else if (!Settings.bool(M, "Sprint")) {
            KeyBinding.setKeyBindState(code, false);
            sprintForced = true;
            if (p.isSprinting()) p.setSprinting(false);
        } else if (sprintForced) {
            KeyBinding.setKeyBindState(code, code > 0 && Keyboard.isKeyDown(code));
            sprintForced = false;
        }
    }

    // Eagle: sneak automatically at the edge of a block so you do not walk off
    private void handleEagle(EntityPlayerSP p, int mode, boolean goDown) {
        int code = mc.gameSettings.keyBindSneak.getKeyCode();
        boolean eagle = (int) Settings.get(M, "Eagle") == 1 && !goDown && mode != 2;

        if (eagle) {
            boolean edge = p.onGround && mc.theWorld.getCollidingBoundingBoxes(p,
                    p.getEntityBoundingBox().offset(p.motionX, -1.0, p.motionZ)).isEmpty();
            if (edge) {
                KeyBinding.setKeyBindState(code, true);
                sneakForced = true;
                return;
            }
        }

        if (sneakForced) {
            KeyBinding.setKeyBindState(code, code > 0 && Keyboard.isKeyDown(code));
            sneakForced = false;
        }
    }

    // Tower: hold jump while standing still to climb straight up.
    //   Motion = the mod gives you the jump boost right after each block (fast).
    //   Legit  = nothing is changed: you just jump normally and the block is placed under you
    //            while you are in the air, exactly like towering by hand (slower, looks natural).
    private void handleTower(EntityPlayerSP p, boolean moving, boolean jumpDown) {
        if ((int) Settings.get(M, "Tower") != 1 || !jumpDown || moving) return;

        double minY = p.getEntityBoundingBox().minY;
        BlockPos below = new BlockPos(Math.floor(p.posX), Math.floor(minY - 0.01), Math.floor(p.posZ));
        boolean solidBelow = mc.theWorld.getBlockState(below).getBlock().getMaterial().isSolid();

        if (solidBelow && minY - Math.floor(minY) < 0.15 && p.motionY < 0.1) {
            p.motionY = 0.42;
        }
    }

    private void releaseKeys() {
        if (sneakForced) {
            int c = mc.gameSettings.keyBindSneak.getKeyCode();
            KeyBinding.setKeyBindState(c, c > 0 && Keyboard.isKeyDown(c));
            sneakForced = false;
        }
        if (sprintForced) {
            int c = mc.gameSettings.keyBindSprint.getKeyCode();
            KeyBinding.setKeyBindState(c, c > 0 && Keyboard.isKeyDown(c));
            sprintForced = false;
        }
    }

    private static boolean physicalDown(KeyBinding kb) {
        int c = kb.getKeyCode();
        return c > 0 && Keyboard.isKeyDown(c);
    }

    // ---------------------------------------------------------------- blocks and slots

    private static boolean isBlock(ItemStack s) {
        if (s == null || s.stackSize <= 0 || !(s.getItem() instanceof ItemBlock)) return false;
        // Full blocks only (no flowers, torches, chests ...)
        return ((ItemBlock) s.getItem()).getBlock().isFullCube();
    }

    private int pickSlot(EntityPlayerSP p) {
        int cur = p.inventory.currentItem;
        if (isBlock(p.inventory.mainInventory[cur])) return cur;
        if ((int) Settings.get(M, "AutoBlock") == 0) return -1;
        for (int i = 0; i < 9; i++) {
            if (isBlock(p.inventory.mainInventory[i])) return i;
        }
        return -1;
    }

    private boolean isEmpty(BlockPos pos) {
        return mc.theWorld.getBlockState(pos).getBlock().getMaterial().isReplaceable();
    }

    // ---------------------------------------------------------------- finding where to place

    private Spot findSpot(final EntityPlayerSP p, int baseY, int mode) {
        int bx = (int) Math.floor(p.posX);
        int bz = (int) Math.floor(p.posZ);
        List<BlockPos> cands = new ArrayList<BlockPos>();

        if (mode == 1) {
            // Expand: a line of spots in the direction you are facing
            EnumFacing dir = p.getHorizontalFacing();
            int len = (int) Settings.get(M, "Expand length");
            for (int i = 0; i < len; i++) {
                cands.add(new BlockPos(bx + dir.getFrontOffsetX() * i, baseY, bz + dir.getFrontOffsetZ() * i));
            }
        } else {
            // Normal / Telly.
            // "here" is the block under you, "ahead" is the block you will be over in a moment.
            // Placing the one ahead is what stops you from falling when you move fast.
            BlockPos here = new BlockPos(bx, baseY, bz);
            double la = Settings.get(M, "Lookahead");
            BlockPos ahead = new BlockPos(
                    (int) Math.floor(p.posX + p.motionX * la), baseY,
                    (int) Math.floor(p.posZ + p.motionZ * la));

            boolean needHere = isEmpty(here);
            if (needHere) cands.add(here);
            if (!ahead.equals(here) && isEmpty(ahead)) cands.add(ahead);

            // Only when the block under you is missing and cannot be reached directly:
            // nearby spots on the same level, closest first (Clutch setting), to build a link
            if (needHere) {
                int r = (int) Settings.get(M, "Clutch");
                List<BlockPos> near = new ArrayList<BlockPos>();
                for (int dx = -r; dx <= r; dx++) {
                    for (int dz = -r; dz <= r; dz++) {
                        if (dx == 0 && dz == 0) continue;
                        near.add(new BlockPos(bx + dx, baseY, bz + dz));
                    }
                }
                final double px = p.posX;
                final double pz = p.posZ;
                Collections.sort(near, new Comparator<BlockPos>() {
                    public int compare(BlockPos a, BlockPos b) {
                        return Double.compare(dist2(a), dist2(b));
                    }

                    private double dist2(BlockPos c) {
                        double dx = c.getX() + 0.5 - px;
                        double dz = c.getZ() + 0.5 - pz;
                        return dx * dx + dz * dz;
                    }
                });
                cands.addAll(near);
            }
        }

        for (BlockPos c : cands) {
            Spot s = spotFor(p, c);
            if (s != null) return s;
        }
        return null;
    }

    // A solid neighbour of an empty position that we can click on, within reach
    private Spot spotFor(EntityPlayerSP p, BlockPos pos) {
        if (!isEmpty(pos)) return null;

        Vec3 eyes = p.getPositionEyes(1.0F);
        double reach = mc.playerController.getBlockReachDistance();

        for (EnumFacing f : EnumFacing.values()) {
            BlockPos n = pos.offset(f);
            Material m = mc.theWorld.getBlockState(n).getBlock().getMaterial();
            if (!m.isSolid()) continue;

            EnumFacing side = f.getOpposite();
            Vec3 hit = faceCenter(n, side);
            if (eyes.distanceTo(hit) > reach) continue;

            return new Spot(n, side);
        }
        return null;
    }

    private static Vec3 faceCenter(BlockPos anchor, EnumFacing side) {
        return new Vec3(
                anchor.getX() + 0.5 + side.getFrontOffsetX() * 0.5,
                anchor.getY() + 0.5 + side.getFrontOffsetY() * 0.5,
                anchor.getZ() + 0.5 + side.getFrontOffsetZ() * 0.5);
    }

    // ---------------------------------------------------------------- placing

    private void place(EntityPlayerSP p, Spot spot, final int slot, boolean solo, final IntegratedServer server) {
        final BlockPos anchor = spot.anchor;
        final EnumFacing side = spot.side;
        Vec3 hit = faceCenter(anchor, side);

        // Rotations module: turn the camera while placing (see its settings)
        if (FarReach.rotationsEnabled) {
            float speed = (float) Settings.get("Rotations", "Speed");
            if ((int) Settings.get("Rotations", "Mode") == 1) {
                // Backwards: look opposite to where you are walking, steeply down
                double mx = p.motionX;
                double mz = p.motionZ;
                boolean moving = Math.sqrt(mx * mx + mz * mz) > 0.05;
                float yaw = moving
                        ? (float) (Math.toDegrees(Math.atan2(mz, mx)) - 90.0) + 180f
                        : p.rotationYaw;
                Rotations.turnTo(yaw, (float) Settings.get("Rotations", "Pitch"), speed);
            } else {
                // Face block: look at the face we are placing against
                Rotations.lookAt(hit, speed);
            }
        }

        if (!solo) {
            // Server: normal client placement
            if (p.inventory.currentItem != slot) {
                p.inventory.currentItem = slot;
                mc.playerController.updateController();
            }
            boolean ok = mc.playerController.onPlayerRightClick(p, mc.theWorld,
                    p.inventory.getCurrentItem(), anchor, side, hit);
            if (ok) swing(p);
            return;
        }

        // Singleplayer: place through the integrated server, blocks are not used up
        final UUID uuid = p.getUniqueID();
        server.addScheduledTask(new Runnable() {
            public void run() {
                EntityPlayerMP sp = server.getConfigurationManager().getPlayerByUUID(uuid);
                if (sp == null) return;
                ItemStack stack = sp.inventory.mainInventory[slot];
                if (!isBlock(stack)) return;

                int count = stack.stackSize;
                int meta = stack.getMetadata();

                stack.onItemUse(sp, sp.worldObj, anchor, side, 0.5F, 0.5F, 0.5F);

                stack.stackSize = count;
                stack.setItemDamage(meta);
                sp.inventoryContainer.detectAndSendChanges();
            }
        });
        swing(p);
    }

    private void swing(EntityPlayerSP p) {
        if (Settings.bool(M, "Swing")) {
            p.swingItem();
        } else if (mc.getNetHandler() != null) {
            mc.getNetHandler().addToSendQueue(new C0APacketAnimation());
        }
    }
}
