package com.example.fastbeo;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.MathHelper;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;

/**
 * Silent rotations: the server is told you look in a direction that differs from your camera.
 * Your own view does not move and your walking direction is not affected.
 *
 * Meant for testing the anticheat of a server you run yourself. Modules call aim() every tick
 * they want a rotation; when they stop, the real camera rotation is sent again.
 *
 * How it works: when you connect, a small handler is added to the connection. Every movement
 * packet (C03/C04) your client sends is replaced by one that carries the silent yaw and pitch.
 */
public class SilentRotations {
    private static volatile float yaw = 0f;
    private static volatile float pitch = 0f;
    private static volatile long stamp = 0L;
    private static volatile boolean installed = false;
    private static boolean warned = false;

    /** True while a module has asked for a silent rotation very recently. */
    public static boolean active() {
        return System.currentTimeMillis() - stamp < 150L;
    }

    /**
     * Moves the angles the server sees toward the target, at most maxStep degrees per axis,
     * and returns how far (degrees) they still are from it.
     */
    public static float aim(float targetYaw, float targetPitch, float maxStep) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP p = mc.thePlayer;
        if (p == null) return 999f;

        if (!installed && !warned) {
            warned = true;
            p.addChatMessage(new ChatComponentText("[" + Client.NAME
                    + "] Silent rotations are not active on this connection (rejoin the world or server)."));
        }

        long now = System.currentTimeMillis();
        if (now - stamp > 300L) {
            // Start from where the camera is looking
            yaw = p.rotationYaw;
            pitch = p.rotationPitch;
        }

        float dy = MathHelper.wrapAngleTo180_float(targetYaw - yaw);
        float dp = targetPitch - pitch;
        float sy = Math.max(-maxStep, Math.min(maxStep, dy));
        float sp = Math.max(-maxStep, Math.min(maxStep, dp));

        yaw += sy;
        pitch = MathHelper.clamp_float(pitch + sp, -90.0F, 90.0F);
        stamp = now;

        float ry = dy - sy;
        float rp = dp - sp;
        return (float) Math.sqrt(ry * ry + rp * rp);
    }

    // ---------------------------------------------------------------- connection hook

    @SubscribeEvent
    public void onConnect(FMLNetworkEvent.ClientConnectedToServerEvent e) {
        try {
            e.manager.channel().pipeline().addBefore("packet_handler", "rajon_silent_rotations", new Out());
            installed = true;
        } catch (Exception ex) {
            installed = false;
        }
    }

    @SubscribeEvent
    public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent e) {
        installed = false;
        warned = false;
    }

    /** Rewrites the rotation of outgoing movement packets while a silent rotation is active. */
    private static class Out extends ChannelOutboundHandlerAdapter {
        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
            if (msg instanceof C03PacketPlayer && active()) {
                C03PacketPlayer pk = (C03PacketPlayer) msg;
                Packet<?> replacement;
                if (pk.isMoving()) {
                    replacement = new C03PacketPlayer.C06PacketPlayerPosLook(
                            pk.getPositionX(), pk.getPositionY(), pk.getPositionZ(),
                            yaw, pitch, pk.isOnGround());
                } else {
                    replacement = new C03PacketPlayer.C05PacketPlayerLook(yaw, pitch, pk.isOnGround());
                }
                super.write(ctx, replacement, promise);
                return;
            }
            super.write(ctx, msg, promise);
        }
    }
}
