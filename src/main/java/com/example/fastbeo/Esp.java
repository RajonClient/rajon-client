package com.example.fastbeo;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import org.lwjgl.opengl.GL11;

/** ESP. Current mode: Box (a 3D outline around each entity, visible through walls). */
public class Esp {
    public boolean enabled = false;
    public String mode = "Box";

    private final Minecraft mc = Minecraft.getMinecraft();

    @SubscribeEvent
    public void onRender(RenderWorldLastEvent e) {
        if (!enabled) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;

        // Same lock as the other gameplay modules
        if (!FarReach.canRun()) {
            enabled = false;
            return;
        }

        Entity view = mc.getRenderViewEntity();
        if (view == null) return;

        float pt = e.partialTicks;
        double vx = view.lastTickPosX + (view.posX - view.lastTickPosX) * pt;
        double vy = view.lastTickPosY + (view.posY - view.lastTickPosY) * pt;
        double vz = view.lastTickPosZ + (view.posZ - view.lastTickPosZ) * pt;

        GlStateManager.disableTexture2D();
        GlStateManager.disableDepth();
        GlStateManager.depthMask(false);
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GL11.glLineWidth((float) Settings.get("ESP", "Line width"));

        for (Object o : mc.theWorld.loadedEntityList) {
            if (!(o instanceof EntityLivingBase)) continue;
            EntityLivingBase en = (EntityLivingBase) o;
            if (en == view || en.isDead || en.getHealth() <= 0f) continue;

            boolean player = en instanceof EntityPlayer;
            if (player ? !Settings.bool("ESP", "Players") : !Settings.bool("ESP", "Mobs")) continue;

            if ("Box".equals(mode)) {
                drawBox(en, pt, vx, vy, vz, player);
            }
        }

        GlStateManager.enableTexture2D();
        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.disableBlend();
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    private void drawBox(EntityLivingBase en, float pt, double vx, double vy, double vz, boolean player) {
        double ex = en.lastTickPosX + (en.posX - en.lastTickPosX) * pt;
        double ey = en.lastTickPosY + (en.posY - en.lastTickPosY) * pt;
        double ez = en.lastTickPosZ + (en.posZ - en.lastTickPosZ) * pt;

        // Move the box from the entity's current position to its smoothed position,
        // relative to the camera
        AxisAlignedBB bb = en.getEntityBoundingBox()
                .offset(ex - en.posX - vx, ey - en.posY - vy, ez - en.posZ - vz)
                .expand(0.05, 0.05, 0.05);

        if (player) {
            RenderGlobal.drawOutlinedBoundingBox(bb, 79, 195, 247, 255);
        } else {
            RenderGlobal.drawOutlinedBoundingBox(bb, 255, 167, 38, 255);
        }
    }
}
