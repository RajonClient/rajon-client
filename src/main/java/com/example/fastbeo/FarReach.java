package com.example.fastbeo;

import java.util.List;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod(modid = "rajon", name = Client.NAME, version = "1.0", clientSideOnly = true)
public class FarReach {
    private double REACH = 20.0;

    public static boolean reachEnabled = false;
    public static Scaffold scaffold;
    public static AutoClicker autoClicker;
    public static ClickDelay clickDelay;
    public static boolean allowMultiplayer = false;
    public static boolean hudEnabled = true;
    public static boolean arrayListEnabled = true;
    public static AutoSprint autoSprint;
    public static Fullbright fullbright;
    public static Esp esp;
    public static AimAssist aimAssist;
    public static boolean rotationsEnabled = false;
    public static NoHurtCam noHurtCam;
    public static TriggerBot triggerBot;
    public static Freelook freelook;
    public static Animations animations;

    public static boolean canRun() {
        return Minecraft.getMinecraft().isSingleplayer() || allowMultiplayer;
    }

    public static FastPlace fastPlace;
    public static LegitScaffold legitScaffold;

    private final Minecraft mc = Minecraft.getMinecraft();

    @EventHandler
    public void init(FMLInitializationEvent e) {
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new MainMenuHook());
        MinecraftForge.EVENT_BUS.register(new Hud());

        scaffold = new Scaffold();
        FMLCommonHandler.instance().bus().register(scaffold);

        autoClicker = new AutoClicker();
        FMLCommonHandler.instance().bus().register(autoClicker);

        clickDelay = new ClickDelay();
        FMLCommonHandler.instance().bus().register(clickDelay);

        autoSprint = new AutoSprint();
        FMLCommonHandler.instance().bus().register(autoSprint);

        fullbright = new Fullbright();
        FMLCommonHandler.instance().bus().register(fullbright);

        esp = new Esp();
        MinecraftForge.EVENT_BUS.register(esp);

        noHurtCam = new NoHurtCam();
        FMLCommonHandler.instance().bus().register(noHurtCam);

        MovementFix movementFix = new MovementFix();
        MinecraftForge.EVENT_BUS.register(movementFix);
        FMLCommonHandler.instance().bus().register(movementFix);

        animations = new Animations();
        MinecraftForge.EVENT_BUS.register(animations);

        freelook = new Freelook();
        FMLCommonHandler.instance().bus().register(freelook);

        triggerBot = new TriggerBot();
        FMLCommonHandler.instance().bus().register(triggerBot);

        aimAssist = new AimAssist();
        FMLCommonHandler.instance().bus().register(aimAssist);

        Binds.load();
        HudLayout.load();
        FMLCommonHandler.instance().bus().register(new Binds());

        fastPlace = new FastPlace();
        FMLCommonHandler.instance().bus().register(fastPlace);

        legitScaffold = new LegitScaffold();
        FMLCommonHandler.instance().bus().register(legitScaffold);

        ClientRegistry.registerKeyBinding(GuiOpener.KEY);
        FMLCommonHandler.instance().bus().register(new GuiOpener());

