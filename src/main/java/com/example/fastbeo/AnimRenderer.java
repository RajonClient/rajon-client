package com.example.fastbeo;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.MathHelper;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

/**
 * Replaces the game's first-person item renderer. Everything is drawn by the normal game code
 * except one case: holding a sword and blocking with the "1.7" style on. In that case the
 * sword keeps playing the swing while it is raised (the old Lunar/1.7 "block hit" look),
 * instead of freezing the swing like 1.8 does.
 */
public class AnimRenderer extends ItemRenderer {
    private final Minecraft mc;

    private static boolean installed = false;
    private static boolean failed = false;

    /** Swaps the game's first-person item renderer for ours (it passes everything else through). */
    public static void install() {
        Minecraft mc = Minecraft.getMinecraft();
        if (installed || failed || mc.entityRenderer == null) return;
        try {
            AnimRenderer renderer = new AnimRenderer(mc);
            ReflectionHelper.setPrivateValue(EntityRenderer.class, mc.entityRenderer, renderer,
                    "itemRenderer", "field_78516_c");
            installed = true;
            try {
                // The game keeps a second reference to the same renderer
                ReflectionHelper.setPrivateValue(Minecraft.class, mc, renderer,
                        "itemRenderer", "field_175620_Y");
            } catch (Exception ignored) {
                // Only matters for a tiny equip animation glitch
            }
        } catch (Exception ex) {
            failed = true;
            if (mc.thePlayer != null) {
                mc.thePlayer.addChatMessage(new ChatComponentText(
                        "[" + Client.NAME + "] Could not install the item renderer (1.7 block / fake block)."));
            }
        }
    }

    public AnimRenderer(Minecraft mc) {
        super(mc);
        this.mc = mc;
    }

    @Override
    public void renderItemInFirstPerson(float partialTicks) {
        EntityPlayerSP p = mc.thePlayer;
        ItemStack held = p == null ? null : p.getHeldItem();

        boolean realBlock = FarReach.animations != null
                && FarReach.animations.enabled
                && (int) Settings.get("Animations", "Style") == 1
                && held != null
                && p.getItemInUseCount() > 0
                && held.getItemUseAction() == EnumAction.BLOCK;

        // Fake autoblock (KillAura): show the block pose without really blocking
        boolean fakeBlock = held != null
                && held.getItem() instanceof ItemSword
                && FarReach.killAura != null
                && FarReach.killAura.isFakeBlocking();

        boolean custom = realBlock || fakeBlock;

        if (!custom) {
            super.renderItemInFirstPerson(partialTicks);
            return;
        }

        // How far the item is raised (1 = fully equipped), read from the game's own state
        float prev = progress("prevEquippedProgress", "field_78451_d");
        float cur = progress("equippedProgress", "field_78454_c");
        float equip = 1.0F - (prev + (cur - prev) * partialTicks);

        float swing = p.getSwingProgress(partialTicks);
        float pitch = p.prevRotationPitch + (p.rotationPitch - p.prevRotationPitch) * partialTicks;
        float yaw = p.prevRotationYaw + (p.rotationYaw - p.prevRotationYaw) * partialTicks;

        rotateAroundXAndY(pitch, yaw);
        setLightFromPlayer(p);
        rotateWithPlayer(p, partialTicks);

        GlStateManager.enableRescaleNormal();
        GlStateManager.pushMatrix();

        // Same item transform as the game, but with the real swing progress
        // (the 1.8 game passes 0 here while blocking)
        transformItem(equip, swing);

        // The blocking pose
        GlStateManager.translate(-0.5F, 0.2F, 0.0F);
        GlStateManager.rotate(30.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(-80.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(60.0F, 0.0F, 1.0F, 0.0F);

        this.renderItem(p, held, ItemCameraTransforms.TransformType.FIRST_PERSON);

        GlStateManager.popMatrix();
        GlStateManager.disableRescaleNormal();
        RenderHelper.disableStandardItemLighting();
    }

    private float progress(String name, String srgName) {
        try {
            Float v = ReflectionHelper.getPrivateValue(ItemRenderer.class, this, name, srgName);
            return v == null ? 1.0F : v;
        } catch (Exception ex) {
            return 1.0F;
        }
    }

    private static void transformItem(float equip, float swing) {
        GlStateManager.translate(0.56F, -0.52F, -0.71999997F);
        GlStateManager.translate(0.0F, equip * -0.6F, 0.0F);
        GlStateManager.rotate(45.0F, 0.0F, 1.0F, 0.0F);
        float f = MathHelper.sin(swing * swing * (float) Math.PI);
        float f1 = MathHelper.sin(MathHelper.sqrt_float(swing) * (float) Math.PI);
        GlStateManager.rotate(f * -20.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(f1 * -20.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.rotate(f1 * -80.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.scale(0.4F, 0.4F, 0.4F);
    }

    private static void rotateAroundXAndY(float pitch, float yaw) {
        GlStateManager.pushMatrix();
        GlStateManager.rotate(pitch, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(yaw, 0.0F, 1.0F, 0.0F);
        RenderHelper.enableStandardItemLighting();
        GlStateManager.popMatrix();
    }

    private void setLightFromPlayer(EntityPlayerSP p) {
        int light = mc.theWorld.getCombinedLight(
                new BlockPos(p.posX, p.posY + (double) p.getEyeHeight(), p.posZ), 0);
        float x = (float) (light & 65535);
        float y = (float) (light >> 16);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, x, y);
    }

    private static void rotateWithPlayer(EntityPlayerSP p, float partialTicks) {
        float armPitch = p.prevRenderArmPitch + (p.renderArmPitch - p.prevRenderArmPitch) * partialTicks;
        float armYaw = p.prevRenderArmYaw + (p.renderArmYaw - p.prevRenderArmYaw) * partialTicks;
        GlStateManager.rotate((p.rotationPitch - armPitch) * 0.1F, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate((p.rotationYaw - armYaw) * 0.1F, 0.0F, 1.0F, 0.0F);
    }
}
