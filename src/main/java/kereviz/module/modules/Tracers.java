package kereviz.module.modules;

import kereviz.Kereviz;
import kereviz.enums.ChatColors;
import kereviz.event.EventTarget;
import kereviz.events.Render2DEvent;
import kereviz.events.Render3DEvent;
import kereviz.mixin.IAccessorMinecraft;
import kereviz.module.Module;
import kereviz.util.RenderUtil;
import kereviz.util.RotationUtil;
import kereviz.util.TeamUtil;
import kereviz.property.properties.BooleanProperty;
import kereviz.property.properties.PercentProperty;
import kereviz.property.properties.ModeProperty;
import kereviz.property.properties.IntProperty;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;

import java.awt.*;
import java.util.List;

public class Tracers extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    public final ModeProperty colorMode = new ModeProperty("color", 0, new String[]{"DEFAULT", "TEAMS", "HUD"});
    public final BooleanProperty drawLines = new BooleanProperty("lines", true);
    public final BooleanProperty drawArrows = new BooleanProperty("arrows", false);
    public final PercentProperty opacity = new PercentProperty("opacity", 100);
    public final IntProperty distance = new IntProperty("distance", 512, 0, 512);
    public final BooleanProperty showPlayers = new BooleanProperty("players", true);
    public final BooleanProperty showFriends = new BooleanProperty("friends", true);
    public final BooleanProperty showEnemies = new BooleanProperty("enemies", true);
    public final BooleanProperty showBots = new BooleanProperty("bots", false);

    private boolean shouldRender(EntityPlayer entityPlayer) {
        if (entityPlayer.deathTime > 0) {
            return false;
        } else if (mc.getRenderViewEntity().getDistanceToEntity(entityPlayer) > (float) this.distance.getValue()) {
            return false;
        } else if (entityPlayer != mc.thePlayer && entityPlayer != mc.getRenderViewEntity()) {
            if (TeamUtil.isBot(entityPlayer)) {
                return this.showBots.getValue();
            } else if (TeamUtil.isFriend(entityPlayer)) {
                return this.showFriends.getValue();
            } else {
                return TeamUtil.isTarget(entityPlayer) ? this.showEnemies.getValue() : this.showPlayers.getValue();
            }
        } else {
            return false;
        }
    }

    private Color getEntityColor(EntityPlayer entityPlayer, float alpha) {
        if (TeamUtil.isFriend(entityPlayer)) {
            Color color = Kereviz.friendManager.getColor();
            return new Color((float) color.getRed() / 255.0F, (float) color.getGreen() / 255.0F, (float) color.getBlue() / 255.0F, alpha);
        } else if (TeamUtil.isTarget(entityPlayer)) {
            Color color = Kereviz.targetManager.getColor();
            return new Color((float) color.getRed() / 255.0F, (float) color.getGreen() / 255.0F, (float) color.getBlue() / 255.0F, alpha);
        } else {
            switch (this.colorMode.getValue()) {
                case 0:
                    return TeamUtil.getTeamColor(entityPlayer, alpha);
                case 1:
                    int teamColor = TeamUtil.isSameTeam(entityPlayer) ? ChatColors.BLUE.toAwtColor() : ChatColors.RED.toAwtColor();
                    return new Color(teamColor & Color.WHITE.getRGB() | (int) (alpha * 255.0F) << 24, true);
                case 2:
                    int color = ((HUD) Kereviz.moduleManager.modules.get(HUD.class)).getColor(System.currentTimeMillis()).getRGB();
                    return new Color(color & Color.WHITE.getRGB() | (int) (alpha * 255.0F) << 24, true);
                default:
                    return new Color(1.0F, 1.0F, 1.0F, alpha);
            }
        }
    }

    public Tracers() {
        super("Tracers", false);
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (this.isEnabled() && this.drawLines.getValue()) {
            RenderUtil.enableRenderState();
            Vec3 position;
            if (mc.gameSettings.thirdPersonView == 0) {
                position = new Vec3(0.0, 0.0, 1.0)
                        .rotatePitch(
                                (float) (
                                        -Math.toRadians(
                                                RenderUtil.lerpFloat(
                                                        mc.getRenderViewEntity().rotationPitch,
                                                        mc.getRenderViewEntity().prevRotationPitch,
                                                        ((IAccessorMinecraft) mc).getTimer().renderPartialTicks
                                                )
                                        )
                                )
                        )
                        .rotateYaw(
                                (float) (
                                        -Math.toRadians(
                                                RenderUtil.lerpFloat(
                                                        mc.getRenderViewEntity().rotationYaw,
                                                        mc.getRenderViewEntity().prevRotationYaw,
                                                        ((IAccessorMinecraft) mc).getTimer().renderPartialTicks
                                                )
                                        )
                                )
                        );
            } else {
                position = new Vec3(0.0, 0.0, 0.0)
                        .rotatePitch(
                                (float) (
                                        -Math.toRadians(
                                                RenderUtil.lerpFloat(
                                                        mc.thePlayer.cameraPitch, mc.thePlayer.prevCameraPitch, ((IAccessorMinecraft) mc).getTimer().renderPartialTicks
                                                )
                                        )
                                )
                        )
                        .rotateYaw(
                                (float) (
                                        -Math.toRadians(
                                                RenderUtil.lerpFloat(mc.thePlayer.cameraYaw, mc.thePlayer.prevCameraYaw, ((IAccessorMinecraft) mc).getTimer().renderPartialTicks)
                                        )
                                )
                        );
            }
            position = new Vec3(position.xCoord, position.yCoord + (double) mc.getRenderViewEntity().getEyeHeight(), position.zCoord);
            List<Entity> loadedEntities = TeamUtil.getLoadedEntitiesSorted();
            for (Entity entity : loadedEntities) {
                if (!(entity instanceof EntityPlayer)) {
                    continue;
                }
                EntityPlayer player = (EntityPlayer) entity;
                if (!this.shouldRender(player)) {
                    continue;
                }
                Color color = this.getEntityColor(player, (float) this.opacity.getValue() / 100.0F);
                double x = RenderUtil.lerpDouble(player.posX, player.lastTickPosX, event.getPartialTicks());
                double y = RenderUtil.lerpDouble(player.posY, player.lastTickPosY, event.getPartialTicks()) - (player.isSneaking() ? 0.125 : 0.0);
                double z = RenderUtil.lerpDouble(player.posZ, player.lastTickPosZ, event.getPartialTicks());
                RenderUtil.drawLine3D(
                        position,
                        x,
                        y + (double) player.getEyeHeight(),
                        z,
                        (float) color.getRed() / 255.0F,
                        (float) color.getGreen() / 255.0F,
                        (float) color.getBlue() / 255.0F,
                        (float) color.getAlpha() / 255.0F,
                        1.5F
                );
            }
            RenderUtil.disableRenderState();
        }
    }

    @EventTarget
    public void onRender(Render2DEvent event) {
        if (this.isEnabled() && this.drawArrows.getValue()) {
            HUD hud = (HUD) Kereviz.moduleManager.modules.get(HUD.class);
            float hudScale = hud.scale.getValue();
            ScaledResolution scaledResolution = new ScaledResolution(mc);
            float centerX = (float) scaledResolution.getScaledWidth() / 2.0F / hudScale;
            float centerY = (float) scaledResolution.getScaledHeight() / 2.0F / hudScale;
            List<Entity> loadedEntities = TeamUtil.getLoadedEntitiesSorted();
            for (Entity entity : loadedEntities) {
                if (!(entity instanceof EntityPlayer)) {
                    continue;
                }
                EntityPlayer player = (EntityPlayer) entity;
                if (!this.shouldRender(player)) {
                    continue;
                }
                float yawBetween = RotationUtil.getYawBetween(
                        RenderUtil.lerpDouble(mc.thePlayer.posX, mc.thePlayer.prevPosX, event.getPartialTicks()),
                        RenderUtil.lerpDouble(mc.thePlayer.posZ, mc.thePlayer.prevPosZ, event.getPartialTicks()),
                        RenderUtil.lerpDouble(player.posX, player.prevPosX, event.getPartialTicks()),
                        RenderUtil.lerpDouble(player.posZ, player.prevPosZ, event.getPartialTicks())
                );
                if (mc.gameSettings.thirdPersonView == 2) {
                    yawBetween += 180.0F;
                }
                float arrowDirX = (float) Math.sin(Math.toRadians(yawBetween));
                float arrowDirY = (float) Math.cos(Math.toRadians(yawBetween)) * -1.0F;
                float opacity = this.opacity.getValue().floatValue() / 100.0F;
                yawBetween = Math.abs(MathHelper.wrapAngleTo180_float(yawBetween));
                if (yawBetween < 30.0F) {
                    opacity = 0.0F;
                } else if (yawBetween < 60.0F) {
                    opacity *= (yawBetween - 30.0F) / 30.0F;
                }
                GlStateManager.pushMatrix();
                GlStateManager.scale(hudScale, hudScale, 0.0F);
                GlStateManager.translate(
                        centerX,
                        centerY,
                        0.0F
                );
                GlStateManager.pushMatrix();
                GlStateManager.translate(55.0F * arrowDirX + 1.0F, 55.0F * arrowDirY + 1.0F, -100.0F);
                RenderUtil.enableRenderState();
                RenderUtil.drawTriangle(
                        0.0F,
                        0.0F,
                        (float) (Math.atan2(arrowDirY, arrowDirX) + Math.PI),
                        10.0F,
                        this.getEntityColor(player, opacity).getRGB()
                );
                RenderUtil.disableRenderState();
                GlStateManager.popMatrix();
                GlStateManager.popMatrix();
            }
        }
    }
}