        // Last: every module exists now, restore the autosaved state
        ConfigManager.loadAuto();
    }

    @SubscribeEvent
    public void onMouse(MouseEvent e) {
        if (!reachEnabled) return;
        if (!e.buttonstate || (e.button != 0 && e.button != 1)) return;
        if (mc.currentScreen != null || mc.thePlayer == null || mc.theWorld == null) return;

        // Singleplayer, or multiplayer when 'Allow Multiplayer' is on
        if (!canRun()) return;

        REACH = Settings.get("Reach", "Distance");

        final IntegratedServer server = mc.getIntegratedServer();
        final boolean solo = server != null && mc.isSingleplayer();

        // If vanilla already targets something in range, let vanilla handle it
        if (mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit != MovingObjectType.MISS) return;

        final UUID uuid = mc.thePlayer.getUniqueID();
        Vec3 eye = mc.thePlayer.getPositionEyes(1.0F);

        MovingObjectPosition blockHit = mc.thePlayer.rayTrace(REACH, 1.0F);
        boolean hasBlock = blockHit != null && blockHit.typeOfHit == MovingObjectType.BLOCK;
        double blockDist = hasBlock ? eye.distanceTo(blockHit.hitVec) : Double.MAX_VALUE;

        Entity target = null;
        double entityDist = Double.MAX_VALUE;
        if (e.button == 0) {
            Object[] r = findEntity(eye);
            if (r != null) {
                target = (Entity) r[0];
                entityDist = (Double) r[1];
            }
        }

        // Multiplayer: go through the normal client actions, the server decides what is valid
        if (!solo) {
            if (target != null && entityDist < blockDist) {
                mc.playerController.attackEntity(mc.thePlayer, target);
                mc.thePlayer.swingItem();
            } else if (hasBlock) {
                BlockPos hitPos = blockHit.getBlockPos();
                if (e.button == 1) {
                    mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld,
                            mc.thePlayer.getHeldItem(), hitPos, blockHit.sideHit, blockHit.hitVec);
                } else {
                    mc.playerController.clickBlock(hitPos, blockHit.sideHit);
                }
                mc.thePlayer.swingItem();
            }
            return;
        }

        if (target != null && entityDist < blockDist) {
            final int id = target.getEntityId();
            mc.thePlayer.swingItem();
            server.addScheduledTask(new Runnable() {
                public void run() {
                    EntityPlayerMP sp = server.getConfigurationManager().getPlayerByUUID(uuid);
                    if (sp == null) return;
                    Entity t = sp.worldObj.getEntityByID(id);
                    if (t != null) sp.attackTargetEntityWithCurrentItem(t);
                }
            });
        } else if (hasBlock) {
            final BlockPos pos = blockHit.getBlockPos();
            final EnumFacing side = blockHit.sideHit;
            final float hx = (float) (blockHit.hitVec.xCoord - pos.getX());
            final float hy = (float) (blockHit.hitVec.yCoord - pos.getY());
            final float hz = (float) (blockHit.hitVec.zCoord - pos.getZ());
            final boolean breakBlock = e.button == 0;
            mc.thePlayer.swingItem();
            server.addScheduledTask(new Runnable() {
                public void run() {
                    EntityPlayerMP sp = server.getConfigurationManager().getPlayerByUUID(uuid);
                    if (sp == null) return;
                    if (breakBlock) {
                        sp.theItemInWorldManager.tryHarvestBlock(pos);
                    } else {
                        sp.theItemInWorldManager.activateBlockOrUseItem(
                                sp, sp.worldObj, sp.getHeldItem(), pos, side, hx, hy, hz);
                    }
                }
            });
        }
    }

    private Object[] findEntity(Vec3 eye) {
        Entity viewer = mc.getRenderViewEntity();
        Vec3 look = viewer.getLook(1.0F);
        Vec3 end = eye.addVector(look.xCoord * REACH, look.yCoord * REACH, look.zCoord * REACH);
        List<Entity> list = mc.theWorld.getEntitiesWithinAABBExcludingEntity(viewer,
                viewer.getEntityBoundingBox()
                        .addCoord(look.xCoord * REACH, look.yCoord * REACH, look.zCoord * REACH)
                        .expand(1.0, 1.0, 1.0));
        Entity best = null;
        double bestDist = REACH;
        for (Entity en : list) {
            if (!en.canBeCollidedWith()) continue;
            float b = en.getCollisionBorderSize();
            AxisAlignedBB bb = en.getEntityBoundingBox().expand(b, b, b);
            MovingObjectPosition hit = bb.calculateIntercept(eye, end);
            if (hit != null) {
                double d = eye.distanceTo(hit.hitVec);
                if (d < bestDist) {
                    bestDist = d;
                    best = en;
                }
            }
        }
        return best == null ? null : new Object[] { best, bestDist };
    }
}
